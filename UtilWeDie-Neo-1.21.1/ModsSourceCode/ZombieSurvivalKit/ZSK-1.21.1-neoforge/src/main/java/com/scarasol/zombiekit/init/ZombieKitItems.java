package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.projectile.MortarShellEntity;
import com.scarasol.zombiekit.item.*;
import com.scarasol.zombiekit.item.armor.*;
import com.scarasol.zombiekit.item.bonus.*;
import com.scarasol.zombiekit.item.medical.*;
import com.scarasol.zombiekit.item.projectile.*;
import com.scarasol.zombiekit.item.weapon.*;
import com.scarasol.zombiekit.item.weapon.parts.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZombieKitItems {

    public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(ZombieKitMod.MODID);

    public static final DeferredItem<Item> SKIING_HELMET = REGISTRY.registerItem("skiing_helmet", props -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.HELMET, props), new Item.Properties());
    public static final DeferredItem<Item> SKIING_CHESTPLATE = REGISTRY.registerItem("skiing_chestplate", props -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.CHESTPLATE, props), new Item.Properties());
    public static final DeferredItem<Item> SKIING_LEGGINGS = REGISTRY.registerItem("skiing_leggings", props -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.LEGGINGS, props), new Item.Properties());
    public static final DeferredItem<Item> SKIING_BOOTS = REGISTRY.registerItem("skiing_boots", props -> new SkiingArmor(ModArmorMaterial.SKIING, ArmorItem.Type.BOOTS, props), new Item.Properties());

    public static final DeferredItem<Item> STANDARD_TACTICAL_HELMET = REGISTRY.registerItem("standard_tactical_helmet", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, props, 0), new Item.Properties());
    public static final DeferredItem<Item> STANDARD_TACTICAL_CHESTPLATE = REGISTRY.registerItem("standard_tactical_chestplate", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, props, 0), new Item.Properties());
    public static final DeferredItem<Item> STANDARD_TACTICAL_LEGGINGS = REGISTRY.registerItem("standard_tactical_leggings", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, props, 0), new Item.Properties());
    public static final DeferredItem<Item> STANDARD_TACTICAL_BOOTS = REGISTRY.registerItem("standard_tactical_boots", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, props, 0), new Item.Properties());

    public static final DeferredItem<Item> STANDARD_RIOT_HELMET = REGISTRY.registerItem("standard_riot_helmet", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, props, 0), new Item.Properties());
    public static final DeferredItem<Item> STANDARD_RIOT_CHESTPLATE = REGISTRY.registerItem("standard_riot_chestplate", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, props, 0), new Item.Properties());
    public static final DeferredItem<Item> STANDARD_RIOT_LEGGINGS = REGISTRY.registerItem("standard_riot_leggings", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, props, 0), new Item.Properties());
    public static final DeferredItem<Item> STANDARD_RIOT_BOOTS = REGISTRY.registerItem("standard_riot_boots", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, props, 0), new Item.Properties());

    public static final DeferredItem<Item> DESERT_TACTICAL_HELMET = REGISTRY.registerItem("desert_tactical_helmet", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, props, 1), new Item.Properties());
    public static final DeferredItem<Item> DESERT_TACTICAL_CHESTPLATE = REGISTRY.registerItem("desert_tactical_chestplate", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, props, 1), new Item.Properties());
    public static final DeferredItem<Item> DESERT_TACTICAL_LEGGINGS = REGISTRY.registerItem("desert_tactical_leggings", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, props, 1), new Item.Properties());
    public static final DeferredItem<Item> DESERT_TACTICAL_BOOTS = REGISTRY.registerItem("desert_tactical_boots", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, props, 1), new Item.Properties());

    public static final DeferredItem<Item> DESERT_RIOT_HELMET = REGISTRY.registerItem("desert_riot_helmet", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, props, 1), new Item.Properties());
    public static final DeferredItem<Item> DESERT_RIOT_CHESTPLATE = REGISTRY.registerItem("desert_riot_chestplate", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, props, 1), new Item.Properties());
    public static final DeferredItem<Item> DESERT_RIOT_LEGGINGS = REGISTRY.registerItem("desert_riot_leggings", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, props, 1), new Item.Properties());
    public static final DeferredItem<Item> DESERT_RIOT_BOOTS = REGISTRY.registerItem("desert_riot_boots", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, props, 1), new Item.Properties());

    public static final DeferredItem<Item> FOREST_TACTICAL_HELMET = REGISTRY.registerItem("forest_tactical_helmet", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, props, 2), new Item.Properties());
    public static final DeferredItem<Item> FOREST_TACTICAL_CHESTPLATE = REGISTRY.registerItem("forest_tactical_chestplate", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, props, 2), new Item.Properties());
    public static final DeferredItem<Item> FOREST_TACTICAL_LEGGINGS = REGISTRY.registerItem("forest_tactical_leggings", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, props, 2), new Item.Properties());
    public static final DeferredItem<Item> FOREST_TACTICAL_BOOTS = REGISTRY.registerItem("forest_tactical_boots", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, props, 2), new Item.Properties());

    public static final DeferredItem<Item> FOREST_RIOT_HELMET = REGISTRY.registerItem("forest_riot_helmet", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, props, 2), new Item.Properties());
    public static final DeferredItem<Item> FOREST_RIOT_CHESTPLATE = REGISTRY.registerItem("forest_riot_chestplate", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, props, 2), new Item.Properties());
    public static final DeferredItem<Item> FOREST_RIOT_LEGGINGS = REGISTRY.registerItem("forest_riot_leggings", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, props, 2), new Item.Properties());
    public static final DeferredItem<Item> FOREST_RIOT_BOOTS = REGISTRY.registerItem("forest_riot_boots", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, props, 2), new Item.Properties());

    public static final DeferredItem<Item> SNOW_TACTICAL_HELMET = REGISTRY.registerItem("snow_tactical_helmet", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.HELMET, props, 3), new Item.Properties());
    public static final DeferredItem<Item> SNOW_TACTICAL_CHESTPLATE = REGISTRY.registerItem("snow_tactical_chestplate", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.CHESTPLATE, props, 3), new Item.Properties());
    public static final DeferredItem<Item> SNOW_TACTICAL_LEGGINGS = REGISTRY.registerItem("snow_tactical_leggings", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.LEGGINGS, props, 3), new Item.Properties());
    public static final DeferredItem<Item> SNOW_TACTICAL_BOOTS = REGISTRY.registerItem("snow_tactical_boots", props -> new TacticalArmor(ModArmorMaterial.TACTICAL, ArmorItem.Type.BOOTS, props, 3), new Item.Properties());

    public static final DeferredItem<Item> SNOW_RIOT_HELMET = REGISTRY.registerItem("snow_riot_helmet", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.HELMET, props, 3), new Item.Properties());
    public static final DeferredItem<Item> SNOW_RIOT_CHESTPLATE = REGISTRY.registerItem("snow_riot_chestplate", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.CHESTPLATE, props, 3), new Item.Properties());
    public static final DeferredItem<Item> SNOW_RIOT_LEGGINGS = REGISTRY.registerItem("snow_riot_leggings", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.LEGGINGS, props, 3), new Item.Properties());
    public static final DeferredItem<Item> SNOW_RIOT_BOOTS = REGISTRY.registerItem("snow_riot_boots", props -> new RiotArmor(ModArmorMaterial.RIOT, ArmorItem.Type.BOOTS, props, 3), new Item.Properties());

    public static final DeferredItem<Item> BOMB_HELMET = REGISTRY.registerItem("bomb_helmet", props -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.HELMET, props), new Item.Properties());
    public static final DeferredItem<Item> BOMB_CHESTPLATE = REGISTRY.registerItem("bomb_chestplate", props -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.CHESTPLATE, props), new Item.Properties());
    public static final DeferredItem<Item> BOMB_LEGGINGS = REGISTRY.registerItem("bomb_leggings", props -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.LEGGINGS, props), new Item.Properties());
    public static final DeferredItem<Item> BOMB_BOOTS = REGISTRY.registerItem("bomb_boots", props -> new BombArmor(ModArmorMaterial.BOMB, ArmorItem.Type.BOOTS, props), new Item.Properties());

    public static final DeferredItem<Item> EXO_HELMET = REGISTRY.registerItem("exo_helmet", props -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.HELMET, props), new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> EXO_CHESTPLATE = REGISTRY.registerItem("exo_chestplate", props -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.CHESTPLATE, props), new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> EXO_LEGGINGS = REGISTRY.registerItem("exo_leggings", props -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.LEGGINGS, props), new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> EXO_BOOTS = REGISTRY.registerItem("exo_boots", props -> new ExoArmor(ModArmorMaterial.EXO, ArmorItem.Type.BOOTS, props), new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));


    public static final DeferredItem<Item> BASEBALL_BAT = REGISTRY.registerItem("baseball_bat", props -> new BaseballBat(Tiers.WOOD, 8, -2.7f, props, false), new Item.Properties());
    public static final DeferredItem<Item> STUDDED_BASEBALL_BAT = REGISTRY.registerItem("studded_baseball_bat", props -> new BaseballBat(Tiers.WOOD, 11, -2.7f, props, true), new Item.Properties());
    public static final DeferredItem<Item> NETHERITE_BASEBALL_BAT = REGISTRY.registerItem("netherite_baseball_bat", props -> new BaseballBat(Tiers.NETHERITE, 10, -2.7f, props, false), new Item.Properties().fireResistant());

    public static final DeferredItem<Item> CROWBAR = REGISTRY.registerItem("crowbar", props -> new Crowbar(Tiers.IRON, 12, -2.4f, props), new Item.Properties());
    public static final DeferredItem<Item> NETHERITE_CROWBAR = REGISTRY.registerItem("netherite_crowbar", props -> new Crowbar(Tiers.NETHERITE, 19, -2.4f, props), new Item.Properties().fireResistant());

    public static final DeferredItem<Item> FIRE_AXE = REGISTRY.registerItem("fire_axe", props -> new FireAxe(Tiers.IRON, 15f, -3.2f, props), new Item.Properties());
    public static final DeferredItem<Item> NETHERITE_FIRE_AXE = REGISTRY.registerItem("netherite_fire_axe", props -> new FireAxe(Tiers.NETHERITE, 22f, -3.2f, props), new Item.Properties().fireResistant());

    public static final DeferredItem<Item> KNIFE = REGISTRY.registerItem("knife", props -> new Knife(Tiers.IRON, 6, -1.5f, props), new Item.Properties());
    public static final DeferredItem<Item> NETHERITE_KNIFE = REGISTRY.registerItem("netherite_knife", props -> new Knife(Tiers.NETHERITE, 7, -1f, props), new Item.Properties().fireResistant());
    public static final DeferredItem<Item> TRIANGULAR_THORN = REGISTRY.registerItem("triangular_thorn", props -> new Knife(Tiers.NETHERITE, 10, -1f, props), new Item.Properties().fireResistant());

    public static final DeferredItem<Item> MACHETE = REGISTRY.registerItem("machete", props -> new Machete(Tiers.IRON, 9, -2.4f, props), new Item.Properties());
    public static final DeferredItem<Item> NETHERITE_MACHETE = REGISTRY.registerItem("netherite_machete", props -> new Machete(Tiers.NETHERITE, 13, -2.4f, props), new Item.Properties().fireResistant());

    public static final DeferredItem<Item> RAKE = REGISTRY.registerItem("rake", props -> new Rake(Tiers.IRON, 7, -3f, props), new Item.Properties());
    public static final DeferredItem<Item> NETHERITE_RAKE = REGISTRY.registerItem("netherite_rake", props -> new Rake(Tiers.NETHERITE, 10, -3f, props), new Item.Properties().fireResistant());

    public static final DeferredItem<Item> WRENCH = REGISTRY.registerItem("wrench", Wrench::new, new Item.Properties().stacksTo(4));

    public static final DeferredItem<Item> CHAINSAW = REGISTRY.registerItem("chainsaw", Chainsaw::new, new Item.Properties().stacksTo(1).durability(101).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> FLAMETHROWER = REGISTRY.registerItem("flamethrower", Flamethrower::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> FUEL_CANISTER = REGISTRY.registerItem("fuel_canister", props -> new FuelCanister(props, FuelCanister.FuelType.NORMAL), new Item.Properties().durability(10000));
    public static final DeferredItem<Item> NAPALM_CANISTER = REGISTRY.registerItem("napalm_fuel_canister", props -> new FuelCanister(props, FuelCanister.FuelType.NAPALM), new Item.Properties().durability(10000));
    public static final DeferredItem<Item> HIGH_TEMPERATURE_CANISTER = REGISTRY.registerItem("high_temperature_fuel_canister", props -> new FuelCanister(props, FuelCanister.FuelType.HIGH_TEMPERATURE), new Item.Properties().durability(10000));

    public static final DeferredItem<Item> MORTAR_SHELL = REGISTRY.registerItem("mortar_shell", MortarShell::new, new Item.Properties());
    public static final DeferredItem<Item> MINE_LAYER_MORTAR_SHELL = REGISTRY.registerItem("mine_layer_mortar_shell", props -> new MortarShell(props, MortarShellEntity::mineLaying), new Item.Properties());
    public static final DeferredItem<Item> FIRE_MORTAR_SHELL = REGISTRY.registerItem("fire_mortar_shell", props -> new MortarShell(props, MortarShellEntity::burn), new Item.Properties());
    public static final DeferredItem<Item> SMOKE_MORTAR_SHELL = REGISTRY.registerItem("smoke_mortar_shell", props -> new MortarShell(props, MortarShellEntity::smoke), new Item.Properties());

    public static final DeferredItem<Item> MOLOTOV_COCKTAIL = REGISTRY.registerItem("molotov_cocktail", MolotovCocktail::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> POTION_JAR = REGISTRY.registerItem("potion_jar", PotionJar::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> BILE_JAR = REGISTRY.registerItem("bile_jar", BileJar::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> FIRECRACKER = REGISTRY.registerItem("firecracker", Firecracker::new, new Item.Properties().stacksTo(16));

    public static final DeferredItem<Item> PRIMARY_LIGHT_WEIGHTED_GRIP = REGISTRY.registerItem("primary_light_weighted_grip", props -> new GripParts(props, 0, GripParts.Material.LIGHT_WEIGHTED), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_LIGHT_WEIGHTED_GRIP = REGISTRY.registerItem("improved_light_weighted_grip", props -> new GripParts(props, 1, GripParts.Material.LIGHT_WEIGHTED), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_LIGHT_WEIGHTED_GRIP = REGISTRY.registerItem("advanced_light_weighted_grip", props -> new GripParts(props, 2, GripParts.Material.LIGHT_WEIGHTED), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> PRIMARY_HEAVY_WEIGHTED_GRIP = REGISTRY.registerItem("primary_heavy_weighted_grip", props -> new GripParts(props, 0, GripParts.Material.HEAVY_WEIGHTED), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_HEAVY_WEIGHTED_GRIP = REGISTRY.registerItem("improved_heavy_weighted_grip", props -> new GripParts(props, 1, GripParts.Material.HEAVY_WEIGHTED), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_HEAVY_WEIGHTED_GRIP = REGISTRY.registerItem("advanced_heavy_weighted_grip", props -> new GripParts(props, 2, GripParts.Material.HEAVY_WEIGHTED), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> PRIMARY_EXTEND_WEIGHTED_GRIP = REGISTRY.registerItem("primary_extend_weighted_grip", props -> new GripParts(props, 0, GripParts.Material.EXTEND), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_EXTEND_WEIGHTED_GRIP = REGISTRY.registerItem("improved_extend_weighted_grip", props -> new GripParts(props, 1, GripParts.Material.EXTEND), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_EXTEND_WEIGHTED_GRIP = REGISTRY.registerItem("advanced_extend_weighted_grip", props -> new GripParts(props, 2, GripParts.Material.EXTEND), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> PRIMARY_BURNING_CHARGING_PARTS = REGISTRY.registerItem("primary_burning_charging_parts", props -> new BurningChargingParts(props, 0), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_BURNING_CHARGING_PARTS = REGISTRY.registerItem("improved_burning_charging_parts", props -> new BurningChargingParts(props, 1), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_BURNING_CHARGING_PARTS = REGISTRY.registerItem("advanced_burning_charging_parts", props -> new BurningChargingParts(props, 2), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> PRIMARY_FREEZING_CHARGING_PARTS = REGISTRY.registerItem("primary_freezing_charging_parts", props -> new FreezingChargingParts(props, 0), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_FREEZING_CHARGING_PARTS = REGISTRY.registerItem("improved_freezing_charging_parts", props -> new FreezingChargingParts(props, 1), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_FREEZING_CHARGING_PARTS = REGISTRY.registerItem("advanced_freezing_charging_parts", props -> new FreezingChargingParts(props, 2), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> PRIMARY_BURNING_BATTLE_PARTS = REGISTRY.registerItem("primary_burning_battle_parts", props -> new BurningBattleParts(props, 0), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_BURNING_BATTLE_PARTS = REGISTRY.registerItem("improved_burning_battle_parts", props -> new BurningBattleParts(props, 1), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_BURNING_BATTLE_PARTS = REGISTRY.registerItem("advanced_burning_battle_parts", props -> new BurningBattleParts(props, 2), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> PRIMARY_FREEZING_BATTLE_PARTS = REGISTRY.registerItem("primary_freezing_battle_parts", props -> new FreezingBattleParts(props, 0), new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> IMPROVED_FREEZING_BATTLE_PARTS = REGISTRY.registerItem("improved_freezing_battle_parts", props -> new FreezingBattleParts(props, 1), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> ADVANCED_FREEZING_BATTLE_PARTS = REGISTRY.registerItem("advanced_freezing_battle_parts", props -> new FreezingBattleParts(props, 2), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<Item> BANDAGE = REGISTRY.registerItem("bandage", Bandage::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> PAINKILLER = REGISTRY.registerItem("painkiller", Painkiller::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> MEDICAL_KIT = REGISTRY.registerItem("medical_kit", MedicalKit::new, new Item.Properties().durability(8));
    public static final DeferredItem<Item> SUSPICIOUS_DRUG = REGISTRY.registerItem("suspicious_drug", SuspiciousDrug::new, new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> MIRACLE = REGISTRY.registerItem("miracle", Miracle::new, new Item.Properties().stacksTo(16).rarity(Rarity.RARE));

    public static final DeferredItem<Item> COMPRESSED_BISCUIT = REGISTRY.registerItem("compressed_biscuit", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CHOCOLATE = REGISTRY.registerItem("chocolate", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_YELLOW_PEACH = REGISTRY.registerItem("canned_yellow_peach", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_BEEF_HOTPOT = REGISTRY.registerItem("canned_beef_hotpot", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(12).saturationModifier(1.2f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_LUNCHEON_MEAT = REGISTRY.registerItem("canned_luncheon_meat", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_FISH = REGISTRY.registerItem("canned_fish_in_black_bean_sauce", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_BREAD = REGISTRY.registerItem("canned_bread", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_BEANS = REGISTRY.registerItem("canned_beans", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).build())), new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> CANNED_TOMATOES = REGISTRY.registerItem("canned_tomatoes", props -> new TooltipItem(props
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3f).build())), new Item.Properties().stacksTo(64));


    public static final DeferredItem<Item> FLARE_GUN = REGISTRY.registerItem("flare_gun", FlareGun::new, new Item.Properties().stacksTo(4));

    public static final DeferredItem<Item> BATTERY = REGISTRY.registerItem("battery", Item::new, new Item.Properties().durability(100));

    public static final DeferredItem<Item> SANDPAPER = REGISTRY.registerItem("sandpaper", Item::new, new Item.Properties().durability(5));

    public static final DeferredItem<Item> PLASTIC_BAG = REGISTRY.registerItem("plastic_bag", Item::new, new Item.Properties());

    public static final DeferredItem<Item> ENERGY_ROD = REGISTRY.registerItem("energy_rod", EnergyRod::new, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> TRAP_COVER = block(ZombieKitBlocks.TRAP_COVER);

    public static final DeferredItem<Item> BARBED_WIRE = block(ZombieKitBlocks.BARBED_WIRE);
    public static final DeferredItem<Item> BARBED_WIRE_BROKEN = block(ZombieKitBlocks.BARBED_WIRE_BROKEN);
    public static final DeferredItem<Item> BARBED_WIRE_EXTREMELY_BROKEN = block(ZombieKitBlocks.BARBED_WIRE_EXTREMELY_BROKEN);

    public static final DeferredItem<Item> LANDMINE = block(ZombieKitBlocks.LANDMINE);
    public static final DeferredItem<Item> CHEMICAL_LANDMINE = block(ZombieKitBlocks.CHEMICAL_LANDMINE);

    public static final DeferredItem<Item> CHARGER = block(ZombieKitBlocks.CHARGER);
    public static final DeferredItem<Item> ULTRA_WIDEBAND_RADAR = block(ZombieKitBlocks.ULTRA_WIDEBAND_RADAR);
    public static final DeferredItem<Item> UV_LAMP = REGISTRY.registerItem("uv_lamp", UvLamp::new, new Item.Properties().durability(20));

    public static final DeferredItem<Item> INJECTOR = block(ZombieKitBlocks.INJECTOR);

    public static final DeferredItem<Item> SHOOTING_PARAMETER = REGISTRY.registerItem("shooting_parameters_table", TooltipItem::new, new Item.Properties());

    public static final DeferredItem<Item> POCKET_RADIO = REGISTRY.registerItem("pocket_radio", PocketRadio::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHORTWAVE_RADIO = block(ZombieKitBlocks.SHORTWAVE_RADIO);

    public static final DeferredItem<Item> VACUUM_PACKAGING_MACHINE = block(ZombieKitBlocks.VACUUM_PACKAGING_MACHINE);
    public static final DeferredItem<Item> MORTAR_RACK = REGISTRY.registerItem("mortar_rack", props -> new ZombieKitGeoBlockItem(ZombieKitBlocks.MORTAR_RACK.get(), props), new Item.Properties());

    public static final DeferredItem<Item> HEAVY_MACHINE_GUN_AMMO = REGISTRY.registerItem("heavy_machine_gun_ammo", Item::new, new Item.Properties());
    public static final DeferredItem<Item> HEAVY_MACHINE_GUN_SUMMON = REGISTRY.registerItem("heavy_machine_gun_summon", HeavyMachineGun::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MORTAR_SUMMON = REGISTRY.registerItem("mortar_summon", Mortar::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> DRONE_SUMMON = REGISTRY.registerItem("drone_summon", Drone::new, new Item.Properties());

    public static final DeferredItem<Item> GAS_TANK = block(ZombieKitBlocks.GAS_TANK);


    public static final DeferredItem<Item> DESERT_CAMOUFLAGE_DYE = REGISTRY.registerItem("desert_camouflage_dye", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> FOREST_CAMOUFLAGE_DYE = REGISTRY.registerItem("forest_camouflage_dye", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> SNOW_CAMOUFLAGE_DYE = REGISTRY.registerItem("snow_camouflage_dye", Item::new, new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<Item> LATEX = REGISTRY.registerItem("latex", Item::new, new Item.Properties());
    public static final DeferredItem<Item> RUBBER = REGISTRY.registerItem("rubber", Item::new, new Item.Properties());
    public static final DeferredItem<Item> FIBRE = REGISTRY.registerItem("fibre", Item::new, new Item.Properties());
    public static final DeferredItem<Item> CLOTH = REGISTRY.registerItem("cloth", Item::new, new Item.Properties());
    public static final DeferredItem<Item> QUARTZ_SAND = REGISTRY.registerItem("quartz_sand", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SPECIAL_CERAMICS = REGISTRY.registerItem("special_ceramics", Item::new, new Item.Properties());
    public static final DeferredItem<Item> BULLETPROOF_INSERT = REGISTRY.registerItem("bulletproof_insert", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SPECIAL_STEEL_SHEET = REGISTRY.registerItem("special_steel_sheet", Item::new, new Item.Properties());
    public static final DeferredItem<Item> CAMPHOR = REGISTRY.registerItem("camphor", Item::new, new Item.Properties());
    public static final DeferredItem<Item> DEATH_BAT = REGISTRY.registerItem("death_bat", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SALTPETER = REGISTRY.registerItem("saltpeter", Item::new, new Item.Properties());
    public static final DeferredItem<Item> CRUDE_NITRATE = REGISTRY.registerItem("crude_nitrate", Item::new, new Item.Properties());
    public static final DeferredItem<Item> HYDROGEN_NITRATE = REGISTRY.registerItem("hydrogen_nitrate", Item::new, new Item.Properties());
    public static final DeferredItem<Item> NITROCELLULOSE = REGISTRY.registerItem("nitrocellulose", Item::new, new Item.Properties());
    public static final DeferredItem<Item> PLASTICS = REGISTRY.registerItem("plastics", Item::new, new Item.Properties());
    public static final DeferredItem<Item> ALCOHOL = REGISTRY.registerItem("alcohol", Item::new, new Item.Properties());
    public static final DeferredItem<Item> IRON_WIRE = REGISTRY.registerItem("iron_wire", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SMOKELESS_GUNPOWDER = REGISTRY.registerItem("smokeless_gunpowder", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SULFUR = REGISTRY.registerItem("sulfur", Item::new, new Item.Properties());
    public static final DeferredItem<Item> SALTPETER_BUCKET = REGISTRY.registerItem("saltpeter_bucket", SaltpeterBucket::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SALTPETER_SOIL = REGISTRY.registerItem("saltpeter_soil", SaltpeterSoil::new, new Item.Properties());


    public static final DeferredItem<Item> DRONE_COMPONENTS = REGISTRY.registerItem("drone_components", Item::new, new Item.Properties());
    public static final DeferredItem<Item> MACHINE_GUN_COMPONENTS = REGISTRY.registerItem("machine_gun_components", Item::new, new Item.Properties());
    public static final DeferredItem<Item> MORTAR_COMPONENTS = REGISTRY.registerItem("mortar_components", Item::new, new Item.Properties());

    public static final DeferredItem<Item> BLUEPRINT1 = REGISTRY.registerItem("blueprint_of_steel", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> BLUEPRINT2 = REGISTRY.registerItem("blueprint_of_dye", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> BLUEPRINT3 = REGISTRY.registerItem("blueprint_of_riot", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> BLUEPRINT4 = REGISTRY.registerItem("blueprint_of_landmine", Item::new, new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<Item> BLUEPRINT5 = REGISTRY.registerItem("blueprint_of_drone", Item::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> BLUEPRINT6 = REGISTRY.registerItem("blueprint_of_heavy_machine_gun", Item::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> BLUEPRINT7 = REGISTRY.registerItem("blueprint_of_mortar", Item::new, new Item.Properties().rarity(Rarity.EPIC));


    public static final DeferredItem<Item> SCARASOL = REGISTRY.registerItem("scarasol", BonusItem::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> KYOKO = REGISTRY.registerItem("kyoko", BonusItem::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> WHITE_FOOD_CHESTPLATE = REGISTRY.registerItem("white_food_chestplate", props -> new WhiteFood(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, props), new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> KARAZHAN = REGISTRY.registerItem("karazhan", BonusItem::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> IRON_CURTAIN = REGISTRY.registerItem("iron_curtain", BonusItem::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> FLAME = REGISTRY.registerItem("flame", BonusItem::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> AOKINAO = REGISTRY.registerItem("aokinao", Aokinao::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> LHX_QING = REGISTRY.registerItem("lhx_qing", props -> new LHXQing(Tiers.WOOD, 5, -2.4f, props), new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> SONA_BAKEMONO = REGISTRY.registerItem("sona_bakemono", BonusItem::new, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> KONN_GARA = REGISTRY.registerItem("konn_gara", KonnGara::new, new Item.Properties().rarity(Rarity.EPIC).jukeboxPlayable(ZombieKitJukeboxSongs.KONN_GARA));


    private static DeferredItem<Item> block(DeferredHolder<Block, ? extends Block> block) {
        return REGISTRY.registerItem(block.getId().getPath(), props -> new BlockItem(block.get(), props), new Item.Properties());
    }


}
