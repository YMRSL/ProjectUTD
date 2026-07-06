package com.utdpatch.doomsday;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class UtdDeskWorkbench {
   private static final double MAX_USE_DISTANCE_SQUARED = 400.0;
   public static final Set<String> DESK_BLOCK_IDS = Set.of(
      "doomsday_decoration:desk",
      "doomsday_decoration:desk_2",
      "doomsday_decoration:desk_3",
      "doomsday_decoration:classroomdesk",
      "doomsday_decoration:officepartitiondesk",
      "doomsday_decoration:officepartitiondesk_2",
      "doomsday_decoration:officepartitiondesk_3",
      "doomsday_decoration:officepartitiondesk_4",
      "doomsday_decoration:officepartitiondesk_5"
   );

   private UtdDeskWorkbench() {
   }

   public static ResourceLocation blockIdAt(Level var0, BlockPos var1) {
      Block var2 = var0.getBlockState(var1).getBlock();
      return BuiltInRegistries.BLOCK.getKey(var2);
   }

   public static boolean isDeskBlock(Level var0, BlockPos var1) {
      ResourceLocation var2 = blockIdAt(var0, var1);
      return var2 != null && DESK_BLOCK_IDS.contains(var2.toString());
   }

   public static boolean isCloseEnough(ServerPlayer var0, BlockPos var1) {
      return var0.blockPosition().distSqr(var1) <= 400.0;
   }

   public static boolean hasLoot(ItemStackHandler var0) {
      for (int var1 = 0; var1 < var0.getSlots(); var1++) {
         if (!var0.getStackInSlot(var1).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public static void openCrafting(ServerPlayer var0, Level var1, BlockPos var2) {
      var0.openMenu(
         new SimpleMenuProvider((var2x, var3, var4) -> new UtdDeskCraftingMenu(var2x, var3, var1, var2), Component.translatable("container.crafting")), var2
      );
   }
}
