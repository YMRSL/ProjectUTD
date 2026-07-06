package com.utdpatch.doomsday.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Removes the ground shadow under dropped TaCZ guns. {@code EntityRenderDispatcher.render} draws the
 * entity shadow via the private static {@code renderShadow}; we skip that call for gun item entities
 * ({@code tacz:modern_kinetic_gun}) so they lie flat with no shadow, leaving every other entity's
 * shadow untouched.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityGunShadowMixin {

    @Shadow
    private static void renderShadow(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity,
                                     float weight, float partialTicks, LevelReader level, float size) {
    }

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"),
        require = 0
    )
    private void utd$skipGunShadow(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity,
            float weight, float partialTicks, LevelReader level, float size) {
        if (entity instanceof ItemEntity itemEntity) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem());
            if (id != null && id.toString().equals("tacz:modern_kinetic_gun")) {
                return;
            }
        }
        renderShadow(poseStack, bufferSource, entity, weight, partialTicks, level, size);
    }
}
