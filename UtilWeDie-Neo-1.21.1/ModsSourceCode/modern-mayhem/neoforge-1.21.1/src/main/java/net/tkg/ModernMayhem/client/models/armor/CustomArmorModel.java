package net.tkg.ModernMayhem.client.models.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.tkg.ModernMayhem.client.renderer.armor.CustomArmorRenderer;
import net.tkg.ModernMayhem.server.item.armor.CustomArmorItem;
import software.bernie.geckolib.model.GeoModel;

public class CustomArmorModel
extends GeoModel<CustomArmorItem> {
    public ResourceLocation getModelResource(CustomArmorItem customArmorItem) {
        boolean isSlim = CustomArmorRenderer.SLIM_CONTEXT.get();
        switch (customArmorItem.getConfig().getName()) {
            case "kevlar": {
                switch (customArmorItem.getType()) {
                    case HELMET: {
                        return switch (customArmorItem.getVariant()) {
                            case 0, 2, 6 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/combat_helmet.geo.json");
                            case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/ssh68_helmet.geo.json");
                            default -> throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
                        };
                    }
                    case LEGGINGS: {
                        return switch (customArmorItem.getVariant()) {
                            case 0, 1, 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/kevlar_clothing.geo.json");
                            case 3, 4, 5 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/iola.geo.json");
                            default -> throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
                        };
                    }
                }
                if (isSlim) {
                    return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/kevlar_clothing_thin.geo.json");
                }
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/kevlar_clothing.geo.json");
            }
            case "nothing": {
                switch (customArmorItem.getType()) {
                    case HELMET: {
                        switch (customArmorItem.getVariant()) {
                            case 0: {
                                break;
                            }
                            default: {
                                throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
                            }
                        }
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/head_mount.geo.json");
                    }
                }
                throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
            }
            case "ronin": {
                switch (customArmorItem.getType()) {
                    case HELMET: {
                        switch (customArmorItem.getVariant()) {
                            case 0: {
                                break;
                            }
                            default: {
                                throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
                            }
                        }
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/ronin.geo.json");
                    }
                }
                throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
            }
            case "hazmat": {
                return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/armor/hazmat_suit.geo.json");
            }
        }
        throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
    }

    public ResourceLocation getTextureResource(CustomArmorItem customArmorItem) {
        boolean isSlim = CustomArmorRenderer.SLIM_CONTEXT.get();
        switch (customArmorItem.getConfig().getName()) {
            case "kevlar": {
                switch (customArmorItem.getVariant()) {
                    case 0: {
                        if (customArmorItem.getType() == ArmorItem.Type.HELMET) {
                            return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/black_combat_helmet.png");
                        }
                        if (isSlim) {
                            return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/black_kevlar_clothing_thin.png");
                        }
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/black_kevlar_clothing.png");
                    }
                    case 1: {
                        if (customArmorItem.getType() == ArmorItem.Type.HELMET) {
                            return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/green_ssh68_helmet.png");
                        }
                        if (isSlim) {
                            return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/green_kevlar_clothing_thin.png");
                        }
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/green_kevlar_clothing.png");
                    }
                    case 2: {
                        if (customArmorItem.getType() == ArmorItem.Type.HELMET) {
                            return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/tan_combat_helmet.png");
                        }
                        if (isSlim) {
                            return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/tan_kevlar_clothing_thin.png");
                        }
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/tan_kevlar_clothing.png");
                    }
                    case 3: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/black_iola.png");
                    }
                    case 4: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/green_iola.png");
                    }
                    case 5: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/tan_iola.png");
                    }
                    case 6: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/c1300_helmet.png");
                    }
                }
                throw new IllegalStateException("Unexpected value: No such variant with id " + customArmorItem.getVariant());
            }
            case "nothing": {
                switch (customArmorItem.getVariant()) {
                    case 0: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/black_head_mount.png");
                    }
                }
                throw new IllegalStateException("Unexpected value: No such variant with id " + customArmorItem.getVariant());
            }
            case "ronin": {
                switch (customArmorItem.getVariant()) {
                    case 0: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/black_ronin.png");
                    }
                }
                throw new IllegalStateException("Unexpected value: No such variant with id " + customArmorItem.getVariant());
            }
            case "hazmat": {
                switch (customArmorItem.getVariant()) {
                    case 0: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/yellow_hazmat_suit.png");
                    }
                    case 1: {
                        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/armor/orange_hazmat_suit.png");
                    }
                }
                throw new IllegalStateException("Unexpected value: No such variant with id " + customArmorItem.getVariant());
            }
        }
        throw new IllegalStateException("Unexpected value: no such armor type as " + customArmorItem.getConfig().getName());
    }

    public ResourceLocation getAnimationResource(CustomArmorItem customArmorItem) {
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animation/empty.animation.json");
    }
}

