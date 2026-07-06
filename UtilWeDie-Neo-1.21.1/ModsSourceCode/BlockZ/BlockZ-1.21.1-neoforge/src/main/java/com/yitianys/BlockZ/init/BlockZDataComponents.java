package com.yitianys.BlockZ.init;

import com.mojang.serialization.Codec;
import com.yitianys.BlockZ.BlockZ;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * BlockZ 自定义 DataComponent 注册（1.21.1 NeoForge）。
 *
 * 1.20.1 中物品自定义数据走 ItemStack 的 CompoundTag（getOrCreateTag/getTag）。1.21 改为 DataComponent。
 * 本类承载 util 侧（ItemSizeManager 占格旋转标记）所需组件。
 *
 * 注意（与存储代理对齐）：占格内嵌库存组件 BACKPACK_INVENTORY（见 PORTING_CONVENTIONS 5c）
 * 也应注册到本类。若存储代理已另建此文件，请将 ROTATED 合并进去，避免重复注册导致冲突。
 */
public class BlockZDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(BlockZ.MODID);

    /**
     * 占格物品的旋转标记（对应上游 ItemStack NBT 的 "blockz_rotated" 布尔键）。
     * 持久化 + 网络同步，因为客户端 GUI 需据此渲染旋转后的占格。
     */
    public static final Supplier<DataComponentType<Boolean>> ROTATED =
            COMPONENTS.registerComponentType("rotated", builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL));

    /**
     * 背包/服装内嵌库存（对应上游 ItemStack NBT 的 "Inventory" 复合标签）。
     * 承载 NestedStorageItemHandler 序列化的 ItemStackHandler NBT。
     * 持久化 + 网络同步，使客户端能读到背包内容（tooltip / 占格预览）。
     */
    public static final Supplier<DataComponentType<CompoundTag>> BACKPACK_INVENTORY =
            COMPONENTS.registerComponentType("backpack_inventory", builder -> builder
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(CompoundTag.CODEC)));
}
