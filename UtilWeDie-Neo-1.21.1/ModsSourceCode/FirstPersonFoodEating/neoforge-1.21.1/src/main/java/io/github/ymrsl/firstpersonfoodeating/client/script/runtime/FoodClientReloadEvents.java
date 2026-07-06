package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.diagnostic.BootTrace;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.ModList;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class FoodClientReloadEvents {
    private static final String DEFERRED_BOOTSTRAP_TOGGLE = "firstpersonfoodeating.deferReloadListener";

    private FoodClientReloadEvents() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        boolean deferred = shouldUseDeferredBootstrap();
        BootTrace.event(
                "reload.listeners.mode",
                "deferred=" + deferred
                        + ", overrideProp=" + System.getProperty(DEFERRED_BOOTSTRAP_TOGGLE)
                        + ", connectormod=" + ModList.get().isLoaded("connectormod")
                        + ", connectorextras=" + ModList.get().isLoaded("connectorextras")
        );
        if (deferred) {
            BootTrace.event("reload.listeners.skip",
                    "skip registering FoodAssetsManager listener; deferred bootstrap enabled");
            FoodDeferredReloadBootstrap.arm();
            return;
        }
        BootTrace.event("reload.listeners.register", "registering FoodAssetsManager listener");
        event.registerReloadListener(FoodAssetsManager.get());
    }

    private static boolean shouldUseDeferredBootstrap() {
        String forced = System.getProperty(DEFERRED_BOOTSTRAP_TOGGLE);
        if (forced != null) {
            return Boolean.parseBoolean(forced);
        }
        // Default to the standard Forge reload listener path.
        // Deferred bootstrap can still be enabled explicitly with:
        // -Dfirstpersonfoodeating.deferReloadListener=true
        return false;
    }
}
