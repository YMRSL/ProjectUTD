# HANDOFF — Implementation Kickoff Package

> For the coding agent (codex) taking this project from documentation to implementation. The documentation set below survived **7 adversarial review rounds** (2 independent AI reviewers alternating author/attacker roles, ~70 findings, all adjudicated in `docs/REVISION_LOG.md`). The docs are the contract; the current `src/` is a **v0.3-era prototype that predates most of the contract — treat it as reference material, not as a base to patch** (see "Delete-and-rebuild" below).
>
> **One conceptual framing to keep straight (and to repeat in any player-facing doc):** the player-protection machinery is a restraint on **the AI's hands, not the player's** — it stops automated styling from chewing player builds as collateral. It is *not* a land-claim/territory system; players are not granted permissions by it, and the wargame's builder-consent path (wargame_interface §7) is the sanctioned way work lands on a player's own base at their request.

---

## 1. Kickoff Prompt (paste-ready)

> Read `ARCHITECTURE.md` fully, then the reading order in its §14. Rebuild `src/picasso/` to match the v0.4.4 contracts plus the signed-off v0.5 specs (`docs/brush_room_system.md`; queue *format* from `docs/wargame_interface.md`). Follow the dependency map in `docs/implementation_order.md`: finish the Phase 1.5 in-game check before any DD write ships, but do not let that human gate block read-only Rail R0/R1 or SR0 inventory work (fallbacks: `docs/phase15_contingency.md`). On any ambiguity between documents: check `docs/REVISION_LOG.md` for an existing adjudication (E/F/G/H/I/J sections) before deciding; if truly novel, leave `TODO(arch):` and take the most literal reading of ARCHITECTURE.md. Never point any write at the production save. Every "Done When" list is a pytest list — write the tests with the phase, not after.

## 2. Document Map & Authority

| Read order | Document | Status |
|---|---|---|
| 1 | `ARCHITECTURE.md` (v0.4.4) | **authoritative** for cross-cutting contracts |
| 2 | `docs/fragment_system.md` | normative, primary mechanism |
| 3 | `docs/style_pass_schema.md` | normative, data formats |
| 4 | `docs/tool_specs.md` | normative, base tool I/O |
| 5 | `docs/style_learning.md` | normative for Phase 6 |
| 6 | `docs/agent_semantic_review.md` | normative Agent evidence/interpretation boundary |
| 7 | `docs/semantic_segmentation.md` + `structure_detection_tool_specs.md` | normative for S-phases |
| 8 | `docs/mechanical_structures.md` | normative static-ruin / high-priority Create-rail / later functional-machine split |
| 9 | `docs/implementation_order.md` + `segmentation_implementation_phases.md` | build sequence + status truth |
| 10 | `docs/brush_room_system.md` | **v0.5 normative** (round-5 signed off) |
| 11 | `docs/wargame_interface.md`, `docs/player_activity_pipeline.md` | **v0.5 normative** (round-7 reviewed + spot-checked); tools land Phases 9–10 |
| ref | `docs/REVISION_LOG.md` | adjudication record — the tiebreaker |
| ref | `docs/agent_playbooks.md` | how agents will drive what you build |
| ref | `docs/phase15_contingency.md` | only if the spike fails |
| — | `docs/v05_forward_requirements.md` | intent record; superseded sections marked |
| — | `README.md` | marketing-level, never authoritative |

**Precedence on conflict:** higher version wins; same version → ARCHITECTURE.md wins; REVISION_LOG adjudications explain *why* and stop re-litigation.

## 3. Dependency Map (parallel tracks; not one serial queue)

```
Phase 1.5  in-game DD review                    ← GATES DD writes only
R0–R1      vanilla-source rail evidence/graph → RailTemplate preview
SR0        exact-version stateless inventory (no DD write required)
Refactor ✅ choke point unification: ONE validation path carrying
           write_context {decoration, pattern_clear, room_envelope} +
           air-transparency rule + leaves persistent-injection +
           player-protection row (reserved slot) + H4 governance gate
Phase 5🔁  orientation substrate implemented; visual/shipped-content QA remains
Phase 3.5✅ halo read (boundary classification)
Phase 8 ✅ journal + revert — WITH conflict detection (H6), (instance_id,
           layer, on_behalf_of) tags from day one
Phase 6    learning tools  │  Phases S0–S4 segmentation  (parallel tracks)
R1–R5      brush/room system (R1 needs only the choke-point refactor)
Phase 9    work-order dispatcher (after Phase 8; queue format buildable earlier)
Phase 10✅ build-log reader + activity tools + protection feed hook
SR1        reviewed static mechanical ruins (DD members wait for Phase 1.5)
R2–R3      internal native-executor harness → reload/graph review →
           bidirectional train acceptance (high-priority exception)
Later M0+  functional machine executor → commissioned production line/farm;
           automatic siting waits for S2/S4, elevator waits for room graph
```

**Delete-and-rebuild guidance:** keep `models/` shapes as starting points, keep `amulet_bridge.py`'s working call patterns as reference for the pinned amulet version — rewrite everything else against spec. The 🔁 markers in implementation_order list every known divergence between prototype and contract.

## 4. The Two Recurring Spec-Bug Families (review-derived checklist)

Sixty findings distilled to the two failure patterns that kept recurring — check every subsystem you build against both:

1. **"Delegates to X" that silently needs a new input on X.** (E10 pattern-clear vs destructive gate; G1 whitelist vs air-writes; I1 brush size/angle vs engine inputs; I6 spacing vs per-kind execution.) *Check: for every "compiles to / delegates to / reuses" claim, enumerate the exact inputs the lower layer accepts. Any new input = a declared interface change, never an implicit one.*
2. **A predicate borrowed from the wrong domain.** (I2 replaceable-whitelist used as occupancy test; H5 area threshold as governance reachability; G2's original leaf-transparency temptation.) *Check: for every filter/threshold/list consulted, ask "was this list built to answer THIS question?" If not, define the right predicate — even if it's one line.*

Two operational rules with the same pedigree: **read shipped data before writing its consumer** (F6: the cosine sketch didn't match the shipped fingerprints file), and **every 🚧 must carry a phase assignment** (G-round: orphaned mitigations never happen).

## 5. Response-Field Honesty Contract

Every warning/marker field exists because a review round found a silent failure behind it. None are optional:

`journal_status` · `reversibility_warning` · `noise_backend` (required, all preview/apply) · `space_classification` · `space_kinds` · `region_mode_warning` (unconditional in region mode) · `player_protection` · `modded_write_warning`/gate · `conflicts[]` · `reachability_warning` · `no_match_warning` · `fatigue_warning` · `log_coverage` · `placements_skipped` · deprecation warnings.

## 6. Test Fixture World (build this first, Phase 1)

A purpose-built flat save, committed as a generation script (not binary), containing exactly:

- **Building A** "furnished house": 9×7×5 stone-brick shell, oak floor, 2 rooms + corridor, 1 door, 2 chairs (fence+slab), 1 table, 1 bed_frame pattern, torches. Exercises: classification (all 6 classes), pattern matching, room-graph bootstrap, carve-mode targets.
- **Building B** "tower": 5×5×20, windows on all 4 faces. Exercises: orientation on all faces (the B6 regression test), outer_wall/sky_open at height.
- **Road strip** 30×4 smooth stone at y=64 adjacent to A. Exercises: flat detection, road bundle entry, exterior space.
- **Boundary case**: building C straddling a chunk-region edge at radius cuts used in tests. Exercises: halo (Phase 3.5 seam test), partial-structure identity (G5).
- **Protected corner**: 1 structure_void marker + marker JSON; 1 command block. Exercises: choke-point rows 1–2.
- 40+ chunk flat margin for annex/freestanding placement tests.

Every phase's "Done When" that says "on the fixture" refers to this world. In-game visual verification happens on a copy of it with the DD mod installed.

## 7. Environment Notes

- Windows primary; C `noise` lib optional (`pip install picasso[perlin]` needs MSVC) — fallback backend is the baseline; `PICASSO_NOISE_BACKEND` pins.
- `scipy>=1.11` becomes a dependency when S-phases start (not before).
- Pin `amulet-core` at the Phase-1.5-verified version; record the (amulet, DD-mod, DataVersion) triple in `phase15_contingency.md` §1.
- The server plugin (build log, wargame live surface, potential Fallback-C stamper) is a separate repo; Picasso only ever reads/writes the documented file contracts.

## 8. What NOT to Build

- `apply_style_profile` (retired), Zone registry (cut), `unseal` (deliberate non-op, use `connect`), raw `write_blocks` work-order kind (rejected by design), auto-variant generation (telemetry suggests, humans author), per-bundle-entry seeds (rejected), geometric fragment scaling (rejected — author size variants), auto-merge in `reconcile` (report-only v1).

Each of these has a REVISION_LOG entry explaining why. Re-proposing them is re-litigation; the answer is there.
