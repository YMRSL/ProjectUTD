# Agent Playbooks — Tool-Surface Walkthroughs

> Paper-run validation of the tool surface against three real scenarios, written **before implementation** so gaps surface as spec fixes, not runtime surprises. Doubles as system-prompt material for agents driving Picasso. Tracks `ARCHITECTURE.md` v0.4.4 + signed-off v0.5 specs.
>
> Convention: `→` = tool call; *italic* = agent judgment; **GAP** = friction found by the walkthrough and where it was resolved.

---

## Playbook 0 — Read-Only Structure Interpretation

**Situation:** the user asks what a complex local construction means. No world
change is requested. The `interpret_world_structure` MCP prompt is selected with
the user-confirmed inclusive bounds.

```
0.  → set_world(user-confirmed path)
1.  → analyze_region(chunks covering bounds, bounded y_min/y_max)
       record candidate kinds, bounds, confidence, totals and truncation
2.  → inspect_volume(exact focus bounds)
       if complete=false: subdivide and inspect every relevant sub-volume
3.  *Interpret* exact properties/connectivity:
       observed facts → ranked meanings + alternatives → unresolved risks
4.  Ask for human intent when a novel/ambiguous label matters.
5.  → close_world()
```

Rules:

- Do not call any write tool; interpretation is not write authorization.
- A `storey_level_candidate` is not automatically "second floor".
- Missing runs mean air only when `complete=true`.
- One confirmed construction becomes a fixture. It becomes a global rule only
  after multiple positives, meaningful negatives, tests, and human approval.
- Formal candidate/evidence IDs and authored registry reviews land in S4; until
  then cite tool response bounds, palette entries, and role counts.

Normative contract: `docs/agent_semantic_review.md`.

---

## Playbook 1 — Full TLOU Stylization of a 20×20-Chunk District

**Situation:** urban district, world copy, `PICASSO_MODDED_WRITE_VERIFIED=true` (Phase 1.5 passed), journal live. 20×20 chunks exceeds `PICASSO_MAX_RADIUS_CHUNKS=12` (25×25 max is the *radius* cap: radius 12 = 25×25, so a 20×20 area fits **one** read — but at ~40M candidate positions it may be memory-unwise; this playbook uses 4 tiles of radius 5 to model the tiling path deliberately).

```
0.  → set_world(copy_path)
       check journal_status == "active"; note noise_backend
1.  → detect_structures(cx=10, cz=10, radius_chunks=12)     # covers the district
       registry now has buildings/roads; player attribution computed (if log configured)
2.  *Review* → list_structures(type_filter="building") — sanity: counts, bounds plausible?
3.  Per tile (4 tiles, radius 5, 1-chunk overlap per §12.4 tiling caveat):
    a. → apply_bundle("tlou_complete", cx, cz, radius_chunks=5,
                      structure_type_filter="building", dry_run=true)
         inspect would_change, region_mode_warning, space_classification tier
    b. → apply_bundle(..., dry_run=false)
    c. → apply_bundle("tlou_complete", cx, cz, radius_chunks=5,
                      structure_type_filter="road", dry_run=false)
4.  → analyze_region over 2 sample tiles: vegetation_coverage & damage_estimate
       rose to target band? If under: re-run selected passes at higher intensity
       (idempotency warning: fragment passes are NOT idempotent — §1; prefer
       raising intensity on a REVERTED tile over double-applying)
5.  In-game visual pass; player drops review markers where needed
6.  → scan_review_markers(...) → localized re-runs → clear_review_markers
```

**Findings from this walkthrough:**
- **GAP (resolved in spec):** structure mode would remove the per-tile double-application risk entirely — noted as the reason `apply_bundle` structure mode is the intended end-state; region+filter is the documented interim.
- **Friction (accepted, documented):** step 3's tile loop is agent-managed. A `tile_iterator` convenience was considered and **rejected**: the agent choosing tile seams (through streets, per §12.4) is a judgment call, exactly what shouldn't be automated pre-halo.
- **Rule of thumb for agents:** always `structure_type_filter` per entry-type in region mode; never run the same bundle twice on a tile without reverting first.

---

## Playbook 2 — One Day of the Editorial Loop (with wargame settlement)

**Situation:** maintenance window opens (server stopped, save synced at T_sync). Journal live, build log configured, work orders pending from the wargame's round settlement.

```
0.  → set_world(synced_save)         # journal_status, player_protection both "active"
1.  → query_player_activity(since=<last window close>)
       *Judge sites*: site_3fa9 "construction", 539 events, 2 players → a real base
2.  → detect_structures over active areas
       (scope rule, player_activity_pipeline §9 Q4: union of new sites' bounds,
        1-chunk padded)  → struct_0107 appears, attribution "player_built"
3.  → process_work_orders(dry_run=true)   # inspect the queue first
       pending: wg_00042 construct bunk_room @ struct_0044 (faction survivor_camp)
                wg_00043 degrade sky_bridge site_77ab (reason: implausible span)
       *Check*: wg_00043 targets an activity site (H5 path) with reason — legit
4.  → process_work_orders(dry_run=false)
       receipts: wg_00042 applied (room_inst_0031); wg_00043 applied, journaled
5.  Editorial reactions:
       *NPC arrival staging near the new player base* (narrative context says so)
    → add_room(struct_0107_adjacent…) — NO: struct_0107 is player_built; a tent
       BESIDE it targets terrain, not the structure → construct-freestanding
       via add_room annex on adjacent ground, include_player_built NOT needed
       (annex predicate forbids intersecting their structure anyway — I2)
    → place_npc_marker(x,y,z, npc_type="ambient", faction="survivor_camp")
6.  → revert check: journal shows wg_00043's degrade; if the wargame misjudged,
       next window can revert_last_apply → conflicts[] reports any player repair
7.  Window closes; window_log.json appends round 19
```

**Findings:**
- **GAP (resolved):** step 5 initially reached for `add_room` on the player's structure — the I2 annex predicate catching this *in the walkthrough* is the pre-validation working as designed. Playbook rule: **reactions near player builds target terrain or wargame-owned structures, never the player's own bounds.**
- **Ordering is load-bearing:** steps 1→2 before 3→4 is the §6-pipeline normative order (attribution refresh before writes). The playbook makes it muscle memory.

---

## Playbook 3 — Learn a Style from a Reference Save, Apply to Target

**Situation:** styled reference save (hand-built ruins district); goal: reusable bundle for the main world. All learning tools 🚧 — this playbook is the acceptance walkthrough for Phase 6.

```
1.  → set_world(reference_save)
2.  → analyze_region(cx, cz, 6)          # orient: surfaces, patterns, coverage
3.  → learn_style(cx, cz, 6, name="ruins_ref")
       profile: 38% mossy, matched_fragments[rubble_pile_small ~0.08],
       unmatched_cluster_count 7 → suggested_extract_clusters
4.  → extract_block_clusters(cx, cz, 6, min_occurrences=5)
       12 clusters; candidate_groups grp_01 = {cl_0001, cl_0003, cl_0009}
       rotation-variants (E9) → *sum occurrences for density, pick one
       representative orientation as canonical frame*
5.  *Author*: for the 3 most characteristic clusters:
    → create_fragment("ref_pipe_debris", …, probability values set BELOW
       observed density — style_learning §10 conservatism)
       (wall-anchored cluster: set orientable=true, author in canonical frame —
        capture_brush would do this automatically (I4) but cluster data is
        already offset-normalized; rotate manually per fragment_system §4)
6.  → create_bundle("ruins_style", entries=[building: material_hints(0.4),
       ref_pipe_debris-pass(0.6), rubble_scatter(0.7), furniture_modreplace(1.0)])
       — wrap fragments in fragment_passes first: bundle entries reference
       PASSES, not fragments (fragment_system §6 retraction)
7.  → set_world(target_copy) → apply_bundle("ruins_style", …, dry_run=true) → apply
```

**Findings:**
- **GAP (resolved in v0.5 specs):** step 6's "wrap fragments in a pass first" requires hand-authoring a fragment-pass JSON — `create_pass` is block-pass-only (E13). The Brush system closes this: `create_brush` IS the runtime authoring surface for fragment application (brush_room_system §2), superseding the D6 ruling's "brush tools will own it". Playbook note: post-R1, step 6 uses create_brush instead of hand-written pass JSON.
- **Checklist reminder surfaced:** every E/F/G/H-round response field an agent should check is exercised across these three playbooks: `journal_status`, `noise_backend`, `space_classification`, `space_kinds`, `region_mode_warning`, `player_protection`, `reversibility_warning`, `modded_write_warning`, `conflicts[]`, `reachability_warning`, `no_match_warning`, `fatigue_warning`, `log_coverage`.

---

## Cross-Playbook Rules (system-prompt distillate)

1. **Always preview.** `dry_run: true` first, inspect, then apply with identical args + seed. Verify `noise_backend` matches between the two if the session changed.
2. **Never re-apply onto un-reverted output.** Fragment passes layer; re-running doubles the story.
3. **Read the warnings.** Every warning field above exists because a review round found silent failure; silence in your transcript should mean *checked and clean*, not *didn't look*.
4. **Player bounds are not your canvas.** Terrain beside, wargame orders with reason and journal — those are the two doors. There is no third.
5. **The journal is your undo, your audit, and the narrative layer's story feed.** Operations that can't be journaled (pre-Phase-8) get world copies; no exceptions on shared saves.
6. **Tiles: seams through streets, 1-chunk overlap, structure_type_filter always** (until halo + structure mode land, then this rule dissolves).
