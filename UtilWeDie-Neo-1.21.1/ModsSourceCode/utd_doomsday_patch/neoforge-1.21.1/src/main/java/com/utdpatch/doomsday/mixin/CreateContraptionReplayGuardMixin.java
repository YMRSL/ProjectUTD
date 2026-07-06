package com.utdpatch.doomsday.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create's ControlledContraptionEntity.writeAdditional dereferences
 * {@code controllerPos} unconditionally. On Flashback's replay server,
 * contraption entities are reconstructed from the recorded vanilla add-entity
 * packet only (Flashback is a Fabric mod and doesn't feed NeoForge's custom
 * spawn-data payload back into the entity), so {@code controllerPos} stays
 * null — and the NPE during viewer pairing kills the whole replay server.
 * Substitute a zero offset when the controller is unknown; playback motion
 * comes from recorded position packets anyway.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.ControlledContraptionEntity")
public class CreateContraptionReplayGuardMixin {
    @WrapOperation(
            method = "writeAdditional(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;subtract(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/core/BlockPos;"),
            remap = false)
    private BlockPos utd$nullSafeControllerPos(BlockPos controllerPos, Vec3i ownPos, Operation<BlockPos> original) {
        return controllerPos == null ? BlockPos.ZERO : original.call(controllerPos, ownPos);
    }
}
