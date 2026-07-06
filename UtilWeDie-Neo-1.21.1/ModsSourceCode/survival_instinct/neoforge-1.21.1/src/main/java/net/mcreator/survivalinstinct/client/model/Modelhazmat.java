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

public class Modelhazmat<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelhazmat"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart right_shoe;
    public final ModelPart left_shoe;

    public Modelhazmat(ModelPart root) {
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
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.4f)).texOffs(0, 24).addBox(-5.0f, -3.0f, -1.0f, 10.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(29, 21).addBox(-4.0f, -5.0f, -5.0f, 8.0f, 4.0f, 3.0f, new CubeDeformation(0.45f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(99, 49).addBox(-1.6736f, -0.8573f, -1.3264f, 3.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(116, 62).addBox(-1.6736f, 0.1427f, -1.3264f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.2f)).texOffs(108, 68).addBox(-1.6736f, 1.6903f, -1.2839f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(108, 74).addBox(-1.6736f, 1.4903f, -1.3264f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.4f)), PartPose.offsetAndRotation((float)0.0f, (float)0.2025f, (float)-4.5425f, (float)-1.1111f, (float)0.4176f, (float)-0.6863f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(3.0f, 0.5f, 1.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.65f)).mirror(false).texOffs(0, 32).addBox(-4.0f, 0.5f, 1.5f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.65f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.5f, (float)-2.5f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(24, 0).addBox(-5.0f, -1.0f, -3.5f, 10.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-1.0f, (float)-2.5f, (float)0.1309f, (float)0.0f, (float)0.0f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(0, 95).addBox(-4.0f, -1.0f, -7.0f, 8.0f, 2.0f, 10.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)-2.0f, (float)-5.0f, (float)2.0f, (float)0.0f, (float)0.0f, (float)1.5708f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(0, 95).mirror().addBox(-4.0f, -1.0f, -7.0f, 8.0f, 2.0f, 10.0f, new CubeDeformation(-0.1f)).mirror(false), PartPose.offsetAndRotation((float)2.0f, (float)-5.0f, (float)2.0f, (float)0.0f, (float)0.0f, (float)-1.5708f));
        PartDefinition head_r6 = head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(27, 11).addBox(-5.0f, -1.5f, -3.5f, 10.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.5f, (float)-2.5f, (float)-0.1745f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(28, 28).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.15f)).texOffs(26, 18).addBox(-3.0f, 3.0f, 1.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(32, 7).addBox(-3.0f, 5.0f, 1.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(32, 9).addBox(-3.0f, 7.0f, 1.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(39, 19).addBox(-3.0f, 9.0f, 1.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(62, 61).addBox(-5.0f, 6.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.7f)).texOffs(60, 34).addBox(-5.0f, 3.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.7f)).texOffs(60, 34).mirror().addBox(2.0f, 3.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(62, 61).mirror().addBox(2.0f, 6.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(0, 73).addBox(-3.0f, 9.0f, -2.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(78, 13).addBox(-3.0f, 7.0f, -2.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(80, 66).addBox(-3.0f, 5.0f, -2.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(80, 68).addBox(-3.0f, 3.0f, -2.8f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(47, 85).addBox(-4.0f, 0.0f, 1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).texOffs(47, 85).mirror().addBox(1.0f, 0.0f, 1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(10, 86).addBox(-4.0f, 0.0f, -3.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).texOffs(10, 86).mirror().addBox(1.0f, 0.0f, -3.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(52, 5).addBox(-4.0f, 2.0f, 1.0f, 8.0f, 9.0f, 2.0f, new CubeDeformation(-0.4f)).texOffs(66, 10).addBox(1.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(48, 56).addBox(-4.0f, 2.0f, -3.0f, 8.0f, 9.0f, 2.0f, new CubeDeformation(-0.4f)).texOffs(66, 10).mirror().addBox(-4.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).mirror(false), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(64, 80).mirror().addBox(-1.0f, 3.5f, -2.0f, 3.0f, 3.0f, 3.0f, new CubeDeformation(-0.4f)).mirror(false), PartPose.offsetAndRotation((float)-2.0f, (float)4.5f, (float)5.0f, (float)0.0f, (float)0.7854f, (float)0.0f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(74, 70).mirror().addBox(-2.0f, -1.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(0, 75).mirror().addBox(-2.0f, -4.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.5f)).mirror(false), PartPose.offsetAndRotation((float)-2.0f, (float)8.5f, (float)4.0f, (float)0.0f, (float)-0.7854f, (float)0.0f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(64, 80).addBox(-2.0f, 3.5f, -2.0f, 3.0f, 3.0f, 3.0f, new CubeDeformation(-0.4f)), PartPose.offsetAndRotation((float)2.0f, (float)4.5f, (float)5.0f, (float)0.0f, (float)-0.7854f, (float)0.0f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(16, 56).mirror().addBox(-2.0f, -4.5f, -2.0f, 4.0f, 9.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(72, 34).mirror().addBox(-2.0f, -3.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offsetAndRotation((float)-2.0f, (float)4.5f, (float)4.0f, (float)0.0f, (float)0.7854f, (float)0.0f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(72, 34).addBox(-2.0f, -1.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.2f)).texOffs(16, 56).addBox(-2.0f, -2.5f, -2.0f, 4.0f, 9.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)2.0f, (float)2.5f, (float)4.0f, (float)0.0f, (float)-0.7854f, (float)0.0f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(74, 70).addBox(-2.0f, -1.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.2f)).texOffs(0, 75).addBox(-2.0f, -4.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.5f)), PartPose.offsetAndRotation((float)2.0f, (float)8.5f, (float)4.0f, (float)0.0f, (float)0.7854f, (float)0.0f));
        PartDefinition body_r7 = body.addOrReplaceChild("body_r7", CubeListBuilder.create().texOffs(84, 54).mirror().addBox(1.0f, -0.75f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)).mirror(false).texOffs(80, 49).addBox(-2.0f, -1.75f, -1.5f, 4.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(85, 83).mirror().addBox(-2.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(0, 80).addBox(-4.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).texOffs(52, 34).addBox(-2.0f, -0.75f, -1.5f, 4.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)3.75f, (float)-2.5f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r8 = body.addOrReplaceChild("body_r8", CubeListBuilder.create().texOffs(0, 80).mirror().addBox(1.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(85, 83).addBox(1.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).texOffs(84, 54).addBox(-4.0f, -0.75f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)0.0f, (float)3.75f, (float)-2.5f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r9 = body.addOrReplaceChild("body_r9", CubeListBuilder.create().texOffs(46, 7).mirror().addBox(-1.0f, -2.25f, -0.9f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(0, 6).mirror().addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(0, 16).mirror().addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(24, 32).mirror().addBox(-0.5f, -0.25f, -0.7f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)-3.0f, (float)9.25f, (float)-3.7f, (float)0.0928f, (float)0.3477f, (float)0.0317f));
        PartDefinition body_r10 = body.addOrReplaceChild("body_r10", CubeListBuilder.create().texOffs(16, 40).addBox(-0.5f, -0.25f, -0.7f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 24).addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(26, 20).addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).texOffs(64, 43).addBox(-1.0f, -2.25f, -0.9f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)9.25f, (float)-3.7f, (float)0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition body_r11 = body.addOrReplaceChild("body_r11", CubeListBuilder.create().texOffs(24, 32).addBox(-0.5f, -0.25f, -0.7f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 16).addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(0, 6).addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).texOffs(46, 7).addBox(-1.0f, -2.25f, -0.9f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.0f, (float)9.25f, (float)-3.7f, (float)0.0928f, (float)-0.3477f, (float)-0.0317f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(68, 28).mirror().addBox(-1.0f, 6.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(52, 18).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_arm_r1 = left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(94, 5).mirror().addBox(2.3f, -2.5f, -1.5f, 2.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(68, 52).mirror().addBox(-1.7f, -2.5f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.6f)).mirror(false).texOffs(107, 5).mirror().addBox(2.3f, -2.5f, -1.5f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.5f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)1.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition left_arm_r2 = left_arm.addOrReplaceChild("left_arm_r2", CubeListBuilder.create().texOffs(95, 17).mirror().addBox(-2.7f, -0.5f, -3.0f, 6.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)0.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 18).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(68, 28).addBox(-3.0f, 6.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm_r1 = right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(107, 5).addBox(-4.3f, -2.5f, -1.5f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.5f)).texOffs(94, 5).addBox(-4.3f, -2.5f, -1.5f, 2.0f, 5.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(68, 52).addBox(-2.3f, -2.5f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offsetAndRotation((float)-1.0f, (float)1.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition right_arm_r2 = right_arm.addOrReplaceChild("right_arm_r2", CubeListBuilder.create().texOffs(95, 17).addBox(-3.3f, -0.5f, -3.0f, 6.0f, 2.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.0f, (float)0.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 48).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(40, 76).mirror().addBox(-2.0f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition left_leg_r1 = left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(60, 70).mirror().addBox(-1.75f, -2.5f, -2.0f, 3.0f, 6.0f, 4.0f, new CubeDeformation(-0.1f)).mirror(false).texOffs(52, 77).mirror().addBox(-0.75f, -2.5f, -2.0f, 2.0f, 4.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false), PartPose.offsetAndRotation((float)2.75f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(40, 76).addBox(-2.0f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(0, 48).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(52, 77).addBox(-1.25f, -2.5f, -2.0f, 2.0f, 4.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(60, 70).addBox(-1.25f, -2.5f, -2.0f, 3.0f, 6.0f, 4.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)-2.75f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(97, 28).addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(97, 28).mirror().addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
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

