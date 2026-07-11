from __future__ import annotations

import sys
import json
import tempfile
import types
import unittest
from datetime import datetime, timezone
from pathlib import Path


dotenv_stub = types.ModuleType("dotenv")
dotenv_stub.load_dotenv = lambda *args, **kwargs: False
sys.modules.setdefault("dotenv", dotenv_stub)

from picasso.core.fragment_engine import FragmentEngine
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.player_protection import PlayerProtectionEvaluator
from picasso.core.style_engine import StyleEngine
from picasso.core.write_choke import WriteChoke
from picasso.models.block import BlockPos, BlockState
from picasso.models.player_activity import BlockBounds, ProtectionArea
from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass


class WriteSafetyTests(unittest.TestCase):
    def _source(
        self,
        *,
        radius_chunks: int = 0,
        loaded_chunks: set[tuple[int, int]] | None = None,
    ) -> RegionData:
        return RegionData(
            origin_cx=0,
            origin_cz=0,
            radius_chunks=radius_chunks,
            y_min=0,
            y_max=10,
            loaded_chunks=set(loaded_chunks or {(0, 0)}),
        )

    def _choke(self, **kwargs) -> WriteChoke:
        return WriteChoke(
            safe_replaceable={
                "minecraft:chest",
                "minecraft:dirt",
                "minecraft:stone",
            },
            structural_never_touch={"minecraft:bedrock"},
            **kwargs,
        )

    def _single_change(self, pos: BlockPos, block_id: str = "minecraft:stone") -> RegionData:
        changes = RegionData(origin_cx=0, origin_cz=0, radius_chunks=1)
        changes.set(pos, BlockState.from_id(block_id))
        return changes

    def test_y_window_write_is_rejected(self) -> None:
        source = self._source()
        target = BlockPos(1, 11, 1)

        result = self._choke().validate(source, self._single_change(target))

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "outside_y_window")

    def test_xz_region_write_is_rejected(self) -> None:
        source = self._source()
        target = BlockPos(16, 5, 0)

        result = self._choke().validate(source, self._single_change(target))

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "outside_region")

    def test_write_into_unread_chunk_is_rejected(self) -> None:
        source = self._source(radius_chunks=1, loaded_chunks={(0, 0)})
        target = BlockPos(16, 5, 0)

        result = self._choke().validate(source, self._single_change(target))

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "chunk_not_loaded")

    def test_halo_write_is_rejected(self) -> None:
        target = BlockPos(1, 5, 1)
        source = self._source()
        source.halo_positions.add(target)

        result = self._choke().validate(source, self._single_change(target))

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "halo_position")

    def test_block_entity_is_unconditionally_protected(self) -> None:
        target = BlockPos(1, 5, 1)
        source = self._source()
        source.set(target, BlockState.from_id("minecraft:chest"))
        source.block_entity_positions.add(target)
        changes = self._single_change(target)
        changes.write_contexts[target] = "room_envelope"

        result = self._choke().validate(source, changes, only_safe_blocks=False)

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "block_entity_protected")

    def test_overlapping_atomic_groups_drop_the_transitive_component(self) -> None:
        source = self._source()
        first = BlockPos(1, 5, 1)
        shared = BlockPos(2, 5, 1)
        tail = BlockPos(3, 5, 1)
        raw_changes = RegionData()
        for pos in (first, shared, tail):
            raw_changes.set(pos, BlockState.from_id("minecraft:stone"))
        raw_changes.atomic_groups = [{first, shared}, {shared, tail}]

        result = self._choke(marker_positions={first}).validate(
            source,
            raw_changes,
        )

        self.assertEqual(result.changes.blocks, {})
        self.assertIn(
            {"pos": first.to_dict(), "reason": "marker_protected"},
            result.skipped,
        )
        atomic_failures = {
            (entry["pos"]["x"], entry["pos"]["y"], entry["pos"]["z"])
            for entry in result.skipped
            if entry["reason"] == "atomic_group_failed"
        }
        self.assertEqual(
            atomic_failures,
            {(shared.x, shared.y, shared.z), (tail.x, tail.y, tail.z)},
        )

    def test_player_protection_is_enforced_and_reports_snapshot_status(self) -> None:
        target = BlockPos(1, 5, 1)
        evaluator = PlayerProtectionEvaluator.from_sources(
            activity_events=(),
            protection_areas=(
                ProtectionArea(
                    "base",
                    "player_built",
                    bounds=BlockBounds(0, 2, 0, 10, 0, 2),
                ),
            ),
            as_of=datetime(2026, 7, 10, tzinfo=timezone.utc),
        )

        result = self._choke(player_protection_evaluator=evaluator).validate(
            self._source(),
            self._single_change(target),
        )

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "player_built_structure")
        self.assertEqual(result.player_protection["status"], "active")

    def test_player_override_does_not_bypass_operator_protected_region(self) -> None:
        target = BlockPos(1, 5, 1)
        evaluator = PlayerProtectionEvaluator.from_sources(
            activity_events=(),
            protection_areas=(
                ProtectionArea(
                    "operator-zone",
                    "protected_region",
                    bounds=BlockBounds(0, 2, 0, 10, 0, 2),
                    source="operator_policy",
                ),
            ),
            as_of=datetime(2026, 7, 10, tzinfo=timezone.utc),
        )

        result = self._choke(player_protection_evaluator=evaluator).validate(
            self._source(),
            self._single_change(target),
            include_player_built=True,
        )

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "protected_region")

    def test_player_override_allows_player_built_zone(self) -> None:
        target = BlockPos(1, 5, 1)
        evaluator = PlayerProtectionEvaluator.from_sources(
            activity_events=(),
            protection_areas=(
                ProtectionArea(
                    "base",
                    "player_built",
                    bounds=BlockBounds(0, 2, 0, 10, 0, 2),
                ),
            ),
            as_of=datetime(2026, 7, 10, tzinfo=timezone.utc),
        )

        result = self._choke(player_protection_evaluator=evaluator).validate(
            self._source(),
            self._single_change(target),
            include_player_built=True,
        )

        self.assertEqual(result.changed_count, 1)

    def test_region_copy_and_summary_preserve_read_safety_metadata(self) -> None:
        entity_pos = BlockPos(1, 5, 1)
        source = self._source(radius_chunks=1, loaded_chunks={(0, 0), (1, 0)})
        source.block_entity_positions.add(entity_pos)

        copied = source.copy()
        source.loaded_chunks.clear()
        source.block_entity_positions.clear()

        self.assertEqual(copied.loaded_chunks, {(0, 0), (1, 0)})
        self.assertEqual(copied.block_entity_positions, {entity_pos})
        self.assertEqual(copied.to_summary()["loaded_chunk_count"], 2)
        self.assertEqual(copied.to_summary()["block_entity_count"], 1)

    def test_missing_or_invalid_safety_policy_blocks_the_operation(self) -> None:
        source = self._source()
        target = BlockPos(1, 5, 1)
        changes = self._single_change(target)
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            paths = [root / "missing.json", root / "invalid.json"]
            paths[1].write_text("{not-json", encoding="utf-8")
            for policy_path in paths:
                with self.subTest(policy_path=policy_path.name):
                    engine = StyleEngine({}, policy_path)
                    result = WriteChoke(
                        engine.safe_replaceable,
                        engine.structural_never_touch,
                        safety_policy_error=engine.safety_policy_error,
                    ).validate(source, changes)

                    self.assertEqual(result.blocked_error, "safety_policy_unavailable")
                    self.assertEqual(result.changed_count, 0)

    def test_unknown_non_vanilla_target_is_rejected_when_catalog_is_known(self) -> None:
        source = self._source()
        target = BlockPos(1, 5, 1)
        changes = self._single_change(target, "example_mod:unknown")

        result = self._choke(
            known_block_ids={"example_mod:known"},
        ).validate(source, changes)

        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.blocked_error, "unknown_catalog_block")
        self.assertEqual(result.skipped[0]["reason"], "unknown_catalog_block")

        allowed_result = self._choke(
            known_block_ids={"example_mod:known"},
            allowed_non_vanilla_ids={"example_mod:unknown"},
        ).validate(source, changes)
        self.assertEqual(allowed_result.changed_count, 1)

    def test_style_generator_does_not_emit_above_the_read_window(self) -> None:
        source = RegionData(
            origin_cx=0,
            origin_cz=0,
            radius_chunks=0,
            y_min=0,
            y_max=0,
            loaded_chunks={(0, 0)},
        )
        anchor = BlockPos(0, 0, 0)
        source.set(anchor, BlockState.from_id("minecraft:stone"))
        source.surface_classes[anchor] = "floor"
        style_pass = StylePass.model_validate(
            {
                "name": "window_edge",
                "description": "Must not write above the read window.",
                "rules": [
                    {
                        "match": {"block": "minecraft:stone"},
                        "action": "place_adjacent",
                        "place_block": "minecraft:vine",
                        "direction": "above",
                    }
                ],
            }
        )
        with tempfile.TemporaryDirectory() as tmp:
            safe_path = Path(tmp) / "safe.json"
            safe_path.write_text(
                json.dumps(
                    {
                        "replaceable": ["minecraft:stone"],
                        "structural_never_touch": ["minecraft:bedrock"],
                    }
                ),
                encoding="utf-8",
            )
            changes = StyleEngine({style_pass.name: style_pass}, safe_path).apply(
                style_pass.name,
                source,
            )

        self.assertEqual(changes.blocks, {})

    def test_fragment_generator_rejects_footprint_outside_the_read_window(self) -> None:
        source = RegionData(
            origin_cx=0,
            origin_cz=0,
            radius_chunks=0,
            y_min=0,
            y_max=0,
            loaded_chunks={(0, 0)},
        )
        anchor = BlockPos(0, 0, 0)
        source.set(anchor, BlockState.from_id("minecraft:stone"))
        source.surface_classes[anchor] = "floor"
        source.space_classes[anchor] = "interior"
        with tempfile.TemporaryDirectory() as tmp:
            fragments_dir = Path(tmp)
            (fragments_dir / "edge_fragment.json").write_text(
                json.dumps(
                    {
                        "name": "edge_fragment",
                        "description": "Extends above the read window.",
                        "anchor_surface": "floor",
                        "footprint": "1x1",
                        "requires_clear_above": False,
                        "blocks": [
                            {
                                "offset": [0, 1, 0],
                                "block": "minecraft:vine",
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            engine = FragmentEngine(
                FragmentLibrary(fragments_dir),
                safe_replaceable={"minecraft:stone"},
                structural_never_touch={"minecraft:bedrock"},
            )
            changes = engine.apply(
                {
                    "name": "edge_pass",
                    "anchor_surface": "floor",
                    "fragments": ["edge_fragment"],
                    "density": 1.0,
                },
                source,
                seed=42,
            )

        self.assertEqual(changes.blocks, {})


if __name__ == "__main__":
    unittest.main()
