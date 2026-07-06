package com.scarasol.sona.mixin;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.scarasol.sona.client.renderer.AlphaVertexConsumer;
import com.scarasol.sona.client.renderer.CamouflageRenderUtil;
import com.scarasol.sona.client.renderer.SonaRenderType;
import com.scarasol.sona.compat.ShaderCompatUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * @author Scarasol
 *
 * Camouflage ("循声"/stealth) buffer interception. Forge {@code net.minecraftforge.fml.ModList}
 * -> NeoForge {@code net.neoforged.fml.ModList}. The alpha is applied via the
 * {@link AlphaVertexConsumer} wrapper, which is independent of the 1.21.1
 * {@code renderToBuffer(..., int color)} vertex-color change.
 */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceMixin {

    @Shadow public abstract VertexConsumer getBuffer(RenderType p_109919_);

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void sona$globalBufferIntercept(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        Float alpha = SonaRenderType.camoAlpha.get();
        if (alpha != null && alpha < 1.0f) {
            SonaRenderType.camoAlpha.remove();

            try {
                if (renderType.mode() != VertexFormat.Mode.QUADS && renderType.mode() != VertexFormat.Mode.TRIANGLES) {
                    cir.setReturnValue(this.getBuffer(renderType));
                    return;
                }

                VertexFormat format = renderType.format();
                boolean isShaderActive = ModList.get().isLoaded("oculus") && ShaderCompatUtil.isShaderActive();
                RenderType targetType = renderType;
                boolean shouldWrap = false;

                if (format == DefaultVertexFormat.BLOCK) {
                    if (renderType.affectsCrumbling()) {
                        targetType = isShaderActive ? RenderType.translucent() : SonaRenderType.itemDither();
                        shouldWrap = true;
                    }
                }

                else if (format == DefaultVertexFormat.NEW_ENTITY) {
                    if (renderType instanceof CompositeRenderTypeAccessor accessor) {
                        RenderType.CompositeState state = accessor.sona$getState();
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
                    cir.setReturnValue(new AlphaVertexConsumer(this.getBuffer(targetType), CamouflageRenderUtil.itemAlpha(alpha)));
                } else {
                    cir.setReturnValue(this.getBuffer(renderType));
                }

            } finally {
                SonaRenderType.camoAlpha.set(alpha);
            }
        }
    }
}
