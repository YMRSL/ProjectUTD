# Tool API Specifications — Base Tools

> Tool registry with status lives in `ARCHITECTURE.md` §8. This file specifies the **base** tools (world I/O, analysis, catalog, style, NPC, learning). Bundle tools: `docs/fragment_system.md` §6. Fragment/bundle authoring tools: `docs/style_learning.md`. Segmentation tools: `docs/structure_detection_tool_specs.md`.

All tools return a JSON object with `"ok": true/false`. On failure, `"error"` (a code from `ARCHITECTURE.md` §11) and `"message"` are always present.

Write-capable tool responses include two provenance markers (`ARCHITECTURE.md` §4.5/§4.6): `"space_classification": "heuristic" | "flood_fill"` and `"noise_backend": "c" | "fallback"`.

---

## World I/O Tools

### `set_world` ✅

Open a Minecraft save directory for all subsequent operations. Must be called before any read/write tool.

**Input:**
```json
{ "world_path": "D:\\MC\\...\\saves\\MyWorld" }
```

**Output (success):**
```json
{ "ok": true, "level_name": "MyWorld", "version": "1.21.1", "world_path": "D:\\MC\\..." }
```

**Output (failure):**
```json
{ "ok": false, "error": "world_not_found", "message": "Path does not exist: ..." }
```

**Notes:**
- If a world is already open, it is closed first; `last_region` cache is cleared.
- Validates that Amulet can read the path before returning success.
- The world stays open (in `session.bridge`) until the next `set_world` or server exit.

---

### `read_region` ✅

Read block data from a square chunk area around a center chunk.

**Input:**
```json
{ "cx": 0, "cz": 0, "radius_chunks": 4 }
```
- `cx`, `cz`: center **chunk** coordinates (block coords ÷ 16, floored).
- `radius_chunks`: inclusive radius; 4 → 9×9 = 81 chunks. Must satisfy `0 ≤ radius_chunks ≤ PICASSO_MAX_RADIUS_CHUNKS` (default 12); above the cap → `region_too_large` with tiling advice.

**Output (success):**
```json
{
  "ok": true,
  "block_count": 14823,
  "chunks_read": 79,
  "chunks_missing": 2,
  "summary": "Read 79/81 chunks (cx=0±4, cz=0±4). 14823 non-air blocks. Y range: 58–94.",
  "bounds": { "min": {"x": -64, "y": 58, "z": -64}, "max": {"x": 79, "y": 94, "z": 79} }
}
```

**Notes:**
- Raw block data is **not** returned (too large for MCP); it is cached in `session.last_region`.
- Cache rules (`ARCHITECTURE.md` §4.1): reused when `(cx, cz, radius_chunks)` match exactly; invalidated by any non-dry write or `set_world`. Style/analysis tools call the shared `ensure_region` helper, so agents rarely need an explicit `read_region` — it exists for warming the cache and for orientation.
- Ungenerated chunks are skipped and counted in `chunks_missing`.

---

### `write_region` *(internal — not an MCP tool)*

Called internally by apply-type tools. Agents apply changes through style/bundle tools, never by constructing raw diffs. Every write passes the §12.1 choke-point validation and (🚧) is journaled per `ARCHITECTURE.md` §12.3.

---

## Analysis Tools

### `analyze_region` ✅

Semantic scan of a region: surface classification, vanilla furniture patterns, coverage statistics. Uses the region cache (`ensure_region`).

**Input:**
```json
{ "cx": 0, "cz": 0, "radius_chunks": 4 }
```

**Output:**
```json
{
  "ok": true,
  "surface_counts": { "floor": 3200, "outer_wall": 1850, "inner_wall": 980, "rooftop": 420, "ceiling": 310 },
  "top_blocks_by_surface": {
    "floor": [
      {"block": "minecraft:stone_bricks", "count": 1200, "fraction": 0.375},
      {"block": "minecraft:polished_andesite", "count": 800, "fraction": 0.25}
    ]
  },
  "pattern_matches": [
    {"pattern": "chair", "count": 34, "sample_pos": {"x": 12, "y": 65, "z": -8}},
    {"pattern": "table", "count": 18, "sample_pos": {"x": 20, "y": 65, "z": 4}}
  ],
  "vegetation_coverage": 0.02,
  "damage_estimate": 0.05,
  "space_classification": "heuristic",
  "summary": "Mostly clean urban area. 34 chairs, 18 tables. Low vegetation (2%). Good candidate for TLOU stylization."
}
```

Surface classes and their priority rules: `ARCHITECTURE.md` §4.5. `vegetation_coverage` = fraction of exterior-class blocks bearing vegetation blocks (vines/leaves/moss family). `damage_estimate` = heuristic fraction of wall/roof surfaces adjacent to unexpected air gaps.

---

## Catalog Tools

### `query_catalog` ✅

Query the DD semantic catalog. All parameters optional AND-filters; list params match any-of within the field.

**Input:**
```json
{
  "category": "furniture",
  "surface": ["floor"],
  "context": ["indoor"],
  "tags": ["rusted"],
  "function": "decorative_seating"
}
```

**Output:**
```json
{
  "ok": true,
  "count": 3,
  "blocks": [
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
  ]
}
```

If the catalog file was absent at startup: `{"ok": false, "error": "catalog_not_loaded", ...}` (resilient startup, `ARCHITECTURE.md` §10).

---

## Style Tools

### `list_passes` ✅

**Input:** *(none)*

**Output:**
```json
{
  "ok": true,
  "passes": [
    {
      "name": "tlou_rubble_scatter",
      "type": "fragment_pass",
      "description": "Scatter small rubble piles across floors and streets",
      "version": "1.0",
      "deprecated": false,
      "targets": ["floor"],
      "rule_count": 0,
      "fragment_count": 2
    },
    {
      "name": "tlou_structural_damage",
      "type": "block_pass",
      "deprecated": true,
      "description": "[DEPRECATED — superseded by wall_collapse fragment pass] ...",
      "version": "1.0",
      "targets": ["outer_wall"],
      "rule_count": 4
    }
  ]
}
```

---

### `preview_pass` ✅

Dry-run any pass type. Returns what would change; touches no disk state, never invalidates the cache.

**Input:**
```json
{
  "pass_name": "tlou_nature_reclaim",
  "cx": 0, "cz": 0, "radius_chunks": 4,
  "intensity": 0.8,
  "seed": 42,
  "space_filter": null
}
```
- `intensity` 0.0–1.0, default 1.0 — per-type semantics in `ARCHITECTURE.md` §5.1.
- `seed` — default 42. Determinism guarantee: `preview` result ≡ the diff `apply` would write, for identical inputs (`ARCHITECTURE.md` §6).
- `space_filter` — `"interior"` | `"exterior"` | null (`ARCHITECTURE.md` §5.2).

**Output:**
```json
{
  "ok": true,
  "would_change": 412,
  "by_rule": [
    {"rule_index": 0, "description": "stone_bricks → mossy variants", "count": 280},
    {"rule_index": 1, "description": "place vine on outer_wall", "count": 132}
  ],
  "sample_changes": [
    {"pos": {"x": 15, "y": 68, "z": -3}, "from": "minecraft:stone_bricks", "to": "minecraft:mossy_stone_bricks"},
    {"pos": {"x": 8, "y": 72, "z": 10}, "from": "air", "to": "minecraft:vine"}
  ],
  "space_classification": "heuristic",
  "noise_backend": "fallback",
  "summary": "412 blocks would change: mossy variants (280), vines (132)."
}
```
(For fragment passes, `by_rule` is replaced by `by_fragment` counts; for pattern passes, `by_pattern`.)

---

### `apply_pass` ✅

Execute a pass and write changes to the active world. Same input as `preview_pass`.

**Output:**
```json
{
  "ok": true,
  "changed": 412,
  "journal_entry": "picasso_journal/20260706T090000Z_apply_pass_42.json",
  "space_classification": "heuristic",
  "noise_backend": "fallback",
  "summary": "Applied tlou_nature_reclaim. 412 blocks changed. World saved."
}
```
(`journal_entry` 🚧 until §12.3 lands.) Invalidates `session.last_region`.

---

### `create_pass` ✅

Define a new **block pass** at runtime, validate it, save to `PICASSO_PASSES_DIR`, register.

**Input:**
```json
{
  "name": "custom_moss_heavy",
  "description": "Heavy moss coverage for deep jungle ruins",
  "rules": [
    {
      "match": {"surface": "outer_wall"},
      "action": "replace",
      "replace_with": [
        {"block": "minecraft:mossy_stone_bricks", "weight": 0.8},
        {"block": "minecraft:mossy_cobblestone", "weight": 0.2}
      ],
      "weight": 0.9,
      "noise": {"scale": 0.04, "threshold": 0.2}
    }
  ]
}
```

**Output:**
```json
{ "ok": true, "saved_path": "...\\data\\passes\\custom_moss_heavy.json", "message": "Pass 'custom_moss_heavy' created and registered." }
```

Runtime creation of fragment passes goes through `create_fragment` + a hand-authored fragment-pass JSON or `create_bundle` (see `docs/style_learning.md`); `create_pass` is block-pass-only by design (validation surface is per-type).

---

## Learning Tools

### `learn_style` 🚧

Analyze a reference region → `StyleProfile` (statistics + fragment-presence estimates). The profile is **diagnostic** — an agent reads it to decide what to author next; it is never executed. Extended output spec: `docs/style_learning.md` §7.

**Input:**
```json
{ "cx": 12, "cz": -5, "radius_chunks": 3, "name": "my_reference_ruins" }
```

**Output:**
```json
{
  "ok": true,
  "profile_name": "my_reference_ruins",
  "saved_path": "...\\data\\profiles\\my_reference_ruins.json",
  "matched_fragments": [
    {"fragment": "rubble_pile_small", "estimated_density": 0.08, "confidence": 0.75}
  ],
  "unmatched_cluster_count": 7,
  "suggested_extract_clusters": true,
  "summary": "38% mossy stone surfaces, 15% vine coverage, 8% structural damage. 7 unmatched recurring clusters — run extract_block_clusters."
}
```

### `apply_style_profile` ❌ retired (v0.4)

Removed from the tool surface. Rationale: profile→pass intensity inference was a second, weaker path to the same outcome as the cluster → `create_fragment` → `create_bundle` workflow, and its "suggested_passes" coupling to the deprecated v0.1 pass set made it misleading. Replay a learned style by applying the bundle the agent authored from the profile.

---

## NPC Interface Tools

### `place_npc_marker` ✅

Place an NPC spawn marker: a `minecraft:structure_void` block + companion JSON in `<world>/picasso_markers/`.

**Input:**
```json
{
  "x": 100, "y": 64, "z": -200,
  "npc_type": "key_npc",
  "faction": "survivor_camp",
  "facing": "south",
  "dialogue_id": null,
  "quest_id": null,
  "source_agent": null
}
```
- `npc_type`: `"key_npc"` | `"ambient"` | `"enemy"` | `"vendor"`
- `facing`: `"north"` | `"south"` | `"east"` | `"west"`
- Optional IDs are filled by the Narrative Layer later.

**Output:**
```json
{
  "ok": true,
  "marker_file": "D:\\...\\picasso_markers\\100_64_-200.json",
  "summary": "NPC marker placed at (100, 64, -200). Type: key_npc, faction: survivor_camp."
}
```

Marker positions are permanently protected from all passes (`ARCHITECTURE.md` §12.2). Placing a marker is a write: it invalidates the region cache.
