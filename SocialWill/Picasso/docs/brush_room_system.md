# Brush & Room System — Specification (v0.5 draft 2)

> **Status: ✅ normative (v0.5)** — round-5 reviewed, re-checked, signed off; §11 integration applied in ARCHITECTURE.md v0.4.4 (incl. I14/I15). Upgrades `docs/v05_forward_requirements.md` §1 + §7 from requirements to spec. Tracks `ARCHITECTURE.md` v0.4.4.
>
> Authored by the round-1–4 *reviewer* side; the round-1–4 *defender* side reviews. Draft 1's open questions were adjudicated in REVISION_LOG §I; §12 records the rulings.

---

## 1. Design Intent & the Three-Layer Model

The Fragment/Pass/Bundle stack decorates surfaces. This system edits **spaces**. Per the product owner: room/corridor structure editing beats facade editing in value; the design driver is the **circulation route**.

Every room-related artifact and operation belongs to exactly one of three independently editable layers:

| Layer | Contents | Written via | Re-editable without touching other layers |
|---|---|---|---|
| **1 Structure** | envelope blocks (walls/floor/ceiling), openings, the interior graph | `room_envelope` write context (§9) | walls move, furniture stays put — *not* supported in v1 (move = remove + re-add) |
| **2 Decoration** | furnishing: placements + scatter | `decoration` context | `refurnish_room` (§6) |
| **3 Condition** | decay/aging/wear brushes | `decoration` context | `recondition_room` (§6) |

A **Brush** is the vocabulary unit of layers 2–3. A **Room Template** is the composition unit spanning all three. The **interior graph** is the editable model of layer 1.

**Scope guard (v1):** brushes and rooms compile down to the existing Fragment/Pass machinery and the existing write choke point. No new placement engine, no new safety machinery — new *composition* only. Anything in this spec that would require a second placement engine is a spec bug; flag it. *(Round-5 audit: draft 1 violated this twice — `size_bias` and `angle: "fixed"` both silently demanded engine inputs that don't exist (I1). Draft 2 resolves size_bias by multiplicity encoding — zero engine change — and cuts `fixed`. One **declared** engine accommodation survives: the optional `initial_placed_anchors` parameter (I6, §11 item 7) — an input extension, not a second engine.)*

---

## 2. Brush

### 2.1 Data model

A Brush is a JSON file in `data/brushes/` (env: `PICASSO_BRUSHES_DIR`, default `src/picasso/data/brushes/`).

```python
class BrushDensity(BaseModel):
    # keys are space kinds (§2.2); "default" is REQUIRED, others optional
    default: float                      # anchors fraction, pre-intensity
    corridor: float | None = None
    room: float | None = None
    hall: float | None = None
    exterior: float | None = None

class Brush(BaseModel):
    name: str                           # == filename sans .json
    description: str
    fragments: list[str]                # ≥1 fragment names; selection per §2.3
    anchor_surface: str                 # same enum as fragment passes; no default (G-lesson: required)
    density: BrushDensity               # per-space-kind density
    size_bias: Literal["uniform", "prefer_small", "prefer_large"] = "uniform"
    angle: Literal["free", "wall_aligned"] = "free"   # "fixed" cut in draft 2 (I1: it reintroduces the B6 axis-fixed failure mode)
    min_spacing: int = 0
    noise: NoiseConfig | None = None
    condition_layer: bool = False       # True → this brush belongs to layer 3 (§1); recorded per-layer (§7)
    tags: list[str] = []
```

**`size` semantics (decision, upheld in review):** v1 does **no geometric scaling** of fragments — integer-scaling an arbitrary block arrangement produces garbage (a 2× chair is not a bigger chair). "Size variation" is expressed by listing size-variant fragments (`rubble_pile_small`, `rubble_pile_medium`) and biasing selection. **Weight encoding (I1 fix — zero engine change):** the compiler sorts the compatible fragment list by footprint area and encodes bias by **multiplicity** — each fragment name appears `max(1, round(w))` times in the compiled list, where `uniform` → w=1 for all; `prefer_small` → w = (n − rank) + 1; `prefer_large` → w = rank + 1 (rank 0-based by ascending area, n = list length). The engine's uniform selection roll over the expanded list yields the weighted distribution unchanged (fragment_system §5 step d needs no modification). Expansion is bounded by n·(n+1) entries. Geometric scaling, if ever wanted, is a fragment-authoring concern (author the sizes), not an engine transform.

**`angle` semantics:** `free` → hash-rolled yaw per horizontal-anchor rules; `wall_aligned` → wall-normal-derived yaw (only meaningful for wall anchors; on horizontal anchors it degrades to `free` with a load-time warning). Both respect each fragment's own `orientable` flag — a non-orientable fragment is never rotated regardless of brush angle policy. *(Draft 1 had `angle: "fixed"` with a caller-supplied yaw; **cut per I1** — it required a yaw-override input the engine doesn't have, and a fixed world yaw applied across multiple wall faces is exactly the B6 axis-fixed bug this system exists to avoid. `free`/`wall_aligned` cover the stated use cases; cars-on-roads face-the-road behavior is `wall_aligned` semantics generalized later if ever needed, not a v1 hole.)*

### 2.2 Space kinds — the density context

Brush density is parameterized per **space kind**. Space kinds come from shape analysis of tier-2 flood-fill volumes (Phase S1):

An anchor whose space class is **exterior** gets kind `exterior` outright (disjoint from the volume rules below — I12). Interior anchors take the kind of their adjacent flood-fill volume, first match wins (v1 thresholds tunable):

| Kind | Rule |
|---|---|
| `corridor` | volume's XZ footprint: `max_horiz / min_horiz ≥ 3` ∧ `min_horiz ≤ 3` |
| `hall` | floor area ≥ 100 blocks² ∨ volume ≥ `PICASSO_MIN_STADIUM_VOLUME / 4` |
| `room` | any other `indoor_room` volume |

**Degraded mode (pre-S1, normative):** without flood fill, the three *indoor* kinds collapse to `default` density and the response carries `"space_kinds": "unavailable"`; `exterior` classification (tier-1 heuristic) still works pre-S1 (I12). Brushes are usable from day one; they get *smarter* when S1 lands, not *unlocked* by it.

**Determinism boundary disclosure (I11, same honesty pattern as the noise-backend caveat):** when S1 lands, space kinds flip from `default` to real kinds — the same (brush, scope, seed) then legitimately produces a *different* diff, because anchors move between compiled per-kind passes (stable keys change). Reproducibility across the S1 boundary is not guaranteed; re-preview after upgrading.

### 2.3 Application algorithm

`apply_brush` **compiles to a fragment-pass execution** — one compiled pass per space kind present in the scope, each with `effective_density = density[kind] × intensity`, then delegates to `FragmentEngine` unchanged (same anchor collection, same noise gate, same spacing, same choke point). The compiled pass's stable-key namespace is `brush:<name>:<kind>` so brush rolls never collide with hand-authored pass rolls.

```
apply_brush(brush_name, scope, intensity=1.0, seed=…, dry_run=true)
  scope: exactly one of
    {structure_id}   — clip to structure bounds (registry)
    {room_id}        — clip to one interior-graph node's bounds EXPANDED BY 1
                       (I5: node bounds are flood-fill air volumes; wall/ceiling
                       anchors are the solid envelope shell OUTSIDE them — an
                       unexpanded clip gives wall brushes zero anchors)
    {cx, cz, radius_chunks} — region mode
1. Resolve scope → region + optional clip bounds. Player-protection row and
   governance gate apply exactly as for passes (§12.1 fifth row, H4).
2. Partition anchor candidates by space kind (§2.2; degraded → indoor kinds
   collapse to "default").
3. For each kind with candidates (kinds processed in fixed order: corridor,
   hall, room, exterior, default): compile pass, run FragmentEngine, passing
   the accumulated placed-anchor list from prior kinds as the engine's
   initial-spacing state (I6: min_spacing must hold ACROSS the per-kind
   executions — a corridor stamp and a room stamp on opposite sides of a
   doorway are the common case, not the corner case. Engine accommodation:
   FragmentEngine.preview/apply gain an optional `initial_placed_anchors`
   parameter consulted by the §5 step c spacing check; named in §11).
4. Aggregate response: per-kind {kind, density_used, placed}, plus the standard
   required fields (noise_backend, space_classification, space_kinds).
```

Determinism: same (world, brush, scope, intensity, seed) → same diff — inherited from §6 because compilation is pure and the engine already complies.

### 2.4 `capture_brush` — interactive recording

The player builds an example arrangement in-world; the agent captures it.

```
capture_brush(bounds, name, anchor_surface, tags=[], min_probability=0.35)
```

1. Read all non-air, non-air-like blocks in `bounds` (small AABB; hard cap 16×16×16, else `region_too_large`).
2. Infer anchor: lowest-Y block closest to the XZ centroid (deterministic tie-break by (x,z)).
3. **Canonical-frame normalization for wall anchors (I4 — without this, capture re-imports the B6 bug):** if `anchor_surface` is `outer_wall`/`inner_wall`, derive the example's outward normal from the anchor's adjacent-air side (ties broken by position-hash roll, same rule as fragment_system §4.2), rotate all captured offsets by the yaw that maps that normal onto canonical +Z, remap directional block properties (`facing`, `axis`) by the same yaw, and emit `orientable: true`, `requires_clear_above: false`. A vine arrangement captured off a north wall then applies correctly to an east wall. Floor/rooftop captures stay in world frame with `orientable: false` (radially-symmetric assumption; the agent may hand-flip to `true`).
4. Emit **one Fragment** named `<name>_frag` (offsets relative to anchor, all `probability: 1.0` — capture is literal; the agent hand-tunes probabilities afterwards, per style_learning §10 conservatism guidance) and **one Brush** named `<name>` referencing it (`density.default = 0.05`, placeholder the agent must tune).
5. Both files go through `create_*` collision semantics (G7: `name_already_exists` / `overwrite: true` + archival).

Multi-fragment brushes are authored by editing the brush JSON or via `create_brush` (same schema as §2.1) — capture is deliberately one-example-one-fragment; clustering multiple examples is `extract_block_clusters`' job (the statistical path).

---

## 3. Room Templates

### 3.1 Data model

A Room Template is a JSON file in `data/room_templates/` (env: `PICASSO_ROOM_TEMPLATES_DIR`).

```python
class DimRange(BaseModel):
    min: int; max: int                  # inclusive; interior dimensions (air volume, not counting envelope)

class EntranceSlot(BaseModel):
    kind: Literal["door", "breach"]     # door = 1×2 clean opening; breach = ragged 2×2±
    side: Literal["any", "attach"] = "attach"  # v1: entrances only on the attach side ("any" reserved, load-warns)
    dressing_fragment: str | None = None  # breach-kind only (Q2): ragged-edge dressing; null → shipped default "breach_dressing_default"

class Placement(BaseModel):
    item: str                           # fragment name OR catalog block id (resolved: fragment first, then catalog)
    slot: Literal["against_wall", "corner", "center", "free"]
    count: DimRange                     # how many instances, rolled within range
    facing: Literal["into_room", "hash_rolled"] = "into_room"

class RoomTemplate(BaseModel):
    name: str
    description: str
    identity: str                       # "bunk_room", "storeroom", … — semantic label, free-form
    role: Literal["room", "circulation"] = "room"
    dims: dict[str, DimRange]           # keys: "width", "length", "height"
    envelope: dict[str, str]            # palette SLOT names per face: {"wall": "wall_primary", "floor": "floor_main", "ceiling": "ceiling_main"}
    palette_defaults: dict[str, str]    # REQUIRED fallback block id per slot name used in envelope (H9 chain tier 3)
    entrances: list[EntranceSlot]       # ≥1
    placements: list[Placement] = []    # layer 2: positioned furnishing
    scatter: list[str] = []             # layer 2: brush names, density-driven fill
    condition: list[str] = []           # layer 3: condition brushes applied at instantiation (optional)
    variants: dict[str, float] = {}     # sibling template names → selection weights (§3.3); {} = no variants
    tags: list[str] = []
```

**Placement slots (v1 semantics):** `against_wall` — anchor on a floor cell horizontally adjacent to an envelope wall, facing rotated to point away from that wall (`into_room`); `corner` — floor cell adjacent to two perpendicular walls; `center` — floor cell within the middle third of both interior axes; `free` — any floor cell. Slot candidates are collected deterministically (sort by (x,y,z)), selection and count rolled via position-hash. A placement that finds no free candidate is skipped and reported (`placements_skipped`), never an error — small rooms legitimately can't fit everything.

### 3.2 Palette resolution (H9 chain, normative)

Envelope slots resolve to concrete blocks per host structure, in order:

1. **`authored.palette`** on the host structure registry entry (`{"wall_primary": "minecraft:polished_deepslate", …}`) — set by agent/human, wins outright.
2. **`detected.dominant_materials` filtered through the slot-compatibility table** — `data/palette_compatibility.json` maps slot names to acceptable block *families* (e.g. `wall_primary` accepts full solid cubes tagged wall-suitable; `glass_pane`/slabs/stairs are excluded). First dominant material passing the filter wins. The table ships with the same taxonomy-file mechanics as `block_taxonomy.json` (data, not code).
3. **`palette_defaults`** from the template — always present (required field), so resolution never fails.

The instantiation record (§7) stores the *resolved* palette, so re-editing renders consistently even if the host's dominant materials later change.

### 3.3 Variants & anti-fatigue

Three mechanisms, layered (v05 §7 verbatim, now with mechanics):

1. **Deterministic micro-variation — always on, free.** Dimension solve (§5 step 2), placement counts/positions, and scatter rolls derive from the position-hash scheme keyed by `(template, structure_id, node_id, seed)`. **Palette jitter granularity (Q5 ruling): per-slot-per-building** — when a slot's compatibility filter passes ≥2 dominant materials, the choice is hash-rolled with key `(structure_id, slot_name, seed)`, so all rooms in one host resolve each slot identically (coherent architecture, no patchwork between adjacent rooms) while different hosts diverge. Two instances are automatically non-identical through dims/placements/scatter even with identical palettes.
2. **Authored variants.** `variants` maps sibling template names to weights; instantiation rolls once (`purpose tag "variant"`). A variant is a full template (same file format) — same `role` and compatible `entrances` are the author's responsibility; the loader warns if a variant's `identity` differs from the parent's.
3. **Usage telemetry.** `list_room_templates` aggregates per-world instantiation counts from `picasso_rooms.json` and reports `usage_count`, `variant_count`, and `fatigue_warning: true` when `usage_count / max(variant_count, 1) ≥ PICASSO_VARIANT_FATIGUE_RATIO` (default 12). The system never auto-generates variants; the warning is an authoring prompt, not a trigger.

---

## 4. The Interior Graph

### 4.1 Model & ownership

Per building, stored in the structure registry entry under **`authored.interior_graph`** (H8: re-detection never touches it):

```json
{
  "nodes": [
    {"id": "room_001", "kind": "room" | "corridor" | "hall",
     "bounds": {"x_min": …}, "identity": "bunk_room" | null,
     "origin": "detected" | "instantiated", "instance_id": "room_inst_0007" | null}
  ],
  "edges": [
    {"id": "open_001", "a": "room_001", "b": "room_002" | "exterior",
     "kind": "door" | "breach" | "stair" | "ladder",
     "pos": {"x": …, "y": …, "z": …}, "state": "open" | "sealed"}
  ]
}
```

**Bootstrap (decision, attackable):** the graph is authored-owned *from birth*. `build_interior_graph(structure_id)` runs flood fill within the structure's bounds, classifies volumes into node kinds (§2.2 rules), derives edges from air apertures connecting adjacent volumes (aperture ≥ 1×2 → `door`-kind edge; ragged/larger → `breach`; vertical adjacency with stair blocks → `stair`, ladder blocks → `ladder`), and writes the result to `authored.interior_graph` — **only if absent**; re-running with an existing graph requires `overwrite: true` (which discards edits) or `reconcile: true` (which re-derives, **reports** divergence — nodes/edges present in only one side — and changes nothing; auto-merge is deliberately out of v1). `detect_structures` never writes the graph — an authored artifact must never be created as a detection side effect.

**Divergence is expected**, not an error: a `wall_breach` fragment applied by a condition pass punches a de-facto opening the graph doesn't know. The graph models *intent*; `reconcile: true` measures drift. Tools that mutate openings through the graph (§4.2) keep it consistent by construction.

### 4.2 Graph operations

All graph operations are writes (cache-invalidating, journaled once Phase 8 lands, `dry_run` default `true`).

- `add_room(structure_id, template, attach_via, variant=null, seed=…, dry_run=true)` — §5.
- `remove_room(room_id, mode)` — three modes (Q3 ruling): `"abandon"` (seal all edges only, interior untouched — the cheapest primitive and the correct rehabitation prep: close the wing, keep the contents), `"fill"` (solid-fill the node's volume with the resolved `wall` palette block; envelope stays), `"collapse"` (= abandon + the template's/default collapse condition brush applied inside — sealed ruin). All three mark the node `removed: true` (kept for record lineage), set its edges `state: "sealed"`, and physically seal the openings with the wall palette block.
- `connect(room_a, room_b, opening_spec)` — cut a new opening through the envelope wall between the two nodes. **Adjacency tolerance (I9): the nodes' volumes may be separated by up to 2 blocks of wall** (2-thick walls are ordinary MC construction, and every carve-mode room shares a double shell with its neighbor); the cut runs through the full thickness. Separation > 2 → `no_valid_placement`. Adds an edge.
- `seal(opening_id)` — fill the opening's blocks (full wall thickness) with the adjacent wall's palette block, set `state: "sealed"`.

> **No `unseal` operation exists (I15, deliberate).** Re-opening a sealed edge is done with `connect` (cutting a new opening, possibly at the same spot); the sealed edge stays in the graph as record. This is consistent with graph-as-intent: sealing is history, not a toggle. Rehabitation playbooks should reach for `connect`, not look for an unseal tool.

**Op → destructiveness → write-context mapping (I7, normative — this is what makes the H4 gate server-decidable for room ops, which have no pass content to inspect):**

| Operation | Destructive? | Write context |
|---|---|---|
| `add_room` carve mode (steps 3–4) | **yes** (removes existing blocks) | `room_envelope` |
| `add_room` annex mode (steps 3–4) | no (writes into air only, post-I2 predicate) | `room_envelope` |
| `add_room` steps 5–6 (furnish/condition) | no | `decoration` |
| `remove_room` `abandon` | **yes** (seals openings) | `room_envelope` |
| `remove_room` `fill` / `collapse` | **yes** | `room_envelope` (fill, seals) + `decoration` (collapse dressing) |
| `connect` | **yes** (cuts wall) | `room_envelope` |
| `seal` | **yes** (alters openings) | `room_envelope` |
| `apply_brush` (any scope) | per compiled pass content (existing rule) | `decoration` |
| `recondition_room` / `refurnish_room` | **yes** (selective revert) | replayed entries keep their original contexts |

The H4 governance gate fires on `include_player_built: true` ∧ (op is destructive per this table) ∧ `journal_status != "active"` — same three server-decidable conditions, with the table supplying the middle one.

**Reachability warning (H10, normative):** `seal` and `remove_room` responses include `reachability_warning` naming every node left with no open path to any `exterior` edge. Advisory, never blocking.

### 4.3 Route queries

`get_routes(structure_id, from, to, max_paths=8, max_len=12)` — `from`/`to` are node ids, opening ids, or `"exterior"`. BFS over `state: "open"` edges with deterministic adjacency order (sorted by edge id); returns up to `max_paths` simple paths as `[{nodes: […], edges: […], length}]`. Read-only. `graph_not_built` if the structure has no interior graph.

---

## 5. Room Instantiation Algorithm (normative)

`add_room` executes:

```
1. RESOLVE ATTACH: attach_via is an existing edge id (reuse a sealed/open
   opening) or {node_id | "exterior", wall_hint?}. Attaching to a node → the
   new room is carved/built adjacent to that node through a shared wall.
   Attaching to "exterior" → the room extends outward from the structure's
   outer wall (annex mode).
2. SOLVE DIMENSIONS (H9: constraint solve, not free roll):
   candidate dims = all (w, l, h) in the template's DimRanges, enumerated in
   position-hash-shuffled order (stable key: template, structure, attach, seed,
   "dims"). Admissible positions adjacent to the attach wall are enumerated
   sorted by (x, y, z) (I10 — "first wins" is deterministic only if the
   enumeration is). For each (dims, position) candidate: test
     a. CHOKE-POINT PRE-VALIDATION (I2, normative for both modes): every
        position the envelope + interior would write is checked against every
        predicate the write choke point enforces — never-touch blocks, marker
        protection, player-attribution zones (registry player_built/modified
        bounds AND activity sites). If even ONE envelope block would be
        skipped at write time, the whole candidate is rejected. Rationale:
        choke-point protection firing mid-envelope produces a shell with
        holes — a structurally broken write is worse than a refusal. The
        choke point remains the guarantee; the solver pre-validates so the
        guarantee never fires mid-envelope.
     b. carve mode (Q1 ruling — three named predicates, no percentage soup):
        (i) `authored.interior_graph` exists (I3: without it, a fully-air
            lobby is indistinguishable from dead space — carving would wall
            it in half; error `graph_not_built`),
        (ii) the target volume intersects no non-removed graph node's volume,
        (iii) floor support: ≥70% of the new room's floor face rests on
              solid blocks (a carved room must not span a courtyard's air).
     c. annex mode (I2 rewrite): the target volume lies outside the host's
        current bounds and contains ONLY air / air-like blocks — the
        replaceable whitelist is NOT an occupancy test (a neighbor's
        stone_bricks wall is whitelisted; whitelisted ≠ unowned). The volume
        must additionally intersect no other registry structure's bounds and
        no activity site (a player's shed too small for the registry still
        shows in the log). Trees are annexable — they are not structures;
        that is intended. Floor support ≥70% as in carve (no floating
        annexes — the governance policy would otherwise have to correct our
        own output).
     d. entrance slot aligns with the attach opening (door: exact; breach: ±1).
   First candidate passing all tests wins (deterministic). None →
   error "no_valid_placement" with per-test failure counts.
3. WRITE ENVELOPE (layer 1, write_context = room_envelope): carve interior to
   air; write floor/ceiling/wall shells (thickness 1) in resolved palette
   blocks (§3.2), skipping faces shared with existing envelope. Annex mode:
   flat roof v1; structural blending (roof lines, foundations) is out of scope
   and listed as a v2 item.
4. CUT ENTRANCE(S): openings per template entrances, write_context =
   room_envelope. Breach-kind entrances get their ragged edge from the slot's
   dressing_fragment (Q2: template-specified; null → shipped
   "breach_dressing_default"), placed with the room's seed.
5. FURNISH (layer 2, write_context = decoration): placements (§3.1 slots),
   then scatter brushes clipped to the new node's bounds.
6. CONDITION (layer 3, decoration context): template's condition brushes.
7. RECORD: append instantiation record (§7); add graph node + edge(s);
   registry write-back per multi-writer protocol (read-merge + atomic rename).
```

Steps 3–6 emit through the standard choke point; per-step journal entries (Phase 8) are tagged with the layer name, which is what makes §6 possible.

**Bounds refresh:** annex mode grows the structure's effective footprint. The registry entry's `detected.bounds` is *not* edited (it's detection-owned); instead the instantiation record carries the annex bounds, and the next `detect_structures` run absorbs the new geometry naturally (IoU matching tolerates the growth; flag `partial` rules apply).

**Preview:** `dry_run: true` runs steps 1–2 fully and computes 3–6 as a diff without writing, returning `would_change`, the solved dims/position, and the resolved palette — the agent can inspect *where the room will land* before committing. Same seed → same landing (determinism §6).

---

## 6. Layer-Selective Re-Editing

- `recondition_room(room_id, brushes, seed?, dry_run=true)` — revert the instance's **condition-layer** journal entries (selective revert by layer tag), then apply the new condition brushes. 
- `refurnish_room(room_id, template_or_variant?, seed?, dry_run=true)` — revert **decoration + condition** layers, then re-run steps 5–6 (optionally from a different variant).

**Revert ordering (I8a, normative):** selected layers' journal entries revert in **reverse chronological order** — condition entries before decoration entries. Condition applies over decoration; reverting decoration first would make every later-touched block a spurious conflict (world no longer matches the condition entries' `after`).

**Journal tagging (I8c, normative):** every journal entry produced by room instantiation (steps 3–6) or by any later write scoped to an instantiated room — including a standalone `apply_brush(room_id=…)` — carries `(instance_id, layer)` tags. `condition_layer: true` brushes tag `condition`; others `decoration`. Without this, recondition would revert only the template's original condition pass and miss brushwork added afterwards.

**Pre-Phase-8 instances (I8b):** instances created before the journal existed have empty `journal_entries` arrays in their records and are **permanently outside** `recondition`/`refurnish` — visibly so (the record shows it), not silently. The supported path for them is `remove_room` + `add_room`.

**Mechanical gate (H4 pattern):** both tools require `journal_status == "active"` — layer-selective clearing *is* selective revert; without the journal there is nothing to revert from. Error: `governance_requires_journal` (reused — same meaning: "this operation's safety story is the journal"). Conflict-safe replay (§12.3 skip-and-report) applies: player-placed blocks inside a room survive a refurnish as `conflicts[]`.

Structure-layer re-editing (moving walls) is **not** in v1: `remove_room` + `add_room` is the supported path.

---

## 7. Instantiation Records — `<world>/picasso_rooms.json`

Fixed per-world path (already in §10's not-configurable list). Multi-writer protocol applies (read-merge + atomic rename).

```json
{
  "instances": [
    {
      "instance_id": "room_inst_0007",
      "structure_id": "struct_0001", "node_id": "room_003",
      "template": "bunk_room", "variant": "spartan", "seed": 42,
      "resolved_palette": {"wall_primary": "minecraft:mud_bricks", "floor_main": "…"},
      "solved_dims": {"width": 5, "length": 6, "height": 3},
      "mode": "carve",
      "layers": {
        "structure":  {"applied_at": "…", "journal_entries": ["…"]},
        "decoration": {"applied_at": "…", "journal_entries": ["…"], "placements_skipped": []},
        "condition":  {"applied_at": "…", "journal_entries": ["…"]}
      }
    }
  ]
}
```

Records store journal *references*, not block lists (position lists ballooned the registry; the journal already owns the diffs). Usage telemetry (§3.3) aggregates over `instances`.

---

## 8. Tool Surface (registry additions, all 🚧 v0.5)

Fifteen tools total (I12 count fix): 4 brush, 2 template, 3 graph-read/build, 4 graph-write, 2 re-edit.

| Tool | Category | Notes |
|---|---|---|
| `create_brush` / `list_brushes` | Brush | G7 collision semantics; list supports `tags_filter` |
| `capture_brush` | Brush | §2.4; bounds cap 16³; canonical-frame normalization for wall anchors |
| `apply_brush` | Brush | §2.3; scope = structure/room/region; std write-tool response fields |
| `create_room_template` / `list_room_templates` | Room | telemetry fields §3.3 |
| `build_interior_graph` | Graph | §4.1; `overwrite` / `reconcile` modes |
| `get_interior_graph` / `get_routes` | Graph | read-only |
| `add_room` / `remove_room` / `connect` / `seal` | Graph | §4.2/§5; `dry_run` default true; destructiveness per §4.2 table |
| `recondition_room` / `refurnish_room` | Room | §6; journal-gated |

New error codes: `brush_not_found` · `room_not_found` · `opening_not_found` · `graph_not_built` · `no_valid_placement` (existing reserved: `template_not_found`).

New config: `PICASSO_BRUSHES_DIR` · `PICASSO_ROOM_TEMPLATES_DIR` · `PICASSO_VARIANT_FATIGUE_RATIO` (default 12).

---

## 9. Determinism, Safety, Write Contexts

- **Determinism:** every roll in this spec uses the §6 position-hash scheme; stable keys are namespaced (`brush:*`, `room:*`) and enumerated in this doc's algorithms. No wall clock anywhere; `applied_at` in records is stamped from the completed write, never used as an input.
- **Write contexts:** envelope + opening operations emit `room_envelope`; furnishing/condition emit `decoration`. **Pending §12.1 amendment (§11):** the `room_envelope` column's air-write row must become **pass through** — carving and opening-cutting *are* air-writes; blocking them blocks the subsystem (the G1/pattern_clear lesson, applied proactively this time). `structural_never_touch` and marker protection remain unconditional.
- **Player protection & governance:** the §12.1 fifth row and the H4 gate apply to every tool here unchanged. Room-op destructiveness for the gate's middle condition comes from the §4.2 mapping table (I7) — carve-mode `add_room` into a `player_built` structure requires `include_player_built: true` and an active journal; annex mode is additive and passes without the journal (matching the reinforce-may-precede-Phase-8 policy). The §5 step 2a solver pre-validation additionally guarantees no room envelope is ever half-written by protection skips (I2).
- **Phase 1.5:** palette defaults and placements may reference `doomsday:*` blocks; the modded-write gate applies unchanged.

---

## 10. Source Layout & Build Phases

```
src/picasso/
├── core/
│   ├── brush_compiler.py       🚧 Brush → compiled fragment passes (§2.3)
│   ├── room_engine.py          🚧 templates, palette resolution, instantiation (§3, §5)
│   └── interior_graph.py       🚧 graph model, bootstrap, ops, routes (§4)
├── models/
│   ├── brush.py                🚧 Brush, BrushDensity
│   └── room_template.py        🚧 RoomTemplate, Placement, EntranceSlot, DimRange
├── tools/
│   ├── brush.py                🚧 4 brush tools
│   └── room.py                 🚧 11 room/graph tools (I14: §8's corrected count)
└── data/
    ├── brushes/                🚧
    ├── room_templates/         🚧
    └── palette_compatibility.json 🚧
```

| Phase | Delivers | Hard dependencies |
|---|---|---|
| **R1** | Brush model + `create/list/apply_brush` + `capture_brush` (degraded space kinds) | choke-point refactor done (G1 etc.) |
| **R2** | Space-kind classification wired into R1 | **Phase S1** (flood fill) |
| **R3** | Templates + palette resolution + `build_interior_graph` + `get_routes` | S1 |
| **R4** | `add_room` carve mode + `connect`/`seal`/`remove_room` | R3, Phase 8 recommended (records reference journal) |
| **R5** | Annex mode, variants + telemetry, `recondition/refurnish_room` | R4, **Phase 8 required** (§6 gate) |

R1 is deliberately early and S1-independent: brushes deliver value (better-controlled scatter than raw fragment passes) before any segmentation work lands. **R1 scope reality (I13):** structure scope needs the registry (Phase S4) and room scope needs R3 — **region scope is R1's only usable scope**; H1's choke-point protection is its safety story (which is exactly why H1 moved protection there). Implementers should not discover this; it is stated here.

---

## 11. Pending Integration into ARCHITECTURE.md (apply after review)

1. §8 registry: add the §8-table tools (🚧 v0.5).
2. §10 config: add the three new vars; `picasso_rooms.json` already listed.
3. §11 errors: add the five new codes.
4. §12.1: amend `room_envelope` air-write row to **pass through** (rationale in §9).
5. §3 source layout: add the §10 files.
6. `docs/v05_forward_requirements.md` §1/§7: mark "superseded by docs/brush_room_system.md" (requirements remain as intent record).
7. **`docs/fragment_system.md` §5 / FragmentEngine interface (I6 — the one engine accommodation this spec requires):** `preview`/`apply` gain an optional `initial_placed_anchors: list[BlockPos] = []` parameter, consulted by step c's min_spacing check before this execution's own placements. Backward-compatible (default empty = current behavior); used by the brush compiler to hold spacing across per-kind executions. This is a declared exception to the "zero engine change" claim — one input parameter, no behavioral change for existing callers.
8. **Journal entry format (§12.3):** entries gain optional `instance_id` and `layer` tag fields (I8c) — needed by §6 selective revert; format extension only, Phase 8 implements it from day one alongside H6 conflict detection.

---

## 12. Open Questions — adjudicated in round-5 review (REVISION_LOG §I)

All six of draft 1's open questions were ruled on; the rulings are applied in the body above. Record:

1. **Carve threshold** → REPLACED by three named predicates (§5 step 2b): graph present + zero node intersection + 70% floor support. No percentage soup.
2. **Breach dressing** → template-specified `dressing_fragment` with shipped default (§3.1 EntranceSlot).
3. **`abandon` mode** → ADDED (§4.2); `collapse` is honestly abandon + dressing.
4. **Graph staleness** → report-only stands for v1; intent ≠ damage. `graph_drift_warning` on destructive writes inside graphed structures noted as the eventual cheap hook, deferred until the daily editorial loop makes drift measurable.
5. **Palette jitter** → per-slot-per-building, key `(structure_id, slot_name, seed)` (§3.3.1).
6. **Brush region scope** → KEPT — it is R1's only usable scope (§10); H1 choke-point protection is its safety story.

No open questions remain in draft 2. New attack surface, if any, comes from the reviewer's re-check.
