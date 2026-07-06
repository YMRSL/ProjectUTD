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

## D. Open questions (need product-owner input, not more spec)

**D1.** Should texture-pass coverage caps (≤15%) be *enforced* (engine clamps effective coverage) or remain a design convention? Enforcement penalizes intentional heavy-moss custom passes.

**D2.** Per-structure style variation: bundles currently apply uniformly per structure_type. Is "this particular building should be extra collapsed" a real need (→ per-structure intensity overrides on Structure, resurrecting the one useful Zone field), or is `apply_pass_to_structure` ad-hoc layering enough?

**D3.** Journal retention: unbounded journals grow forever on a large project. Cap by count/age/size? Auto-archive after N applies?

**D4.** Multi-dimension support (nether/end) is currently out of scope (`"minecraft:overworld"` fixed). Confirm out-of-scope or spec now (affects RegionData keys, structure registry schema).

**D5.** `min_spacing` across passes (C4): accept collisions, or bundle-level occupancy grid?

**D6.** Should `create_pass` support authoring fragment passes (currently block-pass-only, fragments via `create_fragment` + hand-authored pass JSON)? Matters for the learning workflow's ergonomics — right now step 7 of style_learning §9 requires the agent to write a fragment-pass JSON without a dedicated tool.
