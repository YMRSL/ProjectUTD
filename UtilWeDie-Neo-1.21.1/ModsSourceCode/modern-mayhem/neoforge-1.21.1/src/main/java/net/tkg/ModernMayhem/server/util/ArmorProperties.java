package net.tkg.ModernMayhem.server.util;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.neoforge.common.ModConfigSpec;

public enum ArmorProperties {
    KEVLAR("kevlar", new int[]{25, 25, 25, 25}, SoundEvents.ARMOR_EQUIP_LEATHER, new double[]{3.0, 6.0, 8.0, 3.0}, new double[]{3.0, 3.0, 3.0, 3.0}, new double[]{0.15, 0.15, 0.15, 0.15}),
    NOTHING("nothing", new int[]{25, 25, 25, 25}, SoundEvents.ARMOR_EQUIP_LEATHER, new double[]{0.0, 0.0, 0.0, 0.0}, new double[]{0.0, 0.0, 0.0, 0.0}, new double[]{0.0, 0.0, 0.0, 0.0}),
    RONIN("ronin", new int[]{25, 25, 25, 25}, SoundEvents.ARMOR_EQUIP_LEATHER, new double[]{4.0, 8.0, 10.0, 4.0}, new double[]{3.0, 3.0, 3.0, 3.0}, new double[]{0.25, 0.25, 0.25, 0.25}),
    HAZMAT("hazmat", new int[]{25, 25, 25, 25}, SoundEvents.ARMOR_EQUIP_LEATHER, new double[]{2.0, 5.0, 4.0, 1.0}, new double[]{0.0, 0.0, 0.0, 0.0}, new double[]{0.0, 0.0, 0.0, 0.0});

    private final String name;
    private final int[] durabilityMultiplierArray;
    private final Holder<SoundEvent> equipSound;
    private final double[] protectionAmountArray;
    private final double[] toughnessAmountArray;
    private final double[] knockbackResistanceArray;
    private final ArmorConfigFile armorConfigFile;

    private ArmorProperties(String name, int[] durabilityMultiplierArray, Holder<SoundEvent> equipSound, double[] protectionAmountArray, double[] toughnessAmountArray, double[] knockbackResistanceArray) {
        this.name = name;
        this.durabilityMultiplierArray = durabilityMultiplierArray;
        this.equipSound = equipSound;
        this.protectionAmountArray = protectionAmountArray;
        this.toughnessAmountArray = toughnessAmountArray;
        this.knockbackResistanceArray = knockbackResistanceArray;
        this.armorConfigFile = new ArmorConfigFile(this.name, this.durabilityMultiplierArray, this.protectionAmountArray, this.toughnessAmountArray, this.knockbackResistanceArray);
    }

    public String getName() {
        return this.name;
    }

    public Holder<SoundEvent> getEquipSound() {
        return this.equipSound;
    }

    public int getDurabilityMultiplier(ArmorItem.Type type) {
        return this.armorConfigFile.getDurability(type);
    }

    public double getProtectionAmount(ArmorItem.Type type) {
        return this.armorConfigFile.getProtection(type);
    }

    public double getToughnessAmount(ArmorItem.Type type) {
        return this.armorConfigFile.getToughness(type);
    }

    public double getKnockbackResistance(ArmorItem.Type type) {
        return this.armorConfigFile.getKnockback(type);
    }

    public int getDefaultDurability(ArmorItem.Type type) {
        return this.durabilityMultiplierArray[type.getSlot().getIndex()];
    }

    public double getDefaultProtection(ArmorItem.Type type) {
        return this.protectionAmountArray[type.getSlot().getIndex()];
    }

    public double getDefaultToughness(ArmorItem.Type type) {
        return this.toughnessAmountArray[type.getSlot().getIndex()];
    }

    public double getDefaultKnockback(ArmorItem.Type type) {
        return this.knockbackResistanceArray[type.getSlot().getIndex()];
    }

    public ModConfigSpec getConfig() {
        return this.armorConfigFile.getConfig();
    }

    public static class ArmorConfigFile {
        public final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public ModConfigSpec CONFIG;
        public ModConfigSpec.IntValue DURABILITY_HEAD;
        public ModConfigSpec.IntValue DURABILITY_CHEST;
        public ModConfigSpec.IntValue DURABILITY_LEGS;
        public ModConfigSpec.IntValue DURABILITY_BOOTS;
        public ModConfigSpec.DoubleValue PROTECTION_AMOUNT_HEAD;
        public ModConfigSpec.DoubleValue PROTECTION_AMOUNT_CHESTPLATE;
        public ModConfigSpec.DoubleValue PROTECTION_AMOUNT_LEGGINGS;
        public ModConfigSpec.DoubleValue PROTECTION_AMOUNT_BOOTS;
        public ModConfigSpec.DoubleValue TOUGHNESS_AMOUNT_HEAD;
        public ModConfigSpec.DoubleValue TOUGHNESS_AMOUNT_CHESTPLATE;
        public ModConfigSpec.DoubleValue TOUGHNESS_AMOUNT_LEGGINGS;
        public ModConfigSpec.DoubleValue TOUGHNESS_AMOUNT_BOOTS;
        public ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE_HEAD;
        public ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE_CHESTPLATE;
        public ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE_LEGGINGS;
        public ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE_BOOTS;

        public ArmorConfigFile(String name, int[] durability, double[] protection, double[] toughness, double[] knockback) {
            this.BUILDER.push("Armor type : " + name);
            this.DURABILITY_HEAD = this.defineInt("durabilityMultiplierHead", "Durability multiplier for helmet", durability[3]);
            this.DURABILITY_CHEST = this.defineInt("durabilityMultiplierChest", "Durability multiplier for chestplate", durability[2]);
            this.DURABILITY_LEGS = this.defineInt("durabilityMultiplierLegs", "Durability multiplier for leggings", durability[1]);
            this.DURABILITY_BOOTS = this.defineInt("durabilityMultiplierBoots", "Durability multiplier for boots", durability[0]);
            this.PROTECTION_AMOUNT_HEAD = this.define("protectionAmountHead", "Protection amount for head armor", protection[3]);
            this.PROTECTION_AMOUNT_CHESTPLATE = this.define("protectionAmountChestplate", "Protection amount for chestplate armor", protection[2]);
            this.PROTECTION_AMOUNT_LEGGINGS = this.define("protectionAmountLeggings", "Protection amount for leggings armor", protection[1]);
            this.PROTECTION_AMOUNT_BOOTS = this.define("protectionAmountBoots", "Protection amount for boots armor", protection[0]);
            this.TOUGHNESS_AMOUNT_HEAD = this.define("toughnessAmountHead", "Toughness amount for head armor", toughness[3]);
            this.TOUGHNESS_AMOUNT_CHESTPLATE = this.define("toughnessAmountChestplate", "Toughness amount for chestplate armor", toughness[2]);
            this.TOUGHNESS_AMOUNT_LEGGINGS = this.define("toughnessAmountLeggings", "Toughness amount for leggings armor", toughness[1]);
            this.TOUGHNESS_AMOUNT_BOOTS = this.define("toughnessAmountBoots", "Toughness amount for boots armor", toughness[0]);
            this.KNOCKBACK_RESISTANCE_HEAD = this.define("knockbackResistanceHead", "Knockback resistance for head armor", knockback[3]);
            this.KNOCKBACK_RESISTANCE_CHESTPLATE = this.define("knockbackResistanceChestplate", "Knockback resistance for chestplate armor", knockback[2]);
            this.KNOCKBACK_RESISTANCE_LEGGINGS = this.define("knockbackResistanceLeggings", "Knockback resistance for leggings armor", knockback[1]);
            this.KNOCKBACK_RESISTANCE_BOOTS = this.define("knockbackResistanceBoots", "Knockback resistance for boots armor", knockback[0]);
            this.BUILDER.pop();
            this.CONFIG = this.BUILDER.build();
        }

        private ModConfigSpec.DoubleValue define(String path, String description, double defaultValue) {
            return this.BUILDER.comment(new String[]{"\n" + description, "Default: " + defaultValue}).defineInRange(path, defaultValue, 0.0, 100.0);
        }

        private ModConfigSpec.IntValue defineInt(String path, String description, int defaultValue) {
            return this.BUILDER.comment(new String[]{"\n" + description, "Default: " + defaultValue}).defineInRange(path, defaultValue, 0, 10000);
        }

        public int getDurability(ArmorItem.Type type) {
            return switch (type.getSlot()) {
                case EquipmentSlot.HEAD -> (Integer)this.DURABILITY_HEAD.get();
                case EquipmentSlot.CHEST -> (Integer)this.DURABILITY_CHEST.get();
                case EquipmentSlot.LEGS -> (Integer)this.DURABILITY_LEGS.get();
                case EquipmentSlot.FEET -> (Integer)this.DURABILITY_BOOTS.get();
                default -> 1;
            };
        }

        public double getProtection(ArmorItem.Type type) {
            return switch (type.getSlot()) {
                case EquipmentSlot.HEAD -> (Double)this.PROTECTION_AMOUNT_HEAD.get();
                case EquipmentSlot.CHEST -> (Double)this.PROTECTION_AMOUNT_CHESTPLATE.get();
                case EquipmentSlot.LEGS -> (Double)this.PROTECTION_AMOUNT_LEGGINGS.get();
                case EquipmentSlot.FEET -> (Double)this.PROTECTION_AMOUNT_BOOTS.get();
                default -> 0.0;
            };
        }

        public double getToughness(ArmorItem.Type type) {
            return switch (type.getSlot()) {
                case EquipmentSlot.HEAD -> (Double)this.TOUGHNESS_AMOUNT_HEAD.get();
                case EquipmentSlot.CHEST -> (Double)this.TOUGHNESS_AMOUNT_CHESTPLATE.get();
                case EquipmentSlot.LEGS -> (Double)this.TOUGHNESS_AMOUNT_LEGGINGS.get();
                case EquipmentSlot.FEET -> (Double)this.TOUGHNESS_AMOUNT_BOOTS.get();
                default -> 0.0;
            };
        }

        public double getKnockback(ArmorItem.Type type) {
            return switch (type.getSlot()) {
                case EquipmentSlot.HEAD -> (Double)this.KNOCKBACK_RESISTANCE_HEAD.get();
                case EquipmentSlot.CHEST -> (Double)this.KNOCKBACK_RESISTANCE_CHESTPLATE.get();
                case EquipmentSlot.LEGS -> (Double)this.KNOCKBACK_RESISTANCE_LEGGINGS.get();
                case EquipmentSlot.FEET -> (Double)this.KNOCKBACK_RESISTANCE_BOOTS.get();
                default -> 0.0;
            };
        }

        public ModConfigSpec getConfig() {
            return this.CONFIG;
        }
    }
}

