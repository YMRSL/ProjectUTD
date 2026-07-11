from __future__ import annotations

import pytest

from picasso.core.volume_inspector import (
    VolumeInspectionError,
    inspect_region_volume,
    validate_volume_request,
)
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData


def _region() -> RegionData:
    return RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=0,
        y_max=23,
        loaded_chunks={(0, 0)},
    )


def _flatten_runs(result: dict) -> list[tuple[int, int, dict]]:
    return [
        (layer["y"], row["z"], run)
        for layer in result["layers"]
        for row in layer["rows"]
        for run in row["runs"]
    ]


def test_palette_preserves_properties_and_air_gaps_split_runs() -> None:
    region = _region()
    for x in (1, 2, 3, 5):
        region.set(BlockPos(x, 4, 2), BlockState.from_id("minecraft:stone"))
    region.set(
        BlockPos(6, 4, 2),
        BlockState.from_id(
            "minecraft:oak_stairs",
            {"shape": "straight", "facing": "east", "half": "top"},
        ),
    )
    region.set(BlockPos(7, 4, 2), AIR)

    result = inspect_region_volume(region, 0, 4, 2, 7, 4, 2)

    assert result["bounds"] == {
        "min": {"x": 0, "y": 4, "z": 2},
        "max": {"x": 7, "y": 4, "z": 2},
    }
    assert result["dimensions"] == {"x": 8, "y": 1, "z": 1, "volume": 8}
    assert result["non_air_blocks"] == 5
    assert result["run_count"] == 3
    assert result["complete"] is True
    assert result["truncated"] is False
    assert result["omitted_runs"] == 0

    palette_by_block = {entry["block"]: entry for entry in result["palette"]}
    assert palette_by_block["minecraft:stone"]["count"] == 4
    assert palette_by_block["minecraft:stone"]["properties"] == {}
    assert palette_by_block["minecraft:oak_stairs"]["properties"] == {
        "facing": "east",
        "half": "top",
        "shape": "straight",
    }
    assert palette_by_block["minecraft:oak_stairs"]["count"] == 1

    runs = _flatten_runs(result)
    stone_index = palette_by_block["minecraft:stone"]["index"]
    stair_index = palette_by_block["minecraft:oak_stairs"]["index"]
    assert runs == [
        (
            4,
            2,
            {"x_min": 1, "x_max": 3, "length": 3, "palette_index": stone_index},
        ),
        (
            4,
            2,
            {"x_min": 5, "x_max": 5, "length": 1, "palette_index": stone_index},
        ),
        (
            4,
            2,
            {"x_min": 6, "x_max": 6, "length": 1, "palette_index": stair_index},
        ),
    ]


def test_palette_and_rle_are_independent_of_insertion_order() -> None:
    states = [
        (BlockPos(2, 2, 2), BlockState.from_id("minecraft:dirt")),
        (BlockPos(3, 2, 2), BlockState.from_id("minecraft:dirt")),
        (
            BlockPos(4, 2, 2),
            BlockState.from_id("minecraft:oak_log", {"axis": "x"}),
        ),
        (BlockPos(1, 3, 1), BlockState.from_id("minecraft:stone")),
    ]
    forward = _region()
    reverse = _region()
    for pos, state in states:
        forward.set(pos, state)
    for pos, state in reversed(states):
        reverse.set(pos, state)

    first = inspect_region_volume(forward, 0, 0, 0, 7, 5, 7)
    second = inspect_region_volume(reverse, 0, 0, 0, 7, 5, 7)

    assert first == second


def test_surface_counts_are_complete_and_sorted() -> None:
    region = _region()
    floor_a = BlockPos(1, 2, 1)
    floor_b = BlockPos(2, 2, 1)
    wall = BlockPos(3, 2, 1)
    for pos in (floor_a, floor_b, wall):
        region.set(pos, BlockState.from_id("minecraft:stone"))
    region.surface_classes[floor_a] = "floor"
    region.surface_classes[floor_b] = "floor"
    region.surface_classes[wall] = "outer_wall"
    region.surface_classes[BlockPos(9, 2, 9)] = "rooftop"

    result = inspect_region_volume(region, 1, 2, 1, 3, 2, 1)

    assert result["surface_counts"] == {"floor": 2, "outer_wall": 1}


@pytest.mark.parametrize(
    "bounds",
    [
        (-1, 0, 0, 0, 0, 0),
        (0, -1, 0, 0, 0, 0),
        (15, 0, 15, 16, 0, 15),
        (0, 23, 0, 0, 24, 0),
    ],
)
def test_bounds_outside_target_are_rejected(bounds: tuple[int, ...]) -> None:
    with pytest.raises(VolumeInspectionError) as caught:
        inspect_region_volume(_region(), *bounds)

    assert caught.value.code == "bounds_outside_target"
    assert caught.value.to_dict()["error"] == "bounds_outside_target"


def test_invalid_or_non_integer_bounds_are_structured_errors() -> None:
    with pytest.raises(VolumeInspectionError) as reversed_bounds:
        inspect_region_volume(_region(), 2, 0, 0, 1, 0, 0)
    assert reversed_bounds.value.code == "invalid_bounds"

    with pytest.raises(VolumeInspectionError) as non_integer:
        inspect_region_volume(_region(), 0.0, 0, 0, 1, 0, 0)  # type: ignore[arg-type]
    assert non_integer.value.code == "invalid_bounds"


@pytest.mark.parametrize(
    ("bounds", "violation"),
    [
        ((0, 0, 0, 32, 0, 0), "x_span"),
        ((0, 0, 0, 0, 24, 0), "y_span"),
        ((0, 0, 0, 31, 23, 32), "z_span"),
    ],
)
def test_span_and_volume_limits_are_structured(
    bounds: tuple[int, ...],
    violation: str,
) -> None:
    region = RegionData(
        origin_cx=1,
        origin_cz=1,
        radius_chunks=2,
        y_min=0,
        y_max=40,
        loaded_chunks={(0, 0)},
    )

    with pytest.raises(VolumeInspectionError) as caught:
        inspect_region_volume(region, *bounds)

    assert caught.value.code == "volume_limit_exceeded"
    assert violation in caught.value.details["violations"]
    if violation == "z_span":
        assert "volume" in caught.value.details["violations"]
        assert caught.value.details["dimensions"]["volume"] > 24_576


def test_maximum_allowed_volume_is_inclusive() -> None:
    region = RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=1,
        y_min=0,
        y_max=23,
        loaded_chunks={(0, 0), (0, 1), (1, 0), (1, 1)},
    )

    result = inspect_region_volume(region, 0, 0, 0, 31, 23, 31)

    assert result["dimensions"]["volume"] == 24_576
    assert result["non_air_blocks"] == 0
    assert result["layers"] == []
    assert result["complete"] is True


def test_validate_volume_request_can_run_before_a_region_read() -> None:
    assert validate_volume_request(0, 4, 8, 31, 23, 15, max_runs=12) == {
        "x": 32,
        "y": 20,
        "z": 8,
        "volume": 5120,
    }


def test_bounds_covering_an_unloaded_chunk_fail_closed() -> None:
    region = RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=1,
        y_min=0,
        y_max=5,
        loaded_chunks={(0, 0)},
    )

    with pytest.raises(VolumeInspectionError) as caught:
        inspect_region_volume(region, 0, 0, 0, 16, 5, 15)

    assert caught.value.code == "incomplete_read"
    assert caught.value.details["missing_chunks"] == [(1, 0)]


def test_run_truncation_is_deterministic_and_never_silent() -> None:
    region = _region()
    for x in range(10):
        block = "minecraft:stone" if x % 2 == 0 else "minecraft:dirt"
        region.set(BlockPos(x, 2, 2), BlockState.from_id(block))

    result = inspect_region_volume(region, 0, 2, 2, 9, 2, 2, max_runs=3)

    assert result["non_air_blocks"] == 10
    assert sum(entry["count"] for entry in result["palette"]) == 10
    assert result["run_count"] == 3
    assert result["complete"] is False
    assert result["truncated"] is True
    assert result["omitted_runs"] == 7
    assert [run[2]["x_min"] for run in _flatten_runs(result)] == [0, 1, 2]


def test_zero_run_budget_reports_omissions_but_empty_air_is_complete() -> None:
    populated = _region()
    populated.set(BlockPos(1, 1, 1), BlockState.from_id("minecraft:stone"))
    truncated = inspect_region_volume(populated, 0, 0, 0, 2, 2, 2, max_runs=0)
    assert truncated["run_count"] == 0
    assert truncated["omitted_runs"] == 1
    assert truncated["complete"] is False

    empty = inspect_region_volume(_region(), 0, 0, 0, 2, 2, 2, max_runs=0)
    assert empty["run_count"] == 0
    assert empty["omitted_runs"] == 0
    assert empty["complete"] is True


def test_block_entities_are_sorted_and_halo_metadata_is_excluded() -> None:
    region = _region()
    first = BlockPos(4, 3, 2)
    second = BlockPos(2, 2, 4)
    halo = BlockPos(3, 2, 4)
    for pos in (first, second, halo):
        region.set(pos, BlockState.from_id("minecraft:chest"))
        region.block_entity_positions.add(pos)
    region.halo_positions.add(halo)

    result = inspect_region_volume(region, 0, 0, 0, 7, 5, 7)

    assert result["non_air_blocks"] == 2
    assert result["block_entity_positions"] == [second.to_dict(), first.to_dict()]
    assert halo.to_dict() not in result["block_entity_positions"]


def test_invalid_max_runs_is_rejected() -> None:
    with pytest.raises(VolumeInspectionError) as negative:
        inspect_region_volume(_region(), 0, 0, 0, 0, 0, 0, max_runs=-1)
    assert negative.value.code == "invalid_max_runs"

    with pytest.raises(VolumeInspectionError) as boolean:
        inspect_region_volume(_region(), 0, 0, 0, 0, 0, 0, max_runs=True)
    assert boolean.value.code == "invalid_max_runs"

    with pytest.raises(VolumeInspectionError) as excessive:
        inspect_region_volume(_region(), 0, 0, 0, 0, 0, 0, max_runs=4097)
    assert excessive.value.code == "invalid_max_runs"
    assert excessive.value.details["max_allowed"] == 4096
