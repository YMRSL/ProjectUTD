package net.tkg.ModernMayhem.client.renderer.curios.knee;

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
import net.minecraft.world.item.ItemStack;
import net.tkg.ModernMayhem.client.models.curios.knee.KneepadModel;
import net.tkg.ModernMayhem.server.item.curios.knee.KneepadItems;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class KneepadRenderer
extends GeoArmorRenderer<KneepadItems>
implements ICurioRenderer {
    public KneepadRenderer() {
        super((GeoModel)new KneepadModel());
    }

    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        this.prepForRender((Entity)slotContext.entity(), stack, EquipmentSlot.LEGS, (HumanoidModel)renderLayerParent.getModel());
        VertexConsumer consumer = renderTypeBuffer.getBuffer(RenderType.armorCutoutNoCull((ResourceLocation)this.getTextureLocation((KneepadItems)stack.getItem())));
        this.renderToBuffer(matrixStack, consumer, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }

    public void setupAnim(Entity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
    }

    public RenderType getRenderType(KneepadItems animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent((ResourceLocation)this.getTextureLocation(animatable));
    }

    public static class KneepadItemRenderer
    extends GeoItemRenderer<KneepadItems> {
        public KneepadItemRenderer() {
            super((GeoModel)new KneepadModel());
        }
    }
}

