package com.scarasol.zombiekit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.scarasol.zombiekit.api.FixedVehicle;
import com.scarasol.zombiekit.item.weapon.Knife;
import com.scarasol.zombiekit.item.weapon.SweepWeapon;
import com.scarasol.zombiekit.item.weapon.parts.BattleParts;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    @Unique
    private boolean zombiekit$battleEffect;

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Unique
    @Override
    public void setXRot(float xRot) {
        if (getVehicle() instanceof FixedVehicle fixedVehicle && !fixedVehicle.validXRot(this, xRot))
            return;
        super.setXRot(xRot);
    }

    @Unique
    @Override
    public void setYRot(float yRot) {
        if (getVehicle() instanceof FixedVehicle fixedVehicle && !fixedVehicle.validYRot(this, yRot))
            return;
        super.setYRot(yRot);
    }

    @Unique
    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean startRiding(Entity vehicle) {
        if (vehicle instanceof FixedVehicle fixedVehicle && level().isClientSide)
            Minecraft.getInstance().options.setCameraType(fixedVehicle.getVehicleCameraType());
        return super.startRiding(vehicle);
    }

    // Sweep weapon effect: runs for every sweep-hit target. In 1.21.1 the sweep loop target is the LivingEntity local
    // (3rd LivingEntity in scope: livingentity / livingentity1 / livingentity2) hurt via LivingEntity#hurt; f is the
    // base damage (1st float local).
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", shift = At.Shift.AFTER))
    private void onAttackSweepTarget(Entity entity, CallbackInfo ci, @Local(ordinal = 0) LivingEntity sweepTarget, @Local(ordinal = 0) float f) {
        if (getMainHandItem().getItem() instanceof SweepWeapon sweepWeapon) {
            sweepWeapon.sweepEffect(sweepTarget, this, f);
        }
        if (zombiekit$battleEffect) {
            ItemStack stack = getMainHandItem();
            if (stack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
                Item parts = modifiableWeapon.getBattleParts(stack);
                if (parts instanceof BattleParts battleParts) {
                    battleParts.partsEffect(this, sweepTarget, f);
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackTail(Entity entity, CallbackInfo ci) {
        zombiekit$battleEffect = false;
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttackHead(Entity entity, CallbackInfo ci) {
        ItemStack stack = getMainHandItem();
        if (stack.getItem() instanceof ModifiableWeapon modifiableWeapon && !level().isClientSide()) {
            Item parts = modifiableWeapon.getBattleParts(stack);
            if (parts instanceof BattleParts battleParts && battleParts.draw()) {
                zombiekit$battleEffect = true;
            }
        }
    }

    // Main-target battle-parts effect: after the primary target.hurt (Entity#hurt). f is the base damage (1st float).
    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", shift = At.Shift.AFTER))
    private void onAttackMainTarget(Entity entity, CallbackInfo ci, @Local(ordinal = 0) float f) {
        if (zombiekit$battleEffect && entity instanceof LivingEntity livingEntity) {
            ItemStack stack = getMainHandItem();
            if (stack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
                Item parts = modifiableWeapon.getBattleParts(stack);
                if (parts instanceof BattleParts battleParts) {
                    battleParts.partsEffect(this, livingEntity, f);
                }
            }
        }
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void onAttackMainTargetWithKnife(Entity entity, CallbackInfo ci) {
        ItemStack stack = getMainHandItem();
        if (stack.getItem() instanceof Knife) {
            entity.getPersistentData().putBoolean("CancelKnockback", true);
        }
    }

}
