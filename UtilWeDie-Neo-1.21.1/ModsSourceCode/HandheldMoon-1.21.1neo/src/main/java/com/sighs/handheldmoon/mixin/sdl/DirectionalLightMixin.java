package com.sighs.handheldmoon.mixin.sdl;

import com.sighs.handheldmoon.lights.HmLightHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

/**
 * Directional flashlight light source for HandheldMoon, implemented by mixing into
 * SodiumDynamicLights' per-block luminance query {@code maxDynamicLightLevel(BlockPos, ...)}.
 *
 * <p>This is the core of the "directional light over SDDL" route: SDDL's public
 * {@link DynamicLightSource} interface only exposes a point + scalar luminance (no direction),
 * but {@code maxDynamicLightLevel} is queried per BlockPos, so injecting at HEAD lets us
 * rewrite the value cell-by-cell to form a cone.
 */
@Mixin(value = SodiumDynamicLights.class, remap = false)
public abstract class DirectionalLightMixin {
    @Inject(method = "maxDynamicLightLevel", at = @At("HEAD"), cancellable = true)
    private static void handheldMoon$directional(BlockPos pos, DynamicLightSource lightSource, double currentLightLevel, CallbackInfoReturnable<Double> cir) {
        HmLightHandler.entityLight(pos, lightSource, currentLightLevel, cir);
    }

    @Inject(method = "getLivingEntityLuminanceFromItems", at = @At("HEAD"), cancellable = true)
    private static void handheldMoon$selfLight(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        HmLightHandler.selfLight(entity, cir);
    }
}
