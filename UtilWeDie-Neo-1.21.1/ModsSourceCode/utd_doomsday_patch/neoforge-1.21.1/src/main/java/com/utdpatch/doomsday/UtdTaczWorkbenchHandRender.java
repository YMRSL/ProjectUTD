package com.utdpatch.doomsday;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
   modid = "utd_doomsday_patch",
   value = {Dist.CLIENT},
   bus = EventBusSubscriber.Bus.GAME
)
public final class UtdTaczWorkbenchHandRender {
   private static final int TAG_STRING = 8;
   private static final Logger LOGGER = LogManager.getLogger("UTD TaCZ Workbench Hand Event");
   private static final ResourceLocation TACZ_WORKBENCH_B = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_b");
   private static final ResourceLocation OLD_WORKBENCH = ResourceLocation.fromNamespaceAndPath("hamster", "oldworkbench");
   private static final ResourceLocation SMITH_TABLE = ResourceLocation.fromNamespaceAndPath("lrtactical", "smith_table");
   private static final Set<String> LOGGED_HAND_RENDER = ConcurrentHashMap.newKeySet();
   private static final String NEAR_HAND_POSE_STRATEGY = "near-hand-direct-anchor-v6";
   private static final float NEAR_HAND_X = 0.54F;
   private static final float NEAR_HAND_Y = -0.32F;
   private static final float NEAR_HAND_Z = -0.92F;
   private static final float NEAR_HAND_SCALE = 0.2F;

   private UtdTaczWorkbenchHandRender() {
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void onRenderHand(RenderHandEvent var0) {
      ItemStack var1 = var0.getItemStack();
      if (LOGGED_HAND_RENDER.add("hand-active")) {
         LOGGER.info("RenderHandEvent subscriber ACTIVE — first event seen (load-probe marker=hand-active)");
      }
      if (var1 != null && !var1.isEmpty()) {
         net.minecraft.resources.ResourceLocation var1id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(var1.getItem());
         if (var1id != null && var1id.getPath().contains("workbench")) {
            String var1probe = "probe|" + var1id;
            if (LOGGED_HAND_RENDER.add(var1probe)) {
               LOGGER.info("RenderHandEvent held workbench-ish item={} targetBlockId={} hand={} (marker=hand-probe)", var1id, targetBlockId(var1), var0.getHand());
            }
         }
      }
      ResourceLocation var2 = targetBlockId(var1);
      if (var2 != null) {
         Optional var3 = GunSmithTableRenderer.getIndex(var1);
         if (var3.isEmpty()) {
            logHandRender(var1, var2, var0.getHand(), null, false, "index=empty");
         } else {
            ClientBlockIndex var4 = (ClientBlockIndex)var3.get();
            BedrockModel var5 = var4.getModel();
            ResourceLocation var6 = var4.getTexture();
            if (var5 != null && var6 != null) {
               HumanoidArm var7 = armFor(var0.getHand());
               ItemDisplayContext var8 = displayContextFor(var7);
               PoseStack var9 = var0.getPoseStack();
               renderVanillaFirstPersonArmWithoutDepthWrite(
                  var9, var0.getMultiBufferSource(), var0.getPackedLight(), var7, var0.getEquipProgress(), var0.getSwingProgress()
               );
               var9.pushPose();

               try {
                  applyNearHandWorkbenchPose(var9, var7, var0.getSwingProgress(), var0.getEquipProgress());
                  applyTaczBedrockRootTransform(var9);
                  var5.render(var9, var8, RenderType.entityCutout(var6), var0.getPackedLight(), OverlayTexture.NO_OVERLAY);
                  flushBufferSource(var0.getMultiBufferSource());
               } finally {
                  var9.popPose();
               }

               var0.setCanceled(true);
               logHandRender(var1, var2, var0.getHand(), var6, true, "rendered-near-hand-direct-anchor");
            } else {
               logHandRender(var1, var2, var0.getHand(), var6, false, "modelOrTextureMissing");
            }
         }
      }
   }

   private static void renderVanillaFirstPersonArmWithoutDepthWrite(PoseStack var0, MultiBufferSource var1, int var2, HumanoidArm var3, float var4, float var5) {
      RenderSystem.depthMask(false);

      try {
         renderVanillaFirstPersonArm(var0, var1, var2, var3, var4, var5);
         flushBufferSource(var1);
      } finally {
         RenderSystem.depthMask(true);
      }
   }

   private static void flushBufferSource(MultiBufferSource var0) {
      if (var0 instanceof BufferSource var1) {
         var1.endBatch();
      }
   }

   private static void renderVanillaFirstPersonArm(PoseStack var0, MultiBufferSource var1, int var2, HumanoidArm var3, float var4, float var5) {
      Minecraft var6 = Minecraft.getInstance();
      LocalPlayer var7 = var6.player;
      if (var7 != null && !var7.isInvisible()) {
         if (var6.getEntityRenderDispatcher().getRenderer(var7) instanceof PlayerRenderer var9) {
            var0.pushPose();

            try {
               applyVanillaArmRenderPose(var0, var3, var4, var5);
               if (var3 == HumanoidArm.RIGHT) {
                  var9.renderRightHand(var0, var1, var2, var7);
               } else {
                  var9.renderLeftHand(var0, var1, var2, var7);
               }
            } finally {
               var0.popPose();
            }
         }
      }
   }

   private static void applyVanillaArmRenderPose(PoseStack var0, HumanoidArm var1, float var2, float var3) {
      float var4 = var1 == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      float var5 = Mth.sqrt(var3);
      float var6 = -0.3F * Mth.sin(var5 * (float) Math.PI);
      float var7 = 0.4F * Mth.sin(var5 * (float) (Math.PI * 2));
      float var8 = -0.4F * Mth.sin(var3 * (float) Math.PI);
      float var9 = Mth.sin(var3 * var3 * (float) Math.PI);
      float var10 = Mth.sin(var5 * (float) Math.PI);
      var0.translate(var4 * (var6 + 0.64000005F), var7 - 0.6F + var2 * -0.6F, var8 - 0.71999997F);
      var0.mulPose(Axis.YP.rotationDegrees(var4 * 45.0F));
      var0.mulPose(Axis.YP.rotationDegrees(var4 * var10 * 70.0F));
      var0.mulPose(Axis.ZP.rotationDegrees(var4 * var9 * -20.0F));
      var0.translate(var4 * -1.0F, 3.6F, 3.5F);
      var0.mulPose(Axis.ZP.rotationDegrees(var4 * 120.0F));
      var0.mulPose(Axis.XP.rotationDegrees(200.0F));
      var0.mulPose(Axis.YP.rotationDegrees(var4 * -135.0F));
      var0.translate(var4 * 5.6F, 0.0F, 0.0F);
   }

   private static void applyNearHandWorkbenchPose(PoseStack var0, HumanoidArm var1, float var2, float var3) {
      float var4 = var1 == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      float var5 = Mth.sin(Mth.sqrt(var2) * (float) Math.PI);
      float var6 = Mth.sin(var2 * (float) Math.PI);
      var0.translate(var4 * (0.54F - var5 * 0.04F), -0.32F - var3 * 0.25F + var5 * 0.03F, -0.92F + var6 * 0.04F);
      var0.mulPose(Axis.XP.rotationDegrees(8.0F));
      var0.mulPose(Axis.YP.rotationDegrees(var4 * 35.0F));
      var0.mulPose(Axis.ZP.rotationDegrees(var4 * -8.0F));
      var0.scale(0.2F, 0.2F, 0.2F);
   }

   private static void applyTaczBedrockRootTransform(PoseStack var0) {
      var0.translate(0.5, 1.5, 0.5);
      var0.mulPose(Axis.ZN.rotationDegrees(180.0F));
   }

   private static HumanoidArm armFor(InteractionHand var0) {
      LocalPlayer var1 = Minecraft.getInstance().player;
      HumanoidArm var2 = var1 == null ? HumanoidArm.RIGHT : var1.getMainArm();
      if (var0 == InteractionHand.OFF_HAND) {
         var2 = var2.getOpposite();
      }

      return var2;
   }

   private static ItemDisplayContext displayContextFor(HumanoidArm var0) {
      return var0 == HumanoidArm.LEFT ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
   }

   private static ResourceLocation targetBlockId(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ResourceLocation var1 = BuiltInRegistries.ITEM.getKey(var0.getItem());
         if (!TACZ_WORKBENCH_B.equals(var1)) {
            return null;
         } else {
            CompoundTag var2 = utd$customData(var0);
            if (var2 != null && var2.contains("BlockId", 8)) {
               ResourceLocation var3 = ResourceLocation.tryParse(var2.getString("BlockId"));
               return !OLD_WORKBENCH.equals(var3) && !SMITH_TABLE.equals(var3) ? null : var3;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static net.minecraft.nbt.CompoundTag utd$customData(net.minecraft.world.item.ItemStack stack) {
      CustomData cd = stack == null ? null : stack.get(DataComponents.CUSTOM_DATA);
      return cd == null ? null : cd.copyTag();
   }

   private static void logHandRender(ItemStack var0, ResourceLocation var1, InteractionHand var2, ResourceLocation var3, boolean var4, String var5) {
      ResourceLocation var6 = BuiltInRegistries.ITEM.getKey(var0.getItem());
      String var7 = var6 + "|" + var1 + "|" + var2 + "|" + var4 + "|" + var5;
      if (LOGGED_HAND_RENDER.add(var7)) {
         LOGGER.info(
            "hand event item={} BlockId={} hand={} rendered={} texture={} reason={} poseStrategy={} anchorX={} anchorY={} anchorZ={} anchorScale={}",
            new Object[]{var6, var1, var2, var4, var3, var5, "near-hand-direct-anchor-v6", 0.54F, -0.32F, -0.92F, 0.2F}
         );
      }
   }
}
