package com.utdpatch.doomsday.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents duplicate-entity conflicts during Flashback replay seeks.
 *
 * During a seek, our sable carrier snapshot adds entities (e.g. Superb vehicles)
 * to the client world.  Flashback's replay server then fast-forwards through the
 * recording and, via normal vanilla entity tracking, re-sends
 * ClientboundAddEntityPacket for those same entity IDs because they are at
 * world-space coordinates within the replay player's view distance.
 *
 * When handleAddEntity receives a packet for an ID that already exists, it
 * replaces the entity object — breaking the riding relationship established by
 * our SetPassengersPacket and leaving the entity invisible.
 *
 * Fix: if the entity already exists in the client world and is alive, cancel
 * the duplicate add.  Riding entities get their rendered position from their
 * vehicle, so no explicit position update is needed.
 */
@Mixin(ClientPacketListener.class)
public class ReplayAddEntityDeduplicateMixin {

    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");

    @Inject(method = "handleAddEntity", at = @At("HEAD"), cancellable = true)
    private void utd$deduplicateAddEntity(ClientboundAddEntityPacket packet,
                                          CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity existing = mc.level.getEntity(packet.getId());
        if (existing != null && !existing.isRemoved()) {
            // Entity is already alive — this is a duplicate add from Flashback's
            // vanilla entity streaming conflicting with our carrier-added entity.
            // Cancel the re-add so the existing entity (and its riding state) is
            // preserved.
            UTD$LOGGER.info("[SABLE-REPLAY] deduplicate: cancelled re-add of id={} ({})",
                    packet.getId(), existing.getClass().getSimpleName());
            ci.cancel();
        }
    }
}
