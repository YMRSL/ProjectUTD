package com.ymrsl.utdassetmanager.common.blocktransform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Fail-closed file repository. A malformed existing file disables every transform and is never replaced.
 */
public final class BlockTransformRepository {
    private static final Logger LOGGER = Logger.getLogger(BlockTransformRepository.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final BlockTransformRepository INSTANCE = new BlockTransformRepository(
            () -> FMLPaths.CONFIGDIR.get().resolve("utd_asset_manager").resolve(BlockTransformPaths.ACTIVE_FILE_NAME),
            BlockTransformStaging::atomicReplace);

    private final Supplier<Path> pathSupplier;
    private final BlockTransformStaging.AtomicMover atomicMover;
    private final ActiveConfigLoader activeConfigLoader;
    private boolean loaded;
    private long generation;
    private Snapshot snapshot = new Snapshot(0L, List.of(), "not loaded", null);

    private BlockTransformRepository(
            Supplier<Path> pathSupplier,
            BlockTransformStaging.AtomicMover atomicMover) {
        this(pathSupplier, atomicMover, BlockTransformRepository::readConfig);
    }

    private BlockTransformRepository(
            Supplier<Path> pathSupplier,
            BlockTransformStaging.AtomicMover atomicMover,
            ActiveConfigLoader activeConfigLoader) {
        this.pathSupplier = pathSupplier;
        this.atomicMover = atomicMover;
        this.activeConfigLoader = activeConfigLoader;
    }

    BlockTransformRepository(Path path) {
        this(() -> path, BlockTransformStaging::atomicReplace);
    }

    BlockTransformRepository(Path path, BlockTransformStaging.AtomicMover atomicMover) {
        this(() -> path, atomicMover);
    }

    BlockTransformRepository(
            Path path,
            BlockTransformStaging.AtomicMover atomicMover,
            ActiveConfigLoader activeConfigLoader) {
        this(() -> path, atomicMover, activeConfigLoader);
    }

    public static BlockTransformRepository get() {
        return INSTANCE;
    }

    public synchronized Snapshot snapshot() {
        Path path = pathSupplier.get();
        // The active document is loaded once at startup. Merely querying status or handling a
        // right-click must never turn an externally edited file into a new live generation.
        if (!loaded) {
            ensureDefaultExists(path);
            loadActive(path);
        }
        return snapshot;
    }

    /** Explicitly re-reads the activity file into a new generation. */
    public synchronized Snapshot forceReload() {
        Path path = pathSupplier.get();
        ensureDefaultExists(path);
        loadActive(path);
        return snapshot;
    }

    /**
     * Reads and parses a separate validation candidate without changing the active snapshot or generation.
     */
    public synchronized ValidationSnapshot validationSnapshot() {
        Path path = BlockTransformPaths.candidateFor(pathSupplier.get());
        BlockTransformStaging.Candidate candidate = BlockTransformStaging.inspect(path);
        return new ValidationSnapshot(
                candidate.rules(), candidate.error(), candidate.path(), candidate.sha256());
    }

    /**
     * Re-reads, hash-pins and validates the fixed candidate before atomically promoting it.
     * The active in-memory generation changes only after both atomic file replacements succeed.
     */
    public synchronized PromotionAttempt promoteCandidate(
            String expectedSha256,
            BlockTransformStaging.CandidateValidator validator) {
        Path activePath = pathSupplier.get();
        if (!loaded) snapshot();
        BlockTransformStaging.Promotion promotion = BlockTransformStaging.promote(
                activePath,
                BlockTransformPaths.candidateFor(activePath),
                expectedSha256,
                validator,
                atomicMover);
        if (!promotion.promoted()) {
            return new PromotionAttempt(false, promotion.sha256(), promotion.error(), snapshot);
        }
        Snapshot previousSnapshot = snapshot;
        long previousGeneration = generation;
        try {
            BlockTransformConfig config = activeConfigLoader.load(activePath);
            activate(config, activePath);
        } catch (Exception activationError) {
            BlockTransformStaging.Restore restore = BlockTransformStaging.restoreBackup(activePath, atomicMover);
            // The previous snapshot was never overwritten, so a successful file restore gives a true
            // no-op failure: disk, in-memory rules and generation all remain at the pre-promotion state.
            snapshot = previousSnapshot;
            generation = previousGeneration;
            loaded = true;
            String error = "promoted file could not be reloaded: " + activationError.getMessage();
            if (restore.restored()) {
                error += "; active config restored from retained .bak";
            } else {
                error += "; rollback also failed: " + restore.error();
            }
            return new PromotionAttempt(false, promotion.sha256(), error, snapshot);
        }
        return new PromotionAttempt(true, promotion.sha256(), "", snapshot);
    }

    private void ensureDefaultExists(Path path) {
        if (Files.exists(path)) return;
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            String json = GSON.toJson(new DefaultDocument(BlockTransformConfig.SCHEMA, List.of())) + System.lineSeparator();
            Files.writeString(temporary, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnavailable) {
                if (Files.exists(path)) {
                    Files.deleteIfExists(temporary);
                } else {
                    Files.move(temporary, path);
                }
            }
        } catch (IOException error) {
            disable("cannot create default config: " + error.getMessage(), path);
        }
    }

    private void loadActive(Path path) {
        if (!Files.exists(path)) {
            disable("config is missing", path);
            return;
        }
        loaded = true;
        try {
            activate(activeConfigLoader.load(path), path);
        } catch (Exception malformed) {
            disable("invalid config; all transforms disabled: " + malformed.getMessage(), path);
        }
    }

    private void activate(BlockTransformConfig config, Path path) {
        generation++;
        snapshot = new Snapshot(generation, config.rules(), "", path);
        LOGGER.info(() -> "Loaded " + config.rules().size() + " UTD block transform rule(s) from " + path);
    }

    private static BlockTransformConfig readConfig(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return BlockTransformConfigParser.parse(reader);
        }
    }

    private void disable(String error, Path path) {
        generation++;
        loaded = true;
        snapshot = new Snapshot(generation, List.of(), error, path);
        LOGGER.log(Level.SEVERE, "UTD block transforms disabled: {0} ({1})", new Object[]{error, path});
    }

    public record Snapshot(long generation, List<BlockTransformRule> rules, String error, Path path) {
        public Snapshot {
            rules = rules == null ? List.of() : List.copyOf(rules);
            error = error == null ? "" : error;
        }

        public boolean usable() {
            return error.isBlank();
        }
    }

    public record ValidationSnapshot(List<BlockTransformRule> rules, String error, Path path, String sha256) {
        public ValidationSnapshot {
            rules = rules == null ? List.of() : List.copyOf(rules);
            error = error == null ? "" : error;
            sha256 = sha256 == null ? "" : sha256;
        }

        public boolean usable() {
            return error.isBlank();
        }
    }

    public record PromotionAttempt(
            boolean promoted,
            String candidateSha256,
            String error,
            Snapshot active) {
        public PromotionAttempt {
            candidateSha256 = candidateSha256 == null ? "" : candidateSha256;
            error = error == null ? "" : error;
        }
    }

    @FunctionalInterface
    interface ActiveConfigLoader {
        BlockTransformConfig load(Path path) throws Exception;
    }

    private record DefaultDocument(String schema_version, List<Object> rules) {
    }
}
