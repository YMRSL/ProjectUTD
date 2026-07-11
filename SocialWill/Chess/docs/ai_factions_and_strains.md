# AI Factions & Infection Strains — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> Draft 3 adds §6 Strain Playbooks (D31/D35): the owner's Spore (hunt/expand/farm/assault) and
> Sculk (single physical heart, ink-splash/hotspot/lurk) designs normalized into the
> behavior-pack framework, ephemeral counters, and the window-lockdown airlock.
> Draft 2 integrates checkpoint-1 rulings **D11** (strain mods are being source-modified into
> callable tool libraries; live infest/structure calls answer only to Chess, not to the
> wargame_interface boundary — that boundary governs Picasso writes) and **D13** (v1 AI-faction
> archetypes: infected strains, wandering survivor bands, small settlement survivors; ≤ 16 AI
> factions total).
> Encodes **D6** (infected sides are instances of a general AI-faction concept; the binding
> constraint is low performance cost — simple rule logic, no heavy AI decision-making) and
> **D5** (联动待办 L4: sculkhorde/spore are being rewritten into driven engines exposing
> infest / spawn / structure / raid APIs, autonomous hive AI off by default).
>
> Three parts: **(a)** the AI-faction driver framework (§1–§3, general); **(b)** infection
> semantics layered on top (§4); **(c)** the three strain adapters (§5). The framework is
> deliberately larger than its v1 payload — v1 ships only the three infected strains, but
> nothing in §1–§3 assumes hostility or infection.

---

## 1. The AI Faction — One Faction Machinery, a Second Driver

An AI faction **is a `Faction`** (ARCHITECTURE §5): it owns cells, garrisons counters, and its
assets flow through the same battle, occupation, and event machinery as player factions. What it
lacks is a polity — where a player faction is driven by commands filtered through governance,
an AI faction is driven by a **behavior pack**: a data-file rule list evaluated on a slow cadence.

What AI factions deliberately do **not** get (cost ceilings, normative):

| Not this | But this |
|---|---|
| pathfinding across the board | acts only on its **frontier** (cells adjacent to its territory) and its own counters |
| lookahead / planning / search | greedy: evaluate rules in priority order, spend budget until dry |
| per-entity micro decisions | entity behavior belongs to the mods (mob AI); Chess decides only *board* moves |
| reactions between settlements | thinks every `ai_decide_period_ticks` (default 6 ticks ≈ 1 min), acts through the normal pipeline |
| its own economy simulation | one scalar **budget** income (§2.2) instead of buildings/population/upkeep bookkeeping |

The cost envelope: one decision pass is O(rules × frontier-sample + counters), runs at 1/6th of
settlement cadence, and the frontier is sampled (`ai_frontier_sample`, PLACEHOLDER 64 cells max
per pass) — a 500-cell strain never scans 500 cells to decide. This is D-CHEAPBRAIN made
mechanical.

### 1.1 Stance & v1 archetypes (D13)

Each AI faction declares a stance: `hostile_all` (at war with everyone by definition, no
declaration needed, no diplomacy surface) · `neutral` · `defensive` (retaliates, never expands
into claimed cells). v1 ships three archetypes, ≤ `max_ai_factions` (16) total:

| Archetype | Stance | Territory | Sketch |
|---|---|---|---|
| **Infected strain** ×3 | `hostile_all` | infestation cells + nodes | §4–§5 |
| **Wandering survivor band** | `neutral` | none — counters only, `roam_step` behavior | trade/recruit interactions are a v0.2 extension; v1 they roam, defend themselves, and flee strong threats |
| **Small settlement survivors** | `defensive` | camp core + a few cells | a static micro-faction: garrison, slow growth, retaliates but never expands into claimed territory |

### 1.2 Territory anchors (D12 interplay)

The player-faction occupation-core rule (counters_and_combat.md §5.6) does not apply to AI
factions: strains anchor territory by **infestation intensity** (nodes are strongholds, not
prerequisites for control), settlements by their **camp core**, wanderers anchor nothing.
This is deliberate — cores are a *governance* mechanic (claim capacity, purchase chaining),
and AI factions have no governance.

---

## 2. Behavior Packs — `data/swchess/ai_factions/<id>.json`

### 2.1 Schema (v1)

```json
{
  "v": 1,
  "faction": "sculk_horde",
  "kind": "strain",
  "stance": "hostile_all",
  "decide_period_ticks": 6,
  "income": {"base": 2.0, "per_cell": 0.05, "per_node": 1.0, "cap": 60},
  "dormancy": {"below_budget": 5, "wake_on": ["territory_attacked", "node_attacked"]},
  "rules": [
    {"id": "defend_node",   "when": "node_under_attack",              "action": "reinforce_defense",      "priority": 100, "cost": 4, "cadence": "tick"},
    {"id": "counterattack", "when": "cell_lost_recently(12)",         "action": "attack_lost_cell",       "priority": 80,  "cost": 6},
    {"id": "deepen",        "when": "frontier_intensity_below(2)",    "action": "raise_intensity",        "priority": 50,  "cost": 2},
    {"id": "expand",        "when": "frontier_has_soft_neighbor",     "action": "expand_cheapest",        "priority": 40,  "cost": 3},
    {"id": "grow_node",     "when": "cells_per_node_above(40)",       "action": "place_node",             "priority": 30,  "cost": 25, "cooldown_ticks": 720},
    {"id": "raid",          "when": "player_base_within(4)",          "action": "raid_nearest_player",    "priority": 20,  "cost": 20, "cooldown_ticks": 360}
  ]
}
```

All numbers PLACEHOLDER (D-BALANCE). `kind: "strain"` activates §4 infection semantics and
requires a registered adapter; `kind: "plain"` is the general case. `cadence` (default
`"tick"`) may be `"round"`: round-cadence rules evaluate once at round end, during the window
**lockdown phase** (§6.4) — this is where the owner's window-time strain decisions live
(Spore hunt targeting, Sculk deploys) without ever touching the fast tick.

### 2.2 The budget

One scalar per AI faction. Income per settlement tick = `base + per_cell × owned_cells +
per_node × nodes`, clamped to `cap`. Every rule action has a cost; the decide pass walks rules
by descending `priority` and fires each rule while its predicate holds and budget remains
(re-evaluating the predicate after each firing, `max_firings_per_rule_per_pass` PLACEHOLDER 4).
Unspent budget accumulates to `cap` — a quiet strain is a coiled spring, which produces surge
behavior without any planning code.

**Dormancy:** below `dormancy.below_budget` income-days the faction skips decide passes entirely
(zero cost while beaten down) until a `wake_on` event re-arms it. This is also the L4 answer to
sculkhorde's old `DEFEATED`-state throttling — the state lives on the board now, not in the mod.

### 2.3 Predicate & action vocabularies (fixed, normative)

Rules compose from **fixed enumerated vocabularies** — there is no scripting, no expression
language in v1 (a KubeJS binding may expose custom predicates later; extension point, not v1).
This is what keeps the cost envelope provable: every predicate is documented with its complexity.

Predicates (v1): `node_under_attack` · `cell_lost_recently(ticks)` · `territory_attacked` ·
`frontier_has_soft_neighbor` (adjacent cell whose defense estimate < own attack estimate — uses
the same public estimates the player UI shows; AI factions get **no fog exemption** beyond their
own vision, ARCHITECTURE §5) · `frontier_intensity_below(n)` · `cells_per_node_above(n)` ·
`player_base_within(cells)` (nearest visible player-faction building) · `budget_above(n)` ·
`counters_below(n)` · `threat_nearby(cells)` · `always`. All are O(1) against maintained
aggregates or O(frontier-sample).

Actions (v1): `expand_cheapest` (claim/contest one frontier cell) · `raise_intensity` ·
`attack_lost_cell` · `reinforce_defense` (spawn/strengthen garrison counter) ·
`spawn_counter(template)` · `place_node` · `raid_nearest_player` (assemble a raid counter and
issue attack orders along a straight-line corridor — no pathfinding; if the corridor is blocked
the raid stalls and dissolves, which is acceptable dumbness by design) · `withdraw` (abandon
lowest-value cell) · `roam_step` (nomads: one-cell drift, biased away from `threat_nearby`) ·
`flee` (nomads: forced march opposite the threat). Every action compiles to the same board mutations player commands produce,
enters the same settlement phases, and logs the same events (`actor.kind: "ai_faction"`).

### 2.4 Determinism

Decide passes draw from `ChessRandom(world_seed, tick, "ai", faction_id)` (ARCHITECTURE §7) — **except round-cadence rules, which seed on the round number per PC9 (R14: tick-seeding them reintroduces the double-deploy bug)**;
given identical board state and tick, an AI faction always makes identical moves. Rule-order
ties break by rule list order. Replays and golden tests cover AI passes like everything else.

---

## 3. AI Counters

AI factions field counters from their behavior pack's template set (`counter_templates/` with
`side: "ai"`). Same attributes, same battle math (counters_and_combat.md); differences:

- **No upkeep bookkeeping** — AI counters decay org slowly outside owned territory instead
  (`ai_out_of_territory_org_decay`, PLACEHOLDER 0.02/tick); inside territory they are free
  and **regenerate org at the standard rate** (A19). Outside territory the decay has a floor
  (`ai_org_floor` 0.5) — wanderers and raid columns arrive worn, never pre-broken.
  (The budget already paid for them at spawn; simulating strain logistics buys nothing.)
- **Garrison by default** — an AI counter without an active rule-issued order sits still.
  **Combat stances (D46, post-close review)**: AI counters carry the same
  `hold_ground`/`free_engage` stance axis as player counters (distinct from the *faction's*
  diplomatic stance, §1.1 — two different axes). Defaults by archetype, overridable per
  template in the behavior pack (`default_stance`): **strains = `free_engage`**
  (`hostile_all` limbs auto-fight what touches them — a sculk blob is never a peaceful
  neighbor); **wanderers = `hold_ground`** (their `flee`/`roam_step` rules own the
  initiative); **settlement survivors = `free_engage` inside their own territory,
  `hold_ground` outside** (defensive means defensive).
- **Mirror provider is the adapter** (§5): where player counters mirror through the CNPC unit
  provider, strain counters delegate manifestation to their strain adapter (the sculk swarm is
  spawned by sculkhorde's own spawn API in driver mode).

---

## 4. Infection Semantics (strains only)

A strain is an AI faction whose territory is *physically expressed as infestation*.

### 4.1 Cell intensity

Each strain-controlled cell carries `intensity ∈ {1 foothold, 2 established, 3 deep}` plus an
optional `node` flag (strain stronghold — the sculk node / spore hive / zhanghai lair). Intensity
is raised by the `raise_intensity` action, feeds the strain's defense modifier (`×(1+0.15·i)`,
PLACEHOLDER), scales spawn budgets, and drives paint-on-load depth.

### 4.2 Paint-on-load (the alignment mechanism)

When a strain-controlled cell's chunk loads, the adapter receives
`paint(cell, intensity, node?)` and makes the chunk *look and behave* infected to that depth —
infestation blocks, ambient spawns, node structures. Painting is **budgeted and incremental**
(`paint_budget_blocks_per_tick`, PLACEHOLDER 256): a freshly-loaded chunk converges toward its
intensity over seconds, never in one hitch. The inverse duty: when a cell the mod has physically
infested is *not* strain-controlled on the board (player cleansed it abstractly, or control
flipped in battle), the adapter **suppresses/purges** on load — in driver mode by simply not
painting and cleaning residue via the mod's own APIs; in observer mode this direction is
impossible and the divergence is an accepted v1 limitation (§5.1).

### 4.3 Cleansing

Winning a battle over an infected cell flips control (counters_and_combat.md §5.6); intensity
drops to 0 and the cell carries a `scarred_infested` terrain flag until either purge-on-load
completes (driver mode) or a Picasso `restyle`/`degrade` order rehabilitates it during a window
(integrations.md — the "cleanup work order" path for deep/node cells is deliberate fiction:
big cleanups happen "overnight"). Node cells cannot be cleansed by abstract battle alone while
the physical node structure stands **if any player has the chunk loaded** (D-REALITY: the node
entity/structure must die physically); unloaded node cells fall to abstract siege normally.

### 4.4 Infection pressure on players

Strain cells adjacent to player territory exert `infection_pressure` — a stability drain and
(later, L2 联动) gas/status effects on players inside — making the front line *felt* in-game.
PLACEHOLDER rates; wired in economy_and_buildings.md §6 gauges.

---

## 5. The Three Strain Adapters

`StrainAdapter` is the only place a mod name appears. Interface (shell-side):

```
sense(chunk)        -> observed infestation summary        [observer mode: board input]
paint(cell, i, n?)  -> drive mod to express intensity      [driver mode]
purge(cell)         -> remove residue on load              [driver mode]
spawnUnits(counter, cell, budget) -> manifest counter      [both modes]
countUnits(cell)    -> census of strain entities           [writeback support]
onModEvent(e)       -> node destroyed, raid ended, …       [board input]
```

### 5.1 Modes (L4 staging — config `strain_mode.<strain>`, per-strain)

| Mode | When | Who is the brain | Divergence story |
|---|---|---|---|
| **observer** | today, pre-L4 rewrite | the mod's own AI in loaded chunks; Chess mirrors what it senses into board state and simulates only unloaded expansion | loaded reality wins (D-REALITY); board may lag the mod; Chess cannot suppress mod spread — accepted v1 limitation, logged as `infection/divergence` events |
| **driver** | post-L4 rewrite | Chess (behavior pack); mod autonomous AI disabled by config; adapter calls the four L4 API families: ① infest (block/area/chunk) ② spawn unit ③ generate structure ④ raid orchestration | none by construction — the mod does nothing Chess didn't order |
| **off** | mod absent / disabled | nobody; cells thaw to `derelict_infested` neutral flag | n/a |

The L4 rewrite (sculkhorde+spore, possibly merged into one mod) is tracked in
`UtilWeDie-Neo-1.21.1/联动待办.txt` §L4 and is **out of this repo's scope**; this spec pins what
Chess needs from it: the four API families, stable Java entry points, events for node/raid
lifecycle, and a config flag that fully disables gravemind/hive autonomy. Feasibility is
confirmed (D20): **both mods' source is in hand** — the castration is a source-level patch,
not a bytecode hack.

### 5.2 Per-strain notes

| Strain | Mod | Adapter specifics |
|---|---|---|
| `sculk_horde` | Sculk Horde 0.11.3+ (1.21.1 port) | nodes ↔ sculk node structures (③), generated **live** — ruled (D11): the strain mods are being source-modified into tool-function libraries whose infest/spawn/structure/raid capabilities answer to Chess's management alone; the wargame_interface structure-boundary rule governs *Picasso* writes and does not apply here. Raids map to RaidHandler orchestration (④). Old AutoPerformanceSystem throttling is superseded by budget+dormancy (§2.2); cursor-based spread is replaced in driver mode by `infestChunk`-family calls under the paint budget |
| `spore` | Fungal Infection: Spore 2.2.0+ | hive/proto-hive structures as nodes; gas fields as intensity-3 paint; L2 gas-mask 联动 hooks its effect application, not Chess |
| `zhanghai_mitu` (障骸迷途) | CustomNPCs (unofficial port) + our scripts | **always effectively driver-mode** (there is no autonomous mod — the monsters are CNPC templates): spawnUnits = CNPC spawn from `data/swchess/ai_factions/zhanghai_mitu.json` spawn tables; paint = none or sparse props v1 (optional Picasso `restyle` for deep cells as a window-time extension); nodes = scripted lair spawner NPCs |

### 5.3 Spawn/census discipline

Strain entity spawns obey the same global mirror caps as player mirrors (ARCHITECTURE §10) and
**defer to mod-native spawns in observer mode** (never double-spawn: adapter census counts
mod entities toward the counter's manifestation before spawning anything). All strain entities
carry a `swchess:counter` tag for writeback attribution (counters_and_combat.md §6.3); untagged
mod-spawned infected in observer mode write back **only** as cell-level pressure, not counter
casualties — attribution needs a tag behind it (same principle as build-log attribution needing
a player behind an event).

---

## 6. Strain Playbooks (D31 — owner-authored designs, normalized into the framework)

The three strains diverge sharply by design: **Sculk and Spore are strong-territory,
hard-capped expanders; zhanghai_mitu is the map-wide ambient infected.** Everything below is
expressed as behavior-pack rules (budget + cadence + caps) so the §1 cost envelope still holds.
Strain resources are just the §2.2 budget renamed: Spore's **biomass 生物质**, Sculk's
**mind points 精神值**.

**Doctrine exception — window-time targeting is DM-view (A6, normative).** §2.3's
no-fog-exemption rule governs **tick-cadence** rules (expansion, defense, tactical fights).
**Round-cadence playbook targeting** (Spore hunt ranking, Sculk hotspot/lurk site selection)
deliberately reads objective board state — population figures, battle records — because these
rules are the *director's pressure system*, not a competing player. Fairness holds because
information is never converted to tactical advantage faster than physical counterplay: raid
counters march visibly, hotspots deploy only at round boundaries, lurks creep at 1 block/5 s
and are physically discoverable. ("We are making a game, not simulating an apocalypse" — D40's
principle, applied to the director's chair.) Tick-cadence rules remain vision-bound; "public
estimates" in tick predicates means exactly the fog-visible strength bands
(board_and_fog.md §6).

### 6.1 Ephemeral counters (regenerated each round end, lockdown phase)

| Type | Placement | Purpose |
|---|---|---|
| `wildlife` 野生动物 | noise-driven random placement **within an activity band** (cells within `wildlife_band` (8) of any player territory, strain territory, or explored cluster — NOT the whole raw map; D36/Q6 resolved) | Spore hunt targets; ambient fauna |
| `infected_farmland` 感染者农业用地 | random distribution around Spore territory (`farmland_ring` 3) | Spore farm consumable |

Both are destroyed wholesale and re-rolled at each round end; caps `wildlife_max` (24) /
`farmland_max_per_region` (4). They are counters (visible, attackable) but own no territory.

**The `env` pseudo-faction (A16, normative):** ephemeral counters belong to the reserved
faction id `env` — cap-exempt (outside D13's 16), no behavior pack, no diplomacy, stance
effectively neutral. Combat profile: fixed template stats, no retreat, instant shatter on
break; attacking one opens an ordinary two-sided battle with no legality gate. Ephemeral
placement is the board's third cell-materialization trigger (ARCHITECTURE §5 invariant 3,
amended).

### 6.2 Spore — hunt · expand · farm · assault

- **Hunt** (`cadence: round`): rank candidate targets — wildlife counters, zhanghai counters,
  and *high-population player factions* — by a deterministic value score
  `expected_biomass / expected_losses` computed from public-estimate stats; dispatch hunt
  counters to the best. Anti-player hunts field slightly **weaker** counters at a steep
  **biomass discount** (cheap harassment by design). The ranking is a fixed formula with an
  **override API hook** so the narrative AI may substitute its own choice when present
  (never a blocking dependency — D-CHEAPBRAIN, open Q7).
- **Expand** (`cadence: tick`): purchase random adjacent cells, weighted toward consolidating
  its smallest contiguous territory patches. Hard expansion cap per round.
- **Farm** (`cadence: tick`): consume one adjacent `infected_farmland` counter → biomass.
- **Assault** (admin-driven): admin designates a region + intensity in the admin panel (or via
  external API — `/chess strain assault <strain> <bounds> <intensity>`); Spore dispatches
  multiple counters there. Spore counters know only attack / not-attack; the Spore *faction*
  carries favorability toward player blocs — low favorability → hunts/assaults engage player
  factions, high → it leaves them alone (governance.md §7 gauge, extended to AI factions).

### 6.3 Sculk — one physical heart · ink-splash · hotspot · lurk

**The core region**: exactly one, map-wide. It cannot be occupied or destroyed at the Chess
layer — only physically destroying the **ancient sculk node** inside it kills the strain
(D-REALITY at its purest). Admin can resurrect the node at a chosen region and reset the
horde's death state at any time. Mind points accrue when non-Sculk units die inside infected
regions.

- **Ink-splash spread** (`cadence: round`): deploy ≤ `sculk_deploys_per_round` (3) infection
  regions per window at chosen map positions, ≤ `sculk_regions_max` (10) total, generation
  efficiency decaying with count. Each region: center cell gets a **sculk node structure**
  (mod API ③) + an infection cursor; the region grows cell-by-cell in live time to
  ≤ `sculk_region_cells_max` (21); sculk mobs spawn inside (mirror caps apply). A region dies
  with its node cell, by either path (A20, consistent with §4.3): **physically** destroying
  the node structure, or **abstract siege of the node cell while unloaded** — annex cells can
  always be sieged cell-by-cell; the node cell itself resists abstract fall only while any
  player has it loaded (D-REALITY: a standing node must die physically in front of
  witnesses). An abstractly-fallen node's structure is marked for purge-on-load and the
  region + remaining annexes collapse. While ≤ 2 regions are active, deploys cost no mind
  points (comeback mechanic).
- **Hotspot focus** (`cadence: round`): player-faction battle sites become candidate deploy
  points for infection cores, ≤ `sculk_hotspots_max` (6), same region machinery.
- **Lurk** (`cadence: round`): invisible on the player map (admin-visible; fog rule exception,
  logged `visibility: admin`). Each round end, pick one high-population camp; place a 1-cell
  lurk seed: spawns a passive sculk phantom (attacks nobody), runs a slow infection cursor
  (~1 block / 5 s). The seed counter tracks its infected-block count: **≥ 200 blocks →
  converts to a full ink-splash node region.** Counterplay ladder (config table, not code
  branches): progress > 80 and player cleanses (reduces blocks + kills the phantom) → drops
  to 50; progress 50–80 → drops to 20; progress < 50 **and** a purification potion/node is
  applied → lurk fails. Cells within 3 of a purification node are never selected as seeds.

### 6.3a zhanghai_mitu — the ambient strain (D36, resolves Q6)

zhanghai owns **no cells**. It is a noise-driven **ambient density field** over the activity
band plus ephemeral roaming counters (re-rolled each round like wildlife); only its horde/raid
counters persist. Its physical presence is fully lazy: monsters in unwatched areas are
**despawned outright** and regenerate gradually when a player loads the chunk (paint-on-load
applied to mobs — nothing exists where nobody looks, D-NOCHUNK's entity twin).

**The AmbientField, made first-class (A17):** a sparse per-cell `ambient_level ∈ 0..3`,
stored in `ChessSavedData` beside the board, regenerated each round from noise ∩ activity
band. It is environmental state, not control — `cell.controller` never says zhanghai.
Normative consequences: "zhanghai/strain-controlled cells" in D36's heat rule reads
**`ambient_level > 0` or strain-controlled**; zhanghai income = `base + k_amb ×
Σ ambient_level` (the pack's `per_cell`/`per_node` terms are unused); its "lairs" are flags on
its persistent horde counters, not cell nodes (§4.1 node flags remain strain-territory-only).
Fog & UI: ambient level is cell data like terrain — live in `visible`, frozen in `explored`
snapshots; the map's Infection layer renders it as a haze/stipple ramp distinct from the
territorial intensity ramp (map_interface §3).

### 6.3b The player activity heat meter (D36 — cross-strain)

Per-player gauge `heat` 0–100, server-side: gunfire, sprinting/jumping, and block-breaking add
(weights PLACEHOLDER; sources named per B2 — block-breaks ride the recorder feed, gunfire and
movement need Chess's own entity/input hooks, the recorder contract covers place/break only);
decays `heat_decay_per_tick` each settlement tick **while online — heat freezes on logout**
(B2: no offline laundering; stealth is played, not waited out). Effects inside
strain-controlled/ambient cells: **spawn budgets scale with the entering player's heat**
(`spawn_mult = 1 + k_heat × heat/100`), and mobs spawned while a nearby player's heat > 60
**auto-target that player**. Loud players summon their own audience; sneaking through infected
ruins is a real stealth mechanic priced in the same meter.

### 6.3c Window-time unit-composition tuning (D37)

At window entry, the composition (unit-type weights) that Spore/Sculk attack counters will
spawn next round is re-weighted from the round's player-vs-strain combat record
(`chess_log` combat topic: what killed strain units, what killed players). Default is a
**deterministic counter-adaptation table** (data: tactic signal → weight shift, e.g. "deaths
mostly to gunfire at range → more fast/flanker types"); the narrative AI may *replace* the
weights via the same API hook as hunt targeting — present = smarter, absent = still adaptive,
never blocking (the Q7 pattern, now doctrine for every strain↔AI touchpoint). **Determinism
duty (B1)**: any override lands as a `bridge/ai_override` ingest event *before* the decide
pass consumes it — replays see identical inputs; no event means the deterministic default ran.

### 6.4 Round-end sequence & window lockdown (D35)

Chess owns the **window airlock**: `/chess window lockdown` (admin command + external API)
kicks all non-admin players and bars joins. In lockdown, while the server is still up, Chess
runs round-end computation: ephemeral counter re-roll (§6.1), round-cadence strain rules
(§6.2–§6.3 deploys and targeting), work-order submission flush. Then ops stops the server for
the Picasso window — **lockdown is the airlock, not a live-edit license**: Picasso's
closed-save contract (`wargame_interface.md` §1) is untouched. On restart, `/chess window open`
lifts the bar and the new round begins (quota resets, D15).

**Lockdown is a persisted latch, not a session flag (owner Q, post-close clarification).**
The lockdown state lives in `ChessSavedData`, made durable **as the lockdown command's first staged action — a latch save before the kick step reports ✓ (R13)**: a server that stops in lockdown at any later stage **boots in lockdown** — non-admins stay barred until the explicit `/chess window open`, giving ops the
post-window verification gap (read receipts, inspect Picasso's edits) before players flood
in. There is no auto-open; the login screen shows a maintenance message (`lockdown_motd`
config). Two edge semantics: a crash during normal live play never enters lockdown (only the
command does — a crash is not maintenance); a lockdown → restart with **no** Picasso window
(D45) keeps the latch too — opening then resumes the *same* round (no close record → no
round increment → no quota reset).

**Lockdown pauses the whole simulation (owner Q, post-close clarification).** After the
final settlement pass + round-end computation, the settlement scheduler halts — production,
upkeep, battles, movement, AI decide passes, gauges, everything (they are all pipeline
phases; pausing the pipeline pauses the world). Nobody schemes during maintenance, and
player industry idles identically for everyone. `/chess window open` resumes the scheduler.

**Progress reporting & crash-resume (owner Q).** `/chess window lockdown` reports staged
progress to the invoker and console (kick ✓ → final settlement ✓ → ephemeral re-roll ✓ →
strain deploys ✓ → orders flushed (n) ✓ → snapshot written ✓ → **SAFE TO STOP**);
`/chess window status` queries it any time. Round-end computation is **atomic with respect
to persistence**: it runs in memory between the two PC9b saves — the input pin before it, the completion save (marker + effects) after it. Ordering is normative (R12): the input-pin save completes strictly before the in-memory `roundend_done_for` marker is set; the marker becomes durable only in the completion save. A stop before SAFE TO STOP loses marker and effects together, and
the restart (still latched) detects the missing marker and re-runs the computation in full;
the only external side effect, work-order files, is crash-safe by the contract's `order_id`
dedup (`wargame_interface.md` §2 — resubmission is safe). **Re-run determinism makes that
dedup claim true (post-close review)**: round-end computation draws its randomness from
`ChessRandom(world_seed, round, subsystem, subject_id)` — seeded on the **round number, not
the tick** (ARCH §7) — so an aborted round-end re-runs *bit-identically*: same ephemeral
rolls, same deploy choices, same order ids. Tick-seeded re-runs would generate
different-content orders under fresh ids, and the crashed attempt's orphans in `pending/`
would execute alongside them — double deploys the dedup can't see. **Input pinning (PC9b —
same seeds need same inputs)**: the lockdown sequence performs **two explicit saves** — one
immediately after the final settlement pass (pinning the exact board state round-end
computation reads), one at completion (marker + effects). A crash between them reloads the
pinned input, so state-dependent choices (deploy sites, hunt ranking) reproduce exactly, not
just the dice.

**Round-key idempotence (A18, normative; persistence semantics sharpened post-close).** The
only authoritative round number is `window_log.json`'s counter; round-end computation runs
*for* round `R+1` (current counter `R`), sets `roundend_done_for = R+1` **in memory before
acting** (same-session re-entry guard), strictly *after* the PC9b input-pin save completes —
durable persistence of the marker happens only in the completion-time save (R12), so a
crash can never strand a persisted marker over half-done effects, and a pinned input can
never carry a marker. A lockdown that finds
`roundend_done_for` already at `R+1` (aborted maintenance, crashed window with no close
record) **skips** round-end computation — no double re-rolls, no double Sculk deploys, no
duplicate `issued_round` orders. All per-round caps (`sculk_deploys_per_round` etc.) are
keyed to round numbers, never to lockdown events. Receipt classification: `in_progress`
receipts visible at server start are **pending, not terminal** — they await the next window's
quarantine per contract §2; the receipt poller must not resolve their state machines.

## 7. Events (topic `infection` unless noted)

`strain_expanded` · `strain_intensity_changed` · `node_placed` / `node_destroyed` (mod event ①③)
· `raid_started` / `raid_ended` (④) · `cell_cleansed` · `divergence` (observer-mode board/world
mismatch, with reconciliation applied) · `strain_dormant` / `strain_awakened` ·
`lurk_placed` / `lurk_progress` / `lurk_converted` / `lurk_failed` (`visibility: admin`) ·
`assault_ordered` (admin) · `ephemeral_reroll` (round summary) · AI moves land in
`combat`/`territory` topics with `actor.kind: "ai_faction"`. Schemas in event_log.md.

---

## 8. Open Questions (v0.1)

1. ~~Node generation live vs window~~ **Resolved (D11)**: live, under Chess's management only.
   Residual for L4: node generation emits a `chess_log` event with bounds (auditability).
2. ~~Raid corridors~~ **Resolved (D30)**: no raid planning — counters are pawns; straight
   corridors stand. (Player counters separately gain shift-queued waypoints,
   counters_and_combat.md §3.)
3. ~~Observer-mode divergence~~ **Resolved (checkpoint 4)**: reconcile only on chunk load,
   no background sweep.
4. ~~`zhanghai_mitu` id~~ **Resolved (checkpoint 4)**: confirmed.
5. ~~Cross-strain hostility~~ **Resolved (D31)**: they fight; sculk/spore are hard-capped
   territorial, zhanghai is the ambient map-wide strain.
6. ~~zhanghai representation~~ **Resolved (D36)**: density field + ephemeral roamers + lazy
   mob despawn/regen (§6.3a).
7. **Hunt-ranking override hook** (§6.2): v1 target choice is a deterministic formula; the
   narrative AI may override via API when present. D37 adopted this exact pattern for
   composition tuning, so it is treated as settled doctrine unless the owner objects —
   formal ack still welcome.
8. ~~Favorability for AI factions~~ **Resolved (D39a)**: one unified table keyed by
   `(side_a, side_b)` where a side is a bloc or an AI faction. AI-side driver: completing an
   AI faction's NPC quests raises favorability with it (CNPC affiliation/quest-record
   interface — integrations §8 ask 8).
9. ~~Lurk fog exception~~ **Resolved (D39b)**: confirmed; physical discoverability is the
   counterplay channel.
