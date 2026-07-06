package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.api.FixedVehicle;
import com.scarasol.zombiekit.item.weapon.Knife;
import com.scarasol.zombiekit.item.weapon.SweepWeapon;
import com.scarasol.zombiekit.item.weapon.parts.BattleParts;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Random;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {

    @Unique
    private boolean battleEffect;

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

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onAttackSweepTarget(Entity entity, CallbackInfo ci, float f, float f1, float f2, boolean flag, boolean flag1, float i, boolean flag2, CriticalHitEvent hitResult, boolean flag3, double d0, float f4, boolean flag4, int j, Vec3 vec3, boolean flag5, float f3, Iterator var19, LivingEntity livingentity) {
        if (getMainHandItem().getItem() instanceof SweepWeapon sweepWeapon) {
            sweepWeapon.sweepEffect(livingentity, this, f);
        }
        if (battleEffect) {
            ItemStack stack = getMainHandItem();
            if (stack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
                Item parts = modifiableWeapon.getBattleParts(stack);
                if (parts instanceof BattleParts battleParts) {
                    battleParts.partsEffect(this, livingentity, f);
                }
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void onAttackTail(Entity entity, CallbackInfo ci) {
        battleEffect = false;
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void onAttackHead(Entity entity, CallbackInfo ci) {
        ItemStack stack = getMainHandItem();
        if (stack.getItem() instanceof ModifiableWeapon modifiableWeapon && !level().isClientSide()) {
            Item parts = modifiableWeapon.getBattleParts(stack);
            if (parts instanceof BattleParts battleParts && battleParts.draw()) {
                battleEffect = true;
            }
        }
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onAttackMainTarget(Entity entity, CallbackInfo ci, float f, float f1, float f2, boolean flag, boolean flag1, float i, boolean flag2, CriticalHitEvent hitResult, boolean flag3, double d0, float f4, boolean flag4, int j, Vec3 vec3) {
        if (battleEffect && entity instanceof LivingEntity livingEntity) {
            ItemStack stack = getMainHandItem();
            if (stack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
                Item parts = modifiableWeapon.getBattleParts(stack);
                if (parts instanceof BattleParts battleParts) {
                    ((BattleParts) parts).partsEffect(this, livingEntity, f);
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
