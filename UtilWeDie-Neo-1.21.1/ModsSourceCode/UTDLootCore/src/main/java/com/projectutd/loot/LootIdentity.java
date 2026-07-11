package com.projectutd.loot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mirrors the logical-stack identity rules used by the retired KubeJS pity scanner. */
final class LootIdentity {
    private static final Pattern GUN_ID = Pattern.compile("(?:^|[,{])\\s*GunId\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern FOOD_ID = Pattern.compile("(?:^|[,{])\\s*food_id\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern BLOCK_ID = Pattern.compile("(?:^|[,{])\\s*BlockId\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private LootIdentity() {}

    static String resolve(String baseId, String customDataSnbt) {
        String base = baseId == null ? "" : baseId;
        String snbt = customDataSnbt == null ? "" : customDataSnbt;
        if ("tacz:modern_kinetic_gun".equals(base)) {
            String gunId = first(GUN_ID, snbt);
            return gunId.isBlank() ? base : gunId;
        }
        if ("firstpersonfoodeating:pack_food".equals(base)) {
            String foodId = first(FOOD_ID, snbt);
            return foodId.isBlank()
                    ? base
                    : "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\""
                            + foodId + "\"}}";
        }
        if ("tacz:workbench_b".equals(base)) {
            String blockId = first(BLOCK_ID, snbt);
            return blockId.isBlank() ? base : "tacz:workbench_b{BlockId:\"" + blockId + "\"}";
        }
        return base;
    }

    private static String first(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }
}
