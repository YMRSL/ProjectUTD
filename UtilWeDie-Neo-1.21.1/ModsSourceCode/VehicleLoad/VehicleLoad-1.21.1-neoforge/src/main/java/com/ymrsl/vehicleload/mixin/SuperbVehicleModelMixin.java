package com.ymrsl.vehicleload.mixin;

import com.ymrsl.vehicleload.compat.SuperbTurretCompat;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.client.model.entity.VehicleModel", remap = false)
public abstract class SuperbVehicleModelMixin {
    @Shadow private float turretYRot;
    @Shadow private float turretXRot;
    @Shadow private float turretYaw;
    @Shadow private float yaw;
    @Shadow private float pitch;

    @Inject(
        method = "setCustomAnimations(Lcom/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity;JLsoftware/bernie/geckolib/animation/AnimationState;)V",
        at = @At(
            value = "FIELD",
            target = "Lcom/atsuishio/superbwarfare/client/model/entity/VehicleModel;TRANSFORMS:Ljava/util/List;",
            opcode = Opcodes.GETFIELD,
            ordinal = 1,
            shift = At.Shift.BEFORE
        )
    )
    private void vehicleload$alignTurretToShootVec(Object animatable, long instanceId, Object animationState, CallbackInfo ci) {
        if (!(animatable instanceof Entity vehicle)) {
            return;
        }
        Entity mount = vehicle.getVehicle();
        if (mount == null || !mount.getTags().contains("vehicleloadSeat")) {
            return;
        }
        if (!SuperbTurretCompat.isVehicle(vehicle) || !SuperbTurretCompat.hasTurret(vehicle)) {
            return;
        }
        Entity controller = SuperbTurretCompat.getTurretController(vehicle);
        if (controller == null) {
            return;
        }
        float partialTicks = getPartialTick(animationState);
        Vec3 shootVec = SuperbTurretCompat.getShootVec(vehicle, controller, partialTicks);
        if (shootVec == null) {
            return;
        }
        float worldYaw = yawFromVector(shootVec);
        float worldPitch = pitchFromVector(shootVec);
        float turretYawLocal = Mth.wrapDegrees(worldYaw - this.yaw);
        float turretPitchLocal = Mth.wrapDegrees(worldPitch - this.pitch);
        this.turretYRot = turretYawLocal;
        this.turretYaw = turretYawLocal;
        this.turretXRot = turretPitchLocal;
    }

    private float getPartialTick(Object animationState) {
        if (animationState == null) {
            return 1.0f;
        }
        try {
            Object value = animationState.getClass().getMethod("getPartialTick").invoke(animationState);
            if (value instanceof Number number) {
                return number.floatValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback below.
        }
        return 1.0f;
    }

    private float yawFromVector(Vec3 vec) {
        return (float) (Mth.atan2(vec.z, vec.x) * (180.0D / Math.PI)) - 90.0F;
    }

    private float pitchFromVector(Vec3 vec) {
        double horizontal = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
        return (float) (-(Mth.atan2(vec.y, horizontal) * (180.0D / Math.PI)));
    }
}
