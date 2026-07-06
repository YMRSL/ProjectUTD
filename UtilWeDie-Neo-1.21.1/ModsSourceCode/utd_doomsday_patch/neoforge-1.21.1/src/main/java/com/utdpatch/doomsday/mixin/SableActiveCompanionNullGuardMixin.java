package com.utdpatch.doomsday.mixin;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sable can be asked for a tracking sublevel while Minecraft is between world
 * join/render states and no camera entity is available yet. Its helper methods
 * dereference the entity directly, so treat a null entity as "not in a sublevel".
 */
@Pseudo
@Mixin(value = ActiveSableCompanion.class, remap = false)
public class SableActiveCompanionNullGuardMixin {
    @Inject(method = "getTrackingSubLevel(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utd$guardNullTrackingEntity(Entity entity, CallbackInfoReturnable<SubLevel> cir) {
        if (entity == null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getLastTrackingSubLevel(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utd$guardNullLastTrackingEntity(Entity entity, CallbackInfoReturnable<SubLevel> cir) {
        if (entity == null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getTrackingOrVehicleSubLevel(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utd$guardNullTrackingOrVehicleEntity(Entity entity, CallbackInfoReturnable<SubLevel> cir) {
        if (entity == null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getVehicleSubLevel(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void utd$guardNullVehicleEntity(Entity entity, CallbackInfoReturnable<SubLevel> cir) {
        if (entity == null) {
            cir.setReturnValue(null);
        }
    }
}
