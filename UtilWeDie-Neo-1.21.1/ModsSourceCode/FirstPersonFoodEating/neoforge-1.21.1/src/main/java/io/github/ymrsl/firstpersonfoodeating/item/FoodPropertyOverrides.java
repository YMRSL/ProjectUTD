package io.github.ymrsl.firstpersonfoodeating.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

/** Read-only bridge for reviewed UTD food-property candidates. */
final class FoodPropertyOverrides {
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("firstpersonfoodeating")
            .resolve("utd_food_overrides.json");
    private static final long CHECK_INTERVAL_MILLIS = 1_000L;
    private static Map<String, Profile> profiles = Map.of();
    private static long lastCheck;
    private static long observedModified = Long.MIN_VALUE;
    private static long observedSize = Long.MIN_VALUE;

    private FoodPropertyOverrides() {
    }

    static synchronized Profile find(ItemStack stack) {
        reloadIfChanged();
        ResourceLocation foodId = FoodStackData.getFoodId(stack).orElse(null);
        if (foodId != null) {
            Profile exact = profiles.get(foodId.toString());
            if (exact != null) return exact;
        }
        ResourceLocation registryId = FoodStackData.resolveFoodId(stack);
        return profiles.get(registryId.toString());
    }

    private static void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_INTERVAL_MILLIS) return;
        lastCheck = now;
        try {
            if (!Files.isRegularFile(FILE)) {
                profiles = Map.of();
                observedModified = -1L;
                observedSize = -1L;
                return;
            }
            long modified = Files.getLastModifiedTime(FILE).toMillis();
            long size = Files.size(FILE);
            if (modified == observedModified && size == observedSize) return;
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                profiles = parse(root);
                observedModified = modified;
                observedSize = size;
            }
        } catch (Exception ignored) {
            // Fail closed: retain the last valid in-memory snapshot.
        }
    }

    static Map<String, Profile> parse(JsonObject root) {
        if (root == null || !root.has("foods") || !root.get("foods").isJsonArray()) return Map.of();
        Map<String, Profile> parsed = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("foods")) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String foodId = text(row, "food_id");
            if (ResourceLocation.tryParse(foodId) == null) continue;
            int nutrition = clamp(integer(row, "nutrition", 0), 0, 100);
            float saturation = clamp(decimal(row, "saturation", 0.0f), 0.0f, 100.0f);
            int thirst = clamp(integer(row, "thirst_delta", 0), -100, 100);
            int water = clamp(integer(row, "water_delta", 0), -100, 100);
            FoodStackData.ThirstMode mode = FoodStackData.ThirstMode.fromString(text(row, "thirst_mode"));
            List<FoodStackData.FoodEffect> effects = parseEffects(row.getAsJsonArray("effects"));
            parsed.put(foodId, new Profile(nutrition, saturation, thirst, water, mode, effects));
        }
        return Map.copyOf(parsed);
    }

    private static List<FoodStackData.FoodEffect> parseEffects(JsonArray values) {
        if (values == null || values.isEmpty()) return List.of();
        List<FoodStackData.FoodEffect> result = new ArrayList<>();
        for (JsonElement element : values) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(text(row, "id"));
            if (id == null) continue;
            result.add(new FoodStackData.FoodEffect(
                    id,
                    clamp(integer(row, "duration_ticks", 1), 1, 72_000),
                    clamp(integer(row, "amplifier", 0), 0, 255),
                    clamp(decimal(row, "chance", 1.0f), 0.0f, 1.0f),
                    false,
                    true,
                    true));
        }
        return List.copyOf(result);
    }

    private static String text(JsonObject row, String key) {
        return row.has(key) && row.get(key).isJsonPrimitive() ? row.get(key).getAsString().trim() : "";
    }

    private static int integer(JsonObject row, String key, int fallback) {
        try {
            return row.has(key) ? row.get(key).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static float decimal(JsonObject row, String key, float fallback) {
        try {
            return row.has(key) ? row.get(key).getAsFloat() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    record Profile(
            int nutrition,
            float saturation,
            int thirstDelta,
            int waterDelta,
            FoodStackData.ThirstMode thirstMode,
            List<FoodStackData.FoodEffect> effects
    ) {
        Profile {
            effects = effects == null ? List.of() : List.copyOf(effects);
        }
    }
}
