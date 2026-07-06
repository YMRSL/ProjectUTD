package com.scarasol.sona.init;

import com.scarasol.sona.SonaMod;
import com.scarasol.sona.entity.SoundDecoy;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = SonaMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class SonaEntities {

    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, SonaMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SoundDecoy>> SOUND_DECOY = register("sound_decoy",
            EntityType.Builder.<SoundDecoy>of(SoundDecoy::new, MobCategory.MONSTER)
                    .clientTrackingRange(0).updateInterval(3).fireImmune().sized(0.0f, 0.0f));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryName, EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register(registryName, () -> entityTypeBuilder.build(registryName));
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SOUND_DECOY.get(), SoundDecoy.createAttributes().build());
    }

}
