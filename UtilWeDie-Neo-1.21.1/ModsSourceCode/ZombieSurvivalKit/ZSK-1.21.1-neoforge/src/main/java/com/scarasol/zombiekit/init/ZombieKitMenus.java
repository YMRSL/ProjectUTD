package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.inventory.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZombieKitMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, ZombieKitMod.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<ShortwaveRadioMenu>> SHORTWAVE_RADIO_GUI = REGISTRY.register("shortwave_radio_gui", () -> IMenuTypeExtension.create(ShortwaveRadioMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WeaponModificationMenu>> WEAPON_MODIFICATION_GUI = REGISTRY.register("weapon_modification_gui", () -> IMenuTypeExtension.create(WeaponModificationMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<VacuumPackagingMachineMenu>> VACUUM_PACKAGING_MACHINE_GUI = REGISTRY.register("vacuum_packaging_machine_gui", () -> IMenuTypeExtension.create(VacuumPackagingMachineMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MortarRackMenu>> MORTAR_RACK_MENU = REGISTRY.register("mortar_rack_gui", () -> IMenuTypeExtension.create(MortarRackMenu::new));
}
