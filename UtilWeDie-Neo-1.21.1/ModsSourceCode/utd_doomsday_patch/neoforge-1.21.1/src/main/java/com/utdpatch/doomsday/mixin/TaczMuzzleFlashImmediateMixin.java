package com.utdpatch.doomsday.mixin;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.resource.pojo.display.gun.MuzzleFlash;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes the TaCZ muzzle flash rendering world-locked ("facing north") in first person.
 *
 * <p>{@code MuzzleFlashRender.doRender} draws the flash into the shared MultiBufferSource
 * ({@code renderBuffers().bufferSource()}), whose flush is deferred to a point where the modelview
 * is no longer the first-person camera-view matrix. The flash's captured pose already carries the
 * camera-rotation inverse (it is the gun's own screen-locked pose at the muzzle), so when the flash
 * is finally drawn under the wrong modelview the inverse no longer cancels -- the flash freezes to a
 * fixed world orientation.
 *
 * <p>Redirect the flash to a private immediate buffer and flush it on the spot, while the modelview
 * is still the camera view, so the captured inverse cancels and the flash stays at the muzzle. Same
 * root cause and fix as the FirstPersonFoodEating food model.
 */
@Mixin(value = {MuzzleFlashRender.class}, remap = false)
public abstract class TaczMuzzleFlashImmediateMixin {

    private static final MultiBufferSource.BufferSource utd$flashImmediate =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    @Redirect(
        method = "doRender",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/RenderBuffers;bufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"),
        remap = true
    )
    private static MultiBufferSource.BufferSource utd$redirectFlashBuffer(RenderBuffers instance) {
        return utd$flashImmediate;
    }

    @Inject(method = "doRender", at = @At("TAIL"))
    private static void utd$flushFlash(int light, int overlay, MuzzleFlash muzzleFlash, long time, CallbackInfo ci) {
        utd$flashImmediate.endBatch();
    }
}
