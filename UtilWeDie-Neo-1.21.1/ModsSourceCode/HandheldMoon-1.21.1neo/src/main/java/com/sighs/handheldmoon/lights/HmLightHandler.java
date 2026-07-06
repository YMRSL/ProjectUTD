package com.sighs.handheldmoon.lights;

import com.sighs.handheldmoon.registry.Config;
import com.sighs.handheldmoon.util.LineLightMath;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import toni.sodiumdynamiclights.DynamicLightSource;

/**
 * Per-block-position light computation for HandheldMoon sources, called from the SDDL
 * {@code maxDynamicLightLevel} mixin. Reuses {@link LineLightMath} (the cone / point falloff math
 * shared with the rest of the mod) so behavior matches the renderer and the previous LambDynLights
 * implementation.
 */
public final class HmLightHandler {
    private HmLightHandler() {
    }

    public static void entityLight(BlockPos pos, DynamicLightSource lightSource, double currentLightLevel, CallbackInfoReturnable<Double> cir) {
        HmLightCache.RealLightData data = HmLightCache.getRealLightData(lightSource);
        if (data == null) return;

        Level level = (lightSource instanceof Entity e) ? e.level() : null;
        boolean occlude = Config.LIGHT_OCCLUSION.get();

        double light;
        if (data.omniMoon) {
            double sx = Math.floor(data.x) + 0.5;
            double sy = Math.floor(data.y) + 0.5;
            double sz = Math.floor(data.z) + 0.5;
            if (occlude && level != null) {
                light = LineLightMath.computePointLightOccluded(level, sx, sy, sz, data.luminance, pos, data.range);
            } else {
                light = computePoint(sx, sy, sz, data.luminance, pos, data.range);
            }
        } else if (data.directional) {
            if (occlude && level != null) {
                light = LineLightMath.computeLightOccluded(level,
                        data.x, data.y, data.z, data.dir.x, data.dir.y, data.dir.z,
                        data.luminance, pos, data.range, HmLightCache.innerAngle(), HmLightCache.outerAngle());
            } else {
                light = LineLightMath.computeLight(
                        data.x, data.y, data.z, data.dir.x, data.dir.y, data.dir.z,
                        data.luminance, pos, data.range, HmLightCache.innerAngle(), HmLightCache.outerAngle());
            }
        } else {
            return;
        }

        if (light > currentLightLevel) {
            cir.setReturnValue(light);
        }
    }

    private static double computePoint(double sx, double sy, double sz, double luminance, BlockPos query, double range) {
        double dx = query.getX() + 0.5 - sx;
        double dy = query.getY() + 0.5 - sy;
        double dz = query.getZ() + 0.5 - sz;
        double distSq = dx * dx + dy * dy + dz * dz;
        double rangeSq = range * range;
        if (distSq > rangeSq) return 0.0;
        double dist = Math.sqrt(distSq);
        double t3 = (distSq * dist) / (range * range * range);
        return luminance * (1.0 - t3);
    }

    public static void selfLight(LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (HmLightCache.getSelfLightSourceList().contains((DynamicLightSource) entity)) {
            cir.setReturnValue(15);
        }
    }
}
