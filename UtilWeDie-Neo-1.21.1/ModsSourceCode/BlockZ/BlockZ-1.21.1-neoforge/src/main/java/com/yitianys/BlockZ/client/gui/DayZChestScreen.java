package com.yitianys.BlockZ.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;

/**
 * DayZ 风格的箱子界面（用于 Arclight 服务端无法用 RequestSwitch 时的本地回退）。
 *
 * <p>1.21.1 API 迁移：{@code Slot#x/#y} 改为直接赋值（官方映射 public 字段，取代 Forge SRG 反射）；
 * {@code renderBackground} 增加参数；{@code renderEntityInInventoryFollowsMouse} 改为包围盒签名。
 */
public class DayZChestScreen extends AbstractContainerScreen<ChestMenu> {

   public DayZChestScreen(ChestMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = UIConstants.GUI_WIDTH;
      this.imageHeight = UIConstants.GUI_HEIGHT;
   }

   @Override
   protected void init() {
      this.imageWidth = getTotalWidth();
      this.imageHeight = UIConstants.HEIGHT;

      super.init();

      repositionSlots();
   }

   private int getVicinityPanelWidth() {
      return 9 * 18 + 8; // 170
   }

   private int getTotalWidth() {
      return getVicinityPanelWidth() + 2 + UIConstants.PANEL_W + 2 + UIConstants.PANEL_W;
   }

   @Override
   protected void containerTick() {
      super.containerTick();
      repositionSlots();
   }

   private void repositionSlots() {
      // 注意：Slot 的 x, y 是相对于 guiLeft, guiTop 的相对坐标！

      int containerRows = this.menu.getRowCount();
      int containerSlots = containerRows * 9;

      // 1. Vicinity Panel (Container Slots) - 9 Columns
      int vicX = 4; // Padding inside panel
      int vicY = UIConstants.PANEL_Y + 10;

      for (int i = 0; i < containerSlots; i++) {
         if (i >= this.menu.slots.size()) break;
         Slot slot = this.menu.slots.get(i);

         int col = i % 9;
         int row = i / 9;

         setSlotPos(slot, vicX + col * UIConstants.SLOT_PITCH, vicY + row * UIConstants.SLOT_PITCH);
      }

      // 2. Inventory Panel (Player Backpack 9-35)
      int playerPanelStartRelX = getVicinityPanelWidth() + 2;
      int inventoryPanelStartRelX = playerPanelStartRelX + UIConstants.PANEL_W + 2;

      int invX = inventoryPanelStartRelX + 4;
      int invY = UIConstants.INVENTORY_SLOTS_Y;

      int playerStart = containerSlots;

      for (int i = 0; i < 27; i++) {
         int slotIndex = playerStart + i;
         if (slotIndex >= this.menu.slots.size()) break;
         Slot slot = this.menu.slots.get(slotIndex);

         int col = i % UIConstants.INVENTORY_COLS;
         int row = i / UIConstants.INVENTORY_COLS;

         setSlotPos(slot, invX + col * UIConstants.SLOT_PITCH, invY + row * UIConstants.SLOT_PITCH);
      }

      // 3. Hotbar Panel (Player Hotbar 0-8)
      int hotbarStart = playerStart + 27;
      int hotbarRelX = playerPanelStartRelX + 4;
      int hotbarRelY = UIConstants.HOTBAR_Y + 4;

      for (int i = 0; i < 9; i++) {
         int slotIndex = hotbarStart + i;
         if (slotIndex >= this.menu.slots.size()) break;
         Slot slot = this.menu.slots.get(slotIndex);

         int col = i % 5;
         int row = i / 5;

         setSlotPos(slot, hotbarRelX + col * UIConstants.SLOT_PITCH, hotbarRelY + row * UIConstants.SLOT_PITCH);
      }
   }

   private void setSlotPos(Slot slot, int x, int y) {
      com.yitianys.BlockZ.util.InventoryUtils.setSlotPosition(slot, x, y);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);

      // 渲染玩家实体
      if (this.minecraft.player != null) {
         int playerPanelX = this.leftPos + getVicinityPanelWidth() + 2;
         int x = playerPanelX + UIConstants.PANEL_W / 2; // Center of Player Panel
         int y = this.topPos + UIConstants.PLAYER_MODEL_Y;

         int scale = 32;
         InventoryScreen.renderEntityInInventoryFollowsMouse(
                 graphics, x - scale, y - scale * 2, x + scale, y, scale, 0.0625F,
                 (float)mouseX, (float)mouseY, this.minecraft.player);
      }
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      int x = this.leftPos;
      int y = this.topPos;

      int vicW = getVicinityPanelWidth();
      int playerPanelX = x + vicW + 2;
      int inventoryPanelX = playerPanelX + UIConstants.PANEL_W + 2;

      // 1. Vicinity Panel (Container) - Wider
      drawPanel(graphics, x, y + UIConstants.PANEL_Y, vicW, UIConstants.PANEL_H, this.title.getString());

      // 2. Player Panel (Middle)
      int playerPanelH = UIConstants.PANEL_H - UIConstants.HOTBAR_H - 2;
      drawPanel(graphics, playerPanelX, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, playerPanelH, "PLAYER");

      // 3. Hotbar Panel
      drawPanel(graphics, playerPanelX, y + UIConstants.HOTBAR_Y, UIConstants.HOTBAR_W, UIConstants.HOTBAR_H, "HOTBAR");

      // 4. Inventory Panel (Backpack)
      drawPanel(graphics, inventoryPanelX, y + UIConstants.PANEL_Y, UIConstants.PANEL_W, UIConstants.PANEL_H, "INVENTORY");

      // 渲染槽位背景
      renderSlotRangeBackground(graphics);
   }

   private void renderSlotRangeBackground(GuiGraphics graphics) {
      for (int i = 0; i < this.menu.slots.size(); i++) {
         Slot slot = this.menu.slots.get(i);
         if (!slot.isActive()) continue;

         int slotX = this.leftPos + slot.x - 1;
         int slotY = this.topPos + slot.y - 1;

         int bgColor = slot.hasItem() ? 0x30FFFFFF : 0x10FFFFFF;
         graphics.fill(slotX, slotY, slotX + 18, slotY + 18, bgColor);

         int outlineColor = slot.hasItem() ? 0x60FFFFFF : 0x20FFFFFF;
         graphics.renderOutline(slotX, slotY, 18, 18, outlineColor);
      }
   }

   private void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, String title) {
      graphics.fill(x, y, x + w, y + h, 0xB0000000);
      graphics.fill(x, y - 1, x + w, y, 0xFF555555);
      graphics.fill(x, y + h, x + w, y + h + 1, 0xFF555555);
      graphics.fill(x - 1, y, x, y + h, 0xFF555555);
      graphics.fill(x + w, y, x + w + 1, y + h, 0xFF555555);

      graphics.fill(x, y - 12, x + w, y, 0xCC000000);
      // 截断过长的标题
      if (title.length() > 14) title = title.substring(0, 14);
      graphics.drawString(this.font, title, x + 4, y - 10, 0xFFDDDDDD, false);
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      // 禁用原版 Label 渲染
   }
}
