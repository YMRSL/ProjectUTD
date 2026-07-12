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
            () -> FMLPaths.CONFIGDIR.get().resolve("utd_asset_manager").resolve("block_transforms.json"));

    private final Supplier<Path> pathSupplier;
    private boolean loaded;
    private long observedModified = Long.MIN_VALUE;
    private long observedSize = Long.MIN_VALUE;
    private long generation;
    private Snapshot snapshot = new Snapshot(0L, List.of(), "not loaded", null);

    private BlockTransformRepository(Supplier<Path> pathSupplier) {
        this.pathSupplier = pathSupplier;
    }

    BlockTransformRepository(Path path) {
        this(() -> path);
    }

    public static BlockTransformRepository get() {
        return INSTANCE;
    }

    public synchronized Snapshot snapshot() {
        Path path = pathSupplier.get();
        ensureDefaultExists(path);
        reloadIfChanged(path);
        return snapshot;
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

    private void reloadIfChanged(Path path) {
        try {
            if (!Files.exists(path)) {
                if (!loaded) disable("config is missing", path);
                return;
            }
            long modified = Files.getLastModifiedTime(path).toMillis();
            long size = Files.size(path);
            if (loaded && modified == observedModified && size == observedSize) return;
            observedModified = modified;
            observedSize = size;
            loaded = true;
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                BlockTransformConfig config = BlockTransformConfigParser.parse(reader);
                generation++;
                snapshot = new Snapshot(generation, config.rules(), "", path);
                LOGGER.info(() -> "Loaded " + config.rules().size() + " UTD block transform rule(s) from " + path);
            } catch (Exception malformed) {
                disable("invalid config; all transforms disabled: " + malformed.getMessage(), path);
            }
        } catch (IOException error) {
            disable("cannot read config; all transforms disabled: " + error.getMessage(), path);
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

    private record DefaultDocument(String schema_version, List<Object> rules) {
    }
}
