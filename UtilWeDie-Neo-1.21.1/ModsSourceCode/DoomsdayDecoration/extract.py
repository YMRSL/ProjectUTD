# -*- coding: utf-8 -*-
"""
Doomsday Decoration -> NeoForge 1.21.1 data-driven extractor.
Parses MCreator decompiled init + block classes into a manifest JSON.
"""
import os, re, json, sys

DEC = r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\decompiled\doomsday_decoration"
INIT = os.path.join(DEC, "net", "mcreator", "doomsdaydecoration", "init")
BLOCKDIR = os.path.join(DEC, "net", "mcreator", "doomsdaydecoration", "block")
OUT = os.path.join(r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration", "manifest.json")
RES_OUT = r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration\neoforge-1.21.1\src\main\resources\doomsday_decoration_manifest.json"

def read(p):
    with open(p, "r", encoding="utf-8", errors="replace") as f:
        return f.read()

# --- 1. registry name -> ClassName ---
blocks_src = read(os.path.join(INIT, "DoomsdayDecorationModBlocks.java"))
reg_re = re.compile(r'REGISTRY\.register\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+(\w+)\(\)')
reg = {}   # registry_name -> ClassName
order = []
for m in reg_re.finditer(blocks_src):
    reg[m.group(1)] = m.group(2)
    order.append(m.group(1))

# Also map ClassName -> registry_name and the JAVA const name used in registry
const_re = re.compile(r'RegistryObject<Block>\s+(\w+)\s*=\s*REGISTRY\.register\(\s*"([^"]+)"')
const_to_name = {}
for m in const_re.finditer(blocks_src):
    const_to_name[m.group(1)] = m.group(2)

# --- SoundType SRG -> vanilla constant name (only the ones we see) ---
SOUND_SRG = {
    "f_56736_": "WOOD", "f_56742_": "STONE", "f_56743_": "METAL", "f_56762_": "WOOL",
    "f_56745_": "GLASS", "f_56751_": "GRAVEL", "f_56739_": "GRASS", "f_56752_": "SAND",
    "f_56744_": "SNOW", "f_56756_": "SLIME_BLOCK", "f_154654_": "COPPER", "f_56757_": "LADDER",
    "f_56740_": "GRAVEL", "f_56758_": "ANVIL", "f_56750_": "STONE", "f_56738_": "DIRT",
}
NOTE_SRG = {
    "BASS": "BASS", "BASEDRUM": "BASEDRUM", "SNARE": "SNARE", "HAT": "HAT",
    "HARP": "HARP", "GUITAR": "GUITAR", "BELL": "BELL", "FLUTE": "FLUTE",
    "BANJO": "BANJO", "PLING": "PLING", "IRON_XYLOPHONE": "IRON_XYLOPHONE",
}

# extends-type -> our normalized "type"
def classify(src, classname):
    m = re.search(r'public class\s+\w+\s+extends\s+(\w+)', src)
    ext = m.group(1) if m else "Block"
    impls_m = re.search(r'public class\s+\w+\s+extends\s+\w+\s+implements\s+([\w, ]+)', src)
    impls = impls_m.group(1) if impls_m else ""
    t = "block"
    if ext == "StairBlock": t = "stair"
    elif ext == "SlabBlock": t = "slab"
    elif ext == "WallBlock": t = "wall"
    elif ext == "FenceBlock": t = "fence"
    elif ext == "FenceGateBlock": t = "fence_gate"
    elif ext == "DoorBlock": t = "door"
    elif ext == "TrapDoorBlock": t = "trapdoor"
    elif ext == "PressurePlateBlock": t = "pressure_plate"
    elif ext == "ButtonBlock": t = "button"
    elif ext in ("IronBarsBlock",): t = "pane"
    elif ext == "CarpetBlock": t = "carpet"
    elif ext == "Block": t = "block"
    else: t = "other:" + ext
    return ext, impls, t

def get_sound(src):
    m = re.search(r'm_60918_\(SoundType\.(\w+)\)', src)
    if m:
        return SOUND_SRG.get(m.group(1), None)
    return None

def get_strength(src):
    m = re.search(r'm_60913_\(([\d.]+)F?\s*,\s*([\d.]+)F?\)', src)
    if m:
        return float(m.group(1)), float(m.group(2))
    return None

def get_instrument(src):
    m = re.search(r'm_280658_\(NoteBlockInstrument\.(\w+)\)', src)
    if m:
        return NOTE_SRG.get(m.group(1), m.group(1))
    return None

def get_light(src):
    # m_60953_(s -> N) light level
    m = re.search(r'm_60953_\([^)]*?->\s*(\d+)\)', src)
    if m:
        return int(m.group(1))
    return None

manifest = {"modid": "doomsday_decoration", "blocks": [], "tabs": []}
type_dist = {}
ext_dist = {}

for name in order:
    cls = reg[name]
    path = os.path.join(BLOCKDIR, cls + ".java")
    if not os.path.exists(path):
        manifest["blocks"].append({"name": name, "class": cls, "type": "block",
                                   "missing_class": True})
        type_dist["block"] = type_dist.get("block", 0) + 1
        continue
    src = read(path)
    ext, impls, t = classify(src, cls)
    facing = "FACING" in src and ("HorizontalDirectionalBlock.f_54117_" in src or "DirectionProperty FACING" in src)
    waterlogged = "WATERLOGGED" in src
    no_occ = "m_60955_(" in src       # noOcclusion
    light = get_light(src)
    sound = get_sound(src)
    strength = get_strength(src)
    instr = get_instrument(src)
    has_be = "EntityBlock" in impls
    b = {
        "name": name, "class": cls, "type": t,
        "facing": facing, "waterlogged": waterlogged, "noOcclusion": no_occ,
    }
    if light is not None: b["light"] = light
    if sound: b["sound"] = sound
    if strength: b["strength"] = strength
    if instr: b["instrument"] = instr
    if has_be: b["blockEntity"] = True
    manifest["blocks"].append(b)
    type_dist[t] = type_dist.get(t, 0) + 1
    ext_dist[ext] = ext_dist.get(ext, 0) + 1

# --- tabs ---
tabs_src = read(os.path.join(INIT, "DoomsdayDecorationModTabs.java"))
# split per register block
tab_blocks = re.split(r'REGISTRY\.register\(', tabs_src)
for chunk in tab_blocks[1:]:
    nm = re.search(r'^\s*"([^"]+)"', chunk)
    if not nm: continue
    tabname = nm.group(1)
    icon_m = re.search(r'new ItemStack\(\(ItemLike\)DoomsdayDecorationModBlocks\.(\w+)\.get', chunk)
    icon = const_to_name.get(icon_m.group(1)) if icon_m else None
    # collect ordered block consts added
    adds = re.findall(r'DoomsdayDecorationModBlocks\.(\w+)\.get\(\)\)\.m_5456_', chunk)
    add_names = [const_to_name[a] for a in adds if a in const_to_name]
    manifest["tabs"].append({"name": tabname, "icon": icon, "blocks": add_names})

with open(OUT, "w", encoding="utf-8") as f:
    json.dump(manifest, f, indent=1)
os.makedirs(os.path.dirname(RES_OUT), exist_ok=True)
with open(RES_OUT, "w", encoding="utf-8") as f:
    json.dump(manifest, f, separators=(",", ":"))

# stats
print("TOTAL blocks in registry:", len(order))
print("Blocks parsed:", len(manifest["blocks"]))
print("Missing class files:", sum(1 for b in manifest["blocks"] if b.get("missing_class")))
print("\nTYPE DISTRIBUTION:")
for k, v in sorted(type_dist.items(), key=lambda x: -x[1]):
    print(f"  {k:24} {v}")
print("\nEXT (raw superclass) DISTRIBUTION:")
for k, v in sorted(ext_dist.items(), key=lambda x: -x[1]):
    print(f"  {k:30} {v}")
print("\nFACING:", sum(1 for b in manifest['blocks'] if b.get('facing')))
print("WATERLOGGED:", sum(1 for b in manifest['blocks'] if b.get('waterlogged')))
print("BlockEntity:", sum(1 for b in manifest['blocks'] if b.get('blockEntity')))
print("with light:", sum(1 for b in manifest['blocks'] if 'light' in b))
print("no sound parsed:", sum(1 for b in manifest['blocks'] if 'sound' not in b))
print("\nTABS:")
for t in manifest["tabs"]:
    print(f"  {t['name']:40} icon={t['icon']} blocks={len(t['blocks'])}")
print("\nmanifest ->", OUT)
