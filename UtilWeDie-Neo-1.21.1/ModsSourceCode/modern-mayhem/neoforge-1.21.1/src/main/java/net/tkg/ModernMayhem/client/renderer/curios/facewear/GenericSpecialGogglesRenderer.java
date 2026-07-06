package net.tkg.ModernMayhem.client.renderer.curios.facewear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.tkg.ModernMayhem.client.models.curios.facewear.GenericSpecialGogglesModel;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class GenericSpecialGogglesRenderer<T extends GenericSpecialGogglesItem>
extends GeoArmorRenderer<T>
implements ICurioRenderer {
    private ItemStack currentRenderStack = ItemStack.EMPTY;

    public GenericSpecialGogglesRenderer() {
        super(new GenericSpecialGogglesModel());
    }

    public ResourceLocation getTextureLocation(T animatable) {
        if (!this.currentRenderStack.isEmpty() && this.model instanceof GenericSpecialGogglesModel) {
            GenericSpecialGogglesModel geoModel = (GenericSpecialGogglesModel)this.model;
            return geoModel.getTextureResource(animatable, this.currentRenderStack);
        }
        return super.getTextureLocation(animatable);
    }

    public <L extends LivingEntity, M extends EntityModel<L>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<L, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.currentRenderStack = stack;
        if (this.model instanceof GenericSpecialGogglesModel) {
            GenericSpecialGogglesModel geoModel = (GenericSpecialGogglesModel)this.model;
            geoModel.setCurrentStack(stack);
        }
        this.prepForRender((Entity)slotContext.entity(), stack, EquipmentSlot.HEAD, (HumanoidModel)renderLayerParent.getModel());
        VertexConsumer consumer = renderTypeBuffer.getBuffer(RenderType.armorCutoutNoCull((ResourceLocation)this.getTextureLocation((T)stack.getItem())));
        this.renderToBuffer(matrixStack, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        if (this.model instanceof GenericSpecialGogglesModel) {
            GenericSpecialGogglesModel geoModel = (GenericSpecialGogglesModel)this.model;
            geoModel.setCurrentStack(ItemStack.EMPTY);
        }
        this.currentRenderStack = ItemStack.EMPTY;
    }

    public void setupAnim(@NotNull Entity entity, float pLimbswing, float pLimbswingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
    }

    public static class GenericNVGGogglesSlotRenderer<T extends GenericSpecialGogglesItem>
    extends GeoItemRenderer<T> {
        private ItemStack renderingStack = ItemStack.EMPTY;

        public GenericNVGGogglesSlotRenderer() {
            super(new GenericSpecialGogglesModel());
        }

        public void renderByItem(ItemStack stack, @NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
            GenericSpecialGogglesModel geoModel;
            this.currentItemStack = stack;
            this.renderingStack = stack;
            GenericSpecialGogglesItem item = (GenericSpecialGogglesItem)stack.getItem();
            if (this.model instanceof GenericSpecialGogglesModel) {
                geoModel = (GenericSpecialGogglesModel)this.model;
                geoModel.setCurrentStack(stack);
            }
            super.renderByItem(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
            if (this.model instanceof GenericSpecialGogglesModel) {
                geoModel = (GenericSpecialGogglesModel)this.model;
                geoModel.setCurrentStack(ItemStack.EMPTY);
            }
            this.renderingStack = ItemStack.EMPTY;
            this.currentItemStack = null;
        }

        public ResourceLocation getTextureLocation(T animatable) {
            if (!this.renderingStack.isEmpty() && this.model instanceof GenericSpecialGogglesModel) {
                GenericSpecialGogglesModel geoModel = (GenericSpecialGogglesModel)this.model;
                return geoModel.getTextureResource(animatable, this.renderingStack);
            }
            return super.getTextureLocation(animatable);
        }
    }
}

