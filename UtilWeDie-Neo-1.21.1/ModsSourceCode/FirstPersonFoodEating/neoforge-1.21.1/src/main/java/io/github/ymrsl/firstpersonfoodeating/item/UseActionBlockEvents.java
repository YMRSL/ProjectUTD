package io.github.ymrsl.firstpersonfoodeating.item;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(modid = FirstPersonFoodEatingMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class UseActionBlockEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int blockLogBudget = 80;

    private UseActionBlockEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (shouldCancelUse(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            logBlockedUse(event.getEntity(), "RightClickItem");
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (shouldCancelUse(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            logBlockedUse(event.getEntity(), "RightClickBlock");
        }
    }

    private static boolean shouldCancelUse(Player player) {
        return ConsumableUseLockController.isUseTemporarilyBlocked(player);
    }

    private static void logBlockedUse(Player player, String source) {
        if (player == null || blockLogBudget <= 0) {
            return;
        }
        blockLogBudget--;
        ItemStack main = player.getMainHandItem();
        ResourceLocation itemId = main.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(main.getItem());
        int remaining = ConsumableUseLockController.getRemainingUseBlockTicks(player);
        LOGGER.info("[firstpersonfoodeating] Blocked use event from {}: player={}, mainHand={}, remainingBlockTicks={}",
                source,
                player.getScoreboardName(),
                itemId == null ? "<empty>" : itemId,
                remaining);
    }
}
