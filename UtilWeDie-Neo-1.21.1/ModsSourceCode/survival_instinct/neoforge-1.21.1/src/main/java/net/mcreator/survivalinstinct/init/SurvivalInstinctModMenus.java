package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.world.inventory.CashRegisterGUIMenu;
import net.mcreator.survivalinstinct.world.inventory.EmptyBagGUIMenu;
import net.mcreator.survivalinstinct.world.inventory.GabageBagGUIMenu;
import net.mcreator.survivalinstinct.world.inventory.RefrigeratorGUIMenu;
import net.mcreator.survivalinstinct.world.inventory.TrashCanGUIMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SurvivalInstinctModMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, (String)"survival_instinct");
    public static final DeferredHolder<MenuType<?>, MenuType<RefrigeratorGUIMenu>> REFRIGERATOR_GUI = REGISTRY.register("refrigerator_gui", () -> IMenuTypeExtension.create(RefrigeratorGUIMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<GabageBagGUIMenu>> GABAGE_BAG_GUI = REGISTRY.register("gabage_bag_gui", () -> IMenuTypeExtension.create(GabageBagGUIMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CashRegisterGUIMenu>> CASH_REGISTER_GUI = REGISTRY.register("cash_register_gui", () -> IMenuTypeExtension.create(CashRegisterGUIMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TrashCanGUIMenu>> TRASH_CAN_GUI = REGISTRY.register("trash_can_gui", () -> IMenuTypeExtension.create(TrashCanGUIMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<EmptyBagGUIMenu>> EMPTY_BAG_GUI = REGISTRY.register("empty_bag_gui", () -> IMenuTypeExtension.create(EmptyBagGUIMenu::new));
}

