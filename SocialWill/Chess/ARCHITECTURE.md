# Chess — Wargame Layer Architecture Reference

> **Status: ✅ v0.1.4 (2026-07-09) — three adversarial rounds + one agent round CLOSED (108 findings adjudicated, zero rebuttals outstanding; rulings D1–D51; `docs/REVISION_LOG.md` §A/§B/§C/§D). Implementation-ready: start from `docs/HANDOFF.md` at P0 (external-surface spikes).**
> Chess is the **wargame/faction layer** of SocialWill. It consumes the frozen contract in
> `../Picasso/docs/wargame_interface.md` (normative, round-7 signed off) and the recorder-plugin
> conventions in `../Picasso/docs/player_activity_pipeline.md`. Nothing in this repo re-specifies
> those contracts; where they apply, this document cites them.
>
> Product-owner decisions this document encodes (recorded in `docs/REVISION_LOG.md` §0):
> **D1** Chess is a self-authored NeoForge mod (MC 1.21.1, NeoForge 21.1.x, Java 21) — all
> simulation server-side, the client is a render-only map terminal. **D2** Functional buildings are
> a *core block* plus a physical structure reachable by two paths (Picasso work-order
> materialization, or player self-build + registration). **D3** Loaded-chunk entity combat writes
> casualties back into counters; the abstract layer never overrules loaded reality. **D4** Docs are
> English, matching the Picasso corpus. **D5** (inherited, 联动待办 L4, user-ruled 2026-06-28) the
> infection mods are being rewritten into *driven engines* — autonomous hive AI off by default,
> four API families (infest / spawn / structure / raid) exposed for Chess to call. **D6** Infected
> sides are instances of a general **AI faction** concept — factions computed by the system
> itself, merely *natively hostile* in the infected case. The binding constraint on AI factions
> is **low performance cost: simple rule-driven behavior, no heavy AI decision-making**.
> Checkpoint-1 rulings **D7–D15** (three blocs & their currencies, resource taxonomy,
> occupation cores, block-accurate map via Xaero import, strain-mod tooling, no minimap, no
> entrenchment) are recorded in `docs/REVISION_LOG.md` §0.2 and integrated throughout.

### Changelog

| Version | Date | Notes |
|---|---|---|
| 0.1 draft 1 | 2026-07-08 | Initial architecture. Every subsystem 🚧. |
| 0.1 draft 2 | 2026-07-08 | Checkpoint-1 rulings D7–D15 integrated: occupation cores (presence ≠ control), blocs & three currencies, D7 resource taxonomy, no HUD minimap, block-accurate map base via Xaero import, no entrenchment, strain mods as callable tools. |
| 0.1 draft 3 | 2026-07-08 | Checkpoint-2 rulings D16–D19 (intra-bloc war unity gate, one-core-one-cell, currency/polity mapping, claim-capacity formula & manpower allocation); full doc set completed (all 8 subsystem docs draft 1). |
| 0.1 draft 4 | 2026-07-08 | Checkpoint-3 rulings D20–D23: item alignment source = ItemNameCatch workbook; cross-bloc war costs scale with inter-bloc favorability (dynamics TBD); treasury-paid interest + reserve requirement + crashable finance + bailout focuses; abstain=no, water impassable, republic 5-round terms. |
| 0.1 draft 5 | 2026-07-08 | Checkpoint-4 batch (D24–D35): variable footprints + stackable NPC counters; per-cell reserves; armor/medical mitigation + TACZ loadouts; refined core collapse; waypoint queues; strain playbooks (Spore hunt/expand/farm/assault, Sculk heart/ink-splash/hotspot/lurk, ephemeral wildlife/farmland counters); food spoilage; physical energy bridge; intel trading; window-lockdown airlock; favorability drivers. |
| **0.1 frozen** | 2026-07-08 | Checkpoints 5–6 (D36–D39): zhanghai density field + activity heat meter; window-time composition tuning; energy-bridge item ids pinned; unified favorability table + NPC-quest driver; lurk exception ack; wounded survive shatter. All open questions adjudicated or explicitly deferred. **Frozen for round-1 adversarial review.** |
| **0.1.1** | 2026-07-08 | **Round 1 closed.** A1–A30 adjudicated (29 accepted; A30→D41); spot-checks SC1–SC8 landed; rulings D40–D44: abstract renewable loot ("game, not simulation"), exclusivity-lock griefing floor, cell-integrity siege gate (combat dead = population dead; core mining = auto-declaration; leaver accountability), world boundary tool. Notable doctrine additions: terrain = non-secret old-world geography; DM-view window targeting; `env` pseudo-faction; AmbientField; round-key idempotence. |
| **0.1.2** | 2026-07-08 | **Round 2 (self-attack) closed**: B1–B18 (AI-override determinism duty; energy-bridge loop metering; benefit-side sabotage taint; heat freeze offline; wounded excluded from shatter roll; favorability pump caps; settling_round stamp; tie arbitration; template-drift freeze; mirror priority). **D45: no Picasso, no round.** `docs/HANDOFF.md` added — implementation-ready. |
| **0.1.3** | 2026-07-08 | **Round 3 closed (21/21 accepted)**: command pipeline defined (immediate apply + intra-tick journal, C1); net abuse limits; **jar-verified FTB reality** — gate-quest content-authoring rules, internal-class binding isolation, Chess-ledger-authoritative fallback (C9–C12); sabotage-taint victim exemption + preview; scar-order core carve-out; intruder-presence mirror floor; HANDOFF gains P0 external-surface spikes + P3 headless command mirror. |
| **0.1.4** | 2026-07-09 | **Post-close batch + agent round §D (R1–R21, 21/21 accepted).** D46–D51: overlap engagement model + stances + selection; mobility/tags/road layer; merge & split; currency final form (bullets/caps/tickets ×3 denominations 1:20:100, D51 UI iconography, assets delivered). §D fixes: territorial siege, no mirror battles, stack conscription, provisional conversion (the 4 HIGH); mint gating, recency road merge, PC9b save ordering, latch durability, env-veterancy exclusion, and 12 more seams. |

---

## 1. Philosophy & Mental Model

### The board and the world

Minecraft already simulates the world at block resolution — Chess must not compete with that.
Chess maintains a **second, abstract world**: a sparse chessboard whose cell is the chunk
(16×16 column). Counters, territory, economy, and infection live on the board and are settled by
arithmetic every 10 seconds, whether or not any chunk is loaded. The physical world is the board's
*rendering* — and, wherever players are present, its *court of appeal*.

The whole system is therefore a **consistency protocol between two worlds**:

1. **Board → world (manifestation).** A counter overlapping a loaded chunk manifests as real
   entities (soldiers, infected). A building's numbers hang on a real core block. Deep infection
   paints real infestation blocks. Long wars leave real scars (via Picasso work orders).
2. **World → board (writeback).** Entities killed by players reduce the counter that spawned
   them. Items deposited at an exchange terminal become faction resources. A player-built
   structure registers as a building. Loaded chunks are sensed, never simulated twice.

### Core doctrine (normative)

- **D-SERVER: The client computes nothing.** Every rule — settlement, combat, permissions,
  fog — runs on the server. The client receives fog-filtered snapshots and submits *commands*;
  it holds no authoritative state. This is simultaneously the anti-cheat story and the
  answer to client-performance risk (§10).
- **D-NOCHUNK: Chess never loads a chunk.** No force-loading, no ticket creation, no
  "peek" reads of unloaded terrain. The abstract layer exists precisely so the wargame can run
  at world scale without touching the chunk system. All world sensing happens opportunistically
  on chunks other actors (players) already loaded.
- **D-REALITY: Loaded reality wins.** Where the board and a loaded chunk disagree (a counter's
  mirrored entities are dead; an infested cell has been physically cleansed under observer-mode
  strain rules), the board reconciles toward the world, never the reverse. Abstract resolution
  is authoritative *only* where no one is looking.
- **D-CONTRACT: Data contracts, not code imports** (inherited from Picasso v05 §8). Chess talks
  to Picasso through `picasso_workorders/` + receipts + `window_log.json`; to the narrative layer
  (MiroFish) and any AI through `chess_log/` + `chess_state.json`; to the infection mods through
  a narrow adapter interface. No cross-layer Java dependencies except the strain-mod APIs that
  D5 explicitly creates.
- **D-REFUSAL: Every command can be refused, and every refusal says why.** Mirrors the
  work-order receipt doctrine (`wargame_interface.md` §6). The UI renders machine-readable
  rejection reasons; silence is never an outcome. Chess in turn *models refusal from Picasso* —
  a construction the window rejected must surface to the player, not vanish.
- **D-CHEAPBRAIN: AI factions think in rules, not plans** (D6). Any non-player faction —
  infected strains today, survivor camps or bandits later — is driven by a budgeted,
  data-driven rule list evaluated on a slow cadence: O(frontier) predicate checks, greedy
  budget spending, no search, no lookahead, no per-entity pathfinding. Infected factions are
  not special machinery; they are AI factions whose stance is `hostile_all` and whose limbs
  are infection-mod adapters (`docs/ai_factions_and_strains.md`).
- **D-LEDGER: Every abstract quantity declares its physical alignment.** The Alignment Ledger
  (`docs/integrations.md` §2) names, for each resource/number, its in-game counterpart, the
  conversion rate source, and the reconciliation trigger — or states honestly that it is purely
  abstract (stability, unity). "对齐" is a table, not a vibe.
- **D-BALANCE: Mechanisms are normative, numbers are config.** Every coefficient in this corpus
  (costs, rates, radii, caps) is a `PLACEHOLDER` default living in a data file; the product owner
  balances by editing JSON, never code. (Established user practice — 联动待办 备注.)

---

## 2. System Context — SocialWill's Three Layers

```
┌───────────────────────────────────────────────────────────────────────┐
│  SocialWill                                                           │
│                                                                       │
│  ┌─────────────┐   work orders / receipts    ┌──────────────────┐     │
│  │   Picasso   │◀────picasso_workorders/─────│      Chess       │     │
│  │ world layer │─────window_log.json────────▶│  wargame layer   │     │
│  │ (MCP server,│      (round clock)          │  (NeoForge mod,  │     │
│  │  maintenance│                             │   live server)   │     │
│  │  windows)   │──picasso_sync.json─────────▶│                  │     │
│  └──────┬──────┘   build_log/ (plugin──────▶ └────────┬─────────┘     │
│         │           duties shared, §3)                │               │
│         │ npc markers                                 │ chess_log/    │
│         ▼                                             │ chess_state   │
│  ┌────────────────────────────────────────────────────▼───────────┐   │
│  │              MiroFish — narrative layer (future)               │   │
│  │        reads journals, markers, chess_log; writes dialogue     │   │
│  └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
```

Execution model (normative, inherited): the server runs **live** while Chess plays in real time;
Picasso edits the save only during **maintenance windows**; **window entries are Chess's
construction round clock** (`wargame_interface.md` §1, §5). Chess additionally runs its own fast
clock — the 10-second settlement tick — which Picasso never sees.

The recorder plugin of `player_activity_pipeline.md` §2 and Chess are **one artifact**: that spec
already assigns the plugin "three duties — recorder, wargame live surface, fallback stamper".
Chess *is* the wargame live surface, so the Chess mod ships the recorder duty too (same JSONL
contract, unchanged).

---

## 3. In-Mod Architecture — Three Modules, Two Layers

The user-facing split is two layers: a **rules layer** (server) and an **interface layer**
(client). Internally the server side is further split so the simulation is testable headless.

```
┌────────────────────────── client (render terminal) ──────────────────────────┐
│  client/   Map Client — fullscreen strategic map · panels (no HUD, D10)      │
│            renders snapshots, submits commands, zero simulation              │
└──────────────────────────────────△────────────────────────────────────────────┘
                        net/  snapshots ▽ △ commands (fog-filtered, validated)
┌──────────────────────────────────▽────────────────────────────────────────────┐
│  shell/    Game Shell (NeoForge server glue)                                  │
│    mirror/   counter ⇄ entity manifestation & casualty writeback              │
│    world/    chunk-load sensing: terrain sampling, loot scan, core blocks     │
│    bridge/   picasso/ (work-order queue client) · ftb/ (teams+quests)         │
│              strains/ (sculkhorde · spore · zhanghai_mitu adapters)           │
│    log/      chess_log writer · chess_state snapshotter · recorder duty       │
│    persist/  SavedData codecs, save-version migration                         │
│    commands/ /chess admin commands                                            │
├────────────────────────────────────────────────────────────────────────────────┤
│  core/     Rules Core — pure Java, no Minecraft imports, deterministic        │
│    board   cells · sectors · fog        econ    resources · buildings        │
│    mil     counters · battles           gov     factions · polities · votes  │
│    ai      AI-faction rule drivers      sched   settlement pipeline · RNG    │
└────────────────────────────────────────────────────────────────────────────────┘
```

**Layering rule (normative, mirrors Picasso §2):** `core/` must not import Minecraft, NeoForge,
or shell classes — it is a headless simulation kernel driven entirely through interfaces
(`WorldSense`, `WorldAct`, `Clock`, `EventSink`) that `shell/` implements. `client/` may import
`net/` and read-only model DTOs only. All Minecraft registries, events, and I/O live in `shell/`.
This is what makes golden-replay tests (§7) and a future standalone balance simulator possible.

Identity: mod id **`swchess`**, package `net.socialwill.chess`. (`chess` alone is too generic a
namespace for registries; reviewable.)

---

## 4. Planned Source Layout

Status markers: ✅ implemented · 🚧 planned (spec exists, not built). Everything is 🚧 at v0.1.

```
src/main/java/net/socialwill/chess/
├── ChessMod.java            🚧 mod entry; config bootstrap; resilient startup (§12)
├── core/
│   ├── board/               🚧 Board, Cell, Sector, FogField        (docs/board_and_fog.md)
│   ├── mil/                 🚧 Counter, CounterTemplate, Battle,
│   │                           MovementPlanner, CombatResolver      (docs/counters_and_combat.md)
│   ├── econ/                🚧 Treasury, Building, ProductionGraph,
│   │                           ExchangeBook, ScavengeMission        (docs/economy_and_buildings.md)
│   ├── gov/                 🚧 Faction, Polity, PermissionMatrix,
│   │                           Ticket, Motion                       (docs/governance.md)
│   ├── ai/                  🚧 AiDriver, BehaviorPack, GrowthBudget,
│   │                           Strain (infection semantics)         (docs/ai_factions_and_strains.md)
│   ├── sched/               🚧 SettlementPipeline (§6), ChessRandom (§7)
│   └── api/                 🚧 WorldSense, WorldAct, Clock, EventSink, CommandBus
├── shell/
│   ├── mirror/              🚧 UnitMirror, MirrorBudget, WritebackReconciler
│   ├── world/               🚧 TerrainSampler, LootScanner, CoreBlockEntity (+ 8 building cores)
│   ├── bridge/picasso/      🚧 WorkOrderClient, ReceiptPoller, WindowClockReader
│   ├── bridge/ftb/          🚧 TeamMirror, FocusBindings (FTB Quests → effects)
│   ├── bridge/strains/      🚧 SculkHordeAdapter, SporeAdapter, ZhanghaiAdapter
│   ├── log/                 🚧 ChessLogWriter, StateSnapshotter, BuildLogRecorder
│   ├── persist/             🚧 ChessSavedData, migrations
│   └── commands/            🚧 /chess admin surface (§12.4)
├── net/                     🚧 packet codecs: Snapshot, Delta, Command, Notice
└── client/                  🚧 strategic map screen + panels (no HUD, D10)
                                (docs/map_interface.md)

src/main/resources/data/swchess/     ← all balance & content, datapack-reloadable (§11, §13)
├── counter_templates/*.json         🚧 militia, soldier, veteran, strain units…
├── buildings/*.json                 🚧 9 building specs (8 functional + faction_core)
├── polities.json                    🚧 dictatorship / republic / democratic_centralism
├── resources.json                   🚧 resource registry per D7 taxonomy (stocks: build
│                                       materials/raw materials/equipment/food · per-bloc
│                                       currencies: 子弹/瓶盖/粮票 (D49) · energy: power/fuel ·
│                                       people · gauges: stability/unity/political points)
├── exchange_table.json              🚧 resource ⇄ item alignment (D-LEDGER)
├── scavenge_blocks.json             🚧 generated from DDF可搜刮方块清单_283.txt
├── ai_factions/*.json               🚧 behavior packs: sculk_horde, spore, zhanghai_mitu
├── policies.json                    🚧 policy-center policy slots
└── focus_bindings.json              🚧 FTB quest id → faction effect
```

---

## 5. Core Domain Model

| Entity | Key | Essence | Spec |
|---|---|---|---|
| **Cell** | `dim:cx,cz` | One chunk column on the board: controller (faction/strain/none), terrain class, loot richness, fortification, contested flag | board_and_fog.md §2 |
| **Sector** | `sec_<seq>` | Faction-defined named group of cells; the jurisdiction unit for democratic centralism and the UI grouping unit | board_and_fog.md §5 |
| **Bloc** | polity id | Meta-camp of all factions sharing one polity: per-bloc base currency (子弹/瓶盖/粮票, D49), **shared focus-tree progress** across its factions (D7, D9), and pairwise **favorability 好感度** scaling cross-bloc war costs (D21) | governance.md · integrations.md |
| **Faction** | `f_<slug>` | Territory-owning organization: treasury, counters, buildings, relations. **Driver is either a polity (players) or a behavior pack (AI)** — one Faction machinery, two drivers | governance.md · ai_factions_and_strains.md |
| **Counter** | `cnt_<seq>` | Abstract military unit on 1..N cells: organization, strength, manpower; template-typed. **Presence ≠ control** (D12) | counters_and_combat.md §2 |
| **Occupation core** | `occ_<seq>` | The claim anchor (RTS-MCV analogue): a physical block placed by a player or built by a counter's occupy order; control of cells exists only chained to a core (or by adjacent land purchase). Claim capacity bounded by organization scale (D12) | counters_and_combat.md §5.6 · economy_and_buildings.md |
| **Battle** | `btl_<seq>` | Live engagement over a defender cell; participants, per-tick exchange, casualty report | counters_and_combat.md §5 |
| **Building** | `bld_<seq>` | Functional structure anchored on a physical core block in one cell; production/effects while powered & staffed | economy_and_buildings.md §4 |
| **Strain** | `sculk_horde` \| `spore` \| `zhanghai_mitu` | An **AI faction** with infection semantics added: per-cell intensity, paint-on-load, and a mod adapter (observer/driver mode); stance `hostile_all` | ai_factions_and_strains.md |
| **Command** | `cmd_<seq>` | A player intent from the UI (move, train, build, exchange, vote…); validated → applied, ticketed, or refused | map_interface.md §6 |
| **Ticket** | `tkt_<seq>` | A command awaiting approval under polity rules (e.g. dictatorship subordinate ops) | governance.md §5 |
| **Motion** | `mot_<seq>` | A vote in progress (overthrow, election, treaty) | governance.md §6 |
| **Work order** | `wg_<seq>_<kind>_<slug>` | *Picasso's* queue item — Chess only ever writes `pending/` and reads receipts | integrations.md §3 |
| **Event** | `evt_<tick>_<seq>` | One immutable line in `chess_log/` | event_log.md |

Board invariants (normative):

1. A cell has at most one controller and at most one garrisoning side's counters
   (stack limit `max_counters_per_cell`, PLACEHOLDER 3). Opposing forces on one cell is
   represented as a **Battle**, never as co-occupancy — under D46's overlap engagement
   model, "contested front cells" are exactly the cells a Battle owns.
2. Counters and buildings always belong to exactly one faction (player- or AI-driven);
   orphaned assets (faction dissolved) become `derelict` and decay (economy doc §7).
   Ephemeral counters (wildlife, infected farmland) belong to the reserved **`env`
   pseudo-faction** — cap-exempt, no behavior pack, fixed combat profile
   (ai_factions_and_strains.md §6.1; A16).
3. The board is sparse: cells exist only once *explored by someone*, *touched by a strain*,
   or *materialized by ephemeral-counter placement* (A16). Unexplored map costs nothing.
4. **Counter presence never flips control** (D12). A player faction controls a cell only
   through an occupation-core anchor or a land purchase chained to one; AI factions anchor
   differently (strain intensity/nodes, settlement camps — ai_factions_and_strains.md §1.2).

---

## 6. Time Model — Three Clocks

Chess inherits Picasso's clock doctrine (`player_activity_pipeline.md` §7) and adds the fast tick.

| Clock | Source | Granularity | Governs |
|---|---|---|---|
| **Settlement tick** | game time, every 200 game ticks (10 s) | fast | economy, battles, movement, strain growth, votes' timers |
| **Window round** | `picasso_workorders/window_log.json` (`round`, read-only, live-readable) | slow, ops-scheduled | construction & everything that changes the physical world through Picasso |
| **Wall clock** | server UTC (the *only* stamp in logs, per §7.1 one-wall-clock rule) | continuous | `chess_log/` timestamps, vote deadlines in real time |

The settlement tick counter (`tick`, monotonic, persisted) is Chess's logical time; every event
carries it. Missed ticks while the server is stopped are **not** simulated on resume — Chess time
is server-alive time (design choice: an offline server must not starve factions; reviewable).

**Normative settlement pipeline order** (one tick; subsystem docs detail each phase, this list is
the authority):

```
1. ingest      world writeback queue: entity deaths, core-block placements/breaks,
               exchange deposits, chunk-load sensing results, AI overrides (D-REALITY first).
               NOTE (C1): player commands are NOT in this queue — they applied immediately
               on receipt, journaled with intra-tick sequence (§7); the pipeline reads
               post-command state
2. power       generators burn fuel → power pool; buildings marked powered/unpowered
3. production  lumber, tools, scavenge progress, tavern effects
4. upkeep      counter & building upkeep from treasury; shortfall → org/condition decay
5. people      population growth/decline; manpower regeneration
6. gauges      stability & unity drivers integrate
7. battles     all live battles exchange one combat round (loaded-cell discount, §D3)
8. movement    counters advance their paths; new engagements open battles
9. ai          AI factions on decide cadence: rule packs evaluate, budgets spend,
               attacks/raids declared; strain adapters notified
10. construction/training progress in fast-clock items; round-gated items check
               window_log round (construction round semantics: integrations.md §3.4)
11. governance timers: tickets expire, motions tally/close
12. flush      events → chess_log; dirty deltas → subscribed clients; state marked
```

Phase order is load-bearing (e.g. ingest-before-battles is what makes player kills count in the
same tick; upkeep-before-battles is what makes starving armies fight weak) — reviewers should
attack it.

---

## 7. Determinism & Randomness (normative)

- All `core/` randomness flows from `ChessRandom`, seeded per
  `(world_seed, tick, subsystem, subject_id)` — same convention family as Picasso §6. Replaying
  a tick with identical inputs is bit-identical. **Exception (post-close review): round-end
  computation and all round-cadence rules seed on the round number instead —
  `(world_seed, round, subsystem, subject_id)` — so an aborted round-end (crash before
  SAFE TO STOP, ai_factions §6.4) re-runs bit-identically at whatever tick the restart
  reaches it; identical re-runs regenerate identical work-order ids, which is what makes the
  `order_id` dedup crash-safety argument hold.**
- Non-deterministic *world feedback* (entity deaths, chunk-load sensing, receipts, **and every
  narrative-AI override** — `bridge/ai_override` events written before the decide pass reads
  them; B1) is quarantined in the **ingest phase** and logged before it mutates state.
  **Player commands are NOT ingest-deferred (C1)**: they validate and apply **immediately on
  receipt** (UI latency ≤ RTT, never ≤ tick), each stamped with a monotonic **intra-tick
  sequence** and logged `orders/command_applied(tick, seq)` at its apply point — replay
  re-applies commands at their recorded (tick, seq) positions between settlement phases.
  "Quarantine" thus means *journaled inputs*, not *delayed inputs*. Either way, a transcript
  of `chess_log/` + the previous `chess_state.json` snapshot replays the abstract simulation
  exactly. This is the debugging and dispute-resolution story (who lost the battle
  and why).
- The entity layer itself (mirrored mob AI, gunfights) is expressly *not* deterministic and never
  needs to be: it enters the sim only as ingested casualty events.
- Golden tests: `core/` runs headless under JUnit with scripted `WorldSense`/`EventSink` fakes;
  CI keeps golden transcripts of canonical scenarios (siege, blackout, coup, sculk surge).

---

## 8. Persistence

- **Authoritative state**: one `ChessSavedData` per server (level-attached `SavedData`), NBT via
  codecs, saved with the world save cycle — crash consistency rides Minecraft's own save
  machinery. Contains: board, factions, counters, battles, buildings, strains, tickets, motions,
  tick counter, RNG cursors, pending work-order ledger.
- **Save-version field + forward-only migrations** (`persist/migrations`): loading a newer save
  on older code refuses loudly (mirrors H7 major-version honesty).
- **Exports (read-only mirrors for other layers, never read back):**
  - `<world>/chess_state.json` — full-state keyframe, atomic temp+rename (H11 protocol),
    written every `snapshot_period_ticks` (PLACEHOLDER 30 = 5 min) and at server stop.
  - `<world>/chess_log/` — the append-only event stream (event_log.md).
  - Consumers reconstruct state as *keyframe + event replay*; they never parse NBT.
- Work-order dedup ledger: submitted `order_id`s persist so a crash between "write pending/" and
  "record submitted" resolves by re-listing the queue directories (order_id is the dedup key per
  `wargame_interface.md` §2 — resubmission after crash is safe by contract).

---

## 9. Networking Summary

Full protocol in `map_interface.md` §5–§6. Shape:

- **Subscribe/snapshot/delta.** Client subscribes to a viewport; server sends a fog-filtered
  snapshot then deltas. Fog filtering is server-side — a client never receives data its faction
  cannot see (anti-cheat by construction, not by obfuscation).
- **Commands upstream.** Every UI interaction is a `Command` packet; server validates against
  permission matrix + resources + game state; responds `applied` / `ticketed(tkt_id)` /
  `refused(reason_code)` (D-REFUSAL).
- **Notices.** Push channel for approval requests, vote calls, battle alerts, receipt outcomes.
- Budgets: initial strategic snapshot ≤ 256 KB (cell overlay; terrain tiles are local files,
  not network traffic); steady-state ≤ ~4 KB/s per client (all PLACEHOLDER, enforced by
  coalescing, §10).

---

## 10. Performance Budget (the D1 concern, answered)

Design-scale targets (PLACEHOLDER, config-reviewable): 20 concurrent players, 6 player factions,
3 AI factions (strains) × 500 infected cells, 10 000 explored cells, 3 000 owned cells,
300 counters, 30 live battles.

**Server** — settlement is O(counters + battles + active buildings + AI frontier), all sparse
in-memory arithmetic; AI factions decide on a slow cadence (`ai_decide_period_ticks`, ≥ 6 ticks)
with a fixed cheap-predicate vocabulary (D-CHEAPBRAIN), so their cost is a fraction of one
settlement pass; **budget ≤ 5 ms typical / 20 ms worst per 10-s tick** (spread across ticks
if a phase overruns: pipeline phases 2–11 may defer to next game tick; ingest and flush never
defer). No chunk loading (D-NOCHUNK), no block iteration outside opportunistic loaded-chunk
sensing, which is itself budgeted (`sense_budget_ms_per_tick`, PLACEHOLDER 2 ms, amortized
sampling — board_and_fog.md §4).

**Client** — there is **no HUD minimap** (D10 ruling: fullscreen strategic map only); nothing
runs while the map is closed. While open, the map composes two layers: a **block-accurate
terrain base** served from a pre-rendered local tile pyramid (one-time import of the owner's
already-generated Xaero world-map data, plus incremental server-side re-render of changed
bounds — map_interface.md), and the **cell overlay** (ownership/fog/counters) as a texture-atlas
quad batch. No live per-block world scanning ever happens for rendering — tiles are static
images until invalidated. Target < 0.5 ms/frame added while open.

**Mirror caps** — `max_mirrored_entities_per_cell` (PLACEHOLDER 24) and global
`max_mirrored_entities` (PLACEHOLDER 200): the abstract layer scales; the entity layer is
budgeted flavor + interaction surface, never 1:1 (counters_and_combat.md §6).

---

## 11. Configuration

Server config (`config/swchess-server.toml`), all PLACEHOLDER defaults:

| Key | Default | Notes |
|---|---|---|
| `settlement_period_ticks` | 200 | the 10-s tick |
| `snapshot_period_ticks` | 30 | chess_state.json cadence |
| `max_counters_per_cell` | 3 | stack limit |
| `max_mirrored_entities_per_cell` / `_global` | 24 / 200 | mirror budget |
| `sense_budget_ms_per_tick` | 2 | loaded-chunk sensing amortization |
| `loaded_damage_discount` | 0.25 | abstract combat damping in loaded cells (D3) |
| `chess_log_dir` | `<world>/chess_log` | one-way out |
| `workorders_dir` | `<world>/picasso_workorders` | must match Picasso §10 fixed layout |
| `strain_mode.<strain>` | `observer` | `observer` \| `driver` \| `off` (L4 staging) |
| `ai_decide_period_ticks` | 6 | AI-faction decision cadence (~1 min); D-CHEAPBRAIN |
| `max_ai_factions` | 16 | hard cap (D13) |
| `ftb_integration` | `true` | teams mirror + focus bindings |

Content & balance live in `data/swchess/` (datapack-reloadable, §4). The split rule: *ops knobs
in TOML, game content in datapack JSON* — reload of content never requires restart.

---

## 12. Safety, Failure & Degraded Modes

### 12.1 The command choke point
Every state mutation initiated by a player enters through one validator (permission matrix →
resource check → game-state check → apply-or-ticket-or-refuse), and every outcome is logged.
There is no second path; admin commands go through the same choke point flagged `actor: admin`.
(Same single-choke-point philosophy as Picasso §12.1.)

### 12.2 Crash recovery
SavedData rides the world save. On load: re-list work-order directories against the submitted
ledger (§8); mark in-flight battles/missions `resumed`; emit `system/server_resumed` event with
the gap duration (consumers see the hole in wall-clock time; tick time is contiguous by design §6).

### 12.3 Degraded modes (resilient startup — nothing optional may prevent boot)
| Missing | Behavior |
|---|---|
| Picasso queue dir absent / never serviced | construction path B & war-scar orders disabled with visible UI notice; path A (self-build) unaffected; round-gated items hold (rounds simply don't advance) |
| `window_log.json` absent | round clock = 0, same as above; Chess never fabricates rounds |
| FTB Quests/Teams absent | mirror + focus bindings off; factions fully functional standalone |
| A strain mod absent | that strain `off`; its cells thaw to neutral `derelict_infested` terrain flag |
| Datapack drift (counter/building template id gone on reload) | asset freezes as `invalid_template` — inert, admin-resolvable, never crashes or silently deletes (B15). Same rule for vanished resource ids, policy ids, loadout pools, and exchange rows (C18): stock/effects freeze under the dead id, visible in the admin dump |
| `chess_log` unwritable | **hard refusal to start** — the log is not optional; a wargame without an audit trail violates D-REFUSAL and the cross-layer contract |
| Windows simply not running (ops gap, weeks) | **accepted fiction with three exceptions (A23), sharpened by D45: no Picasso, no round** — the round clock is `window_log.json` and nothing else; Picasso-less maintenance restarts advance nothing (a restart is weather, not history). Quotas, spoilage, ephemeral re-rolls, and construction all wait. Exactly three recovery-critical items get a wall-clock fallback (`fallback_round_days`, PLACEHOLDER 3 real days = one round-equivalent): interest payment cycles, the bank-run exit condition, and scheduled elections — a republic must never be locked in crisis by an ops gap |

### 12.4 Admin surface
`/chess` commands (all logged `actor: admin`): pause/resume settlement, force cell owner, spawn/
kill counter, grant resources, dump state, replay-verify a tick range, rebuild snapshot;
**window airlock** — `/chess window lockdown|open` (D35: lockdown kicks non-admins, bars
joins, runs round-end computation; ai_factions_and_strains.md §6.4) and strain direction —
`/chess strain assault <strain> <bounds> <intensity>`, node resurrect/reset (D31);
**world boundary** — `/chess boundary` paint/edit (D43: one outline binds board
materialization, AI reach, and player physical movement — board_and_fog.md §1.1). Admin
commands are mirrored by an external API surface so other SocialWill systems can invoke them.
**API trust boundary (A28)**: the external surface binds loopback-only (or a unix socket) and
requires an ops token from server config; every call is logged `actor: admin` with its source.
Same two-writer spirit as `wargame_interface.md` §7.

---

## 13. Extension Points

| To add… | Do this | Code changes |
|---|---|---|
| a counter type | drop JSON in `counter_templates/` | none |
| a building type | JSON in `buildings/` + core block model/texture | block registration only |
| a resource | row in `resources.json` + exchange rows | none |
| a policy | row in `policies.json` | none |
| a polity | row in `polities.json` (permission preset + founding rules) | none |
| an exchange rate | row in `exchange_table.json` | none |
| a 国策 hook | row in `focus_bindings.json` (quest id → effects) | none |
| an AI faction | behavior pack JSON in `ai_factions/` (rules from the fixed predicate/action vocabulary) | none |
| an infection strain | behavior pack + an adapter class implementing `StrainAdapter` for its mod | adapter class |
| KubeJS scripting | 🚧 extension: publish CommandBus + EventSink to KubeJS bindings | binding shim |
| narrative hooks | consume `chess_log/` (event_log.md §6); no Chess changes | none |

---

## 14. Document Set & Reading Order

| Document | Covers | Status |
|---|---|---|
| `ARCHITECTURE.md` | this file — doctrine, layering, time, persistence, budgets | 🚧 draft 1 |
| `docs/counters_and_combat.md` | counter model, movement, battle math, entity mirror & writeback, occupation | 🚧 draft 1 |
| `docs/ai_factions_and_strains.md` | AI-faction rule framework (D-CHEAPBRAIN); infection strains; sculkhorde/spore/zhanghai adapters; observer→driver staging (L4) | 🚧 draft 1 |
| `docs/board_and_fog.md` | cells, terrain attributes, sensing, sectors, fog of war | 🚧 draft 1 |
| `docs/economy_and_buildings.md` | resources, manpower model, the 9 buildings, scavenging, exchange, policies | 🚧 draft 1 |
| `docs/governance.md` | blocs, faction lifecycle, three polities, tickets, motions, diplomacy | 🚧 draft 1 |
| `docs/map_interface.md` | fullscreen strategic map, tile pyramid (Xaero import), net protocol, perf | 🚧 draft 1 |
| `docs/event_log.md` | `chess_log/` envelope, topics, file discipline, consumers, queries | 🚧 draft 1 |
| `docs/integrations.md` | Alignment Ledger; Picasso bridge duties; FTB quests/teams; cross-layer asks | 🚧 draft 1 |
| `docs/REVISION_LOG.md` | decision record (rulings D1–D45; review rounds §A/§B) | living — the tiebreaker |
| `docs/HANDOFF.md` | implementation kickoff: authority map, build order P1–P9, trap checklists, fixture, do-not-build list | handoff |
| `../Picasso/docs/wargame_interface.md` | the frozen Chess↔Picasso contract | **normative, external** |
| `../Picasso/docs/player_activity_pipeline.md` | recorder plugin contract + clock doctrine | **normative, external** |

**Reviewer/implementer reading order:** this file → wargame_interface.md (external, frozen) →
counters_and_combat → ai_factions_and_strains → economy_and_buildings → governance →
board_and_fog → map_interface → event_log → integrations.

---

## 15. Open Questions (v0.1 — for round 1 review)

1. ~~Offline-time policy~~ **Resolved (D15)**: rounds advance with maintenance windows
   ("每次维护结束之后开服时" is the round boundary — per-round withdrawal quotas reset there);
   fast-clock items freeze while the server is down.
2. ~~Footprint asymmetry~~ **Resolved (D24)**: footprints vary by type/scale; multi-cell
   counters are static while multi-cell; stackable NPC counters added.
3. ~~Population mirror~~ **Resolved (D25)**: decorative interactive CNPC residents, default-off,
   narrative-system-supplied, density config.
4. ~~Mod id~~ **Resolved (checkpoint 4)**: `swchess` / `net.socialwill.chess` confirmed.
5. ~~Xaero end-state~~ **Resolved (D10)**: Chess ships no minimap; the fullscreen map's terrain
   base imports the owner's pre-generated Xaero world-map data. Whether the Xaero mods stay in
   the pack afterwards is a modpack decision, out of scope.
6. ~~AI-faction generality scope~~ **Resolved (D13)**: v1 ships three archetypes — infected
   strains, wandering survivor bands, small settlement survivors; ≤ 16 AI factions total.
