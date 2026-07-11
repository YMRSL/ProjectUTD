# MiroFish — Narrative Layer Architecture Reference

> **Status: 🚧 v0.1 draft 2 (2026-07-10) — checkpoint-0 v2 CLOSED (D1–D18,
> `docs/REVISION_LOG.md` §0–§0.6).** MiroFish is **two subsystems** — the **Stage** (the
> original MiroFish agent chat network: an OASIS dual-platform social simulation) and the
> **Desk** (draft 1's screenwriter pipeline) — plus a v1 **continuity graph** (D18).
> Recommendations are marked ⏳REC in place. Next: three spikes (§8), then subsystem docs
> (§10). This draft consumes, and never re-specifies, the frozen contracts of
> Chess v0.1.4 (`../Chess/`) and Picasso v0.5 (`../Picasso/`). Where a mechanism depends on an
> unverified external surface it is stamped **[SPIKE]** (§8) and may not be frozen until the
> spike report lands — the Chess C9/C10 lesson, adopted here as founding law.
>
> Prior art: this layer inherits a *lineage* of proven design from the pre-refoundation
> blueprints (`D:\ModDevelop\[Important]Mirrofish\SocialWill\开发蓝图`) and the MiroFish
> source (`D:\ModDevelop\[Important]Mirrofish\MiroFish-main`). Lineage items enter this doc
> as ⏳REC with attribution (§11), never as rulings — the owner has not re-ratified them.

---

## 1. Philosophy & Mental Model

Picasso and Chess produce **facts**: journal entries with `on_behalf_of`, an append-only
`chess_log/` of battles, coups, bank runs, infections. Nobody in the world *says anything
about them*. Draft 1 made MiroFish the missing narrator. D1/D2 add the narrator's missing
organ: a **rehearsal stage** — a social simulation whose population chatters, argues, spreads
rumors, and takes sides *about* the facts, so the screenwriter never writes into silence.

The mental model is a triangle:

- **Facts come from the layers.** `chess_log/`, the Picasso journal, quest-completion
  records. Authoritative, append-only, visibility-tagged.
- **Imagination comes from the Stage.** Simulation output is what people *would plausibly
  say and feel* — never what happened. It is reference material with provenance, not a fact
  source and not an agent supply (**D2** — the old project's "simulation supplies agents"
  purpose is retired).
- **Fiction comes from the Desk — citing its facts, drawing its color from imagination.**
  Every artifact that reaches the world passes the compile gate (§6).

### Core doctrine

- **M-TRUTH**: every narrative artifact carries provenance references. References are split
  (new in draft 2): **`fact_refs[]`** (event ids `evt_…`, journal entry ids, battle/order
  ids — must resolve against the real logs) and **`sim_refs[]`** (SimResult item ids — must
  resolve against the Stage archive). *Factual claims require fact_refs.* sim_refs license
  only color: attitude, voice, phrasing, social texture, rumor-content (§6). Pure color
  (weather talk, personality) needs no refs.
- **M-IMAG** (new, from D2): Stage output is imagination. It never asserts world truth, never
  spawns a world entity, never reaches the world except through the Desk's authoring and
  compile gate. A simulation is discarded-able by construction: deleting every batch must
  change *flavor*, never *function or truth*.
- **M-VISIBILITY**: MiroFish may *know* everything but must not *narrate* faction-private
  information to the wrong audience (`event_log.md` §6 states this as a requirement on this
  layer). Draft 2 enforces it at **two mechanical gates, both Desk-owned**: (a) **seed
  compile** — each Stage env receives only the facts its sphere is entitled to (§5.2); (b)
  **pack compile** — artifact audience ⊆ cited facts' visibility (§6). Enforcement is
  mechanical, not editorial.
- **M-NONBLOCKING**: no live system ever waits on MiroFish. The live server consumes
  pre-published artifacts; a missing/stale/malformed pack means "yesterday's content", never
  a player-visible error. Internally (**amended D20**): during the window the Desk waits on
  the Stage batch only up to a **hard wall-clock timeout** (PLACEHOLDER config — in
  addition to the D16 budget cap; server downtime must be bounded); on timeout the batch
  winds down at a round boundary and the Desk authors from facts alone (§7).
- **M-OFFLINE**: no LLM call ever sits in a tick path, a login path, or a dialogue-open path.
  Live behavior is *selection* from pre-authored content. The Stage is likewise fully
  offline: batches run out-of-band (owner-triggered), never against the live server.
  **Ruled D8**: additionally, all world-writes (packs, overrides) happen during maintenance
  windows only.
- **M-CONTRACT**: files and audited APIs only; no code imports across layer boundaries
  (inherited, Picasso v05 §8 / Chess D-CONTRACT). Stage↔Desk is also a file contract
  (SeedContext in, SimResult out) even though both live in this repo.
- **M-ONEWRITER**: MiroFish writes only its own lanes (`mirofish_content/`, `mirofish_log/`,
  `mirofish_sim/`), calls the Chess external API, and (**ruled D15**) files narrative work
  orders into `picasso_workorders/pending/` with `on_behalf_of.layer: "narrative"` — a
  *request*, not a world-write: Picasso remains the world's only writer and may reject.
  Contract side: `wargame_interface.md` §7's writer list gains a narrative entry (agreed
  2026-07-10 — **not yet landed Picasso-side; tracked cross-layer, R4**). Conventions
  (**D23**): narrative order ids are **`nar_<seq>`**, never `wg_` (`order_id` is the
  queue's global dedup key); **priority 80** (Chess occupies 50/60/70); the J4
  player-protection matrix gains a narrative row — `degrade` requires reason + the H4
  journal gate, and narrative `construct`/`mark` never carry `include_player_built`.
  Author-on-receipt applies (§6).
- **M-SPIKE (founding law)**: an external-surface capability may not appear in mechanism text
  until a spike report verifies it. Spike item zero: the CNPC script layer. Spike item one:
  the vendored OASIS surface. Spike item two (chartered by D18): the graphiti continuity
  graph (§8).

---

## 2. System Context

```
 owner (story settings, batch triggers, editorial gate)
   │ co-writes
   ▼
┌─────────────────────────────────── MiroFish ───────────────────────────────────┐
│                                                                                │
│  GENESIS (Desk, mode A — day-0 & ongoing)                                      │
│    story bible + faction docs + character seeds  ──►  AgentCards               │
│    (settings are data: tone gate + cast source)       (prior memories baked)   │
│                                                            │ cast              │
│                                                            ▼                   │
│  DESK (window cadence)                          STAGE (in-window batch, D20)   │
│   ingest facts ◄── chess_log/, journal,          OASIS envs, fresh per batch:  │
│   distill beats     window_log.json               · Twitter — public sphere    │
│   advance threads                                 · Reddit  — private sphere(s)│
│       │                    seed compile ────────► (SeedContext per env,        │
│       │                    (visibility closure)    visibility-filtered)        │
│       ▼                                                   │                    │
│   author  ◄──────── sim material (SimResult ◄── run → interviews → export)     │
│       │             archive, sim_refs)                                         │
│       ▼                                                                        │
│   compile (M-TRUTH / M-VISIBILITY / budgets / tone)                            │
│       ▼                                                                        │
│   publish                                                                      │
└──────┬────────────────────────────────┬────────────────────────────────────────┘
       │ mirofish_content/ packs        │ Chess external API (loopback+token):
       │ (dialogue, quests, characters) │ ai_override submitted post-restart (D22)
       ▼                                ▼ → bridge/ai_override ingest (B1 lane)
  CNPC script layer (live)         Chess core (deterministic defaults
  [SPIKE zero — ask 8 + pack        when no override present — D37/Q7)
   consumption contract]
```

Execution model (**ruled D6/D8, amended D20/D21** — product ruling via the PM
cross-architecture review, 2026-07-10): **two runtimes, both offline**, sequenced inside
the maintenance window:

```
lockdown → round-end → server stop → save sync
  → Desk seed-compile (visibility closure, §5.2)
  → Stage in-window batch      [hard limits: D16 budget cap + wall-clock timeout, D20]
  → Desk phase A: ingest → distill → advance → author
                  (files narrative work orders + ai_override intents)
  → Picasso edit loop          (builds; journal receipts land)
  → Desk phase B: compile (resolves this window's receipts) → publish
  → window_log closes → server restart
  → orchestrator submits ai_override intents to the Chess external API (D22)
```

The **in-window batch is the Stage's main channel** (`facts_through_round` = the current
round). Owner-triggered daytime batches remain an *optional supplemental* channel for
material accumulation (lineage blueprint D16, demoted from main); the Desk may draw color
from their archives in addition to the window batch. All world-writes stay window-bound
(D8).

Management & authoring surfaces (**ruled D7**): the layer exposes an **MCP
management/monitoring interface** — external agent sessions (Claude Code, Codex, …) inspect
and operate MiroFish through it (batch status, staged packs, gate promotion, graph/material
queries; tool list lands in `integrations.md`). World-facing authoring itself runs as
**headless frontier-model sessions** (`claude -p` / `codex exec`) dispatched by the window
orchestrator and connected back to those same MCP tools; **cheap models never author
world-facing content** — they are confined to mechanical extraction (Stage actors, graph
internals; D11).

NPC supply (**D5**): the Desk's characters must be consumable by **both** Chess and
Picasso-manifested works — concretely: Chess-side via D24/D25 counters, `mark` orders
(already carrying `dialogue_id` + `faction`), ask-8 quest/affiliation records; Picasso-side
props, dressing, and plot demolition via **direct narrative work orders**
(`construct`/`mark`/`restyle` + `degrade` under H4 and mandatory reason,
`on_behalf_of.layer: "narrative"` — ruled D15). Chess's own lanes remain in use where the
prop is wargame-born.

Console & frontend (**ruled D17**): the MCP surface is also the layer's primary UX — the
owner drives MiroFish (and cross-layer coordination with Picasso) through Codex/Claude
sessions. In addition MiroFish gets a **web frontend** (the source repo's
`frontend/`+`backend/` are the revival candidate; reuse assessed with spike one; ⏳REC: v1
frontend is read-only monitoring — batch status, feeds, packs, budgets — mutating actions
stay MCP-only).

---

## 3. Clocks

| Clock | Source | MiroFish use |
|---|---|---|
| Wall clock | server UTC — the one wall clock (pipeline §7.1) | the only stamp in `mirofish_log/` |
| Round | `window_log.json` (read-only; Picasso-owned) | pack versioning (`pack_r<round>`; the round value = the one recorded at this window's `window_log` closure — R6), thread scheduling |
| Settlement tick | Chess events carry it | provenance only |
| **Batch** | `bat_<seq>`, MiroFish-owned; **window batches (main channel, D20)** + optional owner-triggered daytime runs | Stage archive versioning; window batches carry `facts_through_round` = the current round; daytime batches stamp the last round they consumed |
| **Theatrical time** (new) | inside a batch (OASIS simulated hours) | fictional only — **never maps to world time**. The Desk treats a SimResult as "material as of round R", nothing finer |

MiroFish never invents world time and never advances anything: no round, no tick, no world
clock (Chess D45 discipline applies trivially — this layer has no clock authority at all).
The Batch clock orders only MiroFish's own internal archive.

---

## 4. Core Domain Model

| Entity | Key | Essence |
|---|---|---|
| **Story bible / settings** | repo docs (data) | Owner+Desk co-authored (Genesis, **D4**): world premise, tone bible, faction docs, character seeds. Settings are *data*: the compiler's tone gate and the cast generator both read them. Multiple "works" (different story settings) may coexist as separate bibles (D4 — "different contents") |
| **AgentCard** | `agt_<slug>` | A Stage persona: bio, long-form persona, **prior-memory section** (mandatory at genesis — the agent's stake in the setting), platform stats, sphere memberships. Carries batch-boundary carryover summaries (`[近期]` sections; lineage WP-206). *Not a world entity* (M-IMAG) |
| **Cast** | per work + per sphere rosters | The set of active AgentCards; hard-capped (`cast_max`). A small cast with deep memory beats a crowd of goldfish |
| **Batch** | `bat_<seq>` | One fresh Stage short-run: SeedContexts + cast in → actions, interviews, SimResult out; archived immutably under `mirofish_sim/`. **No mid-run injection** (lineage: blueprint D4 batch semantics) |
| **SeedContext** | per env per batch | The Desk's deterministic digest of world facts for one env, **visibility-closed at compile** (§5.2): the public-sphere env receives only `visibility: public` facts; a faction's private env receives that faction's closure. Plus owner injections (topic/policy/whisper; lineage D15) |
| **SimResult** | `sim_<batch>` | The Stage's only export: communications (posts/comments with platform+round), thoughts (batch-end interviews; lineage D2-采访), decisions (distilled stances whose `evidence` must be **verbatim substrings** of source material — anti-hallucination; lineage WP-205). Items are addressable as `sim_refs` |
| **Character** | `chr_<slug>` | A persistent named NPC: identity, faction affiliation (ask 8), voice sheet, **knowledge scope**, home anchor, memory pointer — plus (new) optional **`source_agent`** → AgentCard (lineage D17): a character *adapted from* a sim persona inherits voice and history as color; adaptation never transfers authority (M-IMAG) |
| **Beat** | `bt_<seq>` | One narratable fact-cluster distilled from the round's logs, with `fact_refs[]` and an audience scope derived from the facts' `visibility` |
| **Thread** | `thr_<seq>` | A multi-beat story arc as an explicit state machine; persists across windows; orphaned threads resolve or retire, never dangle |
| **Quest** | CNPC quest def | A thread step made playable; completion records flow back (ask 8) → favorability (Chess D39a) and thread advancement |
| **Memory record** | Chess-owned (D24) | For special-NPC counters, Chess persists the in-game memory; MiroFish reads/writes via the counter-state API. Note the split brain: **game-side memory lives in the save (Chess); sim-side memory lives in the AgentCard (MiroFish)**; the Character sheet binds the two |
| **Content pack** | `pack_r<round>` | The window's published bundle: dialogue trees, quest defs, character updates, retirement notices. Versioned, atomic swap; the live script layer holds the last good pack forever if no new one arrives |
| **Collectible** (**ruled D19**) | `col_<slug>` | A placeable lore artifact — written-book text, sign, container note, relic + description: Desk-authored narrative content **plus** a physical placement. Content compiles like any artifact (§6, incl. the placement-visibility rule); placement rides a D15 narrative work order (`construct`/`mark`), author-on-receipt applies. Base = vanilla surfaces (books/signs/containers, zero spike-zero dependency), fire-and-forget by default. D19 extensions: **collect-and-submit quests** (native CNPC ITEM quests, §spike-zero report) where a thread wants them; **registry-item drops** (modded loot) gated by a **curated item whitelist** — compiler rejects unlisted item ids |
| **Continuity graph** | graphiti store (local) | **Ships in v1 (D18).** Desk-internal temporal relation store: entities/relations with validity windows, every edge carrying refs (M-TRUTH). Fed by the Desk at authoring time (agent-as-brain via `add_triplet` — D7/D11 division of labor; Genesis relations seed it); consumed by Desk continuity queries, the D17 frontend's relation-map view, and self-media export (D18 rationale). Mechanism specifics **[SPIKE two]** |

Budgets (all PLACEHOLDER, D-BALANCE): `cast_max` · `beats_per_window_max` ·
`active_quests_max` · `pack_size_max` · `threads_active_max` · **`envs_max` ·
`agents_per_env_max` · `rounds_per_batch_max` · `llm_calls_per_batch_max` ·
`interview_top_k`** (Stage). No unbounded anything — the Stage is the dominant LLM cost
center and is budgeted first (**ruled D16**: Stage model = DeepSeek; per-window simulation
budget hard cap ¥20, target ¥6; record/replay cache adopted; near-cap the batch winds down
at a round boundary and exports normally, ⏳REC).

---

## 5. The Three Loops

### 5.1 Genesis (Desk mode A — day-0, then ongoing)

The layer is usable **before the world exists** (D4): the owner and the Desk co-write story
settings, and the Desk births the corresponding cast.

```
1. co-write   owner ↔ Desk: premise, tone bible, faction docs, character seeds
              (interactive; output = repo data docs, versioned)
2. cast       settings docs → AgentCards via LLM (direct generation — D11; no cloud
              dependency; the local continuity graph ships in v1 per D18 and is seeded
              from Genesis output — specifics [SPIKE two])
              · prior-memory section mandatory: the card states what this persona
                lived through in the setting and where it stands
              · deterministic under a stated seed (lineage D8): same docs + seed →
                same cast skeleton; LLM prose may vary, structure may not
3. seed roster spheres assigned (public + which private env(s)); initial relations
              declared in-card and mirrored into the continuity graph (D18)
```

Genesis re-runs additively later (new work, new faction, cast expansion). Named-character
creation *from the world side* (e.g. a war hero emerging from chess_log) is normal authoring
(§5.3), not Genesis.

### 5.2 The Rehearsal Loop (Stage, batch cadence)

```
1. seed-compile  Desk compiles one SeedContext per env from facts since the last
                 batch (chess_log topics, journal digests, quest completions) +
                 owner injections. **Visibility closure enforced here** (M-VISIBILITY
                 gate a): public env ⊆ public facts; faction env ⊆ that faction's
                 entitlement (event_log §6 vocabulary). Deterministic, golden-testable
2. run           fresh OASIS envs (lineage: blueprint D4 — no state resurrection, no
                 mid-run injection): Twitter env = the public sphere (all spheres'
                 cast may hold accounts); Reddit env(s) = private sphere(s) —
                 partition model [SPIKE one]: per-faction envs vs one env with
                 communities, decided by what vendored OASIS actually supports
3. interview     batch-end top-K active agents (K=`interview_top_k`; exclude system
                 herald; lineage D2/WP-204) — the Desk's directed questions ride here
4. export        SimResult with verbatim-substring evidence checks (lineage WP-205);
                 archive batch immutably under mirofish_sim/bat_<seq>/
5. carryover     per-card `[近期]` summary sections updated (replace-style, capped;
                 lineage WP-206); world herald never carries memory; unchanged cards
                 pass through byte-identical
```

Cross-sphere bleed inside the Stage is **harmless by construction**: even if private-sphere
facts echo in public-sphere chatter (an agent active in both), sim material can never assert
facts world-side without fact_refs (§6), which reimposes M-VISIBILITY at the pack gate. The
guarantee that matters is at the two Desk-owned gates, not inside the fiction.

### 5.3 The Narrative Loop (Desk mode B — one window)

```
Phase A — before the Picasso edit loop (D21):
1. ingest    read chess_log since last pack + the *previous* window's journal entries
             + quest-completion records (ask 8) + memory records touched
2. distill   facts → beats (dedup, cluster, rank by drama score; cap); every beat
             carries fact_refs[] and an audience scope
3. advance   threads consume matching beats; finished threads close with an epilogue
             beat; stale threads retire
4. author    new/updated dialogue, quests, character changes, collectible drops
             (D19, §4) from beats + threads, **drawing color, voice, stances, and
             rumor-content from the SimResult archive (cited as sim_refs)** —
             constrained by each character's knowledge scope and the tone bible.
             Narrative work orders (D15) and ai_override intents are filed here.

Phase B — after the Picasso edit loop, before window_log closes (D21):
5. compile   the pack compiler validates (§6); *this* window's journal receipts are
             now resolvable — orders filed in phase A and built mid-window become
             citable (author-on-receipt satisfied within one window)
6. publish   pack_r<round> atomic swap into mirofish_content/ · everything logged to
             mirofish_log/ · ai_override intents queued for the orchestrator, which
             submits them to the Chess external API **after server restart** (D22 —
             the API is down during the window; B1 ingest lane unchanged)
```

Steps 1–3 are deterministic given the logs (replayable, golden-testable). Step 4 is where
the LLM lives; its output is *proposals* that step 5 proves safe or rejects. **The compiler,
not the model, is the guarantee.**

The rumor channel (**ON — ruled D10**): a character may speak *degraded* cross-faction facts
(delayed ≥1 round, coordinates fuzzed, sources unnamed). Draft 2 gives it a natural content
source: public-sphere chatter is already organically distorted — the Desk distills rumor
*text* from sim material (sim_refs) while the *factual payload* still passes the degradation
rules, proven per artifact at compile (§6).

---

## 6. Content Packs & the Compiler (the layer's choke point)

Everything MiroFish ships passes one validator:

- **Schema**: versioned (`"v": 1`), unknown majors rejected loudly.
- **M-TRUTH check**: every factual claim carries `fact_refs[]` resolving against the real
  logs read this window; dangling/fabricated refs fail the artifact, not the pack (per-item
  isolation, continue-on-error). Picasso work orders follow **author-on-receipt** (D15): an
  ordered construction/demolition becomes narratable only once its journal entry exists —
  a rejected order never does.
- **M-IMAG check** (new): `sim_refs[]` resolve against the Stage archive; an artifact whose
  *factual* content rests only on sim_refs fails. sim_refs license color and rumor-content
  only. (This is also the cross-sphere-bleed backstop — §5.2.)
- **M-VISIBILITY check**: artifact audience ⊆ intersection of cited facts' visibility,
  unless a rumor-channel stamp (with its degradation proof) applies.
- **Placement-visibility rule (collectibles, D19)**: a collectible's audience is *whoever
  can physically reach it*. Content citing faction-private facts must be placed inside that
  faction's entitled territory, or pass the D10 degradation rules — a leaked diary *is* a
  rumor with provenance. Enforced at compile against the placement request's coordinates.
- **Tone check** (new, Genesis): style/lore constraints from the story bible, applied as
  lint (warn) in v1 — editorial judgment stays human via the D12 hold ticket.
- **Budget check**: caps of §4.
- **Editorial gate (ruled D12)**: default is **fully automatic to live** — the compiler is
  the always-on gate. The owner may file a **hold ticket** before the Stage batch ends
  (before auto-export into save-usable content begins); a held flow needs explicit owner
  confirmation to go live. Post-live, interviews and amendment requests on live content
  stay open **until the maintenance window closes**; every hold/confirm/amendment is
  logged in `mirofish_log/`.

Pack layout, dialogue-tree format, and quest-def format are **[SPIKE zero]-gated**:
`docs/content_pack_format.md` is written only after the CNPC spike reports what the script
layer actually consumes. The old blueprint's ContentPack schema v1.1.0 and the WS2
capability catalog (§11) are the *hypothesis inputs* to that spike, not pre-approved formats.

---

## 7. Failure & Degraded Modes

| Failure | Behavior |
|---|---|
| MiroFish never runs | Chess deterministic defaults everywhere (D37/Q7); NPCs use last pack's (or shipped default) dialogue; world fully playable |
| Pack malformed/missing | script layer keeps the previous pack; rejection logged; never player-visible |
| Chess API unreachable post-restart (D22) | override submission retried by the orchestrator, else skipped this window; the content pack already published in phase B |
| **Stage in-window timeout / over budget (D20)** | batch winds down at a round boundary and exports what it has (D16); if nothing usable, Desk authors from facts alone — plainer social texture, full function |
| **Batch crashes mid-run** | batch marked failed; archive keeps last good SimResult; no partial export (batches are atomic at the archive boundary) |
| **Genesis absent (no cast)** | Stage idles; Desk still narrates chronicle-style from facts; world unaffected |
| LLM unavailable / over budget | Desk steps 1–3 still run; step 4 degrades to template-text beats; Stage batches simply don't run (skipping batches is free by M-IMAG) |
| Memory record API absent | characters go amnesiac-but-functional (greetings only); logged, not crashed |

---

## 8. External Surfaces & Spikes

### Spike item zero — CNPC script layer **[SPIKE — report draft landed 2026-07-10]**

**`docs/spike_zero_cnpc.md`** documents a two-track dissection (mod source
`customnpcs-1.20.1-git` + the live 1.21.1 sample pack `D:\MC\ProjectUTD\CNPCScripts`).
Verified against source: dialogs/quests/clones are **plain JSON/SNBT files** under
`world/customnpcs/` (`dialogs/{cat}/{id}.json` · `quests/{cat}/{id}.json` ·
`clones/{tab}/{name}.json`) with `/noppes dialog|quest reload` and `/noppes clone spawn` —
**no GUI in the write path**; quest type ITEM natively supports collect-and-submit (D19);
the **Availability** condition system (dialog/quest state ×4, faction stance, daytime,
scoreboard) is the natural compile target for thread gating; player hooks (`questTurnIn`,
`questCompleted`, `factionUpdate`) are the candidate ask-8 records lane. The WS2 catalog's
core claims are thereby largely **confirmed** (its "Scene DSL" maps to a separate VN-dialog
datapack mod that coexists on the client — candidate for cinematic beats only).

Constraints found: clone files are SNBT-flavored (booleans `0b`, suffixed floats, **embedded
newlines in script strings**) — strict JSON tooling corrupts them; `factions.dat` is
binary+load-once → **read-only for MiroFish** (factions pre-created by hand; config maps
Chess faction slugs → CNPC ints); dialog/quest ids are bare integers → MiroFish claims a
dedicated id range + `mirofish` category + its own clone tab (id registry in
`mirofish_log/`). **Residual in-game verification before `content_pack_format.md` freezes**
(report §10): 1.21.1 jar parity, script-newline round-trip, modded-item quest matching,
quest-reload semantics, records-lane probe. Fallbacks unchanged (generated script files →
thin self-authored dialogue mod, C12 pattern). Collectibles' base lane (D19, §4) rides
vanilla surfaces and does not depend on this spike.

### Spike item one — vendored OASIS surface **[SPIKE]** (new)

Source in hand (`camel-oasis==0.2.5`, `camel-ai==0.2.78`; MiroFish-main scripts), so this
spike is cheap — but per M-SPIKE the following may not appear in frozen mechanism text until
verified by running code:

1. private-sphere partition: per-faction Reddit envs vs one env with communities (drives
   §5.2 step 2 and the cost model);
2. `initial_posts` seeding path for SeedContext delivery (lineage WP-203 says it exists —
   verify against the vendored version);
3. batch-end interview at K agents (IPC lane; lineage WP-204) and its failure modes;
4. determinism envelope: what `random.seed` actually pins (activation sampling yes; LLM
   content no; asyncio completion order no — lineage D8) — states the golden-test boundary;
5. cost measurement: LLM calls per batch as f(agents, rounds, activation curve) — feeds Q10.

### Spike item two — graphiti continuity graph **[SPIKE]** (chartered by D18)

The graph ships in v1 (D18), so this spike is a **prerequisite**, not an option. Verify with
running code before the mechanism text freezes:

1. `add_triplet` expressiveness — temporal validity (`valid_at`/`invalid_at`) and custom
   properties (enough to hang refs on every edge, M-TRUTH);
2. retrieval quality on a **direct-write** graph (no episode ingestion): node summaries,
   embeddings, hybrid search behavior;
3. Windows deployment shape: FalkorDB Lite embedded (needs Python 3.12+) vs docker
   container; the official graphiti MCP server over stdio;
4. Chinese-content retrieval quality (BM25 tokenization is the suspect);
5. structured-output reliability of the DeepSeek / DashScope endpoints on the classic
   ingestion path (fallback lane; keys in `.env.local`);
6. graph export shape for the D17 frontend relation-map view and self-media material (D18).

Secondary surfaces: Chess external API auth scopes (exists — Chess ARCH §12.4; confirm
granularity in spike zero) · Picasso journal **blessed file read** + the narrative
work-order lane (ruled D15; `wargame_interface.md` §7 amendment lands Picasso-side).

---

## 9. Checkpoint-0 v2 — status after adjudication round 2 (2026-07-10)

Round 1 (REVISION_LOG §0.4): **Q1→D6** (two runtimes) · **Q2→D7** (MCP management surface +
frontier-only authoring + headless dispatch; pack-vs-scripts stays spike-zero-gated) ·
**Q4→D8** (no live LLM; world-writes window-bound) · **Q6→D9** (permanence + promotion) ·
**Q8→D10** (rumor ON) · **Q10/Q11→D11** (graph brains = DeepSeek+Qwen, cloud rejected).

Round 2 (REVISION_LOG §0.5): **Q3+review→D12** (auto-publish + owner hold ticket +
post-live amendments until window close) · **Q5→D13** (tone bible confirmed) · **Q7→D14**
(shape M: 1 public env ~50 + per-major-faction private envs, 3–4 × ~15) · **Q9→D15**
(direct Picasso work orders IN; three cross-layer answers delivered; journal = blessed file
read) · **Q10→D16** (Stage = DeepSeek; ¥20 cap / ¥6 target per window; cache adopted) ·
new→**D17** (Codex-as-console; web frontend, source `frontend/` revival candidate).

Final: **Q11 → D18 (2026-07-10): 带图** — the graphiti continuity graph ships in v1. Owner
rationale: even at degraded retrieval value, the relation map dressed with NPC
avatars/skins is **self-media source material** (second consumer besides Desk continuity).
Spike two thereby becomes a prerequisite (§8).

**Checkpoint-0 v2 is CLOSED (D1–D18).**

Post-checkpoint amendments (owner + PM cross-architecture review, 2026-07-10 — REVISION_LOG
§0.8/§0.9): **D19** (collectibles, with submit-quest + whitelisted-item amendments) ·
**D20** (window sequence; Stage in-window main channel + wall-clock hard timeout) · **D21**
(Desk two-phase split around the Picasso edit loop) · **D22** (ai_override submitted
post-restart by the orchestrator) · **D23** (narrative order conventions: `nar_<seq>`,
priority 80, J4 narrative row) · R5/R6 editorial (Chess v0.1.4; pack round = the
window_log-closure value). **Open chase item (R4)**: the Picasso-side `wargame_interface.md`
§7 writer-list amendment is agreed but **not yet landed** — cross-layer pending.

---

## 10. What Gets Built (nothing yet)

Checkpoint-0 v2 closed 2026-07-10 (D1–D18). In order:

1. **Spike zero** (CNPC jar/spike report), **spike one** (OASIS surface report), and
   **spike two** (graphiti report — D18 prerequisite) — may run in parallel; each is a
   prerequisite to freezing the mechanism text it gates.
2. Subsystem docs: `docs/simulation_stage.md` (envs, SeedContext/SimResult formats, batch
   runner, carryover — parts gated by spike one) · `docs/genesis_and_casting.md` (bible
   format, AgentCard schema, cast generation) · `docs/beats_and_threads.md` ·
   `docs/characters_and_content.md` · `docs/content_pack_format.md` (gated by spike zero) ·
   `docs/integrations.md` (Chess/Picasso touchpoints, ask-8 consumption).
3. Adversarial review rounds in `docs/REVISION_LOG.md` (double-Fable protocol) → freeze →
   build (vendoring plan lineage: WP-201).

---

## 11. Lineage Map (imported prior art — ⏳REC with attribution, not rulings)

| Import | Source (pre-refoundation) | Lands in |
|---|---|---|
| Batch semantics: fresh short-runs, no mid-run injection, carryover via profile summaries | blueprint D4, WP-203/206 | §5.2 |
| Batch-end top-K interviews over IPC | blueprint D2, WP-204 | §5.2 step 3 |
| SimResult with verbatim-substring evidence (anti-hallucination) | WP-205 | §4, §5.2 step 4 |
| `[近期]` carryover sections, replace-style, capped; herald exempt | WP-206 | §5.2 step 5 |
| Owner injections typed topic/policy/private_message/whisper with scope×visibility routing | blueprint D15, WP-209 | §5.2 step 1 |
| Daytime rhythm; admin console; cadence state machine | blueprint D16, WP-210 | §2, Q1 |
| Character↔agent identity mapping (`source_agent`) | blueprint D17, WP-208 | §4 Character, Q6 |
| No-Zep cast generation from settings docs | blueprint D6, WP-202 | §5.1, Q11 |
| Seed-controlled statistical reproducibility + full audit logs | blueprint D8 | §5.1/§8 spike one |
| LLM response record/replay cache | blueprint D7 | Q10 |
| Downed/"薛定谔" fate adjudication for named NPCs (no editorial resurrection) | blueprint D14 | Q6 (feeds characters_and_content.md) |
| CNPC capability hypothesis catalog | WS2 | §8 spike zero |
| Vendoring plan for the sim kernel | WP-201 | §10 |
| Overridden: single-platform simulation | blueprint D5 | **overridden by ruling D3** (dual-platform, public/private spheres) |

The old T-series formal specs (`…\三系统分开设计的系统架构\01_SocialWill_正式稿\v0\`) remain
background reading only: their wargame/economy/AP content is superseded by Chess; their
lasting contributions (fact/memory separation, visibility tiers, choice-style prompting)
already echo in the doctrines above.
