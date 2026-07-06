package net.mcreator.survivalinstinct.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import net.mcreator.survivalinstinct.world.inventory.CashRegisterGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

public class CashRegisterGUIScreen
extends AbstractContainerScreen<CashRegisterGUIMenu> {
    private static final HashMap<String, Object> guistate = CashRegisterGUIMenu.guistate;
    private final Level world;
    private final int x;
    private final int y;
    private final int z;
    private final Player entity;
    private static final ResourceLocation texture = ResourceLocation.parse("survival_instinct:textures/screens/cash_register_gui.png");

    public CashRegisterGUIScreen(CashRegisterGUIMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.world = container.world;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.entity = container.entity;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(texture, this.leftPos, this.topPos, 0.0f, 0.0f, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        RenderSystem.disableBlend();
    }

    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        return super.keyPressed(key, b, c);
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, (Component)Component.translatable((String)"gui.survival_instinct.cash_register_gui.label_refrigerator"), 6, 6, -12829636, false);
        guiGraphics.drawString(this.font, (Component)Component.translatable((String)"gui.survival_instinct.cash_register_gui.label_inventory"), 6, 72, -12829636, false);
    }

    public void init() {
        super.init();
    }
}

