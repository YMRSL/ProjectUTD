from __future__ import annotations

import pytest

from picasso.core.stair_semantics import (
    StairBounds,
    detect_stair_semantics,
)
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


FACING_VECTOR = {
    "north": (0, -1),
    "south": (0, 1),
    "west": (-1, 0),
    "east": (1, 0),
}
OPPOSITE = {
    "north": "south",
    "south": "north",
    "west": "east",
    "east": "west",
}


def _region() -> RegionData:
    return RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=0,
        y_max=15,
        loaded_chunks={(0, 0)},
    )


def _stair(
    facing: str | None = "north",
    *,
    half: str | None = "bottom",
    shape: str | None = "straight",
    block_id: str = "minecraft:oak_stairs",
) -> BlockState:
    properties = {}
    if facing is not None:
        properties["facing"] = facing
    if half is not None:
        properties["half"] = half
    if shape is not None:
        properties["shape"] = shape
    return BlockState.from_id(block_id, properties)


def _roles(candidate) -> dict[BlockPos, str]:
    return {member.pos: member.role for member in candidate.members}


def test_isolated_vanilla_stair_is_low_confidence_seat_candidate() -> None:
    region = _region()
    seat = BlockPos(4, 2, 4)
    region.set(seat, _stair("east"))
    region.set(
        BlockPos(10, 2, 10),
        _stair("west", block_id="example_mod:steel_stairs"),
    )

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    assert candidates[0].kind == "seat_candidate"
    assert candidates[0].anchor == seat
    assert candidates[0].bounds == StairBounds(seat, seat)
    assert _roles(candidates[0]) == {seat: "seat"}
    assert candidates[0].confidence == pytest.approx(0.55)


def test_missing_properties_never_produce_high_confidence_candidate() -> None:
    region = _region()
    seat = BlockPos(4, 2, 4)
    region.set(seat, _stair(None, half=None, shape=None))

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    assert candidates[0].kind == "seat_candidate"
    assert candidates[0].confidence < 0.5


def test_top_straight_horizontal_run_is_decorative_trim() -> None:
    region = _region()
    positions = [BlockPos(x, 3, 6) for x in range(3, 7)]
    for pos in reversed(positions):
        region.set(pos, _stair("south", half="top"))

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    trim = candidates[0]
    assert trim.kind == "decorative_floor_trim"
    assert trim.anchor == positions[0]
    assert trim.member_positions == frozenset(positions)
    assert set(_roles(trim).values()) == {"trim"}
    assert trim.bounds == StairBounds(positions[0], positions[-1])


@pytest.mark.parametrize("facing", ["north", "south", "west", "east"])
def test_functional_staircase_follows_facing_and_includes_underfill(
    facing: str,
) -> None:
    region = _region()
    start = BlockPos(8, 2, 8)
    dx, dz = FACING_VECTOR[facing]
    steps = [start.offset(dx=dx * index, dy=index, dz=dz * index) for index in range(3)]
    underfills = [step.offset(dx=dx, dz=dz) for step in steps]
    for step in steps:
        region.set(step, _stair(facing, half="bottom"))
    for underfill in underfills:
        region.set(underfill, _stair(OPPOSITE[facing], half="top"))

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    staircase = candidates[0]
    assert staircase.kind == "functional_staircase"
    assert staircase.anchor == start
    roles = _roles(staircase)
    assert {pos for pos, role in roles.items() if role == "step"} == set(steps)
    assert {pos for pos, role in roles.items() if role == "underfill"} == set(
        underfills
    )
    assert staircase.confidence == pytest.approx(0.93)


def test_functional_staircase_without_shape_is_detected_at_lower_confidence() -> None:
    region = _region()
    steps = [BlockPos(5, 2 + index, 5 + index) for index in range(3)]
    for step in steps:
        region.set(step, _stair("south", shape=None))

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    assert candidates[0].kind == "functional_staircase"
    assert candidates[0].confidence < 0.8


@pytest.mark.parametrize(
    ("row_a_facing", "row_a_start", "row_step", "row_separation"),
    [
        ("north", BlockPos(4, 2, 6), (1, 0), (0, 1)),
        ("west", BlockPos(6, 2, 4), (0, 1), (1, 0)),
    ],
)
def test_two_back_facing_rows_over_two_layers_are_supermarket_shelf(
    row_a_facing: str,
    row_a_start: BlockPos,
    row_step: tuple[int, int],
    row_separation: tuple[int, int],
) -> None:
    region = _region()
    expected: set[BlockPos] = set()
    for layer in range(2):
        for index in range(4):
            row_a = row_a_start.offset(
                dx=row_step[0] * index,
                dy=layer,
                dz=row_step[1] * index,
            )
            row_b = row_a.offset(dx=row_separation[0], dz=row_separation[1])
            region.set(row_a, _stair(row_a_facing, half="top"))
            region.set(row_b, _stair(OPPOSITE[row_a_facing], half="top"))
            expected.update((row_a, row_b))

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    shelf = candidates[0]
    assert shelf.kind == "supermarket_shelf"
    assert shelf.member_positions == frozenset(expected)
    assert set(_roles(shelf).values()) == {"shelf_row_a", "shelf_row_b"}
    assert shelf.confidence == pytest.approx(0.95)


def test_shelf_priority_prevents_overlapping_trim_candidates() -> None:
    region = _region()
    for y in (2, 3, 4):
        for x in range(4, 7):
            region.set(BlockPos(x, y, 6), _stair("north", half="top"))
            region.set(BlockPos(x, y, 7), _stair("south", half="top"))

    candidates = detect_stair_semantics(region)

    assert [candidate.kind for candidate in candidates] == ["supermarket_shelf"]


def test_shelf_accepts_picasso_fixture_row_order_and_bottom_half() -> None:
    region = _region()
    expected: set[BlockPos] = set()
    for y in (4, 5):
        for x in range(3, 8):
            south_row = BlockPos(x, y, 6)
            north_row = BlockPos(x, y, 7)
            region.set(south_row, _stair("south", half="bottom"))
            region.set(north_row, _stair("north", half="bottom"))
            expected.update((south_row, north_row))

    candidates = detect_stair_semantics(region)

    assert len(candidates) == 1
    assert candidates[0].kind == "supermarket_shelf"
    assert candidates[0].member_positions == frozenset(expected)


def test_top_stair_run_parallel_to_facing_is_not_floor_trim() -> None:
    region = _region()
    for z in range(3, 7):
        region.set(BlockPos(5, 3, z), _stair("north", half="top"))

    assert detect_stair_semantics(region) == ()


def test_invalid_shelf_height_and_short_trim_are_not_misnamed_as_seats() -> None:
    region = _region()
    for y in range(4):
        for x in range(4, 7):
            region.set(BlockPos(x, 2 + y, 6), _stair("north"))
            region.set(BlockPos(x, 2 + y, 7), _stair("south"))
    region.set(BlockPos(10, 2, 10), _stair("east", half="top"))
    region.set(BlockPos(11, 2, 10), _stair("east", half="top"))

    assert detect_stair_semantics(region) == ()


def test_output_is_independent_of_region_insertion_order() -> None:
    states = [
        (BlockPos(x, 2, 5), _stair("south", half="top"))
        for x in range(4, 8)
    ] + [(BlockPos(12, 2, 12), _stair("west"))]
    forward = _region()
    reverse = _region()
    for pos, state in states:
        forward.set(pos, state)
    for pos, state in reversed(states):
        reverse.set(pos, state)

    assert detect_stair_semantics(forward) == detect_stair_semantics(reverse)
