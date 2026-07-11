# UTD Loot Core validation report

Date: 2026-07-11
Target: Minecraft 1.21.1 / NeoForge 21.1.233
Deployment status: **installed locally; full-modpack in-game acceptance pending**

## Provenance

- Runtime baseline archive:
  `_local_snapshots/runtime-kubejs-baseline-20260711.zip`
- Baseline SHA-256:
  `DED3D9882C5EA821E7FFBD602BB137AE9F0BF4784331951CA548569A1874042A`
- Generated-data aggregate SHA-256:
  `B6C3CB2610BA07EA949BE051AB8ADC57C997AB4C3922C97B60592E6B0CCA9D06`

## Validated data

| Check | Result |
| --- | ---: |
| Registry seed rows | 798 |
| Enabled registry rows | 678 |
| Absent-mod rows forced disabled | 14 |
| Helper loot tables | 78 |
| Doomsday chest tables | 280 |
| Container configurations | 280 |
| Templates | 13 |
| Helper references | 1,316 |
| Distinct referenced helpers | 61 |
| Generated files covered by SHA-256 index | 360 |
| Missing helper references | 0 |
| Legacy `doomsday_functionality` runtime references | 0 |
| Legacy `kubejs:utd/` chest references | 0 |
| Remaining `paraglider:paraglider` helper entries | 0 |

The four invalid paraglider entries were removed from
`corpse_archive/tier1`, `retail_home/tier1`, `vehicle_civil/tier1`, and
`vehicle_wreck/tier1`. No chest file needed to be deleted.

## Automated verification

Command executed twice from a clean state:

```powershell
.\gradlew.bat clean check --no-daemon
```

Both runs completed successfully. The check graph included:

- offline schema, count, namespace, candidate, reference, and SHA-256 validation;
- four Python `unittest` resource tests;
- six JUnit tests: three pity-policy tests plus three logical-stack identity
  tests for TaCZ `GunId`, First Person Food Eating `food_id`, and TaCZ
  workbench `BlockId`;
- Java compilation against NeoForge 21.1.233;
- final-JAR metadata and embedded-resource verification.

The two clean verification runs produced the same JAR SHA-256.

## Built artifact

- Path: `build/libs/utd_loot_core-1.0.0-1.21.1.jar`
- Size: 294,570 bytes
- SHA-256:
  `42DF9ABC835B9B206E49970005627EF16D8F7E658EEC972C1E1D0788EAB33823`

The final JAR verifier confirmed:

- main catalog, pity handler, policy, state, and command classes are present;
- logical variant identity is recognized before generic component signatures,
  while disabled legacy rows participate only in inventory-level detection and
  remain excluded from injection candidates;
- all 78 helper and 280 chest JSON files are present at singular 1.21.1
  `loot_table` paths;
- all 360 embedded generated-file hashes match the manifest;
- `neoforge.mods.toml` declares `doomsday_decoration` as `required` and
  `ordering = "AFTER"`;
- no old `doomsday_functionality`, plural `loot_tables`, or KubeJS helper path
  is embedded.

## Remaining acceptance gate

The result is build-verified but not yet game-verified. Deployment must remain a
separate reversible step because the existing KubeJS pack can override JAR data.
The required cold-start and civilian/medical/military pity tests are documented
in the project README.
