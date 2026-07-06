package net.tkg.ModernMayhem.server.registry;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tkg.ModernMayhem.server.GUI.GenericBackpackGUI;

public class GUIRegistryMM {
    public static final DeferredRegister<MenuType<?>> GUIS = DeferredRegister.create(Registries.MENU, "mm");
    public static final DeferredHolder<MenuType<?>, MenuType<GenericBackpackGUI>> BACKPACK_GUI = GUIS.register("backpack_gui", () -> IMenuTypeExtension.create(GenericBackpackGUI::new));

    public static void init(IEventBus eventBus) {
        GUIS.register(eventBus);
    }
}
