# Implementation Order & Status — Base System

> Build sequence for the base system, updated for v0.4. Each phase produces something testable before the next begins. Status: ✅ done · 🔁 done but revisit in refactor · 🚧 not built.
>
> Segmentation build sequence: `docs/segmentation_implementation_phases.md`.
> Read `ARCHITECTURE.md` fully before touching anything.

**Standing rule for every phase:** never point Picasso at the production `UtilWeDie-Neo-1.21.1` save. Work on copies until journal/revert (`ARCHITECTURE.md` §12.3) lands. A small purpose-built test world (flat terrain, a few vanilla buildings with furniture) is the standard fixture.

**Testing stance (revised in v0.4):** the layering rule exists precisely so `core/` is unit-testable without MCP. Deterministic algorithms (surface classification, position-hash rolls, fragment rotation, pattern matching) get **pytest** tests with synthetic `RegionData` fixtures — each phase's "Done When" doubles as its test list. In-game visual verification remains required for anything touching a real world file.

---

## Phase 1 — Project Skeleton & Amulet Bridge ✅

**Goal:** the server starts, opens a world, reads a region, prints a block count.

Built: `pyproject.toml`, `config.py`, `models/block.py`, `models/region.py`, `core/amulet_bridge.py`, `session.py`, `server.py`, `tools/world_io.py` (`set_world`, `read_region`).

**Done When** (met): server starts; `set_world` on a 1.21.1 save returns `ok`; `read_region(0,0,2)` returns non-zero `block_count`.

---

## Phase 1.5 — Modded-Block Round-Trip Spike 🚧 **← gating, do before further polish**

**Goal:** verify the project's load-bearing assumption (`ARCHITECTURE.md` §4.2): a `doomsday:*` block written via Amulet survives save → open in Minecraft 1.21.1 + DD mod → renders correctly, with block state properties intact.

### Tasks
1. On a **copy** of a world: `place_block` one DD block (simple), one DD block with properties (e.g. facing), one vanilla block as control.
2. `level.save()`, close, open in the actual game, screenshot/verify.
3. Record the result in this file. If it fails: investigate Amulet's universal-format handling of unknown namespaces before writing any further fragment/pass content that uses DD blocks.

### Done When
- All three blocks verified in-game, or a documented failure analysis with a fallback plan (structure-block staging / MCEdit-schematic export / direct region-file NBT).

---

## Phase 2 — Catalog Index ✅ *(code)* / 🚧 *(data)*

Built: `core/catalog_index.py`, `tools/catalog.py` (`query_catalog`), startup load into `session.catalog`.

**Outstanding:** `data/catalog/doomsday_decoration_semantic.json` has not been copied from the DD mod source tree. Until it is, `query_catalog` correctly returns `catalog_not_loaded` (resilient startup). Copy (don't symlink — Windows) as part of environment setup, not code.

**Done When** (partially met): `query_catalog({"category": "furniture"})` returns DD furniture; empty filter returns all 1143 entries.

---

## Phase 3 — Region Analysis ✅

Built: `core/surface_classifier.py` (see normative spec `ARCHITECTURE.md` §4.5 — verify implementation matches the priority table exactly, esp. the `ceiling` rule 🔁), `core/pattern_matcher.py`, 6 pattern JSONs, `tools/analysis.py` (`analyze_region`).

🔁 Revisit: pattern matching must check all 4 rotations (`docs/style_pass_schema.md` §3); confirm and add pytest fixtures for rotated furniture.

**Done When** (met): non-zero counts for ≥3 surface classes on an urban fixture; ≥1 chair/table detected where present.

---

## Phase 4 — Style Engine & Core Style Tools ✅

Built: `models/style_pass.py` (type-discriminated), `core/noise_field.py` (C lib optional + deterministic fallback, `ARCHITECTURE.md` §4.6), `core/style_engine.py` (three-type dispatcher), pass auto-load, `tools/style.py` (`list_passes`, `preview_pass`, `apply_pass`, `create_pass`).

🔁 Revisit in refactor:
- All stochastic decisions must use the position-hash scheme (`ARCHITECTURE.md` §6) — audit for any iteration-order dependence.
- `preview ≡ apply` diff equality gets a pytest property test.
- Write choke point (`ARCHITECTURE.md` §12.1) — currently guards are scattered per-engine; consolidate.

**Done When** (met): `list_passes` returns the registry; `preview_pass` > 0 changes on fixture; `apply_pass` writes and the world shows mossy stone in-game.

---

## Phase 5 — Fragment System ✅ *(initial)* / 🔁

Built: `models/fragment.py`, `core/fragment_library.py`, `core/fragment_engine.py`, 10 fragment JSONs, 7 fragment passes + 1 pattern pass + `tlou_material_hints` block pass, `tlou_complete` bundle, `tools/bundle.py` (`list_bundles`, `apply_bundle` region mode), `tools/learning.py` (`list_fragments`, `create_fragment`, `create_bundle`).

🔁 Revisit in refactor (spec'd in v0.4, not yet implemented):
1. **Orientation** (`docs/fragment_system.md` §4): `orientable` field, wall-normal rotation, yaw variety on floor anchors, `facing`/`axis` property remapping. Until this lands, wall fragments (`wall_breach`, `window_overgrown`, `vine_anchor`) are only correct on south-facing walls — treat their output as placeholder.
2. **Write choke point** for fragment blocks (target-position never-touch + marker protection + destructive air-gate).
3. Move bundle orchestration from `tools/bundle.py` to `core/bundle_executor.py` (layering rule).
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

## Phase 7 — NPC Marker Interface ✅

Built: `tools/npc.py` (`place_npc_marker`), marker dir creation, structure_void placement.

🔁 Revisit: marker-position protection must be enforced at the write choke point once Phase 8 lands.

**Done When** (met): marker file with correct schema appears; block placed in-world.

---

## Phase 8 — Safety: Journal & Revert 🚧 **← next priority after refactor**

**Goal:** `ARCHITECTURE.md` §12.3. Every non-dry apply writes a reverse diff; `revert_last_apply` restores it.

### Tasks
1. `core/journal.py`: journal write (before-states captured from the region snapshot at apply time), entry listing, revert replay.
2. Wire into the write choke point so journaling is automatic for every write path (pass, bundle, marker).
3. `revert_last_apply` MCP tool; error `journal_empty` → add to §11 code list.
4. Journal entries include tool name, argument summary, seed, timestamps.

### Done When
- apply → revert → world block-identical to before (pytest on fixture via bridge, plus in-game spot check).
- `apply_bundle` writes one journal entry per pass write, revertible newest-first.

---

## Dependency & Environment Notes

- **`scipy` is a hard dependency of segmentation** (flood fill) and must be added to `pyproject.toml` when segmentation work starts — it is *not* currently listed.
- The C `noise` library stays an **optional extra** (`pip install picasso[perlin]`); Windows needs MSVC build tools for it. The built-in fallback keeps every feature functional without it. The two backends differ per-seed (`ARCHITECTURE.md` §4.6) — pin one backend per world if long-term reproducibility across machines matters.
- Amulet-core version is pinned loosely (`>=1.0`); the bridge isolates all API drift (`ARCHITECTURE.md` §4.2).
