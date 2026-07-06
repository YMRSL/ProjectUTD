package com.scarasol.zombiekit.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ZombieKitTags {
    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse("zombiekit:prison"));

    public static final TagKey<EntityType<?>> UV_RESISTANCE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:uv_resistance"));
    public static final TagKey<EntityType<?>> UV_NONRESISTANCE = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:uv_nonresistance"));
    public static final TagKey<EntityType<?>> SURVIVORS = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:survivors"));
    public static final TagKey<EntityType<?>> MACHINE_GUNNER = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:machine_gunner"));
    public static final TagKey<EntityType<?>> ARTILLERY = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:artillery"));
    public static final TagKey<EntityType<?>> FLAMETHROWER = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("forge:flamethrower"));

    public static TagKey<Biome> DESERT = TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:desert_camouflage"));
    public static TagKey<Biome> FOREST = TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:forest_camouflage"));
    public static TagKey<Biome> SNOW = TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:snow_camouflage"));
    public static TagKey<Biome> DESERT_CAVE = TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:desert_camouflage_cave"));
    public static TagKey<Biome> FOREST_CAVE = TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:forest_camouflage_cave"));
    public static TagKey<Biome> SNOW_CAVE = TagKey.create(Registries.BIOME, ResourceLocation.parse("forge:snow_camouflage_cave"));

    public static final TagKey<Structure> SHELTER = TagKey.create(Registries.STRUCTURE, ResourceLocation.parse("zombiekit:shelter"));

    public static TagKey<Item> MACHINE_GUN_AMMO = TagKey.create(Registries.ITEM, ResourceLocation.parse("forge:machine_gun_ammo"));
}
