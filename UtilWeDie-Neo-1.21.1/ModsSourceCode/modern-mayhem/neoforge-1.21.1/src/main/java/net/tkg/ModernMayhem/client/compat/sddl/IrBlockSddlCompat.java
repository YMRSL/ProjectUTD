package net.tkg.ModernMayhem.client.compat.sddl;

import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import net.minecraft.world.entity.EntityType;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.client.config.ClientConfig;
import net.tkg.ModernMayhem.client.light.ir.IrLightCache;
import net.tkg.ModernMayhem.server.registry.BlockEntityRegistryMM;

/**
 * 给 SodiumDynamicLights 注册 IR 红外【全向】动态光 handler (viewer-gated: 只有戴夜视才看得见)。
 *
 * 用于 IR 测试/演示载体 —— 末影螨 (/summon minecraft:endermite): 走 SDDL 可靠的实体光路,
 * 验证"客观光 + 只有夜视可见"。这跟护目镜玩家 IR 锥光是两回事 (锥光走 mixin), 末影螨是全向的。
 * 方块实体光路在本包(NeoForge+Connector)环境下未生效, 保留但不发光。
 *
 * 唯一直接接触 SDDL/LambDynLights API 的类; 仅在 SDDL 存在时由 ModernMayhemMod 守卫后调用。
 */
public final class IrBlockSddlCompat {
    private IrBlockSddlCompat() {
    }

    private static int irLuminance() {
        return (int) Math.round(ClientConfig.IR_BLOCK_LUMINANCE.get());
    }

    public static void register() {
        // 方块实体光路 (本包环境下未生效, 保留)
        DynamicLightHandlers.registerDynamicLightHandler(
                BlockEntityRegistryMM.IR_LIGHT.get(),
                be -> IrLightCache.viewerCanSeeIr() ? irLuminance() : 0);

        // 实体光路 (可靠) —— 末影螨: IR 测试载体, /summon minecraft:endermite (需 SDDL entities=true)
        DynamicLightHandlers.registerDynamicLightHandler(
                EntityType.ENDERMITE,
                e -> IrLightCache.viewerCanSeeIr() ? irLuminance() : 0);

        ModernMayhemMod.LOGGER.info("[mm] IR dynamic-light handlers registered (block + endermite test entity)");
    }
}
