from __future__ import annotations

import json
import os
import tempfile
import uuid
import unittest
from pathlib import Path
from unittest.mock import patch

import picasso.core.journal as journal_module
from picasso.core.journal import (
    Journal,
    JournalArtifact,
    JournalCorruptError,
    JournalRevertError,
    JournalUnavailableError,
    JournalWorldMismatchError,
    JournalWriteError,
)
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData


class FakeBridge:
    dimension = "minecraft:overworld"

    def __init__(self, states: dict[BlockPos, BlockState]) -> None:
        self.states = dict(states)
        self.entities: set[BlockPos] = set()
        self.write_calls: list[dict[BlockPos, BlockState]] = []
        self.fail_next_write = False
        self.corrupt_next_write = False

    def read_block_with_entity(self, x: int, y: int, z: int):
        pos = BlockPos(x, y, z)
        return self.states.get(pos, AIR), pos in self.entities

    def write_region(self, changes: RegionData) -> int:
        snapshot = dict(changes.blocks)
        self.write_calls.append(snapshot)
        if self.fail_next_write:
            self.fail_next_write = False
            raise RuntimeError("injected save failure")
        corrupt = self.corrupt_next_write
        self.corrupt_next_write = False
        for pos, state in snapshot.items():
            if corrupt:
                self.states[pos] = BlockState.from_id(
                    "minecraft:barrier", {"injected": "true"}
                )
            else:
                self.states[pos] = state
        return len(snapshot)


class JournalTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.world = Path(self.temp_dir.name)
        self.a = BlockPos(1, 64, 1)
        self.b = BlockPos(2, 64, 1)
        self.before_a = BlockState.from_id("minecraft:oak_log", {"axis": "x"})
        self.before_b = BlockState.from_id("minecraft:dirt", {"snowy": "false"})
        self.after_a = BlockState.from_id("minecraft:spruce_log", {"axis": "z"})
        self.after_b = BlockState.from_id("minecraft:coarse_dirt")
        self.bridge = FakeBridge({self.a: self.before_a, self.b: self.before_b})
        self.journal = Journal(self.world, self.bridge, self.bridge.dimension)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def _changes(self, *, two: bool = False) -> RegionData:
        changes = RegionData()
        changes.set(self.a, self.after_a)
        if two:
            changes.set(self.b, self.after_b)
        return changes

    def _apply(self, *, two: bool = False):
        return self.journal.apply(
            self._changes(two=two),
            tool="apply_pass",
            pass_name="test_pass",
            seed=17,
            argument_summary={"two": two},
        )

    def _set_status(self, operation_id: str, status: str) -> None:
        path = self.journal._find_entry_path(operation_id)
        entry = self.journal._load_entry(path)
        entry["status"] = status
        self.journal._write_entry(path, entry)

    def test_apply_list_inspect_and_revert_round_trip_exact_state(self) -> None:
        result = self._apply()

        self.assertEqual(result.status, "committed")
        self.assertEqual(self.bridge.states[self.a], self.after_a)
        listing = self.journal.list_entries()
        self.assertEqual(listing["corrupt_entries"], [])
        self.assertEqual(listing["entries"][0]["operation_id"], result.operation_id)
        entry = self.journal.inspect(result.operation_id)
        self.assertEqual(entry["changes"][0]["before"], {
            "id": "minecraft:oak_log",
            "properties": {"axis": "x"},
        })
        self.assertEqual(entry["changes"][0]["after"], {
            "id": "minecraft:spruce_log",
            "properties": {"axis": "z"},
        })

        reverted = self.journal.revert_last()

        self.assertEqual(reverted["changed"], 1)
        self.assertEqual(reverted["conflicts"], [])
        self.assertEqual(self.bridge.states[self.a], self.before_a)
        self.assertEqual(self.journal.inspect(result.operation_id)["status"], "reverted")

    def test_revert_skips_conflict_and_writes_other_positions_as_one_batch(self) -> None:
        result = self._apply(two=True)
        external = BlockState.from_id("minecraft:diamond_block")
        self.bridge.states[self.b] = external
        write_count_before = len(self.bridge.write_calls)

        reverted = self.journal.revert_last()

        self.assertEqual(reverted["changed"], 1)
        self.assertEqual(reverted["skipped"], 1)
        self.assertEqual(len(reverted["conflicts"]), 1)
        self.assertEqual(reverted["conflicts"][0]["pos"], self.b.to_dict())
        self.assertEqual(reverted["conflicts"][0]["expected_after"]["id"], self.after_b.full_id)
        self.assertEqual(reverted["conflicts"][0]["found"]["id"], external.full_id)
        self.assertEqual(len(self.bridge.write_calls), write_count_before + 1)
        self.assertEqual(set(self.bridge.write_calls[-1]), {self.a})
        self.assertEqual(self.bridge.states[self.a], self.before_a)
        self.assertEqual(self.bridge.states[self.b], external)
        self.assertEqual(self.journal.inspect(result.operation_id)["status"], "reverted")

    def test_pending_before_world_write_is_already_reverted_not_conflict(self) -> None:
        result = self._apply()
        self.bridge.states[self.a] = self.before_a
        self._set_status(result.operation_id, "pending")
        write_count_before = len(self.bridge.write_calls)

        reverted = self.journal.revert_last()

        self.assertEqual(reverted["changed"], 0)
        self.assertEqual(reverted["already_reverted"], 1)
        self.assertEqual(reverted["conflicts"], [])
        self.assertEqual(len(self.bridge.write_calls), write_count_before)

    def test_partially_applied_pending_reverts_after_positions_only(self) -> None:
        result = self._apply(two=True)
        self.bridge.states[self.a] = self.before_a
        self._set_status(result.operation_id, "pending")

        reverted = self.journal.revert_last()

        self.assertEqual(reverted["changed"], 1)
        self.assertEqual(reverted["already_reverted"], 1)
        self.assertEqual(reverted["conflicts"], [])
        self.assertEqual(set(self.bridge.write_calls[-1]), {self.b})
        self.assertEqual(self.bridge.states[self.a], self.before_a)
        self.assertEqual(self.bridge.states[self.b], self.before_b)

    def test_corrupt_newest_entry_is_reported_and_revert_fails_closed(self) -> None:
        self._apply()
        corrupt_path = self.journal.directory / "zzzz_corrupt.json"
        corrupt_path.write_text('{"truncated":', encoding="utf-8")

        listing = self.journal.list_entries()

        self.assertEqual(len(listing["entries"]), 1)
        self.assertEqual(len(listing["corrupt_entries"]), 1)
        with self.assertRaises(JournalCorruptError):
            self.journal.revert_last()

    def test_save_failure_leaves_failed_reverse_diff_and_original_world(self) -> None:
        self.bridge.fail_next_write = True

        with self.assertRaises(JournalWriteError) as raised:
            self._apply()

        self.assertEqual(raised.exception.stage, "world_write")
        self.assertTrue(raised.exception.rollback_succeeded)
        self.assertEqual(self.bridge.states[self.a], self.before_a)
        entry = self.journal.list_entries()["entries"][0]
        self.assertEqual(entry["status"], "failed")
        detail = self.journal.inspect(entry["operation_id"])
        self.assertTrue(detail["failure"]["rolled_back"])

    def test_commit_status_failure_retains_recoverable_pending_diff(self) -> None:
        original = self.journal._write_entry

        def fail_committed(path, entry, *, create=False):
            if entry.get("status") == "committed":
                raise OSError("injected commit status failure")
            return original(path, entry, create=create)

        with patch.object(self.journal, "_write_entry", side_effect=fail_committed):
            result = self._apply()

        self.assertEqual(result.status, "pending")
        self.assertIsNotNone(result.warning)
        self.assertEqual(self.journal.inspect(result.operation_id)["status"], "pending")
        reverted = self.journal.revert_last()
        self.assertEqual(reverted["changed"], 1)
        self.assertEqual(self.bridge.states[self.a], self.before_a)

    def test_apply_readback_mismatch_rolls_back_and_marks_failed(self) -> None:
        self.bridge.corrupt_next_write = True

        with self.assertRaises(JournalWriteError) as raised:
            self._apply()

        self.assertEqual(raised.exception.stage, "world_verify")
        self.assertTrue(raised.exception.rollback_succeeded)
        self.assertEqual(self.bridge.states[self.a], self.before_a)
        entry = self.journal.list_entries()["entries"][0]
        self.assertEqual(entry["status"], "failed")

    def test_revert_readback_mismatch_explicitly_restores_after_state(self) -> None:
        result = self._apply()
        self.bridge.corrupt_next_write = True

        with self.assertRaises(JournalRevertError) as raised:
            self.journal.revert_last()

        self.assertTrue(raised.exception.details["rollback_succeeded"])
        self.assertEqual(self.bridge.states[self.a], self.after_a)
        entry = self.journal.inspect(result.operation_id)
        self.assertEqual(entry["status"], "committed")
        self.assertEqual(entry["last_revert_failure"]["stage"], "world_verify")

    def test_replacing_identity_at_same_path_rejects_old_journal(self) -> None:
        result = self._apply()
        replacement = {
            "schema_version": 1,
            "world_uuid": uuid.uuid4().hex,
        }
        self.journal.identity_path.write_text(json.dumps(replacement), encoding="utf-8")

        with self.assertRaises(JournalWorldMismatchError):
            self.journal.inspect(result.operation_id)

        replacement_journal = Journal(self.world, self.bridge, self.bridge.dimension)
        with self.assertRaises(JournalWorldMismatchError):
            replacement_journal.inspect(result.operation_id)

    def test_compound_artifact_round_trip_removes_marker_without_ghost(self) -> None:
        artifact = JournalArtifact.json_file(
            "picasso_markers/1_64_1.json",
            {"pos": self.a.to_dict(), "npc_type": "ambient"},
            compound_positions=(self.a,),
            must_not_exist=True,
        )
        result = self.journal.apply(
            self._changes(),
            tool="place_npc_marker",
            argument_summary={"pos": self.a.to_dict()},
            artifacts=(artifact,),
        )
        marker_path = self.world / artifact.relative_path
        self.assertTrue(marker_path.exists())

        reverted = self.journal.revert_last(result.operation_id)

        self.assertEqual(reverted["changed"], 1)
        self.assertEqual(reverted["artifacts_reverted"], 1)
        self.assertFalse(marker_path.exists())
        self.assertEqual(self.bridge.states[self.a], self.before_a)

    def test_compound_artifact_conflict_skips_bound_block_as_one_group(self) -> None:
        artifact = JournalArtifact.json_file(
            "picasso_markers/1_64_1.json",
            {"pos": self.a.to_dict(), "npc_type": "ambient"},
            compound_positions=(self.a,),
            must_not_exist=True,
        )
        result = self.journal.apply(
            self._changes(),
            tool="place_npc_marker",
            artifacts=(artifact,),
        )
        marker_path = self.world / artifact.relative_path
        external_bytes = b'{"external": true}'
        marker_path.write_bytes(external_bytes)
        write_count_before = len(self.bridge.write_calls)

        reverted = self.journal.revert_last(result.operation_id)

        self.assertEqual(reverted["changed"], 0)
        self.assertEqual(reverted["skipped"], 1)
        self.assertEqual(reverted["conflicts"][0]["reason"], "companion_artifact_conflict")
        self.assertEqual(len(self.bridge.write_calls), write_count_before)
        self.assertEqual(self.bridge.states[self.a], self.after_a)
        self.assertEqual(marker_path.read_bytes(), external_bytes)

    def test_companion_artifact_path_must_stay_inside_world(self) -> None:
        artifact = JournalArtifact.json_file(
            "../escaped.json",
            {"unsafe": True},
            compound_positions=(self.a,),
        )

        with self.assertRaises(JournalCorruptError):
            self.journal.apply(
                self._changes(),
                tool="place_npc_marker",
                artifacts=(artifact,),
            )

        self.assertEqual(self.bridge.write_calls, [])
        self.assertFalse((self.world.parent / "escaped.json").exists())

    def test_corrupt_existing_identity_is_unavailable_and_not_overwritten(self) -> None:
        other_world = self.world / "corrupt_identity_world"
        other_world.mkdir()
        identity_path = other_world / "picasso_world_id.json"
        original = b'{"schema_version": 1, "world_uuid": "not-a-uuid"}'
        identity_path.write_bytes(original)

        with self.assertRaises(JournalUnavailableError):
            Journal(other_world, self.bridge, self.bridge.dimension)

        self.assertEqual(identity_path.read_bytes(), original)

    def test_exclusive_atomic_create_never_overwrites_racing_external_file(self) -> None:
        target = self.world / "exclusive.json"
        external = b"external-writer-content"
        real_link = os.link

        def racing_link(source, destination):
            Path(destination).write_bytes(external)
            return real_link(source, destination)

        with (
            patch.object(journal_module, "_OS_LINK", side_effect=racing_link),
            self.assertRaises(FileExistsError),
        ):
            journal_module._atomic_replace_bytes(target, b"picasso", create=True)

        self.assertEqual(target.read_bytes(), external)

    def test_journal_directory_symlink_outside_world_is_unavailable(self) -> None:
        symlink_world = self.world / "symlink_world"
        outside = self.world / "outside_journal"
        symlink_world.mkdir()
        outside.mkdir()
        link = symlink_world / "picasso_journal"
        try:
            os.symlink(outside, link, target_is_directory=True)
        except (OSError, NotImplementedError) as exc:
            self.skipTest(f"Directory symlinks unavailable on this host: {exc}")

        with self.assertRaises(JournalUnavailableError):
            Journal(symlink_world, self.bridge, self.bridge.dimension)


if __name__ == "__main__":
    unittest.main()
