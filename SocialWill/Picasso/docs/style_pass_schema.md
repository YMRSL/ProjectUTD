# Pass, Pattern & Safe-Blocks Schema Reference

> Tracks `ARCHITECTURE.md` v0.4.4. Authoritative JSON schemas for everything under `data/`. Pass **type system** (dispatcher, intensity/space_filter semantics, seed precedence) is defined in `ARCHITECTURE.md` §5; this file specifies the per-type file formats.

A pass is any `*.json` in `data/passes/`. The top-level `type` field selects the schema:

| `type` value | Schema section |
|---|---|
| `"block_pass"` or **omitted** (v0.1 back-compat) | §1 |
| `"fragment_pass"` | `docs/fragment_system.md` §3 |
| `"pattern_replace"` | §2 |

Fields common to all types: `name` (must equal filename sans `.json`), `description`, `version` (default `"1.0"`), `deprecated` (bool, default `false` — surfaced by `list_passes`, applying warns), `only_safe_blocks` (bool, default `true`).

---

## 1. Block Pass Schema (`type: "block_pass"`)

### Top-level fields

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | ✓ | Unique identifier, must match filename. |
| `description` | string | ✓ | Shown by `list_passes`. |
| `type` | string | | `"block_pass"`; may be omitted (default). |
| `version` | string | | Semver string. Default `"1.0"`. |
| `deprecated` | bool | | Default `false`. |
| `targets` | string[] | | Surface types this pass is designed for. **Documentation only** — does not filter execution. |
| `only_safe_blocks` | bool | | If `true`, rules skip any block not in `safe_blocks.replaceable`. Default `true`. |
| `rules` | Rule[] | ✓ | Ordered transformation rules. **First matching rule wins per block.** |

### Rule Object

| Field | Type | Required | Description |
|---|---|---|---|
| `match` | MatchExpr | ✓ | Condition for the rule to apply to a block. |
| `action` | string | ✓ | `"replace"`, `"place_adjacent"`, or `"remove"`. |
| `replace_with` | ReplaceOption[] | if replace | Candidates; one chosen by weighted position-hash roll. |
| `place_block` | string | if place_adjacent | Full block ID to place. |
| `direction` | string | if place_adjacent | `"air_side"`, `"above"`, `"below"`. |
| `weight` | float | | Base probability [0–1] the rule fires when matched; scaled by call-time intensity (`ARCHITECTURE.md` §5.1). Default `1.0`. |
| `noise` | NoiseConfig | | Spatial gate; fires only when noise > threshold. |

`"remove"` writes air and is therefore subject to the destructive gate: a block pass containing a `remove` rule must declare top-level `"destructive": true`, or the write-time choke point (`ARCHITECTURE.md` §12.1) drops those changes.

### MatchExpr Object

All fields AND-combined; list-valued `block` is OR within the field.

| Field | Type | Description |
|---|---|---|
| `block` | string \| string[] | Exact block ID(s). |
| `namespace` | string | All blocks from a namespace. |
| `name_contains` | string | Substring match on block name (case-insensitive). |
| `surface` | string \| string[] | Surface class(es) per `ARCHITECTURE.md` §4.5: `"floor"`, `"outer_wall"`, `"inner_wall"`, `"ceiling"`, `"rooftop"`. (`"embedded"` blocks never match.) |
| `adjacent_air` | bool | `true` → ≥1 horizontally adjacent air block. |
| `y_min` / `y_max` | int | Y-coordinate bounds (inclusive). |

### ReplaceOption Object

| Field | Type | Description |
|---|---|---|
| `block` | string | Full block ID. |
| `properties` | dict | Optional block state properties. |
| `weight` | float | Relative selection weight. Default `1.0`. |

### NoiseConfig Object

| Field | Type | Default | Description |
|---|---|---|---|
| `type` | string | `"perlin"` | Only `"perlin"` in v1. |
| `scale` | float | `0.05` | Spatial frequency; smaller = larger patches. |
| `threshold` | float | `0.4` | Fires when noise > threshold; higher = sparser. |

Backend caveat: with the optional C `noise` library absent, the deterministic fallback sampler is used; fields differ between backends for the same seed (`ARCHITECTURE.md` §4.6).

### Complete example: `tlou_nature_reclaim`

```json
{
  "name": "tlou_nature_reclaim",
  "description": "Moss, vines, leaf intrusions on outer walls and rooftops — nature reclaiming the city.",
  "version": "1.0",
  "targets": ["outer_wall", "rooftop"],
  "only_safe_blocks": true,
  "rules": [
    {
      "match": {
        "block": ["minecraft:stone_bricks", "minecraft:stone_brick_wall", "minecraft:stone_brick_stairs"],
        "surface": "outer_wall"
      },
      "action": "replace",
      "replace_with": [
        {"block": "minecraft:mossy_stone_bricks", "weight": 0.5},
        {"block": "minecraft:mossy_cobblestone", "weight": 0.1},
        {"block": "minecraft:stone_bricks", "weight": 0.4}
      ],
      "weight": 1.0,
      "noise": {"scale": 0.04, "threshold": 0.35}
    },
    {
      "match": {"surface": "outer_wall", "adjacent_air": true},
      "action": "place_adjacent",
      "place_block": "minecraft:vine",
      "direction": "air_side",
      "weight": 0.18,
      "noise": {"scale": 0.08, "threshold": 0.5}
    },
    {
      "match": {"surface": "rooftop"},
      "action": "replace",
      "replace_with": [
        {"block": "minecraft:moss_block", "weight": 0.4},
        {"block": "minecraft:grass_block", "weight": 0.3},
        {"block": "minecraft:dirt", "weight": 0.3}
      ],
      "weight": 0.6,
      "noise": {"scale": 0.06, "threshold": 0.45}
    }
  ]
}
```

---

## 2. Pattern Replace Pass Schema (`type: "pattern_replace"`)

Runs `PatternMatcher` over the region and substitutes matched vanilla furniture with DD blocks.

| Field | Type | Required | Description |
|---|---|---|---|
| `name` / `description` / `version` / `deprecated` | | ✓ | Common fields. |
| `type` | `"pattern_replace"` | ✓ | |
| `mappings` | Mapping[] | ✓ | `{"pattern": "<pattern name>", "dd_block": "<full block id>"}` |

Per match: replacement fires with probability = call-time `intensity` (position-hash roll on the anchor). The DD block is placed at `anchor + replacement_anchor_offset`; positions in the pattern's `clear_offsets` are set to air only when that rotated offset actually matched a required or optional pattern block. Pattern-replace clears are **exempt from the destructive flag** (replacing a chair implies removing its parts) but still respect `structural_never_touch` and marker protection at the write choke point. The replacement plus its clears are validated as one atomic group: if any position in the group is blocked by the choke, the whole pattern replacement is dropped.

```json
{
  "name": "tlou_furniture_modreplace",
  "type": "pattern_replace",
  "description": "Replace vanilla furniture combos with Doomsday Decoration equivalents",
  "mappings": [
    {"pattern": "chair",         "dd_block": "doomsday:rusted_chair"},
    {"pattern": "table",         "dd_block": "doomsday:metal_table"},
    {"pattern": "desk",          "dd_block": "doomsday:office_desk"},
    {"pattern": "computer_desk", "dd_block": "doomsday:broken_computer"},
    {"pattern": "shelf",         "dd_block": "doomsday:wall_shelf"},
    {"pattern": "bed_frame",     "dd_block": "doomsday:metal_bed"}
  ]
}
```

> `TODO(arch)`: mapped DD block IDs must exist in the loaded catalog; registry load should warn (not fail) on unknown IDs, so typos surface before an agent applies the pass. Footprint mismatch (a 1×1 DD block replacing a 2-block pattern) is handled by `clear_offsets` in the pattern file.

---

## 3. Furniture Pattern JSON Schema (`data/patterns/`)

Small 3D block templates for `PatternMatcher`.

### Top-level fields

| Field | Type | Required | Description |
|---|---|---|---|
| `name` | string | ✓ | Must match filename. |
| `description` | string | ✓ | |
| `anchor` | [int,int,int] | ✓ | Local-space position of the reference block. |
| `blocks` | PatternBlock[] | ✓ | Offsets + match conditions. |
| `dd_replacement` | string | | Default DD substitute (pass `mappings` override it). |
| `replacement_anchor_offset` | [int,int,int] | | Where the replacement goes, relative to anchor. Default `[0,0,0]`. |
| `clear_offsets` | [[int,int,int]] | | Matched pattern positions set to air on replacement. Optional offsets that did not match are not cleared. |
| `min_optional_matches` | int | | Minimum number of optional pattern blocks that must match. Default `0`. |
| `experimental` | bool | | Default `false`. If true, skipped by default `PatternMatcher.find_matches`, `analyze_region`, and `pattern_replace` until a caller explicitly opts in. |

### PatternBlock / PatternMatch

| Field | Type | Description |
|---|---|---|
| `offset` | [int,int,int] | Relative to anchor. |
| `optional` | bool | If true, a mismatch does not fail the pattern; matched optional blocks count toward `min_optional_matches`. |
| `match.block` | string \| string[] | Full block id(s), e.g. `minecraft:oak_stairs`. |
| `match.namespace` | string | Block namespace. |
| `match.name` | string | Exact name. |
| `match.name_contains` | string \| string[] | Substring match. A list means any listed substring may match. |
| `match.name_contains_any` | string[] | Any listed substring may match. |
| `match.name_contains_all` | string[] | All listed substrings must match. |
| `match.name_endswith` | string \| string[] | Name suffix match. Useful for `*_slab`, `*_stairs`, etc. |
| `match.any` | bool | Any non-air block. |
| `match.air` | bool | Must be air. |

**Rotation:** `PatternMatcher` checks each pattern in all 4 yaw rotations (0°/90°/180°/270°), rotating offsets as integer vectors. A vanilla chair facing east must match the same template as one facing north. (Match conditions like `name_contains: "stairs"` are rotation-agnostic by construction; property-level facing checks are out of scope for v1.)

### Example: `chair`

```json
{
  "name": "chair",
  "description": "Fence post with slab on top — standard vanilla chair combo",
  "anchor": [0, 0, 0],
  "blocks": [
    {"offset": [0, 0, 0], "match": {"name_contains": "fence"}},
    {"offset": [0, 1, 0], "match": {"name_contains": "slab"}}
  ],
  "dd_replacement": "doomsday:rusted_chair",
  "replacement_anchor_offset": [0, 0, 0],
  "clear_offsets": [[0, 1, 0]]
}
```

---

## 4. `safe_blocks.json` Format

```json
{
  "replaceable": [
    "minecraft:stone_bricks", "minecraft:mossy_stone_bricks", "minecraft:cobblestone",
    "minecraft:polished_andesite", "minecraft:smooth_stone_slab", "minecraft:oak_planks",
    "minecraft:oak_slab", "minecraft:oak_fence", "minecraft:glass_pane", "minecraft:torch",
    "minecraft:lantern", "minecraft:carpet", "minecraft:grass_block", "minecraft:dirt",
    "minecraft:gravel", "minecraft:oak_log", "minecraft:oak_leaves"
  ],
  "structural_never_touch": [
    "minecraft:bedrock", "minecraft:barrier", "minecraft:structure_block",
    "minecraft:command_block", "minecraft:chain_command_block",
    "minecraft:repeating_command_block", "minecraft:structure_void"
  ]
}
```

**Semantics (enforcement points in `ARCHITECTURE.md` §12.1):**
- `replaceable` is an **opt-in whitelist**, checked at match/anchor time when `only_safe_blocks` / `only_safe_anchor_blocks` is true.
- `structural_never_touch` is an **unconditional blacklist**, checked for every written position at the write choke point — no pass setting can override it.
- A block in neither list: skipped by safe-mode passes, writable by `only_safe_blocks: false` passes (unless never-touch).

The shipped `replaceable` list should be expanded toward comprehensive coverage of common vanilla decorative/surface blocks in the 1.21.1 registry. Wood-family coverage (all plank/slab/fence/log species) matters most for pattern anchors.
