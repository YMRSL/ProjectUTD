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

public class Modelriot_armor<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelriot_armor"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart left_shoe;
    public final ModelPart right_shoe;

    public Modelriot_armor(ModelPart root) {
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
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(26, 25).addBox(-5.0f, -5.0f, -1.0f, 10.0f, 4.0f, 6.0f, new CubeDeformation(0.6f)).texOffs(24, 15).addBox(-5.0f, -5.5f, -5.0f, 10.0f, 1.0f, 5.0f, new CubeDeformation(0.3f)).texOffs(47, 37).addBox(-5.0f, -4.5f, -5.0f, 10.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 0).addBox(-5.0f, -9.5f, -5.0f, 10.0f, 5.0f, 10.0f, new CubeDeformation(-0.2f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 31).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(64, 23).addBox(-4.0f, -0.8f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)).texOffs(0, 63).addBox(-4.0f, -0.8f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.65f)).texOffs(0, 63).mirror().addBox(1.0f, -0.8f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.65f)).mirror(false).texOffs(64, 23).mirror().addBox(1.0f, -0.8f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)).mirror(false).texOffs(62, 0).addBox(-4.0f, 2.0f, -3.0f, 8.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(70, 17).addBox(-4.0f, 3.0f, -3.4f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(68, 67).addBox(-4.0f, 5.0f, -3.4f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(68, 34).addBox(-4.0f, 7.0f, -3.4f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(66, 14).addBox(-4.0f, 9.0f, -3.4f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(28, 50).addBox(-2.0f, 4.5f, -0.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(40, 13).addBox(-3.0f, 2.5f, -0.5f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(28, 48).addBox(-3.0f, 0.5f, -0.5f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(0, 7).addBox(-2.0f, 4.5f, -0.5f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(12, 64).addBox(-3.0f, 0.5f, -0.5f, 6.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)10.5f, (float)2.5f, (float)0.3054f, (float)0.0f, (float)0.0f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(0, 7).addBox(-2.0f, 4.5f, -0.5f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(28, 50).addBox(-2.0f, 4.5f, -0.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(40, 13).addBox(-3.0f, 2.5f, -0.5f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(28, 48).addBox(-3.0f, 0.5f, -0.5f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(12, 64).addBox(-3.0f, 0.5f, -0.5f, 6.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)10.5f, (float)-2.5f, (float)-0.2618f, (float)0.0f, (float)0.0f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(66, 14).addBox(-4.0f, 2.5f, -0.95f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(68, 34).addBox(-4.0f, 0.5f, -0.95f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(68, 67).addBox(-4.0f, -1.5f, -0.95f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(70, 17).addBox(-4.0f, -3.5f, -0.95f, 8.0f, 1.0f, 2.0f, new CubeDeformation(0.05f)).texOffs(62, 0).addBox(-4.0f, -4.5f, -0.55f, 8.0f, 10.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5f, (float)2.55f, (float)0.0f, (float)3.1416f, (float)0.0f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(24, 15).addBox(0.5f, -6.5f, 0.0f, 1.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 0).addBox(-1.5f, -2.5f, -1.0f, 3.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.5f, (float)2.5f, (float)-3.0f, (float)-0.0886f, (float)-0.1739f, (float)0.1899f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(54, 76).mirror().addBox(-1.0f, 2.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(76, 47).mirror().addBox(-1.0f, 6.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(16, 48).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_arm_r1 = left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(52, 54).mirror().addBox(-0.5f, -2.0f, -3.0f, 3.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)1.5f, (float)6.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition left_arm_r2 = left_arm.addOrReplaceChild("left_arm_r2", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-2.5f, -4.0f, -3.0f, 5.0f, 7.0f, 6.0f, new CubeDeformation(-0.3f)).mirror(false), PartPose.offsetAndRotation((float)2.5f, (float)1.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(16, 48).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(76, 57).addBox(-3.0f, -1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(54, 76).addBox(-3.0f, 2.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(76, 47).addBox(-3.0f, 6.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm_r1 = right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(52, 54).addBox(-2.5f, -2.0f, -3.0f, 3.0f, 5.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.5f, (float)6.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition right_arm_r2 = right_arm.addOrReplaceChild("right_arm_r2", CubeListBuilder.create().texOffs(40, 0).addBox(-2.5f, -4.0f, -3.0f, 5.0f, 7.0f, 6.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)-2.5f, (float)1.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(70, 37).addBox(-1.9f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)).texOffs(30, 0).addBox(-1.9f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)).texOffs(42, 44).mirror().addBox(-1.9f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition left_leg_r1 = left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(-1.5f, -2.0f, -1.0f, 3.0f, 4.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offsetAndRotation((float)0.1f, (float)5.0f, (float)-1.5f, (float)0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(42, 44).addBox(-2.1f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.2f)).texOffs(42, 73).addBox(-2.1f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)).texOffs(68, 72).addBox(-2.1f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(0, 15).addBox(-1.5f, -2.0f, -1.0f, 3.0f, 4.0f, 1.0f, new CubeDeformation(0.4f)), PartPose.offsetAndRotation((float)-0.1f, (float)5.0f, (float)-1.5f, (float)0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition right_leg_r2 = right_leg.addOrReplaceChild("right_leg_r2", CubeListBuilder.create().texOffs(66, 77).addBox(-1.0f, -2.25f, -2.0f, 2.0f, 3.0f, 4.0f, new CubeDeformation(0.45f)).texOffs(30, 69).addBox(-1.0f, -2.25f, -2.0f, 2.0f, 6.0f, 4.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-3.1f, (float)2.25f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(14, 69).addBox(-2.0f, 7.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.6f)).texOffs(24, 21).addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).texOffs(56, 67).addBox(-2.0f, 7.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(24, 21).mirror().addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(14, 69).mirror().addBox(-2.0f, 7.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.6f)).mirror(false).texOffs(56, 67).mirror().addBox(-2.0f, 7.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
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

