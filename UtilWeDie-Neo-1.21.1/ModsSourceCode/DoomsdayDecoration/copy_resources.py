# -*- coding: utf-8 -*-
"""Copy + convert DD resources from decompiled tree into NeoForge 1.21.1 project."""
import os, shutil, json

SRC = r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\decompiled\doomsday_decoration"
DST = r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration\neoforge-1.21.1\src\main\resources"

def copytree(s, d):
    if os.path.isdir(s):
        shutil.copytree(s, d, dirs_exist_ok=True)
        return True
    return False

# assets: blockstates, models, textures, lang, sounds -> copy as-is
assets_src = os.path.join(SRC, "assets", "doomsday_decoration")
assets_dst = os.path.join(DST, "assets", "doomsday_decoration")
for sub in ("blockstates", "models", "textures", "lang", "sounds"):
    ok = copytree(os.path.join(assets_src, sub), os.path.join(assets_dst, sub))
    print(f"assets/{sub}: {'copied' if ok else 'MISSING'}")

# data: loot_tables -> loot_table (1.21 singular)
lt_src = os.path.join(SRC, "data", "doomsday_decoration", "loot_tables")
lt_dst = os.path.join(DST, "data", "doomsday_decoration", "loot_table")
if copytree(lt_src, lt_dst):
    print("data/loot_table: copied (renamed from loot_tables)")
# any other data subdirs (tags etc) -> copy with renames
data_src = os.path.join(SRC, "data", "doomsday_decoration")
for sub in os.listdir(data_src):
    if sub == "loot_tables":
        continue
    spath = os.path.join(data_src, sub)
    if not os.path.isdir(spath):
        continue
    target = sub
    if sub == "tags":
        # rename blocks->block, items->item inside tags
        tdst = os.path.join(DST, "data", "doomsday_decoration", "tags")
        for tsub in os.listdir(spath):
            ren = {"blocks": "block", "items": "item"}.get(tsub, tsub)
            copytree(os.path.join(spath, tsub), os.path.join(tdst, ren))
            print(f"data/tags/{tsub} -> tags/{ren}: copied")
    else:
        copytree(spath, os.path.join(DST, "data", "doomsday_decoration", target))
        print(f"data/{sub}: copied")

# pack.mcmeta -> pack_format 34 for 1.21.1
packmeta = {"pack": {"pack_format": 34, "description": "Doomsday Decoration (NeoForge 1.21.1)"}}
with open(os.path.join(DST, "pack.mcmeta"), "w", encoding="utf-8") as f:
    json.dump(packmeta, f, indent=2)
print("pack.mcmeta: written (pack_format=34)")

# report counts
def count(p):
    return sum(len(files) for _, _, files in os.walk(p)) if os.path.isdir(p) else 0
print("\nFINAL COUNTS:")
print("  blockstates:", count(os.path.join(assets_dst, "blockstates")))
print("  models:", count(os.path.join(assets_dst, "models")))
print("  textures:", count(os.path.join(assets_dst, "textures")))
print("  lang:", count(os.path.join(assets_dst, "lang")))
print("  loot_table:", count(lt_dst))
