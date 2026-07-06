package com.scarasol.zombiekit.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.21.1 note: {@code LevelRenderer.renderChunksInFrustum} (a {@code List<RenderChunkInfo>}) was removed. The
 * frustum-visible chunk list is now {@code visibleSections} of type
 * {@code ObjectArrayList<SectionRenderDispatcher.RenderSection>}. The inner {@code LevelRenderer$RenderChunkInfo}
 * type no longer exists, so the old {@code RenderChunkInfoAccessor} is dropped -- the list elements ARE the
 * {@link SectionRenderDispatcher.RenderSection}s now, and {@code RenderSection#getOrigin()} gives the chunk origin
 * directly. ThermalShader iterates these RenderSections.
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("visibleSections")
    ObjectArrayList<SectionRenderDispatcher.RenderSection> zombiekit$getVisibleSections();
}
