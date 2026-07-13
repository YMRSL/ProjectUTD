package com.ymrsl.utdassetmanager.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.fml.loading.FMLPaths;

/** Lightweight read-only view of the Workbench property deployment ledger. */
final class ItemPropertyStatusRepository {
    private static final ItemPropertyStatusRepository INSTANCE = new ItemPropertyStatusRepository();
    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("utd_asset_manager")
            .resolve("item_properties.json");
    private static final long CHECK_INTERVAL_MILLIS = 1_000L;

    private Map<String, PropertyStatus> exact = Map.of();
    private Map<String, PropertyStatus> variants = Map.of();
    private Map<String, PropertyStatus> plain = Map.of();
    private long lastCheck;
    private long observedModified = Long.MIN_VALUE;
    private long observedSize = Long.MIN_VALUE;

    private ItemPropertyStatusRepository() {
    }

    static ItemPropertyStatusRepository get() {
        return INSTANCE;
    }

    synchronized PropertyStatus resolve(AssetRecord record) {
        reloadIfChanged();
        if (record == null) return PropertyStatus.NONE;
        PropertyStatus status = exact.get(clean(record.assetKey));
        if (status != null) return status;
        String registry = clean(record.registryId);
        String discriminator = clean(record.variantDiscriminator);
        if (!discriminator.isBlank()) {
            status = variants.get(registry + "\n" + discriminator);
            return status == null ? PropertyStatus.NONE : status;
        }
        status = plain.get(registry);
        return status == null ? PropertyStatus.NONE : status;
    }

    private void reloadIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_INTERVAL_MILLIS) return;
        lastCheck = now;
        try {
            if (!Files.isRegularFile(FILE)) {
                exact = Map.of();
                variants = Map.of();
                plain = Map.of();
                observedModified = -1L;
                observedSize = -1L;
                return;
            }
            long modified = Files.getLastModifiedTime(FILE).toMillis();
            long size = Files.size(FILE);
            if (modified == observedModified && size == observedSize) return;
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                parse(JsonParser.parseReader(reader).getAsJsonObject());
                observedModified = modified;
                observedSize = size;
            }
        } catch (Exception ignored) {
            // Fail closed and keep the last valid snapshot.
        }
    }

    private void parse(JsonObject root) {
        Map<String, PropertyStatus> nextExact = new LinkedHashMap<>();
        Map<String, PropertyStatus> nextVariants = new LinkedHashMap<>();
        Map<String, PropertyStatus> nextPlain = new LinkedHashMap<>();
        if (root == null || !root.has("properties") || !root.get("properties").isJsonArray()) {
            exact = Map.of();
            variants = Map.of();
            plain = Map.of();
            return;
        }
        for (JsonElement element : root.getAsJsonArray("properties")) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            if (!flag(row, "enabled")) continue;
            String assetKey = text(row, "asset_key");
            String registry = text(row, "registry_id");
            String discriminator = text(row, "variant_discriminator");
            if (assetKey.isBlank() || registry.isBlank()) continue;
            PropertyStatus status = new PropertyStatus(
                    object(row, "rarity"), object(row, "blockz"), object(row, "tacz"), object(row, "food"));
            if (!status.managed()) continue;
            nextExact.put(assetKey, status);
            if (discriminator.isBlank()) nextPlain.put(registry, status);
            else nextVariants.put(registry + "\n" + discriminator, status);
        }
        exact = Map.copyOf(nextExact);
        variants = Map.copyOf(nextVariants);
        plain = Map.copyOf(nextPlain);
    }

    private static boolean object(JsonObject row, String key) {
        return row.has(key) && row.get(key).isJsonObject();
    }

    private static boolean flag(JsonObject row, String key) {
        try {
            return row.has(key) && row.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String text(JsonObject row, String key) {
        try {
            return row.has(key) ? clean(row.get(key).getAsString()) : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    record PropertyStatus(boolean rarity, boolean blockz, boolean tacz, boolean food) {
        static final PropertyStatus NONE = new PropertyStatus(false, false, false, false);

        boolean managed() {
            return rarity || blockz || tacz || food;
        }

        String summary() {
            StringBuilder value = new StringBuilder();
            if (rarity) value.append('R');
            if (blockz) value.append('B');
            if (tacz) value.append('T');
            if (food) value.append('F');
            return value.isEmpty() ? "—" : value.toString();
        }
    }
}
