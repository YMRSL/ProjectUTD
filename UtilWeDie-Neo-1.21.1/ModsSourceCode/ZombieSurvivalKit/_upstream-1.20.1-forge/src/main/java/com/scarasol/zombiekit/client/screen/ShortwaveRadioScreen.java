package com.scarasol.zombiekit.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.scarasol.zombiekit.block.entity.ShortwaveRadioBlockEntity;
import com.scarasol.zombiekit.inventory.ShortwaveRadioMenu;
import com.scarasol.zombiekit.network.SyncBlockPacket;
import com.scarasol.zombiekit.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
public class ShortwaveRadioScreen extends AbstractContainerScreen<ShortwaveRadioMenu> {

    private final static HashMap<String, Object> guistate = ShortwaveRadioMenu.guistate;
    private final Level level;
    private final int x, y, z;
    private final Player player;
    EditBox content;
    Button confirm;

    public ShortwaveRadioScreen(ShortwaveRadioMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.level = container.level;
        this.x = container.x;
        this.y = container.y;
        this.z = container.z;
        this.player = container.player;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        content.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();

    }

    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (content.isFocused())
            return content.keyPressed(key, b, c);
        return super.keyPressed(key, b, c);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        content.tick();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String contentValue = content.getValue();
        super.resize(minecraft, width, height);
        content.setValue(contentValue);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("gui.zombiekit.shortwave_radio_gui.title"), -66, 23, 10526880, false);
    }

    @Override
    public void init() {
        super.init();
        content = new EditBox(this.font, this.leftPos - 66, this.topPos + 43, 308, 18, Component.translatable("gui.zombiekit.shortwave_radio_gui.content"));
        content.setMaxLength(32767);
        guistate.put("text:content", content);
        this.addWidget(this.content);
        confirm = Button.builder(Component.translatable("gui.zombiekit.shortwave_radio_gui.confirm"), e -> {
            player.closeContainer();
            NetworkHandler.PACKET_HANDLER.sendToServer(new SyncBlockPacket(0, new BlockPos(x, y, z), content.getValue()));
            SyncBlockPacket.handleButton(level.getBlockEntity(new BlockPos(x, y, z)), 0, content.getValue());
        }).bounds(this.leftPos + 52, this.topPos + 85, 70, 20).build();
        guistate.put("button:confirm", confirm);
        BlockEntity blockEntity = level.getBlockEntity(BlockPos.containing(x, y, z));
        if (blockEntity instanceof ShortwaveRadioBlockEntity shortwaveRadioBlockEntity) {
            content.setValue(shortwaveRadioBlockEntity.getContent());
        }
        this.addRenderableWidget(confirm);
    }

}
