package com.scarasol.zombiekit.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("renderChunksInFrustum")
    ObjectArrayList<?> zombiekit$getRenderChunksInFrustum();
}