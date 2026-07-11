# Economy & Buildings — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> Encodes **D7** (resource taxonomy), **D8/D18** (bidirectional exchange, per-round quotas,
> polity-mapped access models), **D12/D17** (occupation cores & land purchase), and **D19**
> (manpower allocation: recruitment slices the cap; garrisons at home give it back).
> All rates/costs PLACEHOLDER (D-BALANCE).

---

## 1. Resource Registry — `data/swchess/resources.json`

| Category | Resources | Semantics |
|---|---|---|
| **Stocks** | `build_materials` 建材 · `raw_materials` 原料 · `equipment` 装备 · `food` 食物 | faction treasury quantities; produced, consumed, exchanged |
| **Currencies** | `bullets` 子弹 (hard commodity — Metro-style ammo currency, D49) · `bottle_caps` 瓶盖 (financial) · `grain_tickets` 粮票 (financial, physically anchored) | per-bloc base currency (D18); barter-leaning — the exchange book prices goods in currency, and currencies are themselves exchangeable items (§7.5) |
| **Energy** | `power` 电力 (flow, per-tick, never stored) · `coal` 库存煤 · `liquid_fuel` 液体燃料 (stocks; coal-family and oil-family items map in, D33) | generators burn fuel → power pool; buildings consume power; physical bridges in §3.1 |
| **People** | `population` 人口 · `manpower` 人力 | population is the *cap* of manpower (§2); nearly every action spends or reserves manpower |
| **Gauges** | `stability` 稳定度 · `unity` 团结度 (0–100) · `political_points` 政治点 (accumulating, spendable) | purely abstract (declared so in the Alignment Ledger); political points buy policies (§8) |

The registry is data-driven: a new resource is a JSON row plus exchange-table rows
(ARCHITECTURE §13).

---

## 2. The Manpower Model (D19, normative)

```
manpower_cap(faction)   = k_pop × population                      (k_pop 0.6)
available_manpower      = manpower_cap
                          − Σ reserved (building staffing, constructions, missions, purchases)
                          − Σ recruited counters' manpower
                          + Σ manpower of own counters garrisoned on own territory
```

- **Reservation** (staffing a building, running a scavenge mission, buying land, constructing):
  manpower is *held* while the activity runs and released after. Reserved ≠ consumed.
- **Recruitment** carves the counter's manpower out of the cap — a standing army is a standing
  reduction of the labor force.
- **The homecoming rule**: a counter garrisoned on its own faction's territory adds its
  manpower back (soldiers double as laborers at home). Deploying abroad — or losing men — is
  what actually costs the economy. Disbanding returns the slice intact.
- **Combat dead are population dead (A1; arithmetic stated precisely per SC6)**: counter
  casualties reduce population by `war_death_population_ratio` (1.0) per manpower killed.
  Exact consequence: total faction capacity permanently shrinks by
  `k_pop × war_death_population_ratio` per death; the residual fraction of the freed
  recruitment slice (`1 − k_pop × ratio`, 40% at defaults) returns to `available_manpower` —
  *not* a resurrection, but the arithmetic fact that a soldier reserves a full slice while a
  civilian only ever supplied `k_pop` of one. Operators wanting strictly-non-refunding deaths
  set `war_death_population_ratio ≥ 1/k_pop` (the invariant, stated here so the doc's claims
  and its defaults can never disagree). Demography pays either way; rebuilding costs
  resources on top.
- Population deaths (combat, raids reaching population, starvation §5) lower the cap itself.

---

## 3. Settlement Economy (pipeline phases 2–6 detail; order normative, ARCHITECTURE §6)

1. **Power** — generator-role buildings burn `fuel` into the tick's `power` pool; buildings
   are marked powered in priority order (leader-configurable; unpowered buildings idle).
   Fuel is two stocks (D33): `coal` and `liquid_fuel` — both deposit-type resources; their
   physical bridges are §3.1.
2. **Production** — each staffed + powered building applies its per-tick recipe (§4 table).
3. **Upkeep** — counters (food/equipment) and buildings (materials trickle) draw
   **directly and automatically from the faction treasury** every settlement tick — no
   player action, no debt, no bankruptcy state (owner Q, post-close clarification:
   deliberately the simplest model — comprehension cost beats simulation fidelity).
   Shortfalls decay org (military) or condition (buildings; condition 0 = inactive until
   repaired). **Shortage order**: the same leader-configurable priority list that drives
   power allocation (phase 1) drives upkeep — one list to understand, not two; default:
   buildings before counters, counters by training recency (fresh troops starve first).
4. **People** — population consumes `food` (`food_per_pop_tick`); surplus + housing headroom
   (residential capacity) + safety (no adjacent hostile/infected cell) → growth; famine →
   decline + stability hit. **Food storage & spoilage (D32)**: `food` has a storage cap
   (granary capacity from residential/faction-core levels); food income above cap sits in an
   *unstored* buffer that **spoils at the next round boundary** if not consumed — harvest
   gluts must be eaten, exchanged, or lost.
5. **Gauges** — drivers integrate (all PLACEHOLDER weights):

| Gauge | Up | Down |
|---|---|---|
| stability | food security, full power, policies, safehouse coverage | famine, blackout, lost battles, infection pressure (ai_factions §4.4), approvals backlog |
| unity | tavern, won battles, focus completions, shared hardship policies | casualties, purges/kicks, leader overreach (rejected tickets ratio), long wars |
| political_points | policy-center level × stability factor | spent on policies (§8) |

Unity is load-bearing beyond mood: **unity < 60% opens the faction to same-bloc war
declarations** (D16, governance.md §7).

### 3.1 The physical energy bridge (D33 — powered cells touch the real world)

In **controlled cells marked powered**, the game world and the Chess energy economy sync
bidirectionally. All bridge devices are **registered at placement** (block events), never
found by scanning; all are serviced on the settlement cadence (10 s), never per game tick;
per-faction device budget `energy_bridge_devices_max` (PLACEHOLDER 64) — no unbounded hooks.

The principle is **auto-feed, not cost-waiver** (D38 correction): powered cells keep
designated devices *supplied*; nothing becomes free.

**Registration is authorization (A9).** A bridge device serves only if **registered to the
faction controlling its cell**, and registration is a permission-gated act (construction/
economy domain): on placement by an authorized member, or — for devices that arrive without
place events (Picasso path-B interiors, pre-existing blocks) — via an explicit *register
device* interaction (wrench/command) and a **receipt-driven registration pass** that offers
devices found inside newly materialized buildings. Unregistered devices in powered cells get
nothing; an intruder's tank is furniture. Every device carries a per-round draw quota
(leader-configurable, defaults from `energy_bridge_defaults`), and bridge draws integrate
with the D8 quota ledger — the bridge is a *delivery* channel, never a quota bypass.

| Bridge | Direction | Mechanism |
|---|---|---|
| Designated consumer auto-feed | Chess → world | datapack list `energy_bridge_consumers.json` of block ids auto-fed while their cell is powered — pinned v1 entry: `superbwarfare:charging_station` (auto-charges); list extensible by the owner |
| Coal auto-draw | Chess → world | any **registered** coal-burning block entity in a powered cell (registration = authorization, above), on fuel exhaustion, draws from the faction `coal` stock (hook supplies a real coal unit, stock decrements) |
| Create fluid-tank fuel sync | both | `create:fluid_tank` holding fuel fluids (ethanol family — item form `createdieselgenerators:ethanol_bucket`) registers as a fuel interface: below-full tanks fill at `tank_fill_rate` from faction `liquid_fuel` (Create tank-insert API); if a full tank keeps receiving external fuel, the overflow drains into Chess credit — **metered at the insertion interface by volume in/out accounting (C16: merged fluids carry no tags); bridge-inserted volume never counts toward overflow credit (B7)** |
| CNA terminal power sync | both | the **first-placed** `create_new_age:electrical_connector` per cell registers as the cell's power tap: powered cells inject `terminal_inject_rate` into it; feedback credit = **`max(0, measured_input − injected_actual_this_tick)`** (C16 refinement of B6: subtract what was *actually* injected — zero in unpowered cells — so genuine sub-rate contributions are never confiscated); injection and harvest metered separately |

Failure honesty: if a bridge mod's API is absent/changed, that bridge degrades to off with a
`system/bridge_degraded` event — never a crash (resilient startup).

---

## 4. Buildings — `data/swchess/buildings/*.json`

Every building = **one core block** (block entity carrying id/owner/condition) + a physical
structure obtained by either D2 path: **(A)** player self-builds, places the building's core
block, Chess validates loosely (bounding volume, enclosure %, required sub-blocks — per-building
JSON) and registers; **(B)** UI application → cost reserved → **K construction rounds** of
upkeep (window rounds, integrations.md §3.4) → `construct` work order → Picasso materializes
(template embeds the core block) → receipt activates it. Path B requires the target cell be
controlled; path A also allowed in uncontrolled cells but the building idles until the cell is
anchored (a self-built barracks in the wilderness is a claim *invitation*, not a claim).

| Building | Core function (per-tick unless noted) | Notes |
|---|---|---|
| `faction_core` 阵营核心 | founding seat; treasury & exchange terminal (§7); wide sight; claim-capacity base. **Founding auto-claims its cell** (capacity-exempt, A11); a cell containing a foreign `faction_core` or `occupation_core` is **never purchasable** — HQs fall to war, not to escrow | exactly one; destroying it while the faction holds territory = decapitation crisis (grace, checkpoint 4) |
| `occupation_core` 占领核心 | claims its one cell (D17); chain anchor for purchased cells | not a functional building — no staffing/power; cheap; the MCV (counters_and_combat.md §5.6). Data lives as `buildings/occupation_core.json` with `kind: "claim_anchor"` (A25); core-block items come from datapack recipes + the founding kit; path A's "free" structure is priced in the labor and materials the player physically spent — the asymmetry with path B is intended and stated |
| `barracks` 兵营 | trains/reinforces counters; garrison defense bonus in its cell | training consumes manpower slice + equipment/food (counter templates) |
| `safehouse` 安全屋 | respawn anchor for members; shelters population share during raids; small stability | physical alignment: binds player spawn |
| `policy_center` 政策中心 | political-point income; policy slots (§8); claim-capacity bonus; approvals/votes UI anchor | leveled (1–3): more slots, more points |
| `residential` 居民区 | population capacity + growth rate | population is otherwise capped at a starveling base |
| `lumber_camp` 木材生产 | `raw_materials` income; requires forest-terrain cell (board_and_fog.md §2) | terrain-coupled production — geography matters |
| `toolworks` 工具生产 | recipe A: raw → build_materials; recipe B: raw + power → equipment | the industrial spine |
| `scavenger_camp` 搜刮者营地 | dispatches scavenge missions (§6) | the loot-economy engine |
| `tavern` 酒馆 | unity income; recruitment-rate bonus (population→manpower conversion); rumor intel (reveals a random explored-adjacent cell snapshot) | physical template may use the kaleidoscopetavern block set |

Building JSON carries: cost, K rounds (path B), staffing (manpower reserve), power draw,
per-tick recipe/effects, validation footprint (path A), Picasso template id (path B).

---

## 5. Land Purchase (D17)

`purchase(cell)` — command; legality: cell adjacent to controlled territory, unclaimed (or
`uncontrolled` after de-control), **free of any foreign core block** (A11), within claim
capacity; cost = `land_base_cost` ×
(1 + `k_dist` × chebyshev_distance_to_nearest_core) in build_materials + currency + a manpower
reservation for `purchase_ticks`. Purchased cells chain to the nearest own core
(counters_and_combat.md §5.6 collapse rules).

---

## 6. Scavenge Missions

`scavenger_camp` dispatches a mission: pick target cell → manpower reserved, travel ticks by
distance → gather (yield = f(loot_richness, terrain class); urban ruins rich) → return →
treasury credit (mixed food/raw/equipment/currency per `scavenge_yield_table`).

**The abstract-loot doctrine (D40 — owner ruling on A2, principle worth quoting: "we are
making a game, not simulating an apocalypse"):**

- `loot_richness` is a **purely abstract, renewable resource field**. The physical world is
  never affected by chess-layer depletion, and physical blocks never re-derive the value:
  the first-ever load sets `base_richness` once (sampled count against
  `data/swchess/scavenge_blocks.json`, generated from `DDF可搜刮方块清单_283.txt` + the D20
  workbook junk sheets); thereafter
  `richness = clamp(base − harvested + regen, 0, base)`, with `harvested` a per-cell ledger
  and `regen = loot_regen_per_round × base` each round. Depletion is a *pacing* mechanism,
  not a simulation claim.
- **Hand-looting is a separate, parallel resupply channel by design** — players scavenging
  the same district as an abstract mission is not double-dipping, it is the intended
  on-the-ground gameplay. The two channels share a *theme list*, not a stock.

**Mission legality (A14) aligns with movement legality**: valid targets are explored cells
that are unclaimed, derelict, own-faction, or infected/`env`; a *foreign player faction's*
cell requires `at_war` (the mission is then pillage: logged, favorability −). Hostile targets
impose casualty risk unless a friendly counter garrisons the target (escort — meaningful for
unclaimed-but-dangerous and wartime cells; garrisoning foreign territory already presupposes
the declaration).

---

## 7. Treasury, Exchange & Member Access (D8, D18)

### 7.1 The exchange book — `data/swchess/exchange_table.json`

Rows: `{item_id, resource, deposit_rate, withdraw_rate, currency_price?}` — deposit converts
physical items into treasury stock; withdraw materializes items from stock **into the
terminal block's inventory** (faction core / policy center) for physical pickup — the map UI
authorizes, the world delivers; items never teleport (B13). **The terminal is not a plain
container (C8)**: withdrawal slots are **claim-bound** — only the withdrawing member may
extract; no hopper/automation face, member-only GUI access; unclaimed items return to stock
after `withdraw_claim_ticks` (90). Unclaimed claims follow the treasury (they are stock in
transit), so an intact HQ capture inherits them with everything else. `deposit_rate <
withdraw_rate` value-wise (spread prevents deposit/withdraw arbitrage). The table is **the**
resource half of the Alignment Ledger (integrations.md §2); balancing it is balancing the
economy. **Currency exclusion rule (R10)**: the six currency-denomination item ids never appear in any non-currency exchange row (generator rule) — one item, one ledger identity. **Seed source (D20)**: rows are generated from the owner's planning workbook
`策划案以及文档相关/ItemNameCatch分类汇总_合成设计方案_v1.xlsx` — category sheets map to
resources (基础资源→raw_materials · 组件→build_materials · 枪械/装备/近战武器→equipment ·
实用物品→per-row) — then hand-balanced; the junk sheets seed `scavenge_blocks.json` alongside
the DDF-283 list (§6).

### 7.2 Per-round quotas (D8/D15)

Leadership sets per-member withdrawal quotas per resource; quotas reset at each round boundary
(post-maintenance server start). All withdrawals log `economy/withdrawal` with actor + quota
state.

### 7.3 Access models (polity-mapped, D18)

| Polity | Currency | Model |
|---|---|---|
| Dictatorship | `bullets` | **apply-to-use** 申请使用: every withdrawal is a ticket to the leader (governance.md §5); quota is the auto-approve ceiling the leader may set per member |
| Republic (presidential) | `bottle_caps` | **interest accounts** 定存/活期: members hold personal accounts; interest is **paid from the faction treasury** (D22) at rates indexed to faction productivity (`rate = k × faction_production_index`; demand rate < fixed-term rate; early withdrawal of fixed-term forfeits interest); solvency is governed by the reserve discipline of §7.4 |
| Democratic centralism | `grain_tickets` | **deposit-to-use** 存储使用: per-member ledger — withdraw up to own deposits × `k_social` (≥1, the collectivist multiplier), within one's own jurisdiction (sector) |

### 7.4 Financial stability — reserves, bank runs, bailouts (republic, D22)

```
deposits D        = Σ member account balances (demand + fixed-term)
reserve floor R   = reserve_ratio × D          (reserve_ratio PLACEHOLDER 0.3)
liquid treasury L = treasury holdings in bottle_caps (+ exchangeable at haircut, k_haircut 0.8)
```

- Interest accrues each round and is payable only while `L ≥ R` **after** payment. The
  president (or economy minister) can see `L`, `R`, and the projected payment on the dashboard.
- Leadership spending that would push `L` below `R` triggers a **reserve-breach warning**
  (notice + `economy/reserve_breached`, faction-visible) but is **not blocked** — mismanagement
  is a legal move; the system is deliberately crashable.
- A missed interest payment starts a **bank-run state**: withdrawals capped to a trickle,
  fixed-term accounts frozen, stability and unity take per-round hits (which can drop unity
  under 60% — a financial crisis literally exposes the republic to same-bloc war, D16).
  The state ends when `L ≥ R` and one full round's interest is paid on time — with the
  wall-clock fallback of ARCHITECTURE §12.3 (A23): during ops gaps, `fallback_round_days`
  real days count as a round-equivalent for interest cycles and the bank-run exit, so a
  crisis is always escapable by in-game action.
- **Rescue is focus-tree material** (D22): the republic mainline must carry bailout-type
  focuses (`focus_bindings.json` effect kinds: `treasury_grant`, `reserve_ratio_relief`,
  `account_haircut_amnesty` — a one-time write-down of deposits with reduced unity damage).
  Content authoring is the owner's; the effect kinds are the architecture's.

### 7.5 Currencies as items

Each currency has physical item forms in **three denominations at a fixed 1 : 20 : 100
ratio (D50)** — the treasury accounts in base units; the exchange terminal converts
denominations at face value automatically (deposit any, withdraw greedily-largest or
player-chosen). No internal FX between denominations, ever; *cross-currency* exchange is the
separate `currency_barter_table` at the faction core (PLACEHOLDER; inter-bloc trade is
emergent, not a market simulation).

| Currency | ×1 | ×20 | ×100 |
|---|---|---|---|
| `bullets` (existing TACZ ammo, zero registration) | 手枪子弹 `tacz_unidict:pistol` | 步枪子弹 `tacz_unidict:rifle` | 狙击子弹 `tacz_unidict:sniper` |
| `bottle_caps` (registered items, art delivered) | 农夫三泉瓶盖 (red plastic; renamed from 农夫山泉 per D51 — trademark-safe parody) | 冰红茶瓶盖 — tooltip lore: a lucky one with **"再来一瓶"** printed on the back is worth a cola cap — the lucky variant is loot/mint-only content: `face_value: 100` on deposit, never produced by withdrawal (R17) | 可乐瓶盖 (iron-made, best craftsmanship, most durable — lore) |
| `grain_tickets` (registered items, art delivered) | 粮票 一号 | 粮票 二号 | 粮票 三号 (serials/colors differ) |

Assets organized at **`Chess/assets/currency/`** (README maps every file to its item and
face value; originals `瓶盖与纸币.zip` + `农夫山泉瓶盖.png` kept untouched).

---

## 8. Policies — `data/swchess/policies.json`

Enacted at the policy center for `political_points`; slots limited by center level; effects are
modifier bundles: stability drivers, production efficiency, member withdrawal quota defaults,
recruitment rates (the owner's stated policy domains). Sample v1 set (PLACEHOLDER): rationing
(食物配给) · curfew (宵禁) · conscription (征召) · open granary (开仓). Enact/repeal are
commands → polity permission rules apply; effects apply from next settlement tick; all logged.

---

## 9. Derelict Assets

Dissolved factions' cells, buildings, and counters become `derelict`: production stops,
condition decays, cells lose anchors after `derelict_grace_rounds` (2), loot_richness of
derelict building cells rises (ruins are lootable). Strains treat derelict cells as soft
frontier.

---

## 10. Claim Capacity (D19 formula, normative shape)

```
capacity = base
         + k_member × Σ_players (1 + k_contrib × contribution_score(player))
         + k_pop    × population
         + policy_center_bonus(level)
         + Σ focus_unlock_bonuses
```

`contribution_score` accrues per player from completing bloc-focus tasks (scoring events
defined in integrations.md §4; the score is bloc-portable — it follows the player). All
coefficients PLACEHOLDER.

**Over-capacity sheds (A13)**: capacity gates acquisition *and* retention. While
`controlled_cells > capacity` (member loss, focus changes), the faction takes a stability
drain per tick and auto-de-controls its farthest-from-HQ cell each round
(`overcap_shed_per_round` 1) until compliant — invite→claim→leave locks in nothing.

---

## 11. Config & Data Added

`k_pop 0.6` · `food_per_pop_tick` · growth/famine rates · gauge driver weights ·
`land_base_cost`/`k_dist`/`purchase_ticks` · `scavenge_range 8` / yield table ·
exchange spread floors · interest `k` / `k_social` · `derelict_grace_rounds 2` ·
capacity coefficients (§10). Data files: `resources.json`, `buildings/*.json`,
`exchange_table.json`, `scavenge_blocks.json` (generated), `policies.json`,
`currency_barter_table.json`.

---

## 12. Open Questions (v0.1)

1. ~~Interest funding~~ **Resolved (D22)**: paid from treasury under a reserve requirement;
   crashes possible by design; bailout focuses required (§7.4).
2. ~~Food spoilage/storage~~ **Resolved (D32)**: storage caps + unstored food spoils at the
   next round boundary (§3 phase 4).
3. ~~Faction-core destruction~~ **Resolved (checkpoint 4)**: grace timer
   (`core_decapitation_grace_rounds` 1).
4. ~~Power alignment~~ **Resolved (D33/D38)**: generator buildings + fuel stay the internal
   economy; the physical bridge (§3.1) handles world alignment with pinned item ids
   (electrical_connector · fluid_tank · ethanol_bucket · charging_station). The consumer list
   is datapack-extensible; further entries welcome any time.
5. ~~Quota scope~~ **Resolved (checkpoint 4)**: yes — democratic centralism additionally
   scopes quotas per sector (pairs with jurisdictions).
