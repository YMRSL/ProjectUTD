package de.bene2212.holdmyitems.util;

import com.mojang.blaze3d.vertex.PoseStack;
import de.bene2212.holdmyitems.interfaces.AlternateBlockRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.level.block.state.BlockState;

public class AlternateBlockRendererInvoker {
    public static void renderSingleBlockEmission(BlockRenderDispatcher dispatcher, BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light) {
        if (dispatcher instanceof AlternateBlockRenderer) {
            AlternateBlockRenderer renderer = (AlternateBlockRenderer) dispatcher;
            renderer.renderSingleBlockEmission(state, poseStack, buffer, light);
        }
    }
}
