package net.tejty.just_barricades.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tejty.just_barricades.JustBarricades;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JustBarricades.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
