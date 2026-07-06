# D:/ModDevelop Inventory

Scanned on 2026-07-06 while importing source into `YMRSL/ProjectUTD`.

`D:/ModDevelop` is a mixed development workspace, not a single clean source tree. It contains active mod source, upstream source snapshots, decompile workspaces, local build output, archives, crash reports, and temporary analysis files. Do not import it as one directory.

## Imported

| Source path | Repository path | Notes |
| --- | --- | --- |
| `D:/ModDevelop/sw_src/` | `SocialWill/sw_src/` | Gradle/Kotlin SuperbWarfare-related source. Copied without `build/`, `.gradle/`, `.settings/`, or logs. |

## Top-Level Scan

| Path | Files | Size | Handling |
| --- | ---: | ---: | --- |
| `ProjectUTD/` | 469,747 | 9067.56 MB | Mixed workspace; inspect subdirectories only. |
| `CreateA-eronuatics-1.20.1/` | 58,107 | 5380.00 MB | Mixed upstream/local ports; hold for separate review. |
| `Unofficial-CNPC-YMRPacked/` | 24,755 | 2305.69 MB | Mixed source/assets; hold for review. |
| `FlashBack-TransforTo1.20.1/` | 1,174 | 1610.57 MB | Contains source plus large artifacts; hold for review. |
| `BBS/` | 40,635 | 1154.67 MB | Multiple upstream/local animation and TACZ projects; split before import. |
| `voxy-1.20.1/` | 1,292 | 577.17 MB | Source plus build/runtime artifacts; hold for review. |
| `Valkyrien-Skies-SpaceWar/` | 3,589 | 423.27 MB | VS upstream/source-release mix; split before import. |
| `_mts_upstream/` | 68,285 | 340.78 MB | Embedded Git/upstream snapshot; do not import directly. |
| `AlignMap/` | 18,879 | 163.49 MB | Needs separate review. |
| `Warium-Chinese/` | 9,646 | 80.05 MB | Needs separate review. |
| `Flashback/` | 402 | 63.51 MB | Existing Git repo; likely better as fork/submodule if needed. |
| `VS_Orbit/` | 1,552 | 45.67 MB | Candidate for later source subset import. |
| `Create-mc1.20.1-dev/` | 11,572 | 31.42 MB | Upstream Create source; do not import unless documenting patch purpose. |
| `sw_src/` | 2,854 | 28.30 MB | Imported to `SocialWill/sw_src/`. |
| `sable-src/` | 812 | 12.36 MB | Candidate for later source subset import. |
| `Wings of freedom/` | 2,832 | 9.02 MB | Candidate for later source subset import. |
| `3DManeuverGear-master/` | 142 | 7.72 MB | Candidate for later source subset import. |
| `MirrorNewAge/` | 1,097 | 6.58 MB | Candidate for later source subset import. |
| `SodiumDynamicLights/` | 153 | 5.86 MB | Existing Git repo; likely better as fork/submodule if needed. |

## D:/ModDevelop/ProjectUTD Subdirectories

| Path | Files | Size | Handling |
| --- | ---: | ---: | --- |
| `AttackableCreateTrain/` | 77,060 | 1843.55 MB | Large mixed compatibility workspace; extract local mod folders only. |
| `CompatibleOfWariumAndTACZAndSuperbWarfare/` | 101,841 | 1598.91 MB | Large mixed compatibility workspace; extract local patch projects only. |
| `FirstPersonFoodEating/` | 37,293 | 1116.23 MB | Large mixed source/assets/runtime area; review before import. |
| `VeichleSit/` | 44,501 | 806.25 MB | Contains `VehicleLoad` and other mixed projects; review before import. |
| `KuaYue/` | 70,879 | 717.29 MB | Large upstream/local mix; review before import. |
| `C2ME/` | 1,288 | 518.04 MB | Upstream performance mod source/build mix; do not import whole. |
| `RiAutomobility/` | 28,692 | 514.97 MB | Source candidate, but includes large assets/build output. |
| `HandHeldMoon/` | 14,461 | 441.56 MB | Source candidate, but includes large assets/build output. |
| `Spore/` | 27,532 | 321.28 MB | Source/dependency mix; review before import. |
| `mc_semantic/` | 5,666 | 251.71 MB | Tool/source candidate; review separately. |
| `Companability of EMF and SPA/` | 908 | 207.53 MB | Upstream compatibility review area. |
| `ItemNameCatch/` | 22,767 | 137.74 MB | Source candidate, but includes generated/output folders. |
| `ModernFix/` | 830 | 128.83 MB | Upstream source/build mix; do not import whole. |
| `Serious-Player-Animations/` | 616 | 103.12 MB | Upstream/source mix; do not import whole. |
| `Hold-My-Items-Neo-master/` | 14,425 | 103.01 MB | Source candidate; review before import. |
| `Homie!/` | 14,327 | 96.24 MB | Source candidate; review before import. |
| `BugRemove/` | 4,901 | 47.60 MB | Mixed bugfix workspace; review before import. |
| `RecipesWebTool/` | 348 | 40.84 MB | Tool source candidate; review separately. |

## Import Rules

- Import source subsets, not whole mixed workspaces.
- Keep `build/`, `bin/`, `out/`, `.gradle/`, `.settings/`, `run/`, `runs/`, crash reports, logs, archives, decompile scratch directories, and downloaded jars out of Git.
- Prefer one commit per logical source area so later cleanup is reversible.
- Treat upstream projects as license-sensitive. Keep this GitHub repository private until licenses and asset redistribution rights are reviewed.
