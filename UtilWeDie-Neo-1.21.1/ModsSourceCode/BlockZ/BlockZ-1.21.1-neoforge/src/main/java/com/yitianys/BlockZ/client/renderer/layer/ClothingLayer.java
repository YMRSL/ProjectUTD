package com.yitianys.BlockZ.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.init.BlockZAttachments;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.item.BackpackItem;
import com.yitianys.BlockZ.item.ClothingItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 玩家服装渲染层（KEEP）。从原生装备栏 + 数据附件背包读取服装/背包并渲染。
 *
 * <p>1.21.1 / NeoForge 迁移：
 * <ul>
 *   <li>背包 capability 改为数据附件 {@code player.getData(BlockZAttachments.PLAYER_BACKPACK)}，
 *       取代 Forge {@code getCapability(PlayerBackpackProvider...).ifPresent/map}。</li>
 *   <li>{@code ForgeRegistries.ITEMS.getKey} → {@code BuiltInRegistries.ITEM.getKey}。</li>
 *   <li>额外模型查找用 {@code ModelResourceLocation.standalone(...)}。</li>
 *   <li>{@code HumanoidModel#renderToBuffer} 1.21.1 改为单 ARGB color 参数。</li>
 *   <li>DROP：删除 FirstPersonBodyRenderState（第一人称躯体隐藏头/手）引用——不再隐藏部位。</li>
 * </ul>
 */
public class ClothingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final HumanoidModel<AbstractClientPlayer> legacyModel;
    private final HumanoidModel<AbstractClientPlayer> maskModel;

    public record OuterLayerState(boolean hat, boolean jacket, boolean leftSleeve, boolean rightSleeve, boolean leftPants, boolean rightPants) {
    }

    public ClothingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
        // 使用 HumanoidModel 的标准 Mesh 定义，并强制 64x32 纹理实现 DayM 兼容
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.01F), 0.0F);

        this.legacyModel = new HumanoidModel<>(LayerDefinition.create(mesh, 64, 32).bakeRoot());
        this.maskModel = new HumanoidModel<>(LayerDefinition.create(mesh, 64, 32).bakeRoot());
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;

        PlayerModel<AbstractClientPlayer> parentModel = this.getParentModel();
        OuterLayerState previousState = captureOuterLayerState(parentModel);
        applyOuterLayerVisibility(player, parentModel);

        try {
            // 1. 渲染基础衣服 (来自原生装备栏)
            renderClothing(poseStack, buffer, packedLight, player, EquipmentSlot.CHEST, ClothingItem.ClothingType.SHIRT);
            renderClothing(poseStack, buffer, packedLight, player, EquipmentSlot.LEGS, ClothingItem.ClothingType.PANTS);
            renderClothing(poseStack, buffer, packedLight, player, EquipmentSlot.FEET, ClothingItem.ClothingType.SHOES);
            renderClothing(poseStack, buffer, packedLight, player, EquipmentSlot.HEAD, ClothingItem.ClothingType.HAT);

            // 2. 渲染数据附件背包里的装备 (背心、手套、面具、背包)
            PlayerBackpack cap = player.getData(BlockZAttachments.PLAYER_BACKPACK);
            ItemStackHandler inventory = cap.getInventory();

            // Render Backpack (3D Model)
            ItemStack backpack = inventory.getStackInSlot(PlayerBackpack.SLOT_BACKPACK);
            if (isManagedBackpack(backpack)) {
                render3DClothing(poseStack, buffer, packedLight, player, backpack, ClothingItem.ClothingType.BACKPACK);
            }

            // Render Hat or Mask in MASK slot
            ItemStack maskSlotStack = inventory.getStackInSlot(PlayerBackpack.SLOT_MASK);
            if (!maskSlotStack.isEmpty()) {
                if (maskSlotStack.getItem() instanceof ClothingItem c && c.getType() == ClothingItem.ClothingType.HAT) {
                    render3DClothing(poseStack, buffer, packedLight, player, maskSlotStack, ClothingItem.ClothingType.HAT);
                } else {
                    render2DClothing(poseStack, buffer, packedLight, player, maskSlotStack);
                }
            }

            // Render Vest (3D Model)
            ItemStack vest = inventory.getStackInSlot(PlayerBackpack.SLOT_VEST);
            if (hasClothingType(vest, ClothingItem.ClothingType.VEST)) {
                render3DClothing(poseStack, buffer, packedLight, player, vest, ClothingItem.ClothingType.VEST);
            }

            // Render Gloves (2D Texture)
            ItemStack gloves = inventory.getStackInSlot(PlayerBackpack.SLOT_GLOVES);
            if (!gloves.isEmpty()) {
                render2DClothing(poseStack, buffer, packedLight, player, gloves);
            }
        } finally {
            restoreOuterLayerState(parentModel, previousState);
        }
    }

    public static OuterLayerState captureOuterLayerState(PlayerModel<AbstractClientPlayer> model) {
        return new OuterLayerState(model.hat.visible, model.jacket.visible, model.leftSleeve.visible, model.rightSleeve.visible, model.leftPants.visible, model.rightPants.visible);
    }

    public static void restoreOuterLayerState(PlayerModel<AbstractClientPlayer> model, OuterLayerState state) {
        model.hat.visible = state.hat();
        model.jacket.visible = state.jacket();
        model.leftSleeve.visible = state.leftSleeve();
        model.rightSleeve.visible = state.rightSleeve();
        model.leftPants.visible = state.leftPants();
        model.rightPants.visible = state.rightPants();
    }

    public static void applyOuterLayerVisibility(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel) {
        boolean wearingHat = hasClothing(player, EquipmentSlot.HEAD, ClothingItem.ClothingType.HAT) || hasCapabilityClothing(player, PlayerBackpack.SLOT_MASK, ClothingItem.ClothingType.HAT);
        boolean wearingMask = hasClothing(player, EquipmentSlot.HEAD, ClothingItem.ClothingType.MASK) || hasCapabilityClothing(player, PlayerBackpack.SLOT_MASK, ClothingItem.ClothingType.MASK);
        boolean wearingShirt = hasClothing(player, EquipmentSlot.CHEST, ClothingItem.ClothingType.SHIRT);
        boolean wearingVest = hasCapabilityClothing(player, PlayerBackpack.SLOT_VEST, ClothingItem.ClothingType.VEST);
        boolean wearingPants = hasClothing(player, EquipmentSlot.LEGS, ClothingItem.ClothingType.PANTS);
        boolean wearingShoes = hasClothing(player, EquipmentSlot.FEET, ClothingItem.ClothingType.SHOES);
        boolean wearingGloves = hasCapabilityClothing(player, PlayerBackpack.SLOT_GLOVES, ClothingItem.ClothingType.GLOVES);

        if (wearingHat || wearingMask) {
            parentModel.hat.visible = false;
        }
        if (wearingShirt || wearingVest) {
            parentModel.jacket.visible = false;
        }
        if (wearingShirt || wearingGloves) {
            parentModel.leftSleeve.visible = false;
            parentModel.rightSleeve.visible = false;
        }
        if (wearingPants || wearingShoes) {
            parentModel.leftPants.visible = false;
            parentModel.rightPants.visible = false;
        }
    }

    private static boolean hasClothing(AbstractClientPlayer player, EquipmentSlot slot, ClothingItem.ClothingType type) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof ClothingItem clothing && clothing.getType() == type;
    }

    private static boolean hasCapabilityClothing(AbstractClientPlayer player, int slotIndex, ClothingItem.ClothingType type) {
        PlayerBackpack cap = player.getData(BlockZAttachments.PLAYER_BACKPACK);
        ItemStack stack = cap.getInventory().getStackInSlot(slotIndex);
        return stack.getItem() instanceof ClothingItem clothing && clothing.getType() == type;
    }

    private boolean hasClothingType(ItemStack stack, ClothingItem.ClothingType type) {
        return stack.getItem() instanceof ClothingItem clothing && clothing.getType() == type;
    }

    private boolean isManagedBackpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }

    private void renderClothing(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof ClothingItem clothing)) return;

        // 只有帽子 (HAT) 和 背心 (VEST) 使用 3D 模型渲染；其余强制用经典 64x32 材质层
        if (clothing.getType() == ClothingItem.ClothingType.HAT || clothing.getType() == ClothingItem.ClothingType.VEST) {
            render3DClothing(poseStack, buffer, packedLight, player, stack, clothing.getType());
        } else {
            render2DClothing(poseStack, buffer, packedLight, player, stack);
        }
    }

    private void render3DClothing(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ItemStack stack, ClothingItem.ClothingType type) {
        poseStack.pushPose();

        if (type == ClothingItem.ClothingType.HAT) {
            this.getParentModel().head.translateAndRotate(poseStack);

            poseStack.scale(0.65F, -0.65F, 0.65F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            if (player.isCrouching()) {
                poseStack.translate(0.0D, 0.0D, 0.15D);
            }

            poseStack.translate(0.0D, -3.35D, 0.0D);

            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), poseStack, buffer, player.level(), player.getId());
        } else if (type == ClothingItem.ClothingType.BACKPACK || type == ClothingItem.ClothingType.VEST) {
            // 背包和背心使用完全相同的位置变换
            this.getParentModel().body.translateAndRotate(poseStack);

            poseStack.translate(-0.5D, 1.0D, -0.5D);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0D, 0.0D, 0.01D);

            if (type == ClothingItem.ClothingType.VEST && stack.getItem() == ModItems.VEST_0.get()) {
                renderItemModel(poseStack, buffer, packedLight, player, stack, "item/vest_0_3d");
            } else if (type == ClothingItem.ClothingType.BACKPACK) {
                if (stack.getItem() == ModItems.BACKPACK_COYOTE.get()) {
                    renderItemModel(poseStack, buffer, packedLight, player, stack, "item/backpack_coyote_3d");
                } else if (stack.getItem() == ModItems.BACKPACK_ALICE.get()) {
                    renderItemModel(poseStack, buffer, packedLight, player, stack, "item/backpack_alice_3d");
                } else if (stack.getItem() == ModItems.BACKPACK_CZECH.get()) {
                    renderItemModel(poseStack, buffer, packedLight, player, stack, "item/backpack_czech_3d");
                } else if (stack.getItem() == ModItems.BACKPACK_CZECHPOUCH.get()) {
                    renderItemModel(poseStack, buffer, packedLight, player, stack, "item/backpack_czechpouch_3d");
                } else if (stack.getItem() == ModItems.BACKPACK_PATROLPACK.get()) {
                    renderItemModel(poseStack, buffer, packedLight, player, stack, "item/backpack_patrolpack_3d");
                } else {
                    renderItemStatic(poseStack, buffer, packedLight, player, stack);
                }
            } else {
                renderItemStatic(poseStack, buffer, packedLight, player, stack);
            }
        }

        poseStack.popPose();
    }

    private void renderItemModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ItemStack stack, String modelPath) {
        ModelResourceLocation mrl = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, modelPath));
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(mrl);
        if (model != null && model != Minecraft.getInstance().getModelManager().getMissingModel()) {
            Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.NONE, false, poseStack, buffer, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), model);
        } else {
            renderItemStatic(poseStack, buffer, packedLight, player, stack);
        }
    }

    private void renderItemStatic(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ItemStack stack) {
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), poseStack, buffer, player.level(), player.getId());
    }

    private void renderClothing(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, EquipmentSlot slot, ClothingItem.ClothingType type) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof ClothingItem clothing) || clothing.getType() != type) return;
        renderClothing(poseStack, buffer, packedLight, player, stack);
    }

    private void render2DClothing(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof ClothingItem clothing)) return;

        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (rl == null) return;

        String path = rl.getPath();
        // 简写自动映射到 "_0"
        if (path.equals("vest")) path = "vest_0";
        if (path.equals("gloves")) path = "gloves_0";
        if (path.equals("shirt")) path = "shirt_0";
        if (path.equals("pants")) path = "pants_0";
        if (path.equals("shoes")) path = "shoes_0";
        if (path.equals("hat")) path = "headwear_0";
        if (path.equals("mask")) path = "headgear_0";

        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "textures/entity/player/clothing/" + path + ".png");

        // headwear 回退到 headgear（DayM 的 headwear 常无对应 player 纹理）
        if (path.startsWith("headwear_") && !path.equals("headwear_0")) {
            String num = path.substring("headwear_".length());
            texture = ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "textures/entity/player/clothing/headgear_" + num + ".png");
        }

        renderTextureOnPlayer(poseStack, buffer, packedLight, player, texture, clothing.getType());
    }

    private void renderTextureOnPlayer(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, ResourceLocation texture, ClothingItem.ClothingType type) {
        PlayerModel<AbstractClientPlayer> parent = this.getParentModel();
        HumanoidModel<AbstractClientPlayer> modelToUse = switch (type) {
            case MASK -> maskModel;
            default -> legacyModel;
        };

        // 1. 同步姿态
        modelToUse.attackTime = parent.attackTime;
        modelToUse.riding = parent.riding;
        modelToUse.young = parent.young;
        modelToUse.leftArmPose = parent.leftArmPose;
        modelToUse.rightArmPose = parent.rightArmPose;
        modelToUse.crouching = parent.crouching;

        modelToUse.head.copyFrom(parent.head);
        modelToUse.hat.copyFrom(parent.hat);
        modelToUse.body.copyFrom(parent.body);
        modelToUse.rightArm.copyFrom(parent.rightArm);
        modelToUse.leftArm.copyFrom(parent.leftArm);
        modelToUse.rightLeg.copyFrom(parent.rightLeg);
        modelToUse.leftLeg.copyFrom(parent.leftLeg);

        // 2. 设置可见性
        boolean prevHead = modelToUse.head.visible;
        boolean prevHat = modelToUse.hat.visible;
        boolean prevBody = modelToUse.body.visible;
        boolean prevRightArm = modelToUse.rightArm.visible;
        boolean prevLeftArm = modelToUse.leftArm.visible;
        boolean prevRightLeg = modelToUse.rightLeg.visible;
        boolean prevLeftLeg = modelToUse.leftLeg.visible;
        modelToUse.setAllVisible(false);
        switch (type) {
            case HAT:
                modelToUse.head.visible = true;
                modelToUse.hat.visible = true;
                break;
            case MASK:
                modelToUse.head.visible = true;
                modelToUse.hat.visible = true;
                break;
            case VEST:
                modelToUse.body.visible = true;
                break;
            case SHIRT:
                modelToUse.body.visible = true;
                modelToUse.rightArm.visible = true;
                modelToUse.leftArm.visible = true;
                break;
            case GLOVES:
                modelToUse.rightArm.visible = true;
                modelToUse.leftArm.visible = true;
                break;
            case PANTS:
                modelToUse.body.visible = true;
                modelToUse.rightLeg.visible = true;
                modelToUse.leftLeg.visible = true;
                break;
            case SHOES:
                modelToUse.rightLeg.visible = true;
                modelToUse.leftLeg.visible = true;
                break;
            case BACKPACK:
                modelToUse.body.visible = true;
                break;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(texture));
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);

        // 3. 渲染
        float scale;
        switch (type) {
            case MASK: scale = 1.001F; break;
            case SHIRT: scale = 1.001F; break;
            case PANTS: scale = 1.001F; break;
            case SHOES: scale = 1.004F; break;
            case GLOVES: scale = 1.005F; break;
            case VEST: scale = 1.006F; break;
            default: scale = 1.0F; break;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        modelToUse.renderToBuffer(poseStack, vertexConsumer, packedLight, overlay, 0xFFFFFFFF);

        poseStack.popPose();

        modelToUse.head.visible = prevHead;
        modelToUse.hat.visible = prevHat;
        modelToUse.body.visible = prevBody;
        modelToUse.rightArm.visible = prevRightArm;
        modelToUse.leftArm.visible = prevLeftArm;
        modelToUse.rightLeg.visible = prevRightLeg;
        modelToUse.leftLeg.visible = prevLeftLeg;
    }
}
