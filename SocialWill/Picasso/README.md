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

Styles can also be **learned** from a reference save: statistical profiling + recurring-cluster extraction → the agent authors new Fragments and a new Bundle (`docs/style_learning.md`).

## Built for SocialWill

Picasso is the **World Layer** of the SocialWill project. It exposes an NPC marker interface (`place_npc_marker`) as the entry point for the future Narrative Layer to instantiate characters into the world.

## Quick Start

```bash
cd Picasso
pip install -e .          # optional Perlin accel: pip install -e .[perlin]
python -m picasso.server
```

Connect via stdio in Hermes or VSCode Codex as an MCP server. Typical agent session:

```
set_world(path) → analyze_region(...) → apply_bundle("tlou_complete", dry_run=true)
→ review preview → apply_bundle(..., dry_run=false)
```

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

The Doomsday Decoration semantic catalog (1143 blocks annotated with surface/context/category/function) is the replacement vocabulary:

```
D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration\doomsday_decoration_semantic.json
```

Copy it to `src/picasso/data/catalog/` (or point `PICASSO_CATALOG_PATH` at it). The server is not DD-specific — any catalog with the same schema works.

## Key Constraints

- **World path is dynamic.** The calling agent confirms the save path via `set_world()` before any operation. Nothing is hardcoded.
- **Preview before write.** Every write tool has a dry-run mode; `apply_bundle` defaults to `dry_run=true`.
- **Safety rails.** Whitelist-gated replacement, an unconditional never-touch blacklist, NPC-marker protection, and (planned) a reverse-diff journal with `revert_last_apply`. Until the journal lands: **work on world copies only.**
