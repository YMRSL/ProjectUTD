package io.github.ymrsl.firstpersonfoodeating.registry;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.item.GeoProfiledConsumableItem;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FirstPersonFoodEatingMod.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FirstPersonFoodEatingMod.MOD_ID);

    public static final DeferredItem<Item> PACK_FOOD =
            registerGeoConsumable("pack_food", UseAnim.NONE, 81, 2, 0.1f, 16);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.firstpersonfoodeating.main"))
                    .withTabsBefore(CreativeModeTabs.FOOD_AND_DRINKS)
                    .icon(ModItems::createMainTabIcon)
                    .displayItems((parameters, output) -> {
                    })
                    .build()
    );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private static ItemStack createMainTabIcon() {
        ItemStack icon = new ItemStack(PACK_FOOD.get());
        FoodStackData.applyProfile(
                icon,
                ResourceLocation.fromNamespaceAndPath(FirstPersonFoodEatingMod.MOD_ID, "i_bang_a"),
                62,
                2,
                0.1f,
                16
        );
        return icon;
    }

    private static DeferredItem<Item> registerGeoConsumable(
            String path,
            UseAnim useAnim,
            int useDurationTicks,
            int nutrition,
            float saturation,
            int stackSize
    ) {
        return ITEMS.registerItem(path, properties -> new GeoProfiledConsumableItem(
                properties
                        .stacksTo(stackSize)
                        .food(new FoodProperties.Builder()
                                .alwaysEdible()
                                .nutrition(nutrition)
                                .saturationModifier(saturation)
                                .build()),
                useAnim,
                useDurationTicks
        ));
    }

}
