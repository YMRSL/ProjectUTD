package com.scarasol.sona.mixin;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.RotManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(Inventory.class)
public abstract class InventoryMixin implements Container, Nameable {

    // 1.21.1 addResource 重构后局部不同：itemstack(slot4,在场ItemStack第2个=ord1)是合并目标，k(slot6,在场int第4个=ord3)是实际合并量。
    // 改用 MixinExtras @Local（按注入点在场顺序匹配），不再用会因 LVT 变动失配的 LocalCapture。
    @Inject(method = "addResource(ILnet/minecraft/world/item/ItemStack;)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;grow(I)V"))
    private void onAddResource(int slotNumber, ItemStack itemStack, CallbackInfoReturnable<Integer> cir, @Local(ordinal = 1) ItemStack itemStack2, @Local(ordinal = 3) int j){
        if (CommonConfig.ROT_OPEN.get() && itemStack2.has(DataComponents.FOOD) && RotManager.canBeRotten(itemStack)) {
            RotManager.rotWhenStack(itemStack2, RotManager.getRot(itemStack2), RotManager.getRot(itemStack), itemStack2.getCount(), j, RotManager.getRotSaveTime(itemStack));
        }
    }

}
