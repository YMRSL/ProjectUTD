package net.tkg.ModernMayhem.client.models.curios.facewear;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import software.bernie.geckolib.model.GeoModel;

public class GenericSpecialGogglesModel<T extends GenericSpecialGogglesItem>
extends GeoModel<T> {
    private ItemStack currentStack = ItemStack.EMPTY;

    public ResourceLocation getModelResource(T animatable) {
        boolean hasCoti = !this.currentStack.isEmpty() && GenericSpecialGogglesItem.hasCoti(this.currentStack);
        return this.getModelResourceWithCoti(((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getType(), hasCoti);
    }

    public ResourceLocation getModelResource(T animatable, ItemStack stack) {
        boolean hasCoti = GenericSpecialGogglesItem.hasCoti(stack);
        return this.getModelResourceWithCoti(((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getType(), hasCoti);
    }

    private ResourceLocation getModelResourceWithCoti(int type, boolean hasCoti) {
        return switch (type) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/gpnvg.geo.json");
            case 1 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/pvs14_coti.geo.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/pvs14.geo.json");
            }
            case 2 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/pvs7_coti.geo.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/pvs7.geo.json");
            }
            case 3 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/visor.geo.json");
            case 4 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/item/curios/facewear/tvg.geo.json");
            default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
        };
    }

    public void setCurrentStack(ItemStack stack) {
        this.currentStack = stack;
    }

    public ResourceLocation getTextureResource(T animatable) {
        boolean hasCoti = !this.currentStack.isEmpty() && GenericSpecialGogglesItem.hasCoti(this.currentStack);
        return GenericSpecialGogglesModel.getTextureResourceWithCoti(((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getType(), ((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getVariant(), hasCoti);
    }

    public ResourceLocation getTextureResource(T animatable, ItemStack stack) {
        int type = ((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getType();
        int variant = ((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getVariant();
        boolean hasCoti = GenericSpecialGogglesItem.hasCoti(stack);
        return GenericSpecialGogglesModel.getTextureResourceWithCoti(type, variant, hasCoti);
    }

    public ResourceLocation getAnimationResource(T animatable) {
        boolean hasCoti = !this.currentStack.isEmpty() && GenericSpecialGogglesItem.hasCoti(this.currentStack);
        return this.getAnimationResourceWithCoti(((GenericSpecialGogglesItem)((Object)animatable)).getConfig().getType(), hasCoti);
    }

    private ResourceLocation getAnimationResourceWithCoti(int type, boolean hasCoti) {
        return switch (type) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/gpnvg.animation.json");
            case 1 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/pvs14_coti.animation.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/pvs14.animation.json");
            }
            case 2 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/pvs7_coti.animation.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/pvs7.animation.json");
            }
            case 3 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/visor.animation.json");
            case 4 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/curios/facewear/tvg.animation.json");
            default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
        };
    }

    public static ResourceLocation getTextureResource(int type, int variant) {
        return GenericSpecialGogglesModel.getTextureResourceWithCoti(type, variant, false);
    }

    public static ResourceLocation getTextureResourceWithCoti(int type, int variant, boolean hasCoti) {
        switch (type) {
            case 0: {
                return switch (variant) {
                    case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_gpnvg.png");
                    case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/tan_gpnvg.png");
                    case 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/green_gpnvg.png");
                    case 99 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/ultra_gamer_gpnvg.png");
                    default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                };
            }
            case 1: {
                if (hasCoti) {
                    return switch (variant) {
                        case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_pvs14_coti.png");
                        case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/tan_pvs14_coti.png");
                        case 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/green_pvs14_coti.png");
                        default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                    };
                }
                return switch (variant) {
                    case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_pvs14.png");
                    case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/tan_pvs14.png");
                    case 2 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/green_pvs14.png");
                    default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                };
            }
            case 2: {
                if (hasCoti) {
                    return switch (variant) {
                        case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_pvs7_coti.png");
                        default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                    };
                }
                return switch (variant) {
                    case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_pvs7.png");
                    default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                };
            }
            case 3: {
                return switch (variant) {
                    case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_visor.png");
                    case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/tan_visor.png");
                    default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                };
            }
            case 4: {
                return switch (variant) {
                    case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_tvg.png");
                    default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
                };
            }
        }
        return ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
    }
}

