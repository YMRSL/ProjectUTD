# Player-Activity Pipeline — Specification (v0.5 draft 2)

> **Status: 🚧 draft 2 — round-7 findings (REVISION_LOG §J: J1/J2/J6/J7/J8 + Q-rulings) applied; pending reviewer diff spot-check, then normative.** Upgrades `docs/v05_forward_requirements.md` §6 from requirements to spec. Tracks `ARCHITECTURE.md` v0.4.4. The declared clock-consistency attack was largely pre-answered by §7; round 7's four critical findings were all *contract holes* (a value/identifier/permission some side needs with no channel providing it) — closed below.
>
> Three components: **(a)** server-side recorder plugin (outside this repo — §2 is its complete development contract), **(b)** Picasso-side reader + query tools (§3–§4), **(c)** attribution bridge into the structure registry (§5). Governance policy rationale lives in v05 §6 (intent record); this spec is the mechanics.

---

## 1. Data Flow Overview

```
LIVE server                     │  PICASSO_BUILD_LOG_DIR          │  maintenance WINDOW
                                │                                 │
players place/break blocks      │  build_log/                     │  read JSONL (read-only)
  → plugin event handlers       │  ├── 2026-07-07.jsonl  (closed) │    → BuildLogReader
  → append JSONL, atomic line   │  ├── 2026-07-08.jsonl  (active) │    → cluster into activity sites
    writes, daily rotation      │  └── sessions/…  (optional v2)  │    → serve query tools
                                │                                 │    → enrich detect_structures
                                │   plugin WRITES, Picasso READS  │    → feed H1 protection row
```

The directory is a **one-way contract boundary**: plugin writes, Picasso reads, neither imports the other (v05 §8 doctrine). Reading is safe while the server runs (§2.4); *writing the world* still requires the window — sensing and acting have different clocks by design (`docs/wargame_interface.md` §1).

---

## 2. The Recorder Plugin Contract (component outside this repo)

The plugin (Fabric mod or Paper plugin, matching the UtilWeDie server stack) is developed against this section alone. It also hosts the wargame's live surface and the Fallback-C stamper if Phase 1.5 fails (`docs/phase15_contingency.md` §5) — one plugin, three duties, but *this contract only covers the recorder*.

### 2.1 Event line schema (JSONL, one event per line)

```json
{"v": 1, "t": "2026-07-07T21:14:03.412Z", "player": "Steve",
 "action": "place", "pos": {"x": 120, "y": 71, "z": -340},
 "block": "minecraft:oak_planks", "dim": "minecraft:overworld"}
```

| Field | Required | Notes |
|---|---|---|
| `v` | ✓ | Schema major version, currently `1`. Reader skips unknown *minor* additions (unknown fields ignored); rejects unknown *major* with one log line per file, not a crash (H7) |
| `t` | ✓ | UTC ISO-8601 with millis. **Server clock is the only clock** (§7.1) |
| `player` | ✓ | Account name (stable identity; display names change) |
| `action` | ✓ | `"place"` \| `"break"` — v1 scope. Piston/fluid/mob/explosion side-effects are **excluded by design**: attribution needs a player behind the event |
| `pos` | ✓ | Block coordinates |
| `block` | ✓ | Placed block id, or broken block's id (what *was* there) |
| `dim` | ✓ | Reader processes `minecraft:overworld` only in v1; other dims skipped and counted (`events_skipped_dimension`) |

### 2.2 File discipline

- **Files:** `build_log/<UTC-date>.jsonl`, rotated at UTC midnight. The active file is append-only; closed files are immutable (reader may cache closed-file parse results keyed by filename + size).
- **Atomic lines:** the plugin buffers events and writes whole lines (single `write` call per batch, flush ≤ 5 s). A torn final line must still be *possible* to tolerate — the reader skips an unparseable last line silently (H7); any unparseable line *elsewhere* is skipped with a counted warning (`lines_skipped`).
- **No compaction, no rewrite:** the plugin never edits a written line. Corrections are new events.
- **Retention** is the server operator's policy; the reader states its cost model (§4.1) rather than imposing one.

### 2.3 Optional second stream (v2, reserved): `sessions/<date>.jsonl` — join/leave + coarse position samples for "frequented routes". Not required for v1; the schema slot is reserved so v1 readers ignore the subdirectory.

### 2.4 Live-read safety

Reading JSONL from a running server is safe by construction: appends are line-atomic, the reader tolerates a torn tail, and no lock is shared. **What the reader must NOT do:** hold the active file open across a query boundary (Windows file-lock semantics — open, read to EOF, close within one tool call).

---

## 3. Picasso-Side Reader — `core/build_log_reader.py`

```python
class BuildLogReader:
    def __init__(self, log_dir: Path) -> None: ...
    def events(self, since: datetime, until: datetime | None = None,
               player: str | None = None, bounds: BoundingBox | None = None
               ) -> Iterator[BuildEvent]: ...
    def sites(self, since: datetime, until: datetime | None = None,
              **filters) -> list[ActivitySite]: ...   # clustered, §3.1
```

Selects files by date-range overlap, streams line-by-line (never loads a full file), applies filters during the stream. Closed-file results may be cached; the active file is always re-read.

### 3.1 Activity-site clustering (normative algorithm)

An **activity site** is a spatiotemporal cluster of one or more players' build events — the unit of "something happened here".

```
1. Collect filtered events, sort by t (stable tiebreak: pos, then action).
2. Greedy single-pass clustering: an event joins an existing open site if
     horizontal distance to the site's current bounding box ≤ JOIN_DIST (16)
     AND gap since the site's last event ≤ JOIN_GAP (90 min).
   If MULTIPLE open sites qualify (J7): join the one with the smallest
   bbox distance; ties break by older site (earlier first_event). Sites are
   never merged in v1 — two sites growing toward each other stay two sites.
   (Without this rule, join order would depend on site-list iteration order,
   breaking site-id determinism — §6 doctrine applies to clustering too.)
   Else it opens a new site. (Sites are player-agnostic: two players building
   one base form one site; per-player stats live inside the site.)
3. A site closes when the stream moves past its JOIN_GAP window.
4. Post-filter: discard sites with < MIN_EVENTS (8) total events — mining
   scratches and single-torch placements are noise, not sites.
5. site_id = "site_" + first 12 hex of SHA-256(first event's t ‖ pos) —
   deterministic: re-running the same query yields the same ids (§6 doctrine;
   no counters, which would depend on query order).
```

Constants are config: `PICASSO_SITE_JOIN_DIST` (16), `PICASSO_SITE_JOIN_GAP_MIN` (90), `PICASSO_SITE_MIN_EVENTS` (8). Tunable; defaults chosen so a house build is one site and a branch-mine hallway is usually filtered by the `pos`-spread heuristic below.

**Site classification hint (informative, not normative):** the site record carries `character` — a coarse label from cheap signals: `"construction"` (net-positive places, block palette diverse, vertical spread ≥ 2), `"excavation"` (net-negative, mostly breaks, linear/downward spread), `"mixed"`. Agents treat it as a prior, not a verdict.

### 3.2 Site record

```json
{
  "site_id": "site_3fa9c2d10b44",
  "bounds": {"x_min": 96, "x_max": 143, "y_min": 64, "y_max": 82, "z_min": -360, "z_max": -318},
  "first_event": "2026-07-07T20:02:11Z", "last_event": "2026-07-07T23:41:52Z",
  "players": {"Steve": {"places": 412, "breaks": 88}, "Alex": {"places": 37, "breaks": 2}},
  "top_palette": [{"block": "minecraft:oak_planks", "count": 210}, ...],
  "event_count": 539, "net_blocks": +359,
  "character": "construction"
}
```

---

## 4. MCP Tools

### 4.1 `query_player_activity` 🚧

```json
{"since": "2026-07-07T00:00:00Z", "until": null, "player": null,
 "bounds": null, "min_events": null}
```

Returns `{sites: [site records], events_total, lines_skipped, events_skipped_dimension, log_coverage: {from, to}}`. Raw events are **never** returned over MCP (same reasoning as `read_region`). `log_coverage` states the actual date-file range consulted — if the query's `since` predates the oldest retained file, the agent sees the truncation instead of assuming completeness (no-silent-caps doctrine). **Cost model:** linear in days spanned; month-spanning queries are slow, not failing (H7).

If `PICASSO_BUILD_LOG_DIR` is unset or empty → `build_log_not_configured`.

### 4.2 `get_activity_site` 🚧

`{site_id, since, until?}` → one full site record plus `timeline` (per-hour event counts) and `palette_full`. The `since`/`until` must be re-supplied (site ids are deterministic per window, §3.1 — the id alone doesn't carry its query range; this is deliberate, sites are query results, not registry objects).

### 4.3 Protection feed (not a tool — the H1 hook)

The choke point's player-protection row consults **recent activity sites**: at apply time, the engine asks the reader for sites in the last `PICASSO_PROTECTION_LOOKBACK_DAYS` (default 14) intersecting the write region, and protects their bounds (union with registry-attributed structures). This is the freshness half of H1/H2. Degraded mode (`build_log_not_configured` or reader error): the response carries `"player_protection": "unavailable"` — never a hard failure (resilient startup), but agents on shared worlds should stop.

**Protection noise floor is lower than the analysis floor (J6):** the feed clusters with `min_events = PICASSO_PROTECTION_MIN_EVENTS` (default **3**), not the analysis default of 8 — a 6-block starter shack is beneath analytical interest but not beneath protection. The two floors serve different questions (what's worth *studying* vs. what's worth *not bulldozing*) and are deliberately separate knobs. Feed results are cached per immutable closed file (J11); only the active day's file is re-clustered per apply.

**Placement near protected sites (J8, expected behavior — not an error loop):** a solver candidate intersecting a protected site bbox is rejected by pre-validation (I2). For "build near the player's base" reactions this is the *normal path*: the caller retries with outward-stepped placement (the playbook-2 tent lands beside the site bbox, not inside it). `no_valid_placement` responses include which predicate failed per candidate precisely so agents can distinguish "step outward" from "give up".

---

## 5. Attribution Bridge — `detect_structures` enrichment

When the build log is configured, `detect_structures` computes per-structure attribution during assembly:

```
attribution_fraction = attributed_blocks / total_solid_blocks_in_bounds
```

where `attributed_blocks` = current solid blocks in the structure's bounds whose position appears as a *player place* event (any time in retained logs) not later superseded by a break. Three-state ruling (H3):

| Condition | `detected.attribution` | Effect |
|---|---|---|
| fraction ≥ 0.5 | `"player_built"` + `builders`, `first_built`, `last_modified` | Bulk ops skip the whole structure (H1 row) |
| 0 < fraction < 0.5 | `"player_modified"` + same fields | Bulk ops skip **the attributed positions only** (position-set, not bounds); response warns |
| fraction = 0 (or no log) | `"native"` | No protection |

Attribution lives under `detected.*` (it is computed by detection from log evidence — re-detection refreshes it; a player abandoning a base doesn't fossilize the label). The **position set** for `player_modified` is not stored in the registry (it would balloon); it is recomputed at apply time from the log by the protection feed, cached per (structure, log high-watermark) — closed daily files are immutable, so the cache invalidates only when a new file enters the range (J9/Q2 ruling). Registry stores the verdict + stats only.

**Evidence-decay ratchet (J2 — log retention must not silently strip protection):** recomputing attribution from *retained* logs means a base built 60 days ago under a 30-day retention policy re-attributes to `fraction = 0` → `native` → protection silently vanishes — the exact failure this pipeline exists to prevent, arriving on a delay fuse. Normative rule: when re-detection computes a **lower** attribution state than the registry currently holds **and** the oldest retained log file postdates the entry's `first_built`, the evidence has expired rather than the ownership — **keep the previous verdict**, refresh nothing attribution-related, and set `detected.attribution_evidence: "stale"` (fresh evidence present → `"live"`). Downgrades under stale evidence only happen through `annotate_structure` (explicit, audited, `authored`-side). A *higher* recomputed state always applies (new building on old ruins upgrades normally). The asymmetry is deliberate: protection may ratchet up automatically, never down.

**Implausibility signals (governance input, v05 §6):** for `player_built`/`player_modified` structures, detection additionally computes and stores under `detected.implausibility`: `span_to_support_ratio` (longest unsupported horizontal run vs. supports beneath), `floating_fraction` (solid blocks with air ≥ 4 below), `footprint_to_height_ratio`. Raw signals only — **no verdict field**: the judgment that something is exploit-shaped belongs to the agent/wargame (they have context Picasso lacks: is this a build-in-progress?), the numbers belong to Picasso. Same division of labor as `learn_style` (tools give data, agents give judgment).

---

## 6. The Daily Editorial Loop (orchestration contract, not a tool)

The loop lives in the agent's schedule, not in Picasso. Normative *ordering* within one window (this sequencing is load-bearing — see §7.3):

```
1. query_player_activity(since = last window's close)   [sense]
2. detect_structures over active areas                  [attribute — refreshes
   player_built/modified BEFORE any styling runs; H2's ordering constraint.
   Scope rule (J9/Q4): (new sites' bounds + 1 chunk) ∪ registry structures
   intersecting any new site — never a whole-world rescan]
3. process_work_orders(dry_run=false)                   [wargame settlement]
4. editorial reactions (fortify/mark/react per policy)  [act]
5. window_log.json append happens at close              [clock tick]
```

Steps 3 and 4 both write; both are protected by the same H1 row reading the same fresh attribution from step 2. An agent that skips step 2 is not unsafe (the choke point still consults the *log* directly via the protection feed) — it just works with staler registry attribution. This redundancy is deliberate: **the log-based protection feed is the guarantee; step 2 is the optimization** (same pattern as choke point vs. targeting checks).

---

## 7. Clock Consistency (the declared attack surface, answered)

Three clocks exist: the **server clock** (stamps log events), the **window clock** (round counter, `window_log.json`), and the **save-sync instant** (`save_synced_from_live_at`, when infrastructure snapshotted the live save for the window). Rules:

### 7.1 One wall clock
All timestamps in all three artifacts are the **server's UTC clock**. Picasso never stamps an event time from its own host clock (the machines may differ); Picasso-side stamps (window open/close, `applied_at`) are Picasso-host UTC and are only ever compared with each other, never with event times, except through §7.2's ordering rule. NTP sync across both hosts is an ops requirement, stated here once.

### 7.2 The visibility cut
A window operates on the save as of `save_synced_from_live_at` (T_sync). Log events with `t > T_sync` describe blocks **not present in the save being edited**.

**T_sync provisioning (J1 — the value needs a channel, not an assumption):** the sync infrastructure (whatever copies the live save for the window) writes `<world>/picasso_sync.json`: `{"v": 1, "synced_at": "<UTC>", "source": "<free-form>"}` at copy time — one file, written by the copy script, the only thing Picasso asks of ops besides the copy itself. `set_world` reads it and reports `"t_sync"` in its response; the window-close record copies it into `save_synced_from_live_at`. **Missing or unparseable** → attribution degrades to greedy (all retained events, like the protection feed) and every attribution-bearing response carries `"t_sync": "unavailable — attribution may overcount"` (file mtime is explicitly NOT a fallback — it lies across copy tools). Same honesty pattern as every other degraded mode.

Therefore:

- The protection feed (§4.3) uses **all** retained log events *including* those after T_sync — protecting a base whose blocks aren't in the snapshot yet costs nothing and closes the gap for the *next* sync.
- Attribution (§5) uses only events with `t ≤ T_sync` — attributing blocks that aren't in the save would produce fractions over a phantom denominator.
- This split (protection = greedy, attribution = cut-consistent) is normative.

### 7.3 Ordering inside a window
`window_log.json` is appended **at window close** (all writes done). The wargame reads `round` live; a crashed window (no close record) simply doesn't increment the round — its consumed-but-unfinished work orders are governed by the two-phase/idempotency ruling in `wargame_interface.md` §9 Q2 (round-6 review adjudicates). The registry, rooms file, and window log are all written under the H11 protocol; **within one window there is exactly one writer** (Picasso, single-flight), so cross-file consistency inside a window is trivial — the only real seam is live-time concurrent *reads* by the wargame, which see either the pre-window or post-window state of each file (atomic rename), never a torn one. The wargame must not assume the *set* of files flips atomically together — it should treat `window_log.json`'s close record as the "all files final" signal (read it last).

### 7.4 Log rotation vs. window boundary
Rotation is UTC-midnight; windows are ops-scheduled. A window spanning midnight reads two files — the reader's date-range selection handles this; no alignment between the two boundaries is assumed anywhere.

---

## 8. Config & Errors (pending ARCHITECTURE integration)

| Variable | Default | Description |
|---|---|---|
| `PICASSO_BUILD_LOG_DIR` | *(unset)* | already in §10 table |
| `PICASSO_SITE_JOIN_DIST` | `16` | site clustering: max horizontal join distance (blocks) |
| `PICASSO_SITE_JOIN_GAP_MIN` | `90` | site clustering: max time gap (minutes) |
| `PICASSO_SITE_MIN_EVENTS` | `8` | site noise floor (analysis tools) |
| `PICASSO_PROTECTION_MIN_EVENTS` | `3` | site noise floor (protection feed, J6 — deliberately lower) |
| `PICASSO_PROTECTION_LOOKBACK_DAYS` | `14` | protection feed window |

Errors: `build_log_not_configured` (exists) · `site_not_found` (new). Per-world file: `<world>/picasso_sync.json` (J1 — written by sync infrastructure, read by `set_world`; goes into §10's not-configurable list).

New source: `core/build_log_reader.py`, `tools/activity.py`. Build phase: **Phase 10** in implementation_order (after Phase 9 work-order dispatcher; the reader itself has no dependency on Phases 8–9 and may be built any time after Phase 1 — the *protection feed* hook lands with the choke-point refactor).

---

## 9. Open Questions — adjudicated in round 7 (REVISION_LOG §J9)

1. **Attribution supersession cost** → O(events) replay is **accepted for v1**; a reader-maintained (never plugin-maintained) position-index sidecar `build_log/index/` is the named upgrade path if detection latency hurts in practice. Not built speculatively.
2. **Position-set recomputation** → **cache per (structure, log high-watermark)** — applied in §5. Closed files are immutable, so the watermark is the only invalidation trigger.
3. **Site determinism vs. retention** → **accepted and documented**: a site whose first event ages out of retention gets a new id on the next query. Site ids are query artifacts, not registry identities (the registry's structure ids are the durable handle); anchoring ids to file boundaries would buy stability the consumer doesn't need.
4. **Editorial-loop detection scope** → **union of (new sites' bounds padded by 1 chunk) ∪ (registry structures whose bounds intersect any new site)** — the padded-sites half finds new construction, the intersect half refreshes attribution on modified known structures. Never a whole-world rescan. (Applied as the scope rule in §6 step 2.)
