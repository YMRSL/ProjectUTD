from __future__ import annotations

import json
import logging
from pathlib import Path

import pytest
from pydantic import ValidationError

from picasso.core.fragment_engine import FragmentEngine
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.style_engine import load_pass_registry
from picasso.models.block import BlockPos, BlockState
from picasso.models.fragment import Fragment, FragmentBlock
from picasso.models.region import RegionData
from picasso.models.style_pass import NoiseConfig, ReplaceOption, StyleRule, StylePass


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = PROJECT_ROOT / "src" / "picasso" / "data"


def _replace_rule(block: str = "minecraft:stone", *, weight: float = 1.0) -> dict:
    return {
        "match": {"block": "minecraft:dirt"},
        "action": "replace",
        "replace_with": [{"block": block, "weight": weight}],
    }


def _block_pass(rule: dict | None = None, **overrides: object) -> dict:
    data: dict[str, object] = {
        "name": "test_pass",
        "description": "Validation fixture.",
        "rules": [rule or _replace_rule()],
    }
    data.update(overrides)
    return data


@pytest.mark.parametrize(
    ("model", "data"),
    [
        (ReplaceOption, {"block": "stone"}),
        (
            StyleRule,
            {
                "match": {"block": "stone"},
                "action": "replace",
                "replace_with": [{"block": "minecraft:stone"}],
            },
        ),
        (
            StyleRule,
            {
                "match": {},
                "action": "place_adjacent",
                "place_block": "vine",
                "direction": "above",
            },
        ),
        (FragmentBlock, {"offset": [0, 0, 0], "block": "air"}),
    ],
)
def test_block_ids_must_be_canonical_and_namespaced(model: type, data: dict) -> None:
    with pytest.raises(ValidationError, match="namespaced block id"):
        model.model_validate(data)


@pytest.mark.parametrize(
    "data",
    [
        {"scale": 0.0},
        {"scale": -0.1},
        {"threshold": -0.01},
        {"threshold": 1.01},
    ],
)
def test_noise_constraints_fail_fast(data: dict) -> None:
    with pytest.raises(ValidationError):
        NoiseConfig.model_validate(data)


@pytest.mark.parametrize(
    ("model", "data"),
    [
        (NoiseConfig, {"threshhold": 0.5}),
        (ReplaceOption, {"block": "minecraft:stone", "weigth": 1.0}),
        (
            StyleRule,
            {
                "match": {},
                "action": "place_adjacent",
                "place_block": "minecraft:vine",
                "direction": "above",
                "place_bloc": "minecraft:vine",
            },
        ),
        (StylePass, {**_block_pass(), "only_safe_block": True}),
        (
            FragmentBlock,
            {
                "offset": [0, 0, 0],
                "block": "minecraft:stone",
                "probablity": 0.5,
            },
        ),
        (
            Fragment,
            {
                "name": "test_fragment",
                "description": "Fragment fixture.",
                "anchor_surface": "floor",
                "footprint": "1x1",
                "blocks": [
                    {"offset": [0, 0, 0], "block": "minecraft:stone"}
                ],
                "match_hnit": "glass",
            },
        ),
    ],
)
def test_definition_models_forbid_unknown_fields(model: type, data: dict) -> None:
    with pytest.raises(ValidationError, match="Extra inputs are not permitted"):
        model.model_validate(data)


def test_replacement_weights_require_non_negative_and_one_positive_option() -> None:
    with pytest.raises(ValidationError):
        ReplaceOption.model_validate({"block": "minecraft:stone", "weight": -0.1})

    with pytest.raises(ValidationError, match="at least one positive"):
        StyleRule.model_validate(
            {
                "match": {},
                "action": "replace",
                "replace_with": [
                    {"block": "minecraft:stone", "weight": 0.0},
                    {"block": "minecraft:dirt", "weight": 0.0},
                ],
            }
        )


@pytest.mark.parametrize(
    "rule",
    [
        {"match": {}, "action": "replace"},
        {
            "match": {},
            "action": "replace",
            "replace_with": [{"block": "minecraft:stone"}],
            "place_block": "minecraft:vine",
        },
        {
            "match": {},
            "action": "place_adjacent",
            "place_block": "minecraft:vine",
        },
        {
            "match": {},
            "action": "place_adjacent",
            "place_block": "minecraft:vine",
            "direction": "sideways",
        },
        {
            "match": {},
            "action": "place_adjacent",
            "place_block": "minecraft:vine",
            "direction": "above",
            "replace_with": [{"block": "minecraft:stone"}],
        },
        {
            "match": {},
            "action": "remove",
            "place_block": "minecraft:air",
        },
        {"match": {}, "action": "remove", "weight": -0.01},
        {"match": {}, "action": "remove", "weight": 1.01},
    ],
)
def test_style_rule_action_contracts_reject_invalid_combinations(rule: dict) -> None:
    with pytest.raises(ValidationError):
        StyleRule.model_validate(rule)


@pytest.mark.parametrize(
    "match",
    [
        {"blocks": ["minecraft:stone"]},
        {"block": []},
        {"namespace": ""},
        {"namespace": "Minecraft"},
        {"namespace": 123},
        {"name_contains": "   "},
        {"name_contains": 123},
        {"surface": "wall"},
        {"surface": []},
        {"surface": ["floor", "wall"]},
        {"surface": 123},
        {"adjacent_air": 1},
        {"adjacent_air": None},
        {"y_min": "64"},
        {"y_min": True},
        {"y_max": None},
        {"y_min": 80, "y_max": 70},
    ],
)
def test_match_schema_rejects_unknown_keys_and_invalid_values(match: dict) -> None:
    with pytest.raises(ValidationError):
        StyleRule.model_validate(
            {
                "match": match,
                "action": "replace",
                "replace_with": [{"block": "minecraft:stone"}],
            }
        )


def test_match_schema_allows_empty_global_match_and_normalizes_strings() -> None:
    global_rule = StyleRule.model_validate(
        {
            "match": {},
            "action": "replace",
            "replace_with": [{"block": "minecraft:stone"}],
        }
    )
    assert global_rule.match == {}

    constrained_rule = StyleRule.model_validate(
        {
            "match": {
                "block": [" minecraft:stone "],
                "namespace": " minecraft ",
                "name_contains": " stone ",
                "surface": [" floor ", "outer_wall"],
                "adjacent_air": False,
                "y_min": 0,
                "y_max": 100,
            },
            "action": "replace",
            "replace_with": [{"block": "minecraft:stone"}],
        }
    )
    assert constrained_rule.match == {
        "block": ["minecraft:stone"],
        "namespace": "minecraft",
        "name_contains": "stone",
        "surface": ["floor", "outer_wall"],
        "adjacent_air": False,
        "y_min": 0,
        "y_max": 100,
    }


@pytest.mark.parametrize(
    "data",
    [
        {"name": "missing_rules", "description": "No rules field."},
        {"name": "empty_rules", "description": "Empty rules.", "rules": []},
        _block_pass(type="unknown"),
        {
            "name": "fragment_missing_fragments",
            "description": "Missing fragments.",
            "type": "fragment_pass",
            "anchor_surface": "floor",
            "density": 0.5,
        },
        {
            "name": "fragment_missing_anchor",
            "description": "Missing anchor.",
            "type": "fragment_pass",
            "fragments": ["rubble"],
            "density": 0.5,
        },
        {
            "name": "fragment_missing_density",
            "description": "Missing density.",
            "type": "fragment_pass",
            "fragments": ["rubble"],
            "anchor_surface": "floor",
        },
        {
            "name": "fragment_bad_density",
            "description": "Bad density.",
            "type": "fragment_pass",
            "fragments": ["rubble"],
            "anchor_surface": "floor",
            "density": 1.01,
        },
        {
            "name": "fragment_bad_spacing",
            "description": "Bad spacing.",
            "type": "fragment_pass",
            "fragments": ["rubble"],
            "anchor_surface": "floor",
            "density": 0.5,
            "min_spacing": -1,
        },
        {
            "name": "fragment_bad_anchor",
            "description": "Bad anchor.",
            "type": "fragment_pass",
            "fragments": ["rubble"],
            "anchor_surface": "wall",
            "density": 0.5,
        },
        {
            "name": "fragment_embedded_filter",
            "description": "Space filtering belongs to the invocation.",
            "type": "fragment_pass",
            "fragments": ["rubble"],
            "anchor_surface": "floor",
            "density": 0.5,
            "space_filter": "interior",
        },
        {
            "name": "pattern_missing_mappings",
            "description": "Missing mappings.",
            "type": "pattern_replace",
        },
        _block_pass(fragments=["rubble"]),
    ],
)
def test_style_pass_type_contracts_reject_invalid_definitions(data: dict) -> None:
    with pytest.raises(ValidationError):
        StylePass.model_validate(data)


def test_definition_strings_are_non_empty_and_stripped() -> None:
    with pytest.raises(ValidationError):
        StylePass.model_validate(_block_pass(name="   "))
    with pytest.raises(ValidationError):
        StylePass.model_validate(_block_pass(description="   "))
    with pytest.raises(ValidationError):
        StylePass.model_validate(
            {
                "name": "fragment_pass",
                "description": "Fragment pass.",
                "type": "fragment_pass",
                "fragments": ["   "],
                "anchor_surface": "floor",
                "density": 0.5,
            }
        )
    with pytest.raises(ValidationError):
        Fragment.model_validate(_fragment_data(tags=["debris", "   "]))
    with pytest.raises(ValidationError):
        Fragment.model_validate(_fragment_data(match_hint="   "))

    style_pass = StylePass.model_validate(_block_pass(name=" padded_name "))
    fragment = Fragment.model_validate(
        _fragment_data(name=" padded_fragment ", match_hint=" glass_pane ")
    )
    assert style_pass.name == "padded_name"
    assert fragment.name == "padded_fragment"
    assert fragment.match_hint == "glass_pane"


@pytest.mark.parametrize(
    "mappings",
    [
        [{"pattern": "chair"}],
        [{"pattern": "chair", "dd_block": "chair"}],
        [{"pattern": "", "dd_block": "example:chair"}],
        [{"pattern": "chair", "dd_block": "example:chair", "extra": True}],
        [
            {"pattern": "chair", "dd_block": "example:chair"},
            {"pattern": "chair", "dd_block": "example:other_chair"},
        ],
    ],
)
def test_pattern_mapping_schema_and_duplicates_are_rejected(mappings: list[dict]) -> None:
    with pytest.raises(ValidationError):
        StylePass.model_validate(
            {
                "name": "bad_mapping",
                "description": "Bad mapping fixture.",
                "type": "pattern_replace",
                "mappings": mappings,
            }
        )


@pytest.mark.parametrize(
    "rule",
    [
        {"match": {}, "action": "remove"},
        _replace_rule("minecraft:air"),
        {
            "match": {},
            "action": "place_adjacent",
            "place_block": "minecraft:air",
            "direction": "above",
        },
    ],
)
def test_destructive_rule_requires_pass_declaration(rule: dict) -> None:
    with pytest.raises(ValidationError, match="destructive=true"):
        StylePass.model_validate(_block_pass(rule))

    valid = StylePass.model_validate(_block_pass(rule, destructive=True))
    assert valid.destructive is True


def _fragment_data(**overrides: object) -> dict:
    data: dict[str, object] = {
        "name": "test_fragment",
        "description": "Fragment validation fixture.",
        "anchor_surface": "floor",
        "footprint": "1x1",
        "blocks": [{"offset": [0, 0, 0], "block": "minecraft:stone"}],
    }
    data.update(overrides)
    return data


@pytest.mark.parametrize(
    "data",
    [
        _fragment_data(blocks=[]),
        _fragment_data(anchor_surface="wall"),
        _fragment_data(min_clear_height=-1),
        _fragment_data(
            blocks=[
                {
                    "offset": [0, 0, 0],
                    "block": "minecraft:stone",
                    "probability": -0.01,
                }
            ]
        ),
        _fragment_data(
            blocks=[
                {
                    "offset": [0, 0, 0],
                    "block": "minecraft:stone",
                    "probability": 1.01,
                }
            ]
        ),
        _fragment_data(
            blocks=[{"offset": [0, 0, 0], "block": "minecraft:air"}]
        ),
        _fragment_data(footprint="0x1"),
        _fragment_data(footprint="1X1"),
        _fragment_data(
            footprint="1x1",
            blocks=[
                {"offset": [0, 0, 0], "block": "minecraft:stone"},
                {"offset": [1, 0, 0], "block": "minecraft:stone"},
            ],
        ),
        _fragment_data(match_hint="glass"),
    ],
)
def test_fragment_contracts_reject_invalid_definitions(data: dict) -> None:
    with pytest.raises(ValidationError):
        Fragment.model_validate(data)


def test_repeated_fragment_offsets_preserve_declaration_order_layering() -> None:
    fragment = Fragment.model_validate(
        _fragment_data(
            destructive=True,
            requires_clear_above=False,
            blocks=[
                {"offset": [0, 0, 0], "block": "minecraft:air"},
                {
                    "offset": [0, 0, 0],
                    "block": "minecraft:vine",
                    "preserve_existing": True,
                },
            ],
        )
    )

    class _Library:
        def get(self, name: str) -> Fragment | None:
            return fragment if name == fragment.name else None

    anchor = BlockPos(0, 0, 0)
    region = RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=0,
        y_max=0,
        loaded_chunks={(0, 0)},
    )
    region.set(anchor, BlockState.from_id("minecraft:stone"))
    region.surface_classes[anchor] = "floor"
    changes = FragmentEngine(
        _Library(),  # type: ignore[arg-type]
        safe_replaceable={"minecraft:stone"},
        structural_never_touch=set(),
    ).apply(
        {
            "name": "layering_pass",
            "anchor_surface": "floor",
            "fragments": [fragment.name],
            "density": 1.0,
        },
        region,
        seed=42,
    )

    assert changes.get(anchor) == BlockState.from_id("minecraft:vine")


def test_pass_loader_warns_and_skips_bad_files(tmp_path: Path, caplog: pytest.LogCaptureFixture) -> None:
    (tmp_path / "valid.json").write_text(
        json.dumps(
            {
                "name": "valid",
                "description": "Valid fixture.",
                "rules": [_replace_rule()],
            }
        ),
        encoding="utf-8",
    )
    (tmp_path / "undeclared_remove.json").write_text(
        json.dumps(
            {
                "name": "undeclared_remove",
                "description": "Invalid destructive contract.",
                "rules": [{"match": {}, "action": "remove"}],
            }
        ),
        encoding="utf-8",
    )
    (tmp_path / "malformed.json").write_text("{", encoding="utf-8")
    caplog.set_level(logging.WARNING, logger="picasso.core.style_engine")

    registry = load_pass_registry(tmp_path)

    assert set(registry) == {"valid"}
    skipped = [record for record in caplog.records if "Skipping invalid pass" in record.message]
    assert len(skipped) == 2


def test_all_shipped_passes_and_active_fragments_load() -> None:
    pass_files = sorted((DATA_ROOT / "passes").glob("*.json"))
    registry = load_pass_registry(DATA_ROOT / "passes")
    assert len(pass_files) == 14
    assert set(registry) == {path.stem for path in pass_files}

    fragment_files = []
    for path in sorted((DATA_ROOT / "fragments").glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        if not data.get("deprecated"):
            fragment_files.append(path)
    library = FragmentLibrary(DATA_ROOT / "fragments")
    assert len(fragment_files) == 11
    assert set(library.fragments) == {path.stem for path in fragment_files}
