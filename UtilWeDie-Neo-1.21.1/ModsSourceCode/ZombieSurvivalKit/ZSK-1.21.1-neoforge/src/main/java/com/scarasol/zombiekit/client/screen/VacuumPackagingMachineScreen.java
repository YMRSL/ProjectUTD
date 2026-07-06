package com.scarasol.zombiekit.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.scarasol.zombiekit.block.entity.VacuumPackagingMachineBlockEntity;
import com.scarasol.zombiekit.inventory.VacuumPackagingMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class VacuumPackagingMachineScreen extends AbstractContainerScreen<VacuumPackagingMachineMenu> {

    private final Level level;
    private final int x, y, z;
    private final Player player;

    private static final ResourceLocation texture = ResourceLocation.parse("zombiekit:textures/screens/gui/gui_with_slots.png");
    private static final ResourceLocation slot = ResourceLocation.parse("zombiekit:textures/screens/gui/slot.png");
    private static final ResourceLocation battery_slot = ResourceLocation.parse("zombiekit:textures/screens/gui/battery_slot.png");
    private static final ResourceLocation plastic_bag_slot = ResourceLocation.parse("zombiekit:textures/screens/gui/plastic_bag_slot.png");
    private static final ResourceLocation arrow_empty = ResourceLocation.parse("zombiekit:textures/screens/gui/arrow_empty.png");
    private static final ResourceLocation arrow = ResourceLocation.parse("zombiekit:textures/screens/gui/arrow.png");

    public VacuumPackagingMachineScreen(VacuumPackagingMachineMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.level = container.level;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.imageWidth = 180;
        this.imageHeight = 175;
        this.player = container.player;
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
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        guiGraphics.blit(slot, this.leftPos + 30, this.topPos + 20, 0, 0, 18, 18 ,18, 18);
        guiGraphics.blit(slot, this.leftPos + 80, this.topPos + 39, 0, 0, 18, 18 ,18, 18);
        guiGraphics.blit(arrow_empty, this.leftPos + 50, this.topPos + 40, 0, 0, 24, 17 ,24, 17);
        guiGraphics.blit(arrow, this.leftPos + 50, this.topPos + 40, 0, 0, (int) (24 * this.menu.getPackagingProgress()), 17 ,24, 17);
        guiGraphics.blit(plastic_bag_slot, this.leftPos + 30, this.topPos + 60, 0, 0, 18, 18 ,18, 18);
        guiGraphics.blit(battery_slot, this.leftPos + 132, this.topPos + 60, 0, 0, 18, 18 ,18, 18);
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

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY + 10, 4210752, false);
    }
}
