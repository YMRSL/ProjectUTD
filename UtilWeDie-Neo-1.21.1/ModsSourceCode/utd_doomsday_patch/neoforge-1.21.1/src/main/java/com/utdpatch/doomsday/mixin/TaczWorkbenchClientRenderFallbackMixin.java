package com.utdpatch.doomsday.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {GunSmithTableRenderer.class},
   remap = false
)
public abstract class TaczWorkbenchClientRenderFallbackMixin {
   private static final int TAG_STRING = 8;
   private static final Logger LOGGER = LogManager.getLogger("UTD TaCZ Workbench Render");
   private static final Set<String> LOGGED_RENDER_ROUTES = ConcurrentHashMap.newKeySet();
   private static final ResourceLocation WORKBENCH_A = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_a");
   private static final ResourceLocation WORKBENCH_C = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_c");
   private static final ResourceLocation AMMO_WORKBENCH = ResourceLocation.fromNamespaceAndPath("tacz", "ammo_workbench");
   private static final ResourceLocation ATTACHMENT_WORKBENCH = ResourceLocation.fromNamespaceAndPath("tacz", "attachment_workbench");
   private static final ResourceLocation OLD_WORKBENCH = ResourceLocation.fromNamespaceAndPath("hamster", "oldworkbench");
   private static final ResourceLocation SMITH_TABLE = ResourceLocation.fromNamespaceAndPath("lrtactical", "smith_table");
   private static final ResourceLocation UTD_AMMO_WORKBENCH = ResourceLocation.fromNamespaceAndPath("tacz", "utd_ammo_workbench_item");
   private static final ResourceLocation UTD_ATTACHMENT_WORKBENCH = ResourceLocation.fromNamespaceAndPath("tacz", "utd_attachment_workbench_item");
   private static final ResourceLocation UTD_OLD_WORKBENCH = ResourceLocation.fromNamespaceAndPath("tacz", "utd_oldworkbench_item");
   private static final ResourceLocation UTD_SMITH_TABLE = ResourceLocation.fromNamespaceAndPath("tacz", "utd_lrtactical_smith_table_item");
   private static final ResourceLocation UTD_OLD_WORKBENCH_RENDER = ResourceLocation.fromNamespaceAndPath("utd_workbench_pack", "oldworkbench_render");
   private static final ResourceLocation UTD_SMITH_TABLE_RENDER = ResourceLocation.fromNamespaceAndPath("utd_workbench_pack", "lrtactical_smith_table_render");

   @Inject(
      method = {"getIndex(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void utd$renderDefaultWorkbenchForBareCarrier(ItemStack var0, CallbackInfoReturnable<Optional<ClientBlockIndex>> var1) {
      if (var0 != null && !var0.isEmpty()) {
         ResourceLocation var2 = BuiltInRegistries.ITEM.getKey(var0.getItem());
         ResourceLocation var3 = blockIdFromStack(var0);
         if (var3 == null && WORKBENCH_A.equals(var2)) {
            var3 = AMMO_WORKBENCH;
         } else if (var3 == null && WORKBENCH_C.equals(var2)) {
            var3 = ATTACHMENT_WORKBENCH;
         }

         if (var3 != null) {
            ResourceLocation var4 = renderBlockIdFor(var3);
            if (var4 != null) {
               Optional<ClientBlockIndex> var7 = TimelessAPI.getClientBlockIndex(var4);
               logRoute(var2, var3, var4, var7.isPresent(), true);
               var7.ifPresent(var1x -> var1.setReturnValue(Optional.of(var1x)));
            } else {
               Optional<ClientBlockIndex> var5 = var1.getReturnValue();
               if (var5 != null && var5.isPresent()) {
                  logRoute(var2, var3, var3, true, false);
               } else {
                  Optional<ClientBlockIndex> var6 = TimelessAPI.getClientBlockIndex(var3);
                  logRoute(var2, var3, var3, var6.isPresent(), false);
                  var6.ifPresent(var1x -> var1.setReturnValue(Optional.of(var1x)));
               }
            }
         }
      }
   }

   private static ResourceLocation blockIdFromStack(ItemStack var0) {
      CompoundTag var1 = utd$customData(var0);
      return var1 != null && var1.contains("BlockId", 8) ? canonicalBlockId(ResourceLocation.tryParse(var1.getString("BlockId"))) : null;
   }

   private static net.minecraft.nbt.CompoundTag utd$customData(net.minecraft.world.item.ItemStack stack) {
      CustomData cd = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return cd == null ? null : cd.copyTag();
   }

   private static ResourceLocation canonicalBlockId(ResourceLocation var0) {
      if (UTD_AMMO_WORKBENCH.equals(var0)) {
         return AMMO_WORKBENCH;
      } else if (UTD_ATTACHMENT_WORKBENCH.equals(var0)) {
         return ATTACHMENT_WORKBENCH;
      } else if (UTD_OLD_WORKBENCH.equals(var0)) {
         return OLD_WORKBENCH;
      } else if (UTD_SMITH_TABLE.equals(var0)) {
         return SMITH_TABLE;
      } else if (UTD_OLD_WORKBENCH_RENDER.equals(var0)) {
         return OLD_WORKBENCH;
      } else {
         return UTD_SMITH_TABLE_RENDER.equals(var0) ? SMITH_TABLE : var0;
      }
   }

   private static ResourceLocation renderBlockIdFor(ResourceLocation var0) {
      return null;
   }

   private static void logRoute(ResourceLocation var0, ResourceLocation var1, ResourceLocation var2, boolean var3, boolean var4) {
      String var5 = var0 + "|" + var1 + "|" + var2 + "|" + var3 + "|" + var4;
      if (LOGGED_RENDER_ROUTES.add(var5)) {
         LOGGER.info("workbench item={} BlockId={} renderIndex={} loaded={} forced={}", var0, var1, var2, var3, var4);
      }
   }
}
