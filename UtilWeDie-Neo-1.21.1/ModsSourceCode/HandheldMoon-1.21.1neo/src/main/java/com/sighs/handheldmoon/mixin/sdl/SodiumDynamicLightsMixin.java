package com.sighs.handheldmoon.mixin.sdl;

import com.sighs.handheldmoon.lights.HmLightCache;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

import java.lang.reflect.Field;

/**
 * Forces self-light luminance to 15 for HandheldMoon flashlight / moon sources while SDDL
 * is updating tracking. SDDL stores the per-entity luminance in the package-private field
 * {@code Entity#sodiumdynamiclights$luminance} (added by SDDL's own EntityMixin), so we set it
 * by reflection at the start of {@code updateTracking}.
 */
@Mixin(value = SodiumDynamicLights.class, remap = false)
public class SodiumDynamicLightsMixin {
    private static final Field LUMINANCE_FIELD;

    static {
        try {
            LUMINANCE_FIELD = Entity.class.getDeclaredField("sodiumdynamiclights$luminance");
            LUMINANCE_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("HandheldMoon: SDDL luminance field not found", e);
        }
    }

    @Inject(method = "updateTracking", at = @At("HEAD"))
    private static void handheldMoon$forceSelfLuminance(DynamicLightSource lightSource, CallbackInfo ci) {
        if (HmLightCache.getSelfLightSourceList().contains(lightSource)) {
            try {
                LUMINANCE_FIELD.setInt(lightSource, 15);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
