from __future__ import annotations

from pathlib import Path

import pytest

from picasso.core.amulet_bridge import AmuletBridge
from picasso.core.journal import Journal
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass
from picasso.session import session
from picasso.tools import journal as journal_tools
from picasso.tools import npc, style, world_io


try:
    import amulet
    from amulet.api.block import Block
    from amulet.level.formats.anvil_world.format import AnvilFormat

    AMULET_AVAILABLE = True
except ImportError:  # pragma: no cover - Amulet is a pinned runtime dependency
    AMULET_AVAILABLE = False


JAVA_VERSION = (1, 21, 1)
OVERWORLD = "minecraft:overworld"


class _FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def _create_java_world(root: Path) -> Path:
    world_path = root / "world"
    wrapper = AnvilFormat(str(world_path))
    wrapper.create_and_open("java", JAVA_VERSION)
    wrapper.close()

    level = amulet.load_level(str(world_path))
    try:
        level.create_chunk(0, 0, OVERWORLD)
        level.set_version_block(
            0,
            64,
            0,
            OVERWORLD,
            ("java", JAVA_VERSION),
            Block("minecraft", "oak_planks"),
            None,
        )
        level.save()
    finally:
        level.close()
    return world_path


@pytest.mark.skipif(not AMULET_AVAILABLE, reason="amulet-core is required")
def test_journal_apply_and_revert_survive_world_reopen(tmp_path: Path) -> None:
    world_path = _create_java_world(tmp_path)
    target = BlockPos(0, 64, 0)

    bridge = AmuletBridge(world_path)
    before, _ = bridge.read_block_with_entity(target.x, target.y, target.z)
    changes = RegionData()
    changes.set(
        target,
        BlockState(
            "minecraft",
            "oak_log",
            {"axis": "x"},
        ),
    )
    applied = Journal(world_path, bridge, bridge.dimension).apply(
        changes,
        tool="e2e_apply",
        argument_summary={"purpose": "save_reopen_revert"},
    )
    operation_id = applied.operation_id
    bridge.close()

    reopened = AmuletBridge(world_path)
    after, _ = reopened.read_block_with_entity(target.x, target.y, target.z)
    assert after.full_id == "minecraft:oak_log"
    assert after.properties["axis"] == "x"
    reverted = Journal(world_path, reopened, reopened.dimension).revert_last(operation_id)
    assert reverted["changed"] == 1
    assert reverted["skipped"] == 0
    reopened.close()

    verified = AmuletBridge(world_path)
    try:
        restored, _ = verified.read_block_with_entity(target.x, target.y, target.z)
        assert restored == before
    finally:
        verified.close()


@pytest.mark.skipif(not AMULET_AVAILABLE, reason="amulet-core is required")
def test_dd_block_id_survives_amulet_save_and_reopen(tmp_path: Path) -> None:
    world_path = _create_java_world(tmp_path)
    target = BlockPos(3, 65, 3)
    changes = RegionData()
    changes.set(target, BlockState.from_id("doomsday_decoration:chair"))

    bridge = AmuletBridge(world_path)
    try:
        assert bridge.write_region(changes) == 1
    finally:
        bridge.close()

    reopened = AmuletBridge(world_path)
    try:
        restored, has_block_entity = reopened.read_block_with_entity(
            target.x,
            target.y,
            target.z,
        )
        assert restored.full_id == "doomsday_decoration:chair"
        assert has_block_entity is False
    finally:
        reopened.close()


@pytest.mark.skipif(not AMULET_AVAILABLE, reason="amulet-core is required")
def test_style_tool_apply_and_revert_survive_world_reopen(tmp_path: Path) -> None:
    world_path = _create_java_world(tmp_path)
    mcp = _FakeMCP()
    world_io.register(mcp)
    style.register(mcp)
    journal_tools.register(mcp)
    previous_registry = session.pass_registry
    session.pass_registry = {
        "e2e_replace": StylePass.model_validate(
            {
                "name": "e2e_replace",
                "description": "Replace the E2E fixture block.",
                "only_safe_blocks": False,
                "rules": [
                    {
                        "match": {"block": "minecraft:oak_planks"},
                        "action": "replace",
                        "replace_with": [{"block": "minecraft:stone"}],
                    }
                ],
            }
        )
    }

    try:
        assert mcp.tools["set_world"](str(world_path))["ok"] is True
        applied = mcp.tools["apply_pass"](
            "e2e_replace",
            0,
            0,
            0,
            y_min=64,
            y_max=64,
        )
        assert applied["ok"] is True
        assert applied["changed"] == 1
        operation_id = applied["journal_entry"]["operation_id"]

        assert mcp.tools["close_world"]()["ok"] is True
        assert mcp.tools["set_world"](str(world_path))["ok"] is True
        written, _ = session.bridge.read_block_with_entity(0, 64, 0)
        assert written.full_id == "minecraft:stone"

        reverted = mcp.tools["revert_last_apply"](operation_id)
        assert reverted["ok"] is True
        assert reverted["changed"] == 1
        assert reverted["skipped"] == 0

        assert mcp.tools["close_world"]()["ok"] is True
        assert mcp.tools["set_world"](str(world_path))["ok"] is True
        restored, _ = session.bridge.read_block_with_entity(0, 64, 0)
        assert restored.full_id == "minecraft:oak_planks"
    finally:
        if session.bridge is not None:
            session.close_bridge()
        session.pass_registry = previous_registry


@pytest.mark.skipif(not AMULET_AVAILABLE, reason="amulet-core is required")
def test_npc_marker_and_metadata_revert_as_one_durable_operation(tmp_path: Path) -> None:
    world_path = _create_java_world(tmp_path)
    mcp = _FakeMCP()
    world_io.register(mcp)
    npc.register(mcp)
    journal_tools.register(mcp)

    try:
        opened = mcp.tools["set_world"](str(world_path))
        assert opened["ok"] is True
        assert opened["journal_status"] == "active"

        placed = mcp.tools["place_npc_marker"](
            x=2,
            y=65,
            z=2,
            npc_type="ambient",
            faction="e2e_test",
        )
        assert placed["ok"] is True
        marker_path = Path(placed["marker_file"])
        operation_id = placed["journal_entry"]["operation_id"]
        assert marker_path.exists()

        assert mcp.tools["close_world"]()["ok"] is True
        assert mcp.tools["set_world"](str(world_path))["ok"] is True
        marker_state, _ = session.bridge.read_block_with_entity(2, 65, 2)
        assert marker_state.full_id == "minecraft:structure_void"
        assert marker_path.exists()

        reverted = mcp.tools["revert_last_apply"](operation_id)
        assert reverted["ok"] is True
        assert reverted["changed"] == 1
        assert reverted["skipped"] == 0
        assert not marker_path.exists()

        assert mcp.tools["close_world"]()["ok"] is True
        assert mcp.tools["set_world"](str(world_path))["ok"] is True
        restored, has_block_entity = session.bridge.read_block_with_entity(2, 65, 2)
        assert restored.is_air
        assert has_block_entity is False
        assert not marker_path.exists()
    finally:
        if session.bridge is not None:
            session.close_bridge()
