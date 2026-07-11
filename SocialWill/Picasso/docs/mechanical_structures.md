# Mechanical Ruins, Create Rails, and Later Functional Machines

> Normative forward design for Picasso. Tracks `ARCHITECTURE.md` v0.4.4.
>
> Decision status: **static-first**. The implemented Fragment substrate may be
> reused for static mechanical ruins, but no mechanical prefab registry or
> Create rail executor is implemented yet. Functional machinery is explicitly
> **later**. The only high-priority Create exception is train-track replacement.

## 1. Scope and Priority Split

The SocialWill 1.21.1 pack contains Create `6.0.10` plus add-ons. Picasso does
not currently ship a `create:*` catalog, Create block-entity capability map,
kinetic topology, Create-native placement bridge, or commissioning probe. The
global `PICASSO_MODDED_WRITE_VERIFIED` flag is a DD gate and must never be read
as Create authorization.

Mechanical work is split into three deliberately separate tracks:

| Track | Goal | Priority | Execution boundary |
|---|---|---|---|
| **Static mechanical ruins** | Plausible broken machinery, pipes, inert frames and abandoned industrial compositions | SR1, first mechanical slice | Fragment or a thin static prefab; block-state-only |
| **Create rail replacement** | Replace one bounded reviewed vanilla-rail component with a continuous Create train-track network | High-priority exception | Dedicated `RailTemplate` / `RailNetwork`; game-native placement |
| **Functional machinery** | Running production lines, farms, elevators and contraptions | **Later** | `MechanicalTemplate` + game-native executor + commissioning |

These tracks do not inherit capabilities from one another. A block proven safe
inside a static ruin does not authorize a rail connection, and a verified rail
executor does not authorize arbitrary kinetic machines.

## 2. SR1: First Mechanical Slice — Static Mechanical Ruins

SR1 only promises a visually and physically plausible *ruin*. It does not
promise rotation, stress transfer, item transport, inventories, schedules,
contraptions, or any other runtime behavior.

Examples in scope:

- a broken machine casing with rubble and disconnected pipes;
- an inert shaft/cog composition whose exact block states were verified safe;
- collapsed factory framing, abandoned work platforms and decorative conduits;
- DD or vanilla industrial props arranged around a damaged foundation.

Examples out of scope:

- powered belts, presses, deployers, bearings, pistons or elevators;
- inventories, filters, schedules, train stations or moving contraptions;
- anything whose correct state depends on kinetic-network commissioning;
- any block that requires a block entity or NBT, even if the intended instance
  would contain an "empty" configuration.

### 2.1 Fragment first, StaticPrefab only when needed

Use an ordinary Fragment when the composition is small, probabilistic, organic,
or anchored to a surface. It automatically inherits rotation, clearance,
instance atomic groups, the write choke, player protection and journal behavior.

A future `StaticPrefab` is justified only for a larger exact layout where every
block and offset must be emitted together. It remains a block-state artifact,
not a schematic or mechanical template:

```json
{
  "name": "collapsed_factory_drive_static",
  "version": 1,
  "size": [9, 5, 6],
  "anchor": [2, 0, 3],
  "allowed_rotations": [0, 90, 180, 270],
  "tags": ["static_ruin", "industrial", "nonfunctional"],
  "blocks": [
    {
      "offset": [0, 0, 0],
      "block": "minecraft:oxidized_copper",
      "properties": {}
    }
  ]
}
```

`StaticPrefab` placement must compile to the same `RegionData` change model and
one atomic group. It gets no ports, speed, inventories, commissioning, instance
registry, or special removal path. Journal revert is sufficient.

### 2.2 Strict no-BE/no-NBT gate

Static content is fail-closed at authoring, load, preview and apply:

1. Every block ID must exist in the exact-version semantic/capability inventory.
2. That inventory must explicitly state `block_entity=false` and
   `requires_nbt=false`; missing metadata means **reject**, not assume safe.
3. If any legal state of an ID requires a block entity, SR0/SR1 rejects the
   entire ID rather than attempting a fragile state-specific exception.
4. Templates contain only namespaced block IDs and ordinary blockstate
   properties. NBT blobs, inventory payloads, filters and serialized entities
   are schema errors.
5. Existing block-entity positions in the target remain unconditionally
   protected by the current write choke.
6. Create/add-on blocks additionally require exact-mod-version block-state
   save/reopen and in-game stability verification. DD verification does not
   satisfy this requirement.

This gate must be backed by extracted registry/BE evidence from the installed
mod version, not a hand-maintained list of names that merely look decorative.

### 2.3 Static placement acceptance

A shipped static ruin must pass:

- full footprint/clearance and support checks after rotation;
- no floating falling blocks or unsupported attachable blocks;
- no liquid, marker, player-protection, never-touch or block-entity collision;
- exact preview/apply agreement and one-group atomic rejection;
- save/reopen block ID/property verification;
- in-game visual review confirming that neighbor updates do not turn the ruin
  into a functioning or self-destructing mechanism.

## 3. High-Priority Exception: Create Rail Replacement

Create train track is not modeled as a Fragment or StaticPrefab. A route is a
connected geometric network; curves, slopes and joins may depend on Create block
entities, NBT or runtime placement APIs. Raw Amulet block writes are forbidden
for all rail segment kinds, including apparently simple straight segments.

The first rail scope is **bounded replacement of one reviewed, connected
vanilla-rail component**. The source vocabulary is the Minecraft rail family,
not an existing Create/Railways network. Ordinary `minecraft:rail` shapes form
the first accepted source; powered, detector and activator rails are retained as
typed evidence and block authorization until an explicit semantic-loss policy
is reviewed. Existing Create/Railways network adoption is later work.

Target-side evidence is separate: fixtures and post-placement verification must
still read Create/Railways block states, track block entities/Bezier connections
and `create_tracks.dat`. That evidence validates the generated target; it does
not make target-network parsing a prerequisite for reading the vanilla source.
The first scope is not an autonomous whole-world railway designer. Automatic
route planning, stations, signals, schedules and train assembly are later.

## 4. RailTemplate

A `RailTemplate` describes one validated geometric segment and its native
placement requirements, not a raw list of blocks:

```json
{
  "name": "create_track_curve_r16_90deg",
  "version": 1,
  "required_mods": {"create": "6.0.10"},
  "kind": "curve",
  "backend": "create_native_track",
  "geometry": {
    "horizontal_radius": 16,
    "turn_degrees": 90,
    "elevation_delta": 0
  },
  "endpoints": [
    {"name": "a", "offset": [0, 0, 0], "tangent": "south"},
    {"name": "b", "offset": [16, 0, 16], "tangent": "east"}
  ],
  "clearance": {"horizontal": 2, "above": 5, "below": 1},
  "support": {"max_unsupported_span": 4},
  "artifact_sha256": "..."
}
```

Required segment families:

- **Straight:** collinear endpoints, identical elevation and tangent; arbitrary
  length is composed from validated native placements, not stretched block
  arrays.
- **Slope:** collinear endpoints with an explicit elevation delta and validated
  grade. Preview must prove support and overhead clearance for every step.
- **Curve:** explicit entry/exit tangents, radius/control geometry and elevation.
  Curves are produced through the Create API/validated native artifact; Picasso
  never invents curve NBT or rotates a straight block list into a curve.

Junctions, stations and signals are not implicit fourth segment types. They
require their own reviewed templates and are outside the first replacement
slice.

## 5. RailNetwork

A `RailNetwork` is the graph-level plan and receipt for one replacement:

```json
{
  "id": "railnet_0001",
  "source_bounds": {
    "x_min": 100, "x_max": 240,
    "y_min": 62, "y_max": 78,
    "z_min": -40, "z_max": 90
  },
  "source_digest": "...",
  "nodes": [
    {"id": "n1", "pos": [100, 64, 0], "tangent": "east", "degree": 1},
    {"id": "n2", "pos": [180, 68, 0], "tangent": "east", "degree": 2}
  ],
  "edges": [
    {"id": "e1", "from": "n1", "to": "n2", "template": "create_track_slope_1in20"}
  ],
  "status": "previewed",
  "native_receipt": null
}
```

A `status=previewed` object is an ephemeral response bound to its source digest;
R1 does not write it into the world registry. Durable lifecycle begins only
after the native executor has captured a rollback artifact and, before the first
world mutation, records the same reviewed plan as `pending`.

Connection rules are exact:

1. Joined endpoints occupy the same native connection point with compatible
   gauge, elevation and opposing tangents.
2. Every non-terminal node has the expected degree; no accidental gap, overlap,
   duplicate connection or orphan edge is accepted.
3. Straight/slope/curve transitions use a validated template pair. Geometrically
   close endpoints are not silently snapped.
4. The first release preserves one source connected component at a time and
   rejects ambiguous switches, crossings or unsupported source shapes.
5. Network identity binds the source digest, Create version, template versions,
   transformed geometry and native executor receipt.

## 6. Rail Replacement Workflow

1. Read a bounded, fully loaded vanilla source route and retain every rail-family
   ID/property. More than one connected component is a blocking ambiguity.
2. Build the selected vanilla component and classify its edges as straight,
   ascending or curved from vanilla rail shapes and coordinates. Powered,
   detector and activator rails remain explicit unsupported semantics unless a
   reviewed policy handles them.
3. Fit validated RailTemplates while preserving terminal positions and route
   connectivity. Unrepresentable shapes become blocking diagnostics.
4. Validate foundation, support, clearance, protected areas, liquids, chunk
   boundaries and all endpoint connections.
5. Produce a preview; no world or Create state changes at this stage.
6. During R2/R3, only an internal acceptance harness on a disposable copy may
   send the exact reviewed plan to the exact-version Create-native executor.
   The public MCP apply tool is not registered/enabled yet.
7. The executor captures its native rollback artifact, then durably writes the
   `RailNetwork` as `pending` **before** the first world mutation. It transitions
   through `applying` and `awaiting_acceptance` while removing only the approved
   source rails, placing through Create/Minecraft APIs and reloading the result.
8. R3 acceptance changes the record to `accepted`. A recoverable failure uses
   the native artifact and records `rolled_back`; an incomplete recovery records
   `failed_needs_recovery` and blocks further rail writes. Preview objects are
   never persisted, while every mutation attempt is.
9. Only after the executor/template families pass R3 reload, graph and
   bidirectional traversal acceptance may the public `apply_rail_replacement`
   tool be registered. Each public call remains bound to the exact preview/source digest
   and follows the same durable state machine.

Current Picasso block-state journal cannot claim complete rollback of Create
track block entities/NBT. Until the native executor provides a verified rollback
artifact, `apply_rail_replacement` must remain unavailable even if preview works.

## 7. Rail Preview Contract

Planned R1 read-only tools:

- `preview_rail_replacement(bounds, options)`
- `list_rail_templates()`

Planned R2 recovery/registry read tools (durable records start at `pending`,
never at preview):

- `list_rail_networks()`
- `inspect_rail_network(rail_network_id)`

The preview response must include:

- source digest, exact Create version and covered chunk list;
- source/target node and edge counts plus connected-component counts;
- segment plan with straight/slope/curve geometry and endpoint tangents;
- removed vanilla rail count and proposed native Create track count/extent;
- unsupported source shapes, endpoint gaps, grade/radius failures;
- support/clearance/protection/block-entity conflicts;
- native artifact/operation digest and estimated affected bounds;
- `complete`, `truncated`, and explicit omissions; incomplete evidence cannot be
  authorized for apply.

`apply_rail_replacement` is the later public write twin. R2/R3 exercise an
internal disposable-world harness; the MCP tool is exposed only after R3. It
defaults to dry-run and may execute only the exact preview/source digest.

## 8. Rail Acceptance

Every shipped template and replacement operation requires:

1. schema, transform and deterministic graph tests;
2. positive and adversarial straight/slope/curve connection fixtures;
3. zero missing/orphan/duplicate native connections after placement;
4. Create-native graph inspection matching the previewed node/edge topology;
5. save, close, reload and re-inspection under the exact modpack version;
6. an in-game Create train traversing every edge in both directions, including
   each slope and curve family, without derailment or manual repair;
7. visual review of supports, clearances and transitions;
8. rollback testing that restores the original source network after an injected
   mid-operation failure;
9. no unplanned block entities, inventories, stations, signals or schedules.

Passing DD Phase 1.5, static-prefab verification, or a straight-track test does
not waive curve/slope/network acceptance.

## 9. Functional Mechanical Executor — Later

Running production lines, farms, elevators, bearings and contraptions remain a
valid future design, but they are outside SR1 and do not block static ruins
or the rail replacement exception.

A later versioned `MechanicalTemplate` retains the previous design obligations:

```json
{
  "name": "create_basic_press_line",
  "version": 1,
  "kind": "production_line",
  "required_mods": {"create": "6.0.10"},
  "artifact": {
    "backend": "game_native_schematic",
    "path": "mechanical/create_basic_press_line.nbt",
    "sha256": "..."
  },
  "size": [13, 6, 7],
  "anchor": [1, 0, 3],
  "allowed_rotations": [0, 90, 180, 270],
  "ports": [
    {"name": "item_input", "kind": "item", "offset": [0, 1, 3], "facing": "west"},
    {"name": "kinetic_input", "kind": "rotation", "offset": [2, 1, 6], "axis": "z"}
  ],
  "commissioning": {
    "probe": "item_transfer",
    "timeout_ticks": 200,
    "expected": {"minimum_items_moved": 1}
  }
}
```

The later subsystem still requires:

- exact mod/version/artifact digests and legal NBT transforms;
- foundation, clearance, access, moving-envelope and typed-port validation;
- a game-native schematic/plugin executor, never fabricated Amulet NBT;
- `MechanicalInstance` records in `<world>/picasso_mechanical.json`;
- commissioning and reload receipts;
- instance-scoped, conflict-safe native disassembly;
- explicit adoption for manually built machines;
- per-namespace/version/template capability gates.

Planned later tools remain:

- `list_mechanical_templates`
- `preview_mechanical_structure`
- `apply_mechanical_structure`
- `remove_mechanical_structure`
- `adopt_mechanical_structure`
- `list_mechanical_instances`
- `inspect_mechanical_instance`

Work orders may eventually dispatch a reviewed template, but never gain a second
write path around the mechanical executor, journal, protection, or receipts.

## 10. Revised Implementation Order

1. **SR0 — static vocabulary gate:** extract exact-version BE/NBT capability
   inventory; define the strict stateless allowlist.
2. **SR1 — static ruins:** ship several reviewed mechanical-looking Fragments;
   introduce `StaticPrefab` only if exact larger layouts cannot remain Fragments.
3. **R0 — rail evidence:** bounded vanilla-source component reader/graph plus
   exact-version Create/Railways target-state evidence; classify source
   straight/slope/corner geometry without pretending BE payloads were read.
4. **R1 — RailTemplate preview:** connection solver, support/clearance checks,
   preview digest and adversarial fixtures.
5. **R2 — native rail executor:** exact Create API bridge, rollback artifact and
   straight/slope/curve placement.
6. **R3 — rail acceptance:** reload, graph inspection and bidirectional train
   traversal; only then enable apply.
7. **Later M0+ — functional machines:** capability matrix, MechanicalTemplate,
   MechanicalInstance, commissioning, removal, farms, production lines and
   elevators. Automatic site search follows segmentation/Room readiness.
