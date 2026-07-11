# Runtime Issues

## TEST-002: Picasso-Test1 local semantic fixture

Status: passed for read-only local semantics; not a Phase 1.5 rendering/write verification.

Fixture intent supplied by the builder around `x=-3, y=-59, z=4`:

- one isolated stair used as a seat;
- one inverted horizontal stair row used as visible lower-floor trim;
- one ascending functional staircase whose reverse-facing top-half stairs are
  decorative underfill belonging to the same staircase;
- two back-facing stair rows stacked over two levels as a supermarket shelf;
- one vanilla fence + white-carpet table;
- two constructed horizontal floor levels.

Read-only `analyze_region(cx=-1, cz=0, radius_chunks=1, y_min=-64, y_max=-45)`
against the local Java 1.21.1 save returned:

- `table`: anchor `(-4,-59,4)`, count 1;
- `supermarket_shelf`: bounds `(-6,-54,4)..(-2,-53,5)`, 20 members;
- `functional_staircase`: bounds `(1,-59,1)..(1,-55,5)`, 5 steps + 4 underfill;
- `decorative_floor_trim`: bounds `(-7,-55,1)..(0,-55,1)`, 8 members;
- `seat_candidate`: anchor `(-6,-59,4)`;
- horizontal candidates at Y `-60` (area 68) and `-55` (area 67), plus the
  surrounding superflat ground at Y `-61`.
- bounded `inspect_volume((-8,-60,0)..(2,-52,8))`: 891 positions, 225 non-air
  blocks, 8 exact block-state palette entries, 38 RLE runs, `complete=true`.

The test opened and closed the save through Amulet without invoking `save`, a
write tool, or any world mutation. Formal room/building segmentation remains
unimplemented; these results are deliberately reported as local candidates.

## RUNTIME-001: Unicode / section-sign world path can time out in `set_world`

Status: observed, workaround available, no implementation change yet.

Observed during external MCP session:

- Direct `set_world` on the original save path timed out:
  `D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\建筑存档\§aMosslorn v3.2-誓死坚守前传地图`
- Creating a short Windows junction worked:
  `D:\MC\picasso_mosslorn_test`
- Junction target:
  `D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\建筑存档\§aMosslorn v3.2-誓死坚守前传地图`
- Successful `set_world` response through the junction included:
  `ok=true`, `level_name=picasso_mosslorn_test`, `journal_status=unavailable`,
  `noise_backend=fallback`, `modded_write_verified=false`.
- `list_passes`, `list_bundles`, and `list_fragments` worked after connection.
- No `apply_pass` was executed; the save has not been modified by Picasso.

Source inspection:

- `tools/world_io.py:set_world` currently converts the input with `Path(world_path)`,
  checks `path.exists()`, and then constructs `AmuletBridge(path)`.
- `core/amulet_bridge.py:AmuletBridge.__init__` calls `amulet.load_level(str(self.world_path))`
  directly. There is no path normalization, short-path fallback, timeout guard, or
  special handling for Unicode / section-sign save names inside Picasso.
- Because the same world opens through a junction, the likely failure point is the
  Amulet / lower-level world loading path when handed the original path string, not
  the MCP tool registration layer.

Current workaround and safety correction:

- `D:\MC\picasso_mosslorn_test` is an NTFS Junction directly targeting the
  building archive. It solves the Unicode-path loader issue but is **not a copy**.
- The junction may be used only for coordinated read-only analysis while
  Minecraft and other editors are closed. Never use it for a write test.
- Any write or revert experiment requires a separately named physical copy. Do
  not confuse the second Mosslorn directory under the launcher's `saves` folder
  with a disposable test copy; it may be the active game save.
- Continue treating `journal_status=unavailable` as non-revertible.

Follow-up options, not implemented yet:

- Add explicit diagnostics around `amulet.load_level(...)` timing and path value.
- Add a documented Windows path workaround section.
- Consider resolving or creating stable ASCII aliases before calling Amulet, only
  after confirming this is consistently an Amulet/path issue rather than a one-off
  save load delay.

## TEST-001: First read/analyze smoke test on Mosslorn junction

Status: passed for basic map reading, partially passed for basic region analysis,
not sufficient for complex semantic target location.

Test session:

- Connected through the short junction path:
  `D:\MC\picasso_mosslorn_test`
- Approximate world target: `x=44`, `z=161`
- Chunk center: `cx=2`, `cz=10`
- Radius: `radius_chunks=1`

`read_region` result:

- `ok=true`
- `chunks_read=9/9`
- `block_count=271128`
- Bounds: `x=16..63`, `y=-64..98`, `z=144..191`

`analyze_region` result:

- `ok=true`
- Surface counts:
  - `ceiling=4139`
  - `outer_wall=19328`
  - `floor=1790`
  - `rooftop=1177`
  - `embedded=197583`
  - `inner_wall=1225`
- `pattern_matches=[]`
- `vegetation_coverage=0.022`
- `damage_estimate=0.0069`
- Summary: analyzed `271128` blocks and detected `0` furniture pattern groups.

Interpretation:

- Basic map reading passed: chunk access, non-air block counting, bounds, and
  surface classification all returned plausible results.
- Basic region analysis partially passed: top-level surface and coverage metrics
  exist, but no known furniture pattern was detected in this sample.
- Complex semantic target location is not proven. Current public tools do not
  expose `find_structure(type=crane)`, `detect_highrise`, `query_blocks`, or
  coordinate-level structure search. Existing `read_region` and `analyze_region`
  intentionally return summaries, not full block coordinate dumps.

Next test step:

- Continue with dry-run only: `preview_pass` or `apply_bundle(dry_run=true)` on a
  small radius.
- Do not use `apply_pass`, `apply_bundle(dry_run=false)`, or `place_npc_marker`
  until write safety and expected targets are reviewed.

## RUNTIME-002: Tool discovery can hide registered Picasso tools

Status: observed in client/tool discovery layer, no Picasso code change yet.

Observed behavior:

- The Picasso MCP server had registered `read_region` and `analyze_region`.
- A first generic tool search for Picasso exposed only a partial set:
  `set_world`, `create_bundle`, `create_fragment`, `apply_pass`,
  `list_passes`, `list_bundles`, `list_fragments`.
- A later more specific search for `read_region analyze_region` exposed the
  missing map-reading and analysis tools.
- This led the test agent to initially misjudge Picasso as lacking map read /
  region analysis capabilities.

Source inspection:

- `server.py:register_tools()` registers all tool modules:
  `world_io`, `analysis`, `catalog`, `style`, `bundle`, `learning`, and `npc`.
- `tools/world_io.py` defines `read_region`.
- `tools/analysis.py` defines `analyze_region`.
- Therefore the partial visibility appears to come from the client/tool-search
  recall mechanism, not from Picasso failing to register these tools.

Suggested client/tooling improvements:

- Searching a server name such as `picasso` should expose the full tool list for
  that MCP namespace, or clearly label results as partial matches.
- Add a `list_all_tools` or `describe_server_capabilities` path for a namespace.
- Keep core Picasso tools discoverable together:
  `set_world`, `read_region`, `analyze_region`, `query_catalog`, `list_passes`,
  `preview_pass`, `apply_pass`, `list_bundles`, `apply_bundle`,
  `list_fragments`, `create_fragment`, and `place_npc_marker`.
- Improve tool descriptions for `read_region` and `analyze_region` so generic
  searches for map reading, chunk reading, and region recognition find them.

## TEST-002: Dry-run preview attempt timed out before producing preview payload

Status: not passed, no save write performed.

Intent:

- Run a second-round dry-run preview only, without writing to the save.
- Test target: `D:\MC\picasso_mosslorn_test`
- Initial requested area: `cx=2`, `cz=10`, `radius_chunks=1`
- Pass: `tlou_rubble_scatter`

Sub-agent result:

- A sub-agent was launched with explicit instructions forbidding `apply_pass`,
  `apply_bundle(dry_run=false)`, `place_npc_marker`, file edits, and save writes.
- The sub-agent did not return within the wait window for `radius_chunks=1`.
- It was interrupted and redirected to `radius_chunks=0` with timing output.
- It still did not return within the wait window and was shut down.

Local controlled retry:

- A local Python dry-run script was run with `radius_chunks=0`, direct
  `StyleEngine.apply(...)`, and `WriteChoke.validate(enforce_modded_gate=False)`.
- The process logged successful Picasso initialization and Amulet load start:
  `Loading level D:\MC\picasso_mosslorn_test`.
- It did not produce preview counts within roughly 90 seconds and was stopped.
- The stopped process was the local stdin Python test process only; no MCP server
  or save content was intentionally stopped or modified.

Interpretation:

- Dry-run preview is conceptually safe because it computes candidate block changes
  and safety filtering without calling `bridge.write_region(...)`.
- This specific dry-run preview attempt is not yet usable as a passed test because
  it timed out before producing `would_change` / sample-change output.
- Likely next debugging step is stage-level instrumentation around world open,
  `ensure_region`, fragment anchor scanning, and `StyleEngine.apply` on the
  Mosslorn save.

Implemented mitigation:

- `read_region`, `analyze_region`, `preview_pass`, `apply_pass`, and
  `apply_bundle` now accept optional `y_min` / `y_max` parameters.
- The region cache key includes the resolved vertical scan window, so a workflow
  that calls `read_region`, `analyze_region`, and `preview_pass` with the same
  `cx/cz/radius/y_min/y_max` can avoid repeated full-height scans inside one
  MCP server session.
- Tool responses now include `y_window`, `region_cache_hit`, `region_source`, and
  `timings` fields for read/analyze/preview paths.

Recommended next dry-run command shape:

- `read_region(cx=2, cz=10, radius_chunks=1, y_min=40, y_max=120)`
- `analyze_region(cx=2, cz=10, radius_chunks=1, y_min=40, y_max=120)`
- `preview_pass(pass_name="tlou_rubble_scatter", cx=2, cz=10, radius_chunks=1, y_min=40, y_max=120)`

Further optimization ideas, not implemented yet:

- Add an explicit `scan_mode` such as `full`, `window`, or `surface_band`.
- Use chunk palette / heightmap data where Amulet exposes it safely, instead of
  calling per-block accessors for every Y coordinate.
- Add structure-specific scanners that inspect columns or sparse candidate blocks
  first, then expand into detailed block reads only around candidate buildings.

## TEST-003: Height-window cache and synthetic dry-run core test

Status: passed for pure-code validation, no save opened and no save write performed.

Sub-agent attempt:

- A sub-agent was launched to validate the new height-window and dry-run diagnostics.
- It was explicitly forbidden from opening `D:\MC\picasso_mosslorn_test` or writing
  any save data.
- The first attempt did not return in time, so it was redirected to pure-code /
  synthetic tests only.
- The redirected attempt still did not return and was shut down.

Main-session controlled test:

- `initialize()` and `register_tools()` passed with `14` passes and `1` bundle.
- A fake bridge was used to test `ensure_region` without opening any Minecraft save.
- First `ensure_region(2,10,1,y_min=40,y_max=120)` read from the fake bridge.
- Second call with the same window hit cache: `region_source=cache`,
  `region_cache_hit=True`, fake bridge call count remained `1`.
- Third call with `y_min=60,y_max=120` missed cache as expected and raised the fake
  bridge call count to `2`.
- A synthetic indoor region produced:
  - `surface_counts`: `ceiling=256`, `floor=256`, `rooftop=256`
  - `tlou_rubble_scatter` raw changes: `33`
  - write-choke would-change count: `33`
  - skipped placements: `0`
  - modded positions: `0`
- `describe_capabilities` returned `15` tools, including `read_region` and
  `preview_pass`, with descriptions mentioning `y_min/y_max`.

Implementation note:

- `session.close_bridge()` now tolerates test doubles without a `close()` method
  while preserving normal Amulet bridge close behavior.

## TEST-004: Agent dry-run blocked at `set_world` on Mosslorn junction

Status: not passed, no save write performed.

Test request:

- Use an agent to run the real Mosslorn windowed dry-run sequence:
  `set_world`, `read_region`, `analyze_region`, `preview_pass`.
- Target path: `D:\MC\picasso_mosslorn_test`
- Region: `cx=2`, `cz=10`, `radius_chunks=1`, `y_min=40`, `y_max=120`
- Pass: `tlou_rubble_scatter`

Agent run 1:

- Completed only `describe_capabilities`.
- Was interrupted before `set_world`; no world was opened and no write occurred.

Agent run 2:

- Found the Picasso MCP tools.
- Called `set_world(D:\MC\picasso_mosslorn_test)`.
- `set_world` did not return normally and was aborted after roughly `249.4s`.
- `read_region`, `analyze_region`, and `preview_pass` were not executed.
- No write tools were called.

Interpretation:

- This run does not yet test dry-run preview. It shows that a fresh agent/server
  opening the Mosslorn junction can block at `set_world`.
- Multiple `picasso.server` Python processes were visible after agent/test runs,
  so a likely contributing factor is multiple MCP server instances trying to open
  the same Amulet world concurrently.
- Recommended test path is to reuse one already-connected Picasso MCP session for
  `read_region`, `analyze_region`, and `preview_pass`, rather than launching a new
  agent/server for each dry-run attempt.

Follow-up options:

- Add a read-only `set_world` timing/lock diagnostic that reports before and after
  `amulet.load_level(...)`.
- Add an MCP-side `close_world` or session cleanup tool.
- Avoid parallel agents against the same world path until Amulet concurrent open
  behavior is understood.

## TEST-005: Single-owner Mosslorn windowed dry-run passed

Status: passed, no save write performed.

Setup:

- All visible `python.exe -m picasso.server` processes were stopped first, after
  explicit user approval, to release competing Amulet connections.
- A single main-session Python process then opened:
  `D:\MC\picasso_mosslorn_test`
- Region: `cx=2`, `cz=10`, `radius_chunks=1`, `y_min=40`, `y_max=120`
- Pass: `tlou_rubble_scatter`

Timings:

- Picasso initialize: `0.014s`
- Open world: `1.696s`
- Read region: `0.875s`
- Analyze region: `2.571s`
- Style engine apply: `0.008s`
- Write choke validate: `0.001s`
- Total: `5.165s`

Read result:

- `chunks_read=9`
- `chunks_missing=0`
- `block_count=55021`
- Bounds: `x=16..63`, `y=40..98`, `z=144..191`
- Requested `y_window=40..120`

Analysis result:

- Surface counts:
  - `ceiling=1598`
  - `outer_wall=1938`
  - `floor=929`
  - `rooftop=784`
  - `embedded=10789`
  - `inner_wall=736`
- Pattern matches: `0`
- Vegetation coverage: `0.0609`
- Damage estimate: `0.0219`

Dry-run preview result:

- Raw changes: `24`
- Would change after write choke: `22`
- Skipped: `2`
- Modded positions: `0`
- No `bridge.write_region(...)`, `level.save`, `apply_pass`, or
  `apply_bundle(dry_run=false)` was called.

Interpretation:

- The windowed dry-run path is viable when one process owns the world connection.
- Prior agent failures are consistent with process/session contention during
  `set_world`, not with `tlou_rubble_scatter` dry-run computation itself.

## TEST-006: Advisory world lock and `close_world` validation passed

Status: passed, no save write performed.

Implemented behavior:

- `set_world` now acquires a Picasso advisory lock for the resolved world path
  before calling `amulet.load_level(...)`.
- If the same server is already connected to the same world, `set_world` returns
  `already_connected=true` and does not reopen Amulet.
- If another Picasso process owns the same world lock, `set_world` returns
  `error=world_locked` instead of entering a long Amulet open.
- `close_world` closes the active bridge and releases the advisory lock.
- `describe_capabilities` now reports `close_world` and the active lock summary.

Validation:

- First `set_world(D:\MC\picasso_mosslorn_test)` succeeded:
  `open_world_seconds=0.463`, `total_seconds=0.491`.
- Second `set_world` on the same path returned `already_connected=true` with
  `open_world_seconds=0.0`.
- Attempting to acquire the same world lock again while connected was blocked.
- `describe_capabilities` reported `world_connected=true` and a lock summary.
- `close_world` returned `was_connected=true` and `lock_released=true`.
- After `close_world`, acquiring the same lock succeeded, proving release worked.

Operational note:

- This lock is advisory for Picasso processes only. It does not prevent external
  editors or Minecraft itself from opening the save.

## TEST-007: Composite furniture synthetic recognition passed, default replacement withheld

Status: synthetic recognition passed, but default detection/replacement is intentionally
disabled for high-risk composite templates pending structure/room context. No save
opened and no save write performed.

Implemented:

- `PatternMatcher` now prefers more complex templates before simple ones, so a
  composite desk is not consumed by a simpler chair/table pattern first.
- Pattern blocks support `optional`, `min_optional_matches`, `match.block`,
  `match.name_contains_any`, `match.name_contains_all`, and `match.name_endswith`.
- `pattern_replace` now clears only actually matched `clear_offsets`, avoiding
  writes against absent or non-matching optional parts.
- Pattern replacement and its clears are treated as an atomic write group at
  the write choke: if any position is blocked, the entire replacement group is
  dropped.
- Added `computer_desk_combo`:
  slab desktop + player-head monitor + optional slab/carpet/fence/stair parts.
- Added `supermarket_stair_shelf`:
  two-tier side-by-side stair shelf with optional extension/carpet goods.
- Both new templates are marked `experimental=true` and skipped by default
  `PatternMatcher.find_matches`, `analyze_region`, and `pattern_replace`.
- `tlou_furniture_modreplace` does not map either experimental template.

Validation:

- Synthetic canonical layout matched:
  `computer_desk_combo` and `supermarket_stair_shelf` when explicitly loaded
  with experimental matching enabled.
- Synthetic 90-degree rotated layout also matched both patterns.
- Adversarial review found these templates unsafe as default replacements:
  stadium seating, real stair runs/stairwells, stepped shelves, counters,
  railings, trophy/player-head displays, and library shelving can collide with
  the same local blocks.
- Current ruling: do not enable stair/head/fence/slab composite replacement by
  default until segmentation/room context can distinguish "chair/desk/shelf"
  from "stairs, bleachers, displays, railings, and building structure".

## STABILIZATION-2026-07-10: P0 read/write substrate hardened

Status: implemented and covered by automated unit plus temporary-world tests.
This section supersedes older implementation-state statements above but does
not rewrite the historical observations or timings.

### Resolved or stabilized

- **Native Java reads:** `AmuletBridge` now uses the pinned world's versioned
  accessor. Canonical Java IDs are returned; universal-only states and
  conversion failures raise instead of being mislabeled or returned as air.
- **Properties:** Picasso string properties are encoded as Amulet NBT
  `StringTag` values. Temporary-world save/reopen tests preserve `axis`,
  `facing`, and choke-injected leaf `persistent` properties.
- **Transactional bridge writes:** every target block is preconstructed before
  mutation, native block/block-entity before-states are snapshotted, and the
  batch is saved through `write_region`. A failed batch attempts rollback; an
  incomplete rollback poisons that bridge so further writes fail until reopen.
- **Trusted read envelope:** `RegionData` records resolved Y bounds, exactly
  loaded chunks, halo positions, and block-entity positions. Write validation
  rejects `outside_region`, `outside_y_window`, `chunk_not_loaded`,
  `halo_position`, and `block_entity_protected` targets.
- **Fail-closed policy:** missing/invalid/empty `safe_blocks.json` blocks all
  writes with `safety_policy_unavailable`. Empty catalog state reports
  `catalog_not_loaded`; a non-vanilla ID absent from loaded catalogs blocks the
  entire operation with `unknown_catalog_block`.
- **Sparse AIR layering:** merging an AIR change removes the position and its
  stale classification/block-entity metadata from the working snapshot.
- **NPC marker durability:** marker placement directly verifies an empty,
  block-entity-free target, uses the common choke and `write_region` save,
  commits companion JSON via same-directory temp + `os.replace`, and attempts a
  saved before-state rollback if metadata commit fails. It no longer reports
  success after a bare unsaved `place_block` call.
- **Concurrency and ownership:** all 22 registered MCP tools are serialized by
  `SingleFlightFastMCP`; advisory per-world locking plus `close_world` remains
  the cross-process guard described in TEST-006.
- **Self-contained install:** the 1143-entry DD catalog ships in the package;
  the active inventory is 14 passes, 11 loaded fragments (12 files, one
  deprecated), 8 patterns, and a 695-entry replaceable whitelist. CPython is
  constrained to 3.11, with `amulet-core==1.9.41` and `mcp==1.28.1` pinned.

### Verification snapshot

- Latest full suite: `407 passed`, `1 skipped`, plus `14` subtests.
- Last coverage-instrumented 270-test snapshot, omitting tests: `83%`.
- Wheel build and isolated installation smoke: passed (catalog 1143 / passes 14 / fragments 11 / bundle 1).
- No production world was opened or modified by this stabilization suite;
  Amulet integration tests use generated temporary worlds.

### Still open — do not infer completion

- **Phase 1.5 remains 🚧.** The three controlled markers described in TEST-008
  preserve both DD IDs and `facing=east` through Amulet save/reopen, but
  Minecraft 1.21.1 with the DD mod has not yet verified rendering, collision or
  interaction. Keep `PICASSO_MODDED_WRITE_VERIFIED=false`.
- Durable journal/revert, horizontal/vertical classification context, and
  activity/registry player protection are implemented. On a formal/shared
  world, stop if `journal_status` or `player_protection` is not `active`.
- Structure segmentation/mode, Brush/Room, work orders, and statistical style
  learning remain future subsystems.
- RUNTIME-001's original Unicode/section-sign path behavior is not declared
  fixed; the short ASCII junction remains the documented workaround.

## TEST-008: Phase 1.5 markers written to Picasso-Test1

Status: Amulet write/save/reopen passed; actual Minecraft rendering and
interaction review is pending, so the Phase 1.5 gate remains closed.

Preconditions and recovery:

- Minecraft/NeoForge process count was `0` immediately before the operation.
- The destination cells were confirmed as air without block entities, above
  grass at `y=-61`.
- A full 11,873,694-byte backup was created at
  `D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\建筑存档\Picasso-Test1-pre-phase15-20260710-2114`.
  Its byte count and `level.dat` SHA-256 matched the source at backup time.

Recorded compatibility tuple:

- Minecraft world: Java Forge 1.21.1, `DataVersion=3955`, launched from the
  `1.21.1-NeoForge_21.1.233` profile;
- Doomsday Decoration: `1.1.3-neoforge-1.21.1`;
- Amulet Core: `1.9.41` (`PyMCTranslate 1.2.45`, translator data revision 386).

One atomic `AmuletBridge.write_region` batch placed:

- `(5, -60, 4)` — `doomsday_decoration:acrate` with no properties;
- `(7, -60, 4)` — `doomsday_decoration:accessorybox_1` with `facing=east`;
- `(9, -60, 4)` — `minecraft:diamond_block` as the vanilla control.

After save and bridge close, a new bridge instance read back the same three
IDs, retained `facing=east`, and found no block entities. PyMCTranslate emitted
its expected warning that it has no vanilla translation table for the two DD
IDs; it did not rewrite or drop them.

Remaining human/game check:

1. Open `Picasso-Test1` with the exact 1.21.1 NeoForge modpack.
2. Inspect the three blocks in the east-west row at the coordinates above.
3. Confirm both DD models render, the accessory box visibly faces east, their
   collision/interaction does not crash or replace them, and the diamond block
   remains normal.
4. Exit the world before Picasso reopens it, then record pass/failure here. Set
   `PICASSO_MODDED_WRITE_VERIFIED=true` only after a pass.
