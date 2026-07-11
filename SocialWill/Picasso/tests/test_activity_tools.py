from __future__ import annotations

import json
import tempfile
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from picasso.tools import activity


FIXTURE_DIR = Path(__file__).parent / "fixtures" / "build_log"


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def runtime_config(log_dir: Path | None, **overrides):
    values = {
        "build_log_dir": log_dir,
        "site_join_dist": 16,
        "site_join_gap_min": 90,
        "site_min_events": 8,
    }
    values.update(overrides)
    return SimpleNamespace(**values)


def registered_tools() -> dict[str, object]:
    mcp = FakeMCP()
    activity.register(mcp)
    return mcp.tools


def contains_raw_events(value) -> bool:
    if isinstance(value, dict):
        return "events" in value or any(contains_raw_events(item) for item in value.values())
    if isinstance(value, list):
        return any(contains_raw_events(item) for item in value)
    return False


def test_registers_exact_activity_tool_names() -> None:
    tools = registered_tools()

    assert set(tools) == {"query_player_activity", "get_activity_site"}


def test_query_player_activity_returns_sites_stats_and_no_raw_events() -> None:
    tools = registered_tools()
    with patch.object(activity, "config", runtime_config(FIXTURE_DIR)):
        result = tools["query_player_activity"](
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            min_events=4,
        )

    assert result["ok"] is True
    assert result["events_total"] == 5
    assert result["log_coverage"] == {"from": "2026-07-08", "to": "2026-07-08"}
    assert len(result["sites"]) == 1
    assert result["sites"][0]["event_count"] == 4
    assert result["query"]["min_events"] == 4
    assert contains_raw_events(result) is False
    assert len(json.dumps(result)) < 10_000


def test_query_uses_config_default_floor_and_accepts_player_bounds_filters() -> None:
    tools = registered_tools()
    with patch.object(activity, "config", runtime_config(FIXTURE_DIR)):
        default_result = tools["query_player_activity"](
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
        )
        filtered = tools["query_player_activity"](
            since="2026-07-07T00:00:00Z",
            until="2026-07-07T23:59:00Z",
            player="Steve",
            bounds={
                "x_min": 10,
                "x_max": 10,
                "y_min": 70,
                "y_max": 70,
                "z_min": 0,
                "z_max": 0,
            },
            min_events=1,
        )

    assert default_result["ok"] is True
    assert default_result["sites"] == []
    assert default_result["query"]["min_events"] == 8
    assert filtered["ok"] is True
    assert filtered["events_total"] == 1
    assert len(filtered["sites"]) == 1
    assert filtered["sites"][0]["players"] == {"Steve": {"places": 1, "breaks": 0}}
    assert filtered["query"]["bounds"]["x_min"] == 10


def test_existing_empty_directory_is_a_successful_empty_query() -> None:
    tools = registered_tools()
    with tempfile.TemporaryDirectory() as tmp:
        with patch.object(activity, "config", runtime_config(Path(tmp))):
            result = tools["query_player_activity"](since="2026-07-01T00:00:00Z")

    assert result["ok"] is True
    assert result["sites"] == []
    assert result["events_total"] == 0
    assert result["log_coverage"] == {"from": None, "to": None}


def test_unconfigured_and_missing_log_directories_map_to_structured_error() -> None:
    tools = registered_tools()
    with patch.object(activity, "config", runtime_config(None)):
        unconfigured = tools["query_player_activity"](since="2026-07-01T00:00:00Z")
    with tempfile.TemporaryDirectory() as tmp:
        missing_path = Path(tmp) / "missing"
        with patch.object(activity, "config", runtime_config(missing_path)):
            missing = tools["query_player_activity"](since="2026-07-01T00:00:00Z")

    for result in (unconfigured, missing):
        assert result["ok"] is False
        assert result["error"] == "build_log_not_configured"


def test_invalid_inputs_have_specific_structured_errors() -> None:
    tools = registered_tools()
    query = tools["query_player_activity"]
    detail = tools["get_activity_site"]
    with patch.object(activity, "config", runtime_config(FIXTURE_DIR)):
        cases = [
            (query(since="2026-07-08T00:00:00"), "invalid_since"),
            (
                query(
                    since="2026-07-08T01:00:00Z",
                    until="2026-07-08T00:00:00Z",
                ),
                "invalid_time_range",
            ),
            (query(since="2026-07-08T00:00:00Z", bounds={"x_min": 0}), "invalid_bounds"),
            (query(since="2026-07-08T00:00:00Z", min_events=0), "invalid_min_events"),
            (query(since="2026-07-08T00:00:00Z", player=""), "invalid_player"),
            (detail(site_id="", since="2026-07-08T00:00:00Z"), "invalid_site_id"),
        ]

    assert [(result["ok"], result["error"]) for result, _ in cases] == [
        (False, expected) for _, expected in cases
    ]


def test_get_activity_site_returns_detail_stats_and_site_not_found() -> None:
    tools = registered_tools()
    with patch.object(activity, "config", runtime_config(FIXTURE_DIR)):
        query = tools["query_player_activity"](
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            min_events=4,
        )
        site_id = query["sites"][0]["site_id"]
        detail = tools["get_activity_site"](
            site_id=site_id,
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            min_events=4,
        )
        missing = tools["get_activity_site"](
            site_id="site_missing",
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            min_events=1,
        )

    assert detail["ok"] is True
    assert detail["site_id"] == site_id
    assert detail["timeline"] == [
        {"hour": "2026-07-08T00:00:00Z", "event_count": 2},
        {"hour": "2026-07-08T01:00:00Z", "event_count": 2},
    ]
    assert detail["palette_full"][0] == {"block": "minecraft:stone", "count": 2}
    assert detail["query_stats"]["events_total"] == 5
    assert detail["query_stats"]["log_coverage"] == {
        "from": "2026-07-08",
        "to": "2026-07-08",
    }
    assert contains_raw_events(detail) is False
    assert missing["ok"] is False
    assert missing["error"] == "site_not_found"


def test_get_activity_site_replays_query_player_and_bounds_filters() -> None:
    tools = registered_tools()
    bounds = {
        "x_min": 101,
        "x_max": 101,
        "y_min": 71,
        "y_max": 71,
        "z_min": 0,
        "z_max": 0,
    }
    with patch.object(activity, "config", runtime_config(FIXTURE_DIR)):
        query = tools["query_player_activity"](
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            player="Alex",
            bounds=bounds,
            min_events=1,
        )
        site_id = query["sites"][0]["site_id"]
        detail = tools["get_activity_site"](
            site_id=site_id,
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            min_events=1,
            player="Alex",
            bounds=bounds,
        )
        without_filters = tools["get_activity_site"](
            site_id=site_id,
            since="2026-07-08T00:00:00Z",
            until="2026-07-08T23:59:00Z",
            min_events=1,
        )

    assert detail["ok"] is True
    assert detail["site_id"] == site_id
    assert detail["query"]["player"] == "Alex"
    assert detail["query"]["bounds"] == bounds
    assert detail["query_stats"]["events_total"] == 1
    assert without_filters["ok"] is False
    assert without_filters["error"] == "site_not_found"
