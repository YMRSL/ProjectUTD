# -*- coding: utf-8 -*-
"""
Build the deterministic base table for the Doomsday Decoration semantic catalog
(task 2). One row per block with everything we can derive without an LLM:
registry id, Chinese display name, creative tab, block type, facing/waterlogged,
footprint (cells) + raw model bounds, light, sound. The LLM enrichment pass adds
category / tags / description / placement context on top of this.

Output: semantic_base.json  (keyed by registry name, stable order = manifest order)
"""
import os, json, math

ROOT = r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration"
PORT = os.path.join(ROOT, "neoforge-1.21.1", "src", "main", "resources")

man = json.load(open(os.path.join(ROOT, "manifest.json"), encoding="utf-8"))
shapes = json.load(open(os.path.join(ROOT, "shapes.json"), encoding="utf-8"))
lang = json.load(open(os.path.join(PORT, "assets", "doomsday_decoration", "lang", "en_us.json"),
                  encoding="utf-8"))

TAB_LABEL = {
    "doomsday_decoration_block": "建材方块",   # building blocks / tiles
    "doomsday_decorationcar":    "载具",        # vehicles
    "doomsday_decoration":       "通用装饰",    # general decoration
}

# block -> tab
b2tab = {}
for t in man["tabs"]:
    for n in t.get("blocks", []):
        b2tab.setdefault(n, t["name"])

def name_zh(reg):
    for key in (f"block.doomsday_decoration.{reg}", f"item.doomsday_decoration.{reg}"):
        if key in lang:
            return lang[key]
    return None

def footprint(reg):
    """Return (cells x,y,z), raw bounds px, overhang) from the recovered shape, or 1×1×1 default."""
    e = shapes.get(reg)
    if not e:
        return [1, 1, 1], None, False
    boxes = []
    if e["facing"]:
        for d in ("north", "east", "south", "west"):
            boxes += e["shapes"].get(d, [])
    else:
        boxes += e["shapes"].get("all", [])
    if not boxes:
        return [1, 1, 1], None, e.get("overhang", False)
    minx = min(b[0] for b in boxes); miny = min(b[1] for b in boxes); minz = min(b[2] for b in boxes)
    maxx = max(b[3] for b in boxes); maxy = max(b[4] for b in boxes); maxz = max(b[5] for b in boxes)
    cx = math.ceil(maxx / 16) - math.floor(minx / 16)
    cy = math.ceil(maxy / 16) - math.floor(miny / 16)
    cz = math.ceil(maxz / 16) - math.floor(minz / 16)
    return [cx, cy, cz], [minx, miny, minz, maxx, maxy, maxz], e.get("overhang", False)

out = {}
missing_zh = 0
for b in man["blocks"]:
    reg = b["name"]
    zh = name_zh(reg)
    if zh is None:
        missing_zh += 1
    fp, bounds, overhang = footprint(reg)
    out[reg] = {
        "id": f"doomsday_decoration:{reg}",
        "name_zh": zh,
        "tab": TAB_LABEL.get(b2tab.get(reg), b2tab.get(reg)),
        "type": b.get("type", "block"),
        "facing": bool(b.get("facing")),
        "waterlogged": bool(b.get("waterlogged")),
        "footprint": fp,                 # [x,y,z] in cells
        "bounds_px": bounds,             # raw model AABB across facings (px), null if full cube
        "oversized": overhang,
        "light": b.get("light", 0),
        "sound": b.get("sound"),
    }

with open(os.path.join(ROOT, "semantic_base.json"), "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=1)

print("rows:", len(out))
print("missing zh name:", missing_zh)
from collections import Counter
print("by tab:", Counter(v["tab"] for v in out.values()))
print("oversized:", sum(1 for v in out.values() if v["oversized"]))
# sample
for k in ("blackpickuptruck", "fridge", "blackandwhiteceramictiles", "acrate", "sandbag_4"):
    if k in out:
        print(" ", k, "->", json.dumps(out[k], ensure_ascii=False))
