# ProjectUTD Repository Map

Generated from the current local workspace on 2026-07-06.

## Top-Level Inventory

| Path | Files | Size | Initial handling |
| --- | ---: | ---: | --- |
| `UtilWeDie-Neo-1.21.1/` | 72,752 | 14.00 GB | Review and import source/config subsets only |
| `UntilWeDieOriginal/` | 29,900 | 9.44 GB | Keep out of Git until backup/archive policy is decided |
| `地图管理/` | 362 | 2.49 GB | Keep out of Git unless specific maps move to Git LFS/releases |
| `待删除或整改升级的mod/` | 35 | 0.19 GB | Keep out of Git; treat as triage/download area |
| `Code/` | 5,467 | 0.15 GB | Review local code, exclude upstream/vendor copies where possible |
| `SocialWill/` | mixed | small | Picasso tooling plus copied `sw_src` mod source |
| `Migration-1.21.1/` | 2 | small | Good candidate for normal Git tracking |
| `_inspect_sable_tmp/` | 1 | small | Temporary; ignored |
| `策划案以及文档相关/` | 0 | empty | Keep as docs area if needed |
| `合成表修改/` | 0 | empty | Keep as future source/config area if needed |

## Detected Source Areas

- `SocialWill/Picasso/`: Python package with `pyproject.toml`, docs, and structured data.
- `SocialWill/sw_src/`: Gradle/Kotlin SuperbWarfare-related source copied from `D:/ModDevelop/sw_src/`.
- `Code/ItemsReFresh/`: Java plugin project with Gradle/Maven files.
- `Code/WorldEditCUI-Arch-master/`, `Code/WorldEdit-version-7.2.x/`, `Code/SuperbWarfare-superbwarfare/`: likely upstream/vendor source snapshots; do not mix local patches into them without documenting origin.
- `UtilWeDie-Neo-1.21.1/ModsSourceCode/`: active mod porting workspaces now tracked. Build folders, Gradle caches, decompile scratch areas, archives, and embedded upstream Git snapshots are ignored.
- `UtilWeDie-Neo-1.21.1/npc/`: NPC scripts, KubeJS scripts, resource pack work, and docs. Track scripts/configs; keep generated packs and binary jars out unless intentionally moved to Git LFS.
- `D:/ModDevelop/`: external mixed development workspace. See `docs/moddevelop-inventory.md`; copy source subsets into this repo only after checking scope and ignore rules.

## Recommended Repository Strategy

Start with this root as a lightweight monorepo for coordination docs and actively maintained source. Use separate GitHub repositories later for mature standalone projects:

- `projectutd-picasso` for `SocialWill/Picasso`
- `projectutd-items-refresh` for `Code/ItemsReFresh`
- one repo per NeoForge port that has independent releases

Avoid pushing full client folders, PCL folders, game versions, packaged mods, maps, crash reports, or build outputs to GitHub. Store release zips and client builds in GitHub Releases or external storage; store source in Git.
