from __future__ import annotations

from unittest.mock import patch

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.session import session
from picasso.tools import inspection


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def _tool():
    mcp = FakeMCP()
    inspection.register(mcp)
    return mcp.tools["inspect_volume"]


def test_inspect_volume_reads_covering_chunks_and_returns_rle_evidence() -> None:
    region = RegionData(
        origin_cx=-1,
        origin_cz=0,
        radius_chunks=1,
        y_min=4,
        y_max=8,
        loaded_chunks={(-2, 0), (-1, 0), (0, 0)},
    )
    region.set(
        BlockPos(-1, 5, 2),
        BlockState.from_id(
            "minecraft:oak_stairs",
            {"facing": "north", "half": "bottom", "shape": "straight"},
        ),
    )

    previous_bridge = session.bridge
    previous_cache = session.last_region_cache_hit
    previous_source = session.last_region_source
    try:
        session.bridge = object()
        session.last_region_cache_hit = False
        session.last_region_source = "amulet"
        with patch.object(inspection, "ensure_region", return_value=region) as ensure:
            result = _tool()(-17, 4, 0, 14, 8, 3)
    finally:
        session.bridge = previous_bridge
        session.last_region_cache_hit = previous_cache
        session.last_region_source = previous_source

    assert result["ok"] is True
    ensure.assert_called_once_with(-1, 0, 1, y_min=4, y_max=8)
    assert result["dimensions"] == {"x": 32, "y": 5, "z": 4, "volume": 640}
    assert result["non_air_blocks"] == 1
    assert result["palette"][0]["properties"]["facing"] == "north"
    assert result["complete"] is True
    assert result["encoding"]["air"] == "implicit only when complete=true"


def test_invalid_volume_is_rejected_before_world_read() -> None:
    previous_bridge = session.bridge
    try:
        session.bridge = object()
        with patch.object(inspection, "ensure_region") as ensure:
            result = _tool()(0, 0, 0, 32, 0, 0)
    finally:
        session.bridge = previous_bridge

    assert result["ok"] is False
    assert result["error"] == "volume_limit_exceeded"
    ensure.assert_not_called()


def test_missing_chunk_is_not_silently_encoded_as_air() -> None:
    region = RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=1,
        y_min=0,
        y_max=2,
        loaded_chunks={(0, 0)},
    )
    previous_bridge = session.bridge
    try:
        session.bridge = object()
        with patch.object(inspection, "ensure_region", return_value=region):
            result = _tool()(0, 0, 0, 31, 2, 1)
    finally:
        session.bridge = previous_bridge

    assert result["ok"] is False
    assert result["error"] == "incomplete_read"
    assert result["details"]["missing_chunks"] == [(1, 0)]


def test_inspect_volume_requires_an_active_world() -> None:
    previous_bridge = session.bridge
    try:
        session.bridge = None
        result = _tool()(0, 0, 0, 0, 0, 0)
    finally:
        session.bridge = previous_bridge

    assert result == {
        "ok": False,
        "error": "world_not_set",
        "message": "Call set_world before inspect_volume.",
    }
