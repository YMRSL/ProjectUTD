package org.yanbwe.searchcarefully;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Configuration options for the search system
    public static ModConfigSpec.BooleanValue ENABLE_SEARCH_SYSTEM;
    public static ModConfigSpec.IntValue MAX_SEARCH_TIME_TICKS;
    public static ModConfigSpec.DoubleValue SEARCH_SPEED_MULTIPLIER;
    
    // 各稀有度等级的基础搜索时间
    public static ModConfigSpec.IntValue[] RARITY_BASE_TIMES = new ModConfigSpec.IntValue[8]; // 索引0未使用，1-7对应稀有度
    
    // 各稀有度等级的独立随机时间增量
    public static ModConfigSpec.IntValue[] RARITY_RANDOM_TIMES = new ModConfigSpec.IntValue[8]; // 索引0未使用，1-7对应稀有度
    
    // Custom loot table paths configuration
    public static ModConfigSpec.ConfigValue<List<? extends String>> CUSTOM_LOOT_TABLE_PATHS;
    
    // Chest path segments for dynamic matching
    public static ModConfigSpec.ConfigValue<List<? extends String>> CHEST_PATH_SEGMENTS;
    
    // Single slot search configuration
    public static ModConfigSpec.BooleanValue ENABLE_SINGLE_SLOT_SEARCH;
    public static ModConfigSpec.BooleanValue SINGLE_SLOT_SEARCH_TIME_MULTIPLIER;

    // Mouse target search configuration
    public static ModConfigSpec.BooleanValue ENABLE_MOUSE_TARGET_SEARCH;
    public static ModConfigSpec.DoubleValue MOUSE_TARGET_SWITCH_DELAY;

    // Search progress sound configuration
    public static ModConfigSpec.BooleanValue ENABLE_SEARCH_PROGRESS_SOUND;

    // Mask rendering configuration
    public static ModConfigSpec.BooleanValue MASK_RENDER_ON_TOP;

    // Hotbar search configuration
    public static ModConfigSpec.BooleanValue ENABLE_HOTBAR_SEARCH;
    
    static {
        BUILDER.push("Search System Configuration");

        ENABLE_SEARCH_SYSTEM = BUILDER
                .comment("Enable the tactical search system")
                .define("enableSearchSystem", true);

        MAX_SEARCH_TIME_TICKS = BUILDER
                .comment("Maximum search time in ticks (20 ticks = 1 second)")
                .defineInRange("maxSearchTimeTicks", 200, 1, 1000);

        SEARCH_SPEED_MULTIPLIER = BUILDER
                .comment("Multiplier for search speed (higher values = faster search)")
                .defineInRange("searchSpeedMultiplier", 1.0, 0.1, 10.0);
        
        // Base search times for each rarity level
        RARITY_BASE_TIMES[1] = BUILDER
                .comment("Base search time for rarity 1 (in ticks)")
                .defineInRange("rarity1BaseTime", 10, 1, 1000);
        RARITY_BASE_TIMES[2] = BUILDER
                .comment("Base search time for rarity 2 (in ticks)")
                .defineInRange("rarity2BaseTime", 30, 1, 1000);
        RARITY_BASE_TIMES[3] = BUILDER
                .comment("Base search time for rarity 3 (in ticks)")
                .defineInRange("rarity3BaseTime", 55, 1, 1000);
        RARITY_BASE_TIMES[4] = BUILDER
                .comment("Base search time for rarity 4 (in ticks)")
                .defineInRange("rarity4BaseTime", 85, 1, 1000);
        RARITY_BASE_TIMES[5] = BUILDER
                .comment("Base search time for rarity 5 (in ticks)")
                .defineInRange("rarity5BaseTime", 110, 1, 1000);
        RARITY_BASE_TIMES[6] = BUILDER
                .comment("Base search time for rarity 6 (in ticks)")
                .defineInRange("rarity6BaseTime", 130, 1, 1000);
        RARITY_BASE_TIMES[7] = BUILDER
                .comment("Base search time for rarity 7 (in ticks)")
                .defineInRange("rarity7BaseTime", 140, 1, 1000);
        
        // Individual random time additions for each rarity level
        RARITY_RANDOM_TIMES[1] = BUILDER
                .comment("Random time addition for rarity 1 (in ticks, 0 = no randomness)")
                .defineInRange("rarity1RandomTime", 10, 0, 1000);
        RARITY_RANDOM_TIMES[2] = BUILDER
                .comment("Random time addition for rarity 2 (in ticks, 0 = no randomness)")
                .defineInRange("rarity2RandomTime", 10, 0, 1000);
        RARITY_RANDOM_TIMES[3] = BUILDER
                .comment("Random time addition for rarity 3 (in ticks, 0 = no randomness)")
                .defineInRange("rarity3RandomTime", 10, 0, 1000);
        RARITY_RANDOM_TIMES[4] = BUILDER
                .comment("Random time addition for rarity 4 (in ticks, 0 = no randomness)")
                .defineInRange("rarity4RandomTime", 10, 0, 1000);
        RARITY_RANDOM_TIMES[5] = BUILDER
                .comment("Random time addition for rarity 5 (in ticks, 0 = no randomness)")
                .defineInRange("rarity5RandomTime", 10, 0, 1000);
        RARITY_RANDOM_TIMES[6] = BUILDER
                .comment("Random time addition for rarity 6 (in ticks, 0 = no randomness)")
                .defineInRange("rarity6RandomTime", 10, 0, 1000);
        RARITY_RANDOM_TIMES[7] = BUILDER
                .comment("Random time addition for rarity 7 (in ticks, 0 = no randomness)")
                .defineInRange("rarity7RandomTime", 10, 0, 1000);
        
        // Custom loot table paths configuration
        CUSTOM_LOOT_TABLE_PATHS = BUILDER
                .comment("Additional loot table paths to apply search times to (one path per line)",
                         "Example: ['modid:special_chest', 'anothermod:treasure_box']",
                         "Note: These are full resource locations, not just path prefixes")
                .defineListAllowEmpty(List.of("customLootTablePaths"), 
                                    ArrayList::new, 
                                    obj -> obj instanceof String s && !s.isEmpty());
        
        // Chest path segments for dynamic middle-path matching
        CHEST_PATH_SEGMENTS = BUILDER
                .comment("Path segments that indicate chest-type loot tables for middle-path matching",
                         "Example: ['chest', 'chests', 'treasure']",
                         "Used to match paths like 'structures/village/chest' or 'modid/special/chests'")
                .defineListAllowEmpty(List.of("chestPathSegments"),
                                    () -> List.of("chest", "chests", "block"),
                                    obj -> obj instanceof String s && !s.isEmpty());
        
        // Single slot search configuration
        ENABLE_SINGLE_SLOT_SEARCH = BUILDER
                .comment("Enable single slot search mode (search one item at a time, progressing through slots sequentially)")
                .define("enableSingleSlotSearch", false);

        SINGLE_SLOT_SEARCH_TIME_MULTIPLIER = BUILDER
                .comment("When enabled, single slot search processes items 3x faster than normal search",
                         "Default: true")
                .define("singleSlotSearchTimeMultiplier", true);

        // Hotbar search configuration
        ENABLE_HOTBAR_SEARCH = BUILDER
                .comment("Enable search system for hotbar slots (items in hotbar will be searched automatically)",
                         "Default: false")
                .define("enableHotbarSearch", false);

        BUILDER.pop();

        BUILDER.push("Mouse Target Search");

        ENABLE_MOUSE_TARGET_SEARCH = BUILDER
                .comment("Enable mouse target search mode (prioritizes the item under your mouse cursor for searching)")
                .define("enableMouseTargetSearch", true);

        MOUSE_TARGET_SWITCH_DELAY = BUILDER
                .comment("Delay in seconds before switching to a new mouse target when hovering over a different item",
                         "Range: 0.0 - 20.0 seconds")
                .defineInRange("mouseTargetSwitchDelay", 3.0, 0.0, 20.0);

        BUILDER.pop();

        BUILDER.push("Search Progress Sound");

        ENABLE_SEARCH_PROGRESS_SOUND = BUILDER
                .comment("Enable a looping sound effect that plays while searching items in containers")
                .define("enableSearchProgressSound", true);

        BUILDER.pop();

        BUILDER.push("Mask Rendering");

        MASK_RENDER_ON_TOP = BUILDER
                .comment("Render search mask on top of all UI elements (including tooltips) for better compatibility with other mods")
                .define("maskRenderOnTop", false);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}