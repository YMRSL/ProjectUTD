# One-shot generator for Picasso content data files (E15 / G2 / draft-2 palette table).
# Run once, review output, commit JSON. This script is a build artifact, not runtime code.
import json, os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "picasso", "data")

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
         "mangrove", "cherry", "bamboo", "crimson", "warped"]  # 1.21.1 set — NO pale_oak (1.21.4+)
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
          "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
FLOWERS = ["dandelion", "poppy", "blue_orchid", "allium", "azure_bluet",
           "red_tulip", "orange_tulip", "white_tulip", "pink_tulip", "oxeye_daisy",
           "cornflower", "lily_of_the_valley", "torchflower"]
TALL_FLOWERS = ["sunflower", "lilac", "rose_bush", "peony", "pitcher_plant"]
SAPLINGS = [f"{w}_sapling" for w in WOODS if w not in ("bamboo", "crimson", "warped")] + \
           ["mangrove_propagule"]
SAPLINGS = sorted(set(SAPLINGS) - {"mangrove_sapling"})

def mc(names): return [f"minecraft:{n}" for n in names]

# ---------------- safe_blocks.json (E15: comprehensive 1.21.1 decorative/surface coverage)
replaceable = set()

# Wood families — planks/slab/stairs/fence/fence_gate/log/wood/stripped/leaves/door/trapdoor/sign
for w in WOODS:
    stem = "stem" if w in ("crimson", "warped") else ("block" if w == "bamboo" else "log")
    replaceable |= {f"{w}_planks", f"{w}_slab", f"{w}_stairs", f"{w}_fence", f"{w}_fence_gate",
                    f"{w}_door", f"{w}_trapdoor", f"{w}_pressure_plate", f"{w}_button",
                    f"{w}_sign", f"{w}_wall_sign", f"{w}_hanging_sign", f"{w}_wall_hanging_sign"}
    if w == "bamboo":
        replaceable |= {"bamboo_block", "stripped_bamboo_block", "bamboo_mosaic",
                        "bamboo_mosaic_slab", "bamboo_mosaic_stairs"}
    elif w in ("crimson", "warped"):
        replaceable |= {f"{w}_stem", f"stripped_{w}_stem", f"{w}_hyphae", f"stripped_{w}_hyphae"}
    else:
        replaceable |= {f"{w}_log", f"stripped_{w}_log", f"{w}_wood", f"stripped_{w}_wood",
                        f"{w}_leaves"}
replaceable |= {"azalea_leaves", "flowering_azalea_leaves"}

# Stone & processed-stone families
replaceable |= {
    "stone", "smooth_stone", "smooth_stone_slab", "stone_slab", "stone_stairs", "stone_pressure_plate", "stone_button",
    "cobblestone", "mossy_cobblestone", "cobblestone_slab", "cobblestone_stairs", "cobblestone_wall",
    "mossy_cobblestone_slab", "mossy_cobblestone_stairs", "mossy_cobblestone_wall",
    "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks",
    "stone_brick_slab", "stone_brick_stairs", "stone_brick_wall",
    "mossy_stone_brick_slab", "mossy_stone_brick_stairs", "mossy_stone_brick_wall",
    "andesite", "polished_andesite", "andesite_slab", "andesite_stairs", "andesite_wall",
    "polished_andesite_slab", "polished_andesite_stairs",
    "diorite", "polished_diorite", "diorite_slab", "diorite_stairs", "diorite_wall",
    "polished_diorite_slab", "polished_diorite_stairs",
    "granite", "polished_granite", "granite_slab", "granite_stairs", "granite_wall",
    "polished_granite_slab", "polished_granite_stairs",
    "deepslate", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
    "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles", "chiseled_deepslate",
    "cobbled_deepslate_slab", "cobbled_deepslate_stairs", "cobbled_deepslate_wall",
    "polished_deepslate_slab", "polished_deepslate_stairs", "polished_deepslate_wall",
    "deepslate_brick_slab", "deepslate_brick_stairs", "deepslate_brick_wall",
    "deepslate_tile_slab", "deepslate_tile_stairs", "deepslate_tile_wall",
    "tuff", "polished_tuff", "chiseled_tuff", "tuff_bricks", "chiseled_tuff_bricks",
    "tuff_slab", "tuff_stairs", "tuff_wall", "polished_tuff_slab", "polished_tuff_stairs",
    "polished_tuff_wall", "tuff_brick_slab", "tuff_brick_stairs", "tuff_brick_wall",
    "blackstone", "polished_blackstone", "polished_blackstone_bricks",
    "cracked_polished_blackstone_bricks", "chiseled_polished_blackstone",
    "blackstone_slab", "blackstone_stairs", "blackstone_wall",
    "polished_blackstone_slab", "polished_blackstone_stairs", "polished_blackstone_wall",
    "polished_blackstone_brick_slab", "polished_blackstone_brick_stairs", "polished_blackstone_brick_wall",
    "polished_blackstone_pressure_plate", "polished_blackstone_button",
    "bricks", "brick_slab", "brick_stairs", "brick_wall",
    "mud_bricks", "mud_brick_slab", "mud_brick_stairs", "mud_brick_wall", "packed_mud",
    "sandstone", "smooth_sandstone", "cut_sandstone", "chiseled_sandstone",
    "sandstone_slab", "sandstone_stairs", "sandstone_wall",
    "red_sandstone", "smooth_red_sandstone", "cut_red_sandstone", "chiseled_red_sandstone",
    "red_sandstone_slab", "red_sandstone_stairs", "red_sandstone_wall",
    "quartz_block", "smooth_quartz", "quartz_bricks", "chiseled_quartz_block", "quartz_pillar",
    "quartz_slab", "quartz_stairs", "smooth_quartz_slab", "smooth_quartz_stairs",
    "calcite", "dripstone_block",
}

# Copper family (oxidation = free post-apocalyptic decay vocabulary)
for prefix in ["", "exposed_", "weathered_", "oxidized_"]:
    replaceable |= {f"{prefix}copper" if prefix else "copper_block",
                    f"{prefix}cut_copper", f"{prefix}cut_copper_slab", f"{prefix}cut_copper_stairs",
                    f"{prefix}chiseled_copper", f"{prefix}copper_grate", f"{prefix}copper_bulb",
                    f"{prefix}copper_door", f"{prefix}copper_trapdoor"}
replaceable.discard("copper")  # base is copper_block

# Color families
for c in COLORS:
    replaceable |= {f"{c}_concrete", f"{c}_concrete_powder", f"{c}_terracotta",
                    f"{c}_glazed_terracotta", f"{c}_wool", f"{c}_carpet",
                    f"{c}_stained_glass", f"{c}_stained_glass_pane", f"{c}_bed", f"{c}_candle"}
replaceable |= {"terracotta", "glass", "glass_pane", "tinted_glass", "candle"}

# Ground / terrain surface
replaceable |= {"grass_block", "dirt", "coarse_dirt", "rooted_dirt", "podzol", "mycelium",
                "dirt_path", "farmland", "mud", "clay", "gravel", "sand", "red_sand",
                "snow", "snow_block", "moss_block", "moss_carpet"}

# Vegetation & small decorations
replaceable |= set(FLOWERS) | set(TALL_FLOWERS) | set(SAPLINGS)
replaceable |= {"short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "sweet_berry_bush",
                "brown_mushroom", "red_mushroom", "vine", "glow_lichen", "pink_petals",
                "azalea", "flowering_azalea", "big_dripleaf", "small_dripleaf", "hanging_roots",
                "cobweb", "sugar_cane"}

# Functional decoration / furniture (vanilla furniture-combo vocabulary)
replaceable |= {"torch", "wall_torch", "soul_torch", "soul_wall_torch",
                "lantern", "soul_lantern", "chain", "campfire", "soul_campfire",
                "iron_bars", "iron_door", "iron_trapdoor", "heavy_weighted_pressure_plate",
                "light_weighted_pressure_plate", "ladder", "bell", "lectern", "bookshelf",
                "chiseled_bookshelf", "crafting_table", "cartography_table", "fletching_table",
                "smithing_table", "loom", "stonecutter", "grindstone", "anvil", "chipped_anvil",
                "damaged_anvil", "furnace", "smoker", "blast_furnace", "chest", "trapped_chest",
                "barrel", "composter", "cauldron", "water_cauldron", "flower_pot",
                "decorated_pot", "hay_block", "scaffolding", "lightning_rod"}
replaceable |= {f"potted_{f}" for f in FLOWERS} | {f"potted_{s}" for s in SAPLINGS} | \
               {"potted_fern", "potted_dead_bush", "potted_red_mushroom", "potted_brown_mushroom",
                "potted_azalea_bush", "potted_flowering_azalea_bush", "potted_bamboo",
                "potted_crimson_fungus", "potted_warped_fungus"}

# Banners (classification-transparent wall/pole decoration)
for c in COLORS:
    replaceable |= {f"{c}_banner", f"{c}_wall_banner"}

never_touch = mc([
    "bedrock", "barrier", "structure_block", "structure_void", "jigsaw",
    "command_block", "chain_command_block", "repeating_command_block",
    "spawner", "trial_spawner", "vault",                       # 1.21 trial blocks
    "end_portal", "end_portal_frame", "end_gateway", "nether_portal",
    "reinforced_deepslate", "moving_piston", "piston_head", "light",
])

safe_blocks = {"replaceable": sorted(mc(sorted(replaceable))), "structural_never_touch": never_touch}

# ---------------- block_taxonomy.json (G2: air-like membership + liquid; air category is hardcoded)
air_like = set()
air_like |= {"torch", "wall_torch", "soul_torch", "soul_wall_torch", "redstone_torch",
             "redstone_wall_torch", "lantern", "soul_lantern", "chain",
             "lever", "tripwire", "tripwire_hook", "string", "cobweb", "ladder",
             "rail", "powered_rail", "detector_rail", "activator_rail",
             "snow", "moss_carpet", "pink_petals", "flower_pot", "decorated_pot",
             "sugar_cane", "glow_lichen", "vine", "hanging_roots",
             "small_dripleaf", "big_dripleaf", "sweet_berry_bush",
             "short_grass", "tall_grass", "fern", "large_fern", "dead_bush",
             "brown_mushroom", "red_mushroom", "seagrass", "tall_seagrass", "kelp", "kelp_plant",
             "stone_pressure_plate", "polished_blackstone_pressure_plate",
             "heavy_weighted_pressure_plate", "light_weighted_pressure_plate",
             "stone_button", "polished_blackstone_button"}
air_like |= set(FLOWERS) | set(TALL_FLOWERS) | set(SAPLINGS)
air_like |= {f"potted_{f}" for f in FLOWERS} | {f"potted_{s}" for s in SAPLINGS} | \
            {"potted_fern", "potted_dead_bush", "potted_red_mushroom", "potted_brown_mushroom",
             "potted_azalea_bush", "potted_flowering_azalea_bush", "potted_bamboo",
             "potted_crimson_fungus", "potted_warped_fungus"}
for w in WOODS:
    air_like |= {f"{w}_pressure_plate", f"{w}_button", f"{w}_sign", f"{w}_wall_sign",
                 f"{w}_hanging_sign", f"{w}_wall_hanging_sign"}
for c in COLORS:
    air_like |= {f"{c}_carpet", f"{c}_banner", f"{c}_wall_banner"}

taxonomy = {
    "version": 1,
    "_comment": "ARCHITECTURE.md §4.5 block taxonomy. air category (air/cave_air/void_air) is hardcoded in BlockState.is_air; leaves are SOLID (tree-canopy caveat is deliberate). Mod blocks default to solid unless listed here.",
    "air_like": sorted(mc(sorted(air_like))),
    "liquid": mc(["water", "lava", "bubble_column"]),
}

# ---------------- palette_compatibility.json (brush_room_system §3.2 tier-2 slot filter)
palette = {
    "version": 1,
    "_comment": "Slot-compatibility for palette resolution tier 2 (dominant_materials filter). Glob patterns, reject wins over accept. Falling blocks (sand/gravel/concrete_powder) are rejected for ALL envelope slots — a room shell must not obey gravity (fragment_system physics checklist).",
    "slots": {
        "wall_primary": {
            "accept": ["minecraft:*_planks", "minecraft:*_log", "minecraft:*_wood", "minecraft:stone_bricks",
                       "minecraft:mossy_stone_bricks", "minecraft:bricks", "minecraft:mud_bricks",
                       "minecraft:*_concrete", "minecraft:*_terracotta", "minecraft:terracotta",
                       "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:smooth_stone",
                       "minecraft:stone", "minecraft:andesite", "minecraft:polished_andesite",
                       "minecraft:diorite", "minecraft:polished_diorite", "minecraft:granite",
                       "minecraft:polished_granite", "minecraft:deepslate_bricks", "minecraft:deepslate_tiles",
                       "minecraft:polished_deepslate", "minecraft:cobbled_deepslate", "minecraft:tuff_bricks",
                       "minecraft:polished_tuff", "minecraft:blackstone", "minecraft:polished_blackstone",
                       "minecraft:polished_blackstone_bricks", "minecraft:sandstone", "minecraft:smooth_sandstone",
                       "minecraft:red_sandstone", "minecraft:quartz_block", "minecraft:smooth_quartz",
                       "minecraft:quartz_bricks", "minecraft:*_wool", "minecraft:packed_mud",
                       "minecraft:*cut_copper", "minecraft:copper_block", "minecraft:exposed_copper",
                       "minecraft:weathered_copper", "minecraft:oxidized_copper", "minecraft:calcite"],
            "reject": ["minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:*_concrete_powder",
                       "minecraft:*_glazed_terracotta", "minecraft:glass*", "minecraft:*_pane",
                       "minecraft:*_slab", "minecraft:*_stairs", "minecraft:*_fence*", "minecraft:*_leaves",
                       "minecraft:dirt", "minecraft:grass_block", "minecraft:*_carpet"]
        },
        "floor_main": {
            "accept": ["minecraft:*_planks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                       "minecraft:bricks", "minecraft:mud_bricks", "minecraft:*_concrete",
                       "minecraft:*_terracotta", "minecraft:terracotta", "minecraft:*_glazed_terracotta",
                       "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:smooth_stone",
                       "minecraft:stone", "minecraft:polished_andesite", "minecraft:polished_diorite",
                       "minecraft:polished_granite", "minecraft:deepslate_tiles", "minecraft:polished_deepslate",
                       "minecraft:tuff_bricks", "minecraft:polished_tuff", "minecraft:polished_blackstone",
                       "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:quartz_block",
                       "minecraft:smooth_quartz", "minecraft:packed_mud", "minecraft:dirt",
                       "minecraft:grass_block", "minecraft:*_wool", "minecraft:calcite", "minecraft:mud"],
            "reject": ["minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:*_concrete_powder",
                       "minecraft:glass*", "minecraft:*_pane", "minecraft:*_slab", "minecraft:*_stairs",
                       "minecraft:*_fence*", "minecraft:*_leaves", "minecraft:*_carpet"]
        },
        "ceiling_main": {
            "accept": ["minecraft:*_planks", "minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                       "minecraft:bricks", "minecraft:mud_bricks", "minecraft:*_concrete",
                       "minecraft:*_terracotta", "minecraft:terracotta", "minecraft:smooth_stone",
                       "minecraft:stone", "minecraft:polished_andesite", "minecraft:deepslate_tiles",
                       "minecraft:polished_deepslate", "minecraft:tuff_bricks", "minecraft:polished_tuff",
                       "minecraft:polished_blackstone", "minecraft:sandstone", "minecraft:smooth_sandstone",
                       "minecraft:quartz_block", "minecraft:smooth_quartz", "minecraft:packed_mud",
                       "minecraft:*_wool", "minecraft:calcite"],
            "reject": ["minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:*_concrete_powder",
                       "minecraft:glass*", "minecraft:*_pane", "minecraft:*_slab", "minecraft:*_stairs",
                       "minecraft:*_fence*", "minecraft:*_leaves", "minecraft:dirt", "minecraft:grass_block",
                       "minecraft:*_carpet", "minecraft:*_glazed_terracotta"]
        }
    }
}

# ---------------- write
def dump(name, obj):
    p = os.path.abspath(os.path.join(OUT, name))
    with open(p, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"{name}: written")

dump("safe_blocks.json", safe_blocks)
dump("block_taxonomy.json", taxonomy)
dump("palette_compatibility.json", palette)
print(f"replaceable: {len(safe_blocks['replaceable'])}, never_touch: {len(never_touch)}, "
      f"air_like: {len(taxonomy['air_like'])}")
