package com.scarasol.zombiekit.mixin;

import com.scarasol.zombiekit.entity.mechanics.Mechanics;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.network.CoverFirePacket;
import com.scarasol.zombiekit.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Final public Options options;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void onHandleKeybinds(CallbackInfo ci) {
        if (this.player.isUsingItem() && this.player.getUseItem().is(Items.SPYGLASS) && this.options.keyAttack.consumeClick()) {
            BlockPos pos = MortarEntity.getCoverPos(this.player);
            if (pos != null) {
                this.player.stopUsingItem();
                NetworkHandler.PACKET_HANDLER.sendToServer(new CoverFirePacket(pos));
//                this.player.level().playLocalSound(BlockPos.containing(this.player.position()), ZombieKitSounds.radio_response.get(), SoundSource.PLAYERS, 1, 1, false);
            }
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", cancellable = true, at = @At("RETURN"))
    private void onShouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir){
        if (!cir.getReturnValue() && player != null) {
            if (entity instanceof ArmorStand || entity instanceof Mechanics || player.getUUID().equals(entity.getUUID()))
                return;
            if (ExoArmor.numberOfSuit(player) == 4) {
                if (ExoArmor.getRadar(player.getItemBySlot(EquipmentSlot.CHEST)) == 2) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}