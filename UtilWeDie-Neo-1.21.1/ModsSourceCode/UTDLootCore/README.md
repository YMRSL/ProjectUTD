# UTD Loot Core

UTD Loot Core is a standalone NeoForge 1.21.1 mod that packages Project UTD's
Doomsday Decoration loot data and pity runtime into one versioned JAR.

**Current status:** source, generated resources, tests, and JAR build are complete.
On 2026-07-11 the JAR was copied into the local NeoForge 21.1.233 client and
the duplicate KubeJS Loot paths were moved, not deleted, into the local rollback
directory. Full-modpack cold-start and in-game container acceptance are pending.

## What is included

- 798 preserved loot-registry records; 678 remain enabled.
- 14 records from absent mods are preserved for auditability but forced disabled.
- 13 templates and 280 normalized container configurations.
- 78 helper loot tables under `data/utd_loot_core/loot_table/utd/`.
- 280 Doomsday chest tables under
  `data/doomsday_decoration/loot_table/chests/`.
- A persistent Java pity listener for civilian, medical, and military channels.
- Logical-stack recognition for TaCZ guns (`GunId`), First Person Food Eating
  packs (`food_id`), and TaCZ workbenches (`BlockId`).
- In-game diagnostic commands, offline validation, unit tests, and embedded
  SHA-256 manifests.

The four former `paraglider:paraglider` entries were removed from the affected
tier-1 tables. All chest references now target `utd_loot_core:utd/...`; all
container IDs use `doomsday_decoration:chests/...`.

## Known absent-mod records

These records remain in `registry.json` with `lootEnabled=false`, zero weights,
and `disabledReason=missing_mod_not_in_1_21_1_pack`:

- `locks:*` (10 records)
- `paraglider:paraglider`
- `ropebridge:bridge_builder`
- `ropebridge:ladder_builder`
- `the_ravenous:anti_ravenous`

The Java candidate selector also verifies the physical item registry at runtime,
so a future modpack removal cannot turn a pity roll into a missing item.
Disabled rows are still retained in the inventory-level index: an already-opened
container containing one of those legacy high-tier variants can therefore reset
the pity counter exactly as the retired KubeJS scanner did. Disabled rows can
never be selected for a new pity injection.

## Runtime design

The old startup-script ordering problem no longer exists: the registry and
balance JSON are loaded together from the JAR before the listener is registered.

When a Doomsday container opens, the listener:

1. accepts only the current `DoomsdayContainerMenu` and its first 27 loot slots;
2. obtains the current `doomsday_decoration:chests/...` ID and refresh day from
   the backing BlockEntity;
3. checks a per-BlockEntity persistent roll marker to prevent repeated opens from
   incrementing pity;
4. applies the preserved soft/hard pity policy;
5. scans and updates the player's existing KubeJS-compatible counter keys; and
6. writes injected items directly through the menu slots and broadcasts changes.

This avoids both fragile KubeJS paths discovered in the audit: it does not depend
on `block.getInventory()` or on an unknown root-NBT field being retained by the
Doomsday BlockEntity.

Pity counters retain their existing names and are copied through player death:

- `utdCivilianT4Miss`
- `utdMedicalT4Miss`
- `utdMilitaryT4Miss`
- `utdMilitaryT5Miss`

## Data provenance and deterministic generation

The input was protected by the local archive:

- `_local_snapshots/runtime-kubejs-baseline-20260711.zip`
- SHA-256:
  `DED3D9882C5EA821E7FFBD602BB137AE9F0BF4784331951CA548569A1874042A`

Generated-data aggregate SHA-256:

`B6C3CB2610BA07EA949BE051AB8ADC57C997AB4C3922C97B60592E6B0CCA9D06`

The exact source-file hashes, counts, disabled IDs, generated file hashes, and
aggregate are stored in:

- `src/main/resources/data/utd_loot_core/manifest.json`
- `src/main/resources/data/utd_loot_core/manifest.hashes.json`

To rebuild the resources from the preserved runtime baseline:

```powershell
python tools/import_runtime_data.py
python tools/validate.py --resource-root src/main/resources
```

The importer is one-way and refuses to write outside this mod project.

## Build and tests

Requirements: JDK 21, Python 3, and network/cache access for NeoForge Gradle
dependencies.

```powershell
.\gradlew.bat clean check --no-daemon
.\gradlew.bat build --no-daemon
```

`check` runs all of the following:

- offline JSON/schema/reference validation;
- four Python resource tests;
- six JUnit tests: three for the preserved pity thresholds and three for
  logical TaCZ/FPE/workbench stack identity;
- final-JAR verification for Java classes, NeoForge metadata, 78 helper tables,
  280 chest tables, forbidden legacy paths, and all 360 embedded data hashes.

The build artifact is:

`build/libs/utd_loot_core-1.0.0-1.21.1.jar`

## Diagnostic commands

- `/utdloot status` — catalog counts.
- `/utdloot pity` — current player counters.
- `/utdloot pity reset` — reset counters; permission level 2.
- `/utdloot set <civilian|medical|military> <tier4Miss> <tier5Miss>` — set test
  counters; permission level 2. Tier-5 applies only to military.

## Deployment boundary

`neoforge.mods.toml` declares Doomsday Decoration `required` and `AFTER`, so this
mod's `doomsday_decoration:chests/...` resources follow the owning mod's pack.

An external KubeJS resource pack can have higher priority than mod JAR resources,
so deployment must be atomic and reversible:

1. preserve the current runtime snapshot;
2. install this JAR;
3. quarantine, rather than delete, the old seven UTD Loot scripts and duplicate
   78/280 KubeJS data trees;
4. cold-start the client; and
5. pass the in-game acceptance checks before making the quarantine permanent.

Steps 1–3 were completed locally on 2026-07-11. The seven scripts and duplicate
78/280 data trees are preserved as 365 files under
`_local_snapshots/utd_loot_kubejs-retired-20260711/`. Steps 4–5 remain the
current user acceptance gate.

## In-game acceptance checklist

1. Cold start with no `kubejs:utd` parse errors or unknown helper tables.
2. Confirm `/utdloot status` reports 798 registry rows and 280 containers.
3. Open representative containers:
   - civilian: `blackvan_1`;
   - medical: `ambulance_1`;
   - military: `ammunitionbox` or `airdrop`.
4. Set civilian/medical/military T4 misses to 10 and verify the next eligible
   container injects a tier-4 item and resets the corresponding counter.
5. Set military T5 misses to 20 and verify the next eligible military container
   injects a tier-5 item.
6. Reopen the same container and verify counters do not change again.
7. Advance through a Doomsday seven-day refresh and verify the refreshed roll is
   processed exactly once.
8. Restart the world and confirm both player counters and container roll markers
   persist.

Third-party model, texture, blockstate, Railways, ZSK, and Tracks warnings are not
part of this mod's acceptance scope.
