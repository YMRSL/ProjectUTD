from __future__ import annotations

import copy

import pytest

from picasso.core.storey_semantics import detect_storey_level_candidates
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


STONE = BlockState.from_id("minecraft:stone")


def _region() -> RegionData:
    return RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=0,
        y_max=15,
        chunks_read=1,
    )


def _add_plane(
    region: RegionData,
    *,
    y: int,
    xs: range,
    zs: range,
    alternate_classes: bool = False,
) -> set[BlockPos]:
    positions = {BlockPos(x, y, z) for x in xs for z in zs}
    for index, pos in enumerate(sorted(positions)):
        region.blocks[pos] = STONE
        region.surface_classes[pos] = (
            "floor" if alternate_classes and index % 2 else "rooftop"
        )
    return positions


def test_two_large_platforms_return_two_ordered_storey_level_candidates() -> None:
    region = _region()
    _add_plane(region, y=9, xs=range(8, 15), zs=range(8, 13), alternate_classes=True)
    _add_plane(region, y=2, xs=range(1, 7), zs=range(1, 6))
    blocks_before = copy.copy(region.blocks)
    classes_before = copy.copy(region.surface_classes)

    candidates = detect_storey_level_candidates(region)

    assert [candidate["type"] for candidate in candidates] == [
        "storey_level_candidate",
        "storey_level_candidate",
    ]
    assert [candidate["y"] for candidate in candidates] == [2, 9]
    assert [candidate["area"] for candidate in candidates] == [30, 35]
    assert candidates[0]["bounds"] == {
        "x_min": 1,
        "x_max": 6,
        "y_min": 2,
        "y_max": 2,
        "z_min": 1,
        "z_max": 5,
    }
    assert candidates[1]["bounds"] == {
        "x_min": 8,
        "x_max": 14,
        "y_min": 9,
        "y_max": 9,
        "z_min": 8,
        "z_max": 12,
    }
    assert all(0.0 < candidate["confidence"] <= 1.0 for candidate in candidates)
    assert region.blocks == blocks_before
    assert region.surface_classes == classes_before


def test_table_top_and_stair_treads_stay_below_connected_area_threshold() -> None:
    region = _region()
    _add_plane(region, y=6, xs=range(2, 5), zs=range(2, 5))
    for y, width in ((1, 5), (2, 4), (3, 3), (4, 2)):
        _add_plane(region, y=y, xs=range(9, 9 + width), zs=range(1, 2))

    assert detect_storey_level_candidates(region) == []


def test_halo_positions_do_not_complete_a_subthreshold_target_plane() -> None:
    region = _region()
    _add_plane(region, y=4, xs=range(0, 4), zs=range(0, 5))
    region.halo_positions.update(BlockPos(3, 4, z) for z in range(0, 5))

    # There are 20 classified and connected positions, but only 15 targets.
    assert detect_storey_level_candidates(region, min_area=16) == []


@pytest.mark.parametrize("min_area", [0, -1, True, 1.5])
def test_min_area_must_be_a_positive_integer(min_area: object) -> None:
    with pytest.raises(ValueError, match="positive integer"):
        detect_storey_level_candidates(_region(), min_area=min_area)  # type: ignore[arg-type]
