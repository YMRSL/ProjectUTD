from __future__ import annotations

import inspect
import tempfile
from contextlib import contextmanager
from datetime import datetime, timezone
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from picasso.core.bundle_executor import BundleExecutor
from picasso.core.journal import JournalUnavailableError
from picasso.core.player_protection import PlayerProtectionEvaluator
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.player_activity import BlockBounds, ProtectionArea
from picasso.models.region import RegionData
from picasso.session import session
from picasso.tools import bundle, npc, style


TARGET = BlockPos(1, 70, 1)
NOW = datetime(2026, 7, 10, tzinfo=timezone.utc)


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


class FakeBridge:
    dimension = "minecraft:overworld"

    def __init__(self) -> None:
        self.states: dict[BlockPos, BlockState] = {}
        self.write_calls: list[dict[BlockPos, BlockState]] = []

    def read_block_with_entity(self, x: int, y: int, z: int):
        return self.states.get(BlockPos(x, y, z), AIR), False

    def write_region(self, changes: RegionData) -> int:
        snapshot = dict(changes.blocks)
        self.write_calls.append(snapshot)
        self.states.update(snapshot)
        return len(snapshot)


class FakeEngine:
    safe_replaceable = {"minecraft:stone"}
    structural_never_touch = {"minecraft:bedrock"}
    safety_policy_error = None

    def __init__(self, pass_registry: dict[str, object]) -> None:
        self.pass_registry = pass_registry

    def apply(self, _pass_name, _region, _intensity, _seed, _space_filter):
        changes = RegionData()
        changes.set(TARGET, BlockState.from_id("minecraft:andesite"))
        return changes


def source_region() -> RegionData:
    region = RegionData(
        origin_cx=0,
        origin_cz=0,
        radius_chunks=0,
        y_min=60,
        y_max=80,
        loaded_chunks={(0, 0)},
    )
    region.set(TARGET, BlockState.from_id("minecraft:stone"))
    return region


def player_built_area() -> ProtectionArea:
    return ProtectionArea(
        "player-base",
        "player_built",
        bounds=BlockBounds(0, 2, 60, 80, 0, 2),
    )


def operator_area() -> ProtectionArea:
    return ProtectionArea(
        "operator-zone",
        "protected_region",
        bounds=BlockBounds(0, 2, 60, 80, 0, 2),
        source="operator_policy",
    )


def evaluator(*areas: ProtectionArea, unavailable: bool = False) -> PlayerProtectionEvaluator:
    return PlayerProtectionEvaluator.from_sources(
        activity_events=None if unavailable else (),
        protection_areas=areas,
        as_of=NOW,
    )


@contextmanager
def isolated_session(world_path: Path):
    keys = (
        "bridge",
        "world_path",
        "pass_registry",
        "bundle_registry",
        "pattern_matcher",
        "fragment_library",
        "catalog",
        "last_region",
        "journal",
        "journal_status",
        "journal_error",
        "noise_backend",
    )
    original = {key: getattr(session, key) for key in keys}
    session.bridge = FakeBridge()
    session.world_path = world_path
    session.pass_registry = {
        "paint": SimpleNamespace(
            only_safe_blocks=False,
            deprecated=False,
            destructive=False,
        )
    }
    session.bundle_registry = {
        "test-bundle": {
            "name": "test-bundle",
            "entries": [
                {
                    "structure_type": "test",
                    "passes": [{"name": "paint"}],
                }
            ],
        }
    }
    session.pattern_matcher = None
    session.fragment_library = None
    session.catalog = None
    session.last_region = None
    session.journal = None
    session.journal_status = "unavailable"
    session.journal_error = None
    session.noise_backend = "fallback"
    try:
        yield
    finally:
        for key, value in original.items():
            setattr(session, key, value)


def register(module) -> dict[str, object]:
    mcp = FakeMCP()
    module.register(mcp)
    return mcp.tools


def test_style_preserves_unavailable_status_while_skipping_known_player_zone() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        engine = FakeEngine(session.pass_registry)
        tools = register(style)
        snapshot = evaluator(player_built_area(), unavailable=True)
        with (
            patch.object(style, "ensure_region", return_value=source_region()),
            patch.object(style, "_engine", return_value=engine),
            patch.object(style, "current_player_protection", return_value=snapshot),
        ):
            result = tools["preview_pass"]("paint", 0, 0, 0)

    assert result["ok"] is True
    assert result["would_change"] == 0
    assert result["placements_skipped"][0]["reason"] == "player_built_structure"
    assert result["player_protection"] == "unavailable"
    assert result["player_protection_details"]["status"] == "unavailable"


def test_style_override_bypasses_player_zone_but_not_operator_region() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        engine = FakeEngine(session.pass_registry)
        tools = register(style)
        with (
            patch.object(style, "ensure_region", return_value=source_region()),
            patch.object(style, "_engine", return_value=engine),
            patch.object(
                style,
                "current_player_protection",
                return_value=evaluator(player_built_area(), operator_area()),
            ),
        ):
            operator_result = tools["preview_pass"](
                "paint", 0, 0, 0, include_player_built=True
            )
        with (
            patch.object(style, "ensure_region", return_value=source_region()),
            patch.object(style, "_engine", return_value=engine),
            patch.object(
                style,
                "current_player_protection",
                return_value=evaluator(player_built_area()),
            ),
        ):
            player_only_result = tools["preview_pass"](
                "paint", 0, 0, 0, include_player_built=True
            )

    assert operator_result["would_change"] == 0
    assert operator_result["placements_skipped"][0]["reason"] == "protected_region"
    assert player_only_result["would_change"] == 1


def test_style_journal_failure_keeps_frozen_protection_status() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        engine = FakeEngine(session.pass_registry)
        tools = register(style)
        snapshot = evaluator()
        with (
            patch.object(style, "ensure_region", return_value=source_region()),
            patch.object(style, "_engine", return_value=engine),
            patch.object(style, "current_player_protection", return_value=snapshot),
            patch.object(
                session,
                "require_journal",
                side_effect=JournalUnavailableError("injected journal failure"),
            ),
        ):
            result = tools["apply_pass"]("paint", 0, 0, 0)

    assert result["ok"] is False
    assert result["error"] == "journal_unavailable"
    assert result["player_protection"] == "active"
    assert result["player_protection_details"]["status"] == "active"


def test_bundle_contract_exposes_include_player_built_through_tool_and_executor() -> None:
    tool = register(bundle)["apply_bundle"]

    assert "include_player_built" in inspect.signature(tool).parameters
    assert "include_player_built" in inspect.signature(
        BundleExecutor.execute_region
    ).parameters


def test_bundle_default_skip_and_protection_status_propagate_to_response() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        tools = register(bundle)
        snapshot = evaluator(player_built_area(), unavailable=True)
        engine = FakeEngine(session.pass_registry)
        with (
            patch.object(bundle, "ensure_region", return_value=source_region()),
            patch.object(bundle, "StyleEngine", return_value=engine),
            patch.object(
                bundle,
                "current_player_protection",
                return_value=snapshot,
                create=True,
            ),
        ):
            result = tools["apply_bundle"]("test-bundle", 0, 0, 0, dry_run=True)

    assert result["ok"] is True
    assert result["total_would_change"] == 0
    assert result["placements_skipped"][0]["reason"] == "player_built_structure"
    assert result["player_protection"] == "unavailable"
    assert result["player_protection_details"]["status"] == "unavailable"


def test_bundle_override_still_skips_operator_region() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        tools = register(bundle)
        snapshot = evaluator(player_built_area(), operator_area())
        engine = FakeEngine(session.pass_registry)
        with (
            patch.object(bundle, "ensure_region", return_value=source_region()),
            patch.object(bundle, "StyleEngine", return_value=engine),
            patch.object(
                bundle,
                "current_player_protection",
                return_value=snapshot,
                create=True,
            ),
        ):
            result = tools["apply_bundle"](
                "test-bundle",
                0,
                0,
                0,
                dry_run=True,
                include_player_built=True,
            )

    assert result["ok"] is True
    assert result["total_would_change"] == 0
    assert result["placements_skipped"][0]["reason"] == "protected_region"


def test_bundle_journal_failure_keeps_frozen_protection_status() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        tools = register(bundle)
        snapshot = evaluator()
        engine = FakeEngine(session.pass_registry)
        with (
            patch.object(bundle, "ensure_region", return_value=source_region()),
            patch.object(bundle, "StyleEngine", return_value=engine),
            patch.object(bundle, "current_player_protection", return_value=snapshot),
            patch.object(
                session,
                "require_journal",
                side_effect=JournalUnavailableError("injected journal failure"),
            ),
        ):
            result = tools["apply_bundle"](
                "test-bundle",
                0,
                0,
                0,
                dry_run=False,
            )

    assert result["ok"] is False
    assert result["error"] == "journal_unavailable"
    assert result["player_protection"] == "active"
    assert result["player_protection_details"]["status"] == "active"


def test_npc_default_skip_and_protection_status_propagate_to_response() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        tools = register(npc)
        snapshot = evaluator(player_built_area(), unavailable=True)
        with patch.object(
            npc,
            "current_player_protection",
            return_value=snapshot,
            create=True,
        ):
            result = tools["place_npc_marker"](
                1,
                70,
                1,
                "ambient",
                "neutral",
            )

        bridge = session.bridge

    assert result["ok"] is False
    assert result["error"] == "marker_write_rejected"
    assert result["placements_skipped"][0]["reason"] == "player_built_structure"
    assert result["player_protection"] == "unavailable"
    assert result["player_protection_details"]["status"] == "unavailable"
    assert bridge.write_calls == []


def test_npc_always_skips_operator_region_without_an_override_surface() -> None:
    with tempfile.TemporaryDirectory() as tmp, isolated_session(Path(tmp)):
        tool = register(npc)["place_npc_marker"]
        snapshot = evaluator(operator_area())
        with patch.object(
            npc,
            "current_player_protection",
            return_value=snapshot,
            create=True,
        ):
            result = tool(
                1,
                70,
                1,
                "ambient",
                "neutral",
            )

    assert result["ok"] is False
    assert result["placements_skipped"][0]["reason"] == "protected_region"
