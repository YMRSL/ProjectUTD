package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.BlockZAttachments;
import com.yitianys.BlockZ.init.ModItems;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 通用服务端事件（SPLIT 后只保留 KEEP 逻辑）。
 *
 * <p>上游 1.1.6 的本类承载大量 DROP 功能：体力/跳跃消耗、卧倒(Prone)、骨折(Fracture)/流血(Bleeding)、
 * 护理(Nursing)效果应用、低血量/卧倒移动惩罚与属性修饰符、状态同步(SyncPlayerStatusS2C)、
 * 摔落/受伤事件、以及右键拾取/打开 DayZ 容器(RightClick* 系列)。这些一律删除：
 * <ul>
 *   <li>护理/骨折/流血/体力/卧倒：DROP 功能，整段删除。</li>
 *   <li>右键拾取掉落物：已被 {@code LootPickupC2S} 网络包取代。</li>
 *   <li>右键打开工作台/附魔台/Lootr 的 DayZ 布局：已被 {@code OpenDayZContainerC2S}/
 *       {@code RequestSwitchToDayZMenuC2S} 取代。</li>
 * </ul>
 *
 * <p>保留两项 KEEP（属于 A 占格库存 / H 支撑，且在已落地工程中暂无其它归宿）：
 * <ol>
 *   <li>原版背包锁(Vanilla Backpack Lock)：DayZ UI 关闭且开启锁定时，把超出口袋容量的
 *       原版背包槽(9-35)填充为不可拾取的 LOCK_ITEM，并清理热栏/已解锁区域里的残留锁定物品。
 *       配合 {@code MixinSlot#mayPickup}。</li>
 *   <li>DayZ 模式下取消走过自然拾取(walk-over pickup)。</li>
 * </ol>
 */
@EventBusSubscriber(modid = BlockZ.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CommonModEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        Inventory inv = player.getInventory();

        // 清理鼠标持有 / 热栏(0-8) 上不应存在的锁定物品。
        if (player.containerMenu != null && player.containerMenu.getCarried().is(ModItems.LOCK_ITEM.get())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i).is(ModItems.LOCK_ITEM.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }

        boolean dayzEnabled = player.getData(BlockZAttachments.PLAYER_BACKPACK).isDayzEnabled();
        boolean isAdmin = player.hasPermissions(2);
        boolean lockEnabled = BlockZConfigs.getEnableVanillaBackpackLock();

        int allowedSlots = 0;
        if (!dayzEnabled && !isAdmin && lockEnabled) {
            // 基础口袋槽位（对应原版 9-13 等），装备提供的额外槽位在 Vanilla UI 模式下禁用。
            allowedSlots = BlockZConfigs.getInitialPocketSlots();
        }

        int unlockedEndIndex = 9 + allowedSlots;
        if (unlockedEndIndex > 36) {
            unlockedEndIndex = 36;
        }

        if (!dayzEnabled && !isAdmin && lockEnabled) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = inv.getItem(i);
                if (i < unlockedEndIndex) {
                    // 应解锁的区域：清除残留锁定物品。
                    if (stack.is(ModItems.LOCK_ITEM.get())) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                } else {
                    // 应锁定的区域。
                    if (stack.isEmpty()) {
                        inv.setItem(i, new ItemStack(ModItems.LOCK_ITEM.get()));
                    } else if (!stack.is(ModItems.LOCK_ITEM.get())) {
                        // 玩家强行塞入了非锁定物品：弹出并重新锁定。
                        player.drop(stack.copy(), true);
                        inv.setItem(i, new ItemStack(ModItems.LOCK_ITEM.get()));
                    }
                }
            }
        } else {
            // DayZ 启用或管理员：清除全部锁定物品。
            for (int i = 9; i < 36; i++) {
                if (inv.getItem(i).is(ModItems.LOCK_ITEM.get())) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) {
            return;
        }

        boolean dayzEnabled = player.getData(BlockZAttachments.PLAYER_BACKPACK).isDayzEnabled();
        // DayZ 模式下禁用自然拾取（走过物品时不拾取）。
        if (dayzEnabled) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
