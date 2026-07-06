package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class MortarModel extends GeoModel<MortarEntity> {
    @Override
    public ResourceLocation getAnimationResource(MortarEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "animations/mortar.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MortarEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "geo/" + entity.getModel() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MortarEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "textures/entities/mortar.png");
    }

    @Override
    public void setCustomAnimations(MortarEntity animatable, long instanceId, AnimationState<MortarEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX((entityData.headPitch() - 45) * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

    }
}
