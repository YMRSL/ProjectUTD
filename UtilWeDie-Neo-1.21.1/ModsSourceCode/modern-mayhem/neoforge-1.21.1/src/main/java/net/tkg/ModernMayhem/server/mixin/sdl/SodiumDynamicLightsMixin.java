package net.tkg.ModernMayhem.server.mixin.sdl;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.tkg.ModernMayhem.client.light.ir.IrLightCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/**
 * 让登记了 IR 锥光的玩家被 SDDL 当作有效光源、且迭代足够大的半径。
 *
 * SDDL 按 luminance 决定为光源迭代多大范围的方块(并对每格调 maxDynamicLightLevel)。我们把登记 IR 的玩家
 * 物品发光等级强制成 15 → SDDL 迭代约 15 格 → 锥光范围(≤15)内的方块都会被查询。实际每格亮度仍由
 * DirectionalLightMixin 用锥光覆写(锥外为 0), 所以玩家本体/身后不会发出径向自光。
 *
 * 取 max(原值, 15): 不破坏手持火把等原有发光。仅 SDDL 在场时由 MMMixinPlugin 放行加载。
 */
@Mixin(value = SodiumDynamicLights.class, remap = false)
public class SodiumDynamicLightsMixin {
    @Inject(method = "getLivingEntityLuminanceFromItems", at = @At("RETURN"), cancellable = true)
    private static void mm$forceIrLuminance(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof Player) || !(entity instanceof DynamicLightSource src)) {
            return;
        }
        net.tkg.ModernMayhem.client.light.ir.IrLightData data = IrLightCache.getData(src);
        if (data == null) {
            return;
        }
        // 强制 luminance = 锥光范围(取整)。SDDL 用裸 luminance 当"为该源迭代多大半径方块", 故可 >15 → 更远的锥。
        // 注意: SDDL 的 EntityRendererMixin 会拿同一个 luminance 去算"实体渲染光照", >15 会越界使该玩家隐身,
        // 这个副作用由 EntityRenderLightClampMixin 把实体渲染光照夹回 ≤15 来根治(不影响这里的半径)。
        int forced = (int) Math.ceil(data.range);
        int original = cir.getReturnValue() == null ? 0 : cir.getReturnValue();
        if (forced > original) {
            cir.setReturnValue(forced);
        }
    }
}
