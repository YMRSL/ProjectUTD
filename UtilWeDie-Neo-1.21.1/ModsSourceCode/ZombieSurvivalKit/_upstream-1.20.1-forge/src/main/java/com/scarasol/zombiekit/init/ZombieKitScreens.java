package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.client.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ZombieKitScreens {
    @SubscribeEvent
    public static void clientLoad(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ZombieKitMenus.SHORTWAVE_RADIO_GUI.get(), ShortwaveRadioScreen::new);
            MenuScreens.register(ZombieKitMenus.WEAPON_MODIFICATION_GUI.get(), WeaponModificationScreen::new);
            MenuScreens.register(ZombieKitMenus.VACUUM_PACKAGING_MACHINE_GUI.get(), VacuumPackagingMachineScreen::new);
            MenuScreens.register(ZombieKitMenus.MORTAR_RACK_MENU.get(), MortarRackScreen::new);
        });
    }

}
