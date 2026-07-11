# Board, Terrain & Fog of War — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> The board is the abstract world (doctrine §1): sparse, chunk-celled, sensed opportunistically
> (D-NOCHUNK), reconciled toward loaded reality (D-REALITY).

---

## 1. The Board

Sparse map `cell_key → Cell`, `cell_key = (dimension, chunkX, chunkZ)` packed as
`dim_index:long`. Cells materialize when first **explored by any faction** or **touched by a
strain**; the untouched world costs zero bytes and zero ticks. v1 processes
`minecraft:overworld` only (same scoping as the build-log reader; other dims are counted and
skipped).

```
Cell {
  controller: none | faction_id,          // strains are factions too (ARCH §5)
  anchor:     core_id | purchase→core_id | strain_intensity | camp_core | none,   // D12/D17
  terrain:    open | forest | urban | water | ruin,     // §2
  road:       0..2,                       // D47 §2.1 — none / street / arterial
  loot_richness: 0..3,                    // §3, scavenging input
  fortification: 0..3,                    // building-derived, econ doc
  integrity:  0..hp_max,                  // D42 siege gate — governance.md §7
  ambient_level: 0..3,                    // zhanghai field (ai_factions §6.3a)
  intensity:  0..3,                       // strain cells only
  flags:      contested · scarred_infested · derelict_infested · derelict,
  sensed_at:  tick | never                // last loaded-chunk refinement
}
```

Per-faction data (fog, exploration) lives outside the Cell in per-faction fields (§6) — the
Cell itself is objective truth.

## 1.1 The World Boundary (D43)

An admin-drawn outline at cell resolution — painted on the admin strategic map or via
`/chess boundary` commands, stored in `ChessSavedData`, live-editable, API-exposed — bounds
**both worlds**:

- **Board**: cells outside the boundary never materialize (no exploration, no strain touch,
  no ephemeral placement); counters, missions, strain growth, the ambient field, and the
  wildlife band all clip to it; pathing treats outside as void.
- **Players**: the same outline binds physical movement via a custom enforcement layer
  (escalating: warning overlay → pushback → damage, `boundary_enforcement` config) — vanilla
  WorldBorder is square-only and cannot express a drawn outline.
- Tooling: the Xaero import and tile pyramid clip to the boundary; the map renders
  out-of-bounds as void/parchment.

Edit semantics (SC8):

- **Shrinking over materialized/controlled cells is refused** unless `--force`; forced,
  affected cells de-control at the next round end (assets → derelict, owners notified and
  logged — an admin act, never silent).
- **Observer-mode strains may physically spread past the board's clip** — named the *rim
  divergence*: accepted pre-L4, logged `infection/divergence`; in driver mode the adapter's
  paint/purge duties include clipping to the boundary.
- **Players already outside at edit time** get the escalation ladder from its first rung:
  warning overlay with a grace period (`boundary_grace_ticks`, 1200 ≈ 60 s) before pushback
  ever engages — no instant damage or teleport.

## 2. Terrain Classes & First-Touch Sampling

On cell materialization the `TerrainSampler` derives `terrain` **without loading anything**:
biome lookup (available from world gen without chunk load? — **no**: biome queries can trigger
generation. Rule: at materialization time the triggering context always has the chunk or its
data in hand — a player stood in it, a counter's vision touched it *from* a loaded
neighborhood, or a strain adapter sensed it. Sampling uses only that in-hand data; a cell
touched purely abstractly (distant strain growth) gets `terrain: open` provisionally with
`sensed_at: never` and refines on first real load. This keeps D-NOCHUNK absolute).

Classes: `open` (plains/desert/etc) · `forest` (lumber-camp eligible) · `urban` (structure
density high — from Picasso's structure registry when readable, else column-sample heuristics)
· `water` (ocean/river majority — impassable v1) · `ruin` (urban + heavy damage markers).
Terrain drives combat modifiers (counters_and_combat.md §5.3), movement cost (§3 ibid.),
production eligibility, scavenge yield, and map tinting.

## 2.1 The Road Layer (D47)

`road: 0..2` per cell, feeding road-dependent mobility (counters_and_combat.md §3.1) and the
map's road overlay. Two sources, **merged by recency — the newer source wins per cell; max-wins only between same-age sources (R11: destroyed roads must be able to die; a stale product value never outlives fresh sensing)**:

1. **Own sensing**: loaded-chunk sampling against `data/swchess/road_blocks.json` (path
   blocks, asphalt families) — rides the §4 sense queue.
2. **Picasso road product (integrations ask 10)**: a cell-resolution road-class layer +
   connectivity graph published read-only beside the structure registry — covers unloaded
   areas from day one. Re-read after round increments (same cadence as the registry).

Xaero/tile alignment is automatic: the terrain tiles and the road layer derive from the same
world — the strategic road overlay drawn from cell data depicts the same roads visible in
the tile base.

## 3. Loot Richness

**Abstract and renewable (D40 — resolves A2).** Initial estimate by terrain (`urban 3 ·
ruin 2 · forest 1 · open 1 · water 0`, PLACEHOLDER). The `LootScanner` refines
`base_richness` **exactly once** (first-ever load; sampled count against
`scavenge_blocks.json`) — it never re-derives the value afterwards: current richness is
`clamp(base − harvested + regen, 0, base)` with a per-cell `harvested` ledger and per-round
regeneration (econ §6). Physical blocks are deliberately untouched by abstract depletion, and
hand-looting is a parallel channel, not a conflict. Picasso restyles that add lootable
dressing may raise `base` (receipt-triggered re-estimate — the one sanctioned re-derivation,
integrations.md §3).

## 4. Loaded-Chunk Sensing

One budgeted queue (`sense_budget_ms_per_tick`, 2 ms) serves all sensing: terrain refinement,
loot scans, core-block audits (does the registered building's core still exist? broken →
`building_destroyed` ingest), strain census (adapter `sense`/`countUnits`), scar verification.
Chunk-load events enqueue work items; the queue drains oldest-first within budget; overflow
simply waits (sensing is eventually-consistent by design). Nothing in this queue ever loads a
chunk — it only rides loads that players cause.

## 5. Sectors

Faction-defined named cell groups (`sec_<seq>`, non-overlapping within a faction, cells must be
faction-controlled). Uses: democratic-centralism jurisdictions (governance.md §3–§4), UI
grouping/dashboards, quota scoping (econ open Q5). Auto-suggestion (flood-fill contiguous
territory) is a UI nicety, not a rule.

## 6. Fog of War (per player faction)

Three states per (faction, cell):

**Terrain is not a secret (A4 ruling).** The tile base layer is, in fiction and in fact, the
old world's public geography — every survivor owns pre-apocalypse maps. Fog therefore guards
the **living layer** (control, units, changes, ambient levels), never static geography; for
tiles, fog gates *change propagation* (patch distribution, map_interface §2), not existence.
"Anti-cheat by construction" (ARCHITECTURE §9) applies to living data exactly as before.

| State | Meaning | Client sees |
|---|---|---|
| `unknown` | never explored | old-world terrain tile only — zero living data, no grid overlay |
| `explored` | seen before, not now | **memory snapshot** of living data at the tick visibility was lost (grey-masked, D34); terrain tile stays at the version last seen while explored — scars never appear inside your fog (A4-3) |
| `visible` | in live vision | live: controller, counters (side + template class + rough strength band — not exact numbers), battles, intensity, ambient level |

**Vision sources** (union, recomputed incrementally on change events, not per tick):
controlled cells + radius 1 · counters (`sight_radius`) · buildings (faction_core 2,
policy_center 1, others 0, PLACEHOLDER) · online member positions radius 2 · tavern rumor
reveals (one-shot snapshot refresh, econ doc §4). AI factions use the same machinery
(ai_factions §2.3); no omniscience anywhere.

**Enforcement is server-side** (ARCHITECTURE §9): snapshots are filtered per faction before
serialization; `visible`-only detail never reaches a client that lacks it. Bloc members do
**not** share vision v1 (confirmed checkpoint 4). Exact enemy stats are never shown even in
`visible` — strength bands (`weak/steady/strong`) keep scouting valuable without making the
map a wallhack.

**UI cue (D34)**: when a cell drops from `visible` to `explored`, the client animates a grey
mask over it — players *see* their vision receding rather than discovering stale data later.

**Intel trading & alliance sharing (D34; offer lifecycle per C4)**: intel moves through a
two-sided handshake — `offer(target, cell set, price in any currency, expiry
intel_offer_ttl_ticks 180)` → buyer previews **cell list + per-cell snapshot ages** (never
content) → `accept` settles **atomically** (payment and merge in one operation, or neither) /
`decline` / expiry; open offers per faction pair capped (`intel_offers_max` 3). On accept the
buyer's `explored` layer merges the seller's snapshots (stamped with source + snapshot time, rendered
as intel, not live vision; **merge rule: newest-timestamp-wins per cell** — bought intel
never overwrites fresher first-hand knowledge, B4). Alliance-grade info-sharing is the same
mechanism as a standing
permission (auto-share regions), gated by leadership. All transfers log
`territory/intel_traded`. One deliberate consequence: intel can be stale or *weaponized*
(selling old snapshots is legal — caveat emptor, and the timestamp is visible).

## 7. Events (topic `territory`)

`cell_explored` (per faction, visibility faction-scoped) · `cell_claimed` (core placed/built) ·
`cell_purchased` · `cell_decontrolled` · `cell_captured` → superseded by
claim events (capture grants no control, D12 — the event pair is `cell_decontrolled` +
later `cell_claimed`) · `terrain_refined` · `sector_created/edited` · `core_destroyed`.

## 8. Config

`sense_budget_ms_per_tick 2` · vision radii table · strength-band thresholds ·
loot initial estimates · `sector_max_cells` (64).

## 9. Open Questions (v0.1)

1. ~~Memory-snapshot approach~~ **Resolved (checkpoint 4 + D34)**: snapshot-on-visibility-loss
   confirmed, with the grey-mask transition cue (§6); golden test still required.
2. ~~Bloc-shared vision~~ **Resolved (D34)**: no automatic sharing; intel trading and
   alliance info-sharing permissions (§6) are the explicit channels.
3. ~~Water cells~~ **Resolved (D23)**: impassable v1 confirmed; boats/bridges remain v0.2
   material (counters_and_combat.md Q5).
4. ~~Terrain provisional-default~~ **Resolved (checkpoint 4)**: accepted inaccuracy;
   refines on first load.
