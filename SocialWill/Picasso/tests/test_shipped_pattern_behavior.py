from __future__ import annotations

from pathlib import Path

import pytest

from picasso.core.pattern_matcher import PatternMatcher
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SHIPPED_PATTERNS = PROJECT_ROOT / "src" / "picasso" / "data" / "patterns"


def _table_matches(base: str, top: str | None):
    region = RegionData()
    base_pos = BlockPos(4, 64, 4)
    top_pos = base_pos.offset(dy=1)
    region.set(base_pos, BlockState.from_id(base))
    if top is not None:
        region.set(top_pos, BlockState.from_id(top))

    matches = [
        match
        for match in PatternMatcher(SHIPPED_PATTERNS).find_matches(region)
        if match.pattern_name == "table"
    ]
    return matches, base_pos, top_pos


@pytest.mark.parametrize(
    "top",
    ["minecraft:white_carpet", "minecraft:oak_pressure_plate"],
)
def test_shipped_table_accepts_carpet_or_pressure_plate_top(top: str) -> None:
    matches, base_pos, top_pos = _table_matches("minecraft:oak_fence", top)

    assert len(matches) == 1
    assert matches[0].anchor_pos == base_pos
    assert set(matches[0].blocks) == {base_pos, top_pos}


@pytest.mark.parametrize(
    "top",
    ["minecraft:white_carpet", "minecraft:oak_pressure_plate"],
)
def test_shipped_table_rejects_fence_gate_base(top: str) -> None:
    matches, _base_pos, _top_pos = _table_matches("minecraft:oak_fence_gate", top)

    assert matches == []


def test_shipped_table_rejects_modded_fence_base() -> None:
    matches, _base_pos, _top_pos = _table_matches(
        "example_mod:oak_fence",
        "minecraft:white_carpet",
    )

    assert matches == []


def test_shipped_table_requires_a_top() -> None:
    matches, _base_pos, _top_pos = _table_matches("minecraft:oak_fence", None)

    assert matches == []
