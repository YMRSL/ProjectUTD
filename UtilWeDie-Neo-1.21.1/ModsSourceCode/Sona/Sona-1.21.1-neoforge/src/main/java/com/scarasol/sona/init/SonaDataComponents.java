package com.scarasol.sona.init;

import com.mojang.serialization.Codec;
import com.scarasol.sona.SonaMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 1.20.1 把腐烂/锈蚀/聊天等自定义数据存进 ItemStack 的 CompoundTag（getOrCreateTag）。
 * 1.21 改为 DataComponent。本类承载这 5 系统的 ItemStack 自定义数据组件。
 *
 * 命名与上游 NBT 键一一对应，迁移时逐个替换 getOrCreateTag().putX/getX。
 * 全部 persistent + networkSynchronized（tooltip / 渲染需客户端读到）。
 */
public class SonaDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(SonaMod.MODID);

    // RotManager: "RotValue"
    public static final Supplier<DataComponentType<Double>> ROT_VALUE =
            COMPONENTS.registerComponentType("rot_value", builder -> builder
                    .persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    // RotManager: "RotMultiplier"
    public static final Supplier<DataComponentType<Double>> ROT_MULTIPLIER =
            COMPONENTS.registerComponentType("rot_multiplier", builder -> builder
                    .persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    // RotManager: "RotSaveTime"
    public static final Supplier<DataComponentType<Long>> ROT_SAVE_TIME =
            COMPONENTS.registerComponentType("rot_save_time", builder -> builder
                    .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    // RotManager: "Warped"
    public static final Supplier<DataComponentType<Boolean>> WARPED =
            COMPONENTS.registerComponentType("warped", builder -> builder
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // RustManager: "RustValue"
    public static final Supplier<DataComponentType<Double>> RUST_VALUE =
            COMPONENTS.registerComponentType("rust_value", builder -> builder
                    .persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    // RustManager: "Waxed"
    public static final Supplier<DataComponentType<Integer>> WAXED =
            COMPONENTS.registerComponentType("waxed", builder -> builder
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // ChatManager: "MessageRange"
    public static final Supplier<DataComponentType<Integer>> MESSAGE_RANGE =
            COMPONENTS.registerComponentType("message_range", builder -> builder
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
}
