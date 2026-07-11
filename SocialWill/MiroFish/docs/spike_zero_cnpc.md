# Spike Zero — CNPC Script Layer Dissection Report

> **Status: draft 1 (2026-07-10) — two-track source dissection complete; residual in-game
> verification items in §10.** Sources: mod source `D:\ModDevelop\Unofficial-CNPC-YMRPacked\
> customnpcs-1.20.1-git` (Java, Gradle, unofficial fork) + live sample pack
> `D:\MC\ProjectUTD\CNPCScripts` (targets the **1.21.1 NeoForge** unofficial build actually
> running on the dev client — note the version delta, §10.1). This report unblocks
> `content_pack_format.md` drafting; freezing still waits on §10.

## 1. Headline verdict

The CNPC surface is **file-first**: dialogs, quests, and NPC definitions (clones) are plain
JSON/SNBT files under the world save, loaded from disk, with console commands for reload and
spawn. An external generator (the Desk's pack installer) can ship content by **writing files
and issuing commands** — no GUI anywhere in the write path. The WS2 catalog's core claims
(external-JSON drop-in dialogs/quests with reload, faction-point gating, Nashorn scripting)
are **confirmed against source**; the "Scene DSL" claim maps to a *separate* VN-dialog
datapack mod that coexists on the client (§7).

## 2. Disk layout (verified in source)

| Content | Path (under `world/customnpcs/`) | Format | Write pattern |
|---|---|---|---|
| Dialogs | `dialogs/{category}/{id}.json` | JSON (NBT→JSON via `NBTJsonUtil`) | `{id}.json_new` → atomic rename (mod's own pattern; ours too) |
| Quests | `quests/{category}/{id}.json` | JSON, same codec | same |
| NPC clones | `clones/{tab}/{name}.json` | SNBT-flavored JSON (§5) | same; filename = NPC name, spaces allowed, do not rename |
| Factions | `factions.dat` | **binary compressed NBT, load-once** | **read-only for MiroFish** (§9.4) |
| Player data | `playerdata/*.dat` | binary NBT | read-only (quest/dialog progress lives here) |

Evidence: `DialogController.java:54,67,307` · `QuestController.java:46,59,235` ·
`ServerCloneController.java:59,99,118,149` · `NBTJsonUtil.java` · `FactionController.java:54`.

**Commands** (`CmdNoppes` family): `/noppes dialog reload` (recreates controller + syncs all
clients; `CmdDialog.java:33-36`), `/noppes quest reload`, `/noppes clone spawn <npc> <tab>
[pos] [name]` (reads clone file from disk at call time — new files spawnable without
restart), `/noppes clone list|add|remove|grid`, `/noppes dialog show <players> <id> <npc>`.

## 3. Dialog model (the tree format)

`Dialog.java:24-200`, `DialogOption.java:10-114`:

```jsonc
// dialogs/mirofish/1001.json — minimal working skeleton
{
  "DialogId": 1001,
  "DialogTitle": "Greeting — Old Chen, post-battle",
  "DialogText": "你来了，{player}。北墙的事听说了吗……",   // {player} placeholder supported
  "DialogQuest": -1,          // quest id offered/linked by this node (-1 none)
  "DialogCommand": "",        // command run when dialog read
  "Options": [
    { "OptionSlot": 0, "Option": {
        "Title": "听说了。守卫队怎么样了？",
        "Dialog": 1002,        // ← tree wiring: option → next dialog id
        "OptionType": 1,       // 0 QUIT · 1 DIALOG (chain) · 2 DISABLED · 3 ROLE · 4 COMMAND
        "DialogColor": 14671839,
        "DialogCommand": "" } },
    { "OptionSlot": 1, "Option": { "Title": "先不说这个。", "Dialog": -1, "OptionType": 0 } }
  ],
  // + full Availability block (§4), FactionOptions (rep changes on read), sound, mail
  "ModRev": 5
}
```

A dialog **tree** is a set of these files chained by `Options[].Option.Dialog` ids. An NPC
binds entry dialogs via its interaction slots (`NPCDialogOptions`, slots 0–9) — slot
availability itself passes the same Availability system, so one NPC offers different entry
dialogs as the story state changes.

## 4. Availability — the thread-gating compile target

`Availability.java:21-574`. Every dialog, dialog option, and quest carries this condition
block; all conditions AND together:

- **Dialog prerequisites** ×4: After/Before a given dialog id was read;
- **Quest prerequisites** ×4: Always/After/Before/**Active/NotActive/Completed/CanStart**;
- **Faction stance** ×2: Is/IsNot Friendly/Neutral/Hostile toward faction id;
- **Daytime** (Always/Day/Night), **scoreboard** ×2 (=, >, <), **min player level**.

This is exactly a *story-state gate*: **Thread state machines (§5.3 of ARCHITECTURE) compile
to quest chains + availability conditions.** A thread step = a quest (`nextQuestid` chains
them) + dialogs whose availability keys off that quest's Active/Completed state. No custom
runtime needed — the mod already evaluates the state machine per player.

## 5. Clone (NPC definition) format — generation rules

Live sample: `CNPCScripts\clones\1\*.json` (six Fungal clones). Field groups: identity
(`Name`, entity `id`), display (`Model`/`AnimFile`/`Texture` — namespace refs into a
resourcepack for Gecko NPCs; plain skins for normal NPCs), stats (`MaxHealth`, `Accuracy`,
`AttackStrenght` [sic — keep the typo], `attributes[]` incl. modded ones like
`tacz:tacz.bullet_resistance`), AI flags, `FactionID` (int), `NPCDialogOptions[]` (dialog
binding), interact/kill/random `Lines`, loot (`NpcInv` + `DropChance`), and `Scripts[]`
(inline ECMAScript, `ScriptLanguage: "ECMAScript"`, `ScriptEnabled: 1b`).

**Serialization rules for machine generation (load-bearing):**

1. This is **SNBT-flavored JSON**, not strict JSON: booleans `0b/1b`, floats `0.6f`, doubles
   `0.12d`, longs `40252L`, shorts `20s`. A strict JSON serializer will corrupt it.
2. Script string fields contain **embedded newlines** (the pack README warns: if a formatter
   escapes them to `\n`, the NPC goes `script errored`). Emit with an SNBT-aware writer;
   never run generic JSON pretty-printers over clone files. Round-trip test in §10.2.
3. Clone files must NOT carry instance state — the mod strips `Pos`, `UUID*`, `StartPos*`,
   `MovingPathNew`, `Riding` on save (`ServerCloneController.cleanTags`, :162-208). Our
   generator emits them clean from the start.
4. Changed clone files do **not** retro-update already-spawned NPCs — republish = `/kill`
   tagged instance + respawn (pack installer's job, journaled).

## 5b. Known-good script dialect — **normative reference** (owner-supplied)

`clones/1/Fungal_Infected.json` is the **golden reference** for generated NPC scripts: its
dialect was established by trial and error on the live 1.21.1 client (owner attribution:
Opus 4.8 sessions — other syntaxes repeatedly caused *silent* failures: the script loads
but functions never fire). **Generated scripts must stay inside this dialect**; deviations
are compile-gate rejections until individually proven in-game.

Rules observed in the golden file:

1. **ES5 strictly**: `var` + `function` declarations only — no `let`/`const`, no arrow
   functions, no template literals, no classes, no `for…of`. Strings built by `+` concat.
2. **Every hook body is one enclosing try/catch** reporting through a tiny helper —
   `function _err(npc,where,e){try{npc.say('[ERR '+where+'] '+e);}catch(x){}}` — so errors
   surface in-game instead of failing silently.
3. **Every risky sub-feature gets its own micro try/catch with a safe default** (e.g.
   `blockHardness` → 1.5 on any failure; `blacklisted` → `true` = don't dig). One failing
   API never kills the whole tick.
4. **State discipline**: `getStoreddata()` (persistent) vs `getTempdata()` (volatile);
   reads always `has()`-guarded with a default:
   `var last=td.has('patAt')?td.get('patAt'):-9999;`.
5. **Throttle everything** on `world.getTotalTime()` deltas; early-`return` fast paths.
6. **Never fight native AI** — the golden file's own comment: native CNPC aggro owns
   targeting (faction + AggroRange + LOS); scripts read `getAttackTarget()` and react,
   never `setAttackTarget`.
7. **Java interop is fenced**: raw-MC access (`npc.getMCEntity()`, `w.getMCLevel()`,
   `getMCBlockPos`, `getPersistentData()`) only inside try/catch; cross-mod signaling via
   entity tags + persistent NBT (the KubeJS bridge pattern, §8).
8. Hooks confirmed firing in the golden file: `tick`, `meleeAttack`, `damaged`, `died`
   (plus `init`/`interact`/`timer` across the wider sample library). Chinese comments OK.

**Generation strategy (rule): template + patch, never from scratch.** Start from a
game-saved template clone; patch only narrative-relevant fields (Name, Title,
skin/Texture, FactionID, dialog bindings, Lines, stats, Scripts); keep everything else
verbatim. The golden file carries tolerated instance-state fields (`Motion`, `Rotation`,
`TotalTicksAlive`, `Brain`, `NeoForgeData`) — a from-scratch file that guesses wrong here
is exactly the silent-failure path. Normal narrative NPCs drop the Gecko fields
(`Model`/`AnimFile`, `NpcModelData.EntityName`) in favor of standard skins (owner's skin
pool, later); Gecko stays for owner-curated ambient monsters (Fungal 感染者).

## 6. Quest model — collectibles' native vehicle

`Quest.java:32+`; types (`QuestType.java`): **ITEM(0)** · DIALOG(1) · KILL(2) · LOCATION(3)
· AREA_KILL(4) · MANUAL(5). Repeat: NONE/REPEATABLE/MCDAILY/MCWEEKLY/RLDAILY/RLWEEKLY.
Completion: **Npc** (turn-in at named NPC) or **Instant**. Rewards: exp, ≤9 item stacks
(optionally randomized), faction points, command, mail. `nextQuestid` chains.

**QuestItem** (`QuestItem.java:20-163`) is the collect-and-submit quest D19 wants: up to 3
required stacks, `leaveItems` (keep vs consume on turn-in), `ignoreDamage`, `ignoreNBT`
(critical for modded loot matching — Superb/Sable/Golem items carry NBT). **QuestLocation**
gives discovery quests ("reach the ruined chapel") — pairs naturally with collectible
placement sites. **QuestDialog** completes on reading dialogs (lore-trail quests).

## 7. Two dialog systems coexist on the client

1. **CNPC native dialogs** (§3) — quest-integrated, availability-gated, per-NPC bound. The
   default lane for narrative content.
2. **VN-dialog datapack mod** (tutorial in `CnpcScript\文档\[1.20.1专项学习]VN对话框教程.txt`):
   separate mod, datapack JSON under `data/dialog/dialogs/`, visual-novel presentation
   (portraits, backgrounds, per-entry commands, `/dialog show <id>`, `/dialog reload`).
   No quest integration. Candidate for **cinematic beats** (window-opening story recaps,
   major-event scenes); not the default lane.

## 8. Scripting engine & hooks (for behavior NPCs and records)

JSR-223 (`ScriptController.java:54-120`): Nashorn JS (primary, ES5), Lua, others.
NPC hooks (`EnumScriptType.java`): `init, tick, interact, dialog, dialogOption, dialogClose,
damaged, died, meleeAttack, target, kill, …`. **Player** hooks include `questStart,
questCompleted, questTurnIn, factionUpdate, pickup` — the candidate lane for **ask-8
completion records**: a player-script on `questTurnIn` can emit a record (command →
scoreboard/file) that Chess-side ingestion reads. Scripts **cannot create** quests/dialogs
at runtime — content stays data-driven (matches our compile-time model exactly).

The Fungal pack demonstrates the advanced end (Gecko models via self-compiled
`CNPC-Gecko-Addon` + resourcepack namespace, KubeJS marker-entity bridge for gunshot
lures). For MiroFish v1: **narrative NPCs use standard skins** (owner will supply a skin
pool later); Fungal-class Gecko NPCs stay owner-curated ambient monsters (普通感染者), not
Desk-authored content.

## 9. What this settles for MiroFish

1. **Q2's shape (pack-vs-scripts) resolves as "both, layered"**: the content pack stays a
   MiroFish-internal bundle (reviewable, versioned, atomic); a **pack installer** step
   compiles it to native CNPC files (`dialogs/mirofish/…`, `quests/mirofish/…`,
   `clones/<mirofish tab>/…`) + a command sequence (`/noppes dialog reload`, `/noppes quest
   reload`, `/noppes clone spawn`, `/kill` for retirements) executed at window time via the
   server console/RCON. Final D-ruling once §10 residuals pass.
2. **Id-space registry required**: dialog/quest ids are bare integers; category folders
   don't namespace them. MiroFish claims a dedicated range (e.g. ≥100000, PLACEHOLDER) and
   its own category (`mirofish`) + clone tab; the compiler tracks allocations in
   `mirofish_log/` and never reuses ids. Hand-authored content stays below the range.
3. **Thread compilation** (§4): thread steps → quest chains + availability conditions;
   dialog trees per §3.
4. **Factions are read-only**: `factions.dat` is binary + load-once. Factions are
   pre-created by hand (owner/GUI) and referenced by `FactionID`; MiroFish maps Chess
   faction slugs → CNPC faction ints in config, and drives *player standing* only through
   quest/dialog `FactionOptions`.
5. **D19 collectibles**: submit quests = QuestItem with `ignoreNBT` as needed; whitelist
   enforcement (D19) sits in our compiler, not the mod.

## 10. Residual verification (before `content_pack_format.md` freezes)

1. **1.21.1 parity** — source dissected is the 1.20.1 git repo; the live client runs the
   1.21.1 NeoForge unofficial build. Verify on the dev client: storage paths, `/noppes …
   reload|spawn` availability, Quest/Dialog field names (drop one hand-made file of each
   kind into a test world; confirm load).
2. **Script-string newline round-trip** — write a clone with a multi-line script via our
   SNBT writer (dialect per §5b; embedded real newlines confirmed in the golden file);
   confirm the NPC runs un-errored; diff against a game-saved clone.
3. **Modded-item quest matching** — one QuestItem quest requiring a modded item (e.g. a
   Superb part) with/without `ignoreNBT`; confirm turn-in works.
4. **`/noppes quest reload` semantics** in the port (dialog reload is confirmed in source;
   quest side partially explored).
5. **Records lane probe** — a player-script `questTurnIn` hook emitting a scoreboard/file
   record; confirms the ask-8 consumption path end-to-end.

*Report ends. Residuals are in-game tests on the dev client — cheap, no code to write
beyond one hand-made file of each kind.*
