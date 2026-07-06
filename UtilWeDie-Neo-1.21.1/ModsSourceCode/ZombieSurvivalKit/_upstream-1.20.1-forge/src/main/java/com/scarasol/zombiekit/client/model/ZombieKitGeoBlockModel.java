package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

public class ZombieKitGeoBlockModel<T extends BlockEntity & GeoAnimatable> extends GeoModel<T> {


    @Override
    public ResourceLocation getModelResource(T animatable) {
        String id = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(animatable.getType()).getPath();
        return new ResourceLocation(ZombieKitMod.MODID, "geo/" + id + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        String id  = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(animatable.getType()).getPath();
        return new ResourceLocation(ZombieKitMod.MODID, "textures/block/" + id + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String id  = ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(animatable.getType()).getPath();
        return new ResourceLocation(ZombieKitMod.MODID, "animations/" + id + ".animation.json");
    }
}
