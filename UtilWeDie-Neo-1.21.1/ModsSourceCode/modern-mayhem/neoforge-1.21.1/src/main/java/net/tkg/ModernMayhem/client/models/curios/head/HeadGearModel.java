package net.tkg.ModernMayhem.client.models.curios.head;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.head.HeadGearItems;
import software.bernie.geckolib.model.GeoModel;

public class HeadGearModel
extends GeoModel<HeadGearItems> {
    public ResourceLocation getModelResource(HeadGearItems animatable) {
        return switch (animatable.getType()) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/head/balaclava.geo.json");
            case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/glasses.geo.json");
            case 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/goggles.geo.json");
            case 3 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/earwear/headset.geo.json");
            case 4 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/head/military_balaclava.geo.json");
            case 5 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/head/gp5_gas_mask.geo.json");
            default -> throw new IllegalStateException("Unexpected value: no such armor type as " + animatable.getType());
        };
    }

    public ResourceLocation getTextureResource(HeadGearItems animatable) {
        switch (animatable.getType()) {
            case 0: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/head/balaclava.png");
            }
            case 1: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_glasses.png");
            }
            case 2: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_goggles.png");
            }
            case 3: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/earwear/black_headset.png");
            }
            case 4: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/head/black_military_balaclava.png");
            }
            case 5: {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/head/gp5_gas_mask.png");
            }
        }
        throw new IllegalStateException("Unexpected value: no such armor type as " + animatable.getType());
    }

    public ResourceLocation getAnimationResource(HeadGearItems animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/empty.animation.json");
    }
}

