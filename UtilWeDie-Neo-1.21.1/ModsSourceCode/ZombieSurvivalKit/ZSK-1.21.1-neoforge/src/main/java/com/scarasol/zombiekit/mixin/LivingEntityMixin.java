package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.init.ZombieKitItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow public abstract ItemStack getMainHandItem();

    @Inject(method = "getAttackAnim", cancellable = true, at = @At("RETURN"))
    private void onGetAttackAnim(float p_21325_, CallbackInfoReturnable<Float> cir){
        if (getMainHandItem().is(ZombieKitItems.FLAMETHROWER.get()))
            cir.setReturnValue(0f);
    }
}
