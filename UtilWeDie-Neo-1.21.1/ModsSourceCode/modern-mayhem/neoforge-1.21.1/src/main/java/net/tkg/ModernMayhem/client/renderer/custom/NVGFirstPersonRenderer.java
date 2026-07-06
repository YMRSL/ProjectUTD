package net.tkg.ModernMayhem.client.renderer.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.tkg.ModernMayhem.client.item.NVGFirstPersonFakeItem;
import net.tkg.ModernMayhem.client.models.custom.NVGFirstPersonModel;
import net.tkg.ModernMayhem.server.util.AnimUtils;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.RenderUtil;

public class NVGFirstPersonRenderer
extends GeoItemRenderer<NVGFirstPersonFakeItem> {
    private static final float SCALE_RECIPROCAL = 0.0625f;
    protected boolean renderArms = false;
    protected MultiBufferSource currentBuffer;
    protected RenderType renderType;

    public NVGFirstPersonRenderer() {
        super((GeoModel)new NVGFirstPersonModel());
    }

    public RenderType getRenderType(NVGFirstPersonFakeItem animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull((ResourceLocation)this.getTextureLocation(animatable));
    }

    public void initCurrentItemStack(NVGFirstPersonFakeItem item) {
        this.currentItemStack = new ItemStack((ItemLike)item);
    }

    public void actuallyRender(PoseStack poseStack, NVGFirstPersonFakeItem animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        LocalPlayer player = Minecraft.getInstance().player;
        this.currentBuffer = bufferSource;
        this.renderType = renderType;
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (!this.renderArms) {
            return;
        }
        this.renderArms = false;
    }

    public void renderRecursively(PoseStack poseStack, NVGFirstPersonFakeItem animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            String boneName = bone.getName();
            boolean renderingArms = false;
            if (boneName.equals("left_arm") || boneName.equals("right_arm")) {
                bone.setHidden(true);
                renderingArms = true;
            }
            if (renderingArms) {
                float armsAlpha = player.isInvisible() ? 0.0f : 1.0f;
                PlayerRenderer playerRenderer = (PlayerRenderer)(Object)mc.getEntityRenderDispatcher().getRenderer((Entity)player);
                PlayerModel model = (PlayerModel)playerRenderer.getModel();
                poseStack.pushPose();
                RenderUtil.translateMatrixToBone((PoseStack)poseStack, (GeoBone)bone);
                RenderUtil.translateToPivotPoint((PoseStack)poseStack, (GeoBone)bone);
                RenderUtil.rotateMatrixAroundBone((PoseStack)poseStack, (GeoBone)bone);
                RenderUtil.scaleMatrixForBone((PoseStack)poseStack, (GeoBone)bone);
                RenderUtil.translateAwayFromPivotPoint((PoseStack)poseStack, (GeoBone)bone);
                poseStack.translate(0.0f, -0.8f, 0.0f);
                ResourceLocation playerSkin = player.getSkin().texture();
                VertexConsumer armBuilder = this.currentBuffer.getBuffer(RenderType.entitySolid((ResourceLocation)playerSkin));
                VertexConsumer sleeveBuilder = this.currentBuffer.getBuffer(RenderType.entityTranslucentCull((ResourceLocation)playerSkin));
                if (boneName.equals("left_arm")) {
                    poseStack.translate(-0.0625f, 0.125f, 0.0f);
                    AnimUtils.renderPartOverBone(model.leftArm, bone, poseStack, armBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                    AnimUtils.renderPartOverBone(model.leftSleeve, bone, poseStack, sleeveBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                } else {
                    poseStack.translate(0.0625f, 0.125f, 0.0f);
                    AnimUtils.renderPartOverBone(model.rightArm, bone, poseStack, armBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                    AnimUtils.renderPartOverBone(model.rightSleeve, bone, poseStack, sleeveBuilder, packedLight, OverlayTexture.NO_OVERLAY, armsAlpha);
                }
                this.currentBuffer.getBuffer(RenderType.entityTranslucent((ResourceLocation)this.getTextureLocation((NVGFirstPersonFakeItem)this.animatable)));
                poseStack.popPose();
            }
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}

