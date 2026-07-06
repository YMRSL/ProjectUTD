package com.scarasol.sona.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.client.renderer.AlphaVertexConsumer;
import com.scarasol.sona.client.renderer.CamouflageRenderUtil;
import com.scarasol.sona.client.renderer.SonaRenderType;
import com.scarasol.sona.compat.ShaderCompatUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * @author Scarasol
 *
 * Camouflage ("循声") whole-entity buffer wrap. Forge -> NeoForge {@code ModList} import only;
 * alpha is applied via {@link AlphaVertexConsumer}, independent of the 1.21.1 vertex-color int change.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    private void sona$wrapMultiBufferSource(EntityRenderer instance, Entity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Operation<Void> original) {
        if (!(entity instanceof LivingEntity) || !(entity instanceof ILivingEntityAccessor accessor)) {
            original.call(instance, entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            return;
        }

        float alpha = accessor.getCamouflageAlpha();
        if (alpha >= 1.0f) {
            original.call(instance, entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
            return;
        }

        boolean isShaderActive = ModList.get().isLoaded("oculus") && ShaderCompatUtil.isShaderActive();

        MultiBufferSource finalSource = renderType -> {
            if (renderType.mode() != VertexFormat.Mode.QUADS && renderType.mode() != VertexFormat.Mode.TRIANGLES) {
                return bufferSource.getBuffer(renderType);
            }

            VertexFormat format = renderType.format();
            RenderType targetType = renderType;
            boolean shouldWrap = false;

            if (format == DefaultVertexFormat.BLOCK) {
                if (renderType.affectsCrumbling()) {
                    targetType = isShaderActive ? RenderType.translucent() : SonaRenderType.itemDither();
                    shouldWrap = true;
                }
            }
            else if (format == DefaultVertexFormat.NEW_ENTITY) {
                if (renderType instanceof CompositeRenderTypeAccessor accessorRender) {
                    RenderType.CompositeState state = accessorRender.sona$getState();
                    RenderStateShard.EmptyTextureStateShard textureState = ((CompositeStateAccessor) (Object) state).sona$getTextureState();

                    if (textureState instanceof TextureStateShardAccessor texAccessor) {
                        Optional<ResourceLocation> optTexture = texAccessor.sona$getTexture();
                        if (optTexture.isPresent()) {
                            ResourceLocation texture = optTexture.get();
                            targetType = isShaderActive ? RenderType.entityTranslucent(texture) : SonaRenderType.entityDither(texture);
                            shouldWrap = true;
                        }
                    }
                }
            }

            if (shouldWrap) {
                return new AlphaVertexConsumer(bufferSource.getBuffer(targetType), CamouflageRenderUtil.itemAlpha(alpha));
            }

            return bufferSource.getBuffer(renderType);
        };

        original.call(instance, entity, entityYaw, partialTicks, poseStack, finalSource, packedLight);
    }
}
