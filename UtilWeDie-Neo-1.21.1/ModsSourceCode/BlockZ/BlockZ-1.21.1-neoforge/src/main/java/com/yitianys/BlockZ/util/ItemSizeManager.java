package com.yitianys.BlockZ.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.BlockZDataComponents;
import com.yitianys.BlockZ.init.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.loading.FMLPaths;
import com.google.gson.stream.JsonReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemSizeManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<Item, ItemSize> SIZES = new ConcurrentHashMap<>();
    private static final Map<Item, Integer> CUSTOM_SLOTS = new ConcurrentHashMap<>();
    private static final Map<Item, Integer> CUSTOM_CAP_WIDTH = new ConcurrentHashMap<>();
    private static final Map<Item, Integer> GRID_COLORS = new ConcurrentHashMap<>();
    private static final java.util.List<NbtRule> NBT_RULES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static volatile Boolean syncedGridEnabled = null;

    public record NbtRule(Item item, String nbtKey, String nbtValue, int width, int height) {}

    public static void registerSize(Item item, int width, int height) {
        SIZES.put(item, new ItemSize(width, height));
    }

    public static void registerNbtSize(Item item, String key, String value, int width, int height) {
        NBT_RULES.add(new NbtRule(item, key, value, width, height));
    }

    public static ItemSize getSize(ItemStack stack) {
        if (stack.isEmpty()) return new ItemSize(1, 1);
        if (!isGridEnabled()) return new ItemSize(1, 1);

        ItemSize base = getBaseSize(stack);
        if (isRotated(stack)) {
            return new ItemSize(base.height(), base.width());
        }
        return base;
    }

    private static ItemSize getBaseSize(ItemStack stack) {
        // Check NBT rules first.
        // 1.21: 物品自定义 NBT 走 CUSTOM_DATA 组件（CompoundTag 包装）。规则按任意字符串键匹配。
        if (!NBT_RULES.isEmpty()) {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag tag = customData.copyTag();
                for (NbtRule rule : NBT_RULES) {
                    if (rule.item() == stack.getItem()) {
                        String nbtKey = rule.nbtKey();
                        String nbtValue = rule.nbtValue();
                        if (nbtKey == null || nbtValue == null) {
                            continue;
                        }
                        if (nbtValue.equals(readStringPath(tag, nbtKey))) {
                            return new ItemSize(rule.width(), rule.height());
                        }
                    }
                }
            }
        }
        return SIZES.getOrDefault(stack.getItem(), new ItemSize(1, 1));
    }

    /** Supports both legacy top-level keys and dotted paths inside 1.21 custom-data compounds. */
    static String readStringPath(CompoundTag root, String path) {
        if (root == null || path == null || path.isBlank()) return "";
        String[] segments = path.split("\\.");
        CompoundTag current = root;
        for (int index = 0; index < segments.length - 1; index++) {
            String segment = segments[index];
            if (!current.contains(segment, Tag.TAG_COMPOUND)) return "";
            current = current.getCompound(segment);
        }
        String leaf = segments[segments.length - 1];
        return current.contains(leaf, Tag.TAG_STRING) ? current.getString(leaf) : "";
    }

    public record ItemSize(int width, int height) {}

    public static boolean toggleRotation(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!isGridEnabled()) return false;

        ItemSize base = getBaseSize(stack);
        if (base.width() == base.height()) return false;

        setRotated(stack, !isRotated(stack));
        return true;
    }

    private static boolean isRotated(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(BlockZDataComponents.ROTATED.get()));
    }

    private static void setRotated(ItemStack stack, boolean rotated) {
        if (stack.isEmpty()) return;
        if (rotated) {
            stack.set(BlockZDataComponents.ROTATED.get(), true);
        } else {
            stack.remove(BlockZDataComponents.ROTATED.get());
        }
    }

    public static int getCustomSlots(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        Integer slots = CUSTOM_SLOTS.get(stack.getItem());
        return slots == null ? -1 : slots;
    }

    public static void registerSlots(Item item, int slots) {
        CUSTOM_SLOTS.put(item, slots);
    }

    public static void registerCapacityShape(Item item, int width, int height) {
        if (width <= 0 || height <= 0) return;
        int slots = width * height;
        registerSlots(item, slots);
        CUSTOM_CAP_WIDTH.put(item, width);
    }

    public static void registerGridColor(Item item, int argbColor) {
        GRID_COLORS.put(item, argbColor);
    }

    public static void clearGridColor(Item item) {
        if (item == null) {
            return;
        }
        GRID_COLORS.remove(item);
    }

    public static void updateItemRule(Item item, int width, int height, Integer gridColor) {
        if (item == null || item == Items.AIR || width <= 0 || height <= 0) {
            return;
        }
        registerSize(item, width, height);
        if (gridColor == null) {
            clearGridColor(item);
            return;
        }
        registerGridColor(item, gridColor);
    }

    public static void updateCapacityRule(Item item, int width, int height) {
        if (item == null || item == Items.AIR || width <= 0 || height <= 0) {
            return;
        }
        registerCapacityShape(item, width, height);
    }

    public static int getCapacityCols(ItemStack stack, int defaultCols) {
        if (stack.isEmpty()) return defaultCols;
        Integer w = CUSTOM_CAP_WIDTH.get(stack.getItem());
        if (w != null && w > 0) {
            return w;
        }
        return defaultCols;
    }

    public static boolean isGridEnabled() {
        if (syncedGridEnabled != null) {
            return syncedGridEnabled;
        }
        return BlockZConfigs.isGridEnabled();
    }

    public static Integer getGridColor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return GRID_COLORS.get(stack.getItem());
    }

    public static void loadCustomSizes() {
        // 清理旧数据，准备重新加载
        SIZES.clear();
        CUSTOM_SLOTS.clear();
        CUSTOM_CAP_WIDTH.clear();
        NBT_RULES.clear();
        GRID_COLORS.clear();

        // 1. 注册硬编码的默认值
        registerDefaults();

        // 2. 从 JSON 文件加载自定义值
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("blockz");
        Path filePath = configDir.resolve("grid_items.json");
        ensureConfigFile(configDir, filePath);
        if (!Files.exists(filePath)) return;
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(true); // 允许注释
            JsonObject root = JsonParser.parseReader(jsonReader).getAsJsonObject();

            if (root.has("items")) {
                JsonObject items = root.getAsJsonObject("items");
                for (Map.Entry<String, JsonElement> entry : items.entrySet()) {
                    String itemIdText = entry.getKey();
                    if (itemIdText == null || itemIdText.isBlank()) continue;
                    ResourceLocation id = ResourceLocation.tryParse(itemIdText);
                    if (id == null) continue;
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item == null || item == Items.AIR) continue;
                    if (!entry.getValue().isJsonObject()) continue;
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    if (obj.has("width") && obj.has("height")) {
                        int w = obj.get("width").getAsInt();
                        int h = obj.get("height").getAsInt();
                        if (w > 0 && h > 0) {
                            registerSize(item, w, h);
                        }
                    }
                    int capW = obj.has("cap_width") ? obj.get("cap_width").getAsInt() : -1;
                    int capH = obj.has("cap_height") ? obj.get("cap_height").getAsInt() : -1;
                    if (capW > 0 && capH > 0) {
                        registerCapacityShape(item, capW, capH);
                    }
                    if (obj.has("grid_color")) {
                        Integer color = parseColor(obj.get("grid_color"));
                        if (color != null) {
                            registerGridColor(item, color);
                        }
                    }
                }
            }

            if (root.has("nbt_items")) {
                com.google.gson.JsonArray nbtItems = root.getAsJsonArray("nbt_items");
                for (JsonElement el : nbtItems) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("id") && obj.has("nbt_key") && obj.has("nbt_value") && obj.has("width") && obj.has("height")) {
                        String itemIdText = obj.get("id").getAsString();
                        if (itemIdText == null || itemIdText.isBlank()) continue;
                        ResourceLocation id = ResourceLocation.tryParse(itemIdText);
                        if (id == null) continue;
                        Item item = BuiltInRegistries.ITEM.get(id);
                        if (item == null || item == Items.AIR) continue;

                        String key = obj.get("nbt_key").getAsString();
                        String value = obj.get("nbt_value").getAsString();
                        if (key == null || value == null) continue;
                        int w = obj.get("width").getAsInt();
                        int h = obj.get("height").getAsInt();

                        if (w > 0 && h > 0) {
                            registerNbtSize(item, key, value, w, h);
                        }
                    }
                }
            }
        } catch (Exception e) {
            BlockZ.LOGGER.error("Failed to load custom grid item sizes", e);
        }
    }

    private static void registerDefaults() {
        // --- 衣服装备尺寸 ---
        // 背包作为物品时通常较大
        registerSize(ModItems.BACKPACK_COYOTE.get(), 4, 4);
        registerSize(ModItems.BACKPACK_ALICE.get(), 4, 4);
        registerSize(ModItems.BACKPACK_CZECH.get(), 3, 3);
        registerSize(ModItems.BACKPACK_CZECHPOUCH.get(), 2, 2);
        registerSize(ModItems.BACKPACK_PATROLPACK.get(), 3, 3);

        // --- 容量形状 (cols × rows)：决定装备后内部储物格按几列×几行排成干净矩形。
        //     总格数 = cols × rows，列数压在界面单排上限内。可被 grid_items.json 的 cap_width/cap_height
        //     或游戏内 /blockz_clothing_capacity <列> <行> 覆盖。 ---
        registerCapacityShape(ModItems.BACKPACK_COYOTE.get(), 6, 4);      // 24
        registerCapacityShape(ModItems.BACKPACK_ALICE.get(), 5, 4);       // 20
        registerCapacityShape(ModItems.BACKPACK_CZECH.get(), 4, 4);       // 16
        registerCapacityShape(ModItems.BACKPACK_CZECHPOUCH.get(), 3, 2);  // 6
        registerCapacityShape(ModItems.BACKPACK_PATROLPACK.get(), 4, 2);  // 8
        registerCapacityShape(ModItems.VEST_0.get(), 4, 3);               // 12
        // 上衣/裤子：所有变体统一形状（上衣 3×2=6，裤子 2×2=4）
        for (var shirt : new Item[]{
                ModItems.SHIRT_0.get(), ModItems.SHIRT_1.get(), ModItems.SHIRT_2.get(), ModItems.SHIRT_3.get(),
                ModItems.SHIRT_4.get(), ModItems.SHIRT_5.get(), ModItems.SHIRT_6.get(), ModItems.SHIRT_7.get(),
                ModItems.SHIRT_8.get(), ModItems.SHIRT_9.get(), ModItems.SHIRT_10.get(), ModItems.SHIRT_11.get(),
                ModItems.SHIRT_12.get(), ModItems.SHIRT_13.get(), ModItems.SHIRT_14.get(), ModItems.SHIRT.get()}) {
            registerCapacityShape(shirt, 3, 2);
        }
        for (var pants : new Item[]{
                ModItems.PANTS_0.get(), ModItems.PANTS_1.get(), ModItems.PANTS_2.get(), ModItems.PANTS_3.get(),
                ModItems.PANTS_4.get(), ModItems.PANTS_5.get(), ModItems.PANTS.get()}) {
            registerCapacityShape(pants, 2, 2);
        }

        // 背心和上衣
        registerSize(ModItems.SHIRT_0.get(), 2, 2);
        registerSize(ModItems.SHIRT_1.get(), 2, 2);
        registerSize(ModItems.SHIRT_2.get(), 2, 2);
        registerSize(ModItems.SHIRT_3.get(), 2, 2);
        registerSize(ModItems.SHIRT_4.get(), 2, 2);
        registerSize(ModItems.SHIRT_5.get(), 2, 2);
        registerSize(ModItems.SHIRT_6.get(), 2, 2);
        registerSize(ModItems.SHIRT_7.get(), 2, 2);
        registerSize(ModItems.SHIRT_8.get(), 2, 2);
        registerSize(ModItems.SHIRT_9.get(), 2, 2);
        registerSize(ModItems.SHIRT_10.get(), 2, 2);
        registerSize(ModItems.SHIRT_11.get(), 2, 2);
        registerSize(ModItems.SHIRT_12.get(), 2, 2);
        registerSize(ModItems.SHIRT_13.get(), 2, 2);
        registerSize(ModItems.SHIRT_14.get(), 2, 2);

        // 裤子 (通常 2x2)
        registerSize(ModItems.PANTS_0.get(), 2, 2);
        registerSize(ModItems.PANTS_1.get(), 2, 2);
        registerSize(ModItems.PANTS_2.get(), 2, 2);
        registerSize(ModItems.PANTS_3.get(), 2, 2);
        registerSize(ModItems.PANTS_4.get(), 2, 2);
        registerSize(ModItems.PANTS_5.get(), 2, 2);

        // 鞋子 (通常 2x2)
        registerSize(ModItems.SHOES_0.get(), 2, 2);
        registerSize(ModItems.SHOES_1.get(), 2, 2);
        registerSize(ModItems.SHOES_2.get(), 2, 2);
        registerSize(ModItems.SHOES_3.get(), 2, 2);

        // 手套
        registerSize(ModItems.GLOVES_2.get(), 2, 2);
        // 护理物品 (Bandage/Splint/Rags/Morphine/Codeine) 已整体 DROP，不再注册尺寸。

        // --- 原版物品尺寸 (DayZ 风格) ---
        registerSize(Items.MILK_BUCKET, 2, 2);
        registerSize(Items.WATER_BUCKET, 2, 2);
        registerSize(Items.LAVA_BUCKET, 2, 2);

        // 远程武器
        registerSize(Items.BOW, 3, 2);
        registerSize(Items.CROSSBOW, 3, 2);
        registerSize(Items.TRIDENT, 5, 1);

        // 工具和近战
        registerSize(Items.WOODEN_AXE, 2, 2);
        registerSize(Items.STONE_AXE, 2, 2);
        registerSize(Items.IRON_AXE, 2, 2);
        registerSize(Items.GOLDEN_AXE, 2, 2);
        registerSize(Items.DIAMOND_AXE, 2, 2);
        registerSize(Items.NETHERITE_AXE, 2, 2);

        registerSize(Items.WOODEN_PICKAXE, 3, 2);
        registerSize(Items.STONE_PICKAXE, 3, 2);
        registerSize(Items.IRON_PICKAXE, 3, 2);
        registerSize(Items.GOLDEN_PICKAXE, 3, 2);
        registerSize(Items.DIAMOND_PICKAXE, 3, 2);
        registerSize(Items.NETHERITE_PICKAXE, 3, 2);

        registerSize(Items.WOODEN_SHOVEL, 3, 1);
        registerSize(Items.STONE_SHOVEL, 3, 1);
        registerSize(Items.IRON_SHOVEL, 3, 1);
        registerSize(Items.GOLDEN_SHOVEL, 3, 1);
        registerSize(Items.DIAMOND_SHOVEL, 3, 1);
        registerSize(Items.NETHERITE_SHOVEL, 3, 1);

        registerSize(Items.WOODEN_SWORD, 3, 1);
        registerSize(Items.STONE_SWORD, 3, 1);
        registerSize(Items.IRON_SWORD, 3, 1);
        registerSize(Items.GOLDEN_SWORD, 3, 1);
        registerSize(Items.DIAMOND_SWORD, 3, 1);
        registerSize(Items.NETHERITE_SWORD, 3, 1);

        registerSize(Items.WOODEN_HOE, 3, 2);
        registerSize(Items.STONE_HOE, 3, 2);
        registerSize(Items.IRON_HOE, 3, 2);
        registerSize(Items.GOLDEN_HOE, 3, 2);
        registerSize(Items.DIAMOND_HOE, 3, 2);
        registerSize(Items.NETHERITE_HOE, 3, 2);

        registerSize(Items.SHIELD, 2, 2);
    }

    public static Integer parseColor(JsonElement element) {
        try {
            if (element == null || element.isJsonNull()) return null;
            if (element.isJsonPrimitive()) {
                var prim = element.getAsJsonPrimitive();
                if (prim.isNumber()) {
                    return prim.getAsInt();
                }
                if (prim.isString()) {
                    String raw = prim.getAsString().trim();
                    if (raw.isEmpty()) return null;
                    if (raw.startsWith("#")) {
                        raw = raw.substring(1);
                    } else if (raw.startsWith("0x") || raw.startsWith("0X")) {
                        raw = raw.substring(2);
                    }
                    long value = Long.parseUnsignedLong(raw, 16);
                    if (raw.length() <= 6) {
                        value |= 0xFF000000L;
                    }
                    return (int)value;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Integer parseColorString(String rawColor) {
        if (rawColor == null) {
            return null;
        }
        return parseColor(new com.google.gson.JsonPrimitive(rawColor));
    }

    public static boolean saveItemRule(Item item, int width, int height, Integer gridColor) {
        if (item == null || item == Items.AIR || width <= 0 || height <= 0) {
            return false;
        }
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("blockz");
        Path filePath = configDir.resolve("grid_items.json");
        ensureConfigFile(configDir, filePath);

        try {
            Files.createDirectories(configDir);
            JsonObject root;
            if (Files.exists(filePath)) {
                try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    JsonReader jsonReader = new JsonReader(reader);
                    jsonReader.setLenient(true);
                    JsonElement parsed = JsonParser.parseReader(jsonReader);
                    root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
                }
            } else {
                root = new JsonObject();
            }

            JsonObject itemsObject = root.has("items") && root.get("items").isJsonObject()
                    ? root.getAsJsonObject("items")
                    : new JsonObject();
            root.add("items", itemsObject);

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) {
                return false;
            }

            JsonObject itemObject = itemsObject.has(itemId.toString()) && itemsObject.get(itemId.toString()).isJsonObject()
                    ? itemsObject.getAsJsonObject(itemId.toString())
                    : new JsonObject();
            itemObject.addProperty("width", width);
            itemObject.addProperty("height", height);

            if (gridColor == null) {
                itemObject.remove("grid_color");
            } else {
                itemObject.addProperty("grid_color", String.format("#%08X", gridColor));
            }

            itemsObject.add(itemId.toString(), itemObject);

            Files.writeString(filePath, GSON.toJson(root), StandardCharsets.UTF_8);
            updateItemRule(item, width, height, gridColor);
            return true;
        } catch (Exception e) {
            BlockZ.LOGGER.error("Failed to save grid item rule for {}", BuiltInRegistries.ITEM.getKey(item), e);
            return false;
        }
    }

    public static boolean saveCapacityRule(Item item, int capWidth, int capHeight) {
        if (item == null || item == Items.AIR || capWidth <= 0 || capHeight <= 0) {
            return false;
        }
        Path configDir = FMLPaths.CONFIGDIR.get().resolve("blockz");
        Path filePath = configDir.resolve("grid_items.json");
        ensureConfigFile(configDir, filePath);

        try {
            Files.createDirectories(configDir);
            JsonObject root;
            if (Files.exists(filePath)) {
                try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    JsonReader jsonReader = new JsonReader(reader);
                    jsonReader.setLenient(true);
                    JsonElement parsed = JsonParser.parseReader(jsonReader);
                    root = parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
                }
            } else {
                root = new JsonObject();
            }

            JsonObject itemsObject = root.has("items") && root.get("items").isJsonObject()
                    ? root.getAsJsonObject("items")
                    : new JsonObject();
            root.add("items", itemsObject);

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (itemId == null) {
                return false;
            }

            JsonObject itemObject = itemsObject.has(itemId.toString()) && itemsObject.get(itemId.toString()).isJsonObject()
                    ? itemsObject.getAsJsonObject(itemId.toString())
                    : new JsonObject();
            itemObject.addProperty("cap_width", capWidth);
            itemObject.addProperty("cap_height", capHeight);
            itemsObject.add(itemId.toString(), itemObject);

            Files.writeString(filePath, GSON.toJson(root), StandardCharsets.UTF_8);
            updateCapacityRule(item, capWidth, capHeight);
            return true;
        } catch (Exception e) {
            BlockZ.LOGGER.error("Failed to save capacity rule for {}", BuiltInRegistries.ITEM.getKey(item), e);
            return false;
        }
    }

    private static void ensureConfigFile(Path configDir, Path filePath) {
        if (Files.exists(filePath)) return;
        try {
            Files.createDirectories(configDir);
            try (InputStream in = ItemSizeManager.class.getClassLoader().getResourceAsStream("config/blockz/grid_items.json")) {
                if (in == null) return;
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            BlockZ.LOGGER.error("Failed to create default grid_items.json", e);
        }
    }

    static {
        loadCustomSizes();
    }

    public static void setSyncedGridEnabled(boolean enabled) {
        syncedGridEnabled = enabled;
    }

    public static void clearSyncedClientState() {
        syncedGridEnabled = null;
        loadCustomSizes();
    }

    public static Map<Item, ItemSize> snapshotSizes() {
        return new java.util.HashMap<>(SIZES);
    }

    public static java.util.List<NbtRule> snapshotNbtRules() {
        return new java.util.ArrayList<>(NBT_RULES);
    }

    public static Map<Item, Integer> snapshotCustomSlots() {
        return new java.util.HashMap<>(CUSTOM_SLOTS);
    }

    public static Map<Item, Integer> snapshotCapacityWidths() {
        return new java.util.HashMap<>(CUSTOM_CAP_WIDTH);
    }

    public static Map<Item, Integer> snapshotGridColors() {
        return new java.util.HashMap<>(GRID_COLORS);
    }

    public static void setRules(Map<Item, ItemSize> sizes, java.util.List<NbtRule> nbtRules, Map<Item, Integer> customSlots,
                                Map<Item, Integer> capacityWidths, Map<Item, Integer> gridColors) {
        SIZES.clear();
        if (sizes != null) {
            SIZES.putAll(sizes);
        }
        NBT_RULES.clear();
        if (nbtRules != null) {
            NBT_RULES.addAll(nbtRules);
        }
        CUSTOM_SLOTS.clear();
        if (customSlots != null) {
            CUSTOM_SLOTS.putAll(customSlots);
        }
        CUSTOM_CAP_WIDTH.clear();
        if (capacityWidths != null) {
            CUSTOM_CAP_WIDTH.putAll(capacityWidths);
        }
        GRID_COLORS.clear();
        if (gridColors != null) {
            GRID_COLORS.putAll(gridColors);
        }
    }
}
