from __future__ import annotations

from picasso import server


EXPECTED_TOOLS = {
    "set_world",
    "close_world",
    "read_region",
    "analyze_region",
    "inspect_volume",
    "query_catalog",
    "list_passes",
    "preview_pass",
    "apply_pass",
    "create_pass",
    "list_bundles",
    "apply_bundle",
    "list_journal_entries",
    "inspect_journal_entry",
    "revert_last_apply",
    "query_player_activity",
    "get_activity_site",
    "list_fragments",
    "create_fragment",
    "create_bundle",
    "place_npc_marker",
    "describe_capabilities",
}


def test_register_tools_exposes_the_complete_public_surface(monkeypatch) -> None:
    isolated = server.SingleFlightFastMCP("picasso-registration-test")
    monkeypatch.setattr(server, "mcp", isolated)

    server.register_tools()

    assert set(isolated._tool_manager._tools) == EXPECTED_TOOLS
