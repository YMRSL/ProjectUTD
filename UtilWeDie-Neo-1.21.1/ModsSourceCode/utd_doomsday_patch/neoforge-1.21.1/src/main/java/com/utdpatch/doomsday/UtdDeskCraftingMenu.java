package com.utdpatch.doomsday;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.level.Level;

public class UtdDeskCraftingMenu extends CraftingMenu {
   private final Level level;
   private final BlockPos pos;

   public UtdDeskCraftingMenu(int var1, Inventory var2, Level var3, BlockPos var4) {
      super(var1, var2, ContainerLevelAccess.create(var3, var4));
      this.level = var3;
      this.pos = var4;
   }

   public boolean stillValid(Player var1) {
      return var1.blockPosition().distSqr(this.pos) <= 400.0 && UtdDeskWorkbench.isDeskBlock(this.level, this.pos);
   }
}
