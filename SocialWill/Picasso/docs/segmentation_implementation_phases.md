# Semantic Segmentation — Implementation Phases

> Supplement to `docs/implementation_order.md` — tracks `ARCHITECTURE.md` v0.4.4. **Status: segmentation Phases 0/S1-S4 remain 🚧; Agent-evidence Phase A0 is ✅.** Prerequisite: base Phase 1 (AmuletBridge) ✅. Can proceed in parallel with base Phases 6/8.
>
> Before Phase 0: add `scipy>=1.11` to `pyproject.toml` dependencies (flood fill requires `scipy.ndimage.label`; it is **not** currently listed).

---

## Phase A0 — Agent Evidence Substrate ✅

**Goal:** let an Agent inspect bounded physical evidence and interpret local
semantic candidates without exposing whole-world block dumps or mutating a save.

Implemented:

1. `analyze_region.local_semantics` reports conservative stair assemblies and
   horizontal-level candidates with `scope=candidate_only`.
2. `inspect_volume` returns an inclusive, capped palette + Y/Z/X-RLE voxel view,
   complete/truncated markers, surfaces, properties, and block-entity positions.
3. FastMCP initialization instructions publish the honesty/no-implicit-write
   boundary; `interpret_world_structure` supplies the explicit review workflow.
4. Synthetic tests cover semantic assemblies, level candidates, RLE evidence,
   properties, limits, truncation, halo exclusion, and prompt registration.

Full contract: `docs/agent_semantic_review.md`.

### Done When

- A controlled fixture's known local structures are recovered from read-only
  analysis and an Agent can request their bounded geometry.
- No incomplete view can silently represent omitted/missing data as air.
- Prompt rendering never touches the shared world session.

---

## Phase 0 — Signal Extraction Foundation

**Goal:** Convert a `RegionData` into the 2D/3D signal arrays that all detectors consume.

### Tasks

1. Add `scipy` to `pyproject.toml` (see note above).
2. Create `src/picasso/core/segmentation/` package with `__init__.py`.
3. Create `src/picasso/core/segmentation/signal_extractor.py`:
   - `SignalExtractor(region: RegionData)` class
   - `compute_heightmap() -> np.ndarray` — shape (X, Z), dtype float32
   - `compute_density_map(voxel_size: int = 8) -> np.ndarray` — shape (X//8, Y//8, Z//8)
   - `compute_material_map() -> np.ndarray` — shape (X, Z), value = dominant namespace index
   - `compute_special_block_map(block_ids: list[str]) -> np.ndarray` — binary (X, Z)
   - All arrays use consistent coordinate offsets from `region.bounding_box().min`. Y range is −64…319 (1.21.1); never assume 0-based Y.
4. Create `src/picasso/models/structure.py`: `BoundingBox`, `StructureCandidate`, `Structure` dataclasses per `docs/semantic_segmentation.md` §6 (including `stale` flag, §7).

### Done When
- pytest: given a synthetic `RegionData` with known blocks, `compute_heightmap()` returns correct Y values at all (X, Z).
- pytest: `compute_density_map()` values ∈ [0.0, 1.0].

---

## Phase S1 — Flood Fill + Enclosed Volume Detector

**Goal:** Detect all enclosed air volumes (rooms, tunnels, basements). This also unlocks **tier-2 space classification** (`ARCHITECTURE.md` §4.5) for `space_filter`.

### Tasks

1. Create `src/picasso/core/segmentation/flood_fill.py`:
   - `label_air_volumes(region: RegionData) -> dict[int, VoidVolume]`
   - `scipy.ndimage.label` on a 3D binary air array (26-connected).
   - Classify each component: `outdoor_sky`, `indoor_room`, `underground_tunnel`, `basement`, `open_cavity` (per `docs/semantic_segmentation.md` §3.2).
2. Create `src/picasso/core/segmentation/enclosed_volume_detector.py`:
   - `EnclosedVolumeDetector` — groups labeled volumes into building candidates.
   - Stadium special case: single volume > `PICASSO_MIN_STADIUM_VOLUME` (default 2000) with concave top.
3. Wire tier-2 space classification: expose a `space_class_from_volumes(region, volumes)` helper the StyleEngine can consume when flood-fill results are available.

### Done When
- pytest: 5×5×5 enclosed stone room fixture → one `indoor_room` volume with correct bounds.
- pytest: open courtyard fixture → `outdoor_sky`.
- `preview_pass(..., space_filter="interior")` on a fixture reports `"space_classification": "flood_fill"`.

---

## Phase S2 — Flat Region + Linear + Elevated Platform Detectors

**Goal:** Detect roads, railways, bridges, and plazas.

### Tasks

1. Create `src/picasso/core/segmentation/flat_detector.py`:
   - Local variance over heightmap (sliding window, numpy); threshold `PICASSO_FLAT_VARIANCE_THRESHOLD`.
   - Connected components of flat cells; material-fingerprint sub-typing (road/plaza/beach/rail_yard).
2. Create `src/picasso/core/segmentation/linear_detector.py`:
   - Rail positions → connectivity graph → PCA per component → orientation, length, elevation profile → `ground_rail` / `subway_line` / `elevated_rail` / `highway`.
3. Create `src/picasso/core/segmentation/elevated_platform_detector.py`:
   - Elevated regions (Y > ground + 8) from heightmap; support-column scan via density map → `bridge` / `overpass` / `elevated_walkway` / `dock` / `pier`.

### Done When
- pytest: flat road at Y=64 → `road`.
- pytest: straight rail line → `ground_rail` with correct orientation vector.
- pytest: elevated platform with 2 supports → `bridge`.

---

## Phase S3 — Underground + Water Structure Detectors

**Goal:** Detect subway systems, tunnels, ships, and harbor structures.

### Tasks

1. Create `src/picasso/core/segmentation/underground_detector.py`:
   - Underground volumes from flood fill; PCA on tunnel segments; platform-width detection → `subway_system` / `utility_tunnel` / `underground_hall` / `bunker` / `basement`.
2. Create `src/picasso/core/segmentation/water_structure_detector.py`:
   - Solid blocks near water surface; PCA eccentricity → `ship`; adjacency → `dock` / `harbor_building` / `coastal_building`.

### Done When
- pytest: 5-wide corridor at Y=30 → `utility_tunnel`.
- pytest: elongated structure floating on water → `ship`.

---

## Phase S4 — Assembler, Registry, Identity & Tools

**Goal:** Merge detector candidates into a coherent, re-detection-stable Structure Registry, and expose the tool surface.

### Tasks

1. Create `src/picasso/core/segmentation/structure_assembler.py`:
   - `assemble(candidates) -> list[Structure]`: overlap resolution, merge rules, relationship recording (`docs/semantic_segmentation.md` §5).
   - Fingerprint matching against `data/structure_fingerprints.json` → `sub_type`, `confidence`. **Algorithm: rule-based signal scoring, matching the shipped file's actual schema** (the file is per-`sub_type` entries with a `signals` object and a `confidence_boost` — *not* a feature vector; an earlier cosine-similarity sketch in this doc was misaligned with the shipped data and is retracted):
     1. For each assembled Structure, evaluate each fingerprint entry whose `parent_type` matches the structure's detected `type`.
     2. Each signal in the entry's `signals` object is a predicate over computed structure properties: `dominant_materials_include` (≥1 listed material in the structure's top-5 materials), range checks (`height_range`, `footprint_area`, `enclosed_volume`, `footprint_aspect_ratio`, …), `special_blocks_present` (≥1 present), boolean flags (`tiered_profile`).
     3. Entry score = fraction of its signals that pass (each signal equally weighted in v1).
     4. Best entry with score ≥ **0.6** wins: `sub_type` = entry's, `confidence = min(1.0, base_confidence + confidence_boost × score)` where `base_confidence` comes from the detector.
     5. No entry reaches 0.6 → `sub_type = null`, confidence unchanged.
     6. Signals referencing properties the assembler didn't compute are skipped and excluded from the denominator (a fingerprint using a future signal doesn't penalize present structures); if >50% of an entry's signals are skipped, the entry is ineligible.
     The **shipped JSON schema is the stable interface**; the scoring function is swappable (upgrade path: per-signal weights, then a learned classifier once labeled examples accumulate).
2. Create `src/picasso/core/segmentation/structure_registry.py`:
   - Load/save `<world>/picasso_structures.json`.
   - **Identity resolution on re-detection** per `docs/semantic_segmentation.md` §7 (IoU ≥ 0.5 greedy matching; containment-ratio ≥ 0.7 when either side is `partial`; override preservation; full-containment-only stale marking).
   - Lookups: `get_by_id`, `get_by_type`, `get_at_pos`.
3. Create `src/picasso/tools/segmentation.py`: all **8** tools — read-only `scan_semantic_candidates`, `get_candidate_evidence`, then `detect_structures`, `list_structures`, `get_structure`, `annotate_structure`, `apply_pass_to_structure`, `apply_pass_by_type` (`docs/structure_detection_tool_specs.md`). Candidate/evidence responses are paginated/run-scoped and follow `docs/agent_semantic_review.md`.
4. Enable `apply_bundle` **structure mode** (`docs/fragment_system.md` §6).

### Done When
- pytest: fixture with 3 buildings + 1 road + 1 rail → ≥5 structures.
- pytest: re-running detection on the same fixture preserves IDs and an `annotate_structure` override (identity rule test).
- `list_structures(type_filter="building")` returns only buildings; stale entries hidden by default.
- `apply_pass_to_structure(..., space_filter="exterior")` changes only exterior blocks.
- `apply_bundle` without coordinates runs in structure mode against the registry.

---

## Dependency Graph

```
Base Phase 1 (Amulet Bridge) ✅
    ├── Phase A0 (Agent Evidence) ✅
    └── Phase 0 (Signal Extraction)
            ├── Phase S1 (Flood Fill)  ──→ unlocks tier-2 space_filter
            ├── Phase S2 (Flat + Linear + Elevated)
            └── Phase S3 (Underground + Water; needs S1's flood fill)
                    └── Phase S4 (Assembler + Registry + Identity + Tools + bundle structure mode)
```

S2 can run in parallel with S1; S3 depends on S1's flood-fill output.
