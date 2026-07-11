package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.core.AssetIdentity;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import com.ymrsl.utdassetmanager.model.ManifestEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, Minecraft-independent view of a successfully parsed status manifest.
 */
final class ManifestCatalog {
    private static final String FALLBACK_SEPARATOR = "\n";

    private final Map<String, ManifestEntry> exactEntries;
    private final Map<String, ManifestEntry> plainEntries;
    private final Map<String, ManifestEntry> variantEntries;
    private final Set<String> ambiguousPlainKeys;
    private final Set<String> ambiguousVariantKeys;
    private final List<AssetRecord> directoryRecords;

    private ManifestCatalog(
            Map<String, ManifestEntry> exactEntries,
            Map<String, ManifestEntry> plainEntries,
            Map<String, ManifestEntry> variantEntries,
            Set<String> ambiguousPlainKeys,
            Set<String> ambiguousVariantKeys,
            List<AssetRecord> directoryRecords
    ) {
        this.exactEntries = Collections.unmodifiableMap(new LinkedHashMap<>(exactEntries));
        this.plainEntries = Collections.unmodifiableMap(new LinkedHashMap<>(plainEntries));
        this.variantEntries = Collections.unmodifiableMap(new LinkedHashMap<>(variantEntries));
        this.ambiguousPlainKeys = Collections.unmodifiableSet(new LinkedHashSet<>(ambiguousPlainKeys));
        this.ambiguousVariantKeys = Collections.unmodifiableSet(new LinkedHashSet<>(ambiguousVariantKeys));
        this.directoryRecords = List.copyOf(directoryRecords);
    }

    static ManifestCatalog empty() {
        return new ManifestCatalog(Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), List.of());
    }

    static ManifestCatalog from(List<ManifestEntry> entries) {
        if (entries == null) {
            throw new IllegalStateException("status manifest has no items array");
        }

        Map<String, ManifestEntry> exact = new LinkedHashMap<>();
        Map<String, ManifestEntry> plain = new LinkedHashMap<>();
        Map<String, ManifestEntry> variant = new LinkedHashMap<>();
        Set<String> ambiguousPlain = new LinkedHashSet<>();
        Set<String> ambiguousVariant = new LinkedHashSet<>();
        List<AssetRecord> directory = new ArrayList<>(entries.size());

        for (ManifestEntry source : entries) {
            if (source == null) {
                throw new IllegalStateException("status manifest contains a null entry");
            }
            ManifestEntry entry = snapshot(source);
            if (entry.assetKey.isBlank()) {
                throw new IllegalStateException("status manifest contains an entry without asset_key");
            }
            if (entry.registryId.isBlank()) {
                throw new IllegalStateException("status manifest entry has no registry_id: " + entry.assetKey);
            }
            if (exact.putIfAbsent(entry.assetKey, entry) != null) {
                throw new IllegalStateException("status manifest duplicate asset_key: " + entry.assetKey);
            }

            if (!entry.variantDiscriminator.isBlank()) {
                addFallback(variant, ambiguousVariant,
                        variantFallbackKey(entry.registryId, entry.variantDiscriminator), entry);
            } else if (entry.variantKey.isBlank() && isManifestPlainKind(entry.identityKind)) {
                addFallback(plain, ambiguousPlain, entry.registryId, entry);
            }
            directory.add(toDirectoryRecord(entry));
        }

        return new ManifestCatalog(exact, plain, variant, ambiguousPlain, ambiguousVariant, directory);
    }

    Resolution resolve(String assetKey, AssetRecord record) {
        String requestedAssetKey = clean(assetKey);
        if (requestedAssetKey.isBlank() && record != null) {
            requestedAssetKey = clean(record.assetKey);
        }
        ManifestEntry exact = exactEntries.get(requestedAssetKey);
        if (exact != null || record == null) {
            return new Resolution(exact, "");
        }

        String discriminator = clean(record.variantDiscriminator);
        if (discriminator.isBlank()) {
            discriminator = AssetIdentity.variantDiscriminator(record.componentsSnbt);
        }
        if (!discriminator.isBlank()) {
            String key = variantFallbackKey(clean(record.registryId), discriminator);
            if (ambiguousVariantKeys.contains(key)) {
                return new Resolution(null, "manifest_fallback_ambiguous: " + discriminator);
            }
            return new Resolution(variantEntries.get(key), "");
        }

        // Component-bearing records require a frozen discriminator. Falling back only by
        // registry id would silently merge distinct guns, ammunition or food profiles.
        if (!isRecordPlainKind(record.variantKind)) {
            return new Resolution(null, "");
        }

        String registryId = clean(record.registryId);
        if (ambiguousPlainKeys.contains(registryId)) {
            return new Resolution(null, "manifest_fallback_ambiguous: " + registryId);
        }
        return new Resolution(plainEntries.get(registryId), "");
    }

    List<AssetRecord> directorySnapshot() {
        return directoryRecords.stream().map(AssetRecord::copy).toList();
    }

    int size() {
        return directoryRecords.size();
    }

    String canonicalRowKey(AssetRecord record) {
        if (record == null) {
            return "";
        }
        Resolution resolution = resolve(record.assetKey, record);
        if (resolution.entry != null) {
            return resolution.entry.assetKey;
        }
        String assetKey = clean(record.assetKey);
        if (!assetKey.isBlank()) {
            return assetKey;
        }
        return clean(record.registryId) + FALLBACK_SEPARATOR
                + clean(record.variantDiscriminator) + FALLBACK_SEPARATOR
                + clean(record.variantKey);
    }

    private static void addFallback(
            Map<String, ManifestEntry> target,
            Set<String> ambiguous,
            String key,
            ManifestEntry entry
    ) {
        ManifestEntry existing = target.putIfAbsent(key, entry);
        if (existing != null && !existing.assetKey.equals(entry.assetKey)) {
            ambiguous.add(key);
        }
    }

    private static boolean isManifestPlainKind(String identityKind) {
        String kind = clean(identityKind).toLowerCase(Locale.ROOT);
        return "plain".equals(kind) || "item".equals(kind);
    }

    private static boolean isRecordPlainKind(String variantKind) {
        String kind = clean(variantKind).toLowerCase(Locale.ROOT);
        return "plain".equals(kind) || "item".equals(kind);
    }

    private static String variantFallbackKey(String registryId, String discriminator) {
        return registryId + FALLBACK_SEPARATOR + discriminator;
    }

    private static AssetRecord toDirectoryRecord(ManifestEntry entry) {
        AssetRecord record = new AssetRecord();
        record.assetKey = entry.assetKey;
        record.variantKey = entry.variantKey;
        record.registryId = entry.registryId;
        record.modId = namespace(entry.registryId);
        record.variantKind = entry.identityKind.isBlank() ? "plain" : entry.identityKind;
        record.variantDiscriminator = entry.variantDiscriminator;
        record.componentsSnbt = objectDefault(entry.componentsSnbt);
        record.componentsCanonical = objectDefault(entry.componentsCanonical);
        record.identityComponentsCanonical = objectDefault(entry.identityComponentsCanonical);
        record.itemStackSnbt = "{}";
        record.translationKey = entry.translationKey;
        record.displayNameZhCn = entry.clientNameZhCn.isBlank() ? entry.registryId : entry.clientNameZhCn;
        record.capturedLocale = "";
        record.humanSelected = entry.humanSelected;
        record.selectedAt = "";
        record.updatedAt = "";
        return record;
    }

    private static ManifestEntry snapshot(ManifestEntry source) {
        ManifestEntry copy = new ManifestEntry();
        copy.assetKey = clean(source.assetKey);
        copy.registryId = clean(source.registryId);
        copy.variantKey = clean(source.variantKey);
        copy.identityKind = clean(source.identityKind);
        copy.variantDiscriminator = clean(source.variantDiscriminator);
        copy.clientNameZhCn = clean(source.clientNameZhCn);
        copy.translationKey = clean(source.translationKey);
        copy.componentsSnbt = clean(source.componentsSnbt);
        copy.componentsCanonical = clean(source.componentsCanonical);
        copy.identityComponentsCanonical = clean(source.identityComponentsCanonical);
        copy.humanSelected = source.humanSelected;
        copy.catalogued = source.catalogued;
        copy.recipeInputCount = source.recipeInputCount;
        copy.recipeOutputCount = source.recipeOutputCount;
        copy.lootEnabled = source.lootEnabled;
        copy.lootLevel = source.lootLevel;
        copy.syncState = clean(source.syncState);
        copy.catalogHash = clean(source.catalogHash);
        copy.deployedHash = clean(source.deployedHash);
        copy.stale = source.stale;
        copy.issues = source.issues == null ? new ArrayList<>() : new ArrayList<>(source.issues);
        return copy;
    }

    private static String namespace(String registryId) {
        int separator = registryId.indexOf(':');
        return separator <= 0 ? "" : registryId.substring(0, separator);
    }

    private static String objectDefault(String value) {
        String cleaned = clean(value);
        return cleaned.isBlank() ? "{}" : cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    record Resolution(ManifestEntry entry, String issue) {
    }
}
