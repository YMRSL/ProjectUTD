# Integrations & the Alignment Ledger — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> Everything where Chess touches another system: the Alignment Ledger (D-LEDGER), the
> Picasso bridge (consuming the frozen `wargame_interface.md` contract), FTB Quests/Teams
> (国策 + membership), the recorder duty, the Xaero import, and the consolidated list of
> cross-layer asks.

---

## 1. Boundary Inventory

| Artifact | Writer | Readers | Contract |
|---|---|---|---|
| `picasso_workorders/pending/` | **Chess** | Picasso | wargame_interface §2–§3 (frozen) |
| `picasso_workorders/applied|failed/` (receipts) | Picasso | **Chess** | ibid. §2.2, §6 |
| `picasso_workorders/window_log.json` | Picasso | **Chess** (live, read-only) | ibid. §5 |
| `<world>/picasso_sync.json` | sync infra | Picasso (Chess may peek, informative) | pipeline §7.2 |
| `build_log/` | **Chess** (recorder duty) | Picasso | pipeline §2 (unchanged) |
| `chess_log/` + `chess_state.json` | **Chess** | MiroFish, Picasso agents, AI, ops | event_log.md |
| Picasso structure registry | Picasso | **Chess** (optional, read-only enrichment) | §8 ask 3 |
| FTB Teams/Quests state | FTB mods | **Chess** via API (+ writes via API only) | §4 |
| Strain mod APIs | strain mods (post-L4) | **Chess** calls | ai_factions_and_strains.md §5 |

No code imports across the Picasso/MiroFish boundaries — files only (D-CONTRACT). FTB and
strain mods are same-process Java APIs by necessity; both are wrapped behind `shell/bridge/`
interfaces so `core/` never sees them.

---

## 2. The Alignment Ledger (D-LEDGER, normative)

Every abstract quantity names its physical counterpart, conversion source, and reconciliation
trigger — or declares itself honestly abstract:

| Abstract | Physical counterpart | Conversion | Reconciliation trigger |
|---|---|---|---|
| `build_materials` | planks/stone/bricks families | `exchange_table.json` | exchange transactions |
| `raw_materials` | logs, ores, scrap | ibid. | ibid. |
| `equipment` | tools, weapons (incl. superbwarfare/TACZ items) — item lists per the D20 workbook sheets 枪械/装备/可制作近战武器 | ibid. | ibid.; also counter training/mirror loadouts |
| `food` | FarmersDelight / kaleidoscope-family foods, bread | ibid. | ibid. |
| `coal` / `liquid_fuel` | coal-family items · ethanol family (`createdieselgenerators:ethanol_bucket`) via `create:fluid_tank` interfaces | exchange table + econ §3.1 bridge | exchange transactions + bridge auto-draw/overflow-credit (D38; row split per A10) |
| `power` | CNA terminal per powered cell (`create_new_age:electrical_connector`, first-placed, bidirectional) + designated auto-fed consumers (`superbwarfare:charging_station`, …) | econ §3.1 bridge rates | settlement-cadence bridge service (D33/D38; row fixed per A10) |
| `bullets` / `bottle_caps` / `grain_tickets` | bullets = TACZ ammo trio `tacz_unidict:{pistol,rifle,sniper}` via `tacz:ammo` at face 1/20/100 (D49/D50, R16); caps/tickets = registered items with owner-delivered textures (§8 ask 5) | face value | exchange/trade transactions |
| `manpower` (in counters) | mirrored soldier entities | `manpower_per_entity` (5) | spawn on load / casualty writeback (D3) |
| `population` | resident CNPCs (D25 optional feature, narrative-supplied; **load-bearing when enabled, D42**: kills deduct owner **population** (SC1) + cell integrity and trigger war legality) | `resident_density` config (channel width only, SC3a) | resident-kill writeback (governance §7); feature off → population abstract, garrison mirrors carry the siege channel |
| `stability` · `unity` · `political_points` | **none — declared abstract** | — | — |
| buildings | core block + structure | building JSON (validation footprint / Picasso template) | core-block audit on load (board_and_fog §4) |
| territory | occupation-core blocks (cell claims); purchased cells are map-only | D12/D17 | core audit on load; core destroyed → de-control cascade |
| infection intensity | mod infestation state | adapter paint/sense | chunk load (paint/purge/census) |
| war damage | Picasso `degrade`/`composite_event` results | scar thresholds (combat §8) | receipts |

The scavenge economy's alignment source is `DDF可搜刮方块清单_283.txt` →
`scavenge_blocks.json` (econ §6) — abstract loot and hand-lootable blocks are one list.

---

## 3. The Picasso Bridge (Chess-side duties under the frozen contract)

`shell/bridge/picasso/` implements the *wargame half* of `wargame_interface.md`. Duties, all
normative there, restated as implementation obligations:

1. **Submission**: write only into `pending/`, atomic temp+rename, ids
   `wg_<seq>_<kind>_<slug>` persisted in the submitted-ledger (ARCH §8); `on_behalf_of`
   mandatory (`{layer:"wargame", faction, actor}`); set `priority` (convention, B18:
   construction 50 · war scars 60 · restyle 70 — lower runs earlier per contract),
   `expires_after_round` (default: +3 rounds), `requires` for multi-order chains.
2. **Receipt loop**: poll `applied/`+`failed/` at server start and each settlement tick
   (cheap directory listing filtered by round); every receipt drives a state machine
   (construction → active/failed-with-notice; scar → verified; restyle → flavor applied) and
   logs `bridge/receipt_received`. **Refusal is modeled**: `failed`/`rejected`/`partial`
   receipts surface to the requesting player as notices with the receipt's reasons — never
   silently swallowed, never auto-retried (re-submission is a fresh player-visible decision).
3. **Round clock**: read `window_log.json` live; `round` is the construction clock (D15);
   `save_synced_from_live_at` is informative context (events after it were invisible to that
   window). Chess never writes this file and never fabricates rounds when it's absent
   (degraded mode, ARCH §12.3).
4. **Construction rounds (§3.4 semantics)**: path-B application at round R with K construction
   rounds pays upkeep each round R+1…R+K (miss a payment → paused, notice); at R+K Chess
   submits the `construct` order; the building activates on the `applied` receipt (typically
   round R+K+1). UI shows "K rounds remaining / awaiting window / built".
5. **Order vocabulary use**: `construct` (buildings, path B) · `degrade`/`composite_event`
   (war scars, combat §8; mandatory `reason` carries battle id) · `restyle` (occupation
   flavor, long holds) · `mark` (NPC markers: tavern keeper, faction flavor NPCs — feeds the
   narrative layer) · `demolish` (faction-ordered teardown). **Never** a raw block write; if
   Chess needs vocabulary Picasso lacks, the template/bundle gets authored first (contract
   rule, §8 ask 1).
6. **Never-do list**: never write outside `pending/`; never scan the world to infer order
   outcomes (receipts are the only feedback); never assume an order applied; never submit
   targeting by `activity_site_id` (bounds only — J3 ruling).

## 4. FTB Integration — `shell/bridge/ftb/`

### 4.1 Teams (membership substrate)

Chess is **authoritative** for membership; each faction mirrors to one FTB party team (created
at founding). Joins/leaves/kicks propagate Chess → FTB via API. Manual FTB-side edits are
detected (team events) and **corrected back**, logging `governance/ftb_drift_corrected` — one
source of truth, visibly enforced. FTB absent → degraded mode (ARCH §12.3).

### 4.2 Quests as focus trees (D9) — integrate, don't fork

Ruling context: the owner asked fork-vs-integrate. Recommendation encoded here: **use FTB
Quests as the runtime via its API; never copy its code** (maintenance and upgrade cost of a
fork dwarfs the integration; the pack already ships it; its editor is the content pipeline).

**Jar-verified reality (round 3, C9/C10 — class-checked against the shipped
`ftb-quests-neoforge-2101.1.25` / `ftb-teams-neoforge-2101.1.10`; runtime spike still owed,
HANDOFF P0):**

- **The quest file is a global singleton** — no per-team chapter attachment exists. All three
  bloc trees are global content; only *progress* is per-team. Bloc visibility is therefore
  **authored in**: every bloc tree hangs off a per-bloc **gate quest** whose completion is a
  Chess-controlled `CustomTask`; Chess auto-completes the right bloc's gate per team at
  founding, dependency visibility hides the other trees. **Content-authoring rule (the owner
  must know this before writing the trees)**: every bloc mainline root depends on its bloc
  gate; every exclusive-branch member depends on its own Chess-controlled branch-gate task.
- **No lock/hide API** (public api ≈ 9 classes). Implementable mechanics, both jar-present:
  `CustomTask`/`CustomTaskEvent` for all gates; the **internal** `ChangeProgress`/`TeamData`
  machinery (behind `/ftbquests change_progress complete|reset`) for bloc-wide replication
  and B10 reverts. Internal-binding risk accepted and managed: isolated in
  `shell/bridge/ftb/`, pinned to 2101.1.25, breakage degrades per-capability, never crashes.
- **Fallback doctrine (C12, pre-agreed so a spike failure never reopens D9)**: the
  **Chess-side progress ledger is the authority** — focus effects apply from Chess's own
  ledger regardless of quest-UI state; FTB is presentation + content editor; divergence is
  logged (`bridge/ftb_divergence`) and cosmetic by definition.
- **Per-capability degradation (C11)** replaces the all-or-nothing flag: teams-mirror, gate
  completion, replication, and reset degrade independently (reset unavailable → exclusivity
  enforced Chess-side, stale quest UI accepted + logged). Teams-API semantics (programmatic
  party creation for an arbitrary founder, offline moves, party-of-one) are P0 spike items.

The binding layer supplies the four things quests alone don't do:

1. **Effects**: `data/swchess/focus_bindings.json` rows
   `{quest_id, effects: [unlock_building|unlock_counter|modifier|grant|capacity_bonus…]}` —
   on team-completion events, apply to that team's faction. Effects are Chess vocabulary;
   quests stay pure FTB content.
2. **Bloc-wide progress sync**: when any faction of bloc B completes quest Q, replicate Q's
   completion to every other bloc-B team via the FTB progress API (and to future factions at
   founding). This is how "所有同政体玩家共享国策进度" rides team-scoped FTB progress.
   Same-window ties on exclusive branches arbitrate by chess_log ingest order — tick, then
   event id; the loser's completion is reverted (internal reset machinery, above) with a
   notice (B10). **Focus effects apply only at the arbitration step, never directly on the
   FTB completion event (C17)** — a reverted loser has nothing to roll back.
3. **Branch exclusivity (first-completed-wins)**: `focus_bindings.json` declares exclusive
   groups; on first completion in a group (arbitrated per B10), the binding layer permanently
   **withholds the sibling branch-gate tasks** for the whole bloc — the C9/C10 gate mechanism
   above; there is no direct lock/hide API, and FTB dependencies alone can't express
   bloc-level exclusivity.
   **Griefing floor (D41)**: the bloc-wide lock triggers only when the completing faction has
   ≥ `focus_lock_min_members` (5) and age ≥ `focus_lock_min_age_rounds` (2); sub-threshold
   completions grant faction-local effects without locking siblings. Two clarifications
   (SC8): crossing the threshold later never locks retroactively (locks evaluate only at
   completion time); and sub-threshold local effects persist even if another faction later
   locks a sibling branch bloc-wide — earned effects are never revoked, the lock binds only
   future completions.
4. **Contribution scoring (D19)**: the completing players (task contributors per FTB's own
   progress attribution where available, else the completing team's online members —
   PLACEHOLDER policy, open Q2) accrue `contribution_score`, feeding claim capacity
   (econ §10).

Chess-state-gated quests ("control 20 cells to unlock this focus") are the one genuinely
missing primitive — v1.5 ships a **custom FTB task type** (`ChessStatTask`, small API-side
class) rather than any fork. The tree content itself (three bloc mainlines merging story +
tech) is authored in the FTB editor by the owner — content, not architecture.

## 5. Recorder Duty (build_log)

Chess ships the recorder half of `player_activity_pipeline.md` §2 unchanged: place/break JSONL,
atomic lines, daily rotation, `PICASSO_BUILD_LOG_DIR`. One mod, three duties (recorder ·
wargame live surface · fallback stamper if Phase 1.5 triggers) — as that spec anticipated.
No schema changes; Chess's own events go to `chess_log/`, never mixed into `build_log/`.

## 6. Strain Mod APIs

Pointer: ai_factions_and_strains.md §5. The L4 rewrite asks are consolidated in §8 (ask 6).

## 7. Xaero Import

Offline converter: Xaero world-map data (the owner's pre-run cache) → `chess_tiles/` pyramid
(map_interface.md §2). Runs at pack-build time, pinned to the pack's Xaero version; format
drift is tooling risk, not runtime risk (map Q5). Licensing: private-pack internal tooling
reading local files — acceptable; the converter is never distributed as a Xaero derivative.

## 8. Cross-Layer Asks (consolidated; the other side's pending-edits list)

| # | To | Ask |
|---|---|---|
| 1 | Picasso | Author **9 building templates** (8 functional + faction core), each embedding its core block at a declared anchor — required for path-B `construct` orders (vocabulary-first rule). Occupation cores need no template (single block, path A only) |
| 2 | Picasso | None for scars — `degrade`/`composite_event`/`restyle` vocabulary already suffices (confirmed against wargame_interface §3) |
| 3 | Picasso | Bless read-only Chess access to the structure registry file for urban-terrain enrichment (board_and_fog §2) — additive, read-only; needs a one-line ruling on their side |
| 4 | Picasso | Optional: `mark` templates for tavern keeper / faction flavor NPCs |
| 5 | Modpack | Register **six** currency items (D50): 3 bottle caps + 3 grain tickets, denominations 1/20/100 — **textures delivered and organized at `Chess/assets/currency/`** (see its README for file↔item↔face-value mapping); bullets need no registration (existing TACZ ammo trio `tacz_unidict:{pistol,rifle,sniper}` via `tacz:ammo` at 1/20/100) |
| 6 | L4 rewrite (sculkhorde/spore) | Stable Java API for the four families (infest/spawn/structure/raid) · autonomy kill-switch config · lifecycle events (node placed/destroyed, raid start/end) · node-generation emits bounds (auditability, ai_factions §8.1) · census query (count entities by type in chunk) · **cursor progress callbacks** (incremental infected-block count per region — the lurk mechanic's ≥200 threshold and cleansing detection must never require block scanning, ai_factions §6.3) · region-scoped cursor rate control (lurk ~1 block/5 s vs ink-splash growth) |
| 7 | MiroFish (future) | Consume `chess_log/` per event_log.md §6; honor `visibility`; faction ids shared namespace |
| 8 | CNPC script layer | NPC **faction affiliation** field + quest-completion records countable by Chess (D39a — feeds the AI-side favorability driver and the D24 special-NPC counters' memory) |
| 9 | Picasso (`player_activity_pipeline.md` §2) | Stale line "Fabric mod or Paper plugin" → the recorder is the Chess **NeoForge mod** (one artifact, three duties). Cross-layer erratum for their §8 pending-edits list (A29) |
| 10 | Picasso | **Road product (D47)**: publish a cell-resolution road-class layer + connectivity graph as a read-only artifact beside the structure registry (same channel family as ask 3 — a read product, not a work order). Feeds Chess's `road` cell layer for vehicle mobility |
| 12 | Modpack | **Gate the mint (R9)**: review/rebalance TACZ recipes for the three currency calibers toward the 1:20:100 crafting-cost ratio, or disable them server-side — the dictatorship's monetary base is otherwise player-printable |
| 11 | Sable (`sable-schematic-api`) | **v0.2 spike (D47)**: capability check for `physical_unique` counters — contraption identity tracking, position readback, destruction events. Pre-agreed fallback: airships stay abstract counters with spawned vehicle mirrors |

## 9. Open Questions (v0.1)

1. ~~Exchange-item lists~~ **Resolved (D20)**: seeds generate from the planning workbook
   `策划案以及文档相关/ItemNameCatch分类汇总_合成设计方案_v1.xlsx` (category sheets → resource
   mapping, econ §7.1) + the DDF-283 list for scavenge blocks; owner hand-balances the output.
2. ~~Contribution attribution~~ **Resolved (checkpoint 4)**: v1 accepts the online-members
   policy; v1.5 tightens to reward-claimers.
3. ~~Registry read cadence~~ **Resolved (checkpoint 4)**: re-read after each round increment.
