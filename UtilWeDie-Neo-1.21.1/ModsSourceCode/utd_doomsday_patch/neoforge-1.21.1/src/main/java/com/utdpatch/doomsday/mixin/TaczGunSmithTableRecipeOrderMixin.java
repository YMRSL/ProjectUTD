package com.utdpatch.doomsday.mixin;

import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.resource.pojo.data.block.TabConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {GunSmithTableScreen.class},
   remap = false
)
public abstract class TaczGunSmithTableRecipeOrderMixin {
   private static final Logger LOGGER = LogManager.getLogger("UTD TaCZ Recipe Order");
   private static final Pattern UTD_UI_ORDER_ID = Pattern.compile("^kubejs:utd_[^/]+/ui(\\d{4})_.*$");
   @Shadow
   @Final
   private LinkedHashMap<ResourceLocation, TabConfig> recipeKeys;
   @Shadow
   @Final
   private Map<ResourceLocation, List<ResourceLocation>> recipes;

   @Inject(
      method = {"classifyRecipes"},
      at = {@At("TAIL")}
   )
   private void utd$sortClassifiedRecipes(CallbackInfo var1) {
      int var2 = 0;
      int var3 = 0;

      for (List<ResourceLocation> var5 : this.recipes.values()) {
         var2 += var5.size();
         ArrayList var6 = new ArrayList(var5);
         var5.sort(TaczGunSmithTableRecipeOrderMixin::compareRecipeIds);
         if (!var6.equals(var5)) {
            var3++;
         }
      }

      int var7 = this.reorderGroups();
      LOGGER.info("classifyRecipes TAIL ran: groups={} totalRecipes={} (load-probe marker=recipe-probe)", this.recipes.size(), var2);
      if (var2 > 0 && (var3 > 0 || var7 > 0)) {
         LOGGER.info(
            "sorted classified recipes groups={} movedGroups={} touchedGroups={} recipes={} policy=final-gui-consumer", this.recipes.size(), var7, var3, var2
         );
      }
   }

   private int reorderGroups() {
      ArrayList<Entry<ResourceLocation, TabConfig>> var1 = new ArrayList<>(this.recipeKeys.entrySet());
      ArrayList<Entry<ResourceLocation, List<ResourceLocation>>> var2 = new ArrayList<>(this.recipes.entrySet());
      Comparator var3 = Comparator.comparingInt(TaczGunSmithTableRecipeOrderMixin::groupOrder).thenComparing(ResourceLocation::toString);
      var1.sort(Entry.comparingByKey(var3));
      var2.sort(Entry.comparingByKey(var3));
      int var4 = countMoved(this.recipeKeys.keySet().stream().toList(), keysOf(var1)) + countMoved(this.recipes.keySet().stream().toList(), keysOf(var2));
      if (var4 > 0) {
         this.recipeKeys.clear();

         for (Entry var6 : var1) {
            this.recipeKeys.put((ResourceLocation)var6.getKey(), (TabConfig)var6.getValue());
         }

         this.recipes.clear();

         for (Entry var8 : var2) {
            this.recipes.put((ResourceLocation)var8.getKey(), (List<ResourceLocation>)var8.getValue());
         }
      }

      return var4;
   }

   private static List<ResourceLocation> keysOf(List<? extends Entry<ResourceLocation, ?>> var0) {
      ArrayList var1 = new ArrayList();

      for (Entry var3 : var0) {
         var1.add((ResourceLocation)var3.getKey());
      }

      return var1;
   }

   private static int countMoved(List<ResourceLocation> var0, List<ResourceLocation> var1) {
      int var2 = 0;
      int var3 = Math.min(var0.size(), var1.size());

      for (int var4 = 0; var4 < var3; var4++) {
         if (!((ResourceLocation)var0.get(var4)).equals(var1.get(var4))) {
            var2++;
         }
      }

      return var2 + Math.abs(var0.size() - var1.size());
   }

   private static int compareRecipeIds(ResourceLocation var0, ResourceLocation var1) {
      int var2 = explicitUiOrder(var0);
      int var3 = explicitUiOrder(var1);
      return var2 != var3 ? Integer.compare(var2, var3) : var0.toString().compareTo(var1.toString());
   }

   private static int explicitUiOrder(ResourceLocation var0) {
      Matcher var1 = UTD_UI_ORDER_ID.matcher(var0.toString());
      return var1.matches() ? Integer.parseInt(var1.group(1)) : 10000;
   }

   private static int groupOrder(ResourceLocation var0) {
      String var1 = var0.toString();
      if ("tacz:ammo".equals(var1)) {
         return 5;
      } else if ("tacz:pistol".equals(var1)) {
         return 10;
      } else if ("tacz:smg".equals(var1)) {
         return 20;
      } else if ("tacz:shotgun".equals(var1)) {
         return 30;
      } else if ("tacz:rifle".equals(var1)) {
         return 40;
      } else if ("tacz:sniper".equals(var1)) {
         return 50;
      } else if ("tacz:mg".equals(var1)) {
         return 60;
      } else if ("tacz:rpg".equals(var1)) {
         return 70;
      } else if ("tacz:scope".equals(var1) || "tacz:sight".equals(var1)) {
         return 100;
      } else if ("tacz:muzzle".equals(var1)) {
         return 110;
      } else if ("tacz:grip".equals(var1)) {
         return 120;
      } else if ("tacz:stock".equals(var1)) {
         return 130;
      } else if ("tacz:extended_mag".equals(var1)) {
         return 140;
      } else if ("tacz:laser".equals(var1)) {
         return 150;
      } else if ("tacz:misc".equals(var1)) {
         return 800;
      } else {
         return "utd:lowtech_oddity".equals(var1) ? 900 : 700;
      }
   }
}
