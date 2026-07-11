# v0.5+ Forward Requirements — Product Owner Decisions (2026-07-07)

> Captured verbatim-in-spirit from the product owner. These are **requirements, not specs** — each section names what must eventually exist and the constraints it puts on today's architecture. Nothing here is implemented. Adversarial review should attack: (a) whether current v0.4.x contracts block any of this, (b) whether the sketched data models are the right shape.
>
> §1–§5 from the first intake session; §6–§8 added the same evening (player-activity awareness, room-graph/template library, module boundary). Tracks `ARCHITECTURE.md` v0.4.4.

---

## 1. Brush & Room System (largest new subsystem)

> **SUPERSEDED by `docs/brush_room_system.md`** (v0.5, round-5 reviewed, signed off). This section remains as the intent record; on conflict the spec wins.

### Intent

Two new authoring surfaces above the Fragment level:

**Brush（笔刷）** — a recorded decay/decoration vocabulary element with *tunable application parameters*. Players assist agents in **recording** brushes (capture a hand-built arrangement from the world, à la `extract_block_clusters` but interactive); agents **edit and apply** brushes with structure-aware parameters:
- `density` — how many stamp points per structural context, *specified per structure kind*: a corridor, a hall, and a small room each get their own density expectation
- `size` / `scale` — footprint variation range per stamp
- `angle` — orientation distribution (free / wall-aligned / fixed)

**Room（房间）** — a semantic composition unit the agent can author and instantiate:
- envelope: what blocks make up walls / ceiling / floor
- contents: furnishings, debris, decoration (as fragments/brushes)
- identity: what kind of room this is (bedroom, storeroom, lobby…)
- placement: attached to which structure, at which position within it
- entrance: door or breach hole

So the agent's editing vocabulary becomes two-tier: **edit brushes** (the vocabulary) and **edit rooms** (composed spaces that consume the vocabulary).

### Constraints on v0.4 architecture

- Fragments stay the atomic unit; a Brush is roughly *fragment(s) + application-parameter envelope*, a Room is a *bounded volume + envelope materials + placement slots*. Neither invalidates the Fragment/Pass/Bundle stack — they sit above it.
- The structure registry must eventually classify **sub-structure spaces** (corridor vs hall vs room) since brush density is parameterized per space kind. Flood-fill volumes (Phase S1) are the natural substrate: room/corridor/hall classification is a shape analysis of `indoor_room` volumes. **This raises S1's priority.**
- Interactive brush recording needs a "capture region as template" tool (player builds → agent captures → names → tags). `extract_block_clusters` covers the statistical path; a `capture_brush(bounds, name)` direct path is new.
- Room instantiation is the first feature that **builds** rather than decorates — it writes envelope blocks, not just decoration. The write choke point and `destructive` gating already accommodate this (a Room write is a declared-destructive placement), but `safe_blocks.replaceable` whitelist semantics need a Room-scoped exemption model (a Room overwrite legitimately replaces blocks that are not on the decoration whitelist).

---

## 2. Rehabitation — Styling Is Bidirectional

Narrative causality runs both ways: after decay is applied, simulation events (a faction occupying a building) require **overwriting decayed areas with signs of habitation** — cleared rubble, sealed windows, lighting, personal effects.

### Constraints

- The pass/fragment/bundle machinery is already direction-agnostic (nothing hardcodes "decay"); a `rehabitation` bundle is just new content — confirmed as designed.
- But rehabitation targets *previously styled* areas: it must **undo** specific decoration (remove vines/rubble) before adding. This needs either (a) inverse fragments (`clear_vines`, `clear_rubble` — fragments whose blocks are air + floor restoration, feasible today), or (b) journal-assisted selective revert (revert only certain passes in a bounded volume — much stronger, needs journal entries tagged per pass, which §12.3's format already supports).
- Consequence for §12.3: journal entries should be queryable by (bounds, pass name), not only revertible newest-first. Upgrade journal from "undo stack" to "styling history database". **Flagged for spec in v0.5.**

---

## 3. Physical Plausibility — Scale-Dependent

Small damage needs no physics. The larger the destruction event, the stronger the required causal correlation. **Threshold: a destruction event > ~150 blocks must have strongly correlated consequences** (a collapsed floor produces a debris mass below of plausible volume; a fallen tower section lies where it fell).

### Constraints

- Below threshold: current independent-fragment model is fine (explicitly: visual impression over physics — this is now written doctrine, coding agents should not over-engineer small events).
- Above threshold: needs **causally linked fragment groups** — one authored event with multiple anchored consequences (breach + projected debris pile + dust staining), placed as a unit. Data-model sketch:

  ```
  CompositeEvent {
    trigger_fragment: str,            # placed first, at a normal surface anchor
    consequences: [
      {
        fragment: str,
        anchor_mode: "offset" | "gravity_projected",
        offset: [dx, dy, dz]          # in the trigger's canonical frame (rotates with it)
      }
    ]
  }
  ```

  **Anchor derivation (two modes):**
  - `"offset"` — consequence anchors at `trigger_anchor + rotate(offset, trigger_yaw)`. For consequences rigidly attached to the event (dust staining around a breach).
  - `"gravity_projected"` — take `trigger_anchor + rotate(offset, trigger_yaw)`, then scan straight **down** from that position to the first solid block; anchor the consequence on top of it. For debris that "falls": a 4th-floor breach projects its rubble pile onto whatever surface is below — street, awning, or lower roof — without the author knowing the drop height. If no solid block within the region below, the consequence is skipped (logged, not an error). **The scan runs against the post-trigger world state** (trigger fragment's changes applied first, consequences resolved after) — consistent with bundle layering semantics: if the trigger blew out the 4th floor slab, the debris falls *through* the new hole to the 3rd floor, not onto a slab that no longer exists.
  - Consequences run through the same per-fragment clearance/safety pipeline as standalone fragments (`docs/fragment_system.md` §5); a skipped consequence never blocks the trigger. Volume plausibility (debris mass ∝ removed mass) stays the *author's* responsibility — the engine provides anchoring, not physics.
- Prompt-level guidance (agent instructions) is the accepted mitigation for judgment calls; the architecture only needs to make composite events *expressible*, not to simulate physics.

---

## 4. Human Acceptance Loop — In-Game Review Markers

Acceptance is human, in-game: **the player places designated marker blocks in areas needing revision; the agent re-scans, treats each marker as a review annotation, and revises that area.** Agents with multimodal ability may additionally use screenshots.

### Constraints

- New tool (🚧, added to registry): `scan_review_markers(cx, cz, radius_chunks)` → list of `{pos, marker_block, surrounding_context}`. A designated marker block ID set (configurable, e.g. gold_block = "redo this", redstone_block = "too much, tone down") maps to review verdicts.
- Marker blocks must be excluded from styling (add the designated set to a session-level protected list, like NPC markers) and **cleaned up** after the revision is accepted (`clear_review_markers`).
- This closes the aesthetic feedback loop without any rendering infrastructure inside Picasso.

---

## 5. Multi-Source Catalog & Style Families

The catalog must be **multi-source merged** — DD is the first vocabulary, not the only one. Future style families: sci-fi, modern, fantasy, cyberpunk. Future scope extends to **automatic building-area editing/generation**. External call interfaces must be reserved.

### Constraints (some actionable in v0.4 refactor)

- `PICASSO_CATALOG_PATH` → `PICASSO_CATALOG_PATHS` (multiple paths, `os.pathsep`-separated). `CatalogIndex` merges entries; `id` collisions: last-loaded wins with a startup warning. Each entry gains a `source` field. **Cheap now, expensive later — do in the refactor.**
- `query_catalog` gains optional `source` filter.
- Style-family neutrality audit: nothing in core may assume "decay" (already true) or assume the DD namespace (verify — pattern files hardcode `doomsday:*` defaults, which is fine because they're *content*, not engine).
- Building generation is out of v0.5 scope but reserves: the Room system (§1) is deliberately shaped as its seed — a generated building is Rooms all the way down.
- "External call interfaces" = MCP is the only public surface today; keep `core/` importable as a library (no MCP imports — already the layering rule) so a future non-MCP orchestrator can drive it directly.

---

## 6. Player-Activity Awareness — Daily Review of Player Builds

> **SUPERSEDED by `docs/player_activity_pipeline.md`** (v0.5 draft, pending review). This section remains as the intent record incl. the governance-policy rationale; on conflict the spec wins.

### Intent

The agent runs a **daily editorial pass** over the world: see what players built since the last review, judge it, and react in-world. Reactions are driven by the player's constructions *plus* external context (e.g. the Narrative Layer announces "an NPC will settle near the player's base"). Example reactions:

- Build a tent/camp next to a player's base (NPC arrival staging)
- **Correct implausible player structures** — this is a governance duty, not just decoration (see policy below): an overlong sky bridge gains supports and wear *or loses spans*, a floating platform gains anchoring pillars *or partially collapses* — so the world absorbs player artifacts into its fiction *and* refuses to normalize exploit-shaped construction
- Leave traces of NPC activity near frequented player routes

### The core gap: Picasso sees snapshots, not history

Everything Picasso has today reads the *current* save. `detect_structures` can find a player's base geometrically but cannot distinguish player-built from map-original, and cannot answer "what changed since yesterday." **Diffing daily snapshots was considered and rejected**: a full-map block diff is expensive, loses attribution (who built it), loses ordering and time, and misses removed blocks entirely unless both snapshots are kept. The correct source of truth is an **event log recorded server-side while the game runs** — the product owner's instinct is confirmed: this needs a server plugin.

### Required: server-plugin → log → MCP pipeline (three parts)

**(a) Server-side recorder (new component, outside this repo).** A server plugin/mod (Fabric mod or Paper/Spigot plugin — whichever matches the UtilWeDie server stack) that records player block events:

```jsonl
{"t": "2026-07-07T21:14:03Z", "player": "Steve", "action": "place", "pos": {"x": 120, "y": 71, "z": -340}, "block": "minecraft:oak_planks", "dim": "overworld"}
{"t": "2026-07-07T21:14:09Z", "player": "Steve", "action": "break", "pos": {"x": 121, "y": 70, "z": -340}, "block": "minecraft:dirt", "dim": "overworld"}
```

- Format: **JSONL, append-only, rotated daily** (`build_log/2026-07-07.jsonl`). JSONL because the log must be streamable, greppable, and partially readable — a single JSON array fails all three.
- Output location: configurable on the plugin side; Picasso reads it via **`PICASSO_BUILD_LOG_DIR`** (new env var). The directory is a **contract boundary**: plugin writes, Picasso reads, neither imports the other.
- Scope v1: block place/break by players only (not piston/fluid/mob side-effects). Player session events (join/leave, coarse position samples) are a valuable *optional* second stream for "frequented routes" but not required for v1.
- **Contract engineering rules (H7):**
  - Every event line carries a schema version: `"v": 1`. Plugin and reader evolve independently across the contract boundary; an unversioned format's first evolution is a silent parse failure. Reader rejects unknown major versions with a clear log message, not a crash.
  - **Truncated-tail tolerance:** the plugin appends while the reader reads; a half-written final line is normal operation. Reader spec: a line that fails to parse **and is the last line of the file** is skipped silently; a parse failure elsewhere is logged as corruption.
  - **Dimension filter:** events with `dim != "overworld"` are skipped and counted (`events_skipped_other_dim` in query responses) — consistent with the overworld-only stance (open question D4).
  - **Retention:** rotation is daily; retention policy (how many days kept, archival) is the server operator's decision, but the reader must state its cost model: `query_player_activity` cost is linear in the days spanned by `since..until`; queries spanning months should be expected to be slow rather than fail.

**(b) Picasso-side log reader + query tools (new, this repo).** `core/build_log_reader.py` + two MCP tools:

- `query_player_activity(since, until?, player?, bounds?)` → aggregated summary: events per player, clustered into **activity sites** (positions within N blocks merged), dominant block palette, place/break counts, time ranges. Raw events are NOT returned over MCP (same reasoning as `read_region` — too large); the tool returns sites with statistics.
- `get_activity_site(site_id)` → one site in detail: bounding box, involved players, timeline, top palette — enough to judge "new base" vs "mining trip" vs "temporary scaffold".

**(c) Attribution bridge to the structure registry.** `detect_structures` gains optional enrichment from the build log. **Attribution is a fraction, not a boolean (H3):** the entry records `player_attribution: {fraction, builders, first_built, last_modified}` where `fraction` = share of the structure's blocks placed by players per the log. Three states derive from it:

| State | Condition (thresholds tunable) | Bulk-styling behavior |
|---|---|---|
| `player_built` | fraction ≥ 0.5 | Whole structure excluded by default (choke-point row) |
| `player_modified` | 0 < fraction < 0.5 | Structure *is* bulk-targetable, but individual player-placed positions (from the log) are skipped at the choke point; response notes `player_modified_positions_skipped` |
| native | fraction = 0 / no log coverage | Normal targeting |

A binary flag fails both directions: three torches in a native office tower must not immunize the whole tower from styling, and a native building the player substantially renovated must not get chewed by a decay pass. The fraction lives in `detected.*` (it is recomputed from the log at detection time); thresholds are config, not code.

### Constraints on current architecture

- **Amulet must not write a live server's save.** The daily editorial pass runs against a stopped server (maintenance window) or on a copy synced back by server infrastructure. This is an operational contract for the whole SocialWill loop — not Picasso's to solve, but Picasso's docs must state it. Reading the JSONL log has no such restriction.
- **Player structures: protected from *bulk* styling, subject to *deliberate* governance.** Two distinct rules that must not be conflated:
  - *Bulk protection (default):* `player_built: true` structures are excluded from `apply_bundle` / `apply_pass_by_type` targeting. A world-wide decay pass must never chew through a player's base as collateral. Same philosophy as markers and never-touch.
  - *Governance editing (explicit, sanctioned):* the world **is expected to** modify — including partially destroy — player structures judged implausible. Rationale (product owner): if exploit-shaped construction (endless sky bridges, floating platforms, 1-block towers) is never corrected, players learn to depend on its convenience and stop engaging with plausible building; the world's *tolerance* would teach the wrong lesson. Correction is graduated: **reinforce** (add supports/pillars that make the structure diegetically legitimate) → **degrade** (weathering, partial span loss on the untraveled middle of a sky bridge) → **partial collapse** (structure loses function until rebuilt plausibly). Never full deletion — the corrected structure should read as "the world happened to it", not "an admin removed it".
  - Mechanically, governance editing is the existing explicit path (`structure_id` targeting or `include_player_built: true`) — no new write machinery. What is new: **judgment inputs and audit trail.** The structure registry entry should carry an `implausibility` assessment (computed signals: span-to-support ratio, floating-volume fraction, footprint-to-height ratio — the same signal machinery as fingerprints) so the agent's judgment is grounded in queryable data, and every governance edit must be journaled with reason + before-state. **The Phase 8 gate is mechanically enforced (H4):** `include_player_built: true` ∧ pass contains destructive content ∧ `journal_status != "active"` → rejected with `governance_requires_journal` (all three conditions server-decidable; spec'd in `ARCHITECTURE.md` §12.1). Purely additive reinforcement passes the gate — exactly matching the reinforce-before-Phase-8 policy.
  - **Governance scope includes activity sites, not only registry structures (H5):** the named exploit shapes include 1-block towers, whose XZ footprint is below `PICASSO_MIN_STRUCTURE_AREA` (50) — they never enter the structure registry, so `structure_id` targeting can never reach them. Activity sites (log-clustered, no area threshold) close the blind spot: governance edits accept an `activity_site_id` scope alongside `structure_id`, using the site's bounding box. Sky bridges usually clear the area threshold; 1-block towers only exist to the system through the log.
  - Misjudgment recovery relies on `revert_last_apply`'s conflict-safe replay (`ARCHITECTURE.md` §12.3): if the player already repaired the structure after a governance edit, revert skips their repairs and reports conflicts rather than steamrolling them.
  - Player communication is the Narrative Layer's job (an NPC mentions "that bridge finally gave way"), not Picasso's; but the journal entry gives that layer the event to narrate.
- **Reactions are ordinary Picasso operations** (fragments, rooms, passes placed near or — under the governance policy — on player structures). No new write machinery; the novelty is entirely in sensing (log pipeline), judgment inputs (implausibility signals), and targeting (proximity scope, e.g. a `near_structure_id` placement scope).
- The daily-review routine itself (when to run, how to judge) is orchestration — it lives in the agent's prompt/schedule, not in Picasso. Picasso provides sensors and effectors.

---

## 7. Room-Graph Editing, Template Library & Variants

> **SUPERSEDED by `docs/brush_room_system.md`** (v0.5, round-5 reviewed, signed off). This section remains as the intent record; on conflict the spec wins.

### Intent (deepens §1's Room system)

Product-owner conviction: **room/corridor structure editing beats facade editing in value.** The unit of architectural editing is the room and the corridor; the design driver is the **circulation route** — the path a player walks through entrances, rooms, corridors, exits. Decoration layers inside rooms (DD + future decoration mods via the §5 multi-source catalog); decay/aging/corruption brushes layer on top. Three explicit, independently editable layers:

```
1. Structure layer:   rooms + corridors + openings      (what spaces exist, how they connect)
2. Decoration layer:  furnishing via catalog vocabulary  (what the space is furnished as)
3. Condition layer:   decay/aging/corruption brushes     (what state the space is in)
```

The Room data model must keep these layers separate: re-brush a room without re-furnishing it; re-furnish without moving walls.

### Room graph (new abstraction over §1's Room)

A building interior is a **graph**: nodes = rooms and corridors (a corridor is a room with `role: "circulation"` — first-class, not an implicit gap), edges = openings (door, breach, stairway, ladder). The agent designs by editing the graph:

- `add_room(building_structure_id, template, attach_via)` — carve or build a room attached through a new or existing opening
- `remove_room(room_id)` — seal-and-fill, or leave as collapsed rubble (the condition layer decides which)
- `connect(room_a, room_b, opening_spec)` / `seal(opening_id)`
- Route queries: `get_routes(from_opening, to)` — enumerate circulation paths so the agent can reason about flow ("the player should pass the workshop before the vault"; "this wing has no second exit — add an escape breach")

Flood-fill volumes (Phase S1) are the *detector* for existing rooms in found buildings; the graph is the *editable model* for both detected and newly built interiors. **S1's priority rises again** — it now feeds three consumers: tier-2 space classes, §1 brush-density context, and room-graph detection. The graph persists per building as an `interior_graph` field on the structure registry entry (nodes/edges/openings) — no new registry.

### Template library

A **room template** = §1's Room definition (envelope materials, dimensions, contents, entrance slots) saved as JSON in `data/room_templates/`, authored via `create_room_template` — from scratch, or captured from a built example ("successful rooms become reusable stock", mirroring the brush-capture path).

- Templates are **parametric, not literal**: dimensions carry tolerances (a `bunk_room` is 4–7 wide, not exactly 5); contents are density-driven fragment/brush references; envelope materials are **palette slots** resolved per building (the same `bunk_room` renders in concrete inside a bunker, in planks inside a farmhouse). Parametricity is what makes cross-building reuse viable.
- **Palette resolution order (H9 — this is a spec-blocking decision, resolved here):** slot → block mapping resolves through a three-level chain, first hit wins: **(1)** the host structure's `authored.palette` declaration if present (authored, precise, optional); **(2)** derivation from the host's `detected.dominant_materials`, filtered through a slot-compatibility table (a `wall` slot accepts only blocks tagged wall-suitable — this is what stops `glass_pane` being chosen as a wall fill; the compatibility table ships as data next to the taxonomy); **(3)** the template's own declared defaults (every template must declare fallback materials — a template must be instantiable in a building with no palette and useless dominant materials). Catalog-tag lookup as a resolution source is rejected for v1: the catalog covers decoration mods, not vanilla construction blocks, and §5's multi-source merge doesn't change that.
- **Dimension rolls are constraint-solved, not free:** a rolled dimension must keep declared entrance slots aligned with the `attach_via` opening. Tolerance rolling happens *within* the attachment constraint; implementers should treat it as a small constraint-satisfaction step, not independent per-axis rolls.
- Same collision/versioning semantics as all `create_*` artifacts (`name_already_exists`, `overwrite: true` with archival).
- Per-world **instantiation records** (which template/variant/seed produced which room) — needed for telemetry (below) and for re-editing ("this room was a bunk_room; re-condition it as looted"). **Storage (H10): fixed per-world path `<world>/picasso_rooms.json`** — added to `ARCHITECTURE.md` §10's "not configurable" list alongside markers/journal/structures (per-world artifacts never get env vars; the `PICASSO_STRUCTURES_DIR` retraction is precedent). Subject to the same multi-writer protocol (read-merge-write + atomic rename) as the structure registry.
- **Reachability warnings (H10):** graph connectivity is free for the engine and easy for an agent to forget. `seal` and `remove_room` responses must include a `reachability_warning` when the operation leaves any room with no path to an exterior opening ("room_012 no longer reachable from any exterior opening"). Advisory, never blocking — sealed-off spaces are a legitimate design move (hidden vaults, collapsed wings); the warning exists so it is always a *decision*, not an accident.

### Variants — anti-fatigue by design

Owner's concern: a template instantiated 30 times reads as copy-paste. Three layered mechanisms:

1. **Deterministic micro-variation (free, always on):** instantiation is seeded by the position-hash scheme (`ARCHITECTURE.md` §6) — dimensions roll within tolerance, contents roll per instance, palette jitters. Two instances of one template are automatically non-identical, the same way two rubble piles already are. This covers most fatigue at zero authoring cost.
2. **Authored variants (structural difference):** a template may declare siblings (`bunk_room/spartan`, `bunk_room/looted`, `bunk_room/barricaded`) — same graph role and entrance slots, different contents/condition presets. Instantiation picks by position-hash roll with configurable weights.
3. **Usage telemetry drives authoring, not generation:** the registry counts instantiations per template/variant (`list_room_templates` reports counts). Crossing a usage threshold with too few variants raises an authoring suggestion to the agent ("bunk_room used 23×, 1 variant — author 2 more"). The system never auto-generates variants; agent/human authorship stays the quality gate.

### Constraints on current architecture

- Builds directly on already-reserved seams: §1 Room, `room_envelope` write context (§12.1), Phase S1 flood fill, §5 multi-source catalog, §1 brush system (condition layer). No v0.4.3 contract blocks this — the seams were cut for it.
- New artifacts: `data/room_templates/` registry + per-world instantiation records + `interior_graph` on structure entries.

---

## 8. Module Boundary — Picasso inside SocialWill

Picasso is one module of the three-layer SocialWill system: **World Layer (Picasso)** · NPC society simulation (not built) · in-game wargame/faction layer (not built). Standing requirements this places on Picasso:

- **Independently runnable and verifiable:** Picasso works standalone (MCP + a world file), no dependency on the other layers existing. Already true; must stay true.
- **All cross-layer interfaces are data contracts, not code imports:** NPC markers (`picasso_markers/`, outbound), build log (`build_log/` JSONL, inbound — §6), structure registry (`picasso_structures.json`, shared read). Each contract is documented well enough that the other layers can be developed against the files alone.
- The daily editorial loop (§6) is the **first closed loop across layers**: players act (game) → plugin logs → agent senses (Picasso queries) → agent reacts (Picasso writes) → players see the world respond. It is the integration test bed for the whole SocialWill architecture — worth building early for that reason alone, before the society simulation exists.
