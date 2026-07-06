# Source Import Plan

This repository is being imported in small batches so that Git history stays useful and large Minecraft runtime files do not leak into source control.

## Imported

- `Migration-1.21.1/`: migration notes.
- `SocialWill/Picasso/`: Python tooling and documentation.
- `Code/ItemsReFresh/`: maintained Java plugin source. Build outputs are ignored.
- `UtilWeDie-Neo-1.21.1/ModsSourceCode/`: active UtilWeDie mod source workspaces and curated assets. Build output, Gradle caches, extracted jars, temporary tooling folders, and selected embedded upstream Git snapshots remain ignored.

## Held For Review

- `Code/SuperbWarfare-superbwarfare/`: upstream/source snapshot, about 117 MB.
- `Code/WorldEdit-version-7.2.x/`: upstream/source snapshot.
- `Code/WorldEditCUI-Arch-master/`: upstream/source snapshot.
- `UtilWeDie-Neo-1.21.1/npc/`: good source candidate, but includes scripts, docs, textures, sounds, and resource-pack assets; import in a separate reviewed commit.
- `D:/ModDevelop/`: large external development workspace discovered during repository cleanup. Needs a separate inventory and copy/import pass before it is managed by this repository.

## Rules For Future Imports

- Prefer source and hand-written configuration.
- Keep PCL, `.minecraft`, client zips, crash reports, extracted jars, downloaded archives, and build output out of Git.
- Use Git LFS only for intentional shared binary assets such as textures, sound effects, Blockbench files, schematics, and small curated resource-pack assets.
- Keep third-party upstream code in separate fork repositories where possible. If a local patch is important, document the upstream origin and patch purpose.
- Keep the GitHub repository private until third-party licenses and redistribution rights for imported source/assets are reviewed.
