from __future__ import annotations

import json
import os
import sys
import tempfile
import types
import unittest
from pathlib import Path
from unittest.mock import patch


dotenv_stub = types.ModuleType("dotenv")
dotenv_stub.load_dotenv = lambda *args, **kwargs: False
sys.modules.setdefault("dotenv", dotenv_stub)

import picasso.core.journal as journal_module
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.session import session
from picasso.tools import npc


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


class FakeBridge:
    def __init__(
        self,
        state: BlockState = AIR,
        *,
        has_block_entity: bool = False,
        fail_read: bool = False,
        fail_write: bool = False,
    ) -> None:
        self.state = state
        self.has_block_entity = has_block_entity
        self.fail_read = fail_read
        self.fail_write = fail_write
        self.read_calls: list[tuple[int, int, int]] = []
        self.write_calls: list[dict[BlockPos, BlockState]] = []

    def read_block_with_entity(self, x: int, y: int, z: int) -> tuple[BlockState, bool]:
        self.read_calls.append((x, y, z))
        if self.fail_read:
            raise RuntimeError("read failed")
        return self.state, self.has_block_entity

    def write_region(self, changes) -> int:
        snapshot = dict(changes.blocks)
        self.write_calls.append(snapshot)
        if self.fail_write:
            raise RuntimeError("save failed")
        if snapshot:
            self.state = next(iter(snapshot.values()))
        return len(snapshot)


class NpcMarkerTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.world_path = Path(self.temp_dir.name)
        self.original = {
            "bridge": session.bridge,
            "world_path": session.world_path,
            "pass_registry": session.pass_registry,
            "pattern_matcher": session.pattern_matcher,
            "fragment_library": session.fragment_library,
            "last_region": session.last_region,
            "journal": session.journal,
            "journal_status": session.journal_status,
            "journal_error": session.journal_error,
            "noise_backend": session.noise_backend,
        }
        session.world_path = self.world_path
        session.pass_registry = {}
        session.pattern_matcher = None
        session.fragment_library = None
        session.last_region = object()
        session.journal = None
        session.journal_status = "active"
        session.journal_error = None
        session.noise_backend = "fallback"

        mcp = FakeMCP()
        npc.register(mcp)
        self.place_marker = mcp.tools["place_npc_marker"]

    def tearDown(self) -> None:
        for key, value in self.original.items():
            setattr(session, key, value)
        self.temp_dir.cleanup()

    def test_success_uses_write_region_and_commits_metadata(self) -> None:
        bridge = FakeBridge()
        session.bridge = bridge

        result = self.place_marker(
            x=3,
            y=70,
            z=-4,
            npc_type="key_npc",
            faction="survivor_camp",
        )

        self.assertTrue(result["ok"])
        self.assertEqual(len(bridge.write_calls), 1)
        written = bridge.write_calls[0]
        self.assertEqual(written[BlockPos(3, 70, -4)].full_id, "minecraft:structure_void")
        self.assertEqual(bridge.state.full_id, "minecraft:structure_void")
        marker_path = Path(result["marker_file"])
        self.assertTrue(marker_path.exists())
        payload = json.loads(marker_path.read_text(encoding="utf-8"))
        self.assertEqual(payload["pos"], {"x": 3, "y": 70, "z": -4})
        self.assertEqual(marker_path.name, "3_70_n4.json")
        self.assertIsNone(session.last_region)
        self.assertEqual(result["journal_entry"]["status"], "committed")
        self._assert_provenance(result)

    def test_occupied_target_is_rejected_without_write(self) -> None:
        bridge = FakeBridge(BlockState.from_id("minecraft:stone"))
        session.bridge = bridge

        result = self.place_marker(1, 64, 2, "ambient", "neutral")

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_target_occupied")
        self.assertEqual(result["found"], "minecraft:stone")
        self.assertEqual(bridge.write_calls, [])
        self._assert_provenance(result)

    def test_block_entity_target_is_rejected_without_write(self) -> None:
        bridge = FakeBridge(
            BlockState.from_id("minecraft:chest"),
            has_block_entity=True,
        )
        session.bridge = bridge

        result = self.place_marker(1, 64, 2, "vendor", "survivor")

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_target_has_block_entity")
        self.assertEqual(bridge.write_calls, [])
        self._assert_provenance(result)

    def test_target_read_failure_is_not_treated_as_air(self) -> None:
        bridge = FakeBridge(fail_read=True)
        session.bridge = bridge

        with patch.object(npc.logger, "exception"):
            result = self.place_marker(1, 64, 2, "ambient", "neutral")

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_target_read_failed")
        self.assertEqual(bridge.write_calls, [])
        self._assert_provenance(result)

    def test_existing_marker_is_rejected_before_world_read(self) -> None:
        bridge = FakeBridge()
        session.bridge = bridge
        marker_dir = self.world_path / "picasso_markers"
        marker_dir.mkdir()
        marker_path = marker_dir / "5_70_6.json"
        marker_path.write_text(
            json.dumps({"pos": {"x": 5, "y": 70, "z": 6}}),
            encoding="utf-8",
        )

        result = self.place_marker(5, 70, 6, "ambient", "neutral")

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_already_exists")
        self.assertEqual(bridge.read_calls, [])
        self.assertEqual(bridge.write_calls, [])

    def test_metadata_commit_failure_rolls_world_back(self) -> None:
        bridge = FakeBridge()
        session.bridge = bridge
        journal = session.require_journal()

        with (
            patch.object(
                journal,
                "_apply_artifact",
                side_effect=OSError("injected artifact commit failure"),
            ),
            patch.object(npc.logger, "exception"),
        ):
            result = self.place_marker(8, 65, 9, "enemy", "infected")

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_metadata_write_failed")
        self.assertTrue(result["rollback_succeeded"])
        self.assertIsNone(result["rollback_error"])
        self.assertEqual(len(bridge.write_calls), 2)
        self.assertEqual(
            bridge.write_calls[0][BlockPos(8, 65, 9)].full_id,
            "minecraft:structure_void",
        )
        self.assertTrue(bridge.write_calls[1][BlockPos(8, 65, 9)].is_air)
        self.assertTrue(bridge.state.is_air)
        marker_dir = self.world_path / "picasso_markers"
        self.assertFalse((marker_dir / "8_65_9.json").exists())
        self.assertEqual(list(marker_dir.glob("*.tmp")), [])
        entries = session.require_journal().list_entries()["entries"]
        self.assertEqual(entries[0]["status"], "failed")
        detail = session.require_journal().inspect(entries[0]["operation_id"])
        self.assertEqual(detail["failure"]["stage"], "artifact_write")
        self.assertTrue(detail["failure"]["rolled_back"])
        self._assert_provenance(result)

    def test_world_save_failure_is_not_reported_as_success(self) -> None:
        bridge = FakeBridge(fail_write=True)
        session.bridge = bridge

        with patch.object(npc.logger, "exception"):
            result = self.place_marker(2, 63, 4, "ambient", "neutral")

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_world_write_failed")
        self.assertGreaterEqual(len(bridge.write_calls), 1)
        self.assertFalse((self.world_path / "picasso_markers" / "2_63_4.json").exists())
        self._assert_provenance(result)

    def test_external_marker_create_race_is_preserved_and_world_rolls_back(self) -> None:
        bridge = FakeBridge()
        session.bridge = bridge
        session.require_journal()
        external_content = b'{"created_by": "external"}'
        real_link = os.link

        def racing_link(source, destination):
            destination = Path(destination)
            if destination.parent.name == "picasso_markers":
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(external_content)
            return real_link(source, destination)

        with (
            patch.object(journal_module, "_OS_LINK", side_effect=racing_link),
            patch.object(npc.logger, "exception"),
        ):
            result = self.place_marker(6, 66, 7, "ambient", "neutral")

        marker_path = self.world_path / "picasso_markers" / "6_66_7.json"
        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "marker_metadata_write_failed")
        self.assertFalse(result["rollback_succeeded"])
        self.assertTrue(bridge.state.is_air)
        self.assertEqual(marker_path.read_bytes(), external_content)

    def _assert_provenance(self, result: dict) -> None:
        self.assertEqual(result["journal_status"], "active")
        self.assertIsNone(result["reversibility_warning"])
        self.assertEqual(result["space_classification"], "heuristic")
        self.assertEqual(result["noise_backend"], "fallback")
        self.assertEqual(result["player_protection"], "unavailable")
        self.assertIn("placements_skipped", result)


if __name__ == "__main__":
    unittest.main()
