package com.scarasol.sona.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Scarasol
 *
 * 1.21.1: {@code render(...)} keeps the {@code (PoseStack, MultiBufferSource, int, T, FFFFFF)} shape.
 * {@code renderModel} changed from {@code (PoseStack, MultiBufferSource, int, Model, boolean withGlint,
 * float red, float green, float blue, ResourceLocation)} to
 * {@code (PoseStack, MultiBufferSource, int packedLight, Model, int packedOverlay, ResourceLocation)}.
 * The camouflage translucent re-render is adapted: alpha folded into a packed {@code int color}.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public HumanoidArmorLayerMixin(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Unique
    private float sona$RenderTranslucent = 1f;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"))
    private void sona$renderArmorPre(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (entity instanceof ILivingEntityAccessor livingEntityAccessor) {
            sona$RenderTranslucent = livingEntityAccessor.getCamouflageAlpha();
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("TAIL"))
    private void sona$renderArmorPost(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        sona$RenderTranslucent = 1f;
    }

    @Inject(method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V", cancellable = true, at = @At("HEAD"))
    private void sona$renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            Model armorModel,
            int packedOverlay,
            ResourceLocation armorTexture,
            CallbackInfo ci
    ) {
        if (sona$RenderTranslucent < 1) {
            if (armorTexture != null) {
                VertexConsumer vertexconsumer = bufferSource.getBuffer(RenderType.entityTranslucent(armorTexture));
                int color = FastColor.ARGB32.color((int) (255 * sona$RenderTranslucent), 255, 255, 255);
                armorModel.renderToBuffer(poseStack, vertexconsumer, packedLight, packedOverlay, color);
                ci.cancel();
            }
        }
    }
}
