package com.scarasol.sona.mixin.tacz;

import com.scarasol.sona.client.renderer.SonaRenderType;
import com.scarasol.sona.compat.ShaderCompatUtil;
import com.scarasol.sona.mixin.CompositeRenderTypeAccessor;
import com.scarasol.sona.mixin.CompositeStateAccessor;
import com.scarasol.sona.mixin.TextureStateShardAccessor;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

/**
 * Camouflage ("循声") translucent RenderType swap for held TaCZ guns. Forge -> NeoForge {@code ModList};
 * only swaps the RenderType (does NOT touch the 1.21.1 int-color {@code renderToBuffer}), so it is
 * vanilla-API stable. The tacz-internal {@code lambda$renderByItem$6} target ({@code remap = false})
 * needs verification against tacz 1.1.8.
 */
@Mixin(value = GunItemRendererWrapper.class, remap = false)
public abstract class TACZGunItemRendererWrapperMixin {

    @ModifyArg(
            method = "lambda$renderByItem$6",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/tacz/guns/client/model/BedrockGunModel;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/renderer/RenderType;II)V"
            ),
            index = 3
    )
    private static RenderType sona$useCamouflageRenderType(RenderType originalRenderType) {
        Float alpha = SonaRenderType.camoAlpha.get();
        if (alpha == null || alpha >= 1.0f) {
            return originalRenderType;
        }

        if (!(originalRenderType instanceof CompositeRenderTypeAccessor accessor)) {
            return originalRenderType;
        }

        RenderType.CompositeState state = accessor.sona$getState();
        RenderStateShard.EmptyTextureStateShard textureState = ((CompositeStateAccessor) (Object) state).sona$getTextureState();
        if (!(textureState instanceof TextureStateShardAccessor texAccessor)) {
            return originalRenderType;
        }

        Optional<ResourceLocation> texture = texAccessor.sona$getTexture();
        if (texture.isEmpty()) {
            return originalRenderType;
        }

        return ModList.get().isLoaded("oculus") && ShaderCompatUtil.isShaderActive()
                ? RenderType.entityTranslucent(texture.get())
                : SonaRenderType.entityDither(texture.get());
    }
}
