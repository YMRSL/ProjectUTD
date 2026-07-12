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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.neoforged.fml.loading.FMLPaths;

public final class AssetRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final AssetRepository INSTANCE = new AssetRepository();
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final Map<String, AssetRecord> selected = new LinkedHashMap<>();
    private final Supplier<Path> directorySupplier;
    private ManifestCatalog manifestCatalog = ManifestCatalog.empty();
    private boolean loaded;
    private boolean whitelistWritable = true;
    private boolean forceManifestReload;
    private long manifestModified = Long.MIN_VALUE;
    private long lastObservedManifestModified = Long.MIN_VALUE;
    private long lastManifestCheck;
    private String repositoryError = "";
    private String manifestError = "";
    private String exportError = "";

    private AssetRepository() {
        this(() -> FMLPaths.CONFIGDIR.get().resolve("utd_asset_manager"));
    }

    AssetRepository(Path directory) {
        this(() -> directory);
    }

    private AssetRepository(Supplier<Path> directorySupplier) {
        this.directorySupplier = directorySupplier;
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

    public synchronized List<AssetRecord> allManifestDirectory() {
        ensureLoaded();
        reloadManifestIfChanged();
        return manifestCatalog.directorySnapshot();
    }

    public synchronized int manifestDirectorySize() {
        ensureLoaded();
        reloadManifestIfChanged();
        return manifestCatalog.size();
    }

    /**
     * Last successfully loaded manifest mtime, or -1 when the manifest was successfully observed absent.
     * A malformed replacement keeps the previous value together with the last-known-good catalog.
     */
    public synchronized long manifestRevision() {
        ensureLoaded();
        reloadManifestIfChanged();
        return manifestModified;
    }

    public synchronized String canonicalRowKey(AssetRecord record) {
        ensureLoaded();
        reloadManifestIfChanged();
        return manifestCatalog.canonicalRowKey(record);
    }

    public synchronized boolean manifestHumanSelected(AssetRecord record) {
        ensureLoaded();
        reloadManifestIfChanged();
        ManifestCatalog.Resolution resolution = manifestCatalog.resolve(
                record == null ? "" : record.assetKey, record);
        return resolution.entry() != null && resolution.entry().humanSelected;
    }

    public synchronized boolean isSelectedIdentity(AssetRecord record) {
        ensureLoaded();
        reloadManifestIfChanged();
        String target = manifestCatalog.canonicalRowKey(record);
        if (target.isBlank()) {
            return false;
        }
        for (AssetRecord selectedRecord : selected.values()) {
            if (target.equals(manifestCatalog.canonicalRowKey(selectedRecord))) {
                return true;
            }
        }
        return false;
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
        return statusForInternal(assetKey, selected.get(assetKey), false);
    }

    public synchronized AssetStatus statusFor(AssetRecord record) {
        ensureLoaded();
        return statusForInternal(record == null ? "" : record.assetKey, record, false);
    }

    public synchronized AssetStatus projectStatusFor(AssetRecord record) {
        ensureLoaded();
        return statusForInternal(record == null ? "" : record.assetKey, record, true);
    }

    private AssetStatus statusForInternal(String assetKey, AssetRecord record, boolean includeManifestHumanSelected) {
        ensureLoaded();
        reloadManifestIfChanged();
        ManifestCatalog.Resolution resolution = manifestCatalog.resolve(assetKey, record);
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
                selected.containsKey(assetKey)
                        || (includeManifestHumanSelected && manifest != null && manifest.humanSelected),
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

    public synchronized void forceReloadManifest() {
        ensureLoaded();
        forceManifestReload = true;
        lastManifestCheck = 0L;
        reloadManifestIfChanged();
    }

    public synchronized Path exportSnapshot() {
        return exportSnapshot(Map.of());
    }

    public synchronized Path exportSnapshot(Map<String, String> iconDataUrls) {
        ensureLoaded();
        requireWritableWhitelist();
        reloadManifestIfChanged();
        validateExportable();
        return exportSnapshotRecords(allSelected(), iconDataUrls, false);
    }

    /** Exports an explicit UI selection, including read-only project-directory records. */
    public synchronized Path exportSnapshot(
            List<AssetRecord> recordsToExport,
            Map<String, String> iconDataUrls,
            boolean includeManifestHumanSelected
    ) {
        ensureLoaded();
        requireWritableWhitelist();
        reloadManifestIfChanged();
        Map<String, AssetRecord> uniqueByKey = new LinkedHashMap<>();
        for (AssetRecord record : recordsToExport == null ? List.<AssetRecord>of() : recordsToExport) {
            if (record != null && record.assetKey != null && !record.assetKey.isBlank()) {
                uniqueByKey.putIfAbsent(record.assetKey, record);
            }
        }
        List<AssetRecord> unique = new ArrayList<>(uniqueByKey.values());
        return exportSnapshotRecords(unique, iconDataUrls, includeManifestHumanSelected);
    }

    private Path exportSnapshotRecords(
            List<AssetRecord> recordsToExport,
            Map<String, String> iconDataUrls,
            boolean includeManifestHumanSelected
    ) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", 1);
        root.put("producer", "utd_asset_manager");
        root.put("exported_at", Instant.now().toString());
        List<Map<String, Object>> records = new ArrayList<>();
        for (AssetRecord record : recordsToExport) {
            AssetStatus status = includeManifestHumanSelected ? projectStatusFor(record) : statusFor(record);
            records.add(exportRecord(record, status, iconDataUrls.get(record.assetKey)));
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
        return exportRecord(record, status, null);
    }

    static Map<String, Object> exportRecord(AssetRecord record, AssetStatus status, String iconDataUrl) {
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
        if (iconDataUrl != null && iconDataUrl.startsWith("data:image/png;base64,")) {
            exported.put("icon_data_url", iconDataUrl);
        }
        exported.put("captured_locale", record.capturedLocale);
        exported.put("human_selected", status.humanSelected());
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

    public synchronized String manifestErrorMessage() {
        ensureLoaded();
        reloadManifestIfChanged();
        return manifestError;
    }

    public Path directory() {
        return directorySupplier.get();
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
        boolean forced = forceManifestReload;
        if (!forced && now - lastManifestCheck < 1_000L) {
            return;
        }
        forceManifestReload = false;
        lastManifestCheck = now;
        Path path = directory().resolve("status_manifest.json");
        try {
            long modified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
            if (!forced && modified == lastObservedManifestModified) {
                return;
            }
            lastObservedManifestModified = modified;
            if (modified < 0L) {
                manifestCatalog = ManifestCatalog.empty();
                manifestModified = modified;
                manifestError = "";
                return;
            }
            ManifestCatalog nextCatalog;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                StatusManifest manifest = GSON.fromJson(reader, StatusManifest.class);
                validateManifestHeader(manifest);
                nextCatalog = ManifestCatalog.from(manifest.entries);
            }
            manifestCatalog = nextCatalog;
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
