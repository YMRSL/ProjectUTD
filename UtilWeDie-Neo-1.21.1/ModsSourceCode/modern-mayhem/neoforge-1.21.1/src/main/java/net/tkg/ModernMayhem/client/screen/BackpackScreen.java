package net.tkg.ModernMayhem.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.tkg.ModernMayhem.server.GUI.GenericBackpackGUI;
import org.jetbrains.annotations.NotNull;

public class BackpackScreen
extends AbstractContainerScreen<GenericBackpackGUI> {
    ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/gui/container/generic_backpack_inventory.png");
    public static final int playerInventoryLeftPos = 0;
    public static final int playerInventoryTopPos = 0;
    public static final int playerInventoryWidth = 176;
    public static final int playerInventoryHeight = 90;
    public static final int slotLeftPos = 7;
    public static final int slotTopPos = 7;
    public static final int slotSize = 18;
    public static final int cornerNWLeftPos = 178;
    public static final int cornerNWTopPos = 0;
    public static final int cornerNWWidth = 7;
    public static final int cornerNWHeight = 7;
    public static final int cornerNELeftPos = 207;
    public static final int cornerNETopPos = 0;
    public static final int cornerNEWidth = 7;
    public static final int cornerNEHeight = 7;
    public static final int cornerSWLeftPos = 178;
    public static final int cornerSWTopPos = 29;
    public static final int cornerSWWidth = 7;
    public static final int cornerSWHeight = 7;
    public static final int cornerSELeftPos = 207;
    public static final int cornerSETopPos = 29;
    public static final int cornerSEWidth = 7;
    public static final int cornerSEHeight = 7;
    public static final int topBorderLeftPos = 187;
    public static final int topBorderTopPos = 0;
    public static final int topBorderWidth = 18;
    public static final int topBorderHeight = 7;
    public static final int bottomBorderLeftPos = 187;
    public static final int bottomBorderTopPos = 29;
    public static final int bottomBorderWidth = 18;
    public static final int bottomBorderHeight = 7;
    public static final int leftBorderLeftPos = 178;
    public static final int leftBorderTopPos = 9;
    public static final int leftBorderWidth = 7;
    public static final int leftBorderHeight = 18;
    public static final int rightBorderLeftPos = 207;
    public static final int rightBorderTopPos = 9;
    public static final int rightBorderWidth = 7;
    public static final int rightBorderHeight = 18;
    public int slotPerLine = ((GenericBackpackGUI)this.menu).getSlotPerLine();
    public int numberOfLine = ((GenericBackpackGUI)this.menu).getNumberOfLine();
    public int slotsVerticalSize = 18 * this.numberOfLine;
    public int slotsHorizontalSize = 18 * this.slotPerLine;
    public int backpackPartWidth = Math.max(7, 7) + this.slotsHorizontalSize + Math.max(7, 7);
    public int backpackPartHeight = Math.max(7, 7) + this.slotsVerticalSize;

    public BackpackScreen(GenericBackpackGUI pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = Math.max(this.backpackPartWidth, 176);
        this.imageHeight = this.backpackPartHeight + 90 + 3;
    }

    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        this.renderBackpackInventory(pGuiGraphics);
    }

    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTicks) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTicks);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderBackpackInventory(GuiGraphics pGuiGraphics) {
        int i;
        int backpackLeftPos = this.leftPos + (this.imageWidth - this.backpackPartWidth) / 2;
        pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos, this.topPos, 178, 0, 7, 7);
        pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos + 7 + this.slotsHorizontalSize, this.topPos, 207, 0, 7, 7);
        pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos, this.topPos + 7 + this.slotsVerticalSize, 178, 29, 7, 7);
        pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos + 7 + this.slotsHorizontalSize, this.topPos + 7 + this.slotsVerticalSize, 207, 29, 7, 7);
        for (i = 0; i < this.slotPerLine; ++i) {
            pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos + 7 + i * 18, this.topPos, 187, 0, 18, 7);
            pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos + 7 + i * 18, this.topPos + 7 + this.slotsVerticalSize, 187, 29, 18, 7);
        }
        for (i = 0; i < this.numberOfLine; ++i) {
            pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos, this.topPos + 7 + i * 18, 178, 9, 7, 18);
            pGuiGraphics.blit(this.BACKGROUND_TEXTURE, backpackLeftPos + 7 + this.slotsHorizontalSize, this.topPos + 7 + i * 18, 207, 9, 7, 18);
        }
        for (int line = 0; line < this.numberOfLine; ++line) {
            for (int column = 0; column < this.slotPerLine; ++column) {
                int x = backpackLeftPos + 7 + column * 18;
                int y = this.topPos + 7 + line * 18;
                pGuiGraphics.blit(this.BACKGROUND_TEXTURE, x, y, 7, 7, 18, 18);
            }
        }
        pGuiGraphics.blit(this.BACKGROUND_TEXTURE, this.leftPos + (this.imageWidth - 176) / 2, this.topPos + this.imageHeight - 90, 0, 0, 176, 90);
    }
}

