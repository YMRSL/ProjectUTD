package net.tkg.ModernMayhem.server.mixin.sdl;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.tkg.ModernMayhem.client.light.ir.IrLightCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;

/**
 * 修"开 IR 的玩家被异常渲染"(不开光影→隐身; 开光影→过亮)。
 *
 * 根因: 为了把 IR 锥光半径撑大, 我们强制把发 IR 玩家的 SDDL luminance 设成"锥光范围"(可 >15)。
 * 但 SDDL 的 EntityRendererMixin 会在 EntityRenderer.getBlockLightLevel 的 RETURN 处把这个 luminance
 * max 进"渲染该实体用的方块光照"。光照本应 0~15: 原版光照贴图采样越界 → 隐身; Iris 则渲染成超亮。
 * 关键是: IR 是【前方世界光】, 根本不该让玩家【自身】发光。
 *
 * 修法: 以更高优先级在 getBlockLightLevel 的 HEAD 拦截 —— 对正在发 IR 的玩家, 直接返回【原版】方块光照
 * 并 cancel。这样 SDDL 的 RETURN 注入根本不会执行(方法已在 HEAD 返回) → 不再有 self-glow → 玩家按正常
 * 环境光渲染(不隐身、不过亮)。锥光的【世界方块光】走 maxDynamicLightLevel(另一条路), 不受影响 → 大半径保留。
 * 对非 IR 实体不拦截(走 SDDL 原逻辑, 手持火把照常自发光)。
 */
@Mixin(value = EntityRenderer.class, priority = 1500)
public class EntityRenderLightClampMixin {
    @Inject(method = "getBlockLightLevel(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
    private void mm$noIrSelfGlow(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof Player && entity instanceof DynamicLightSource src && IrLightCache.getData(src) != null) {
            int vanilla = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, pos);
            cir.setReturnValue(vanilla);
        }
    }
}
