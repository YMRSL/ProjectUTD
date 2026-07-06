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

public class Modelchiken_head<T extends Entity>
extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("survival_instinct", "modelchiken_head"), "main");
    public final ModelPart head;

    public Modelchiken_head(ModelPart root) {
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(32, 41).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 10.0f, 8.0f, new CubeDeformation(0.7f)).texOffs(37, 12).addBox(-5.0f, -6.0f, -3.0f, 1.0f, 4.0f, 4.0f, new CubeDeformation(0.1f)).texOffs(30, 34).addBox(4.0f, -6.0f, -3.0f, 1.0f, 4.0f, 4.0f, new CubeDeformation(0.1f)), PartPose.offset((float)0.0f, (float)0.0f, (float)0.0f));
        PartDefinition head_r1 = head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(53, 12).mirror().addBox(-0.5f, -1.0f, -2.0f, 1.0f, 2.0f, 4.0f, new CubeDeformation(0.4f)).mirror(false), PartPose.offsetAndRotation((float)4.5f, (float)-5.0f, (float)-1.0f, (float)0.132f, (float)0.1298f, (float)0.0172f));
        PartDefinition head_r2 = head.addOrReplaceChild("head_r2", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(2.0f, -1.0f, -0.75f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)-1.0f, (float)-1.0f, (float)-5.25f, (float)-0.0462f, (float)0.0302f, (float)-0.1719f));
        PartDefinition head_r3 = head.addOrReplaceChild("head_r3", CubeListBuilder.create().texOffs(40, 34).mirror().addBox(2.0f, -1.0f, -0.75f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)).mirror(false), PartPose.offsetAndRotation((float)0.0f, (float)-2.0f, (float)-5.25f, (float)-0.1555f, (float)-0.1642f, (float)-0.3335f));
        PartDefinition head_r4 = head.addOrReplaceChild("head_r4", CubeListBuilder.create().texOffs(10, 32).addBox(-1.0f, -5.5f, -0.5f, 2.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.5f, (float)1.5f, (float)-0.6981f, (float)0.0f, (float)0.0f));
        PartDefinition head_r5 = head.addOrReplaceChild("head_r5", CubeListBuilder.create().texOffs(39, 20).addBox(-1.0f, -3.5f, -0.5f, 2.0f, 3.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-7.5f, (float)3.5f, (float)-1.0472f, (float)0.0f, (float)0.0f));
        PartDefinition head_r6 = head.addOrReplaceChild("head_r6", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0f, -7.5f, -0.5f, 2.0f, 7.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.5f, (float)-1.5f, (float)-0.48f, (float)0.0f, (float)0.0f));
        PartDefinition head_r7 = head.addOrReplaceChild("head_r7", CubeListBuilder.create().texOffs(20, 32).addBox(-1.0f, -5.5f, -0.5f, 2.0f, 6.0f, 3.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-8.5f, (float)-4.5f, (float)-0.2618f, (float)0.0f, (float)0.0f));
        PartDefinition head_r8 = head.addOrReplaceChild("head_r8", CubeListBuilder.create().texOffs(29, 28).addBox(-1.5f, 0.0f, -2.75f, 3.0f, 2.0f, 4.0f, new CubeDeformation(0.0f)).texOffs(24, 16).addBox(-2.0f, -2.0f, -3.75f, 4.0f, 2.0f, 5.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-2.0f, (float)-5.25f, (float)0.1745f, (float)0.0f, (float)0.0f));
        PartDefinition head_r9 = head.addOrReplaceChild("head_r9", CubeListBuilder.create().texOffs(40, 34).addBox(-4.0f, -1.0f, -0.75f, 2.0f, 5.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)0.0f, (float)-2.0f, (float)-5.25f, (float)-0.1555f, (float)0.1642f, (float)0.3335f));
        PartDefinition head_r10 = head.addOrReplaceChild("head_r10", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0f, -1.0f, -0.75f, 2.0f, 6.0f, 2.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation((float)1.0f, (float)-1.0f, (float)-5.25f, (float)-0.0462f, (float)-0.0302f, (float)0.1719f));
        PartDefinition head_r11 = head.addOrReplaceChild("head_r11", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0f, -1.0f, -2.5f, 4.0f, 2.0f, 5.0f, new CubeDeformation(-0.2f)), PartPose.offsetAndRotation((float)0.0f, (float)-3.7716f, (float)-5.9335f, (float)0.48f, (float)0.0f, (float)0.0f));
        PartDefinition head_r12 = head.addOrReplaceChild("head_r12", CubeListBuilder.create().texOffs(53, 12).addBox(-0.5f, -1.0f, -2.0f, 1.0f, 2.0f, 4.0f, new CubeDeformation(0.4f)), PartPose.offsetAndRotation((float)-4.5f, (float)-5.0f, (float)-1.0f, (float)0.132f, (float)-0.1298f, (float)-0.0172f));
        return LayerDefinition.create((MeshDefinition)meshdefinition, (int)64, (int)64);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }
}

