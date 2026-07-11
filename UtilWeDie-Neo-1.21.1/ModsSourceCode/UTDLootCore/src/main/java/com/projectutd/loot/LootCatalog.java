package com.projectutd.loot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable runtime view of the generated registry and balance resources. */
public final class LootCatalog {
    private static final String REGISTRY_RESOURCE = "/data/utd_loot_core/loot/registry.json";
    private static final String BALANCE_RESOURCE = "/data/utd_loot_core/loot/balance.json";
    private static final Gson GSON = new Gson();

    private final List<LootEntry> entries;
    private final Map<String, ContainerConfig> containers;
    private final Map<String, List<String>> channelTemplates;
    private final Map<String, Integer> simpleLevels;
    private final Map<String, Integer> variantLevels;
    private final Map<String, Integer> logicalLevels;
    private final int templateCount;

    private LootCatalog(
        List<LootEntry> entries,
        Map<String, ContainerConfig> containers,
        Map<String, List<String>> channelTemplates,
        int templateCount
    ) {
        this.entries = List.copyOf(entries);
        this.containers = Map.copyOf(containers);
        this.channelTemplates = Map.copyOf(channelTemplates);
        this.templateCount = templateCount;
        this.simpleLevels = buildSimpleLevelIndex(entries);
        this.variantLevels = buildVariantLevelIndex(entries);
        this.logicalLevels = buildLogicalLevelIndex(entries);
    }

    public static LootCatalog loadBundled() {
        JsonArray registry = readJson(REGISTRY_RESOURCE).getAsJsonArray();
        JsonObject balance = readJson(BALANCE_RESOURCE).getAsJsonObject();
        List<LootEntry> entries = new ArrayList<>(registry.size());
        for (JsonElement element : registry) {
            JsonObject row = element.getAsJsonObject();
            entries.add(new LootEntry(
                requiredString(row, "id"),
                getBoolean(row, "lootEnabled", true),
                getInt(row, "level", 0),
                Math.max(1, getInt(row, "count", 1)),
                stringSet(row.getAsJsonArray("directedTemplates")),
                getInt(row, "directedWeight", 0),
                optionalString(row, "lootItemId"),
                optionalString(row, "lootNbt")
            ));
        }

        Map<String, ContainerConfig> containers = new LinkedHashMap<>();
        JsonObject configObject = balance.getAsJsonObject("containerConfig");
        for (Map.Entry<String, JsonElement> configEntry : configObject.entrySet()) {
            JsonObject config = configEntry.getValue().getAsJsonObject();
            containers.put(configEntry.getKey(), new ContainerConfig(
                configEntry.getKey(),
                optionalString(config, "family"),
                optionalString(config, "template"),
                optionalString(config, "pityChannel"),
                getBoolean(config, "countPity", false)
            ));
        }

        Map<String, List<String>> channelTemplates = new LinkedHashMap<>();
        JsonObject channelObject = balance.getAsJsonObject("channelTemplates");
        for (Map.Entry<String, JsonElement> channelEntry : channelObject.entrySet()) {
            List<String> values = new ArrayList<>();
            for (JsonElement value : channelEntry.getValue().getAsJsonArray()) {
                values.add(value.getAsString());
            }
            channelTemplates.put(channelEntry.getKey(), List.copyOf(values));
        }
        return new LootCatalog(
            entries,
            containers,
            channelTemplates,
            balance.getAsJsonObject("templates").size()
        );
    }

    private static JsonElement readJson(String resource) {
        try (InputStream stream = LootCatalog.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Bundled loot resource is missing: " + resource);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read bundled loot resource: " + resource, exception);
        }
    }

    private static String requiredString(JsonObject object, String key) {
        String value = optionalString(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required loot field: " + key);
        }
        return value;
    }

    private static String optionalString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    private static Set<String> stringSet(JsonArray values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> output = new LinkedHashSet<>();
        for (JsonElement value : values) {
            output.add(value.getAsString());
        }
        return Collections.unmodifiableSet(output);
    }

    public int registryCount() {
        return entries.size();
    }

    public long enabledCount() {
        return entries.stream().filter(LootEntry::lootEnabled).count();
    }

    public int containerCount() {
        return containers.size();
    }

    public int templateCount() {
        return templateCount;
    }

    public ContainerConfig container(String lootTableId) {
        return containers.get(lootTableId);
    }

    public LootEntry pickDirectedCandidate(
        int level,
        String template,
        String channel,
        RandomSource random
    ) {
        List<LootEntry> candidates = directedCandidates(level, template);
        if (candidates.isEmpty() && channel != null && !channel.isBlank()) {
            Map<String, LootEntry> deduplicated = new LinkedHashMap<>();
            for (String channelTemplate : channelTemplates.getOrDefault(channel, List.of())) {
                for (LootEntry entry : directedCandidates(level, channelTemplate)) {
                    LootEntry existing = deduplicated.get(entry.id());
                    if (existing == null || existing.directedWeight() < entry.directedWeight()) {
                        deduplicated.put(entry.id(), entry);
                    }
                }
            }
            candidates = List.copyOf(deduplicated.values());
        }
        int totalWeight = candidates.stream().mapToInt(LootEntry::directedWeight).sum();
        if (totalWeight <= 0) {
            return null;
        }
        int roll = random.nextInt(totalWeight);
        for (LootEntry candidate : candidates) {
            roll -= candidate.directedWeight();
            if (roll < 0) {
                return candidate;
            }
        }
        return candidates.getLast();
    }

    private List<LootEntry> directedCandidates(int level, String template) {
        if (template == null || template.isBlank()) {
            return List.of();
        }
        return entries.stream()
            .filter(LootEntry::lootEnabled)
            .filter(this::physicalItemExists)
            .filter(entry -> entry.level() == level)
            .filter(entry -> entry.directedWeight() > 0)
            .filter(entry -> entry.directedTemplates().contains(template))
            .toList();
    }

    private boolean physicalItemExists(LootEntry entry) {
        String physicalId = entry.lootItemId() == null ? entry.id() : entry.lootItemId();
        ResourceLocation location = ResourceLocation.tryParse(physicalId);
        return location != null && BuiltInRegistries.ITEM.containsKey(location);
    }

    public ItemStack createStack(LootEntry entry) {
        if (entry == null || !entry.lootEnabled()) {
            return ItemStack.EMPTY;
        }
        String physicalId = entry.lootItemId() == null ? entry.id() : entry.lootItemId();
        ResourceLocation location = ResourceLocation.tryParse(physicalId);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
            UtdLootCore.LOGGER.warn("Pity candidate is not present in the item registry: {}", physicalId);
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == Items.AIR && !"minecraft:air".equals(physicalId)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, entry.count());
        if (entry.lootNbt() != null && !entry.lootNbt().isBlank()) {
            try {
                CompoundTag tag = TagParser.parseTag(entry.lootNbt());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            } catch (CommandSyntaxException exception) {
                UtdLootCore.LOGGER.error("Invalid custom-data SNBT for {}", entry.id(), exception);
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    public int levelOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        ResourceLocation baseLocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String base = baseLocation.toString();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            Integer logicalLevel = logicalLevels.get(LootIdentity.resolve(base, tag.toString()));
            if (logicalLevel != null) {
                return logicalLevel;
            }
            String signature = variantSignature(base, tag);
            Integer variantLevel = variantLevels.get(signature);
            if (variantLevel != null) {
                return variantLevel;
            }
        }
        return simpleLevels.getOrDefault(base, -1);
    }

    private static Map<String, Integer> buildSimpleLevelIndex(List<LootEntry> entries) {
        Map<String, Integer> output = new HashMap<>();
        for (LootEntry entry : entries) {
            if (entry.lootNbt() != null && !entry.lootNbt().isBlank()) {
                continue;
            }
            String physicalId = entry.lootItemId() == null ? entry.id() : entry.lootItemId();
            output.merge(physicalId, entry.level(), Math::max);
        }
        return Map.copyOf(output);
    }

    private static Map<String, Integer> buildVariantLevelIndex(List<LootEntry> entries) {
        Map<String, Integer> output = new HashMap<>();
        for (LootEntry entry : entries) {
            if (entry.lootNbt() == null || entry.lootNbt().isBlank()) {
                continue;
            }
            try {
                CompoundTag tag = TagParser.parseTag(entry.lootNbt());
                String base = entry.lootItemId() == null ? entry.id() : entry.lootItemId();
                output.put(variantSignature(base, tag), entry.level());
            } catch (CommandSyntaxException exception) {
                throw new IllegalStateException("Invalid registry SNBT for " + entry.id(), exception);
            }
        }
        return Map.copyOf(output);
    }

    private static Map<String, Integer> buildLogicalLevelIndex(List<LootEntry> entries) {
        Map<String, Integer> output = new HashMap<>();
        for (LootEntry entry : entries) {
            output.merge(entry.id(), entry.level(), Math::max);
        }
        return Map.copyOf(output);
    }

    private static String variantSignature(String base, CompoundTag tag) {
        if (tag.contains("GunId")) {
            return base + "|GunId=" + tag.getString("GunId");
        }
        if (tag.contains("BlockId")) {
            return base + "|BlockId=" + tag.getString("BlockId");
        }
        if (tag.contains("firstpersonfoodeating_profile")) {
            CompoundTag profile = tag.getCompound("firstpersonfoodeating_profile");
            if (profile.contains("food_id")) {
                return base + "|food_id=" + profile.getString("food_id");
            }
        }
        if (tag.contains("food_id")) {
            return base + "|food_id=" + tag.getString("food_id");
        }
        return base + "|custom=" + tag;
    }

    public record LootEntry(
        String id,
        boolean lootEnabled,
        int level,
        int count,
        Set<String> directedTemplates,
        int directedWeight,
        String lootItemId,
        String lootNbt
    ) {}

    public record ContainerConfig(
        String lootTableId,
        String family,
        String template,
        String pityChannel,
        boolean countPity
    ) {}
}
