from __future__ import annotations

import copy

import pytest

from picasso.core.create_track_semantics import (
    CREATE_TRACK_ID,
    EXCLUDED_TRACKS_MOUNT_ID,
    GENERIC_CROSSING_ID,
    RAILWAYS_REQUIRED_TRACK_IDS,
    SUPPORTED_CREATE_TRAIN_TRACK_IDS,
    TRACK_SEMANTICS_AUDIT_BASIS,
    analyze_create_tracks,
)
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


def _region(*, radius_chunks: int = 0) -> RegionData:
    return RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=radius_chunks,
        y_min=0,
        y_max=31,
        chunks_read=(radius_chunks * 2 + 1) ** 2,
    )


def _track(
    block_id: str = CREATE_TRACK_ID,
    *,
    shape: str = "xo",
    turn: str | None = "false",
    waterlogged: str | None = "false",
) -> BlockState:
    properties: dict[str, str] = {"shape": shape}
    if turn is not None:
        properties["turn"] = turn
    if waterlogged is not None:
        properties["waterlogged"] = waterlogged
    return BlockState.from_id(block_id, properties)


@pytest.mark.parametrize(
    ("shape", "category", "axis", "slope", "portal"),
    [
        ("xo", "straight", "x", None, None),
        ("zo", "straight", "z", None, None),
        ("pd", "diagonal", "positive_diagonal", None, None),
        ("nd", "diagonal", "negative_diagonal", None, None),
        ("an", "slope", None, "north", None),
        ("as", "slope", None, "south", None),
        ("ae", "slope", None, "east", None),
        ("aw", "slope", None, "west", None),
        ("tn", "portal", None, None, "north"),
        ("ts", "portal", None, None, "south"),
        ("te", "portal", None, None, "east"),
        ("tw", "portal", None, None, "west"),
        ("cr_o", "crossing", None, None, None),
        ("cr_d", "crossing", None, None, None),
        ("cr_pdx", "crossing", None, None, None),
        ("cr_pdz", "crossing", None, None, None),
        ("cr_ndx", "crossing", None, None, None),
        ("cr_ndz", "crossing", None, None, None),
        ("none", "none", None, None, None),
    ],
)
def test_all_create_track_shapes_have_explicit_semantics(
    shape: str,
    category: str,
    axis: str | None,
    slope: str | None,
    portal: str | None,
) -> None:
    region = _region()
    region.set(BlockPos(1, 5, 1), _track(shape=shape))

    result = analyze_create_tracks(region)[0]

    assert result["shape"] == shape
    assert result["shape_category"] == category
    assert result["axis"] == axis
    assert result["slope_direction"] == slope
    assert result["portal_direction"] == portal
    assert result["material"] == "create:andesite"
    assert result["curve_payload_status"] == "unavailable"
    assert result["diagnostics"] == []


@pytest.mark.parametrize(
    ("turn", "present", "expected", "consistency", "diagnostic_code"),
    [
        ("false", False, False, "consistent", None),
        ("false", True, False, "unexpected_present", "unexpected_block_entity"),
        ("true", True, True, "consistent", None),
        ("true", False, True, "missing_expected", "missing_block_entity"),
    ],
)
def test_turn_property_is_the_block_entity_contract(
    turn: str,
    present: bool,
    expected: bool,
    consistency: str,
    diagnostic_code: str | None,
) -> None:
    pos = BlockPos(2, 5, 2)
    region = _region()
    region.set(pos, _track(turn=turn))
    if present:
        region.block_entity_positions.add(pos)

    result = analyze_create_tracks(region)[0]

    assert result["turn"] is expected
    assert result["block_entity_expected"] is expected
    assert result["block_entity_present"] is present
    assert result["block_entity_consistency"] == consistency
    codes = [diagnostic["code"] for diagnostic in result["diagnostics"]]
    assert codes == ([] if diagnostic_code is None else [diagnostic_code])


def test_required_railways_material_families_are_an_exact_bounded_whitelist() -> None:
    assert len(RAILWAYS_REQUIRED_TRACK_IDS) == 52
    assert len(SUPPORTED_CREATE_TRAIN_TRACK_IDS) == 53
    assert {
        "railways:track_oak",
        "railways:track_oak_wide",
        "railways:track_oak_narrow",
        "railways:track_create_andesite_wide",
        "railways:track_create_andesite_narrow",
        "railways:track_monorail",
        GENERIC_CROSSING_ID,
    }.issubset(RAILWAYS_REQUIRED_TRACK_IDS)
    assert "railways:track_not_real" not in RAILWAYS_REQUIRED_TRACK_IDS
    assert "railways:track_create_dd_rose" not in RAILWAYS_REQUIRED_TRACK_IDS


def test_audit_basis_is_public_versioned_and_immutable() -> None:
    assert dict(TRACK_SEMANTICS_AUDIT_BASIS) == {
        "create": "6.0.10",
        "railways": "0.2.0+neoforge-mc1.21.1",
        "tracks": "1.0.1",
    }
    with pytest.raises(TypeError):
        TRACK_SEMANTICS_AUDIT_BASIS["create"] = "different"  # type: ignore[index]


@pytest.mark.parametrize(
    ("block_id", "material"),
    [
        ("railways:track_oak", "railways:oak"),
        ("railways:track_oak_wide", "railways:oak_wide"),
        ("railways:track_oak_narrow", "railways:oak_narrow"),
        ("railways:track_monorail", "railways:monorail"),
        ("railways:track_create_andesite_wide", "railways:create_andesite_wide"),
    ],
)
def test_required_railways_track_material_is_derived_from_exact_id(
    block_id: str,
    material: str,
) -> None:
    region = _region()
    region.set(BlockPos(3, 5, 3), _track(block_id))

    result = analyze_create_tracks(region)[0]

    assert result["id"] == block_id
    assert result["material"] == material
    assert result["shape_category"] == "straight"


def test_generic_crossing_uses_its_distinct_property_and_entity_contract() -> None:
    pos = BlockPos(4, 5, 4)
    region = _region()
    region.set(
        pos,
        BlockState.from_id(
            GENERIC_CROSSING_ID,
            {"shape": "cr_o", "waterlogged": "false"},
        ),
    )
    region.block_entity_positions.add(pos)

    result = analyze_create_tracks(region)[0]

    assert result["shape_category"] == "crossing"
    assert result["turn"] is None
    assert result["material"] is None
    assert result["block_entity_expected"] is True
    assert result["block_entity_consistency"] == "consistent"
    assert result["diagnostics"] == [
        {
            "code": "material_requires_block_entity_payload",
            "severity": "warning",
            "property": "material",
            "value": None,
        }
    ]


def test_generic_crossing_without_block_entity_is_structurally_incomplete() -> None:
    region = _region()
    region.set(
        BlockPos(4, 5, 5),
        BlockState.from_id(
            GENERIC_CROSSING_ID,
            {"shape": "cr_d", "waterlogged": "true"},
        ),
    )

    result = analyze_create_tracks(region)[0]

    assert result["waterlogged"] is True
    assert result["block_entity_expected"] is True
    assert result["block_entity_present"] is False
    assert result["block_entity_consistency"] == "missing_expected"
    assert [item["code"] for item in result["diagnostics"]] == [
        "material_requires_block_entity_payload",
        "missing_block_entity",
    ]


def test_generic_crossing_rejects_non_crossing_shape_without_guessing_geometry() -> None:
    pos = BlockPos(4, 5, 6)
    region = _region()
    region.set(
        pos,
        BlockState.from_id(
            GENERIC_CROSSING_ID,
            {"shape": "xo", "waterlogged": "false"},
        ),
    )
    region.block_entity_positions.add(pos)

    result = analyze_create_tracks(region)[0]

    assert result["shape"] == "xo"
    assert result["shape_category"] is None
    assert result["axis"] is None
    assert result["slope_direction"] is None
    assert result["portal_direction"] is None
    assert [item["code"] for item in result["diagnostics"]] == [
        "invalid_generic_crossing_shape",
        "material_requires_block_entity_payload",
    ]


def test_missing_and_invalid_properties_are_diagnosed_without_inference() -> None:
    pos = BlockPos(5, 5, 5)
    region = _region()
    region.set(
        pos,
        BlockState.from_id(
            CREATE_TRACK_ID,
            {"turn": "yes"},
        ),
    )

    result = analyze_create_tracks(region)[0]

    assert result["shape"] is None
    assert result["shape_category"] is None
    assert result["axis"] is None
    assert result["turn"] is None
    assert result["waterlogged"] is None
    assert result["block_entity_expected"] is None
    assert result["block_entity_consistency"] == "unknown_expectation"
    assert [(item["code"], item["property"], item["value"]) for item in result["diagnostics"]] == [
        ("missing_property", "shape", None),
        ("invalid_property", "turn", "yes"),
        ("missing_property", "waterlogged", None),
    ]


def test_invalid_shape_is_retained_as_evidence_but_never_classified() -> None:
    region = _region()
    region.set(BlockPos(6, 5, 6), _track(shape="curve"))

    result = analyze_create_tracks(region)[0]

    assert result["shape"] == "curve"
    assert result["shape_category"] is None
    assert result["axis"] is None
    assert result["diagnostics"] == [
        {
            "code": "invalid_property",
            "severity": "error",
            "property": "shape",
            "value": "curve",
        }
    ]


def test_non_rail_tracks_mount_optional_compat_and_unknown_ids_are_excluded() -> None:
    region = _region()
    region.set(BlockPos(1, 5, 1), _track(EXCLUDED_TRACKS_MOUNT_ID))
    region.set(BlockPos(2, 5, 1), _track("railways:track_create_dd_rose"))
    region.set(BlockPos(3, 5, 1), _track("railways:track_not_real"))
    region.set(BlockPos(4, 5, 1), _track("minecraft:rail"))

    assert analyze_create_tracks(region) == []


def test_halo_is_excluded_and_results_are_coordinate_sorted_without_mutation() -> None:
    positions = [
        BlockPos(8, 4, 8),
        BlockPos(1, 7, 1),
        BlockPos(1, 4, 2),
        BlockPos(2, 4, 1),
    ]
    region = _region()
    for pos in reversed(positions):
        region.set(pos, _track())
    halo = positions[0]
    region.halo_positions.add(halo)
    blocks_before = copy.deepcopy(region.blocks)
    entities_before = set(region.block_entity_positions)

    results = analyze_create_tracks(region)

    assert [item["pos"] for item in results] == [
        pos.to_dict() for pos in sorted(set(positions) - {halo})
    ]
    assert region.blocks == blocks_before
    assert region.block_entity_positions == entities_before


def test_requires_region_data() -> None:
    with pytest.raises(TypeError, match="RegionData"):
        analyze_create_tracks(object())  # type: ignore[arg-type]
