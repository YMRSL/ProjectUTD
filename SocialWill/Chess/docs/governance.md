# Factions & Governance — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> Encodes the three-polity design (owner brief + D16/D18): founding flows, permission
> machinery, the approval-ticket system, votes, and diplomacy. Blocs (polity-level meta-camps)
> are defined here; their focus-tree mechanics live in integrations.md §4.

---

## 1. Blocs — `data/swchess/polities.json`

A **bloc** is the set of all factions sharing one polity. Blocs are not organizations — they
have no treasury, no leader, no commands. They carry exactly four things:

1. a **base currency** (D18): dictatorship → `bullets` (D49), republic → `bottle_caps`,
   democratic centralism → `grain_tickets`;
2. a **focus tree** (国策, merged mainline-story + tech tree): one bloc mainline in the
   global quest file, **gated per bloc** — Chess auto-completes the bloc's gate quest for
   each of its teams at founding (C9: no per-team chapter attachment exists; visibility is
   authored via dependency gating) — **progress shared bloc-wide**, exclusive branches
   resolved first-completed-wins (D9; mechanics in integrations.md §4.2);
3. an implicit **non-aggression norm**: same-bloc war is legal only against a faction whose
   unity < 60% (D16 — see §7);
4. **pairwise favorability 好感度** (D21): a gauge per bloc pair that scales the internal cost
   of declaring cross-bloc war (§7). Its rise/fall dynamics are undesigned (open Q6).

## 2. Faction Lifecycle

```
place faction_core block → founding wizard (polity choice = bloc entry)
→ invite founding members (online players) → polity minimums met → FOUNDED:
  the faction core's cell is auto-claimed (capacity-exempt founding seat, A11) ·
  FTB party team created/mirrored · bloc gate quest auto-completed (C9) ·
  starting package granted (initial manpower/resources + core-block kit per polity, PLACEHOLDER)
→ … → DISSOLVED (leader command + confirmation vote where polity allows;
  or membership below minimum for `dissolve_grace_rounds`): assets → derelict
```

Founding minimums, leader counts, and starting packages are `polities.json` fields; the owner
balances them ("初始人力不同"). One player belongs to at most one faction. Membership changes
(join/leave/kick) are commands with polity-specific approval paths (§5) and all are logged.

## 3. The Three Polities

| | Dictatorship 独裁 | Republic (presidential) 共和总统制 | Democratic centralism 民主集中制 |
|---|---|---|---|
| Leaders | 1 dictator | 1 president + ministers (one per domain) | 1 supreme leader |
| Min members (PLACEHOLDER) | 1 | 4 | 3 |
| Operating model | only the dictator operates freely; **every other member's operation becomes an approval ticket to the dictator** (§5) | each minister operates freely **within their domain**; cross-domain ops ticket to the owning minister or president | every member operates freely **within their assigned jurisdiction** (sector, board_and_fog.md §5); supreme leader assigns jurisdictions, per-member permission masks, and per-member resource priority weights (power/quota ordering) |
| Member access to treasury (D18) | apply-to-use | interest accounts | deposit-to-use |
| Overthrow | **impossible** | recall motion, >50% of political-identity holders | recall motion, >50% |
| Terms | none | **scheduled election every 5 rounds** (`republic_term_rounds`, D23) | none |
| Succession | dictator names heir; none → oldest member | term/recall/vacancy → election motion (plurality) | recall/vacancy → election motion |

Domains (republic ministries and the permission-matrix axes, v1 set, PLACEHOLDER):
`military` (counters, war) · `construction` (buildings, purchases, occupation) ·
`economy` (exchange, quotas, missions) · `interior` (membership, policies) ·
`diplomacy` (war/peace, inter-faction agreements).

## 4. Permission Machinery

One **permission matrix** — `(member, domain, operation-class) → allow | ticket | deny` —
evaluated at the single command choke point (ARCHITECTURE §12.1). Polities are presets over
this matrix; democratic centralism adds per-member masks and a **jurisdiction scope check**
(the target cell/building must lie in the member's sectors). Leaders hold `allow` everywhere
except polity-hardcoded exceptions (a dictator cannot exempt themself from being unremovable —
there is nothing to exempt; a republic president cannot strip the recall motion). The matrix is
data (polity preset JSON + per-faction overrides), enforcement is code, every evaluation
outcome is loggable (`orders` topic).

**Political identity**: all full members hold it (vote weight 1). A polity may define a
non-voting `recruit` grade (`polities.json` flag; probation promotion by leader). Only
political-identity holders count for motion quorums.

## 5. Approval Tickets

A command that evaluates to `ticket` is parked, not rejected:

```
Ticket { id: tkt_<seq>, command, actor, approvers (matrix-derived), cost_preview,
         created_tick, expires_tick, state: pending → approved | denied | expired }
```

- Approvers get a **notice** (map UI inbox + chat line). Approve/deny is itself a command
  (logged, `governance/ticket_resolved`).
- **Expiry = deny** (`ticket_ttl_ticks`, PLACEHOLDER 360 ≈ 1 h): silence must not become
  consent, and expired-as-denied is visible to the requester (D-REFUSAL).
- Costs are **reserved** at ticket creation and released on deny/expire — a dictator's inbox
  must not be a resource-desync engine.
- An approved ticket **re-validates game state at apply time** (A27); if the world moved on
  (target captured, building gone), the requester gets `approved_but_failed(reason)` and the
  reserved costs release — approval is permission, not a time machine.
- **Concurrency (C6)**: with multiple eligible approvers, the first resolution by ingest
  order wins; later attempts get `already_resolved`. Approval racing expiry inside one tick
  is settled by phase order — command apply precedes governance timers, so a same-tick
  approval beats the TTL. The requester may **cancel** their own pending ticket at any time
  (reservations release immediately).
- The dictatorship runs almost entirely on this system; its UX ceiling (a dictator drowning in
  tickets) is deliberately the polity's designed weakness. Quotas (§ econ 7.2) are the relief
  valve: within-quota withdrawals auto-approve.

## 6. Motions (votes)

```
Motion { id: mot_<seq>, kind: recall | election | dissolve | custom_polity,
         eligible: political-identity holders at creation, window_hours (wall clock, 24),
         threshold: >50% of eligible (recall/dissolve) · plurality (election),
         state: open → passed | failed }
```

- Offline members may vote any time inside the window (wall-clock deadline, notice on login).
  **Counting cut (C7)**: votes received before the wall-clock deadline count; the tally
  executes at the next governance phase even if it runs after the deadline. Votes may be
  changed freely until the deadline; the last ballot stands.
- **Recall** (overthrow): unavailable in dictatorships; passing deposes the leader, opens an
  election motion; the deposed keeps membership (exile is a kick, a separate act).
- **Scheduled elections** (republics, D23): an election motion auto-opens every
  `republic_term_rounds` (5) rounds; the incumbent may run; plurality wins.
- Threshold counts *eligible members*, not votes cast — abstention is a "no" (prevents
  3-of-20-online coups; **confirmed D23**).
- **Churn-proofing (A15)**: the eligible set *and* its size freeze at motion creation; cast
  ballots survive the caster's kick; mid-motion joiners are ineligible; and while a recall
  against a member is open, that member cannot kick eligible voters (the electorate cannot be
  purged by its defendant).
- One open motion per kind per faction; `motion_cooldown_rounds` (1) after a failed recall.

## 7. Diplomacy & War Legality (D16)

Relations between factions: `neutral` (default) · `at_war` · `ceasefire` (timed neutral,
non-redeclarable for its duration). No formal alliances v1 (same-bloc non-aggression is the
alliance; explicit alliance/vassalage is v0.2 material).

- **Cross-bloc war** (D21): declarable at will by the leader/military-domain holder, but the
  declaration hits the declarer's **stability and unity**, scaled by inter-bloc favorability:
  `cost = base_cost × favorability_factor(bloc_declarer, bloc_target)` — attacking a friendly
  bloc tears your own society; attacking a despised one is nearly free. Favorability is a
  bloc-pair gauge; **its dynamics (what raises/lowers it) are undesigned** — open Q6.
- **Same-bloc war** (D16, scope confirmed narrow): declarable **only while the target's
  unity < 60%** — low unity reads as lost legitimacy, and the bloc's norm no longer shields
  the faction.
- Wars end by mutual `ceasefire` command or the disappearance of a side. AI `hostile_all`
  factions ignore this section entirely.

**Physical hostility & the cell-integrity siege gate (A5, model per D42; SC1–SC5 applied).**
Players themselves are not gated by board legality — instead, the physical layer *is* the
siege:

1. **Cell integrity HP**: every controlled cell carries
   `integrity_max = hp_base + k_hp × population_housed(cell)` — anchored in **abstract**
   quantities (SC3a): `population_housed` is the faction population apportioned across its
   residential-capacity cells **proportionally to each cell's residential capacity, the
   faction core counting as one `hp_base`-grade house** (B14); non-residential controlled
   cells get `hp_base` only.
   Resident-NPC *density* (D25 config) therefore sets channel width — how fast HP can be
   ground down — never the pool size. Recomputed each settlement; regenerates
   `cell_hp_regen`/tick while the named predicate **`cell_contested_recently`** is false
   (true iff the cell took integrity or battle damage within `hp_regen_lockout_ticks`, 18;
   SC3b). Board field: board_and_fog.md §1.
2. **First blood is the declaration**: killing resident NPCs (D25, when enabled) or garrison
   mirrors inside faction territory auto-creates `at_war` with full D16/D21 legality and
   costs (an illegal intra-bloc attack — target unity ≥ 60% — has its territorial effects
   reverted at next settlement while the attacker still eats the unity/stability costs;
   favorability is a bloc-pair gauge and does not apply within a bloc — SC4). Factionless
   attackers are flagged hostile to the victim (attackable anywhere; bounty surface v0.2).
   The build-log recorder + entity attribution name the attacker; alerts fire faction-wide
   (`territory/cell_under_attack`).
3. **Each such kill deducts cell HP and the owner's *population*** (SC1 — manpower is a
   derived quantity and cannot be debited directly; the manpower cap shrinks with the
   population behind it, consistent with the Alignment Ledger row and A1). Mirror deaths
   additionally write back to their counter per D3. **Mob/environment kills drain HP and
   population the same way but never declare anything** (SC5) — intended: strain raids
   soften cells for everyone. Under D44 this enables no bloodless decapitation: the *mining*
   itself declares.
4. **Core blocks are unbreakable while their cell's integrity > 0.** At 0 the core is
   exposed and minable (alert + attribution as before). Decapitation is a siege through the
   people, never a lock-pick past them. With residents disabled, garrison mirrors are the
   only HP channel — an ungarrisoned, unpopulated cell's core is correspondingly soft
   (working as designed: empty land is takeable — at the §5.6/econ §5 price, see D44).
5. **Mining a foreign registered core block is itself a declaration (D44, closes SC2)**:
   breaking any exposed foreign core (occupation, faction, or building core) runs the same
   auto-declaration machine as first blood — `at_war` + D16/D21 legality and costs; illegal
   intra-bloc → revert + costs; factionless → hostile flag. Monster-washed HP, vulture
   third parties, and undefended border cores all pay the same price as an honest war.
   **Total immunity while HP > 0 (B11)**: "unbreakable" includes explosions, pistons, and
   mob grief — not just mining. The laundering family is additionally closed *benefit-side*:
   **claiming or purchasing a cell de-controlled by sabotage within `sabotage_taint_rounds`
   (2) runs the auto-declaration machine against the victim** — an unattributable creeper
   chain can still fell an exposed core, but whoever takes the land pays the war price.
   Two C13 refinements: **`claimant == victim` is exempt** (D29's discounted grace
   re-purchase is the canonical recovery path — a victim re-anchoring their own cells
   declares nothing), and **the taint is visible in every claim/purchase preview**
   ("claiming this cell declares war on <victim>") — informed consent turns the landmine
   into a fence (D-REFUSAL preview honesty).
6. **Proxy accountability (D44)**: hostile acts within `leaver_accountability_rounds` (2) of
   leaving a faction are charged to the former faction (the quit→sabotage→rejoin dance
   changes nothing); players carrying a hostile flag cannot **join, rejoin, or found** any
   faction until it expires (`hostile_flag_duration_rounds` 2; founding closed per B12).
7. Once legitimately `at_war`, the same kills are ordinary war actions (no re-declaration;
   HP/population deductions unchanged).

## 8. Events (topic `governance`)

`faction_founded` · `member_joined/left/kicked` · `leader_changed` · `ticket_created/resolved`
· `motion_opened/closed` (with tallies) · `war_declared` / `ceasefire` · `policy_enacted/
repealed` · `jurisdiction_assigned` · `quota_changed` · `ftb_drift_corrected`
(integrations.md §4). Every entry carries `actor` and lands fog-appropriately
(`visibility: faction:<id>` for internal politics, `public` for wars and founding).

## 9. Open Questions (v0.1)

1. ~~Recall threshold base~~ **Resolved (D23)**: eligible-members base, abstain = no.
2. ~~Republic terms~~ **Resolved (D23)**: scheduled election every 5 rounds.
3. ~~Minister vacancy~~ **Resolved (checkpoint 4)**: unfilled domain falls to the president.
4. ~~Jurisdiction overlap~~ **Resolved (checkpoint 4)**: sharing allowed; conflicts resolved
   by leader priority weights.
5. ~~Kick refund~~ **Resolved (D34)**: deposits converted by exchange rate, importance and
   proportion, delivered **into the kicked player's ender chest**.
6. ~~Favorability dynamics~~ **Resolved (D34 + D39a)**: all four drivers adopted — inter-bloc
   trade volume (+), focus choices flagged pro/anti-bloc (±), wars and battle casualties
   between the blocs' factions (−, decaying), returning cleansed territory / honoring
   ceasefires (+). The table is unified over `(bloc|ai_faction)` pairs; the AI-side driver is
   completing that AI faction's NPC quests (+) — **pump-proofed (B8, widened by C19)**: the
   per-round gain cap (`favorability_gain_cap_per_round`) covers **all positive drivers**
   (quests *and* trade volume — wash-trading is additionally self-taxing through the exchange
   spread, but the cap is the guarantee), and every pair decays toward its baseline
   (`favorability_baseline`, `favorability_decay_per_round`) — peace is maintained, never
   banked. Weights PLACEHOLDER.
