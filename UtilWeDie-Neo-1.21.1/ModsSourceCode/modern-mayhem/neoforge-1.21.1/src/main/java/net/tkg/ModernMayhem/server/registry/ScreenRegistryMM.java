package net.tkg.ModernMayhem.server.registry;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.tkg.ModernMayhem.client.screen.BackpackScreen;

public class ScreenRegistryMM {
    public static void register(RegisterMenuScreensEvent event) {
        event.register(GUIRegistryMM.BACKPACK_GUI.get(), BackpackScreen::new);
    }
}
