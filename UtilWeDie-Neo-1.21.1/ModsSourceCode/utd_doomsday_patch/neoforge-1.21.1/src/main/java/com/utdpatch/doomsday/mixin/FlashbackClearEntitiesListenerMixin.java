package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.compat.SableReplayClientHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks Flashback's FlashbackClearEntities client handler to emit a
 * [SABLE-REPLAY] SEEK log line whenever a seek/snapshot-reload begins.
 * This lets us correlate the seek event with subsequent HANDLER calls
 * in the log to verify that snapshot carriers arrive AFTER the clear.
 */
@Pseudo
@Mixin(targets = "com.moulberry.flashback.Flashback", remap = false)
public class FlashbackClearEntitiesListenerMixin {

    /**
     * Flashback's FlashbackClearEntities client handler is a lambda registered
     * via ClientPlayNetworking.registerGlobalReceiver. We can't mixin a lambda
     * directly, so we hook the discard loop's side-effect instead: every entity
     * in the level is discarded during the seek clear, which triggers
     * Entity.discard(). We approximate the seek event by hooking the static
     * Minecraft.getInstance() call that starts the iteration — but that's too
     * fragile.
     *
     * Simpler alternative: hook Level.entitiesForRendering being called from the
     * Flashback lambda context. Still fragile.
     *
     * ACTUAL approach used here: hook the Flashback class's lambda-generated
     * synthetic method that handles FlashbackClearEntities. In Flashback 0.39.5
     * the lambda body for FlashbackClearEntities is compiled into a synthetic
     * static method named lambda$onInitializeClient$N inside Flashback.java.
     * Because the exact synthetic name varies by build, we instead mixin on the
     * first entity discard call that happens in that lambda — which is the
     * for-loop over entitiesForRendering calling entity.discard().
     *
     * Given the complexity, we use a lightweight workaround: watch for
     * FlashbackClearEntities payload being registered (onInitialize) and count
     * clears ourselves.  The HANDLER log lines are sufficient for diagnosis; a
     * dedicated SEEK log line is optional.  Leave this mixin as a stub for now
     * — the critical logging is already in handleFromNeoForge.
     */
    // No injections — stub class kept for documentation purposes.
    // If the seek-start log line is needed, the simplest approach is to
    // hook SableReplayClientHandler.TickFlusher.onLoggingOut which clears
    // the queue (that event already logs the clear).
}
