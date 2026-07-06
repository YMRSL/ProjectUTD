package net.mcreator.doomsdaydecoration.functionality;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Container screen for the lootable BlockEntity, native NeoForge 1.21.1 port of
 * Raiiiden/DoomsdayFunctionality's {@code DoomsdayScreen}.
 *
 * <p>Reuses vanilla's shulker-box GUI texture so no extra asset is shipped. Registered
 * client-side via {@code RegisterMenuScreensEvent} (see the client setup hook the
 * mount layer wires up).</p>
 */
public class DoomsdayScreen extends AbstractContainerScreen<DoomsdayContainerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    public DoomsdayScreen(DoomsdayContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 150;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
