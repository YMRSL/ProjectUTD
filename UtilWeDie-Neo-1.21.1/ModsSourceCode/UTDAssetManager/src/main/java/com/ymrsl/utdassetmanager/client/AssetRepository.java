package com.ymrsl.utdassetmanager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ymrsl.utdassetmanager.core.AssetStatus;
import com.ymrsl.utdassetmanager.core.SyncState;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import com.ymrsl.utdassetmanager.model.ManifestEntry;
import com.ymrsl.utdassetmanager.model.StatusManifest;
import com.ymrsl.utdassetmanager.model.WhitelistDocument;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLPaths;

public final class AssetRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final AssetRepository INSTANCE = new AssetRepository();
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final Map<String, AssetRecord> selected = new LinkedHashMap<>();
    private final Map<String, ManifestEntry> manifestEntries = new LinkedHashMap<>();
    private final Map<String, ManifestEntry> plainManifestEntries = new LinkedHashMap<>();
    private final Map<String, ManifestEntry> variantManifestEntries = new LinkedHashMap<>();
    private final Set<String> ambiguousPlainManifestKeys = new HashSet<>();
    private final Set<String> ambiguousVariantManifestKeys = new HashSet<>();
    private boolean loaded;
    private boolean whitelistWritable = true;
    private long manifestModified = Long.MIN_VALUE;
    private long lastManifestCheck;
    private String repositoryError = "";
    private String manifestError = "";
    private String exportError = "";

    private AssetRepository() {
    }

    public static AssetRepository get() {
        return INSTANCE;
    }

    public synchronized List<AssetRecord> allSelected() {
        ensureLoaded();
        return selected.values().stream()
                .map(AssetRecord::copy)
                .sorted(Comparator.comparing((AssetRecord entry) -> entry.displayNameZhCn)
                        .thenComparing(entry -> entry.registryId)
                        .thenComparing(entry -> entry.assetKey))
                .toList();
    }

    public synchronized boolean isSelected(String assetKey) {
        ensureLoaded();
        return selected.containsKey(assetKey);
    }

    public synchronized AssetRecord find(String assetKey) {
        ensureLoaded();
        AssetRecord record = selected.get(assetKey);
        return record == null ? null : record.copy();
    }

    public synchronized void select(AssetRecord record) {
        ensureLoaded();
        requireWritableWhitelist();
        requireZhCn(record);
        AssetRecord previous = selected.get(record.assetKey);
        AssetRecord next = record.copy();
        next.humanSelected = true;
        if (previous != null && previous.selectedAt != null && !previous.selectedAt.isBlank()) {
            next.selectedAt = previous.selectedAt;
        }
        next.updatedAt = Instant.now().toString();
        selected.put(next.assetKey, next);
        try {
            saveWhitelist();
        } catch (RuntimeException error) {
            if (previous == null) selected.remove(next.assetKey);
            else selected.put(previous.assetKey, previous);
            throw error;
        }
    }

    public synchronized boolean unselect(String assetKey) {
        ensureLoaded();
        requireWritableWhitelist();
        AssetRecord removed = selected.remove(assetKey);
        if (removed == null) {
            return false;
        }
        try {
            saveWhitelist();
        } catch (RuntimeException error) {
            selected.put(assetKey, removed);
            throw error;
        }
        return true;
    }

    public synchronized int selectAll(Collection<AssetRecord> records) {
        ensureLoaded();
        requireWritableWhitelist();
        Map<String, AssetRecord> before = copySelectedMap();
        int changed = 0;
        try {
            for (AssetRecord record : records) {
                requireZhCn(record);
                if (selected.containsKey(record.assetKey)) continue;
                AssetRecord next = record.copy();
                next.humanSelected = true;
                next.updatedAt = Instant.now().toString();
                selected.put(next.assetKey, next);
                changed++;
            }
            if (changed > 0) saveWhitelist();
            return changed;
        } catch (RuntimeException error) {
            restoreSelected(before);
            throw error;
        }
    }

    public synchronized int unselectAll(Collection<String> assetKeys) {
        ensureLoaded();
        requireWritableWhitelist();
        Map<String, AssetRecord> before = copySelectedMap();
        int changed = 0;
        try {
            for (String assetKey : assetKeys) {
                if (selected.remove(assetKey) != null) changed++;
            }
            if (changed > 0) saveWhitelist();
            return changed;
        } catch (RuntimeException error) {
            restoreSelected(before);
            throw error;
        }
    }

    public synchronized AssetStatus statusFor(String assetKey) {
        ensureLoaded();
        return statusForInternal(assetKey, selected.get(assetKey));
    }

    public synchronized AssetStatus statusFor(AssetRecord record) {
        ensureLoaded();
        return statusForInternal(record == null ? "" : record.assetKey, record);
    }

    private AssetStatus statusForInternal(String assetKey, AssetRecord record) {
        ensureLoaded();
        reloadManifestIfChanged();
        ManifestResolution resolution = resolveManifest(assetKey, record);
        ManifestEntry manifest = resolution.entry();
        List<String> issues = new ArrayList<>();
        if (manifest != null && manifest.issues != null) {
            issues.addAll(manifest.issues);
        }
        if (!resolution.issue().isBlank()) issues.add(resolution.issue());
        if (!repositoryError.isBlank()) issues.add("local_repository: " + repositoryError);
        if (!manifestError.isBlank()) issues.add("status_manifest: " + manifestError);
        if (!exportError.isBlank()) issues.add("last_export: " + exportError);
        return new AssetStatus(
                selected.containsKey(assetKey),
                manifest != null && manifest.catalogued,
                manifest == null ? 0 : manifest.recipeInputCount,
                manifest == null ? 0 : manifest.recipeOutputCount,
                manifest != null && manifest.lootEnabled,
                manifest == null ? 0 : manifest.lootLevel,
                manifest == null
                        ? (selected.containsKey(assetKey) ? SyncState.PENDING : SyncState.LOCAL_ONLY)
                        : (manifest.stale ? SyncState.STALE : SyncState.parse(manifest.syncState)),
                manifest == null ? "" : manifest.catalogHash,
                manifest == null ? "" : manifest.deployedHash,
                manifest != null && manifest.stale,
                issues
        );
    }

    private ManifestResolution resolveManifest(String assetKey, AssetRecord record) {
        ManifestEntry exact = manifestEntries.get(assetKey);
        if (exact != null || record == null) {
            return new ManifestResolution(exact, "");
        }
        String discriminator = record.variantDiscriminator == null || record.variantDiscriminator.isBlank()
                ? com.ymrsl.utdassetmanager.core.AssetIdentity.variantDiscriminator(record.componentsSnbt)
                : record.variantDiscriminator;
        if (!discriminator.isBlank()) {
            String key = manifestVariantKey(record.registryId, discriminator);
            if (ambiguousVariantManifestKeys.contains(key)) {
                return new ManifestResolution(null, "manifest_fallback_ambiguous: " + discriminator);
            }
            ManifestEntry fallback = variantManifestEntries.get(key);
            return new ManifestResolution(fallback, "");
        }
        if ("plain".equalsIgnoreCase(record.variantKind)) {
            if (ambiguousPlainManifestKeys.contains(record.registryId)) {
                return new ManifestResolution(null, "manifest_fallback_ambiguous: " + record.registryId);
            }
            ManifestEntry fallback = plainManifestEntries.get(record.registryId);
            return new ManifestResolution(fallback, "");
        }
        return new ManifestResolution(null, "");
    }

    public synchronized void forceReloadManifest() {
        ensureLoaded();
        manifestModified = Long.MIN_VALUE;
        lastManifestCheck = 0L;
        reloadManifestIfChanged();
    }

    public synchronized Path exportSnapshot() {
        ensureLoaded();
        requireWritableWhitelist();
        reloadManifestIfChanged();
        validateExportable();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("producer", "utd_asset_manager");
        root.put("exported_at", Instant.now().toString());
        List<Map<String, Object>> records = new ArrayList<>();
        for (AssetRecord record : allSelected()) {
            records.add(exportRecord(record, statusFor(record.assetKey)));
        }
        root.put("items", records);
        Path exports = directory().resolve("exports");
        Path output = exports.resolve("utd-assets-" + FILE_STAMP.format(Instant.now()) + ".json");
        try {
            Files.createDirectories(exports);
            writeJsonAtomic(output, root);
            exportError = "";
            return output;
        } catch (Exception error) {
            exportError = "export_failed: " + error.getMessage();
            throw new IllegalStateException("Unable to export UTD asset snapshot", error);
        }
    }

    static Map<String, Object> exportRecord(AssetRecord record, AssetStatus status) {
        Map<String, Object> exported = new LinkedHashMap<>();
        exported.put("asset_key", record.assetKey);
        exported.put("variant_key", record.variantKey);
        exported.put("registry_id", record.registryId);
        exported.put("mod_id", record.modId);
        exported.put("identity_kind", record.variantKind);
        exported.put("variant_discriminator", record.variantDiscriminator);
        exported.put("components_snbt", record.componentsSnbt);
        exported.put("components_canonical", record.componentsCanonical);
        exported.put("identity_components_canonical", record.identityComponentsCanonical);
        exported.put("item_stack_snbt", record.itemStackSnbt);
        exported.put("translation_key", record.translationKey);
        exported.put("client_name_zh_cn", record.displayNameZhCn);
        exported.put("captured_locale", record.capturedLocale);
        exported.put("human_selected", true);
        exported.put("selected_at", record.selectedAt);
        exported.put("updated_at", record.updatedAt);
        exported.put("catalogued", status.catalogued());
        exported.put("recipe_input_count", status.recipeInputCount());
        exported.put("recipe_output_count", status.recipeOutputCount());
        exported.put("loot_enabled", status.lootEnabled());
        exported.put("loot_level", status.lootLevel());
        exported.put("sync_state", status.syncState().name().toLowerCase(java.util.Locale.ROOT));
        exported.put("catalog_hash", status.catalogHash());
        exported.put("deployed_hash", status.deployedHash());
        exported.put("stale", status.stale());
        exported.put("issues", status.issues());
        return exported;
    }

    public synchronized String lastError() {
        return String.join(" | ", java.util.stream.Stream.of(repositoryError, manifestError, exportError)
                .filter(value -> value != null && !value.isBlank()).toList());
    }

    public Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve("utd_asset_manager");
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path whitelist = directory().resolve("whitelist.json");
        try {
            Files.createDirectories(directory());
            if (Files.exists(whitelist)) {
                Map<String, AssetRecord> loadedRecords = new LinkedHashMap<>();
                try (Reader reader = Files.newBufferedReader(whitelist, StandardCharsets.UTF_8)) {
                    WhitelistDocument document = GSON.fromJson(reader, WhitelistDocument.class);
                    if (document == null || document.entries == null) {
                        throw new IllegalStateException("whitelist document is empty or has no entries array");
                    }
                    for (AssetRecord record : document.entries) {
                        if (record == null || record.assetKey == null || record.assetKey.isBlank()) {
                            throw new IllegalStateException("whitelist contains an entry without assetKey");
                        }
                        if (loadedRecords.putIfAbsent(record.assetKey, record) != null) {
                            throw new IllegalStateException("whitelist contains duplicate assetKey: " + record.assetKey);
                        }
                    }
                }
                selected.putAll(loadedRecords);
            }
        } catch (Exception error) {
            whitelistWritable = false;
            repositoryError = "whitelist_load_failed_read_only: " + error.getMessage();
        }
        reloadManifestIfChanged();
    }

    private void saveWhitelist() {
        requireWritableWhitelist();
        WhitelistDocument document = new WhitelistDocument();
        document.updatedAt = Instant.now().toString();
        document.entries = new ArrayList<>(selected.values());
        try {
            Files.createDirectories(directory());
            Path target = directory().resolve("whitelist.json");
            if (Files.exists(target)) {
                Files.copy(target, target.resolveSibling("whitelist.json.bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            writeJsonAtomic(target, document);
            repositoryError = "";
        } catch (Exception error) {
            repositoryError = "whitelist_save_failed: " + error.getMessage();
            throw new IllegalStateException("Unable to save UTD asset whitelist", error);
        }
    }

    private void reloadManifestIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastManifestCheck < 1_000L) {
            return;
        }
        lastManifestCheck = now;
        Path path = directory().resolve("status_manifest.json");
        try {
            long modified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
            if (modified == manifestModified) {
                return;
            }
            if (modified < 0L) {
                manifestEntries.clear();
                plainManifestEntries.clear();
                variantManifestEntries.clear();
                ambiguousPlainManifestKeys.clear();
                ambiguousVariantManifestKeys.clear();
                manifestModified = modified;
                manifestError = "";
                return;
            }
            Map<String, ManifestEntry> nextExact = new LinkedHashMap<>();
            Map<String, ManifestEntry> nextPlain = new LinkedHashMap<>();
            Map<String, ManifestEntry> nextVariant = new LinkedHashMap<>();
            Set<String> nextAmbiguousPlain = new HashSet<>();
            Set<String> nextAmbiguousVariant = new HashSet<>();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                StatusManifest manifest = GSON.fromJson(reader, StatusManifest.class);
                validateManifestHeader(manifest);
                for (ManifestEntry entry : manifest.entries) {
                    if (entry == null || entry.assetKey == null || entry.assetKey.isBlank()) {
                        throw new IllegalStateException("status manifest contains an entry without asset_key");
                    }
                    if (entry.registryId == null || entry.registryId.isBlank()) {
                        throw new IllegalStateException("status manifest entry has no registry_id: " + entry.assetKey);
                    }
                    if (nextExact.putIfAbsent(entry.assetKey, entry) != null) {
                        throw new IllegalStateException("status manifest duplicate asset_key: " + entry.assetKey);
                    }
                    if (entry.variantDiscriminator != null && !entry.variantDiscriminator.isBlank()) {
                        addFallback(nextVariant, nextAmbiguousVariant,
                                manifestVariantKey(entry.registryId, entry.variantDiscriminator), entry);
                    } else if ("plain".equalsIgnoreCase(entry.identityKind)
                            && (entry.variantKey == null || entry.variantKey.isBlank())) {
                        addFallback(nextPlain, nextAmbiguousPlain, entry.registryId, entry);
                    }
                }
            }
            manifestEntries.clear();
            manifestEntries.putAll(nextExact);
            plainManifestEntries.clear();
            plainManifestEntries.putAll(nextPlain);
            variantManifestEntries.clear();
            variantManifestEntries.putAll(nextVariant);
            ambiguousPlainManifestKeys.clear();
            ambiguousPlainManifestKeys.addAll(nextAmbiguousPlain);
            ambiguousVariantManifestKeys.clear();
            ambiguousVariantManifestKeys.addAll(nextAmbiguousVariant);
            manifestModified = modified;
            manifestError = "";
        } catch (Exception error) {
            manifestError = "manifest_load_failed_using_last_known_good: " + error.getMessage();
        }
    }

    private static void validateManifestHeader(StatusManifest manifest) {
        if (manifest == null || manifest.entries == null) {
            throw new IllegalStateException("status manifest is empty or has no items array");
        }
        String schema = manifest.schemaVersion == null ? "" : manifest.schemaVersion.trim();
        if (!schema.isEmpty() && !"1".equals(schema) && !"utd-asset-status/v1".equals(schema)) {
            throw new IllegalStateException("unsupported status manifest schema: " + schema);
        }
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

    private void requireWritableWhitelist() {
        if (!whitelistWritable) {
            throw new IllegalStateException("白名单文件读取失败；为避免覆盖原数据，当前为只读状态。请先修复或移走 whitelist.json。 ");
        }
    }

    private static void requireZhCn(AssetRecord record) {
        if (record == null || !"zh_cn".equalsIgnoreCase(record.capturedLocale)) {
            String locale = record == null ? "<none>" : record.capturedLocale;
            throw new IllegalStateException("客户端语言必须为 zh_cn，当前为 " + locale);
        }
    }

    private void validateExportable() {
        for (AssetRecord record : selected.values()) {
            requireZhCn(record);
        }
    }

    private Map<String, AssetRecord> copySelectedMap() {
        return new LinkedHashMap<>(selected);
    }

    private void restoreSelected(Map<String, AssetRecord> before) {
        selected.clear();
        selected.putAll(before);
    }

    private static String manifestVariantKey(String registryId, String discriminator) {
        return registryId + "\n" + discriminator;
    }

    private record ManifestResolution(ManifestEntry entry, String issue) {}

    private static void writeJsonAtomic(Path target, Object value) throws Exception {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            GSON.toJson(value, writer);
        }
        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
