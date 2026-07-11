from __future__ import annotations

from picasso.session import session


def register(mcp) -> None:
    @mcp.tool()
    def describe_capabilities() -> dict:
        """Return Picasso's complete public tool list and safe testing workflow."""
        tools = [
            {
                "name": "set_world",
                "writes_save": False,
                "purpose": "Connect a Minecraft save directory and acquire Picasso's advisory world lock.",
            },
            {
                "name": "close_world",
                "writes_save": False,
                "purpose": "Close the active save and release Picasso's advisory world lock.",
            },
            {
                "name": "read_region",
                "writes_save": False,
                "purpose": "Read chunk-region non-air block summary and bounds; supports y_min/y_max.",
            },
            {
                "name": "analyze_region",
                "writes_save": False,
                "purpose": "Summarize surfaces, top blocks, patterns, and conservative local semantic candidates; supports y_min/y_max.",
            },
            {
                "name": "inspect_volume",
                "writes_save": False,
                "purpose": "Expose bounded Agent-readable block-state evidence through a palette and Y/Z/X RLE view.",
            },
            {
                "name": "query_catalog",
                "writes_save": False,
                "purpose": "Search the decoration block catalog.",
            },
            {
                "name": "list_passes",
                "writes_save": False,
                "purpose": "List registered style passes.",
            },
            {
                "name": "preview_pass",
                "writes_save": False,
                "purpose": "Dry-run one pass and return would-change samples plus timings; supports y_min/y_max.",
            },
            {
                "name": "apply_pass",
                "writes_save": True,
                "purpose": "Write one pass to the active save after safety filtering.",
            },
            {
                "name": "create_pass",
                "writes_save": False,
                "purpose": "Create a pass definition file in Picasso data.",
            },
            {
                "name": "list_bundles",
                "writes_save": False,
                "purpose": "List registered style bundles.",
            },
            {
                "name": "apply_bundle",
                "writes_save": "only when dry_run=false",
                "purpose": "Dry-run or apply a style bundle; supports y_min/y_max.",
            },
            {
                "name": "list_fragments",
                "writes_save": False,
                "purpose": "List registered fragment definitions.",
            },
            {
                "name": "create_fragment",
                "writes_save": False,
                "purpose": "Create a fragment definition file in Picasso data.",
            },
            {
                "name": "create_bundle",
                "writes_save": False,
                "purpose": "Create a bundle definition file in Picasso data.",
            },
            {
                "name": "place_npc_marker",
                "writes_save": True,
                "purpose": "Place a structure-void NPC marker and metadata file.",
            },
            {
                "name": "list_journal_entries",
                "writes_save": False,
                "purpose": "List durable reverse-diff entries for the active world.",
            },
            {
                "name": "inspect_journal_entry",
                "writes_save": False,
                "purpose": "Inspect a paginated reverse diff and its transaction status.",
            },
            {
                "name": "revert_last_apply",
                "writes_save": True,
                "purpose": "Revert the newest apply while preserving third-party conflicts.",
            },
            {
                "name": "query_player_activity",
                "writes_save": False,
                "purpose": "Cluster retained player build events into activity sites.",
            },
            {
                "name": "get_activity_site",
                "writes_save": False,
                "purpose": "Inspect one query-scoped activity site without exposing raw events.",
            },
            {
                "name": "describe_capabilities",
                "writes_save": False,
                "purpose": "Return this complete tool list and safe test workflow.",
            },
        ]
        return {
            "ok": True,
            "world_connected": session.bridge is not None,
            "world_path": str(session.world_path) if session.world_path else None,
            "world_lock": (
                {
                    "lock_path": str(session.world_lock.lock_path),
                    "world_key": session.world_lock.world_key,
                }
                if session.world_lock
                else None
            ),
            "journal_status": session.journal_status,
            "noise_backend": session.noise_backend,
            "tools": tools,
            "prompts": [
                {
                    "name": "interpret_world_structure",
                    "purpose": "Guide a read-only Agent review of bounded candidate and voxel evidence.",
                }
            ],
            "safe_workflow": [
                "set_world",
                "read_region, optionally with y_min/y_max to avoid underground bulk",
                "analyze_region with the same cx/cz/radius/y_min/y_max to hit cache",
                "for semantic review, inspect_volume only around relevant candidates and subdivide truncated views",
                "preview_pass or apply_bundle(dry_run=true) with the same window",
                "review would_change, sample_changes, placements_skipped, timings",
                "after a write, inspect its journal entry; use revert_last_apply if needed",
                "close_world when testing is done",
                "write only on a copied save after dry-run passes",
            ],
            "never_in_first_round": [
                "apply_pass",
                "apply_bundle(dry_run=false)",
                "place_npc_marker",
            ],
        }
