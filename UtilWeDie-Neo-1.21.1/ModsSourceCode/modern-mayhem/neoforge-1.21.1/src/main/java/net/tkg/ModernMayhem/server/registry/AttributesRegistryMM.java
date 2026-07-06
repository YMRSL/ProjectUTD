package net.tkg.ModernMayhem.server.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AttributesRegistryMM {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, (String)"mm");
    public static final DeferredHolder<Attribute, Attribute> SAFE_FALL_DISTANCE = ATTRIBUTES.register("safe_fall_distance", () -> new RangedAttribute("attribute.name.generic.safe_fall_distance", 3.0, -1024.0, 1024.0).setSyncable(true));

    public static void init(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }
}

