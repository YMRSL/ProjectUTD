package net.tkg.ModernMayhem.client.renderer.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.tkg.ModernMayhem.client.models.armor.CustomArmorModel;
import net.tkg.ModernMayhem.client.models.curios.back.BackpackModels;
import net.tkg.ModernMayhem.server.item.armor.CustomArmorItem;
import net.tkg.ModernMayhem.server.item.curios.back.BackpackItem;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CustomArmorRenderer
extends GeoArmorRenderer<CustomArmorItem> {
    public static final ThreadLocal<Boolean> SLIM_CONTEXT = ThreadLocal.withInitial(() -> false);

    public CustomArmorRenderer(EquipmentSlot slot) {
        super((GeoModel)new CustomArmorModel());
    }

    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        if (entity instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer)entity;
            SLIM_CONTEXT.set(player.getSkin().model() == PlayerSkin.Model.SLIM);
        } else {
            SLIM_CONTEXT.set(false);
        }
        super.prepForRender(entity, stack, slot, baseModel);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
        SLIM_CONTEXT.remove();
    }

    public static class CustomBackpackItemRenderer
    extends GeoItemRenderer<BackpackItem> {
        public CustomBackpackItemRenderer() {
            super((GeoModel)new BackpackModels());
        }
    }
}

