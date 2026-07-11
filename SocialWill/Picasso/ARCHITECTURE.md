# Picasso — Architecture Reference

**Document version: v0.4.4** (2026-07-07)
**Implementation status audited:** 2026-07-10 (current safety/read/tool substrate)

> **For coding agents:** Read this document in full before writing any code. Every design decision here is intentional. If you find a conflict or ambiguity, leave a `TODO(arch):` comment and proceed with the most literal interpretation of this document.
>
> **Document precedence:** This document is authoritative for cross-cutting contracts (layering, pass type system, determinism, safety). Supplements in `docs/` are authoritative for their own subsystem's details; each supplement header states which ARCHITECTURE.md version it tracks. On conflict: the doc tracking the higher version wins; same version → this document wins. `README.md` is marketing-level and never authoritative.

### Changelog

| Version | Scope |
|---|---|
| v0.1 | Initial architecture: block-replacement Style Passes, 6 TLOU passes, learning via StyleProfile |
| v0.2 | Semantic segmentation supplement (`docs/semantic_segmentation.md`) |
| v0.3 | Fragment-first redesign (`docs/fragment_system.md`): Fragments become the primary mechanism, block passes demoted to texture accents |
| v0.4 | **Consolidation.** Pass Type System formalized (§5); surface classification formalized (§4.5); determinism spec (§6); safety & reversibility spec (§12); unified tool registry with status (§8); unified config (§10); Zone concept **cut**; `apply_style_profile` **retired**; legacy v0.1 passes marked deprecated |
| v0.4.1 | Adversarial review round 1 (REVISION_LOG §E): write choke point `write_context` table, fragment orientation, noise-backend disclosure, Phase 1.5 gate, marker snapshot, structure identity IoU, `candidate_groups`, per-type intensity — 15 findings |
| v0.4.2 | Round 2 (REVISION_LOG §F): `pattern_clear` context (E10 regression fix), `PICASSO_NOISE_BACKEND` session pinning, `PICASSO_MODDED_WRITE_VERIFIED` blocking gate, halo → Phase 3.5, fragment_pass required fields, fingerprint algorithm aligned to shipped schema — 10 findings, 2 retractions |
| v0.4.3 | Round 3 (REVISION_LOG §G): **block taxonomy** (air/air-like/liquid/solid, §4.5) and **choke-point air-transparency rule** (§12.1) — the two foundation fixes; leaves `persistent` injection + game-physics authoring checklist; structure-scoped apply semantics (dry_run, internal tiling); `partial` structures + containment-ratio identity; `create_*` collision semantics — 8 findings |
| v0.4.4 | Round 4 (REVISION_LOG §H) + v0.5 spec integration: player-protection choke-point row + governance gate (H1–H4); `detected`/`authored` registry namespaces (H8); Brush/Room system integrated (§8/§10–§12 ← docs/brush_room_system.md, round-5 signed off); work-order queue + `on_behalf_of` attribution (docs/wargame_interface.md); player-activity pipeline (docs/player_activity_pipeline.md); `room_envelope` air-write pass-through; journal tag fields; FragmentEngine `initial_placed_anchors`; safe_blocks 17→695, taxonomy + palette data shipped |
| 2026-07-10 implementation sync | Current substrate: native Java/sparse-palette reads with horizontal halo and vertical context; transactional writes; strict definitions/bundles; durable journal/revert including NPC compound artifacts; player-activity protection wiring; fragment/pattern atomic groups; conservative local stair/storey candidates; bounded Agent-readable voxel evidence; MCP semantic-review instructions/prompt; advisory locks and 22 serialized tools. This is not completion of Phase 1.5, segmentation/structure mode, Brush/Room, work orders, style learning, static-ruin capability gating, Create rail replacement, or functional machinery. No DD/Create authorization is implied; the mechanical split is specified in `docs/mechanical_structures.md`. |

---

## 1. Philosophy & Mental Model

### The Impressionist Metaphor

World stylization in Picasso works like building up an oil painting in **ordered layers**:

- A **Fragment** is one brushstroke — a small compositional event (a rubble pile, a breached wall, an overgrown window) that tells a micro-story. Fragments are the *primary* stylization mechanism. **Decay is a structural story, not a color change.**
- A **Style Pass** is one layer of paint — it sweeps a surface with a single concern. There are three pass types (§5): fragment placement, block-texture replacement, and furniture pattern replacement.
- A **Style Bundle** is the full painting plan: an ordered pass sequence per structure type.

### On composability (revised in v0.4)

v0.1 claimed passes were order-independent. That claim is now **scoped**:

- **Non-destructive block passes** are approximately commutative with each other (statistically identical results modulo noise).
- **Fragment passes are order-sensitive.** A destructive fragment (wall collapse) changes the surface classification that later passes read. Bundles therefore define a **canonical order**: material hints → structural events (destructive) → vegetation → interior scatter → furniture replacement. Do not reorder bundle entries casually; re-running a bundle is *not* idempotent.
- Block passes are capped by design intent at **≤15% surface coverage** — they are texture accents. Fragments carry the story.

### Core Design Principles

1. **World path is always dynamic.** The calling AI confirms the save path via `set_world()`. Nothing is hardcoded. The server is world-agnostic.
2. **Preview before write.** Every destructive tool has a dry-run mode (`preview_pass`, `apply_bundle(dry_run=true)`). Agents should preview before applying.
3. **Non-destructive by default.** Passes only touch "decoration-safe" blocks (`data/safe_blocks.json`). Air-writes (block removal) are allowed **only** from a fragment/pass that declares `destructive: true`, and even then never on `structural_never_touch` blocks or protected markers (§12).
4. **Catalog-driven.** Replacement vocabulary comes from the DD semantic catalog. The server never invents block IDs. Agents query the catalog to discover valid replacements.
5. **Style as a reusable artifact.** Fragments, Passes, and Bundles are JSON files. They can be saved, versioned, and shared between agents.
6. **Deterministic by construction.** Same (world state, pass, intensity, seed) → same diff, always. All randomness is derived from position hashing, never from iteration order or wall clock (§6).
7. **Durably reversible.** Every non-dry apply first publishes a pending reverse diff and then records commit/failure; revert is conflict-safe (§12.3). This does not replace dry-run or live-world coordination.

---

## 2. System Architecture

```
┌─────────────────────────────────────────────┐
│         MCP Client (Hermes / VSCode Codex)  │
└───────────────────┬─────────────────────────┘
                    │ stdio (MCP protocol)
┌───────────────────▼─────────────────────────┐
│              MCP Server (server.py)          │
│   FastMCP instance — registers all tools    │
└──┬───────────┬──────────────┬───────────────┘
   │           │              │
┌──▼──────┐ ┌──▼─────────┐ ┌──▼──────────────┐
│ tools/  │ │ tools/     │ │ tools/          │
│ world_io│ │ catalog    │ │ style · bundle  │
│ analysis│ │ learning   │ │ npc             │
└──┬──────┘ └──┬─────────┘ └──┬──────────────┘
   │           │              │
┌──▼───────────▼──────────────▼──────────────┐
│                  core/                      │
│  AmuletBridge   CatalogIndex   StyleEngine │
│  PatternMatcher FragmentEngine NoiseField  │
│  FragmentLibrary SurfaceClassifier         │
│  BundleExecutor  WriteChoke  WorldLock        │
│  BlockTaxonomy  segmentation/(planned)        │
└──┬───────────────────────────┬─────────────┘
   │                           │
┌──▼──────────────┐   ┌────────▼────────────┐
│  Amulet-Core    │   │   data/ (static)    │
│  MC World Files │   │  catalog · passes   │
│  (disk I/O)     │   │  patterns · fragments│
└─────────────────┘   │  bundles · safe     │
                      └─────────────────────┘
```

**Layering rule:** `tools/` modules may only import from `core/`, `models/`, `config`, and `session`. They must never import Amulet directly, and must contain no business logic beyond input validation and response shaping. `core/` modules must never import `mcp`. This separation allows unit-testing core logic without an MCP session.

Bundle orchestration was extracted to `core/bundle_executor.py` during the
2026-07-10 stabilization; `tools/bundle.py` is now the MCP-facing wrapper.

---

## 3. Source Layout

Status markers: ✅ implemented · 🚧 planned (spec exists, not built) · ⚠️ deprecated

```
src/picasso/
├── server.py               ✅ Entry point. SingleFlightFastMCP registers 22 tools + 1 prompt.
├── prompts.py              ✅ Server guidance + read-only structure interpretation prompt
├── config.py               ✅ Env-var config, exposes a Config singleton.
├── session.py              ✅ Server-level session state (see §4.1).
│
├── tools/
│   ├── world_io.py         ✅ set_world, close_world, read_region (+ cache/world lock)
│   ├── analysis.py         ✅ analyze_region
│   ├── inspection.py       ✅ inspect_volume (bounded palette/RLE voxel evidence)
│   ├── rail.py             🚧 high-priority RailTemplate/RailNetwork preview/native apply tools
│   ├── mechanical.py       🚧 later MechanicalTemplate/Instance preview/apply/remove tools
│   ├── catalog.py          ✅ query_catalog
│   ├── style.py            ✅ list_passes, preview_pass, apply_pass, create_pass
│   ├── bundle.py           ✅ list_bundles, apply_bundle (thin wrapper over BundleExecutor)
│   ├── learning.py         ✅ list_fragments, create_fragment, create_bundle
│   │                       🚧 learn_style, extract_block_clusters
│   ├── npc.py              ✅ place_npc_marker (empty-target read, choke, saved write, atomic JSON)
│   ├── diagnostics.py      ✅ describe_capabilities
│   ├── journal.py          ✅ list/inspect/revert journal entries
│   ├── activity.py         ✅ query_player_activity, get_activity_site
│   └── segmentation.py     🚧 detect_structures, list_structures, get_structure,
│                              annotate_structure, apply_pass_to_structure, apply_pass_by_type
│
├── core/
│   ├── amulet_bridge.py    ✅ Native Java reads; transactional saved writes; StringTag properties
│   ├── world_lock.py       ✅ Per-world advisory process lock
│   ├── write_choke.py      ✅ Read-envelope/block-entity/safety/catalog validation
│   ├── block_taxonomy.py   ✅ air / air-like / liquid / solid predicates
│   ├── surface_classifier.py ✅ Surface + space classification (§4.5)
│   ├── stair_semantics.py  ✅ Local stair-assembly candidates (not structure segmentation)
│   ├── storey_semantics.py ✅ Connected horizontal-level candidates (not ordinal floors)
│   ├── volume_inspector.py ✅ Deterministic bounded voxel evidence encoder
│   ├── create_track_semantics.py ✅ Exact-version target track-state/BE-coordinate evidence
│   ├── vanilla_rail_graph.py ✅ Read-only vanilla source ports/components/partial evidence
│   ├── rail/               🚧 high-priority graph solver, template registry, native executor client
│   ├── mechanical/         🚧 later site solver, template registry, game-side executor client
│   ├── style_engine.py     ✅ Pass dispatcher: block_pass / fragment_pass / pattern_replace (§5)
│   ├── fragment_engine.py  ✅ Fragment placement (docs/fragment_system.md)
│   ├── fragment_library.py ✅ Loads and indexes Fragment JSON files
│   ├── pattern_matcher.py  ✅ Scans a region for vanilla furniture pattern templates
│   ├── catalog_index.py    ✅ Loads DD catalog JSON, provides query interface
│   ├── noise_field.py      ✅ Perlin noise (C lib) with built-in deterministic fallback (§4.6)
│   ├── bundle_executor.py  ✅ Ordered region-mode bundle orchestration
│   ├── journal.py          ✅ Durable pending/commit/failure journal + conflict-safe revert (§12.3)
│   ├── build_log_reader.py ✅ Player activity query/cluster substrate
│   ├── style_learner.py    🚧 StyleProfile extraction (docs/style_learning.md)
│   ├── zone_registry.py    ⚠️ CUT in v0.4 — structures are targeted directly (§8, semantic_segmentation.md)
│   └── segmentation/       🚧 Detection pipeline package (docs/semantic_segmentation.md §9)
│
├── models/
│   ├── block.py            ✅ BlockPos, BlockState dataclasses
│   ├── region.py           ✅ RegionData + read envelope, block-entity and surface/space metadata
│   ├── style_pass.py       ✅ Flat StylePass model + loader type validation, StyleRule, NoiseConfig
│   ├── fragment.py         ✅ Fragment, FragmentBlock Pydantic models
│   ├── static_prefab.py    🚧 optional stateless exact-layout artifact for SR1
│   ├── rail.py             🚧 high-priority RailTemplate / RailNetwork / native receipt
│   ├── mechanical.py       🚧 later MechanicalTemplate / MechanicalInstance / ports / receipts
│   ├── structure.py        🚧 Structure, BoundingBox, StructureCandidate
│   └── style_profile.py    🚧 StyleProfile (diagnostic artifact only, §8 note on retirement)
│
└── data/
    ├── catalog/            ✅ Packaged 1143-entry DD semantic catalog
    ├── passes/             ✅ 14 loaded pass definitions
    ├── fragments/          ✅ 12 files; 11 active fragments loaded (1 deprecated skipped)
    ├── bundles/            ✅ tlou_complete.json
    ├── patterns/           ✅ 8 patterns (2 experimental and skipped by default)
    ├── static_prefabs/     🚧 optional SR1 stateless exact-layout manifests
    ├── rails/              🚧 high-priority RailTemplate/native artifact manifests
    ├── mechanical/         🚧 later functional template artifacts/manifests
    ├── safe_blocks.json    ✅ 695 replaceable IDs + structural_never_touch blacklist
    └── structure_fingerprints.json ✅ (consumed by 🚧 segmentation)
```

2026-07-10 verification baseline: **407 passed, 1 skipped, 14 subtests**. The
isolated wheel smoke loads 1143 catalog entries,
14 passes, 11 fragments, and 1 bundle. Amulet integration uses generated
temporary worlds, never the production save.

---

## 4. Component Specifications

### 4.1 `session.py` — Server Session State

Holds the single mutable state object shared across all tool calls within one MCP session.

```python
@dataclass
class PicassoSession:
    world_path: Path | None = None       # Set by set_world()
    bridge: AmuletBridge | None = None   # Opened Amulet level handle
    catalog: CatalogIndex | None = None  # Loaded at startup (may be None → catalog_not_loaded)
    pattern_matcher: PatternMatcher | None = None
    fragment_library: FragmentLibrary | None = None
    pass_registry: dict[str, StylePass] = field(default_factory=dict)
    bundle_registry: dict[str, dict] = field(default_factory=dict)
    last_region: RegionData | None = None  # Read cache, see invalidation rules below
    operation_lock: Lock                   # Process-local MCP single-flight guard
    world_lock: WorldLock | None = None    # Cross-process advisory lock for the active save
```

The session is a module-level singleton (`from picasso.session import session`).

**Cache invalidation rules (normative):**
- `last_region` is reused only when `(cx, cz, radius_chunks, resolved_y_min, resolved_y_max)` match exactly.
- Any successful non-dry write (`apply_pass`, `apply_bundle`, `place_npc_marker`, or `revert_last_apply`) **must** set `last_region = None`. A preview never invalidates.
- `set_world()` closes any existing bridge and clears `last_region`.

**Concurrency (implemented):** `SingleFlightFastMCP` wraps every synchronous tool
handler in `session.operation_lock`; async tool registration is rejected. This is
an explicit process-local guarantee rather than an assumption about stdio or MCP
SDK scheduling. `set_world` additionally acquires a filesystem-backed advisory
lock for the resolved world path, so a second Picasso process returns
`world_locked` instead of opening the same save. The world lock cannot exclude
Minecraft or third-party editors, so operational coordination is still required.

### 4.2 `core/amulet_bridge.py` — Amulet Wrapper

All Amulet-Core imports are confined to this file. If Amulet's API changes, only this file changes.

**Interface:**

```python
class AmuletBridge:
    def __init__(self, world_path: str) -> None: ...

    def read_region(self, cx: int, cz: int, radius: int,
                    y_min: int | None = None,
                    y_max: int | None = None) -> RegionData: ...
    # Returns native Java BlockState values plus the trusted read envelope.

    def read_block_with_entity(self, x: int, y: int, z: int) -> tuple[BlockState, bool]: ...
    # Direct native-version read. Errors raise; they are never converted to air.

    def write_region(self, changes: RegionData) -> int: ...
    # Preconstructs every target, snapshots native block/entity states, applies
    # the batch, and saves. On failure it rolls back attempted positions; an
    # incomplete rollback poisons the bridge until the world is reopened.

    def place_block(self, x: int, y: int, z: int, block_state: BlockState) -> None: ...

    def close(self) -> None: ...
```

**Amulet usage notes:**
- Open a level: `amulet.load_level(world_path)`.
- Picasso reads through Amulet's versioned accessor using the pinned world's native Java version. Universal-only IDs are rejected rather than mislabeled as canonical `minecraft:*` states.
- `BlockState.properties` remain strings at Picasso's boundary; the bridge encodes them as Amulet NBT `StringTag` values. Temporary-world save/reopen tests cover `axis`, `facing`, and choke-injected `persistent` properties.
- World height range for 1.21.1 is **−64…319**. The bridge must not assume 0…255.
- `RegionData` records resolved Y bounds, exactly loaded chunks, halo positions, and positions containing block entities. Missing chunks are reported and remain outside the trusted write envelope.
- Block-entity NBT is not exposed in `RegionData`; such positions are therefore unconditionally protected at the write choke point rather than overwritten without a restorable snapshot.
- Always operate in `"minecraft:overworld"` unless otherwise specified.
- The supported dependency set is CPython `>=3.11,<3.12`, `amulet-core==1.9.41`, and `mcp==1.28.1`; upgrades must accompany the temporary-world bridge suite.

> **Phase 1.5 remains open:** the controlled `Picasso-Test1` save now contains `doomsday_decoration:acrate`, `doomsday_decoration:accessorybox_1[facing=east]`, and a vanilla diamond-block control. All three survived Amulet save/close/reopen at the recorded coordinates. This is **not** an in-game DD rendering, facing, collision or interaction check. `PICASSO_MODDED_WRITE_VERIFIED` must remain false until Minecraft 1.21.1 with the installed DD mod performs that check.
>
> The exact chunk/block accessor API differs between amulet-core versions. The bridge pins whatever API the installed version exposes; **the exact calls are an implementation detail of this file** and deliberately not specified here.

### 4.3 `core/catalog_index.py` — DD Catalog Query

Loads `doomsday_decoration_semantic.json` at startup. Provides a fast in-memory query interface.

```python
class CatalogIndex:
    def __init__(self, catalog_path: str | list[str]) -> None: ...
    def query(
        self,
        category: str | None = None,
        surface: list[str] | None = None,
        context: list[str] | None = None,
        tags: list[str] | None = None,
        function: str | None = None,
    ) -> list[dict]: ...
    # All params are optional AND-filters. surface/context/tags match any-of within the field.

    def get_by_id(self, block_id: str) -> dict | None: ...
```

Catalog entry shape (from the existing DD file):

```json
{
  "id": "doomsday:rusted_chair",
  "name": "锈蚀椅子",
  "category": "furniture",
  "surface": ["floor"],
  "context": ["indoor", "outdoor"],
  "function": "decorative_seating",
  "footprint": "1x1",
  "oversized": false,
  "tags": ["metal", "rusted", "seating"],
  "desc": "单格椅子，末日风格锈蚀金属"
}
```

### 4.4 `core/pattern_matcher.py` — Vanilla Furniture Detection

Scans a `RegionData` for known vanilla furniture patterns (small 3D block templates in `data/patterns/*.json`; schema in `docs/style_pass_schema.md`).

```python
class PatternMatcher:
    def __init__(self, patterns_dir: str) -> None: ...
    def find_matches(self, region: RegionData) -> list[PatternMatch]: ...

@dataclass
class PatternMatch:
    pattern_name: str
    anchor_pos: BlockPos
    blocks: list[BlockPos]
    dd_replacement: str | None
```

### 4.5 `core/surface_classifier.py` — Surface & Space Classification

Every solid block in a region gets exactly one **surface class**. This spec is normative; the v0.1 prose heuristics are superseded.

#### Block taxonomy (normative — the foundation every predicate below stands on)

Every block state falls into exactly one of four categories:

| Category | Membership | Role in classification | Role at the write choke point |
|---|---|---|---|
| **air** | `minecraft:air`, `minecraft:cave_air`, `minecraft:void_air` | Is air, everywhere | Writing here = "adding to air" (§12.1 air-transparency rule) |
| **air-like** | Non-collision decorations: plant family (grass, ferns, flowers, saplings, vines — **not** `*_leaves`, which are solid, see canopy note below), plus `torch`, `*_torch`, `lantern` (hanging deco), `*_carpet`, `snow` (layers 1–7), `rail` family, `*_button`, `lever`, `tripwire`, `string`, `*_sign` | **Transparent to classification predicates**: `above_air`/`side_air`/`sky_open` treat air-like positions as air. A carpeted floor is still a floor; the carpet itself is never classified (it is not a surface) | Replaceable per whitelist as usual; **never a valid fragment anchor** (a rubble pile must not anchor on a torch — anchor candidates require category solid) |
| **liquid** | `water`, `lava` (+ waterlogged property noted below) | **Opaque to classification**: a submerged block has `above_air = false` → underwater terrain is not classified as floor and is not decorated. This is a deliberate v0.4 decision, not an accident — underwater styling (flooded streets, sunken ships) is deferred to the water-structure subsystem (🚧 S3), which will own its own anchoring rules | Never written to, never a write target (liquids are not in any whitelist) |
| **solid** | Everything else — including `*_leaves` (they have collision and form canopies; see the tree-canopy note below), glass, slabs, stairs, fences | Participates in classification normally | Normal choke-point rules |

Membership is data, not code: `data/block_taxonomy.json` ships the air-like list (extensible for mod blocks; DD blocks default to solid unless listed). `BlockState.is_air` covers the air category only; the classifier consults the taxonomy for air-like transparency.

**Waterlogged property:** a `waterlogged: true` solid block is solid for classification (the block is there), but authors should treat it as wet context — no engine special-case in v0.4.

**Tree-canopy caveat (known, accepted):** leaves are solid, so a street-tree canopy blocks `sky_open` for wall segments behind it → those walls read `inner_wall`/interior and exterior-filtered passes skip them. Tier-2 flood-fill space classification (Phase S1) resolves this correctly (the air under a canopy connects to `outdoor_sky`); until then this is a known tier-1 artifact — one more reason tier reporting is mandatory. Treating leaves as air-like was considered and rejected: it would misclassify the *inside* of dense forests as sky-open and would make leaf-block rooftops (roof_tree_growth output) unanchorable.

**Definitions** (for a solid block at position `p`; positions outside the read region count as **air**; air-like positions count as **air** in every predicate below):

| Predicate | Meaning |
|---|---|
| `above_air(p)` | block at `p+ŷ` is air |
| `below_air(p)` | block at `p−ŷ` is air |
| `side_air(p)` | ≥1 of the 4 horizontal neighbors is air |
| `column_top(p)` | `p.y ≥ (highest solid Y in column (p.x, p.z)) − 1` |
| `sky_open(s)` | for an *air* position `s`: ≥3 of the 4 positions `s+1ŷ … s+4ŷ` are air (heuristic "column continues upward") |

**Classification — first matching rule wins (priority order is normative):**

| Priority | Class | Condition |
|---|---|---|
| 1 | `rooftop` | `above_air ∧ column_top` |
| 2 | `floor` | `above_air ∧ ¬below_air` |
| 3 | `ceiling` | `¬above_air ∧ below_air` (underside of a solid mass) |
| 4 | `outer_wall` | `side_air ∧ ∃ air neighbor s: sky_open(s)` |
| 5 | `inner_wall` | `side_air` |
| 6 | `embedded` | otherwise — **never targeted by any rule** |

Notes:
- A thin slab over a room is simultaneously a walkable top and a ceiling for the space below; the single label is decided by priority (top wins). Rules that need the underside use `surface: "ceiling"` or `direction: "below"` on an adjacent-place action.
- `read_region` consumes populated chunk-palette cells from the requested core plus a one-chunk horizontal halo and vertical context (`y_min−1` through `y_max+4`, clamped to world bounds). Halo/context blocks participate in neighbor classification but are excluded from modification by the trusted-envelope choke.
- **Space classification is two-tier:**
  - **Tier 1 — heuristic, always available:** `exterior = {rooftop, outer_wall}`, everything else `interior`. Crude but dependency-free.
  - **Tier 2 — flood-fill (🚧, after segmentation):** a block's space is the classification of its adjacent air volume (`outdoor_sky` → exterior; `indoor_room`/`underground_*` → interior; block adjacent to both → exterior).
  - Any tool honoring `space_filter` **must report which tier it used** in its response (`"space_classification": "heuristic" | "flood_fill"`) so agents can judge reliability.

### 4.6 `core/noise_field.py` — Organic Variation

Seeded 2D/3D noise sampler in `[0, 1]`, gating rule application so effects feel hand-painted.

```python
class NoiseField:
    def __init__(self, seed: int) -> None: ...
    def sample_2d(self, x: float, z: float, scale: float) -> float: ...
    def sample_3d(self, x: float, y: float, z: float, scale: float) -> float: ...
```

**Backend policy (normative):** two backends exist — the optional C library `noise` (`pnoise2/pnoise3, octaves=4, base=seed`) and the built-in deterministic hash-based value noise. The fallback is the **portable baseline** (no compiler needed on Windows); the C library is an optional accelerator. They produce *different* fields for the same seed, so `preview` and `apply` agree only when both ran on the same backend.

**Backend pinning (primary mechanism, normative):** `PICASSO_NOISE_BACKEND = auto | c | fallback` (default `auto`). Resolved **once at server startup**; it never changes within a session — installing the C library mid-session has no effect until restart. `auto` picks `c` if importable, else `fallback`; `c` fails startup with a clear error if the library is absent; `fallback` ignores the C library entirely. **Pin `fallback` when cross-machine reproducibility matters** (e.g. a world styled across several machines over months). The resolved backend is included in the `set_world` response, so the agent learns it once per session.

**Response reporting (secondary check):** `"noise_backend"` remains a **required field** in all `preview_pass`, `apply_pass`, and `apply_bundle` responses. Since the backend is session-pinned, a preview/apply mismatch can only occur *across* sessions or machines: if an agent resumes work in a new session and the reported backend differs from the one recorded alongside earlier previews, it must re-preview before applying.

### 4.7 `core/fragment_engine.py` + `core/fragment_library.py`

The primary stylization mechanism. Full spec: `docs/fragment_system.md` (data model, placement algorithm, **orientation/rotation rules**, safety guards).

### 4.8 `core/bundle_executor.py` (✅ region mode)

Loads bundle JSON, iterates entries, calls `StyleEngine`, validates each pass
through the common `WriteChoke`, and merges earlier pass changes (including AIR
removals) into the next pass's working snapshot. Structure mode remains planned.
Execution semantics (modes, error collection, seed precedence) are specified in
`docs/fragment_system.md` §6.

### 4.9 Agent-in-the-Loop Semantic Interpretation

Picasso deliberately separates block access, geometric detection, semantic
interpretation, and persistent truth. Amulet and deterministic core modules state
physical facts; an MCP-connected Agent may combine bounded evidence into ranked
semantic hypotheses; humans confirm novel, ambiguous, or high-impact intent; the
Structure Registry preserves stable `detected` and `authored` ownership; reusable
rules are promoted only from multiple confirmed examples with adversarial tests.

The implemented read path is progressive: `analyze_region` first returns compact
local candidates, then `inspect_volume` exposes an inclusive volume through a
canonical block-state palette and Y-layer/Z-row/X-run encoding. The Agent must
treat `local_semantics.scope=candidate_only`, truncation, and scan boundaries as
honesty constraints. Aggregate counts alone are never enough to name a room or
building. Whole-world block dumps are not an MCP surface.

FastMCP server instructions carry the universal candidate/no-implicit-write
rules. The explicit `interpret_world_structure` prompt expands the read-only
workflow and required response shape. Neither is a security boundary: write
safety remains enforced by the choke, journal, player protection, world locks,
and modded-write gate. Prompt functions remain static and never read the shared
world session; serialized tools own all bridge access.

Full responsibilities, evidence contracts, rule-promotion gates, and the
Mosslorn validation plan are normative in `docs/agent_semantic_review.md`.

### 4.10 Static Mechanical Ruins, Create Rails, Functional Machines (🚧)

The first mechanical milestone (SR1) is **static scenery**, not automation. Small
broken-machine compositions reuse Fragment; a future `StaticPrefab` is only a
larger exact block-state composition compiled through the same atomic
choke/journal path. Both strictly reject every block requiring a block entity or
NBT. Unknown capability metadata fails closed. Static output may look mechanical
but must never claim speed, stress, transport, inventory or commissioning.

The sole high-priority Create exception is bounded replacement of one reviewed
vanilla-rail component. Existing Create/Railways network adoption is later.
Track is a connected geometric network, so it uses dedicated `RailTemplate`
(straight/slope/curve geometry and typed endpoints) and `RailNetwork` graph
records. Create track connections may depend on BE/NBT/runtime APIs; therefore
all segment kinds use an exact-version game-native placement/rollback backend,
never raw Fragment, StaticPrefab or Amulet block writes. R2/R3 may exercise the
native executor only through an internal harness on disposable worlds. Public
MCP apply remains disabled until preview topology, durable rollback/receipt,
reload and bidirectional train-traversal acceptance pass.

The first R0 target-evidence slice is implemented in
`core/create_track_semantics.py`: it deterministically classifies the audited
Create 6.0.10 and Railways 0.2.0 required track IDs from `RegionData`, checks
`turn` against block-entity presence, excludes halo/unknown IDs and reports
curve payload as unavailable. It does **not** read BE NBT, `create_tracks.dat`,
or the vanilla source graph, and it is not an MCP tool.

`core/vanilla_rail_graph.py` implements the complementary source slice. It
parses the four vanilla rail IDs into exact doubled half-block ports, components
and proven terminals; slope high ends join `y+1` rails. Halo continuations,
missing chunk/Y evidence, non-reciprocal neighbours, multiple components,
waterlogged rails and powered/detector/activator semantic loss all block. It is
also pure/read-only and not registered as an MCP tool.

Running production lines, farms, elevators and contraptions are **later**. They
retain the versioned `MechanicalTemplate` + game-native executor +
`MechanicalInstance` + commissioning/removal design, but are not part of SR1.
Normative boundaries and revised phase order: `docs/mechanical_structures.md`.

---

## 5. Pass Type System

A **pass** is any JSON file in `data/passes/`. The top-level `type` field discriminates three schemas. `StyleEngine` is the single dispatcher.

| `type` | Mechanism | Engine path | Schema |
|---|---|---|---|
| `"block_pass"` *(default when omitted)* | Per-block texture replacement via match rules | `_apply_block_pass` | `docs/style_pass_schema.md` |
| `"fragment_pass"` | Place Fragment templates at surface anchors | `FragmentEngine` | `docs/fragment_system.md` §3 |
| `"pattern_replace"` | Replace matched vanilla furniture combos with DD blocks | `PatternMatcher` + mappings | `docs/style_pass_schema.md` |

The contract target is a Pydantic discriminated union on `type`; the 2026-07-10
implementation still uses one flat `StylePass` model plus explicit loader
validation. A `block_pass` JSON may omit `type` for v0.1 compatibility, and
missing per-type required fields still fail registry load and are logged/skipped.
Converting the model itself to a strict union remains refactor work.

### 5.1 `intensity` semantics (normative, per type)

`intensity ∈ [0.0, 1.0]`, default `1.0`, passed per call or per bundle entry:

| Pass type | Effect of intensity |
|---|---|
| `block_pass` | `effective_weight = clamp01(rule.weight × intensity)` per rule. Noise thresholds unchanged. |
| `fragment_pass` | `effective_density = clamp01(density × intensity)`. Per-block `probability` values unchanged — intensity scales *how many* fragments, not how ragged each one is. |
| `pattern_replace` | Probability that each individual pattern match is replaced = `clamp01(intensity)`. |

### 5.2 `space_filter` semantics (normative, per type)

`space_filter ∈ {"interior", "exterior", null}`, available on `preview_pass`, `apply_pass`, and bundle entries:

| Pass type | What is filtered |
|---|---|
| `block_pass` | Candidate blocks (a block outside the filter never matches any rule) |
| `fragment_pass` | Anchor candidates (fragment *blocks* may still spill across the boundary; footprint clearance does not re-check space) |
| `pattern_replace` | Pattern anchor block's space class |

Space classes come from §4.5's two-tier scheme; responses must state the tier used.

### 5.3 Seed precedence

`explicit tool-call seed` > `bundle default_seed` > global default `42`. (Per-bundle-entry seeds are deliberately not supported — one bundle run, one seed.)

### 5.4 Legacy passes (v0.1)

`tlou_decay_surfaces`, `tlou_nature_reclaim`, `tlou_street_debris` remain as standalone texture passes but are **not** part of `tlou_complete`. `tlou_structural_damage` and `tlou_vine_bridge` are **deprecated** — superseded by the `wall_breach` / `vine_anchor` fragment passes. Deprecated passes carry `"deprecated": true` in their JSON; `list_passes` surfaces the flag; applying one still works but the response includes a deprecation warning.

---

## 6. Determinism & Randomness (normative)

The preview-before-write principle is only meaningful if preview and apply agree exactly. Therefore:

1. **Every stochastic decision** (rule fires?, which replacement?, which fragment?, per-block probability, yaw selection) derives from
   `roll = H(seed ‖ stable_key) → [0, 1)` where `H` = SHA-256, first 8 bytes as uint64 / 2⁶⁴.
2. `stable_key` is built from stable identifiers only: pass name, rule index / stage tag, the block or anchor **world position**, and a purpose tag when one position needs multiple independent rolls.
3. **Forbidden:** shared `random.Random` streams, anything dependent on dict/set iteration order, wall-clock time, object ids.
4. **Guarantees that follow:** `preview(...)` ≡ diff of `apply(...)` for identical inputs; re-running on an unchanged world yields an identical diff; results are independent of block iteration order and of Python version.
5. Noise fields are seeded by the same seed but are a *spatial* gate, not a per-decision RNG. **Noise-gate determinism is per-backend** (see §4.6). The position-hash rolls in points 1–4 are fully portable; only the noise gate differs across backends. The backend is pinned per session via `PICASSO_NOISE_BACKEND` (§4.6), so within a session preview ≡ apply always holds; across sessions/machines, hold the backend constant (pin `fallback`) or re-preview.

---

## 7. Data Models

### `models/block.py`

```python
@dataclass(frozen=True)
class BlockPos:
    x: int; y: int; z: int

@dataclass
class BlockState:
    namespace: str                       # "minecraft", "doomsday"
    name: str                            # "stone_bricks"
    properties: dict[str, str] = field(default_factory=dict)

    @property
    def full_id(self) -> str: ...        # "namespace:name"
    @property
    def is_air(self) -> bool: ...
```

### `models/region.py`

```python
@dataclass
class RegionData:
    blocks: dict[BlockPos, BlockState]   # world snapshot is sparse; raw diffs may contain explicit AIR
    origin_cx: int
    origin_cz: int
    radius_chunks: int
    y_min: int | None
    y_max: int | None
    loaded_chunks: set[tuple[int, int]]
    halo_positions: set[BlockPos]
    block_entity_positions: set[BlockPos]
    surface_classes: dict[BlockPos, str] = field(default_factory=dict)  # filled by classifier
    space_classes: dict[BlockPos, str] = field(default_factory=dict)
    write_contexts: dict[BlockPos, str] = field(default_factory=dict)

    def get(self, pos: BlockPos) -> BlockState | None: ...
    def set(self, pos: BlockPos, state: BlockState) -> None: ...
    def modification_block_reason(self, pos: BlockPos) -> str | None: ...
    def is_modifiable(self, pos: BlockPos) -> bool: ...
    def bounding_box(self) -> tuple[BlockPos, BlockPos]: ...
```

The trusted read envelope is `(requested XZ bounds, resolved Y window,
loaded_chunks) - halo_positions`. A target outside it fails closed. Bundle
layering preserves the sparse-world invariant: merging an explicit AIR change
removes that position from `blocks` and clears stale surface/space/block-entity
metadata instead of storing AIR as if it were a solid world block.

> `TODO(arch)` (performance, refactor-scope): the sparse dict representation double-converts against Amulet's native palette arrays and balloons memory on dense urban regions. The refactor may switch RegionData's internal storage to chunk-aligned numpy arrays behind the same accessor API. The accessors above are the stable contract; the storage is not.

### `models/style_pass.py`

```python
class NoiseConfig(BaseModel):
    type: Literal["perlin"] = "perlin"
    scale: float = 0.05
    threshold: float = 0.4

class ReplaceOption(BaseModel):
    block: str
    properties: dict[str, str] = {}
    weight: float = 1.0

class StyleRule(BaseModel):
    match: dict                  # docs/style_pass_schema.md
    action: Literal["replace", "place_adjacent", "remove"]
    replace_with: list[ReplaceOption] | None = None
    place_block: str | None = None
    direction: str | None = None # "air_side", "above", "below"
    weight: float = 1.0
    noise: NoiseConfig | None = None

class StylePass(BaseModel):
    name: str
    description: str
    type: str = "block_pass"     # discriminator, §5
    version: str = "1.0"
    deprecated: bool = False
    targets: list[str] = []      # documentation only

    # --- block_pass fields ---
    rules: list[StyleRule] = []
    only_safe_blocks: bool = True

    # --- fragment_pass fields (full spec: docs/fragment_system.md §3) ---
    # REQUIRED when type == "fragment_pass" (validation, not schema defaults —
    # a silent default here is a footgun: density defaulting to 0.0 would make a
    # forgotten field produce an always-empty pass with no error, and a default
    # anchor_surface would silently override each fragment's own surface):
    fragments: list[str]                 # fragment names; must be non-empty
    anchor_surface: str                  # surface class to scan for anchors
    density: float                       # base fraction of anchors receiving a fragment
    # optional:
    min_spacing: int = 0                 # min XZ distance between placed anchors (this pass)
    only_safe_anchor_blocks: bool = True # anchor block must be in safe_blocks.replaceable

    # --- pattern_replace fields ---
    mappings: list[dict]                 # REQUIRED when type == "pattern_replace"; non-empty
```

The sketch matches the current flat model. Loader validation compensates for
its permissive defaults: a `fragment_pass` file missing `density`, `fragments`,
or `anchor_surface` fails registry load rather than loading as a silent no-op.
The strict per-type discriminated models remain the desired end state.

### `models/fragment.py` — see `docs/fragment_system.md` §2.

### `models/style_profile.py` (🚧, diagnostic only)

`StyleProfile` survives as the **read-only output** of `learn_style` — a statistical summary an agent reads before deciding which fragments/bundles to author. `apply_style_profile` is retired (§8); a profile is never executed directly. Fields as in v0.1 spec plus `matched_fragments` (docs/style_learning.md §7).

---

## 8. MCP Tool Registry (authoritative)

Status: ✅ implemented · 🚧 planned · ❌ retired

| Tool | Category | Status | Spec |
|---|---|---|---|
| `set_world` | World I/O | ✅ | docs/tool_specs.md |
| `close_world` | World I/O | ✅ | closes Amulet and releases the advisory world lock |
| `read_region` | World I/O | ✅ | docs/tool_specs.md |
| `analyze_region` | Analysis | ✅ | docs/tool_specs.md |
| `inspect_volume` | Evidence | ✅ read-only | docs/tool_specs.md + docs/agent_semantic_review.md |
| `query_catalog` | Catalog | ✅ | docs/tool_specs.md |
| `list_passes` | Style | ✅ | docs/tool_specs.md |
| `preview_pass` | Style | ✅ | docs/tool_specs.md |
| `apply_pass` | Style | ✅ | docs/tool_specs.md |
| `create_pass` | Style | ✅ block-pass only | docs/tool_specs.md — fragment passes authored via `create_fragment` + JSON |
| `list_fragments` | Fragment | ✅ | docs/style_learning.md §6 |
| `create_fragment` | Fragment | ✅ | docs/style_learning.md §4 |
| `list_bundles` | Bundle | ✅ | docs/fragment_system.md §6 |
| `create_bundle` | Bundle | ✅ | docs/style_learning.md §5 |
| `apply_bundle` | Bundle | ✅* | docs/fragment_system.md §6 — *implemented in region mode; structure mode 🚧 |
| `place_npc_marker` | NPC | ✅ | docs/tool_specs.md |
| `describe_capabilities` | Diagnostics | ✅ | complete current tool surface + safe workflow |
| `list_journal_entries` | Safety | ✅ | docs/tool_specs.md + §12.3 |
| `inspect_journal_entry` | Safety | ✅ | docs/tool_specs.md + §12.3 |
| `revert_last_apply` | Safety | ✅ | conflict-safe reverse replay, §12.3 |
| `query_player_activity` / `get_activity_site` | Sensing | ✅ | docs/tool_specs.md + docs/player_activity_pipeline.md |
| `learn_style` | Learning | 🚧 | docs/tool_specs.md + style_learning.md §7 |
| `extract_block_clusters` | Learning | 🚧 | docs/style_learning.md §3 |
| `scan_semantic_candidates` | Evidence | 🚧 read-only S4 | docs/structure_detection_tool_specs.md + docs/agent_semantic_review.md |
| `get_candidate_evidence` | Evidence | 🚧 read-only S4 | docs/structure_detection_tool_specs.md + docs/agent_semantic_review.md |
| `detect_structures` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `list_structures` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `get_structure` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `annotate_structure` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `apply_pass_to_structure` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `apply_pass_by_type` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `scan_review_markers` / `clear_review_markers` | Review | 🚧 v0.5 | docs/v05_forward_requirements.md §4 |
| `create_brush` / `list_brushes` / `capture_brush` / `apply_brush` | Brush | 🚧 v0.5 | docs/brush_room_system.md §2/§8 |
| `create_room_template` / `list_room_templates` | Room | 🚧 v0.5 | docs/brush_room_system.md §3 |
| `build_interior_graph` / `get_interior_graph` / `get_routes` | Graph | 🚧 v0.5 | docs/brush_room_system.md §4 |
| `add_room` / `remove_room` / `connect` / `seal` | Graph | 🚧 v0.5 | docs/brush_room_system.md §4–§5; dry_run default true |
| `recondition_room` / `refurnish_room` | Room | 🚧 v0.5 | docs/brush_room_system.md §6 — journal-gated |
| `list_rail_templates` / `preview_rail_replacement` | Create rail | 🚧 **high priority** | dedicated straight/slope/curve network path; docs/mechanical_structures.md |
| `list_rail_networks` / `inspect_rail_network` | Create rail | 🚧 R2 read-only recovery surface | durable records begin at `pending`, never at preview; docs/mechanical_structures.md |
| `apply_rail_replacement` | Create rail | 🚧 public game-native write | R2/R3 internal harness first; public tool disabled until R3 traversal acceptance |
| `list_mechanical_templates` / `preview_mechanical_structure` | Functional mechanical | 🚧 **later** | docs/mechanical_structures.md |
| `apply_mechanical_structure` / `remove_mechanical_structure` / `adopt_mechanical_structure` | Functional mechanical | 🚧 **later**, game-native backend | docs/mechanical_structures.md |
| `list_mechanical_instances` / `inspect_mechanical_instance` | Functional mechanical | 🚧 **later** | docs/mechanical_structures.md |
| `process_work_orders` / `list_work_orders` | Wargame | 🚧 v0.5 | docs/wargame_interface.md §8 |
| `apply_style_profile` | Learning | ❌ retired v0.4 | superseded by cluster → fragment → bundle workflow (docs/style_learning.md) |

There are **22 registered tools** in the 2026-07-10 implementation snapshot;
planned rows above are specifications and are not registered.

**MCP guidance registry:** the server publishes initialization `instructions`
covering semantic honesty and no implicit writes, plus one explicit
`interpret_world_structure` prompt. Prompts guide an Agent but do not grant data
access, mutate session state, or replace server-side safety enforcement.

**Zone concept is cut (v0.4).** v0.2 proposed promoting Structures to "Zones" as a separate registry. Nothing consumed zones: bundles target `structure_type`, tools target `structure_id`. Structures **are** the targeting handle; `models/zone.py` and `core/zone_registry.py` are removed from scope. (Structure registry loading lives with the segmentation package.)

---

## 9. NPC Marker Interface (Stub for Narrative Layer)

`place_npc_marker` performs a native single-block read and accepts only a real
air target with no block entity and no existing marker. It constructs a
single-point trusted `RegionData`, passes the change through `WriteChoke`, and
uses `write_region()` so the `minecraft:structure_void` block is actually saved.
The companion JSON is written to a same-directory temporary file and committed
with `os.replace`; if metadata commit fails after the world save, Picasso makes
a best-effort saved rollback to the verified before-state and reports the
rollback outcome. Negative coordinates use the `n` filename prefix documented
in `docs/tool_specs.md`.

**Marker file schema:**

```json
{
  "pos": {"x": 100, "y": 64, "z": -200},
  "npc_type": "key_npc",
  "faction": "survivor_camp",
  "facing": "south",
  "dialogue_id": null,
  "quest_id": null,
  "source_agent": null,
  "created_at": "2026-07-05T00:00:00Z"
}
```

`source_agent` maps to the simulation agent's username (ADR-D17 identity mapping in the SocialWill narrative system); `null` until the Narrative Layer populates it.

**Protection:** marker positions are immutable to all passes (§12.2). `structure_void` is additionally in `structural_never_touch` as defense-in-depth.

---

## 10. Configuration (authoritative table)

All configuration via environment variables (loadable from `.env` via `python-dotenv`). This table supersedes all config lists in supplements.

| Variable | Default | Used by | Description |
|---|---|---|---|
| `PICASSO_CATALOG_PATHS` | packaged `src/picasso/data/catalog/doomsday_decoration_semantic.json` | ✅ | `os.pathsep`-separated catalog paths, merged in order with collision warnings; legacy `PICASSO_CATALOG_PATH` remains accepted |
| `PICASSO_PASSES_DIR` | `src/picasso/data/passes/` | ✅ | Style Pass JSON files (all three types) |
| `PICASSO_PATTERNS_DIR` | `src/picasso/data/patterns/` | ✅ | Vanilla furniture pattern templates |
| `PICASSO_FRAGMENTS_DIR` | `src/picasso/data/fragments/` | ✅ | Fragment templates |
| `PICASSO_BUNDLES_DIR` | `src/picasso/data/bundles/` | ✅ | Style Bundles |
| `PICASSO_SAFE_BLOCKS` | `src/picasso/data/safe_blocks.json` | ✅ | Replaceable whitelist + never-touch blacklist |
| `PICASSO_BLOCK_TAXONOMY` | `src/picasso/data/block_taxonomy.json` | ✅ | air-like/liquid classification data |
| `PICASSO_FINGERPRINTS_PATH` | `src/picasso/data/structure_fingerprints.json` | 🚧 | Structure fingerprint library |
| `PICASSO_PROFILES_DIR` | `src/picasso/data/profiles/` | 🚧 | learn_style output |
| `PICASSO_LOG_LEVEL` | `INFO` | ✅ | Logging level |
| `PICASSO_NOISE_BACKEND` | `auto` | ✅ | `auto \| c \| fallback` — noise backend, pinned at startup (§4.6). Pin `fallback` for cross-machine reproducibility |
| `PICASSO_MODDED_WRITE_VERIFIED` | `false` | ✅ legacy gate | Current human-set global gate for Phase 1.5 DD writes. It is insufficient for multiple mod namespaces and must not authorize Create. Before loading any Create catalog, SR0/R0 replaces or augments it with the capability matrix below |
| `PICASSO_MOD_CAPABILITIES_PATH` | `src/picasso/data/mod_capabilities.json` | 🚧 SR0/R0 | Exact-version BE/NBT/stateless and backend capability matrix; DD verification never grants Create capability |
| `PICASSO_STATIC_PREFABS_DIR` | `src/picasso/data/static_prefabs/` | 🚧 conditional SR1 | Optional exact block-state-only ruins when Fragment is insufficient; all BE/NBT content forbidden |
| `PICASSO_RAIL_TEMPLATES_DIR` | `src/picasso/data/rails/` | 🚧 high-priority R1 | Straight/slope/curve RailTemplate manifests and native artifacts |
| `PICASSO_MECHANICAL_TEMPLATES_DIR` | `src/picasso/data/mechanical/` | 🚧 later M0+ | Functional MechanicalTemplate manifests and game-native artifacts |
| `PICASSO_BRUSHES_DIR` | `src/picasso/data/brushes/` | 🚧 | Brush JSON files (docs/brush_room_system.md §2) |
| `PICASSO_ROOM_TEMPLATES_DIR` | `src/picasso/data/room_templates/` | 🚧 | Room template JSON files (docs/brush_room_system.md §3) |
| `PICASSO_VARIANT_FATIGUE_RATIO` | `12` | 🚧 | usage/variant ratio that triggers `fatigue_warning` (docs/brush_room_system.md §3.3) |
| `PICASSO_BUILD_LOG_DIR` | *(unset)* | ✅ optional | Directory of server-plugin player-activity JSONL logs (docs/player_activity_pipeline.md). Unset → activity tools return `build_log_not_configured` and protection reports the source unavailable |
| `PICASSO_MAX_RADIUS_CHUNKS` | `12` | ✅ | Hard cap for read_region/analysis (§12.4) |
| `PICASSO_FLAT_VARIANCE_THRESHOLD` | `1.5` | 🚧 | Flat-region detector |
| `PICASSO_MIN_STRUCTURE_AREA` | `50` | 🚧 | Min XZ footprint (blocks²) kept by detection |
| `PICASSO_MIN_ROOM_VOLUME` | `12` | 🚧 | Min enclosed air volume (blocks³) |
| `PICASSO_MIN_STADIUM_VOLUME` | `2000` | 🚧 | Stadium special-case threshold |
| `PICASSO_GROUND_DETECTION_RADIUS` | `3` | 🚧 | Chunk radius for local ground-Y baseline |

**Not configurable (fixed per-world paths):** structure registry `<world>/picasso_structures.json`, markers `<world>/picasso_markers/`, journal `<world>/picasso_journal/`, room instantiation records `<world>/picasso_rooms.json` (🚧 v0.5), rail network registry `<world>/picasso_rail_networks.json` (🚧 high-priority R2/R3; preview plans are ephemeral, mutation records begin durably at `pending`), functional mechanical instance registry `<world>/picasso_mechanical.json` (🚧 later M0+), work-order queue `<world>/picasso_workorders/` incl. `window_log.json` (🚧 v0.5, docs/wargame_interface.md §2/§5), sync marker `<world>/picasso_sync.json` (🚧 v0.5 — written by sync infrastructure, read by `set_world`; docs/player_activity_pipeline.md §7.2). (v0.2's `PICASSO_STRUCTURES_DIR` env var is retracted — a per-world artifact must not live in static env config.) Multi-writer JSON files among these follow the read-merge-write + atomic-rename protocol (`docs/semantic_segmentation.md` §9).

**Resilient startup rule (normative):** the server must start and register all tools even if the catalog, passes dir, or fragments dir is missing or partially invalid. Missing resources degrade the affected tools to structured errors (`catalog_not_loaded`, `pass_not_found`, …); invalid individual JSON files are logged and skipped, never fatal.

**Fail-closed distinction (implemented):** resilient startup is not permission to
write without policy. A missing, invalid, or empty safety file makes every
validated write fail with `safety_policy_unavailable`. An empty catalog makes
`query_catalog` return `catalog_not_loaded`; any proposed non-vanilla ID absent
from all loaded catalogs blocks the whole operation with
`unknown_catalog_block`. Vanilla writes remain independent of catalog presence.

---

## 11. Error Handling Strategy

All tool handlers catch exceptions and return structured errors; exceptions never propagate to MCP:

```python
try:
    result = core_function(...)
    return {"ok": True, ...}
except FileNotFoundError as e:
    return {"ok": False, "error": "world_not_found", "message": str(e)}
except Exception as e:
    logger.exception("Unexpected error in tool X")
    return {"ok": False, "error": "internal_error", "message": str(e)}
```

**Standard error codes (complete list, v0.4):**
`world_not_found` · `world_not_set` · `world_locked` · `invalid_coordinates` · `invalid_y_window` · `region_too_large` · `pass_not_found` · `invalid_pass_definition` · `fragment_not_found` · `bundle_not_found` · `ambiguous_bundle_scope` · `pattern_not_found` · `structure_not_found` · `catalog_not_loaded` · `unknown_catalog_block` · `safety_policy_unavailable` · `profile_not_found` · `modded_write_unverified` · `name_already_exists` · `marker_already_exists` · `marker_target_read_failed` · `marker_target_occupied` · `marker_target_has_block_entity` · `marker_write_rejected` · `marker_world_write_failed` · `marker_metadata_write_failed` · `amulet_error` · `internal_error` · *(v0.5, specced: `build_log_not_configured` · `journal_empty` · `template_not_found` · `governance_requires_journal` · `brush_not_found` · `room_not_found` · `opening_not_found` · `graph_not_built` · `no_valid_placement` · `order_expired` · `dependency_failed` · `unknown_order_kind`)*

**`create_*` name-collision semantics (normative):** `create_pass`, `create_fragment`, and `create_bundle` **fail with `name_already_exists`** when the name is taken — never silently overwrite (an agent overwriting a shipped fragment by accident destroys library content with no journal to revert file-level changes). Deliberate replacement passes `overwrite: true`, which archives the previous definition to `<dir>/_replaced/<name>.<utc_ts>.json` before writing. Names are case-insensitive for collision purposes (Windows filesystem).

Partial-failure semantics for multi-pass operations (`apply_bundle`): continue-on-error per pass, collect an `errors: [{pass, error, message}]` array, `ok: true` if ≥1 pass succeeded, `ok: false` only if none did.

---

## 12. Safety & Reversibility

### 12.1 The write choke point

Every block change from every pass type and `place_npc_marker` funnels through
**one** final validation before `write_region`. The internal write call carries
a `write_context` field (an enum, not a public parameter) that gates which
checks apply:

| Check | `decoration` | `pattern_clear` | `room_envelope` (🚧 v0.5) |
|---|---|---|---|
| Target outside requested XZ/Y bounds, in halo, or in an unloaded chunk | **skip** | **skip** | **skip** (unconditional read-envelope guarantee) |
| Target contains a block entity | **skip** | **skip** | **skip** (unconditional until NBT snapshots exist) |
| Target in `structural_never_touch` | **skip** | **skip** | **skip** (unconditional) |
| Target is marker-protected | **skip** | **skip** | **skip** (unconditional) |
| Change writes air but not `destructive: true` | **skip** | **pass through** — implicit destructive grant | **pass through** — carving interiors and cutting openings ARE air-writes; blocking them blocks the Room subsystem (docs/brush_room_system.md §9 — the G1/pattern_clear lesson applied proactively). Destructiveness gating for room ops happens at the *operation* level via the §4.2 op-mapping table, not per-block |
| Block not in `safe_blocks.replaceable` | **skip** (if only_safe_blocks) — **unless target is air/air-like** (see air-transparency rule) | **pass through** | **pass through** — Room envelope writes legitimately replace structural blocks |

Before per-position checks, an unavailable safety policy rejects the whole
operation. After them, any unknown non-vanilla target ID also rejects the whole
operation rather than allowing a partial catalog-invalid write.

`room_envelope` may only be emitted by `core/room_engine.py`; its air-write grant is bounded upstream by the placement solver's pre-validation (docs/brush_room_system.md §5 step 2a: any envelope block that would be skipped at the choke point voids the whole candidate — the gate never fires mid-envelope).

**Air-transparency rule (normative):** the `replaceable` whitelist governs **replacing existing solid blocks** — that is what it protects. **Adding a block where the target is currently air (or air-like, §4.5 taxonomy) passes the whitelist check unconditionally**: placing vines into air beside a wall, stacking rubble into the air above a floor, growing a tree through a rooftop — these are the primary decorative actions, and their safety is governed by the other three rows (never-touch, markers, destructive gate) plus anchor/clearance checks upstream. Without this rule, every `place_adjacent` rule and the majority of fragment blocks (anything written into empty space) would be silently dropped, because `minecraft:air` is not — and must never be — on the replaceable whitelist. Overwriting an *air-like* block (a torch, a carpet) by an added block is permitted under the same rule; overwriting a *solid* block still requires whitelist membership.

**Game-physics property injection (normative):** Picasso writes save files; the game engine then runs block updates on load. The choke point applies one automatic correction: any written block in the `*_leaves` family gets `persistent: true` injected into its properties (unless the author explicitly set `persistent`) — otherwise decoratively placed leaves decay within minutes of loading wherever no log block is near, and roof trees become bare trunks. This is the only engine-side physics correction: it is mechanically decidable and always correct for decorative placement. All other game-physics interactions (falling gravel, vine attachment, water flow) are **authoring responsibilities** — see the authoring checklist in `docs/fragment_system.md` §2.

**Player-build protection row (✅ implemented):** player-activity and registry-derived protection is evaluated at the same final choke point (`docs/player_activity_pipeline.md`):

- Target position inside the bounds of a registry structure with player attribution (`player_built` or `player_modified`, three-state model per v05 §6), **or** inside a recent activity-site bounding box from the build log (sites are fresher than the registry — a base built yesterday is protected even if `detect_structures` hasn't re-run; this closes the freshness gap) → **skip**, unless the call passes `include_player_built: true`.
- Enforcement at the choke point — not at targeting — is deliberate: `apply_pass` and `apply_bundle` region mode never consult the registry, so targeting-level exclusion cannot protect a base sitting inside a styled region. Same philosophy as never-touch: upstream checks are optimizations, the choke point is the guarantee.
- Degraded mode: if the structure registry / build log are unavailable, the check is skipped and the response carries `"player_protection": "unavailable"` (resilient startup; agents on live worlds should treat that as a stop signal).
- **Governance gate (mechanical):** `include_player_built: true` ∧ the pass contains destructive content (any `destructive: true` fragment or `remove` rule) ∧ `journal_status != "active"` → **reject** with error `governance_requires_journal`. All three conditions are server-decidable at call time. Purely additive edits (reinforcement) pass — matching the policy that reinforce-only governance may precede Phase 8 while degrade/collapse may not.

Context assignment: `"decoration"` for block/fragment pass writes; `"pattern_clear"` for matched `clear_offsets` air-writes emitted by a `pattern_replace` pass (replacing a chair implies removing its parts — the exemption previously stated in prose in `docs/style_pass_schema.md` §2, now carried by this context so the table and the schema doc agree); `"room_envelope"` (🚧 v0.5) for Room writes. The `pattern_clear` context may only be emitted by the pattern-replace engine path for offsets listed in the matched pattern's `clear_offsets` that actually matched a pattern block; the replacement and clears are validated as one atomic group at the write choke. `room_envelope` may only be passed by `core/room_engine.py` (future). The tools layer has no access to either. `structural_never_touch` and marker protection remain unconditional across all contexts.

Each fragment instance likewise records the unique positions it actually emits
(probability/preserve skips excluded) as one atomic group when more than one
position remains. A failed constituent drops the whole instance; overlapping
groups are closed transitively so shared positions cannot leave partial tails.

This closes the v0.3 hole where destructive fragments checked only the *anchor*, not the *written positions*.

### 12.2 Marker protection

Positions listed in `<world>/picasso_markers/*.json` are immutable to all passes (unconditional, all write contexts). The engine **snapshots marker positions once at the start of the apply call** and holds that snapshot for the call's duration — all passes within a bundle run against a consistent protection set.

**Threat model note:** the marker directory is a *filesystem* interface, not only an MCP one — §13 explicitly invites the Narrative Layer (an external process) to read it, and by the same token an external process or a human may **write** to it while a long apply call is running. MCP single-flight serialization does not cover filesystem writers. A marker created externally mid-apply is therefore **not protected until the next apply call**. This window is accepted for v0.4 (externally-created markers during an active apply are an operational anti-pattern; document it in the Narrative Layer integration guide), not denied. `TODO(safety)`: if the window proves real in practice, upgrade from call-start snapshot to per-pass re-scan or a directory watcher.

### 12.3 Journal & revert (✅ implemented)

Every non-dry apply durably publishes a complete `pending` reverse-diff entry
under `<world>/picasso_journal/` before Amulet may mutate the world. The same
entry is atomically advanced to `committed` after saved-state verification or
to `failed` with rollback diagnostics. Entries bind to a persistent world UUID,
dimension, and path hash; malformed or mismatched entries fail closed.

`list_journal_entries`, `inspect_journal_entry`, and `revert_last_apply` are
public tools. Revert compares the current block with the recorded `after` state:
equal states restore `before`; third-party changes are skipped and returned as
conflicts. Revert itself uses durable `revert_pending` → `reverted`/failure
transitions. NPC marker block and companion JSON are recorded as one compound
artifact, so neither half can be reverted independently.

Journal availability is necessary but not sufficient for live writes. Copied
saves are the supported baseline. On formal/shared worlds, write only while
Minecraft is closed, after an exact dry-run, and only when both
`journal_status=active` and player protection report active.

### 12.4 Resource limits

`radius_chunks > PICASSO_MAX_RADIUS_CHUNKS` (default 12 → 25×25 chunks) returns `region_too_large` with a suggestion to tile the work. Rationale: a 1.21 chunk column is 384 blocks tall; dense urban regions at radius 16+ reach tens of millions of candidate positions and can OOM the sparse representation.

`inspect_volume` has an independent evidence budget: inclusive spans are capped
at X/Z 32 and Y 24 blocks, total volume 24,576, with at most 4,096 RLE runs in a
response. Run overflow is explicitly truncated (`complete=false`,
`omitted_runs>0`); the Agent must subdivide and may not interpret omitted data as
air. These limits protect model context as well as memory.

**Tiling note:** the implemented one-chunk halo removes ordinary tile-seam neighbor blindness. Tiles still represent independent pass executions (spacing and bundle layering do not cross calls), so preview the exact tiling plan and avoid concurrent writers.

---

## 13. Extension Points

| To add… | Do this | Code changes |
|---|---|---|
| a Style Pass (any type) | Drop JSON in `data/passes/` | none |
| a Fragment | Drop JSON in `data/fragments/` (or `create_fragment` at runtime) | none |
| a Bundle | Drop JSON in `data/bundles/` (or `create_bundle`) | none |
| a furniture Pattern | Drop JSON in `data/patterns/` | none |
| a static mechanical ruin | Prefer a normal Fragment; use a stateless StaticPrefab only when an exact larger layout needs it | strict capability gate; every block must be block-state-only with no BE/NBT requirement |
| a Create rail replacement | Author and in-game validate versioned RailTemplate geometry; record the reviewed route as a RailNetwork | dedicated native rail executor and rollback artifact required; never raw Fragment/Amulet writes |
| a functional mechanical installation | Later: author and in-game validate a versioned MechanicalTemplate; register its exact mod/backend capabilities | deferred mechanical subsystem + game-native executor required; never a raw Fragment shortcut |
| a Catalog | Point `PICASSO_CATALOG_PATH` at any JSON matching the DD schema | none |
| Narrative Layer hookup | Read `<world>/picasso_markers/*.json`; call `place_npc_marker` with populated `source_agent`/`dialogue_id`/`quest_id` | none |

---

## 14. Supplementary Documents & Reading Order

| Document | Covers | Status |
|---|---|---|
| `docs/fragment_system.md` | Fragment model, Fragment Pass, orientation rules, Bundle format & execution semantics | primary mechanism |
| `docs/style_pass_schema.md` | All three pass JSON schemas, Pattern schema, safe_blocks format | |
| `docs/tool_specs.md` | Base tool API (I/O schemas) | |
| `docs/agent_semantic_review.md` | Agent-assisted structure interpretation, evidence and rule-promotion contract | **normative** |
| `docs/mechanical_structures.md` | Phase-one static mechanical ruins, priority Create rail replacement, and later functional machinery | 🚧 normative forward design |
| `docs/style_learning.md` | Reverse-learning workflow: reference save → clusters → fragments → bundle | |
| `docs/semantic_segmentation.md` | Multi-signal structure detection pipeline; structure identity & re-detection | 🚧 subsystem |
| `docs/structure_detection_tool_specs.md` | Segmentation tool API | 🚧 subsystem |
| `docs/implementation_order.md` | Build sequence & current status, base system | |
| `docs/segmentation_implementation_phases.md` | Build sequence, segmentation system | |
| `docs/REVISION_LOG.md` | Decision record: every change, rationale, adjudications across all review rounds | review artifact — the tiebreaker |
| `docs/brush_room_system.md` | Brush vocabulary, Room templates, interior graph, instantiation — v0.5 primary new subsystem | **v0.5 normative** (round-5 signed off) |
| `docs/wargame_interface.md` | Work-order queue, round clock, faction attribution — the wargame layer contract | v0.5 draft |
| `docs/player_activity_pipeline.md` | Build-log plugin contract, activity sites, attribution, clock consistency | v0.5 draft |
| `docs/agent_playbooks.md` | Tool-surface walkthroughs for driving agents | reference |
| `docs/phase15_contingency.md` | Pre-committed fallback plans if the modded-block spike fails | contingency |
| `docs/HANDOFF.md` | Implementation kickoff package for the coding agent | handoff |
| `docs/v05_forward_requirements.md` | Product-owner decisions for v0.5+ (superseded sections marked) | intent record |

**Coding agent reading order:**
1. This document — full read required
2. `docs/fragment_system.md` — primary mechanism
3. `docs/style_pass_schema.md` — data formats
4. `docs/tool_specs.md` — tool contracts
5. `docs/style_learning.md` — learning workflow
6. `docs/agent_semantic_review.md` — Agent evidence/interpretation boundary
7. `docs/semantic_segmentation.md` + `docs/structure_detection_tool_specs.md` — detection subsystem
8. `docs/mechanical_structures.md` — static ruins, native Create rail replacement, and deferred functional machinery
9. `docs/implementation_order.md` + `docs/segmentation_implementation_phases.md` — build sequence & status
