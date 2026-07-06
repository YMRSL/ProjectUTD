package com.scarasol.zombiekit.init;

import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.projectile.MolotovCocktailEntity;
import com.scarasol.zombiekit.entity.projectile.MortarShellEntity;
import com.scarasol.zombiekit.item.*;
import com.scarasol.zombiekit.item.armor.*;
import com.scarasol.zombiekit.item.bonus.*;
import com.scarasol.zombiekit.item.medical.*;
import com.scarasol.zombiekit.item.projectile.*;
import com.scarasol.zombiekit.item.weapon.*;
import com.scarasol.zombiekit.item.weapon.parts.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZombieKitItems {

    public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, ZombieKitMod.MODID);

    public static final RegistryObject<Item> SKIING_HELMET = REGISTRY.register("skiing_helmet", () -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> SKIING_CHESTPLATE = REGISTRY.register("skiing_chestplate", () -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> SKIING_LEGGINGS = REGISTRY.register("skiing_leggings", () -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> SKIING_BOOTS = REGISTRY.register("skiing_boots", () -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> STANDARD_TACTICAL_HELMET = REGISTRY.register("standard_tactical_helmet", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, new Item.Properties(), 0));
    public static final RegistryObject<Item> STANDARD_TACTICAL_CHESTPLATE = REGISTRY.register("standard_tactical_chestplate", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 0));
    public static final RegistryObject<Item> STANDARD_TACTICAL_LEGGINGS = REGISTRY.register("standard_tactical_leggings", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, new Item.Properties(), 0));
    public static final RegistryObject<Item> STANDARD_TACTICAL_BOOTS = REGISTRY.register("standard_tactical_boots", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, new Item.Properties(), 0));

    public static final RegistryObject<Item> STANDARD_RIOT_HELMET = REGISTRY.register("standard_riot_helmet", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, new Item.Properties(), 0));
    public static final RegistryObject<Item> STANDARD_RIOT_CHESTPLATE = REGISTRY.register("standard_riot_chestplate", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 0));
    public static final RegistryObject<Item> STANDARD_RIOT_LEGGINGS = REGISTRY.register("standard_riot_leggings", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, new Item.Properties(), 0));
    public static final RegistryObject<Item> STANDARD_RIOT_BOOTS = REGISTRY.register("standard_riot_boots", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, new Item.Properties(), 0));

    public static final RegistryObject<Item> DESERT_TACTICAL_HELMET = REGISTRY.register("desert_tactical_helmet", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, new Item.Properties(), 1));
    public static final RegistryObject<Item> DESERT_TACTICAL_CHESTPLATE = REGISTRY.register("desert_tactical_chestplate", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 1));
    public static final RegistryObject<Item> DESERT_TACTICAL_LEGGINGS = REGISTRY.register("desert_tactical_leggings", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, new Item.Properties(), 1));
    public static final RegistryObject<Item> DESERT_TACTICAL_BOOTS = REGISTRY.register("desert_tactical_boots", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, new Item.Properties(), 1));

    public static final RegistryObject<Item> DESERT_RIOT_HELMET = REGISTRY.register("desert_riot_helmet", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, new Item.Properties(), 1));
    public static final RegistryObject<Item> DESERT_RIOT_CHESTPLATE = REGISTRY.register("desert_riot_chestplate", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 1));
    public static final RegistryObject<Item> DESERT_RIOT_LEGGINGS = REGISTRY.register("desert_riot_leggings", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, new Item.Properties(), 1));
    public static final RegistryObject<Item> DESERT_RIOT_BOOTS = REGISTRY.register("desert_riot_boots", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, new Item.Properties(), 1));

    public static final RegistryObject<Item> FOREST_TACTICAL_HELMET = REGISTRY.register("forest_tactical_helmet", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, new Item.Properties(), 2));
    public static final RegistryObject<Item> FOREST_TACTICAL_CHESTPLATE = REGISTRY.register("forest_tactical_chestplate", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 2));
    public static final RegistryObject<Item> FOREST_TACTICAL_LEGGINGS = REGISTRY.register("forest_tactical_leggings", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, new Item.Properties(), 2));
    public static final RegistryObject<Item> FOREST_TACTICAL_BOOTS = REGISTRY.register("forest_tactical_boots", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, new Item.Properties(), 2));

    public static final RegistryObject<Item> FOREST_RIOT_HELMET = REGISTRY.register("forest_riot_helmet", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, new Item.Properties(), 2));
    public static final RegistryObject<Item> FOREST_RIOT_CHESTPLATE = REGISTRY.register("forest_riot_chestplate", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 2));
    public static final RegistryObject<Item> FOREST_RIOT_LEGGINGS = REGISTRY.register("forest_riot_leggings", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, new Item.Properties(), 2));
    public static final RegistryObject<Item> FOREST_RIOT_BOOTS = REGISTRY.register("forest_riot_boots", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, new Item.Properties(), 2));

    public static final RegistryObject<Item> SNOW_TACTICAL_HELMET = REGISTRY.register("snow_tactical_helmet", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, new Item.Properties(), 3));
    public static final RegistryObject<Item> SNOW_TACTICAL_CHESTPLATE = REGISTRY.register("snow_tactical_chestplate", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 3));
    public static final RegistryObject<Item> SNOW_TACTICAL_LEGGINGS = REGISTRY.register("snow_tactical_leggings", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, new Item.Properties(), 3));
    public static final RegistryObject<Item> SNOW_TACTICAL_BOOTS = REGISTRY.register("snow_tactical_boots", () -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, new Item.Properties(), 3));

    public static final RegistryObject<Item> SNOW_RIOT_HELMET = REGISTRY.register("snow_riot_helmet", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, new Item.Properties(), 3));
    public static final RegistryObject<Item> SNOW_RIOT_CHESTPLATE = REGISTRY.register("snow_riot_chestplate", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, new Item.Properties(), 3));
    public static final RegistryObject<Item> SNOW_RIOT_LEGGINGS = REGISTRY.register("snow_riot_leggings", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, new Item.Properties(), 3));
    public static final RegistryObject<Item> SNOW_RIOT_BOOTS = REGISTRY.register("snow_riot_boots", () -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, new Item.Properties(), 3));

    public static final RegistryObject<Item> BOMB_HELMET = REGISTRY.register("bomb_helmet", () -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> BOMB_CHESTPLATE = REGISTRY.register("bomb_chestplate", () -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> BOMB_LEGGINGS = REGISTRY.register("bomb_leggings", () -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> BOMB_BOOTS = REGISTRY.register("bomb_boots", () -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> EXO_HELMET = REGISTRY.register("exo_helmet", () -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.HELMET, new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EXO_CHESTPLATE = REGISTRY.register("exo_chestplate", () -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EXO_LEGGINGS = REGISTRY.register("exo_leggings", () -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> EXO_BOOTS = REGISTRY.register("exo_boots", () -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON)));


    public static final RegistryObject<Item> BASEBALL_BAT = REGISTRY.register("baseball_bat", () -> new BaseballBat(Tiers.WOOD, 8, -2.7f, new Item.Properties(), false));
    public static final RegistryObject<Item> STUDDED_BASEBALL_BAT = REGISTRY.register("studded_baseball_bat", () -> new BaseballBat(Tiers.WOOD, 11, -2.7f, new Item.Properties(), true));
    public static final RegistryObject<Item> NETHERITE_BASEBALL_BAT = REGISTRY.register("netherite_baseball_bat", () -> new BaseballBat(Tiers.NETHERITE, 10, -2.7f, new Item.Properties().fireResistant(), false));

    public static final RegistryObject<Item> CROWBAR = REGISTRY.register("crowbar", () -> new Crowbar(Tiers.IRON, 12, -2.4f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_CROWBAR = REGISTRY.register("netherite_crowbar", () -> new Crowbar(Tiers.NETHERITE, 19, -2.4f, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> FIRE_AXE = REGISTRY.register("fire_axe", () -> new FireAxe(Tiers.IRON, 15f, -3.2f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_FIRE_AXE = REGISTRY.register("netherite_fire_axe", () -> new FireAxe(Tiers.NETHERITE, 22f, -3.2f, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> KNIFE = REGISTRY.register("knife", () -> new Knife(Tiers.IRON, 6, -1.5f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_KNIFE = REGISTRY.register("netherite_knife", () -> new Knife(Tiers.NETHERITE, 7, -1f, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> TRIANGULAR_THORN = REGISTRY.register("triangular_thorn", () -> new Knife(Tiers.NETHERITE, 10, -1f, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> MACHETE = REGISTRY.register("machete", () -> new Machete(Tiers.IRON, 9, -2.4f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_MACHETE = REGISTRY.register("netherite_machete", () -> new Machete(Tiers.NETHERITE, 13, -2.4f, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> RAKE = REGISTRY.register("rake", () -> new Rake(Tiers.IRON, 7, -3f, new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_RAKE = REGISTRY.register("netherite_rake", () -> new Rake(Tiers.NETHERITE, 10, -3f, new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> WRENCH = REGISTRY.register("wrench", () -> new Wrench(new Item.Properties().stacksTo(4)));

    public static final RegistryObject<Item> CHAINSAW = REGISTRY.register("chainsaw", () -> new Chainsaw(new Item.Properties().stacksTo(1).durability(101).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FLAMETHROWER = REGISTRY.register("flamethrower", () -> new Flamethrower(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FUEL_CANISTER = REGISTRY.register("fuel_canister", () -> new FuelCanister(new Item.Properties().durability(10000), FuelCanister.FuelType.NORMAL));
    public static final RegistryObject<Item> NAPALM_CANISTER = REGISTRY.register("napalm_fuel_canister", () -> new FuelCanister(new Item.Properties().durability(10000), FuelCanister.FuelType.NAPALM));
    public static final RegistryObject<Item> HIGH_TEMPERATURE_CANISTER = REGISTRY.register("high_temperature_fuel_canister", () -> new FuelCanister(new Item.Properties().durability(10000), FuelCanister.FuelType.HIGH_TEMPERATURE));

    public static final RegistryObject<Item> MORTAR_SHELL = REGISTRY.register("mortar_shell", () -> new MortarShell(new Item.Properties()));
    public static final RegistryObject<Item> MINE_LAYER_MORTAR_SHELL = REGISTRY.register("mine_layer_mortar_shell", () -> new MortarShell(new Item.Properties(), MortarShellEntity::mineLaying));
    public static final RegistryObject<Item> FIRE_MORTAR_SHELL = REGISTRY.register("fire_mortar_shell", () -> new MortarShell(new Item.Properties(), MortarShellEntity::burn));
    public static final RegistryObject<Item> SMOKE_MORTAR_SHELL = REGISTRY.register("smoke_mortar_shell", () -> new MortarShell(new Item.Properties(), MortarShellEntity::smoke));

    public static final RegistryObject<Item> MOLOTOV_COCKTAIL = REGISTRY.register("molotov_cocktail", () -> new MolotovCocktail(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> POTION_JAR = REGISTRY.register("potion_jar", () -> new PotionJar(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BILE_JAR = REGISTRY.register("bile_jar", () -> new BileJar(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> FIRECRACKER = REGISTRY.register("firecracker", () -> new Firecracker(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> PRIMARY_LIGHT_WEIGHTED_GRIP = REGISTRY.register("primary_light_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0, GripParts.Material.LIGHT_WEIGHTED));
    public static final RegistryObject<Item> IMPROVED_LIGHT_WEIGHTED_GRIP = REGISTRY.register("improved_light_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1, GripParts.Material.LIGHT_WEIGHTED));
    public static final RegistryObject<Item> ADVANCED_LIGHT_WEIGHTED_GRIP = REGISTRY.register("advanced_light_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2, GripParts.Material.LIGHT_WEIGHTED));

    public static final RegistryObject<Item> PRIMARY_HEAVY_WEIGHTED_GRIP = REGISTRY.register("primary_heavy_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0, GripParts.Material.HEAVY_WEIGHTED));
    public static final RegistryObject<Item> IMPROVED_HEAVY_WEIGHTED_GRIP = REGISTRY.register("improved_heavy_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1, GripParts.Material.HEAVY_WEIGHTED));
    public static final RegistryObject<Item> ADVANCED_HEAVY_WEIGHTED_GRIP = REGISTRY.register("advanced_heavy_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2, GripParts.Material.HEAVY_WEIGHTED));

    public static final RegistryObject<Item> PRIMARY_EXTEND_WEIGHTED_GRIP = REGISTRY.register("primary_extend_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0, GripParts.Material.EXTEND));
    public static final RegistryObject<Item> IMPROVED_EXTEND_WEIGHTED_GRIP = REGISTRY.register("improved_extend_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1, GripParts.Material.EXTEND));
    public static final RegistryObject<Item> ADVANCED_EXTEND_WEIGHTED_GRIP = REGISTRY.register("advanced_extend_weighted_grip", () -> new GripParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2, GripParts.Material.EXTEND));

    public static final RegistryObject<Item> PRIMARY_BURNING_CHARGING_PARTS = REGISTRY.register("primary_burning_charging_parts", () -> new BurningChargingParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> IMPROVED_BURNING_CHARGING_PARTS = REGISTRY.register("improved_burning_charging_parts", () -> new BurningChargingParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1));
    public static final RegistryObject<Item> ADVANCED_BURNING_CHARGING_PARTS = REGISTRY.register("advanced_burning_charging_parts", () -> new BurningChargingParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2));

    public static final RegistryObject<Item> PRIMARY_FREEZING_CHARGING_PARTS = REGISTRY.register("primary_freezing_charging_parts", () -> new FreezingChargingParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> IMPROVED_FREEZING_CHARGING_PARTS = REGISTRY.register("improved_freezing_charging_parts", () -> new FreezingChargingParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1));
    public static final RegistryObject<Item> ADVANCED_FREEZING_CHARGING_PARTS = REGISTRY.register("advanced_freezing_charging_parts", () -> new FreezingChargingParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2));

    public static final RegistryObject<Item> PRIMARY_BURNING_BATTLE_PARTS = REGISTRY.register("primary_burning_battle_parts", () -> new BurningBattleParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> IMPROVED_BURNING_BATTLE_PARTS = REGISTRY.register("improved_burning_battle_parts", () -> new BurningBattleParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1));
    public static final RegistryObject<Item> ADVANCED_BURNING_BATTLE_PARTS = REGISTRY.register("advanced_burning_battle_parts", () -> new BurningBattleParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2));

    public static final RegistryObject<Item> PRIMARY_FREEZING_BATTLE_PARTS = REGISTRY.register("primary_freezing_battle_parts", () -> new FreezingBattleParts(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 0));
    public static final RegistryObject<Item> IMPROVED_FREEZING_BATTLE_PARTS = REGISTRY.register("improved_freezing_battle_parts", () -> new FreezingBattleParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 1));
    public static final RegistryObject<Item> ADVANCED_FREEZING_BATTLE_PARTS = REGISTRY.register("advanced_freezing_battle_parts", () -> new FreezingBattleParts(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 2));

    public static final RegistryObject<Item> BANDAGE = REGISTRY.register("bandage", () -> new Bandage(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> PAINKILLER = REGISTRY.register("painkiller", () -> new Painkiller(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MEDICAL_KIT = REGISTRY.register("medical_kit", () -> new MedicalKit(new Item.Properties().durability(8)));
    public static final RegistryObject<Item> SUSPICIOUS_DRUG = REGISTRY.register("suspicious_drug", () -> new SuspiciousDrug(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> MIRACLE = REGISTRY.register("miracle", () -> new Miracle(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> COMPRESSED_BISCUIT = REGISTRY.register("compressed_biscuit", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6f).build())));
    public static final RegistryObject<Item> CHOCOLATE = REGISTRY.register("chocolate", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.8f).build())));
    public static final RegistryObject<Item> CANNED_YELLOW_PEACH = REGISTRY.register("canned_yellow_peach", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.3f).build())));
    public static final RegistryObject<Item> CANNED_BEEF_HOTPOT = REGISTRY.register("canned_beef_hotpot", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(12).saturationMod(1.2f).build())));
    public static final RegistryObject<Item> CANNED_LUNCHEON_MEAT = REGISTRY.register("canned_luncheon_meat", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.8f).build())));
    public static final RegistryObject<Item> CANNED_FISH = REGISTRY.register("canned_fish_in_black_bean_sauce", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(6).saturationMod(0.8f).build())));
    public static final RegistryObject<Item> CANNED_BREAD = REGISTRY.register("canned_bread", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6f).build())));
    public static final RegistryObject<Item> CANNED_BEANS = REGISTRY.register("canned_beans", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(5).saturationMod(0.6f).build())));
    public static final RegistryObject<Item> CANNED_TOMATOES = REGISTRY.register("canned_tomatoes", () -> new TooltipItem(new Item.Properties().stacksTo(64)
            .food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.3f).build())));


    public static final RegistryObject<Item> FLARE_GUN = REGISTRY.register("flare_gun", () -> new FlareGun(new Item.Properties().stacksTo(4)));

    public static final RegistryObject<Item> BATTERY = REGISTRY.register("battery", () -> new Item(new Item.Properties().durability(100)));

    public static final RegistryObject<Item> SANDPAPER = REGISTRY.register("sandpaper", () -> new Item(new Item.Properties().durability(5)));

    public static final RegistryObject<Item> PLASTIC_BAG = REGISTRY.register("plastic_bag", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ENERGY_ROD = REGISTRY.register("energy_rod", () -> new EnergyRod(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> TRAP_COVER = block(ZombieKitBlocks.TRAP_COVER);

    public static final RegistryObject<Item> BARBED_WIRE = block(ZombieKitBlocks.BARBED_WIRE);
    public static final RegistryObject<Item> BARBED_WIRE_BROKEN = block(ZombieKitBlocks.BARBED_WIRE_BROKEN);
    public static final RegistryObject<Item> BARBED_WIRE_EXTREMELY_BROKEN = block(ZombieKitBlocks.BARBED_WIRE_EXTREMELY_BROKEN);

    public static final RegistryObject<Item> LANDMINE = block(ZombieKitBlocks.LANDMINE);
    public static final RegistryObject<Item> CHEMICAL_LANDMINE = block(ZombieKitBlocks.CHEMICAL_LANDMINE);

    public static final RegistryObject<Item> CHARGER = block(ZombieKitBlocks.CHARGER);
    public static final RegistryObject<Item> ULTRA_WIDEBAND_RADAR = block(ZombieKitBlocks.ULTRA_WIDEBAND_RADAR);
    public static final RegistryObject<Item> UV_LAMP = REGISTRY.register("uv_lamp", () -> new UvLamp(new Item.Properties().durability(20)));

    public static final RegistryObject<Item> INJECTOR = block(ZombieKitBlocks.INJECTOR);

    public static final RegistryObject<Item> SHOOTING_PARAMETER = REGISTRY.register("shooting_parameters_table", () -> new TooltipItem(new Item.Properties()));

    public static final RegistryObject<Item> POCKET_RADIO = REGISTRY.register("pocket_radio", () -> new PocketRadio(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SHORTWAVE_RADIO = block(ZombieKitBlocks.SHORTWAVE_RADIO);

    public static final RegistryObject<Item> VACUUM_PACKAGING_MACHINE = block(ZombieKitBlocks.VACUUM_PACKAGING_MACHINE);
    public static final RegistryObject<Item> MORTAR_RACK = REGISTRY.register("mortar_rack", () -> new ZombieKitGeoBlockItem(ZombieKitBlocks.MORTAR_RACK.get(), new Item.Properties()));

    public static final RegistryObject<Item> HEAVY_MACHINE_GUN_AMMO = REGISTRY.register("heavy_machine_gun_ammo", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HEAVY_MACHINE_GUN_SUMMON = REGISTRY.register("heavy_machine_gun_summon", () -> new HeavyMachineGun(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MORTAR_SUMMON = REGISTRY.register("mortar_summon", () -> new Mortar(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DRONE_SUMMON = REGISTRY.register("drone_summon", () -> new Drone(new Item.Properties()));

    public static final RegistryObject<Item> GAS_TANK = block(ZombieKitBlocks.GAS_TANK);


    public static final RegistryObject<Item> DESERT_CAMOUFLAGE_DYE = REGISTRY.register("desert_camouflage_dye", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> FOREST_CAMOUFLAGE_DYE = REGISTRY.register("forest_camouflage_dye", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SNOW_CAMOUFLAGE_DYE = REGISTRY.register("snow_camouflage_dye", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistryObject<Item> LATEX = REGISTRY.register("latex", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RUBBER = REGISTRY.register("rubber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIBRE = REGISTRY.register("fibre", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLOTH = REGISTRY.register("cloth", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> QUARTZ_SAND = REGISTRY.register("quartz_sand", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPECIAL_CERAMICS = REGISTRY.register("special_ceramics", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BULLETPROOF_INSERT = REGISTRY.register("bulletproof_insert", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPECIAL_STEEL_SHEET = REGISTRY.register("special_steel_sheet", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CAMPHOR = REGISTRY.register("camphor", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEATH_BAT = REGISTRY.register("death_bat", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SALTPETER = REGISTRY.register("saltpeter", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CRUDE_NITRATE = REGISTRY.register("crude_nitrate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HYDROGEN_NITRATE = REGISTRY.register("hydrogen_nitrate", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NITROCELLULOSE = REGISTRY.register("nitrocellulose", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PLASTICS = REGISTRY.register("plastics", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ALCOHOL = REGISTRY.register("alcohol", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> IRON_WIRE = REGISTRY.register("iron_wire", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SMOKELESS_GUNPOWDER = REGISTRY.register("smokeless_gunpowder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SULFUR = REGISTRY.register("sulfur", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SALTPETER_BUCKET = REGISTRY.register("saltpeter_bucket", () -> new SaltpeterBucket(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SALTPETER_SOIL = REGISTRY.register("saltpeter_soil", () -> new SaltpeterSoil(new Item.Properties()));


    public static final RegistryObject<Item> DRONE_COMPONENTS = REGISTRY.register("drone_components", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MACHINE_GUN_COMPONENTS = REGISTRY.register("machine_gun_components", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MORTAR_COMPONENTS = REGISTRY.register("mortar_components", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BLUEPRINT1 = REGISTRY.register("blueprint_of_steel", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BLUEPRINT2 = REGISTRY.register("blueprint_of_dye", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BLUEPRINT3 = REGISTRY.register("blueprint_of_riot", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BLUEPRINT4 = REGISTRY.register("blueprint_of_landmine", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BLUEPRINT5 = REGISTRY.register("blueprint_of_drone", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BLUEPRINT6 = REGISTRY.register("blueprint_of_heavy_machine_gun", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BLUEPRINT7 = REGISTRY.register("blueprint_of_mortar", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));


    public static final RegistryObject<Item> SCARASOL = REGISTRY.register("scarasol", () -> new BonusItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> KYOKO = REGISTRY.register("kyoko", () -> new BonusItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> WHITE_FOOD_CHESTPLATE = REGISTRY.register("white_food_chestplate", () -> new WhiteFood(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> KARAZHAN = REGISTRY.register("karazhan", () -> new BonusItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> IRON_CURTAIN = REGISTRY.register("iron_curtain", () -> new BonusItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> FLAME = REGISTRY.register("flame", () -> new BonusItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> AOKINAO = REGISTRY.register("aokinao", () -> new Aokinao(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> LHX_QING = REGISTRY.register("lhx_qing", () -> new LHXQing(Tiers.WOOD, 5, -2.4f, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> SONA_BAKEMONO = REGISTRY.register("sona_bakemono", () -> new BonusItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> KONN_GARA = REGISTRY.register("konn_gara", () -> new KonnGara(15, ZombieKitSounds.konn_gara, new Item.Properties().rarity(Rarity.EPIC), 204 * 20));


    private static RegistryObject<Item> block(RegistryObject<Block> block) {
        return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
    }


}
