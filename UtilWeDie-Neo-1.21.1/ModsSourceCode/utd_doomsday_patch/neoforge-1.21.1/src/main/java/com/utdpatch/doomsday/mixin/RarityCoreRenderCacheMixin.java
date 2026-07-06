package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.UtdVariantRarityResolver;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"org.yanbwe.raritycore.cache.RenderCacheManager"},
   remap = false
)
public abstract class RarityCoreRenderCacheMixin {
   @Inject(
      method = {"getCachedRarity(Lnet/minecraft/world/item/Item;)Ljava/lang/Integer;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void utd$preferConfiguredRarityForPlainItems(Item var0, CallbackInfoReturnable<Integer> var1) {
      if (UtdVariantRarityResolver.shouldBypassRarityCore(var0)) {
         Integer var2 = UtdVariantRarityResolver.configuredRarity(var0);
         if (var2 != null) {
            var1.setReturnValue(var2);
         }
      }
   }

   @Inject(
      method = {"getCachedRarity(Lnet/minecraft/world/item/ItemStack;)Ljava/lang/Integer;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void utd$preferNbtRarityForVariantCarriers(ItemStack var0, CallbackInfoReturnable<Integer> var1) {
      if (UtdVariantRarityResolver.shouldBypassRarityCore(var0)) {
         Integer var2 = UtdVariantRarityResolver.configuredRarity(var0);
         if (var2 != null) {
            UtdVariantRarityResolver.logProbe("render-cache-head", var0, var2, true);
            var1.setReturnValue(var2);
         }
      }
   }
}
