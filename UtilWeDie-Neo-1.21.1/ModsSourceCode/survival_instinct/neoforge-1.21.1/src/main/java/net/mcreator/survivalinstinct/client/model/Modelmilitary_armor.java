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

public class Modelmilitary_armor<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelmilitary_armor"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart left_shoe;
    public final ModelPart right_shoe;

    public Modelmilitary_armor(ModelPart root) {
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
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)).texOffs(0, 16).addBox(-4.0f, -9.0f, -4.0f, 8.0f, 1.0f, 8.0f, new CubeDeformation(0.0f)).texOffs(62, 52).addBox(-4.0f, -8.0f, 4.0f, 8.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(72, 48).addBox(-4.0f, -8.0f, -5.0f, 8.0f, 3.0f, 1.0f, new CubeDeformation(0.05f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(1, 79).mirror().addBox(-0.4163f, 0.7392f, 1.0f, 1.0f, 3.0f, 3.0f, new CubeDeformation(-0.01f)).mirror(false).texOffs(0, 16).addBox(-0.4163f, 0.7392f, 1.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(-0.01f)).texOffs(44, 45).addBox(-0.4163f, -1.2608f, -4.0f, 1.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)4.4163f, (float)-6.7392f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(18, 80).addBox(-5.2222f, -0.6075f, -0.5f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.412f, (float)0.912f, (float)-0.6504f, (float)-0.0795f, (float)-0.1041f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(22, 80).addBox(4.2222f, -0.6075f, -0.5f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.412f, (float)0.912f, (float)-0.6504f, (float)0.0795f, (float)0.1041f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(44, 56).addBox(4.0907f, -0.1044f, -2.25f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-2.506f, (float)3.25f, (float)-0.2615f, (float)-0.0076f, (float)-0.0869f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(66, 62).addBox(-0.3353f, 0.4848f, -1.5f, 1.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)5.2091f, (float)-5.5708f, (float)-1.5f, (float)0.0f, (float)0.0f, (float)0.1309f));
        PartDefinition head_r6 = head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(67, 25).addBox(-0.4042f, -1.4939f, -1.5f, 1.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)5.2091f, (float)-5.5708f, (float)-1.5f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition head_r7 = head.addOrReplaceChild("head_r7", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-0.5837f, 0.7392f, -1.0f, 1.0f, 2.0f, 3.0f, new CubeDeformation(-0.01f)).mirror(false).texOffs(0, 0).addBox(-0.5837f, 0.7392f, -1.0f, 1.0f, 3.0f, 3.0f, new CubeDeformation(-0.01f)).texOffs(12, 44).addBox(-0.5837f, -1.2608f, -6.0f, 1.0f, 3.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-4.4163f, (float)-6.7392f, (float)2.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition head_r8 = head.addOrReplaceChild("head_r8", CubeListBuilder.create().texOffs(57, 59).addBox(0.2837f, -2.2608f, 2.0f, 1.0f, 3.0f, 7.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)4.4163f, (float)-4.7392f, (float)-5.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition head_r9 = head.addOrReplaceChild("head_r9", CubeListBuilder.create().texOffs(42, 66).addBox(-0.5f, -0.5f, -2.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)4.5907f, (float)-4.5f, (float)-1.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition head_r10 = head.addOrReplaceChild("head_r10", CubeListBuilder.create().texOffs(24, 16).addBox(-0.5958f, -1.4939f, -1.5f, 1.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-5.2091f, (float)-5.5708f, (float)-1.5f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition head_r11 = head.addOrReplaceChild("head_r11", CubeListBuilder.create().texOffs(58, 21).addBox(-1.2837f, -2.2608f, 2.0f, 1.0f, 3.0f, 7.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)-4.4163f, (float)-4.7392f, (float)-5.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition head_r12 = head.addOrReplaceChild("head_r12", CubeListBuilder.create().texOffs(48, 66).addBox(-0.6647f, 0.4848f, -1.5f, 1.0f, 1.0f, 7.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-5.2091f, (float)-5.5708f, (float)-1.5f, (float)0.0f, (float)0.0f, (float)-0.1309f));
        PartDefinition head_r13 = head.addOrReplaceChild("head_r13", CubeListBuilder.create().texOffs(0, 25).addBox(0.1972f, -1.5f, -0.13f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.13f)), PartPose.offsetAndRotation((float)-4.1972f, (float)-6.5038f, (float)-4.87f, (float)0.0f, (float)-0.6545f, (float)0.0f));
        PartDefinition head_r14 = head.addOrReplaceChild("head_r14", CubeListBuilder.create().texOffs(24, 16).addBox(0.2f, -3.0f, -0.1f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-4.5f, (float)-5.0f, (float)3.5f, (float)0.0f, (float)-0.7418f, (float)0.0f));
        PartDefinition head_r15 = head.addOrReplaceChild("head_r15", CubeListBuilder.create().texOffs(33, 20).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)3.9658f, (float)-4.5038f, (float)-4.1154f, (float)0.0983f, (float)0.478f, (float)0.0453f));
        PartDefinition head_r16 = head.addOrReplaceChild("head_r16", CubeListBuilder.create().texOffs(32, 0).addBox(-1.1972f, -1.5f, -0.13f, 1.0f, 3.0f, 1.0f, new CubeDeformation(0.13f)), PartPose.offsetAndRotation((float)4.1972f, (float)-6.5038f, (float)-4.87f, (float)0.0f, (float)0.6545f, (float)0.0f));
        PartDefinition head_r17 = head.addOrReplaceChild("head_r17", CubeListBuilder.create().texOffs(0, 6).addBox(1.9489f, -0.494f, 3.1245f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-2.506f, (float)3.25f, (float)-0.1181f, (float)0.7383f, (float)-0.0797f));
        PartDefinition head_r18 = head.addOrReplaceChild("head_r18", CubeListBuilder.create().texOffs(32, 47).addBox(-1.2f, -3.0f, -0.1f, 1.0f, 6.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)4.5f, (float)-5.0f, (float)3.5f, (float)0.0f, (float)0.7418f, (float)0.0f));
        PartDefinition head_r19 = head.addOrReplaceChild("head_r19", CubeListBuilder.create().texOffs(22, 47).addBox(-0.3742f, -0.5f, -4.0f, 1.0f, 1.0f, 8.0f, new CubeDeformation(0.1f)), PartPose.offsetAndRotation((float)-4.1258f, (float)-8.0425f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.8727f));
        PartDefinition head_r20 = head.addOrReplaceChild("head_r20", CubeListBuilder.create().texOffs(24, 66).addBox(-0.5f, -0.5f, -2.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)-4.5907f, (float)-4.5f, (float)-1.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition head_r21 = head.addOrReplaceChild("head_r21", CubeListBuilder.create().texOffs(44, 56).mirror().addBox(-5.0907f, -0.1044f, -2.25f, 1.0f, 1.0f, 3.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offsetAndRotation((float)0.0f, (float)-2.506f, (float)3.25f, (float)-0.2615f, (float)0.0076f, (float)0.0869f));
        PartDefinition head_r22 = head.addOrReplaceChild("head_r22", CubeListBuilder.create().texOffs(56, 21).addBox(-1.5f, 0.0f, -0.5f, 3.0f, 2.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-7.5f, (float)-4.5f, (float)-0.1745f, (float)0.0f, (float)0.0f));
        PartDefinition head_r23 = head.addOrReplaceChild("head_r23", CubeListBuilder.create().texOffs(75, 67).addBox(-4.0f, -0.5f, 0.0f, 8.0f, 1.0f, 1.0f, new CubeDeformation(0.1f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.5f, (float)-4.5f, (float)-0.5672f, (float)0.0f, (float)0.0f));
        PartDefinition head_r24 = head.addOrReplaceChild("head_r24", CubeListBuilder.create().texOffs(78, 0).addBox(-4.0f, -0.5f, -0.5f, 8.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.5f, (float)-4.5f, (float)0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition head_r25 = head.addOrReplaceChild("head_r25", CubeListBuilder.create().texOffs(75, 65).addBox(-4.0f, -0.494f, 0.75f, 8.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-2.506f, (float)3.25f, (float)-0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition head_r26 = head.addOrReplaceChild("head_r26", CubeListBuilder.create().texOffs(76, 30).addBox(-4.0f, -0.5f, -0.8172f, 8.0f, 1.0f, 1.0f, new CubeDeformation(0.18f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.2172f, (float)4.2172f, (float)0.7418f, (float)0.0f, (float)0.0f));
        PartDefinition head_r27 = head.addOrReplaceChild("head_r27", CubeListBuilder.create().texOffs(48, 24).addBox(-0.6258f, -0.5f, -4.0f, 1.0f, 1.0f, 8.0f, new CubeDeformation(0.1f)), PartPose.offsetAndRotation((float)4.1258f, (float)-8.0425f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.8727f));
        PartDefinition head_r28 = head.addOrReplaceChild("head_r28", CubeListBuilder.create().texOffs(54, 44).addBox(-0.5f, -0.5f, -0.5f, 1.0f, 1.0f, 2.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)-3.9658f, (float)-4.5038f, (float)-4.1154f, (float)0.0983f, (float)-0.478f, (float)-0.0453f));
        PartDefinition head_r29 = head.addOrReplaceChild("head_r29", CubeListBuilder.create().texOffs(4, 6).addBox(-2.9489f, -0.494f, 3.1245f, 1.0f, 1.0f, 1.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)0.0f, (float)-2.506f, (float)3.25f, (float)-0.1181f, (float)-0.7383f, (float)0.0797f));
        PartDefinition head_r30 = head.addOrReplaceChild("head_r30", CubeListBuilder.create().texOffs(18, 41).addBox(-4.0f, 4.9969f, -0.5f, 8.0f, 1.0f, 1.0f, new CubeDeformation(-0.001f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.412f, (float)0.912f, (float)-0.6545f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 25).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(0, 41).addBox(-4.0f, -4.5015f, -1.4827f, 8.0f, 9.0f, 2.0f, new CubeDeformation(-0.15f)).texOffs(78, 2).addBox(-3.0f, -2.0015f, -0.4827f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(22, 45).addBox(-3.0f, -3.5015f, -0.4827f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5015f, (float)2.4173f, (float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(68, 33).addBox(-3.5f, -1.1167f, -0.2176f, 7.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(40, 33).addBox(-3.0f, -0.5167f, -0.0176f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)).texOffs(40, 35).addBox(-3.0f, 0.9833f, -0.0176f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)).texOffs(22, 43).addBox(-3.0f, 2.4833f, -0.0176f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5015f, (float)2.4173f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(68, 33).addBox(-3.5f, -2.6f, -0.6f, 7.0f, 5.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(22, 43).addBox(-3.0f, 1.0f, -0.4f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)).texOffs(40, 33).addBox(-3.0f, -2.0f, -0.4f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)).texOffs(40, 35).addBox(-3.0f, -0.5f, -0.4f, 6.0f, 1.0f, 1.0f, new CubeDeformation(-0.01f)), PartPose.offsetAndRotation((float)0.0f, (float)8.0f, (float)-2.9f, (float)3.098f, (float)0.0f, (float)-3.1416f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(22, 45).addBox(-3.0f, -3.5f, -0.45f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(78, 2).addBox(-3.0f, -2.0f, -0.45f, 6.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).texOffs(0, 41).addBox(-4.0f, -4.5f, -1.45f, 8.0f, 9.0f, 2.0f, new CubeDeformation(-0.15f)), PartPose.offsetAndRotation((float)0.0f, (float)6.5f, (float)-2.55f, (float)0.0f, (float)3.1416f, (float)0.0f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(68, 0).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.15f)).texOffs(22, 47).addBox(-1.0f, -0.95f, -3.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(10, 80).addBox(-1.0f, -0.95f, 1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(0, 71).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.0f, (float)0.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0873f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(66, 39).addBox(-2.0f, 0.05f, -3.5f, 3.0f, 4.0f, 5.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)-3.0f, (float)5.95f, (float)1.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition body_r7 = body.addOrReplaceChild("body_r7", CubeListBuilder.create().texOffs(54, 44).addBox(-4.9504f, -1.1506f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)7.0208f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition body_r8 = body.addOrReplaceChild("body_r8", CubeListBuilder.create().texOffs(0, 63).addBox(-2.0f, 0.05f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)-3.0f, (float)7.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition body_r9 = body.addOrReplaceChild("body_r9", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, -0.95f, 1.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(48, 11).addBox(-1.0f, -0.95f, -3.0f, 2.0f, 3.0f, 2.0f, new CubeDeformation(-0.3f)).texOffs(68, 8).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)).texOffs(58, 70).addBox(-1.0f, -1.55f, -3.0f, 2.0f, 2.0f, 6.0f, new CubeDeformation(-0.15f)), PartPose.offsetAndRotation((float)-3.0f, (float)0.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0873f));
        PartDefinition body_r10 = body.addOrReplaceChild("body_r10", CubeListBuilder.create().texOffs(12, 66).addBox(-1.0f, 0.05f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)3.0f, (float)7.95f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition body_r11 = body.addOrReplaceChild("body_r11", CubeListBuilder.create().texOffs(30, 66).addBox(1.9504f, -1.1506f, -3.0f, 3.0f, 2.0f, 6.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)7.0208f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition body_r12 = body.addOrReplaceChild("body_r12", CubeListBuilder.create().texOffs(67, 16).addBox(-1.0f, 0.05f, -3.5f, 3.0f, 4.0f, 5.0f, new CubeDeformation(-0.3f)), PartPose.offsetAndRotation((float)3.0f, (float)5.95f, (float)1.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(68, 70).mirror().addBox(-1.0f, 6.0f, -2.0f, 4.0f, 2.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_arm_r1 = left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(0, 52).mirror().addBox(-2.0f, -4.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.5f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)3.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.0436f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(68, 70).addBox(-3.0f, 6.0f, -2.0f, 4.0f, 2.0f, 4.0f, new CubeDeformation(0.3f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm_r1 = right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(0, 52).addBox(-2.0f, -4.0f, -2.0f, 4.0f, 5.0f, 4.0f, new CubeDeformation(0.5f)), PartPose.offsetAndRotation((float)-1.0f, (float)3.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.0436f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 37).mirror().addBox(-1.9f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).mirror(false).texOffs(76, 25).mirror().addBox(-1.9f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(76, 25).mirror().addBox(-1.9f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offset((float)1.9f, (float)12.0f, (float)0.0f));
        PartDefinition left_leg_r1 = left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(28, 56).mirror().addBox(-1.5f, -1.5f, -0.5f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false).texOffs(24, 5).mirror().addBox(-1.5f, -1.0f, -0.5f, 3.0f, 2.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offsetAndRotation((float)0.1f, (float)4.5f, (float)-2.1f, (float)0.0873f, (float)-0.0873f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 37).addBox(-2.1f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(76, 25).addBox(-2.1f, 1.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)).texOffs(76, 25).addBox(-2.1f, 4.0f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.3f)), PartPose.offset((float)-1.9f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(24, 5).addBox(-1.5f, -1.0f, -0.5f, 3.0f, 2.0f, 1.0f, new CubeDeformation(0.4f)).texOffs(28, 56).addBox(-1.5f, -1.5f, -0.5f, 3.0f, 3.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-0.1f, (float)4.5f, (float)-2.1f, (float)0.0873f, (float)0.0873f, (float)0.0f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create().texOffs(48, 56).addBox(-2.0f, 6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.49f)).texOffs(33, 16).mirror().addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).mirror(false).texOffs(32, 56).addBox(-2.0f, 6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create().texOffs(48, 56).mirror().addBox(-2.0f, 6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.49f)).mirror(false).texOffs(33, 16).addBox(-2.0f, 11.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.4f)).texOffs(32, 56).mirror().addBox(-2.0f, 6.0f, -2.0f, 4.0f, 6.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
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

