package com.scarasol.zombiekit.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.scarasol.zombiekit.inventory.MortarRackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MortarRackScreen extends AbstractContainerScreen<MortarRackMenu> {

    private static final ResourceLocation texture = ResourceLocation.parse("minecraft:textures/gui/container/dispenser.png");
    private static final ResourceLocation shooting_parameters_table_slot = ResourceLocation.parse("zombiekit:textures/screens/gui/shooting_parameters_table_slot.png");

    public MortarRackScreen(MortarRackMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.pose().pushPose();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(texture, i, j, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(shooting_parameters_table_slot, this.leftPos + 25, this.topPos + 34, 0, 0, 18, 18, 18, 18);
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }
}
