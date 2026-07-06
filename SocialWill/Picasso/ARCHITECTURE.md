# Picasso — Architecture Reference

**Document version: v0.4** (2026-07-06)

> **For coding agents:** Read this document in full before writing any code. Every design decision here is intentional. If you find a conflict or ambiguity, leave a `TODO(arch):` comment and proceed with the most literal interpretation of this document.
>
> **Document precedence:** This document is authoritative for cross-cutting contracts (layering, pass type system, determinism, safety). Supplements in `docs/` are authoritative for their own subsystem's details. On conflict: higher version wins; same version → this document wins. `README.md` is marketing-level and never authoritative.

### Changelog

| Version | Scope |
|---|---|
| v0.1 | Initial architecture: block-replacement Style Passes, 6 TLOU passes, learning via StyleProfile |
| v0.2 | Semantic segmentation supplement (`docs/semantic_segmentation.md`) |
| v0.3 | Fragment-first redesign (`docs/fragment_system.md`): Fragments become the primary mechanism, block passes demoted to texture accents |
| v0.4 | **This consolidation.** Pass Type System formalized (§5); surface classification formalized (§4.5); determinism spec (§6); safety & reversibility spec (§12); unified tool registry with status (§8); unified config (§10); Zone concept **cut**; `apply_style_profile` **retired**; legacy v0.1 passes marked deprecated |

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
7. **Reversible in principle.** Every non-dry apply is journaled for revert (§12.3 — *planned, not yet implemented; use world copies until it lands*).

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
│  BundleExecutor(planned) segmentation/(planned)│
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

> `TODO(arch)`: bundle orchestration logic currently lives in `tools/bundle.py`; per the layering rule it must move to `core/bundle_executor.py` during the refactor.

---

## 3. Source Layout

Status markers: ✅ implemented · 🚧 planned (spec exists, not built) · ⚠️ deprecated

```
src/picasso/
├── server.py               ✅ Entry point. Instantiates FastMCP, registers all tools.
├── config.py               ✅ Env-var config, exposes a Config singleton.
├── session.py              ✅ Server-level session state (see §4.1).
│
├── tools/
│   ├── world_io.py         ✅ set_world, read_region (+ ensure_region cache helper)
│   ├── analysis.py         ✅ analyze_region
│   ├── catalog.py          ✅ query_catalog
│   ├── style.py            ✅ list_passes, preview_pass, apply_pass, create_pass
│   ├── bundle.py           ✅ list_bundles, apply_bundle (logic to move to core/, see §2)
│   ├── learning.py         ✅ list_fragments, create_fragment, create_bundle
│   │                       🚧 learn_style, extract_block_clusters
│   ├── npc.py              ✅ place_npc_marker
│   └── segmentation.py     🚧 detect_structures, list_structures, get_structure,
│                              annotate_structure, apply_pass_to_structure, apply_pass_by_type
│
├── core/
│   ├── amulet_bridge.py    ✅ Wraps Amulet-Core. All Amulet imports live here exclusively.
│   ├── surface_classifier.py ✅ Surface + space classification (§4.5)
│   ├── style_engine.py     ✅ Pass dispatcher: block_pass / fragment_pass / pattern_replace (§5)
│   ├── fragment_engine.py  ✅ Fragment placement (docs/fragment_system.md)
│   ├── fragment_library.py ✅ Loads and indexes Fragment JSON files
│   ├── pattern_matcher.py  ✅ Scans a region for vanilla furniture pattern templates
│   ├── catalog_index.py    ✅ Loads DD catalog JSON, provides query interface
│   ├── noise_field.py      ✅ Perlin noise (C lib) with built-in deterministic fallback (§4.6)
│   ├── bundle_executor.py  🚧 Bundle orchestration (extracted from tools/bundle.py)
│   ├── journal.py          🚧 Reverse-diff journal + revert (§12.3)
│   ├── style_learner.py    🚧 StyleProfile extraction (docs/style_learning.md)
│   ├── zone_registry.py    ⚠️ CUT in v0.4 — structures are targeted directly (§8, semantic_segmentation.md)
│   └── segmentation/       🚧 Detection pipeline package (docs/semantic_segmentation.md §9)
│
├── models/
│   ├── block.py            ✅ BlockPos, BlockState dataclasses
│   ├── region.py           ✅ RegionData + surface/space class caches
│   ├── style_pass.py       ✅ StylePass (discriminated by `type`, §5), StyleRule, NoiseConfig
│   ├── fragment.py         ✅ Fragment, FragmentBlock Pydantic models
│   ├── structure.py        🚧 Structure, BoundingBox, StructureCandidate
│   └── style_profile.py    🚧 StyleProfile (diagnostic artifact only, §8 note on retirement)
│
└── data/
    ├── catalog/            🚧 doomsday_decoration_semantic.json (copy step is Phase 2, not yet done —
    │                          server must start cleanly without it, see §10 resilient-startup rule)
    ├── passes/             ✅ 15 files: v0.3 nine-pass set + v0.1 legacy set (legacy marked deprecated, §5.4)
    ├── fragments/          ✅ 10 fragment templates
    ├── bundles/            ✅ tlou_complete.json
    ├── patterns/           ✅ chair, table, desk, computer_desk, shelf, bed_frame
    ├── safe_blocks.json    ✅ Replaceable whitelist + structural_never_touch blacklist
    └── structure_fingerprints.json ✅ (consumed by 🚧 segmentation)
```

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
```

The session is a module-level singleton (`from picasso.session import session`).

**Cache invalidation rules (normative):**
- `last_region` is reused only when `(cx, cz, radius_chunks)` match exactly.
- Any successful non-dry write (`apply_pass`, `apply_bundle`, `place_npc_marker`) **must** set `last_region = None`. A preview never invalidates.
- `set_world()` closes any existing bridge and clears `last_region`.

**Concurrency (normative):** the session is **single-flight**. Tool handlers assume no concurrent tool execution; FastMCP over stdio serializes requests, and this assumption is documented here so nobody "fixes" it with threads later without adding locking.

### 4.2 `core/amulet_bridge.py` — Amulet Wrapper

All Amulet-Core imports are confined to this file. If Amulet's API changes, only this file changes.

**Interface:**

```python
class AmuletBridge:
    def __init__(self, world_path: str) -> None: ...

    def read_region(self, cx: int, cz: int, radius: int) -> RegionData: ...
    # Returns all blocks in chunks within `radius` chunks of (cx, cz).

    def write_region(self, changes: RegionData) -> int: ...
    # Applies BlockState changes. Returns count of changed blocks.
    # Must call level.save() after applying all changes.

    def place_block(self, x: int, y: int, z: int, block_state: BlockState) -> None: ...

    def close(self) -> None: ...
```

**Amulet usage notes:**
- Open a level: `amulet.load_level(world_path)`.
- Amulet stores blocks in a **universal format** and translates to/from the save's game version. All version translation happens inside Amulet; the bridge deals in `BlockState` only.
- World height range for 1.21.1 is **−64…319**. The bridge must not assume 0…255.
- Ungenerated / missing chunks inside the requested radius are **skipped silently**; `read_region` reports how many chunks were actually read.
- Always operate in `"minecraft:overworld"` unless otherwise specified.

> **Gating assumption — verify before building more on top (Phase 1.5 spike):** modded blocks (`doomsday:*`) must survive a full round-trip: `write_region` → `level.save()` → open world in Minecraft 1.21.1 with the DD mod → block renders correctly. Amulet represents unknown modded blocks as opaque universal entries; this is *believed* to work but has **not been verified in-game**. If it fails, the entire replacement vocabulary strategy needs rethinking (e.g., structure-block staging). Do not build Phases 4+ polish before this spike passes.
>
> The exact chunk/block accessor API differs between amulet-core versions. The bridge pins whatever API the installed version exposes; **the exact calls are an implementation detail of this file** and deliberately not specified here.

### 4.3 `core/catalog_index.py` — DD Catalog Query

Loads `doomsday_decoration_semantic.json` at startup. Provides a fast in-memory query interface.

```python
class CatalogIndex:
    def __init__(self, catalog_path: str) -> None: ...
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

**Definitions** (for a solid block at position `p`; positions outside the read region count as **air**):

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
- Region-boundary blocks can misclassify because out-of-region reads as air. Mitigation (🚧): `read_region` reads a 1-chunk halo used for classification but excluded from modification.
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

**Backend policy (normative):** if the optional C library `noise` is importable, use `pnoise2/pnoise3 (octaves=4, base=seed)`; otherwise fall back to the built-in deterministic hash-based value noise. The fallback is the **portable baseline** (no compiler needed on Windows); the C library is an optional accelerator. The two backends produce *different* fields for the same seed — reproducibility is guaranteed only within one backend. `list_passes`-level metadata is unaffected, but apply/preview responses should include `"noise_backend": "c" | "fallback"`.

### 4.7 `core/fragment_engine.py` + `core/fragment_library.py`

The primary stylization mechanism. Full spec: `docs/fragment_system.md` (data model, placement algorithm, **orientation/rotation rules**, safety guards).

### 4.8 `core/bundle_executor.py` (🚧 extraction)

Loads bundle JSON, iterates entries, calls `StyleEngine`. Execution semantics (modes, error collection, seed precedence) are specified in `docs/fragment_system.md` §6.

---

## 5. Pass Type System

A **pass** is any JSON file in `data/passes/`. The top-level `type` field discriminates three schemas. `StyleEngine` is the single dispatcher.

| `type` | Mechanism | Engine path | Schema |
|---|---|---|---|
| `"block_pass"` *(default when omitted)* | Per-block texture replacement via match rules | `_apply_block_pass` | `docs/style_pass_schema.md` |
| `"fragment_pass"` | Place Fragment templates at surface anchors | `FragmentEngine` | `docs/fragment_system.md` §3 |
| `"pattern_replace"` | Replace matched vanilla furniture combos with DD blocks | `PatternMatcher` + mappings | `docs/style_pass_schema.md` |

The Pydantic model is a discriminated union on `type`; a `block_pass` JSON that omits `type` is valid (back-compat with v0.1 files). A pass whose fields don't match its declared type fails registry load with `invalid_pass_definition` (the server logs it, skips the file, and keeps starting — resilient startup, §10).

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
5. Noise fields are seeded by the same seed but are a *spatial* gate, not a per-decision RNG (see backend caveat §4.6).

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
    blocks: dict[BlockPos, BlockState]   # sparse: non-air blocks only
    origin_cx: int
    origin_cz: int
    radius_chunks: int
    surface_classes: dict[BlockPos, str] = field(default_factory=dict)  # filled by classifier
    space_classes: dict[BlockPos, str] = field(default_factory=dict)

    def get(self, pos: BlockPos) -> BlockState | None: ...
    def set(self, pos: BlockPos, state: BlockState) -> None: ...
    def bounding_box(self) -> tuple[BlockPos, BlockPos]: ...
```

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
    rules: list[StyleRule] = []          # block_pass
    mappings: list[dict] = []            # pattern_replace
    # fragment_pass fields (fragments, density, min_spacing, …) — docs/fragment_system.md §3
    only_safe_blocks: bool = True
```

### `models/fragment.py` — see `docs/fragment_system.md` §2.

### `models/style_profile.py` (🚧, diagnostic only)

`StyleProfile` survives as the **read-only output** of `learn_style` — a statistical summary an agent reads before deciding which fragments/bundles to author. `apply_style_profile` is retired (§8); a profile is never executed directly. Fields as in v0.1 spec plus `matched_fragments` (docs/style_learning.md §7).

---

## 8. MCP Tool Registry (authoritative)

Status: ✅ implemented · 🚧 planned · ❌ retired

| Tool | Category | Status | Spec |
|---|---|---|---|
| `set_world` | World I/O | ✅ | docs/tool_specs.md |
| `read_region` | World I/O | ✅ | docs/tool_specs.md |
| `analyze_region` | Analysis | ✅ | docs/tool_specs.md |
| `query_catalog` | Catalog | ✅ | docs/tool_specs.md |
| `list_passes` | Style | ✅ | docs/tool_specs.md |
| `preview_pass` | Style | ✅ | docs/tool_specs.md |
| `apply_pass` | Style | ✅ | docs/tool_specs.md |
| `create_pass` | Style | ✅ | docs/tool_specs.md |
| `list_fragments` | Fragment | ✅ | docs/style_learning.md §6 |
| `create_fragment` | Fragment | ✅ | docs/style_learning.md §4 |
| `list_bundles` | Bundle | ✅ | docs/fragment_system.md §6 |
| `create_bundle` | Bundle | ✅ | docs/style_learning.md §5 |
| `apply_bundle` | Bundle | ✅* | docs/fragment_system.md §6 — *implemented in region mode; structure mode 🚧 |
| `place_npc_marker` | NPC | ✅ | docs/tool_specs.md |
| `learn_style` | Learning | 🚧 | docs/tool_specs.md + style_learning.md §7 |
| `extract_block_clusters` | Learning | 🚧 | docs/style_learning.md §3 |
| `detect_structures` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `list_structures` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `get_structure` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `annotate_structure` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `apply_pass_to_structure` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `apply_pass_by_type` | Segmentation | 🚧 | docs/structure_detection_tool_specs.md |
| `revert_last_apply` | Safety | 🚧 | §12.3 |
| `apply_style_profile` | Learning | ❌ retired v0.4 | superseded by cluster → fragment → bundle workflow (docs/style_learning.md) |

**Zone concept is cut (v0.4).** v0.2 proposed promoting Structures to "Zones" as a separate registry. Nothing consumed zones: bundles target `structure_type`, tools target `structure_id`. Structures **are** the targeting handle; `models/zone.py` and `core/zone_registry.py` are removed from scope. (Structure registry loading lives with the segmentation package.)

---

## 9. NPC Marker Interface (Stub for Narrative Layer)

`place_npc_marker` places a `minecraft:structure_void` block (invisible in-game) plus a companion JSON file `<world_path>/picasso_markers/<x>_<y>_<z>.json`, since vanilla MC does not support arbitrary NBT on non-tile-entity blocks.

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
| `PICASSO_CATALOG_PATH` | `src/picasso/data/catalog/doomsday_decoration_semantic.json` | ✅ | DD semantic catalog |
| `PICASSO_PASSES_DIR` | `src/picasso/data/passes/` | ✅ | Style Pass JSON files (all three types) |
| `PICASSO_PATTERNS_DIR` | `src/picasso/data/patterns/` | ✅ | Vanilla furniture pattern templates |
| `PICASSO_FRAGMENTS_DIR` | `src/picasso/data/fragments/` | ✅ | Fragment templates |
| `PICASSO_BUNDLES_DIR` | `src/picasso/data/bundles/` | ✅ | Style Bundles |
| `PICASSO_SAFE_BLOCKS` | `src/picasso/data/safe_blocks.json` | ✅ | Replaceable whitelist + never-touch blacklist |
| `PICASSO_FINGERPRINTS_PATH` | `src/picasso/data/structure_fingerprints.json` | 🚧 | Structure fingerprint library |
| `PICASSO_PROFILES_DIR` | `src/picasso/data/profiles/` | 🚧 | learn_style output |
| `PICASSO_LOG_LEVEL` | `INFO` | ✅ | Logging level |
| `PICASSO_MAX_RADIUS_CHUNKS` | `12` | 🚧 | Hard cap for read_region/analysis/detection (§12.4) |
| `PICASSO_FLAT_VARIANCE_THRESHOLD` | `1.5` | 🚧 | Flat-region detector |
| `PICASSO_MIN_STRUCTURE_AREA` | `50` | 🚧 | Min XZ footprint (blocks²) kept by detection |
| `PICASSO_MIN_ROOM_VOLUME` | `12` | 🚧 | Min enclosed air volume (blocks³) |
| `PICASSO_MIN_STADIUM_VOLUME` | `2000` | 🚧 | Stadium special-case threshold |
| `PICASSO_GROUND_DETECTION_RADIUS` | `3` | 🚧 | Chunk radius for local ground-Y baseline |

**Not configurable (fixed per-world paths):** structure registry `<world>/picasso_structures.json`, markers `<world>/picasso_markers/`, journal `<world>/picasso_journal/`. (v0.2's `PICASSO_STRUCTURES_DIR` env var is retracted — a per-world artifact must not live in static env config.)

**Resilient startup rule (normative):** the server must start and register all tools even if the catalog, passes dir, or fragments dir is missing or partially invalid. Missing resources degrade the affected tools to structured errors (`catalog_not_loaded`, `pass_not_found`, …); invalid individual JSON files are logged and skipped, never fatal.

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
`world_not_found` · `world_not_set` · `invalid_coordinates` · `region_too_large` · `pass_not_found` · `invalid_pass_definition` · `fragment_not_found` · `bundle_not_found` · `ambiguous_bundle_scope` · `pattern_not_found` · `structure_not_found` · `catalog_not_loaded` · `profile_not_found` · `amulet_error` · `internal_error`

Partial-failure semantics for multi-pass operations (`apply_bundle`): continue-on-error per pass, collect an `errors: [{pass, error, message}]` array, `ok: true` if ≥1 pass succeeded, `ok: false` only if none did.

---

## 12. Safety & Reversibility

### 12.1 The write choke point

Every block change from any pass type funnels through **one** final validation before `write_region`:

1. Target block currently in `structural_never_touch` → **skip** (regardless of pass settings, including destructive fragments).
2. Target position is marker-protected (§12.2) → **skip**.
3. Change writes air but the originating pass/fragment is not `destructive: true` → **skip**.
4. `only_safe_blocks`/`only_safe_anchor_blocks` filters apply earlier (match/anchor time) as designed, but the choke point is the guarantee.

This closes the v0.3 hole where destructive fragments checked only the *anchor*, not the *written positions*.

### 12.2 Marker protection

Positions listed in `<world>/picasso_markers/*.json` are immutable to all passes. The engine loads marker positions once per apply call.

### 12.3 Journal & revert (🚧 — highest-priority planned safety feature)

Every non-dry apply writes a reverse diff to `<world>/picasso_journal/<utc_ts>_<tool>_<seed>.json`:

```json
{
  "tool": "apply_bundle", "argument_summary": {...}, "applied_at": "...",
  "changes": [{"pos": {...}, "before": {"id": "...", "properties": {}}, "after": {...}}]
}
```

`revert_last_apply` (🚧) replays `before` states of the most recent journal entry, then archives it. Journals are plain JSON — hand-inspectable and hand-revertible in emergencies. **Until this lands, the only safety net is operating on a copy of the world. This is an operational rule, repeated in every implementation doc: never point Picasso at the production save.**

### 12.4 Resource limits

`radius_chunks > PICASSO_MAX_RADIUS_CHUNKS` (default 12 → 25×25 chunks) returns `region_too_large` with a suggestion to tile the work. Rationale: a 1.21 chunk column is 384 blocks tall; dense urban regions at radius 16+ reach tens of millions of candidate positions and can OOM the sparse representation.

---

## 13. Extension Points

| To add… | Do this | Code changes |
|---|---|---|
| a Style Pass (any type) | Drop JSON in `data/passes/` | none |
| a Fragment | Drop JSON in `data/fragments/` (or `create_fragment` at runtime) | none |
| a Bundle | Drop JSON in `data/bundles/` (or `create_bundle`) | none |
| a furniture Pattern | Drop JSON in `data/patterns/` | none |
| a Catalog | Point `PICASSO_CATALOG_PATH` at any JSON matching the DD schema | none |
| Narrative Layer hookup | Read `<world>/picasso_markers/*.json`; call `place_npc_marker` with populated `source_agent`/`dialogue_id`/`quest_id` | none |

---

## 14. Supplementary Documents & Reading Order

| Document | Covers | Status |
|---|---|---|
| `docs/fragment_system.md` | Fragment model, Fragment Pass, orientation rules, Bundle format & execution semantics | primary mechanism |
| `docs/style_pass_schema.md` | All three pass JSON schemas, Pattern schema, safe_blocks format | |
| `docs/tool_specs.md` | Base tool API (I/O schemas) | |
| `docs/style_learning.md` | Reverse-learning workflow: reference save → clusters → fragments → bundle | |
| `docs/semantic_segmentation.md` | Multi-signal structure detection pipeline; structure identity & re-detection | 🚧 subsystem |
| `docs/structure_detection_tool_specs.md` | Segmentation tool API | 🚧 subsystem |
| `docs/implementation_order.md` | Build sequence & current status, base system | |
| `docs/segmentation_implementation_phases.md` | Build sequence, segmentation system | |
| `docs/REVISION_LOG.md` | v0.4 decision record: every change, rationale, open questions | review artifact |

**Coding agent reading order:**
1. This document — full read required
2. `docs/fragment_system.md` — primary mechanism
3. `docs/style_pass_schema.md` — data formats
4. `docs/tool_specs.md` — tool contracts
5. `docs/style_learning.md` — learning workflow
6. `docs/semantic_segmentation.md` + `docs/structure_detection_tool_specs.md` — detection subsystem
7. `docs/implementation_order.md` + `docs/segmentation_implementation_phases.md` — build sequence & status
