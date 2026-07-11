# MiroFish — Revision Log & Decision Record

Same role as the Picasso and Chess REVISION_LOGs: every structural decision, review finding,
and adjudication lands here with a stable number; spec prose stays clean, debate lives here.
Review rounds append lettered sections (A, B, …) per the established double-Fable protocol.

---

## §0 Founding Decisions — **PARTIAL (first ruling batch 2026-07-09; checkpoint-0 v2 open)**

Owner direction given 2026-07-09 (chat), recorded as rulings **D1–D5**. These reshape draft 1
(which specified a screenwriter-only layer); `ARCHITECTURE.md` is bumped to **v0.1 draft 2**
accordingly. Remaining open choices are re-listed as **checkpoint-0 v2** in `ARCHITECTURE.md`
§9 (11 questions, ⏳REC recommendations in place).

- **D1 — Scope: two subsystems, one layer.** The MiroFish layer = the original MiroFish
  **agent chat network** (the *Stage*: OASIS dual-platform social simulation) + the
  **screenwriter** (the *Desk*: draft 1's authoring pipeline). One repo, one doctrine set,
  two runtimes. (Owner: "无非是需要两个重要的点，一个是原版MiroFish的agent聊天网络，一个是编剧层".)
- **D2 — Repositioning: the Stage supplies reference material, not agents.** The old
  project's purpose — simulation output becomes game agents/NPCs directly — is retired. The
  simulation exists **for the screenwriter's reference** (doctrine **M-IMAG**: imagination,
  never facts, never world entities; world-reaching content passes the Desk's compile gate
  only). (Owner: "让模拟层主要是供应agent的目的转换给编剧进行参考".)
- **D3 — Dual platform stays: Twitter = public sphere, Reddit = private sphere.** The public
  env models cross-faction public discourse; the private env(s) model closed-circle
  discourse. (Owner: "模拟部分依然还是oasis的双端模拟 twitter和reddit，因为一个偏公域一个偏私域".)
  **Explicitly overrides** the pre-refoundation blueprint decision "D5 单平台模拟" — that
  decision optimized SimResult volume per cost; the new purpose (visibility-differentiated
  narrative material feeding M-VISIBILITY-aware authoring) is precisely what a second,
  private sphere provides. Exact private-sphere partition (per-faction envs vs one env with
  communities) is deliberately **not** ruled here — it is spike-one territory (ARCH §8).
- **D4 — Genesis duty: the Desk co-authors settings and births the cast.** From day 0
  (before any world runs) the layer assists the owner in writing story settings for
  different works/contents, and generates the corresponding AI agents **with prior memories
  baked in** (AgentCard prior-memory section). (Owner: "让编剧层在一开始就能协助我去撰写不同内容的
  故事设定并生成与之对应的带有一些先验记忆的ai agent".)
- **D5 — NPC supply serves both Chess and Picasso.** The Desk's character/NPC products must
  be consumable by Chess (D24/D25 counters, `mark` orders with `dialogue_id`+`faction`,
  ask-8 affiliation/quest records) and by Picasso-manifested works. Recorded narrowly: this
  does **not** overturn M-ONEWRITER — Picasso-side physical props still route through
  Chess's lanes pending Q9. (Owner: "让他们能写出供给picasso和chess可以调用的npc".)

## §0.1 Workflow notes (inherited, binding from day one)

- **Doc-first, code-later**; balance numbers are PLACEHOLDER config by doctrine (D-BALANCE) —
  reviewers attack mechanisms, the owner sets numbers.
- **Docs in English, review discussion in Chinese** (Chess D4 practice).
- **Checkpoint cycles**: partial doc set → owner discussion → numbered rulings here → next
  batch. Open questions accumulate in each doc's tail section.
- **Adversarial review**: rounds append as §A, §B, … — attacker findings numbered A1…,
  defender responds per item (accept → doc edit noted here · rebut → reasoning here).
  Role-swap and spot-check closure per the Chess §A/§C precedent.
- **Freeze checklist**: before any freeze, cross-layer tables are diffed against every ruling
  batch since the last freeze (Chess A10 lesson).

## §0.2 Inherited structural law (adopted before any ruling, by precedent)

- **M-SPIKE — external-surface assumptions must be spike/jar-verified before they become
  mechanism text.** Source: Chess round 3 (C9/C10). Applications here: **spike zero** = CNPC
  script layer (`content_pack_format.md` may not be written before its report); **spike
  one** (added in draft 2) = the vendored OASIS surface (private-sphere partition,
  initial_posts seeding, interview IPC, determinism envelope, cost curve) — cheap because
  the source is in hand, mandatory all the same.
- **Never-blocking narrative** (Chess D37/Q7): every MiroFish touchpoint has a deterministic
  default; absence degrades flavor, never function. Draft 2 extends it inward: the Desk
  never waits on the Stage.
- **Visibility guardrail** (Chess `event_log.md` §6): MiroFish may know everything, must
  narrate audience-appropriately. Draft 2 enforces at two mechanical gates (seed compile +
  pack compile), both Desk-owned.
- **Determinism seam** (Chess B1): every MiroFish override enters Chess as a
  `bridge/ai_override` ingest event before any decide pass reads it.

## §0.3 Lineage imports (2026-07-09, draft 2) — prior art adopted as ⏳REC, not rulings

The pre-refoundation corpus was mined on 2026-07-09 (three sweeps: MiroFish-main source ·
old SocialWill T-series specs · 开发蓝图 contracts/work-packages). Proven design imported
into draft 2 **as recommendations with attribution** (full table: `ARCHITECTURE.md` §11):
batch semantics (blueprint D4) · top-K batch-end interviews (D2) · SimResult
verbatim-substring evidence (WP-205) · `[近期]` carryover (WP-206) · injections with
scope×visibility (D15) · daytime rhythm + console (D16) · `source_agent` identity mapping
(D17) · no-Zep cast generation (D6/WP-202) · seeded reproducibility (D8) · LLM
record/replay cache (D7) · downed/"薛定谔" fate mechanism (D14) · WS2 CNPC capability
catalog (as spike-zero *hypotheses*) · WP-201 vendoring plan. Blueprint "D5 单平台" is the
one item **overridden** (by ruling D3 above). The old T-series' wargame/economy/AP material
is superseded by Chess and stays background reading.

Numbering hygiene: references to the old blueprint's decisions are always written
"blueprint D<n>" — bare D<n> in this repo means a MiroFish ruling from this log.

---

## §0.4 Checkpoint-0 v2 — adjudication round 1 (2026-07-09): rulings **D6–D11**

Owner answered the eleven-question list in chat; six land as rulings now, five await a
second pass after concept explanations (Q3 editorial gate · Q5 tone bible · Q7 scale shape ·
Q9 Picasso paths · Q10 remainder · Q11 remainder — explained to the owner in Chinese in
chat, re-listed in `ARCHITECTURE.md` §9).

- **D6 — Two runtimes confirmed (Q1).** Desk = window-cadence external agent service;
  Stage = owner-triggered batch service, decoupled from maintenance windows. (Owner: "确认".)
- **D7 — MCP management surface + frontier-only authoring + headless dispatch (Q2 partial;
  new requirement).** (a) The layer must expose an **MCP management/monitoring interface**
  so external agent sessions (Claude Code, Codex, …) can inspect and operate MiroFish —
  batch status, staged packs, gate promotion, graph/material queries (final tool list
  belongs to `integrations.md`). (b) **World-facing content authoring (CNPC scripts/packs)
  is reserved to frontier-class models** (owner names the Fable-5 / GPT-5.5 tier) — cheap
  models never author anything an NPC will say or do (breakage aversion). (c) Dispatch
  shape: the window orchestrator launches **headless authoring sessions** (`claude -p …` /
  `codex exec …`, both officially non-interactive-capable, running on the owner's
  subscriptions) which connect back to the layer's MCP tools; the VS Code UI is not in the
  loop. The pack-vs-direct-scripts consumption question remains **spike-zero-gated**.
- **D8 — Offline discipline sharpened (Q4).** No live/realtime LLM anywhere, confirmed; and
  **all world-writes (packs, overrides) happen during maintenance windows only**. Recorded
  reading (flagged to owner): Stage batch *runs* write nothing world-side (M-IMAG) and stay
  schedule-free per D6; *publication* is window-bound. The mid-batch injection ban stays
  ⏳REC (blueprint D4 lineage), not separately ratified yet.
- **D9 — Character permanence & promotion (Q6).** Named cast members die only by narrative;
  AgentCard→Character promotion (`source_agent`) is the sanctioned path from sim persona to
  world NPC; adaptation never transfers authority. (Owner: "确认".)
- **D10 — Rumor channel ON (Q8).** Degraded cross-faction leaks allowed under the §5.3
  rules (≥1 round delay, fuzzed coordinates, unnamed sources); public-sphere chatter is the
  sanctioned content source. (Owner: "确认，允许降级泄密".)
- **D11 — Graph brains & cost posture (Q10/Q11 partial).** The continuity graph's own LLM
  calls go to **DeepSeek and Qwen (DashScope) APIs — both interfaces retained**; keys are
  provided out-of-band into local env config and are **never committed to any repo**.
  Cloud-graph dependency rejected (owner, this round: minimize cloud deps). Division of
  labor fixed: cheap APIs for mechanical extraction (graph internals, Stage); frontier
  sessions for world-facing authoring (per D7). Whether the graph ships in v1 or stays a
  spike-two-gated upgrade is **still open** (Q11 remainder).

Still open after round 1: **Q3 · Q5 · Q7 · Q9 · Q10 remainder (Stage model, budgets, cache
adoption, review policy) · Q11 remainder (graph in v1 vs spike-gated)**.

---

## §0.5 Checkpoint-0 v2 — adjudication round 2 (2026-07-10): rulings **D12–D17**

Owner's second pass answers Q3/Q5/Q7/Q9/Q10 and adds two design directives (console model,
web frontend). Only **Q11** (graph in v1) remains open — re-explained, awaiting a one-word
answer.

- **D12 — Gate model: auto-publish with owner interception (Q3 + Q10 review, merged).**
  Default flow is **fully automatic to live**; the compiler (mechanical gate) always runs.
  Human review is **opt-in by ticket**: the owner may file a hold/post-processing ticket
  before the Stage batch ends (i.e. before auto-export into save-usable content begins); a
  held flow requires explicit owner confirmation to go live. Post-live, the owner may still
  interview and request amendments to live content **until the maintenance window closes**;
  every hold, confirmation, and amendment is logged in `mirofish_log/`. (Supersedes the
  draft-2 ⏳REC staged→live default.)
- **D13 — Tone bible confirmed (Q5).** Lives in this repo as data; owner+Desk co-authored
  (D4); content Chinese, docs English.
- **D14 — Scale shape M (Q7).** One public env (cast ~50) + one private env per major
  faction (3–4 envs, ~15 each). All numbers remain tunable config; the *shape* is ruled and
  drives spike one and the D16 budget.
- **D15 — Picasso seam: direct work orders IN (Q9; supersedes the draft-2 no-direct-asks
  ⏳REC).** The Picasso session's own analysis (relayed by owner) established the queue is
  mechanically compatible (`on_behalf_of.layer` free-form; `construct`/`mark`/`restyle` are
  the three types needed) and offered a contract amendment; with the contract owner
  amending, direct is strictly better than laundering narrative intent through Chess
  `mark`/`restyle` (which would pollute Chess provenance). Cross-layer answers delivered:
  1. **Direct write** to `picasso_workorders/pending/` with `on_behalf_of.layer:
     "narrative"` — auditable, receipted; wargame_interface.md §7's writer list gains a
     narrative entry (Picasso-side edit: three wording spots + one field note).
  2. **Expiry**: narrative orders default `expires_after_round: null` (never auto-expire;
     explicit withdrawal instead); when set, the value is read in **maintenance windows**,
     the narrative layer's natural clock.
  3. **Degrade orders**: yes (plot demolition is routine); MiroFish accepts H4 journal
     gating, mandatory `reason`, and rejection — structurally free under M-TRUTH:
     **author on receipt, not on request** (content may reference an ordered
     construction/demolition only once its journal entry exists; a rejected order never
     becomes narratable). Ask: rejections should carry a reason for thread rewriting.
  Journal read path: **blessed file read** (adopted — coherent with the file-based queue;
  needs the one-line Picasso-side pending-edit). M-ONEWRITER preserved: orders are
  requests, not world-writes; Picasso remains the world's only writer.
- **D16 — Stage economics (Q10).** Stage model = **DeepSeek**. Per-window simulation
  budget: **hard cap ¥20, target ¥6**. Record/replay cache **adopted** (lineage blueprint
  D7). Spike one's cost-curve item measures against these numbers; levers if ¥6 is tight
  under shape M: activation curve, rounds per batch, off-peak pricing, cache hits. Budget
  metering is orchestrator-side; near-cap behavior (⏳REC): wind down at a round boundary
  and export normally, keeping batches atomic.
- **D17 — Console model & web frontend (new).** MiroFish capabilities are MCP-toolized so
  the owner drives the layer (and cross-layer coordination with Picasso) primarily
  **through Codex/Claude sessions** — extends D7 from management surface to primary UX.
  Additionally MiroFish gets a **usable web frontend** (unlike Picasso). Lineage asset: the
  source repo ships `frontend/` + `backend/` (web UI over the sim) — revival candidate,
  reuse assessed alongside spike one. ⏳REC: v1 frontend is read-only monitoring (batch
  status, feeds, packs, budgets); mutating actions stay MCP-only.

Open after round 2: **Q11 only** — v1 graph-less with graphiti as a spike-two-gated
upgrade (⏳REC), or ship the local continuity graph in v1.

---

## §0.6 Checkpoint-0 v2 — final ruling (2026-07-10): **D18** — checkpoint CLOSED

- **D18 — Graph ships in v1 (Q11: 带图).** The graphiti-based continuity graph is a v1
  component, not a spike-gated upgrade. Owner rationale: even if retrieval underdelivers,
  the relation map — dressed with NPC skins/avatars — is **self-media source material**;
  this adds a second consumer besides Desk continuity: visualization/export through the
  D17 web frontend. Consequences: **spike two (graphiti verification) becomes a
  prerequisite** and joins spikes zero/one (§8); the §4 domain model gains a
  Continuity-graph entity; Genesis-declared relations seed the graph. Mechanism specifics
  remain [SPIKE two]-gated (M-SPIKE — role may be stated, unverified capabilities may not).
- **Keys**: the DeepSeek API key was received out-of-band and parked in `.env.local`
  (untracked; `.gitignore` added). The Qwen/DashScope key is still pending (both
  interfaces retained per D11). Keys never appear in docs, logs, or memory — location
  references only.

**Checkpoint-0 v2 is CLOSED (D1–D18).** Next per §10: three spikes (may run in parallel)
→ subsystem docs → adversarial review rounds → freeze.

---

## §0.7 Post-checkpoint proposal (2026-07-10): plot collectibles — ⏳REC, awaiting ratification

Owner proposal: the Desk should also author **collectibles** — plot items left in the map
for players to find (diaries of the fallen, distress notes in ruins, ledgers in crypts).
REC drafted in place (ARCH §4 Collectible entity · §5.3 step 4 · §6 placement-visibility
rule · §8 zero-spike-dependency note): content compiles like any artifact; placement rides
a D15 narrative work order with author-on-receipt; audience = physical reachability, so
faction-private facts must sit inside that faction's territory or pass D10 degradation;
vanilla surfaces only (books/signs/containers) → the one content lane with **no spike-zero
risk**. v1 fire-and-forget; collection-sets → quest/favorability deferred as a v2 hook.
**Lands as D19 on the owner's confirmation.**

---

## §0.8 (2026-07-10): **D19** ratified with amendments + spike zero report draft lands

- **D19 — Plot collectibles (owner ratified, two amendments).** Base lane as §0.7 REC
  (vanilla books/signs/containers; D15 placement; author-on-receipt; placement-visibility
  rule). Amendments:
  1. **Collect-and-submit quests over collectibles are in scope** — verified native in
     CNPC: quest type ITEM (`QuestItem`: ≤3 required stacks, `leaveItems`, `ignoreDamage`,
     `ignoreNBT`), turn-in at a named NPC, rewards incl. faction points, `nextQuestid`
     chaining. Amends §0.7's "v1 fire-and-forget only" — submit quests may ship in v1 where
     a thread wants them; plain fire-and-forget drops remain the default.
  2. **Registry-item drops** (modded loot — e.g. Superb vehicles, Sable structures, Golem
     wreckage) are allowed as *extended* drops, **gated by a curated item whitelist**: the
     owner flags item management as currently messy, so the compiler rejects any item id
     not on the whitelist; the list starts tiny and grows deliberately. Base function
     remains books/signs/containers.
- **Spike zero: report draft landed** (`docs/spike_zero_cnpc.md`) from a two-track
  dissection (mod source `customnpcs-1.20.1-git` + live 1.21.1 sample pack
  `D:\MC\ProjectUTD\CNPCScripts`). Headlines: dialogs/quests/clones are **plain JSON/SNBT
  files** under `world/customnpcs/` with `/noppes … reload|spawn` commands — no GUI in the
  write path; the Availability condition system (dialog/quest state, faction stance,
  daytime, scoreboard) is the natural compile target for thread gating; player hooks
  (`questTurnIn` etc.) are the candidate ask-8 records lane; clone files are SNBT-flavored
  (0b/1.0f/embedded newlines) — strict JSON tooling corrupts them. **Q2's pack-vs-scripts
  resolves as "both, layered"**: packs stay MiroFish-internal, a pack-installer compiles
  them to native CNPC files + a command sequence (final D-ruling once the report's §10
  residuals pass on the dev client: 1.21.1 parity, newline round-trip, modded-item
  matching, quest reload semantics, records-lane probe). Factions.dat is binary+load-once
  → **read-only for MiroFish** (factions pre-created by hand; config maps Chess faction
  slugs → CNPC faction ints).

---

## §0.9 (2026-07-10): PM cross-architecture review (R1–R6) → rulings **D20–D23** + script dialect canon

The owner relayed the SocialWill PM session's six-item review, carrying a **product ruling
on the window sequence**: lockdown → round-end → server stop → save sync → Desk
seed-compile + **Stage in-window batch** → Desk authoring → Picasso edit loop → window_log
closure → restart. Applied as follows:

- **D20 — Stage moves in-window (R1; amends D6).** The in-window batch is the Stage's main
  channel (`facts_through_round` = current round); owner-triggered daytime batches demote
  to an optional supplemental material channel (lineage blueprint D16). New hard limit: a
  **wall-clock timeout** (PLACEHOLDER config) alongside the D16 ¥20 budget cap — on either,
  the batch winds down at a round boundary and exports; server downtime must be bounded.
  M-NONBLOCKING's internal clause revised ("never waits" → "waits up to hard timeout, then
  facts-only authoring"); §7 degraded row updated.
- **D21 — Desk two-phase split (R2).** Phase A (ingest→distill→advance→author, incl.
  filing narrative work orders + ai_override intents) runs **before** the Picasso edit
  loop; phase B (compile→publish) runs **after** it, before window_log closes — phase-A
  orders built mid-window become citable in phase B (author-on-receipt satisfied within
  one window). Phase-A ingest reads the *previous* window's journal; this window's
  receipts enter at phase-B compile.
- **D22 — ai_override submission timing (R3, option a adopted).** The Chess external API
  is down during the window; the orchestrator submits queued override intents **after
  server restart** as a closing action. Zero Chess-side changes; B1 ingest lane unchanged.
  Option (b) (file channel + Chess startup read) rejected as an unnecessary cross-layer
  ask.
- **D23 — Narrative work-order conventions (R4, MiroFish side).** Order ids `nar_<seq>`
  (never `wg_`; `order_id` is the queue's global dedup key) · priority **80** (Chess holds
  50/60/70) · J4 player-protection matrix gains a narrative row: `degrade` requires reason
  + H4 journal gate; narrative `construct`/`mark` never carry `include_player_built`.
  **Chase item**: the Picasso-side `wargame_interface.md` §7 writer-list amendment is
  agreed (D15) but verified NOT yet landed — cross-layer pending until the Picasso session
  commits it.
- **R5 (editorial):** header bumped to Chess v0.1.4. Assessment: Chess D46–D51 (engagement
  model, merge/split, currency finalization) produce ordinary chess_log events consumed
  generically by ingest — no narrative-layer interface impact.
- **R6 (editorial):** `pack_r<round>`'s round value is pinned to the round recorded at
  this window's `window_log` closure.

Also this round: **script dialect canon** added to the spike-zero report (§5b) — the owner
identifies `clones/1/Fungal_Infected.json` as the trial-and-error-verified dialect that
actually runs on the live 1.21.1 client (Opus 4.8 lineage; deviations historically failed
*silently*). Generated scripts are dialect-locked; clone generation is **template+patch,
never from-scratch**.
