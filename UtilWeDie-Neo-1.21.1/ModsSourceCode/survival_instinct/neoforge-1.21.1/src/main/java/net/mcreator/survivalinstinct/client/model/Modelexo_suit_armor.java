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

public class Modelexo_suit_armor<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelexo_suit_armor"), "main");
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart left_arm;
    public final ModelPart right_arm;
    public final ModelPart left_leg;
    public final ModelPart right_leg;
    public final ModelPart right_shoe;
    public final ModelPart left_shoe;

    public Modelexo_suit_armor(ModelPart root) {
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
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(24, 4).mirror().addBox(-2.827f, -0.2834f, -2.7217f, 3.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)3.4088f, (float)-0.8426f, (float)-1.65f, (float)0.0999f, (float)-0.5148f, (float)0.1918f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(23, 7).mirror().addBox(-1.0976f, -1.381f, -2.3512f, 2.0f, 2.0f, 9.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)3.4088f, (float)-0.8426f, (float)-1.65f, (float)-0.0382f, (float)-0.1434f, (float)1.8215f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(46, 52).mirror().addBox(-1.0f, -0.5f, 1.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)4.0f, (float)-3.5f, (float)0.0f, (float)-0.0382f, (float)-0.1434f, (float)1.1233f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(24, 4).addBox(-0.173f, -0.2834f, -2.7217f, 3.0f, 1.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-3.4088f, (float)-0.8426f, (float)-1.65f, (float)0.0999f, (float)0.5148f, (float)-0.1918f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(23, 7).addBox(-0.9024f, -1.381f, -2.3512f, 2.0f, 2.0f, 9.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-3.4088f, (float)-0.8426f, (float)-1.65f, (float)-0.0382f, (float)0.1434f, (float)-1.8215f));
        PartDefinition head_r6 = head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(46, 52).addBox(-1.0f, -0.5f, 1.0f, 2.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-4.0f, (float)-3.5f, (float)0.0f, (float)-0.0382f, (float)0.1434f, (float)-1.1233f));
        PartDefinition head_r7 = head.addOrReplaceChild("head_r7", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0f, 0.5f, -1.5f, 6.0f, 2.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.7559f, (float)5.2365f, (float)-0.0436f, (float)0.0f, (float)0.0f));
        PartDefinition head_r8 = head.addOrReplaceChild("head_r8", CubeListBuilder.create().texOffs(16, 62).addBox(-1.0f, -1.5f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-5.5593f, (float)4.4589f, (float)0.0873f, (float)0.0f, (float)0.0f));
        PartDefinition head_r9 = head.addOrReplaceChild("head_r9", CubeListBuilder.create().texOffs(8, 61).addBox(-1.0f, 1.0f, -1.0f, 2.0f, 4.0f, 2.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-4.0f, (float)5.0f, (float)-0.2618f, (float)0.0f, (float)0.0f));
        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0f, 0.0f, -2.0f, 8.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition body_r1 = body.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(50, 47).addBox(-2.5f, -0.5f, -1.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.0f, (float)8.5f, (float)0.0f, (float)0.0f, (float)0.1309f, (float)0.1309f));
        PartDefinition body_r2 = body.addOrReplaceChild("body_r2", CubeListBuilder.create().texOffs(67, 18).mirror().addBox(-0.5f, -2.0f, -1.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)-2.5f, (float)2.9802f, (float)2.8021f, (float)0.1476f, (float)-0.3562f, (float)-0.2346f));
        PartDefinition body_r3 = body.addOrReplaceChild("body_r3", CubeListBuilder.create().texOffs(50, 47).mirror().addBox(-1.5f, -0.5f, -1.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)-3.0f, (float)8.5f, (float)0.0f, (float)0.0f, (float)-0.1309f, (float)-0.1309f));
        PartDefinition body_r4 = body.addOrReplaceChild("body_r4", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1.5f, -0.5f, -1.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)-3.0f, (float)2.5f, (float)0.0f, (float)0.0f, (float)-0.2182f, (float)0.2618f));
        PartDefinition body_r5 = body.addOrReplaceChild("body_r5", CubeListBuilder.create().texOffs(17, 58).mirror().addBox(-1.5f, -0.5f, 0.0f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)-3.0f, (float)5.5f, (float)0.0f, (float)0.0f, (float)-0.2182f, (float)0.2618f));
        PartDefinition body_r6 = body.addOrReplaceChild("body_r6", CubeListBuilder.create().texOffs(61, 32).mirror().addBox(-2.0f, -0.5f, 0.6041f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offsetAndRotation((float)3.1746f, (float)10.523f, (float)-1.1041f, (float)-2.9001f, (float)0.1434f, (float)2.2786f));
        PartDefinition body_r7 = body.addOrReplaceChild("body_r7", CubeListBuilder.create().texOffs(52, 0).addBox(-2.5f, -0.5f, -1.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.0f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.2182f, (float)-0.2618f));
        PartDefinition body_r8 = body.addOrReplaceChild("body_r8", CubeListBuilder.create().texOffs(17, 58).addBox(-1.5f, -0.5f, 0.0f, 3.0f, 1.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)3.0f, (float)5.5f, (float)0.0f, (float)0.0f, (float)0.2182f, (float)-0.2618f));
        PartDefinition body_r9 = body.addOrReplaceChild("body_r9", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, -5.5709f, -0.1553f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)4.7114f, (float)2.9438f, (float)0.3491f, (float)0.0f, (float)0.0f));
        PartDefinition body_r10 = body.addOrReplaceChild("body_r10", CubeListBuilder.create().texOffs(43, 58).addBox(-1.0f, -0.8706f, -0.316f, 2.0f, 6.0f, 2.0f, new CubeDeformation(-0.1f)), PartPose.offsetAndRotation((float)0.0f, (float)4.7114f, (float)2.9438f, (float)-0.3491f, (float)0.0f, (float)0.0f));
        PartDefinition body_r11 = body.addOrReplaceChild("body_r11", CubeListBuilder.create().texOffs(67, 18).addBox(-1.5f, -2.0f, -1.0f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)2.5f, (float)2.9802f, (float)2.8021f, (float)0.1476f, (float)0.3562f, (float)0.2346f));
        PartDefinition body_r12 = body.addOrReplaceChild("body_r12", CubeListBuilder.create().texOffs(24, 18).addBox(-1.0f, -4.5f, -2.75f, 2.0f, 2.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)3.5f, (float)2.75f, (float)0.1309f, (float)0.0f, (float)0.0f));
        PartDefinition body_r13 = body.addOrReplaceChild("body_r13", CubeListBuilder.create().texOffs(0, 53).addBox(-2.0f, -4.5f, -0.75f, 4.0f, 7.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)4.5f, (float)2.75f, (float)0.1309f, (float)0.0f, (float)0.0f));
        PartDefinition body_r14 = body.addOrReplaceChild("body_r14", CubeListBuilder.create().texOffs(66, 46).addBox(-1.0f, -2.5f, -0.5f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-4.2261f, (float)11.6787f, (float)-0.5f, (float)0.0f, (float)0.3927f, (float)-0.1309f));
        PartDefinition body_r15 = body.addOrReplaceChild("body_r15", CubeListBuilder.create().texOffs(61, 32).addBox(-2.0f, -0.5f, 0.6041f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-3.1746f, (float)10.523f, (float)-1.1041f, (float)-2.9001f, (float)-0.1434f, (float)-2.2786f));
        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(33, 30).mirror().addBox(-1.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        PartDefinition left_arm_r1 = left_arm.addOrReplaceChild("left_arm_r1", CubeListBuilder.create().texOffs(43, 40).mirror().addBox(-3.0f, -0.5f, -3.0f, 6.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)7.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.48f));
        PartDefinition left_arm_r2 = left_arm.addOrReplaceChild("left_arm_r2", CubeListBuilder.create().texOffs(27, 60).mirror().addBox(-1.0f, -3.5f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)2.8622f, (float)5.3929f, (float)0.0143f, (float)-0.2736f, (float)-0.1602f, (float)0.1295f));
        PartDefinition left_arm_r3 = left_arm.addOrReplaceChild("left_arm_r3", CubeListBuilder.create().texOffs(44, 28).mirror().addBox(-0.75f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offsetAndRotation((float)3.5518f, (float)-1.756f, (float)0.3796f, (float)1.192f, (float)0.0519f, (float)-0.4877f));
        PartDefinition left_arm_r4 = left_arm.addOrReplaceChild("left_arm_r4", CubeListBuilder.create().texOffs(59, 61).mirror().addBox(-0.75f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.3f)).mirror(false), PartPose.offsetAndRotation((float)3.5518f, (float)-1.756f, (float)0.3796f, (float)0.5375f, (float)0.0519f, (float)-0.4877f));
        PartDefinition left_arm_r5 = left_arm.addOrReplaceChild("left_arm_r5", CubeListBuilder.create().texOffs(45, 25).mirror().addBox(-3.0f, -0.5f, -3.0f, 6.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.3054f));
        PartDefinition left_arm_r6 = left_arm.addOrReplaceChild("left_arm_r6", CubeListBuilder.create().texOffs(58, 17).mirror().addBox(-1.5f, 0.0f, 0.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.65f)).mirror(false), PartPose.offsetAndRotation((float)0.5f, (float)-2.5f, (float)-0.5f, (float)-0.7418f, (float)0.0f, (float)0.0f));
        PartDefinition left_arm_r7 = left_arm.addOrReplaceChild("left_arm_r7", CubeListBuilder.create().texOffs(61, 37).mirror().addBox(-0.5f, -3.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)2.5f, (float)1.0f, (float)0.0f, (float)0.1946f, (float)-0.2191f, (float)-0.266f));
        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(33, 30).addBox(-3.0f, -2.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        PartDefinition right_arm_r1 = right_arm.addOrReplaceChild("right_arm_r1", CubeListBuilder.create().texOffs(27, 60).addBox(-1.0f, -3.5f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-2.8622f, (float)5.3929f, (float)0.0143f, (float)-0.2736f, (float)0.1602f, (float)-0.1295f));
        PartDefinition right_arm_r2 = right_arm.addOrReplaceChild("right_arm_r2", CubeListBuilder.create().texOffs(59, 61).addBox(-0.25f, -1.5f, -1.5f, 1.0f, 3.0f, 3.0f, new CubeDeformation(0.3f)), PartPose.offsetAndRotation((float)-3.5518f, (float)-1.756f, (float)0.3796f, (float)0.5375f, (float)-0.0519f, (float)0.4877f));
        PartDefinition right_arm_r3 = right_arm.addOrReplaceChild("right_arm_r3", CubeListBuilder.create().texOffs(44, 28).addBox(-1.25f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-3.5518f, (float)-1.756f, (float)0.3796f, (float)1.192f, (float)-0.0519f, (float)0.4877f));
        PartDefinition right_arm_r4 = right_arm.addOrReplaceChild("right_arm_r4", CubeListBuilder.create().texOffs(43, 40).addBox(-3.0f, -0.5f, -3.0f, 6.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.0f, (float)7.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.48f));
        PartDefinition right_arm_r5 = right_arm.addOrReplaceChild("right_arm_r5", CubeListBuilder.create().texOffs(45, 25).addBox(-3.0f, -0.5f, -3.0f, 6.0f, 1.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.0f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.3054f));
        PartDefinition right_arm_r6 = right_arm.addOrReplaceChild("right_arm_r6", CubeListBuilder.create().texOffs(58, 17).addBox(-2.5f, 0.0f, 0.5f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.65f)), PartPose.offsetAndRotation((float)-0.5f, (float)-2.5f, (float)-0.5f, (float)-0.7418f, (float)0.0f, (float)0.0f));
        PartDefinition right_arm_r7 = right_arm.addOrReplaceChild("right_arm_r7", CubeListBuilder.create().texOffs(61, 37).addBox(-1.5f, -3.0f, -1.0f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-2.5f, (float)1.0f, (float)0.0f, (float)0.1946f, (float)0.2191f, (float)0.266f));
        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition left_leg_r1 = left_leg.addOrReplaceChild("left_leg_r1", CubeListBuilder.create().texOffs(28, 30).mirror().addBox(-1.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)3.7993f, (float)4.2227f, (float)0.4075f, (float)0.883f, (float)-0.1116f, (float)-0.0857f));
        PartDefinition left_leg_r2 = left_leg.addOrReplaceChild("left_leg_r2", CubeListBuilder.create().texOffs(55, 55).mirror().addBox(0.5f, -3.2638f, -0.9255f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)1.5f, (float)6.2753f, (float)-0.0298f, (float)0.2285f, (float)-0.1116f, (float)-0.0857f));
        PartDefinition left_leg_r3 = left_leg.addOrReplaceChild("left_leg_r3", CubeListBuilder.create().texOffs(49, 32).mirror().addBox(-2.0f, -0.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offsetAndRotation((float)0.0f, (float)9.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.1309f));
        PartDefinition left_leg_r4 = left_leg.addOrReplaceChild("left_leg_r4", CubeListBuilder.create().texOffs(12, 50).mirror().addBox(-2.0f, -0.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offsetAndRotation((float)0.0f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.1309f));
        PartDefinition left_leg_r5 = left_leg.addOrReplaceChild("left_leg_r5", CubeListBuilder.create().texOffs(62, 52).mirror().addBox(0.5f, -4.7264f, -1.7436f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.3f)).mirror(false).texOffs(65, 58).mirror().addBox(0.5f, -3.7264f, -0.7436f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)0.5f, (float)5.2753f, (float)-0.0298f, (float)2.9234f, (float)-0.1309f, (float)0.0f));
        PartDefinition left_leg_r6 = left_leg.addOrReplaceChild("left_leg_r6", CubeListBuilder.create().texOffs(42, 66).mirror().addBox(0.5f, -4.2638f, 0.0745f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)).mirror(false), PartPose.offsetAndRotation((float)0.5f, (float)5.2753f, (float)-0.0298f, (float)0.2182f, (float)-0.1309f, (float)0.0f));
        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_leg_r1 = right_leg.addOrReplaceChild("right_leg_r1", CubeListBuilder.create().texOffs(28, 30).addBox(-1.0f, -0.5f, -0.5f, 2.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-3.7993f, (float)4.2227f, (float)0.4075f, (float)0.883f, (float)0.1116f, (float)0.0857f));
        PartDefinition right_leg_r2 = right_leg.addOrReplaceChild("right_leg_r2", CubeListBuilder.create().texOffs(49, 32).addBox(-2.0f, -0.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offsetAndRotation((float)0.0f, (float)9.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.1309f));
        PartDefinition right_leg_r3 = right_leg.addOrReplaceChild("right_leg_r3", CubeListBuilder.create().texOffs(62, 52).addBox(-2.5f, -4.7264f, -1.7436f, 2.0f, 4.0f, 2.0f, new CubeDeformation(0.3f)).texOffs(65, 58).addBox(-2.5f, -3.7264f, -0.7436f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-0.5f, (float)5.2753f, (float)-0.0298f, (float)2.9234f, (float)0.1309f, (float)0.0f));
        PartDefinition right_leg_r4 = right_leg.addOrReplaceChild("right_leg_r4", CubeListBuilder.create().texOffs(55, 55).addBox(-2.5f, -3.2638f, -0.9255f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.5f, (float)6.2753f, (float)-0.0298f, (float)0.2285f, (float)0.1116f, (float)0.0857f));
        PartDefinition right_leg_r5 = right_leg.addOrReplaceChild("right_leg_r5", CubeListBuilder.create().texOffs(42, 66).addBox(-2.5f, -4.2638f, 0.0745f, 2.0f, 4.0f, 1.0f, new CubeDeformation(0.2f)), PartPose.offsetAndRotation((float)-0.5f, (float)5.2753f, (float)-0.0298f, (float)0.2182f, (float)0.1309f, (float)0.0f));
        PartDefinition right_leg_r6 = right_leg.addOrReplaceChild("right_leg_r6", CubeListBuilder.create().texOffs(12, 50).addBox(-2.0f, -0.5f, -2.0f, 4.0f, 1.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offsetAndRotation((float)0.0f, (float)2.5f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.1309f));
        PartDefinition right_shoe = partdefinition.addOrReplaceChild("right_shoe", CubeListBuilder.create(), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        PartDefinition left_shoe_r1 = right_shoe.addOrReplaceChild("left_shoe_r1", CubeListBuilder.create().texOffs(0, 69).mirror().addBox(0.3f, -1.0f, -3.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)1.0f, (float)12.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.3927f));
        PartDefinition left_shoe_r2 = right_shoe.addOrReplaceChild("left_shoe_r2", CubeListBuilder.create().texOffs(46, 22).mirror().addBox(-4.0f, 0.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)2.0f, (float)12.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)0.3927f));
        PartDefinition left_shoe = partdefinition.addOrReplaceChild("left_shoe", CubeListBuilder.create(), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        PartDefinition right_shoe_r1 = left_shoe.addOrReplaceChild("right_shoe_r1", CubeListBuilder.create().texOffs(46, 22).addBox(0.0f, 0.0f, -3.0f, 4.0f, 1.0f, 1.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-2.0f, (float)12.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.3927f));
        PartDefinition right_shoe_r2 = left_shoe.addOrReplaceChild("right_shoe_r2", CubeListBuilder.create().texOffs(0, 69).addBox(-1.3f, -1.0f, -3.0f, 1.0f, 1.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)-1.0f, (float)12.0f, (float)0.0f, (float)0.0f, (float)0.0f, (float)-0.3927f));
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

