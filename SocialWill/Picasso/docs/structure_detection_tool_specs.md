# Structure Detection Tool Specs

> Supplement to `docs/tool_specs.md`. **Status: all 6 tools 🚧 (spec only, not built — see `docs/segmentation_implementation_phases.md` Phase S4).** Structure identity across re-detection: `docs/semantic_segmentation.md` §7.

---

## Segmentation Tools

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

### `apply_pass_to_structure`

Apply a Style Pass scoped to a specific detected structure. Reads the structure's bounds from the registry automatically.

**Input:**
```json
{
  "pass_name": "tlou_nature_reclaim",
  "structure_id": "struct_0001",
  "intensity": 0.8,
  "seed": 42,
  "space_filter": "exterior"
}
```
- `space_filter`: optional. `"exterior"` only modifies blocks classified as exterior (outdoor surfaces). `"interior"` only touches indoor spaces. Omit to apply everywhere within structure bounds.

**Output:**
```json
{
  "ok": true,
  "structure_id": "struct_0001",
  "pass": "tlou_nature_reclaim",
  "changed": 387,
  "summary": "Applied tlou_nature_reclaim to struct_0001 (exterior only). 387 blocks changed."
}
```

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
  "dry_run": false
}
```
- `dry_run`: if `true`, returns what would change without writing.

**Output:**
```json
{
  "ok": true,
  "structures_affected": 23,
  "total_changed": 14820,
  "per_structure": [
    {"id": "struct_0001", "changed": 412},
    {"id": "struct_0003", "changed": 389}
  ],
  "summary": "Applied tlou_decay_surfaces to 23 buildings. 14820 blocks changed total."
}
```
