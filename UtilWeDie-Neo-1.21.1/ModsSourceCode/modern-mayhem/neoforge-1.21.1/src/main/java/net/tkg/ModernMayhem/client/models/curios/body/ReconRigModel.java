package net.tkg.ModernMayhem.client.models.curios.body;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.curios.body.ReconRigItem;
import software.bernie.geckolib.model.GeoModel;

public class ReconRigModel
extends GeoModel<ReconRigItem> {
    public ResourceLocation getModelResource(ReconRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/body/recon_rig.geo.json");
    }

    public ResourceLocation getTextureResource(ReconRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/body/green_recon_rig.png");
    }

    public ResourceLocation getAnimationResource(ReconRigItem animatable) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animation/empty.animation.json");
    }
}

