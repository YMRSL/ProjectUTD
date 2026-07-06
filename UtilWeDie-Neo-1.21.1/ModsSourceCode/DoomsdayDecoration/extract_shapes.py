# -*- coding: utf-8 -*-
"""
Doomsday Decoration shape extractor.

Reads the decompiled 1.20.1 MCreator block classes and recovers each block's
hand-written VoxelShape (the getShape override `m_5940_`, which the data-driven
1.21.1 port dropped — PORT_REPORT.md §7 gap 1). Emits a compact JSON keyed by
registry name, packed into the jar as `doomsday_decoration_shapes.json` and
loaded by DecoShapeStore at registration.

A getShape body is always a union of axis-aligned boxes (`Block.box` == m_49796_,
optionally wrapped in `Shapes.or` == m_83110_). So per facing-case we simply
collect every box literal in that case's text span — the union reproduces the
shape exactly, without parsing the or-structure.

Facing blocks: switch on FACING with case NORTH/EAST/WEST/SOUTH (default == the
missing one, conventionally SOUTH). Non-facing blocks: single `return <expr>;`.

Output entry:
  "blackpickuptruck": {
     "facing": true,
     "overhang": true,                 # any box exceeds the [0,16]px cell
     "shapes": { "north": [[...6...]], "east": [...], "south": [...], "west": [...] }
  }
  "smallthing": { "facing": false, "overhang": false, "shapes": {"all": [[...]]} }
  "walkthrough": { "facing": false, "overhang": false, "shapes": {"all": []} }  # empty (no collision)
"""
import os, re, json, math

ROOT = r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode"
DEC = os.path.join(ROOT, "decompiled", "doomsday_decoration",
                   "net", "mcreator", "doomsdaydecoration")
BLOCKDIR = os.path.join(DEC, "block")
INIT = os.path.join(DEC, "init")
RES_OUT = os.path.join(ROOT, "DoomsdayDecoration", "neoforge-1.21.1",
                       "src", "main", "resources", "doomsday_decoration_shapes.json")
HUMAN_OUT = os.path.join(ROOT, "DoomsdayDecoration", "shapes.json")

def read(p):
    with open(p, "r", encoding="utf-8", errors="replace") as f:
        return f.read()

# Hand-added collision boxes (block -> NORTH box in px) for parts the original mod
# left collision-less — e.g. street-lamp heads / broadcaster speakers, so the player
# can climb onto them. Merged into the recovered shape (rotated per facing). The box
# sits at the head's real height (a stand-on platform); the space under stays open.
_ov_path = os.path.join(r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration",
                        "shape_overrides_head.json")
HEAD_OV = json.load(open(_ov_path, encoding="utf-8")) if os.path.exists(_ov_path) else {}

def rot_box(b, facing):
    """Rotate a NORTH box by the blockstate y-rotation (north=0/east=90/south=180/west=270, CW)."""
    x1, y1, z1, x2, y2, z2 = b
    if facing == "north": return [x1, y1, z1, x2, y2, z2]
    if facing == "east":  return [16 - z2, y1, x1, 16 - z1, y2, x2]
    if facing == "south": return [16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1]
    if facing == "west":  return [z1, y1, 16 - x2, z2, y2, 16 - x1]
    return b

# --- registry name -> ClassName (reuse the init mapping) ---
blocks_src = read(os.path.join(INIT, "DoomsdayDecorationModBlocks.java"))
reg_re = re.compile(r'REGISTRY\.register\(\s*"([^"]+)"\s*,\s*\(\)\s*->\s*new\s+(\w+)\(\)')
cls_to_name = {}
for m in reg_re.finditer(blocks_src):
    cls_to_name[m.group(2)] = m.group(1)

BOX_RE = re.compile(
    r'm_49796_\(\s*([\-0-9.]+)\s*,\s*([\-0-9.]+)\s*,\s*([\-0-9.]+)\s*,'
    r'\s*([\-0-9.]+)\s*,\s*([\-0-9.]+)\s*,\s*([\-0-9.]+)\s*\)')
EMPTY_RE = re.compile(r'm_83040_\(\)')            # Shapes.empty()
GETSHAPE_RE = re.compile(r'public\s+VoxelShape\s+m_5940_\s*\([^)]*\)\s*\{')
# case label extraction inside an arrow-switch
CASE_RE = re.compile(r'case\s+([A-Z]+)\s*->', )

def boxes_in(span):
    out = []
    for m in BOX_RE.finditer(span):
        out.append([num(m.group(i)) for i in range(1, 7)])
    return out

def num(s):
    f = float(s)
    return int(f) if f == int(f) else f

def method_body(src, start_brace_idx):
    """Return substring of the balanced { } block beginning at start_brace_idx."""
    depth = 0
    i = start_brace_idx
    while i < len(src):
        c = src[i]
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                return src[start_brace_idx:i + 1]
        i += 1
    return src[start_brace_idx:]

def overhangs(boxlist):
    for b in boxlist:
        if min(b) < 0 or max(b) > 16:
            return True
    return False

def solidify(boxes):
    """Per-cell solid collision for oversized blocks (see task §8 / crawl fix).

    The exact recovered shapes (hollow cabins, roofs narrower than body, thin
    protrusions) create head-only pockets across multi-piece vehicles, which
    forces the player into the crawl pose and lets them squeeze inside. We
    replace an oversized shape with a "solid column per cell" version: for every
    1x1 horizontal cell the shape's bounding footprint touches, emit one box from
    the shape floor up to that cell's tallest point, clipped to the shape's XZ
    bounding box. Result: no vertical pockets (no forced crawl), the full
    footprint is walkable on top, and collision >= the visual envelope — while
    staying within the model's XZ extent (no invisible walls beyond it).
    Non-oversized (sub-cell) decorations are left exact so they stay tight.
    """
    if not boxes:
        return boxes
    floor_y = min(0.0, min(b[1] for b in boxes))
    min_x = min(b[0] for b in boxes); max_x = max(b[3] for b in boxes)
    min_z = min(b[2] for b in boxes); max_z = max(b[5] for b in boxes)
    cell_top = {}   # (cx,cz) -> max y2 (px)
    for b in boxes:
        cx0 = math.floor(b[0] / 16.0); cx1 = math.ceil(b[3] / 16.0) - 1
        cz0 = math.floor(b[2] / 16.0); cz1 = math.ceil(b[5] / 16.0) - 1
        for cx in range(cx0, cx1 + 1):
            for cz in range(cz0, cz1 + 1):
                cell_top[(cx, cz)] = max(cell_top.get((cx, cz), floor_y), b[4])
    out = []
    for (cx, cz), top in sorted(cell_top.items()):
        x1 = max(cx * 16, min_x); x2 = min((cx + 1) * 16, max_x)
        z1 = max(cz * 16, min_z); z2 = min((cz + 1) * 16, max_z)
        if x2 > x1 and z2 > z1 and top > floor_y:
            out.append([norm(x1), norm(floor_y), norm(z1), norm(x2), norm(top), norm(z2)])
    return out

def norm(v):
    return int(v) if float(v) == int(v) else round(float(v), 3)

# default case maps to whichever horizontal dir is not explicitly listed.
ALL_DIRS = ["north", "east", "south", "west"]

result = {}
stats = {"facing": 0, "plain": 0, "empty": 0, "no_shape": 0, "overhang": 0}

for fname in sorted(os.listdir(BLOCKDIR)):
    if not fname.endswith("Block.java"):
        continue
    cls = fname[:-5]
    name = cls_to_name.get(cls)
    if name is None:
        continue
    src = read(os.path.join(BLOCKDIR, fname))
    gm = GETSHAPE_RE.search(src)
    if not gm:
        stats["no_shape"] += 1
        continue
    body = method_body(src, gm.end() - 1)

    if "switch" in body and "FACING" in body:
        # arrow-switch on FACING — split into case spans
        cases = list(CASE_RE.finditer(body))
        shapes = {}
        listed = set()
        for i, cm in enumerate(cases):
            label = cm.group(1).lower()
            seg_start = cm.end()
            seg_end = cases[i + 1].start() if i + 1 < len(cases) else body.rfind("default")
            if seg_end == -1:
                seg_end = len(body)
            seg = body[seg_start:seg_end]
            shapes[label] = boxes_in(seg)
            if label in ALL_DIRS:
                listed.add(label)
        # default span
        dpos = body.rfind("default")
        default_boxes = []
        if dpos != -1:
            default_boxes = boxes_in(body[dpos:])
        missing = [d for d in ALL_DIRS if d not in listed]
        for d in missing:
            shapes[d] = list(default_boxes)
        # keep only the 4 canonical dirs
        shapes = {d: shapes.get(d, list(default_boxes)) for d in ALL_DIRS}
        # merge hand-added head/speaker collision (rotated per facing) so it is climbable
        if name in HEAD_OV:
            for d in ALL_DIRS:
                shapes[d] = shapes[d] + [rot_box(HEAD_OV[name], d)]
        oh = any(overhangs(v) for v in shapes.values())
        entry = {"facing": True, "overhang": oh, "shapes": shapes}
        if oh:
            entry["solid"] = {d: solidify(shapes[d]) for d in ALL_DIRS}
        result[name] = entry
        stats["facing"] += 1
        if oh:
            stats["overhang"] += 1
    else:
        bx = boxes_in(body)
        if not bx and EMPTY_RE.search(body):
            result[name] = {"facing": False, "overhang": False, "shapes": {"all": []}}
            stats["empty"] += 1
        elif not bx:
            # getShape present but no recognizable boxes — skip (fall back to full block)
            stats["no_shape"] += 1
            continue
        else:
            oh = overhangs(bx)
            entry = {"facing": False, "overhang": oh, "shapes": {"all": bx}}
            if oh:
                entry["solid"] = {"all": solidify(bx)}
            result[name] = entry
            stats["plain"] += 1
            if oh:
                stats["overhang"] += 1

with open(RES_OUT, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, separators=(",", ":"))
with open(HUMAN_OUT, "w", encoding="utf-8") as f:
    json.dump(result, f, ensure_ascii=False, indent=1)

print("blocks with shape recovered:", len(result))
print("stats:", stats)
print("resource ->", RES_OUT)
# sanity peek
for k in ("blackpickuptruck", "ambulance1"):
    if k in result:
        print(k, "=>", json.dumps(result[k], ensure_ascii=False)[:200])
