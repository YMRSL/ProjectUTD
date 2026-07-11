from __future__ import annotations

from unittest.mock import patch

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.session import session
from picasso.tools import analysis


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def _stair(facing: str, half: str = "bottom") -> BlockState:
    return BlockState.from_id(
        "minecraft:oak_stairs",
        {"facing": facing, "half": half, "shape": "straight"},
    )


def test_analyze_region_reports_compact_local_semantic_candidates() -> None:
    region = RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=0,
        y_max=15,
        chunks_read=1,
    )
    for x in range(4):
        for z in range(4):
            pos = BlockPos(x, 0, z)
            region.set(pos, BlockState.from_id("minecraft:stone"))
            region.surface_classes[pos] = "floor"

    steps = [BlockPos(8, 2 + index, 8 - index) for index in range(3)]
    underfills = [step.offset(dz=-1) for step in steps]
    for step in steps:
        region.set(step, _stair("north"))
    for underfill in underfills:
        region.set(underfill, _stair("south", "top"))

    previous_matcher = session.pattern_matcher
    previous_noise = session.noise_backend
    try:
        session.pattern_matcher = None
        session.noise_backend = "fallback"
        mcp = FakeMCP()
        analysis.register(mcp)
        with patch.object(analysis, "ensure_region", return_value=region):
            result = mcp.tools["analyze_region"](0, 0, 0)
    finally:
        session.pattern_matcher = previous_matcher
        session.noise_backend = previous_noise

    assert result["ok"] is True
    semantics = result["local_semantics"]
    assert semantics["scope"] == "candidate_only"
    assert semantics["stair_assembly_count"] == 1
    assert semantics["stair_assemblies"] == [
        {
            "kind": "functional_staircase",
            "confidence": 0.93,
            "anchor": {"x": 8, "y": 2, "z": 8},
            "bounds": {
                "min": {"x": 8, "y": 2, "z": 5},
                "max": {"x": 8, "y": 4, "z": 8},
            },
            "member_count": 6,
            "role_counts": {"step": 3, "underfill": 3},
        }
    ]
    assert semantics["storey_level_candidate_count"] == 1
    assert semantics["storey_level_candidates"][0]["y"] == 0
    assert semantics["storey_level_candidates"][0]["area"] == 16
    assert "1 stair assemblies" in result["summary"]
