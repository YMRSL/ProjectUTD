package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.client.shaders.ThermalShader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract Level level();

    @Inject(method = "isInvisible", at = @At("HEAD"), cancellable = true)
    private void zombiekit$thermalVisible(CallbackInfoReturnable<Boolean> cir) {
        if (level().isClientSide() && ThermalShader.isActive()) {
            cir.setReturnValue(false);
        }
    }
}