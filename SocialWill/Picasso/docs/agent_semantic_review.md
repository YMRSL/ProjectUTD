# Agent-in-the-Loop Semantic Review

> Normative supplement to `ARCHITECTURE.md` v0.4.4. This document defines how
> deterministic world analysis, an MCP-connected Agent, and human design intent
> cooperate without turning model guesses into persistent world truth.
>
> Status: local stair/storey candidates, bounded voxel inspection, MCP server
> instructions, and the `interpret_world_structure` prompt are implemented.
> Candidate-run caching, evidence references, formal building/room segmentation,
> and registry annotation tools remain planned S-phase work.

## 1. The Boundary: Seeing Is Not Understanding

Amulet gives Picasso canonical block IDs, properties, coordinates, and block-
entity presence. Deterministic detectors can then prove geometric facts such as
"five bottom-half stairs rise northward" or "two horizontal support planes are
five blocks apart." Neither layer can prove human intent by itself.

An Agent may interpret those facts in context, compare hypotheses, and explain
why an assembly is likely a staircase, shelf, seat, room, or decorative trim.
Its interpretation remains a hypothesis until it is supported by sufficient
evidence and, where ambiguity or impact is high, confirmed by a human.

The governing rule is:

> Algorithms state physical evidence. Agents propose meaning. Humans own novel
> or high-impact intent. Registries preserve identity. Versioned rules generalize
> only confirmed, tested patterns.

## 2. End-to-End Flow

```text
RegionData
  -> deterministic signals and local candidates
  -> bounded, referenceable evidence
  -> Agent hypotheses + alternatives + confidence
  -> human confirmation when required
  -> detected/authored Structure Registry namespaces
  -> repeated confirmed examples
  -> reviewed Pattern/fingerprint/detector proposal + negative tests
```

The Agent is part of the interpretation layer, not a bypass around the write
choke, journal, player protection, or structure identity rules.

## 3. Responsibility Matrix

| Layer | Owns | Must not do |
|---|---|---|
| Amulet bridge | Native Java block state, properties, coordinates, block-entity presence | Infer furniture, rooms, floors, or buildings |
| Deterministic analysis | Connectivity, surfaces, height levels, voids, material statistics, known patterns, local assemblies, bounds and confidence | Convert one weak signal into an authoritative semantic label |
| Agent | Read compressed evidence, combine context, rank interpretations, cite facts, disclose alternatives and boundary risks | Treat summaries as geometry, hide truncation, or persist a guess as truth |
| Human | Supply design intent, confirm new/ambiguous types, correct bounds and meanings | Manually label every repeated high-confidence instance forever |
| Structure Registry | Stable IDs, `detected` evidence, `authored` corrections, provenance and stale state | Store raw chunks or erase authored meaning during re-detection |
| Rule library | Reusable Patterns, fingerprints and detectors backed by positive and adversarial tests | Promote a one-off Agent interpretation directly into global behavior |

## 4. Progressive Evidence Reading

The Agent reads only as much world evidence as the current question needs:

1. `analyze_region` returns surface/material summaries, known furniture patterns,
   local stair assemblies, and horizontal-level candidates.
2. `inspect_volume` returns an inclusive bounded voxel view. Air is implicit;
   non-air states are encoded through a deterministic palette and
   Y-layer/Z-row/X-run RLE while preserving properties such as `facing`, `half`,
   `shape`, `axis`, and fence connections.
3. If a view is truncated, partial, or touches the analysis boundary, the Agent
   subdivides or requests adjacent evidence. Missing data is never interpreted
   as air.
4. Formal S-phase detection will add candidate IDs, evidence references,
   relationships, contradictions, and paginated evidence retrieval. Those
   planned objects must reference the same underlying facts rather than copying
   whole chunk dumps into model context.

Whole-world block dumps are prohibited as an Agent interface: they are expensive,
hard to reason about, and encourage silent token truncation. Large maps are read
hierarchically: district -> structure candidate -> storey/room -> local volume.

## 5. MCP Guidance Surfaces

Picasso publishes two complementary guidance layers:

- FastMCP server `instructions` are sent in the MCP initialization result. They
  carry the universal honesty and no-implicit-write rules. A host may choose how
  strongly to use them, so they are guidance rather than a safety mechanism.
- The `interpret_world_structure` MCP prompt is an explicit read-only workflow
  for a supplied bounding volume and focus. It tells the Agent to call analysis
  and inspection tools, separate facts from interpretations, cite evidence,
  report alternatives, and avoid all world-mutating tools.

Prompts and resources do not grant data access by themselves. The bounded
inspection tool is required because an Agent cannot reconstruct geometry from
aggregate counts. Conversely, inspection data without the prompt does not define
how uncertainty, human intent, or rule promotion must be handled.

Prompt rendering must remain static and must not read the shared world session;
only serialized MCP tools access the session and Amulet bridge.

## 6. Interpretation Result Contract

An Agent review should return three clearly separated sections:

1. **Observed facts** — bounds, block states/properties, connected levels,
   openings, candidate kinds and detector confidence.
2. **Ranked interpretations** — primary hypothesis, alternatives, Agent
   confidence, and the specific evidence supporting each.
3. **Unknowns and risks** — truncated/partial views, boundary clipping, missing
   vertical context, ambiguous ground level, flood-fill leakage through doors,
   or absent human ground truth.

For future persisted evidence, facts receive stable IDs and each semantic claim
must carry `basis: [fact_id, ...]`. Until that evidence store lands, bounds,
palette entries, role counts, and tool response fields serve as citations inside
the review response.

## 7. Human Confirmation and Registry Ownership

The Structure Registry's two namespaces remain mandatory:

- `detected.*` contains reproducible machine evidence and may be refreshed by
  re-detection.
- `authored.*` contains human/Agent-reviewed meaning, corrections, room graphs,
  and explicit overrides. Re-detection never overwrites it.

Low-confidence new types, close competing hypotheses, partial boundaries, and
anything used to authorize destructive editing require human confirmation.
Confirmed low-risk repeated instances may be batch-accepted, but the acceptance
operation must remain explicit and auditable.

## 8. Rule Promotion

One explanation is a fixture, not a universal rule. Promoting semantics into a
Pattern, fingerprint, or detector requires:

1. multiple confirmed positive examples, preferably from different buildings;
2. meaningful negative examples such as real stairs, bleachers, roofs, fences,
   and decorative columns;
3. a deterministic proposal with declared confidence and ambiguity behavior;
4. positive, rotated/property, boundary, and adversarial regression tests;
5. human approval of the versioned data/code change.

When a user explanation conflicts with a detector, Picasso preserves the physical
facts, records the explanation as authored ground truth for that example, and
changes the general rule only through this promotion process.

## 9. Resource and Token Limits

- Candidate summaries are paginated and return totals plus truncation markers.
- Bounded voxel views are capped at 32x24x32 blocks and 4096 encoded runs per
  response; truncated output is explicit and must be subdivided before inference.
- Palette entries aggregate identical block ID/property states and include counts.
- Air is implicit inside the inclusive bounds; absence is meaningful only when
  `complete=true`.
- Block-entity presence may be reported, but NBT is not exposed by RegionData.
- Halo/context may inform detectors but is not returned as target evidence.

## 10. Complex-Map Validation

`Picasso-Test1` is the controlled local semantic fixture. Mosslorn is the next
read-only realism challenge because it contains dense, multi-level architecture,
doors, stairs, roofs, and decorated interiors. Validation proceeds on a bounded
building area before district-scale scanning and initially emits room/storey
candidates rather than authoritative room names.

The existing `D:\MC\picasso_mosslorn_test` path is a junction to a real building
archive, not a disposable copy. It may be used only for coordinated read-only
inspection while Minecraft and other editors are closed. Any write experiment
requires a separately named physical copy.

## 11. Implementation Phases

1. **A0 - local evidence (implemented):** local semantics, bounded RLE inspection,
   server instructions, interpretation prompt, synthetic tests.
2. **A1 - candidate evidence:** scan run IDs, candidate/evidence IDs, pagination,
   relationships and contradiction fields; read-only.
3. **S1-S4 - formal segmentation:** enclosed volumes, structures, stable identity,
   registry and room/storey relationships.
4. **A2 - reviewed annotation:** explicit authored corrections and acceptance,
   preserving detected/authored ownership.
5. **A3 - rule learning:** confirmed-example collection, proposal generation,
   adversarial validation and human-approved promotion.

