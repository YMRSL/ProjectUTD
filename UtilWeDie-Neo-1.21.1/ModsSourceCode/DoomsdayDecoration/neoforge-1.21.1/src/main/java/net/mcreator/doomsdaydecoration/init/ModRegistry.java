package net.mcreator.doomsdaydecoration.init;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mcreator.doomsdaydecoration.DoomsdayDecoration;
import net.mcreator.doomsdaydecoration.block.DecoBlockFacing;
import net.mcreator.doomsdaydecoration.block.DecoBlockFacingWaterlogged;
import net.mcreator.doomsdaydecoration.block.DecoBlockPlain;
import net.mcreator.doomsdaydecoration.block.DecoBlockWaterlogged;
import net.mcreator.doomsdaydecoration.block.DecoLootBlockFacing;
import net.mcreator.doomsdaydecoration.block.DecoLootBlockFacingWaterlogged;
import net.mcreator.doomsdaydecoration.block.DecoLootBlockPlain;
import net.mcreator.doomsdaydecoration.block.DecoLootBlockWaterlogged;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/**
 * Data-driven registrar. Reads {@code /doomsday_decoration_manifest.json} from the
 * classpath and registers every block / blockitem / creative tab described there,
 * dispatching block construction by "type". Replaces ~1153 hand-written MCreator classes.
 */
public final class ModRegistry {
    private ModRegistry() {}

    private static final String MODID = DoomsdayDecoration.MODID;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /**
     * Invisible collision-only filler placed by oversized decoration blocks to make
     * their whole footprint gap-free solid. No BlockItem / creative tab — placed only
     * programmatically by {@code DecoFillerManager}. Unbreakable + non-selectable so
     * players never interact with it directly.
     */
    public static final DeferredBlock<Block> COLLISION_FILLER = BLOCKS.register("collision_filler",
            () -> new net.mcreator.doomsdaydecoration.block.CollisionFillerBlock(
                    BlockBehaviour.Properties.of()
                            .noOcclusion()
                            .noLootTable()
                            .strength(-1.0F, 3600000.0F)
                            .isSuffocating((s, l, p) -> false)
                            .isViewBlocking((s, l, p) -> false)
                            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    /** registry name -> block holder, for tab building and item registration. */
    private static final Map<String, DeferredBlock<Block>> BLOCK_BY_NAME = new LinkedHashMap<>();
    /** blocks that need a cutout render layer (noOcclusion / facing decorations). */
    private static final List<DeferredBlock<Block>> CUTOUT = new ArrayList<>();
    /**
     * Lootable blocks (blockEntity:true) — must be injected into the
     * {@code DoomsdayBlockEntity} type's validBlocks via
     * {@code BlockEntityTypeAddBlocksEvent}. Without this, NeoForge 1.21.1's
     * {@code BlockEntity} constructor throws IllegalStateException ("Invalid block
     * entity ... state") because the type's validBlocks set would stay empty.
     */
    private static final List<DeferredBlock<Block>> LOOTABLE = new ArrayList<>();

    /** name -> {h,r,sound} material-based hardness overrides (optional resource). */
    private static JsonObject HARDNESS;

    public static void init(IEventBus bus) {
        JsonObject manifest = loadManifest();
        HARDNESS = loadOptionalJson("/doomsday_decoration_hardness.json");
        registerBlocksAndItems(manifest.getAsJsonArray("blocks"));
        registerTabs(manifest.getAsJsonArray("tabs"));
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static JsonObject loadManifest() {
        try (InputStream in = ModRegistry.class.getResourceAsStream("/doomsday_decoration_manifest.json")) {
            if (in == null) throw new IllegalStateException("manifest resource not found");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load doomsday_decoration manifest", e);
        }
    }

    private static JsonObject loadOptionalJson(String path) {
        try (InputStream in = ModRegistry.class.getResourceAsStream(path)) {
            return in == null ? null
                    : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static void registerBlocksAndItems(JsonArray blocks) {
        for (JsonElement el : blocks) {
            JsonObject b = el.getAsJsonObject();
            final String name = b.get("name").getAsString();
            final String type = optStr(b, "type", "block");
            final boolean facing = optBool(b, "facing");
            final boolean waterlogged = optBool(b, "waterlogged");
            final boolean noOcclusion = optBool(b, "noOcclusion");
            // Mount layer: blocks flagged lootable carry a DoomsdayBlockEntity.
            final boolean blockEntity = optBool(b, "blockEntity");
            final int light = b.has("light") ? b.get("light").getAsInt() : 0;
            String sound = optStr(b, "sound", null);
            final String instrument = optStr(b, "instrument", null);
            float destroy = 1.0F, resist = 10.0F;
            if (b.has("strength")) {
                JsonArray s = b.getAsJsonArray("strength");
                destroy = s.get(0).getAsFloat();
                resist = s.get(1).getAsFloat();
            }
            // Material-based hardness/sound (from the semantic categories): steel/metal
            // things break like iron, partitions/soft like wool, tiles like stone, etc.
            // Overrides the original uniform strength so mining feels right per material.
            if (HARDNESS != null && HARDNESS.has(name)) {
                JsonObject h = HARDNESS.getAsJsonObject(name);
                destroy = h.get("h").getAsFloat();
                resist = h.get("r").getAsFloat();
                sound = h.get("sound").getAsString();
            }

            final BlockBehaviour.Properties props = buildProps(sound, instrument, destroy, resist,
                    noOcclusion, light, type);

            Supplier<Block> factory = () -> makeBlock(name, type, props, facing, waterlogged, blockEntity);
            DeferredBlock<Block> holder = BLOCKS.register(name, factory);
            BLOCK_BY_NAME.put(name, holder);

            if (noOcclusion || facing || isThinType(type)) {
                CUTOUT.add(holder);
            }
            // Only "block"-type entries get a BE variant in makeBlock; never stairs/slabs/etc.
            if (blockEntity && "block".equals(type)) {
                LOOTABLE.add(holder);
            }

            // BlockItem (every block has one in the original). DecoBlockItem adds
            // placement lift (below-cell shapes sit on the ground) + obstruction
            // refusal for big multi-cell blocks; plain blocks behave as vanilla.
            ITEMS.register(name, () -> new net.mcreator.doomsdaydecoration.block.DecoBlockItem(
                    holder.get(), new Item.Properties()));
        }
    }

    private static BlockBehaviour.Properties buildProps(String sound, String instrument,
                                                        float destroy, float resist,
                                                        boolean noOcclusion, int light, String type) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of();
        p = p.strength(destroy, resist);
        SoundType st = mapSound(sound);
        if (st != null) p = p.sound(st);
        NoteBlockInstrument ni = mapInstrument(instrument);
        if (ni != null) p = p.instrument(ni);
        if (noOcclusion) p = p.noOcclusion();
        if (light > 0) {
            final int lv = light;
            p = p.lightLevel(s -> lv);
        }
        // doors/trapdoors/iron-bars commonly need non-occluding for rendering
        if (isThinType(type)) p = p.noOcclusion();
        return p;
    }

    private static Block makeBlock(String name, String type, BlockBehaviour.Properties p,
                                   boolean facing, boolean waterlogged, boolean blockEntity) {
        switch (type) {
            case "stair":
                return new StairBlock(Blocks.AIR.defaultBlockState(), p);
            case "slab":
                return new SlabBlock(p);
            case "wall":
                return new WallBlock(p);
            case "fence":
                return new FenceBlock(p);
            case "fence_gate":
                return new FenceGateBlock(WoodType.OAK, p);
            case "door":
                return new DoorBlock(BlockSetType.IRON, p);
            case "trapdoor":
                return new TrapDoorBlock(BlockSetType.IRON, p);
            case "pressure_plate":
                return new PressurePlateBlock(BlockSetType.STONE, p);
            case "button":
                return new ButtonBlock(BlockSetType.STONE, 20, p);
            case "pane":
                return new IronBarsBlock(p);
            case "carpet":
                return new CarpetBlock(p);
            case "block":
            default:
                // Dispatch to a fixed-property subclass per (facing, waterlogged) combo.
                // A single class cannot work here: createBlockStateDefinition runs during
                // super(props), before instance fields are assigned, so it must hardcode
                // its property set rather than read flags.
                // Mount layer: blockEntity:true blocks use the lootable EntityBlock variant
                // (same blockstate, plus a DoomsdayBlockEntity container). The non-lootable
                // path below is byte-identical to the original to avoid disturbing the other
                // ~860 plain decoration blocks.
                // The DecoLoot* variants EXTEND the matching DecoBlock* class, so they
                // inherit the shape overrides + DecoShaped; they just also need their
                // recovered shape registered in the store (the loot layer flags ~242
                // shaped decorations, incl. most vehicles, as lootable blockEntities).
                final Block deco;
                if (blockEntity) {
                    if (facing && waterlogged) deco = new DecoLootBlockFacingWaterlogged(p);
                    else if (facing)           deco = new DecoLootBlockFacing(p);
                    else if (waterlogged)      deco = new DecoLootBlockWaterlogged(p);
                    else                       deco = new DecoLootBlockPlain(p);
                } else {
                    if (facing && waterlogged) deco = new DecoBlockFacingWaterlogged(p);
                    else if (facing)           deco = new DecoBlockFacing(p);
                    else if (waterlogged)      deco = new DecoBlockWaterlogged(p);
                    else                       deco = new DecoBlockPlain(p);
                }
                // Restore the original hand-written VoxelShape (PORT_REPORT §7 gap 1).
                // Attached post-construction because simpleCodec needs the (Properties) ctor.
                net.mcreator.doomsdaydecoration.block.DecoShapeStore.apply(deco, name);
                return deco;
        }
    }

    private static boolean isThinType(String type) {
        switch (type) {
            case "door": case "trapdoor": case "pane": case "fence":
            case "fence_gate": case "wall": case "button": case "pressure_plate":
            case "carpet":
                return true;
            default:
                return false;
        }
    }

    private static void registerTabs(JsonArray tabs) {
        for (JsonElement el : tabs) {
            JsonObject t = el.getAsJsonObject();
            final String tabName = t.get("name").getAsString();
            final String icon = t.has("icon") && !t.get("icon").isJsonNull() ? t.get("icon").getAsString() : null;
            final List<String> contents = new ArrayList<>();
            for (JsonElement c : t.getAsJsonArray("blocks")) contents.add(c.getAsString());

            TABS.register(tabName, () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group." + MODID + "." + tabName))
                    .icon(() -> {
                        DeferredBlock<Block> ic = icon != null ? BLOCK_BY_NAME.get(icon) : null;
                        ItemLike like = ic != null ? ic.get() : firstBlock();
                        return new net.minecraft.world.item.ItemStack(like);
                    })
                    .displayItems((params, output) -> {
                        for (String n : contents) {
                            DeferredBlock<Block> h = BLOCK_BY_NAME.get(n);
                            if (h != null) output.accept(h.get().asItem());
                        }
                    })
                    .build());
        }
    }

    private static ItemLike firstBlock() {
        return BLOCK_BY_NAME.values().iterator().next().get();
    }

    public static void applyCutoutRenderLayers() {
        for (DeferredBlock<Block> h : CUTOUT) {
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    h.get(), net.minecraft.client.renderer.RenderType.cutout());
        }
    }

    /**
     * Injects every lootable block into the {@code DoomsdayBlockEntity} type's
     * validBlocks set. Required: NeoForge 1.21.1 validates the BlockState against
     * {@code BlockEntityType.validBlocks} in the {@code BlockEntity} constructor and
     * in {@code LevelChunk.setBlockEntity}; an empty set makes every placement /
     * chunk-load of a lootable block throw. Called from
     * {@code BlockEntityTypeAddBlocksEvent} (fires after blocks are registered,
     * before any level loads).
     */
    public static void registerLootableBlocks(
            net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
        Block[] blocks = new Block[LOOTABLE.size()];
        for (int i = 0; i < blocks.length; i++) blocks[i] = LOOTABLE.get(i).get();
        if (blocks.length > 0) {
            event.modify(
                    net.mcreator.doomsdaydecoration.functionality.ModFunctionality.DOOMSDAY_BE.get(),
                    blocks);
        }
    }

    // --- helpers ---
    private static String optStr(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }
    private static boolean optBool(JsonObject o, String k) {
        return o.has(k) && o.get(k).getAsBoolean();
    }

    private static SoundType mapSound(String s) {
        if (s == null) return null;
        switch (s) {
            case "WOOD": return SoundType.WOOD;
            case "STONE": return SoundType.STONE;
            case "METAL": return SoundType.METAL;
            case "WOOL": return SoundType.WOOL;
            case "GLASS": return SoundType.GLASS;
            case "GRAVEL": return SoundType.GRAVEL;
            case "GRASS": return SoundType.GRASS;
            case "SAND": return SoundType.SAND;
            case "SNOW": return SoundType.SNOW;
            case "SLIME_BLOCK": return SoundType.SLIME_BLOCK;
            case "COPPER": return SoundType.COPPER;
            case "LADDER": return SoundType.LADDER;
            case "DIRT": return SoundType.GRAVEL;
            case "ANVIL": return SoundType.ANVIL;
            default: return null;
        }
    }

    private static NoteBlockInstrument mapInstrument(String s) {
        if (s == null) return null;
        try {
            return NoteBlockInstrument.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
