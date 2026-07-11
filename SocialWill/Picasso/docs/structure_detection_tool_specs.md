# Structure Detection Tool Specs

> Supplement to `docs/tool_specs.md` — tracks `ARCHITECTURE.md` v0.4.4. **Status: all 8 tools 🚧 (spec only, not built — see `docs/segmentation_implementation_phases.md` Phase S4).** The already implemented local evidence substrate (`analyze_region`, `inspect_volume`, MCP prompt) is specified in `docs/tool_specs.md` and `docs/agent_semantic_review.md`; it is not formal segmentation. Structure identity across re-detection: `docs/semantic_segmentation.md` §7.

---

## Segmentation Tools

### `scan_semantic_candidates` *(read-only)*

Run selected S-phase detectors and assemble temporary candidates without writing
the Structure Registry. This is the Agent review/preflight surface.

**Input:**

```json
{
  "cx": 0,
  "cz": 0,
  "radius_chunks": 8,
  "detectors": ["enclosed_volume", "flat", "linear"],
  "cursor": null,
  "max_items": 20
}
```

**Output:**

```json
{
  "ok": true,
  "read_only": true,
  "run_id": "scan_01J...",
  "candidate_count": 42,
  "candidates": [
    {
      "candidate_id": "cand_0017",
      "detector": "enclosed_volume",
      "bounds": {"x_min": 10, "x_max": 30, "y_min": 60, "y_max": 78, "z_min": 5, "z_max": 25},
      "hypotheses": [{"kind": "building", "score": 0.82}],
      "ambiguity": 0.18,
      "partial": false,
      "evidence_ref": "scan_01J.../cand_0017"
    }
  ],
  "next_cursor": "...",
  "space_classification": "flood_fill"
}
```

The run cache is bound to the current open-world session and is invalidated by
`set_world`, `close_world`, or any Picasso write. Minecraft/third-party editing
while the world is open in Picasso remains prohibited, so external mutation is
not treated as a supported cache-coherence case. `max_items` is capped at 20;
totals and `next_cursor` make pagination explicit.

---

### `get_candidate_evidence` *(read-only)*

Return progressive, referenceable evidence for one temporary candidate. It never
returns an unbounded coordinate dump.

**Input:**

```json
{
  "evidence_ref": "scan_01J.../cand_0017",
  "detail": "evidence",
  "view": null,
  "slice_y": null
}
```

`detail`: `summary | evidence | slice`. `view` may request a bounded palette/RLE
slice derived through the same encoder as `inspect_volume`; a slice must obey its
32x24x32 and run limits.

**Output:**

```json
{
  "ok": true,
  "read_only": true,
  "evidence_ref": "scan_01J.../cand_0017",
  "facts": [
    {"id": "f1", "kind": "horizontal_levels", "value": [64, 69]},
    {"id": "f2", "kind": "enclosed_volumes", "count": 4},
    {"id": "f3", "kind": "vertical_connector", "value": "functional_staircase"}
  ],
  "top_materials": [{"block": "minecraft:stone_bricks", "count": 850}],
  "relationships": [],
  "contradictions": [],
  "partial": false,
  "omitted": {"block_samples": 326}
}
```

Agent interpretations cite `basis: ["f1", ...]`, alternatives, and confidence as
defined by `docs/agent_semantic_review.md`. Accepting a candidate later copies a
compact evidence digest/summary into `detected.*`; raw scan evidence is not stored
in `picasso_structures.json`.

---

### `detect_structures`

Run the full multi-signal detection pipeline over a region. This is a compute-heavy operation — expect it to take several seconds on large regions.

**Input:**
```json
{
  "cx": 0,
  "cz": 0,
  "radius_chunks": 12,
  "detectors": ["flat", "enclosed_volume", "linear", "elevated_platform", "underground", "water_structure"]
}
```
- `detectors`: optional list to run only specific detectors. Omit to run all.
- Typical radius for a city block: 8–16 chunks.

**Output:**
```json
{
  "ok": true,
  "structure_count": 47,
  "structures_file": "D:\\...\\saves\\MyWorld\\picasso_structures.json",
  "summary": "Detected 47 structures: 23 buildings, 4 road segments, 2 rail lines (1 underground), 1 elevated bridge, 1 ship, 3 plazas, 13 unclassified.",
  "by_type": {
    "building": 23,
    "road": 4,
    "subway_line": 1,
    "ground_rail": 1,
    "bridge": 1,
    "ship": 1,
    "plaza": 3,
    "unknown": 13
  }
}
```

**Notes:**
- `radius_chunks` respects `PICASSO_MAX_RADIUS_CHUNKS` (default 12); larger areas are covered by multiple overlapping runs — the §7 identity rules merge results across runs.
- Results are written to `<world>/picasso_structures.json`. Entries with `manual_override` keep their overridden fields; IDs are stable for continuously-existing structures (IoU matching, `docs/semantic_segmentation.md` §7).
- Structures that vanish from a re-scanned area are marked `stale`, never deleted.
- The `structures_file` path is returned so the calling AI can instruct the user to open it for review.

---

### `list_structures`

List detected structures in the active world's registry.

**Input:**
```json
{
  "type_filter": "building",
  "bounds_filter": {"x_min": 0, "x_max": 500, "z_min": -300, "z_max": 300},
  "include_stale": false
}
```
All filters optional. Stale entries (§7) are hidden unless `include_stale: true`.

**Output:**
```json
{
  "ok": true,
  "count": 12,
  "structures": [
    {
      "id": "struct_0001",
      "type": "building",
      "sub_type": "residential",
      "confidence": 0.82,
      "centroid": {"x": 130, "y": 72, "z": -170},
      "footprint_area": 3600,
      "height_range": 25,
      "manual_override": null
    }
  ]
}
```

---

### `get_structure`

Get full details of one structure.

**Input:**
```json
{ "structure_id": "struct_0001" }
```

**Output:**
```json
{
  "ok": true,
  "structure": {
    "id": "struct_0001",
    "type": "building",
    "sub_type": "residential",
    "confidence": 0.82,
    "bounds": {"x_min": 100, "x_max": 160, "y_min": 60, "y_max": 85, "z_min": -200, "z_max": -140},
    "centroid": {"x": 130, "y": 72, "z": -170},
    "footprint_area": 3600,
    "height_range": 25,
    "volume_estimate": 8200,
    "dominant_materials": ["minecraft:stone_bricks", "minecraft:oak_planks", "minecraft:glass_pane"],
    "interior_spaces": ["void_023", "void_024", "void_025"],
    "relationships": [{"type": "adjacent_to", "target_id": "struct_0002"}],
    "manual_override": null,
    "detected_at": "2026-07-05T11:00:00Z"
  }
}
```

---

### `annotate_structure`

Apply a human or AI correction to a detected structure. Sets `manual_override` — the entry will not be overwritten by future `detect_structures` calls.

**Input:**
```json
{
  "structure_id": "struct_0042",
  "corrections": {
    "type": "stadium",
    "sub_type": "sports_complex",
    "bounds": {"x_min": 200, "x_max": 350, "y_min": 60, "y_max": 95, "z_min": 100, "z_max": 280}
  }
}
```

**Output:**
```json
{
  "ok": true,
  "message": "struct_0042 annotated. manual_override set. Overridden fields survive future detection runs (identity rules: semantic_segmentation.md §7)."
}
```

---

### Structure-scoped apply — common semantics (normative for both tools below)

- **`dry_run` defaults to `true`** on both tools (core principle #2: every destructive tool has a dry-run; the earlier draft omitted it on `apply_pass_to_structure` — fixed).
- **Responses carry the standard write-tool fields** (`docs/tool_specs.md` header): `noise_backend` (required), `space_classification` (required — structure-scoped applies should report `"flood_fill"` once S1 lands, since structure mode implies segmentation ran), `reversibility_warning`, and the modded-write gate (`modded_write_unverified` / `force_modded_write`) all apply exactly as for `apply_pass`.
- **Bounds → chunk mapping:** the executor computes the chunk AABB covering the structure's block bounds (+1-chunk halo per Phase 3.5) and reads those chunks. If the chunk AABB exceeds `PICASSO_MAX_RADIUS_CHUNKS`² in area, the executor **tiles internally** (row-band tiles within the cap, ≥1-chunk overlap, changes deduplicated by position — position-hash determinism §6 makes overlapping recomputation idempotent, so tiling is safe). It never returns `region_too_large` for a registry structure; the cap protects against runaway *requests*, not against legitimately large *structures*. Blocks outside the structure's bounds are read (context) but never written.
- **Zero matches:** `apply_pass_by_type` with a type matching no registry structures returns `ok: true` with `structures_affected: 0` and a `no_match_warning` — not an error (an empty map region is a valid state), but never silent.

### `apply_pass_to_structure`

Apply a Style Pass scoped to a specific detected structure. Reads the structure's bounds from the registry automatically.

**Input:**
```json
{
  "pass_name": "tlou_nature_reclaim",
  "structure_id": "struct_0001",
  "intensity": 0.8,
  "seed": 42,
  "space_filter": "exterior",
  "dry_run": true
}
```
- `space_filter`: optional. `"exterior"` only modifies blocks classified as exterior (outdoor surfaces). `"interior"` only touches indoor spaces. Omit to apply everywhere within structure bounds.

**Output:**
```json
{
  "ok": true,
  "structure_id": "struct_0001",
  "pass": "tlou_nature_reclaim",
  "dry_run": true,
  "would_change": 387,
  "noise_backend": "fallback",
  "space_classification": "flood_fill",
  "reversibility_warning": null,
  "summary": "Preview: tlou_nature_reclaim on struct_0001 (exterior only) would change 387 blocks."
}
```
(`would_change` becomes `changed` when `dry_run: false`.)

---

### `apply_pass_by_type`

Apply a Style Pass to all structures of a given type in the registry. Useful for batch stylization ("apply decay to every building in the map").

**Input:**
```json
{
  "pass_name": "tlou_decay_surfaces",
  "structure_type": "building",
  "intensity": 0.7,
  "seed": 42,
  "dry_run": true
}
```

**Output:**
```json
{
  "ok": true,
  "dry_run": true,
  "structures_affected": 23,
  "total_would_change": 14820,
  "per_structure": [
    {"id": "struct_0001", "would_change": 412},
    {"id": "struct_0003", "would_change": 389}
  ],
  "noise_backend": "fallback",
  "space_classification": "flood_fill",
  "reversibility_warning": null,
  "summary": "Preview: tlou_decay_surfaces would change 14820 blocks across 23 buildings."
}
```
