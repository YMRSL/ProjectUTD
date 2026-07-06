package net.tkg.ModernMayhem.client.models.curios.knee;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.knee.KneepadItems;
import software.bernie.geckolib.model.GeoModel;

public class KneepadModel
extends GeoModel<KneepadItems> {
    public ResourceLocation getModelResource(KneepadItems animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/knee/knee_pads.geo.json");
    }

    public ResourceLocation getTextureResource(KneepadItems animatable) {
        switch (animatable.getVariant()) {
            case 0: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/knee/black_knee_pads.png");
            }
            case 1: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/knee/green_knee_pads.png");
            }
            case 2: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/knee/tan_knee_pads.png");
            }
        }
        throw new IllegalStateException("Unexpected value: no such visor variant as " + animatable.getVariant());
    }

    public ResourceLocation getAnimationResource(KneepadItems animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/empty.animation.json");
    }
}

