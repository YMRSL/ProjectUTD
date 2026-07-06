package io.github.ymrsl.firstpersonfoodeating.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;

public final class PlayerArmRenderUtil {
    private PlayerArmRenderUtil() {
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm arm, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int light) {
        if (player == null) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (!(dispatcher.getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        int oldTexture = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, player.getSkin().texture());
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poseStack, bufferSource, light, player);
        } else {
            renderer.renderLeftHand(poseStack, bufferSource, light, player);
        }
        RenderSystem.setShaderTexture(0, oldTexture);
    }
}

