package com.scarasol.sona.client.gui;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.manager.RotManager;
import com.scarasol.sona.manager.RustManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// 该类是被 mixin(GuiMixin/AbstractContainerScreenMixin)直接调用的工具类，不订阅事件。
// 1.21.1 下 @EventBusSubscriber 要求至少一个 @SubscribeEvent 方法，否则注册时抛错崩溃，故移除该注解。
@OnlyIn(Dist.CLIENT)
public class ItemMarkHandler {

    private static final ResourceLocation SAFE = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/safe.png");
    private static final ResourceLocation MILD = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/mild.png");
    private static final ResourceLocation BAD = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/bad.png");
    private static final ResourceLocation AWFUL = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/awful.png");
    private static final ResourceLocation WAXED = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/waxed.png");
    private static final ResourceLocation WARPED = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/warped.png");

    // 0.5 alpha tint, replaces the 1.20.1 RenderSystem.setShaderColor(1,1,1,0.5f) approach.
    private static final float HALF_ALPHA_F = 0.5F;

    public static void renderMark(GuiGraphics guiGraphics, Slot slot) {
        if (!slot.hasItem())
            return;
        if (CommonConfig.ROT_OPEN.get() && RotManager.canBeRotten(slot.getItem()) && isEdible(slot.getItem())) {
            renderRot(guiGraphics, slot.getItem(), slot.x, slot.y);
        } else if (CommonConfig.RUST_OPEN.get() && RustManager.canBeRust(slot.getItem())) {
            renderRust(guiGraphics, slot.getItem(), slot.x, slot.y);
        }
    }

    public static void renderMark(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        if (itemStack.isEmpty())
            return;
        if (CommonConfig.ROT_OPEN.get() && RotManager.canBeRotten(itemStack) && isEdible(itemStack)) {
            renderRot(guiGraphics, itemStack, x, y);
        } else if (CommonConfig.RUST_OPEN.get() && RustManager.canBeRust(itemStack)) {
            renderRust(guiGraphics, itemStack, x, y);
        }
    }

    private static boolean isEdible(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD);
    }

    private static void renderRot(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        double value = RotManager.getRot(itemStack);
        // 1.21.1: GuiGraphics.blit has no per-call color overload -> use setColor(alpha) + plain blit + reset.
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, HALF_ALPHA_F);
        if (RotManager.isWarped(itemStack))
            guiGraphics.blit(WARPED, x, y, 0, 0, 16, 16, 16, 16);
        guiGraphics.blit(rotLevel(value), x, y, 0, 0, 8, 8, 8, 8);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderRust(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        double value = RustManager.getRust(itemStack);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, HALF_ALPHA_F);
        guiGraphics.blit(rustLevel(value), x, y, 0, 0, 8, 8, 8, 8);
        if (RustManager.isWaxed(itemStack)) {
            guiGraphics.blit(WAXED, x, y + 8, 0, 0, 8, 8, 8, 8);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation rustLevel(double value) {
        if (value >= 70) {
            return AWFUL;
        } else if (value >= 40) {
            return BAD;
        } else {
            return SAFE;
        }
    }

    private static ResourceLocation rotLevel(double value) {
        if (value >= 90) {
            return AWFUL;
        } else if (value >= 70) {
            return BAD;
        } else if (value >= 40) {
            return MILD;
        } else {
            return SAFE;
        }
    }
}
