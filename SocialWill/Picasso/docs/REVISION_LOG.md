# v0.4 Revision Log — Decision Record for Adversarial Review

> This file exists for the review process: every consequential change in the v0.4 documentation pass, its rationale, and the alternatives considered. Attack these decisions here; the specs themselves stay clean. Also lists **open questions** deliberately left unresolved.
>
> Review ground rules proposed: (1) challenge decisions by referencing the numbered items below; (2) a challenge should name a concrete failure scenario, not a preference; (3) resolved items get a `RESOLVED(vN):` annotation rather than deletion, so the argument trail survives.

---

## A. Structural decisions

**A1. Single authoritative main doc + versioned changelog, instead of rewriting supplements into one mega-doc.**
Rationale: supplements are per-subsystem work surfaces for coding agents; one 3000-line file would churn constantly. Cost: cross-references can rot. Mitigation: precedence rule in the header (higher version wins; same version → ARCHITECTURE.md wins), and §8/§10/§11 declared "authoritative tables" that supplements must not duplicate normatively.

**A2. Status markers (✅/🚧/⚠️/❌/🔁) embedded in specs.**
The v0.1–v0.3 docs described planned and built things in the same tense — a coding agent could not tell what exists. The registry (§8) and layout (§3) now carry status. Cost: status rots as code changes; accepted because the alternative (a separate status file) rots faster. The refactor owner updates markers per phase.

**A3. Kept both implementation-order docs separate** (base vs segmentation) but cross-linked and status-stamped. Merging was considered and rejected: segmentation is an independent workstream with its own dependency graph.

**A4. Legacy v0.1 passes: deprecate-in-place, not delete.**
`tlou_structural_damage`, `tlou_vine_bridge` → `deprecated: true` (superseded by fragments). `tlou_decay_surfaces`, `tlou_nature_reclaim`, `tlou_street_debris` remain as usable standalone texture passes — they don't conflict with the fragment-first doctrine as long as they're not in the canonical bundle. Alternative (delete files) rejected: existing worlds/bundles may reference them.

**A5. Zone concept cut entirely** (was: promote every Structure to a Zone). Nothing consumed zones; two names for one thing is pure alias burden. If per-structure style overrides are ever needed (the one thing Zones had that Structures lack: `style_overrides`), add that field to Structure instead. ← *this is a real functional question for the user, asked in the summary.*

**A6. `apply_style_profile` retired; StyleProfile kept as diagnostic.**
Two learning paths existed: (old) profile → suggested pass intensities → replay; (new) clusters → author fragments → author bundle → apply bundle. The old path's output space (intensities over built-in passes) can't express what the new path expresses, and its `suggested_passes` pointed at the deprecated v0.1 set. One path, one artifact flow. `learn_style` survives because its statistics genuinely inform the agent's authoring decisions.

---

## B. Semantic gaps filled (were unspecified or contradictory)

**B1. Pass Type System (ARCHITECTURE §5).** v0.3 had three de-facto pass shapes but the Pydantic model only described one. Now: discriminated union on `type`, omitted `type` = `block_pass` (back-compat), per-type schema ownership table, `invalid_pass_definition` + resilient-startup skip for malformed files.

**B2. `intensity` semantics per type (§5.1).** Was only defined for block passes. Decision: fragment passes scale *density* (how many events), not per-block probability (how ragged each event is) — scaling raggedness would make high-intensity runs look shredded rather than more decayed. Pattern passes: intensity = per-match replace probability.

**B3. `space_filter` moved from pass JSON to call/bundle parameter (§5.2).** A pass describes *what* it does; *where* it applies is the caller's scope decision. v0.3's `space_filter` inside fragment-pass JSON is retracted. Also specified per-type filtering target (candidate blocks vs anchors vs pattern anchor) and the two-tier space classification with mandatory tier reporting — so agents know whether "interior" meant real flood-fill or the crude heuristic.

**B4. Surface classification formalized (§4.5).** v0.1 prose had: `ceiling` defined as an air block (unusable by replace rules), `floor` not requiring solidity, rooftop/floor overlap unresolved, "column extends to sky" ambiguous. Now: predicate table + priority order (rooftop > floor > ceiling > outer_wall > inner_wall > embedded), out-of-region = air convention stated, halo mitigation flagged 🚧. Priority choice "top wins over ceiling" matches the painterly use cases (moss the top of a slab, hang vines under it via `direction: "below"`).

**B5. Determinism spec (§6).** "Same seed → same result" was asserted but unfounded: nothing forbade iteration-order-dependent RNG. Now: every stochastic decision = SHA-256 over (seed, stable identifiers, world position, purpose tag); shared RNG streams and order-dependence explicitly forbidden; preview ≡ apply guaranteed as a consequence. This is the load-bearing guarantee behind preview-before-write.

**B6. Fragment orientation (fragment_system §4).** Wall fragments with axis-fixed offsets are wrong on 3 of 4 wall faces — a breach must punch along the wall normal. Now: canonical frame (+Z = outward normal), `orientable` flag, wall-normal-derived yaw for wall anchors, hash-rolled yaw for floor anchors (cars shouldn't all face north), `facing`/`axis` property remapping, rotation before clearance checks. Default `orientable: false` keeps radially-symmetric fragments (rubble) cheap.

**B7. Write choke point (§12.1).** v0.3's fragment algorithm checked safety only at the *anchor*; a destructive fragment could zero out never-touch blocks or NPC markers at offset positions. Now one final validation for every written position across all engines: never-touch, marker protection, destructive air-gate. Also: `remove`-action block passes must declare `destructive: true`; pattern-replace clears exempted (replacing a chair implies removing its parts) but never-touch/marker still hold.

**B8. Bundle execution semantics (fragment_system §6).** Region mode vs structure mode made explicit (v0.3 conflated them — `apply_bundle` took no coordinates in the spec but region behavior was implied). Named the **double-application hazard** in region mode (every entry runs over the whole region; `rubble_scatter` appearing in two entries runs twice) and prescribed `structure_type_filter`. `dry_run` default **true** — the one-command tool defaults to safe. Seed precedence: call > bundle default > 42; per-entry seeds rejected (one run, one seed, pass-name-keyed hashing decorrelates).

**B9. Structure identity across re-detection (semantic_segmentation §7).** "Re-running preserves manual corrections" was promised with no mechanism — sequential IDs can't match entries across runs. Now: 3D IoU ≥ 0.5 greedy matching with type-family compatibility, per-case update semantics, stale marking instead of deletion. Threshold 0.5 is a first guess — flagged as tunable.

**B10. Resource limits (§12.4).** `radius_chunks` was unbounded; radius 16+ on dense urban terrain plausibly OOMs the sparse dict representation. `PICASSO_MAX_RADIUS_CHUNKS` default 12 + `region_too_large` + tiling advice. Structure detection at larger scales = multiple overlapping runs + identity merging (B9).

**B11. Cache invalidation rules (§4.1).** `last_region` reuse/invalidation was implied, now normative: exact-match reuse, any non-dry write invalidates (including `place_npc_marker`), previews never do. Single-flight concurrency assumption documented so nobody threads the session later.

**B12. Config unified (§10).** Env vars were scattered across four docs with gaps (`PICASSO_BUNDLES_DIR`, `PROFILES_DIR`, `MIN_STADIUM_VOLUME` existed in code or prose but no table). One authoritative table + a "not configurable" list for per-world paths; `PICASSO_STRUCTURES_DIR` retracted (per-world artifact ≠ static env config). Resilient-startup rule formalized (server always starts; missing resources degrade to structured errors).

**B13. Noise backend policy (§4.6).** `noise` C lib needs a compiler on Windows — as a hard dependency it would block the primary dev environment. Fallback hash-noise = portable baseline, C lib = optional accelerator, `noise_backend` reported in responses because the two fields differ per-seed (a real reproducibility caveat, disclosed rather than hidden).

**B14. Errors completed (§11).** Added the missing codes for fragments/bundles/structures/journal + partial-failure semantics for `apply_bundle` (continue-on-error, `errors[]`, ok iff ≥1 success).

**B15. Journal & revert spec'd (§12.3)** as the highest-priority planned safety feature (reverse diff per write, `revert_last_apply`, plain-JSON hand-revertible). Until it lands, "copies only" is stated as an operational rule in every implementation doc. Alternative (world-file snapshot per apply) rejected: multi-GB copies per pass application.

**B16. Amulet round-trip spike inserted as Phase 1.5 (gating).** The whole replacement vocabulary assumes `doomsday:*` blocks survive Amulet write → game load. Believed-but-unverified; if false, the fallback plans (structure-block staging, schematic export) change the architecture materially. Also corrected: 1.21 height range −64…319; ungenerated chunks skipped and counted; exact Amulet accessor API declared an implementation detail of the bridge (the v0.1 pseudo-API didn't match any real Amulet version).

**B17. Pattern rotation made explicit (style_pass_schema §3):** matcher checks all 4 yaws; a chair facing east must match. v0.1 was silent, and the shipped pattern files only make sense with rotation.

**B18. Testing stance reversed (implementation_order):** deterministic core (classification, hashing, rotation, matching) gets pytest with synthetic fixtures — the "Done When" lists double as test lists. In-game verification stays for anything touching real world files. v0.1's "no automated tests needed" contradicted the layering rule's own justification.

---

## C. Known remaining defects (unfixed, acknowledged)

**C1. Layering violation in `tools/bundle.py`** — bundle orchestration sits in the tools layer. Marked `TODO(arch)` for extraction to `core/bundle_executor.py` during the refactor. Doc change only would misdescribe current code, so the violation is *documented*, not hidden.

**C2. Implemented code predates several v0.4 normative specs.** Known divergences flagged 🔁 in implementation_order: orientation not implemented (wall fragments only correct on south faces — outputs are placeholder), choke point scattered per-engine, `ceiling` class possibly missing from the classifier, pattern rotation unverified. The docs now describe the *target*; 🔁 marks the deltas. This is deliberate: docs lead, refactor follows.

**C3. RegionData sparse-dict representation** is a memory/perf liability at scale and double-converts against Amulet's arrays. Accessors declared the stable contract; storage swap deferred to refactor. Not fixable by documentation.

**C4. Cross-pass fragment collisions** (tree placed on rubble pile): accepted for now; `preserve_existing` softens it. Bundle-level occupancy grid named as the designated fix if it proves ugly. ← *functional question for the user.*

**C5. Tier-1 space heuristic is crude** (exterior = rooftop|outer_wall). A windowless interior wall two blocks from a breach reads "interior" while the breach edge reads "exterior". Tier-2 (flood fill) fixes this but gates on segmentation Phase S1. Mandatory tier reporting is the interim honesty mechanism.

**C6. `learn_style`/`extract_block_clusters` remain 🚧** — the learning workflow doc describes tools that don't exist yet. Status-stamped; no semantic changes made to §3 algorithm (it was already precise).

**C7. Fingerprint matching underspecified.** `structure_fingerprints.json` exists as shipped data, but the matching algorithm ("§8 fingerprint match updates sub_type/confidence") is still hand-wavy. Left as-is: it's inside a 🚧 subsystem and doesn't block anything upstream. Flag if you disagree with deferring it.

---

## E. Adversarial review responses (2026-07-07, Fable vs Fable)

**E1. ACCEPTED — noise backend cross-machine determinism.** The preview≡apply guarantee is demoted to "single-machine, single-installation." `noise_backend` is upgraded from optional to a **required** response field on all apply/preview tools. Agent guidance added: before issuing `dry_run: false` after a preview, the agent must verify `noise_backend` matches the preview response; if different, re-preview first. Pure-Python re-implementation of pnoise rejected (different fade-curve implementations produce numerically different outputs; the guarantee is false regardless). Documented in §4.6 and §6. *(Reviewer: §II·1)*

**E2. ACCEPTED — `noise_backend` is a required field.** Cascades from E1. Updated tool_specs.md to mark it required in all write/preview responses. *(Reviewer: §II·1)*

**E3. ACCEPTED — single-session concurrency note upgraded.** §4.1 now gives operational guidance for HTTP SSE expansion: "if extending to multi-connection transport, add an `asyncio.Lock` at AmuletBridge call granularity (one lock per write operation, not per session)." Runtime assertion in session.py deferred (code freeze) but `TODO(safety)` added. *(Reviewer: §I·1)*

**E4. ACCEPTED — Phase 1.5 gate hardened.** apply_pass / apply_bundle tool descriptions gain an explicit precondition block: `dry_run: false` on any pass containing `doomsday:*` blocks requires Phase 1.5 verified. Documented as `TODO(safety)` in implementation layer. *(Reviewer: §I·2)*

**E5. ACCEPTED — StylePass fragment_pass fields inlined in §7.** `fragments`, `density`, `min_spacing`, `only_safe_anchor_blocks` added to the Pydantic model sketch with cross-ref. *(Reviewer: §IV·1)*

**E6. ACCEPTED — apply_bundle region-mode mandatory warning.** When `dry_run: false`, region mode without `structure_type_filter`, and bundle has >1 entry: response must contain `"region_mode_warning"` naming which passes appear in multiple entries. *(Reviewer: §V·2)*

**E7. ACCEPTED — set_world adds `journal_status` field.** Response always includes `"journal_status": "unavailable" | "active"`. apply_pass / apply_bundle responses in `dry_run: false` mode include `"reversibility_warning"` when journal is unavailable. *(Reviewer: §III·1)*

**E8. PARTIAL — marker protection load timing.** Reviewer is correct in principle; the current single-flight guarantee means the race window does not exist today. However, the correct implementation is "snapshot marker positions at the start of the apply call and hold for the call's duration" — this is both safer and conceptually cleaner. Spec updated to reflect this. Adding `(TODO: upgrade to call-scoped snapshot once concurrent paths exist)`. *(Reviewer: §III·2)*

**E9. ACCEPTED — `candidate_groups` field in extract_block_clusters output.** Clusters whose block sets are rotation-variants of each other are grouped. This gives the agent a merge signal without requiring the engine to choose a canonical rotation (which would require a definition of "same" across block-property variants). Preferred over `similar_clusters` (that label implied content-similarity, not rotation-identity). *(Reviewer: §II·2)*

**E10. ACCEPTED — Room system safe_list bypass extension point.** §12.1 adds a `write_context` field concept (values: `"decoration"` | `"room_envelope"`) to the internal write call. Room envelope writes bypass the `safe_blocks.replaceable` whitelist but still respect `structural_never_touch`. The choke point is not removed — it's parameterized. *(Reviewer: §VI·1)*

**E11. ACCEPTED — CompositeEvent anchor derivation sketched.** v05_forward_requirements §3 updated with two anchor modes: offset-relative (consequence fragment anchored at a fixed world-space offset from trigger fragment's anchor) and gravity-projected (consequence fragment anchored at the first solid block below a given offset column). *(Reviewer: §VI·2)*

**E12. ACCEPTED — fingerprint matching gets an algorithm sketch.** Phase S4 in segmentation_implementation_phases.md gets a cosine-similarity placeholder. *(Reviewer: §V·1)*

**E13. ACCEPTED (minor) — §8 create_pass missing block-pass-only annotation.** Added to tool registry table. *(Reviewer: §VII·1)*

**E14. ACCEPTED (minor) — marker filename negative coordinate TODO.** Added to npc.py and place_npc_marker spec: filenames use `{x}_{y}_{z}` where negative values are written with `n` prefix (e.g. `100_64_n200.json`) for cross-platform filesystem safety. This is a format change; old `100_64_-200.json` names will co-exist until a migration tool is written. *(Reviewer: §VII·2)*

**E15. ACCEPTED (minor) — safe_blocks.json 17-entry coverage gap acknowledged as P0 content task.** Added to Phase 3 🔁 list: "expand safe_blocks.json to comprehensive 1.21.1 decorative/surface coverage before Phase 5 content is considered production-ready." *(Reviewer: §VII·3)*

**REJECTED — halo misclassification `boundary_margin_chunks` field.** The reviewer asks for a field marking edge-block classification as unreliable. However: (1) the halo mitigation (§4.5) already describes the fix when implemented; (2) a `boundary_margin_chunks` field in responses tells the agent which blocks are unreliable *for classification* but doesn't restrict modification, creating a confusing partial guarantee. The right fix is the halo read (🚧), not a runtime caveat field. Until the halo lands, the existing note in §4.5 is the correct disclosure. *(Reviewer: §IV·2)* — *Round 2 follow-up: the rejection stood, but the reviewer converted it into a sharper finding (orphan task + tiling conflict), accepted as F2.*

---

## F. Round-2 adversarial review responses (2026-07-07)

Reviewer verified all 15 round-1 patches as genuinely landed, then attacked the patches themselves. All 10 round-2 findings **accepted**, including two retractions on our side. The reviewer's meta-observation — that parameterizing prose into tables (E10) squeezed out a hidden conflict — is exactly why this process is worth running.

**F1. ACCEPTED + RETRACTION — E8's "risk = 0 today" claim withdrawn.** The reviewer is right on both counts: (a) the marker directory is a *filesystem* interface — §13 invites the Narrative Layer to read it, so external writers are in the same threat model, and MCP single-flight doesn't govern them; a marker written externally during a long apply is unprotected until the next call. (b) The patched §12.2 TODO said "upgrade to call-scoped" when the text above it already specified call-scoped — a nonsense sentence produced by trying to have it both ways ("reviewer right but risk 0"). §12.2 rewritten: threat-model note names external filesystem writers, the window is *accepted and documented* (not denied), TODO now points at the real upgrade (per-pass re-scan / directory watcher). *(Reviewer R2 §I·1)*

**F2. ACCEPTED — halo was an orphan task and fought the tiling advice.** Halo read now has a phase home: **Phase 3.5** in implementation_order.md (1-chunk halo, classification-only halo blocks, choke-point enforcement, seam-identity pytest). §12.4's tiling advice now carries the caveat with mitigation (≥1-chunk overlap, seams through open ground) and cross-references Phase 3.5. Standing rule adopted from the reviewer's framing: **every 🚧 in a spec must name its owning phase.** *(Reviewer R2 §I·2)*

**F3. ACCEPTED — E10 regression: the context table contradicted pattern_replace's clear exemption.** Round-1's two-context table made `clear_offsets` air-writes fail the destructive gate — literal execution would leave floating slabs above replaced chairs, and the same-version precedence rule (ARCHITECTURE wins) would enforce the *wrong* behavior. Fixed with a third **`pattern_clear`** context column: implicit destructive grant + whitelist pass-through, emittable only by the pattern-replace engine path for the matched pattern's own `clear_offsets`. The exemption is now carried by the mechanism instead of stated in prose two documents away. *(Reviewer R2 §II·3)*

**F4. ACCEPTED — E1's agent protocol was temporally impossible.** "Verify noise_backend before apply" required knowing a response field before the response exists. Replaced with config-level pinning: **`PICASSO_NOISE_BACKEND` = `auto | c | fallback`**, resolved once at server startup, immutable per session, reported in `set_world`. Within a session preview ≡ apply now holds unconditionally; cross-session drift is handled by the (retained) required response field + re-preview rule. Pin `fallback` for cross-machine reproducibility. *(Reviewer R2 §II·4)*

**F5. ACCEPTED — E4's "requires" had no teeth and no truth source.** Both defects fixed: (a) enforcement upgraded from warning to **block** (`modded_write_unverified` error, added to §11) with a deliberate `force_modded_write: true` override that downgrades to a warning; (b) the undecidable precondition got a decidable source — **`PICASSO_MODDED_WRITE_VERIFIED`** env var (§10), set by a human after the Phase 1.5 spike passes. The server cannot know whether the spike ran; a human-set gate is honest about that. *(Reviewer R2 §II·5)*

**F6. ACCEPTED + RETRACTION — cosine sketch had a math flaw AND didn't match the shipped data.** Checked the shipped `structure_fingerprints.json` this time: it is per-sub_type entries with a `signals` predicate object and a `confidence_boost` — not feature vectors at all. The round-1 sketch was doubly wrong (unnormalized dimensions *and* misaligned with the artifact it claimed to consume). Replaced with a rule-based signal-scoring algorithm matching the actual schema (predicate pass-fraction scoring, 0.6 eligibility threshold, `confidence_boost × score` scaling, skip-unknown-signals rule with a >50%-skipped ineligibility guard). The shipped JSON schema is declared the stable interface; retraction noted in the spec itself. Lesson recorded: when a spec claims to consume a shipped artifact, read the artifact before speccing the consumer. *(Reviewer R2 §II·6)*

**F7. ACCEPTED — silent-default footgun in the inlined fragment_pass fields.** `density = 0.0` (forgotten field → validates → places nothing, no error) and `anchor_surface = "floor"` (forgotten field → silently overrides every fragment's own surface) were the "resilient startup gone wrong" pattern. `fragments`, `anchor_surface`, `density` are now **required with no defaults** for `fragment_pass` (and `mappings` for `pattern_replace`); missing → `invalid_pass_definition` at load, logged and skipped. §7 sketch annotated with the rationale; fragment_system §3 table gained a Required column. *(Reviewer R2 §II·7)*

**F8. ACCEPTED — "reads can proceed concurrently" was an unverified promise about Amulet internals.** Weakened per the reviewer's proposal: writes must serialize; reads must ALSO be treated as serialized until concurrent reads are explicitly verified against the pinned Amulet version — and that verification is bridge-internal, consistent with §4.2's encapsulation stance. *(Reviewer R2 §II·8)*

**F9. ACCEPTED — region_mode_warning now unconditional.** The single-entry-wrong-terrain case (building-only bundle applied to a road → interior passes silently no-op, no duplicates, no warning) had no coverage under the duplicates-only trigger. The warning now always fires in region mode without `structure_type_filter`, in both dry_run modes; duplicate-pass details are an additional clause. *(Reviewer R2 §III·9a)*

**F10. ACCEPTED — gravity_projected timing: post-trigger world state.** Debris falls through the hole the trigger just made, consistent with bundle layering semantics. One sentence in v05 §3, real semantic difference. *(Reviewer R2 §III·9b)*

---

## G. Round-3 adversarial review responses (2026-07-07)

Round 3 falsified our "only edge-wording left" prediction: the reviewer switched strategy from auditing patches to auditing untouched foundations (classifier predicates, gate × pass-type interactions, structure tool surface, game physics) and found two mechanism-level holes. All 8 findings **accepted**. Document version bumped to **v0.4.3**; changelog now records all three review rounds.

**G1. ACCEPTED (critical) — choke-point whitelist killed every write-into-air.** The §12.1 table's whitelist row checked the *current block at the write position*; for `place_adjacent` vines and most fragment blocks that block is `minecraft:air`, which is not (and must never be) whitelisted → every vine, every rubble stack, every tree would be silently dropped by a faithful implementation. Same failure family as E10/F3: table precision squeezed out a semantic the prose era never had to state — that the whitelist governs *replacing solids*, not *adding to air*. Fixed with the **air-transparency rule** in §12.1 (target is air/air-like → whitelist passes unconditionally; other three rows still apply) and a sync note in fragment_system §5 step f. This was the "must fix before codex" item and it is fixed. *(Reviewer R3 §II·G1)*

**G2. ACCEPTED (critical) — "air" and "solid" were never defined.** Added the **block taxonomy** to §4.5: four normative categories — `air` (air/cave_air/void_air), `air-like` (non-collision decorations: plants, torches, carpets, snow layers, rails, buttons/levers/signs — transparent to classification predicates, never valid fragment anchors), `liquid` (opaque to classification; underwater decoration deliberately deferred to the S3 water subsystem), `solid` (everything else, leaves included). Membership ships as data (`data/block_taxonomy.json`), extensible for mod blocks. Reviewer's three scenarios all resolve: carpeted floors classify as floors (carpet transparent), rubble can no longer anchor on a torch (air-like ≠ anchor), and the street-tree canopy case is documented as a known tier-1 artifact with rationale for why leaves stay solid (leaf-transparent would misclassify forest interiors and make roof_tree output unanchorable) — tier-2 flood fill resolves it correctly. Waterlogged property noted. *(Reviewer R3 §II·G2)*

**G3. ACCEPTED — structure-scoped applies violated core principle #2.** New "common semantics" block in structure_detection_tool_specs: `dry_run` defaults **true** on both apply tools (was absent on `apply_pass_to_structure`); standard write-tool fields (noise_backend, space_classification, reversibility_warning, modded gate) explicitly inherited; **bounds → chunk mapping** specified — chunk AABB + halo, and structures exceeding the radius cap are **tiled internally** (row-bands, ≥1-chunk overlap, dedup by position — safe because position-hash determinism makes overlapping recomputation idempotent) rather than erroring, since the cap protects against runaway requests, not legitimate large structures; zero-match returns `ok: true` + `structures_affected: 0` + `no_match_warning`. *(Reviewer R3 §II·G3)*

**G4. ACCEPTED — game physics were nobody's job.** Split per the reviewer's proposal: **engine-side** — the choke point auto-injects `persistent: true` on all `*_leaves` writes (mechanically decidable, always correct for decoration; author can override explicitly); **author-side** — a normative game-physics checklist in fragment_system §2 (falling blocks need support, vines/torches need attachment faces, no destructive breaches below the local water line until S3 owns flooding), plus a shipped-content audit note naming the known exposure points (rubble y=1 layers, breach windowsill gravel). *(Reviewer R3 §II·G4)*

**G5. ACCEPTED — boundary-clipped structures orphaned their manual overrides.** semantic_segmentation §7 gains: `partial: true` flag for candidates touching the scan boundary; identity matching switches from IoU ≥ 0.5 to **containment ratio ≥ 0.7** (`intersection / min(volume)`) when either side is partial — the right question when one box is known-truncated; stale marking narrowed to **full containment only** ("partial overlap is not evidence of disappearance" — without this, every tile seam would stale the structures it slices). Bounds refresh on partial→full upgrade preserves overrides. *(Reviewer R3 §II·G5)*

**G6. ACCEPTED — version management was already leaking.** Bumped to **v0.4.3**; changelog gained v0.4.1/2/3 rows (one per review round); all 8 supplements now carry a "tracks ARCHITECTURE.md v0.4.3" header line, making the precedence rule executable. *(Reviewer R3 §III·G6)*

**G7. ACCEPTED — `create_*` collision semantics defined.** Fail with new error code `name_already_exists` (never silently overwrite — no journal covers file-level destruction); deliberate replacement via `overwrite: true` which archives the old definition to `_replaced/<name>.<utc_ts>.json`; names case-insensitive for collision (Windows). *(Reviewer R3 §III·G7)*

**G8. ACCEPTED — `revert_last_apply` added to the §4.1 cache-invalidation list** (it is a write). One-word class of fix, exactly as priced. *(Reviewer R3 §III·G8)*

**Process note:** three rounds, same lesson three times (E10 → F3 → G1): **precision is excavation** — every conversion of prose to tables/predicates exposes conflicts the prose survived on. The reviewer's round-3 strategy shift (audit foundations, not patches) is what found G1/G2; worth remembering for future review cycles: rotate the attack surface each round.

---

## H. Round-4 adversarial review responses (2026-07-07)

Round 4 attacked the seams between the new §6–§8 requirements and the v0.4.x contracts — per the v05 doc's own attack rules ((a) does an existing contract block the requirement, (b) is the data model the right shape). Reviewer's verdict: **v0.4.3 base docs cleared for codex**; two structural findings (H1, H8) must be resolved before §6–§8 graduate to spec. All 12 findings **accepted**. H1 and H8 are resolved *now* (not deferred) precisely because they shape the choke-point schema and registry schema — both on codex's imminent refactor path.

**H1. ACCEPTED (critical, (a)-class hit) — bulk protection had no enforcement point in the only implemented mode.** §6 promised player_built structures were excluded from bulk styling, but `apply_bundle` region mode never consults the structure registry — a base inside a styled region had zero protection, and the whitelist can't save it (players build with whitelisted vanilla blocks; post-G1, additions into air pass unconditionally). Resolved per the reviewer's recommended direction: **protection sinks to the write choke point** — a fifth (🚧 v0.5, schema-reserved) row in the §12.1 table: target inside a player-attributed structure's bounds *or* a recent activity site → skip, unless `include_player_built: true`. Degraded mode (registry/log unavailable → check skipped + `"player_protection": "unavailable"` in response) keeps resilient startup. Targeting-level exclusion stays as an optimization; the choke point is the guarantee — same philosophy as never-touch, now applied consistently. *(Reviewer R4 §I·H1)*

**H2. ACCEPTED — freshness gap closed via activity sites.** A base built yesterday isn't in the registry until detect re-runs, but it *is* in the build log. The choke-point row (H1) checks **registry structures ∪ recent activity-site bounds** — the log is the fresher sensor and the protection follows it. A detect-before-style ordering rule was rejected as primary fix (doesn't bind an agent calling `apply_bundle` directly); it remains good practice for the daily routine. *(Reviewer R4 §I·H2)*

**H3. ACCEPTED — attribution is a fraction with three derived states.** `player_attribution.fraction` (share of structure blocks placed by players per log) → `player_built` (≥0.5, whole-structure exclusion), `player_modified` (0<f<0.5, structure targetable but player-placed positions skipped at the choke point + response note), native. Thresholds are config. A boolean failed both directions (three torches immunizing an office tower; a renovated native building left unprotected). Lives in `detected.*` — recomputed from the log at detection time. *(Reviewer R4 §I·H3)*

**H4. ACCEPTED — governance gate mechanized, exactly the E4 lesson re-applied.** The reviewer supplied the enforcement condition ready-made: `include_player_built: true` ∧ destructive content ∧ `journal_status != "active"` → reject with new error `governance_requires_journal` (reserved in §11). All three server-decidable; purely additive reinforcement passes — mapping 1:1 onto the reinforce-before-Phase-8 policy. Written into §12.1 alongside the protection row. *(Reviewer R4 §I·H4)*

**H5. ACCEPTED — 1-block towers were in a detection blind spot.** `PICASSO_MIN_STRUCTURE_AREA=50` means the named exploit shape can never be a registry structure, so `structure_id` scope can never reach it. Fixed with the reviewer's own spare part: governance edits accept `activity_site_id` scope (log-clustered sites have no area threshold; `get_activity_site` already returns a bounding box). *(Reviewer R4 §I·H5)*

**H6. ACCEPTED — revert conflict detection written into §12.3 before Phase 8 starts.** Per-position three-way check: current == journal `after` → replay `before`; else skip + report in `conflicts[]`. The governance scenario (player repairs the bridge before a misjudged edit is reverted) turns this from theoretical into inevitable; steamrolling player repairs to fix one mistake would inflict a second. Applies to all reverts, not just governance. *(Reviewer R4 §I·H6)*

**H7. ACCEPTED — four JSONL contract engineering rules added to §6:** per-line schema version (`"v": 1`, reject unknown majors gracefully); truncated-tail tolerance (last-line parse failure = normal, mid-file = corruption); dimension filter (non-overworld skipped + counted, consistent with D4); retention/cost model (linear in days spanned; slow ≠ fail). *(Reviewer R4 §I·H7)*

**H8. ACCEPTED (critical, (a)-class hit) — registry entries split into `detected` / `authored` namespaces.** The old update rule "matched, no override → replace all detected fields" would erase `interior_graph` (an afternoon of room-graph authoring) on any routine re-detection. Root cause identified by the reviewer: detection products and authored products cohabited one flat schema, and every future authored field would need individually-remembered exemptions. semantic_segmentation §7 now defines the two-namespace entry schema: **re-detection refreshes `detected.*` and never touches `authored.*`**; `manual_override` is simply the first resident of `authored` (shadowing resolution: authored-over-detected for readers). Update-semantics table rewritten per-namespace. This lands *now* so codex reserves the schema shape during the refactor. *(Reviewer R4 §II·H8)*

**H9. ACCEPTED — palette resolution decided (was a spec-blocking blank).** Three-level chain, first hit wins: (1) host's `authored.palette` (precise, optional) → (2) host's `detected.dominant_materials` filtered through a slot-compatibility table shipped as data (stops glass_pane walls) → (3) template's mandatory declared defaults (every template must be instantiable with zero host information). Catalog-tag lookup rejected for v1 (catalog covers decoration mods, not vanilla construction blocks). Plus: dimension tolerance rolls are constraint-solved against `attach_via` opening alignment, not free per-axis rolls. *(Reviewer R4 §II·H9)*

**H10. ACCEPTED — two small gaps:** instantiation records at fixed per-world path `<world>/picasso_rooms.json`, added to §10's not-configurable list (the `PICASSO_STRUCTURES_DIR` retraction is the precedent — no new env var); `seal`/`remove_room` responses carry `reachability_warning` when a room loses all paths to exterior openings — advisory, never blocking (sealed vaults are legitimate design; the warning makes it a decision, not an accident). *(Reviewer R4 §II·H10)*

**H11. ACCEPTED — multi-writer protocol for the structure registry (and all per-world JSON).** Same threat model as F1's external marker writers, previously unaddressed for the registry: three writers (Picasso, humans — explicitly encouraged, future layers), in-memory bulk write-back = lost updates. semantic_segmentation §9 now mandates: read-merge-write (entry-granular, entries not modified this operation taken from file) + atomic write (temp file + rename — a truncated single-source-of-truth file is never acceptable). Extended to `picasso_rooms.json`; journal files are append-only so atomicity alone suffices. *(Reviewer R4 §III·H11)*

**H12. ACCEPTED — editorial residue removed from the taxonomy table.** The "…no wait — leaves are solid" self-correction lived in a normative table; conclusion stays, correction process belongs here in the log (which is where you are reading it). *(Reviewer R4 §III·H12)*

**Round-4 process notes:** (1) the reviewer's rotation strategy hit again — "new requirements × old contracts" found both criticals at seams no prior round touched; (2) H4 shows the review loop compounding: the E4 lesson ("requires without a decidable source is unimplementable") was re-applied by the *reviewer* to our own new text, arriving with the fix attached; (3) reviewer pre-announced round 5's attack surface (clock consistency of the maintenance-window contract vs. continuously-appending log vs. shared registry, treated as a distributed system) — noted for when/if a round 5 runs.

---

## I. Round-5 review — `docs/brush_room_system.md` v0.5 draft 1 (2026-07-07, roles swapped)

Roles reversed this round: the round-1–4 defender authored the draft; the round-1–4 reviewer (this section's author) attacks it. Same rules: concrete failure scenarios, §12's six open questions adjudicated individually. Overall verdict: **the skeleton is right** (compile-to-existing-machinery, three-layer separation, authored-namespace ownership, R1-before-S1 phasing all hold). But the draft violates its own scope guard ("anything requiring a second placement engine is a spec bug") in two places, and one placement predicate lets an annex eat the neighboring building. Findings below are **PENDING author response** — author annotates each with the fix or rebuttal.

### 🔴 Critical

**I1. The compilation claim is broken twice — `size_bias` and `fixed_yaw` need engine features fragment passes don't have.** §2.3 claims `apply_brush` "delegates to `FragmentEngine` unchanged." But: (a) `size_bias` skews fragment selection by footprint rank, while the engine's selection roll indexes a *uniform* list (fragment_system §5 step d) — there is no weighted-selection input; (b) `angle: "fixed"` demands a caller-supplied yaw, while yaw is derived (wall normal) or hash-rolled (fragment_system §4) — no override input exists. Both are exactly what the scope guard §1 says to flag. Cheapest fixes: (a) encode weights by **multiplicity** — compile the fragment list with names repeated ∝ weight (zero engine change, bounded expansion; specify rounding); (b) either add an optional `yaw_override` field to the compiled-pass model — a real engine change that must then be named in §11's pending-edit list — or cut `angle: "fixed"` from v1 (recommended: cut; `free`/`wall_aligned` cover the stated use cases, and `fixed` reintroduces the B6 axis-fixed failure mode for anything applied across multiple wall faces).

**I2. Room placement must pre-validate choke-point predicates, or partial envelopes result — and the annex "unoccupied" predicate is wrong.** Two compounding problems in §5 step 2b: (a) the annex test accepts any volume of "air / air-like / replaceable-whitelist blocks" — but the neighboring building's `stone_bricks` wall IS whitelisted, so an annex candidate overlapping the neighbor **passes the test and carves into it**; a player's small shed (footprint < `MIN_STRUCTURE_AREA`, so not a registry structure) is likewise whitelisted vanilla blocks — eaten. (b) Even where H1's choke-point row protects (activity sites cover the shed), protection *at write time* produces a **worse** outcome than refusal: the choke point skips the protected positions mid-envelope, leaving a room with holes in its shell — a structurally broken write, not a safe one. Required fix, one rule: **the placement solver must pre-check every predicate the choke point enforces (never-touch, markers, player-attribution zones incl. activity sites, other structures' bounds) and reject any candidate where even one envelope block would be skipped** — "the choke point remains the guarantee; the solver pre-validates so the guarantee never fires mid-envelope." Annex occupancy predicate becomes: air/air-like only, plus no intersection with any other registry structure's bounds or activity site (trees remain annexable — they're not structures; that is correct and should be said).

**I3. Carve mode without an interior graph carves through furnished lobbies.** §5 step 2a's "≥60% solid-or-interior-air" test: an existing *fully-air* lobby scores 100% "interior air" and **passes** — the test cannot distinguish dead space from occupied rooms without the graph, and the graph is optional in the current text ("no existing graph node's volume" is only meaningful if nodes exist). Failure: `add_room` carve on a graph-less building lands the candidate volume inside the lobby → envelope walls erected across it, furniture bisected. Fix: **carve mode requires `authored.interior_graph` present** (`graph_not_built` otherwise) — R4 already depends on R3, so this costs nothing in sequencing. See Q1 ruling for what replaces the 60% number.

### 🟠 Major

**I4. `capture_brush` re-imports the B6 orientation bug for wall anchors.** Capture records literal world offsets and emits `probability: 1.0` fragments — but for `anchor_surface: "outer_wall"/"inner_wall"` it neither rotates the example into the canonical frame (+Z = outward normal) nor sets `orientable: true`. A vine arrangement captured off a north-facing wall, applied to an east-facing wall: offsets are world-axis-fixed → the arrangement embeds into the wall or floats. Fix: capture must (i) derive the example's wall normal from the anchor's adjacent-air side, (ii) rotate captured offsets into the canonical frame, (iii) emit `orientable: true` and `requires_clear_above: false` for wall anchors.

**I5. `room_id` scope clips to the node's *air* bounds — wall brushes get zero anchors.** Graph node bounds are flood-fill air volumes; wall anchors are solid envelope blocks *outside* those bounds. `apply_brush(room_id=…)` with a wall-anchored brush collects no candidates and silently places nothing. Fix: room-scope clip = node bounds **expanded by 1** (include the envelope shell).

**I6. `min_spacing` breaks at space-kind boundaries.** §2.3 compiles one pass per kind and runs the engine per kind; spacing is enforced within one execution only (fragment_system §3). Failure: brush `min_spacing: 4`, corridor meets room at a doorway → one stamp each side, 2 apart. Fix options: (a) engine accepts an initial placed-anchors list, shared across the per-kind executions (small engine accommodation — name it in §11); (b) document per-kind spacing as accepted. Recommend (a); the doorway case is the common case, not the corner case.

**I7. Room-op destructiveness and write contexts are unmapped — the H4 gate can't fire mechanically.** §9 asserts the governance gate applies to `add_room` "being destructive — it carves", but the gate's server-decidable condition is defined over *pass content* (`destructive: true` fragments / `remove` rules), which room ops don't have. Define the mapping once: carve / `fill` / `collapse` / `seal` / `connect` = destructive; annex / furnish / condition = additive. Same table should assign write contexts explicitly per op (`fill`/`collapse` interior fill and `seal` plugs = `room_envelope`; §9 currently only covers "envelope + opening operations" loosely).

**I8. §6 re-editing: revert order unspecified, and two coverage gaps.** (a) Condition applies after decoration; reverting decoration entries first would conflict on every block condition later touched (journal `after` no longer matches). Specify: selected layers revert in **reverse chronological order** (condition → decoration). (b) Instances created before Phase 8 have no journal refs — state plainly they are permanently outside `recondition`/`refurnish` (visible in the record: empty `journal_entries`). (c) A standalone `apply_brush(room_id=…)` on an instantiated room must tag its journal entries with `(instance_id, layer)` — otherwise recondition misses brushwork added after instantiation and reverts only the template's original condition pass.

**I9. `connect` fails on walls thicker than 1 block.** Two air volumes separated by a 2-thick wall don't share a face — bounds don't touch — so "must share a wall face" returns `no_valid_placement` for ordinary MC construction. Define adjacency tolerance: wall thickness ≤ 2 (cut through up to 2 blocks); beyond → error.

### 🟡 Minor

**I10.** §5 step 2's position enumeration order is unspecified ("first candidate wins" is only deterministic if the enumeration is); sort admissible positions by (x, y, z) like everything else.

**I11.** S1 landing flips space kinds from `default` to real kinds → same (brush, scope, seed) produces a different diff across that boundary (stable keys change). Legitimate, but disclose it next to the degraded-mode note — same honesty pattern as the noise-backend caveat.

**I12.** Editorial: §8's "8 room/graph tools" is 11 by its own table; §2.2's `exterior` row isn't a "first match wins" rule (it's disjoint from the volume rules — restate as such); degraded mode should state explicitly that `exterior` classification (tier-1) still works pre-S1, only the three indoor kinds collapse to `default`.

**I13.** R1 reality check worth stating in §10: structure scope needs the registry (Phase S4) and room scope needs R3 — so **region scope is R1's only usable scope**. This is fine (and feeds the Q6 ruling) but should be a sentence in the phase table, not something the implementer discovers.

### §12 open questions — adjudicated

**Q1 (carve 60% threshold): REPLACE.** Drop the composite magic number. Carve-mode admissibility = (i) `interior_graph` present (I3), (ii) zero intersection with any non-removed graph node volume, (iii) floor support: ≥70% of the new room's floor face rests on solid blocks (reuse the annex ground rule — prevents carving a room that spans a courtyard's air and floats). Three named predicates, no percentage soup.

**Q2 (breach dressing coupling): TEMPLATE-SPECIFIED with shipped default.** `EntranceSlot` gains `dressing_fragment: str | null = null`; null resolves to shipped `breach_dressing_default`. One field, decoupled, zero cost when unused.

**Q3 (`abandon` mode): YES, add it.** Seal edges only, interior untouched. It's the cheapest primitive, the correct one for rehabitation prep ("close the wing, keep the contents"), and `collapse` is then honestly "abandon + dressing" as the draft itself noticed. Three modes: `abandon` / `fill` / `collapse`.

**Q4 (graph staleness): REPORT-ONLY STANDS for v1.** Auto-adopting condition-pass breaches as edges pollutes the graph with every decay run — intent and damage are different things (the draft's own §4.1 framing is right). Revisit only when the daily editorial loop exists and drift becomes measurable; a `graph_drift_warning` on destructive writes inside graphed structures is the eventual cheap hook, deferred.

**Q5 (palette jitter granularity): PER-SLOT-PER-BUILDING.** Hash key `(structure_id, slot_name, seed)`. All rooms in one host resolve each slot identically (coherent architecture — no patchwork between adjacent rooms), different hosts diverge (variety preserved via differing dominant materials and rolls). Per-instance jitter reads as patchwork; per-slot-per-instance is the clown-wall the draft feared.

**Q6 (brush region scope): KEEP — it is R1's only usable scope.** Structure scope requires the structure registry, which requires Phase S4; room scope requires R3. Dropping region scope would make R1 ship nothing usable, contradicting its own "value before segmentation" rationale. H1's choke-point protection is the correct safety story for region-scoped brushes (that's precisely why H1 moved protection to the choke point). Keep, with I13's sentence making the R1 dependency explicit.

**Round-5 process note:** the draft pre-empted two previously-recurring failure classes (it flagged the G1 air-write amendment proactively and left its magic numbers as declared attack surface) — the review loop is visibly training its participants. The failures that remained are the two classes the loop keeps finding everywhere: *claimed delegation that quietly needs new engine inputs* (I1, I6 — E10/G1's table-vs-prose cousin), and *predicates borrowed from the wrong domain* (I2's whitelist-as-occupancy — H5's area-threshold cousin).

### Round-5 re-check (draft 2) — SIGNED OFF

All 13 findings and 6 rulings verified genuinely landed; no fake patches. Spot-checks worth recording: the I1 multiplicity math is correct and its stated bound (n·(n+1)) safely over-covers the actual worst case (n·(n+1)/2); the I7 table classifying `seal`/`abandon` as **destructive despite writing solid-into-air** is a deliberate and correct policy divergence from the pass-content rule — sealing removes *access* to player space even though no block is removed, and the table (not pass content) is the normative source for room ops, so the H4 gate stays decidable; the I2 pre-validation correctly covers *both* modes (not just annex); §11 item 7 properly declares the one engine accommodation with a backward-compatibility note.

Two residuals found in re-check, both editorial (fix during §11 integration, no re-review needed):

- **I14 (trivial):** §10 source layout still says `tools/room.py — 8 room/graph tools`; per §8's own corrected count it is **11** (2 template + 3 graph-read + 4 graph-write + 2 re-edit). Draft 1's §8 miscount was fixed but its §10 echo wasn't.
- **I15 (minor, note-only):** there is no `unseal`/reopen operation — re-opening a sealed edge requires `connect` cutting a *new* opening (the sealed edge stays as record). Acceptable for v1 and consistent with the graph-as-intent model, but worth one sentence in §4.2 so rehabitation authors don't hunt for a missing tool. *(Author may fold this into the §11 integration pass.)*

**Verdict: draft 2 is normative on merge.** Author proceeds with the §11 eight-item integration into ARCHITECTURE.md; I14/I15 folded in. Next per plan: §6 player-activity pipeline spec (author: opposite session) + the pre-announced clock-consistency attack (reviewer: this session).

---

## J. Round-7 review — `wargame_interface.md` + `player_activity_pipeline.md` (v0.5 draft 1s) + HANDOFF/playbooks incidental check (2026-07-07)

Reviewer: this session (round-5 reviewer). The pre-announced clock-consistency attack was **largely pre-answered** by pipeline §7 — the one-clock rule, the greedy-protection/cut-attribution asymmetry, and the close-record-as-final-signal are all correct and survived scrutiny. The findings below live in the seams §7 did *not* cover: value provisioning (J1), time-decay of evidence (J2), and cross-layer identifier reachability (J3, J4). Playbooks incidental check: paper-runs are sound (playbook 2's I2 catch is real); HANDOFF is excellent — one staleness sweep needed (J10). Findings **PENDING author response**.

### 🔴 Critical

**J1. T_sync has no provisioning channel — the visibility cut can't be computed.** Pipeline §7.2 makes attribution normatively dependent on `save_synced_from_live_at`, and wargame §5 stores it in the window record. But the window record is appended **at close** (§7.3), while attribution runs **during** the window — and nothing anywhere says how Picasso *learns* T_sync at window open. It is infrastructure knowledge (when the save was snapshotted), not derivable from the save itself. Failure: detection runs, needs the cut, has no value — implementer either guesses (save-file mtime: wrong across copy tools) or skips the cut (attribution over phantom denominator, the exact bug §7.2 exists to prevent). Fix (recommended): the sync infrastructure writes a **sync marker file** `<save>/picasso_sync.json {"synced_from_live_at": …}` when it copies the save; `set_world` reads it and reports `t_sync` in its response; absent marker → `"t_sync": null` + warning, attribution falls back to greedy-with-caveat (`attribution_cut: "unavailable"`). One file, one contract line for the ops runbook, and the window record's field becomes a copy of it.

**J2. Log retention silently strips player protection.** Attribution lives in `detected.*` and is **recomputed from retained logs** at each detection (§5: "a player abandoning a base doesn't fossilize the label" — deliberate). But retention is an ops policy (§2.2): with 30-day retention, a base built 60 days ago has zero place-events in retained logs → next re-detection computes fraction = 0 → `native` → **H1 protection gone**. Anti-fossilization was aimed at *abandonment*; retention decay hits *active, occupied* bases whose construction merely scrolled out of the log. This is the strongest finding of the round because it is silent, delayed, and destroys the thing the whole pipeline exists to protect. Fix: attribution refresh must be **ratchet-down-guarded** — if the new fraction is lower *and* the log's oldest retained file is newer than the structure's recorded `first_built`, the refresh keeps the old verdict and flags `attribution: "stale-evidence"` (the evidence expired, not the ownership). Explicit downgrade requires `annotate_structure` (human/agent action, auditable). `first_built`/`builders` move to... no — they stay in `detected.*` but the *ratchet rule* is part of the §5 refresh semantics.

**J3. The wargame cannot know activity-site ids — `degrade @ activity_site_id` is unusable as specced.** Site ids are deterministic **per query window** and explicitly "query results, not registry objects" (pipeline §4.2); `query_player_activity` is an MCP tool — the wargame is a plugin-side engine that never calls MCP. Playbook 2's `wg_00043 degrade sky_bridge site_77ab` has no story for where the wargame got that id. Fix: work orders targeting non-registry geometry carry **explicit `bounds`** (the wargame observes the sky bridge live — it knows where it is); `site_id` becomes optional informative context. Picasso-side, the H5 governance-scope rule is satisfied by bounds directly (the site machinery remains for *agent*-initiated governance, where the agent did run the query). One param change in wargame §3's table, closes the hole.

**J4. The consent gap: a player can't get additions built onto their own base.** Wargame §7: `include_player_built` honored only for `degrade|demolish`. But the *canonical* flow (§3.1) is a player applying for construction — often onto **their own** `player_built` structure. That construct order hits the H1 row, can't carry the override (§7 forbids it for `construct`), and fails. The product's flagship loop is blocked by its own protection. Fix: **builder-consent rule** — an order whose `on_behalf_of.actor` is `player:<name>` where `<name>` ∈ the target structure's `builders` list may carry `include_player_built: true` for `construct`/`fortify`/`restyle` as well; the journal entry records consent basis (`consent: "builder"`). Protection still guards against *other* factions building on your base; the owner asking is not an intrusion. (Wargame-side validation of the application's authenticity is the wargame's job — Picasso checks the actor-builder match, which it can.)

### 🟠 Major

**J5. Q2 ruling (window atomicity): idempotent replay is NOT safe — two-phase with quarantine.** The draft's own determinism argument fails here: after a crash mid-`construct`, the world contains a **half-built envelope**; re-running the same order re-runs the I2 solver against that world, and the half-built walls now fail the annex "air-only" predicate (or carve's node-intersection) → the retry doesn't harmlessly replay, it *rejects* — or worse, solves to a different position and builds a second half-room. Ruling: consume-move-first (`applied/` with `"status": "in_progress"` before execution, receipt finalized after); on window open, any `in_progress` found = crashed predecessor → move to `failed/` with `crashed_window`, **never auto-re-execute**; recovery is journal-assisted (revert the crashed order's journal entries — they're tagged with the order's `on_behalf_of` — then the wargame may re-submit under a new id). Pre-Phase-8 (no journal): `construct` orders are only as crash-safe as their absence of journal — acceptable because Phase 9 lands after Phase 8 anyway (implementation_order already sequences this).

**J6. Protection noise floor: builds smaller than 8 events are invisible to the protection feed.** `MIN_EVENTS=8` filters mining scratches from *sites* — but the H1 protection feed consumes sites, so a 6-block shelter forms no site, is too small for the registry, and gets zero protection (H5's blind spot reborn one layer down). Fix: the protection feed clusters with its **own floor** `PICASSO_PROTECTION_MIN_EVENTS` (default **3**) — over-protecting costs a skipped decoration; under-protecting costs a player's shelter. Query tools keep the analytical floor (8).

**J7. Clustering join ambiguity breaks site determinism.** §3.1 step 2: "joins an existing open site if…" — when two open sites both qualify (player builds a connecting corridor between two clusters), the choice is unspecified, so site membership — and every downstream bound — depends on implementation accident. Fix one line: join the qualifying site with the **nearest bbox** (tie: earliest `first_event`); the merged-site case (event qualifies for both and bridges them) is *not* merged in v1 — it joins one, the other stays separate (site merging is analytical complexity the protection union doesn't need).

**J8. The flagship "tent beside the base" collides with site-bounds protection.** Protection covers the site's *bbox*; an NPC tent "beside" the base (v05 §6's first example, playbook 2 step 5) will often land inside that bbox → envelope pre-validation rejects every candidate → `no_valid_placement`. The playbook's terrain-annex path is correct but under-documented: add to pipeline §4.3 one sentence — protected zones are **hard constraints for the placement solver**; editorial reactions target *adjacent-to-but-outside* protected bounds, and `no_valid_placement` + retry-further-out is the expected loop, not an error condition. (Considered and rejected: shrinking protection to event-positions instead of bbox — a base's unbuilt courtyard deserves protection too.)

### 🟡 Minor

**J9. Remaining open-question rulings.** Pipeline Q1: **accept O(events) v1**; the compacted position-index sidecar (reader-maintained, `build_log/index/`, keyed by closed-file name) is the declared upgrade path when detection latency is measured to hurt. Pipeline Q2: **yes** — cache per (structure_id, log high-watermark); closed files are immutable so the watermark is sound; active-file events beyond the watermark are the only re-scan. Pipeline Q3: **accepted-documented** — ids are query-scoped by design; anchoring to file boundaries buys nothing (get_activity_site already requires the range). Pipeline Q4 (editorial-loop scope): **union of (new sites' bounds, 1-chunk padded) ∪ (registry structures intersecting those bounds)** — playbook 2 already models exactly this; promote its phrasing to pipeline §6. Wargame Q1: **bounds-hint + annex predicates suffice for v1**; explicit lot/parcel input waits for a wargame territory model (out of scope). Wargame Q3: **add `cadence_hint`** as an optional informative top-level field in `window_log.json` (`"cadence_hint": "daily"`) — one field, decouples the wargame's upkeep pricing from guessing; explicitly non-contractual (ops may deviate). Wargame Q4: **pull suffices** — receipts are files, the wargame polls at server start and after observing a round increment; a plugin-side convenience read is a plugin-repo decision, not contract.

**J10. Version-tracking staleness sweep.** Both drafts and HANDOFF's document table say "tracks v0.4.3" / "(v0.4.3)" but ARCHITECTURE is now **v0.4.4** (the author's own integration bumped it). Per the G6 rule ("the precedence rule is only executable if tracking lines are true"), sweep all three docs' headers + HANDOFF §1 kickoff prompt ("v0.4.3 contracts" → v0.4.4) + §2 table.

**J11. Apply-time protection-feed cost.** Every non-dry write re-clusters 14 days of logs (§4.3). Fold into J9-Q2's cache: clustering results per closed-file set + params are cacheable (files immutable); only the active file re-clusters per call. One sentence in §4.3.

### Verdict

The two specs are **sound in shape** — the queue-as-dispatcher with semantic-orders-only is the right contract, the receipt-as-only-feedback discipline is right, §7's clock rules survived the declared attack. J1–J4 are all **contract-hole** class (a value/identifier/permission that one side needs and no channel provides) rather than design-error class — which is what's left when the design itself is right. All four have one-file or one-field fixes. After the author lands J1–J8 + J10 and folds the J9 rulings, both docs are **normative on merge** without a further full round — spot-check of the diffs suffices (same closure protocol as round 5's re-check).

### Round-7 spot-check (draft 2s) — SIGNED OFF, dossier frozen

All 11 findings verified genuinely landed. Quality notes: **J5's** landed form is *stronger* than the ruling — the crash-recovery text adds "one window, one writer, so `in_progress` can never be a concurrent-execution marker — only a crash marker," which closes an ambiguity the ruling didn't name; **J2's** ratchet correctly preserves the upgrade path (higher recomputed state always applies — new building on old ruins upgrades normally) so the ratchet is one-directional exactly as intended; **J8's** landed text adds per-predicate failure reporting to `no_valid_placement` so agents can distinguish "step outward" from "give up" — beyond the ruling, correct; **J4's** override matrix header ("consent is a channel, not an exception") is the right framing and `composite_event`/`mark` correctly excluded from any override. J1's mtime prohibition, J3's `site_ref`-informative/`bounds`-authoritative split, J6's two-knob separation, J7's no-merge rule, J9's eight foldings, J10's version sweep: all verified.

Spot-check residuals fixed by the reviewer directly (editorial, no author round-trip): HANDOFF kickoff prompt's adjudication-section list extended to include §J; HANDOFF document-map row 9 upgraded from "v0.5 draft" to normative (it was self-stale after this very sign-off); the author's proposed "protection restrains the AI's hands, not the player's — not a territory system" framing added to the HANDOFF header (accepted — cheap insurance against the likeliest future misreading).

**Both specs are normative. The documentation system is frozen for implementation.** Final tally across 7 rounds: ~71 adjudicated findings (15+10+8+12+13+2 re-check+11), 3 retractions, 2 role swaps, 0 unresolved conflicts. Wargame-engine development can proceed against `wargame_interface.md` §2–§7 as a stable contract; codex proceeds per HANDOFF.md.

---

## D. Open questions — RESOLVED by product owner (2026-07-07)

All six were answered; decisions captured in `docs/v05_forward_requirements.md`. Summary:

**D1. RESOLVED(v0.5):** coverage caps stay a convention, but the real answer is bigger — control moves to a **Brush system** with per-structure-kind density/size/angle parameters (v05 §1). The ≤15% doctrine applies to texture passes only.

**D2. RESOLVED(v0.5):** per-structure variation is real and goes further than intensity overrides — agents will author **Rooms** (envelope + contents + entrance + placement) attached to structures (v05 §1). Zone stays cut; Structure grows sub-space classification instead.

**D3. RESOLVED(v0.5):** journal upgrades from undo-stack to **queryable styling history** (by bounds + pass) to support rehabitation's selective undo (v05 §2). Retention policy still open, but the query requirement now shapes the format.

**D4. Still open** (multi-dimension) — not raised; overworld-only stands until challenged.

**D5. RESOLVED(v0.5):** collisions stay accepted below the plausibility threshold; destruction events > ~150 blocks require causally linked composite events (v05 §3). Occupancy grid superseded by `CompositeEvent` sketch.

**D6. RESOLVED implicitly:** brush editing becomes the primary authoring surface, so `create_pass`'s block-pass-only limit is moot — brush tools will own fragment-application authoring (v05 §1).

**New decisions from the same session:** bidirectional styling / rehabitation bundles (v05 §2); human acceptance via in-game review-marker blocks + `scan_review_markers` tool (v05 §4); multi-source catalog — `PICASSO_CATALOG_PATHS`, `source` field, merge-with-warning — actionable in the current refactor (v05 §5); core/ stays MCP-free as the reserved external interface (v05 §5).
