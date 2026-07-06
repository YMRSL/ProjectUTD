package net.tkg.ModernMayhem.client.models.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tkg.ModernMayhem.client.item.NVGFirstPersonFakeItem;
import net.tkg.ModernMayhem.client.models.curios.facewear.GenericSpecialGogglesModel;
import net.tkg.ModernMayhem.server.item.curios.facewear.NVGGogglesItem;
import net.tkg.ModernMayhem.server.item.curios.facewear.TVGGogglesItem;
import net.tkg.ModernMayhem.server.item.curios.facewear.VisorItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import software.bernie.geckolib.model.GeoModel;

public class NVGFirstPersonModel
extends GeoModel<NVGFirstPersonFakeItem> {
    private final Minecraft mc = Minecraft.getInstance();

    public ResourceLocation getModelResource(NVGFirstPersonFakeItem animatable) {
        ItemStack stack = this.getStack();
        boolean hasCoti = GenericSpecialGogglesItem.hasCoti(stack);
        return switch (this.getType()) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/gpnvg_fpm.geo.json");
            case 1 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/pvs14_coti_fpm.geo.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/pvs14_fpm.geo.json");
            }
            case 2 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/pvs7_coti_fpm.geo.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/pvs7_fpm.geo.json");
            }
            case 3 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/visor_fpm.geo.json");
            case 4 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"geo/fpm/facewear/tvg_fpm.geo.json");
            default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
        };
    }

    public ResourceLocation getTextureResource(NVGFirstPersonFakeItem animatable) {
        int type = this.getType();
        int variant = this.getVariant();
        ItemStack stack = this.getStack();
        boolean hasCoti = GenericSpecialGogglesItem.hasCoti(stack);
        if (type == 3) {
            return switch (variant) {
                case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/black_visor_transparent.png");
                case 1 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/item/curios/facewear/tan_visor_transparent.png");
                default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
            };
        }
        return GenericSpecialGogglesModel.getTextureResourceWithCoti(type, variant, hasCoti);
    }

    public ResourceLocation getAnimationResource(NVGFirstPersonFakeItem animatable) {
        ItemStack stack = this.getStack();
        boolean hasCoti = GenericSpecialGogglesItem.hasCoti(stack);
        return switch (this.getType()) {
            case 0 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/gpnvg_fpa.animation.json");
            case 1 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/pvs14_coti_fpa.animation.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/pvs14_fpa.animation.json");
            }
            case 2 -> {
                if (hasCoti) {
                    yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/pvs7_coti_fpa.animation.json");
                }
                yield ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/pvs7_fpa.animation.json");
            }
            case 3 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/visor_fpa.animation.json");
            case 4 -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"animations/item/fpa/facewear/tvg_fpa.animation.json");
            default -> ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"NOT_FOUND");
        };
    }

    private ItemStack getStack() {
        if (this.mc.player == null) {
            return ItemStack.EMPTY;
        }
        return CuriosUtil.getFaceWearItem((Player)this.mc.player);
    }

    private int getType() {
        ItemStack facewear = this.getStack();
        Item item = facewear.getItem();
        if (item instanceof NVGGogglesItem) {
            NVGGogglesItem nvgGogglesItem = (NVGGogglesItem)item;
            return nvgGogglesItem.getConfig().getType();
        }
        item = facewear.getItem();
        if (item instanceof VisorItem) {
            VisorItem visorItem = (VisorItem)item;
            return visorItem.getConfig().getType();
        }
        item = facewear.getItem();
        if (item instanceof TVGGogglesItem) {
            TVGGogglesItem tvgGogglesItem = (TVGGogglesItem)item;
            return tvgGogglesItem.getConfig().getType();
        }
        return -1;
    }

    private int getVariant() {
        ItemStack facewear = this.getStack();
        Item item = facewear.getItem();
        if (item instanceof NVGGogglesItem) {
            NVGGogglesItem nvgGogglesItem = (NVGGogglesItem)item;
            return nvgGogglesItem.getConfig().getVariant();
        }
        item = facewear.getItem();
        if (item instanceof VisorItem) {
            VisorItem visorItem = (VisorItem)item;
            return visorItem.getConfig().getVariant();
        }
        item = facewear.getItem();
        if (item instanceof TVGGogglesItem) {
            TVGGogglesItem tvgGogglesItem = (TVGGogglesItem)item;
            return tvgGogglesItem.getConfig().getVariant();
        }
        return -1;
    }
}

