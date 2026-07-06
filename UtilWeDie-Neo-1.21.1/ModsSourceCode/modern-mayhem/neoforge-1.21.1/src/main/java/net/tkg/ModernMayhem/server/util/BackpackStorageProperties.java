package net.tkg.ModernMayhem.server.util;

import net.neoforged.neoforge.common.ModConfigSpec;

public enum BackpackStorageProperties {
    TIER_1("tier_1", 3, 3),
    TIER_2("tier_2", 3, 6),
    TIER_3("tier_3", 3, 9);

    private final String name;
    private final int defaultLines;
    private final int defaultColumns;
    private final StorageConfigFile configFile;

    private BackpackStorageProperties(String name, int defaultLines, int defaultColumns) {
        this.name = name;
        this.defaultLines = defaultLines;
        this.defaultColumns = defaultColumns;
        this.configFile = new StorageConfigFile(name, defaultLines, defaultColumns);
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

    public ModConfigSpec getConfig() {
        return this.configFile.getConfig();
    }

    public static BackpackStorageProperties getByTier(int tier) {
        return switch (tier) {
            case 1 -> TIER_1;
            case 2 -> TIER_2;
            case 3 -> TIER_3;
            default -> TIER_1;
        };
    }

    public static class StorageConfigFile {
        public final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
        public ModConfigSpec CONFIG;
        public ModConfigSpec.IntValue LINES;
        public ModConfigSpec.IntValue COLUMNS;

        public StorageConfigFile(String name, int defaultLines, int defaultCols) {
            this.BUILDER.push("Backpack Configuration: " + name);
            this.LINES = this.BUILDER.comment(new String[]{"\nNumber of lines (rows) in the inventory.", "Default: " + defaultLines}).defineInRange("lines", defaultLines, 0, 9);
            this.COLUMNS = this.BUILDER.comment(new String[]{"\nNumber of columns in the inventory.", "Default: " + defaultCols}).defineInRange("columns", defaultCols, 0, 9);
            this.BUILDER.pop();
            this.CONFIG = this.BUILDER.build();
        }

        public ModConfigSpec getConfig() {
            return this.CONFIG;
        }
    }
}

