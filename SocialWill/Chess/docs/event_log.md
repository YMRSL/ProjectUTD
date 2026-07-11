# Chess Event Log — Specification (v0.1.4)

> **Status: 🧊 v0.1.4 (2026-07-09) — rounds 1-3 + agent round §D closed; implementation-ready, see docs/HANDOFF.md.** Tracks `ARCHITECTURE.md` v0.1.4.
> The owner's explicit requirement: a *management-grade* log — many event classes, structured
> for consumption by other systems (Picasso, MiroFish) and by AI — **not** a latest.log that
> swallows everything. This is Chess's primary outbound data contract (D-CONTRACT) and the
> mechanism behind replayability (ARCHITECTURE §7) and the in-game log browser
> (map_interface.md §4). File discipline is inherited from the proven build-log rules
> (`../Picasso/docs/player_activity_pipeline.md` §2.2) rather than re-invented.

---

## 1. Why Topics, Not One File

Consumers want different slices at different cadences: MiroFish reads `combat` + `governance`
for narrative beats; Picasso agents read `bridge` + `territory` for editorial context; ops
reads `system`; the in-game browser reads a faction-filtered union. Topic-partitioned files
mean each consumer streams only its slice, retention can differ per topic, and one noisy topic
(economy ticks) can't drown the coup d'état.

## 2. Layout — `<world>/chess_log/`

```
chess_log/
├── manifest.json                    ← atomic-rewrite (H11); see §5
├── territory/2026-07-08.jsonl      ← per-topic daily files, UTC-midnight rotation
├── combat/…      economy/…      governance/…
├── infection/…   orders/…       bridge/…      system/…
```

One-way boundary: **Chess writes, everyone else reads** (mirror of the build_log contract).
Topics (fixed v1 set): `territory` · `combat` · `economy` · `governance` · `infection` ·
`orders` (every player command's outcome — the audit trail of the choke point) · `bridge`
(work orders, receipts, FTB sync, round observations) · `system` (startup, resume, degraded
modes, config reloads).

## 3. Event Envelope (one JSON line)

```json
{"v": 1, "id": "evt_184223_0007", "t": "2026-07-08T13:20:11.412Z",
 "tick": 184223, "round": 18,
 "topic": "combat", "type": "battle_ended",
 "actor": {"kind": "faction", "id": "f_ironpact"},
 "faction": "f_ironpact", "bloc": "republic",
 "subject": {"battle": "btl_00071", "front_cells_final": ["overworld:12,-34"]},
 "visibility": ["faction:f_ironpact", "faction:f_redline"],
 "refs": {"cause": "evt_184100_0002", "order_id": null},
 "data": { ...type-specific payload... }}
```

| Field | Rules |
|---|---|
| `v` | schema major version; readers ignore unknown fields, reject unknown majors with one warning per file (H7 semantics, verbatim) |
| `id` | `evt_<tick>_<seq>`; unique, monotonic within a tick — deterministic given the sim (ARCH §7), so replays regenerate identical ids |
| `t` | server UTC with millis — **the one wall clock** (pipeline §7.1); nothing else ever stamps |
| `tick` / `round` | logical clocks (ARCH §6); `round` only when meaningful. `round` = window counter *at emission*; lockdown-phase round-end events additionally carry `data.settling_round = R+1` (B9 — consumers joining on round never go off-by-one for deploys/re-rolls/flushes) |
| `actor.kind` | `player` \| `faction` \| `ai_faction` \| `system` \| `admin` |
| `visibility` | `public` \| `faction:<id>` \| `admin` — the machine-readable secrecy basis (§6) |
| `refs` | causality threading: `cause` (event id), plus domain ids (`order_id`, `battle_id`, `ticket_id`, `motion_id`) as applicable |
| `data` | per-type payload; catalogs live beside the subsystem specs (each doc's Events section) — this spec owns the envelope, not every payload |

## 4. File Discipline (normative, inherited verbatim)

Append-only; whole-line atomic writes, batch-buffered, flush ≤ 5 s; UTC-midnight rotation;
closed files immutable (readers may cache by filename+size); **no rewrite ever — corrections
are new events** (`type: "correction"`, `refs.cause` = the corrected event); torn final line
tolerated by readers, torn lines elsewhere skipped with a counted warning; live reads are safe
(open, read to EOF, close within one operation — Windows lock semantics, pipeline §2.4).
Retention is the operator's policy; **readers must report coverage** (`log_coverage` in any
query answer — no-silent-caps).

## 5. Manifest & Keyframes

`manifest.json` (atomic rewrite, updated at rotation + every `manifest_period_ticks` 30):
`{v, schema_version, topics: {name: {oldest_file, newest_file, newest_id}}, tick_hw, round_hw,
state_snapshot: {path: "../chess_state.json", tick, t}}`.

Consumers reconstruct any state as **keyframe + replay**: read `chess_state.json`
(ARCHITECTURE §8), then apply events with `tick > snapshot.tick`. Because non-deterministic
inputs are themselves ingested events (ARCH §7), the replay is exact for the abstract layer —
this is also the dispute-resolution and debugging story.

## 6. Visibility & the Fairness Contract

Log files contain the **full truth** — they live server-side, inside the trust boundary.
`visibility` is the machine-readable basis on which consumers *withhold*:

- The in-game log browser serves a member only `public` + `faction:<their id>` (server-side
  filter, same fog philosophy).
- **Per-type defaults (A21, normative): faction-scoped unless argued public.** `visibility`
  accepts a string or an array of scopes. Topic defaults — `combat`: the participating
  factions + any faction with live vision of the cell at event time (a `battle_ended` is
  *not* public; the envelope example is corrected); `governance`: faction, except
  war/ceasefire/founding/dissolution = public; `territory`: faction, except control changes
  visible to onlookers with vision; `economy`/`orders`: faction; `infection`: gated by cell
  visibility at emission (lurk events: admin); `bridge`: faction + admin; `system`: admin.
  Each subsystem doc's Events section owns its deviations from these defaults.
- MiroFish may *know* everything but must not *narrate* faction-private information to the
  wrong audience; the field is its guardrail, stated here as a requirement on that layer
  (same style as wargame_interface §6 stating requirements on the wargame).
- Anything shipped off-server (web dashboards, Discord relays) filters to `public` unless
  scoped per-faction.

## 7. Cross-Layer Correlation

Shared identifier namespaces make joins trivial:

- **`order_id`** appears in Chess `bridge` events, the work-order file, Picasso's receipt, and
  Picasso's journal `on_behalf_of` thread — one construction is one thread across three
  systems. The canonical chain: `orders/command_applied` (player applies) →
  `economy/construction_progress`×K → `bridge/order_submitted` → `bridge/receipt_received` →
  `economy/building_activated`, all sharing `refs.order_id`.
- **`faction` ids** are the same strings Picasso journals under `on_behalf_of.faction` and the
  registry stores in `authored.controlled_by` — agreed cross-layer at wargame_interface §4.
- **`battle_id`** threads `battle_opened → round summaries (sampled, `battle_tick_sample_n`
  every 6th round — full rounds would be noise) → mirror_casualty* → battle_ended → scar order`.

## 8. Query Surfaces

1. **v1 (normative): files + manifest, pull.** Same posture as build_log — cheap, robust,
   already proven across the Picasso boundary. Cost model linear in days spanned.
2. **In-game browser** (map_interface.md §4): server-filtered, paginated, topic/time/type
   filters.
3. **v2 (extension, not built speculatively)**: a read-only MCP tool mirroring Picasso's
   pattern (`query_chess_events`, `get_chess_state`) for agent ergonomics — the file contract
   already makes this a thin wrapper.

## 9. Volume & Rotation Sanity

At design scale (ARCH §10) the steady state is a few events/second worst case (economy ticks
emit *summaries*, not per-building lines; battle rounds sampled). Rough envelope ≤ 20 MB/day
across all topics (PLACEHOLDER, measured in golden runs). If a topic runs hot, the fix is
coarser summary events, never dropping events silently.

## 10. Open Questions (v0.1)

1. ~~Economy summary granularity~~ **Resolved (D34)**: digest every **40 settlement ticks**
   (`economy_digest_period_ticks`, ≈ 6.7 min) — the owner chose coarser than proposed for
   performance headroom. Threshold-crossing events (famine start, blackout, reserve breach)
   still emit immediately; the digest is for the steady-state ledger only.
2. ~~Player-position breadcrumbs~~ **Resolved (checkpoint 4)**: excluded; build_log/sessions
   owns player activity.
3. ~~Correction protocol~~ **Resolved (checkpoint 4)**: correction events + replay
   determinism suffice.
