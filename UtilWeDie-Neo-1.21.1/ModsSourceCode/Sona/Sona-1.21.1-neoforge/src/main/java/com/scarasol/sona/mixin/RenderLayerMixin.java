package com.scarasol.sona.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.client.renderer.SonaRenderType;
import com.scarasol.sona.compat.ShaderCompatUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Scarasol
 *
 * 1.21.1: {@code renderColoredCutoutModel} signature changed from per-channel
 * {@code float red, green, blue} to a single packed {@code int color}; {@code renderToBuffer}
 * likewise takes an {@code int color}. Camouflage alpha is folded into the color int.
 * Forge -> NeoForge {@code ModList}.
 */
@Mixin(RenderLayer.class)
public abstract class RenderLayerMixin<T extends Entity, M extends EntityModel<T>> {
    @Inject(method = "renderColoredCutoutModel", cancellable = true, at = @At("HEAD"))
    private static <T extends LivingEntity> void sona$renderColoredCutoutModel(
            EntityModel<T> model,
            ResourceLocation texture,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T entity,
            int color,
            CallbackInfo ci
    ) {
        if (entity instanceof ILivingEntityAccessor livingEntityAccessor && livingEntityAccessor.getCamouflageAmplifier() > 0) {
            RenderType targetType = ModList.get().isLoaded("oculus") && ShaderCompatUtil.isShaderActive()
                    ? RenderType.entityTranslucent(texture)
                    : SonaRenderType.entityDither(texture);

            VertexConsumer vertexconsumer = buffer.getBuffer(targetType);
            float alpha = livingEntityAccessor.getCamouflageAlpha();
            int srcAlpha = FastColor.ARGB32.alpha(color);
            int packedColor = FastColor.ARGB32.color((int) (srcAlpha * alpha), FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color));
            model.renderToBuffer(poseStack, vertexconsumer, packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0.0F), packedColor);
            ci.cancel();
        }
    }
}
