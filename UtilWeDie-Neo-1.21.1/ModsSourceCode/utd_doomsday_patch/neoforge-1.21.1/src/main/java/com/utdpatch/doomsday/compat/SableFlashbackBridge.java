package com.utdpatch.doomsday.compat;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFinalizeSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStartTrackingSubLevelPacket;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector2i;

/**
 * Sable x Flashback bridge: makes physics structures appear in replays.
 *
 * Flashback records the vanilla TCP packet stream. With sable's UDP pipeline
 * disabled (sable-common.toml, disable_udp_pipeline=true) the structure
 * MOVEMENT packets are recorded fine — but a structure's initial full sync
 * (start-tracking + plot chunks + finalize, sent once when a player begins
 * tracking it) usually happened before the recording started, so replays have
 * movement packets referencing structures that don't exist (sable#705).
 *
 * This bridge runs at Flashback's official snapshot hook
 * ({@code Recorder.writeCustomSnapshot}) and synthesizes, from CLIENT state,
 * the exact packet sequence sable's own SubLevelTrackingSystem.sendFullSync
 * produces: for every currently tracked sub level, a StartTracking payload,
 * one vanilla chunk-with-light packet per loaded plot chunk (client chunks +
 * plot light engine), and a Finalize payload. They are appended to the
 * snapshot, so on playback the client rebuilds every structure before the
 * recorded movement packets arrive.
 */
public final class SableFlashbackBridge {
    private static final Logger LOGGER = LogManager.getLogger("utd_doomsday_patch");

    private SableFlashbackBridge() {
    }

    private static boolean replayCheckInit;
    private static java.lang.reflect.Method isInReplayMethod;

    /** True while the client is inside a Flashback replay world (reflection gate). */
    public static boolean isInReplay() {
        if (!replayCheckInit) {
            replayCheckInit = true;
            try {
                isInReplayMethod = Class.forName("com.moulberry.flashback.Flashback").getMethod("isInReplay");
            } catch (ReflectiveOperationException e) {
                LOGGER.warn("[SABLE-REPLAY] Flashback.isInReplay reflection init failed", e);
            }
        }
        if (isInReplayMethod == null) {
            return false;
        }
        try {
            return (boolean) isInReplayMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public static void writeSableSnapshot(Consumer<Packet<? super ClientGamePacketListener>> consumer) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.info("[SABLE-REPLAY] snapshot hook fired but client level is null, skipping");
            return;
        }
        SubLevelContainer generic = SubLevelContainer.getContainer(level);
        if (!(generic instanceof ClientSubLevelContainer container)) {
            LOGGER.info("[SABLE-REPLAY] snapshot hook fired but no client sub-level container, skipping");
            return;
        }
        Vector2i origin = container.getOrigin();
        // Same tick base the recorded movement (dual snapshot) packets use, so
        // the interpolator picks up seamlessly after the synthetic full sync.
        int tick = (int) container.getInterpolation().mostRecentInterpolationTick;

        // Everything (sable payloads AND chunk packets) is serialized and wrapped
        // in our carrier payload: Flashback forwards custom payloads raw to the
        // viewer, while it would swallow vanilla chunk packets into the replay
        // server's world (plot coords are beyond view distance, never streamed).
        var codec = net.minecraft.network.protocol.game.GameProtocols.CLIENTBOUND_TEMPLATE
                .bind(net.minecraft.network.RegistryFriendlyByteBuf.decorator(level.registryAccess()))
                .codec();
        Consumer<Packet<? super ClientGamePacketListener>> carrier = packet -> {
            io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
            codec.encode(buf, packet);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            consumer.accept(new ClientboundCustomPayloadPacket(new SableReplayPayload(bytes)));
        };

        int structures = 0;
        for (SubLevel sub : new ArrayList<>(container.getAllSubLevels())) {
            if (!(sub instanceof ClientSubLevel clientSub)) {
                continue;
            }
            LevelPlot plot = clientSub.getPlot();
            long plotCoordinate = ChunkPos.asLong(plot.plotPos.x - origin.x, plot.plotPos.z - origin.y);

            carrier.accept(new ClientboundCustomPayloadPacket(new ClientboundStartTrackingSubLevelPacket(
                    plotCoordinate,
                    clientSub.getUniqueId(),
                    clientSub.lastPose(),
                    new Pose3d(clientSub.logicalPose()),
                    plot.getBoundingBox(),
                    clientSub.getName(),
                    tick)));

            int chunks = 0;
            int skippedChunks = 0;
            for (PlotChunkHolder holder : plot.getLoadedChunks()) {
                LevelChunk chunk = holder.getChunk();
                if (chunk == null) {
                    continue;
                }
                try {
                    // Building the packet client-side runs each block entity's
                    // getUpdateTag; some modded BEs assume server context there
                    // (e.g. aeronautics' hot air burner) — skip those chunks
                    // rather than aborting the whole structure.
                    carrier.accept(new ClientboundLevelChunkWithLightPacket(chunk, plot.getLightEngine(), null, null));
                    chunks++;
                } catch (Throwable t) {
                    skippedChunks++;
                    LOGGER.warn("[SABLE-REPLAY] skipped plot chunk {} of structure {} ({}: {})",
                            chunk.getPos(), clientSub.getUniqueId(),
                            t.getClass().getSimpleName(), t.getMessage());
                }
            }

            carrier.accept(new ClientboundCustomPayloadPacket(new ClientboundFinalizeSubLevelPacket(plotCoordinate)));
            structures++;
            LOGGER.info("[SABLE-REPLAY] snapshot: structure {} plot={} chunks={} skipped={} name={}",
                    clientSub.getUniqueId(), plot.plotPos, chunks, skippedChunks, clientSub.getName());
        }

        // Entities living in plot space (Create SeatEntities, aboard mobs...):
        // the replay server re-streams entities by view distance, which never
        // reaches plot coordinates — deliver them (and their riding links, e.g.
        // seated vehicles) through the carrier so the viewer client rebuilds
        // TRUE riding: passengers then follow the structure per-frame via
        // sable's pose interpolation, instead of twitching on tick snaps.
        int plotEntities = 0;
        for (net.minecraft.world.entity.Entity entity : level.entitiesForRendering()) {
            if (!generic.inBounds(entity.blockPosition())) {
                continue;
            }
            // 1. Add the seat/vehicle entity first.
            carrier.accept(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(
                    entity.getId(), entity.getUUID(),
                    entity.getX(), entity.getY(), entity.getZ(),
                    entity.getXRot(), entity.getYRot(),
                    entity.getType(), 0, entity.getDeltaMovement(), entity.getYHeadRot()));
            plotEntities++;
            // 2. Add all passenger entities BEFORE sending SetPassengersPacket.
            // Passenger entities (e.g. Superb vehicles riding a Create seat) live in the
            // main world (not the sable sub-level) and are therefore invisible to the
            // entitiesForRendering() loop. Without capturing them, FlashbackClearEntities
            // will erase them on seek and they are never re-injected.
            // SetPassengersPacket must be sent AFTER the passengers' AddEntityPackets;
            // otherwise the client cannot resolve the passenger IDs and the riding
            // relationship is silently dropped (passenger falls as a free entity).
            for (net.minecraft.world.entity.Entity passenger : entity.getPassengers()) {
                carrier.accept(new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(
                        passenger.getId(), passenger.getUUID(),
                        passenger.getX(), passenger.getY(), passenger.getZ(),
                        passenger.getXRot(), passenger.getYRot(),
                        passenger.getType(), 0,
                        passenger.getDeltaMovement(),
                        passenger.getYHeadRot()));
                if (!passenger.getPassengers().isEmpty()) {
                    carrier.accept(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(passenger));
                }
                plotEntities++;
                LOGGER.info("[SABLE-REPLAY] snapshot: captured passenger {} type={} of structure entity {}",
                        passenger.getId(), net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(passenger.getType()), entity.getId());
            }
            // 3. Now that all passengers exist on the client, establish the riding link.
            if (!entity.getPassengers().isEmpty()) {
                carrier.accept(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(entity));
            }
        }
        LOGGER.info("[SABLE-REPLAY] snapshot injection done: {} structure(s), {} plot entities, interpolationTick={}",
                structures, plotEntities, tick);
    }
}
