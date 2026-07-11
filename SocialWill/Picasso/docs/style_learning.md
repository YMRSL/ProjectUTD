# Style Learning from Reference Saves

> Supplement to `ARCHITECTURE.md` (tracks v0.4.4) and `docs/fragment_system.md`.
> This document describes the reverse-engineering workflow: extracting a Style Bundle from a reference world save.

---

## 1. Concept: AI-Driven Style Extraction

The system is designed around a clear division of responsibility:

- **MCP tools provide raw data**: what blocks are where, what combinations recur, what statistics describe the region.
- **The AI agent provides intelligence**: it interprets the data, names the patterns, decides which ones matter, and generates Fragment + Bundle JSON.

This means the quality of the extracted style is bounded by the AI agent's judgment, not by the tools. The tools give reliable quantitative data; the AI provides the qualitative synthesis.

---

## 2. Workflow: Reference Save → New Style Bundle

```
1. set_world(reference_save_path)
2. analyze_region(cx, cz, radius)        → surfaces, pattern matches, coverage stats
3. learn_style(cx, cz, radius, name)     → StyleProfile with block distributions + suggested passes
4. extract_block_clusters(cx, cz, radius)→ list of recurring spatial block arrangements
5. [AI agent reviews all results]
6. create_fragment(name, blocks, ...)    × N  (one per meaningful cluster)
7. create_bundle(name, entries)          → new Style Bundle saved to disk
```

Steps 1–4 are tool calls that gather data. Steps 5–7 are the AI synthesizing and writing.

The AI agent in step 5 is not constrained to the existing Fragment library. It can define entirely new Fragments by calling `create_fragment` with block arrangements it observed in the reference save.

---

## 3. New Tool: `extract_block_clusters`

Returns a list of recurring spatial block arrangements found in the region. These are candidate Fragment templates.

### Input

```json
{
  "cx": 0,
  "cz": 0,
  "radius_chunks": 6,
  "min_occurrences": 3,
  "max_cluster_size": 20,
  "min_cluster_size": 2
}
```

- `min_occurrences`: only return clusters that appear at least this many times.
- `max_cluster_size`: ignore connected components larger than this (they're buildings, not decorations).
- `min_cluster_size`: ignore single isolated blocks.

### Output

```json
{
  "ok": true,
  "cluster_count": 12,
  "clusters": [
    {
      "cluster_id": "cl_0001",
      "occurrences": 47,
      "representative_pos": {"x": 120, "y": 65, "z": -180},
      "block_count": 5,
      "blocks": [
        {"offset": [0, 0, 0], "block": "minecraft:iron_bars"},
        {"offset": [0, 1, 0], "block": "minecraft:iron_bars"},
        {"offset": [1, 0, 0], "block": "minecraft:gravel"},
        {"offset": [1, 1, 0], "block": "minecraft:cobblestone"},
        {"offset": [0, 0, 1], "block": "minecraft:cobblestone"}
      ],
      "dominant_surface": "outer_wall",
      "dominant_space": "exterior",
      "material_tags": ["metal", "stone", "debris"],
      "spatial_description": "2-block-tall iron bars with stone debris cluster — appears on outer walls"
    }
  ],
  "candidate_groups": [
    {"group_id": "grp_01", "cluster_ids": ["cl_0001", "cl_0003", "cl_0009"], "reason": "rotation_variants"}
  ],
  "summary": "Found 12 recurring cluster types. Top 3 by frequency: cl_0001 (47×), cl_0004 (31×), cl_0007 (22×). grp_01 groups 3 rotation-variant clusters (combined 71×). Suggest reviewing clusters with occurrences > 10 first."
}
```

**`candidate_groups` (normative):** clusters whose canonical block lists are 90°/180°/270° yaw-rotations (or X/Z mirrors) of one another are reported in the same group. Detection: for each cluster signature, compute the signatures of its 3 rotations + 2 mirrors and check for matches among the other clusters. The engine does **not** merge them — orientation may be meaningful (all clusters facing one street) or incidental; that judgment belongs to the AI agent. But without this field the agent would systematically undercount recurrence: the same wall decoration on four wall faces shows as 4 separate clusters at ¼ the density each, leading to too-low `probability` values in `create_fragment`. When authoring a fragment from a grouped cluster, sum the group's occurrences for density estimation and pick one representative orientation as the canonical frame (fragment orientation rules: `docs/fragment_system.md` §4).

### Algorithm (for `core/segmentation/cluster_extractor.py`)

```
1. Build 3D numpy array of non-air blocks in region.
2. Identify "decoration candidates":
   - Blocks adjacent to at least one air block horizontally (not embedded in solid).
   - Blocks NOT in safe_blocks structural_never_touch list.
   - Blocks whose Y position is not the lowest solid Y in their column (not ground surface).
3. Build adjacency graph of decoration candidates (6-connected).
4. Extract connected components → raw clusters.
5. Filter: keep clusters where min_cluster_size ≤ size ≤ max_cluster_size.
6. Normalize each cluster to anchor-relative coordinates (anchor = lowest-Y, most-negative-X,Z block).
7. Canonicalize block list: sort by (offset_y, offset_x, offset_z).
8. Hash each canonical cluster → cluster_signature.
9. Group by cluster_signature → occurrences count + list of world positions.
10. Return groups where occurrences ≥ min_occurrences, sorted by occurrences desc.
```

**Note on rotation**: v1 does NOT normalize for rotation or mirroring. Two clusters with the same blocks in different orientations are treated as different. This is intentional simplicity — the AI agent can recognize they are related when reviewing the output.

---

## 4. New Tool: `create_fragment`

Save a new Fragment definition to the Fragment library. Used by AI agents during style extraction.

### Input

```json
{
  "name": "iron_debris_wall_cluster",
  "description": "2-tall iron bar stack with stone debris at base — observed on outer walls of reference save",
  "anchor_surface": "outer_wall",
  "footprint": "2x2",
  "requires_clear_above": false,
  "min_clear_height": 0,
  "destructive": false,
  "tags": ["metal", "debris", "wall"],
  "blocks": [
    {"offset": [0, 0, 0], "block": "minecraft:iron_bars",   "probability": 1.0},
    {"offset": [0, 1, 0], "block": "minecraft:iron_bars",   "probability": 0.9},
    {"offset": [1, 0, 0], "block": "minecraft:gravel",       "probability": 0.8},
    {"offset": [1, 1, 0], "block": "minecraft:cobblestone", "probability": 0.6},
    {"offset": [0, 0, 1], "block": "minecraft:cobblestone", "probability": 0.5}
  ]
}
```

### Output

```json
{
  "ok": true,
  "saved_path": "D:\\...\\data\\fragments\\iron_debris_wall_cluster.json",
  "message": "Fragment 'iron_debris_wall_cluster' created and registered."
}
```

**Notes:**
- The file is saved to `PICASSO_FRAGMENTS_DIR`.
- The fragment is immediately available for use in `Fragment Pass` definitions.
- The AI agent is responsible for converting cluster data (from `extract_block_clusters`) into appropriate `probability` values and `anchor_surface` settings.

---

## 5. New Tool: `create_bundle`

Save a new Style Bundle definition.

### Input

```json
{
  "name": "industrial_ruin",
  "description": "Heavy industrial decay — learned from reference save 'FactoryDistrict'",
  "version": "1.0",
  "default_seed": 77,
  "entries": [
    {
      "structure_type": "building",
      "passes": [
        {"name": "tlou_material_hints",         "intensity": 0.4, "space_filter": "exterior"},
        {"name": "iron_debris_wall_cluster",    "intensity": 0.6, "space_filter": "exterior"},
        {"name": "tlou_rubble_scatter",         "intensity": 0.7},
        {"name": "tlou_furniture_modreplace",   "intensity": 1.0, "space_filter": "interior"}
      ]
    }
  ]
}
```

### Output

```json
{
  "ok": true,
  "saved_path": "D:\\...\\data\\bundles\\industrial_ruin.json",
  "message": "Bundle 'industrial_ruin' created and registered."
}
```

---

## 6. New Tool: `list_fragments`

List all available Fragment templates. Needed so AI agents know what vocabulary is available when composing bundles.

### Input

```json
{ "tags_filter": ["vegetation"] }
```
Optional. Omit for all fragments.

### Output

```json
{
  "ok": true,
  "count": 4,
  "fragments": [
    {
      "name": "window_overgrown",
      "description": "Broken window filled with vines and leaf growth",
      "anchor_surface": "outer_wall",
      "tags": ["vegetation", "structural"],
      "footprint": "3x3"
    }
  ]
}
```

---

## 7. Extended `learn_style` Output

The existing `learn_style` tool is extended to also record which built-in fragments were observed (estimated presence based on block cluster matching against the fragment library).

Additional fields in the `StyleProfile` output:

```json
{
  "matched_fragments": [
    {"fragment": "rubble_pile_small",  "estimated_density": 0.08, "confidence": 0.75},
    {"fragment": "vine_anchor",        "estimated_density": 0.12, "confidence": 0.82}
  ],
  "unmatched_cluster_count": 7,
  "suggested_extract_clusters": true
}
```

`suggested_extract_clusters: true` tells the AI agent that there are unmatched recurring patterns worth extracting via `extract_block_clusters`.

---

## 8. Source Layout Additions

```
src/picasso/
├── core/
│   └── segmentation/
│       └── cluster_extractor.py    ← NEW: extract_block_clusters algorithm
│
└── tools/
    └── learning.py                 ← MODIFIED: add create_fragment, create_bundle, list_fragments,
                                       extend learn_style output
```

---

## 9. Example: Full Style Learning Session

```
# 1. Open the reference save
set_world("D:\\MC\\...\\saves\\FactoryDistrict")

# 2. Read and analyze a representative area
analyze_region(cx=5, cz=-3, radius_chunks=6)
→ "Mostly industrial buildings. Heavy iron/concrete. 8 patterns detected. Low vegetation."

# 3. Extract statistical style profile
learn_style(cx=5, cz=-3, radius_chunks=6, name="factory_style")
→ "Dominant: iron_bars (18%), black_concrete (14%), gravel (11%).
   Matched fragments: rubble_pile_medium (density 0.15). 
   9 unmatched cluster types found — recommend extract_block_clusters."

# 4. Extract recurring block clusters
extract_block_clusters(cx=5, cz=-3, radius_chunks=6, min_occurrences=5)
→ 9 cluster types returned. Top cluster: 5-block iron+stone arrangement, appears 41×.

# 5. [AI agent reviews clusters, picks the meaningful ones]
# 6. Create new Fragments for the 3 most characteristic clusters
create_fragment("factory_iron_debris", ...)
create_fragment("factory_pipe_cluster", ...)
create_fragment("factory_broken_machinery", ...)

# 7. Create the new bundle
create_bundle("factory_ruins", {
  building → [material_hints (0.5), factory_iron_debris (0.7), 
               factory_pipe_cluster (0.4), rubble_scatter (0.8)]
  road →     [material_hints (0.3), rubble_scatter (0.9)]
})

# 8. Apply to a new area
apply_bundle("factory_ruins")
→ "Applied to 18 structures. 23,440 blocks changed."
```

---

## 10. Limitations & Notes for AI Agents

- `extract_block_clusters` returns raw spatial data, not semantics. The AI agent must interpret what a cluster "means" (e.g., "this looks like a pipe array") and decide whether to include it.
- Clusters that appear in the reference save because of structural necessity (support columns, stairwells) are usually too large and get filtered by `max_cluster_size`. But the AI agent should still sanity-check that extracted clusters are truly decorative.
- A learned Style Bundle will only look good on areas with similar architectural base structure as the reference save. A factory-style bundle applied to gothic cathedral geometry will produce odd results.
- Fragment `probability` values should be set conservatively (lower than what was observed) when writing `create_fragment`, to avoid over-saturating the target area.
