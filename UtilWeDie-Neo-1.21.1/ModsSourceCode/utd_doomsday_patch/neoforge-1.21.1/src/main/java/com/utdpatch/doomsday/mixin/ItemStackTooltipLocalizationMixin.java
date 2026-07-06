package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.UtdTooltipLocalization;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipLocalizationMixin {
   @Inject(
      method = {"getTooltipLines"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void utd$localizeHardcodedSystemTooltips(Item.TooltipContext var0, Player var1, TooltipFlag var2, CallbackInfoReturnable<List<Component>> var3) {
      ItemStack var4 = (ItemStack)(Object)this;
      List var5 = (List)var3.getReturnValue();
      List var6 = UtdTooltipLocalization.localize(var4, var5);
      if (var6 != var5) {
         var3.setReturnValue(var6);
      }
   }
}
