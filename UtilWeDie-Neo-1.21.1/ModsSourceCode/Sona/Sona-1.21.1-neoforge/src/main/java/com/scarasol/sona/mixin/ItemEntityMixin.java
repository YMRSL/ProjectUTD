package com.scarasol.sona.mixin;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.RotManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "merge(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)Lnet/minecraft/world/item/ItemStack;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void onMerge(ItemStack itemStack1, ItemStack itemStack2, int count, CallbackInfoReturnable<ItemStack> cir, @Local(ordinal = 1) int i, @Local(ordinal = 2) ItemStack itemStack){
        if (CommonConfig.ROT_OPEN.get() && itemStack.has(DataComponents.FOOD) && RotManager.canBeRotten(itemStack))
            RotManager.rotWhenStack(itemStack, RotManager.getRot(itemStack), RotManager.getRot(itemStack2), itemStack.getCount(), i, RotManager.getRotSaveTime(itemStack2));
    }
}
