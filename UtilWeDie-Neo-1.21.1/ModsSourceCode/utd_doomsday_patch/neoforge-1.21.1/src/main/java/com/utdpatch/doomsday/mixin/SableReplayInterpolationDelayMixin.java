package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.compat.SableFlashbackBridge;
import dev.ryanhcode.sable.network.client.ClientSableInterpolationState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zero structure interpolation delay inside Flashback replays.
 *
 * Sable renders structure poses through a snapshot jitter buffer
 * (getTickPointer = clock - INTERPOLATION_DELAY, default 1.5 ticks): it always
 * interpolates between two CONFIRMED snapshots instead of extrapolating. On a
 * real network that hides jitter; in a replay everything else (camera, player
 * animation, particles, Flashback's per-tick entity positions) plays on the
 * exact recorded timeline — so the whole structure system visibly trails by
 * the buffer.
 *
 * Replay packets come from the local replay server with perfectly regular
 * pacing, so the buffer is pure latency there: return 0 while in a replay.
 * Live play (including future dedicated-server use) keeps the configured
 * default smoothing untouched.
 */
@Pseudo
@Mixin(value = ClientSableInterpolationState.class, remap = false)
public abstract class SableReplayInterpolationDelayMixin {
    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");
    private static boolean utd$loggedActive;
    private static long utd$lastTelemetryMs;

    @org.spongepowered.asm.mixin.Shadow(remap = false)
    private double mostRecentTick;
    @org.spongepowered.asm.mixin.Shadow(remap = false)
    private double interpolationTick;
    @org.spongepowered.asm.mixin.Shadow(remap = false)
    private double latestDelay;

    @Inject(method = "getInterpolationDelay", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utd$zeroDelayInReplay(CallbackInfoReturnable<Double> cir) {
        if (SableFlashbackBridge.isInReplay()) {
            if (!utd$loggedActive) {
                utd$loggedActive = true;
                UTD$LOGGER.info("[SABLE-REPLAY] interpolation delay override ACTIVE: 0 ticks (replay world)");
            }
            cir.setReturnValue(0.0D);
        } else if (utd$loggedActive) {
            utd$loggedActive = false;
            UTD$LOGGER.info("[SABLE-REPLAY] interpolation delay override released (left replay)");
        }
    }

    /**
     * Clock telemetry (1 line/second in replay): quantifies exactly where any
     * remaining structure lag lives. pointerLag = mostRecentTick -
     * interpolationTick; positive values mean the visual pose trails the newest
     * received movement data by that many ticks.
     */
    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void utd$clockTelemetry(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!SableFlashbackBridge.isInReplay()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - utd$lastTelemetryMs < 1000L) {
            return;
        }
        utd$lastTelemetryMs = now;
        UTD$LOGGER.info("[SABLE-CLOCK] mostRecentTick={} interpolationTick={} pointerLag={} latestDelay={}",
                String.format("%.2f", this.mostRecentTick),
                String.format("%.2f", this.interpolationTick),
                String.format("%.2f", this.mostRecentTick - this.interpolationTick),
                String.format("%.2f", this.latestDelay));
    }
}
