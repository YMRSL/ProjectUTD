# MiroFish — SocialWill Narrative Layer

**MiroFish** is the third layer of the SocialWill project, and it is **two rooms in one
house** (ruling D1): the **Stage** — the original MiroFish agent chat network, an OASIS
dual-platform social simulation (Twitter = public sphere, Reddit = private sphere, D3) —
and the **Desk** — the screenwriter. Picasso edits the physical world; Chess settles the
factual world (territory, war, economy); the Stage rehearses what people *would say* about
those facts; the Desk turns facts (cited) plus rehearsal material (referenced) into meaning
delivered in-world: named characters, dialogue, NPC quests, rumors, memory.

Founding creed: **facts from the layers, imagination from the Stage, fiction from the Desk —
and the fiction must cite its facts** (M-TRUTH + M-IMAG). Simulation output is reference
material for the screenwriter, never a fact source and never an agent supply (D2). From day
zero the Desk also co-authors story settings with the owner and births the cast as AI agents
with prior memories (D4); its NPC products serve both Chess and Picasso-manifested works (D5).

> Orientation only — never authoritative. Read **`ARCHITECTURE.md`** first. Status:
> 🚧 **v0.1 draft 2, checkpoint-0 PARTIAL** — rulings D1–D5 landed 2026-07-09
> (`docs/REVISION_LOG.md` §0); eleven checkpoint-0 v2 questions await the owner
> (`ARCHITECTURE.md` §9).

## The three layers

| Layer | System | Runs | Product |
|---|---|---|---|
| World | **Picasso** (MCP server + Amulet) | maintenance windows | physical world edits |
| Wargame | **Chess** (NeoForge mod, v0.1.3 frozen) | live server | facts: territory, battles, economy |
| Narrative | **MiroFish** (this repo) | Desk: window cadence · Stage: owner-triggered batches · zero live dependency | fiction: characters, dialogue, quests — plus the rehearsal archive feeding it |

## Document map

```
ARCHITECTURE.md              ← authoritative; read first (v0.1 draft 2)
docs/
└── REVISION_LOG.md          decision record (D1–D5 landed; checkpoint-0 v2 pending; reviews append)
   (planned, gated:          simulation_stage.md [after spike one] · genesis_and_casting.md ·
                             beats_and_threads.md · characters_and_content.md ·
                             content_pack_format.md [after spike zero] · integrations.md)
```

External normative contracts (frozen, other repos — never re-specified here):
`../Chess/docs/event_log.md` (fact feed + visibility guardrail) · `../Chess/docs/integrations.md`
§8 (asks 7/8) · `../Picasso/docs/wargame_interface.md` (marker/`mark` vocabulary, journal
`on_behalf_of`) · `../Picasso/docs/player_activity_pipeline.md` §7 (clock doctrine).

Non-normative lineage (pre-refoundation, mined 2026-07-09 — see `ARCHITECTURE.md` §11):
`D:\ModDevelop\[Important]Mirrofish\MiroFish-main` (the vendorable sim kernel) ·
`…\SocialWill\开发蓝图` (contracts + WP-2xx work packages + WS2 CNPC catalog) ·
`…\SocialWill\三系统分开设计的系统架构` (old T-series specs; background only).

## Inherited working rules

Docs in English, review discussion in Chinese (Chess D4 practice) · doc-first, code-later ·
all numbers PLACEHOLDER config (D-BALANCE) · adversarial review rounds land in
`docs/REVISION_LOG.md` · **and the lesson Chess round 3 bought with a jar inspection: no
external-surface assumption becomes mechanism text before a spike verifies it** — spike zero
is the CustomNPCs script layer, spike one is the vendored OASIS surface.
