# Picasso — Impressionist World Stylization MCP Server

**Picasso** is a Model Context Protocol (MCP) server that wraps [Amulet-Core](https://github.com/Amulet-Team/Amulet-Core) to give AI agents the tools to stylize Minecraft worlds through layered, composable transformations — like building up an oil painting, one brushstroke at a time.

## Concept: Fragments over Filters

> Decay is a structural story, not a color change.

The primary mechanism is the **Fragment** — a small compositional event (a rubble pile, a breached wall, an overgrown window) placed at semantically valid anchor points. Block-color replacement exists but is deliberately capped as a light texture accent (≤15% coverage). An ordered **Style Bundle** composes fragment passes, texture passes, and furniture replacement into a one-command stylization:

```
1. material_hints        (block_pass)      → ≤15% weathered texture variation
2. window_breach         (fragment_pass)   → glass out, vines in
3. wall_collapse         (fragment_pass)   → localized holes + rubble (destructive)
4. vine_network          (fragment_pass)   → vines climbing outer walls
5. roof_tree_growth      (fragment_pass)   → trees punching through rooftops
6. rubble_scatter        (fragment_pass)   → debris on floors and streets
7. floor_root_crack      (fragment_pass)   → roots cracking interior floors
8. furniture_modreplace  (pattern_replace) → vanilla furniture → DD decoration blocks
```

The planned reverse-learning workflow is: statistical profiling + recurring-cluster extraction → the agent authors new Fragments and a new Bundle (`docs/style_learning.md`). `learn_style` and cluster extraction are not yet part of the public tool surface.

## Built for SocialWill

Picasso is the **World Layer** of the SocialWill project. It exposes an NPC marker interface (`place_npc_marker`) as the entry point for the future Narrative Layer to instantiate characters into the world.

## Quick Start

Picasso is pinned to **CPython 3.11** and the Amulet/MCP versions verified by the
temporary-world integration suite. From this directory, with
[uv](https://docs.astral.sh/uv/) installed:

```powershell
uv python install 3.11
uv sync --extra test
uv run picasso
```

Optional C Perlin backend: `uv sync --extra perlin --extra test`. The built-in
fallback remains the portable baseline.

Connect via stdio in Hermes or VSCode Codex as an MCP server. Typical agent session:

```
set_world(path) → analyze_region(...) → apply_bundle("tlou_complete", dry_run=true)
→ review preview/protection status → apply_bundle(..., dry_run=false)
→ inspect/list journal as needed → close_world()
```

Read-only semantic review uses the MCP `interpret_world_structure` prompt and a
progressive evidence path: `analyze_region(...)` → bounded
`inspect_volume(...)`. The Agent receives exact block properties through a
palette/RLE view without dumping an entire world into context.

The current server registers **22 MCP tools** and one structure-interpretation
prompt, including explicit world close, bounded voxel evidence, three
journal/revert tools, two player-activity tools, and capability diagnostics.

## Project Structure

```
Picasso/
├── README.md                (this file — orientation only, never authoritative)
├── ARCHITECTURE.md          ← authoritative. Read first. v0.4
├── pyproject.toml
├── src/picasso/
│   ├── server.py            MCP entry point
│   ├── tools/               MCP tool wrappers (thin; no business logic)
│   ├── core/                Engines & analysis (no MCP imports; unit-testable)
│   ├── models/              Pydantic/dataclass models
│   └── data/                catalog · passes · fragments · bundles · patterns · safe_blocks
└── docs/                    Subsystem specs — index in ARCHITECTURE.md §14
```

## Catalog

The Doomsday Decoration semantic catalog (1143 blocks annotated with
surface/context/category/function) is packaged at
`src/picasso/data/catalog/doomsday_decoration_semantic.json`; no manual copy is
required. `PICASSO_CATALOG_PATHS` (or the legacy single
`PICASSO_CATALOG_PATH`) may point at compatible catalogs when extending or
replacing the bundled vocabulary.

## Key Constraints

- **World path is dynamic.** The calling agent confirms the save path via `set_world()` before any operation. Nothing is hardcoded.
- **Preview style work before write.** Use `preview_pass`; `apply_bundle` defaults to `dry_run=true`. `place_npc_marker` has no dry-run, but accepts only a directly verified empty target and saves through the common choke point.
- **Fail-closed writes.** The write choke rejects positions outside the fully read envelope, unloaded chunks, block entities, protected markers/player activity, missing/invalid safety policy, and unknown non-vanilla catalog IDs. Fragment instances and pattern replacements are atomic groups, so a protected constituent cannot leave a partial vehicle or furniture replacement.
- **Context-aware reads are implemented.** Region reads use sparse chunk palettes, a one-chunk horizontal halo, and bounded vertical context; context positions inform classification but remain outside the writable envelope.
- **Writes are journaled and conflict-safe.** A durable pending entry precedes every world mutation, then becomes committed or failed. `revert_last_apply` verifies current state before restoring and skips third-party conflicts. NPC block + marker JSON are one compound journal artifact.
- **Phase 1.5 is still open.** The controlled `Picasso-Test1` save now contains a simple DD block, a DD block with `facing=east`, and a vanilla control; all three survived an Amulet save/close/reopen at the recorded coordinates. Minecraft 1.21.1 with the DD mod must still verify their rendering, facing, collision and interaction. Keep `PICASSO_MODDED_WRITE_VERIFIED=false` until that in-game check passes.
- **Current operating boundary.** Picasso is usable on copied saves. For a formal/shared world, write only while Minecraft is closed, `journal_status=active`, player protection is active, and the exact operation was dry-run first. `analyze_region` reports conservative local candidates and `inspect_volume` lets an Agent review bounded physical evidence, but full building/room segmentation and structure mode remain future work. Agent interpretation is a hypothesis, not write authorization or persistent truth; see `docs/agent_semantic_review.md`. Brush/Room, work orders, `learn_style`, and cluster extraction also remain future work; a wall with air on both sides has no semantic exterior signal, so its normal is chosen deterministically from the two valid sides.
- **Create/mechanical boundary.** The first mechanical slice (SR1) permits only static, mechanically plausible ruins after the SR0 capability gate. The high-priority rail line now has pure read-only classifiers for the reviewed vanilla source graph and audited Create/Railways target states; neither is an apply tool, and BE/Bezier/SavedData evidence is still missing. Replacement uses dedicated `RailTemplate`/`RailNetwork` records plus game-native placement, durable rollback and in-game acceptance. Functional production lines, elevators and farms remain later work; see `docs/mechanical_structures.md`.

Verification snapshot: **407 passed, 1 skipped, 14 subtests**, and an isolated
wheel smoke test loading **1143 catalog entries, 14
passes, 11 fragments, and 1 bundle**.
