package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.api.NoAttenuationProjectile;
import com.scarasol.zombiekit.entity.projectile.WrenchEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {
    protected AbstractArrowMixin(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setDeltaMovement(DDD)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onTick(CallbackInfo ci, boolean flag, Vec3 vec3, BlockPos blockpos, BlockState blockstate, Vec3 vec32, Vec3 vec33, HitResult hitresult, double d5, double d6, double d1, double d7, double d2, double d3, double d4, float f, float f1, Vec3 vec34) {
        if (this instanceof NoAttenuationProjectile) {
            this.setDeltaMovement(vec3.subtract(0, 0.05F, 0));
        }
    }
}
