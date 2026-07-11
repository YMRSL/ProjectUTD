# Chess — SocialWill Wargame Layer

**Chess** is the wargame/faction layer of the SocialWill project: a self-authored NeoForge mod
(MC 1.21.1) that lays a Hearts-of-Iron-style abstract board over the Minecraft world — one
board cell per chunk — and settles factions, economies, battles, and infection every
10 seconds, whether or not anyone has the chunks loaded. Its fullscreen strategic map replaces
the Xaero map as the players' navigation and operating surface.

> Orientation only — never authoritative. Read **`ARCHITECTURE.md`** first; it is the
> authority and indexes everything else (§14).

## The two worlds

Minecraft simulates blocks; Chess simulates *meaning* — territory, manpower, organization,
supply. The system is a consistency protocol between them: counters manifest as real entities
in loaded chunks (and player kills write back as real casualties); buildings hang their numbers
on real core blocks; infected cells get physically painted by the infection mods under Chess's
command; wars leave physical scars via Picasso work orders during maintenance windows.

## Status

✅ **v0.1.4 (2026-07-09) — three adversarial rounds + one agent round closed;
implementation-ready.** No code exists yet. Rulings D1–D51 in `docs/REVISION_LOG.md` §0.x;
round 1 (A1–A30 + SC1–SC8) in §A; round 2 self-attack (B1–B18) in §B; round 3 (C1–C21,
jar-verified FTB reality) in §C; post-close agent round (R1–R21) in §D. **108 findings
adjudicated, zero rebuttals outstanding.** Currency art lives in `assets/currency/`.
**Start implementation from `docs/HANDOFF.md`, phase P0 (external-surface spikes).**

## Document map

```
ARCHITECTURE.md                  ← authoritative; read first
docs/
├── counters_and_combat.md       counters, battles, entity mirror, occupation cores
├── ai_factions_and_strains.md   cheap rule-driven AI factions; 3 infection strains
├── economy_and_buildings.md     resources, manpower, 9 buildings, exchange, policies
├── governance.md                blocs, 3 polities, tickets, votes, diplomacy
├── board_and_fog.md             cells, terrain, sensing, sectors, fog of war
├── map_interface.md             the fullscreen strategic map, protocol, Xaero import
├── event_log.md                 chess_log/ — the outbound data contract
├── integrations.md              Alignment Ledger, Picasso bridge, FTB, cross-layer asks
└── REVISION_LOG.md              decision record (D-series rulings, review rounds)
```

External normative contracts (frozen, in the Picasso repo): `wargame_interface.md`
(work orders, receipts, round clock) and `player_activity_pipeline.md` (recorder plugin,
clock doctrine).

## SocialWill context

| Layer | System | Runs |
|---|---|---|
| World | **Picasso** (MCP server + Amulet) | maintenance windows |
| Wargame | **Chess** (this repo) | live server |
| Narrative | **MiroFish** (future) | reads both layers' logs |
