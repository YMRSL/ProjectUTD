from __future__ import annotations

import json
from pathlib import Path

import pytest

from picasso.core.fragment_engine import (
    FragmentEngine,
    _rotate_offset,
    _rotate_properties,
)
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.style_engine import StyleEngine, load_pass_registry
from picasso.core.write_choke import WriteChoke
from picasso.models.block import BlockPos, BlockState
from picasso.models.fragment import Fragment
from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = PROJECT_ROOT / "src" / "picasso" / "data"


@pytest.fixture(scope="module")
def fragment_library() -> FragmentLibrary:
    return FragmentLibrary(DATA_ROOT / "fragments")


@pytest.fixture(scope="module")
def pass_registry() -> dict[str, StylePass]:
    return load_pass_registry(DATA_ROOT / "passes")


@pytest.fixture(scope="module")
def style_engine(
    fragment_library: FragmentLibrary,
    pass_registry: dict[str, StylePass],
) -> StyleEngine:
    return StyleEngine(
        pass_registry,
        DATA_ROOT / "safe_blocks.json",
        fragment_library=fragment_library,
    )


def _region(*, y_min: int = -8, y_max: int = 16) -> RegionData:
    return RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=1,
        y_min=y_min,
        y_max=y_max,
    )


def _mark_surface(
    region: RegionData,
    pos: BlockPos,
    state: str,
    surface: str,
    space: str,
) -> None:
    region.set(pos, BlockState.from_id(state))
    region.surface_classes[pos] = surface
    region.space_classes[pos] = space


def _stone_wall() -> RegionData:
    region = _region()
    for x in range(-8, 24):
        for y in range(0, 11):
            _mark_surface(
                region,
                BlockPos(x, y, 5),
                "minecraft:stone_bricks",
                "outer_wall",
                "exterior",
            )
    return region


def _first_nonempty_style_result(
    engine: StyleEngine,
    pass_name: str,
    region: RegionData,
) -> tuple[int, RegionData]:
    for seed in range(64):
        changes = engine.apply(pass_name, region, seed=seed)
        if changes.blocks:
            return seed, changes
    pytest.fail(f"shipped pass {pass_name!r} produced no changes for seeds 0..63")


def test_shipped_window_fragment_does_nothing_to_plain_stone_wall(
    style_engine: StyleEngine,
) -> None:
    changes = style_engine.apply("tlou_window_breach", _stone_wall(), seed=42)

    assert changes.blocks == {}


def test_shipped_window_fragment_only_modifies_verified_pane_on_wall_plane(
    style_engine: StyleEngine,
) -> None:
    region = _stone_wall()
    pane = BlockPos(8, 5, 5)
    region.set(pane, BlockState.from_id("minecraft:glass_pane"))
    _seed, changes = _first_nonempty_style_result(
        style_engine,
        "tlou_window_breach",
        region,
    )

    assert changes.blocks
    assert changes.get(pane) is not None
    assert changes.get(pane).is_air
    assert {pos for pos in changes.blocks if pos.z == 5} == {pane}
    assert all(
        region.get(pos).full_id == "minecraft:stone_bricks"
        for pos in (
            pane.offset(dx=1),
            pane.offset(dx=-1),
            pane.offset(dy=1),
            pane.offset(dy=-1),
        )
    )


def test_shipped_vine_fragment_extends_only_outside_supported_wall(
    style_engine: StyleEngine,
) -> None:
    region = _stone_wall()
    _seed, changes = _first_nonempty_style_result(
        style_engine,
        "tlou_vine_network",
        region,
    )

    assert changes.blocks
    for pos in changes.blocks:
        assert pos.z != 5
        assert region.get(pos) is None
        supports = [
            region.get(pos.offset(dx=1)),
            region.get(pos.offset(dx=-1)),
            region.get(pos.offset(dz=1)),
            region.get(pos.offset(dz=-1)),
        ]
        assert any(state is not None and not state.is_air for state in supports)


def test_shipped_fragment_passes_have_no_embedded_space_filter(
    pass_registry: dict[str, StylePass],
    fragment_library: FragmentLibrary,
) -> None:
    fragment_pass_files = []
    for path in sorted((DATA_ROOT / "passes").glob("*.json")):
        raw = json.loads(path.read_text(encoding="utf-8"))
        if raw.get("type") != "fragment_pass":
            continue
        fragment_pass_files.append(path)
        assert "space_filter" not in raw, path.name
        assert "space_filter" not in pass_registry[path.stem].model_dump()
        referenced = [fragment_library.get(name) for name in raw["fragments"]]
        assert all(fragment is not None for fragment in referenced)
        assert pass_registry[path.stem].destructive is any(
            fragment.destructive for fragment in referenced if fragment is not None
        )

    assert fragment_pass_files
    bundle = json.loads(
        (DATA_ROOT / "bundles" / "tlou_complete.json").read_text(encoding="utf-8")
    )
    assert any(
        "space_filter" in entry
        for bundle_entry in bundle["entries"]
        for entry in bundle_entry["passes"]
    )


def _floor_region() -> RegionData:
    region = _region(y_min=0, y_max=6)
    for x in range(-12, 28):
        for z in range(-12, 28):
            _mark_surface(
                region,
                BlockPos(x, 0, z),
                "minecraft:stone",
                "floor",
                "interior",
            )
    return region


def test_shipped_vehicle_pass_uses_call_space_filter_and_preserves_floor(
    style_engine: StyleEngine,
) -> None:
    region = _floor_region()
    seed, changes = _first_nonempty_style_result(
        style_engine,
        "tlou_abandoned_vehicles",
        region,
    )

    assert changes.blocks
    assert all(pos.y == 1 for pos in changes.blocks)
    filtered = style_engine.apply(
        "tlou_abandoned_vehicles",
        region,
        seed=seed,
        space_filter="exterior",
    )
    assert filtered.blocks == {}


def test_shipped_directional_fragments_are_orientable_and_fit_footprints(
    fragment_library: FragmentLibrary,
) -> None:
    directional_names = {
        "wall_breach",
        "window_overgrown",
        "vine_anchor",
        "abandoned_car_sedan",
        "abandoned_car_van",
        "abandoned_car_jeep",
    }
    vehicle_names = {
        "abandoned_car_sedan",
        "abandoned_car_van",
        "abandoned_car_jeep",
    }

    for name in directional_names:
        fragment = fragment_library.get(name)
        assert fragment is not None and fragment.orientable
        width, depth = (int(value) for value in fragment.footprint.split("x"))
        for rotation in (0, 90, 180, 270):
            offsets = [_rotate_offset(block.offset, rotation) for block in fragment.blocks]
            actual_width = max(x for x, _y, _z in offsets) - min(
                x for x, _y, _z in offsets
            ) + 1
            actual_depth = max(z for _x, _y, z in offsets) - min(
                z for _x, _y, z in offsets
            ) + 1
            declared_width, declared_depth = (
                (width, depth) if rotation % 180 == 0 else (depth, width)
            )
            assert actual_width <= declared_width
            assert actual_depth <= declared_depth

    for name in vehicle_names:
        fragment = fragment_library.get(name)
        assert fragment is not None
        assert all(block.offset[1] >= 1 for block in fragment.blocks)


def _single_floor_anchor() -> tuple[RegionData, BlockPos]:
    region = _region(y_min=0, y_max=6)
    anchor = BlockPos(8, 0, 8)
    _mark_surface(
        region,
        anchor,
        "minecraft:stone",
        "floor",
        "interior",
    )
    return region, anchor


def _sedan_probe_pass() -> dict:
    return {
        "name": "sedan_rotation_probe",
        "type": "fragment_pass",
        "fragments": ["abandoned_car_sedan"],
        "anchor_surface": "floor",
        "density": 1.0,
        "min_spacing": 0,
        "only_safe_anchor_blocks": True,
    }


def test_exact_shipped_sedan_rotates_in_all_four_floor_directions(
    fragment_library: FragmentLibrary,
) -> None:
    engine = FragmentEngine(
        fragment_library,
        safe_replaceable={"minecraft:stone"},
        structural_never_touch=set(),
    )
    directions: set[tuple[int, int]] = set()

    for seed in range(64):
        region, anchor = _single_floor_anchor()
        changes = engine.apply(_sedan_probe_pass(), region, seed=seed)
        part_two = next(
            pos
            for pos, state in changes.blocks.items()
            if state.full_id == "doomsday_decoration:discardgreycar_2"
        )
        directions.add((part_two.x - anchor.x, part_two.z - anchor.z))

    assert directions == {(1, 0), (-1, 0), (0, 1), (0, -1)}


def test_exact_shipped_sedan_checks_clearance_over_full_rotated_footprint(
    fragment_library: FragmentLibrary,
) -> None:
    engine = FragmentEngine(
        fragment_library,
        safe_replaceable={"minecraft:stone"},
        structural_never_touch=set(),
    )
    region, _anchor = _single_floor_anchor()
    first = engine.apply(_sedan_probe_pass(), region, seed=0)
    part_two = next(
        pos
        for pos, state in first.blocks.items()
        if state.full_id == "doomsday_decoration:discardgreycar_2"
    )
    region.set(
        BlockPos(part_two.x, 2, part_two.z),
        BlockState.from_id("minecraft:stone"),
    )

    blocked = engine.apply(_sedan_probe_pass(), region, seed=0)

    assert blocked.blocks == {}


def test_exact_shipped_vehicle_is_dropped_as_one_atomic_instance(
    fragment_library: FragmentLibrary,
) -> None:
    engine = FragmentEngine(
        fragment_library,
        safe_replaceable={"minecraft:stone"},
        structural_never_touch=set(),
    )
    region, _anchor = _single_floor_anchor()
    raw_changes = engine.apply(_sedan_probe_pass(), region, seed=0)
    protected = next(
        pos
        for pos, state in raw_changes.blocks.items()
        if state.full_id == "doomsday_decoration:discardgreycar_2"
    )

    assert raw_changes.atomic_groups == [set(raw_changes.blocks)]
    result = WriteChoke(
        safe_replaceable={"minecraft:stone"},
        structural_never_touch=set(),
        marker_positions={protected},
    ).validate(region, raw_changes)

    assert result.changes.blocks == {}
    assert any(
        entry["reason"] == "marker_protected"
        and entry["pos"] == protected.to_dict()
        for entry in result.skipped
    )
    assert {
        (entry["pos"]["x"], entry["pos"]["y"], entry["pos"]["z"])
        for entry in result.skipped
        if entry["reason"] == "atomic_group_failed"
    } == {(pos.x, pos.y, pos.z) for pos in raw_changes.blocks if pos != protected}


def test_fragment_atomic_groups_only_include_positions_actually_emitted() -> None:
    class _InlineLibrary:
        def __init__(self, fragment: Fragment) -> None:
            self.fragment = fragment

        def get(self, name: str) -> Fragment | None:
            return self.fragment if name == self.fragment.name else None

    def apply(blocks: list[dict], footprint: str) -> tuple[RegionData, BlockPos]:
        fragment = Fragment.model_validate(
            {
                "name": "atomic_probe",
                "description": "Atomic grouping probe.",
                "anchor_surface": "floor",
                "footprint": footprint,
                "requires_clear_above": False,
                "blocks": blocks,
            }
        )
        region, anchor = _single_floor_anchor()
        changes = FragmentEngine(
            _InlineLibrary(fragment),  # type: ignore[arg-type]
            safe_replaceable={"minecraft:stone"},
            structural_never_touch=set(),
        ).apply(
            {
                "name": "atomic_probe_pass",
                "fragments": [fragment.name],
                "anchor_surface": "floor",
                "density": 1.0,
            },
            region,
            seed=0,
        )
        return changes, anchor

    single, anchor = apply(
        [{"offset": [0, 0, 0], "block": "minecraft:stone"}],
        "1x1",
    )
    assert set(single.blocks) == {anchor}
    assert single.atomic_groups == []

    partial, anchor = apply(
        [
            {"offset": [0, 0, 0], "block": "minecraft:stone"},
            {"offset": [1, 0, 0], "block": "minecraft:dirt"},
            {
                "offset": [2, 0, 0],
                "block": "minecraft:gravel",
                "probability": 0.0,
            },
        ],
        "3x1",
    )
    emitted = {anchor, anchor.offset(dx=1)}
    assert set(partial.blocks) == emitted
    assert partial.atomic_groups == [emitted]
    assert anchor.offset(dx=2) not in partial.atomic_groups[0]


def test_fragment_rotation_updates_horizontal_properties() -> None:
    properties = {"facing": "south", "axis": "x", "waterlogged": "false"}

    assert _rotate_properties(properties, 90) == {
        "facing": "west",
        "axis": "z",
        "waterlogged": "false",
    }
    assert _rotate_properties(properties, 180) == {
        "facing": "north",
        "axis": "x",
        "waterlogged": "false",
    }
    assert _rotate_properties(properties, 270) == {
        "facing": "east",
        "axis": "z",
        "waterlogged": "false",
    }
    assert properties == {"facing": "south", "axis": "x", "waterlogged": "false"}


def _wall_breach_region(
    outside_normal: tuple[int, int],
) -> tuple[RegionData, BlockPos]:
    region = _region(y_min=0, y_max=8)
    anchor = BlockPos(8, 4, 8)
    normal_x, normal_z = outside_normal

    if normal_z:
        for x in range(anchor.x - 1, anchor.x + 2):
            for y in (anchor.y, anchor.y + 1):
                region.set(
                    BlockPos(x, y, anchor.z),
                    BlockState.from_id("minecraft:stone_bricks"),
                )
        region.set(
            anchor.offset(dz=-normal_z),
            BlockState.from_id("minecraft:stone_bricks"),
        )
        for x in range(anchor.x - 1, anchor.x + 2):
            region.set(
                BlockPos(x, anchor.y - 2, anchor.z + normal_z),
                BlockState.from_id("minecraft:stone"),
            )
    else:
        for z in range(anchor.z - 1, anchor.z + 2):
            for y in (anchor.y, anchor.y + 1):
                region.set(
                    BlockPos(anchor.x, y, z),
                    BlockState.from_id("minecraft:stone_bricks"),
                )
        region.set(
            anchor.offset(dx=-normal_x),
            BlockState.from_id("minecraft:stone_bricks"),
        )
        for z in range(anchor.z - 1, anchor.z + 2):
            region.set(
                BlockPos(anchor.x + normal_x, anchor.y - 2, z),
                BlockState.from_id("minecraft:stone"),
            )

    region.surface_classes[anchor] = "outer_wall"
    region.space_classes[anchor] = "exterior"
    return region, anchor


@pytest.mark.parametrize("outside_normal", [(0, 1), (-1, 0)])
def test_exact_shipped_wall_breach_rotates_rubble_outside_with_support(
    fragment_library: FragmentLibrary,
    outside_normal: tuple[int, int],
) -> None:
    engine = FragmentEngine(
        fragment_library,
        safe_replaceable={"minecraft:stone_bricks"},
        structural_never_touch=set(),
    )
    region, anchor = _wall_breach_region(outside_normal)
    probe_pass = {
        "name": "wall_breach_rotation_probe",
        "type": "fragment_pass",
        "fragments": ["wall_breach"],
        "anchor_surface": "outer_wall",
        "density": 1.0,
        "min_spacing": 0,
        "only_safe_anchor_blocks": True,
    }

    changes = None
    for seed in range(64):
        candidate = engine.apply(probe_pass, region, seed=seed)
        if any(not state.is_air for state in candidate.blocks.values()):
            changes = candidate
            break
    assert changes is not None

    normal_x, normal_z = outside_normal
    rubble = [pos for pos, state in changes.blocks.items() if not state.is_air]
    carved = [pos for pos, state in changes.blocks.items() if state.is_air]
    assert rubble
    assert carved
    for pos in rubble:
        assert (pos.x - anchor.x) * normal_x + (pos.z - anchor.z) * normal_z == 1
        support = region.get(pos.offset(dy=-1))
        assert support is not None and not support.is_air
    for pos in carved:
        assert (pos.x - anchor.x) * normal_x + (pos.z - anchor.z) * normal_z == 0
