package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.inventory.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZombieKitMenus {
    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ZombieKitMod.MODID);
    public static final RegistryObject<MenuType<ShortwaveRadioMenu>> SHORTWAVE_RADIO_GUI = REGISTRY.register("shortwave_radio_gui", () -> IForgeMenuType.create(ShortwaveRadioMenu::new));
    public static final RegistryObject<MenuType<WeaponModificationMenu>> WEAPON_MODIFICATION_GUI = REGISTRY.register("weapon_modification_gui", () -> IForgeMenuType.create(WeaponModificationMenu::new));
    public static final RegistryObject<MenuType<VacuumPackagingMachineMenu>> VACUUM_PACKAGING_MACHINE_GUI = REGISTRY.register("vacuum_packaging_machine_gui", () -> IForgeMenuType.create(VacuumPackagingMachineMenu::new));
    public static final RegistryObject<MenuType<MortarRackMenu>> MORTAR_RACK_MENU = REGISTRY.register("mortar_rack_gui", () -> IForgeMenuType.create(MortarRackMenu::new));
}
