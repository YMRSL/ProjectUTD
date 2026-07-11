# Chess — Revision Log & Decision Record

Same role as `../../Picasso/docs/REVISION_LOG.md`: every structural decision, review finding,
and adjudication lands here with a stable number; spec prose stays clean, debate lives here.
Review rounds append lettered sections (A, B, …) following the Picasso workflow.

---

## §0 Founding Decisions (product-owner rulings, 2026-07-08, pre-review)

- **D1 — Carrier: self-authored NeoForge mod** (MC 1.21.1 / NeoForge 21.1.x / Java 21).
  Owner's stated concern: client performance burden. Resolution encoded as doctrine D-SERVER +
  the §10 performance budget: *all* simulation server-side; the client is a render-only map
  terminal (snapshots in, commands out, zero rules). Alternatives rejected: KubeJS assembly
  (cannot build a HOI4-grade map UI; tick-scale settlement in scripts is fragile), external
  engine + bridge mod (real-time coupling and deployment complexity for no v1 gain — note the
  Picasso-side wargame_interface contract works identically either way; an extraction path
  stays open because core/ is Minecraft-free).

- **D2 — Buildings: core block + dual-path physicalization.** Every functional building is
  anchored on a physical core block carrying its numbers. The structure around it arrives by
  either path: (A) player self-builds, places the core block, Chess registers it; (B) player
  applies in the UI → construction rounds accumulate → Chess emits a `construct` work order →
  Picasso materializes during a maintenance window (the wargame_interface §3.1 flagship chain).
  Rejected: work-order-only (kills the self-built-base-to-legitimacy path), pure-abstract
  buildings (violates the alignment requirement).

- **D3 — Combat duality: entity casualties write back into counters.** Unloaded cells resolve
  purely abstractly every settlement tick; loaded cells spawn budgeted mirror entities, entity
  deaths write back at `manpower_per_entity` rate, and abstract damage in loaded battles is
  discounted (`loaded_damage_discount`) so the real fight dominates. Rejected: abstract-layer
  supremacy (player kills would be meaningless), full freeze-on-load (edge cases: partial loads,
  AFK players parking a chunk to stall a war).

- **D4 — Documentation language: English**, matching the Picasso corpus (cross-layer citations
  stay term-exact; the implementing agent's working language is consistent). Review discussion
  happens in Chinese as usual.

- **D5 — (inherited ruling, 联动待办 L4, 2026-06-28) Infection mods become driven engines.**
  sculkhorde/spore get rewritten: autonomous hive/gravemind AI default-off; four API families
  exposed (infest / spawn / structure / raid) for external drivers — explicitly including this
  wargame. Chess therefore specifies two adapter modes: `observer` (pre-rewrite, mod autonomous
  in loaded chunks, Chess mirrors) and `driver` (post-rewrite, Chess is the brain). Staged per
  strain via `strain_mode.<strain>` config.

- **D6 — Infected sides are instances of a general AI-faction concept** (owner clarification
  mid-draft, 2026-07-08): AI factions are factions computed by the system; the infected ones are
  merely natively hostile (`hostile_all`). Binding constraint: **low performance cost — simple
  rule-driven behavior, no heavy AI decision-making.** Encoded as doctrine D-CHEAPBRAIN and the
  behavior-pack framework (fixed predicate/action vocabularies, budget economy, slow decide
  cadence, frontier sampling, dormancy). v1 ships only the three strains as data; the framework
  carries no hostility assumption.

## §0.1 Workflow notes

- Doc-first, code-later (established SocialWill practice); balance numbers are PLACEHOLDER
  config values by doctrine (D-BALANCE) — reviewers attack mechanisms, the owner sets numbers.
- Owner works in checkpoint cycles: partial doc set → discussion → rulings land here as
  numbered decisions → next batch. Open questions accumulate in each doc's tail section and
  §15 of ARCHITECTURE.md.
- **Freeze checklist rule (from A10):** before any freeze or round-close, the Alignment
  Ledger (integrations §2) is diffed against every ruling batch since the last freeze —
  the central table failing silently is a doctrine failure, not a typo.

## §0.2 Checkpoint-1 Rulings (product-owner answers, 2026-07-08, applied in draft 2)

- **D7 — Resource taxonomy.** Stocks: **build materials 建材 / raw materials 原料 / equipment
  装备 / food 食物**. Energy: **power 电力 / fuel 燃料 (coal, oil)**. People: **population is
  the cap of manpower**; nearly every action spends manpower. Gauges: stability, unity, plus
  **political points 政治点** — spent to enact policies (policies touch stability, production
  efficiency, member withdrawal quotas). Currency is barter-leaning with **three per-bloc base
  currencies**: 瓶盖 bottle caps (financial), 黑面包 black bread (physical commodity),
  粮票 grain tickets (financial anchored by physical). Mapping to the three polities: **pending
  (checkpoint-2 question)**.
- **D8 — Exchange is bidirectional**, with leader-set **per-player per-round withdrawal
  quotas** (round = post-maintenance server start). Three member-access models exist —
  apply-to-use (申请使用), deposit-to-use (存储使用), and interest-bearing deposits
  (定存/活期, interest from faction productivity) — mapping to blocs/polities **pending**.
- **D9 — Focus trees (国策).** One mainline per polity, auto-attached at faction creation;
  **all factions of the same polity share focus progress** (bloc-wide); branch focuses resolve
  **first-completed-wins**; the tree merges HOI4-style focus *and* tech into one. Runtime is
  FTB Quests (owner asked fork-vs-integrate; recommendation: integrate via API + thin binding
  layer — see checkpoint-2 summary; final call pending).
- **D10 — Map fidelity.** Block-accurate terrain base is **required** (navigation legibility);
  performance answer: **import the owner's already-generated Xaero world-map data** as a
  pre-rendered tile pyramid; **no HUD minimap — fullscreen strategic map only**.
- **D11 — Strain mods as tools.** sculkhorde/spore get their intelligence removed at source
  level and become callable tool libraries (chunk infection, structure generation, spawns,
  raids). These live calls answer to Chess's management only; the wargame_interface
  structure/window boundary governs Picasso writes and does not bind strain mods.
- **D12 — Occupation cores (presence ≠ control).** A counter covering a cell does not control
  it. Control requires an **occupation core** (RTS-MCV analogue): counter occupy-build order or
  player-placed core block; alternatively **land purchase** annexes core-adjacent cells without
  a new core. Total claimable area is capped by organization scale (metric pending).
- **D13 — AI faction archetypes & cap.** Wandering survivor bands, small settlement survivors,
  infected strains; **≤ 16 AI factions**.
- **D14 — Combat simplification.** No entrenchment; damage model deliberately simpler than
  HOI4 (no armor axis — players can't field armor fleets).
- **D15 — Round/offline semantics** (closes ARCHITECTURE §15 Q1): rounds advance with
  maintenance windows; per-round quotas reset at post-maintenance server start; fast-clock
  items freeze while the server is down.

## §0.3 Checkpoint-2 Rulings (product-owner answers, 2026-07-08)

- **D16 — Intra-bloc war gate.** Focus progress stays bloc-shared. A same-bloc faction may be
  declared war upon only **while its unity < 60%**. Scope confirmed at checkpoint 3 (narrow
  reading): the gate binds *intra-bloc* declarations only; cross-bloc declaration is free of
  the gate but carries D21 costs.
- **D17 — Core coverage: one core, one cell.** All expansion beyond core cells is land
  purchase from controlled-adjacent cells; the purchase price curve is the expansion-speed knob.
- **D18 — Currency/polity/access mapping.** Dictatorship = 黑面包 black bread + apply-to-use ·
  Republic (presidential) = 瓶盖 bottle caps + interest accounts (定存/活期) ·
  Democratic centralism = 粮票 grain tickets + deposit-to-use.
- **D19 — Organization scale & the manpower-allocation mechanic.** Claim capacity is a hybrid
  formula: base + player-member term whose coefficient incorporates **population** and each
  member's **focus-contribution score** (contributions to bloc focus unlocks are scored per
  player and feed the coefficient) + policy-center level bonus + focus-unlock bonuses.
  Additionally: **land purchase, construction, and recruitment all consume manpower**;
  recruiting a counter carves its manpower out of the faction manpower *cap*; a counter
  garrisoned on its own faction's territory adds its manpower back to that territory's
  manpower ceiling (soldiers double as laborers at home — deploying abroad is what costs you).

## §0.4 Checkpoint-3 Rulings (product-owner answers, 2026-07-08)

- **D20 — Item alignment source.** Exchangeable goods and equipment reference the owner's
  planning workbook `ProjectUTD/策划案以及文档相关/ItemNameCatch分类汇总_合成设计方案_v1.xlsx`
  (category sheets: 基础资源 · 组件 · 可制作近战武器 · 枪械 · 装备 · 实用物品 · 工作台, plus the
  junk sheets 所有可拾取的垃圾表格一览 / 垃圾拆解接口建议). `exchange_table.json` and
  `scavenge_blocks.json` seeds are generated from it together with the DDF-283 list.
  Also confirmed: **both infection mods' source is in hand** — D11's source-level castration
  is feasible as specified.
- **D21 — Cross-bloc war costs scale with favorability.** Cross-bloc declaration is free of
  the unity gate but hits the declarer's **stability and unity**; magnitude scales with the
  inter-bloc **favorability 好感度** (new bloc-pair gauge). How favorability rises and falls
  is undesigned — owner TBD (open-question batch).
- **D22 — Interest is treasury-paid; reserves and crashes.** Republic interest is paid from
  the faction treasury, disciplined by a **reserve requirement** (存款准备金) on member
  deposits so every player's interest is payable; leader mismanagement can breach reserves →
  missed payments → **bank run** (financial-crisis state). Deliberately crashable — and
  focus-tree-level rescue measures must exist (bailout focus effects).
- **D23 — Batch confirmations.** Abstain-counts-as-no confirmed · water cells impassable
  confirmed (v1) · republic presidencies get **terms: a scheduled election every 5 rounds**
  (recall motion unchanged).

## §0.5 Checkpoint-4 Rulings (batch answers to the 33+2 open-question list, 2026-07-08)

Defaults confirmed without comment: Q3 (modid) · Q5 · Q6 · Q10 (attacker per-core choice) ·
Q13 · Q14 (`zhanghai_mitu`) · Q17 (core grace) · Q19 · Q20 · Q21 · Q26–Q30 · Q32 · Q33 ·
Q34 (contribution scoring v1 + registry re-read). Non-default rulings:

- **D24 (Q1) — Variable footprint & stackable NPC counters.** Footprint varies by counter type
  and scale. New counter class: **special-NPC counters** that may *stack on top of* other
  counters' cells; they spawn a strictly limited number of special NPCs (tradeable,
  quest-giving); the counter itself is the **memory record** for the NPC's dialogue/quest
  state; content supplied by the (future) SocialWill narrative system.
- **D25 (Q2) — Decorative CNPC residents: yes.** Provided by the narrative system,
  **default-off** feature flag, interactive NPCs, density config option.
- **D26 (Q4) — Combat width redefined as per-cell reserves.** Width = a multi-cell counter's
  average per-cell combat power; cells not touching the battle are **reserves** (预备队).
- **D27 (Q7, Q9) — CNPC gunners & mitigation axes.** CNPC soldiers CAN fire TACZ weapons via
  scripts; loadout restricted by faction and unit type. Damage model gains **armor** (reduces
  strength/materiel loss) and **medical** (reduces manpower/population loss — wounded recover)
  mitigation terms.
- **D28 (Q8) — Naval/heli/amphibious: v0.2, radically simplified.** No frontlines/invasion
  arrows; an equipment flag on the counter ("has boats/helicopters") grants passability.
- **D29 (Q11) — Core collapse, refined.** 1-round grace with **discounted** re-purchase of the
  orphaned cells; unpurchased cells de-control after the round; later re-claiming a cell that
  contains building assets costs **2×** purchase price.
- **D30 (Q12) — No raid planning; player waypoint queues.** Counters are pawns; players get
  RTS-style multi-waypoint path queueing (shift-click).
- **D31 (Q15 + Q35) — Strain doctrine & playbooks.** Strains fight each other. **Sculk and
  Spore are strong-territory, strongly expansion-capped; zhanghai_mitu is the map-wide ambient
  common infected.** Full behavior specs recorded (owner-authored, encoded in
  ai_factions_and_strains.md §6): Spore = hunt (biomass from wildlife/zhanghai/high-pop
  factions; window-time value-ranked target choice; discounted-but-weaker anti-player raid
  counters) / expand (random adjacent purchase, consolidation preference) / farm (consume
  adjacent infected-farmland counters → biomass) / **assault** (admin-designated region +
  intensity, admin UI panel + external API; Spore counters only attack/not-attack; the Spore
  *faction* has favorability — low → attacks, high → holds). Sculk = one physical core region
  (not capturable/destroyable at Chess layer — only physically breaking the ancient sculk node
  kills it; admin can resurrect/reset), mind-point economy (non-Sculk deaths in infected
  regions feed it) / **ink-splash** spread (≤3 deploys per window, ≤10 regions, each grows in
  live time to ≤21 cells, node structure at center, destroying it removes the region+annexes;
  ≤2 active regions → deploys free) / **hotspot** (battle sites get infection cores, ≤6) /
  **lurk** (player-invisible admin-visible 1-cell seed at a high-population camp; passive
  sculk phantom; ~1 block/5 s infection; ≥200 blocks → converts to ink-splash node; cleansing
  ladder 80→50, 50–80→20, <50+purification → fails; cells within 3 of a purification node are
  never selected). Two **ephemeral counter types** regenerate at each round end: wildlife
  (map-wide noise placement) and infected farmland (around Spore territory).
- **D32 (Q16) — Food storage & spoilage.** Food has storage caps; food not moved into storage
  within 1 round spoils.
- **D33 (Q18) — Physical energy bridge.** Powered cells energize designated mod blocks
  directly (e.g. furnaces burn without coal — owner will supply the block list); any
  coal-burning block auto-draws Chess-layer stock coal (coal = deposit-type resource);
  Create fluid tanks holding fuel oil auto-fill at a rate from Chess liquid fuel (tank-insert
  API), and over-supply reverses into Chess credit; powered chunks inject power into the
  **first-placed Create: New Age conductive terminal** per chunk, and excess injected power
  feeds back to Chess. Bidirectional by design.
- **D34 (Q22, Q23, Q24, Q25, Q31) — Batch.** Kick refund: converted by exchange rate,
  importance and proportion **into the player's ender chest**. Favorability drivers: all four
  candidates adopted (trade +, focus stances ±, war casualties − decaying, returned territory/
  honored ceasefires +). Fog: extra grey mask animates the visible→snapshot transition.
  Intel trading (sell own snapshot of chosen cells) + alliance info-sharing permissions.
  Economy digest every **40 settlement ticks**.
- **D35 (Q35) — Window lockdown authority.** Chess manages maintenance-window entry via admin
  commands + API: non-admin players are kicked and barred during the window. Round-end
  computations (ephemeral counter regeneration, Spore hunt target ranking, Sculk deploys)
  run in the **lockdown phase** while the server is still up; Picasso still requires the save
  closed (frozen contract §1) — lockdown is the airlock, not a license to edit a live save.

## §0.6 Checkpoint-5 Rulings (2026-07-08)

- **D36 — zhanghai final form + activity heat meter.** The density-field encoding is accepted
  (zhanghai owns no cells; ambient noise field + ephemeral roamers). Additionally: zhanghai
  monsters in unwatched areas are **despawned outright and lazily regenerated** when players
  load the chunks. New cross-strain mechanic — the **player activity heat meter** (0–100,
  per player): gunfire, sprinting/jumping, and block-breaking raise it (decays over time);
  inside zhanghai/spore/sculk-controlled cells, spawn budgets scale with the entering player's
  heat, and freshly spawned monsters **auto-target players whose heat > 60**.
- **D37 — Window-time unit-composition tuning.** At window entry the narrative-layer AI reads
  the round's player-vs-strain combat logs (`chess_log` combat topic) and adjusts the monster
  composition that Spore/Sculk attack counters will spawn (different monsters answer different
  player tactics). Same integration pattern as hunt targeting: **deterministic default
  composition weights; narrative AI adjusts via API when present; never a blocking dependency.**
- **D38 — Energy-bridge items pinned** (extracted from the owner's reference save inventory):
  power interface `create_new_age:electrical_connector` · liquid-fuel interface
  `create:fluid_tank` · liquid fuel `createdieselgenerators:ethanol_bucket` (ethanol family) ·
  designated energized consumer `superbwarfare:charging_station` (auto-charges in powered
  cells). Owner correction recorded: the bridge is **auto-feed, not fuel-waiver** — powered
  cells auto-fill designated consumers (charge, coal, fluid), they don't nullify costs.

## §0.7 Checkpoint-6 Rulings — final pre-freeze (2026-07-08)

- **D39a — Unified favorability table confirmed** (`(bloc|ai_faction) × (bloc|ai_faction)`).
  AI-side driver: **completing an AI faction's NPC quests raises favorability with it** —
  requires an interface giving CustomNPCs a faction affiliation and making quest-completion
  records countable by Chess (cross-layer ask added, integrations §8).
- **D39b — Lurk fog exception confirmed** (admin-only board visibility; physical counterplay).
- **D39c — Wounded survive shatter.** Medical-spared wounded are NOT lost when a counter
  shatters; they return to the faction pool. Rationale (owner, design principle worth quoting):
  population is hard-loss/hard-growth — avoid large swings, or player morale explodes.

**v0.1 FROZEN 2026-07-08.** All docs status-stamped; round-1 adversarial review is open.

## §0.8 Round-1 Closure Rulings (2026-07-08)

- **D41 (closes A30) — Exclusivity-lock griefing floor.** A bloc-wide branch lock triggers
  only when the completing faction has **≥ 5 members** (`focus_lock_min_members`) and is
  **≥ 2 rounds old** (`focus_lock_min_age_rounds`); sub-threshold completions grant
  faction-local effects without locking the bloc's sibling branches.
- **D42 (refines A5, supersedes the hardened-core pricing) — The cell-integrity siege gate.**
  Every controlled cell carries **integrity HP** derived from the manpower its territory
  supplies; **killing resident NPCs or garrison mirrors in faction territory is itself the
  war declaration** (auto-`at_war`, full D16/D21 legality and costs; factionless attackers
  flagged hostile); each such kill deducts cell HP *and* the owner's population
  *(SC1 correction: originally recorded as "manpower" — manpower is a derived quantity;
  the debit lands on population and the cap shrinks with it)*; **core blocks
  are unbreakable while their cell's HP > 0** — decapitation is a siege through the
  defenders, never a lock-pick past them. D25's resident NPCs become load-bearing when
  enabled (garrison mirrors are the only HP channel when the feature is off). A6 DM-view
  exemption: **owner ACK'd**.
- **D43 (new scope, owner-recalled requirement) — The world boundary tool.** A DM/admin tool
  to draw and manage the map's overall outline at cell resolution (painted on the admin
  strategic map or via `/chess boundary`), binding **both worlds**: the board (cells outside
  never materialize; counters, missions, strains, ephemerals, ambient field all clip to it)
  and players' physical movement (custom enforcement layer — warning → pushback → damage,
  config — since vanilla WorldBorder is square-only). Stored in SavedData, live-editable,
  API-exposed.

---

## §A Round 1 — OPEN (2026-07-08)

Attacker: second Fable session. Format: numbered findings A1, A2, … — author responds per
item (accept → doc edit noted here · rebut → reasoning here, spec stays clean). Same protocol
as the Picasso REVISION_LOG rounds.

### Round-1 findings (attacker session, 2026-07-08)

Scope compliance: no numeric values attacked (D-BALANCE); §0.x rulings not re-litigated.
Findings marked **[D-consequence]** target an unstated implication of a ruling, never the
ruling itself. 30 findings: **6 HIGH · 17 MED · 7 LOW.**

**A1 (HIGH — economy_and_buildings §2 · counters_and_combat §5.3/§7 · D27/D39c).**
Combat deaths refund the manpower cap. `available_manpower = cap − Σ reserved − Σ recruited
counters' manpower + Σ garrisoned-at-home`, and population is reduced only by "raids reaching
population, starvation". So when a counter takes casualties, the `Σ recruited` term shrinks and
the freed slice returns to `available_manpower` — arithmetically identical to disbanding.
"Casualties … are gone for good" is false at faction level: war costs food/equipment/time but
zero demography; recruit→die→recruit cycles never touch population. This contradicts D27's own
wording (medical reduces "manpower/**population** loss", implying unspared dead do cost
population) and the D39c hard-loss rationale. Pick one: dead reduce population; or a cumulative
`war_dead` term stays subtracted from the cap; or state explicitly that combat deaths are
demographically free and delete "gone for good".

**A2 (HIGH — board_and_fog §3/§4 · economy_and_buildings §6 · integrations §2).**
Loot depletion is resurrected by sensing. Missions deplete abstract `loot_richness`, but Chess
removes no physical blocks, and the LootScanner ("refined on load … counting matches") is a
recurring sense-queue item — any later chunk load re-derives richness from the untouched blocks
and the depletion vanishes. Consequences: alternate missions with chunk visits for unbounded
yield; abstract missions + hand-looting double-dip one stock. The ledger row names the source
list but no reconciliation *direction*. Needs a per-cell harvested ledger
(`richness = min(scan_estimate, initial − harvested)`) or an explicit override rule.

**A3 (HIGH — counters_and_combat §5.6 · integrations §8 ask 1 + §2 territory row ·
board_and_fog §4).** Occupy-build cores cannot become physical. §5.6 lets a garrisoning counter
"construct an occupation core in place", but Chess writes no blocks, mirrors spawn only
entities, and ask 1 rules occupation cores "single block, **path A only** — no Picasso
template". A counter-built core therefore has no physical block, and the ledger's own
reconciliation ("core audit on load; core destroyed → de-control") de-controls the cell on
first load. Either specify a physicalization mechanism (a place-core-on-load duty in
`shell/world/` — a world *write* the doctrine must then own explicitly), or route occupy-build
through a work order, or drop occupy-build.

**A4 (HIGH — map_interface §2/§5/Q2 · board_and_fog §6 · ARCHITECTURE §9).**
The terrain layer bypasses fog three ways. (1) The full-world tile pyramid ships in the modpack
while fog promises `unknown` = "black; **not even terrain**" and §9 claims "a client never
receives data its faction cannot see (anti-cheat by construction, not by obfuscation)" — for
terrain it *is* obfuscation; a modified client renders the whole map. (2) The tile-sync channel
(`TileVersions`/`TilePatch`) has no fog filter: patches triggered by receipts/war scars are
fetchable for never-explored regions, leaking *where and when* the world changed. (3) Honest
clients render current tiles under `explored` cells, contradicting §6's "memory snapshot … at
the tick visibility was lost" — players watch scars appear inside their fog. Either declare
terrain non-secret (and fix §6/§9 wording, fog-gating only patch *timing*), or fog-filter tile
versions per faction — but say which.

**A5 (HIGH — economy_and_buildings §4 · counters_and_combat §5.6/§6 · board_and_fog §4/§7 ·
governance §7).** War legality gates counters, not players. All control and production hangs on
physical core blocks; the core audit *expects* breakage; no text states who may break a core
block or any protection/wartime gate. A lone player — no counter, no declaration — walks into
enemy territory, mines the occupation/faction core (→ de-control cascade / decapitation
crisis), or farms mirrored garrisons (writeback needs no war state). D16's unity gate and D21's
declaration costs are decorative against the cheapest physical strategy. If covert sabotage is
*intended* gameplay (defensible), the docs must say so and price it (detection events,
stability/unity consequences, peacetime protection); if not, specify the protection model.

**A6 (HIGH — ai_factions_and_strains §6.2/§6.3 vs §2.3 · board_and_fog §6).**
Round-cadence strain rules are omniscient; doctrine forbids it. §2.3: "AI factions get no fog
exemption beyond their own vision"; board §6: "no omniscience anywhere". Yet Spore hunt ranks
"high-population player factions" (population is faction-private), Sculk lurk picks "a
high-population camp", hotspot enumerates "player-faction battle sites" Sculk never saw.
"Public-estimate stats" is undefined for these inputs — the only defined public estimate is the
strength band of *visible* cells. Either carve an explicit doctrine exception ("window-time
playbook targeting is DM-omniscient by design", fairness implications stated), or vision-gate
targeting and accept dumber strains.

**A7 (MED — counters_and_combat §5.1–§5.2 · ai_factions §1.1 · D31).**
Battles are two-sided; the world is not. `hostile_all` strains are at war with each other (D31)
and with everyone, so ≥3 mutually hostile parties over one defender cell is guaranteed (a
player attacks a cell Spore is already attacking; a zhanghai roamer wanders in). "Joins a
Battle" assumes one attacker side; side assignment among mutually hostile attackers, targeting,
and damage routing are undefined. Specify: concurrent separate battles, a free-for-all model,
or entry-denial while a battle runs.

**A8 (MED — counters_and_combat §6.1–§6.4).** Mirror lifecycle has four holes. (1) No
replenishment: spawn triggers on chunk-load only; wipe the mirrors while the chunk stays loaded
and the counter's surviving manpower sits *invisible in a loaded chunk* — the inverse of
D-REALITY's promise. (2) Chunk-cycling respawns fresh mirrors indefinitely (spawning "deducts
nothing"), enabling drip-farming a garrison with zero abstract exposure. (3) Drop policy
unspecified: mirrors carry TACZ loadouts — if entities drop items, cycling is an equipment
printer. (4) Spawn positions are "surface-sampled": kill-box traps over known garrison cells
farm writeback passively. Needs: top-up rule, per-cell respawn cooldown, no-drop rule, spawn
scatter.

**A9 (MED — economy_and_buildings §3.1 vs §7.2 · D2 path B).** The energy bridge is an
unquota'd treasury tap with no ownership check. Coal auto-draw, tank fill, and charging feed
*whoever owns the device*; placement is not permission-checked, so any member — or an intruder
placing a tank in a powered cell — converts faction `coal`/`liquid_fuel`/power into extractable
items, bypassing D8 quotas and apply-to-use entirely. Separately, "registered at placement
(block events)" misses devices that arrive without place events: Picasso-materialized path-B
interiors (window edits fire no events) and pre-existing blocks — identical furnaces behave
differently by provenance. Needs device placement permissions + per-device draw quotas (or
leader-designated devices only), and a receipt-driven registration pass for path-B builds.

**A10 (MED — integrations §2 vs economy_and_buildings §3.1 · D33/D38).** The Alignment
Ledger — the normative D-LEDGER artifact — is stale against the rulings it froze with. Its
`power` row says "none v1; Create-network adapter is a v0.2 extension (econ Q4)" while econ
§3.1 ships the v1 CNA terminal sync with a pinned id (D38); its `fuel` row says "biodiesel,
biogas" while D38 pins the ethanol family; `charging_station` appears in no row. If the central
table can contradict adjudicated content at freeze time, D-LEDGER is not functioning. Fix rows;
add a freeze-checklist rule: the ledger is diffed against every checkpoint batch.

**A11 (MED — governance §2 · economy_and_buildings §4/§5).** Founding grants no territory and
the HQ cell is annexable. `faction_core`'s function list carries no claim; land purchase
requires adjacency to *controlled* territory, so a new faction controls zero cells until it
separately places an occupation core. Meanwhile an adjacent faction may legally *purchase* the
cell containing the faction core (it is "unclaimed"), after which path-A rules ("idles until
the cell is anchored") arguably idle the founding seat — treasury captured by purchase, no war
declared. Specify: founding claims the core's cell (capacity-exempt?), and whether a cell
containing a foreign `faction_core` is purchasable.

**A12 (MED — counters_and_combat §5.6 · economy_and_buildings §5 · [D17-consequence]).**
Two expansion paths, one knob. D17 makes the purchase price curve "the expansion-speed knob",
but player-placed occupation cores have no adjacency requirement and no stated cost
relationship to the curve — walk a member out, drop cores, and super-linear distance-priced
expansion becomes linear core-spam bounded only by claim capacity. Not attacking the
(PLACEHOLDER) core cost — attacking the missing *invariant*: nothing states core placement must
be gated (adjacency, distance pricing, per-round quota) for the D17 knob to bind.

**A13 (MED — counters_and_combat §5.6 · economy_and_buildings §10).** Claim capacity only
gates acquisition. "Cells beyond capacity can be de-controlled but not anchored or purchased" —
nothing ever sheds them. With `k_member × Σ players` in the formula, invite→claim→leave locks
in over-capacity territory permanently, zero consequence. Specify over-capacity pressure
(stability drain, per-round auto-de-control of farthest cells) or enforce capacity at member
loss.

**A14 (MED — economy_and_buildings §6 · counters_and_combat §3/§5.1).** Scavenge missions
ignore the war-legality model. Target legality is "explored, within range, richness > 0" — no
ownership check, no `at_war` gate — while counters need a declaration to even *enter* a foreign
cell. Abstract mission manpower thus extracts resources from foreign-controlled cells as
undeclared theft; and the escort clause ("unless a counter garrisons the cell") is incoherent
against hostile targets — garrisoning a foreign cell is exactly what requires war. Align
mission legality with movement legality, or declare in-claimed-territory scavenging an act with
diplomatic consequences.

**A15 (MED — governance §4–§6).** Motions vs membership churn. Eligibility snapshots at
creation, but: does kicking an eligible voter shrink the denominator (incumbent purges the
electorate mid-recall — kick powers rest with the leader being recalled) or leave a ghost
voter? Do already-cast ballots survive the caster's kick? Joiners mid-motion are presumably
ineligible — say it. The unity penalty on kicks is a soft brake, not vote math. Specify frozen
denominator + frozen ballots (kick never erases a cast vote), and consider gating kicks while a
recall against the kicker is open.

**A16 (MED — ai_factions §6.1 · ARCHITECTURE §5 invariants 2–3).** Ephemeral counters break
stated invariants. Invariant 2: every counter belongs to exactly one faction — wildlife /
infected-farmland belong to whom, and does that owner count against `max_ai_factions` (D13)?
They are "visible, attackable" — with what stats, org, retreat semantics; does attacking one
open a Battle against a faction with no behavior pack? Their placement band also materializes
cells via a third trigger invariant 3 doesn't list ("explored or strain-touched"). Cheap fixes
(reserved cap-exempt `env` pseudo-faction; no-retreat instant-shatter combat profile; invariant
3 amended) — but they must be written.

**A17 (MED — ai_factions §6.3a/§6.3b/§5.2/§4.1 · D36 text · map_interface §3 ·
ARCHITECTURE §8).** zhanghai's board representation is undefined. It "owns no cells", yet
D36/§6.3b scale spawns "inside zhanghai/spore/sculk-controlled cells" (zhanghai-controlled is
undefined); §5.2 gives it lair "nodes" while node flags exist only on strain-controlled cells
(§4.1); its income formula (`per_cell`, `per_node`) degenerates to `base`; the "ambient density
field" has no schema, no named place in ChessSavedData, no fog/visibility rule, and no
Infection-layer rendering (map §3 draws "strain territory by intensity ramp" — zhanghai has
none, so the ambient threat is invisible on the strategic map). Define the field as a
first-class board structure with save/fog/UI story, and zhanghai's income/node semantics.

**A18 (MED — ai_factions §6.4 · D35 · wargame_interface §2/§5 · integrations §3.2).**
Three different "round ends", and they can disagree. Round-end computations key on *lockdown*;
the round number increments at *window_log close* (Picasso-side); quota resets key on
*post-maintenance start*. An aborted maintenance (lockdown, then no window — or a crashed
window with no close record) re-runs round-end computation for the same round at the next
lockdown: ephemeral counters re-roll twice, Sculk gets double deploys (the cap is even *named*
per-round while *prosed* per-window: "`sculk_deploys_per_round` (3) … per window"), orders
flush twice with the same `issued_round`. Relatedly, the receipt loop never states how
`status: "in_progress"` receipts (visible at server start after a crashed window; quarantined
only at the *next* window per contract §2) are classified — they must read as pending, not
terminal. Specify: round-end computations record the round they ran for and refuse to re-run;
caps keyed to round numbers, not lockdown events; in_progress handling.

**A19 (MED — ai_factions §3/§1.1 · counters_and_combat §7).** AI counters have decay but no
regeneration. Org regen is defined only for player counters ("requires upkeep paid"); AI
counters "decay org slowly outside owned territory" and are merely "free" inside — no regen is
ever granted. Wanderers own no territory → permanent decay to org≈0 → any battle instantly
breaks them, contradicting §1.1's "defend themselves, and flee". Strain raid counters decay
along the whole corridor and arrive pre-broken. Specify AI org regen (in-territory regen,
budget-paid heal action, or a decay floor).

**A20 (MED — ai_factions §4.3 vs §6.3).** Region-node removal rules conflict. §4.3 (general):
"unloaded node cells fall to abstract siege normally". §6.3 (sculk playbook): "physically
destroying the region's node removes the region and its annexes" — framed as the removal path.
Unreconciled: can counters abstractly siege an unloaded ink-splash node cell; if so, does the
whole region + annexes fall or only that cell; and what happens to the physical node structure
still standing (purge-on-load? permanent `scarred_infested`?). The ancient-node exception is
clear; the ordinary-region path is contradictory.

**A21 (MED — event_log §3/§6 · board_and_fog §6 · governance §8).** The `visibility` taxonomy
is unspecified and the one worked example leaks. Only governance states a rule; the envelope
example marks `combat/battle_ended` — with cell coordinates — `public`, and the in-game log
browser serves every `public` event to every faction: factions with no vision learn battle
locations, outcomes, and control changes map-wide. Specify per-type visibility in each doc's
Events section (the envelope spec explicitly declines to own payloads — then someone must own
visibility), defaulting to faction-scoped unless argued public.

**A22 (MED — counters_and_combat §3/§5.1 · economy_and_buildings §5 · board_and_fog §6 ·
D-REFUSAL).** Refusals leak fog-hidden state — or war legality collapses; pick one. Moving into
a foreign faction's cell "requires at_war" and illegal moves are "refused at command time"; but
under stale fog the mover believes the cell neutral. Refusing reveals present-tense control
(and the owner) without vision; allowing it as attack-into-fog bypasses declaration legality.
Same for `purchase` against a secretly-claimed cell. Define fog-consistent validation: validate
against the requester's *snapshot* and let actions fail-forward in-world, or return a
deliberately fog-flat refusal code (`cell_unavailable`). The current text promises both
specific reasons and fog integrity; they are in tension.

**A23 (MED — economy_and_buildings §7.4/§3 · governance §6 · ARCHITECTURE §12.3 ·
[D15-consequence]).** No degraded story for stalled rounds. §12.3 covers Picasso *absent*;
nothing covers windows simply not running for weeks — during which quota resets, spoilage,
interest, scheduled elections, ephemeral re-rolls, and notably the *bank-run exit condition*
("one full round's interest paid on time") all freeze. A republic in bank-run during an ops gap
cannot recover by any in-game action. Either declare this accepted fiction in §12.3 (rounds are
the only political clock; ops owns the pace) or give the few recovery-critical items a
wall-clock fallback.

**A24 (LOW — ARCHITECTURE §3 diagram + §4 client line vs §10/D10 · map_interface §1).**
Minimap remnants: §3 says "client/ Map Client — HUD minimap · fullscreen strategic map"; §4
says "minimap HUD". D10/§10/map doc: "no HUD minimap", "no HUD element at all in v1".
Editorial — but the authority document's own diagram contradicts a ruling.

**A25 (LOW — economy_and_buildings §4 · ARCHITECTURE §4 · integrations §8 asks 1/5).**
Building bookkeeping mismatches: the econ table has 10 rows; ARCH §4 and ask 1 count "9 specs
(8 functional + faction_core)" — where does `occupation_core`'s data (cost, item form) live?
Same seam: how players *obtain* core-block items at all (craft? starter kit? exchange rows?),
and whether path A charges any resource cost — as written, self-build skips the treasury cost
path B reserves; if intended (labor is the price), state the asymmetry.

**A26 (LOW — counters_and_combat §2.4/§3/§5.5).** Stacking vs movement/retreat: nothing says a
move into a cell at `max_counters_per_cell` is refused; retreat destination selection ignores
the stack limit — a broken side may find all candidates "full" (shatter despite adjacent
friendly cells?). Specify both.

**A27 (LOW — governance §5).** Ticket approval staleness: approval can land up to the TTL
later; the doc reserves *costs* but never states that the parked command re-validates game
state at apply time (target captured meanwhile, building gone), nor what the requester sees if
apply-time validation fails after approval.

**A28 (LOW — ARCHITECTURE §12.4).** "Admin commands are mirrored by an external API surface" —
no authentication/authorization/trust-boundary treatment, in contrast to wargame_interface §7's
explicit two-writer story. One sentence (local-only socket / ops token; every call logged
`actor: admin`) closes it.

**A29 (LOW — ARCHITECTURE §2 · integrations §5 · player_activity_pipeline §2).** Stack naming
drift: the frozen pipeline describes the recorder as "Fabric mod or Paper plugin, matching the
UtilWeDie server stack"; Chess is a NeoForge mod claiming that duty (correctly, per "one
artifact"). The external line is stale — needs a cross-layer reconciliation ask (their §8
pending-edits list), since that contract is what the recorder is "developed against".

**A30 (LOW — integrations §4.2 · [D9-consequence]).** First-completed-wins has no griefing
floor: a 1-member throwaway faction can burn an exclusive branch for its entire bloc (locking
the sibling map-wide). Not re-litigating D9 — flagging the absent mitigation surface (minimum
faction age/size to trigger bloc-wide exclusivity locks, or leader confirmation). Owner may
rule "working as intended"; it should be a ruling, not an accident.

*(End of round-1 findings. Author responds per item below.)*

---

### Round-1 defender responses (author session, 2026-07-08)

**Verdict summary: 29 accepted (A2 via owner ruling D40; A8-2 accepted with a partial
rebuttal note), 1 pending owner (A30). No findings rebutted outright — the attacker's frame
(consistency-protocol seams) was the right frame.** All accepted items are already edited
into spec prose; locations below.

**New owner ruling recorded this round:**

- **D40 (owner, supersedes the A2 remedy space) — Abstract loot is a game system, not a
  simulation.** Chess-layer scavenging depletion never touches the physical world; physical
  blocks never re-derive richness (one-time `base_richness` calibration only); richness
  **regenerates per round** toward base; hand-looting is a parallel resupply channel by
  design, not double-dipping. Owner's principle, quoted into doctrine: **"实质上我们不是在真的
  拟真一个末世，而是在做一场游戏" — we are making a game, not simulating an apocalypse.**
  Applied in econ §6 + board §3; also cited by the A6 resolution.

**HIGH:**
- **A1 ACCEPT** — combat dead are population dead, 1:1 (`war_death_population_ratio`),
  medical/wounded systems are the sanctioned swing dampers. counters §5.3 + econ §2.
- **A2 ACCEPT via D40** — per-cell `harvested` ledger + `clamp(base − harvested + regen)`;
  scanner calibrates once, never re-derives. econ §6 + board §3. The "double-dip" half is
  ruled *not a bug* (intended parallel channel).
- **A3 ACCEPT** — `pending_core_placement`: shell/world places the single core block
  (immediately if loaded, else on load). Declared narrow write exception, explicitly the
  live-side lane wargame_interface §1 reserves. Core audit passes pending cells.
  counters §5.6.
- **A4 ACCEPT** — ruling: **terrain is non-secret old-world geography**; fog guards living
  data and *change propagation*: tile patches distribute per-faction on (re-)exploration;
  explored cells keep the last-seen tile version (scars never appear inside fog). §9's
  anti-cheat claim now scoped to living data. board §6 + map §2.
- **A5 ACCEPT** — **sabotage is a priced declaration**: hardened cores, break alert +
  recorder attribution, saboteur's faction auto-enters war with full D16/D21 costs
  (illegal intra-bloc sabotage reverts and still costs), factionless saboteurs flagged
  hostile; sustained undeclared mirror-farming triggers the same auto-declaration.
  governance §7. (Design choice — owner may re-price, mechanism stands.)
- **A6 ACCEPT** — doctrine exception carved: **round-cadence playbook targeting is DM-view**
  (director's pressure system; physical counterplay is the fairness guarantee); tick-cadence
  rules stay vision-bound; "public estimates" defined = fog strength bands. ai §6 preamble.
  (Design choice — flagged for owner ack.)

**MED:**
- **A7 ACCEPT** — one battle per defender cell; third parties refused/queued
  (`cell_contested`); multi-side resolution explicitly v0.2-out. counters §5.1.
- **A8 ACCEPT (with note)** — top-up rule, per-cell respawn cooldown, **no-drop rule**, spawn
  scatter/safety, all normative (counters §6.2). Partial rebuttal on 8-2's framing: cycling
  was never *zero-exposure* (every death writes back, D3) — the cooldown removes only the
  burst-rate advantage; recorded here so the mechanism isn't misread as having been free.
- **A9 ACCEPT** — registration = authorization (permission-gated), per-device quotas
  integrated with D8 ledger, receipt-driven registration pass + manual register-device
  interaction for event-less provenance. econ §3.1.
- **A10 ACCEPT** — ledger rows fixed (power / coal·liquid_fuel / consumers); **freeze
  checklist rule** added to §0.1: ledger diffed against every ruling batch at freeze.
  integrations §2.
- **A11 ACCEPT** — founding auto-claims the core cell (capacity-exempt); cells holding any
  foreign core are never purchasable. governance §2, econ §4/§5.
- **A12 ACCEPT** — invariant stated: player core placement is distance-priced (same curve
  family) + `core_placements_per_round`; physical travel still enables remote claims (owner
  intent preserved), the D17 knob binds everywhere. counters §5.6.
- **A13 ACCEPT** — over-capacity sheds: stability drain + farthest-cell auto-de-control per
  round. econ §10.
- **A14 ACCEPT** — mission legality = movement legality; foreign targets need `at_war`
  (pillage, logged, favorability −); escort clause re-scoped. econ §6.
- **A15 ACCEPT** — frozen denominator + frozen ballots; mid-motion joiners ineligible;
  kicks of eligible voters blocked while a recall against the kicker is open. governance §6.
- **A16 ACCEPT** — reserved `env` pseudo-faction (cap-exempt, no pack, fixed no-retreat
  instant-shatter profile); invariant 3 gains the third materialization trigger. ai §6.1 +
  ARCH §5.
- **A17 ACCEPT** — AmbientField made first-class: per-cell `ambient_level 0..3` in
  SavedData, round-regenerated; heat rule reads `ambient_level > 0`; zhanghai income
  `base + k_amb × Σ level`; lairs = counter flags; fog/UI story defined (haze ramp).
  ai §6.3a.
- **A18 ACCEPT** — round-key idempotence: `roundend_done_for` persisted; caps keyed to round
  numbers; aborted maintenance never double-runs; `in_progress` receipts classified pending.
  ai §6.4.
- **A19 ACCEPT** — AI org regen in own territory + `ai_org_floor` outside; wanderers/raids
  arrive worn, never pre-broken. ai §3.
- **A20 ACCEPT** — reconciled: annexes siege normally; node cell falls abstractly only while
  unloaded (loaded ⇒ physical kill required); abstract fall ⇒ purge-on-load + region
  collapse. ai §6.3.
- **A21 ACCEPT** — visibility defaults table (faction-scoped unless argued public);
  `visibility` may be an array; `battle_ended` example corrected to participants.
  event_log §6 + §3.
- **A22 ACCEPT** — fog-consistent validation: orders validate against the requester's fog
  view; hidden-control moves halt at the border on contact; hidden-conflict purchases return
  fog-flat `cell_unavailable`. counters §3.
- **A23 ACCEPT** — stalled-rounds row in ARCH §12.3: accepted fiction, except three
  wall-clock fallbacks (interest cycle, bank-run exit, scheduled elections). econ §7.4.

**LOW:**
- **A24 ACCEPT** — minimap remnants scrubbed from ARCH §3 diagram + §4.
- **A25 ACCEPT** — `occupation_core` data file placed (`buildings/…, kind: claim_anchor`);
  core items from recipes + founding kit; path-A cost asymmetry declared intended. econ §4.
- **A26 ACCEPT** — full-stack moves refused (halts waypoint queue); retreat requires a free
  slot; none ⇒ shatter stands. counters §3/§5.5.
- **A27 ACCEPT** — approved tickets re-validate at apply; failure notices requester
  (`approved_but_failed`) and releases costs. governance §5 (one line).
- **A28 ACCEPT** — API trust boundary: loopback/socket + ops token, all calls logged with
  source. ARCH §12.4.
- **A29 ACCEPT** — cross-layer erratum recorded as integrations §8 ask 9 (recorder is the
  NeoForge Chess mod).
- **A30 PENDING OWNER** — griefing floor for bloc-wide exclusivity locks. Defender's
  proposed default: locks trigger only from factions with ≥ `focus_lock_min_members` (3)
  and age ≥ `focus_lock_min_age_rounds` (2); sub-threshold completions grant faction-local
  effects without locking siblings. Owner must rule (or declare working-as-intended);
  D9's shared-progress core is untouched either way.

*(Round-1 closure updates: A30 → ruled by **D41**; A5's remedy superseded by the owner's
**D42** cell-integrity model (governance §7 rewritten accordingly); A6 → owner **ACK**.
Round 1 closes on the attacker's diff spot-check of the response edits + D41–D43.)*

---

### Round-1 spot-check (attacker session, 2026-07-08)

Scope: diff verification of the 29 accepted edits + D40–D43, per the closure protocol.
**Verdict: every claimed edit verified present at its claimed location, and the fixes are
mechanically sound — PASS, conditional on the punch list below.** SC1/SC2/SC6 are
load-bearing; SC2 needs one owner ruling (suggest D44); the rest are one-liners. Round 1 can
close as v0.1.1 once the punch list lands.

Verified clean (no further comment): A1 · D40/A2 · A3 (`pending_core_placement` — the
wargame_interface §1 live-lane reading is legitimate) · A4 (board §6 + map §2) · A6 (the
DM-view doctrine paragraph is well-argued: tick-cadence vision-bound, round-cadence
director-view, fairness = counterplay latency) · A7 `cell_contested` · A8 four rules ·
A9 registration-is-authorization · A10 rows + §0.1 freeze checklist · A11 · A12 · A13 · A14 ·
A15 · A16 `env` + amended invariants · A17 AmbientField (controller never zhanghai; lairs =
counter flags — elegant) · A18 `roundend_done_for` + in_progress-as-pending · A19 · A20
(loaded/unloaded node rule is clean) · A21 defaults table + corrected example · A22
`halted: border_encountered` · A23 `fallback_round_days` ×3 · A24–A29 · D41 (5/2) · D43
(board §1.1 + ARCH §12.4 + map admin panel).

**SC1 (D42, load-bearing — governance §7-3 · integrations §2 population row · §0.8 D42).**
Resident kills "deduct the owner's **manpower**" — but manpower has no standalone stock; it
is *derived* (`cap − reserved − recruited + garrisoned`, econ §2). Mirror kills have a defined
sink (counter writeback → population per A1); resident kills have none. The A1-consistent sink
is **population** — residents are population's declared physical counterpart in the very same
ledger row: resident kill → `population − resident_death_ratio`, cap shrinks, done. Three
texts say "manpower"; all three should say population.

**SC2 (D42, needs an owner ruling — governance §7-2/§7-4).** Breaking an exposed core
(integrity 0) is **not itself a declaration** — only *kills* declare. Three consequences:
(a) **kill-laundering**: lure zhanghai/creepers into a *populated* camp — mob kills drain HP
with no player attribution — then mine the exposed core: full decapitation of a defended base,
zero declaration, zero cost; (b) **vulturing**: a third party mines the core a besieger's war
paid to expose, while never entering the war; (c) ungarrisoned border cores are removable
*costlessly* forever — "empty land is takeable" prices the **speed** as intended, but taking
by purchase costs resources while taking by break costs nothing. Proposal: **breaking any
registered core block in foreign territory routes through the same auto-declaration machinery
as first blood** (auto-`at_war` + D16/D21 costs; intra-bloc vs unity ≥ 60% → illegal-revert +
costs; factionless → hostile flag). Related underpricing, same lane: a **factionless agent**
(or quit→sabotage→rejoin) pays no stability/unity at all — add *recent-membership
attribution* (hostile acts within `sabotage_attribution_rounds` of leaving bill the former
faction) and a *rejoin lockout* while hostile-flagged.

**SC3 (D42, missing definitions — governance §7-1 · econ §2 · D25).** (a) `manpower_share
(cell)` is used but never defined — state the basis (e.g. garrisoned counters' manpower +
population housed via the cell's residential capacity). (b) "while not under attack" needs its
*predicate* named (e.g. no integrity deduction within `cell_attack_cooldown_ticks`), not only
the regen rate. (c) Residents are now a damage channel: state that resident NPCs inherit the
§6.2 mirror lifecycle rules (top-up toward density, respawn cooldown, **no drops**) —
otherwise killing the finite spawned roster stalls a siege with HP still on the bar, and
resident density silently becomes an HP-gate knob instead of a channel-width knob.

**SC4 (D42, wording — governance §7-2).** An intra-bloc illegal attack "eats the costs and
favorability crater" — favorability is a *bloc-pair* gauge; an intra-bloc pair doesn't exist.
Strike "favorability" there (stability/unity costs suffice) or define diagonal semantics.

**SC5 (D42, one sentence — governance §7-3).** State explicitly that mob/environment kills
drain HP (and population per SC1) **without** declaring anything — intended behavior (strain
raids soften cells for everyone), but it must be written *because* it is SC2(a)'s enabler and
readers will otherwise assume only attributed kills count.

**SC6 (A1, arithmetic — econ §2 · counters §5.3).** "The freed recruitment slice never nets a
refund" holds only while **`k_pop × war_death_population_ratio ≥ 1`**; the shipped defaults
(0.6 × 1.0) still refund 40% of every death. Not a number attack — the doc's own claim is
false under its own defaults. State the invariant beside the formula (or deduct the *cap*
directly by casualties, bypassing `k_pop`).

**SC7 (residue one-liners).** map §3 fog line still reads "`unknown` = black + no terrain" —
contradicts the A4 ruling (board §6: the old-world tile shows). econ §3.1 coal row still says
"**any** coal-burning block entity … draws" — should read "any *registered*" (the A9 paragraph
governs; the row misleads). ai §6.1 wildlife row: stale "see open Q6" (Q6 is resolved).

**SC8 (D43/D41 clarifications).** D43: (a) boundary **shrink** over already-materialized /
controlled cells — refuse, or derelict + refund? unstated; (b) observer-mode strains spread
*physically* past a boundary the *board* clips at — name the rim divergence (accepted +
`infection/divergence`, or an adapter suppression duty); (c) players already outside on edit:
confirm the escalation ladder is the intended answer. D41: state that a sub-threshold
completion never locks retroactively when the faction later crosses the threshold, and that
its faction-local effects coexist with a later bloc-wide lock of a sibling branch.

*(End of spot-check. SC2 to the owner; SC1/SC3–SC8 are defender-editable under the existing
round-1 acceptances.)*

---

### Spot-check responses (defender, 2026-07-08) — ROUND 1 CLOSED at v0.1.1

New ruling:

- **D44 (closes SC2; attacker's proposal adopted under the owner's "补完整" authorization —
  owner may re-price, mechanism stands).** Mining any exposed foreign **registered core
  block** (occupation/faction/building) runs the same auto-declaration machine as first
  blood: `at_war` + full D16/D21 legality and costs; illegal intra-bloc → revert + costs;
  factionless → hostile flag. Companion accountability rules: hostile acts within
  `leaver_accountability_rounds` (2) of leaving a faction are charged to the former faction;
  hostile-flagged players cannot join/rejoin factions while flagged
  (`hostile_flag_duration_rounds` 2). Closes all three SC2 channels: monster-wash (the mining
  still declares — SC5 explicitly keeps mob kills declaration-free, and it no longer matters),
  vulture third parties (mining declares regardless of who paid the HP bill), and free
  removal of undefended border cores (empty land is takeable *at a war price or a purchase
  price, never zero*).

Punch-list disposition — all eight landed:

- **SC1 ✔** population, not manpower — governance §7-3, integrations population row, and a
  bracketed correction in §0.8 D42 itself.
- **SC2 ✔** → D44 above; governance §7-5/§7-6.
- **SC3 ✔** (a) `integrity_max = hp_base + k_hp × population_housed(cell)`, abstract-anchored,
  density = channel width only — governance §7-1; (b) predicate named
  `cell_contested_recently` (damage within `hp_regen_lockout_ticks` 18) — ibid.;
  (c) residents inherit all four §6.2 mirror lifecycle rules — counters §6.2.
- **SC4 ✔** intra-bloc illegal attack costs = unity/stability only; favorability
  scoped bloc-pair — governance §7-2.
- **SC5 ✔** mob/environment kills drain HP+population, never declare — governance §7-3,
  with the D44 cross-note.
- **SC6 ✔** the false absolute replaced by exact arithmetic: capacity loss
  `k_pop × ratio` per death, labor-slice residual `1 − k_pop × ratio` named for what it is,
  strictly-non-refunding invariant `ratio ≥ 1/k_pop` stated beside the config — econ §2,
  counters §5.3 pointer.
- **SC7 ✔** map §3 fog line → old-world tile wording; econ coal row → "any *registered*";
  ai §6.1 stale Q6 pointer cleared.
- **SC8 ✔** boundary edit semantics (shrink refused unless `--force` → derelict + notice;
  *rim divergence* named, accepted pre-L4, adapter clip duty in driver mode; outside players
  start at the warning rung with `boundary_grace_ticks`) — board §1.1; D41 clarifications
  (no retroactive locks; earned local effects survive later bloc locks) — integrations §4.2.

**Round 1 tally: A1–A30 all adjudicated (29 accepted, A30 → D41), spot-check SC1–SC8 all
landed, rulings D40–D44 recorded. Doc set version: v0.1.1. Round 2 may open at the attacker's
discretion; implementation may start against v0.1.1 in parallel (Picasso precedent: docs
normative on merge, 🔁 marks any future spec-impl gaps).**

---

## §B Round 2 — self-attack (author session under role swap, 2026-07-08, time-boxed)

Context: owner requested an immediate second pass ("能多找一点毛病是一点毛病"). Attack surfaces
deliberately rotated off round 1's consistency-protocol seams: clocks, feedback loops,
determinism vs the narrative AI, fresh D42/D44 edges, cross-layer hanging promises.
Verdicts inline (same-session); ⚙ = fixed in spec this pass, 👑 = owner decision needed.

**B1 (HIGH ⚙ — ARCH §7 vs D37/Q7).** Narrative-AI overrides break replay determinism.
§7 promises exact replay of the abstract sim from snapshot + chess_log, with
non-deterministic inputs quarantined in ingest — but D37 composition overrides and hunt-target
overrides are external decisions consumed *inside* round-end computation, unlogged. FIX:
every AI override enters as an ingested event (`bridge/ai_override`, full payload) *before*
the decide pass reads it; absent event = deterministic default ran. Applied: ARCH §7,
ai §6.3c.

**B2 (MED ⚙ — ai §6.3b).** Heat-meter sources overstated & offline behavior undefined. The
build-log recorder captures place/break only — gunfire/sprint/jump need Chess's own hooks
(text implied otherwise). And unstated logout behavior lets players launder heat by waiting
offline. FIX: sources named (recorder feed for breaks; Chess entity/input hooks for the
rest); **heat freezes while offline** (decays only online). Applied.

**B3 (MED ⚙ — counters §5.3/§5.5 vs D39c).** Wounded vs the shatter death-roll: if wounded
"survive shatter", the 50% shatter death split must exclude them, else D39c is arithmetic
theater. FIX: shatter death-roll applies to able-bodied manpower only; the wounded pool
releases intact alongside the survivor share (as population — they never stopped being
people; SC1 consistency). Applied.

**B4 (LOW ⚙ — board §6 intel trading).** Snapshot merge rule unstated (a bought snapshot may
be older than the buyer's own). FIX: newest-timestamp-wins per cell; provenance + timestamp
render on merged intel. Applied.

**B5 (LOW — accepted quirk, logged only).** Aborted maintenance leaves round R running with
round-R+1 ephemera (wildlife re-rolled at lockdown). Cosmetic; round-key idempotence (A18)
already prevents the harmful double-execution. No spec change.

**B6 (HIGH ⚙ — econ §3.1 CNA terminal).** Power feedback loop: Chess injects into the
terminal; player energy pushed in credits Chess — nothing states the *injected* power is
excluded from the feedback measurement. As written, a powered cell's own injection reads as
player contribution → free energy. FIX: feedback credit = `max(0, measured_input −
terminal_inject_rate)`, injection and harvest metered separately. Applied.

**B7 (MED ⚙ — econ §3.1 tank sync).** Same loop family for fluids: Chess-inserted fuel must
be excluded from the "external fuel arriving while full" overflow-credit measurement. FIX:
bridge-inserted volume is tagged and never counts toward overflow credit. Applied.

**B8 (MED ⚙ — D39a favorability quest driver).** Favorability pump: repeatable AI-faction
NPC quests + bloc-level gauge = one grinding bloc buys permanent Spore pacifism for everyone.
FIX: per-round favorability gain cap from quests (`favorability_quest_cap_per_round`) +
decay toward a per-pair baseline (`favorability_baseline`, `favorability_decay_per_round`) —
peace must be maintained, not banked. Applied: governance §7 note. (Numbers PLACEHOLDER.)

**B9 (MED ⚙ — event_log §3 round stamp).** Lockdown-phase events settle round R+1 but the
window counter still reads R → consumers joining on `round` are off-by-one for exactly the
most interesting events (deploys, re-rolls, order flushes). FIX: `round` = window counter at
emission (unchanged, honest), round-end computation events additionally carry
`data.settling_round = R+1`. Applied.

**B10 (MED ⚙ — integrations §4.2).** First-completed-wins needs a total order for same-window
ties (two factions complete an exclusive branch between syncs). FIX: arbitration = chess_log
ingest order (tick, then event id); the loser's completion is reverted via FTB API with a
notice. Applied.

**B11 (HIGH ⚙ — governance §7 / D44 edge).** The last-block laundering hole: D44 gates
*mining*, but an **exposed core at HP 0 can be destroyed by a lured creeper/TNT chain**
with no attributable miner — monster-wash returns for the final block. FIX (benefit-based,
closes the whole laundering family permanently): core blocks while HP > 0 are immune to
explosions/pistons/mob-grief (the "unbreakable" is total); and **claiming or purchasing a
cell that was de-controlled by sabotage within `sabotage_taint_rounds` (2) runs the
auto-declaration machine against the victim** — whoever *benefits* from an unattributed
takeover pays the war price, whoever did it be damned. Applied. (Owner may re-price the
taint window.)

**B12 (LOW ⚙ — governance §7 / D44).** Hostile-flagged players barred from *joining* can
still *found* — founding is joining your own faction. FIX: flagged players cannot found
either. Applied.

**B13 (MED ⚙ — econ §7.1).** Exchange withdrawals need a physical landing point or the
alignment doctrine leaks: FIX: withdrawals materialize into the terminal block's inventory
(faction core / policy center) for physical pickup — no map-UI teleportation of items.
Applied.

**B14 (LOW ⚙ — governance §7-1).** `population_housed(cell)` apportionment: proportional to
per-cell residential capacity (faction core counts as `hp_base`-grade housing). One line.
Applied.

**B15 (MED ⚙ — persist / resilient startup).** Datapack drift vs live saves: a counter/
building whose template id vanished on reload must freeze as `invalid_template` (inert,
admin-resolvable), never crash or silently delete. Applied: ARCH §12.3 row.

**B16 (LOW ⚙ — counters §6.1).** Global mirror-cap contention (heat multipliers can starve
other cells): allocation priority order fixed as battles > player-faction garrisons >
strain territorial > ambient/ephemeral. Applied.

**B17 (MED — RESOLVED by D45).** Rounds advance only when a Picasso window actually runs;
Picasso-less maintenance advances nothing. **Owner ruling (D45, 2026-07-08): "无 Picasso 无
回合" — no Picasso, no round — is the intended fiction.** The `chess_round_offset` proposal
is REJECTED: Chess never advances rounds by any means other than `window_log.json`; a pure
server restart is weather, not history. A23's wall-clock fallback (interest cycle, bank-run
exit, scheduled elections) remains the only exception family. Applied: ARCH §12.3 row.

**B18 (LOW — logged, no change).** Work-order priorities among Chess's own kinds unstated;
convention adopted: construction 50 · scars 60 · restyle 70 (lower = earlier per contract).
Applied as one line in integrations §3.

*(Round 2 self-attack CLOSED: 18 findings — 3 HIGH, 9 MED, 6 LOW; 17 fixed or ruled
(B17 → D45), 1 accepted quirk (B5). Doc set: **v0.1.2**. Surfaces deliberately left for an
external round 3: netcode abuse, UI-side races, FTB API capability verification —
plus fresh eyes on §B itself, which was a self-attack and deserves hostile re-checking.)*

## §0.9 Final Pre-Handoff Ruling

- **D45 (closes B17) — No Picasso, no round.** The round clock is `window_log.json` and
  nothing else. Picasso-less server maintenance advances nothing — construction, quotas,
  deploys, and ephemeral re-rolls wait for the next true window; a restart is weather, not
  history. The `chess_round_offset` proposal is rejected. The A23 wall-clock fallback
  (interest cycle, bank-run exit, scheduled elections) is the only exception family.

---

## §C Round 3 — OPEN (2026-07-08)

Attacker: external session (round-1 attacker). Surfaces per the defender's brief: ① netcode
abuse & UI-side races · ② FTB Quests API reality vs doc assumptions · ③ hostile recheck of §B
· ④ HANDOFF build-order dependencies. Numbers not attacked (D-BALANCE); D1–D45 respected.
**Method note for ②: capabilities were checked against the shipped jars**
(`ftb-quests-neoforge-2101.1.25.jar`, `ftb-teams-neoforge-2101.1.10.jar` in the UtilWeDie
pack) — class-listing evidence, stated as such; a runtime spike is still required.
21 findings: **3 HIGH · 2 MED-HIGH · 10 MED · 6 LOW.**

### ① Netcode & UI races (never previously attacked)

**C1 (HIGH — ARCH §6/§7 vs map_interface §5 · governance §5 · event_log §7).**
*When do commands mutate state?* §7 quarantines "player commands" into the ingest phase;
§6's ingest enumeration doesn't even list them; map §5 promises an immediate
`CommandResult(applied)`; governance §5 reserves ticket costs at creation (an immediate
mutation). If commands defer to next-tick ingest, every UI action carries up-to-10-s latency
(purchase-paint, approve/deny, votes — untenable). If they apply immediately, the replay
contract (snapshot + chess_log replays *exactly*) requires each command's apply point in the
intra-tick order to be logged — which nothing specifies. Define the command pipeline.
Recommended: validate + apply immediately; log `orders/command_applied` with a monotonic
intra-tick sequence; replay applies logged commands at their logged positions between ticks;
restate §7's "quarantine" as *journaled inputs* and reconcile §6's enumeration.

**C2 (MED — map §5).** `cmd_id` idempotency holes: dedup-window size/eviction unstated; a
deduped resend must return the **cached original result** (not re-execute, not
`refused(duplicate)`); a reused `cmd_id` with a *different* payload must be rejected (payload
hash); state the id namespace (per-connection? per-player? — multi-login collisions).

**C3 (MED — map §5/§6 · ARCH §9/§10 · event_log §8).** The protocol has **no abuse limits**:
per-client command rate; viewport size cap (subscribe-the-whole-explored-map forces worst-case
snapshots); resubscribe/mode-switch churn; **tile-request validation** (A4 defined the
distribution *policy* — the request path must validate against the per-faction version vector,
or a modified client pulls patches it isn't entitled to); log-browser query cost (linear file
scans, §8) needs pagination + rate caps; pending-tickets-per-member cap (mass ticketed
commands = approver notice flooding). §9/§10 budgets are engineering targets, not
enforcement. Add a `net_limits` config family with defined over-limit refusals.

**C4 (MED — board §6 intel trading).** The intel trade is a **one-sided command** ("target
faction, cell set, price"): no offer/accept handshake, no expiry, no spam control, no named
payment currency (three bloc currencies exist), no atomicity rule (payment and snapshot merge
must settle together or not at all), and nothing states what the buyer may inspect
pre-purchase (cell set + snapshot ages?). Specify the offer lifecycle
(offer → accept/decline/expire) and atomic settlement.

**C5 (MED — counters §2.1/§7 · econ §2 · map §4).** **Disband is a combat exploit**: nothing
forbids disbanding mid-battle (dodges the shatter death-roll *and* its A1 population deaths —
a breaking army evaporates into safe manpower) or abroad (the slice returns instantly per
econ §2 — teleport evacuation of a cut-off counter). Fix: disband legal only outside battle,
outside retreat lockout, on own territory; disbanding abroad = march home first (or the slice
returns only after `disband_return_ticks`).

**C6 (LOW — governance §5).** Ticket concurrency: simultaneous approvers (minister +
president) — first command wins, second gets `already_resolved`? Approval racing expiry in one
tick is already resolved by phase order (ingest before governance timers) — *state it*. And
the requester has no cancel-ticket command (reservations locked until TTL).

**C7 (LOW — governance §6).** Vote edges: state the counting cut ("votes received before the
wall-clock deadline count; tallied at the next governance phase even if it runs after the
deadline") and the vote-change policy inside the window.

**C8 (MED — econ §7.1 / B13).** The exchange terminal's inventory is a Minecraft container:
nothing binds withdrawn items to the withdrawer or blocks hoppers/intruders — B13's
physical-delivery fix reopened a theft channel at the treasury's front door. Specify:
withdrawals are claim-bound (only the withdrawer may take them; unclaimed returns to stock
after `withdraw_claim_ticks`), container access member-only, no automation extraction; state
the fate of unclaimed items when an HQ is captured intact.

### ② FTB reality check (jar-inspected)

**C9 (HIGH — integrations §4.2/D9 vs jar).** **The "attach a chapter group per bloc"
primitive does not exist.** ftb-quests has a single global quest file
(`BaseQuestFile`/`ServerQuestFile`); chapters/groups are global content visible to every team
— nothing attaches content per team. All three bloc trees will be visible to all factions
(progress per-team) unless visibility is *authored in* via dependency gating. Rewrite D9's
mechanism text to the implementable form — e.g. every bloc tree hangs off a per-bloc **gate
quest** that Chess auto-completes for the right bloc's teams, hiding the others via
dependency visibility. This is a **content-authoring rule** the owner must know *before*
authoring the trees.

**C10 (HIGH — integrations §4.2/B10 vs jar).** **No per-team lock/hide API for branch
exclusivity.** The public api package is ~9 classes (FTBQuestsAPI · QuestFile · tags · item
filter · one client event) — no lock surface. What *does* exist (jar-verified):
`CustomTask` + `CustomTaskEvent` (mod-controlled task completion — the dependency-gate
pattern), and internal `ChangeProgress`/`TeamData` (the machinery behind
`/ftbquests change_progress complete|reset` — programmatic complete **and** reset at
internal-package level; this also carries B10's "revert via FTB API" and bloc-wide
replication). Consequences: (a) exclusivity = every exclusive-branch quest depends on a
Chess-controlled gate task — again a content-authoring rule; (b) all progress writes bind to
**internal classes**, not API — isolate behind `bridge/ftb/`, pin the version (pack ships
2101.1.25), record the internal-binding risk in integrations §4; (c) the P0 spike (C20) must
confirm per-team complete/reset semantics, event payload attribution (D19 contributor
scoring), and reset side-effects on already-claimed rewards.

**C11 (MED — integrations §4.1 vs jar).** Teams mirroring is *plausible* — the ftbteams api
package is rich (TeamManager · Team · CustomPartyCreationHandler · join/leave/ownership
events all present) — but the exact ops Chess needs have semantics to confirm: create a party
team programmatically for an arbitrary founder, force-move members (offline members?),
party-of-one for solo dictators. Also: the degraded-modes row is all-or-nothing
(`ftb_integration false`); add **per-capability degradation** (teams work but locking doesn't
→ exclusivity enforced Chess-side, cosmetic quest-visibility divergence accepted and logged).

**C12 (MED — D9 fallback design).** If the spike falsifies any assumed capability, D9 has no
designed fallback at all. Pre-agree the degraded design now: **Chess-side progress ledger as
the authority, FTB as display** — effects applied by Chess from its own ledger regardless of
quest-UI state; FTB divergence logged, never blocking. A spike failure must not reopen D9
mid-implementation.

### ③ §B hostile recheck

Overall: the self-attack holds up well — B1/B6/B7/B9/B10/B15 are real catches, correctly
fixed. Five items need tightening, one materially:

**C13 (MED-HIGH — B11 / governance §7-5).** The sabotage-taint machine **fires on the
victim's own re-claim**: D29's discounted grace re-purchase — the *canonical recovery path* —
happens inside `sabotage_taint_rounds` by construction, so a victim re-anchoring their own
cells "declares war on themselves". Exempt `claimant == victim`. Second edge: innocent third
parties walk into wars they cannot see — the taint must be **visible in the purchase/claim
preview** ("claiming this cell declares war on <victim>"); informed consent turns the
landmine into a fence, and D-REFUSAL's preview honesty already demands it.

**C14 (MED — B11 scope vs counters §8).** B11's "total immunity" is live-server enforcement
only; Chess's **own war-scar orders** (`degrade`/`composite_event`, bounds = defender cell)
can bounds-overlap a registered core position and have Picasso remove the core during a
window — an accidental (or engineered, via battle-site choice) decapitation bypassing the
whole D42/D44 machine. One line: Chess excludes registered core positions from the bounds of
its own destructive orders at submission time.

**C15 (MED — B16 + ARCH §10).** **Budget-starvation turtling**: the global mirror cap plus
B16's priority order lets a faction keep the global budget saturated (alt accounts parking
loaded battle/garrison cells elsewhere) so its *own* home cells never manifest garrison
mirrors or residents — the HP channel closes, `cell_contested_recently` stays false, and
cores become effectively unbreakable. Add a reservation rule: a cell with a non-member player
physically present gets a guaranteed minimum mirror/resident allocation (the defense always
manifests for an actual intruder) — or integrity becomes presence-drainable when spawn-starved.

**C16 (LOW — B6/B7).** The feedback formula should subtract the **actually injected** amount
this tick (`measured_input − injected_actual`; in an unpowered cell `injected_actual = 0`),
not the config rate — else genuine contributions below `terminal_inject_rate` are silently
confiscated. B7: state that tank metering happens at the insertion interface (volume in/out
accounting) — fluids carry no tags once merged.

**C17 (LOW — B10).** Arbitration by ingest order is right, but **effects must apply at
arbitration**, not at the FTB completion event — otherwise the loser's already-granted
effects (grants possibly spent) need rollback. One line: focus effects are applied only by
the arbitration step.

**C18 (LOW — B15).** Extend `invalid_template`'s freeze-not-crash rule to vanished resource
ids (treasury stock in a deleted resource), policy ids, loadout pools, and exchange rows.

**C19 (LOW — B8).** The cap + decay fixed the *quest* pump, but trade volume is also a
positive favorability driver and equally grindable (wash-trading between colluding blocs).
Either the per-round cap covers all positive drivers, or write the sentence that the exchange
spread makes wash-trading self-taxing.

### ④ HANDOFF build order

**C20 (MED-HIGH — HANDOFF §3).** **Spike inversion.** The trap list says "verify before
building on it" and P9's own note says "verify actual API capability early" — yet all three
unverified external surfaces land *last*: FTB (P9), the energy-bridge mod APIs (P5,
self-declared "most mod-compat-fragile"), and the CNPC+TACZ mirror provider (P4 — counters Q4
acknowledges the risk; HANDOFF never flags it). C9/C10 prove these aren't integration
details: they change **content authoring** and possibly D9's mechanism. Add **P0 —
external-surface spikes**: throwaway probes + a written capability matrix + pre-agreed
fallbacks per surface (FTB complete/reset/gate-task/teams ops · CNPC spawn/loadout/kill-tag
readback · CNA terminal inject/read · Create tank-insert), run before or parallel to P1.

**C21 (MED — HANDOFF §3/§5).** The P3–P6 acceptance flows (found → claim → build A/B → train
→ fight → bank run) are driven by *player commands* whose only specified surface is the P7
map client. Give the command choke point a **complete headless form in P3** (every player
command invocable via server command / test harness through the same validation path) — this
unblocks phase acceptance and permanently serves golden tests; re-scope §5's "scripted second
client" to that surface before P7.

*(End of round-3 findings. Author responds per item.)*

---

### Round-3 defender responses (author session, 2026-07-08) — ROUND 3 CLOSED at v0.1.3

**Verdict: 21/21 ACCEPTED, zero rebuttals.** The jar-inspection method (C9/C10) sets a new
bar for this review series — empirical verification beats class-of-argument. All fixes are
in spec prose; locations below. No owner rulings were required: C9/C10 change D9's
*mechanism and content-authoring rules*, not its substance (FTB runtime, shared bloc
progress, first-completed-wins all stand).

- **C1 ✔** command pipeline defined: immediate validate+apply on receipt, monotonic
  intra-tick sequence, `orders/command_applied(tick, seq)` journaled at apply point; replay
  re-applies at recorded positions; §7 "quarantine" restated as *journaled inputs*; §6
  ingest enumeration reconciled — ARCH §6/§7, map §5.
- **C2 ✔** cmd_id contract: per-player namespace, sliding window (256/5 min), duplicate →
  cached original result, payload-hash conflict → `cmd_id_conflict` — map §5.
- **C3 ✔** `net_limits` family: command rate, viewport cap, churn cap, **tile requests
  validated against the per-faction entitlement vector**, log pagination + rate caps,
  tickets-per-member cap — map §5.
- **C4 ✔** intel offer lifecycle: offer → preview (cell list + snapshot ages, never content)
  → accept (atomic payment+merge) / decline / expire; per-pair open-offer cap — board §6.
- **C5 ✔** disband legality: outside battle, outside retreat lockout, own territory only —
  counters §1.
- **C6 ✔** ticket concurrency: first-by-ingest wins, `already_resolved`; same-tick
  approval-vs-expiry settled by phase order (stated); requester cancel command —
  governance §5.
- **C7 ✔** counting cut + last-ballot-stands vote changes — governance §6.
- **C8 ✔** claim-bound withdrawal slots, member-only GUI, no automation face, unclaimed →
  stock after `withdraw_claim_ticks`; unclaimed claims follow the treasury — econ §7.1.
- **C9 ✔** global-singleton reality + **gate-quest content-authoring rule** written into
  integrations §4.2 (owner-facing: bloc mainline roots depend on bloc gates; exclusive
  branches depend on branch-gate tasks). D9 mechanism text superseded in place.
- **C10 ✔** lock/hide API absence recorded; CustomTask gates + internal
  ChangeProgress/TeamData path adopted; internal-binding risk isolated in `bridge/ftb/`,
  pinned 2101.1.25, per-capability degradation — integrations §4.2.
- **C11 ✔** teams-API semantics → P0 spike items; per-capability degradation replaces the
  all-or-nothing flag — integrations §4.2, HANDOFF P0.
- **C12 ✔** fallback doctrine pre-agreed: **Chess-side progress ledger is the authority,
  FTB is display**; divergence logged, cosmetic by definition — integrations §4.2.
- **C13 ✔** taint exempts `claimant == victim`; taint visible in claim/purchase preview —
  governance §7-5.
- **C14 ✔** registered core positions carved out of all Chess-issued destructive order
  bounds at submission — counters §8.
- **C15 ✔** intruder-presence reservation: `mirror_floor_intruder` (6) preempts globally —
  the defense always manifests for an actual intruder — counters §6.1.
- **C16 ✔** B6 formula corrected to `measured_input − injected_actual_this_tick`; B7
  metering pinned to the insertion interface (volume accounting; merged fluids carry no
  tags) — econ §3.1.
- **C17 ✔** focus effects apply only at the arbitration step — integrations §4.2.
- **C18 ✔** freeze-not-crash extended to resource/policy/loadout/exchange ids — ARCH §12.3.
- **C19 ✔** favorability gain cap widened to all positive drivers; exchange-spread
  self-taxation noted as secondary — governance §9-6.
- **C20 ✔** **P0 external-surface spikes** added (FTB · CNPC+TACZ · CNA · Create tank;
  probes + capability matrix + pre-agreed fallbacks), before/parallel to P1 — HANDOFF §3.
- **C21 ✔** P3 ships the complete headless command mirror; fixture re-scoped (scripted
  client only from P7) — HANDOFF §3/§5.

**Round 3 tally: C1–C21 all landed. Doc set: v0.1.3. Series total: D1–D45 + 87 findings
(30+8+18+21+8 spot-checks) adjudicated across three external rounds and one self-attack;
zero rebuttals outstanding, zero owner items pending. The one structural lesson worth
carrying to MiroFish: external-surface assumptions must be jar/spike-verified before they
become mechanism text (C9/C10 were writable-looking fiction for three rounds).**

---

### Round-3 spot-check (attacker session, 2026-07-08) — SERIES CLOSED

21/21 verified at their claimed locations, including the four load-bearing rewrites (C1
command pipeline in ARCH §6/§7 + map §5; C9/C10 gate-quest rules in integrations §4.2;
HANDOFF P0 with per-surface fallbacks incl. the custom-soldier ultimate fallback; C13/C15 in
governance/counters). Version stamps consistent across all 11 docs + README (v0.1.3).

**One residue, fixed in place by the attacker under the round-3 role swap (defender to
eyeball the diff):** integrations §4.2 point 3 still said the binding layer "locks/hides
sibling quests for the whole bloc **via API**" — the exact fiction C10 falsified, one
paragraph below the jar-verified "No lock/hide API" block. Superseded with the branch-gate
withholding mechanism (B10 arbitration cross-referenced). No other divergence found.

**Verdict: PASS. Chess doc series CLOSED at v0.1.3 — implementation starts at HANDOFF P0.**

### Defender final audit (author session, 2026-07-08) — release sign-off

Attacker's in-place fix to integrations §4.2-3 **eyeballed and approved** (branch-gate
withholding wording is correct and consistent with the C9/C10 block; B10 cross-ref intact).
A same-class residue sweep (grep for post-ruling fictions across the corpus) found and fixed:

1. **governance §1-2 and §2** still carried the C10-falsified "chapter group per bloc,
   auto-attached" / "chapters attached" phrasing — rewritten to the gate-quest mechanism
   with C9 citations. (Same fiction, two more homes — the attacker's find was not isolated.)
2. All eight subsystem H1 titles normalized from "(v0.1 draft N)" to "(v0.1.3)" (title lines
   had never tracked the status stamps).
3. governance §1 "exactly three things" → four (list had grown with D21).

No further divergence found. **Sign-off: Chess doc series CLOSED at v0.1.3, both sessions
concurring. Implementation begins at HANDOFF P0.**

---

## §0.10 Post-Close Owner Rulings

- **D46 (2026-07-08) — Selection, stances, and the overlap engagement model.**
  (a) **Footprint is the hitbox**: clicking any cell of a counter's footprint selects that
  counter; the center cell is only the pathing/display anchor. Left-click selects/inspects,
  **never** issues commands; commands come only from right-click context (hostile footprint =
  attack that counter, neutral = move) or **A+left-click explicit attack**. Attack orders
  target *counters*, resolved to the nearest edge cell of the target's footprint. Right-click
  on a non-hostile foreign counter = move, halting at the border (A22 legality unchanged).
  (b) **Stances promoted from v1.5 to v1**: `hold_ground` (default) — defends its own
  footprint only; adjacency with a hostile footprint stays peaceful indefinitely absent an
  explicit order; `free_engage` — auto-initiates against *already-legally-hostile* targets
  entering engagement range (adjacent to footprint); never bypasses declaration legality.
  (c) **Engagement = overlap (owner preference over adjacency-fire)**: an attack pushes the
  attacker's edge into the target's edge cells; overlapped cells are **contested front cells
  owned by the Battle object** (the standing invariant's "a Battle, never co-occupancy"
  wording already accommodates this — the overlap *is* the battle). Per-tick resolution runs
  over front cells (D26 per-cell shares + reserves); a lost front cell converts to the
  winner and the front advances; footprint-1 counters degenerate to one contested cell —
  same machinery, HOI4-like front-movement visual. Supersedes the earlier "attacker fights
  from its own cell and only advances on victory" wording (counters §3/§5 rewritten).
  Loaded contested cells spawn *both* sides' mirrors — D3 writeback becomes the literal
  physical battle.

- **D47 (2026-07-08) — Tags, mobility modes, the road layer, and physical-unique vehicles.**
  Staging: **schema ships in v1, only `foot` is engine-enabled; vehicle modes are the
  designed v0.2 mobility subsystem** (elaborates D28, which stays authoritative on "no
  frontline/invasion planning ever").
  (a) Templates gain `tags: []` (free + reserved vocabulary — modifiers, focus effects, and
  the narrative layer target counters by tag, e.g. a motorization focus multiplying
  `speed_mult` on `motorized`) and a `mobility` block
  (`mode/speed_mult/road_bonus/requires_road/…`).
  (b) Five modes: `foot` (standard, v1) · `wheeled` (road-dependent, off-road heavy penalty,
  avoids water/dense rubble) · `tracked` (road-preferring, mild off-road penalty) ·
  `airship` (terrain-blind, grounded↔airborne state machine with takeoff/landing ticks;
  airborne = no occupation, no siege, restricted bombardment-style attack, AA-vulnerable) ·
  `odm` (立体机动: terrain multipliers inverted — urban/forest fast, open slow). Vehicle
  modes carry combat modifiers (the "vehicle weapon bonus"), `liquid_fuel` upkeep (D33
  economy), and may mirror as real superbwarfare vehicle entities
  (`mirror_profile.provider: "vehicle"`).
  (c) **Road layer**: cell gains `road: 0..2` (none/street/arterial), fed by two sources —
  own loaded-chunk sensing against `road_blocks.json`, and a **new read-product ask to
  Picasso (ask 10)**: publish a cell-resolution road-class layer + connectivity graph beside
  the structure registry (read-only, same channel family as ask 3; NOT a work order — reads
  are not writes). Xaero alignment is automatic: terrain tiles and cell road data both
  derive from the same world.
  (d) **Physical-unique counters (v0.2 + spike)**: a new mirror provider `physical_unique`
  registers a specific player-built Sable contraption (airship) as a counter — the physical
  structure IS the counter's manifestation (no spawned mirrors; loaded = the actual ship,
  unloaded = abstract; physical destruction = counter death via writeback). Requires a
  sable-schematic-api capability spike (structure tracking, position readback, destruction
  events) — added to the v0.2 spike list (integrations ask 11).

### Post-close review of the D46/D47 landings (reviewer session, 2026-07-09)

Landing verdict: **substantively sound** — road layer (board §2.1 + ask 10/11), lockdown
latch / scheduler pause / SAFE-TO-STOP atomicity (ai §6.4), shortage-order table (econ §3),
map §4 control scheme, and ARCH invariant 1 all verified in place. Eleven gaps found and
**fixed in place under the standing role-swap authorization** (defender to eyeball):

- **PC1 (counters §5.1, model residue)**: still framed battles as "opening over a defender
  *cell* when an attack order lands on it" — pre-D46. Reframed: battles open against a
  defender *counter*; legality checks the target's owning faction wherever its footprint
  stands (control-entry stays §3's separate gate); A7's third-party rule re-expressed as
  one-battle-per-defender-counter with `target_contested` (the stacking refusal keeps
  `cell_contested` — two situations, two codes).
- **PC2 (counters §5.5, model residue)**: "an attacker is never forced backward off its own
  cell — it was never in the defender cell" is false by construction under overlap. Rewritten:
  broken attackers withdraw from all front cells to their pre-battle footprint (cells revert),
  and observe `retreat_lockout_ticks` before re-initiating.
- **PC3 (counters §5.6, unspecified timing)**: de-control now fires **per front cell at
  conversion time** (production hooks detach when the enemy stands on the cell, not at the
  verdict); overrun enemy cores prompt the capture/raze choice at overrun; battle-end
  `advance` consolidation defined (contiguous template-size normalization at the
  furthest-advanced cell — mirror of the defender-contraction rule).
- **PC4 (counters §2.5, missing semantics)**: attack-order pursuit = re-resolve per movement
  leg while target visible, fog-loss → `halted: target_lost` (A22-consistent);
  `free_engage` triggers **only while stationary** (no attack-move v1), auto-initiations
  resolve in phase 8 as faction acts; combat stance explicitly disambiguated from the AI
  faction's diplomatic stance (name collision was an implementation hazard).
- **PC5 (ai §3, gap)**: AI counters had no combat-stance defaults — a `hold_ground` sculk
  blob would be a peaceful neighbor, contradicting `hostile_all`. Defaults: strains
  `free_engage`; wanderers `hold_ground` (flee rules own initiative); settlements
  `free_engage` at home / `hold_ground` abroad; overridable via behavior-pack
  `default_stance`.
- **PC6 (counters §5.2)**: on contested front cells the per-cell mirror cap applies **per
  side** (both armies manifest; global cap + B16 priority still bind).
- **PC7 (counters §2.3)**: "static while multi-cell" reconciled with D46 — the static rule
  binds movement orders; battle-driven front deformation is Battle machinery, the stated
  exception.
- **PC8 (counters §3.1)**: movement-rate formula shape pinned
  (`ceil(move_period × terrain_mult(mode) / (speed_mult × road_mult))`, `requires_road` =
  impassable off-road); road term active **from v1 for `foot`** — the schema and road layer
  ship v1, only non-foot modes wait.
- **PC9 (ai §6.4 + ARCH §7, load-bearing)**: the crash-resume safety claim ("work orders are
  crash-safe by `order_id` dedup") silently required re-run determinism that tick-seeded
  `ChessRandom` breaks — a re-run at a different tick rolls different deploy choices under
  fresh ids, and the crashed attempt's orphan orders in `pending/` execute alongside the new
  ones (double deploys the dedup cannot see). Fixed: round-end computation and all
  round-cadence rules seed on `(world_seed, round, subsystem, subject_id)` — re-runs are
  bit-identical, ids collide, dedup holds.
- **PC10 (counters §2.1)**: veterancy "battle fought to completion" defined (battle ends by
  any outcome, ≥ 1 combat round participation; retreat counts, shatter doesn't).
- **PC11 (counters §5.3)**: `attacking_cells` in the concentric bonus defined as distinct
  front cells currently pushed (D46 meaning).

**Defender eyeball of PC1–PC11 (author session, 2026-07-08): all eleven APPROVED as landed**
— spot-checked §5.1 reframing, attacker-withdrawal, per-front-cell de-control timing, pursuit
semantics, AI stance defaults, and the PC9 seeding change in both homes. One second-order gap
found in PC9 and fixed by the defender (**PC9b**): round-seeded RNG makes the re-run's
*randomness* bit-identical, but re-run **inputs** must be identical too — a crash loses all
state since the last regular save, so the re-run could read a slightly older board (missing
the final settlement pass) and make different *state-dependent* choices (deploy-site
selection, hunt ranking) under the same seeds — different orders, different slugs, dedup
blind again. Fix: the lockdown sequence performs **two explicit saves**: one immediately
after the final settlement pass (**the input pin** — the exact state round-end computation
reads), one at completion (marker + effects, as before). A crash between the two reloads the
pinned input → bit-identical re-run end to end; the dedup argument is now airtight.
Applied: ai §6.4.

- **D48 (2026-07-08) — Merge & split (owner-prompted; v1, pure core arithmetic).**
  Data-driven pairing via template `reorg: {merge_into, merge_count, split_into,
  split_count}`. Merge: same faction, declared-compatible templates, all participants
  stationary and co-located/adjacent, none in battle or retreat lockout, target footprint
  placeable; manpower sums (overflow above the target template's max returns to the faction
  pool), org/strength are **manpower-weighted averages** (no laundering a broken unit with
  fresh troops), veterancy = weighted average floored, wounded pools merge. Split is the
  inverse (equal manpower division, stats inherited, wounded pro-rata). Equipment
  conservation is a content-authoring duty: merge pairs must be cost-neutral by
  construction. Anti-abuse: both operations impose `reorg_lockout_ticks` (12) — no attack
  initiation, no re-reorg — and are illegal in battle or retreat lockout (C5 family).

- **D49 (2026-07-09) — Dictatorship currency: bullets; currency assets delivered.**
  黑面包 black_bread is replaced by **`bullets` 子弹** as the dictatorship's base currency —
  physical form is the **existing TACZ general pistol ammo** (`tacz_unidict:pistol` ammo
  index carried by the `tacz:ammo` item; jar-verified name 通用手枪子弹), so no new item
  registration is needed and the currency is literally spendable ammunition (Metro-style
  hard commodity). Bottle-cap (red cola-style) and grain-ticket **textures delivered** by
  the owner: `Chess/瓶盖与纸币.zip` — inventory: 瓶盖×2, 粮票×3, plus 纸币×2 (一元/美金)
  held in reserve for future currency expansion. Ask 5 narrowed to registering two items
  with delivered art. Applied: econ §1/§7.3/§7.5, governance §1, integrations §2 + ask 5,
  ARCH §4/§5. (D18's mapping otherwise unchanged: republic = bottle_caps + interest,
  democratic centralism = grain_tickets + deposit-to-use.)

- **D50 (2026-07-09) — Three denominations per currency, fixed 1 : 20 : 100.**
  Bullets: 手枪弹 `tacz_unidict:pistol` (1) · 步枪弹 `tacz_unidict:rifle` (20) · 狙击弹
  `tacz_unidict:sniper` (100) — all existing TACZ ammo, zero registration. Bottle caps:
  农夫山泉 red plastic (1) · 冰红茶 (20; tooltip lore — a lucky "再来一瓶" back-print is worth
  a cola cap; optional rare variant item, content work) · 可乐 iron-made (100, most durable —
  lore). Grain tickets: three serials/colors at 1/20/100. Treasury accounts in base units;
  the terminal converts denominations at face value (no internal FX; cross-currency stays
  the barter table). Owner delivered the Nongfu cap texture alongside the earlier pack;
  all art organized into **`Chess/assets/currency/`** (bottle_caps/ · grain_tickets/ ·
  reserve/ with banknotes; README maps file↔item↔face-value; texture→denomination mapping
  assumptions flagged for owner re-pointing). Ask 5 = six item registrations with delivered
  art. Applied: econ §7.5, integrations ask 5.

- **D51 (2026-07-09) — Currency UI iconography + rename.** Map-UI treasury displays use the
  bloc currency's **×100-denomination icon as its symbol** (sniper round / cola cap /
  粮票三号); factionless players and the pre-polity founding wizard show a neutral
  "货币未确定 / currency undetermined" placeholder — the UI never guesses. Rename:
  农夫山泉瓶盖 → **农夫三泉瓶盖** (trademark-safe). Applied: map §4, econ §7.5,
  assets/currency/README.

---

## §D Post-Close Agent Review (2026-07-09) — R1–R21, all accepted & landed

Scope: D46–D51 + PC-series + post-close clarifications. Agent verdict: conditional pass —
4 HIGH battle-model holes from the D46/PC rewrite. Defender adjudication: 21/21 accepted;
dispositions (all landed same-day, owner delegated land-and-close):

- **R1 HIGH → territorial siege defined**: entering an ungarrisoned hostile-controlled cell
  opens a **degenerate Battle against the cell anchor** (pseudo-defender share =
  f(integrity/intensity, fortification); no org/manpower; falls when the share breaks —
  empty claimed land falls in ~one tick). One mechanism for player and strain cells; keeps
  ai §4.3/§6.3 "battle over a cell" wording valid. counters §5.1a.
- **R2 HIGH → no mirror battles**: an attack/auto-initiation against a counter already
  sharing a live Battle with the initiator **joins that Battle** (counter-push); at most one
  Battle per opposing pair. counters §5.1.
- **R3 HIGH → stack conscription**: counters co-stacked on any cell of the targeted
  defender's footprint join `defender_side` at open; they count as "the same defender" for
  `target_contested`. counters §5.1.
- **R4 HIGH → provisional conversion**: front-cell conversion effects are provisional until
  battle end/consolidation — reverted cells restore prior control atomically (hooks
  reattach, no D29 trigger); the overrun core capture/raze choice is selected at overrun but
  **executed only at battle end** (hit-and-run raze raid closed). counters §5.5/§5.6.
- **R5 MED**: many-vs-one arithmetic fixed — own edge cells first, then join existing front
  cells up to 3/cell; refusal beyond 3×edge-cells capacity. counters §5.2.
- **R6 MED**: consolidation resolves deterministically by counter id, respects stacking,
  unplaceable → withdraw to pre-battle footprint. counters §5.6.
- **R7 MED**: merge overflow returns to pool **only on own territory**; abroad, an
  overflowing merge is refused (`merge_overflow_abroad`) — C5's teleport-closure preserved.
  counters §2.4a.
- **R8 MED**: battles against `env` (and ephemeral counters) never accrue veterancy.
  counters §2.1.
- **R9 MED**: bullets mint = modpack-controlled faucet — **new ask 12**: gate/rebalance TACZ
  recipes for the three currency calibers (cost ratio ≈ 1:20:100 or server-disabled);
  §7.5 note added. econ + integrations.
- **R10 MED**: the six currency item ids are excluded from every non-currency exchange row
  (generator rule). econ §7.1.
- **R11 MED**: road layer merges **by recency** (newer source wins; max only between
  same-age) — destroyed roads can die. board §2.1.
- **R12 MED**: PC9b wording reconciled (two saves); ordering pinned — input-pin save
  completes before the in-memory marker is set; marker durable only in the completion save.
  ai §6.4 + A18 text.
- **R13 MED**: lockdown latch made durable **first** — the command's first staged action is
  a latch save, before the kick reports ✓. ai §6.4.
- **R14 LOW**: round-cadence seeding exception propagated to ai §2.4.
- **R15 LOW**: "**unmodified** left-click never issues commands". counters §2.5 + map §4.
- **R16 LOW**: ledger bullets row lists the denomination trio. integrations §2.
- **R17 LOW**: 再来一瓶 cap = loot/mint-only variant, `face_value: 100` on deposit, never
  produced by withdrawal. econ §7.5.
- **R18 LOW**: v0.2 mobility notes — airship attack = declared non-overlap exception
  (designed with v0.2); landing requires non-water; "dense rubble" = `ruin`. counters §3.1.
- **R19 LOW**: contested-cell click resolution — own side preferred, repeat-click cycles,
  A+click targets the enemy participant. counters §2.5.
- **R20 LOW**: split shares merge preconditions, places under stacking or refuses
  `split_no_room`; reorg lockout suppresses `free_engage` auto-initiation (defense
  unaffected); reorg products inherit the newest participant's training timestamp.
  counters §2.4a.
- **R21 LOW**: econ §1 currency pointer §7.4→§7.5; event_log `battle_ended` subject carries
  `front_cells_final: []`, not a single cell.

**Chess doc series re-closed at v0.1.4.**
