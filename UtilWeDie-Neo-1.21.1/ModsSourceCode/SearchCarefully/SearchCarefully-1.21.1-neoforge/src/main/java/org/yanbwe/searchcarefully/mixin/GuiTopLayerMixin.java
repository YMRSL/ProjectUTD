package org.yanbwe.searchcarefully.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.yanbwe.searchcarefully.Config;
import org.yanbwe.searchcarefully.animation.RotationAnimationHandler;
import org.yanbwe.searchcarefully.textures.CustomTextureHandler;
import org.yanbwe.searchcarefully.util.ItemStackHelper;

import com.mojang.blaze3d.systems.RenderSystem;

@Mixin(Gui.class)
public class GuiTopLayerMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderHotbarTopLayerMasks(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!Config.MASK_RENDER_ON_TOP.get()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Calculate hotbar slot positions (centered at bottom of screen)
        int hotbarCenterX = screenWidth / 2;
        int hotbarY = screenHeight - 22;
        int slotSize = 20;
        int slotSpacing = 2;
        int totalWidth = 9 * slotSize + 8 * slotSpacing;
        int startX = hotbarCenterX - totalWidth / 2;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && ItemStackHelper.hasRemainingSearchTime(stack)) {
                double remainingTime = ItemStackHelper.getRemainingSearchTime(stack);
                if (remainingTime > 0) {
                    int x = startX + i * (slotSize + slotSpacing);
                    int y = hotbarY;

                    // Render mask on top
                    RenderSystem.disableDepthTest();
                    try {
                        var maskTexture = CustomTextureHandler.getMaskTexture();
                        guiGraphics.blit(maskTexture, x, y, 400, 0, 0, 16, 16, 16, 16);
                    } catch (Exception e) {
                        guiGraphics.fill(x, y, x + 16, y + 16, 400, 0xFF000000);
                    }

                    // Render rotation animation
                    try {
                        var rotationTexture = CustomTextureHandler.getRotationAnimationTexture();
                        long currentTime = System.currentTimeMillis();
                        float[] position = RotationAnimationHandler.getRotatingPosition(
                            (long) (remainingTime * 1000L),
                            currentTime
                        );
                        guiGraphics.blit(rotationTexture,
                            (int) (x + position[0]),
                            (int) (y + position[1]),
                            450, 0, 0, 16, 16, 16, 16);
                    } catch (Exception e) {
                        // Ignore
                    }
                    RenderSystem.enableDepthTest();
                }
            }
        }
    }
}
