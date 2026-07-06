package com.utdpatch.doomsday.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.renderer.item.GunSmithTableItemRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {GunSmithTableItemRenderer.class},
   remap = false
)
public abstract class TaczWorkbenchItemRenderGuardMixin {
   private static final int TAG_STRING = 8;
   private static final Logger LOGGER = LogManager.getLogger("UTD TaCZ Workbench Render Guard");
   private static final Set<String> LOGGED_BARE_B = ConcurrentHashMap.newKeySet();
   private static final Set<String> LOGGED_TARGET_RENDER_STATE = ConcurrentHashMap.newKeySet();
   private static final Set<String> LOGGED_FIRST_PERSON_PROXY = ConcurrentHashMap.newKeySet();

   @Inject(
      method = {"renderByItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void utd$skipBareAmbiguousWorkbenchB(
      ItemStack var1, ItemDisplayContext var2, PoseStack var3, MultiBufferSource var4, int var5, int var6, CallbackInfo var7
   ) {
      logTargetRenderState(var1, var2);
      if (renderFirstPersonProxyItem(var1, var2, var3, var4, var5, var6)) {
         var7.cancel();
      } else if (renderTargetWorkbenchWithCutout(var1, var2, var3, var5, var6)) {
         var7.cancel();
      } else if (isBareWorkbenchB(var1)) {
         String var8 = String.valueOf(var2);
         if (LOGGED_BARE_B.add(var8)) {
            LOGGER.warn("Skipped bare tacz:workbench_b render in context {}; BlockId is required", var2);
         }

         var7.cancel();
      }
   }

   private static boolean renderFirstPersonProxyItem(ItemStack var0, ItemDisplayContext var1, PoseStack var2, MultiBufferSource var3, int var4, int var5) {
      if (var1 != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND && var1 != ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
         return false;
      } else {
         ResourceLocation var6 = blockIdFromStack(var0);
         if (!isTargetWorkbenchBlock(var6)) {
            return false;
         } else {
            Item var7 = (Item)BuiltInRegistries.ITEM.get(var6);
            if (var7 == null) {
               logFirstPersonProxy(var0, var6, var1, null, false);
               return false;
            } else {
               ItemStack var8 = new ItemStack(var7);
               var2.pushPose();

               try {
                  Minecraft var9 = Minecraft.getInstance();
                  var9.getItemRenderer().renderStatic(var8, var1, var4, var5, var2, var3, var9.level, 0);
               } finally {
                  var2.popPose();
               }

               ResourceLocation var13 = BuiltInRegistries.ITEM.getKey(var7);
               logFirstPersonProxy(var0, var6, var1, var13, true);
               return true;
            }
         }
      }
   }

   private static boolean renderTargetWorkbenchWithCutout(ItemStack var0, ItemDisplayContext var1, PoseStack var2, int var3, int var4) {
      ResourceLocation var5 = blockIdFromStack(var0);
      if (!isTargetWorkbenchBlock(var5)) {
         return false;
      } else {
         Optional var6 = GunSmithTableRenderer.getIndex(var0);
         if (var6.isEmpty()) {
            return false;
         } else {
            ClientBlockIndex var7 = (ClientBlockIndex)var6.get();
            BedrockModel var8 = var7.getModel();
            ResourceLocation var9 = var7.getTexture();
            if (var8 != null && var9 != null) {
               var2.pushPose();

               try {
                  ItemTransforms var10 = var7.getTransforms();
                  if (var10 != null) {
                     var2.translate(0.5F, 0.5F, 0.5F);
                     ItemTransform var11 = var10.getTransform(var1);
                     if (var11 != null) {
                        var11.apply(false, var2);
                     }

                     var2.translate(-0.5F, -0.5F, -0.5F);
                  }

                  var2.translate(0.5, 1.5, 0.5);
                  var2.mulPose(Axis.ZN.rotationDegrees(180.0F));
                  var8.render(var2, var1, RenderType.entityCutout(var9), var3, var4);
               } finally {
                  var2.popPose();
               }

               logCutoutOverride(var0, var5, var1, var9);
               return true;
            } else {
               return false;
            }
         }
      }
   }

   private static boolean isBareWorkbenchB(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ResourceLocation var1 = BuiltInRegistries.ITEM.getKey(var0.getItem());
         if (var1 != null && "tacz:workbench_b".equals(var1.toString())) {
            CompoundTag var2 = utd$customData(var0);
            return var2 == null || !var2.contains("BlockId", 8);
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static void logTargetRenderState(ItemStack var0, ItemDisplayContext var1) {
      ResourceLocation var2 = blockIdFromStack(var0);
      if (var2 != null) {
         String var3 = var2.toString();
         if (isTargetWorkbenchBlock(var2)) {
            ResourceLocation var4 = BuiltInRegistries.ITEM.getKey(var0.getItem());
            String var5 = var4 + "|" + var3 + "|" + var1;
            if (LOGGED_TARGET_RENDER_STATE.add(var5)) {
               Optional var6 = GunSmithTableRenderer.getIndex(var0);
               if (var6.isEmpty()) {
                  LOGGER.warn("target workbench renderer item={} BlockId={} context={} index=empty", var4, var2, var1);
               } else {
                  ClientBlockIndex var7 = (ClientBlockIndex)var6.get();
                  LOGGER.info(
                     "target workbench renderer item={} BlockId={} context={} model={} texture={} transforms={}",
                     var4,
                     var2,
                     var1,
                     var7.getModel() != null,
                     var7.getTexture(),
                     var7.getTransforms() != null
                  );
               }
            }
         }
      }
   }

   private static ResourceLocation blockIdFromStack(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ResourceLocation var1 = BuiltInRegistries.ITEM.getKey(var0.getItem());
         if (var1 != null && "tacz:workbench_b".equals(var1.toString())) {
            CompoundTag var2 = utd$customData(var0);
            return var2 != null && var2.contains("BlockId", 8) ? ResourceLocation.tryParse(var2.getString("BlockId")) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean isTargetWorkbenchBlock(ResourceLocation var0) {
      if (var0 == null) {
         return false;
      } else {
         String var1 = var0.toString();
         return "hamster:oldworkbench".equals(var1) || "lrtactical:smith_table".equals(var1);
      }
   }

   private static void logCutoutOverride(ItemStack var0, ResourceLocation var1, ItemDisplayContext var2, ResourceLocation var3) {
      ResourceLocation var4 = BuiltInRegistries.ITEM.getKey(var0.getItem());
      String var5 = "cutout|" + var4 + "|" + var1 + "|" + var2;
      if (LOGGED_TARGET_RENDER_STATE.add(var5)) {
         LOGGER.info("cutout override rendered item={} BlockId={} context={} texture={}", var4, var1, var2, var3);
      }
   }

   private static net.minecraft.nbt.CompoundTag utd$customData(net.minecraft.world.item.ItemStack stack) {
      CustomData cd = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return cd == null ? null : cd.copyTag();
   }

   private static void logFirstPersonProxy(ItemStack var0, ResourceLocation var1, ItemDisplayContext var2, ResourceLocation var3, boolean var4) {
      ResourceLocation var5 = BuiltInRegistries.ITEM.getKey(var0.getItem());
      String var6 = "firstPersonProxy|" + var5 + "|" + var1 + "|" + var2 + "|" + var4;
      if (LOGGED_FIRST_PERSON_PROXY.add(var6)) {
         LOGGER.info("first-person proxy rendered={} item={} BlockId={} context={} proxyItem={}", var4, var5, var1, var2, var3);
      }
   }
}
