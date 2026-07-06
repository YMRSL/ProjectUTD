package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.UtdDeskWorkbench;
import net.mcreator.doomsdaydecoration.functionality.DoomsdayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Doomsday Decoration was rewritten for 1.21.1 (MCreator package
 * {@code net.mcreator.doomsdaydecoration.*}); its old custom-packet
 * {@code OpenLootableBlockPacket} no longer exists. The lootable-block open path
 * now funnels through {@code DecoLootBlockSupport.openContainer(state, level, pos, player)}
 * (called from each loot block's {@code useWithoutItem}). We intercept there to keep the
 * UTD behaviour: desks with loot open the loot screen (vanilla path), crouch passes
 * through (CarryOn-compatible), and a desk that has been searched and found empty opens a
 * crafting menu instead.
 */
@Mixin(
   targets = {"net.mcreator.doomsdaydecoration.block.DecoLootBlockSupport"},
   remap = false
)
public abstract class DeskLootSplitMixin {
   private static final Logger LOGGER = LogManager.getLogger("UTD Desk Workbench");

   @Inject(
      method = {"openContainer"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void utd$splitDeskWorkbenchFromDoomsdayLoot(
      BlockState var0, Level var1, BlockPos var2, Player var3, CallbackInfoReturnable<InteractionResult> var4
   ) {
      if (!var1.isClientSide && var3 instanceof ServerPlayer var5) {
         ResourceLocation var6 = UtdDeskWorkbench.blockIdAt(var1, var2);
         if (var6 != null && UtdDeskWorkbench.DESK_BLOCK_IDS.contains(var6.toString())) {
            if (var5.isShiftKeyDown()) {
               LOGGER.info("Passing crouch desk packet to Doomsday/CarryOn-compatible path for {} at {} marker=crouch-pass-through-carryon", var6, var2);
            } else if (var1.getBlockEntity(var2) instanceof DoomsdayBlockEntity var8) {
               boolean var9 = ((DoomsdayBlockEntityAccessor)var8).utd$isFilledFromLoot();
               var8.tryLoadLoot(var5);
               if (UtdDeskWorkbench.hasLoot(var8.getLootHandler())) {
                  LOGGER.info("Opening Doomsday desk loot for {} at {} marker=doomsday-loot-open", var6, var2);
               } else if (!var9) {
                  LOGGER.info(
                     "Desk search produced no loot for {} at {}; next normal right-click opens crafting marker=searched-empty-wait-next-right-click",
                     var6,
                     var2
                  );
                  var5.displayClientMessage(Component.literal("Empty"), true);
                  var4.setReturnValue(InteractionResult.sidedSuccess(false));
               } else {
                  LOGGER.info("Opening desk crafting menu after completed empty search for {} at {} marker=searched-empty-crafting-open", var6, var2);
                  UtdDeskWorkbench.openCrafting(var5, var1, var2);
                  var4.setReturnValue(InteractionResult.sidedSuccess(false));
               }
            }
         }
      }
   }
}
