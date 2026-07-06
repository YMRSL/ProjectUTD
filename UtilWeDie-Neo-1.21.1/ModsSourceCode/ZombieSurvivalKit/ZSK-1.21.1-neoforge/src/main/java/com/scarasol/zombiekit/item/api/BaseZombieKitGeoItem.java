package com.scarasol.zombiekit.item.api;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoItem;

public interface BaseZombieKitGeoItem extends GeoItem {
    ResourceLocation getTexture();
    ResourceLocation getModel();
}
