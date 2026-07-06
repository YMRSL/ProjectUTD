package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.compat.SableReplayClientHandler;
import com.utdpatch.doomsday.compat.SableReplayPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes sable entity/structure replay during Flashback seek (progress-bar scrub).
 *
 * NeoForge overrides ClientPacketListener.handleCustomPayload to accept the
 * already-unwrapped {@link CustomPacketPayload} (not the whole
 * {@code ClientboundCustomPayloadPacket}). This is why using
 * {@code ClientboundCustomPayloadPacket} as the parameter type caused
 * "Invalid descriptor" — the actual bytecode signature is
 * {@code (CustomPacketPayload, CallbackInfo)V}.
 */
@Mixin(value = ClientPacketListener.class, priority = 900)
public class SablePayloadDirectDispatchMixin {

    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");

    @Inject(
            method = "handleCustomPayload",
            at = @At("HEAD"),
            cancellable = true
    )
    private void utd$directDispatchSableReplayPayload(CustomPacketPayload payload, CallbackInfo ci) {
        if (!(payload instanceof SableReplayPayload sablePayload)) {
            return;
        }

        // Cancel NeoForge's deferred IPayloadHandler dispatch for SableReplayPayload.
        ci.cancel();

        ClientPacketListener self = (ClientPacketListener) (Object) this;

        if (Minecraft.getInstance().level == null) {
            SableReplayClientHandler.enqueue(sablePayload);
            return;
        }

        // Dispatch the inner game packet immediately on the current call stack
        // (main client thread, called from Flashback's raw-payload handler).
        try {
            var codec = GameProtocols.CLIENTBOUND_TEMPLATE
                    .bind(RegistryFriendlyByteBuf.decorator(self.registryAccess()))
                    .codec();
            Packet<? super ClientGamePacketListener> inner =
                    codec.decode(Unpooled.wrappedBuffer(sablePayload.data()));
            inner.handle(self);
            SableReplayClientHandler.countDirectDispatched();
        } catch (Throwable t) {
            UTD$LOGGER.error("[SABLE-REPLAY] direct dispatch failed ({} bytes): {}",
                    sablePayload.data().length, t.toString());
            SableReplayClientHandler.countDirectFailed();
        }
    }
}



