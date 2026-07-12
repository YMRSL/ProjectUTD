package com.ymrsl.utdassetmanager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ymrsl.utdassetmanager.model.PresentationApplyScope;
import com.ymrsl.utdassetmanager.model.PresentationDraft;
import com.ymrsl.utdassetmanager.model.PresentationDraftDocument;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Safe local store for Chinese item-name and description editing intents.
 *
 * <p>The repository is intentionally independent from ItemStack mutation. It stores presentation drafts only and
 * exports them for an external, reviewable deployment step.</p>
 */
public final class PresentationDraftRepository {
    public static final String FILE_NAME = "presentation_drafts.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final PresentationDraftRepository INSTANCE = new PresentationDraftRepository();
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final Map<DraftKey, PresentationDraft> drafts = new LinkedHashMap<>();
    private final Supplier<Path> directorySupplier;
    private boolean loaded;
    private boolean writable = true;
    private long lastSuccessfulRevision = Long.MIN_VALUE;
    private long lastObservedRevision = Long.MIN_VALUE;
    private String loadError = "";
    private String saveError = "";
    private String exportError = "";

    private PresentationDraftRepository() {
        this(() -> FMLPaths.CONFIGDIR.get().resolve("utd_asset_manager"));
    }

    PresentationDraftRepository(Path directory) {
        this(() -> directory);
    }

    private PresentationDraftRepository(Supplier<Path> directorySupplier) {
        this.directorySupplier = directorySupplier;
    }

    public static PresentationDraftRepository get() {
        return INSTANCE;
    }

    public synchronized List<PresentationDraft> all() {
        ensureLoaded();
        return drafts.values().stream()
                .map(PresentationDraft::copy)
                .sorted(Comparator.comparing((PresentationDraft draft) -> draft.registryId)
                        .thenComparing(draft -> draft.variantDiscriminator)
                        .thenComparing(draft -> draft.applyScope.name())
                        .thenComparing(draft -> draft.assetKey))
                .toList();
    }

    /** Returns the draft whose stored identity fields exactly match the supplied values. */
    public synchronized PresentationDraft findIdentity(
            String assetKey, String registryId, String variantDiscriminator) {
        ensureLoaded();
        PresentationDraft draft = drafts.get(DraftKey.identity(assetKey, registryId, variantDiscriminator));
        return draft == null ? null : draft.copy();
    }

    /** Returns the single registry-wide draft for the exact registry id. */
    public synchronized PresentationDraft findRegistry(String registryId) {
        ensureLoaded();
        PresentationDraft draft = drafts.get(DraftKey.registry(registryId));
        return draft == null ? null : draft.copy();
    }

    /**
     * Finds the unique identity-scoped draft by stable runtime meaning rather than producer-specific asset key.
     * This bridges project catalog keys such as {@code recipe:...} with the SHA key captured in game.
     */
    public synchronized PresentationDraft findIdentitySemantic(String registryId, String variantDiscriminator) {
        ensureLoaded();
        PresentationDraft draft = findSemanticIdentityInternal(registryId, variantDiscriminator);
        return draft == null ? null : draft.copy();
    }

    /**
     * Resolves an enabled exact-identity draft first, then an enabled registry-wide fallback.
     * Disabled drafts remain queryable through {@link #findIdentity} and {@link #findRegistry} for editing.
     */
    public synchronized PresentationDraft resolveEnabled(
            String assetKey, String registryId, String variantDiscriminator) {
        ensureLoaded();
        PresentationDraft identity = drafts.get(DraftKey.identity(assetKey, registryId, variantDiscriminator));
        if (identity != null && identity.enabled) {
            return identity.copy();
        }
        PresentationDraft semanticIdentity = findSemanticIdentityInternal(registryId, variantDiscriminator);
        if (semanticIdentity != null && semanticIdentity.enabled) {
            return semanticIdentity.copy();
        }
        PresentationDraft registry = drafts.get(DraftKey.registry(registryId));
        return registry != null && registry.enabled ? registry.copy() : null;
    }

    /** Inserts or replaces one semantic target while preserving all identity strings exactly as supplied. */
    public synchronized void upsert(PresentationDraft draft) {
        ensureLoaded();
        requireWritable();
        PresentationDraft next = validatedCopy(draft);
        next.updatedAt = Instant.now().toString();
        DraftKey key = DraftKey.from(next);
        PresentationDraft sameKey = drafts.get(key);
        if (sameKey != null && !sameTarget(sameKey, next)) {
            throw semanticConflict(sameKey, next);
        }
        requireNoSemanticConflict(next, key);
        PresentationDraft previous = drafts.put(key, next);
        try {
            save();
        } catch (RuntimeException error) {
            if (previous == null) drafts.remove(key);
            else drafts.put(key, previous);
            throw error;
        }
    }

    /** Removes a draft only when its complete stored target identity matches the supplied draft. */
    public synchronized boolean remove(PresentationDraft target) {
        ensureLoaded();
        requireWritable();
        PresentationDraft validated = validatedCopy(target);
        DraftKey key = DraftKey.from(validated);
        PresentationDraft current = drafts.get(key);
        if (current == null || !sameTarget(current, validated)) {
            return false;
        }
        drafts.remove(key);
        try {
            save();
        } catch (RuntimeException error) {
            drafts.put(key, current);
            throw error;
        }
        return true;
    }

    /** Writes a standalone, reviewable JSON snapshot without changing the live draft document. */
    public synchronized Path exportSnapshot() {
        ensureLoaded();
        Path exports = directory().resolve("exports");
        Path output = exports.resolve("utd-presentation-drafts-" + FILE_STAMP.format(Instant.now()) + ".json");
        try {
            Files.createDirectories(exports);
            writeJsonAtomic(output, documentSnapshot());
            exportError = "";
            return output;
        } catch (Exception error) {
            exportError = "presentation_export_failed: " + safeMessage(error);
            throw new IllegalStateException("Unable to export UTD presentation drafts", error);
        }
    }

    /** Re-reads the document even when its modification time has not changed. */
    public synchronized void forceReload() {
        if (!loaded) {
            loaded = true;
        }
        reload(true);
    }

    public synchronized boolean isWritable() {
        ensureLoaded();
        return writable;
    }

    /** Last successfully loaded/saved revision, or -1 when absence was successfully observed. */
    public synchronized long revision() {
        ensureLoaded();
        return lastSuccessfulRevision;
    }

    public synchronized String lastError() {
        ensureLoaded();
        return String.join(" | ", java.util.stream.Stream.of(loadError, saveError, exportError)
                .filter(value -> value != null && !value.isBlank())
                .toList());
    }

    public Path directory() {
        return directorySupplier.get();
    }

    private void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            reload(true);
            return;
        }
        // The desktop workbench may also replace this file atomically. Observe it before every query or mutation so
        // a malformed external update can never be overwritten by a later in-game save.
        reload(false);
    }

    private void reload(boolean forced) {
        Path path = directory().resolve(FILE_NAME);
        try {
            Files.createDirectories(directory());
            long modified = Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1L;
            if (!forced && modified == lastObservedRevision) return;
            lastObservedRevision = modified;
            if (modified < 0L) {
                drafts.clear();
                lastSuccessfulRevision = -1L;
                writable = true;
                loadError = "";
                return;
            }

            Map<DraftKey, PresentationDraft> loadedDrafts = new LinkedHashMap<>();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                PresentationDraftDocument document = GSON.fromJson(reader, PresentationDraftDocument.class);
                validateDocument(document);
                for (PresentationDraft draft : document.drafts) {
                    PresentationDraft validated = validatedCopy(draft);
                    DraftKey key = DraftKey.from(validated);
                    if (loadedDrafts.putIfAbsent(key, validated) != null) {
                        throw new IllegalStateException("duplicate presentation target: " + key.describe());
                    }
                    requireNoSemanticConflict(loadedDrafts, validated, key);
                }
            }
            drafts.clear();
            drafts.putAll(loadedDrafts);
            lastSuccessfulRevision = modified;
            writable = true;
            loadError = "";
            saveError = "";
        } catch (Exception error) {
            writable = false;
            loadError = "presentation_load_failed_read_only_using_last_known_good: " + safeMessage(error);
        }
    }

    private void save() {
        requireWritable();
        Path target = directory().resolve(FILE_NAME);
        try {
            Files.createDirectories(directory());
            if (Files.exists(target)) {
                Files.copy(target, target.resolveSibling(FILE_NAME + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            writeJsonAtomic(target, documentSnapshot());
            long modified = Files.getLastModifiedTime(target).toMillis();
            lastObservedRevision = modified;
            lastSuccessfulRevision = modified;
            saveError = "";
        } catch (Exception error) {
            saveError = "presentation_save_failed: " + safeMessage(error);
            throw new IllegalStateException("Unable to save UTD presentation drafts", error);
        }
    }

    private PresentationDraftDocument documentSnapshot() {
        PresentationDraftDocument document = new PresentationDraftDocument();
        document.updatedAt = Instant.now().toString();
        document.drafts = allWithoutLoading();
        return document;
    }

    private List<PresentationDraft> allWithoutLoading() {
        return drafts.values().stream()
                .map(PresentationDraft::copy)
                .sorted(Comparator.comparing((PresentationDraft draft) -> draft.registryId)
                        .thenComparing(draft -> draft.variantDiscriminator)
                        .thenComparing(draft -> draft.applyScope.name())
                        .thenComparing(draft -> draft.assetKey))
                .toList();
    }

    private void requireWritable() {
        if (!writable) {
            throw new IllegalStateException(
                    "物品名称/介绍草稿文件读取失败；当前使用最后一次可用内容并进入只读保护。请先修复或移走 "
                            + FILE_NAME + "。");
        }
    }

    private static PresentationDraft validatedCopy(PresentationDraft source) {
        if (source == null) throw new IllegalArgumentException("presentation draft is required");
        PresentationDraft draft = source.copy();
        requireString(draft.assetKey, "asset_key", true);
        requireString(draft.registryId, "registry_id", true);
        requireString(draft.variantDiscriminator, "variant_discriminator", false);
        requireString(draft.observedNameZhCn, "observed_name_zh_cn", false);
        requireString(draft.nameZhCn, "name_zh_cn", false);
        requireString(draft.baseCatalogHash, "base_catalog_hash", false);
        requireString(draft.updatedAt, "updated_at", false);
        if (draft.applyScope == null) {
            throw new IllegalArgumentException("apply_scope must be registry or identity");
        }
        if (draft.descriptionZhCn == null) {
            throw new IllegalArgumentException("description_zh_cn must be a string list");
        }
        for (String line : draft.descriptionZhCn) {
            if (line == null) throw new IllegalArgumentException("description_zh_cn must not contain null");
        }
        return draft;
    }

    private static void validateDocument(PresentationDraftDocument document) {
        if (document == null || document.drafts == null) {
            throw new IllegalStateException("presentation draft document is empty or has no drafts array");
        }
        if (!PresentationDraftDocument.SCHEMA.equals(document.schemaVersion)) {
            throw new IllegalStateException("unsupported presentation draft schema: " + document.schemaVersion);
        }
    }

    private static void requireString(String value, String field, boolean nonBlank) {
        if (value == null || (nonBlank && value.isBlank())) {
            throw new IllegalArgumentException(field + (nonBlank ? " must be non-blank" : " must be a string"));
        }
    }

    private static boolean sameTarget(PresentationDraft left, PresentationDraft right) {
        return left.applyScope == right.applyScope
                && Objects.equals(left.assetKey, right.assetKey)
                && Objects.equals(left.registryId, right.registryId)
                && Objects.equals(left.variantDiscriminator, right.variantDiscriminator);
    }

    private PresentationDraft findSemanticIdentityInternal(String registryId, String discriminator) {
        PresentationDraft found = null;
        for (PresentationDraft candidate : drafts.values()) {
            if (candidate.applyScope != PresentationApplyScope.IDENTITY
                    || !Objects.equals(candidate.registryId, registryId)
                    || !Objects.equals(candidate.variantDiscriminator, discriminator)) {
                continue;
            }
            if (found != null) {
                // Loading and upsert reject this state. Keep the query defensive in case future storage code changes.
                throw new IllegalStateException(
                        "ambiguous presentation identity: " + registryId + " / " + discriminator);
            }
            found = candidate;
        }
        return found;
    }

    private void requireNoSemanticConflict(PresentationDraft candidate, DraftKey candidateKey) {
        requireNoSemanticConflict(drafts, candidate, candidateKey);
    }

    private static void requireNoSemanticConflict(
            Map<DraftKey, PresentationDraft> existingDrafts, PresentationDraft candidate, DraftKey candidateKey) {
        for (Map.Entry<DraftKey, PresentationDraft> entry : existingDrafts.entrySet()) {
            if (entry.getKey().equals(candidateKey)) continue;
            PresentationDraft existing = entry.getValue();
            if (candidate.applyScope != existing.applyScope) continue;

            boolean conflicts = candidate.applyScope == PresentationApplyScope.REGISTRY
                    ? Objects.equals(candidate.registryId, existing.registryId)
                    : Objects.equals(candidate.registryId, existing.registryId)
                            && Objects.equals(candidate.variantDiscriminator, existing.variantDiscriminator);
            if (conflicts) {
                throw semanticConflict(existing, candidate);
            }
        }
    }

    private static IllegalArgumentException semanticConflict(PresentationDraft existing, PresentationDraft candidate) {
        return new IllegalArgumentException(
                "ambiguous presentation target for " + candidate.registryId + " / "
                        + candidate.variantDiscriminator + ": asset_key " + existing.assetKey + " vs "
                        + candidate.assetKey);
    }

    private static String safeMessage(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static void writeJsonAtomic(Path target, Object value) throws Exception {
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                GSON.toJson(value, writer);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private record DraftKey(PresentationApplyScope scope, String assetKey, String registryId, String discriminator) {
        static DraftKey from(PresentationDraft draft) {
            return draft.applyScope == PresentationApplyScope.REGISTRY
                    ? registry(draft.registryId)
                    : identity(draft.assetKey, draft.registryId, draft.variantDiscriminator);
        }

        static DraftKey registry(String registryId) {
            return new DraftKey(PresentationApplyScope.REGISTRY, "", registryId, "");
        }

        static DraftKey identity(String assetKey, String registryId, String discriminator) {
            return new DraftKey(PresentationApplyScope.IDENTITY, assetKey, registryId, discriminator);
        }

        String describe() {
            return scope == PresentationApplyScope.REGISTRY
                    ? "registry:" + registryId
                    : "identity:" + assetKey + ":" + registryId + ":" + discriminator;
        }
    }
}
