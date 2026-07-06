package com.utdpatch.doomsday.mixin;

import com.tacz.guns.client.resource.ClientIndexManager;
import com.tacz.guns.client.resource.index.ClientBlockIndex;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;
import java.lang.reflect.Field;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ClientIndexManager.class},
   remap = false
)
public abstract class TaczWorkbenchClientBlockIndexRepairMixin {
   private static final Logger LOGGER = LogManager.getLogger("UTD TaCZ Workbench Index Repair");
   private static final ResourceLocation WORKBENCH_B = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_b");
   private static final ResourceLocation OLD_WORKBENCH = ResourceLocation.fromNamespaceAndPath("hamster", "oldworkbench");
   private static final ResourceLocation OLD_WORKBENCH_DISPLAY = ResourceLocation.fromNamespaceAndPath("tacz", "utd_oldworkbench_1x2x1");
   private static final ResourceLocation OLD_WORKBENCH_DATA = ResourceLocation.fromNamespaceAndPath("hamster", "oldworkbench_1x2x1");
   private static final ResourceLocation SMITH_TABLE = ResourceLocation.fromNamespaceAndPath("lrtactical", "smith_table");
   private static final ResourceLocation SMITH_TABLE_DISPLAY = ResourceLocation.fromNamespaceAndPath("tacz", "utd_lrtactical_smith_table");

   @Inject(
      method = {"loadBlockIndex"},
      at = {@At("RETURN")}
   )
   private static void utd$repairTargetWorkbenchIndexes(CallbackInfo var0) {
      repair(OLD_WORKBENCH, "hamster.block.oldworkbench", OLD_WORKBENCH_DISPLAY, OLD_WORKBENCH_DATA, "hamster.oldworkbench.desc");
      repair(SMITH_TABLE, "lrtactical.block.smith_table.name", SMITH_TABLE_DISPLAY, SMITH_TABLE, "lrtactical.block.smith_table.desc");
   }

   private static void repair(ResourceLocation var0, String var1, ResourceLocation var2, ResourceLocation var3, String var4) {
      try {
         BlockIndexPOJO var5 = new BlockIndexPOJO();
         setField(var5, "name", var1);
         setField(var5, "display", var2);
         setField(var5, "data", var3);
         setField(var5, "id", WORKBENCH_B);
         setField(var5, "tooltip", var4);
         ClientBlockIndex var6 = ClientBlockIndex.getInstance(var5);
         ClientIndexManager.BLOCK_INDEX.put(var0, var6);
         LOGGER.info(
            "repaired {} display={} model={} texture={} transforms={}", var0, var2, var6.getModel() != null, var6.getTexture(), var6.getTransforms() != null
         );
      } catch (Exception var7) {
         LOGGER.warn("failed to repair {} display={}: {}", var0, var2, var7.toString());
      }
   }

   private static void setField(BlockIndexPOJO var0, String var1, Object var2) throws ReflectiveOperationException {
      Field var3 = BlockIndexPOJO.class.getDeclaredField(var1);
      var3.setAccessible(true);
      var3.set(var0, var2);
   }
}
