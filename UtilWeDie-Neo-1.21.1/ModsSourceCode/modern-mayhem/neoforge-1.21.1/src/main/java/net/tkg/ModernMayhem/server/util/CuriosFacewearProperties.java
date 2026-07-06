package net.tkg.ModernMayhem.server.util;

import net.neoforged.neoforge.common.ModConfigSpec;

public enum CuriosFacewearProperties {
    VISOR("visor", 4.0, 0.0, 0.0);

    private final String name;
    private final double defaultProtection;
    private final double defaultToughness;
    private final double defaultKnockback;
    private final FacewearConfigFile facewearConfigFile;

    private CuriosFacewearProperties(String name, double defaultProtection, double defaultToughness, double defaultKnockback) {
        this.name = name;
        this.defaultProtection = defaultProtection;
        this.defaultToughness = defaultToughness;
        this.defaultKnockback = defaultKnockback;
        this.facewearConfigFile = new FacewearConfigFile(name, defaultProtection, defaultToughness, defaultKnockback);
    }

    public String getName() {
        return this.name;
    }

    public double getProtection() {
        return (Double)this.facewearConfigFile.PROTECTION.get();
    }

    public double getToughness() {
        return (Double)this.facewearConfigFile.TOUGHNESS.get();
    }

    public double getKnockback() {
        return (Double)this.facewearConfigFile.KNOCKBACK.get();
    }

    public ModConfigSpec getConfig() {
        return this.facewearConfigFile.getConfig();
    }

    public static class FacewearConfigFile {
        public final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public ModConfigSpec CONFIG;
        public ModConfigSpec.DoubleValue PROTECTION;
        public ModConfigSpec.DoubleValue TOUGHNESS;
        public ModConfigSpec.DoubleValue KNOCKBACK;

        public FacewearConfigFile(String name, double protection, double toughness, double knockback) {
            this.BUILDER.push("Facewear Item: " + name);
            this.PROTECTION = this.define("protectionAmount", "Protection amount provided by this item", protection);
            this.TOUGHNESS = this.define("toughnessAmount", "Armor Toughness provided by this item", toughness);
            this.KNOCKBACK = this.define("knockbackResistance", "Knockback Resistance provided by this item", knockback);
            this.BUILDER.pop();
            this.CONFIG = this.BUILDER.build();
        }

        private ModConfigSpec.DoubleValue define(String path, String description, double defaultValue) {
            return this.BUILDER.comment(new String[]{"\n" + description, "Default: " + defaultValue}).defineInRange(path, defaultValue, 0.0, 100.0);
        }

        public ModConfigSpec getConfig() {
            return this.CONFIG;
        }
    }
}

