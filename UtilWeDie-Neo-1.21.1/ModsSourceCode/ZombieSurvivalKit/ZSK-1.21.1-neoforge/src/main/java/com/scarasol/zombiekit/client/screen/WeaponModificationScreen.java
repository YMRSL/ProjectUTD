package com.scarasol.zombiekit.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.inventory.WeaponModificationMenu;
import com.scarasol.zombiekit.item.api.SingleHandWeapon;
import com.scarasol.zombiekit.item.weapon.parts.BattleParts;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;

import java.util.HashMap;

public class WeaponModificationScreen extends AbstractContainerScreen<WeaponModificationMenu> {
    private final static HashMap<String, Object> guiState = WeaponModificationMenu.guiState;
    private final ItemStack itemStack;

    private static final ResourceLocation texture = ResourceLocation.parse("zombiekit:textures/screens/gui/gui_with_slots.png");
    private static final ResourceLocation slot = ResourceLocation.parse("zombiekit:textures/screens/gui/slot.png");
    private static final ResourceLocation lock_slot = ResourceLocation.parse("zombiekit:textures/screens/gui/lock_slot.png");
    private static final ResourceLocation line1 = ResourceLocation.parse("zombiekit:textures/screens/gui/line1.png");
    private static final ResourceLocation line2 = ResourceLocation.parse("zombiekit:textures/screens/gui/line2.png");
    private static final ResourceLocation line3 = ResourceLocation.parse("zombiekit:textures/screens/gui/line3.png");

    public WeaponModificationScreen(WeaponModificationMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 180;
        this.imageHeight = 175;
        this.itemStack = container.itemStack;
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
        guiGraphics.pose().scale(4, 4, 0);
        guiGraphics.renderItem(itemStack, (this.leftPos + 58) / 4 - 1, (this.topPos + 16) / 4);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        guiGraphics.blit(line1, this.leftPos + 48, this.topPos + 18, 0, 0, 57, 11, 57, 11);
        guiGraphics.blit(line2, this.leftPos + 84, this.topPos + 56, 0, 0, 60, 9, 60, 9);
        guiGraphics.blit(line3, this.leftPos + 36, this.topPos + 65, 0, 0, 31, 9, 31, 9);
        if (BattleParts.unlock(itemStack))
            guiGraphics.blit(slot, this.leftPos + 40, this.topPos + 15, 0, 0, 18, 18, 18, 18);
        else
            guiGraphics.blit(lock_slot, this.leftPos + 40, this.topPos + 15, 0, 0, 18, 18, 18, 18);
        if (ChargingParts.unlock(itemStack))
            guiGraphics.blit(slot, this.leftPos + 144, this.topPos + 50, 0, 0, 18, 18, 18, 18);
        else
            guiGraphics.blit(lock_slot, this.leftPos + 144, this.topPos + 50, 0, 0, 18, 18, 18, 18);
        if (GripParts.unlock(itemStack))
            guiGraphics.blit(slot, this.leftPos + 20, this.topPos + 60, 0, 0, 18, 18, 18, 18);
        else
            guiGraphics.blit(lock_slot, this.leftPos + 20, this.topPos + 60, 0, 0, 18, 18, 18, 18);
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
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.5f, 0.5f, 1);
        if (!"zh_cn".equals(Minecraft.getInstance().options.languageCode) || !this.menu.getSlot(0).hasItem())
            guiGraphics.drawString(this.font, Component.translatable("gui.zombiekit.weapon_modification_gui.battle_parts").withStyle(ChatFormatting.BOLD), 118, 26, 16777215, false);
        else
            guiGraphics.drawString(this.font, ((MutableComponent) this.menu.getSlot(0).getItem().getHoverName()).withStyle(ChatFormatting.BOLD), 118, 26, 16777215, false);
        if (!"zh_cn".equals(Minecraft.getInstance().options.languageCode) || !this.menu.getSlot(1).hasItem())
            guiGraphics.drawString(this.font, Component.translatable("gui.zombiekit.weapon_modification_gui.charging_parts").withStyle(ChatFormatting.BOLD), 185, 132, 16777215, false);
        else
            guiGraphics.drawString(this.font, ((MutableComponent) this.menu.getSlot(1).getItem().getHoverName()).withStyle(ChatFormatting.BOLD), 185, 132, 16777215, false);
        if (!"zh_cn".equals(Minecraft.getInstance().options.languageCode) || !this.menu.getSlot(2).hasItem())
            guiGraphics.drawString(this.font, Component.translatable("gui.zombiekit.weapon_modification_gui.grip_parts").withStyle(ChatFormatting.BOLD), 78, 120, 16777215, false);
        else
            guiGraphics.drawString(this.font, ((MutableComponent) this.menu.getSlot(2).getItem().getHoverName()).withStyle(ChatFormatting.BOLD), 78, 120, 16777215, false);
        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }

    @Override
    public void init() {
        super.init();
    }
}

