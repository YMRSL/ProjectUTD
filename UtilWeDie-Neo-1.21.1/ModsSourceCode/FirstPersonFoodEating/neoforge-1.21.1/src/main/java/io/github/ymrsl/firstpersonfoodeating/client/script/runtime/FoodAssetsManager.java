package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.FoodAnimationConstant;
import io.github.ymrsl.firstpersonfoodeating.diagnostic.BootTrace;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationRunner.PlayType;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo.FoodGeoModel;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import io.github.ymrsl.firstpersonfoodeating.client.FoodCreativeTabEvents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

public final class FoodAssetsManager extends SimplePreparableReloadListener<FoodAssetsManager.LoadResult> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FOOD_NAMESPACE = FirstPersonFoodEatingMod.MOD_ID;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .create();
    private static final FileToIdConverter ANIMATION_CONVERTER = new FileToIdConverter("animations", ".animation.json");
    private static final FileToIdConverter GEO_CONVERTER = new FileToIdConverter("geo_models", ".geo.json");
    private static final FileToIdConverter SCRIPT_CONVERTER = new FileToIdConverter("scripts", ".lua");
    private static final FileToIdConverter DISPLAY_CONVERTER = new FileToIdConverter("display/foods", ".json");
    private static final String BUILTIN_PACKS_ROOT = "/assets/" + FirstPersonFoodEatingMod.MOD_ID + "/packs/";
    private static final String BUILTIN_PACK_INFO = "default_food_pack_info.json";
    private static final String PACK_ASSET_PREFIX = "assets/" + FirstPersonFoodEatingMod.MOD_ID + "/";
    private static final FoodAssetsManager INSTANCE = new FoodAssetsManager();

    private final Map<ResourceLocation, BedrockAnimationBank> animationBanks = Maps.newHashMap();
    private final Map<ResourceLocation, FoodGeoModel> geoModels = Maps.newHashMap();
    private final Map<ResourceLocation, FoodDisplayDefinition> displayByItem = Maps.newHashMap();
    private final Map<String, LuaTable> scriptMap = Maps.newHashMap();
    private final Set<ResourceLocation> soundEventIds = new HashSet<>();
    private Globals globals;

    private FoodAssetsManager() {
    }

    public static FoodAssetsManager get() {
        return INSTANCE;
    }

    public boolean reloadNow(ResourceManager resourceManager, String source) {
        if (resourceManager == null) {
            return false;
        }
        try {
            LOGGER.info("[{}] Manual food assets reload started ({})", FirstPersonFoodEatingMod.MOD_ID, source);
            LoadResult prepared = prepare(resourceManager, InactiveProfiler.INSTANCE);
            apply(prepared, resourceManager, InactiveProfiler.INSTANCE);
            LOGGER.info("[{}] Manual food assets reload finished ({})", FirstPersonFoodEatingMod.MOD_ID, source);
            return true;
        } catch (Throwable throwable) {
            LOGGER.error("[{}] Manual food assets reload failed ({})", FirstPersonFoodEatingMod.MOD_ID, source, throwable);
            BootTrace.error("reload.manual.failed", throwable);
            return false;
        }
    }

    @Override
    protected LoadResult prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        long startNs = System.nanoTime();
        LOGGER.info("[{}] Food assets prepare started", FirstPersonFoodEatingMod.MOD_ID);
        BootTrace.markPrepareStarted();
        LoadResult result = new LoadResult();
        this.globals = secureStandardGlobals();
        injectScriptConstants(globals);

        for (Map.Entry<ResourceLocation, Resource> entry : SCRIPT_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            if (!isFoodNamespace(entry.getKey())) {
                continue;
            }
            var wrapped = wrapScriptLoadingFunction(entry.getKey(), entry.getValue());
            result.scriptSuppliers.add(wrapped);
            globals.get("package").get("preload").set(wrapped.getKey(), new LuaFunction() {
                @Override
                public LuaValue call(LuaValue modname, LuaValue env) {
                    LuaTable table = wrapped.getValue().get();
                    return table == null ? LuaValue.NIL : table;
                }
            });
        }

        for (Map.Entry<String, Supplier<LuaTable>> scriptEntry : result.scriptSuppliers) {
            LuaTable table = scriptEntry.getValue().get();
            if (table != null) {
                result.loadedScripts.put(scriptEntry.getKey(), table);
            }
        }

        for (Map.Entry<ResourceLocation, Resource> entry : ANIMATION_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            if (!isFoodNamespace(entry.getKey())) {
                continue;
            }
            ResourceLocation id = ANIMATION_CONVERTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    result.loadedAnimationBanks.put(id, BedrockAnimationBank.fromJson(root));
                }
            } catch (Exception ex) {
                LOGGER.warn("[{}] Failed to read animation file: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            }
        }

        for (Map.Entry<ResourceLocation, Resource> entry : GEO_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            if (!isFoodNamespace(entry.getKey())) {
                continue;
            }
            ResourceLocation id = GEO_CONVERTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    result.loadedGeoModels.put(id, FoodGeoModel.fromJson(root));
                }
            } catch (Exception ex) {
                LOGGER.warn("[{}] Failed to read geo file: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            }
        }

        for (Map.Entry<ResourceLocation, Resource> entry : DISPLAY_CONVERTER.listMatchingResources(resourceManager).entrySet()) {
            if (!isFoodNamespace(entry.getKey())) {
                continue;
            }
            ResourceLocation id = DISPLAY_CONVERTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                FoodDisplayDefinition def = GSON.fromJson(reader, FoodDisplayDefinition.class);
                if (!isDisplayDefinitionValid(id, def, false)) {
                    continue;
                }
                result.loadedDisplayByItem.put(def.getItemId(), def);
            } catch (Exception ex) {
                LOGGER.warn("[{}] Failed to read display file: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            }
        }
        loadKnownSoundEvents(resourceManager, result.loadedSoundEventIds);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        LOGGER.info("[{}] Food assets prepare finished in {} ms (displays={}, animations={}, geos={}, scripts={})",
                FirstPersonFoodEatingMod.MOD_ID,
                elapsedMs,
                result.loadedDisplayByItem.size(),
                result.loadedAnimationBanks.size(),
                result.loadedGeoModels.size(),
                result.loadedScripts.size());
        BootTrace.markPrepareFinished(
                "elapsedMs=" + elapsedMs
                        + ", displays=" + result.loadedDisplayByItem.size()
                        + ", animations=" + result.loadedAnimationBanks.size()
                        + ", geos=" + result.loadedGeoModels.size()
                        + ", scripts=" + result.loadedScripts.size()
        );
        return result;
    }

    @Override
    protected void apply(LoadResult loadResult, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        long startNs = System.nanoTime();
        LOGGER.info("[{}] Food assets apply started", FirstPersonFoodEatingMod.MOD_ID);
        BootTrace.markApplyStarted();
        animationBanks.clear();
        animationBanks.putAll(loadResult.loadedAnimationBanks);
        geoModels.clear();
        geoModels.putAll(loadResult.loadedGeoModels);
        displayByItem.clear();
        displayByItem.putAll(loadResult.loadedDisplayByItem);
        scriptMap.clear();
        scriptMap.putAll(loadResult.loadedScripts);
        soundEventIds.clear();
        soundEventIds.addAll(loadResult.loadedSoundEventIds);
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        LOGGER.info("[{}] Food assets apply finished in {} ms", FirstPersonFoodEatingMod.MOD_ID, elapsedMs);
        LOGGER.info("[{}] Reloaded food assets: displays={}, animations={}, geos={}, scripts={}",
                FirstPersonFoodEatingMod.MOD_ID,
                displayByItem.size(),
                animationBanks.size(),
                geoModels.size(),
                scriptMap.size());
        BootTrace.markApplyFinished(
                "elapsedMs=" + elapsedMs
                        + ", displays=" + displayByItem.size()
                        + ", animations=" + animationBanks.size()
                        + ", geos=" + geoModels.size()
                        + ", scripts=" + scriptMap.size()
        );
        FoodCreativeTabEvents.requestCreativeTabsRebuild();
    }

    public Optional<FoodDisplayDefinition> getDisplay(ResourceLocation itemId) {
        return Optional.ofNullable(displayByItem.get(itemId));
    }

    public boolean hasSoundEvent(ResourceLocation soundId) {
        if (soundId == null) {
            return false;
        }
        return soundEventIds.contains(soundId);
    }

    public Set<ResourceLocation> getSoundEventIdsView() {
        return Collections.unmodifiableSet(soundEventIds);
    }

    public List<ResourceLocation> findSoundEventsByPrefix(String namespace, String pathPrefix) {
        if (namespace == null || namespace.isBlank() || pathPrefix == null || pathPrefix.isBlank()) {
            return List.of();
        }
        List<ResourceLocation> result = new ArrayList<>();
        for (ResourceLocation id : soundEventIds) {
            if (!namespace.equals(id.getNamespace())) {
                continue;
            }
            if (!id.getPath().startsWith(pathPrefix)) {
                continue;
            }
            result.add(id);
        }
        result.sort(Comparator.comparing(ResourceLocation::toString));
        return result;
    }

    public List<Map.Entry<ResourceLocation, FoodDisplayDefinition>> getSortedDisplays() {
        List<Map.Entry<ResourceLocation, FoodDisplayDefinition>> entries = new ArrayList<>(displayByItem.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        return entries;
    }

    public Map<ResourceLocation, FoodDisplayDefinition> getDisplaysView() {
        return Collections.unmodifiableMap(displayByItem);
    }

    public int inferUseDurationTicks(FoodDisplayDefinition display, int fallbackTicks) {
        if (display == null || display.getAnimationId() == null) {
            return Mth.clamp(fallbackTicks, 1, 72_000);
        }
        BedrockAnimationBank bank = animationBanks.get(display.getAnimationId());
        if (bank == null) {
            return Mth.clamp(fallbackTicks, 1, 72_000);
        }
        String itemPath = display.getItemId() == null ? null : display.getItemId().getPath();
        String animBase = display.getAnimationId().getPath();
        float bestLength = 0.0f;
        int bestPriority = Integer.MAX_VALUE;
        for (Map.Entry<String, FoodAnimationClip> entry : bank.clips().entrySet()) {
            FoodAnimationClip clip = entry.getValue();
            if (clip == null) {
                continue;
            }
            String fullClipName = normalizeFullClipName(entry.getKey());
            String shortClipName = extractClipShortName(fullClipName);
            int priority = useClipPriority(fullClipName, shortClipName, itemPath, animBase);
            if (priority >= Integer.MAX_VALUE) {
                continue;
            }
            float length = clip.lengthSeconds();
            if (length <= 0.0001f) {
                continue;
            }
            if (priority < bestPriority || (priority == bestPriority && length > bestLength)) {
                bestPriority = priority;
                bestLength = length;
            }
        }
        if (bestPriority == Integer.MAX_VALUE || bestLength <= 0.0001f) {
            return Mth.clamp(fallbackTicks, 1, 72_000);
        }
        int inferred = Math.max((int) Math.round(bestLength * 20.0f), 1);
        return Mth.clamp(inferred, 1, 72_000);
    }

    public int resolveClipDurationTicks(FoodDisplayDefinition display, String clipName, int fallbackTicks) {
        if (display == null || display.getAnimationId() == null || clipName == null || clipName.isBlank()) {
            return Mth.clamp(fallbackTicks, 1, 72_000);
        }
        BedrockAnimationBank bank = animationBanks.get(display.getAnimationId());
        if (bank == null) {
            return Mth.clamp(fallbackTicks, 1, 72_000);
        }
        String trimmed = clipName.trim();
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);
        if (trimmed.startsWith("animation.")) {
            candidates.add(trimmed.substring("animation.".length()));
        } else {
            candidates.add("animation." + trimmed);
        }
        String animationBase = display.getAnimationId().getPath();
        if (!trimmed.contains(".") && animationBase != null && !animationBase.isBlank()) {
            candidates.add(animationBase + "." + trimmed);
            candidates.add("animation." + animationBase + "." + trimmed);
        }
        FoodAnimationClip matched = null;
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            matched = bank.get(candidate);
            if (matched != null) {
                break;
            }
        }
        if (matched == null) {
            return Mth.clamp(fallbackTicks, 1, 72_000);
        }
        return Mth.clamp(Math.max(Math.round(matched.lengthSeconds() * 20.0f), 1), 1, 72_000);
    }

    public Optional<BedrockAnimationBank> getAnimationBank(ResourceLocation id) {
        return Optional.ofNullable(animationBanks.get(id));
    }

    public Optional<FoodGeoModel> getGeoModel(ResourceLocation id) {
        return Optional.ofNullable(geoModels.get(id));
    }

    public LuaTable getScript(ResourceLocation id) {
        return scriptMap.get(getModuleName(id));
    }

    private Map.Entry<String, Supplier<LuaTable>> wrapScriptLoadingFunction(ResourceLocation raw, Resource resource) {
        ResourceLocation resourceLocation = SCRIPT_CONVERTER.fileToId(raw);
        String moduleName = getModuleName(resourceLocation);
        return new AbstractMap.SimpleEntry<>(moduleName, new Supplier<>() {
            private LuaTable loaded = null;

            @Override
            public LuaTable get() {
                if (loaded != null) {
                    return loaded;
                }
                try (Reader reader = resource.openAsReader()) {
                    String source = readScriptSource(reader);
                    LuaValue chunk = globals.load(stripBom(source), moduleName);
                    loaded = chunk.call().checktable();
                    return loaded;
                } catch (IOException | RuntimeException ex) {
                    LOGGER.warn("[{}] Failed to read script: {}", FirstPersonFoodEatingMod.MOD_ID, resourceLocation, ex);
                    return null;
                }
            }
        });
    }

    private void loadEmbeddedPacks(LoadResult result) {
        EmbeddedPackIndex index;
        try (InputStream inputStream = FoodAssetsManager.class.getResourceAsStream(BUILTIN_PACKS_ROOT + BUILTIN_PACK_INFO)) {
            if (inputStream == null) {
                LOGGER.warn("[{}] Embedded pack info not found: {}{}", FirstPersonFoodEatingMod.MOD_ID, BUILTIN_PACKS_ROOT, BUILTIN_PACK_INFO);
                return;
            }
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                index = GSON.fromJson(reader, EmbeddedPackIndex.class);
            }
        } catch (Exception ex) {
            LOGGER.warn("[{}] Failed to read embedded pack info", FirstPersonFoodEatingMod.MOD_ID, ex);
            return;
        }

        if (index == null || index.packs == null || index.packs.isEmpty()) {
            LOGGER.info("[{}] Embedded pack info loaded but no packs are enabled", FirstPersonFoodEatingMod.MOD_ID);
            return;
        }

        int loadedCount = 0;
        for (EmbeddedPackEntry pack : index.packs) {
            if (pack == null || !pack.enabled || pack.file == null || pack.file.isBlank()) {
                continue;
            }
            if (loadEmbeddedPackZip(result, pack)) {
                loadedCount++;
            }
        }
        LOGGER.info("[{}] Embedded packs loaded: {}", FirstPersonFoodEatingMod.MOD_ID, loadedCount);
    }

    private boolean loadEmbeddedPackZip(LoadResult result, EmbeddedPackEntry pack) {
        String packFile = pack.file;
        String packPath = BUILTIN_PACKS_ROOT + packFile;
        try (InputStream inputStream = FoodAssetsManager.class.getResourceAsStream(packPath)) {
            if (inputStream == null) {
                LOGGER.warn("[{}] Embedded pack zip not found: {}", FirstPersonFoodEatingMod.MOD_ID, packPath);
                return false;
            }
            int animations = 0;
            int geos = 0;
            int displays = 0;
            int scripts = 0;
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String entryName = entry.getName().replace('\\', '/');
                    if (!entryName.startsWith(PACK_ASSET_PREFIX)) {
                        continue;
                    }
                    String relativePath = entryName.substring(PACK_ASSET_PREFIX.length());
                    byte[] data = zipInputStream.readAllBytes();
                    if (relativePath.startsWith("scripts/") && relativePath.endsWith(".lua")) {
                        if (loadEmbeddedScript(result, relativePath, data)) {
                            scripts++;
                        }
                        continue;
                    }
                    if (relativePath.startsWith("animations/") && relativePath.endsWith(".animation.json")) {
                        if (loadEmbeddedAnimation(result, relativePath, data)) {
                            animations++;
                        }
                        continue;
                    }
                    if (relativePath.startsWith("geo_models/") && relativePath.endsWith(".geo.json")) {
                        if (loadEmbeddedGeo(result, relativePath, data)) {
                            geos++;
                        }
                        continue;
                    }
                    if (relativePath.startsWith("display/foods/") && relativePath.endsWith(".json")) {
                        if (loadEmbeddedDisplay(result, relativePath, data)) {
                            displays++;
                        }
                    }
                }
            }
            LOGGER.info("[{}] Embedded pack '{}' loaded: displays={}, animations={}, geos={}, scripts={}",
                    FirstPersonFoodEatingMod.MOD_ID,
                    pack.id == null ? packFile : pack.id,
                    displays,
                    animations,
                    geos,
                    scripts);
            return true;
        } catch (Exception ex) {
            LOGGER.warn("[{}] Failed to load embedded pack zip: {}", FirstPersonFoodEatingMod.MOD_ID, packFile, ex);
            return false;
        }
    }

    private boolean loadEmbeddedScript(LoadResult result, String relativePath, byte[] data) {
        ResourceLocation raw = ResourceLocation.fromNamespaceAndPath(FirstPersonFoodEatingMod.MOD_ID, relativePath);
        ResourceLocation id = SCRIPT_CONVERTER.fileToId(raw);
        String moduleName = getModuleName(id);
        String source = stripBom(new String(data, StandardCharsets.UTF_8));
        try {
            LuaValue chunk = globals.load(source, moduleName);
            LuaTable table = chunk.call().checktable();
            result.loadedScripts.put(moduleName, table);
            globals.get("package").get("preload").set(moduleName, new LuaFunction() {
                @Override
                public LuaValue call(LuaValue modname, LuaValue env) {
                    return table;
                }
            });
            return true;
        } catch (RuntimeException ex) {
            LOGGER.warn("[{}] Failed to compile embedded script: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            return false;
        }
    }

    private boolean loadEmbeddedAnimation(LoadResult result, String relativePath, byte[] data) {
        ResourceLocation raw = ResourceLocation.fromNamespaceAndPath(FirstPersonFoodEatingMod.MOD_ID, relativePath);
        ResourceLocation id = ANIMATION_CONVERTER.fileToId(raw);
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return false;
            }
            result.loadedAnimationBanks.put(id, BedrockAnimationBank.fromJson(root));
            return true;
        } catch (Exception ex) {
            LOGGER.warn("[{}] Failed to read embedded animation file: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            return false;
        }
    }

    private boolean loadEmbeddedGeo(LoadResult result, String relativePath, byte[] data) {
        ResourceLocation raw = ResourceLocation.fromNamespaceAndPath(FirstPersonFoodEatingMod.MOD_ID, relativePath);
        ResourceLocation id = GEO_CONVERTER.fileToId(raw);
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return false;
            }
            result.loadedGeoModels.put(id, FoodGeoModel.fromJson(root));
            return true;
        } catch (Exception ex) {
            LOGGER.warn("[{}] Failed to read embedded geo file: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            return false;
        }
    }

    private boolean loadEmbeddedDisplay(LoadResult result, String relativePath, byte[] data) {
        ResourceLocation raw = ResourceLocation.fromNamespaceAndPath(FirstPersonFoodEatingMod.MOD_ID, relativePath);
        ResourceLocation id = DISPLAY_CONVERTER.fileToId(raw);
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)) {
            FoodDisplayDefinition def = GSON.fromJson(reader, FoodDisplayDefinition.class);
            if (!isDisplayDefinitionValid(id, def, true)) {
                return false;
            }
            result.loadedDisplayByItem.put(def.getItemId(), def);
            return true;
        } catch (Exception ex) {
            LOGGER.warn("[{}] Failed to read embedded display file: {}", FirstPersonFoodEatingMod.MOD_ID, id, ex);
            return false;
        }
    }

    private static String getModuleName(ResourceLocation id) {
        return id.getNamespace() + "_" + id.getPath();
    }

    private static boolean isDisplayDefinitionValid(
            ResourceLocation sourceId,
            FoodDisplayDefinition definition,
            boolean embedded
    ) {
        if (definition == null || definition.getItemId() == null || definition.getAnimationId() == null || definition.getStateMachineId() == null) {
            if (embedded) {
                LOGGER.warn("[{}] Invalid embedded food display: {}", FirstPersonFoodEatingMod.MOD_ID, sourceId);
            } else {
                LOGGER.warn("[{}] Invalid food display: {}", FirstPersonFoodEatingMod.MOD_ID, sourceId);
            }
            return false;
        }
        List<String> warnings = definition.collectSchemaWarnings();
        for (String warning : warnings) {
            LOGGER.warn("[{}] Food display schema warning ({}): {}", FirstPersonFoodEatingMod.MOD_ID, sourceId, warning);
        }
        return true;
    }

    private static String normalizeFullClipName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String normalized = rawName;
        if (normalized.startsWith("animation.")) {
            normalized = normalized.substring("animation.".length());
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String extractClipShortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String normalized = fullName;
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < normalized.length()) {
            normalized = normalized.substring(lastDot + 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static int useClipPriority(
            String fullClipName,
            String shortClipName,
            String itemPath,
            String animationBase
    ) {
        if (fullClipName == null || fullClipName.isBlank()) {
            return Integer.MAX_VALUE;
        }
        if (!isUseClipShortName(shortClipName)) {
            return Integer.MAX_VALUE;
        }

        String itemPrefix = itemPath == null ? null : itemPath.toLowerCase(Locale.ROOT) + ".";
        String animationPrefix = animationBase == null ? null : animationBase.toLowerCase(Locale.ROOT) + ".";

        if (itemPrefix != null && fullClipName.equals(itemPrefix + "use")) {
            return 0;
        }
        if (itemPrefix != null && fullClipName.startsWith(itemPrefix + "use")) {
            return 1;
        }
        if (animationPrefix != null && fullClipName.equals(animationPrefix + "use")) {
            return 2;
        }
        if (animationPrefix != null && fullClipName.startsWith(animationPrefix + "use")) {
            return 3;
        }
        if ("use".equals(shortClipName)) {
            return 4;
        }
        return 5;
    }

    private static boolean isUseClipShortName(String clipName) {
        if (clipName == null || clipName.isBlank()) {
            return false;
        }
        String normalized = clipName.toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("use")) {
            return false;
        }
        return !(normalized.contains("end")
                || normalized.contains("start")
                || normalized.contains("draw")
                || normalized.contains("put")
                || normalized.contains("inspect")
                || normalized.contains("idle"));
    }

    private static boolean isFoodNamespace(ResourceLocation id) {
        return id != null && FOOD_NAMESPACE.equals(id.getNamespace());
    }

    private static void loadKnownSoundEvents(ResourceManager resourceManager, Set<ResourceLocation> out) {
        ResourceLocation soundsJson = ResourceLocation.fromNamespaceAndPath(FOOD_NAMESPACE, "sounds.json");
        for (Resource resource : resourceManager.getResourceStack(soundsJson)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    continue;
                }
                for (Map.Entry<String, com.google.gson.JsonElement> soundEntry : root.entrySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(FOOD_NAMESPACE + ":" + soundEntry.getKey());
                    if (id != null) {
                        out.add(id);
                    }
                }
            } catch (Exception ex) {
                LOGGER.warn("[{}] Failed to parse sounds.json for namespace {}", FirstPersonFoodEatingMod.MOD_ID, FOOD_NAMESPACE, ex);
            }
        }
    }

    private static String readScriptSource(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int len;
        while ((len = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, len);
        }
        return builder.toString();
    }

    private static String stripBom(String scriptSource) {
        if (!scriptSource.isEmpty() && scriptSource.charAt(0) == '\uFEFF') {
            return scriptSource.substring(1);
        }
        return scriptSource;
    }

    private static Globals secureStandardGlobals() {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }

    private static void injectScriptConstants(Globals globals) {
        globals.set("INPUT_DRAW", LuaValue.valueOf(FoodAnimationConstant.INPUT_DRAW));
        globals.set("INPUT_PUT_AWAY", LuaValue.valueOf(FoodAnimationConstant.INPUT_PUT_AWAY));
        globals.set("INPUT_INSPECT", LuaValue.valueOf(FoodAnimationConstant.INPUT_INSPECT));
        globals.set("INPUT_USE", LuaValue.valueOf(FoodAnimationConstant.INPUT_USE));
        globals.set("INPUT_USE_END", LuaValue.valueOf(FoodAnimationConstant.INPUT_USE_END));
        globals.set("INPUT_RUN", LuaValue.valueOf(FoodAnimationConstant.INPUT_RUN));
        globals.set("INPUT_WALK", LuaValue.valueOf(FoodAnimationConstant.INPUT_WALK));
        globals.set("INPUT_IDLE", LuaValue.valueOf(FoodAnimationConstant.INPUT_IDLE));
        globals.set("PLAY_ONCE_HOLD", LuaValue.valueOf(PlayType.PLAY_ONCE_HOLD.ordinal()));
        globals.set("PLAY_ONCE_STOP", LuaValue.valueOf(PlayType.PLAY_ONCE_STOP.ordinal()));
        globals.set("LOOP", LuaValue.valueOf(PlayType.LOOP.ordinal()));
    }

    private static final class EmbeddedPackIndex {
        @SerializedName("format_version")
        private int formatVersion;
        private List<EmbeddedPackEntry> packs = List.of();
    }

    private static final class EmbeddedPackEntry {
        private String id;
        private String name;
        private String file;
        private boolean enabled = true;
    }

    protected static final class LoadResult {
        private final List<Map.Entry<String, Supplier<LuaTable>>> scriptSuppliers = new ArrayList<>();
        private final Map<String, LuaTable> loadedScripts = Maps.newHashMap();
        private final Map<ResourceLocation, BedrockAnimationBank> loadedAnimationBanks = Maps.newHashMap();
        private final Map<ResourceLocation, FoodGeoModel> loadedGeoModels = Maps.newHashMap();
        private final Map<ResourceLocation, FoodDisplayDefinition> loadedDisplayByItem = Maps.newHashMap();
        private final Set<ResourceLocation> loadedSoundEventIds = new HashSet<>();
    }
}
