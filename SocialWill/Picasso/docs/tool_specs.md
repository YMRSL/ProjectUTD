# Tool API Specifications — Base Tools

> Tracks `ARCHITECTURE.md` v0.4.4. Tool registry with status lives in `ARCHITECTURE.md` §8. This file specifies the **base** tools (world I/O, analysis, catalog, style, NPC, learning). Bundle tools: `docs/fragment_system.md` §6. Fragment/bundle authoring tools: `docs/style_learning.md`. Segmentation tools: `docs/structure_detection_tool_specs.md`.

All tools return a JSON object with `"ok": true/false`. On failure, `"error"` (a code from `ARCHITECTURE.md` §11) and `"message"` are always present.

Write-capable tool responses include two provenance markers (`ARCHITECTURE.md` §4.5/§4.6): `"space_classification": "heuristic" | "flood_fill"` and `"noise_backend": "c" | "fallback"`. **`noise_backend` is a required field** (not optional). The backend is pinned per session via `PICASSO_NOISE_BACKEND` and reported once in `set_world`; within a session it cannot drift. Cross-session/machine: if the backend differs from the one recorded with earlier previews, re-preview before applying (`ARCHITECTURE.md` §4.6/§6).

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
{
  "ok": true, "level_name": "MyWorld", "version": "1.21.1", "world_path": "D:\\MC\\...",
  "journal_status": "active",
  "noise_backend": "fallback"
}
```
`journal_status` values: `"active"` means the per-world durable journal is ready;
`"unavailable"` means writes fail closed until journal activation succeeds.

`noise_backend` reports the session-pinned backend (`ARCHITECTURE.md` §4.6) — it cannot change until server restart, so the agent reads it once here.

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
- Cache rules (`ARCHITECTURE.md` §4.1): reused when `(cx, cz, radius_chunks, resolved_y_min, resolved_y_max)` match exactly; invalidated by any non-dry write, revert, or `set_world`. Reads include a one-chunk horizontal halo and bounded vertical context, both read-only.
- Ungenerated chunks are skipped and counted in `chunks_missing`.

---

### `write_region` *(internal — not an MCP tool)*

Called internally by apply-type tools. Agents apply changes through style/bundle tools, never by constructing raw diffs. Every write passes §12.1 validation and requires a durable §12.3 journal transaction.

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
  "local_semantics": {
    "scope": "candidate_only",
    "stair_assemblies": [
      {
        "kind": "functional_staircase",
        "confidence": 0.93,
        "anchor": {"x": 8, "y": 64, "z": 12},
        "bounds": {"min": {"x": 8, "y": 64, "z": 8}, "max": {"x": 8, "y": 68, "z": 12}},
        "member_count": 9,
        "role_counts": {"step": 5, "underfill": 4}
      }
    ],
    "stair_assembly_count": 1,
    "stair_assemblies_truncated": false,
    "storey_level_candidates": [
      {"type": "storey_level_candidate", "y": 64, "area": 80, "bounds": {"x_min": 0, "x_max": 9, "y_min": 64, "y_max": 64, "z_min": 0, "z_max": 7}, "confidence": 1.0}
    ],
    "storey_level_candidate_count": 1,
    "storey_level_candidates_truncated": false
  },
  "vegetation_coverage": 0.02,
  "damage_estimate": 0.05,
  "space_classification": "heuristic",
  "summary": "Mostly clean urban area. 34 chairs, 18 tables. Low vegetation (2%). Good candidate for TLOU stylization."
}
```

Surface classes and their priority rules: `ARCHITECTURE.md` §4.5. `local_semantics.scope=candidate_only` is an honesty boundary: stair assemblies are local geometric interpretations and storey-level entries are connected horizontal supports, not building/room segmentation or authoritative ordinal floor names. At most 100 candidates of each kind are returned; total and truncation fields disclose omitted rows. `vegetation_coverage` = fraction of exterior-class blocks bearing vegetation blocks (vines/leaves/moss family). `damage_estimate` = heuristic fraction of wall/roof surfaces adjacent to unexpected air gaps.

---

### `inspect_volume` ✅

Return exact, bounded block-state evidence for Agent interpretation without an
unbounded coordinate dump. Bounds are inclusive.

**Input:**

```json
{
  "x_min": -8,
  "y_min": 60,
  "z_min": 0,
  "x_max": 15,
  "y_max": 75,
  "z_max": 23,
  "max_runs": 4096
}
```

Limits are enforced before Amulet reads: X/Z span <= 32, Y span <= 24, total
volume <= 24,576, and `max_runs` in `0..4096`.

**Output (abridged):**

```json
{
  "ok": true,
  "bounds": {
    "min": {"x": -8, "y": 60, "z": 0},
    "max": {"x": 15, "y": 75, "z": 23}
  },
  "dimensions": {"x": 24, "y": 16, "z": 24, "volume": 9216},
  "non_air_blocks": 1420,
  "palette": [
    {"index": 0, "block": "minecraft:stone_bricks", "properties": {}, "count": 900},
    {"index": 1, "block": "minecraft:oak_stairs", "properties": {"facing": "north", "half": "bottom", "shape": "straight"}, "count": 5}
  ],
  "layers": [
    {
      "y": 64,
      "rows": [
        {"z": 4, "runs": [{"x_min": -2, "x_max": 8, "length": 11, "palette_index": 0}]}
      ]
    }
  ],
  "surface_counts": {"floor": 80, "inner_wall": 120},
  "block_entity_positions": [],
  "run_count": 310,
  "complete": true,
  "truncated": false,
  "omitted_runs": 0,
  "encoding": {
    "order": "Y layers -> Z rows -> inclusive X runs",
    "air": "implicit only when complete=true",
    "palette_index": "layers[].rows[].runs[].palette_index"
  },
  "space_classification": "heuristic"
}
```

Identical block IDs with different properties receive different palette entries.
Within a complete response, positions absent from the emitted runs are air.
When the run budget is exceeded, palette counts still describe the full loaded
volume but `complete=false`, `truncated=true`, and `omitted_runs>0`; the caller
must subdivide before treating an absent run as air.

The tool fails closed with `incomplete_read` if any chunk covered by the requested
bounds was not loaded/generated. Other structured errors are `invalid_bounds`,
`invalid_max_runs`, `volume_limit_exceeded`, `bounds_outside_target`,
`world_not_set`, and `region_too_large`.

This is evidence, not semantic truth. Agent behavior and rule-promotion policy:
`docs/agent_semantic_review.md`.

---

### MCP prompt: `interpret_world_structure` ✅

This is an MCP prompt, not a tool and not an automatic world operation. Inputs
are inclusive `x/y/z min/max` bounds plus optional `focus`. It instructs the
Agent to combine `analyze_region` candidates with `inspect_volume`, preserve
block properties, separate facts from hypotheses, disclose alternatives and
boundary/truncation risks, and avoid all world-mutating tools.

The same universal honesty/no-implicit-write boundary is also published through
FastMCP initialization `instructions`. Client hosts decide how to present/use MCP
prompts and instructions; server-side safety never depends on prompt compliance.

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

> **Modded-write gate (normative, applies to `apply_pass` and `apply_bundle`):** whether the Phase 1.5 modded-block round-trip spike has passed is signalled to the server via the env var **`PICASSO_MODDED_WRITE_VERIFIED`** (default `false`; a human sets it `true` after completing the spike — the server cannot determine this itself). While `false`, a `dry_run: false` call on any pass whose replacement vocabulary contains non-vanilla-namespace blocks is **blocked** with error `modded_write_unverified`, unless the call passes `force_modded_write: true` — in which case it proceeds and the response carries `"modded_write_warning"`. Enforcement point: the write choke point (`ARCHITECTURE.md` §12.1). Rationale: modded-block persistence through Amulet is the project's load-bearing untested assumption; silent writes on top of it risk corrupting hours of styling work, so the default is refusal, and the override is deliberate and logged.

**Output:**
```json
{
  "ok": true,
  "changed": 412,
  "journal_entry": "picasso_journal/20260706T090000Z_apply_pass_42.json",
  "reversibility_warning": null,
  "space_classification": "heuristic",
  "noise_backend": "fallback",
  "summary": "Applied tlou_nature_reclaim. 412 blocks changed. World saved."
}
```
`reversibility_warning` is `null` when journal is active. When journal is unavailable: `"Journal not running — changes are NOT revertible. Operate on world copies only."`. Agents receiving a non-null value should surface it to the user before proceeding with further destructive operations.

---

### `create_pass` ✅

Define a new **block pass** at runtime, validate it, save to `PICASSO_PASSES_DIR`, register.

**Input:**
```json
{
  "name": "custom_moss_heavy",
  "description": "Heavy moss coverage for deep jungle ruins",
  "version": "1.0",
  "only_safe_blocks": true,
  "destructive": false,
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

`destructive: true` is mandatory when a rule removes a block or writes AIR.
Definitions are published atomically; `overwrite: false` never silently replaces
an existing name, while deliberate replacement archives the predecessor.

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
  "marker_file": "D:\\...\\picasso_markers\\100_64_n200.json",
  "summary": "NPC marker placed at (100, 64, -200). Type: key_npc, faction: survivor_camp."
}
```

**Filename convention (normative):** marker files use `{x}_{y}_{z}.json` where **negative values are written with an `n` prefix** rather than a minus sign — e.g. coordinates `(100, 64, -200)` produce `100_64_n200.json`. Rationale: minus signs in filenames cause parsing ambiguity on some filesystems and in some shell contexts. `TODO(migration)`: any existing `*-*.json` files under `picasso_markers/` must be renamed before the marker-protection scan will find them.

---

## Additional Implemented Tool Families

- World lifecycle: `close_world`.
- Bundles/fragments: `list_bundles`, `apply_bundle`, `list_fragments`,
  `create_fragment`, `create_bundle`. `create_fragment` accepts optional
  `match_hint: "glass_pane"`; bundle/style calls accept
  `include_player_built` (operator-protected regions are never bypassed).
- Journal: `list_journal_entries`, paginated `inspect_journal_entry`, and
  conflict-safe `revert_last_apply`.
- Activity: `query_player_activity` and query-scoped `get_activity_site`; raw
  player events are not exposed.
- Diagnostics: `describe_capabilities` returns the complete 21-tool surface and
  safe workflow.
