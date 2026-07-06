package net.tkg.ModernMayhem.server.util;

import net.neoforged.neoforge.common.ModConfigSpec;

public enum CuriosBodyProperties {
    RECON_RIG("recon_rig", 250, 2.0, 0.0, 0.0),
    BANDOLEER("bandoleer", 250, 2.0, 0.0, 0.0),
    PLATE_CARRIER("plate_carrier", 400, 7.0, 2.0, 0.3),
    HEXAGON_RIG("hexagon_rig", 600, 10.0, 3.0, 0.3);

    private final String name;
    private final int defaultDurability;
    private final double defaultProtection;
    private final double defaultToughness;
    private final double defaultKnockback;
    private final CuriosConfigFile curiosConfigFile;

    private CuriosBodyProperties(String name, int defaultDurability, double defaultProtection, double defaultToughness, double defaultKnockback) {
        this.name = name;
        this.defaultDurability = defaultDurability;
        this.defaultProtection = defaultProtection;
        this.defaultToughness = defaultToughness;
        this.defaultKnockback = defaultKnockback;
        this.curiosConfigFile = new CuriosConfigFile(name, defaultDurability, defaultProtection, defaultToughness, defaultKnockback);
    }

    public String getName() {
        return this.name;
    }

    public int getDurability() {
        return (Integer)this.curiosConfigFile.DURABILITY.get();
    }

    public double getProtection() {
        return (Double)this.curiosConfigFile.PROTECTION.get();
    }

    public double getToughness() {
        return (Double)this.curiosConfigFile.TOUGHNESS.get();
    }

    public double getKnockback() {
        return (Double)this.curiosConfigFile.KNOCKBACK.get();
    }

    public ModConfigSpec getConfig() {
        return this.curiosConfigFile.getConfig();
    }

    public static class CuriosConfigFile {
        public final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public ModConfigSpec CONFIG;
        public ModConfigSpec.IntValue DURABILITY;
        public ModConfigSpec.DoubleValue PROTECTION;
        public ModConfigSpec.DoubleValue TOUGHNESS;
        public ModConfigSpec.DoubleValue KNOCKBACK;

        public CuriosConfigFile(String name, int durability, double protection, double toughness, double knockback) {
            this.BUILDER.push("Curios Item: " + name);
            this.DURABILITY = this.defineInt("durability", "Max Durability of the item (Set to 0 for Unbreakable)", durability);
            this.PROTECTION = this.define("protectionAmount", "Protection amount provided by this item", protection);
            this.TOUGHNESS = this.define("toughnessAmount", "Armor Toughness provided by this item", toughness);
            this.KNOCKBACK = this.define("knockbackResistance", "Knockback Resistance provided by this item", knockback);
            this.BUILDER.pop();
            this.CONFIG = this.BUILDER.build();
        }

        private ModConfigSpec.DoubleValue define(String path, String description, double defaultValue) {
            return this.BUILDER.comment(new String[]{"\n" + description, "Default: " + defaultValue}).defineInRange(path, defaultValue, 0.0, 100.0);
        }

        private ModConfigSpec.IntValue defineInt(String path, String description, int defaultValue) {
            return this.BUILDER.comment(new String[]{"\n" + description, "Default: " + defaultValue}).defineInRange(path, defaultValue, 0, 10000);
        }

        public ModConfigSpec getConfig() {
            return this.CONFIG;
        }
    }
}

