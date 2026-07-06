package net.tkg.ModernMayhem.server.registry;

import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class EntityRegistryMM {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, (String)"mm");

    public static void init(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}

