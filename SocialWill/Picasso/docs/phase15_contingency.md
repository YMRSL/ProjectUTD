# Phase 1.5 Contingency Plan — If the Modded-Block Round-Trip Fails

> Companion to `docs/implementation_order.md` Phase 1.5. Tracks `ARCHITECTURE.md` v0.4.4.
>
> **Why this document exists before the spike runs:** the `doomsday:*` round-trip (Amulet write → save → load in 1.21.1 + DD mod → renders correctly) is the project's only "believed to work, never verified" foundation assumption (§4.2). Every fallback below was designed *before* knowing the failure mode, so that a failure produces a decision in hours, not a redesign under pressure. If the spike **passes**, this document is archived — set `PICASSO_MODDED_WRITE_VERIFIED=true` and never read it again.

---

## 1. Failure taxonomy — diagnose before choosing a fallback

The spike (place simple DD block / DD block with properties / vanilla control → save → in-game verify) can fail in four distinct ways. **Identify which one before reaching for a fallback** — they have different cheapest fixes:

| # | Failure mode | Symptom in game | Likely cause | Cheapest response |
|---|---|---|---|---|
| F-A | **Block vanishes / becomes air** | position is empty | Amulet dropped the unknown-namespace entry on save | Fallback B or C |
| F-B | **Block becomes fallback stone / "update block"** | wrong block, world otherwise fine | palette entry written but not resolvable by the game (id mismatch, missing block-state NBT shape) | Investigate first (§2) — often a fixable serialization detail, not a dead end |
| F-C | **Properties lost, base block correct** | DD chair renders but faces north always | property dict not round-tripping through Amulet's universal format | Partial-pass (§3): ship v1 without property-dependent DD placements; orientation of DD furniture deferred |
| F-D | **Chunk corruption / game crash on load** | crash log, chunk resets | malformed NBT written into the region file | **Stop all writes immediately**; Fallback B only; file Amulet issue upstream |

**Spike protocol addition (do this even on success):** record the exact amulet-core version, the DD mod version, and the world's DataVersion in this file. A pass is only evidence for that triple — pin `amulet-core` in `pyproject.toml` at the verified version.

---

## 2. F-B investigation checklist (before declaring failure)

F-B is the most likely failure and the most likely to be *fixable*. Amulet stores unknown blocks as opaque universal entries keyed by namespaced id + properties; the game needs the palette entry to match the mod's registered block state exactly. Check in order:

1. **Id case/format**: DD registry ids are lowercase snake_case; verify the catalog's `id` strings match the mod's `.json` blockstate filenames exactly (a single mismatch → fallback stone for that block only).
2. **Property completeness**: some modded blocks require *all* their block-state properties present in the palette entry (vanilla tolerates omission, mods may not). Fix: `place_block` fills default properties from a per-block property-defaults table extracted from the mod jar (a data file, generated once — the DD mod's `assets/*/blockstates/*.json` enumerates valid property sets).
3. **DataVersion mismatch**: writing with a DataVersion the mod's runtime doesn't expect can silently invalidate modded palette entries while vanilla survives translation. Fix: open the level with the save's own version, never force-upgrade.
4. **One-block minimal repro**: hand-edit a region file with NBTExplorer to contain the same palette entry; if the hand-made entry loads, the diff between hand-made and Amulet-written NBT is the bug — usually fixable in the bridge.

Only if all four are exhausted does F-B escalate to a real fallback.

---

## 3. Partial-pass mode (F-C): ship without DD properties

If base blocks round-trip but properties don't:

- `pattern_replace` mappings and Room `placements` that use DD blocks **without** property requirements proceed unchanged.
- DD placements that need `facing` (chairs, desks) are **restricted to yaw-0 canonical orientation** in v1; the orientation system (fragment_system §4) skips property remapping for `doomsday:*` namespace with a per-placement log line.
- Catalog entries gain `"requires_properties": true` where applicable (data enrichment, one pass over the DD blockstates); tools warn when placing such an entry in partial-pass mode.
- Visual cost: DD furniture all faces one way per room. Acceptable for v1; revisit with Fallback C machinery if it reads badly.

---

## 4. Fallback B — Structure-block staging (full write path replacement)

**Mechanism:** Picasso never writes DD blocks into region files. Instead:

1. All DD-block placements in a diff are extracted into **staging structures**: vanilla `structure_block` NBT files (`.nbt`, the game's own template format — vanilla-parseable, mod-block-friendly since the game itself deserializes them with the mod loaded).
2. Picasso writes the *vanilla* part of the diff via Amulet as usual, plus one `structure_block` (load mode, with its `.nbt` in `<world>/generated/picasso/structures/`) + one redstone block per staging cluster at a buried marker position.
3. On next world load **with the mod present**, the structure blocks self-trigger and stamp the DD content; a cleanup pass (or the structure itself) removes the trigger blocks.

| Property | Assessment |
|---|---|
| Write correctness | Delegated to the game's own deserializer — the most robust possible path for modded content |
| Preview ≡ apply | **Weakened**: DD blocks land only after a game load; `preview` remains correct for the *intent*, but the world file between apply and first load shows staging blocks. Journal records the intended final state; `verify_staging` (new small tool) reports unstamped staging clusters |
| Choke point | Unchanged — validation runs on the intended diff before staging extraction |
| Determinism | Unchanged (staging extraction is a pure function of the diff) |
| Cost | Medium: staging extractor + NBT template writer (~1 new core module), `structure_void` handling in templates, cleanup logic, one new tool. No spec changes above the bridge layer |
| Risk | Structure block volume limit (48³ in 1.21) → cluster splitting; trigger reliability needs its own mini-spike |

**This is the default fallback.** It preserves every architectural contract except the "world file is final immediately after apply" property, and that weakening is explicitly journaled.

---

## 5. Fallback C — External stamper (last resort)

If even structure-block NBT proves unreliable for DD content: Picasso emits a **placement manifest** (`<world>/picasso_pending/<ts>.json`: list of `{pos, block_id, properties}`), and a tiny **server-side stamper** (same codebase home as the §6 build-log plugin — the project already commits to a server plugin for player-activity sensing, so this adds a receive path to an existing component, not a new component) applies it with real game registry access on next server start, then archives the manifest.

- Write correctness: perfect (game-native placement). Latency: worst (needs a server start). 
- Picasso-side cost: trivial (manifest writer). Plugin-side cost: moderate.
- Preview ≡ apply weakening: same shape as Fallback B, longer window.
- **Choose C over B only if** B's trigger mechanism fails its mini-spike, or the 48³ splitting proves buggy in practice.

---

## 6. Decision rule (pre-committed)

```
Spike fails →
  F-D: halt writes, Fallback B, upstream issue.
  F-B: §2 checklist (budget: one day). Fixed → PASS. Not fixed → treat as F-A.
  F-C: partial-pass mode (§3) for v1; log Fallback-B upgrade as v2 item.
  F-A: Fallback B. Mini-spike its trigger path (one session). Trigger unreliable → Fallback C.
Any fallback adopted → ARCHITECTURE.md §4.2 gating note is replaced by a pointer
to this file + the chosen fallback; PICASSO_MODDED_WRITE_VERIFIED stays false and
the modded-write gate (tool_specs.md) stays active until the fallback's own
verification passes.
```

The catalog, fragments, passes, patterns, and all v0.5 specs are **unaffected in shape** by any branch above — the entire question is confined to the bridge/write layer, which is exactly why §4.2 confined Amulet there (A-series decision paying off, if it does).
