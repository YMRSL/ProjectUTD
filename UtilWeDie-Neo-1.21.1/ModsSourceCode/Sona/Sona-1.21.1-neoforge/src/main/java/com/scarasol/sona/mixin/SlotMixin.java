package com.scarasol.sona.mixin;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.RotManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(Slot.class)
public abstract class SlotMixin {

    // 1.21.1 safeInsert(grow opcode80)在场：itemstack(slot3,在场ItemStack第2个=ord1)是合并目标，i(slot4,在场int第2个=ord1)是合并量。
    @Inject(method = "safeInsert(Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
    private void onSafeInsert(ItemStack itemStack, int p_150658_, CallbackInfoReturnable<ItemStack> cir, @Local(ordinal = 1) ItemStack itemStack2, @Local(ordinal = 1) int i){
        if (CommonConfig.ROT_OPEN.get() && itemStack2.has(DataComponents.FOOD) && RotManager.canBeRotten(itemStack2))
            RotManager.rotWhenStack(itemStack2, RotManager.getRot(itemStack2), RotManager.getRot(itemStack), itemStack2.getCount(), i, RotManager.getRotSaveTime(itemStack));
    }

}
