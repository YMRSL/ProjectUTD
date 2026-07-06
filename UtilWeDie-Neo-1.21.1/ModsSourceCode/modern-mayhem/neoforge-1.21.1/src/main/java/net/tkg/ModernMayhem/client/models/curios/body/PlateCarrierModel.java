package net.tkg.ModernMayhem.client.models.curios.body;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.body.PlateCarrierItem;
import software.bernie.geckolib.model.GeoModel;

public class PlateCarrierModel
extends GeoModel<PlateCarrierItem> {
    public ResourceLocation getModelResource(PlateCarrierItem animatable) {
        return switch (animatable.getType()) {
            case "default" -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/body/plate_carrier.geo.json");
            case "ammo" -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/body/plate_carrier_ammo.geo.json");
            case "pouches" -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/body/plate_carrier_pouches.geo.json");
            default -> throw new IllegalStateException("Unexpected value: no such type as " + animatable.getType());
        };
    }

    public ResourceLocation getTextureResource(PlateCarrierItem animatable) {
        return switch (animatable.getVariant()) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/body/black_plate_carrier.png");
            case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/body/tan_plate_carrier.png");
            default -> throw new IllegalStateException("Unexpected value: no such variant as " + animatable.getVariant());
        };
    }

    public ResourceLocation getAnimationResource(PlateCarrierItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animation/empty.animation.json");
    }
}

