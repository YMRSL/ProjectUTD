package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.compat.SableReplayPayload;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundChangeBoundsSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFinalizeSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundRecentlySplitSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStartTrackingSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStopTrackingSubLevelPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Live-recording half of the sable x Flashback bridge (the snapshot half is
 * FlashbackSableSnapshotMixin). When a structure physicalizes DURING a
 * recording, the original server's full sync goes through the normal packet
 * stream: its plot CHUNK packets would be interpreted into the replay server's
 * world on playback (plot coords are beyond view distance — never streamed to
 * the viewer), so the structure appears empty. Rewrite, at record time:
 *
 *  - vanilla chunk packets whose position lies inside a sable plot, and
 *  - sable's order-sensitive state payloads (start/finalize/split/bounds/stop)
 *
 * into {@link SableReplayPayload} carriers, which playback forwards raw and
 * the client handler replays locally in order. High-volume movement payloads
 * stay on the raw path (timing-sensitive, already working).
 */
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder", remap = false)
public abstract class FlashbackSableLivePacketMixin {
    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");
    private static long utd$wrappedChunks;
    private static long utd$wrappedPayloads;

    @Shadow(remap = false)
    private StreamCodec<ByteBuf, Packet<? super ClientGamePacketListener>> gamePacketCodec;

    @Shadow(remap = false)
    public abstract void writePacketAsync(Packet<?> packet, ConnectionProtocol phase);

    /** Entity ids spawned in plot space this recording (per-Recorder lifetime). */
    @org.spongepowered.asm.mixin.Unique
    private final it.unimi.dsi.fastutil.ints.IntOpenHashSet utd$plotEntityIds =
            new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    @Inject(method = "writePacketAsync", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utd$rerouteSablePackets(Packet<?> packet, ConnectionProtocol phase, CallbackInfo ci) {
        if (phase != ConnectionProtocol.PLAY || !ModList.get().isLoaded("sable")) {
            return;
        }
        try {
            if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
                if (utd$inSablePlot(chunkPacket.getX(), chunkPacket.getZ())) {
                    utd$wrapAndResubmit(packet, phase);
                    utd$wrappedChunks++;
                    if (utd$wrappedChunks <= 5 || utd$wrappedChunks % 50 == 0) {
                        UTD$LOGGER.info("[SABLE-REPLAY] live: rerouted plot chunk packet [{}, {}] (#{})",
                                chunkPacket.getX(), chunkPacket.getZ(), utd$wrappedChunks);
                    }
                    ci.cancel();
                }
            } else if (packet instanceof ClientboundCustomPayloadPacket payloadPacket
                    && utd$isOrderSensitiveSablePayload(payloadPacket.payload())) {
                utd$wrapAndResubmit(packet, phase);
                utd$wrappedPayloads++;
                UTD$LOGGER.info("[SABLE-REPLAY] live: rerouted sable payload {} (#{})",
                        payloadPacket.payload().type().id(), utd$wrappedPayloads);
                ci.cancel();
            } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket addPacket
                    && utd$inSablePlot(((int) addPacket.getX()) >> 4, ((int) addPacket.getZ()) >> 4)) {
                // Entity spawned in plot space (seat entity, aboard mob): the viewer
                // never receives it through vanilla streaming — carrier it and
                // remember the id so its riding/data/removal packets follow.
                utd$plotEntityIds.add(addPacket.getId());
                utd$wrapAndResubmit(packet, phase);
                UTD$LOGGER.info("[SABLE-REPLAY] live: rerouted plot entity add id={} type={}",
                        addPacket.getId(), addPacket.getType());
                ci.cancel();
            } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundSetPassengersPacket passengersPacket
                    && utd$plotEntityIds.contains(passengersPacket.getVehicle())) {
                utd$wrapAndResubmit(packet, phase);
                UTD$LOGGER.info("[SABLE-REPLAY] live: rerouted riding link vehicle={} passengers={}",
                        passengersPacket.getVehicle(), passengersPacket.getPassengers().length);
                ci.cancel();
            } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket dataPacket
                    && utd$plotEntityIds.contains(dataPacket.id())) {
                utd$wrapAndResubmit(packet, phase);
                ci.cancel();
            } else if (packet instanceof net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket removePacket) {
                it.unimi.dsi.fastutil.ints.IntArrayList mine = new it.unimi.dsi.fastutil.ints.IntArrayList();
                for (int id : removePacket.getEntityIds()) {
                    if (utd$plotEntityIds.contains(id)) {
                        mine.add(id);
                        utd$plotEntityIds.remove(id);
                    }
                }
                if (!mine.isEmpty()) {
                    // Carrier a subset removal for the viewer's carrier-delivered
                    // entities; the original packet still records normally for the
                    // replay server's own bookkeeping.
                    utd$wrapAndResubmit(new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(mine), phase);
                    UTD$LOGGER.info("[SABLE-REPLAY] live: rerouted plot entity removal ids={}", mine);
                }
            }
        } catch (Throwable t) {
            UTD$LOGGER.error("[SABLE-REPLAY] live reroute failed, packet recorded on normal path", t);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean utd$isOrderSensitiveSablePayload(CustomPacketPayload payload) {
        return payload instanceof ClientboundStartTrackingSubLevelPacket
                || payload instanceof ClientboundFinalizeSubLevelPacket
                || payload instanceof ClientboundRecentlySplitSubLevelPacket
                || payload instanceof ClientboundChangeBoundsSubLevelPacket
                || payload instanceof ClientboundStopTrackingSubLevelPacket;
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean utd$inSablePlot(int chunkX, int chunkZ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        SubLevelContainer container = SubLevelContainer.getContainer(level);
        return container != null && container.inBounds(new ChunkPos(chunkX, chunkZ));
    }

    @org.spongepowered.asm.mixin.Unique
    @SuppressWarnings("unchecked")
    private void utd$wrapAndResubmit(Packet<?> packet, ConnectionProtocol phase) {
        ByteBuf buf = Unpooled.buffer();
        this.gamePacketCodec.encode(buf, (Packet<? super ClientGamePacketListener>) packet);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        // Resubmit through the same entry so ordering in pendingPackets is kept.
        // The carrier is itself a ClientboundCustomPayloadPacket, but with OUR
        // payload type it does not match the reroute conditions -> no recursion.
        this.writePacketAsync(new ClientboundCustomPayloadPacket(new SableReplayPayload(bytes)), phase);
    }
}
