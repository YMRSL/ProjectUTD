package net.tkg.ModernMayhem.server.util;

import net.neoforged.neoforge.common.ModConfigSpec;

public enum RigStorageProperties {
    RECON_RIG("recon_rig", 2, 6, true),
    BANDOLEER("bandoleer", 2, 3, true),
    PLATE_CARRIER_DEFAULT("plate_carrier_default", 0, 0, false),
    PLATE_CARRIER_AMMO("plate_carrier_ammo", 1, 4, true),
    PLATE_CARRIER_POUCHES("plate_carrier_pouches", 2, 4, false),
    HEXAGON_RIG("hexagon_rig", 0, 0, false);

    private final String name;
    private final int defaultLines;
    private final int defaultColumns;
    private final boolean defaultSuppliesAmmo;
    private final StorageConfigFile configFile;

    private RigStorageProperties(String name, int defaultLines, int defaultColumns, boolean defaultSuppliesAmmo) {
        this.name = name;
        this.defaultLines = defaultLines;
        this.defaultColumns = defaultColumns;
        this.defaultSuppliesAmmo = defaultSuppliesAmmo;
        this.configFile = new StorageConfigFile(name, defaultLines, defaultColumns, defaultSuppliesAmmo);
    }

    public String getName() {
        return this.name;
    }

    public int getLines() {
        return (Integer)this.configFile.LINES.get();
    }

    public int getColumns() {
        return (Integer)this.configFile.COLUMNS.get();
    }

    public boolean suppliesAmmo() {
        return (Boolean)this.configFile.SUPPLIES_AMMO.get();
    }

    public ModConfigSpec getConfig() {
        return this.configFile.getConfig();
    }

    public static class StorageConfigFile {
        public final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public ModConfigSpec CONFIG;
        public ModConfigSpec.IntValue LINES;
        public ModConfigSpec.IntValue COLUMNS;
        public ModConfigSpec.BooleanValue SUPPLIES_AMMO;

        public StorageConfigFile(String name, int defaultLines, int defaultCols, boolean defaultSuppliesAmmo) {
            this.BUILDER.push("Rig Configuration: " + name);
            this.LINES = this.BUILDER.comment(new String[]{"\nNumber of lines (rows) in the inventory.", "Default: " + defaultLines}).defineInRange("lines", defaultLines, 0, 9);
            this.COLUMNS = this.BUILDER.comment(new String[]{"\nNumber of columns in the inventory.", "Default: " + defaultCols}).defineInRange("columns", defaultCols, 0, 9);
            this.SUPPLIES_AMMO = this.BUILDER.comment(new String[]{"\nWhether this rig can automatically supply ammo to guns.", "Default: " + defaultSuppliesAmmo}).define("suppliesAmmo", defaultSuppliesAmmo);
            this.BUILDER.pop();
            this.CONFIG = this.BUILDER.build();
        }

        public ModConfigSpec getConfig() {
            return this.CONFIG;
        }
    }
}

