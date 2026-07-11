from __future__ import annotations

from types import MappingProxyType
from typing import Literal, Mapping, TypedDict

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


CREATE_TRACK_ID = "create:track"
GENERIC_CROSSING_ID = "railways:generic_crossing"
EXCLUDED_TRACKS_MOUNT_ID = "tracks:track_mount"

# Immutable provenance for the local JAR contracts audited by this module.
# Changing either dependency version requires re-checking the registry tag,
# blockstate JSON, and TrackBlock/GenericCrossingBlock bytecode contracts.
TRACK_SEMANTICS_AUDIT_BASIS: Mapping[str, str] = MappingProxyType(
    {
        "create": "6.0.10",
        "railways": "0.2.0+neoforge-mc1.21.1",
        "tracks": "1.0.1",
    }
)

# Exact string entries (not required:false objects) from Railways 0.2.0's
# data/create/tags/block/tracks.json. Optional compatibility entries are
# deliberately not inferred to be registered when their source mods are absent.
_RAILWAYS_STANDARD_MATERIALS = frozenset(
    {
        "acacia",
        "bamboo",
        "birch",
        "blackstone",
        "cherry",
        "crimson",
        "dark_oak",
        "ender",
        "jungle",
        "mangrove",
        "oak",
        "phantom",
        "spruce",
        "stripped_bamboo",
        "tieless",
        "warped",
    }
)
_RAILWAYS_GAUGED_MATERIALS = frozenset(
    {
        "acacia",
        "bamboo",
        "birch",
        "blackstone",
        "cherry",
        "create_andesite",
        "crimson",
        "dark_oak",
        "ender",
        "jungle",
        "mangrove",
        "oak",
        "phantom",
        "spruce",
        "stripped_bamboo",
        "tieless",
        "warped",
    }
)
RAILWAYS_REQUIRED_TRACK_IDS = frozenset(
    {f"railways:track_{material}" for material in _RAILWAYS_STANDARD_MATERIALS}
    | {
        f"railways:track_{material}_{gauge}"
        for material in _RAILWAYS_GAUGED_MATERIALS
        for gauge in ("wide", "narrow")
    }
    | {"railways:track_monorail", GENERIC_CROSSING_ID}
)
SUPPORTED_CREATE_TRAIN_TRACK_IDS = frozenset(
    {CREATE_TRACK_ID} | set(RAILWAYS_REQUIRED_TRACK_IDS)
)


TrackShapeCategory = Literal[
    "straight",
    "diagonal",
    "slope",
    "portal",
    "crossing",
    "none",
]
TrackAxis = Literal["x", "z", "positive_diagonal", "negative_diagonal"]
CardinalDirection = Literal["north", "south", "east", "west"]
BlockEntityConsistency = Literal[
    "consistent",
    "missing_expected",
    "unexpected_present",
    "unknown_expectation",
]
DiagnosticSeverity = Literal["warning", "error"]


class TrackSemanticDiagnostic(TypedDict):
    code: str
    severity: DiagnosticSeverity
    property: str | None
    value: str | None


class CreateTrackSemantic(TypedDict):
    pos: dict[str, int]
    id: str
    material: str | None
    shape: str | None
    shape_category: TrackShapeCategory | None
    axis: TrackAxis | None
    slope_direction: CardinalDirection | None
    portal_direction: CardinalDirection | None
    turn: bool | None
    waterlogged: bool | None
    block_entity_expected: bool | None
    block_entity_present: bool
    block_entity_consistency: BlockEntityConsistency
    curve_payload_status: Literal["unavailable"]
    diagnostics: list[TrackSemanticDiagnostic]


_SHAPE_DETAILS: dict[
    str,
    tuple[
        TrackShapeCategory,
        TrackAxis | None,
        CardinalDirection | None,
        CardinalDirection | None,
    ],
] = {
    "xo": ("straight", "x", None, None),
    "zo": ("straight", "z", None, None),
    "pd": ("diagonal", "positive_diagonal", None, None),
    "nd": ("diagonal", "negative_diagonal", None, None),
    "an": ("slope", None, "north", None),
    "as": ("slope", None, "south", None),
    "ae": ("slope", None, "east", None),
    "aw": ("slope", None, "west", None),
    "tn": ("portal", None, None, "north"),
    "ts": ("portal", None, None, "south"),
    "te": ("portal", None, None, "east"),
    "tw": ("portal", None, None, "west"),
    "cr_o": ("crossing", None, None, None),
    "cr_d": ("crossing", None, None, None),
    "cr_pdx": ("crossing", None, None, None),
    "cr_pdz": ("crossing", None, None, None),
    "cr_ndx": ("crossing", None, None, None),
    "cr_ndz": ("crossing", None, None, None),
    "none": ("none", None, None, None),
}


def analyze_create_tracks(region: RegionData) -> list[CreateTrackSemantic]:
    """Return deterministic, read-only semantics for supported Create train tracks.

    The function consumes only ``RegionData`` block states and its block-entity
    position set. It cannot inspect curve NBT or Create's global railway saved
    data, so every record reports ``curve_payload_status=unavailable``.

    Railways ``required:false`` compatibility IDs and ``tracks:track_mount`` are
    intentionally outside the supported set. Their presence in an asset JAR is
    not proof that the corresponding block was registered in the current pack.
    """
    if not isinstance(region, RegionData):
        raise TypeError("region must be a RegionData")

    results: list[CreateTrackSemantic] = []
    for pos, state in sorted(region.iter_target_blocks(), key=lambda item: item[0]):
        if state.full_id not in SUPPORTED_CREATE_TRAIN_TRACK_IDS:
            continue
        results.append(_analyze_track(pos, state, pos in region.block_entity_positions))
    return results


def _analyze_track(
    pos: BlockPos,
    state: BlockState,
    block_entity_present: bool,
) -> CreateTrackSemantic:
    diagnostics: list[TrackSemanticDiagnostic] = []
    generic_crossing = state.full_id == GENERIC_CROSSING_ID

    shape = _read_shape(state, diagnostics, generic_crossing=generic_crossing)
    shape_details = _SHAPE_DETAILS.get(shape) if shape is not None else None
    if generic_crossing and shape_details is not None and shape_details[0] != "crossing":
        # Preserve the raw value as evidence, but do not present ordinary-track
        # geometry as valid GenericCrossingBlock semantics.
        shape_details = None
    if shape_details is None:
        shape_category = None
        axis = None
        slope_direction = None
        portal_direction = None
    else:
        shape_category, axis, slope_direction, portal_direction = shape_details

    if generic_crossing:
        turn = None
        block_entity_expected: bool | None = True
        material = None
        diagnostics.append(
            _diagnostic(
                "material_requires_block_entity_payload",
                "warning",
                property_name="material",
            )
        )
    else:
        turn = _read_bool_property(state, "turn", diagnostics)
        block_entity_expected = turn
        material = _material_for(state.full_id)

    waterlogged = _read_bool_property(state, "waterlogged", diagnostics)
    consistency = _block_entity_consistency(
        block_entity_expected,
        block_entity_present,
        diagnostics,
    )

    return {
        "pos": pos.to_dict(),
        "id": state.full_id,
        "material": material,
        "shape": shape,
        "shape_category": shape_category,
        "axis": axis,
        "slope_direction": slope_direction,
        "portal_direction": portal_direction,
        "turn": turn,
        "waterlogged": waterlogged,
        "block_entity_expected": block_entity_expected,
        "block_entity_present": block_entity_present,
        "block_entity_consistency": consistency,
        "curve_payload_status": "unavailable",
        "diagnostics": diagnostics,
    }


def _read_shape(
    state: BlockState,
    diagnostics: list[TrackSemanticDiagnostic],
    *,
    generic_crossing: bool,
) -> str | None:
    raw = state.properties.get("shape")
    if raw is None:
        diagnostics.append(_diagnostic("missing_property", "error", "shape"))
        return None
    if not isinstance(raw, str) or raw not in _SHAPE_DETAILS:
        diagnostics.append(
            _diagnostic("invalid_property", "error", "shape", _diagnostic_value(raw))
        )
        return raw if isinstance(raw, str) else None
    if generic_crossing and _SHAPE_DETAILS[raw][0] != "crossing":
        diagnostics.append(
            _diagnostic("invalid_generic_crossing_shape", "error", "shape", raw)
        )
        return raw
    return raw


def _read_bool_property(
    state: BlockState,
    name: str,
    diagnostics: list[TrackSemanticDiagnostic],
) -> bool | None:
    raw = state.properties.get(name)
    if raw is None:
        diagnostics.append(_diagnostic("missing_property", "error", name))
        return None
    if raw == "true":
        return True
    if raw == "false":
        return False
    diagnostics.append(
        _diagnostic("invalid_property", "error", name, _diagnostic_value(raw))
    )
    return None


def _block_entity_consistency(
    expected: bool | None,
    present: bool,
    diagnostics: list[TrackSemanticDiagnostic],
) -> BlockEntityConsistency:
    if expected is None:
        return "unknown_expectation"
    if expected and not present:
        diagnostics.append(_diagnostic("missing_block_entity", "error"))
        return "missing_expected"
    if not expected and present:
        diagnostics.append(_diagnostic("unexpected_block_entity", "error"))
        return "unexpected_present"
    return "consistent"


def _material_for(block_id: str) -> str:
    if block_id == CREATE_TRACK_ID:
        return "create:andesite"
    return "railways:" + block_id.removeprefix("railways:track_")


def _diagnostic(
    code: str,
    severity: DiagnosticSeverity,
    property_name: str | None = None,
    value: str | None = None,
) -> TrackSemanticDiagnostic:
    return {
        "code": code,
        "severity": severity,
        "property": property_name,
        "value": value,
    }


def _diagnostic_value(value: object) -> str | None:
    return None if value is None else str(value)
