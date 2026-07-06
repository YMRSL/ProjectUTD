package net.mcreator.survivalinstinct.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.mcreator.survivalinstinct.client.model.Modelnail_proyectile;
import net.mcreator.survivalinstinct.entity.NailProyectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class NailProyectileRenderer
extends EntityRenderer<NailProyectileEntity> {
    private static final ResourceLocation texture = ResourceLocation.parse("survival_instinct:textures/entities/nail_proyectile.png");
    private final Modelnail_proyectile model;

    public NailProyectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Modelnail_proyectile(context.bakeLayer(Modelnail_proyectile.LAYER_LOCATION));
    }

    public void render(NailProyectileEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        VertexConsumer vb = bufferIn.getBuffer(RenderType.entityCutout((ResourceLocation)this.getTextureLocation(entityIn)));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp((float)partialTicks, (float)entityIn.yRotO, (float)entityIn.getYRot()) - 90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f + Mth.lerp((float)partialTicks, (float)entityIn.xRotO, (float)entityIn.getXRot())));
        this.model.renderToBuffer(poseStack, vb, packedLightIn, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
    }

    public ResourceLocation getTextureLocation(NailProyectileEntity entity) {
        return texture;
    }
}

