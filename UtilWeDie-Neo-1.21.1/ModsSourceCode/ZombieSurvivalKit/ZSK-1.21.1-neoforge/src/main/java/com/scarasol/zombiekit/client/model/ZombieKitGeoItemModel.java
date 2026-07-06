package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.item.api.BaseZombieKitGeoItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.model.GeoModel;

public class ZombieKitGeoItemModel<T extends BaseZombieKitGeoItem> extends GeoModel<T> {

    @Override
    public ResourceLocation getAnimationResource(T animatable) {
        String id = "";
        if (animatable instanceof Item item) {
            id = BuiltInRegistries.ITEM.getKey(item).getPath();
        }
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "animations/" + id + ".animation.json");
    }

    @Override
    public ResourceLocation getModelResource(T animatable) {
        return animatable.getModel();
    }

    @Override
    public ResourceLocation getTextureResource(T animatable) {
        return animatable.getTexture();
    }
}
