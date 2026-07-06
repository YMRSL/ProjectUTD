package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAssetsManager;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodDisplayDefinition;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import io.github.ymrsl.firstpersonfoodeating.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class FoodCreativeTabEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile boolean pendingCreativeTabRebuild = false;

    private FoodCreativeTabEvents() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!isTargetTab(event)) {
            return;
        }
        if (ModItems.PACK_FOOD == null || !ModItems.PACK_FOOD.isBound()) {
            return;
        }

        int added = 0;
        java.util.Set<String> seenItemIds = new java.util.HashSet<>();
        for (var entry : FoodAssetsManager.get().getSortedDisplays()) {
            FoodDisplayDefinition display = entry.getValue();
            if (display == null || display.getItemId() == null) {
                continue;
            }
            String itemPath = display.getItemId().getPath();
            if (itemPath == null || !itemPath.startsWith("i_")) {
                continue;
            }
            if (!seenItemIds.add(itemPath)) {
                continue;
            }
            int useDurationTicks = display.resolveUseDurationTicks();
            if (!display.hasExplicitUseDurationTicks()) {
                useDurationTicks = resolveLegacyPresetUseDuration(display, useDurationTicks);
                useDurationTicks = FoodAssetsManager.get().inferUseDurationTicks(display, useDurationTicks);
            }
            FoodStackData.DurabilityUseSpec durabilitySpec = display.resolveDurabilityUseSpec();
            int maxStackSize = display.resolveMaxStackSize();
            if (durabilitySpec != null && durabilitySpec.enabled()) {
                maxStackSize = 1;
            }
            ItemStack stack = new ItemStack(ModItems.PACK_FOOD.get());
            FoodStackData.applyProfile(
                    stack,
                    display.getItemId(),
                    useDurationTicks,
                    display.resolveNutrition(),
                    display.resolveSaturation(),
                    maxStackSize
            );
            FoodStackData.setEffects(stack, display.resolveEffects());
            FoodStackData.setCustomEffects(stack, display.resolveCustomEffects());
            FoodStackData.setThirstSpec(stack, display.resolveThirstSpec());
            FoodStackData.setFlavorMessages(stack, display.resolveFlavorMessageSpec());
            FoodStackData.UseSelectorSpec selectorSpec = display.resolveUseSelectorSpec(useDurationTicks);
            if (selectorSpec != null && !selectorSpec.rules().isEmpty()) {
                var resolvedRules = new java.util.ArrayList<FoodStackData.UseSelectorRule>();
                for (FoodStackData.UseSelectorRule rule : selectorSpec.rules()) {
                    int resolvedDuration = FoodAssetsManager.get()
                            .resolveClipDurationTicks(display, rule.clipName(), rule.durationTicks());
                    resolvedRules.add(new FoodStackData.UseSelectorRule(
                            rule.minAmount(),
                            rule.maxAmount(),
                            rule.clipName(),
                            resolvedDuration
                    ));
                }
                selectorSpec = new FoodStackData.UseSelectorSpec(
                        selectorSpec.mode(),
                        selectorSpec.defaultClip(),
                        resolvedRules
                );
            }
            FoodStackData.setUseSelectorSpec(stack, selectorSpec);
            FoodStackData.setDurabilityUseSpec(stack, durabilitySpec);
            try {
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                added++;
            } catch (IllegalArgumentException duplicate) {
                // Stack already present in this tab's list (SEARCH-tab aggregation
                // across parent tabs, or a duplicate display id) — skip instead of
                // letting it abort the whole creative-tab build and crash the game.
            }
        }
        LOGGER.info("[{}] Added {} pack-driven food variants to creative tab {}",
                FirstPersonFoodEatingMod.MOD_ID, added, event.getTabKey().location());
    }

    public static void requestCreativeTabsRebuild() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.level == null || minecraft.player == null) {
                pendingCreativeTabRebuild = true;
                LOGGER.info("[{}] Defer creative tab rebuild until world is ready",
                        FirstPersonFoodEatingMod.MOD_ID);
                return;
            }
            pendingCreativeTabRebuild = false;
            tryRebuildCreativeTabs(minecraft, "reload");
        });
    }

    public static void flushPendingCreativeTabsRebuild() {
        if (!pendingCreativeTabRebuild) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        pendingCreativeTabRebuild = false;
        tryRebuildCreativeTabs(minecraft, "deferred");
    }

    private static boolean isTargetTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FOOD_AND_DRINKS)
                || event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)
                || event.getTabKey().equals(CreativeModeTabs.SEARCH)) {
            return true;
        }
        var mainTabKey = ModItems.MAIN_TAB.getKey();
        return mainTabKey != null && event.getTabKey().equals(mainTabKey);
    }

    private static int resolveLegacyPresetUseDuration(FoodDisplayDefinition display, int fallback) {
        if (display == null || display.getItemId() == null) {
            return fallback;
        }
        String path = display.getItemId().getPath();
        if ("i_bang_d".equals(path)) {
            return 89;
        }
        if (path.startsWith("i_bang_")) {
            return 62;
        }
        if (path.startsWith("i_bengdai_")) {
            return 79;
        }
        if (path.startsWith("i_dai_")) {
            return 109;
        }
        if (path.startsWith("i_guan_")) {
            return 81;
        }
        return fallback;
    }

    private static void tryRebuildCreativeTabs(Minecraft minecraft, String reason) {
        boolean rebuilt;
        try {
            rebuilt = CreativeModeTabs.tryRebuildTabContents(
                    minecraft.level.enabledFeatures(),
                    minecraft.player.canUseGameMasterBlocks(),
                    minecraft.level.registryAccess()
            );
        } catch (Exception e) {
            LOGGER.error("[{}] Creative tabs rebuild failed ({}), skipped to avoid crash",
                    FirstPersonFoodEatingMod.MOD_ID, reason, e);
            return;
        }
        if (rebuilt) {
            LOGGER.info("[{}] Creative tabs rebuilt ({})", FirstPersonFoodEatingMod.MOD_ID, reason);
        } else {
            LOGGER.warn("[{}] Creative tabs rebuild skipped ({})", FirstPersonFoodEatingMod.MOD_ID, reason);
        }
    }

}
