package net.mcreator.survivalinstinct.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ModelUmbrelaCasco2<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "model_umbrela_casco_2"), "main");
    public final ModelPart Casco;

    public ModelUmbrelaCasco2(ModelPart root) {
        this.Casco = root.getChild("Casco");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition Casco = partdefinition.addOrReplaceChild("Casco", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -6.9f, -4.0f, 8.0f, 7.0f, 8.0f, new CubeDeformation(0.1f)).texOffs(59, 0).addBox(-3.3f, -4.8f, -4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(59, 12).addBox(1.3f, -4.8f, -4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(0, 16).addBox(-4.0f, -7.4f, -4.0f, 8.0f, 4.0f, 8.0f, new CubeDeformation(0.6f)).texOffs(26, 10).addBox(-4.0f, -4.0f, -2.0f, 8.0f, 2.0f, 6.0f, new CubeDeformation(1.0f)).texOffs(24, 0).addBox(-4.0f, -4.0f, -4.0f, 8.0f, 0.0f, 7.0f, new CubeDeformation(0.8f)).texOffs(24, 18).addBox(-4.0f, -4.0f, 0.0f, 8.0f, 4.0f, 1.0f, new CubeDeformation(0.3f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition group = Casco.addOrReplaceChild("group", CubeListBuilder.create(), PartPose.offset((float)8.0f, (float)0.0f, (float)-12.0f));
        PartDefinition Casco_r1 = group.addOrReplaceChild("Casco_r1", CubeListBuilder.create().texOffs(0, 53).addBox(2.5f, 2.0f, -0.7f, 3.0f, 1.0f, 3.0f, new CubeDeformation(-0.1f)).texOffs(0, 51).addBox(2.5f, 0.0f, -0.7f, 3.0f, 3.0f, 3.0f, new CubeDeformation(-0.2f)).texOffs(12, 48).addBox(2.9f, 2.7f, 0.0f, 2.3f, 1.0f, 2.3f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)-8.0f, (float)0.0f, (float)8.0f, (float)-0.8021f, (float)-0.504f, (float)-0.437f));
        PartDefinition Casco_r2 = group.addOrReplaceChild("Casco_r2", CubeListBuilder.create().texOffs(12, 36).addBox(-1.1f, 1.7f, 0.0f, 2.3f, 1.0f, 2.3f, new CubeDeformation(0.1f)).texOffs(12, 41).addBox(-1.5f, 1.0f, -0.7f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(0, 39).addBox(-1.5f, -3.0f, -0.7f, 3.0f, 3.0f, 3.0f, new CubeDeformation(0.1f)).texOffs(73, 0).addBox(-1.5f, 0.0f, -0.7f, 3.0f, 1.0f, 3.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)-8.0f, (float)0.0f, (float)8.0f, (float)-0.9163f, (float)0.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)200, (int)200);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.Casco.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

