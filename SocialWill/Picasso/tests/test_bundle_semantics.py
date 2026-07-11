from __future__ import annotations

import json
import sys
import tempfile
import types
import unittest
from pathlib import Path


dotenv_stub = types.ModuleType("dotenv")
dotenv_stub.load_dotenv = lambda *args, **kwargs: False
sys.modules.setdefault("dotenv", dotenv_stub)

from picasso.core.bundle_executor import BundleExecutor, _merge_changes
from picasso.core.journal import Journal
from picasso.core.style_engine import StyleEngine
from picasso.core.write_choke import WriteChoke
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass


class BundleSemanticsTests(unittest.TestCase):
    def _region(self) -> RegionData:
        region = RegionData(
            origin_cx=0,
            origin_cz=0,
            radius_chunks=0,
            y_min=0,
            y_max=5,
            loaded_chunks={(0, 0)},
        )
        region.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:stone"))
        region.set(BlockPos(0, 1, 0), BlockState.from_id("minecraft:dirt"))
        return region

    def test_merge_air_restores_sparse_world_state_and_clears_metadata(self) -> None:
        region = self._region()
        target = BlockPos(0, 1, 0)
        region.surface_classes[target] = "rooftop"
        region.space_classes[target] = "exterior"
        region.write_contexts[target] = "decoration"
        region.destructive_positions.add(target)
        region.block_entity_positions.add(target)
        region.atomic_groups.append({target, BlockPos(0, 0, 0)})
        changes = RegionData()
        changes.set(target, AIR)

        _merge_changes(region, changes)

        self.assertNotIn(target, region.blocks)
        self.assertNotIn(target, region.surface_classes)
        self.assertNotIn(target, region.space_classes)
        self.assertNotIn(target, region.write_contexts)
        self.assertNotIn(target, region.destructive_positions)
        self.assertNotIn(target, region.block_entity_positions)
        self.assertTrue(all(target not in group for group in region.atomic_groups))

    def test_later_pass_can_place_into_air_cleared_by_earlier_pass(self) -> None:
        remove_pass = StylePass.model_validate(
            {
                "name": "remove_obstruction",
                "description": "Clear the block above the anchor.",
                "destructive": True,
                "rules": [
                    {
                        "match": {"block": "minecraft:dirt"},
                        "action": "remove",
                    }
                ],
            }
        )
        place_pass = StylePass.model_validate(
            {
                "name": "place_after_clear",
                "description": "Use the newly-cleared air position.",
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
            safe_path = Path(tmp) / "safe_blocks.json"
            safe_path.write_text(
                json.dumps(
                    {
                        "replaceable": ["minecraft:dirt", "minecraft:stone"],
                        "structural_never_touch": ["minecraft:bedrock"],
                    }
                ),
                encoding="utf-8",
            )
            engine = StyleEngine(
                {
                    remove_pass.name: remove_pass,
                    place_pass.name: place_pass,
                },
                safe_path,
            )
            choke = WriteChoke(
                engine.safe_replaceable,
                engine.structural_never_touch,
                safety_policy_error=engine.safety_policy_error,
            )
            bundle = {
                "name": "layering_test",
                "entries": [
                    {
                        "structure_type": "test",
                        "passes": [
                            {"name": remove_pass.name},
                            {"name": place_pass.name},
                        ],
                    }
                ],
            }

            result = BundleExecutor(engine, choke).execute_region(
                bundle,
                self._region(),
                seed=42,
                dry_run=True,
                structure_type_filter=None,
            )

        self.assertTrue(result.ok)
        self.assertEqual(result.errors, [])
        self.assertEqual(result.total_changed, 2)
        self.assertEqual(
            [entry["would_change"] for entry in result.passes_applied],
            [1, 1],
        )

    def test_non_dry_bundle_journals_each_pass_as_a_separate_operation(self) -> None:
        first_pos = BlockPos(0, 0, 0)
        second_pos = BlockPos(0, 1, 0)
        before = {
            first_pos: BlockState.from_id("minecraft:stone"),
            second_pos: BlockState.from_id("minecraft:dirt"),
        }

        class Bridge:
            dimension = "minecraft:overworld"

            def __init__(self):
                self.states = dict(before)

            def read_block_with_entity(self, x, y, z):
                return self.states.get(BlockPos(x, y, z), AIR), False

            def write_region(self, changes):
                self.states.update(changes.blocks)
                return len(changes.blocks)

        class Engine:
            pass_registry = {
                "first": types.SimpleNamespace(only_safe_blocks=False),
                "second": types.SimpleNamespace(only_safe_blocks=False),
            }

            def apply(self, pass_name, _region, _intensity, _seed, _space_filter):
                changes = RegionData()
                if pass_name == "first":
                    changes.set(first_pos, BlockState.from_id("minecraft:andesite"))
                else:
                    changes.set(second_pos, BlockState.from_id("minecraft:coarse_dirt"))
                return changes

        class Choke:
            def validate(self, _source, changes, **_kwargs):
                return types.SimpleNamespace(
                    blocked_error=None,
                    blocked_message=None,
                    changes=changes,
                    changed_count=len(changes.blocks),
                    modded_positions=[],
                    skipped=[],
                )

        bundle = {
            "name": "journal_bundle",
            "entries": [
                {
                    "structure_type": "test",
                    "passes": [{"name": "first"}, {"name": "second"}],
                }
            ],
        }

        with tempfile.TemporaryDirectory() as tmp:
            bridge = Bridge()
            journal = Journal(Path(tmp), bridge, bridge.dimension)
            result = BundleExecutor(
                Engine(), Choke(), bridge=bridge, journal=journal
            ).execute_region(
                bundle,
                self._region(),
                seed=42,
                dry_run=False,
                structure_type_filter=None,
            )

            entries = journal.list_entries()["entries"]
            self.assertTrue(result.ok)
            self.assertEqual(len(result.passes_applied), 2)
            self.assertEqual(len(entries), 2)
            self.assertEqual({entry["pass"] for entry in entries}, {"first", "second"})
            self.assertTrue(
                all(
                    item["journal_entry"]["status"] == "committed"
                    for item in result.passes_applied
                )
            )

            reverted = journal.revert_last()

            self.assertEqual(reverted["changed"], 1)
            self.assertEqual(bridge.states[second_pos], before[second_pos])
            self.assertEqual(bridge.states[first_pos].full_id, "minecraft:andesite")


if __name__ == "__main__":
    unittest.main()
