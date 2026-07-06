package net.tejty.just_barricades.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.tejty.just_barricades.JustBarricades;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, JustBarricades.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
