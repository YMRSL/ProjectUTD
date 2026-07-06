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

public class Modelrockie<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelrockie"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_shoe;
    public final ModelPart right_shoe;
    public final ModelPart right_leg;
    public final ModelPart left_leg;

    public Modelrockie(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.left_arm = root.getChild("left_arm");
        this.right_arm = root.getChild("right_arm");
        this.left_shoe = root.getChild("left_shoe");
        this.right_shoe = root.getChild("right_shoe");
        this.right_leg = root.getChild("right_leg");
        this.left_leg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 92).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.01f)).texOffs(0, 0).addBox(-5.0f, -9.0f, -5.0f, 10.0f, 5.0f, 10.0f, new CubeDeformation(-0.2f)).texOffs(40, 23).addBox(-3.0f, -7.0f, 4.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.2f)).texOffs(16, 41).addBox(-2.0f, -6.0f, 4.0f, 4.0f, 1.0f, 2.0f, new CubeDeformation(0.1f)).texOffs(16, 44).addBox(2.0f, -7.0f, 4.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(16, 44).mirror().addBox(-3.0f, -7.0f, 4.0f, 1.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(0, 38).addBox(-4.0f, -9.0f, -5.0f, 3.0f, 4.0f, 10.0f, new CubeDeformation(-0.1f)).texOffs(62, 51).addBox(-5.0f, -7.0f, -3.0f, 1.0f, 2.0f, 8.0f, new CubeDeformation(0.1f)).texOffs(62, 51).mirror().addBox(4.0f, -7.0f, -3.0f, 1.0f, 2.0f, 8.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(0, 38).mirror().addBox(1.0f, -9.0f, -5.0f, 3.0f, 4.0f, 10.0f, new CubeDeformation(-0.1f)).mirror(false).texOffs(56, 5).addBox(-5.0f, -9.0f, -1.0f, 10.0f, 3.0f, 2.0f, new CubeDeformation(0.3f)).texOffs(54, 0).addBox(-5.0f, -6.0f, -1.0f, 10.0f, 2.0f, 2.0f, new CubeDeformation(0.7f)).texOffs(0, 27).addBox(-5.0f, -5.0f, -5.0f, 10.0f, 1.0f, 10.0f, new CubeDeformation(0.0f)).texOffs(0, 15).addBox(-5.0f, -5.0f, -5.0f, 10.0f, 2.0f, 10.0f, new CubeDeformation(-0.4f)).texOffs(30, 0).addBox(-5.0f, -4.4f, 1.0f, 10.0f, 3.0f, 4.0f, new CubeDeformation(-0.2f)).texOffs(0, 15).addBox(-6.0f, -4.4f, -2.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.2f)).texOffs(0, 15).mirror().addBox(5.0f, -4.4f, -2.0f, 1.0f, 3.0f, 4.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(25, 57).mirror().addBox(-1.5f, -1.5f, -2.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)5.8f, (float)-1.9f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.9599f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(25, 57).addBox(-0.5f, -1.5f, -2.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-5.8f, (float)-1.9f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.9599f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(93, 42).mirror().addBox(1.0f, -0.1109f, -0.2599f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(93, 42).addBox(-2.0f, -0.1109f, -0.2599f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-9.1612f, (float)-5.441f, (float)-1.0472f, (float)0.0f, (float)0.0f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(104, 57).mirror().addBox(1.0f, -0.6606f, -0.1716f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.15f)).mirror(false).texOffs(106, 66).mirror().addBox(-3.0f, 1.1134f, -2.0951f, 2.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(106, 66).addBox(1.0f, 1.1134f, -2.0951f, 2.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(106, 66).mirror().addBox(-3.0f, -1.1224f, -2.0587f, 2.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(106, 66).addBox(1.0f, -1.1224f, -2.0587f, 2.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(107, 58).mirror().addBox(1.0f, -2.1224f, -2.0587f, 2.0f, 5.0f, 2.0f, new CubeDeformation(-0.2f)).mirror(false).texOffs(107, 58).addBox(-3.0f, -2.1224f, -2.0587f, 2.0f, 5.0f, 2.0f, new CubeDeformation(-0.2f)).texOffs(104, 57).addBox(-2.0f, -0.6606f, -0.1716f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(104, 54).addBox(-2.0f, -0.6606f, -0.1716f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-9.1612f, (float)-5.441f, (float)-1.7017f, (float)0.0f, (float)0.0f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(0, 7).addBox(-2.0f, -0.0369f, 1.5803f, 4.0f, 2.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-9.1612f, (float)-5.441f, (float)-0.7418f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(26, 43).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(64, 10).addBox(-4.0f, 2.0f, 2.0f, 8.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(12, 75).addBox(-4.0f, -0.8f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)).texOffs(72, 78).addBox(-5.0f, 3.2f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.4f)).texOffs(72, 78).addBox(-5.0f, 7.2f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.4f)).texOffs(32, 59).addBox(-4.0f, -0.8f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.65f)).texOffs(66, 39).addBox(-4.0f, 2.0f, -3.0f, 8.0f, 9.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(80, 4).addBox(-3.0f, 3.0f, -3.4f, 6.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(80, 4).addBox(-3.0f, 5.0f, -3.4f, 6.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(80, 4).addBox(-3.0f, 7.0f, -3.4f, 6.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(80, 4).addBox(-3.0f, 9.0f, -3.4f, 6.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(47, 44).addBox(-2.0f, 10.0f, -4.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.12f)).texOffs(47, 35).addBox(1.0f, 10.0f, -4.0f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.12f)).texOffs(1, 28).addBox(-2.5f, 7.0f, -4.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(63, 80).addBox(-2.5f, 7.0f, -4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.3f)).texOffs(63, 80).mirror().addBox(0.5f, 7.0f, -4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(31, 8).addBox(-2.5f, 8.0f, -4.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(57, 11).addBox(-2.5f, 7.0f, -4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(45, 60).addBox(0.5f, 7.0f, -4.0f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(31, 8).mirror().addBox(0.5f, 8.0f, -4.0f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(30, 27).addBox(0.5f, 7.0f, -3.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(1, 28).mirror().addBox(0.5f, 7.0f, -4.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(12, 75).mirror().addBox(1.0f, -0.8f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)).mirror(false).texOffs(32, 59).mirror().addBox(1.0f, -0.8f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.65f)).mirror(false).texOffs(72, 78).mirror().addBox(3.0f, 7.2f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(72, 78).mirror().addBox(3.0f, 3.2f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.4f)).mirror(false), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(47, 29).mirror().addBox(-1.0f, -2.5f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(31, 34).addBox(-1.0f, -1.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).texOffs(69, 21).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(23, 45).addBox(-0.5f, 0.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.12f)).texOffs(83, 9).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)4.0f, (float)9.5f, (float)-3.5f, (float)0.0f, (float)-0.3491f, (float)0.0f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(61, 111).mirror().addBox(-3.3f, 0.0f, 0.5f, 0.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(61, 111).addBox(3.3f, 0.0f, 0.5f, 0.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)0.0f, (float)4.0f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(58, 109).mirror().addBox(-5.0f, 0.0f, -2.0f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(58, 109).addBox(2.0f, 0.0f, -2.0f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)), PartPose.offsetAndRotation((float)0.0f, (float)2.0f, (float)5.0f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(17, 54).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(31, 34).mirror().addBox(-1.0f, -1.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(51, 36).addBox(-0.5f, 0.5f, -0.5f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.12f)).texOffs(83, 9).mirror().addBox(-1.0f, -2.5f, -0.5f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(47, 29).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-4.0f, (float)9.5f, (float)-3.5f, (float)0.0f, (float)0.3491f, (float)0.0f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(96, 13).addBox(1.0f, -1.5f, -0.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.2f)).texOffs(96, 13).addBox(4.0f, -1.5f, -0.5f, 1.0f, 3.0f, 2.0f, new CubeDeformation(0.2f)).texOffs(97, 6).addBox(0.0f, -2.5f, -0.5f, 6.0f, 3.0f, 2.0f, new CubeDeformation(0.3f)).texOffs(101, 16).addBox(0.0f, -2.5f, -0.5f, 6.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-3.0f, (float)12.5f, (float)2.5f, (float)-0.1745f, (float)0.0f, (float)0.0f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(40, 115).addBox(-3.0f, -4.0f, 0.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.2f)).texOffs(40, 108).addBox(-3.0f, 0.0f, 0.0f, 6.0f, 5.0f, 2.0f, new CubeDeformation(-0.2f)).texOffs(58, 100).mirror().addBox(-5.0f, 0.0f, -3.0f, 3.0f, 4.0f, 4.0f, new CubeDeformation(-0.2f)).mirror(false).texOffs(58, 100).addBox(2.0f, 0.0f, -3.0f, 3.0f, 4.0f, 4.0f, new CubeDeformation(-0.2f)).texOffs(38, 94).addBox(-3.0f, -5.0f, -3.0f, 6.0f, 10.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)5.0f, (float)5.0f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 112).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 112).mirror().addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(0, 53).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(0, 53).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)).mirror(false), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(83, 105).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(107, 80).addBox(-4.0f, 1.0f, -1.0f, 3.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)).texOffs(101, 90).addBox(-3.0f, 1.0f, -3.0f, 6.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(101, 99).addBox(-2.0f, -1.0f, -3.0f, 3.0f, 7.0f, 6.0f, new CubeDeformation(-0.6f)).texOffs(83, 96).addBox(-3.0f, 4.0f, -3.0f, 6.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(83, 105).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)128, (int)128);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

