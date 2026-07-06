package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.mechanics.DroneEntity;
import com.scarasol.zombiekit.entity.mechanics.HeavyMachineGunEntity;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.entity.mechanics.UvLampEntity;
import com.scarasol.zombiekit.entity.projectile.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = "zombiekit", bus = EventBusSubscriber.Bus.MOD)
public class ZombieKitEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, ZombieKitMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MolotovCocktailEntity>> MOLOTOV_COCKTAIL = register("molotov_cocktail", EntityType.Builder.<MolotovCocktailEntity>of(MolotovCocktailEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<PotionJarEntity>> POTION_JAR = register("potion_jar", EntityType.Builder.<PotionJarEntity>of(PotionJarEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<BileJarEntity>> BILE_JAR = register("bile_jar", EntityType.Builder.<BileJarEntity>of(BileJarEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<FirecrackerEntity>> FIRECRACKER = register("firecracker", EntityType.Builder.<FirecrackerEntity>of(FirecrackerEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<WrenchEntity>> WRENCH = register("wrench", EntityType.Builder.<WrenchEntity>of(WrenchEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<MortarShellEntity>> MORTAR_SHELL = register("mortar_shell", EntityType.Builder.<MortarShellEntity>of(MortarShellEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.6f, 1.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<LandmineEntity>> LANDMINE = register("landmine", EntityType.Builder.<LandmineEntity>of(LandmineEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<FlareGunEntity>> FLARE_GUN = register("flare_gun",
            EntityType.Builder.<FlareGunEntity>of(FlareGunEntity::new, MobCategory.MISC).clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<FlaresEntity>> FLARES = register("flares", EntityType.Builder.<FlaresEntity>of(FlaresEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = register("drone",
            EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC).clientTrackingRange(25).updateInterval(3).sized(0.6f, 0.4f));
    public static final DeferredHolder<EntityType<?>, EntityType<HeavyMachineGunAmmoEntity>> HEAVY_MACHINE_GUN_AMMO = register("heavy_machine_gun_ammo", EntityType.Builder.<HeavyMachineGunAmmoEntity>of(HeavyMachineGunAmmoEntity::new, MobCategory.MISC)
            .clientTrackingRange(64).updateInterval(1).sized(0.5f, 0.5f));
    public static final DeferredHolder<EntityType<?>, EntityType<HeavyMachineGunEntity>> HEAVY_MACHINE_GUN = register("heavy_machine_gun",
            EntityType.Builder.<HeavyMachineGunEntity>of(HeavyMachineGunEntity::new, MobCategory.MISC).clientTrackingRange(64).updateInterval(3).sized(0.8f, 1f));
    public static final DeferredHolder<EntityType<?>, EntityType<UvLampEntity>> UV_LAMP = register("uv_lamp",
            EntityType.Builder.<UvLampEntity>of(UvLampEntity::new, MobCategory.MISC).clientTrackingRange(64).updateInterval(3));
    public static final DeferredHolder<EntityType<?>, EntityType<MortarEntity>> MORTAR = register("mortar",
            EntityType.Builder.<MortarEntity>of(MortarEntity::new, MobCategory.MISC).clientTrackingRange(64).updateInterval(3).sized(1.5f, 1.5f));


    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryName, EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register(registryName, () -> entityTypeBuilder.build(registryName));
    }


    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(UV_LAMP.get(), UvLampEntity.createAttributes().build());
        event.put(DRONE.get(), DroneEntity.createAttributes().build());
        event.put(HEAVY_MACHINE_GUN.get(), HeavyMachineGunEntity.createAttributes().build());
        event.put(MORTAR.get(), MortarEntity.createAttributes().build());
    }


}
