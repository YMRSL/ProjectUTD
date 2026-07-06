package net.tkg.ModernMayhem.client.registry;

import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.tkg.ModernMayhem.client.item.NVGFirstPersonFakeItem;

@OnlyIn(value=Dist.CLIENT)
public class ClientItemRegistryMM {
    public static final DeferredRegister<Item> CLIENT_ITEMS = DeferredRegister.create(Registries.ITEM, (String)"mm");
    public static final DeferredHolder<Item, Item> FIRST_PERSON_NVG = CLIENT_ITEMS.register("first_person_nvg", NVGFirstPersonFakeItem::new);

    public static void init(IEventBus modEventBus) {
        CLIENT_ITEMS.register(modEventBus);
    }
}

