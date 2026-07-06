package net.tkg.ModernMayhem.client.models.curios.back;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.back.BackpackItem;
import software.bernie.geckolib.model.GeoModel;

public class BackpackModels
extends GeoModel<BackpackItem> {
    public ResourceLocation getModelResource(BackpackItem animatable) {
        return switch (animatable.getTier() - 1) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/back/backpack_t1.geo.json");
            case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/back/backpack_t2.geo.json");
            case 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/back/backpack_t3.geo.json");
            default -> throw new IllegalStateException("Unexpected value: no such tier as " + animatable.getTier());
        };
    }

    public ResourceLocation getTextureResource(BackpackItem animatable) {
        return switch (animatable.getVariant()) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/back/black_backpack.png");
            case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/back/green_backpack.png");
            case 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/back/tan_backpack.png");
            default -> throw new IllegalStateException("Unexpected value: no such variant as " + animatable.getVariant());
        };
    }

    public ResourceLocation getAnimationResource(BackpackItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animation/empty.animation.json");
    }
}

