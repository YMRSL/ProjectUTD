# Implementation Order & Status — Base System

> Build sequence for the base system — tracks `ARCHITECTURE.md` v0.4.4. Each phase produces something testable before the next begins. Status: ✅ done · 🔁 done but revisit in refactor · 🚧 not built.
>
> Mechanical work is split in `docs/mechanical_structures.md`: static ruins use
> the existing Fragment substrate after a strict stateless capability gate;
> Create rail replacement is the high-priority dedicated R-track; functional
> production lines, farms and elevators are a later M-track. None inherits
> authorization from another.
>
> Segmentation build sequence: `docs/segmentation_implementation_phases.md`.
> Read `ARCHITECTURE.md` fully before touching anything.

**Standing rule for every phase:** copied saves are the supported baseline. On a formal/shared world, write only while Minecraft is closed, after an exact dry-run, with `journal_status=active` and player protection active. A small purpose-built test world remains the standard fixture.

**Testing stance (revised in v0.4):** the layering rule exists precisely so `core/` is unit-testable without MCP. Deterministic algorithms (surface classification, position-hash rolls, fragment rotation, pattern matching) get **pytest** tests with synthetic `RegionData` fixtures — each phase's "Done When" doubles as its test list. In-game visual verification remains required for anything touching a real world file.

---

## Phase 1 — Project Skeleton & Amulet Bridge ✅

**Goal:** the server starts, opens a world, reads a region, prints a block count.

Built: `pyproject.toml`, `config.py`, `models/block.py`, `models/region.py`, `core/amulet_bridge.py`, `session.py`, `server.py`, `tools/world_io.py` (`set_world`, `read_region`).

**Done When** (met): server starts; `set_world` on a 1.21.1 save returns `ok`; `read_region(0,0,2)` returns non-zero `block_count`.

### 2026-07-10 P0 stabilization ✅ *(base write substrate, not Phase 1.5)*

- CPython is constrained to `>=3.11,<3.12`; `mcp==1.28.1` and
  `amulet-core==1.9.41` are pinned. The wheel builds successfully.
- `SingleFlightFastMCP` serializes every synchronous tool call; per-world
  advisory locking and `close_world` prevent competing Picasso opens.
- Amulet reads native Java-version IDs, detects block entities, encodes string
  properties as `StringTag`, and preconstructs/snapshots write batches before
  mutation. Failed batches roll back; incomplete rollback poisons the bridge
  until reopen.
- `RegionData` carries the resolved Y window, loaded chunks, halo positions and
  block-entity positions. `WriteChoke` rejects anything outside that trusted
  envelope and protects block entities unconditionally.
- Missing/invalid/empty safety policy fails closed; unknown non-vanilla IDs
  block the whole operation. AIR merges remove sparse world entries and stale
  metadata.
- `place_npc_marker` now requires a directly verified empty target, uses the
  common choke and a real saved `write_region`, commits metadata atomically,
  and attempts a saved rollback if metadata commit fails.
- Verification baseline: **407 passed, 1 skipped, 14 subtests**. The last
  coverage-instrumented 270-test run reported **83% source coverage**; isolated
  wheel smoke loads catalog 1143 / passes 14 / fragments 11 / bundle 1.

---

## Phase 1.5 — Modded-Block Round-Trip Spike 🟡 ⛔ **written/reopened; in-game review pending**

**Goal:** verify the project's load-bearing assumption (`ARCHITECTURE.md` §4.2): a `doomsday:*` block written via Amulet survives save → open in Minecraft 1.21.1 + DD mod → renders correctly, with block state properties intact.

### Tasks
1. On a **copy** of a world: `place_block` one DD block (simple), one DD block with properties (e.g. facing), one vanilla block as control.
2. `level.save()`, close, open in the actual game, screenshot/verify.
3. Record the result in this file. If it fails: investigate Amulet's universal-format handling of unknown namespaces before writing any further fragment/pass content that uses DD blocks.

### Done When
- All three blocks verified in-game, or a documented failure analysis with a fallback plan (structure-block staging / MCEdit-schematic export / direct region-file NBT).

**2026-07-10 status:** the controlled `Picasso-Test1` save now contains the
three in-game verification markers at `z=4, y=-60`: simple DD
`doomsday_decoration:acrate` at `x=5`, property-bearing DD
`doomsday_decoration:accessorybox_1[facing=east]` at `x=7`, and the vanilla
control `minecraft:diamond_block` at `x=9`. The save was backed up first, then
all three IDs/properties passed an Amulet save/close/reopen check. Minecraft
1.21.1 with the installed DD mod must still verify their rendering, facing,
collision and interaction. Keep `PICASSO_MODDED_WRITE_VERIFIED=false` until
that visual check passes; this phase remains open.

---

## Phase 2 — Catalog Index ✅ *(code + packaged data)*

Built: `core/catalog_index.py`, `tools/catalog.py` (`query_catalog`), startup load into `session.catalog`.

The 1143-entry DD catalog now ships in
`data/catalog/doomsday_decoration_semantic.json`; no setup copy is required.
`PICASSO_CATALOG_PATHS` supports ordered multi-source override/extension. If no
catalog loads, `query_catalog` returns `catalog_not_loaded`, and non-vanilla
writes fail closed.

**Done When** (met): English and source-vocabulary queries return DD entries;
an empty filter returns all 1143 entries.

---

## Phase 3 — Region Analysis ✅

Built: `core/surface_classifier.py` (see normative spec `ARCHITECTURE.md` §4.5 — verify implementation matches the priority table exactly, esp. the `ceiling` rule 🔁), `core/pattern_matcher.py`, 8 pattern JSONs, `core/stair_semantics.py`, `core/storey_semantics.py`, `core/volume_inspector.py`, `tools/analysis.py` (`analyze_region`), `tools/inspection.py` (`inspect_volume`), and the static MCP semantic-review instructions/prompt. Local semantic helpers emit conservative candidates and bounded evidence only; they do not claim completion of the S-phase building/room segmentation subsystem. Normative Agent workflow: `docs/agent_semantic_review.md`.

🔁 Revisit: pattern matching must check all 4 rotations (`docs/style_pass_schema.md` §3); confirm and add pytest fixtures for rotated furniture.

`safe_blocks.json` now ships **695 replaceable IDs**. The 17-entry coverage gap
is closed. Block-entity positions are nevertheless rejected independently of
whitelist membership because Picasso does not yet preserve their NBT.

**Done When** (met): non-zero counts for ≥3 surface classes on an urban fixture; ≥1 chair/table detected where present; bounded voxel evidence preserves block properties, fails closed on missing chunks, and reports truncation rather than treating omissions as air.

---

## Phase 3.5 — Halo Read for Boundary Classification ✅

The bridge reads a one-chunk horizontal halo plus bounded read-only vertical
context (`y_min - 1` through `y_max + 4`, clamped to world height). Sparse
palette reads keep this affordable. Context participates in classification but
is excluded from analysis targets and the writable envelope.

**Goal:** eliminate region-boundary misclassification (`ARCHITECTURE.md` §4.5: out-of-region reads as air, so blocks at the region edge — and especially at tile seams — misclassify; §12.4's tiling advice multiplies this exposure).

### Tasks
1. ✅ `AmuletBridge.read_region` reads the halo once and records separate core/context counts.
2. ✅ Classification consumes context; `WriteChoke` rejects every halo target.
3. ✅ `PICASSO_MAX_RADIUS_CHUNKS` remains the caller's core-radius allowance;
   the mandatory halo does not silently reduce that quota.

### Done When
- pytest: a wall at the region edge with its air-side just outside the region classifies identically to the same wall mid-region.
- pytest: no pass ever writes to a halo position.
- Tiling a fixture into 4 tiles produces seam-adjacent classifications identical to a single whole-region run.

---

## Phase 4 — Style Engine & Core Style Tools ✅

Built: `models/style_pass.py` (flat model with loader type validation),
`core/noise_field.py` (C lib optional + deterministic fallback,
`ARCHITECTURE.md` §4.6), `core/style_engine.py` (three-type dispatcher), pass
auto-load, and the style tools.

🔁 Revisit in refactor:
- All stochastic decisions must use the position-hash scheme (`ARCHITECTURE.md` §6) — audit for any iteration-order dependence.
- `preview ≡ apply` diff equality gets a pytest property test.
- Write choke point (`ARCHITECTURE.md` §12.1) — ✅ consolidated for pass,
  bundle, and NPC writes with trusted-envelope, block-entity, safety-policy,
  marker, catalog, modded-write, and player-activity/registry protection gates.

**Done When** (met): `list_passes` returns the registry; `preview_pass` > 0 changes on fixture; `apply_pass` writes and the world shows mossy stone in-game.

---

## Phase 5 — Fragment System ✅ *(initial)* / 🔁

Built: `models/fragment.py`, `core/fragment_library.py`, `core/fragment_engine.py`,
12 fragment files (**11 active loaded**, one deprecated), **14 loaded passes** in
total, `tlou_complete`, `core/bundle_executor.py`, the thin bundle tools, and
fragment/bundle authoring tools.

Implemented in the current refactor:
1. ✅ Directional fragments rotate by deterministic wall normal/floor yaw, including `facing`/`axis` properties. A wall with air on both sides still has no semantic exterior signal, so one valid side is chosen deterministically.
2. ✅ Every emitted fragment position passes the common write choke. Each multi-block fragment instance is an atomic group; overlapping groups fail transitively rather than leaving half a vehicle.
3. ✅ Bundle orchestration moved from `tools/bundle.py` to
   `core/bundle_executor.py`; AIR merge semantics preserve sparse state.
4. `apply_bundle` region-mode double-application warning (`docs/fragment_system.md` §6).

**Done When**: canonical 8-pass building sequence on the fixture produces a visually coherent TLOU result; wall breaches punch through walls on all four faces; no never-touch block ever changes (pytest + in-game).

---

## Phase 6 — Style Learning 🚧

**Goal:** the reverse-engineering workflow (`docs/style_learning.md`): reference save → statistics → clusters → new fragments → new bundle.

### Tasks
1. `models/style_profile.py` — diagnostic artifact only (`ARCHITECTURE.md` §7); `apply_style_profile` is retired and must **not** be built.
2. `core/style_learner.py` — block distributions per surface class, vegetation/damage estimates, fragment-presence matching against the loaded library.
3. `core/segmentation/cluster_extractor.py` — `extract_block_clusters` algorithm (`docs/style_learning.md` §3).
4. Extend `tools/learning.py`: `learn_style`, `extract_block_clusters`.
5. `PICASSO_PROFILES_DIR` default `data/profiles/`.

### Done When
- `learn_style` on a TLOU-styled fixture area returns non-zero vegetation/damage and ≥1 matched fragment.
- `extract_block_clusters` on an area with 5+ hand-placed repeated decorations returns those clusters with correct occurrence counts (pytest fixture).
- An agent following `docs/style_learning.md` §9 end-to-end produces a working bundle (manual walkthrough).

---

## Phase 7 — NPC Marker Interface ✅ *(P0-stabilized)*

Built: direct empty-target/native block-entity check, common choke validation,
saved structure-void write, atomic companion JSON, and best-effort rollback on
metadata failure.

Marker-position protection is enforced at the current write choke point. The
marker block and companion JSON are committed and reverted as one Phase 8
compound journal transaction.

**Done When** (met): marker file with correct schema appears; block placed in-world.

---

## Phase 8 — Safety: Journal & Revert ✅

**2026-07-10 status:** durable pending/commit/failure state transitions,
saved-state verification, conflict-safe revert, list/inspect/revert tools, and
NPC block+JSON compound artifacts are implemented. Player protection is wired
through style and bundle writes, including `include_player_built` governance.

**Goal:** `ARCHITECTURE.md` §12.3. Every non-dry apply writes a reverse diff; `revert_last_apply` restores it.

### Tasks
1. ✅ `core/journal.py`: durable reverse diffs, entry listing/inspection, conflict-safe replay.
2. ✅ Automatic journal transactions for pass, bundle, and marker write paths.
3. ✅ `list_journal_entries`, `inspect_journal_entry`, and `revert_last_apply`.
4. ✅ Tool/argument/seed/timestamp metadata plus compound artifact snapshots.

### Done When
- apply → revert → world block-identical to before (pytest on fixture via bridge, plus in-game spot check).
- `apply_bundle` writes one journal entry per pass write, revertible newest-first.

---

## Dependency & Environment Notes

- **`scipy` is a hard dependency of segmentation** (flood fill) and must be added to `pyproject.toml` when segmentation work starts — it is *not* currently listed.
- Use CPython 3.11 and `uv sync --extra test`; run tools with `uv run picasso`.
- The C `noise` library stays an optional `perlin` extra; the built-in fallback
  keeps every base feature functional. Pin one backend per world when
  cross-machine reproducibility matters.
- `amulet-core==1.9.41` and `mcp==1.28.1` are exact pins. Upgrade them only with
  the temporary-world integration suite and a renewed Phase 1.5 in-game check.
