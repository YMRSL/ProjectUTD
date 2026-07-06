package com.utdpatch.doomsday.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * During world join or replay handoff, renderLevel can run before Minecraft has
 * restored a camera entity. Vanilla Camera.setup dereferences it immediately.
 */
@Mixin(GameRenderer.class)
public class GameRendererNullCameraGuardMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"), cancellable = true)
    private void utd$guardNullCameraEntity(DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.getCameraEntity() != null) {
            return;
        }
        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
            return;
        }
        ci.cancel();
    }
}
