package com.scarasol.sona.mixin;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.RotManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Only the {@code extractHook} rot-on-merge injection is retained (serves the food-rot system).
 * The upstream {@code getItemHandler} injection belonged to the container-LOCK feature, which is
 * out of the 5-system migration scope and is DROPPED.
 *
 * NeoForge 1.21.1: extractHook is implemented as a {@code .map(lambda)} over
 * {@code getSourceItemHandler}; the merge happens inside {@code lambda$extractHook$0} at the
 * {@code Hopper#setItem(I, ItemStack)} calls. Captured locals: handler, i, extractItem, j, destStack.
 * The synthetic lambda name and local order need compile-time verification.
 */
@Mixin(VanillaInventoryCodeHooks.class)
public abstract class VanillaInventoryCodeHooksMixin {

    @Inject(method = "lambda$extractHook$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/Hopper;setItem(ILnet/minecraft/world/item/ItemStack;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, remap = false, require = 0)
    private static void onExtractHook(Hopper dest, Pair itemHandlerResult, CallbackInfoReturnable<Boolean> cir, IItemHandler handler, int i, ItemStack extractItem, int j, ItemStack destStack){
        if (dest instanceof BlockEntity blockEntity && extractItem.has(DataComponents.FOOD) && CommonConfig.ROT_OPEN.get() && RotManager.canBeRotten(extractItem)){
            if (destStack.isEmpty()){
                RotManager.putRotSaveTime(extractItem, blockEntity.getLevel().getGameTime());
            }else {
                RotManager.rotWhenStack(destStack, RotManager.getRot(destStack), RotManager.getRot(extractItem), destStack.getCount(), extractItem.getCount(), blockEntity.getLevel().getGameTime());
            }
        }
    }
}
