package com.yitianys.BlockZ.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;
import java.util.List;

/**
 * 物品/拾取相关工具。
 *
 * 注意：上游的 Lootr 联动（getLootrInventory / getLootrTileId / startOpenLootr / stopOpenLootr）
 * 属于 DROP 范围（PORTING_CONVENTIONS §0），已整体删除；其调用处由对应 KEEP 文件移除。
 * 此处仅保留地面物品瞄准拾取（getTargetedItemEntity）与 DayZ 背包添加（addItemToDayZInventory）。
 */
public class InventoryUtils {

    // 1.21.1 (parchment) Slot 的 x / y 字段为 final，无法直接赋值；用反射设位（与 DayZInventoryMenu 一致）。
    private static final java.lang.reflect.Field SLOT_X_FIELD;
    private static final java.lang.reflect.Field SLOT_Y_FIELD;
    static {
        java.lang.reflect.Field x = null;
        java.lang.reflect.Field y = null;
        try {
            x = net.minecraft.world.inventory.Slot.class.getDeclaredField("x");
            y = net.minecraft.world.inventory.Slot.class.getDeclaredField("y");
            x.setAccessible(true);
            y.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        SLOT_X_FIELD = x;
        SLOT_Y_FIELD = y;
    }

    /** 反射设置 Slot 的屏幕坐标（x/y 在 1.21.1 为 final）。 */
    public static void setSlotPosition(net.minecraft.world.inventory.Slot slot, int x, int y) {
        if (SLOT_X_FIELD == null || SLOT_Y_FIELD == null) return;
        try {
            SLOT_X_FIELD.setInt(slot, x);
            SLOT_Y_FIELD.setInt(slot, y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Entity getTargetedItemEntity(Player player, double reach) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 viewVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(viewVec.scale(reach));

        // 1. 精确射线检测 (Precise Raytrace)
        // 扩大搜索范围以覆盖整个触达距离
        AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(reach)).inflate(1.0D);

        EntityHitResult result = ProjectileUtil.getEntityHitResult(
                player,
                eyePos,
                endPos,
                searchBox,
                e -> e instanceof ItemEntity && e.isAlive(),
                reach * reach
        );

        if (result != null) {
            return result.getEntity();
        }

        // 2. 宽容射线检测 (Thick Raytrace / Cone Trace)
        // 如果精确检测失败 (通常因为 ItemEntity 碰撞箱太小)，尝试检测视线周围的物品
        // 这解决了 "准星对准物品却无法拾取" 的问题
        List<ItemEntity> nearby = player.level().getEntitiesOfClass(ItemEntity.class, searchBox);

        ItemEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        double tolerance = 0.5; // 允许 0.5 格的误差范围

        for (ItemEntity item : nearby) {
            // 将物品碰撞箱临时扩大，检测视线是否穿过这个扩大的区域
            AABB expandedBox = item.getBoundingBox().inflate(tolerance);
            Optional<Vec3> clip = expandedBox.clip(eyePos, endPos);

            if (clip.isPresent()) {
                double d = eyePos.distanceToSqr(clip.get());
                if (d < closestDistSq && d < reach * reach) {
                    closestDistSq = d;
                    closest = item;
                }
            }
        }

        return closest;
    }

    /**
     * DayZ 模式下专用的物品添加逻辑
     * 仅允许添加到快捷栏 (0-8) 和口袋 (9-13)
     * @param inv 玩家背包
     * @param stack 要添加的物品
     * @return 是否成功添加（或部分添加）
     */
    public static boolean addItemToDayZInventory(Inventory inv, ItemStack stack) {
        boolean changed = false;
        int pocketCount = com.yitianys.BlockZ.config.BlockZConfigs.getInitialPocketSlots();
        int maxSlot = 9 + pocketCount; // 0-8 (Hotbar) + 9-(9+count-1) (Pockets)

        // 1. 尝试堆叠到现有物品
        for (int i = 0; i < maxSlot; i++) {
            ItemStack inSlot = inv.getItem(i);
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameComponents(inSlot, stack)) {
                int space = inSlot.getMaxStackSize() - inSlot.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    inSlot.grow(toAdd);
                    stack.shrink(toAdd);
                    changed = true;
                    if (stack.isEmpty()) return true;
                }
            }
        }

        // 2. 尝试放入空槽位
        for (int i = 0; i < maxSlot; i++) {
            if (inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack.copy());
                stack.setCount(0);
                return true;
            }
        }

        return changed;
    }
}
