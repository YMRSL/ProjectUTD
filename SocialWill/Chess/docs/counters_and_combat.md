# Counters & Combat — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> Draft 3 integrates checkpoint-4: D24 variable footprints + stackable NPC counters, D26
> per-cell reserves, D27 armor/medical mitigation + TACZ loadout pools, D29 collapse
> refinement, D30 waypoint queues.
> Encodes **D3** (loaded-chunk entity combat writes casualties back into counters; the abstract
> layer never overrules loaded reality), **D12** (counter presence ≠ control — occupation
> requires a core anchor, §5.6), and **D14** (simplified relative to HOI4: no entrenchment, no
> armor axis — the pack has no player-buildable armor fleet). All coefficients are PLACEHOLDER
> config (D-BALANCE) — attack the *mechanisms*, not the numbers.

---

## 1. Scope & Vocabulary

A **counter** (兵棋) is an abstract military unit token on the board. It is the only thing that
fights, occupies, and dies in the abstract layer. Its physical expression — the entities a
loaded chunk sees — is the **unit mirror** (§6), a budgeted representation, never a 1:1 army.

Lifecycle: `training → active → (retreating | shattered | disbanded)`. **Disband legality
(C5)**: only outside battle, outside retreat lockout, and on own controlled territory — a
breaking army cannot evaporate past the shatter roll and its A1 population deaths, and a
cut-off counter abroad marches home instead of teleporting its slice back. Counters belong to
exactly one faction (player- or AI-driven). Player counters are created at barracks
(economy_and_buildings.md §4), AI counters by behavior-pack actions
(ai_factions_and_strains.md §3).

---

## 2. The Counter Model

### 2.1 Attributes (state)

| Attribute | Range | Meaning | HOI4 analogue |
|---|---|---|---|
| `org` | 0..1 | cohesion/will to fight; damaged first, regenerates | organization |
| `strength` | 0..1 | materiel/effectiveness fraction of template base | strength |
| `manpower` | 0..template max | people in the unit; casualties subtract here | manpower |
| `veterancy` | 0..3 | **multiplicative** attack/defense bonus, ×(1 + 0.05×rank), cap +15% (§5.3). Accrual (owner Q, post-close): +1 rank per `veterancy_battles_per_rank` (2) battles fought to completion without shattering — "completion" = the Battle *ends by any outcome* while this counter participated ≥ 1 combat round; an orderly retreat counts (surviving a losing fight is also experience), a shatter does not; battles against the `env` pseudo-faction / ephemeral counters never accrue veterancy (R8); shattered rebuilds start at 0; **no reinforcement dilution v1** (comprehension cost). Distinct from the `veteran_squad` *template* — that is a training tier (focus-gated factory spec); veterancy is the per-counter service record, and a fresh veteran_squad still starts at rank 0 | experience |
| `casualties_total` | counter lifetime | cumulative losses; `伤亡率 = casualties_total / (manpower + casualties_total)` — a derived stat shown in UI and battle reports, never stored as its own truth | — |

`strength` and `manpower` are coupled: manpower loss applies proportional strength loss
(materiel is carried by people), but strength can also drop alone (equipment attrition from
unpaid upkeep). Reinforcement restores both (§7).

### 2.2 Template — `data/swchess/counter_templates/<id>.json`

```json
{
  "v": 1, "id": "swchess:soldier_squad", "name": "Soldier Squad",
  "class": "infantry", "side": "player",
  "base": {"attack": 10, "defense": 12, "max_manpower": 100},
  "move_period_ticks": 3, "sight_radius": 2, "footprint": 1,
  "upkeep_per_tick": {"food": 0.4, "equipment": 0.02},
  "train": {"building": "barracks", "ticks": 90,
            "cost": {"manpower": 100, "food": 15, "equipment": 10}},
  "armor_rating": 0.1, "medical_rating": 0.15,
  "tags": ["motorized"],
  "mobility": {"mode": "foot", "speed_mult": 1.0, "road_bonus": 2.0, "requires_road": false},
  "mirror_profile": {"provider": "cnpc", "unit": "utd:soldier_rifleman",
                     "manpower_per_entity": 5,
                     "loadout": {"gun_pool": "tacz:faction_rifles_t1"}}
}
```

`tags` (D47): free + reserved vocabulary — modifiers, focus effects, and the narrative layer
select counters by tag (a motorization focus can multiply `speed_mult` for `motorized`
counters without naming templates). `mobility` (D47, **schema in v1, only `foot`
engine-enabled; other modes are the v0.2 mobility subsystem** — §3.1).

`loadout` (D27): CNPC soldiers fire TACZ weapons via scripts; the usable gun pool is
constrained by faction (bloc/focus unlocks) and unit type — pools are data
(`data/swchess/loadouts/*.json`), the script layer resolves them at spawn.

Player v1 set (PLACEHOLDER): `militia` (cheap, weak, fast to train) · `soldier_squad` ·
`veteran_squad` (gated behind FTB focus, integrations.md). Strain sets live with their behavior
packs. New counter types are data files (ARCHITECTURE §13) — **the authoring surface is open
by design** (owner Q, post-close clarification): the engine fixes only the combat formula's
*shape*; every stat, cost, and type is template/config data, datapack-hot-reloadable, so the
narrative layer (or the owner) ships new counters without code. The narrative-specific class
is the D24 special-NPC counter (§2.3), which additionally exposes the counter-state memory
API.

### 2.3 Footprint (D24)

Footprint varies by counter type and scale (template `footprint`, 1..N cells): small squads sit
on one cell; large formations and strain blobs span several. v1 keeps multi-cell counters
**static while multi-cell** (a formation contracts to footprint 1 to move — rigid-shape
movement buys nothing yet; ARCHITECTURE §15 Q2). **The one exception is battle-driven front
movement (D46, §5.2)**: the static rule binds *movement orders*; footprints deform
cell-by-cell along an active front (attacker edges extend into converted cells, defender
contracts contiguously toward its anchor) — that deformation is Battle machinery, not
movement. Multi-cell counters fight under the per-cell-reserve rule (§5.3a).

**Special-NPC counters** (D24, narrative class): `class: "npc"`, `stackable: true` — the one
counter class allowed to overlap another counter's cells. They spawn a strictly limited roster
of special NPCs (tradeable, quest-giving); the counter record is the NPC's **memory** —
dialogue state, quest offer/acceptance relations — written and read by the narrative system
(MiroFish) through `chess_log` + a small counter-state API. They have no combat stats and are
never battle participants; hostile occupation of their cell despawns them (the counter
persists, flagged `displaced`).

### 2.4 Stacking

At most `max_counters_per_cell` (3) friendly counters per cell. A cell's garrison fights as one
side in battle. Counters of two factions never co-occupy a cell *at rest* — overlapped cells
are **contested front cells owned by a Battle** (D46: the overlap *is* the battle, §5.2).

### 2.4a Merge & split (D48 — v1)

Templates may declare `reorg: {merge_into, merge_count, split_into, split_count}` —
reorganization is data-driven pairing, never free-form. **Merge** (e.g. 3 × soldier_squad →
1 × soldier_company): legal when all participants are same-faction, declared-compatible,
stationary, co-located/adjacent, none in battle or retreat lockout, and the target footprint
is placeable under stacking rules. Composition: manpower sums — overflow above the target
max returns to the faction pool **only when merging on own controlled territory; abroad, an
overflowing merge is refused (`merge_overflow_abroad`, R7 — C5's teleport closure applies to
reorg too)**; org and strength
are **manpower-weighted averages** (a broken unit cannot be laundered with fresh troops);
veterancy = weighted average, floored; wounded pools merge. **Split** is the inverse: equal
manpower division, stats inherited, wounded pro-rata — **split shares merge's full
preconditions; products place co-located/adjacent under stacking rules or the split is
refused (`split_no_room`, R20)**. Equipment conservation is a
content-authoring duty — merge pairs are authored cost-neutral (3 squads' training equipment
≈ 1 company's). Both operations impose `reorg_lockout_ticks` (12): no attack initiation
(**including `free_engage` auto-initiation — defense unaffected**, R20), no re-reorg — and
are illegal mid-battle or under retreat lockout (the C5 disband family). Reorg products
inherit the newest participant's training timestamp (econ shortage-order recency, R20).

### 2.5 Stances (D46 — v1, promoted from the former v1.5 list)

| Stance | Behavior |
|---|---|
| `hold_ground` (default) | defends its own footprint only. Adjacency with a hostile footprint is **peaceful indefinitely** — edges touch, nothing happens — until an explicit attack order (right-click hostile / A+click) or the enemy pushes in |
| `free_engage` | auto-initiates against **already-legally-hostile** targets (at-war factions, `hostile_all` AI) entering engagement range (adjacent to footprint). Never bypasses declaration legality — against undeclared factions it behaves as `hold_ground` |

Selection & targeting (D46): a counter's **footprint is its hitbox** — clicking any of its
cells selects the counter (center cell is only the pathing/display anchor). **Unmodified
left-click selects/inspects and never issues commands (R15)** (clicking an enemy opens its fog-appropriate
info card); commands come only from right-click context (hostile footprint = attack that
counter · neutral = move) or A+left-click explicit attack. Attack orders target *counters*,
auto-resolved to the nearest edge cell of the target's footprint. On contested front cells
(both footprints present) clicks prefer own-side counters and cycle on repeat; A+click
targets the enemy participant (R19).

Three implementation-facing rules (post-close review):

- **Pursuit / re-resolution**: the edge-cell resolution repeats at each movement leg while
  the target is *visible*; if the target drops to fog, the order degrades to
  move-to-last-known-edge and halts there with `halted: target_lost` (no fog-guided
  pursuit — consistent with A22's per-requester validation).
- **`free_engage` triggers only while stationary** (garrison auto-defense); a counter
  executing a move/attack order follows that order — there is no attack-move in v1.
  Auto-initiated engagements resolve in pipeline phase 8 alongside ordered ones, logged with
  `actor` = the counter's faction (they are faction acts, subject to the same legality).
- **Naming**: the counter *combat stance* (`hold_ground`/`free_engage`) is a different axis
  from the AI faction's *diplomatic stance* (`hostile_all`/`neutral`/`defensive`,
  ai_factions §1.1); AI counters carry combat stances too — defaults in ai_factions §3.

---

## 3. Movement

- Counters move along **4-adjacency** (von Neumann) paths; diagonal = two steps. One cell per
  `move_period_ticks` (template), terrain-modified (board_and_fog.md terrain classes:
  `open ×1 · forest ×1.5 · urban ×1.5 · water impassable v1`, PLACEHOLDER).
- Paths are computed server-side (A*, bounded `max_path_cells` 64) over cells the faction has
  **explored**; destination must be explored (you cannot order armies into the void — fog is
  a real constraint, board_and_fog.md §6).
- **Waypoint queues (D30)**: shift-click appends waypoints RTS-style; the counter executes
  them in order (each leg is an ordinary move/attack; a refused leg halts the queue with a
  notice — no silent skipping).
- Entering a *foreign player faction's* cell requires `at_war` relation (§5.1); entering
  AI-faction (`hostile_all`) or neutral cells is always legal. Illegal moves are refused at
  command time (D-REFUSAL) — **validated against the requester's fog view (A22)**: if the
  blocking control is fog-hidden, the order is *accepted* and the counter halts at the border
  on contact (`halted: border_encountered`, revealing exactly what physical scouting at that
  range would reveal); no attack occurs without a declaration. Fog-hidden conflicts in
  purchases return the deliberately fog-flat code `cell_unavailable` (no owner named).
  Refusal specificity and fog integrity are reconciled by this rule: specific reasons for
  what you can see, flat codes for what you cannot.
- A move into a cell already at `max_counters_per_cell` is refused at order time and halts a
  waypoint queue (A26).
- Moving costs readiness: org regen halves while moving.
- Moving *into* a hostile-garrisoned or hostile-controlled cell converts the move into an
  **attack** (opens/joins a Battle, §5.2): the attacker's edge **pushes into** the target's
  edge cells — the overlapped cells become the contested front (D46) and resolution takes
  over (§5.3); the front advances cell-by-cell as it wins (§5.6). Attacks into fog
  (explored-but-not-visible cells) are legal and discover the garrison the hard way.

### 3.1 Mobility modes (D47 — designed now, built v0.2 except `foot`)

| mode | Movement | Combat |
|---|---|---|
| `foot` | standard cell-by-cell, terrain multipliers (§3) — **the only v1 mode** | baseline |
| `wheeled` | road-dependent: `road_bonus` on road cells, heavy off-road penalty or `requires_road`; avoids water/dense-rubble obstacle classes | speed over survivability; vehicle weapon modifiers |
| `tracked` | road-preferring, mild off-road penalty; avoids water | high attack/defense modifiers; `liquid_fuel` upkeep (D33) |
| `airship` | terrain-blind; **grounded ↔ airborne state machine** (`takeoff_ticks`/`landing_ticks`); moves only airborne | airborne: no occupation, no siege participation, bombardment-style restricted attack, AA-vulnerable; must land to garrison |
| `odm` | terrain multipliers **inverted**: urban/forest fast (anchor structures), open slow | melee-specialized |

Common rules: vehicle modes carry combat modifiers (the "vehicle bonus") and `liquid_fuel`
upkeep; mirrors may use real vehicle entities (`mirror_profile.provider: "vehicle"` —
superbwarfare's fleet); pathing for road-dependent modes runs over the cell `road` layer
(board_and_fog.md §2.1). **Movement-rate formula (shape pinned now — v0.2 builds against
it):** `effective_period = ceil(move_period_ticks × terrain_mult(mode) / (speed_mult ×
road_mult))`, where `road_mult = road_bonus` on cells with `road > 0` (else 1), terrain
multipliers are per-mode tables (odm inverts, airship ignores), and `requires_road: true`
makes off-road cells impassable to that counter. The road term applies **from v1 for `foot`**
(roads speed infantry too; the road layer ships v1) — only the non-foot mode rows wait for
v0.2. **`physical_unique` provider (v0.2 + spike)**: a specific
player-built Sable contraption (e.g. an airship) registers as a counter — the physical
structure *is* its manifestation (loaded = the actual ship, unloaded = abstract; physical
destruction = counter death via writeback). D28's scope ruling stands: no frontlines, no
invasion-planning arrows, ever. Three v0.2 definition notes (R18): airship *attack* is a
declared non-overlap exception to the D46 engagement model, to be designed with v0.2;
landing requires a non-water cell; "dense rubble" reads as the `ruin` terrain class.

---

## 4. Vision

`sight_radius` (template, cells, Chebyshev) contributes to the faction fog field
(board_and_fog.md §6) along with territory, buildings, and online member positions. AI factions
use the same vision rules — no omniscience (ai_factions_and_strains.md §2.3).

---

## 5. Battle

### 5.1 Legality & initiation (reframed per D46 — attacks target counters)

A Battle opens against a **defender counter** when an attack order targeting it (right-click
hostile / A+click, §2.5) or a `free_engage` auto-initiation lands (player command or AI
action). Legality is checked against the **target counter's owning faction**, wherever its
footprint stands (a hostile counter camped on neutral ground is attackable; entering foreign
*controlled territory* is separately gated by §3's movement legality). Against player
factions this requires a standing `at_war` relation — declared by command, broadcast as a
notice, logged (`governance/war_declared`). Cross-bloc declarations cost the declarer
stability **and** unity, scaled by inter-bloc favorability (D21, governance.md §7); same-bloc
declarations are legal only while the target's unity < 60% (D16 — the unity gauge doubles as
a legitimacy shield).

**One battle per defender counter (A7, re-expressed for the overlap model)**: while a Battle
runs, an attack order against the same defender counter from a *third* mutually-hostile side
is refused/queued with `target_contested` — it may instead attack one of the current
*attacker's* counters (attackers are ordinary counters and legal targets; that opens a
normal, separate Battle). True multi-side resolution in one Battle is explicitly out of
scope for v1; entry-denial keeps the two-sided model sound even with three strains and
players in one neighborhood. AI `hostile_all` factions are perpetually at war with everyone.
Multiple same-side attackers join the existing Battle by issuing an attack order on the same
defender counter (multi-front assault, §5.2 many-vs-one rules); reinforcements join
mid-battle the same way.

Three model-soundness rules (agent round, R2/R3/R1):

- **No mirror battles (R2)**: an attack or `free_engage` auto-initiation targeting a counter
  the initiator *already shares a live Battle with* **joins that Battle** as a counter-push
  on the existing front — it never opens a second one. A pair of opposing counters is in at
  most one Battle together; each cell is a front cell of at most one Battle (ARCH §5
  invariant 1, enforced by construction).
- **Stack conscription (R3)**: all counters co-stacked on any cell of the targeted defender's
  footprint are conscripted into `defender_side` at battle open (per §2.4 — a garrison
  fights as one side, regardless of individual stances) and count as "the same defender" for
  `target_contested`. Defeat-in-detail against a watching stack is impossible.
- **Territorial siege (R1)**: entering an ungarrisoned hostile-*controlled* cell opens a
  **degenerate Battle against the cell anchor** — a pseudo-defender whose per-cell share
  derives from the cell's integrity (player cells) or intensity (strain cells) times
  fortification, with no org, no manpower, no retreat; when the share breaks, conversion
  proceeds normally (empty claimed land falls in about one tick; a hardened or deeply
  infested cell resists). This is the single mechanism behind ai_factions §4.3's
  "winning a battle over an infected cell" and §6.3's cell-by-cell annex sieges.

### 5.2 The battle object — the overlap front (D46)

```
Battle { id: btl_<seq>, front_cells: [(cell, att_share, def_share)],
         defender_side: [counters], attacker_side: [(counter, origin_edge)],
         started_tick, rounds: [], loaded_overlap: bool }
```

An attack pushes the attacker's edge cells **into** the defender's edge cells; each
overlapped cell is a **front cell** owned by the Battle (co-presence exists nowhere else —
the standing invariant's "a Battle, never co-occupancy" is satisfied by construction).
Footprint-1 vs footprint-1 degenerates to a single front cell — both counters visually
stacked on it, fighting until one breaks. Loaded front cells spawn **both sides' mirrors**
(the physical battle; D3 writeback applies to both); on front cells the per-cell mirror cap
(`max_mirrored_entities_per_cell`) applies **per side** — the front is the game's showpiece
and is worth the entity density; the global cap and B16 priority still bind (battles rank
first there by design).

One combat round is resolved **per settlement tick** (pipeline phase 7) for every live
battle, computed over the front cells: each side contributes its per-cell share (D26 —
unengaged footprint cells are reserves). **When a defender's front-cell share breaks, that
cell converts to the attacker and the front advances** (defender footprint contracts,
reserves rotate in); symmetric for a repulsed attacker edge. Full-side break → §5.5.

Many-vs-one rules (owner Q, post-close — e.g. 8 footprint-1 counters vs one 3×3): all
attackers of the same defender counter join **one** Battle, each pushing its own front cell
(a 3×3 exposes up to 8 edge cells; the concentric bonus rewards the encirclement, cap +30%).
Same-side attackers take **free edge cells first**; further attackers **join existing front
cells up to the stack limit** (3 per cell) — refusal (`cell_contested`) begins only past
3 × edge-cell capacity (R5: 8 edges × 3 = 24 for a 3×3). **Defender contraction is
contiguous, toward the anchor cell** — a blob
losing edges collapses inward (cross → single cell → normal break), never splits into
orphan fragments. Footprint-1 attackers commit fully (no reserves) — encirclers are strong
in front, brittle from behind.

### 5.3 Round math (normative mechanism, PLACEHOLDER constants)

```
org_effect(org)        = 0.2 + 0.8 × org
side_attack            = Σ counters: base.attack  × strength × org_effect × (1 + 0.05×veterancy)
side_defense           = Σ counters: base.defense × strength × org_effect × (1 + 0.05×veterancy)

defender multipliers   : terrain_def (open 1.0 · forest 1.2 · urban 1.5)
                         × (1 + 0.10 × fortification_level)      [building/faction fort, econ doc]
attacker multipliers   : concentric bonus (1 + 0.10 × (attacking_cells − 1), cap +0.30)
                         (attacking_cells = distinct front cells the attacker side
                          currently pushes — D46 definition)

dmg_to_defender        = k_dmg × side_attack(att) × att_mult / (1 + side_defense(def) × def_mult / K)
dmg_to_attacker        = k_dmg × side_attack(def) × def_mult / (1 + side_defense(att) × att_mult / K)
                         (defender counterfire uses its attack stat — garrisons shoot back)
```

**Per-cell reserves (D26 — combat width, multi-cell counters):** a counter's power is spread
evenly across its footprint; only cells **touching the battle** contribute
(`engaged_power = total_power × engaged_cells / footprint`). Unengaged cells are reserves:
they take no damage until engaged cells are lost, at which point the front rotates inward.
Attacking a big blob from one side therefore fights its edge, not its mass — encirclement is
rewarded arithmetic, not a special rule.

**Mitigation (D27):** damage application, per side, split by weights: `org` takes `w_org`
(0.7) until empty, remainder to `strength` **reduced by the armor factor**
(`str_loss ×= 1 − armor_rating`, template/equipment-derived, cap 0.5); strength loss converts
to manpower casualties (`casualties = str_loss × base.max_manpower`) **reduced by the medical
factor** (`casualties ×= 1 − medical_rating`; the spared fraction returns as wounded —
manpower restored over `wounded_recovery_ticks` instead of dying; wounded survive even counter
shatter and return to the faction pool, D39c). **Combat dead are population dead (A1)**: final
casualties reduce faction population by `war_death_population_ratio` (PLACEHOLDER 1.0) in
the same tick — war costs demography and recruit→die→recruit drains the faction. Exact
capacity arithmetic (including the `1 − k_pop × ratio` labor-slice residual and the
strictly-non-refunding invariant `ratio ≥ 1/k_pop`) lives in econ §2 (SC6). The medical axis
and the wounded pool are the deliberate swing dampers (D39c's principle), not an exemption.
Damage distributes across a side's counters proportional to current strength (the strong soak
more).

### 5.4 Loaded-cell discount (D3, normative)

If any cell participating in a battle is loaded, that battle's abstract damage both ways is
multiplied by `loaded_damage_discount` (0.25) — the real fight (mirrored entities, player
bullets) becomes the dominant term, entering via casualty writeback (§6.3) in the *next* tick's
ingest phase. Abstract resolution never pauses entirely (a stalemate of two AFK mirrors must
still resolve eventually), but it defers to whoever is actually pulling triggers.

### 5.5 Break, retreat, shatter

- A side **breaks** when its aggregate org < `break_threshold` (0.10).
- Broken **defender** retreats: each counter moves to the adjacent friendly-or-neutral,
  non-battle cell **with a free stack slot** (A26) and highest friendly org presence; no such
  cell → **shatters** (counter destroyed; the death roll applies to **able-bodied manpower
  only** — the wounded pool releases intact alongside the survivor share, as population;
  B3, D39c/SC1-consistent; PLACEHOLDER 50% of able-bodied dies, 50% disperses — no POW
  system v1). Retreating counters cannot fight for `retreat_lockout_ticks` (18).
- Broken **attacker** stands down: its edges withdraw from all front cells back onto its own
  pre-battle footprint — **and reversion is atomic (R4): reverted cells restore the
  defender's prior control state exactly** (production hooks reattach; no D29 collapse was
  triggered, because conversion effects are provisional until battle end, §5.6) — and the
  attack is
  called off. An attacker is never forced backward *beyond* its own footprint — under the
  overlap model (D46) it surrenders the contested cells, not its ground. Withdrawn attackers
  observe the same `retreat_lockout_ticks` before initiating again.

### 5.6 Victory, de-control & occupation (D12 — presence ≠ control; timing per D46)

**De-control is per-cell, fires at conversion time — and is provisional until battle end
(R4)**: when a front cell converts to the attacker (§5.2), its `territory/cell_decontrolled`
event fires and production/exchange hooks detach immediately — but the state is **held
provisional**: if the cell later reverts (repulsed attacker, §5.5), the defender's prior
control restores atomically (hooks reattach, no D29 chained-collapse ever triggered by a
provisional conversion; D29 timers arm only at battle end). An enemy core whose cell is
overrun prompts the attacker's per-core choice (capture intact / raze for salvage) **at that
moment, but the choice executes only at battle end** — a raze cannot be banked by a
hit-and-run push that then deliberately breaks; reversion returns the core intact. At battle
end (defender eliminated/retreated), attackers holding an `advance` flag (default on)
**consolidate**: each attacker counter's footprint normalizes to a contiguous shape of its
template size anchored at its furthest-advanced front cell, vacating any excess (the mirror
rule of the defender's contiguous contraction); **consolidation resolves in deterministic
counter-id order, respects stacking limits (A26), and an unplaceable consolidation falls
back to withdrawal to the pre-battle footprint (R6)**. With `advance` off it withdraws to
its pre-battle footprint. Victory strips control but does **not** grant it. A player faction
gains control only through an occupation anchor:

- **occupy-build** — a garrisoning counter with the occupy order constructs an occupation core
  in place (cost `build_materials` X over Y ticks — the RTS-MCV move). **Materialization
  (A3)**: completion registers a `pending_core_placement`; if the chunk is loaded the core
  block is placed immediately, else on next load (`shell/world/` duty, surface-sited). This is
  a declared narrow exception: Chess may write *single blocks in loaded chunks* (core blocks,
  and nothing structure-scale) — exactly the live-side lane `wargame_interface.md` §1 reserves
  ("≤ a handful of blocks and must be visible this session = plugin"). The core audit treats
  pending-placement cells as valid; or
- **player placement** — an authorized member physically places an occupation core block in the
  cell (which is also how a faction claims *peaceful* territory it walked to first). **The D17
  knob must bind here too (A12)**: placement consumes `build_materials` scaled by distance to
  the faction's nearest controlled cell (same curve family as land purchase) and is limited to
  `core_placements_per_round` (PLACEHOLDER 1) — walking far still earns remote claims, but
  core-spam can never undercut the purchase curve; or
- **land purchase** — a cell adjacent to already-controlled territory is annexed for resources
  without a core, chained to the nearest core (economy_and_buildings.md).

Faction **claim capacity** (total controllable cells) is bounded by organization scale
(REVISION_LOG §0.2 D12; scale metric under discussion) — cells beyond capacity can be
de-controlled but not anchored or purchased. An enemy occupation core standing in a captured
cell is captured intact or razed for salvage (attacker's per-core command choice, confirmed
checkpoint 4). **Chained-cell collapse (D29)**: when a core falls, its chained purchased cells
enter a 1-round grace in which the owner may re-anchor them by **discounted** re-purchase
(`collapse_repurchase_discount` 0.5); cells not re-purchased de-control at round end; later
re-claiming a de-controlled cell that contains building assets costs **2×** the normal
purchase price. Infected cells additionally
follow cleansing rules (ai_factions_and_strains.md §4.3) — including the loaded-node
physical-kill requirement. AI factions are exempt from the core rule
(ai_factions_and_strains.md §1.2).

### 5.7 Battle report

On battle end (any cause) a `combat/battle_ended` event carries both sides' initial/final
org·strength·manpower, per-counter casualties, duration, loaded-overlap fraction, and outcome —
the primary MiroFish narrative feed and the input to war-scar work orders (§8).

---

## 6. The Unit Mirror (board → world → board)

### 6.1 Spawn

When a chunk of a counter-occupied cell is loaded, the mirror spawns representative entities:
`entities = ceil(manpower / manpower_per_entity)` clamped by `max_mirrored_entities_per_cell`
(24) and the global cap (200), spawn positions surface-sampled within the chunk. Global-cap
contention resolves by fixed priority (B16): battles > player-faction garrisons > strain
territorial > ambient/ephemeral — heat multipliers can crowd the ambient layer, never a
battle. **Intruder-presence reservation (C15)**: a cell with a non-member player physically
inside gets a guaranteed minimum mirror/resident allocation (`mirror_floor_intruder`, 6)
that preempts every lower-priority allocation globally — the defense always manifests for an
actual intruder; budget saturation (alt accounts parking loaded cells elsewhere) can never
close the HP channel and turtle a core. Provider comes
from `mirror_profile`:

| provider | mechanism |
|---|---|
| `cnpc` | CustomNPCs template spawn (soldier NPCs; loadout/skin per template `unit` id). Whether CNPC NPCs can fire TACZ/superbwarfare guns is an adapter concern under investigation — fallback is CNPC-native ranged attacks with gun props (open question 4) |
| `strain adapter` | delegated to the strain's `spawnUnits` (ai_factions_and_strains.md §5.3), which must census mod-native entities first — never double-spawn |

Mirrored entities carry a `swchess:counter=<id>` tag. The mirror is **presence, not transfer**:
spawning deducts nothing from the counter; only deaths do.

### 6.2 Despawn, top-up & anti-farm rules (A8)

On chunk unload, tagged survivors despawn silently (no writeback — they "return to formation").
On counter movement out of a loaded cell, mirrors walk to the cell edge and despawn.

Four normative lifecycle rules:

1. **Top-up**: while the chunk stays loaded, the mirror trickles back toward its target count
   (`ceil(current_manpower / manpower_per_entity)`) every `mirror_topup_period_ticks` (6) —
   a garrison never sits invisible in a loaded chunk (the inverse D-REALITY hole).
2. **Respawn cooldown**: after mirror casualties, the cell enters
   `mirror_respawn_cooldown_ticks` (12) before top-up or reload-respawn resumes — chunk-cycling
   cannot outpace the abstract bleed. (Note: cycling was never *free* farming — every death
   writes back — the cooldown only removes the burst-rate advantage over fighting the battle.)
3. **No drops**: tagged mirror entities drop nothing — no items, no loadout guns, no XP. The
   equipment printer is closed by rule, not by loot-table luck.
4. **Spawn scatter & safety**: spawn positions are scatter-sampled across the cell, rejecting
   positions inside damage sources (fire, sculk catalysts, kill-box geometry heuristics:
   enclosed 1×1 pits); if no safe position exists, spawn defers a cooldown — a known garrison
   cell cannot be farmed by a trap floor.

**Resident NPCs inherit all four rules (SC3c)**: top-up toward the density target, respawn
cooldown after kills, no drops, scatter/safety — so a siege can never stall by exhausting the
currently-spawned residents while cell HP remains (density is channel width, never the HP
pool; governance.md §7-1).

### 6.3 Casualty writeback (normative)

Death of a tagged entity enqueues `(counter_id, manpower_per_entity)` into the ingest queue;
next settlement tick, the counter loses that manpower + proportional strength, org loses
`org_per_mirror_death` (0.01). Garrison-mirror and resident-NPC deaths inside controlled
territory additionally deduct the cell's **integrity HP** and feed the hostility/
auto-declaration machinery (D42 — governance.md §7). Killer attribution (player kill vs mob kill vs environment) rides
the event (`combat/mirror_casualty`) for logs and veterancy — **a player emptying a magazine
into a sculk swarm is materially reducing that counter**; this is D3's whole point.
Writeback is capped per tick at the counter's remaining manpower (obviously) and the ingest
phase orders events deterministically (by wall-clock then entity id) before applying.

### 6.4 Reconciliation rule

The mirror never *creates* abstract state: an untagged entity (mod-native spawn, observer-mode
infected) is not a counter's business (ai_factions §5.3). If a counter's cell is loaded and its
mirror is entirely dead while abstract battle continues, §5.4's discount already models the
"they're actually all dead" pressure; full abstract elimination follows from writeback, not from
a special rule.

---

## 7. Reinforcement, Training, Upkeep (interface to the economy)

- **Training** (barracks): consumes `train.cost` from the treasury + faction manpower pool;
  progress on the fast clock; emerges at the barracks cell as a fresh counter.
- **Upkeep** (`upkeep_per_tick`): settlement phase 4. Unpaid → org decays (0.03/tick) and
  strength attrits (0.002/tick); starving armies rot before they die.
- **Reinforcement**: counters in friendly territory, not in battle, auto-draw faction manpower +
  equipment to restore manpower/strength at `reinforce_rate` (2 manpower/tick). Org regen
  (0.04/tick) requires upkeep paid; halves while moving.

Economy details in economy_and_buildings.md; this section only pins the coupling points.

---

## 8. War Scars — Battles Leave Marks (Picasso bridge)

After `battle_ended`, if cumulative casualties ≥ `scar_casualty_threshold` (200 manpower) **or**
the battle exceeded `scar_duration_ticks` (90) in an `urban` cell, Chess enqueues a Picasso work
order: `degrade` (bounds = defender cell, mandatory `reason` = battle id + casualty summary) or
`composite_event` for breach-style damage — per `wargame_interface.md` §3's existing vocabulary,
no new Picasso machinery. **Submission-time carve-out (C14)**: registered core-block
positions are excluded from the bounds of every Chess-issued destructive order — the
D42/D44 machine must not be bypassable by Picasso doing Chess's own demolition during a
window. Cap `max_scar_orders_per_window` (5), highest-casualty first; excess
scars are dropped *with a log line* (no-silent-caps). Long occupations (≥ `restyle_rounds` 3)
may enqueue `restyle` for faction flavor. Full bridge duties in integrations.md §3.

---

## 9. Config Keys (added by this doc, all PLACEHOLDER)

`k_dmg` · `K` (defense softening) · `w_org 0.7` · `break_threshold 0.10` ·
`retreat_lockout_ticks 18` · `loaded_damage_discount 0.25` · `manpower_per_entity 5` ·
`org_per_mirror_death 0.01` · `reinforce_rate 2` · `org_regen 0.04` ·
`scar_casualty_threshold 200` · `scar_duration_ticks 90` · `max_scar_orders_per_window 5` ·
terrain/fort/concentric multipliers (§5.3) · `max_path_cells 64` ·
occupy-build cost/duration & land-purchase price curve (§5.6, economy doc).

---

## 10. Open Questions (v0.1)

1. ~~Combat width~~ **Resolved (D26)**: width = per-cell power of a footprint; unengaged cells
   are reserves (§5.3a). Single-cell stacks remain bounded by the stacking limit.
2. ~~Attacker never displaced~~ **Resolved (checkpoint 4)**: accepted.
3. ~~Rally override~~ **Resolved (checkpoint 4)**: v1 no.
4. ~~CNPC + modded guns~~ **Resolved (D27)**: CNPC fires TACZ via scripts; loadout pools per
   faction/unit type. Script implementation is an action item (the provider interface still
   absorbs a custom-entity swap if scripts underdeliver).
5. ~~Naval~~ **Resolved (D23/D28)**: water impassable v1; v0.2 adds equipment-flag passability
   (boats/helicopters/amphibious) — no frontline/invasion planning, ever.
6. ~~Battle math shape~~ **Resolved (D27)**: smooth softening divisor confirmed, now with
   armor (materiel) and medical (manpower/population) mitigation axes.
7. ~~Captured cores~~ **Resolved (checkpoint 4)**: attacker's per-core choice.
8. ~~Chained collapse~~ **Resolved (D29)**: 1-round discounted-re-purchase grace; 2× price to
   re-claim building-bearing cells later.
9. ~~Wounded pool~~ **Resolved (D39c)**: wounded **survive** shatter and return to the
   faction pool. Design principle (owner): population is hard-loss/hard-growth — damp the
   swings, protect player morale.
