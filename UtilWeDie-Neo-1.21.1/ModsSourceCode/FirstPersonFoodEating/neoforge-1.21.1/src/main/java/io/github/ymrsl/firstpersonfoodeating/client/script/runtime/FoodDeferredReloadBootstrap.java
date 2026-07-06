package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.diagnostic.BootTrace;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class FoodDeferredReloadBootstrap {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long RETRY_INTERVAL_MS = 2_000L;
    private static final long WAIT_LOG_INTERVAL_MS = 5_000L;
    private static final long FORCE_ATTEMPT_AFTER_MS =
            Long.getLong("firstpersonfoodeating.deferReload.forceAfterMs", 20_000L);

    private static boolean armed;
    private static boolean done;
    private static long lastAttemptMs;
    private static long lastWaitLogMs;
    private static long armedAtMs;
    private static boolean forceAttemptNotified;
    private static String lastOverlayName = "";
    private static String lastScreenName = "";

    private FoodDeferredReloadBootstrap() {
    }

    public static void arm() {
        armed = true;
        done = false;
        lastAttemptMs = 0L;
        lastWaitLogMs = 0L;
        armedAtMs = System.currentTimeMillis();
        forceAttemptNotified = false;
        lastOverlayName = "";
        lastScreenName = "";
        BootTrace.event("reload.deferred.arm", "forceAfterMs=" + FORCE_ATTEMPT_AFTER_MS);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!armed || done) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAttemptMs < RETRY_INTERVAL_MS) {
            return;
        }
        lastAttemptMs = now;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }
        String overlayName = minecraft.getOverlay() == null
                ? "none"
                : minecraft.getOverlay().getClass().getName();
        String screenName = minecraft.screen == null
                ? "none"
                : minecraft.screen.getClass().getName();
        boolean forceAttempt = armedAtMs > 0L && now - armedAtMs >= FORCE_ATTEMPT_AFTER_MS;

        // Wait until loading overlay is gone to avoid colliding with startup pipeline.
        if (minecraft.getOverlay() != null && !forceAttempt) {
            boolean overlayChanged = !overlayName.equals(lastOverlayName);
            boolean screenChanged = !screenName.equals(lastScreenName);
            if (overlayChanged || screenChanged || now - lastWaitLogMs >= WAIT_LOG_INTERVAL_MS) {
                lastWaitLogMs = now;
                lastOverlayName = overlayName;
                lastScreenName = screenName;
                BootTrace.event(
                        "reload.deferred.wait",
                        "overlay=" + overlayName + ", screen=" + screenName
                );
            }
            return;
        }

        if (minecraft.getOverlay() != null && forceAttempt && !forceAttemptNotified) {
            forceAttemptNotified = true;
            BootTrace.event(
                    "reload.deferred.force_attempt",
                    "overlay still active after wait timeout, forcing reload attempt: overlay="
                            + overlayName + ", screen=" + screenName
            );
        }

        if (minecraft.getOverlay() == null
                && (!"none".equals(lastOverlayName) || !"none".equals(lastScreenName))) {
            BootTrace.event(
                    "reload.deferred.overlay_cleared",
                    "overlay=" + overlayName + ", screen=" + screenName
            );
            lastOverlayName = overlayName;
            lastScreenName = screenName;
        }

        BootTrace.event("reload.deferred.attempt", "manual resource load attempt");
        if (FoodAssetsManager.get().reloadNow(minecraft.getResourceManager(), "deferred_bootstrap")) {
            done = true;
            armed = false;
            LOGGER.info("[{}] Deferred food assets bootstrap completed", FirstPersonFoodEatingMod.MOD_ID);
            BootTrace.event("reload.deferred.done", "manual resource load completed");
        } else {
            BootTrace.event("reload.deferred.retry", "manual resource load failed; will retry");
        }
    }
}
