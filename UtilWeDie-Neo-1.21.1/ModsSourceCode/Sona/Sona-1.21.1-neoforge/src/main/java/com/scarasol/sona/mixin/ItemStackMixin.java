package com.scarasol.sona.mixin;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.ChatManager;
import com.scarasol.sona.manager.RotManager;
import com.scarasol.sona.manager.RustManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge 1.21.1 changes:
 * <ul>
 *   <li>Removed Forge {@code CapabilityProvider<ItemStack>} / {@code IForgeItemStack} supers
 *       (capability provider system gone).</li>
 *   <li>{@code isEdible()} removed -> {@code stack.has(DataComponents.FOOD)}.</li>
 *   <li>{@code isSameItemSameTags(ItemStack,ItemStack)} renamed to
 *       {@code isSameItemSameComponents(ItemStack,ItemStack)} (NBT -> DataComponents).</li>
 *   <li>The old raw-NBT "ignore rot when comparing for stacking" logic
 *       ({@code getTag()} / "RotValue"/"RotSaveTime"/"RotMultiplier" inspection) is delegated to
 *       {@link RotManager#matchesIgnoringRot(ItemStack, ItemStack)} since rot now lives in a
 *       DataComponent owned by RotManager.</li>
 * </ul>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract Item getItem();

    @Unique
    private boolean sona$init;

    @Inject(method = "isSameItemSameComponents", cancellable = true, at = @At("RETURN"))
    private static void OnIsSameItemSameComponents(ItemStack itemStack1, ItemStack itemStack2, CallbackInfoReturnable<Boolean> cir){
        if (!CommonConfig.ROT_STACKABLE.get() || cir.getReturnValue()
                || !itemStack1.has(DataComponents.FOOD) || !itemStack2.has(DataComponents.FOOD))
            return;
        if (!itemStack1.is(itemStack2.getItem()))
            return;
        if (RotManager.matchesIgnoringRot(itemStack1, itemStack2))
            cir.setReturnValue(true);
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void onInventoryTick(Level level, Entity entity, int slot, boolean selected, CallbackInfo ci){
        if (!sona$init && ChatManager.isChatLimit()){
            ChatManager.setItemRange((ItemStack) (Object) this);
            sona$init = true;
        }
        if (level.isClientSide())
            return;
        if (CommonConfig.ROT_OPEN.get())
            RotManager.rotTick((ItemStack) (Object) this, entity, slot, level.getBiome(entity.getOnPos()).value().getBaseTemperature() / 2 + 0.6);
        if (CommonConfig.RUST_OPEN.get())
            RustManager.changeRustModel((ItemStack) (Object) this);
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void useOn(UseOnContext useOnContext, CallbackInfoReturnable<InteractionResult> cir){
        Player player = useOnContext.getPlayer();
        if (player == null || player.isSpectator() || player.isCreative())
            return;
        if (CommonConfig.RUST_OPEN.get() && (cir.getReturnValue() == InteractionResult.CONSUME || cir.getReturnValue() == InteractionResult.SUCCESS))
            RustManager.rustItem((ItemStack) (Object) this, player, useOnContext.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Inject(method = "use", at = @At("RETURN"))
    private void use(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir){
        if (player.isSpectator() || player.isCreative())
            return;
        if (CommonConfig.RUST_OPEN.get() && (cir.getReturnValue().getResult() == InteractionResult.CONSUME || cir.getReturnValue().getResult() == InteractionResult.SUCCESS))
            RustManager.rustItem((ItemStack) (Object) this, player, interactionHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Inject(method = "interactLivingEntity", at = @At("RETURN"))
    private void onInteractLivingEntity(Player player, LivingEntity livingEntity, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir){
        if (player.isSpectator() || player.isCreative())
            return;
        if (CommonConfig.RUST_OPEN.get() && (cir.getReturnValue() == InteractionResult.CONSUME || cir.getReturnValue() == InteractionResult.SUCCESS))
            RustManager.rustItem((ItemStack) (Object) this, player, interactionHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Inject(method = "mineBlock", at = @At("RETURN"))
    private void onMineBlock(Level level, BlockState blockState, BlockPos blockPos, Player player, CallbackInfo ci){
        if (player.isSpectator() || player.isCreative())
            return;
        if (CommonConfig.RUST_OPEN.get())
            RustManager.rustItem((ItemStack) (Object) this, player, EquipmentSlot.MAINHAND);
    }

    @Inject(method = "hurtEnemy", at = @At("RETURN"))
    private void onHurtEnemy(LivingEntity livingEntity, Player player, CallbackInfoReturnable<Boolean> cir){
        if (player.isSpectator() || player.isCreative())
            return;
        if (CommonConfig.RUST_OPEN.get())
            RustManager.rustItem((ItemStack) (Object) this, player, player.getMainHandItem().equals((ItemStack) (Object) this) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }

    @Inject(method = "getDestroySpeed", cancellable = true, at = @At("RETURN"))
    private void onGetDestroySpeed(BlockState blockState, CallbackInfoReturnable<Float> cir){
        float speed = cir.getReturnValue();
        if (!CommonConfig.RUST_OPEN.get() || !RustManager.canBeRust((ItemStack) (Object) this))
            return;
        if (RustManager.getRust((ItemStack) (Object) this) >= 75) {
            cir.setReturnValue(Math.max(1, speed * 0.85f));
        }else if (RustManager.getRust((ItemStack) (Object) this) >= 50) {
            cir.setReturnValue(Math.max(1, speed * 0.95f));
        }
    }
}
