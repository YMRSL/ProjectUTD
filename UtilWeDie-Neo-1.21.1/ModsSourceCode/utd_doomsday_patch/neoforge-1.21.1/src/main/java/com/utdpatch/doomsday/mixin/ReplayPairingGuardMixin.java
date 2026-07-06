package com.utdpatch.doomsday.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Safety net for Flashback playback: entities on the replay server are shells
 * rebuilt from recorded vanilla packets, so modded spawn-data writers that
 * assume server-side state (Create contraptions' controllerPos, vehicle
 * internals, ...) can throw during viewer pairing — which crashes the whole
 * replay server for one bad entity. On a Flashback replay server only, catch
 * the throw, log which entity, and skip its pairing data (the entity may
 * render incomplete instead of killing the replay). Normal servers rethrow.
 */
@Mixin(ServerEntity.class)
public class ReplayPairingGuardMixin {
    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");

    @Shadow
    @Final
    private Entity entity;

    @WrapMethod(method = "sendPairingData")
    private void utd$guardReplayPairing(ServerPlayer player,
                                        PacketAndPayloadAcceptor<ClientGamePacketListener> acceptor,
                                        Operation<Void> original) {
        try {
            original.call(player, acceptor);
        } catch (RuntimeException e) {
            MinecraftServer server = this.entity.getServer();
            boolean isReplay = server != null
                    && server.getClass().getName().startsWith("com.moulberry.flashback");
            if (!isReplay) {
                throw e;
            }
            UTD$LOGGER.error("[UTD-PATCH] Skipping pairing data for {} ({}) in replay - "
                    + "mod spawn data incompatible with replay shell entity",
                    this.entity.getType(), this.entity.getId(), e);
        }
    }
}
