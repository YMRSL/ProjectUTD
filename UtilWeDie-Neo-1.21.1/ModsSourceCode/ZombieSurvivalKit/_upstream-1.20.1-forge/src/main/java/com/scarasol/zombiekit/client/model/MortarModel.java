package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class
MortarModel extends GeoModel<MortarEntity> {
    @Override
    public ResourceLocation getAnimationResource(MortarEntity entity) {
        return new ResourceLocation("zombiekit", "animations/mortar.animation.json");
    }

    @Override
    public ResourceLocation getModelResource(MortarEntity entity) {
        return new ResourceLocation("zombiekit", "geo/" + entity.getModel() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MortarEntity entity) {
        return new ResourceLocation("zombiekit", "textures/entities/mortar.png");
    }

    @Override
    public void setCustomAnimations(MortarEntity animatable, long instanceId, AnimationState animationState) {
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX((entityData.headPitch() - 45) * Mth.DEG_TO_RAD);
            head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
        }

    }
}
