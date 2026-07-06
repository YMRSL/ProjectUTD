package net.tkg.ModernMayhem.server.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 1.20.1 的 ItemStack 自定义 NBT tag (getOrCreateTag/getTag/setTag) 在 1.21.1 由
 * DataComponents.CUSTOM_DATA 承载。注意: CUSTOM_DATA 返回的是 tag 的副本, 任何修改必须
 * 通过 setTag 回写才能持久化 —— 这与 1.20.1 的 getOrCreateTag 返回活引用语义不同。
 */
public final class ItemNBTUtil {
    private ItemNBTUtil() {
    }

    /** 返回 stack 自定义数据 tag 的可变副本 (无则为空 tag)。修改后须 setTag 回写。 */
    public static CompoundTag getOrCreateTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** 返回 stack 自定义数据 tag 的副本; stack 无自定义数据时返回 null (对应 1.20.1 的 getTag)。 */
    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    /** 将 tag 写回 stack 的 CUSTOM_DATA 组件以持久化。 */
    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
