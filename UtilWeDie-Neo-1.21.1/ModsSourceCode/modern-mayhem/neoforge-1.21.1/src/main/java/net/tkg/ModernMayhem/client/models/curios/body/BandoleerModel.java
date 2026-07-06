package net.tkg.ModernMayhem.client.models.curios.body;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.body.BandoleerItem;
import software.bernie.geckolib.model.GeoModel;

public class BandoleerModel
extends GeoModel<BandoleerItem> {
    public ResourceLocation getModelResource(BandoleerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/body/bandoleer.geo.json");
    }

    public ResourceLocation getTextureResource(BandoleerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/body/tan_bandoleer.png");
    }

    public ResourceLocation getAnimationResource(BandoleerItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animation/empty.animation.json");
    }
}

