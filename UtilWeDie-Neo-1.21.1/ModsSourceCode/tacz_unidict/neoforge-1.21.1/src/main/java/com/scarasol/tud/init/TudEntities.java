package com.scarasol.tud.init;

import com.scarasol.tud.TudMod;
import com.scarasol.tud.entity.ShotFallingBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * @author Scarasol
 */
public class TudEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, TudMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ShotFallingBlockEntity>> SHOT_FALLING_BLOCK = register("shot_falling_block_entity", EntityType.Builder.of(ShotFallingBlockEntity::new, MobCategory.MISC)
            .setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.98f, 0.98f));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryName, EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register(registryName, () -> entityTypeBuilder.build(registryName));
    }
}
