package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.mechanics.HeavyMachineGunEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class HeavyMachineGunModel extends GeoModel<HeavyMachineGunEntity> {


    @Override
    public ResourceLocation getAnimationResource(HeavyMachineGunEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "animations/m2_machinegun_bedrock.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(HeavyMachineGunEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "geo/m2_machinegun_bedrock.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HeavyMachineGunEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "textures/entities/" + entity.getTexture() + ".png");
    }

    @Override
    public void setCustomAnimations(HeavyMachineGunEntity animatable, long instanceId, AnimationState<HeavyMachineGunEntity> animationState) {
        GeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

    }

}
