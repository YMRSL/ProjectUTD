package com.ymrsl.vehicleload.mixin;

import com.ymrsl.vehicleload.VehicleLoadConfig;
import com.ymrsl.vehicleload.VehicleLoadMod;
import com.ymrsl.vehicleload.compat.SableStructureCompat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes the turret/barrel model offset for SuperbWarfare vehicles seated on
 * sable structures.
 *
 * SW's turret model rotation is computed RELATIVE to the vehicle body yaw
 * (VehicleEntity.getTurretYaw = -(turretYRot - yRot)). Sable rotates the
 * RENDERING of entities aboard a structure by the structure's yaw, but the
 * entity's yRot is untouched — so the model turret ends up offset by exactly
 * the structure's yaw while the actual aim/ballistics (world-absolute
 * turretYRot on the server) stay correct. We add the structure yaw back into
 * the model-side turret yaw when the vehicle sits on a Create seat inside a
 * sable plot.
 *
 * Sign/behaviour tunable via config sableTurretYawFix (1 = add, -1 = subtract,
 * 0 = off) so the convention can be calibrated in one test iteration.
 */
@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.client.model.entity.VehicleModel", remap = false)
public abstract class SableSeatTurretModelFixMixin {
    /** Drives the main "turret"/"turretLaser" bones (bone.rotY = turretYRot). */
    @Shadow(remap = false)
    private float turretYRot;
    /** Used by secondary yaw bones in some vehicle model subclasses. */
    @Shadow(remap = false)
    private float turretYaw;

    @Inject(
            method = "setCustomAnimations(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;JLsoftware/bernie/geckolib/animation/AnimationState;)V",
            at = @At("TAIL"),
            remap = false,
            require = 0
    )
    private void vehicleload$fixSableSeatTurretYaw(com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity vehicle,
                                                   long instanceId,
                                                   software.bernie.geckolib.animation.AnimationState<?> animationState,
                                                   CallbackInfo ci) {
        double sign = VehicleLoadConfig.SABLE_TURRET_YAW_FIX.get();
        if (sign == 0.0D) {
            return;
        }
        Entity mount = vehicle.getVehicle();
        if (mount == null || !SableStructureCompat.isSeatEntity(mount)) {
            return;
        }
        Float structYaw = SableStructureCompat.structureYawAt(vehicle.level(), mount.blockPosition());
        if (structYaw == null) {
            return;
        }
        float delta = (float) (sign * structYaw);
        float before = this.turretYRot;
        this.turretYRot = Mth.wrapDegrees(this.turretYRot + delta);
        this.turretYaw = Mth.wrapDegrees(this.turretYaw + delta);
        if (VehicleLoadConfig.DEBUG_LOG.get()) {
            vehicleload$protractor(vehicle, structYaw, before);
        }
    }

    /**
     * Angle instrumentation (1 line/second while debugLog=true): every term of
     * the turret orientation equation, so sign/residual can be solved from one
     * log sample instead of eyeballing repeated test builds.
     */
    private void vehicleload$protractor(com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity vehicle,
                                        float structYaw, float modelTurretBefore) {
        if (vehicle.level().getGameTime() % 20L != 0L) {
            return;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        float playerYRot = mc.player != null ? mc.player.getYRot() : Float.NaN;
        float camYaw = mc.gameRenderer.getMainCamera().getYRot();
        VehicleLoadMod.LOGGER.info(
                "[protractor] structYaw={} vehYRot={} entTurretYRot={} modelTurretYRot(before)={} (after)={} playerYRot={} camYaw={}",
                fmt(structYaw), fmt(vehicle.getYRot()),
                fmt(vehicle.getTurretYRot()),
                fmt(modelTurretBefore), fmt(this.turretYRot),
                fmt(playerYRot), fmt(camYaw));
    }

    private static String fmt(float v) {
        return String.format("%.1f", Mth.wrapDegrees(v));
    }
}
