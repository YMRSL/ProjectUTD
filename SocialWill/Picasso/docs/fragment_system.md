# Fragment System — Architecture Supplement

> Supplements `ARCHITECTURE.md` — tracks v0.4.4. Read after the main document.
> The Fragment system is the **primary** stylization mechanism since v0.3. Block-replacement Style Passes are auxiliary (≤15% surface coverage, texture variation only).

---

## 1. Design Intent

> **Decay is a structural story, not a color change.**

A post-apocalyptic building isn't recognizable because its walls are mossy. It's recognizable because:
- A corner of the fourth floor has collapsed, leaving rubble on the street below
- The window on the east face is filled with vines and a small birch tree
- Three cars are wedged in the intersection outside
- The lobby floor has a crack with roots pushing through

These are **compositional events** — small block arrangements that each tell a micro-story. A Fragment is the data model for one such event.

---

## 2. Fragment Data Model

A Fragment is a small block arrangement template stored in `data/fragments/*.json` (Pydantic model in `models/fragment.py`).

```python
class FragmentBlock(BaseModel):
    offset: tuple[int, int, int]    # relative to anchor, in the fragment's canonical frame (§4)
    block: str                      # full block ID
    properties: dict[str, str] = {} # block state properties
    probability: float = 1.0        # per-block placement probability (0–1)
    preserve_existing: bool = False # if True, skip if non-air block exists at offset

class Fragment(BaseModel):
    name: str
    description: str
    anchor_surface: str             # see enum below
    footprint: str                  # "1x1", "3x2", … — XZ clearance extent (§5 step e)
    blocks: list[FragmentBlock]
    requires_clear_above: bool = True
    min_clear_height: int = 2
    destructive: bool = False       # if True, MAY write air (subject to the §12.1 choke point in ARCHITECTURE.md)
    orientable: bool = False        # if True, engine rotates the fragment per anchor (§4)
    match_hint: Literal["glass_pane"] | None = None # optional strict anchor constraint
    tags: list[str] = []
```

### `anchor_surface` enum (normative)

Must be one of the surface classes produced by the classifier (`ARCHITECTURE.md` §4.5): `"floor"`, `"outer_wall"`, `"inner_wall"`, `"ceiling"`, `"rooftop"`, or the wildcard `"any"`.

> v0.3 examples used `"street"`. **Retracted** — the classifier never emits it. "Street-ness" is expressed by targeting `anchor_surface: "floor"` and running the pass against `structure_type: "road"` in a bundle (or `space_filter: "exterior"`).

`match_hint: "glass_pane"` restricts an anchor to vanilla clear/stained glass
panes. Unknown hints fail schema validation; hints are never decorative prose.
The shipped overgrown-window fragment only removes its verified pane anchor and
places surrounding growth in outward air, so the hint cannot authorize damage
to adjacent wall blocks.

### Fragment JSON Example: `rubble_pile_small`

```json
{
  "name": "rubble_pile_small",
  "description": "A small scattering of stone debris on a flat surface",
  "anchor_surface": "floor",
  "footprint": "3x2",
  "requires_clear_above": true,
  "min_clear_height": 2,
  "destructive": false,
  "tags": ["debris", "ground"],
  "blocks": [
    {"offset": [0,  0, 0], "block": "minecraft:cobblestone",  "probability": 1.0},
    {"offset": [1,  0, 0], "block": "minecraft:gravel",        "probability": 0.8},
    {"offset": [-1, 0, 0], "block": "minecraft:gravel",        "probability": 0.7},
    {"offset": [0,  0, 1], "block": "minecraft:cobblestone",   "probability": 0.6},
    {"offset": [1,  0, 1], "block": "minecraft:stone",         "probability": 0.4},
    {"offset": [0,  1, 0], "block": "minecraft:cobblestone",   "probability": 0.35},
    {"offset": [1,  1, 0], "block": "minecraft:gravel",        "probability": 0.2}
  ]
}
```

### Fragment JSON Example: `wall_breach` (orientable, destructive)

```json
{
  "name": "wall_breach",
  "description": "A localized hole in an outer wall with rubble at the base",
  "anchor_surface": "outer_wall",
  "footprint": "3x3",
  "requires_clear_above": false,
  "destructive": true,
  "orientable": true,
  "tags": ["collapse", "structural"],
  "blocks": [
    {"offset": [0,  0, 0], "block": "minecraft:air", "probability": 1.0},
    {"offset": [0,  1, 0], "block": "minecraft:air", "probability": 1.0},
    {"offset": [1,  0, 0], "block": "minecraft:air", "probability": 0.6},
    {"offset": [-1, 0, 0], "block": "minecraft:air", "probability": 0.6},
    {"offset": [0, -1, 1], "block": "minecraft:cobblestone", "probability": 0.8, "preserve_existing": true},
    {"offset": [1, -1, 1], "block": "minecraft:gravel",      "probability": 0.5, "preserve_existing": true},
    {"offset": [-1,-1, 1], "block": "minecraft:gravel",      "probability": 0.5, "preserve_existing": true}
  ]
}
```

(Note the rubble at `z=+1`: in the canonical frame, +Z is the wall's *outward* normal — the debris falls on the outside. See §4.)

### Game-physics authoring checklist (normative for shipped fragments)

Picasso writes save files; the game engine then runs physics on load. A fragment that ignores this looks right in preview and wrong in game. Rules for fragment authors (AI or human):

1. **Leaves** — handled by the engine: the write choke point auto-injects `persistent: true` on all `*_leaves` writes (`ARCHITECTURE.md` §12.1). Authors need do nothing, but may set `persistent` explicitly to override.
2. **Falling blocks** (`gravel`, `sand`, `*_concrete_powder`): only place where directly supported — offset `(x, y, z)` with a solid or same-fragment block at `(x, y−1, z)`, or `y = 0` on a solid anchor surface. A gravel block placed floating (e.g. on a breach windowsill without support) falls on the first tick and lands somewhere unplanned.
3. **Vines** need an adjacent solid face to attach to; place only against solid blocks (the `air_side` direction of a wall anchor guarantees this). Free-floating vine columns pop off on update.
4. **Torches, buttons, signs** (attachable blocks) need a supporting face; same rule as vines.
5. **Water line**: a `destructive` fragment breaching a wall below the local water level lets water flow in on load. Until the water-structure subsystem (🚧 S3) owns flooding semantics, avoid destructive fragments below Y-of-adjacent-water; the authoring workaround is `y_min` matching in the pass or careful anchor_surface choice.

Shipped-content audit note: `rubble_pile_*` (gravel at y=0/y=1 — verify y=1 entries sit on y=0 entries), `wall_breach` windowsill gravel, and `roof_tree_growth`/`window_overgrown` leaves (now engine-covered) are the known exposure points. Verify in the Phase 5 in-game check.

---

## 3. Fragment Pass

A Fragment Pass places Fragment instances at valid anchor points across a surface, using density and noise to control placement. It is one of the three pass types (`ARCHITECTURE.md` §5) and lives in `data/passes/` with `"type": "fragment_pass"`.

### Fragment Pass JSON Format

```json
{
  "name": "tlou_rubble_scatter",
  "type": "fragment_pass",
  "description": "Scatter small rubble piles across floors and streets",
  "version": "1.0",
  "fragments": ["rubble_pile_small", "rubble_pile_medium"],
  "anchor_surface": "floor",
  "density": 0.06,
  "noise": {"scale": 0.07, "threshold": 0.50},
  "min_spacing": 4,
  "only_safe_anchor_blocks": true
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `type` | `"fragment_pass"` | ✓ | Pass type discriminator |
| `fragments` | string[] | ✓ non-empty | Fragment names to draw from; one chosen per placement via position-hash roll |
| `anchor_surface` | string | ✓ | Surface class to scan for anchors. Overrides each fragment's own `anchor_surface` for *candidate collection*; a listed fragment whose `anchor_surface` differs and isn't `"any"` is skipped for incompatible anchors |
| `density` | float 0–1 | ✓ | Fraction of eligible anchors that receive a fragment, **after** intensity scaling: `effective_density = clamp01(density × intensity)` |
| `noise` | NoiseConfig | | Spatial gate — only fire where noise > threshold |
| `min_spacing` | int | | Min XZ distance between anchors placed **by this pass execution** (see cross-pass note below). Default 0 |
| `only_safe_anchor_blocks` | bool | | Anchor block must be in `safe_blocks.json` `replaceable`. Default `true` |

`fragments`, `anchor_surface`, and `density` are **required with no defaults** (`ARCHITECTURE.md` §7): a missing `density` must fail load as `invalid_pass_definition`, not silently place zero fragments; a defaulted `anchor_surface` would silently override each fragment's own surface.

> `space_filter` is **not** a pass field — it is a call-time / bundle-entry parameter (`ARCHITECTURE.md` §5.2). v0.3 showed it inside the pass JSON; retracted to keep "what the pass does" separate from "where it is applied".

**Cross-pass spacing:** `min_spacing` only constrains placements within one pass execution. Two different passes in a bundle can place fragments adjacently (a tree on a rubble pile). This is accepted for now — occasional overlap reads as organic chaos, and per-block `preserve_existing` prevents hard clobbering. If it proves ugly in practice, a bundle-level shared occupancy grid is the designated fix (**open question, see REVISION_LOG**).

---

## 4. Orientation (new in v0.4 — normative)

Wall-anchored fragments are directional: a breach must punch *through* the wall, rubble must fall *outside*. Axis-fixed offsets would be wrong on 3 of 4 wall faces.

**Canonical frame:** a fragment is authored as if anchored on a wall whose **outward normal is +Z** ("the wall runs along X; outside is south/+Z"). For floor/rooftop/ceiling fragments the frame is unrotated world space.

**Rules:**

1. `orientable: false` (default): offsets are applied as world-space offsets, no rotation. Correct for radially symmetric content (rubble piles, root cracks).
2. `orientable: true` + wall anchor (`outer_wall` / `inner_wall`): the engine computes the anchor's outward normal — the horizontal direction of the adjacent air that satisfied the wall classification (ties broken by position-hash roll). The fragment is rotated about the anchor by the yaw that maps canonical +Z onto that normal (0°/90°/180°/270°; offsets rotate as integer vectors).
3. `orientable: true` + horizontal anchor (`floor` / `rooftop` / `ceiling`): yaw is chosen uniformly from {0°, 90°, 180°, 270°} via position-hash roll — free rotation for visual variety (cars shouldn't all face north).
4. **Block state properties are rotated too:** directional properties (`facing`, `axis`, `rotation`) are remapped by the same yaw. v1 scope: `facing` (N/E/S/W values), `axis` (x↔z). Anything else passes through unchanged.
5. Rotation happens **before** clearance/footprint checks (§5 step e) — checks run on rotated offsets.

Shipped vehicles are horizontal `orientable` fragments and place their model
blocks at `y=1` above the floor anchor. This keeps the road surface intact while
still applying full rotated-footprint clearance.

---

## 5. Fragment Pass Execution Algorithm (normative)

```
1. Collect all positions in region whose surface class == pass.anchor_surface
   (or all classes if "any").
2. If call-time space_filter is set: keep anchors whose space class matches.
3. If only_safe_anchor_blocks: keep anchors whose block is in safe_blocks.replaceable.
4. Deterministic iteration: sort candidates by (x, y, z).
5. For each candidate anchor:
   a. Noise gate: sample noise at (x, z); skip if ≤ threshold.
   b. Density roll: H(seed, pass, "density", pos) ≥ effective_density → skip.
   c. min_spacing check against anchors already placed in this execution → skip if too close.
   d. Fragment selection: H(seed, pass, "select", pos) indexes the compatible fragment list.
      Orientation resolved per §4 (roll H(seed, pass, "yaw", pos) if needed).
   e. Clearance: footprint XZ extent must be within region bounds; if requires_clear_above,
      the min_clear_height air column above the anchor must be air.
   f. For each FragmentBlock (offsets rotated per §4):
      - Per-block roll H(seed, pass, "block", pos, offset) ≥ probability → skip.
      - preserve_existing and target non-air → skip.
      - Block is air and fragment not destructive → skip.
      - Emit change. Final write-time validation (never-touch list, marker
        protection, destructive air-gate) is the §12.1 choke point in ARCHITECTURE.md —
        it applies to every emitted position, not just the anchor. Note the
        §12.1 air-transparency rule: fragment blocks written into air/air-like
        positions (most of them — rubble stacks, vines, tree canopies) pass the
        replaceable-whitelist check unconditionally; only blocks that overwrite
        an existing solid block need whitelist membership.
   g. The unique positions actually emitted by this fragment instance form one
      atomic group when there is more than one. Probability/preserve skips are
      excluded. If any member later fails the choke, the whole instance is
      dropped; overlapping groups fail transitively, preventing half vehicles.
```

All rolls use the position-hash scheme of `ARCHITECTURE.md` §6 — never a shared RNG stream.

### Engine interface

```python
class FragmentEngine:
    def __init__(self, fragment_library: FragmentLibrary,
                 safe_replaceable: set[str], structural_never_touch: set[str]) -> None: ...

    def preview(self, pass_def: dict, region: RegionData,
                seed: int = 42, intensity: float = 1.0,
                initial_placed_anchors: list[BlockPos] = []) -> PreviewResult: ...

    def apply(self, pass_def: dict, region: RegionData,
              seed: int = 42, intensity: float = 1.0,
              initial_placed_anchors: list[BlockPos] = []) -> RegionData: ...
```

`initial_placed_anchors` (implemented, backward-compatible — default empty): pre-seeded anchor positions consulted by step 5c's `min_spacing` check *before* this execution's own placements. The future brush compiler will pass accumulated anchors across per-space-kind executions; ordinary fragment passes leave it empty.

`StyleEngine` dispatches to `FragmentEngine` for `"type": "fragment_pass"` (see `ARCHITECTURE.md` §5).

---

## 6. Style Bundle

A Style Bundle maps structure types to ordered pass sequences. It is the top-level abstraction that makes the system one-command operable.

### Bundle JSON Format

```json
{
  "name": "tlou_complete",
  "description": "Full The Last of Us stylization — apply to an entire urban area",
  "version": "1.0",
  "default_seed": 42,
  "entries": [
    {
      "structure_type": "building",
      "passes": [
        {"name": "tlou_material_hints",      "intensity": 0.5,  "space_filter": "exterior"},
        {"name": "tlou_window_breach",        "intensity": 0.7,  "space_filter": "exterior"},
        {"name": "tlou_wall_collapse",        "intensity": 0.25},
        {"name": "tlou_vine_network",         "intensity": 0.9,  "space_filter": "exterior"},
        {"name": "tlou_roof_tree_growth",     "intensity": 0.6,  "space_filter": "exterior"},
        {"name": "tlou_rubble_scatter",       "intensity": 0.5,  "space_filter": "interior"},
        {"name": "tlou_floor_root_crack",     "intensity": 0.35, "space_filter": "interior"},
        {"name": "tlou_furniture_modreplace", "intensity": 1.0,  "space_filter": "interior"}
      ]
    },
    {
      "structure_type": "road",
      "passes": [
        {"name": "tlou_material_hints",    "intensity": 0.15},
        {"name": "tlou_rubble_scatter",    "intensity": 0.8},
        {"name": "tlou_abandoned_vehicles","intensity": 0.4}
      ]
    }
  ]
}
```

Every `passes[i].name` must reference a registered pass of any type. Fragment names are **not** valid here (v0.3's learning example implied they were; retracted — wrap fragments in a `fragment_pass`). Pass order within an entry is execution order and is meaningful (destructive passes early, furniture last — see `ARCHITECTURE.md` §1).

### Canonical pass order (building entry)

| Order | Pass | Type | Effect |
|---|---|---|---|
| 1 | `tlou_material_hints` | `block_pass` | ≤15% texture variation |
| 2 | `tlou_window_breach` | `fragment_pass` | Remove glass, fill with vines/leaves |
| 3 | `tlou_wall_collapse` | `fragment_pass` | Localized wall holes with rubble (destructive) |
| 4 | `tlou_vine_network` | `fragment_pass` | Vine anchors on outer walls |
| 5 | `tlou_roof_tree_growth` | `fragment_pass` | Trees breaking through rooftops |
| 6 | `tlou_rubble_scatter` | `fragment_pass` | Rubble piles on floors |
| 7 | `tlou_floor_root_crack` | `fragment_pass` | Root cracks through floors |
| 8 | `tlou_furniture_modreplace` | `pattern_replace` | Vanilla furniture → DD equivalents |

(Roads additionally use `tlou_abandoned_vehicles`.)

### `apply_bundle` — execution semantics (normative)

Two mutually exclusive scoping modes:

**Region mode (✅ implemented):** caller passes `cx`, `cz`, `radius_chunks`. There is no structure detection; **every entry's pass list runs over the whole region**, so `structure_type` acts only as a grouping label unless `structure_type_filter` narrows it. This mode exists so bundles are usable before the segmentation subsystem lands, and remains useful for small hand-picked areas. Agents should pass `structure_type_filter` in region mode when the area is homogeneous (e.g. a road segment); running the full multi-entry bundle over one region double-applies shared passes like `rubble_scatter`.

**Structure mode (🚧, requires segmentation):** caller passes no coordinates. For each entry, the executor looks up all registry structures with matching `structure_type` and runs the entry's passes clipped to each structure's bounds (interior/exterior filters use flood-fill tier-2 space classes). This is the intended end-state mode.

Calling with both or neither scope → `ambiguous_bundle_scope`.

**Common semantics:**
- `dry_run` (default **true**): aggregate preview, no writes. Applying requires explicit `dry_run: false`.
- Seed precedence: call `seed` > bundle `default_seed` > 42. One seed for the whole run; per-pass keys differ via the position-hash scheme, so passes don't correlate.
- Error handling: continue-on-error per pass; response collects `errors[]`; `ok` semantics per `ARCHITECTURE.md` §11.
- Each pass runs against the region state **including changes from earlier passes in the same run** (layering is the point); the write to disk happens per pass (region mode) or per structure-pass (structure mode), and each write is journaled individually (`ARCHITECTURE.md` §12.3).

**Input:**

```json
{
  "bundle_name": "tlou_complete",
  "cx": 0, "cz": 0, "radius_chunks": 8,
  "seed": 42,
  "dry_run": true,
  "structure_type_filter": "building"
}
```

**Output:** aggregated per-pass results — `passes_applied: [{structure_type, pass, intensity, space_filter, changed}]`, `total_changed`, `errors[]`, `dry_run`, plus `space_classification` tier and `noise_backend` (required field) markers.

**Region-mode warning (normative):** in region mode without `structure_type_filter`, the response **must always** include a `region_mode_warning` stating that every entry's passes run over the whole region. If any pass name appears in more than one entry, the warning additionally names those passes and the repeat count:

```json
"region_mode_warning": "Region mode without structure_type_filter: all 3 entries run over the full region regardless of their structure_type labels. Passes ['tlou_rubble_scatter'] appear in 2 entries and will be applied that many times. Pass structure_type_filter if the area is not homogeneous."
```

The warning is unconditional (not only on duplicate passes) because the mismatch failure mode has two shapes: duplicated effects (a shared pass runs N times) *and* misdirected effects (a building-entry pass runs over a road area, silently producing nothing useful). It fires in both `dry_run` modes, is advisory, and never blocks execution — intentional whole-region application is valid.

### Storage

Bundles are JSON files in `data/bundles/` (`PICASSO_BUNDLES_DIR`), auto-loaded at startup; invalid files are logged and skipped (resilient startup, `ARCHITECTURE.md` §10).

---

## 7. Source Layout (fragment subsystem)

```
src/picasso/
├── core/
│   ├── fragment_engine.py        ✅ placement logic (§4–§5)
│   ├── fragment_library.py       ✅ loads/indexes data/fragments/*.json
│   └── bundle_executor.py        ✅ ordered bundle orchestration
├── models/
│   └── fragment.py               ✅ Fragment, FragmentBlock
├── tools/
│   └── bundle.py                 ✅ list_bundles, apply_bundle (thin wrappers after extraction)
└── data/
    ├── fragments/                ✅ rubble_pile_small/medium, wall_breach, window_overgrown,
    │                                roof_tree_growth, floor_root_crack, vine_anchor,
    │                                abandoned_car{,_sedan,_van,_jeep}
    └── bundles/
        └── tlou_complete.json    ✅
```
