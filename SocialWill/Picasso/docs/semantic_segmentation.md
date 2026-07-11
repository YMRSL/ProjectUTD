# Semantic Segmentation & Structure Detection

> Supplement to `ARCHITECTURE.md` — tracks v0.4.4. Read after the main architecture document.
> Covers the automatic structure recognition subsystem (introduced v0.2; identity rules and Zone-cut updated in v0.4). **Status: 🚧 entirely unimplemented** — this is a build spec, not a description of working code. Tool API: `docs/structure_detection_tool_specs.md`; build order: `docs/segmentation_implementation_phases.md`.
>
> Requires `scipy` (flood fill via `scipy.ndimage.label`) — add to `pyproject.toml` when this subsystem starts.

---

## 1. Problem Statement

A Minecraft world of any real complexity contains structures that cannot be meaningfully stylized without understanding what they are. Applying a "decay" pass uniformly across a grid of chunks would moss a subway tunnel the same way it mosses a stadium. The stylization engine needs to know:

- Where are the building boundaries?
- Is a space indoors or outdoors?
- What kind of structure is this (road, bridge, underground, ship)?
- What is the floor, what is a ceiling, what is a void?

This requires a separate analysis pass — **semantic segmentation** — that runs before any style pass and produces a catalog of detected **Structures** with their spatial metadata and estimated type.

---

## 2. Core Concept: Multi-Signal Detection

No single algorithm can reliably detect a subway, a stadium, and a cruise ship in the same pass. The approach is a **pipeline of specialized detectors** that each look for independent signals, followed by an assembler that merges candidates:

```
RegionData (raw blocks)
       │
       ▼
┌──────────────────────────────────────────┐
│           SIGNAL EXTRACTION              │
│  heightmap │ void_map │ density_map      │
│  material_map │ special_block_map        │
└──────────────────────────────────────────┘
       │
       ▼ (parallel)
┌──────┬──────┬──────┬──────┬──────┬──────┐
│Flat  │Encl. │Linear│Elev. │Under-│Water │
│Det.  │Vol.  │Det.  │Plat. │grd.  │Str.  │
│      │Det.  │      │Det.  │Det.  │Det.  │
└──────┴──────┴──────┴──────┴──────┴──────┘
       │
       ▼
┌──────────────────────────────────────────┐
│        STRUCTURE ASSEMBLER               │
│  merge candidates, resolve overlaps,     │
│  compute metadata, fingerprint match     │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│         Structure Registry               │
│  structures.json (editable by humans)    │
└──────────────────────────────────────────┘
```

---

## 3. Signal Extraction

All signals are computed from a `RegionData` using numpy array operations. The signals are 2D (projected from Y) or 3D volumetric arrays.

### 3.1 Heightmap (2D)

A numpy array of shape `(X, Z)`. Each cell contains the Y coordinate of the highest non-air solid block at that (X, Z). Used to detect surface profile, flat areas, and edges.

Computed sub-signals from the heightmap:
- **Gradient magnitude**: `np.gradient()` → sharp edges = building walls, terrain breaks
- **Local variance**: sliding window std-dev → low variance = road/plaza, high variance = complex structure
- **Local maxima**: peaks = rooftops, elevated bridges

### 3.2 Void Map (3D Flood Fill)

Identifies all connected volumes of air and classifies each by its enclosure:

| Volume type | Condition |
|---|---|
| `outdoor_sky` | Connected to Y_max (open sky) |
| `indoor_room` | Fully enclosed, Y > ground |
| `underground_tunnel` | Fully enclosed, Y < average ground level |
| `basement` | Partially below ground, single entrance |
| `open_cavity` | Enclosed but with openings (doorways, windows) |

**Algorithm:**
1. Label all air blocks with a unique component ID via 3D flood fill (26-connected).
2. For each component, determine if it touches the world top boundary → `outdoor_sky`.
3. Compute the centroid Y of each enclosed component vs. average ground Y → underground vs. indoor.
4. Measure the "openness ratio" (surface area of component touching non-solid / total surface) → open_cavity vs. indoor_room.

Implementation uses `scipy.ndimage.label` on a 3D binary array of air positions.

### 3.3 Density Map (3D Voxel Grid)

A 3D array of shape `(X//8, Y//8, Z//8)`. Each cell is the fraction of solid blocks within an 8×8×8 voxel. Used to distinguish:
- High density = structural core of a building
- Medium density = furnished interior, scaffolding
- Low density = open outdoor area, sky
- Near-zero = open air

### 3.4 Material Fingerprint Map (2D)

For each (X, Z) column, record the material composition vector (fraction of each block namespace). Projected to 2D, this distinguishes:
- Stone-dominant columns = buildings, walls
- Sand/water-dominant columns = beach
- Rail-containing columns = transport infrastructure
- Mod-block-rich columns = decorated structures

### 3.5 Special Block Map (2D)

Binary maps for blocks that reliably indicate structure type:
- Rail blocks (`minecraft:rail`, `minecraft:powered_rail`) → transport
- Water blocks → coastal/aquatic zone
- Chain / iron bars → industrial
- Beds → residential interior
- Spawner → dungeon / combat area
- Specific mod blocks (e.g. aeronautics blocks → aircraft)

---

## 4. Detector Algorithms

Each detector receives all signal arrays and returns a list of `StructureCandidate` objects.

### 4.1 Flat Region Detector → Roads, Plazas, Beaches, Airstrips

**Input:** heightmap, local variance map

**Algorithm:**
1. Apply threshold on local variance: cells with variance < `flat_variance_threshold` (default: 1.5) are "flat".
2. Connected-component analysis on flat cells → candidate flat zones.
3. Filter by minimum area (default: 100 blocks²).
4. Classify sub-type using material fingerprint:
   - Stone/concrete/gravel dominant → road or plaza
   - Sand dominant + adjacent water → beach
   - Gravel + rail blocks → rail yard

**Output type:** `road`, `plaza`, `beach`, `rail_yard`, `flat_unknown`

### 4.2 Enclosed Volume Detector → Buildings, Stadiums, Tunnels

**Input:** void_map (labeled enclosed volumes)

**Algorithm:**
1. For each enclosed volume from void_map:
   - Compute bounding box.
   - If Y-centroid > ground: classify as `interior_space`.
   - If Y-centroid < ground: classify as `underground_space`.
2. Group spatially overlapping interior spaces by their shared structural shell (blocks forming the walls between adjacent volumes).
3. Each group = one `building_candidate` containing multiple rooms.

**Special case (stadium):** Very large single enclosed volume (>2000 m³) with a concave/hollow top surface detected in heightmap → `stadium_candidate`.

**Output type:** `building`, `stadium`, `tunnel_section`, `cave`

### 4.3 Linear Structure Detector → Railways, Highways, Bridges (horizontal spans)

**Input:** special_block_map (rails), heightmap

**Algorithm:**
1. Extract all rail block positions.
2. Build a connectivity graph (adjacent/diagonal rails connected).
3. Connected components → individual track segments.
4. For each segment, compute PCA on block positions:
   - Principal axis = track orientation vector
   - Extent along principal axis = track length
   - Elevation profile along axis → flat (ground rail), sloped (approach ramp), elevated (bridge or overpass)
5. Y-profile classification:
   - Constant Y ≈ ground Y → `ground_rail`
   - Constant Y < ground Y → `subway_line`
   - Y significantly > ground → `elevated_rail` or `bridge`

Same algorithm applies to multi-block-wide roads (detect connected flat stone/concrete at consistent Y, elongated shape via PCA).

**Output type:** `ground_rail`, `subway_line`, `elevated_rail`, `highway`

### 4.4 Elevated Platform Detector → Bridges, Highways, Overpasses, Docks

**Input:** heightmap, density_map

**Algorithm:**
1. Find all heightmap regions where local height significantly exceeds the median ground height (threshold: > ground_Y + 8 blocks).
2. For each elevated region:
   - Check if it has vertical supports below (columns of solid blocks between the platform and ground) → `bridge` or `overpass`
   - Check if it is adjacent to water → `dock` or `pier`
3. Supports are detected by scanning the voxel density map vertically under the platform.

**Output type:** `bridge`, `overpass`, `elevated_walkway`, `dock`, `pier`

### 4.5 Underground Structure Detector → Subway, Basements, Bunkers

**Input:** void_map underground volumes, heightmap

**Algorithm:**
1. Take all void_map components classified as `underground_tunnel` or `basement`.
2. For linear underground tunnels: apply PCA → direction vector, length, branching factor.
   - Branching tunnels with platforms (wider sections) → `subway_system`
   - Single long corridor → `utility_tunnel`
   - Large chamber → `underground_hall` or `bunker`
3. Platform detection: underground section with width > 3 blocks and adjacent staircase blocks → `subway_platform`

**Output type:** `subway_system`, `utility_tunnel`, `underground_hall`, `bunker`, `basement`

### 4.6 Water Structure Detector → Ships, Piers, Coastal Buildings

**Input:** heightmap, material_fingerprint_map, special_block_map

**Algorithm:**
1. Find all solid non-water blocks at or within 3 Y of the water surface level.
2. Connected components of these blocks → candidates.
3. Classify:
   - Component is mostly on top of water (water below >60% of footprint) + elongated (PCA eccentricity > 0.7) → `ship`
   - Component is partially on water, partially on land → `dock` or `harbor_building`
   - Component is adjacent to water but on land → `coastal_building`
4. For ships: compute heading (orientation via PCA).

**Output type:** `ship`, `dock`, `harbor_building`, `coastal_building`

---

## 5. Structure Assembler

After all detectors run in parallel, the assembler merges overlapping candidates and resolves conflicts.

```python
class StructureAssembler:
    def assemble(self, candidates: list[StructureCandidate]) -> list[Structure]: ...
```

**Merge rules:**
1. If two candidates of different types have overlapping bounding boxes, the higher-confidence one subsumes the lower, unless one is `underground_*` and the other is `building` (they can coexist vertically).
2. Linear structures (rails) that pass through buildings are not merged; they get a `passes_through` relationship recorded.
3. Adjacent candidates of the same type within 8 blocks are merged if their material fingerprint similarity > 0.75.

**Fingerprint matching:**
After assembly, each Structure is matched against `data/structure_fingerprints.json` (see §8). The best match updates `sub_type` and `confidence` score.

### 5.1 Agent evidence review (normative)

The assembler owns reproducible geometry and detector hypotheses; it does not own
human intent. An MCP-connected Agent may review a candidate by combining its
bounds, detector facts, relationships, local patterns, and bounded voxel evidence
from `inspect_volume`. The Agent returns ranked interpretations, alternatives,
confidence, and cited evidence. This review is advisory until an explicit human
or authorized annotation operation stores it under `authored.*`.

The full responsibility and promotion contract is
`docs/agent_semantic_review.md`. In particular:

- aggregate counts are not sufficient evidence for room/building names;
- `storey_level_candidate` is not an ordinal floor until a host building, ground
  reference, overlapping horizontal levels, and vertical connectivity exist;
- partial/truncated evidence must be expanded before absence is interpreted;
- one reviewed example cannot silently rewrite a global Pattern or fingerprint;
- `detected.*` remains pipeline-owned and `authored.*` remains review-owned.

Phase S4 adds read-only `scan_semantic_candidates` and
`get_candidate_evidence` before the existing registry/write tools. Candidate
evidence is paginated and run-scoped. When a candidate is accepted into the
registry, Picasso stores a compact evidence digest/summary in `detected.*`; it
does not persist raw chunk dumps. If re-detection changes that digest while an
authored semantic review exists, readers expose `review_stale: true` rather than
silently treating an old explanation as freshly verified.

---

## 6. Structure Data Model

```python
@dataclass
class Structure:
    id: str                      # "struct_0001"
    type: str                    # "building", "subway_system", "ship", etc.
    sub_type: str | None         # "residential", "stadium", "cruise_ship", etc.
    confidence: float            # 0.0–1.0 detector confidence
    bounds: BoundingBox          # {x_min, x_max, y_min, y_max, z_min, z_max}
    centroid: BlockPos           # geometric center
    footprint_area: int          # XZ area in blocks²
    height_range: int            # y_max - y_min
    volume_estimate: int         # approximate enclosed volume in blocks³
    dominant_materials: list[str] # top-5 block IDs by frequency
    interior_spaces: list[str]   # IDs of enclosed void volumes belonging to this structure
    relationships: list[dict]    # [{type: "passes_through", target_id: "struct_0002"}]
    manual_override: dict | None # human corrections; if set, overrides auto-detected fields
    detected_at: str             # ISO timestamp
```

```python
@dataclass
class BoundingBox:
    x_min: int; x_max: int
    y_min: int; y_max: int
    z_min: int; z_max: int

    def contains(self, pos: BlockPos) -> bool: ...
    def intersects(self, other: BoundingBox) -> bool: ...
    def center(self) -> BlockPos: ...
    def xz_area(self) -> int: ...
```

---

## 7. Structure Identity Across Re-Detection (normative, new in v0.4)

The correction workflow (§8) promises that human overrides survive re-detection. That promise is only meaningful with a rule for deciding when a newly detected candidate **is** an existing registry entry. Sequential IDs (`struct_0001`) are labels, not identity.

**Partial detections:** a candidate whose bounds touch the scan boundary (any face of its AABB within 1 chunk of the scanned area's edge) is flagged **`partial: true`** — its true extent is unknown because the structure may continue beyond the scan. Partial entries are first-class registry citizens (annotatable, targetable), but their identity matching differs below. A later scan that covers the structure completely clears the flag.

**Matching rule:** for each new candidate `C` against each registry entry `E` of a *compatible* type:

1. Compatible types: exact type match, or either side's type is `unknown`, or both are in the same family (`{ground_rail, elevated_rail, subway_line}`, `{road, plaza, highway}`, `{building, stadium}`).
2. **Neither side partial:** score = 3D bounding-box IoU (intersection volume / union volume); match threshold **0.5**.
3. **Either side partial:** score = **containment ratio** — `intersection volume / min(volume(C), volume(E))`; match threshold **0.7**. Rationale: a stadium half-captured by an earlier scan has clipped bounds; against the full detection, IoU can fall below 0.5 (the clipped box is a fraction of the true one) and the manual correction on the clipped entry would be orphaned into a stale duplicate. Containment asks "is the smaller box essentially inside the bigger one?", which is the right question when one box is known-truncated.
4. Candidate–entry pairs at/above their threshold are matched greedily, highest score first; each entry matches at most one candidate.

**Entry schema: two namespaces (normative — decided in review round 4, H8).** A registry entry holds two disjoint kinds of data, and the update semantics are defined per namespace, not per field:

```json
{
  "id": "struct_0042",
  "detected": {
    "type": "building", "sub_type": "residential", "confidence": 0.8,
    "bounds": {...}, "partial": false, "dominant_materials": [...],
    "volume_estimate": 30200, "detected_at": "...",
    "evidence_digest": "sha256:...", "evidence_summary": {...}
  },
  "authored": {
    "manual_override": {"type": "stadium"},
    "semantic_reviews": [{"interpretation": {...}, "basis": ["f1", "f2"], "evidence_digest": "sha256:..."}],
    "interior_graph": {...},
    "room_instantiations": [...],
    "annotations": {...}
  },
  "stale": false
}
```

- **`detected.*`** is owned by the detection pipeline: re-detection may refresh any of it (subject to the override shadowing below).
- **`authored.*`** is owned by agents/humans (`annotate_structure`, room-graph editing, future layers): **re-detection never touches this namespace.** Without this split, a re-run of `detect_structures` — a routine operation — would erase an afternoon of room-graph authoring under the old "replace all detected fields" rule; and every future authored field would need to be individually remembered in the exemption list. `manual_override` is simply the first resident of `authored`.
- Where an `authored.manual_override` key shadows a `detected` key (e.g. `type`), readers resolve authored-over-detected; detection refreshes the detected value underneath without effect on consumers until the override is removed.
- An authored semantic review retains the evidence digest it reviewed. A digest mismatch after re-detection sets a derived `review_stale` warning; it never deletes the review or blocks the refreshed detected facts.

**Update semantics:**

| Case | Action |
|---|---|
| Matched (with or without overrides) | Keep entry ID and the **entire `authored` namespace** untouched; refresh `detected.*` from the new detection (bounds refresh includes the partial→full upgrade); update `detected_at` |
| Unmatched new candidate | Append with the next sequential ID, empty `authored` |
| Unmatched old entry whose bounds are **fully contained** in the re-scanned area | Mark `"stale": true` (never silently delete — a human may still reference it, and its `authored` data survives with it). `list_structures` hides stale entries unless `include_stale: true` |
| Unmatched old entry partially overlapping or outside the scanned area | Untouched — **partial overlap is not evidence of disappearance**; only a scan that covers the entry's entire bounds and fails to re-detect it may stale it. (Without this rule, every tile seam in a tiled workflow would stale the structures it slices through.) |

Structure IDs are therefore **stable across re-detection for continuously-existing structures** — including structures first seen clipped at a scan boundary — and everything under `authored` (corrections, interior graphs, room records) persists unconditionally per §8.

---

## 8. Zone Registry — CUT (v0.4)

The v0.2 draft promoted every Structure to a parallel "Zone" registry for pass targeting. **Cut in v0.4**: nothing consumed zones — bundles target `structure_type`, tools target `structure_id`, and both read bounds straight from the Structure Registry. `models/zone.py` and `core/zone_registry.py` are removed from scope. Registry loading and `get_structure_at_pos`-style lookups live in the segmentation package (`structure_registry.py`).

---

## 9. Human Correction Interface

All detected structures are saved to `<world>/picasso_structures.json`. This file is the **single source of truth** after detection. It is intentionally human-editable.

**Correction flow:**
1. `detect_structures()` writes `picasso_structures.json` with auto-detected results.
2. A human (or an Agent following `docs/agent_semantic_review.md` with cited evidence) proposes or confirms `type`, `sub_type`, `bounds`, room meaning, or another semantic annotation — directly in the file or via `annotate_structure`.
3. The edit sets `authored.manual_override` (e.g. `{"type": "stadium", "sub_type": "sports_complex"}`) on the entry.
4. Future `detect_structures()` runs preserve the entire `authored` namespace per the §7 identity/update rules.
5. New structures in newly scanned areas are appended; vanished structures are marked stale, never deleted.

**Multi-writer protocol (normative — H11).** This file has at least three writers: Picasso (detect / annotate / room-graph edits), humans editing directly (explicitly encouraged above), and potentially future layers. Picasso holding the registry in memory and bulk-writing it back would silently destroy concurrent hand edits (lost update — the same threat model as external marker writers, F1). Therefore:

1. **Read-merge-write:** before any write-back, Picasso re-reads the file and merges — entries it did not modify in this operation are taken from the file as-is; entries it did modify are written from memory. Field-level merge within one entry is not attempted (last writer wins per entry); the entry is the merge granule.
2. **Atomic write:** write to a temp file in the same directory, then rename over the original. A crash mid-write must never leave a truncated `picasso_structures.json` — this file is the single source of truth.
3. Same two rules apply to every per-world JSON Picasso writes (`picasso_rooms.json`, journal entries — journal entries are append-only files, so atomicity alone suffices there).

---

## 10. New Source Layout Additions

```
src/picasso/
├── core/
│   ├── segmentation/                ← NEW PACKAGE (all 🚧)
│   │   ├── __init__.py
│   │   ├── signal_extractor.py      # Heightmap, void map, density map, material map
│   │   ├── flood_fill.py            # 3D flood fill, enclosed volume labeling (scipy)
│   │   ├── flat_detector.py         # Flat region → road/plaza/beach
│   │   ├── enclosed_volume_detector.py  # Buildings, stadiums
│   │   ├── linear_detector.py       # Rails, highways (PCA-based)
│   │   ├── elevated_platform_detector.py # Bridges, docks
│   │   ├── underground_detector.py  # Subway, tunnels, basements
│   │   ├── water_structure_detector.py  # Ships, harbors
│   │   ├── structure_assembler.py   # Merge, fingerprint match, §7 identity resolution
│   │   ├── structure_registry.py    # Load/save picasso_structures.json, spatial lookups
│   │   └── cluster_extractor.py     # extract_block_clusters (docs/style_learning.md §3)
│
├── models/
│   └── structure.py                 ← NEW: Structure, BoundingBox, StructureCandidate
│
├── tools/
│   └── segmentation.py              ← NEW: 6 MCP tools (docs/structure_detection_tool_specs.md)
│
└── data/
    └── structure_fingerprints.json  ✅ already shipped
```

(`models/zone.py`, `core/zone_registry.py`: cut, §8.)

---

## 11. Configuration Parameters

Authoritative table: `ARCHITECTURE.md` §10. Segmentation-relevant entries: `PICASSO_FINGERPRINTS_PATH`, `PICASSO_FLAT_VARIANCE_THRESHOLD`, `PICASSO_MIN_STRUCTURE_AREA`, `PICASSO_MIN_ROOM_VOLUME`, `PICASSO_MIN_STADIUM_VOLUME`, `PICASSO_GROUND_DETECTION_RADIUS`, `PICASSO_MAX_RADIUS_CHUNKS`.

The structure registry path is fixed at `<world>/picasso_structures.json` (per-world artifact — the v0.2 `PICASSO_STRUCTURES_DIR` env var is retracted).
