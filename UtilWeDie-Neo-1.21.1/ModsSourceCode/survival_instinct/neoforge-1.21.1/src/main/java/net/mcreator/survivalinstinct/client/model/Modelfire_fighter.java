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

public class Modelfire_fighter<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelfire_fighter"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart right_shoe;
    public final ModelPart left_shoe;

    public Modelfire_fighter(ModelPart root) {
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
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 112).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)).texOffs(24, 22).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 3.0f, 8.0f, new CubeDeformation(0.6f)).texOffs(24, 14).addBox(-5.0f, -9.0f, -1.0f, 10.0f, 4.0f, 2.0f, new CubeDeformation(0.1f)).texOffs(24, 33).addBox(-1.0f, -9.0f, -5.0f, 2.0f, 4.0f, 10.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-5.0f, -0.5f, -6.5f, 10.0f, 1.0f, 13.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)0.0f, (float)-4.5f, (float)0.5f, (float)-0.0433f, (float)-0.0018f, (float)0.0052f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(39, 38).mirror().addBox(-0.5f, -2.0f, -4.5f, 1.0f, 4.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)5.0529f, (float)-2.2044f, (float)-0.5f, (float)-0.0873f, (float)0.0f, (float)-0.2618f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(39, 38).addBox(-0.5f, -2.0f, -4.5f, 1.0f, 4.0f, 9.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-5.0529f, (float)-2.2044f, (float)-0.5f, (float)-0.0873f, (float)0.0f, (float)0.2618f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(60, 50).addBox(-4.0f, 4.0f, -0.5f, 8.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.0f, (float)3.5f, (float)0.1745f, (float)0.0f, (float)0.0f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -0.5f, -6.5f, 10.0f, 1.0f, 13.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.5f, (float)0.5f, (float)-0.0433f, (float)0.0018f, (float)-0.0052f));
        PartDefinition head_r6 = head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(66, 30).addBox(-3.0f, -3.0f, -1.0f, 6.0f, 5.0f, 2.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-6.8f, (float)-5.0f, (float)0.1745f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 30).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(56, 3).addBox(-5.0f, 6.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.6f)).texOffs(44, 0).addBox(-5.0f, 3.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.6f)).texOffs(44, 0).mirror().addBox(2.0f, 3.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.6f)).mirror(false).texOffs(56, 3).mirror().addBox(2.0f, 6.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.6f)).mirror(false).texOffs(78, 45).addBox(-4.0f, 0.0f, 1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).texOffs(80, 26).addBox(-4.0f, 0.0f, -3.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).texOffs(80, 26).mirror().addBox(1.0f, 0.0f, -3.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).mirror(false).texOffs(12, 63).addBox(-3.0f, 3.0f, -3.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.6f)).texOffs(0, 62).addBox(-4.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(70, 21).addBox(-3.0f, 3.0f, 1.0f, 6.0f, 3.0f, 2.0f, new CubeDeformation(-0.6f)).texOffs(0, 62).mirror().addBox(1.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).mirror(false).texOffs(78, 45).mirror().addBox(1.0f, 0.0f, 1.0f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.7f)).mirror(false), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(72, 38).mirror().addBox(1.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).mirror(false), PartPose.offsetAndRotation((float)-2.0f, (float)8.75f, (float)-2.5f, (float)0.134f, (float)0.2608f, (float)0.0233f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(38, 33).mirror().addBox(-1.2f, -4.5f, -1.0f, 3.0f, 8.0f, 2.0f, new CubeDeformation(-0.4f)).mirror(false).texOffs(0, 0).mirror().addBox(-1.2f, -4.5f, -5.0f, 3.0f, 8.0f, 2.0f, new CubeDeformation(-0.4f)).mirror(false), PartPose.offsetAndRotation((float)2.5f, (float)5.5f, (float)2.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(42, 62).mirror().addBox(-0.5f, -0.5f, -2.0f, 3.0f, 7.0f, 4.0f, new CubeDeformation(0.5f)).mirror(false), PartPose.offsetAndRotation((float)1.5f, (float)11.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.3491f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5f, -1.25f, -1.5f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.3f)).texOffs(80, 16).addBox(-1.5f, -2.25f, -1.5f, 3.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(10, 75).addBox(-1.5f, -2.25f, -0.5f, 3.0f, 6.0f, 2.0f, new CubeDeformation(-0.4f)), PartPose.offsetAndRotation((float)3.7553f, (float)3.1755f, (float)-3.2673f, (float)-0.1479f, (float)-0.4025f, (float)-0.0311f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(0, 0).addBox(-1.8f, -4.5f, -1.0f, 3.0f, 8.0f, 2.0f, new CubeDeformation(-0.4f)).texOffs(38, 33).addBox(-1.8f, -4.5f, 3.0f, 3.0f, 8.0f, 2.0f, new CubeDeformation(-0.4f)), PartPose.offsetAndRotation((float)-2.5f, (float)5.5f, (float)-2.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(27, 73).addBox(1.2443f, -1.7486f, -1.5323f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).texOffs(57, 82).addBox(1.2443f, -0.7486f, -1.5323f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)).texOffs(70, 55).addBox(-1.7557f, -0.7486f, -1.5323f, 4.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(40, 80).addBox(1.7443f, -1.7486f, -1.5323f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).texOffs(72, 65).addBox(-1.7557f, -1.7486f, -1.5323f, 4.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(33, 0).addBox(-2.2557f, -1.7486f, -1.5323f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).texOffs(72, 38).addBox(-3.7557f, -1.7486f, -1.5323f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).texOffs(61, 82).addBox(-3.7557f, -0.7486f, -1.5323f, 2.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)-0.2443f, (float)8.75f, (float)2.4678f, (float)-3.0334f, (float)-3.0E-4f, (float)3.1399f));
        PartDefinition body_r7 = body.addOrReplaceChild("body_r7", CubeListBuilder.create().texOffs(27, 73).addBox(1.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).texOffs(40, 80).addBox(1.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)), PartPose.offsetAndRotation((float)2.0f, (float)8.75f, (float)-2.5f, (float)0.134f, (float)-0.2608f, (float)-0.0233f));
        PartDefinition body_r8 = body.addOrReplaceChild("body_r8", CubeListBuilder.create().texOffs(61, 82).addBox(-4.0f, -0.75f, -1.5f, 2.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)).texOffs(57, 82).addBox(1.0f, -0.75f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)).texOffs(72, 65).addBox(-2.0f, -1.75f, -1.5f, 4.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(33, 0).addBox(-2.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).texOffs(70, 55).addBox(-2.0f, -0.75f, -1.5f, 4.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).texOffs(72, 38).addBox(-4.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)), PartPose.offsetAndRotation((float)2.0f, (float)8.75f, (float)-2.5f, (float)0.134f, (float)-0.2608f, (float)-0.0233f));
        PartDefinition body_r9 = body.addOrReplaceChild("body_r9", CubeListBuilder.create().texOffs(74, 6).addBox(-2.0f, -1.75f, -1.5f, 4.0f, 2.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(0, 14).mirror().addBox(-2.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(51, 74).addBox(-4.0f, -1.75f, -1.5f, 3.0f, 4.0f, 3.0f, new CubeDeformation(-0.4f)).texOffs(69, 71).addBox(-2.0f, -0.75f, -1.5f, 4.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)3.75f, (float)-2.5f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r10 = body.addOrReplaceChild("body_r10", CubeListBuilder.create().texOffs(0, 14).addBox(1.5f, -1.75f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.4f)).texOffs(75, 77).addBox(-4.0f, -0.75f, -1.5f, 3.0f, 2.0f, 3.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)0.0f, (float)3.75f, (float)-2.5f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r11 = body.addOrReplaceChild("body_r11", CubeListBuilder.create().texOffs(12, 46).addBox(-1.0f, -2.25f, -0.9f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(4, 10).addBox(-1.0f, -1.25f, -0.1f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).texOffs(44, 0).addBox(-1.0f, -2.25f, -0.1f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-3.0f, (float)9.25f, (float)-3.7f, (float)0.0983f, (float)0.478f, (float)0.0453f));
        PartDefinition body_r12 = body.addOrReplaceChild("body_r12", CubeListBuilder.create().texOffs(0, 30).addBox(-0.5f, -0.25f, -0.7f, 1.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-3.0f, (float)9.25f, (float)-3.7f, (float)0.0928f, (float)0.3477f, (float)0.0317f));
        PartDefinition body_r13 = body.addOrReplaceChild("body_r13", CubeListBuilder.create().texOffs(42, 62).addBox(-2.5f, -0.5f, -2.0f, 3.0f, 7.0f, 4.0f, new CubeDeformation(0.5f)), PartPose.offsetAndRotation((float)-1.5f, (float)11.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.3491f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(14, 69).mirror().addBox(-1.0f, 7.0f, -2.0f, 4.0f, 2.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(50, 33).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(93, 0).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_arm_r1 = left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(56, 65).mirror().addBox(-1.7f, -2.5f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.6f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)1.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(50, 33).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 10.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(93, 0).mirror().addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false).texOffs(14, 69).addBox(-3.0f, 7.0f, -2.0f, 4.0f, 2.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm_r1 = right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(56, 65).addBox(-2.3f, -2.5f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offsetAndRotation((float)-1.0f, (float)1.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)).mirror(false), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 46).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.25f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(32, 51).addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.4f)).texOffs(90, 57).addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).texOffs(86, 34).addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.49f)), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(86, 34).mirror().addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.49f)).mirror(false).texOffs(90, 57).mirror().addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(32, 51).mirror().addBox(-2.0f, 5.0f, -2.0f, 4.0f, 7.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
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

