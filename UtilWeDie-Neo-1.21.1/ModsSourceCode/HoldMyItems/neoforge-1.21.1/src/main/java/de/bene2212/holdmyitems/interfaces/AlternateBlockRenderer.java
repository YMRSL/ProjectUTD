package de.bene2212.holdmyitems.interfaces;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.state.BlockState;

public interface AlternateBlockRenderer {
    void renderSingleBlockEmission(BlockState var1, PoseStack var2, MultiBufferSource var3, int var4);
}
