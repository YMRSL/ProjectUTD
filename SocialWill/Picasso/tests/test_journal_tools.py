from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from picasso.core.journal import JournalArtifact
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.session import session
from picasso.tools import journal as journal_tools


class FakeMCP:
    def __init__(self) -> None:
        self.tools = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


class FakeBridge:
    dimension = "minecraft:overworld"

    def __init__(self, pos: BlockPos, state: BlockState) -> None:
        self.pos = pos
        self.states = {pos: state}

    @property
    def state(self):
        return self.states.get(self.pos, AIR)

    def read_block_with_entity(self, x, y, z):
        return self.states.get(BlockPos(x, y, z), AIR), False

    def write_region(self, changes):
        self.states.update(changes.blocks)
        return len(changes.blocks)


class JournalToolTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.original = {
            "world_path": session.world_path,
            "bridge": session.bridge,
            "journal": session.journal,
            "journal_status": session.journal_status,
            "journal_error": session.journal_error,
            "last_region": session.last_region,
        }
        self.pos = BlockPos(4, 70, 5)
        self.before = BlockState.from_id("minecraft:stone")
        self.after = BlockState.from_id("minecraft:andesite")
        session.world_path = Path(self.temp_dir.name)
        session.bridge = FakeBridge(self.pos, self.before)
        session.journal = None
        session.journal_status = "unavailable"
        session.journal_error = None
        self.assertTrue(session.activate_journal())

        mcp = FakeMCP()
        journal_tools.register(mcp)
        self.tools = mcp.tools

    def tearDown(self) -> None:
        for key, value in self.original.items():
            setattr(session, key, value)
        self.temp_dir.cleanup()

    def test_list_inspect_and_revert_tools(self) -> None:
        changes = RegionData()
        changes.set(self.pos, self.after)
        changes.set(self.pos.offset(dx=1), BlockState.from_id("minecraft:diorite"))
        changes.set(self.pos.offset(dx=2), BlockState.from_id("minecraft:granite"))
        artifact = JournalArtifact.json_file(
            "picasso_markers/paged.json",
            {"pos": self.pos.to_dict()},
            compound_positions=(self.pos,),
            must_not_exist=True,
        )
        applied = session.require_journal().apply(
            changes,
            tool="apply_pass",
            artifacts=(artifact,),
        )
        session.last_region = object()

        listing = self.tools["list_journal_entries"]()
        inspected = self.tools["inspect_journal_entry"](
            applied.operation_id,
            change_offset=0,
            change_limit=2,
        )
        inspected_tail = self.tools["inspect_journal_entry"](
            applied.operation_id,
            change_offset=2,
            change_limit=2,
        )
        inspected_with_content = self.tools["inspect_journal_entry"](
            applied.operation_id,
            include_artifact_content=True,
        )
        invalid_page = self.tools["inspect_journal_entry"](
            applied.operation_id,
            change_limit=501,
        )
        reverted = self.tools["revert_last_apply"]()

        self.assertTrue(listing["ok"])
        self.assertEqual(listing["entries"][0]["operation_id"], applied.operation_id)
        self.assertTrue(inspected["ok"])
        self.assertEqual(inspected["entry"]["status"], "committed")
        self.assertEqual(inspected["change_total"], 3)
        self.assertEqual(len(inspected["entry"]["changes"]), 2)
        self.assertEqual(inspected["next_offset"], 2)
        self.assertEqual(len(inspected_tail["entry"]["changes"]), 1)
        self.assertIsNone(inspected_tail["next_offset"])
        default_snapshot = inspected["entry"]["artifacts"][0]["after"]
        self.assertNotIn("content_base64", default_snapshot)
        self.assertGreater(default_snapshot["byte_count"], 0)
        included_snapshot = inspected_with_content["entry"]["artifacts"][0]["after"]
        self.assertIn("content_base64", included_snapshot)
        self.assertFalse(invalid_page["ok"])
        self.assertEqual(invalid_page["error"], "invalid_argument")
        self.assertTrue(reverted["ok"])
        self.assertEqual(reverted["changed"], 3)
        self.assertEqual(reverted["conflicts"], [])
        self.assertEqual(session.bridge.state, self.before)
        self.assertIsNone(session.last_region)


if __name__ == "__main__":
    unittest.main()
