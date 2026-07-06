package com.yitianys.BlockZ.config;

import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * BlockZ 配置（1.21.1 NeoForge 移植版）。
 * 仅保留占格背包 / 服装系统所需配置；护理、僵尸、尸体、HUD、镜头、体力、倾身、主菜单等
 * 已剔除功能的配置项一并移除。
 *
 * ForgeConfigSpec -> ModConfigSpec；ForgeRegistries.ITEMS -> BuiltInRegistries.ITEM。
 */
public class BlockZConfigs {
    public static ModConfigSpec COMMON_SPEC;

    // --- GUI / 占格 ---
    public static ModConfigSpec.BooleanValue enableGridSystem;
    public static ModConfigSpec.IntValue gridCols;
    public static ModConfigSpec.IntValue gridRows;
    public static ModConfigSpec.DoubleValue uiScale;
    public static ModConfigSpec.BooleanValue enableDayzInventory;
    public static ModConfigSpec.BooleanValue allowPlayerToggleDayz;
    public static ModConfigSpec.BooleanValue showDayzToggleChatHint;
    public static ModConfigSpec.BooleanValue enableVanillaBackpackLock;
    public static ModConfigSpec.IntValue initialPocketSlots;

    // --- 背包 / 服装格子数 ---
    public static ModConfigSpec.IntValue backpackCoyoteSlots;
    public static ModConfigSpec.IntValue backpackAliceSlots;
    public static ModConfigSpec.IntValue backpackCzechSlots;
    public static ModConfigSpec.IntValue backpackCzechPouchSlots;
    public static ModConfigSpec.IntValue backpackPatrolPackSlots;
    public static ModConfigSpec.IntValue vest0Slots;
    public static ModConfigSpec.IntValue shirtSlots;
    public static ModConfigSpec.IntValue pantsSlots;

    // --- 同步缓存（客户端从服务器同步配置时使用） ---
    private static boolean isSynced = false;
    private static int s_gridCols;
    private static int s_gridRows;
    private static boolean s_enableGridSystem;
    private static double s_uiScale;
    private static boolean s_enableDayzInventory;
    private static boolean s_allowPlayerToggleDayz;
    private static boolean s_showDayzToggleChatHint;
    private static boolean s_enableVanillaBackpackLock;
    private static int s_initialPocketSlots;
    private static int s_backpackCoyoteSlots;
    private static int s_backpackAliceSlots;
    private static int s_backpackCzechSlots;
    private static int s_backpackCzechPouchSlots;
    private static int s_backpackPatrolPackSlots;
    private static int s_vest0Slots;
    private static int s_shirtSlots;
    private static int s_pantsSlots;

    public static void setSyncedValues(
        int gridCols, int gridRows, boolean enableGridSystem, double uiScale, boolean enableDayzInventory,
        boolean allowPlayerToggleDayz, boolean showDayzToggleChatHint,
        boolean enableVanillaBackpackLock, int initialPocketSlots,
        int backpackCoyoteSlots, int backpackAliceSlots, int backpackCzechSlots, int backpackCzechPouchSlots,
        int backpackPatrolPackSlots, int vest0Slots, int shirtSlots, int pantsSlots
    ) {
        s_gridCols = gridCols;
        s_gridRows = gridRows;
        s_enableGridSystem = enableGridSystem;
        s_uiScale = uiScale;
        s_enableDayzInventory = enableDayzInventory;
        s_allowPlayerToggleDayz = allowPlayerToggleDayz;
        s_showDayzToggleChatHint = showDayzToggleChatHint;
        s_enableVanillaBackpackLock = enableVanillaBackpackLock;
        s_initialPocketSlots = initialPocketSlots;
        s_backpackCoyoteSlots = backpackCoyoteSlots;
        s_backpackAliceSlots = backpackAliceSlots;
        s_backpackCzechSlots = backpackCzechSlots;
        s_backpackCzechPouchSlots = backpackCzechPouchSlots;
        s_backpackPatrolPackSlots = backpackPatrolPackSlots;
        s_vest0Slots = vest0Slots;
        s_shirtSlots = shirtSlots;
        s_pantsSlots = pantsSlots;
        isSynced = true;
    }

    public static void clearSyncedValues() {
        isSynced = false;
    }

    // --- 占格 / GUI getters ---
    public static boolean isGridEnabled() { return isSynced ? s_enableGridSystem : enableGridSystem.get(); }
    public static int getGridCols() { return isSynced ? s_gridCols : gridCols.get(); }
    public static int getGridRows() { return isSynced ? s_gridRows : gridRows.get(); }
    public static double getUiScale() { return isSynced ? s_uiScale : uiScale.get(); }
    public static boolean isDayzInventoryEnabled() { return isSynced ? s_enableDayzInventory : enableDayzInventory.get(); }
    public static boolean getAllowPlayerToggleDayz() { return isSynced ? s_allowPlayerToggleDayz : allowPlayerToggleDayz.get(); }
    public static boolean getShowDayzToggleChatHint() { return isSynced ? s_showDayzToggleChatHint : showDayzToggleChatHint.get(); }
    public static boolean getEnableVanillaBackpackLock() { return isSynced ? s_enableVanillaBackpackLock : enableVanillaBackpackLock.get(); }
    public static int getInitialPocketSlots() { return isSynced ? s_initialPocketSlots : initialPocketSlots.get(); }

    // --- 背包 / 服装格子数 getters ---
    public static int getBackpackCoyoteSlots() { return isSynced ? s_backpackCoyoteSlots : backpackCoyoteSlots.get(); }
    public static int getBackpackAliceSlots() { return isSynced ? s_backpackAliceSlots : backpackAliceSlots.get(); }
    public static int getBackpackCzechSlots() { return isSynced ? s_backpackCzechSlots : backpackCzechSlots.get(); }
    public static int getBackpackCzechPouchSlots() { return isSynced ? s_backpackCzechPouchSlots : backpackCzechPouchSlots.get(); }
    public static int getBackpackPatrolPackSlots() { return isSynced ? s_backpackPatrolPackSlots : backpackPatrolPackSlots.get(); }
    public static int getVest0Slots() { return isSynced ? s_vest0Slots : vest0Slots.get(); }
    public static int getShirtSlots() { return isSynced ? s_shirtSlots : shirtSlots.get(); }
    public static int getPantsSlots() { return isSynced ? s_pantsSlots : pantsSlots.get(); }

    public static void register() {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("gui");
        enableGridSystem = b.comment("是否启用占格系统 / Enable grid items").define("grid.enable", true);
        gridCols = b.comment("网格列数 / Grid columns").defineInRange("grid.cols", 9, 1, 20);
        gridRows = b.comment("网格行数 / Grid rows").defineInRange("grid.rows", 4, 1, 20);
        uiScale = b.comment("UI 缩放 / UI scale").defineInRange("ui.scale", 1.0, 0.5, 2.0);
        enableDayzInventory = b.comment("是否启用 DayZ 背包界面 / Enable DayZ inventory UI").define("ui.enable_dayz_inventory", true);
        allowPlayerToggleDayz = b.comment("允许玩家切换 DayZ 界面 / Allow player toggle").define("ui.allow_player_toggle", true);
        showDayzToggleChatHint = b.comment("显示 DayZ 切换提示 / Show DayZ toggle chat hint").define("ui.show_dayz_toggle_hint", true);
        enableVanillaBackpackLock = b.comment("是否启用原版背包锁定机制 / Enable vanilla backpack locking").define("ui.enable_vanilla_lock", true);
        initialPocketSlots = b.comment("初始口袋格子数 (无背包时) / Initial pocket slots (without backpack)").defineInRange("ui.initial_pocket_slots", 5, 0, 27);
        b.pop();

        b.push("backpacks");
        backpackCoyoteSlots = b.comment("土狼背包格子数 / Coyote backpack slots").defineInRange("backpack_coyote_slots", 24, 0, 30);
        backpackAliceSlots = b.comment("Alice 背包格子数 / Alice backpack slots").defineInRange("backpack_alice_slots", 20, 0, 30);
        backpackCzechSlots = b.comment("捷克背包格子数 / Czech backpack slots").defineInRange("backpack_czech_slots", 16, 0, 30);
        backpackCzechPouchSlots = b.comment("捷克挂包格子数 / Czech pouch slots").defineInRange("backpack_czechpouch_slots", 6, 0, 30);
        backpackPatrolPackSlots = b.comment("巡逻包格子数 / Patrol pack slots").defineInRange("backpack_patrolpack_slots", 8, 0, 30);
        vest0Slots = b.comment("背心格子数 / Vest slots").defineInRange("vest_0_slots", 12, 0, 30);
        shirtSlots = b.comment("衣服口袋格子数 / Shirt pocket slots").defineInRange("shirt_slots", 6, 0, 30);
        pantsSlots = b.comment("裤子口袋格子数 / Pants pocket slots").defineInRange("pants_slots", 4, 0, 30);
        b.pop();

        COMMON_SPEC = b.build();
    }

    /**
     * 获取指定物品提供的背包格子数。
     */
    public static int getBackpackSlots(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
        if (rl == null) return 0;

        int customSlots = ItemSizeManager.getCustomSlots(stack);
        if (customSlots >= 0) return customSlots;

        String name = rl.getPath();
        if (name.equals("backpack_coyote")) return getBackpackCoyoteSlots();
        if (name.equals("backpack_alice")) return getBackpackAliceSlots();
        if (name.equals("backpack_czech")) return getBackpackCzechSlots();
        if (name.equals("backpack_czechpouch")) return getBackpackCzechPouchSlots();
        if (name.equals("backpack_patrolpack")) return getBackpackPatrolPackSlots();
        if (name.equals("vest_0")) return getVest0Slots();

        // 衣服和裤子通用配置
        if (name.startsWith("shirt_") || name.equals("shirt")) {
            return getShirtSlots();
        }
        if (name.startsWith("pants_") || name.equals("pants")) return getPantsSlots();

        // 兼容旧物品
        if (name.equals("small_backpack")) return 9;
        if (name.equals("medium_backpack")) return 15;
        if (name.equals("large_backpack")) return 22;
        if (name.equals("vest")) return 6;

        return 0;
    }
}
