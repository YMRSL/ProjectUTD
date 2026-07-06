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

public class Modelnight_vision_goggles<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelnight_vision_goggles"), "main");
    public final ModelPart head;

    public Modelnight_vision_goggles(ModelPart root) {
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -5.8f, -5.0f, 10.0f, 1.0f, 10.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r1 = head.addOrReplaceChild("visor_r1", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0.7809f, 1.9292f, -7.0335f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)).mirror(false).texOffs(60, 10).mirror().addBox(0.7809f, 1.9292f, -6.616f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(36, 62).mirror().addBox(0.9109f, 2.0163f, -4.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(64, 13).mirror().addBox(0.9109f, 2.0163f, -2.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(0, 79).mirror().addBox(0.9109f, 2.0163f, -5.6283f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)).mirror(false), PartPose.offsetAndRotation((float)-1.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0457f, (float)-0.3051f, (float)-0.0138f));
        PartDefinition visor_r2 = head.addOrReplaceChild("visor_r2", CubeListBuilder.create().texOffs(62, 49).addBox(-2.0f, -0.713f, -2.0102f, 4.0f, 3.0f, 3.0f, new CubeDeformation(-0.6f)), PartPose.offsetAndRotation((float)0.0f, (float)-7.2628f, (float)-4.1938f, (float)0.2618f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r3 = head.addOrReplaceChild("visor_r3", CubeListBuilder.create().texOffs(54, 58).addBox(-1.0f, -1.4305f, -3.0018f, 2.0f, 2.0f, 4.0f, new CubeDeformation(-0.6f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.8727f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r4 = head.addOrReplaceChild("visor_r4", CubeListBuilder.create().texOffs(24, 12).addBox(-4.0f, 1.3031f, -3.5191f, 8.0f, 2.0f, 3.0f, new CubeDeformation(-0.5f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r5 = head.addOrReplaceChild("visor_r5", CubeListBuilder.create().texOffs(36, 48).addBox(0.0f, 1.9292f, -5.8744f, 2.0f, 2.0f, 1.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r6 = head.addOrReplaceChild("visor_r6", CubeListBuilder.create().texOffs(27, 78).addBox(-1.2981f, -0.9564f, -1.5019f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)).texOffs(40, 55).addBox(-1.2981f, -0.9564f, 1.4981f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(58, 42).addBox(-1.2981f, -0.9564f, -0.5019f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(22, 59).addBox(-1.2981f, -1.0436f, -2.4981f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 12).addBox(-1.2981f, -1.0436f, -2.9156f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)0.2981f, (float)-2.3571f, (float)-8.2073f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r7 = head.addOrReplaceChild("visor_r7", CubeListBuilder.create().texOffs(0, 16).addBox(-2.7809f, 1.9292f, -7.0335f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(60, 10).addBox(-2.7809f, 1.9292f, -6.616f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(36, 62).addBox(-2.9109f, 2.0163f, -4.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(64, 13).addBox(-2.9109f, 2.0163f, -2.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(0, 79).addBox(-2.9109f, 2.0163f, -5.6283f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)1.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0457f, (float)0.3051f, (float)0.0138f));
        PartDefinition visor_r8 = head.addOrReplaceChild("visor_r8", CubeListBuilder.create().texOffs(0, 60).addBox(-2.0f, -0.5344f, -4.0628f, 4.0f, 2.0f, 3.0f, new CubeDeformation(-0.5f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.2618f, (float)0.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)128, (int)128);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

