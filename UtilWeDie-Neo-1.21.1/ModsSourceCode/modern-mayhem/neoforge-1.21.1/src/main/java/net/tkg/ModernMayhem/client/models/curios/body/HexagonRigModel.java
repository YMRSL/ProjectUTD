package net.tkg.ModernMayhem.client.models.curios.body;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.body.HexagonRigItem;
import software.bernie.geckolib.model.GeoModel;

public class HexagonRigModel
extends GeoModel<HexagonRigItem> {
    public ResourceLocation getModelResource(HexagonRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/body/hexagon_rig.geo.json");
    }

    public ResourceLocation getTextureResource(HexagonRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/body/hexagon_rig.png");
    }

    public ResourceLocation getAnimationResource(HexagonRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animation/empty.animation.json");
    }
}

