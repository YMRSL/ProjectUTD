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

public class Modelhunter<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelhunter"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_shoe;
    public final ModelPart right_shoe;
    public final ModelPart left_leg;
    public final ModelPart right_leg;

    public Modelhunter(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.left_arm = root.getChild("left_arm");
        this.right_arm = root.getChild("right_arm");
        this.left_shoe = root.getChild("left_shoe");
        this.right_shoe = root.getChild("right_shoe");
        this.left_leg = root.getChild("left_leg");
        this.right_leg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 12).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.1f)).texOffs(17, 91).addBox(-5.0f, -9.0f, -5.0f, 10.0f, 5.0f, 10.0f, new CubeDeformation(-0.2f)).texOffs(24, 106).addBox(-5.0f, -9.0f, -2.0f, 10.0f, 5.0f, 3.0f, new CubeDeformation(0.2f)).texOffs(74, 26).addBox(-3.0f, -8.0f, -5.0f, 6.0f, 4.0f, 3.0f, new CubeDeformation(-0.6f)).texOffs(28, 51).addBox(-6.0f, -7.0f, -2.5f, 2.0f, 3.0f, 8.0f, new CubeDeformation(-0.4f)).texOffs(50, 47).addBox(4.0f, -7.0f, -2.5f, 2.0f, 3.0f, 8.0f, new CubeDeformation(-0.4f)).texOffs(0, 69).addBox(3.5f, -5.0f, -3.0f, 3.0f, 5.0f, 5.0f, new CubeDeformation(-0.6f)).texOffs(55, 67).addBox(-6.5f, -5.0f, -3.0f, 3.0f, 5.0f, 5.0f, new CubeDeformation(-0.6f)).texOffs(0, 0).addBox(-5.0f, -5.8f, -5.0f, 10.0f, 1.0f, 10.0f, new CubeDeformation(0.0f)).texOffs(30, 0).addBox(-5.0f, -5.2f, 0.0f, 10.0f, 4.0f, 5.0f, new CubeDeformation(-0.4f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r1 = head.addOrReplaceChild("visor_r1", CubeListBuilder.create().texOffs(62, 49).addBox(-2.0f, -0.713f, -2.0102f, 4.0f, 3.0f, 3.0f, new CubeDeformation(-0.6f)), PartPose.offsetAndRotation((float)0.0f, (float)-7.2628f, (float)-4.1938f, (float)0.2618f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r2 = head.addOrReplaceChild("visor_r2", CubeListBuilder.create().texOffs(54, 58).addBox(-1.0f, -1.4305f, -3.0018f, 2.0f, 2.0f, 4.0f, new CubeDeformation(-0.6f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.8727f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r3 = head.addOrReplaceChild("visor_r3", CubeListBuilder.create().texOffs(24, 12).addBox(-4.0f, 1.3031f, -3.5191f, 8.0f, 2.0f, 3.0f, new CubeDeformation(-0.5f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r4 = head.addOrReplaceChild("visor_r4", CubeListBuilder.create().texOffs(76, 75).addBox(0.9109f, 2.0163f, -5.6283f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)).texOffs(38, 17).addBox(0.9109f, 2.0163f, -2.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(12, 44).addBox(0.9109f, 2.0163f, -4.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(18, 44).addBox(0.7809f, 1.9292f, -6.616f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 0).addBox(0.7809f, 1.9292f, -7.0335f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0457f, (float)-0.3051f, (float)-0.0138f));
        PartDefinition visor_r5 = head.addOrReplaceChild("visor_r5", CubeListBuilder.create().texOffs(55, 77).addBox(-0.2981f, 2.0163f, -4.9525f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)).texOffs(46, 36).addBox(-0.2981f, 2.0163f, -1.9525f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(0, 4).addBox(-0.2981f, 1.9292f, -6.3662f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(22, 59).addBox(-0.2981f, 1.9292f, -5.9487f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0438f, (float)-0.0872f, (float)-0.0038f));
        PartDefinition visor_r6 = head.addOrReplaceChild("visor_r6", CubeListBuilder.create().texOffs(48, 25).addBox(0.0f, 2.0163f, -3.8782f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(36, 48).addBox(0.0f, 1.9292f, -5.8744f, 2.0f, 2.0f, 1.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition visor_r7 = head.addOrReplaceChild("visor_r7", CubeListBuilder.create().texOffs(27, 78).addBox(-1.7019f, 2.0163f, -4.9525f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)).texOffs(40, 55).addBox(-1.7019f, 2.0163f, -1.9525f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(58, 42).addBox(-1.7019f, 2.0163f, -3.9525f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(22, 59).addBox(-1.7019f, 1.9292f, -5.9487f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 12).addBox(-1.7019f, 1.9292f, -6.3662f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0438f, (float)0.0872f, (float)0.0038f));
        PartDefinition visor_r8 = head.addOrReplaceChild("visor_r8", CubeListBuilder.create().texOffs(0, 16).addBox(-2.7809f, 1.9292f, -7.0335f, 2.0f, 2.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(60, 10).addBox(-2.7809f, 1.9292f, -6.616f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(36, 62).addBox(-2.9109f, 2.0163f, -4.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(64, 13).addBox(-2.9109f, 2.0163f, -2.6283f, 2.0f, 2.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(0, 79).addBox(-2.9109f, 2.0163f, -5.6283f, 2.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.0457f, (float)0.3051f, (float)0.0138f));
        PartDefinition visor_r9 = head.addOrReplaceChild("visor_r9", CubeListBuilder.create().texOffs(0, 60).addBox(-2.0f, -0.5344f, -4.0628f, 4.0f, 2.0f, 3.0f, new CubeDeformation(-0.5f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.4775f, (float)-4.8897f, (float)0.2618f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(44, 73).addBox(-1.0f, -0.5f, -2.5f, 3.0f, 3.0f, 5.0f, new CubeDeformation(-0.7f)), PartPose.offsetAndRotation((float)-5.5f, (float)-2.5f, (float)-0.5f, (float)0.0f, (float)0.0f, (float)0.5236f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(23, 64).addBox(-2.0f, -2.5f, -2.5f, 3.0f, 5.0f, 5.0f, new CubeDeformation(-0.7f)), PartPose.offsetAndRotation((float)5.5f, (float)-2.5f, (float)-0.5f, (float)-0.001f, (float)0.0089f, (float)-0.5236f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(70, 19).addBox(-3.0f, 3.0f, -3.3f, 6.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(55, 30).addBox(-4.0f, 2.0f, -3.0f, 8.0f, 9.0f, 1.0f, new CubeDeformation(-0.2f)).texOffs(72, 4).addBox(-3.0f, 5.0f, -3.3f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(70, 19).addBox(-3.0f, 3.0f, -3.0f, 6.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(72, 4).addBox(-3.0f, 5.0f, -3.0f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(65, 71).addBox(3.0f, 3.0f, -3.0f, 2.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(70, 49).addBox(3.0f, 7.0f, -3.0f, 2.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(64, 10).addBox(-5.0f, 3.0f, -3.0f, 2.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(33, 69).addBox(-5.0f, 7.0f, -3.0f, 2.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(72, 0).addBox(-3.0f, 7.0f, -3.0f, 6.0f, 3.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(62, 40).addBox(-4.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(60, 0).addBox(-4.0f, -1.0f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.65f)).texOffs(10, 59).addBox(1.0f, -1.0f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.65f)).texOffs(60, 58).addBox(1.0f, -1.0f, -3.0f, 3.0f, 3.0f, 6.0f, new CubeDeformation(-0.5f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(55, 0).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(42, 62).addBox(-1.0f, 0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(0, 65).addBox(-1.0f, -1.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)), PartPose.offsetAndRotation((float)3.0f, (float)4.5f, (float)-3.1f, (float)0.0481f, (float)-0.4359f, (float)-0.0203f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-0.75f, -2.2704f, 0.1159f, 1.0f, 2.0f, 0.0f, new CubeDeformation(0.0f)).texOffs(43, 9).addBox(-0.75f, -0.2296f, -0.6159f, 2.0f, 3.0f, 1.0f, new CubeDeformation(0.1f)), PartPose.offsetAndRotation((float)-3.25f, (float)2.2267f, (float)-3.0832f, (float)0.0426f, (float)0.0094f, (float)-0.218f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(55, 0).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.1f)).texOffs(60, 4).addBox(-1.0f, 0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(34, 65).addBox(-1.0f, -1.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)), PartPose.offsetAndRotation((float)4.0f, (float)9.5f, (float)-3.1f, (float)0.0517f, (float)-0.5666f, (float)-0.0278f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(0, 8).addBox(-1.0f, 0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(0, 67).addBox(-1.0f, -1.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.15f)).texOffs(55, 0).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.1f)), PartPose.offsetAndRotation((float)-4.0f, (float)9.5f, (float)-3.1f, (float)0.0569f, (float)0.6973f, (float)0.0366f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(42, 58).addBox(-1.5f, -2.25f, -3.0f, 3.0f, 5.0f, 6.0f, new CubeDeformation(-0.6f)).texOffs(58, 19).addBox(-1.5f, -2.25f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.5f)), PartPose.offsetAndRotation((float)-4.9924f, (float)8.1632f, (float)0.0f, (float)-3.1416f, (float)0.0f, (float)2.9671f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(58, 19).addBox(-2.0f, -2.25f, -3.0f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.5f)).texOffs(42, 58).addBox(-2.0f, -2.25f, -3.0f, 3.0f, 5.0f, 6.0f, new CubeDeformation(-0.6f)), PartPose.offsetAndRotation((float)5.5f, (float)8.25f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition body_r7 = body.addOrReplaceChild("body_r7", CubeListBuilder.create().texOffs(42, 58).addBox(-2.0f, -2.25f, -3.5f, 3.0f, 5.0f, 6.0f, new CubeDeformation(-0.6f)).texOffs(97, 4).addBox(-2.0f, -2.25f, -3.5f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(58, 19).addBox(-2.0f, -2.25f, -3.5f, 3.0f, 4.0f, 6.0f, new CubeDeformation(-0.5f)), PartPose.offsetAndRotation((float)0.5f, (float)9.25f, (float)-4.0f, (float)1.5765f, (float)1.3092f, (float)1.5651f));
        PartDefinition body_r8 = body.addOrReplaceChild("body_r8", CubeListBuilder.create().texOffs(97, 4).addBox(-2.0f, -2.25f, -3.5f, 3.0f, 4.0f, 2.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)4.5f, (float)9.25f, (float)-4.0f, (float)1.5765f, (float)1.3092f, (float)1.5651f));
        PartDefinition body_r9 = body.addOrReplaceChild("body_r9", CubeListBuilder.create().texOffs(72, 4).addBox(-3.0f, 0.0f, -1.25f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(70, 19).addBox(-3.0f, -2.0f, -1.25f, 6.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(72, 4).addBox(-3.0f, -4.0f, -1.25f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(55, 30).addBox(-4.0f, -7.0f, -1.05f, 8.0f, 9.0f, 1.0f, new CubeDeformation(-0.2f)).texOffs(70, 19).addBox(-3.0f, -6.0f, -1.25f, 6.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(72, 4).addBox(-3.0f, -4.0f, -1.25f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(70, 19).addBox(-3.0f, -6.0f, -1.25f, 6.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)9.0f, (float)2.25f, (float)0.0f, (float)3.1416f, (float)0.0f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(48, 9).addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 44).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(20, 43).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(0, 110).addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).texOffs(0, 112).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(20, 43).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.4f)).texOffs(0, 112).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.6f)).mirror(false).texOffs(0, 110).mirror().addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(93, 101).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(93, 101).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(87, 117).addBox(-2.0f, 0.0f, 0.0f, 4.0f, 4.0f, 1.0f, new CubeDeformation(0.25f)).texOffs(101, 90).addBox(-2.0f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(101, 85).addBox(-2.0f, 2.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(106, 114).addBox(-4.0f, 1.0f, -1.5f, 2.0f, 5.0f, 3.0f, new CubeDeformation(-0.1f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)128, (int)128);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_shoe.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

