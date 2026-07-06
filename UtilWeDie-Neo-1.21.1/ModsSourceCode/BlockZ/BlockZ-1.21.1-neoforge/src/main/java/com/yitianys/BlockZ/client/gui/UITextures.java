package com.yitianys.BlockZ.client.gui;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.resources.ResourceLocation;

/**
 * GUI 贴图引用。仅保留 KEEP（库存界面槽位图标 + 暗角覆盖）。
 * DROP（HUD hub/* 与 mainmenu/* 贴图引用）已移除。
 */
public class UITextures {
    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, path);
    }

    public static final ResourceLocation SLOT_HEADWEAR = tex("textures/gui/inventory/slot_headwear.png");
    public static final ResourceLocation SLOT_HEADGEAR = tex("textures/gui/inventory/slot_headgear.png");
    public static final ResourceLocation SLOT_MASK = SLOT_HEADGEAR;
    public static final ResourceLocation SLOT_GLASSES = SLOT_HEADWEAR;
    public static final ResourceLocation SLOT_VEST = tex("textures/gui/inventory/slot_vest.png");
    public static final ResourceLocation SLOT_SHIRT = tex("textures/gui/inventory/slot_shirt.png");
    public static final ResourceLocation SLOT_PANTS = tex("textures/gui/inventory/slot_pants.png");
    public static final ResourceLocation SLOT_SHOES = tex("textures/gui/inventory/slot_shoes.png");
    public static final ResourceLocation SLOT_GLOVES = tex("textures/gui/inventory/slot_gloves.png");
    public static final ResourceLocation SLOT_BACKPACK = tex("textures/gui/inventory/slot_backpack.png");
    public static final ResourceLocation SLOT_OFFHAND = tex("textures/gui/inventory/slot_gloves.png");

    public static final ResourceLocation OVERLAY_DARK = tex("textures/gui/overlay/dark_overlay.png");
}
