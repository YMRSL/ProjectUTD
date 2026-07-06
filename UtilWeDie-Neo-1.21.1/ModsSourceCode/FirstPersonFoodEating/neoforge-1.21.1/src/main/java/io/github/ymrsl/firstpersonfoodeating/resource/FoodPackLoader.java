package io.github.ymrsl.firstpersonfoodeating.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.diagnostic.BootTrace;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.jetbrains.annotations.Nullable;

public enum FoodPackLoader implements RepositorySource {
    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MARKER = MarkerManager.getMarker("FoodPackFinder");
    private static final Gson GSON = new GsonBuilder().create();
    private static final String FOOD_PACKS_DIR_NAME = "FoodsPack";
    private static final String FOOD_PACK_META_FILE = "foodpack.meta.json";
    private static final String DEFAULT_PACK_FILE = "default_food_pack.zip";
    private static final String BUILTIN_DEFAULT_PACK_PATH =
            "/assets/" + FirstPersonFoodEatingMod.MOD_ID + "/packs/" + DEFAULT_PACK_FILE;

    private boolean firstLoad = true;

    @Override
    public void loadPacks(Consumer<Pack> onLoad) {
        BootTrace.event("pack.loader.loadPacks", "enter");
        List<FoodPack> packs = discoverPacks();
        int created = 0;
        for (FoodPack foodPack : packs) {
            Pack pack = createPack(foodPack);
            if (pack != null) {
                onLoad.accept(pack);
                created++;
            }
        }
        BootTrace.event("pack.loader.loadPacks", "packs created=" + created);
    }

    private List<FoodPack> discoverPacks() {
        Path foodsPackPath = FMLPaths.GAMEDIR.get().resolve(FOOD_PACKS_DIR_NAME);
        BootTrace.event("pack.loader.discover.start", "path=" + foodsPackPath);
        try {
            Files.createDirectories(foodsPackPath);
        } catch (IOException ex) {
            LOGGER.warn(MARKER, "Failed to create FoodsPack directory: {}", foodsPackPath, ex);
            BootTrace.error("pack.loader.discover.createDir.failed", ex);
            return List.of();
        }

        if (firstLoad) {
            exportDefaultPackIfChanged(foodsPackPath);
            firstLoad = false;
        }

        LOGGER.info(MARKER, "Scanning food packs from {}", foodsPackPath);
        List<FoodPack> packs = scanFoodPacks(foodsPackPath);
        LOGGER.info(MARKER, "Detected {} valid food pack(s)", packs.size());
        BootTrace.event("pack.loader.discover.scan", "validPacks=" + packs.size());
        return packs;
    }

    private static @Nullable Pack createPack(FoodPack foodPack) {
        String packId = FirstPersonFoodEatingMod.MOD_ID + "_foodpack_"
                + foodPack.path().getFileName().toString().replaceAll("[^a-zA-Z0-9_]", "_");
        PackLocationInfo locationInfo = new PackLocationInfo(
                packId,
                Component.literal(foodPack.name()),
                PackSource.BUILT_IN,
                Optional.empty()
        );
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                return createPackResources(info, foodPack);
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                return createPackResources(info, foodPack);
            }
        };
        PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, false);
        // Read pack metadata (pack_format / description) from the food pack's own pack.mcmeta.
        return Pack.readMetaAndCreate(locationInfo, supplier, PackType.CLIENT_RESOURCES, selectionConfig);
    }

    private static void exportDefaultPackIfChanged(Path foodsPackPath) {
        Path target = foodsPackPath.resolve(DEFAULT_PACK_FILE);
        try (InputStream inputStream = FoodPackLoader.class.getResourceAsStream(BUILTIN_DEFAULT_PACK_PATH)) {
            if (inputStream == null) {
                LOGGER.warn(MARKER, "Built-in default food pack not found at {}", BUILTIN_DEFAULT_PACK_PATH);
                return;
            }
            byte[] builtinBytes = inputStream.readAllBytes();
            if (Files.exists(target)) {
                byte[] existingBytes = Files.readAllBytes(target);
                if (java.util.Arrays.equals(existingBytes, builtinBytes)) {
                    return;
                }
                LOGGER.info(MARKER, "Updating default food pack at {}", target);
            } else {
                LOGGER.info(MARKER, "Exporting default food pack to {}", target);
            }
            Files.write(target, builtinBytes);
        } catch (IOException ex) {
            LOGGER.warn(MARKER, "Failed to export default food pack to {}", target, ex);
            BootTrace.error("pack.loader.exportDefault.failed", ex);
        }
    }

    private static List<FoodPack> scanFoodPacks(Path foodsPackPath) {
        List<FoodPack> packs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(foodsPackPath)) {
            for (Path entry : stream) {
                FoodPack pack = null;
                if (Files.isDirectory(entry)) {
                    pack = fromDirectory(entry);
                } else if (entry.toString().endsWith(".zip")) {
                    pack = fromZip(entry);
                }
                if (pack != null) {
                    LOGGER.info(MARKER, "- {} ({})", pack.path().getFileName(), pack.name());
                    packs.add(pack);
                }
            }
        } catch (IOException ex) {
            LOGGER.warn(MARKER, "Failed to scan FoodsPack directory: {}", foodsPackPath, ex);
            BootTrace.error("pack.loader.scan.failed", ex);
        }

        boolean hasCustomPack = packs.stream().anyMatch(pack ->
                !DEFAULT_PACK_FILE.equalsIgnoreCase(pack.path().getFileName().toString()));
        if (hasCustomPack) {
            packs.removeIf(pack ->
                    DEFAULT_PACK_FILE.equalsIgnoreCase(pack.path().getFileName().toString()));
        }
        return packs;
    }

    private static @Nullable FoodPack fromDirectory(Path path) {
        Path metaPath = path.resolve(FOOD_PACK_META_FILE);
        if (!Files.exists(metaPath)) {
            return null;
        }
        try (InputStream stream = Files.newInputStream(metaPath)) {
            FoodPackMeta meta = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), FoodPackMeta.class);
            if (meta == null || !meta.enabled) {
                return null;
            }
            return new FoodPack(path, displayName(path, meta.name), false);
        } catch (IOException | JsonSyntaxException | JsonIOException ex) {
            LOGGER.warn(MARKER, "Failed to read food pack metadata: {}", metaPath, ex);
            return null;
        }
    }

    private static @Nullable FoodPack fromZip(Path path) {
        try (ZipFile zipFile = new ZipFile(path.toFile())) {
            ZipEntry metaEntry = zipFile.getEntry(FOOD_PACK_META_FILE);
            if (metaEntry == null) {
                return null;
            }
            try (InputStream stream = zipFile.getInputStream(metaEntry)) {
                FoodPackMeta meta = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), FoodPackMeta.class);
                if (meta == null || !meta.enabled) {
                    return null;
                }
                return new FoodPack(path, displayName(path, meta.name), true);
            }
        } catch (IOException | JsonSyntaxException | JsonIOException ex) {
            LOGGER.warn(MARKER, "Failed to parse food pack: {}", path, ex);
            return null;
        }
    }

    private static String displayName(Path path, String nameFromMeta) {
        if (nameFromMeta == null || nameFromMeta.isBlank()) {
            return path.getFileName().toString();
        }
        return nameFromMeta;
    }

    private static PackResources createPackResources(PackLocationInfo info, FoodPack pack) {
        if (!pack.zip()) {
            return new PathPackResources(info, pack.path());
        }
        try {
            URI uri = URI.create("jar:" + pack.path().toUri());
            FileSystem fileSystem;
            boolean ownFileSystem = true;
            try {
                fileSystem = FileSystems.newFileSystem(uri, Map.of());
            } catch (FileSystemAlreadyExistsException alreadyExists) {
                fileSystem = FileSystems.getFileSystem(uri);
                ownFileSystem = false;
            }
            return new ZipPathPackResources(info, fileSystem.getPath("/"), fileSystem, ownFileSystem);
        } catch (Exception ex) {
            LOGGER.warn(MARKER, "Failed to open food pack zip as filesystem: {}", pack.path(), ex);
            // Fall back to an empty pack so the supplier never returns null.
            int packFormat = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
            return new net.neoforged.neoforge.resource.EmptyPackResources(
                    info,
                    new PackMetadataSection(Component.literal("empty"), packFormat));
        }
    }

    private record FoodPack(Path path, String name, boolean zip) {
    }

    private static final class FoodPackMeta {
        private String name = "";
        private boolean enabled = true;
    }

    private static final class ZipPathPackResources extends PathPackResources {
        private final FileSystem zipFs;
        private final boolean ownZipFs;

        private ZipPathPackResources(
                PackLocationInfo info,
                Path root,
                FileSystem zipFs,
                boolean ownZipFs
        ) {
            super(info, root);
            this.zipFs = zipFs;
            this.ownZipFs = ownZipFs;
        }

        @Override
        public void close() {
            super.close();
            if (!ownZipFs || zipFs == null || !zipFs.isOpen()) {
                return;
            }
            try {
                zipFs.close();
            } catch (IOException ex) {
                LOGGER.warn(MARKER, "Failed to close zip filesystem for food pack resources", ex);
            }
        }
    }
}
