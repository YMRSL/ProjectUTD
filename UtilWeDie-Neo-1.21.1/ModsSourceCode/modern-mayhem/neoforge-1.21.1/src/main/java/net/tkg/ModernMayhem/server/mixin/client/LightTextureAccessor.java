package net.tkg.ModernMayhem.server.mixin.client;

import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={LightTexture.class})
public interface LightTextureAccessor {
    @Accessor(value="updateLightTexture")
    public boolean getUpdateLightTexture();

    @Accessor(value="blockLightRedFlicker")
    public float getBlockLightRedFlicker();
}

