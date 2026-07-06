package net.tkg.ModernMayhem.server.mixin.sdl;

import net.minecraft.core.BlockPos;
import net.tkg.ModernMayhem.client.light.ir.IrLightHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/**
 * 接管 SDDL 的逐方块动态光查询。对本 mod 登记的 IR 锥光源, 把该格亮度改写成"相对玩家视线锥的衰减值",
 * 实现夜视仪前方红外照明锥。其余光源(火把/末影螨等)不干预。仅 SDDL 在场时由 MMMixinPlugin 放行加载。
 */
@Mixin(value = SodiumDynamicLights.class, remap = false)
public abstract class DirectionalLightMixin {
    @Inject(method = "maxDynamicLightLevel", at = @At("HEAD"), cancellable = true)
    private static void mm$irDirectional(BlockPos pos, DynamicLightSource lightSource, double currentLightLevel, CallbackInfoReturnable<Double> cir) {
        IrLightHandler.entityLight(pos, lightSource, currentLightLevel, cir);
    }
}
