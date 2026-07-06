package com.scarasol.sona.mixin.tacz;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.SoundManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Gun-fire sound attraction ("循声"/sound) for TaCZ.
 *
 * HIGH-RISK / needs compile-time verification against tacz 1.1.8:
 * <ul>
 *   <li>The injection target {@code lambda$shootOnce$2} and the 10-arg {@code LocalCapture}
 *       (soundDistance / useSilenceSound etc.) are synthetic-lambda dependent; TaCZ refactored its
 *       shooting pipeline between 1.0 and 1.1.8 so the lambda ordinal/locals may have moved.</li>
 *   <li>GunId is no longer an NBT tag ({@code stack.getTag().getString("GunId")} is gone in 1.21);
 *       read via {@link IGun#getGunId(ItemStack)} (the component-backed accessor).</li>
 * </ul>
 */
@Mixin(ModernKineticGunScriptAPI.class)
public abstract class ModernKineticGunScriptAPIMixin {

    @Shadow private LivingEntity shooter;

    @Shadow private ItemStack itemStack;

    @Inject(method = "lambda$shootOnce$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/network/NetworkHandler;sendToTrackingEntity(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;Lnet/minecraft/world/entity/Entity;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, remap = false, require = 0)
    private void onShoot(boolean consumeAmmo, GunData gunData, int bulletAmount, BulletData bulletData, IGunOperator gunOperator, float shotDamageMultiplier, float processedSpeed, float inaccuracy, int soundDistance, boolean useSilenceSound, CallbackInfoReturnable<Boolean> cir, boolean fire) {
        if (this.itemStack.getItem() instanceof IGun iGun) {
            ResourceLocation gunId = iGun.getGunId(this.itemStack);
            if (gunId != null && CommonConfig.GUN_SOUND_WHITELIST.get().contains(gunId.toString())) {
                return;
            }
        }
        if (CommonConfig.SOUND_OPEN.get() && CommonConfig.GUN_SOUND_ATTRACT.get() && soundDistance > 0){
            int range = useSilenceSound ? CommonConfig.SILENCE_EXPOSURE.get() : CommonConfig.FIRE_EXPOSURE.get();
            if (range == 0) {
                return;
            }
            if (!shooter.level().isClientSide()) {
                SoundManager.addSoundEffect(shooter, 40, range - 1);
            }
        }
    }
}
