# Source Import Plan

This repository is being imported in small batches so that Git history stays useful and large Minecraft runtime files do not leak into source control.

## Imported

- `Migration-1.21.1/`: migration notes.
- `SocialWill/Picasso/`: Python tooling and documentation.
- `Code/ItemsReFresh/`: maintained Java plugin source. Build outputs are ignored.

## Held For Review

- `Code/SuperbWarfare-superbwarfare/`: upstream/source snapshot, about 117 MB.
- `Code/WorldEdit-version-7.2.x/`: upstream/source snapshot.
- `Code/WorldEditCUI-Arch-master/`: upstream/source snapshot.
- `UtilWeDie-Neo-1.21.1/ModsSourceCode/`: many mixed local ports, upstream downloads, decompiled output, dependency jars, and build folders.
- `UtilWeDie-Neo-1.21.1/npc/`: good source candidate, but includes scripts, docs, textures, sounds, and resource-pack assets; import in a separate reviewed commit.

## Rules For Future Imports

- Prefer source and hand-written configuration.
- Keep PCL, `.minecraft`, client zips, crash reports, extracted jars, downloaded archives, and build output out of Git.
- Use Git LFS only for intentional shared binary assets such as textures, sound effects, Blockbench files, schematics, and small curated resource-pack assets.
- Keep third-party upstream code in separate fork repositories where possible. If a local patch is important, document the upstream origin and patch purpose.
