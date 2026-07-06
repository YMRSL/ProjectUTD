package com.scarasol.zombiekit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.scarasol.zombiekit.api.NoAttenuationProjectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {
    protected AbstractArrowMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    /**
     * Faithful port of the 1.20.1 mixin.
     *
     * 1.20.1 injected after {@code setDeltaMovement(DDD)V} (the inline gravity application in tick) and, for a
     * {@link NoAttenuationProjectile}, re-set the velocity to {@code vec3.subtract(0, 0.05F, 0)} where {@code vec3} is
     * the pre-gravity delta -- effectively replacing accumulated gravity with a fixed slow downward drop (no
     * attenuation).
     *
     * 1.21.1 restructured {@code AbstractArrow#tick}: gravity is now applied via {@code applyGravity()} and the
     * per-tick velocity is finalized by {@code setDeltaMovement(Vec3)} ({@code vec3.scale(f)}). We inject right after
     * that finalizing call and capture the local {@code vec3} (the pre-scale delta) to reproduce the original
     * expression exactly. Target/Local layout to be confirmed at first compile/runtime.
     */
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onTick(CallbackInfo ci, @Local(ordinal = 0) Vec3 vec3) {
        if (this instanceof NoAttenuationProjectile) {
            this.setDeltaMovement(vec3.subtract(0, 0.05F, 0));
        }
    }
}
