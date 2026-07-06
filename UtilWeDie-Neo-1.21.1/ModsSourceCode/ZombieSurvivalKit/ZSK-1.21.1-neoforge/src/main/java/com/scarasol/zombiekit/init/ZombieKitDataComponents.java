package com.scarasol.zombiekit.init;

import com.mojang.serialization.Codec;
import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 1.20.1 把这些自定义数据存进 ItemStack 的 CompoundTag（getOrCreateTag().putX/getX）。
 * 1.21 ItemStack 不再有 getOrCreateTag/getTag，改为 DataComponent。
 *
 * 命名与上游 NBT 键一一对应（key 注释见每个字段），迁移时逐个替换。
 * 全部 persistent + networkSynchronized（tooltip / 渲染 / 客户端动画需读到）。
 */
public class ZombieKitDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(ZombieKitMod.MODID);

    // ===== ExoArmor =====
    // "Power"
    public static final Supplier<DataComponentType<Integer>> POWER =
            COMPONENTS.registerComponentType("power", builder -> builder
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    // "FlyMode"
    public static final Supplier<DataComponentType<Boolean>> FLY_MODE =
            COMPONENTS.registerComponentType("fly_mode", builder -> builder
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    // "Mode"
    public static final Supplier<DataComponentType<Integer>> MODE =
            COMPONENTS.registerComponentType("mode", builder -> builder
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    // "Radar"
    public static final Supplier<DataComponentType<Integer>> RADAR =
            COMPONENTS.registerComponentType("radar", builder -> builder
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    // "ReactiveArmor"
    public static final Supplier<DataComponentType<Integer>> REACTIVE_ARMOR =
            COMPONENTS.registerComponentType("reactive_armor", builder -> builder
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // ===== Chainsaw =====
    // "Power"（布尔，与 ExoArmor 的 int Power 不同键空间，单独组件）
    public static final Supplier<DataComponentType<Boolean>> CHAINSAW_POWER =
            COMPONENTS.registerComponentType("chainsaw_power", builder -> builder
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    // 上游用 "CustomModelData" 控制锯子开/关贴图。1.21 有原版 DataComponents.CUSTOM_MODEL_DATA，
    // 直接复用原版组件即可（见 Chainsaw 实现），此处不再单独注册。

    // ===== Flamethrower =====
    // "StartUsingTime"
    public static final Supplier<DataComponentType<Long>> START_USING_TIME =
            COMPONENTS.registerComponentType("start_using_time", builder -> builder
                    .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    // "Using"
    public static final Supplier<DataComponentType<Boolean>> USING =
            COMPONENTS.registerComponentType("using", builder -> builder
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    // "CurrentTexture"
    public static final Supplier<DataComponentType<String>> CURRENT_TEXTURE =
            COMPONENTS.registerComponentType("current_texture", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    // "ReloadTime"
    public static final Supplier<DataComponentType<Long>> RELOAD_TIME =
            COMPONENTS.registerComponentType("reload_time", builder -> builder
                    .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    // "Canister"（存燃料罐物品 id 字符串）
    public static final Supplier<DataComponentType<String>> CANISTER =
            COMPONENTS.registerComponentType("canister", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    // "Pressure"
    public static final Supplier<DataComponentType<Double>> PRESSURE =
            COMPONENTS.registerComponentType("pressure", builder -> builder
                    .persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    // ===== ChargingParts =====
    // "ReleaseTime"
    public static final Supplier<DataComponentType<Long>> RELEASE_TIME =
            COMPONENTS.registerComponentType("release_time", builder -> builder
                    .persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    // ===== ModifiableWeapon（武器改装件 id 字符串） =====
    // "GripParts"
    public static final Supplier<DataComponentType<String>> GRIP_PARTS =
            COMPONENTS.registerComponentType("grip_parts", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    // "ChargingParts"
    public static final Supplier<DataComponentType<String>> CHARGING_PARTS =
            COMPONENTS.registerComponentType("charging_parts", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    // "BattleParts"
    public static final Supplier<DataComponentType<String>> BATTLE_PARTS =
            COMPONENTS.registerComponentType("battle_parts", builder -> builder
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
}
