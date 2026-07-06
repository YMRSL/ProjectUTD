package com.scarasol.zombiekit.mixin;

import com.mojang.authlib.GameProfile;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.network.DoubleJumpPacket;
import com.scarasol.zombiekit.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends Player {

    @Shadow protected abstract boolean vehicleCanSprint(Entity p_265184_);

    @Shadow protected abstract boolean hasEnoughImpulseToStartSprinting();

    @Shadow protected abstract boolean hasEnoughFoodToStartSprinting();

    @Shadow public Input input;
    @Unique
    private boolean canDoubleJump;
    @Unique
    private boolean hasReleasedJumpKey;

    public LocalPlayerMixin(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_) {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "canStartSprinting", cancellable = true, at = @At("RETURN"))
    private void onSprint(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && ExoArmor.numberOfSuit(this) == 4 && this.isFallFlying()) {
            cir.setReturnValue(!this.isSprinting() && this.hasEnoughImpulseToStartSprinting() && this.hasEnoughFoodToStartSprinting() && !this.isUsingItem() && !this.hasEffect(MobEffects.BLINDNESS) && (!this.isPassenger() || this.vehicleCanSprint(this.getVehicle())));
        }
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void onAiStep(CallbackInfo ci){
        if ((onGround() || onClimbable()) && !isInWater() && !this.isFallFlying() && !this.isPassenger()) {
            hasReleasedJumpKey = false;
            canDoubleJump = true;
        }else if (!this.input.jumping) {
            hasReleasedJumpKey = true;
        }else if (ExoArmor.numberOfSuit(this) == 4 && ExoArmor.getPower(this.getItemBySlot(EquipmentSlot.CHEST)) > 0 && !onGround() && !this.isFallFlying() && canDoubleJump && hasReleasedJumpKey && !isInWater() && !this.getAbilities().flying && !this.isPassenger() && !this.onClimbable()) {
            canDoubleJump = false;
            this.jumpFromGround();
            NetworkHandler.PACKET_HANDLER.sendToServer(new DoubleJumpPacket());
        }
    }
}
