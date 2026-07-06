package de.bene2212.holdmyitems.mixin;

import de.bene2212.holdmyitems.config.HoldMyItemsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@OnlyIn(value = Dist.CLIENT)
@Mixin(value = {LivingEntity.class})
public class LivingEntityMixin {
    @ModifyConstant(method = {"getCurrentSwingDuration()I"}, constant = {@Constant(intValue = 6)})
    private int modifySwingDuration(int original) {
        return Minecraft.getInstance().player == (Object) this ? HoldMyItemsClientConfig.SWING_SPEED.get() : original;
    }
}
