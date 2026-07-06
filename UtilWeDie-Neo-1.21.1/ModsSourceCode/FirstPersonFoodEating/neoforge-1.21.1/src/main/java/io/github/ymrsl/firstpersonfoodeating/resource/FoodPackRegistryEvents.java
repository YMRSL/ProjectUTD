package io.github.ymrsl.firstpersonfoodeating.resource;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.diagnostic.BootTrace;
import net.neoforged.fml.ModList;
import net.minecraft.server.packs.PackType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class FoodPackRegistryEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String EXTERNAL_PACKS_TOGGLE = "firstpersonfoodeating.enableExternalFoodPacks";

    private FoodPackRegistryEvents() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        BootTrace.event("pack.finders.event", "packType=" + event.getPackType());
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        boolean enabled = shouldEnableExternalFoodPacks();
        BootTrace.event("pack.finders.compat",
                "external enabled=" + enabled
                        + ", connectormod=" + ModList.get().isLoaded("connectormod")
                        + ", connectorextras=" + ModList.get().isLoaded("connectorextras")
                        + ", overrideProp=" + System.getProperty(EXTERNAL_PACKS_TOGGLE));
        if (!enabled) {
            LOGGER.info("[{}] Skip external FoodsPack repository registration (compat mode). "
                            + "Set -D{}=true to enable.",
                    FirstPersonFoodEatingMod.MOD_ID,
                    EXTERNAL_PACKS_TOGGLE);
            BootTrace.event("pack.finders.skip", "external FoodsPack repository skipped by compat mode");
            return;
        }
        event.addRepositorySource(FoodPackLoader.INSTANCE);
        BootTrace.event("pack.finders.added", "external FoodsPack repository source added");
    }

    private static boolean shouldEnableExternalFoodPacks() {
        String forced = System.getProperty(EXTERNAL_PACKS_TOGGLE);
        if (forced != null) {
            return Boolean.parseBoolean(forced);
        }
        // Default to enabled so external food packs are available out-of-the-box.
        // Use -Dfirstpersonfoodeating.enableExternalFoodPacks=false to disable if needed.
        return true;
    }
}
