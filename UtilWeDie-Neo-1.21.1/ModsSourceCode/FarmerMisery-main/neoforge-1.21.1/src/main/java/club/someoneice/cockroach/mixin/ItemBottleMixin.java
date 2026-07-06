package club.someoneice.cockroach.mixin;

import club.someoneice.cockroach.ItemInit;
import club.someoneice.cockroach.compat.AlexsMobsCompat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemBottleMixin {
    @Inject(method = "interactLivingEntity", at = @At("HEAD"))
    public void farmer_misery$onInteractLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!pStack.is(Items.GLASS_BOTTLE)) {
            return;
        }

        // Only triggers when Alex's Mobs is installed and the target is its cockroach.
        if (!AlexsMobsCompat.isCockroach(pInteractionTarget)) {
            return;
        }

        pStack.shrink(1);
        var item = ItemInit.ROACH_IN_BOTTLE.get().getDefaultInstance();
        if (!pPlayer.addItem(item)) {
            pPlayer.level().addFreshEntity(new ItemEntity(pPlayer.level(), pPlayer.getX(), pPlayer.getY() + 0.5, pPlayer.getZ(), item));
        }

        pInteractionTarget.kill();
    }
}
