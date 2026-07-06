package io.github.ymrsl.firstpersonfoodeating.item;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Locale;

@EventBusSubscriber(modid = FirstPersonFoodEatingMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ConsumableUseLockController {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ROOT_KEY = "firstpersonfoodeating_use_lock";
    private static final String ACTIVE_KEY = "active";
    private static final String SLOT_KEY = "slot";
    private static final String ITEM_KEY = "item";
    private static final String FOOD_ID_KEY = "food_id";
    private static final String REMAINING_KEY = "remaining";
    private static final String TOTAL_KEY = "total";
    private static final String SOUND_SUPPRESS_UNTIL_KEY = "sound_suppress_until";
    private static final String USE_BLOCK_UNTIL_KEY = "use_block_until";
    private static final int POST_USE_BLOCK_TICKS = 5;
    private static final int MAX_ALLOWED_BLOCK_WINDOW_TICKS = 200;
    private static int lockLogBudget = 80;

    private ConsumableUseLockController() {
    }

    public static void tryStartLock(Player player, ItemStack stack, int usedTicks, int triggerTicks, int autoFinishTicks) {
        if (player == null || stack.isEmpty()) {
            return;
        }
        if (usedTicks < triggerTicks) {
            return;
        }
        CompoundTag root = getRootTag(player);
        if (root.getBoolean(ACTIVE_KEY)) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return;
        }
        int finishTicks = Math.max(autoFinishTicks, 1);
        root.putBoolean(ACTIVE_KEY, true);
        root.putInt(SLOT_KEY, player.getInventory().selected);
        root.putString(ITEM_KEY, itemId.toString());
        root.putString(FOOD_ID_KEY, FoodStackData.resolveFoodId(stack).toString());
        root.putInt(REMAINING_KEY, finishTicks);
        root.putInt(TOTAL_KEY, finishTicks);
        if (lockLogBudget > 0) {
            lockLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Use lock started: item={}, food={}, slot={}, triggerTicks={}, autoFinishTicks={}",
                    itemId, FoodStackData.resolveFoodId(stack), player.getInventory().selected, triggerTicks, finishTicks);
        }
        if (!player.level().isClientSide) {
            PostConsumeMessageController.scheduleFromUseStart(player, stack);
        }
    }

    public static boolean isLocked(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) {
            return false;
        }
        CompoundTag root = getRootTag(player);
        if (!root.getBoolean(ACTIVE_KEY)) {
            return false;
        }
        int slot = root.getInt(SLOT_KEY);
        if (slot != player.getInventory().selected) {
            return false;
        }
        return matchItem(root.getString(ITEM_KEY), root.getString(FOOD_ID_KEY), stack);
    }

    public static float getLockedProgress(Player player, ItemStack stack) {
        if (!isLocked(player, stack)) {
            return 0.0f;
        }
        CompoundTag root = getRootTag(player);
        int total = Math.max(root.getInt(TOTAL_KEY), 1);
        int remaining = Mth.clamp(root.getInt(REMAINING_KEY), 0, total);
        return Mth.clamp(1.0f - (float) remaining / (float) total, 0.0f, 1.0f);
    }

    public static boolean shouldSuppressVanillaConsumeSound(Player player) {
        if (player == null) {
            return false;
        }
        CompoundTag root = getRootTag(player);
        long now = currentGameTick(player);
        sanitizeLegacyTimeWindows(player, root, now);
        return readTickWindow(root, SOUND_SUPPRESS_UNTIL_KEY) > now;
    }

    public static boolean isUseTemporarilyBlocked(Player player) {
        if (player == null) {
            return false;
        }
        CompoundTag root = getRootTag(player);
        long now = currentGameTick(player);
        sanitizeLegacyTimeWindows(player, root, now);
        return readTickWindow(root, USE_BLOCK_UNTIL_KEY) > now;
    }

    public static int getRemainingUseBlockTicks(Player player) {
        if (player == null) {
            return 0;
        }
        CompoundTag root = getRootTag(player);
        long now = currentGameTick(player);
        sanitizeLegacyTimeWindows(player, root, now);
        long until = readTickWindow(root, USE_BLOCK_UNTIL_KEY);
        if (until <= now) {
            return 0;
        }
        long remaining = until - now;
        return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        CompoundTag root = getRootTag(player);
        if (!root.getBoolean(ACTIVE_KEY)) {
            return;
        }

        int slot = root.getInt(SLOT_KEY);
        if (slot < 0 || slot >= player.getInventory().items.size()) {
            clear(player);
            return;
        }
        if (player.getInventory().selected != slot) {
            player.getInventory().selected = slot;
        }

        ItemStack lockedStack = player.getInventory().getItem(slot);
        if (lockedStack.isEmpty() || !matchItem(root.getString(ITEM_KEY), root.getString(FOOD_ID_KEY), lockedStack)) {
            clear(player);
            return;
        }

        int remaining = Math.max(root.getInt(REMAINING_KEY) - 1, 0);
        root.putInt(REMAINING_KEY, remaining);
        if (remaining > 0) {
            return;
        }

        markSuppressVanillaConsumeSound(player, 10);
        markUseBlocked(player, POST_USE_BLOCK_TICKS);
        if (player.level().isClientSide && shouldTriggerConsumeFlash(root, lockedStack)
                && FMLEnvironment.dist == Dist.CLIENT) {
            io.github.ymrsl.firstpersonfoodeating.client.ConsumeFlashOverlayEvents.triggerOnConsume(player);
        }
        if (!player.level().isClientSide) {
            ItemStack current = player.getInventory().getItem(slot);
            if (!current.isEmpty() && matchItem(root.getString(ITEM_KEY), root.getString(FOOD_ID_KEY), current)) {
                ItemStack result = current.finishUsingItem(player.level(), player);
                if (result != current) {
                    player.getInventory().setItem(slot, result);
                }
            }
            player.getInventory().setChanged();
        }
        player.stopUsingItem();
        clear(player);
    }

    private static boolean matchItem(String expectedItemId, String expectedFoodId, ItemStack stack) {
        return FoodStackData.matches(stack, expectedItemId, expectedFoodId);
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ROOT_KEY, CompoundTag.TAG_COMPOUND)) {
            data.put(ROOT_KEY, new CompoundTag());
        }
        return data.getCompound(ROOT_KEY);
    }

    private static void clear(Player player) {
        CompoundTag root = getRootTag(player);
        root.putBoolean(ACTIVE_KEY, false);
        root.remove(SLOT_KEY);
        root.remove(ITEM_KEY);
        root.remove(FOOD_ID_KEY);
        root.remove(REMAINING_KEY);
        root.remove(TOTAL_KEY);
    }

    private static void markSuppressVanillaConsumeSound(Player player, int ticks) {
        CompoundTag root = getRootTag(player);
        long untilTick = currentGameTick(player) + Math.max(ticks, 1);
        root.putLong(SOUND_SUPPRESS_UNTIL_KEY, untilTick);
    }

    private static void markUseBlocked(Player player, int ticks) {
        CompoundTag root = getRootTag(player);
        long untilTick = currentGameTick(player) + Math.max(ticks, 1);
        root.putLong(USE_BLOCK_UNTIL_KEY, untilTick);
        if (lockLogBudget > 0) {
            lockLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Use temporarily blocked for {} ticks (until gameTime={})", Math.max(ticks, 1), untilTick);
        }
    }

    private static long currentGameTick(Player player) {
        if (player == null || player.level() == null) {
            return 0L;
        }
        return player.level().getGameTime();
    }

    private static long readTickWindow(CompoundTag root, String key) {
        if (root.contains(key, Tag.TAG_LONG)) {
            return root.getLong(key);
        }
        if (root.contains(key, Tag.TAG_INT)) {
            return root.getInt(key);
        }
        return 0L;
    }

    private static void sanitizeLegacyTimeWindows(Player player, CompoundTag root, long now) {
        sanitizeWindow(player, root, USE_BLOCK_UNTIL_KEY, now);
        sanitizeWindow(player, root, SOUND_SUPPRESS_UNTIL_KEY, now);
    }

    private static void sanitizeWindow(Player player, CompoundTag root, String key, long now) {
        long until = readTickWindow(root, key);
        if (until <= now) {
            return;
        }
        long delta = until - now;
        if (delta <= MAX_ALLOWED_BLOCK_WINDOW_TICKS) {
            return;
        }
        root.remove(key);
        if (lockLogBudget > 0) {
            lockLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Cleared stale '{}' window for player {} (delta={} ticks)",
                    key, player.getScoreboardName(), delta);
        }
    }

    private static boolean shouldTriggerConsumeFlash(CompoundTag root, ItemStack stack) {
        ResourceLocation foodId = FoodStackData.getFoodId(stack).orElse(null);
        if (foodId == null) {
            String rawFoodId = root.getString(FOOD_ID_KEY);
            if (!rawFoodId.isBlank()) {
                foodId = ResourceLocation.tryParse(rawFoodId);
            }
        }
        if (foodId == null) {
            return false;
        }
        String path = foodId.getPath();
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.contains("zhenji") || normalized.contains("syringe");
    }
}
