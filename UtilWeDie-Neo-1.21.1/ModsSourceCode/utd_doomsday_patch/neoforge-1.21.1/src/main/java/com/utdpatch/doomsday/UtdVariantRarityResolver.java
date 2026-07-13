package com.utdpatch.doomsday;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UtdVariantRarityResolver {
   private static final int TAG_COMPOUND = 10;
   private static final int TAG_STRING = 8;
   private static final Logger LOGGER = LogManager.getLogger("UTD Variant Rarity");
   private static final Map<String, Integer> NBT_RULES = new HashMap<>();
   private static final Map<String, Integer> ITEM_FALLBACKS = new HashMap<>();
   private static final Set<String> LOGGED_STACKS = ConcurrentHashMap.newKeySet();
   private static volatile boolean loaded;

   private UtdVariantRarityResolver() {
   }

   public static boolean shouldBypassRarityCore(ItemStack var0) {
      return configuredRarity(var0) != null;
   }

   public static boolean shouldBypassRarityCore(Item var0) {
      return configuredRarity(var0) != null;
   }

   public static Integer configuredRarity(Item var0) {
      ResourceLocation var1 = itemId(var0);
      if (var1 == null) {
         return null;
      } else {
         loadIfNeeded();
         return ITEM_FALLBACKS.get(var1.toString());
      }
   }

   public static Integer configuredRarity(ItemStack var0) {
      ResourceLocation var1 = itemId(var0);
      UtdVariantRarityResolver.VariantIdentity var2 = variantIdentity(var0);
      if (var1 == null) {
         return null;
      } else {
         loadIfNeeded();
         if (var2 == null) {
            return ITEM_FALLBACKS.get(var1.toString());
         } else {
            Integer var3 = highestMatchingNbtRarity(var2);
            return var3 != null ? var3 : ITEM_FALLBACKS.get(var2.itemId);
         }
      }
   }

   public static boolean isFirstpersonPackFood(ItemStack var0) {
      ResourceLocation var1 = itemId(var0);
      return var1 != null && "firstpersonfoodeating:pack_food".equals(var1.toString());
   }

   public static void logProbe(String var0, ItemStack var1, Integer var2, boolean var3) {
      ResourceLocation var4 = itemId(var1);
      CompoundTag var5 = customData(var1);
      String var6 = nestedFoodId(var5);
      String var7 = stringTag(var5, "food_id");
      String var8 = var0 + "|" + var4 + "|" + var6 + "|" + var7 + "|" + var5;
      if (LOGGED_STACKS.add(var8)) {
         LOGGER.info(
            "{} item={} hasTag={} nestedFoodId={} rootFoodId={} configuredRarity={} bypassed={} tag={}", var0, var4, var5 != null, var6, var7, var2, var3, var5
         );
      }
   }

   public static ResourceLocation itemId(ItemStack var0) {
      return var0 != null && !var0.isEmpty() ? BuiltInRegistries.ITEM.getKey(var0.getItem()) : null;
   }

   public static ResourceLocation itemId(Item var0) {
      return var0 == null ? null : BuiltInRegistries.ITEM.getKey(var0);
   }

   private static CompoundTag customData(ItemStack var0) {
      CustomData var1 = var0 == null ? null : var0.get(DataComponents.CUSTOM_DATA);
      return var1 == null ? null : var1.copyTag();
   }

   private static Integer highestMatchingNbtRarity(UtdVariantRarityResolver.VariantIdentity var0) {
      Integer var1 = null;

      for (Entry var3 : var0.pathValues.entrySet()) {
         Integer var4 = NBT_RULES.get(ruleKey(var0.itemId, (String)var3.getKey(), (String)var3.getValue()));
         if (var4 != null) {
            var1 = var1 == null ? var4 : Math.max(var1, var4);
         }
      }

      return var1;
   }

   private static UtdVariantRarityResolver.VariantIdentity variantIdentity(ItemStack var0) {
      ResourceLocation var1 = itemId(var0);
      if (var1 == null) {
         return null;
      } else {
         String var2 = var1.toString();
         CompoundTag var3 = customData(var0);
         HashMap var4 = new HashMap();
         if ("tacz:workbench_a".equals(var2)) {
            var4.put("BlockId", canonicalWorkbenchBlockId(stringTag(var3, "BlockId"), "tacz:ammo_workbench"));
            return new UtdVariantRarityResolver.VariantIdentity(var2, var4);
         } else if ("tacz:workbench_c".equals(var2)) {
            var4.put("BlockId", canonicalWorkbenchBlockId(stringTag(var3, "BlockId"), "tacz:attachment_workbench"));
            return new UtdVariantRarityResolver.VariantIdentity(var2, var4);
         } else if ("tacz:workbench_b".equals(var2)) {
            String var8 = canonicalWorkbenchBlockId(stringTag(var3, "BlockId"), "");
            if (var8.isEmpty()) {
               return null;
            } else {
               var4.put("BlockId", var8);
               return new UtdVariantRarityResolver.VariantIdentity(var2, var4);
            }
         } else if ("tacz:modern_kinetic_gun".equals(var2)) {
            return identityFromStringTag(var2, var3, "GunId");
         } else if ("tacz:ammo".equals(var2)) {
            return identityFromStringTag(var2, var3, "AmmoId");
         } else if ("tacz:attachment".equals(var2)) {
            return identityFromStringTag(var2, var3, "AttachmentId");
         } else if ("firstpersonfoodeating:pack_food".equals(var2)) {
            String var7 = nestedFoodId(var3);
            String var6 = stringTag(var3, "food_id");
            if (var7.isEmpty() && var6.isEmpty()) {
               return null;
            } else {
               if (!var7.isEmpty()) {
                  var4.put("firstpersonfoodeating_profile.food_id", var7);
               }

               if (!var6.isEmpty()) {
                  var4.put("food_id", var6);
               }

               return new UtdVariantRarityResolver.VariantIdentity(var2, var4);
            }
         } else {
            UtdVariantRarityResolver.VariantIdentity var5 = identityFromFirstAvailableStringTag(var2, var3, "Id", "MeleeId", "MeleeWeaponId", "ThrowableId");
            return var5 == null || !"lrtactical:melee".equals(var2) && !"lrtactical:throwable".equals(var2) ? null : var5;
         }
      }
   }

   private static UtdVariantRarityResolver.VariantIdentity identityFromStringTag(String var0, CompoundTag var1, String var2) {
      String var3 = stringTag(var1, var2);
      if (var3.isEmpty()) {
         return null;
      } else {
         HashMap var4 = new HashMap();
         var4.put(var2, var3);
         return new UtdVariantRarityResolver.VariantIdentity(var0, var4);
      }
   }

   private static UtdVariantRarityResolver.VariantIdentity identityFromFirstAvailableStringTag(String var0, CompoundTag var1, String... var2) {
      if (var1 == null) {
         return null;
      } else {
         for (String var6 : var2) {
            String var7 = stringTag(var1, var6);
            if (!var7.isEmpty()) {
               HashMap var8 = new HashMap();
               var8.put(var6, var7);
               return new UtdVariantRarityResolver.VariantIdentity(var0, var8);
            }
         }

         return null;
      }
   }

   private static String nestedFoodId(CompoundTag var0) {
      return var0 != null && var0.contains("firstpersonfoodeating_profile", 10) ? stringTag(var0.getCompound("firstpersonfoodeating_profile"), "food_id") : "";
   }

   private static String stringTag(CompoundTag var0, String var1) {
      return var0 != null && var0.contains(var1, 8) ? var0.getString(var1) : "";
   }

   private static String canonicalWorkbenchBlockId(String var0, String var1) {
      String var2 = var0 != null && !var0.isEmpty() ? var0 : var1;

      return switch (var2) {
         case "tacz:utd_ammo_workbench_item" -> "tacz:ammo_workbench";
         case "tacz:utd_attachment_workbench_item" -> "tacz:attachment_workbench";
         case "tacz:utd_oldworkbench_item" -> "hamster:oldworkbench";
         case "tacz:utd_lrtactical_smith_table_item" -> "lrtactical:smith_table";
         case "utd_workbench_pack:oldworkbench_render" -> "hamster:oldworkbench";
         case "utd_workbench_pack:lrtactical_smith_table_render" -> "lrtactical:smith_table";
         default -> var2;
      };
   }

   private static void loadIfNeeded() {
      if (!loaded) {
         synchronized (UtdVariantRarityResolver.class) {
            if (!loaded) {
               try {
                  loadConfigMaps();
                  LOGGER.info("Loaded {} UTD NBT rarity keys and {} item fallbacks", NBT_RULES.size(), ITEM_FALLBACKS.size());
               } catch (Throwable var7) {
                  LOGGER.warn("Failed to load UTD rarity config maps: {}", var7.toString());
               } finally {
                  loaded = true;
               }
            }
         }
      }
   }

   private static void loadConfigMaps() throws IOException {
      Path var0 = FMLPaths.CONFIGDIR.get().resolve("raritycore");
      loadItemFallbacks(var0.resolve("FinalRarityConfig").resolve("utd_loot_levels.json"));
      // Asset Workbench plain-item overrides use the same native RarityCore
      // folder.  Include them in the compatibility fallback as well so items
      // with their own components (notably BlockZ backpacks) cannot fall back
      // to the vanilla grey rarity after RarityCore's automatic pass.
      loadItemFallbacks(var0.resolve("FinalRarityConfig").resolve("utd_asset_workbench.json"));
      loadNbtRules(var0.resolve("nbt_matches"));
   }

   private static void loadItemFallbacks(Path var0) throws IOException {
      if (Files.isRegularFile(var0)) {
         try (BufferedReader var1 = Files.newBufferedReader(var0)) {
            JsonElement var2 = JsonParser.parseReader(var1);
            if (!var2.isJsonObject()) {
               return;
            }

            for (Entry var4 : var2.getAsJsonObject().entrySet()) {
               if (((JsonElement)var4.getValue()).isJsonPrimitive() && ((JsonElement)var4.getValue()).getAsJsonPrimitive().isNumber()) {
                  ITEM_FALLBACKS.put((String)var4.getKey(), ((JsonElement)var4.getValue()).getAsInt());
               }
            }
         }
      }
   }

   private static void loadNbtRules(Path var0) throws IOException {
      if (Files.isDirectory(var0)) {
         try (Stream<Path> var1 = Files.walk(var0)) {
            var1.filter(var0x -> Files.isRegularFile(var0x))
               .filter(var0x -> var0x.getFileName().toString().startsWith("utd_rule_"))
               .filter(var0x -> var0x.getFileName().toString().endsWith(".json"))
               .forEach(UtdVariantRarityResolver::loadNbtRule);
         }
      }
   }

   private static void loadNbtRule(Path var0) {
      try {
         try (BufferedReader var1 = Files.newBufferedReader(var0)) {
            JsonElement var2 = JsonParser.parseReader(var1);
            if (!var2.isJsonObject()) {
               return;
            }

            JsonObject var3 = var2.getAsJsonObject();
            String var4 = stringProperty(var3, "item_id");
            int var5 = intProperty(var3, "rarity", 0);
            JsonArray var6 = var3.getAsJsonArray("conditions");
            if (!var4.isEmpty() && var5 > 0 && var6 != null && var6.size() == 1) {
               JsonObject var7 = var6.get(0).getAsJsonObject();
               if (!"equals".equals(stringProperty(var7, "type"))) {
                  return;
               }

               String var8 = stringProperty(var7, "path");
               String var9 = stringProperty(var7, "value");
               if ("BlockId".equals(var8)) {
                  var9 = canonicalWorkbenchBlockId(var9, "");
               }

               if (!var8.isEmpty() && !var9.isEmpty()) {
                  String var10 = ruleKey(var4, var8, var9);
                  NBT_RULES.merge(var10, var5, Math::max);
                  return;
               }

               return;
            }
         }
      } catch (Throwable var13) {
         LOGGER.warn("Failed to load UTD NBT rarity rule {}: {}", var0, var13.toString());
      }
   }

   private static String stringProperty(JsonObject var0, String var1) {
      return var0 != null && var0.has(var1) && var0.get(var1).isJsonPrimitive() ? var0.get(var1).getAsString() : "";
   }

   private static int intProperty(JsonObject var0, String var1, int var2) {
      if (var0 != null && var0.has(var1) && var0.get(var1).isJsonPrimitive()) {
         try {
            return var0.get(var1).getAsInt();
         } catch (NumberFormatException var4) {
            return var2;
         }
      } else {
         return var2;
      }
   }

   private static String ruleKey(String var0, String var1, String var2) {
      return var0 + "|" + var1 + "|" + var2;
   }

   private record VariantIdentity(String itemId, Map<String, String> pathValues) {
   }
}
