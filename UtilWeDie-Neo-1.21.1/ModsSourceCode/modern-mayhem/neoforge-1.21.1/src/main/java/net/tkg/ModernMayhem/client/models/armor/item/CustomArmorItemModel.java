package net.tkg.ModernMayhem.client.models.armor.item;

import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.server.item.armor.CustomArmorItem;
import software.bernie.geckolib.model.GeoModel;

public class CustomArmorItemModel
extends GeoModel<CustomArmorItem> {
    public ResourceLocation getModelResource(CustomArmorItem item) {
        return this.getDynamicPath(item, "geo/armor/item", "geo.json");
    }

    public ResourceLocation getTextureResource(CustomArmorItem item) {
        return this.getDynamicPath(item, "textures/item/armor", "png");
    }

    public ResourceLocation getAnimationResource(CustomArmorItem item) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/empty.animation.json");
    }

    private ResourceLocation getDynamicPath(CustomArmorItem item, String folder, String extension) {
        ResourceLocation registryId = item.builtInRegistryHolder().key().location();
        if (registryId == null) {
            throw new IllegalStateException("Item does not have a registry name: " + item);
        }
        return ResourceLocation.fromNamespaceAndPath((String)registryId.getNamespace(), (String)String.format("%s/%s.%s", folder, registryId.getPath(), extension));
    }
}

