package com.utdpatch.doomsday;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public final class UtdTooltipLocalization {
   private static final Map<String, String> EXACT_TRANSLATIONS = new LinkedHashMap<>();

   private UtdTooltipLocalization() {
   }

   public static List<Component> localize(ItemStack var0, List<Component> var1) {
      if (shouldLocalize(var0) && var1 != null && !var1.isEmpty()) {
         ArrayList var2 = new ArrayList(var1.size());
         boolean var3 = false;

         for (Component var5 : var1) {
            Component var6 = localizeLine(var5);
            var2.add(var6);
            var3 |= var6 != var5;
         }

         return (List<Component>)(var3 ? var2 : var1);
      } else {
         return var1;
      }
   }

   private static boolean shouldLocalize(ItemStack var0) {
      if (var0 != null && !var0.isEmpty()) {
         ResourceLocation var1 = BuiltInRegistries.ITEM.getKey(var0.getItem());
         if (var1 == null) {
            return false;
         } else {
            String var2 = var1.getNamespace();
            return "survival_instinct".equals(var2) || "zombiekit".equals(var2);
         }
      } else {
         return false;
      }
   }

   private static Component localizeLine(Component var0) {
      if (var0 == null) {
         return var0;
      } else {
         String var1 = var0.getString();
         String var2 = stripLegacyFormatting(var1).trim();
         String var3 = EXACT_TRANSLATIONS.get(var2);
         if (var3 == null && var2.startsWith("Damage: ")) {
            var3 = "伤害：" + var2.substring("Damage: ".length());
         }

         if (var3 == null) {
            return var0;
         } else {
            MutableComponent var4 = Component.literal(legacyPrefix(var1) + var3);
            var4.setStyle(var0.getStyle());
            return var4;
         }
      }
   }

   private static String stripLegacyFormatting(String var0) {
      StringBuilder var1 = new StringBuilder(var0.length());

      for (int var2 = 0; var2 < var0.length(); var2++) {
         char var3 = var0.charAt(var2);
         if (var3 == 167 && var2 + 1 < var0.length()) {
            var2++;
         } else {
            var1.append(var3);
         }
      }

      return var1.toString();
   }

   private static String legacyPrefix(String var0) {
      return var0.length() >= 2 && var0.charAt(0) == 167 ? var0.substring(0, 2) : "";
   }

   static {
      EXACT_TRANSLATIONS.put("Bonus Armor set:", "套装加成：");
      EXACT_TRANSLATIONS.put("Armor set:", "套装：");
      EXACT_TRANSLATIONS.put("Ability Armor set:", "套装能力：");
      EXACT_TRANSLATIONS.put("Item Effect:", "物品效果：");
      EXACT_TRANSLATIONS.put("Invisibility when crouching", "潜行时隐身");
      EXACT_TRANSLATIONS.put("Resistance & Slowness", "抗性提升与缓慢");
      EXACT_TRANSLATIONS.put("Immunity to all negative effects vanilla", "免疫原版负面效果");
      EXACT_TRANSLATIONS.put("Night Vision", "夜视");
      EXACT_TRANSLATIONS.put("Speed I, Strength I, Haste I", "速度 I、力量 I、急迫 I");
      EXACT_TRANSLATIONS.put("Resistance II, Strength II, Night Vision", "抗性提升 II、力量 II、夜视");
      EXACT_TRANSLATIONS.put("While crouching you will get Jump Boost IV", "潜行时获得跳跃提升 IV");
      EXACT_TRANSLATIONS.put("Press X to dash in the direction you're facing", "按 X 朝面向方向冲刺");
      EXACT_TRANSLATIONS.put("Haste I and Speed I for 8 seconds", "急迫 I 与速度 I，持续 8 秒");
      EXACT_TRANSLATIONS.put("Haste II, Resistance I, Speed I, Hunger I, for 34 seconds", "急迫 II、抗性提升 I、速度 I、饥饿 I，持续 34 秒");
      EXACT_TRANSLATIONS.put("Haste II, Resistance I, Speed II, Hunger II, for 45 seconds", "急迫 II、抗性提升 I、速度 II、饥饿 II，持续 45 秒");
      EXACT_TRANSLATIONS.put("Resistance II and Hunger I for 45 seconds", "抗性提升 II 与饥饿 I，持续 45 秒");
      EXACT_TRANSLATIONS.put("Resistance II and Hunger II for 35 seconds", "抗性提升 II 与饥饿 II，持续 35 秒");
      EXACT_TRANSLATIONS.put("Survival Instinct", "生存本能");
   }
}
