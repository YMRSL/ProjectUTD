package net.tkg.ModernMayhem.server.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.tkg.ModernMayhem.client.Darkness;
import net.tkg.ModernMayhem.server.mixin.client.LightTextureAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={GameRenderer.class})
public abstract class GameRendererMixin {
    @Shadow
    @Final
    public LightTexture lightTexture;
    @Shadow
    @Final
    public Minecraft minecraft;

    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void inject$renderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        boolean shouldUpdate = ((LightTextureAccessor)this.lightTexture).getUpdateLightTexture();
        if (shouldUpdate) {
            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
            this.minecraft.getProfiler().push("darkenLightTexture");
            float flicker = ((LightTextureAccessor)this.lightTexture).getBlockLightRedFlicker();
            Darkness.updateLuminance(tickDelta, this.minecraft, (GameRenderer)(Object)this, flicker);
            this.minecraft.getProfiler().pop();
        }
    }
}

