package com.utdpatch.doomsday;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UtdRuntimeInventoryRepair {
   private static final int TAG_COMPOUND = 10;
   private static final int TAG_FLOAT = 5;
   private static final int TAG_LIST = 9;
   private static final int TAG_NUMERIC = 99;
   private static final int TAG_STRING = 8;
   private static final int SCAN_INTERVAL_TICKS = 40;
   private static final ResourceLocation WORKBENCH_A = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_a");
   private static final ResourceLocation WORKBENCH_B = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_b");
   private static final ResourceLocation WORKBENCH_C = ResourceLocation.fromNamespaceAndPath("tacz", "workbench_c");
   private static final String AMMO_WORKBENCH = "tacz:ammo_workbench";
   private static final String ATTACHMENT_WORKBENCH = "tacz:attachment_workbench";
   private static final String OLD_WORKBENCH = "hamster:oldworkbench";
   private static final String SMITH_TABLE = "lrtactical:smith_table";
   private static final ResourceLocation FIRSTPERSON_PACK_FOOD = ResourceLocation.fromNamespaceAndPath("firstpersonfoodeating", "pack_food");
   private static final String FIRSTPERSON_PROFILE = "firstpersonfoodeating_profile";
   private static final String ZHENJI_E = "firstpersonfoodeating:i_zhenji_e";
   private static final String ZHENJI_G = "firstpersonfoodeating:i_zhenji_g";
   private static final Logger LOGGER = LogManager.getLogger("UTD Inventory Repair");
   private static final Set<String> LOGGED_RARITY_STACKS = ConcurrentHashMap.newKeySet();

   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent.Post var1) {
      Player var2x = var1.getEntity();
      if (!var2x.level().isClientSide && var2x.tickCount % 40 == 0) {
         Inventory var2 = var2x.getInventory();
         boolean var3 = repairList(var2.items);
         var3 = repairList(var2.armor) || var3;
         var3 = repairList(var2.offhand) || var3;
         if (var3) {
            var2.setChanged();
         }
      }
   }

   private static boolean repairList(NonNullList<ItemStack> var0) {
      boolean var1 = false;

      for (ItemStack var3 : var0) {
         var1 = repairStack(var3) || var1;
      }

      return var1;
   }

   private static boolean repairStack(ItemStack var0) {
      if (var0 == null || var0.isEmpty()) {
         return false;
      } else {
         ResourceLocation var1 = BuiltInRegistries.ITEM.getKey(var0.getItem());
         logConfiguredVariantRarity(var1, var0);
         boolean var2 = WORKBENCH_A.equals(var1) || WORKBENCH_B.equals(var1) || WORKBENCH_C.equals(var1);
         boolean var3 = FIRSTPERSON_PACK_FOOD.equals(var1);
         if (!var2 && !var3) {
            return false;
         } else {
            CompoundTag var4 = customDataCopy(var0);
            boolean var5 = false;
            if (var2) {
               var5 = normalizeWorkbenchBlockId(var1, var4);
               if (WORKBENCH_A.equals(var1)) {
                  var5 = ensureWorkbenchBlockId(var4, "tacz:ammo_workbench") || var5;
               } else if (WORKBENCH_C.equals(var1)) {
                  var5 = ensureWorkbenchBlockId(var4, "tacz:attachment_workbench") || var5;
               }
            } else {
               var5 = repairFirstpersonFood(var4);
            }

            if (var5) {
               var0.set(DataComponents.CUSTOM_DATA, CustomData.of(var4));
            }

            return var5;
         }
      }
   }

   private static CompoundTag customDataCopy(ItemStack var0) {
      CustomData var1 = var0.get(DataComponents.CUSTOM_DATA);
      return var1 == null ? new CompoundTag() : var1.copyTag();
   }

   private static CompoundTag customDataOrNull(ItemStack var0) {
      CustomData var1 = var0 == null ? null : var0.get(DataComponents.CUSTOM_DATA);
      return var1 == null ? null : var1.copyTag();
   }

   private static void logConfiguredVariantRarity(ResourceLocation var0, ItemStack var1) {
      Integer var2 = UtdVariantRarityResolver.configuredRarity(var1);
      if (var2 != null) {
         CompoundTag var3 = customDataOrNull(var1);
         String var4 = var0 + "|" + var3;
         if (LOGGED_RARITY_STACKS.add(var4)) {
            LOGGER.info("variant rarity item={} tag={} configuredRarity={}", var0, var3, var2);
         }
      }
   }

   private static boolean ensureWorkbenchBlockId(CompoundTag var0, String var1) {
      if (var0.contains("BlockId", 8)) {
         return false;
      } else {
         var0.putString("BlockId", var1);
         return true;
      }
   }

   private static boolean normalizeWorkbenchBlockId(ResourceLocation var0, CompoundTag var1) {
      if (!WORKBENCH_A.equals(var0) && !WORKBENCH_B.equals(var0) && !WORKBENCH_C.equals(var0)) {
         return false;
      } else if (var1.contains("BlockId", 8)) {
         String var3 = var1.getString("BlockId");
         String var4 = canonicalWorkbenchBlockId(var3);
         if (var3.equals(var4)) {
            return false;
         } else {
            var1.putString("BlockId", var4);
            LOGGER.warn("normalized workbench BlockId {} -> {} for item={}", var3, var4, var0);
            return true;
         }
      } else {
         return false;
      }
   }

   private static String canonicalWorkbenchBlockId(String var0) {
      return switch (var0) {
         case "tacz:utd_ammo_workbench_item" -> "tacz:ammo_workbench";
         case "tacz:utd_attachment_workbench_item" -> "tacz:attachment_workbench";
         case "tacz:utd_oldworkbench_item" -> "hamster:oldworkbench";
         case "tacz:utd_lrtactical_smith_table_item" -> "lrtactical:smith_table";
         case "utd_workbench_pack:oldworkbench_render" -> "hamster:oldworkbench";
         case "utd_workbench_pack:lrtactical_smith_table_render" -> "lrtactical:smith_table";
         default -> var0;
      };
   }

   private static boolean repairFirstpersonFood(CompoundTag var1) {
      if (var1.contains("firstpersonfoodeating_profile", 10)) {
         CompoundTag var2 = var1.getCompound("firstpersonfoodeating_profile");
         String var3 = var2.getString("food_id");
         boolean var4 = ensureRootFoodId(var1, var3);
         var4 = ensureFirstpersonUseDuration(var2, var3) || var4;
         var4 = ensureFirstpersonUseSelectorDurations(var2, var3) || var4;
         var4 = ensureMissingFirstpersonEffects(var2, var3) || var4;
         var4 = repairFloat(var2, "saturation") || var4;
         var4 = repairEffectList(var2, "effects", List.of("chance")) || var4;
         return repairEffectList(var2, "custom_effects", List.of("value", "chance")) || var4;
      } else {
         return false;
      }
   }

   private static boolean ensureRootFoodId(CompoundTag var0, String var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      } else if (var0.contains("food_id", 8) && var1.equals(var0.getString("food_id"))) {
         return false;
      } else {
         var0.putString("food_id", var1);
         return true;
      }
   }

   private static boolean ensureFirstpersonUseDuration(CompoundTag var0, String var1) {
      int var2 = firstpersonUseDurationTicks(var1);
      if (var2 <= 0) {
         return false;
      } else if (var0.contains("use_duration_ticks", 99) && var0.getInt("use_duration_ticks") == var2) {
         return false;
      } else {
         var0.putInt("use_duration_ticks", var2);
         return true;
      }
   }

   private static boolean ensureFirstpersonUseSelectorDurations(CompoundTag var0, String var1) {
      if (!var0.contains("use_selector", 10)) {
         return false;
      } else {
         CompoundTag var2 = var0.getCompound("use_selector");
         if (!var2.contains("rules", 9)) {
            return false;
         } else {
            ListTag var3 = var2.getList("rules", 10);
            boolean var4 = false;

            for (int var5 = 0; var5 < var3.size(); var5++) {
               CompoundTag var6 = var3.getCompound(var5);
               int var7 = firstpersonUseSelectorRuleDurationTicks(var1, var6.getString("clip"));
               if (var7 > 0 && (!var6.contains("duration_ticks", 99) || var6.getInt("duration_ticks") != var7)) {
                  var6.putInt("duration_ticks", var7);
                  var4 = true;
               }
            }

            return var4;
         }
      }
   }

   private static int firstpersonUseDurationTicks(String var0) {
      if (var0 == null) {
         return 0;
      } else if (var0.startsWith("firstpersonfoodeating:i_zhenji_")) {
         return 32;
      } else if (var0.startsWith("firstpersonfoodeating:i_yaoping_")) {
         return 56;
      } else if (var0.startsWith("firstpersonfoodeating:i_bang_")) {
         return 66;
      } else if (var0.startsWith("firstpersonfoodeating:i_bengdai_")) {
         return 82;
      } else if (var0.startsWith("firstpersonfoodeating:i_dai_")) {
         return 112;
      } else if (var0.startsWith("firstpersonfoodeating:i_guan_")) {
         return 84;
      } else if (var0.startsWith("firstpersonfoodeating:i_guantou_")) {
         return 132;
      } else if (var0.startsWith("firstpersonfoodeating:i_he_")) {
         return 112;
      } else if (var0.startsWith("firstpersonfoodeating:i_jia_")) {
         return 127;
      } else if (var0.startsWith("firstpersonfoodeating:i_jijiubao_")) {
         return 115;
      } else {
         return var0.startsWith("firstpersonfoodeating:i_ping_") ? 97 : 0;
      }
   }

   private static int firstpersonUseSelectorRuleDurationTicks(String var0, String var1) {
      if (var0 == null || var1 == null) {
         return 0;
      } else if (var0.startsWith("firstpersonfoodeating:i_jia_")) {
         return switch (var1) {
            case "level_1" -> 54;
            case "level_2" -> 78;
            case "level_3" -> 103;
            case "level_4" -> 127;
            default -> firstpersonUseDurationTicks(var0);
         };
      } else if (var0.startsWith("firstpersonfoodeating:i_jijiubao_")) {
         return switch (var1) {
            case "use1", "use2" -> 115;
            default -> firstpersonUseDurationTicks(var0);
         };
      } else {
         return firstpersonUseDurationTicks(var0);
      }
   }

   private static boolean ensureMissingFirstpersonEffects(CompoundTag var0, String var1) {
      if ("firstpersonfoodeating:i_zhenji_e".equals(var1)) {
         boolean var4 = false;
         if (!var0.contains("effects", 9)) {
            ListTag var6 = new ListTag();
            var6.add(vanillaEffect("minecraft:fire_resistance", 2400, 0));
            var0.put("effects", var6);
            var4 = true;
         }

         if (!var0.contains("custom_effects", 9)) {
            ListTag var7 = new ListTag();
            var7.add(customEffect("immune", 120.0F, 2400));
            var0.put("custom_effects", var7);
            var4 = true;
         }

         return var4;
      } else if ("firstpersonfoodeating:i_zhenji_g".equals(var1)) {
         boolean var2 = false;
         if (!var0.contains("effects", 9)) {
            ListTag var3 = new ListTag();
            var3.add(vanillaEffect("minecraft:strength", 900, 2));
            var3.add(vanillaEffect("minecraft:speed", 900, 1));
            var3.add(vanillaEffect("minecraft:resistance", 900, 1));
            var3.add(vanillaEffect("minecraft:hunger", 900, 2));
            var3.add(vanillaEffect("minecraft:nausea", 300, 0));
            var0.put("effects", var3);
            var2 = true;
         }

         if (!var0.contains("custom_effects", 9)) {
            ListTag var5 = new ListTag();
            var5.add(customEffect("extra_armor", 4.0F, 900));
            var5.add(customEffect("emergency_painkiller", 18.0F, 0));
            var0.put("custom_effects", var5);
            var2 = true;
         }

         return var2;
      } else {
         return false;
      }
   }

   private static CompoundTag vanillaEffect(String var0, int var1, int var2) {
      CompoundTag var3 = new CompoundTag();
      var3.putString("id", var0);
      var3.putInt("duration_ticks", var1);
      var3.putInt("amplifier", var2);
      var3.putFloat("chance", 1.0F);
      return var3;
   }

   private static CompoundTag customEffect(String var0, float var1, int var2) {
      CompoundTag var3 = new CompoundTag();
      var3.putString("type", var0);
      var3.putFloat("value", var1);
      var3.putInt("duration_ticks", var2);
      var3.putInt("interval_ticks", 0);
      var3.putFloat("chance", 1.0F);
      return var3;
   }

   private static boolean repairEffectList(CompoundTag var0, String var1, List<String> var2) {
      if (!var0.contains(var1, 9)) {
         return false;
      } else {
         ListTag var3 = var0.getList(var1, 10);
         boolean var4 = false;

         for (int var5 = 0; var5 < var3.size(); var5++) {
            CompoundTag var6 = var3.getCompound(var5);

            for (String var8 : var2) {
               var4 = repairFloat(var6, var8) || var4;
            }
         }

         return var4;
      }
   }

   private static boolean repairFloat(CompoundTag var0, String var1) {
      if (var0.contains(var1) && !var0.contains(var1, 5) && var0.contains(var1, 99)) {
         var0.putFloat(var1, var0.getFloat(var1));
         return true;
      } else {
         return false;
      }
   }
}
