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

public class Modelrecluit<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelrecluit"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart right_shoe;
    public final ModelPart left_shoe;

    public Modelrecluit(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.left_arm = root.getChild("left_arm");
        this.right_arm = root.getChild("right_arm");
        this.left_leg = root.getChild("left_leg");
        this.right_leg = root.getChild("right_leg");
        this.right_shoe = root.getChild("right_shoe");
        this.left_shoe = root.getChild("left_shoe");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 27).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)).texOffs(0, 82).addBox(-5.0f, -9.0f, -2.0f, 10.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(0, 27).addBox(-5.5f, -4.5f, -2.0f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.3f)).texOffs(76, 2).addBox(4.5f, -4.5f, -2.0f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.2f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(76, 8).mirror().addBox(-1.0f, -0.5f, -1.5f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offsetAndRotation((float)5.5f, (float)-3.0f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)-0.5236f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(18, 66).addBox(-4.0676f, -1.3313f, -5.0f, 3.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.1913f, (float)-5.2093f, (float)-4.0f, (float)0.0807f, (float)0.0334f, (float)-0.3914f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(0, 66).addBox(1.0968f, -1.261f, -5.0f, 3.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.1913f, (float)-5.2093f, (float)-4.0f, (float)0.0807f, (float)-0.0334f, (float)0.3914f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(24, 59).addBox(-2.1913f, -0.7907f, -5.0f, 4.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.1913f, (float)-5.2093f, (float)-4.0f, (float)0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(0, 8).addBox(-2.0f, 0.5f, 12.4f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.5f, (float)-17.0f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition head_r6 = head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(0, 21).addBox(-2.0f, 0.5f, 3.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(0, 15).addBox(-5.0f, 0.5f, -5.0f, 10.0f, 2.0f, 10.0f, new CubeDeformation(-0.2f)).texOffs(0, 0).addBox(-5.0f, -2.5f, -5.0f, 10.0f, 5.0f, 10.0f, new CubeDeformation(-0.6f)), PartPose.offsetAndRotation((float)0.0f, (float)-6.5f, (float)0.0f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition head_r7 = head.addOrReplaceChild("head_r7", CubeListBuilder.create().texOffs(76, 8).addBox(0.0f, -0.5f, -1.5f, 1.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-5.5f, (float)-3.0f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)0.5236f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(32, 27).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(56, 41).addBox(-3.0f, 9.0f, 1.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(86, 0).addBox(-5.0f, 6.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.7f)).texOffs(86, 0).mirror().addBox(2.0f, 6.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(0, 97).addBox(-4.0f, 0.0f, 1.0f, 3.0f, 9.0f, 2.0f, new CubeDeformation(-0.7f)).texOffs(42, 98).addBox(-4.0f, 0.0f, -3.0f, 3.0f, 9.0f, 2.0f, new CubeDeformation(-0.7f)).texOffs(27, 98).addBox(-3.0f, 1.0f, -3.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.8f)).texOffs(27, 98).addBox(-3.0f, 6.0f, -3.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.8f)).texOffs(27, 98).addBox(-3.0f, 1.0f, 1.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.8f)).texOffs(92, 45).mirror().addBox(1.0f, 1.0f, 1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(58, 53).addBox(-4.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(92, 45).addBox(-4.0f, 1.0f, -3.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.4f)).texOffs(92, 45).addBox(-4.0f, 1.0f, 1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.4f)).texOffs(0, 97).mirror().addBox(1.0f, 0.0f, 1.0f, 3.0f, 9.0f, 2.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(58, 53).mirror().addBox(1.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).mirror(false).texOffs(92, 45).mirror().addBox(1.0f, 1.0f, -3.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(42, 98).mirror().addBox(1.0f, 0.0f, -3.0f, 3.0f, 9.0f, 2.0f, new CubeDeformation(-0.7f)).mirror(false), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(12, 66).mirror().addBox(-1.0f, -2.0f, -1.95f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(40, 68).mirror().addBox(-1.0f, -2.0f, -1.95f, 2.0f, 4.0f, 4.0f, new CubeDeformation(-0.1f)).mirror(false).texOffs(44, 43).mirror().addBox(-1.0f, -2.0f, -2.45f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(68, 24).mirror().addBox(-1.0f, -2.0f, 1.45f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(74, 24).mirror().addBox(-1.0f, 0.5f, -1.95f, 2.0f, 1.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false), PartPose.offsetAndRotation((float)5.0f, (float)9.0f, (float)-0.05f, (float)0.0f, (float)0.0f, (float)0.1309f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(76, 34).addBox(-1.0f, -2.25f, -0.9f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(30, 27).addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).texOffs(0, 66).addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(52, 0).addBox(-0.5f, -0.25f, -0.7f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-3.0f, (float)9.25f, (float)-3.7f, (float)0.0928f, (float)0.3477f, (float)0.0317f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(30, 27).mirror().addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(0, 66).mirror().addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offsetAndRotation((float)3.0f, (float)2.25f, (float)-3.7f, (float)0.0873f, (float)-0.0435f, (float)-0.0038f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-0.5f, -0.25f, -0.7f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(0, 66).mirror().addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(30, 27).mirror().addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(76, 34).mirror().addBox(-1.0f, -2.25f, -0.9f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)3.0f, (float)9.25f, (float)-3.7f, (float)0.0928f, (float)-0.3477f, (float)-0.0317f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(40, 68).addBox(-1.0f, -2.0f, -1.95f, 2.0f, 4.0f, 4.0f, new CubeDeformation(-0.1f)).texOffs(12, 66).addBox(-1.0f, -2.0f, -1.95f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(68, 24).addBox(-1.0f, -2.0f, 1.45f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(44, 43).addBox(-1.0f, -2.0f, -2.45f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(74, 24).addBox(-1.0f, 0.5f, -1.95f, 2.0f, 1.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offsetAndRotation((float)-5.0f, (float)9.0f, (float)-0.05f, (float)0.0f, (float)0.0f, (float)-0.1309f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(30, 66).addBox(-2.0f, -0.75f, -1.5f, 4.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(52, 71).mirror().addBox(-4.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(24, 73).addBox(1.0f, -0.75f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)).texOffs(70, 53).addBox(-2.0f, -1.75f, -1.5f, 4.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(33, 75).addBox(1.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).texOffs(33, 75).mirror().addBox(-2.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(52, 71).addBox(1.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).texOffs(24, 73).mirror().addBox(-4.0f, -0.75f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)).mirror(false), PartPose.offsetAndRotation((float)0.0f, (float)9.75f, (float)3.5f, (float)-0.1309f, (float)0.0f, (float)0.0f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 12).addBox(9.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 12).mirror().addBox(-13.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(74, 65).mirror().addBox(-2.0f, 0.0f, 0.0f, 4.0f, 4.0f, 1.0f, new CubeDeformation(0.25f)).mirror(false), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(74, 65).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 4.0f, 1.0f, new CubeDeformation(0.25f)).texOffs(64, 48).addBox(-2.0f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(64, 11).addBox(-2.0f, 2.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(0, 0).addBox(-4.0f, 1.0f, -1.0f, 2.0f, 5.0f, 3.0f, new CubeDeformation(-0.1f)).texOffs(24, 27).addBox(-4.0f, 2.0f, -2.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.2f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(90, 19).addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).texOffs(107, 27).addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(0, 43).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.4f)).texOffs(90, 19).mirror().addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(107, 27).mirror().addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.6f)).mirror(false), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)128, (int)128);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

