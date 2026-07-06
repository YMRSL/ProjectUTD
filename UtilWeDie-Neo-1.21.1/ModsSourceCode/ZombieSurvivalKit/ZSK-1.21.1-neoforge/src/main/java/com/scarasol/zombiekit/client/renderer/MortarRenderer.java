package com.scarasol.zombiekit.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.scarasol.sona.util.SonaMath;
import com.scarasol.sona.util.SonaRenderer;
import com.scarasol.zombiekit.client.model.MortarModel;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MortarRenderer extends GeoEntityRenderer<MortarEntity> {
    public MortarRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MortarModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(MortarEntity mortarEntity, float p_114486_, float p_114487_, PoseStack poseStack, MultiBufferSource multiBufferSource, int p_114490_) {
        super.render(mortarEntity, p_114486_, p_114487_, poseStack, multiBufferSource, p_114490_);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            boolean flag = player.getInventory().hasAnyMatching(itemStack -> itemStack.is(ZombieKitItems.SHOOTING_PARAMETER.get()));
            if (flag || (ExoArmor.numberOfSuit(player) >= 4 && ExoArmor.getPower(player.getItemBySlot(EquipmentSlot.CHEST)) > 0))
                if (Minecraft.getInstance().screen == null && player.getVehicle() == mortarEntity) {

                    SonaRenderer.renderParabolaLightningBeam(poseStack, multiBufferSource, 2, 1, 0xFF0000, 0xFF0000, player.level().getGameTime() % 23, MortarEntity.VELOCITY, mortarEntity.getXRot(), mortarEntity.getYRot(), 0.05, mortarEntity.position(), mortarEntity.position(), player.level());
//                    VertexConsumer vertexconsumer = multiBufferSource.getBuffer(RenderType.lines());
//                    Vec3 dropPoint = SonaMath.parabolaDropPointCalculate(MortarEntity.VELOCITY, mortarEntity);
//                    if (!mortarEntity.level().getBlockState(BlockPos.containing(dropPoint)).isAir()) {
//                        dropPoint = BlockPos.containing(dropPoint).getCenter();
//                        dropPoint = dropPoint.subtract(mortarEntity.position());
//                        LevelRenderer.renderLineBox(poseStack, vertexconsumer, dropPoint.x - 2, dropPoint.y - 1, dropPoint.z - 2, dropPoint.x + 2, dropPoint.y + 1, dropPoint.z + 2, 1, 1, 1, 1);
//                    }

                }
        }


    }

    @Override
    public RenderType getRenderType(MortarEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, MortarEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

}
