from __future__ import annotations

import copy

import pytest

from picasso.core.vanilla_rail_graph import (
    VANILLA_RAIL_AUDIT_BASIS,
    analyze_vanilla_rail_graph,
)
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


def _region(
    *,
    y_min: int = 0,
    y_max: int = 5,
    read_y_min: int = 0,
    read_y_max: int = 5,
    loaded_chunks: set[tuple[int, int]] | None = None,
) -> RegionData:
    return RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=y_min,
        y_max=y_max,
        read_y_min=read_y_min,
        read_y_max=read_y_max,
        chunks_read=1,
        loaded_chunks={(0, 0)} if loaded_chunks is None else loaded_chunks,
    )


def _rail(
    shape: str,
    block_id: str = "minecraft:rail",
    *,
    powered: str | None = None,
) -> BlockState:
    properties = {"shape": shape}
    if powered is not None:
        properties["powered"] = powered
    return BlockState.from_id(block_id, properties)


def _add(
    region: RegionData,
    pos: BlockPos,
    shape: str,
    block_id: str = "minecraft:rail",
    *,
    powered: str | None = None,
) -> None:
    region.set(pos, _rail(shape, block_id, powered=powered))


@pytest.mark.parametrize(
    ("shape", "first", "second", "shared_port"),
    [
        ("east_west", BlockPos(2, 1, 2), BlockPos(3, 1, 2), {"x2": 5, "y2": 2, "z2": 4}),
        ("north_south", BlockPos(2, 1, 2), BlockPos(2, 1, 3), {"x2": 4, "y2": 2, "z2": 5}),
    ],
)
def test_straight_x_and_z_rails_form_exact_edges(
    shape: str,
    first: BlockPos,
    second: BlockPos,
    shared_port: dict[str, int],
) -> None:
    region = _region()
    _add(region, first, shape)
    _add(region, second, shape)

    result = analyze_vanilla_rail_graph(region)

    assert result["edges"] == [{"a": first.to_dict(), "b": second.to_dict(), "port": shared_port}]
    assert len(result["components"]) == 1
    assert len(result["terminals"]) == 2
    assert result["partial"] is False
    assert result["blocking"] is False


@pytest.mark.parametrize(
    ("shape", "slope", "upper", "upper_shape", "shared_port"),
    [
        (
            "ascending_east",
            BlockPos(2, 1, 2),
            BlockPos(3, 2, 2),
            "east_west",
            {"x2": 5, "y2": 4, "z2": 4},
        ),
        (
            "ascending_west",
            BlockPos(3, 1, 2),
            BlockPos(2, 2, 2),
            "east_west",
            {"x2": 5, "y2": 4, "z2": 4},
        ),
        (
            "ascending_south",
            BlockPos(2, 1, 2),
            BlockPos(2, 2, 3),
            "north_south",
            {"x2": 4, "y2": 4, "z2": 5},
        ),
        (
            "ascending_north",
            BlockPos(2, 1, 3),
            BlockPos(2, 2, 2),
            "north_south",
            {"x2": 4, "y2": 4, "z2": 5},
        ),
    ],
)
def test_each_ascending_high_port_matches_flat_rail_at_y_plus_one(
    shape: str,
    slope: BlockPos,
    upper: BlockPos,
    upper_shape: str,
    shared_port: dict[str, int],
) -> None:
    region = _region()
    _add(region, slope, shape)
    _add(region, upper, upper_shape)

    result = analyze_vanilla_rail_graph(region)

    assert len(result["edges"]) == 1
    assert result["edges"][0]["port"] == shared_port
    slope_node = next(node for node in result["nodes"] if node["pos"] == slope.to_dict())
    assert any(port["y2"] == (slope.y + 1) * 2 for port in slope_node["ports"])
    assert result["blocking"] is False


@pytest.mark.parametrize(
    ("corner", "neighbours"),
    [
        ("south_east", [(BlockPos(3, 1, 2), "east_west"), (BlockPos(2, 1, 3), "north_south")]),
        ("south_west", [(BlockPos(1, 1, 2), "east_west"), (BlockPos(2, 1, 3), "north_south")]),
        ("north_west", [(BlockPos(1, 1, 2), "east_west"), (BlockPos(2, 1, 1), "north_south")]),
        ("north_east", [(BlockPos(3, 1, 2), "east_west"), (BlockPos(2, 1, 1), "north_south")]),
    ],
)
def test_each_corner_connects_only_its_two_proven_ports(
    corner: str,
    neighbours: list[tuple[BlockPos, str]],
) -> None:
    region = _region()
    center = BlockPos(2, 1, 2)
    _add(region, center, corner)
    for pos, shape in neighbours:
        _add(region, pos, shape)

    result = analyze_vanilla_rail_graph(region)

    center_edges = [edge for edge in result["edges"] if center.to_dict() in (edge["a"], edge["b"])]
    assert len(center_edges) == 2
    assert len(result["components"]) == 1
    assert len(result["terminals"]) == 2
    assert result["blocking"] is False


def test_multiple_core_components_are_a_global_ambiguity_blocker() -> None:
    region = _region()
    for x in (1, 2, 8, 9):
        _add(region, BlockPos(x, 1, 2), "east_west")

    result = analyze_vanilla_rail_graph(region)

    assert len(result["components"]) == 2
    assert result["blocking"] is True
    assert "multiple_core_components" in [item["code"] for item in result["blockers"]]


def test_isolated_rail_has_two_legal_terminals_when_absence_is_fully_read() -> None:
    region = _region()
    pos = BlockPos(5, 1, 5)
    _add(region, pos, "east_west")

    result = analyze_vanilla_rail_graph(region)

    assert len(result["nodes"]) == 1
    assert len(result["terminals"]) == 2
    assert {terminal["port"]["direction"] for terminal in result["terminals"]} == {"east", "west"}
    assert result["partial"] is False
    assert result["blocking"] is False


def test_halo_continuation_is_context_not_a_core_node_or_edge() -> None:
    region = _region(loaded_chunks={(0, 0), (1, 0)})
    core = BlockPos(15, 1, 5)
    halo = BlockPos(16, 1, 5)
    _add(region, core, "east_west")
    _add(region, halo, "east_west")
    region.halo_positions.add(halo)

    result = analyze_vanilla_rail_graph(region)

    assert [node["pos"] for node in result["nodes"]] == [core.to_dict()]
    assert result["edges"] == []
    assert len(result["terminals"]) == 1
    assert result["partial"] is True
    assert result["blocking"] is True
    assert "halo_continuation" in [item["code"] for item in result["blockers"]]


def test_non_reciprocal_halo_rail_is_not_mistaken_for_a_terminal() -> None:
    region = _region(loaded_chunks={(0, 0), (1, 0)})
    core = BlockPos(15, 1, 5)
    halo = BlockPos(16, 1, 5)
    _add(region, core, "east_west")
    _add(region, halo, "north_south")
    region.halo_positions.add(halo)

    result = analyze_vanilla_rail_graph(region)

    assert len(result["terminals"]) == 1
    assert result["terminals"][0]["port"]["direction"] == "west"
    assert result["partial"] is True
    assert "incompatible_neighbor_geometry" in [
        item["code"] for item in result["blockers"]
    ]


@pytest.mark.parametrize(
    ("core_y", "core_shape", "halo_y", "halo_shape"),
    [
        (1, "east_west", 2, "east_west"),
        (1, "ascending_east", 1, "east_west"),
    ],
)
def test_flat_or_slope_neighbor_at_wrong_y_blocks_terminal_claim(
    core_y: int,
    core_shape: str,
    halo_y: int,
    halo_shape: str,
) -> None:
    region = _region(loaded_chunks={(0, 0), (1, 0)})
    core = BlockPos(15, core_y, 5)
    halo = BlockPos(16, halo_y, 5)
    _add(region, core, core_shape)
    _add(region, halo, halo_shape)
    region.halo_positions.add(halo)

    result = analyze_vanilla_rail_graph(region)

    assert "incompatible_neighbor_geometry" in [
        item["code"] for item in result["blockers"]
    ]
    assert all(
        terminal["port"]["direction"] != "east"
        for terminal in result["terminals"]
    )
    assert result["partial"] is True


def test_exact_match_still_checks_for_latent_stacked_neighbor_rail() -> None:
    region = _region(loaded_chunks={(0, 0), (1, 0)})
    core = BlockPos(15, 1, 5)
    exact = BlockPos(16, 1, 5)
    stacked = BlockPos(16, 2, 5)
    _add(region, core, "east_west")
    _add(region, exact, "east_west")
    _add(region, stacked, "north_south")
    region.halo_positions.update({exact, stacked})

    result = analyze_vanilla_rail_graph(region)

    codes = [item["code"] for item in result["blockers"]]
    assert "halo_continuation" in codes
    assert "incompatible_neighbor_geometry" in codes
    assert result["edges"] == []
    assert result["partial"] is True


def test_vertical_halo_slope_can_reciprocate_flat_rail_one_block_above() -> None:
    region = _region(loaded_chunks={(0, 0), (1, 0)})
    core = BlockPos(15, 2, 5)
    halo_slope = BlockPos(16, 1, 5)
    _add(region, core, "east_west")
    _add(region, halo_slope, "ascending_west")
    region.halo_positions.add(halo_slope)

    result = analyze_vanilla_rail_graph(region)

    codes = [item["code"] for item in result["blockers"]]
    assert "halo_continuation" in codes
    assert "incompatible_neighbor_geometry" not in codes
    assert len(result["terminals"]) == 1
    assert result["terminals"][0]["port"]["direction"] == "west"


def test_invalid_halo_shape_blocks_terminal_as_unknown_geometry() -> None:
    region = _region(loaded_chunks={(0, 0), (1, 0)})
    core = BlockPos(15, 1, 5)
    halo = BlockPos(16, 1, 5)
    _add(region, core, "east_west")
    _add(region, halo, "invalid_shape")
    region.halo_positions.add(halo)

    result = analyze_vanilla_rail_graph(region)

    assert "invalid_neighbour_rail_state" in [
        item["code"] for item in result["blockers"]
    ]
    assert len(result["terminals"]) == 1
    assert result["partial"] is True


def test_missing_neighbour_chunk_makes_open_boundary_port_partial_and_blocking() -> None:
    region = _region(loaded_chunks={(0, 0)})
    _add(region, BlockPos(15, 1, 5), "east_west")

    result = analyze_vanilla_rail_graph(region)

    assert len(result["terminals"]) == 1
    assert result["partial"] is True
    assert "insufficient_chunk_evidence" in [item["code"] for item in result["blockers"]]


def test_missing_lower_read_y_evidence_prevents_claiming_terminals() -> None:
    region = _region(y_min=0, y_max=3, read_y_min=0, read_y_max=3)
    _add(region, BlockPos(5, 0, 5), "east_west")

    result = analyze_vanilla_rail_graph(region)

    assert result["terminals"] == []
    assert result["partial"] is True
    assert [item["code"] for item in result["blockers"]].count("insufficient_y_evidence") == 2


def test_world_min_filters_impossible_below_world_candidate() -> None:
    region = _region(
        y_min=-64,
        y_max=-60,
        read_y_min=-64,
        read_y_max=-60,
    )
    _add(region, BlockPos(5, -64, 5), "east_west")

    result = analyze_vanilla_rail_graph(region)

    assert len(result["terminals"]) == 2
    assert result["partial"] is False
    assert result["blocking"] is False


def test_ascending_high_port_above_world_max_is_invalid_and_blocking() -> None:
    region = _region(
        y_min=318,
        y_max=319,
        read_y_min=318,
        read_y_max=319,
    )
    _add(region, BlockPos(5, 319, 5), "ascending_east")

    result = analyze_vanilla_rail_graph(region)

    node = result["nodes"][0]
    assert node["ports"] == []
    assert node["partial"] is True
    assert [item["code"] for item in node["diagnostics"]] == [
        "invalid_world_height_geometry"
    ]
    assert result["blocking"] is True


@pytest.mark.parametrize(
    ("block_id", "rail_type", "powered"),
    [
        ("minecraft:powered_rail", "powered_rail", True),
        ("minecraft:detector_rail", "detector_rail", False),
        ("minecraft:activator_rail", "activator_rail", True),
    ],
)
def test_special_rails_keep_type_and_powered_semantics_and_block_downgrade(
    block_id: str,
    rail_type: str,
    powered: bool,
) -> None:
    region = _region()
    _add(region, BlockPos(4, 1, 4), "east_west", block_id, powered=str(powered).lower())

    result = analyze_vanilla_rail_graph(region)

    node = result["nodes"][0]
    assert node["rail_type"] == rail_type
    assert node["powered"] is powered
    assert node["diagnostics"] == []
    assert result["partial"] is False
    assert "special_rail_semantic_loss" in [item["code"] for item in result["blockers"]]


@pytest.mark.parametrize(
    "block_id",
    [
        "minecraft:powered_rail",
        "minecraft:detector_rail",
        "minecraft:activator_rail",
    ],
)
@pytest.mark.parametrize("powered", [False, True])
def test_special_rail_remains_connected_between_normal_rails_but_blocks_conversion(
    block_id: str,
    powered: bool,
) -> None:
    region = _region()
    _add(region, BlockPos(3, 1, 4), "east_west")
    _add(
        region,
        BlockPos(4, 1, 4),
        "east_west",
        block_id,
        powered=str(powered).lower(),
    )
    _add(region, BlockPos(5, 1, 4), "east_west")

    result = analyze_vanilla_rail_graph(region)

    assert len(result["edges"]) == 2
    assert len(result["components"]) == 1
    assert len(result["terminals"]) == 2
    middle = next(node for node in result["nodes"] if node["pos"]["x"] == 4)
    assert middle["powered"] is powered
    assert "special_rail_semantic_loss" in [
        item["code"] for item in result["blockers"]
    ]


@pytest.mark.parametrize(
    ("raw", "parsed", "diagnostics", "blocker"),
    [
        ("false", False, [], None),
        ("true", True, [], "liquid_source_semantic_loss"),
        ("invalid", None, ["invalid_property"], "invalid_property"),
    ],
)
def test_waterlogged_property_is_preserved_and_semantically_guarded(
    raw: str,
    parsed: bool | None,
    diagnostics: list[str],
    blocker: str | None,
) -> None:
    region = _region()
    pos = BlockPos(4, 1, 4)
    region.set(
        pos,
        BlockState.from_id(
            "minecraft:rail",
            {"shape": "east_west", "waterlogged": raw},
        ),
    )

    result = analyze_vanilla_rail_graph(region)

    node = result["nodes"][0]
    assert node["waterlogged"] is parsed
    assert node["properties"]["waterlogged"] == raw
    assert [item["code"] for item in node["diagnostics"]] == diagnostics
    codes = [item["code"] for item in result["blockers"]]
    if blocker is None:
        assert "liquid_source_semantic_loss" not in codes
        assert "invalid_property" not in codes
    else:
        assert blocker in codes


@pytest.mark.parametrize(
    ("state", "expected_codes"),
    [
        (BlockState.from_id("minecraft:rail"), ["missing_property"]),
        (_rail("not_a_shape"), ["invalid_property"]),
        (_rail("east_west", "minecraft:powered_rail"), ["missing_property"]),
        (
            _rail("east_west", "minecraft:detector_rail", powered="yes"),
            ["invalid_property"],
        ),
        (
            _rail("north_east", "minecraft:activator_rail", powered="false"),
            ["invalid_property"],
        ),
    ],
)
def test_missing_and_invalid_properties_are_structured_and_blocking(
    state: BlockState,
    expected_codes: list[str],
) -> None:
    region = _region()
    region.set(BlockPos(4, 1, 4), state)

    result = analyze_vanilla_rail_graph(region)

    node = result["nodes"][0]
    assert [item["code"] for item in node["diagnostics"]] == expected_codes
    assert node["partial"] is True
    assert result["blocking"] is True


def test_results_are_deterministic_core_only_and_input_is_unchanged() -> None:
    region = _region()
    positions = [BlockPos(3, 1, 2), BlockPos(1, 1, 2), BlockPos(2, 1, 2)]
    for pos in reversed(positions):
        _add(region, pos, "east_west")
    blocks_before = copy.deepcopy(region.blocks)
    entities_before = set(region.block_entity_positions)

    first = analyze_vanilla_rail_graph(region)
    second = analyze_vanilla_rail_graph(region)

    assert first == second
    assert [node["pos"] for node in first["nodes"]] == [pos.to_dict() for pos in sorted(positions)]
    assert region.blocks == blocks_before
    assert region.block_entity_positions == entities_before


def test_node_preserves_all_source_properties_without_aliasing_input() -> None:
    region = _region()
    pos = BlockPos(4, 1, 4)
    source_properties = {
        "waterlogged": "true",
        "shape": "east_west",
        "custom_source_evidence": "kept",
    }
    region.set(pos, BlockState.from_id("minecraft:rail", source_properties))

    result = analyze_vanilla_rail_graph(region)

    node_properties = result["nodes"][0]["properties"]
    assert list(node_properties) == sorted(source_properties)
    assert node_properties == source_properties
    assert result["nodes"][0]["diagnostics"] == []
    node_properties["shape"] = "changed_in_output"
    assert region.get(pos).properties["shape"] == "east_west"


def test_empty_core_scan_is_explicitly_blocking() -> None:
    result = analyze_vanilla_rail_graph(_region())

    assert result["nodes"] == []
    assert result["edges"] == []
    assert result["components"] == []
    assert result["terminals"] == []
    assert result["partial"] is False
    assert result["blocking"] is True
    assert [item["code"] for item in result["blockers"]] == ["no_core_rails"]


def test_audit_basis_is_versioned_and_immutable() -> None:
    assert dict(VANILLA_RAIL_AUDIT_BASIS) == {
        "minecraft": "1.21.1",
        "data_version": 3955,
        "world_min_y": -64,
        "world_max_y": 319,
    }
    with pytest.raises(TypeError):
        VANILLA_RAIL_AUDIT_BASIS["data_version"] = 0  # type: ignore[index]


def test_requires_region_data() -> None:
    with pytest.raises(TypeError, match="RegionData"):
        analyze_vanilla_rail_graph(object())  # type: ignore[arg-type]
