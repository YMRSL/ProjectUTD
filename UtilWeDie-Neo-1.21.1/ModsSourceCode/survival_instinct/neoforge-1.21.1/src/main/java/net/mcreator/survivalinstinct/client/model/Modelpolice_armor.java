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

public class Modelpolice_armor<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelpolice_armor"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart left_shoe;
    public final ModelPart right_shoe;

    public Modelpolice_armor(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.left_arm = root.getChild("left_arm");
        this.right_arm = root.getChild("right_arm");
        this.left_leg = root.getChild("left_leg");
        this.right_leg = root.getChild("right_leg");
        this.left_shoe = root.getChild("left_shoe");
        this.right_shoe = root.getChild("right_shoe");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)).texOffs(11, 64).addBox(-2.0f, -7.0f, -5.0f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(44, 45).addBox(-1.0f, -8.0f, -5.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -1.4687f, -5.0f, 10.0f, 2.0f, 10.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.0313f, (float)-0.0661f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(24, 20).addBox(-5.0f, 0.5f, -6.25f, 10.0f, 1.0f, 8.0f, new CubeDeformation(0.0f)).texOffs(0, 28).addBox(-4.0f, -1.5f, -3.25f, 8.0f, 2.0f, 8.0f, new CubeDeformation(0.551f)), PartPose.offsetAndRotation((float)0.0f, (float)-6.5f, (float)-0.75f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 29).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(48, 45).addBox(-4.0f, -4.5015f, -1.4827f, 8.0f, 9.0f, 2.0f, new CubeDeformation(-0.15f)).texOffs(38, 64).addBox(-3.0f, -2.0015f, -0.4827f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(36, 16).addBox(-3.0f, -3.5015f, -0.4827f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5015f, (float)2.4173f, (float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(56, 10).addBox(-3.5f, -1.1167f, -0.2176f, 7.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(66, 45).addBox(-3.0f, -0.5167f, -0.0176f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)).texOffs(30, 2).addBox(-3.0f, 0.9833f, -0.0176f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)).texOffs(30, 0).addBox(-3.0f, 2.4833f, -0.0176f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5015f, (float)2.4173f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(0, 64).addBox(-1.5f, -5.6f, -0.6f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(36, 12).addBox(-1.5f, -5.6f, -0.6f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)2.0f, (float)8.0f, (float)-2.9f, (float)3.098f, (float)0.0f, (float)-3.1416f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(0, 4).addBox(-2.0f, -1.75f, -0.5f, 3.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(24, 32).addBox(-2.0f, -1.75f, -0.5f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)2.5f, (float)8.1493f, (float)-2.968f, (float)3.0986f, (float)0.0423f, (float)3.1246f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(24, 12).addBox(-1.0f, -1.75f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(12, 54).addBox(-1.0f, -1.75f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-0.5f, (float)8.1493f, (float)-2.968f, (float)3.0103f, (float)-0.1308f, (float)-3.1359f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(56, 37).addBox(-1.0f, -1.75f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(54, 56).addBox(-1.0f, -1.75f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-3.5f, (float)8.1493f, (float)-2.968f, (float)3.0533f, (float)-0.218f, (float)-3.1319f));
        PartDefinition body_r7 = body.addOrReplaceChild("body_r7", CubeListBuilder.create().texOffs(0, 38).addBox(-1.5f, -5.6f, -0.6f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(68, 38).addBox(-1.5f, -5.6f, -0.6f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(43, 66).addBox(-3.5f, -3.6f, -0.8f, 7.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)8.0f, (float)-2.9f, (float)3.098f, (float)0.0f, (float)-3.1416f));
        PartDefinition body_r8 = body.addOrReplaceChild("body_r8", CubeListBuilder.create().texOffs(0, 12).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.15f)).texOffs(0, 28).addBox(-1.0f, -0.95f, 1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(16, 64).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.0f, (float)0.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition body_r9 = body.addOrReplaceChild("body_r9", CubeListBuilder.create().texOffs(24, 12).addBox(-4.9504f, -1.1506f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)7.0208f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition body_r10 = body.addOrReplaceChild("body_r10", CubeListBuilder.create().texOffs(56, 37).addBox(-2.0f, 0.05f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)-3.0f, (float)7.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition body_r11 = body.addOrReplaceChild("body_r11", CubeListBuilder.create().texOffs(30, 4).addBox(-1.0f, -0.95f, 1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(62, 50).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(68, 0).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(-0.15f)), PartPose.offsetAndRotation((float)-3.0f, (float)0.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition body_r12 = body.addOrReplaceChild("body_r12", CubeListBuilder.create().texOffs(42, 56).addBox(-1.0f, 0.05f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)3.0f, (float)7.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition body_r13 = body.addOrReplaceChild("body_r13", CubeListBuilder.create().texOffs(54, 58).addBox(1.9504f, -1.1506f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)7.0208f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition body_r14 = body.addOrReplaceChild("body_r14", CubeListBuilder.create().texOffs(24, 29).addBox(-2.0f, -0.75f, -0.4f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-2.0f, (float)3.75f, (float)-3.1f, (float)-3.0543f, (float)-0.0019f, (float)3.098f));
        PartDefinition body_r15 = body.addOrReplaceChild("body_r15", CubeListBuilder.create().texOffs(28, 38).addBox(-0.5f, 0.25f, -0.4f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(32, 45).addBox(1.5f, 0.25f, -0.4f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.0f, (float)3.75f, (float)-3.1f, (float)3.098f, (float)-0.0019f, (float)3.098f));
        PartDefinition body_r16 = body.addOrReplaceChild("body_r16", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -0.75f, -0.4f, 4.0f, 3.0f, 1.0f, new CubeDeformation(-0.15f)), PartPose.offsetAndRotation((float)-2.0f, (float)3.75f, (float)-3.1f, (float)3.098f, (float)-0.0019f, (float)3.098f));
        PartDefinition body_r17 = body.addOrReplaceChild("body_r17", CubeListBuilder.create().texOffs(52, 16).addBox(-4.0f, -4.5f, -1.45f, 8.0f, 9.0f, 2.0f, new CubeDeformation(-0.15f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5f, (float)-2.55f, (float)0.0f, (float)3.1416f, (float)0.0f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 38).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 45).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 38).addBox(-1.9f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 0).addBox(-2.1f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(68, 33).addBox(-2.1f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(68, 23).addBox(-2.1f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(93, 0).mirror().addBox(-1.0f, -1.5075f, -1.3285f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(89, 3).mirror().addBox(-0.5f, -0.5075f, -0.1285f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.5f)).mirror(false).texOffs(93, 6).mirror().addBox(-1.0f, -1.5075f, -1.0285f, 2.0f, 2.0f, 2.0f, new CubeDeformation(0.6f)).mirror(false), PartPose.offsetAndRotation((float)-3.1f, (float)2.9253f, (float)-0.2885f, (float)-0.0863f, (float)-1.5272f, (float)0.001f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(56, 27).addBox(-2.0f, 6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.49f)).texOffs(11, 67).addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(56, 27).mirror().addBox(-2.0f, 6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.49f)).mirror(false).texOffs(11, 67).mirror().addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)128, (int)128);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

