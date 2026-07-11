# Chess — Implementation Handoff (v0.1.2)

> **Status: ready.** Two adversarial rounds closed (§A: A1–A30 + SC1–SC8; §B: B1–B18);
> rulings D1–D45 all landed. This is the kickoff package for the coding agent, modeled on
> `../../Picasso/docs/HANDOFF.md`.

## 1. Kickoff prompt (paste to the coding agent)

> Implement the Chess wargame mod (`swchess`, NeoForge 1.21.1, Java 21,
> `net.socialwill.chess`) against the frozen doc set at `SocialWill/Chess/`. Read
> `ARCHITECTURE.md` fully first — it is authoritative; subsystem docs govern their domains;
> `docs/REVISION_LOG.md` is the tiebreaker for any ambiguity (search the D/A/SC/B number
> cited in the prose). Every numeric coefficient marked PLACEHOLDER goes into config/datapack
> exactly as named — never hardcode, never invent balance. `core/` must compile with zero
> Minecraft imports. Mark any deliberate spec deviation with a `🔁` comment and list it in
> your report — the docs are normative; drift is tracked, not silent.

## 2. Authority map

| Question | Authority |
|---|---|
| doctrine, layering, clocks, persistence, budgets | `ARCHITECTURE.md` |
| any "why is it this way" dispute | `docs/REVISION_LOG.md` (D1–D45, §A, §B) |
| battles, mirrors, occupation, siege gate mechanics | counters_and_combat + governance §7 |
| Picasso queue behavior | **`../Picasso/docs/wargame_interface.md` (frozen, external — never reinterpret)** |
| recorder duty | `../Picasso/docs/player_activity_pipeline.md` §2 (external, frozen) |
| what the other layers were promised | integrations §8 asks table (9 items — none are built; treat each as absent until delivered) |

## 3. Build order (phases; each ends runnable + golden-tested)

0. **P0 external-surface spikes (C20 — run before or parallel to P1; throwaway probes, each
   producing a written capability-matrix row + a pre-agreed fallback):**
   - **FTB** (jar-listed in round 3, runtime semantics unproven): CustomTask gate
     completion per team · internal ChangeProgress complete/reset semantics + reward
     side-effects · event payload attribution (D19 scoring) · teams ops (programmatic party
     creation, offline moves, party-of-one). Fallback pre-agreed: Chess-ledger authority
     (integrations §4.2, C12).
   - **CNPC + TACZ mirror provider**: script-driven spawn, TACZ gun firing, loadout
     application, kill-tag readback for writeback attribution. Fallback: CNPC-native ranged
     attacks with gun props; ultimate fallback: custom soldier entity (sw_src in-repo).
   - **CNA terminal**: programmatic inject + input metering (B6/C16 formula feasibility).
     Fallback: bridge row degrades off (econ §3.1 failure honesty).
   - **Create tank-insert**: API insert + volume accounting at the interface (C16).
     Fallback: ditto.
1. **P1 core kernel** — board/cells/factions/counters/battle math/settlement pipeline as pure
   Java + `ChessRandom`; golden transcripts for: siege, blackout, coup, sculk surge,
   overcap shed, bank run. No Minecraft yet. *(Biggest de-risk; everything else is glue.)*
2. **P2 persistence + event log** — SavedData codecs, `chess_log/` writer (file discipline
   verbatim from pipeline §2.2), `chess_state.json`, replay-verify command proving ARCH §7
   (including a `bridge/ai_override` replay case, B1).
3. **P3 shell minimal** — chunk sensing queue, core-block entities +
   `pending_core_placement`, cell HP hooks (D42/D44/B11 immunities), command choke point
   **with a complete headless mirror (C21): every player command invocable via server
   command/test harness through the same validation path** — this is what drives P3–P6
   acceptance and permanently serves golden tests (the map client arrives at P7; nothing
   before it may depend on pixels), `/chess` admin, boundary tool + enforcement (D43/D45).
4. **P4 mirrors** — spawn/top-up/cooldown/no-drop/scatter (§6.2), writeback, residents
   (feature-flagged), heat meter (B2 sources).
5. **P5 economy** — treasury, production, exchange terminal (physical delivery, B13),
   quotas + access models, reserves/bank-run, scavenge (D40 ledger), energy bridge
   (registration=authorization; B6/B7 metering) — bridge last, it is the most
   mod-compat-fragile.
6. **P6 governance** — polities, tickets, motions (churn-proofing A15), diplomacy +
   favorability table (B8 caps/decay), war legality incl. sabotage machine.
7. **P7 map client** — Xaero import converter (offline tool, separate artifact), tile
   pyramid + fog-gated patches, strategic map, panels, protocol. UI last: the sim must not
   wait for pixels.
8. **P8 AI factions** — behavior packs, `env` ephemera, strain adapters **observer mode
   first** (works against today's mods), playbooks; **driver mode lands only when the L4
   rewrite ships its API** (integrations ask 6).
9. **P9 bridges** — Picasso queue client (two-phase-aware receipt poller, `in_progress` =
   pending), FTB teams/quests binding (verify actual API capability early — flagged as an
   unprobed surface in §B), recorder duty.

Cross-phase rule: `chess_log` events land in the same phase as their subsystem, never later —
the log is the test harness's assertion surface.

## 4. Trap checklists (two families, inherited from Picasso + two native ones)

- **"Claims delegation but quietly needs a new input"** (Picasso E10/G1/I1/I6 family):
  watch for — heat meter needs gunfire hooks the recorder doesn't provide (B2); driver-mode
  adapters need L4 APIs that don't exist yet; FTB bloc-sync needs progress-API write
  capability *verify before building on it*; CNPC quest records need ask 8.
- **"Borrows a predicate from the wrong domain"** (Picasso I2/H5 family): watch for — fog
  checks vs objective checks (A22's split is deliberate: per-requester validation);
  `manpower` vs `population` debits (SC1: manpower is derived, only population is a store);
  window round vs settling round (B9); `explored` vs `visible` in every UI/AI predicate.
- **Native trap 1 — the two-worlds seam**: every board→world write is one of exactly three
  sanctioned lanes (mirror entities · single core blocks · strain adapter paint). Anything
  else wanting to touch blocks is a design bug, not a TODO.
- **Native trap 2 — clock leakage**: nothing but `window_log.json` advances rounds (D45);
  nothing but server UTC stamps `t`; nothing but the settlement counter stamps `tick`.
  Any new timestamp source is wrong by construction.

## 5. Fixture world & verification

Dev fixture: a bounded (D43) copy of the pre-run city save; acceptance flows drive the P3
headless command mirror (C21) — a scripted second client is only needed from P7 on.
Milestone acceptance is behavioral, not unit-only: found a faction → claim → build path A
and B (B requires a stub Picasso queue — a script that consumes pending/ and writes
receipts suffices before real Picasso integration) → train → fight abstract + loaded →
sabotage siege → bank run → recover. The golden-replay command (P2) must pass after every
phase.

## 6. Do-not-build list

Boats/helicopters (D28, v0.2) · POW system · alliances/vassals · multi-side battles (A7
queue rule is final for v1) · nether board · KubeJS bindings · MCP query server (event_log
§8 v2) · Create-network SU metering · resident population *simulation* (residents are
spawned representation only) · any raw `write_blocks`-equivalent lane.
