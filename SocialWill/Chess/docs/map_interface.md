# Map Interface — Client Layer Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> Encodes **D1** (client = render terminal, zero simulation) and **D10** (no HUD minimap —
> fullscreen strategic map only; terrain base must be block-accurate for navigation; the
> performance answer is importing the owner's pre-generated Xaero world-map data).
> The map replaces the Xaero map *function*: it is how players navigate, and it is the
> wargame's entire operating surface (HOI4-style: the map **is** the interface).

---

## 1. Doctrine

- The client holds a **view-model**, never game state: fog-filtered snapshots + deltas in,
  commands out. If the server is silent, the map is stale, and stale is visibly marked
  (last-sync indicator) rather than extrapolated.
- Everything renders **only while the map screen is open** (default key `M`). Closed map =
  zero client cost. There is no HUD element at all in v1.
- Every interactive element routes through the Command packet and displays refusals with
  their reason (D-REFUSAL) — the UI never pre-filters an action the server would explain
  better ("insufficient build_materials (need 40, have 12)" beats a greyed button).

## 2. The Terrain Base Layer (D10)

Block-accurate top-down imagery, served as a **tile pyramid** (mip-chain: 1 px/block → 1 px/
2 blocks → … like every slippy map):

1. **Import (primary source)**: an offline converter tool ingests the owner's already-generated
   Xaero world-map data and emits `chess_tiles/` (PNG tiles + index). Ships with the modpack or
   is server-distributed on first join. One-time cost; zero runtime render cost.
2. **Incremental re-render (staleness repair)**: the world *changes* — Picasso restyles whole
   districts between rounds, battles scar cells, players build. Triggers for server-side tile
   patching, budgeted like all sensing:
   - Picasso **receipts** (bounds arrive in the receipt — re-render exactly those columns
     at next natural load, or immediately if the window's save is the current save);
   - war-scar / construction events (Chess knows the bounds it asked for);
   - loaded-chunk diff sampling (low priority, catches player building).
   The server renders top-down column colors (vanilla map-color extraction — cheap, no client
   involvement) into tile patches and pushes them to clients lazily (tile-version vector;
   clients fetch stale tiles on view). **Patch distribution is fog-gated per faction (A4)**:
   a client receives a tile patch only for cells its faction has *seen while the change was
   current* — i.e. patches propagate on (re-)exploration; unexplored regions keep the shipped
   old-world tile, and `explored` regions keep the version last seen (board_and_fog §6).
   The shipped base itself is declared **non-secret old-world geography** — a modified client
   revealing it reveals nothing living.
3. **Never-rendered areas** (beyond the import's coverage) draw as parchment/void — the fog
   system (§4) usually hides them anyway.

The pyramid lives client-side on disk (LRU-capped cache); tiles are static textures — no
per-frame work beyond quad drawing.

## 3. Overlay Layers & View Modes

Composited above the terrain base, all sourced from the fog-filtered snapshot:

| Mode | Shows |
|---|---|
| **Political** (default) | cell ownership tint (faction colors), cores, buildings, sectors, purchase-preview |
| **Military** | counters (side/class/strength band), battles (animated front), orders/paths, war relations |
| **Economy** | own-faction production icons, loot richness, scavenge missions, power status |
| **Infection** | strain territory by intensity ramp, nodes, raid alerts |

Fog is not a mode — it applies always (`unknown` = old-world terrain tile only, zero living
data, no grid — A4 ruling, board_and_fog §6; `explored` = desaturated memory snapshot +
last-sync stamp; `visible` = live). Cell grid lines fade in past a zoom
threshold. Tooltips on hover show the cell record (fog-appropriate). Selection model: click
cell / click counter / drag-select own counters.

## 4. Interactions (command surface)

- **Counters** (control scheme, consolidated per owner Q): **click-select** (cell or
  counter) · **box-select** own counters (drag) · **right-click context command** (neutral
  target = move, hostile = attack) · **shift-click waypoint queues** (D30) · occupy-build
  button (D12) · disband (C5 legality) · **stance toggle** (`hold_ground`/`free_engage`,
  D46 — v1, semantics in counters §2.5; left-click never issues commands, footprint is the
  hitbox). v1.5 extension (open, not built): control groups (Ctrl+number).
- **Territory**: purchase-paint mode (click adjacent cells, sees price curve live, confirms
  batch — one command per cell, batched packet).
- **Buildings**: path-B application wizard (pick building → eligible-cell highlight → cost & K
  rounds preview → apply); path-A registration happens in-world (core block placement), the map
  just reflects it.
- **Currency iconography (D51)**: treasury amounts render with the faction's bloc currency
  **symbol = its ×100-denomination icon** (dictatorship: sniper round · republic: cola cap ·
  democratic centralism: 粮票三号) — three blocs, three instantly-distinguishable symbols.
  Players without a faction, and the founding wizard before the polity is chosen, show a
  neutral **"货币未确定 / currency undetermined"** placeholder glyph — the UI never guesses
  a currency it doesn't have.
- **Panels** (right-side dock): faction dashboard (treasury, gauges, manpower ledger) ·
  approvals inbox (tickets — approve/deny inline) · motions (vote) · members & permissions
  (leader views per polity) · exchange & quotas · policies · **log browser** (faction-visible
  `chess_log` events, filterable by topic/time — the in-game consumer of event_log.md §6) ·
  notices feed.
- **Waypoints**: personal + faction waypoints (create/name/color on map; nav aid replacing
  Xaero's — rendered on the map only, since there is no HUD; a small "compass to active
  waypoint" toast when walking is an accepted v1.5 nicety if navigation without minimap proves
  painful — open Q1).

## 5. Network Protocol

- `Subscribe(viewport, mode)` → `SnapshotChunk*` (cell headers for viewport ∩ explored,
  RLE-packed, ≤ 256 KB worst case) → `Delta*` (dirty cells/counters/battles per settlement
  flush; typical ≤ 2 KB/tick) + `Notice*` (push: tickets, votes, battle alerts, receipts).
- `Command(cmd_id, kind, params)` → `CommandResult(applied | ticketed(tkt) | refused(reason,
  data))`. Idempotency: `cmd_id` dedup window server-side (resends after lag are safe).
- Tile sync: `TileVersions(region)` → client requests stale tiles → `TilePatch*` (bulk,
  low-priority channel, never blocks deltas).
- Viewport moves re-subscribe incrementally (server keeps per-client viewport to scope deltas).
- **`cmd_id` contract (C2)**: per-player namespace (multi-login safe); server keeps a sliding
  dedup window (`cmd_dedup_window` 256 ids / 5 min); a duplicate returns the **cached original
  result** (never re-executes, never `refused(duplicate)`); a reused id with a different
  payload hash is rejected (`cmd_id_conflict`).
- **Abuse limits (C3) — `net_limits` config family, over-limit = refusal with reason, all
  PLACEHOLDER**: per-client command rate (`cmds_per_sec` 8, burst 20) · viewport size cap
  (`viewport_max_cells` 16384) · resubscribe/mode-switch churn cap · **tile requests validated
  against the requester's per-faction entitlement version vector** (A4 policy enforced at the
  request path — an unentitled request is refused, not served) · log-browser queries paginated
  (`log_page_max` 100) + rate-capped · pending tickets per member capped
  (`tickets_per_member_max` 8, anti approver-flooding). §9/§10 budgets are targets; these are
  enforcement.
- Command apply timing per ARCH §7 (C1): immediate on receipt, intra-tick sequenced, journaled
  — `CommandResult` is honest, not optimistic.

## 6. Client Performance Budget (D1 answered, again)

| Item | Budget |
|---|---|
| map closed | 0 ms/frame, 0 allocations (no HUD) |
| map open, steady | < 0.5 ms/frame: tile quads (cached GPU textures) + one overlay quad batch + text |
| delta apply | < 0.2 ms per settlement flush (10 s cadence) |
| tile cache | disk LRU 256 MB / RAM 64 MB (PLACEHOLDER) |
| snapshot memory | viewport cells × ~32 B + counters — sub-MB |

No world access, no chunk meshes, no entity rendering — the map never competes with the game
renderer for anything but a texture bind.

## 7. Server-Side UI Support

Per-client viewport registry (delta scoping) · fog filter at serialization (board_and_fog §6) ·
strength-band quantization · notice fan-out (faction-scoped) · tile render queue (budgeted,
§2.2). All inside `shell/`; `net/` codecs are dumb.

## 8. Open Questions (v0.1)

1. ~~Navigation without HUD~~ **Resolved (checkpoint 4)**: v1.5 compass toast pre-committed.
2. ~~Tile distribution~~ **Resolved (checkpoint 4)**: ship the imported base in the modpack;
   serve patches in-session.
3. ~~Admin map~~ **Resolved (checkpoint 4)**: v1, admin-permission gated (fog filter bypass
   flag). The admin panel also hosts strain direction (assault/node controls — ARCH §12.4)
   and the **world-boundary paint tool** (D43 — draw/edit the outline directly on the map;
   board_and_fog.md §1.1).
4. **Multi-dimension**: overworld only v1 (matches board scope). Nether wargame is explicitly
   out of scope until the board supports it.
5. ~~Xaero format risk~~ **Resolved (checkpoint 4)**: accepted (offline converter, pinned
   version, slow re-render fallback).
