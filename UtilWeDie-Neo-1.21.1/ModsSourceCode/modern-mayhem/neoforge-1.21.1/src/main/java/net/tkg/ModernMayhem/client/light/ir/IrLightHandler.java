package net.tkg.ModernMayhem.client.light.ir;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.tkg.ModernMayhem.client.config.ClientConfig;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;

/**
 * 逐方块亮度计算, 由 SDDL 的 maxDynamicLightLevel mixin 调用。
 *
 * 对我们登记的 IR 锥光源, 【总是】覆写返回值为 max(当前, 锥光值) —— 锥外锥光值为 0,
 * 于是该源只贡献前方锥形、彻底抑制 SDDL 给它默认算的径向点光(玩家自身/身后不亮, 合红外拟真)。
 * 非我们登记的源(火把/末影螨等) data==null → 直接放行, 不干预其原有(径向)光。
 */
public final class IrLightHandler {
    private IrLightHandler() {
    }

    public static void entityLight(BlockPos pos, DynamicLightSource lightSource, double currentLightLevel, CallbackInfoReturnable<Double> cir) {
        IrLightData data = IrLightCache.getData(lightSource);
        if (data == null) {
            return;
        }

        Level level = (lightSource instanceof Entity e) ? e.level() : null;
        boolean occlude = ClientConfig.IR_OCCLUSION.get();

        double light;
        if (occlude && level != null) {
            light = IrLightMath.computeLightOccluded(level,
                    data.x, data.y, data.z, data.dir.x, data.dir.y, data.dir.z,
                    data.luminance, pos, data.range, IrLightCache.innerAngle(), IrLightCache.outerAngle());
        } else {
            light = IrLightMath.computeLight(
                    data.x, data.y, data.z, data.dir.x, data.dir.y, data.dir.z,
                    data.luminance, pos, data.range, IrLightCache.innerAngle(), IrLightCache.outerAngle());
        }

        cir.setReturnValue(Math.max(currentLightLevel, light));
    }
}
