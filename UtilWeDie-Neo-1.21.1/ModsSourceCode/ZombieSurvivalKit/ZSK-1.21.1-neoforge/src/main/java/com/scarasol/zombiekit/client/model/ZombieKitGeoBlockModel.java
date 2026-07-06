package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class ZombieKitGeoBlockModel<T extends BlockEntity & GeoAnimatable> extends GeoModel<T> {


    @Override
    public ResourceLocation getModelResource(T animatable) {
        String id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "geo/" + id + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String id  = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "textures/block/" + id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String id  = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(animatable.getType()).getPath();
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "animations/" + id + ".animation.json");
    }
}
