package com.ymrsl.vehicleload.mixin;

import com.ymrsl.vehicleload.compat.SableStructureCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps seated vehicles in the world frame, model-wise.
 *
 * Sable rotates the RENDERING of entities aboard a structure by the structure
 * orientation, expecting their rotations to be structure-local — a convention
 * it only establishes for players (LocalPlayerMixin converts their rotations
 * on mount). SuperbWarfare vehicles manage their own yRot every tick in the
 * world frame, so the extra render rotation permanently offsets the whole
 * model from the (world-frame) camera, reticle and ballistics.
 *
 * Rather than fighting SW's rotation ownership, exempt target vehicles seated
 * on Create seats from sable's ENTITY render rotation: their model then
 * renders from their true world-frame yRot, matching everything the gunner
 * sees. Turning with the ship is handled by SableSeatLockManager slaving the
 * vehicle's yRot per tick. The CAMERA path is left untouched.
 */
@Pseudo
@Mixin(value = EntitySubLevelRotationHelper.class, remap = false)
public class SableEntityRotationExemptMixin {
    @Inject(method = "getEntityOrientation", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void vehicleload$exemptSeatedVehicles(Entity cameraEntity,
                                                         Function<SubLevel, Pose3dc> poseProvider,
                                                         float partialTicks,
                                                         EntitySubLevelRotationHelper.Type type,
                                                         CallbackInfoReturnable<Quaterniond> cir) {
        if (type != EntitySubLevelRotationHelper.Type.ENTITY || cameraEntity == null) {
            return;
        }
        Entity mount = cameraEntity.getVehicle();
        if (mount != null && SableStructureCompat.isSeatEntity(mount)
                && VehicleCompat.isTargetVehicle(cameraEntity)) {
            cir.setReturnValue(null);
        }
    }
}
