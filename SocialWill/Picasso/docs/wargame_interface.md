# Wargame ↔ Picasso Interface — Work-Order Contract (v0.5 draft 2)

> **Status: 🚧 draft 2 — round-7 findings (REVISION_LOG §J: J3/J4/J5 + Q-rulings) applied; pending reviewer diff spot-check, then normative.** Tracks `ARCHITECTURE.md` v0.4.4. Extends the §8 module-boundary doctrine (data contracts, not code imports) to the third SocialWill layer: the **wargame/faction engine**. The wargame engine itself lives outside this repo; this document is the *complete* contract it develops against — plus the Picasso-side machinery to honor it.
>
> Product-owner decisions this encodes: the wargame is **real-time** (runs while the server is live, via an AI-accessible plugin); Picasso and the wargame run **alternately** (server live ↔ maintenance window); **maintenance-window entries are the wargame's round clock**; after round settlement the wargame submits build/effect requests; Picasso's job is to *reserve sufficient API*, not to simulate factions.

---

## 1. Execution Model — Alternation, Not Coexistence

```
        server LIVE                      maintenance WINDOW
  ┌──────────────────────────┐      ┌───────────────────────────┐
  │ players act              │      │ server stopped            │
  │ plugin logs + senses     │ ───▶ │ Picasso opens the save    │
  │ wargame engine runs      │      │ consumes work orders      │
  │ (real-time, in-memory +  │      │ applies world edits       │
  │  light in-game feedback  │      │ writes receipts + window  │
  │  via plugin API)         │ ◀─── │ record; closes            │
  └──────────────────────────┘      └───────────────────────────┘
        round N plays out               round N settles into
                                        the physical world
```

Two clean consequences, stated as doctrine:

1. **Picasso never runs against a live save** (existing operational contract, v05 §6). The wargame never *waits* on Picasso — it keeps playing; the world's physical response arrives at the next window. Heavy world change landing "overnight" is a rule of the fiction, not a bug.
2. **The window boundary is the round boundary.** The wargame counts window entries as rounds (e.g., a player applies for a building in-game → the wargame requires K window-entries of upkeep → at settlement it emits a `construct` order). Picasso provides the authoritative window clock (§5) so the wargame never has to guess whether a window actually ran.

**Real-time boundary (what does NOT go through Picasso):** live, lightweight feedback — chat, scoreboard, holograms, single sign/banner placement, particle cues — belongs to the **plugin's own server-API surface** while the game runs. The §6 build-log plugin is the seed of that surface; its evolution into an AI-accessible live API is a plugin-repo concern, out of scope here. Rule of thumb: *if it changes ≤ a handful of blocks and must be visible this session, it's plugin; if it changes structures, it's a work order.*

---

## 2. The Work-Order Queue — `<world>/picasso_workorders/`

Fixed per-world layout (goes into §10's not-configurable list):

```
picasso_workorders/
├── pending/     ← wargame writes  <order_id>.json   (atomic: temp + rename, H11)
├── applied/     ← Picasso moves consumed orders here, receipt embedded
├── failed/      ← orders Picasso could not apply (receipt embedded, §6)
└── window_log.json  ← the round clock (§5), Picasso-owned
```

Ownership: **wargame writes only into `pending/`; Picasso owns everything else.** `order_id` is the dedup key: a re-submitted id that already exists in any directory is skipped with a receipt warning — the wargame may safely retry submissions after a crash.

**Two-phase consume (J5 ruling — idempotent re-execution was considered and REJECTED):** determinism guarantees same-seed-same-diff only against the *same world state*; a crash mid-`construct` leaves a half-written envelope, so a naive re-run's I2 pre-validation sees the debris and either refuses or solves to a *different* position — a second half-room, not a repair. Therefore:

1. **Consume:** move `pending/<id>.json → applied/<id>.json` with `"receipt": {"status": "in_progress", "window_id": …}` *before* executing.
2. **Execute**, journaling normally.
3. **Finalize:** rewrite the receipt to its terminal status (atomic rewrite, H11).

Crash recovery (next window's first act): any `applied/*.json` still `in_progress` is moved to `failed/` with `status: "crashed_window"` — **never auto-re-executed**. Cleanup of its partial writes is a journal revert (the entries exist up to the crash point; H6 conflict-safe replay applies); the wargame decides whether to re-submit a fresh order after the receipt tells it what happened. One window, one writer, so `in_progress` can never be a concurrent-execution marker — only a crash marker.

### 2.1 Order schema

```json
{
  "v": 1,
  "order_id": "wg_00042_construct_bunkroom",
  "issued_round": 17,
  "issued_at": "2026-07-07T21:00:00Z",
  "on_behalf_of": {"layer": "wargame", "faction": "survivor_camp", "actor": "player:Steve"},
  "kind": "construct",
  "params": { ... per-kind, §3 ... },
  "priority": 50,
  "expires_after_round": 20,
  "requires": []
}
```

- `on_behalf_of` is **mandatory** and flows into every journal entry, marker, and registry `authored` field the order produces (§4). `actor` is optional context (the player whose application triggered this).
- `priority`: lower = earlier within a window. Ties break by `issued_at`, then `order_id` (total order → deterministic processing sequence).
- `expires_after_round`: if the current window's round number exceeds this, the order moves to `failed/` with `expired` — the wargame's world may have moved on; stale construction is worse than none.
- `requires`: list of `order_id`s that must be in `applied/` before this order runs (same-window chains allowed; a failed dependency fails the dependent with `dependency_failed`, never half-runs it).

### 2.2 Receipt (embedded into the moved file under `"receipt"`)

```json
{
  "receipt": {
    "status": "applied" | "partial" | "failed" | "expired" | "rejected",
    "window_id": "w_2026-07-08T03", "round": 18,
    "journal_entries": ["..."], "structure_ids": ["struct_0107"], "instance_ids": ["room_inst_0031"],
    "conflicts": [], "errors": [], "warnings": [],
    "summary": "bunk_room instantiated on struct_0044; 2 placements skipped (small room)."
  }
}
```

The receipt is the wargame's *only* feedback channel and is therefore complete: everything the wargame needs to update its own state (which structures now exist, which faction owns them per the journal) is in the receipt — it never needs to scan the world.

---

## 3. Order Kinds — mapping to existing Picasso machinery

**No new write machinery.** Every kind compiles to operations this documentation already specifies; the queue is a *dispatcher*, and every dispatched operation passes the same gates (choke point, H1 player protection, H4 governance, modded-write gate) as an agent-issued call.

| `kind` | `params` (essentials) | Compiles to | Notes |
|---|---|---|---|
| `construct` | `template`, `target` (structure_id + attach, or `bounds` search-region hint for freestanding), `variant?`, `seed?` | `add_room` (carve/annex) — or, freestanding, envelope-first room chain | Player-applied buildings settle here. Freestanding v1 = single-room annex on terrain within the `bounds` hint (J9/Q1); multi-room buildings = `requires`-chained orders. Builder-consent override rules: §7 |
| `fortify` | `structure_id`, `bundle` (rehabitation-family) | `apply_bundle` structure mode / reinforce-style passes | Additive → passes H4 without journal |
| `degrade` | `structure_id \| bounds`, `bundle \| composite_event`, `reason` (mandatory) | governance path (H-series) | **Journal-gated (H4) — no live journal, order fails `governance_requires_journal`.** `reason` lands in the journal entry. **Targeting sub-registry builds (J3): explicit `bounds`, not `activity_site_id`** — site ids are MCP query artifacts; the wargame is plugin-side and never sees them. The wargame watches the sky bridge live and knows exactly where it is; `bounds` + `reason` is its native vocabulary. An optional `site_ref` field may carry a site id when a *Picasso agent* authors the order, but Picasso resolves `bounds` authoritatively either way |
| `composite_event` | `event`, `anchor` (structure_id + face hint or pos) | CompositeEvent placement (v05 §3) | Battle damage: breach + projected debris as one causal unit |
| `restyle` | `structure_id`, `bundle`, `intensity?` | `apply_bundle` structure mode | Occupation flavor: faction takes a building, world shows it |
| `mark` | `pos`, `npc_type`, `faction`, `dialogue_id?` … | `place_npc_marker` | Marker schema already carries `faction`/`source_agent` — designed for this since v0.1, now consumed |
| `demolish` | `structure_id \| room_id`, `mode: "collapse" \| "abandon" \| "fill"` | `remove_room` / collapse-family CompositeEvent | Journal-gated when target is player-attributed |

**Mechanical template extension (🚧):** a future `construct` order may name a
versioned `MechanicalTemplate`. It then delegates to the game-native mechanical
executor in `docs/mechanical_structures.md`, not `add_room` or raw block writes.
The ordinary target/governance/journal contract still applies, and a missing
per-mod/template capability causes the order to fail closed.

**Explicitly absent:** a raw `write_blocks` kind. The wargame speaks in *semantic* orders; if it needs vocabulary Picasso lacks, the vocabulary (template/bundle/fragment) gets authored first — same rule as for agents. This keeps every wargame effect inside the safety, determinism, and journal story.

### 3.1 Player-application flow (the canonical chain, end to end)

```
LIVE:    player requests a building (in-game command/sign — plugin captures)
         → wargame validates (resources, faction rules), tracks rounds
WINDOW:  (nothing — upkeep rounds accumulate)
LIVE:    round K settlement → wargame writes construct order to pending/
WINDOW:  Picasso consumes: solves placement (I2 pre-validation — never
         carves the player's own base without include_player_built),
         instantiates, journals with on_behalf_of, receipt names the
         instance + structure
LIVE:    wargame reads receipt, announces in-world via plugin;
         narrative layer may read the same journal entry for an NPC line
```

---

## 4. Attribution — `on_behalf_of` threading (schema extensions)

Three small, backward-compatible field additions (pending-edits list, §8):

1. **Journal entries** gain optional `on_behalf_of` (object, §2.1 shape). Journal queries (Phase 8 query layer) accept an `on_behalf_of.faction` filter — "what did survivor_camp change in these bounds" becomes answerable; that is the wargame's audit trail *and* the narrative layer's story feed.
2. **Registry `authored`** gains optional `controlled_by: {faction, since_round}` — written by `construct`/`restyle` orders, readable by everything. Detection never touches it (H8 shields it by construction).
3. **NPC markers**: no change needed — `faction`/`source_agent` existed since v0.1; work orders simply populate them.

---

## 5. The Round Clock — `window_log.json`

Picasso-owned, append-only, atomic-rewrite (H11):

```json
{"v": 1, "cadence_hint": "daily",
 "windows": [
  {"window_id": "w_2026-07-08T03", "round": 18,
   "opened_at": "...", "closed_at": "...",
   "orders_applied": 4, "orders_failed": 1,
   "picasso_version": "0.5.0", "save_synced_from_live_at": "..."}
]}
```

`cadence_hint` (optional, ops-set, free-form): declared window cadence so the wargame can price upkeep in wall-time. Informative only — tolerate absence and violation. `save_synced_from_live_at` is copied from `<world>/picasso_sync.json` (written by the sync infrastructure at copy time — `docs/player_activity_pipeline.md` §7.2); absent sync file → `null` here, and attribution ran degraded that window.

- `round` is a **monotonic counter incremented once per window entry** — the authoritative round number. The wargame reads it live (the file is readable while the server runs; it's Picasso-written during windows only, so no write contention by construction).
- `save_synced_from_live_at`: when server infrastructure snapshots the live save for Picasso, the sync timestamp goes here — the wargame can tell exactly which live-state cut a window operated on (build-log events after this instant were invisible to that window's H1/H2 protection checks; the wargame should treat them as round-N+1 material).
- A window that opens and applies zero orders still appends (rounds advance on maintenance cadence, not on work volume).

---

## 6. Failure & Partial Semantics

- Per-order isolation: one order's failure never aborts the window; remaining orders proceed (same continue-on-error doctrine as `apply_bundle` §11).
- `partial`: some sub-operations applied (e.g. room built, 2 placements skipped, 1 scatter conflict). The receipt itemizes; **the wargame decides** whether partial satisfies the player's application — Picasso does not retry autonomously.
- `rejected`: order failed a *gate* (H4 without journal, H1 protection without `include_player_built`, modded-write gate, malformed schema, unknown `kind`, unknown template). Rejection reasons reuse the standard error codes verbatim.
- **The wargame must model refusal.** Any order can come back failed — terrain doesn't fit the template, the target got player-protected since settlement, the journal isn't live yet. A wargame that assumes orders always apply will desync from the world; the receipt loop is the sync mechanism, and consuming it is a *requirement* on the wargame engine, stated here because the contract is the only place the two teams meet.

---

## 7. Security Boundary

The queue directory is a filesystem trust boundary with exactly two authorized writers (wargame engine → `pending/`; Picasso → the rest). Orders are **data, not code**: every effect passes Picasso's own gates, so a malformed or hostile order can waste a window slot but cannot bypass never-touch, marker, or player protection — the same argument that makes agent tool-calls safe.

**Player-protection override matrix (J4 — consent is a channel, not an exception):**

| Order kind | `include_player_built` honored when |
|---|---|
| `degrade` / `demolish` | `reason` present (governance path, H4 journal gate applies) |
| `construct` / `fortify` / `restyle` | **builder consent**: order's `on_behalf_of.actor` names a player in the target structure's `builders` list — the flagship flow (a player applying to extend *their own base*) would otherwise be blocked by their own protection. The journal entry records `consent: "builder"`. No consent + no governance reason → the override is ignored and the order fails the H1 row like any other write |
| `composite_event` / `mark` | never (battle damage against player structures goes through `degrade` with reason; markers don't need the override — placement beside, not inside) |

The protection exists to stop the world (and other factions) from chewing a player's base as collateral or without cause; the owner asking for work on their own structure is neither. Consent is verified against `detected.builders` (log-attributed, not self-declared), lands in the journal, and is per-order — sanction is never ambient.

---

## 8. Pending Edits to Other Docs (apply after review)

1. ARCHITECTURE §10 not-configurable list: `picasso_workorders/`, `window_log.json` (done in v0.4.4), `picasso_sync.json` (J1 — pending).
2. ARCHITECTURE §12.3 journal format: optional `on_behalf_of` (done in v0.4.4); Phase 8 query layer gains faction filter; journal entries record `consent: "builder"` where J4 applies (pending).
3. semantic_segmentation §6/§7: `authored.controlled_by` (one line; H8 already shields it).
4. v05_forward_requirements §8: the module-boundary table gains the queue + window log rows; note that the daily editorial loop and the wargame share one plugin.
5. tool_specs: new tool `process_work_orders(dry_run=true)` — the window-time entry point (also invocable by an agent mid-window for inspection); plus `list_work_orders(status_filter)`, read-only.
6. New error codes: `order_expired` · `dependency_failed` · `unknown_order_kind`.
7. implementation_order: new Phase 9 (work-order dispatcher) — after Phase 8 (journal is what makes `degrade`/`demolish`/receipts complete); the queue *format* can ship earlier so the wargame team develops against it.

---

## 9. Open Questions — adjudicated in round 7 (REVISION_LOG §J9)

1. **Freestanding `construct` siting** → **optional `bounds` hint on the order is enough for v1**: the wargame supplies a preferred area (it can see the player's request location live); the solver treats it as the search region, terrain-adaptive foundation rule (annex floor support) governs admissibility within it. No lot/parcel formalism until building generation (post-v0.5) demands one.
2. **Window atomicity** → **two-phase + quarantine** (ruling applied in §2 above); idempotent re-execution rejected — half-written envelopes change the world the re-run would solve against.
3. **Round-clock cadence** → **`window_log.json` gains optional top-level `cadence_hint`** (free-form: `"daily"`, `"6h"`), set by ops config, purely informative — the wargame may price upkeep in wall-time from it but must tolerate its absence and its violation (windows are ops-scheduled reality, the hint is intent).
4. **Receipt push vs pull** → **pull is enough**: receipts are read at server start / round boundaries, not in tight loops; directory listing over `applied/`+`failed/` filtered by `receipt.round` covers it. A plugin-side convenience wrapper is the plugin's own business.
