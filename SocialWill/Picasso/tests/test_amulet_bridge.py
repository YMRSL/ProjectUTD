from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from typing import Any
from unittest.mock import patch

from picasso.core.amulet_bridge import AmuletBridge
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData

try:
    import amulet
    from amulet.api.block import Block
    from amulet.api.block_entity import BlockEntity
    from amulet.api.entity import Entity
    from amulet.level.formats.anvil_world.format import AnvilFormat
    from amulet_nbt import CompoundTag, NamedTag, StringTag

    AMULET_AVAILABLE = True
except ImportError:  # pragma: no cover - the project declares Amulet as a dependency
    AMULET_AVAILABLE = False


JAVA_VERSION = (1, 21, 1)
OVERWORLD = "minecraft:overworld"


def _tagged(properties: dict[str, str]) -> dict[str, Any]:
    return {key: StringTag(value) for key, value in properties.items()}


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
        level.set_version_block(
            1,
            64,
            0,
            OVERWORLD,
            ("java", JAVA_VERSION),
            Block(
                "minecraft",
                "oak_stairs",
                _tagged(
                    {
                        "facing": "north",
                        "half": "bottom",
                        "shape": "straight",
                        "waterlogged": "false",
                    }
                ),
            ),
            None,
        )
        level.set_version_block(
            4,
            64,
            4,
            OVERWORLD,
            ("java", JAVA_VERSION),
            Block(
                "minecraft",
                "chest",
                _tagged(
                    {
                        "facing": "north",
                        "type": "single",
                        "waterlogged": "false",
                    }
                ),
            ),
            BlockEntity(
                "minecraft",
                "chest",
                4,
                64,
                4,
                NamedTag(CompoundTag()),
            ),
        )
        level.save()
    finally:
        level.close()
    return world_path


@unittest.skipUnless(AMULET_AVAILABLE, "amulet-core is required")
class AmuletBridgeWorldTests(unittest.TestCase):
    def test_versioned_read_returns_canonical_java_ids_and_metadata(self) -> None:
        with tempfile.TemporaryDirectory(prefix="picasso-amulet-test-") as tmp:
            world_path = _create_java_world(Path(tmp))
            bridge = AmuletBridge(world_path)
            try:
                self.assertEqual(
                    bridge._version_identifier,
                    bridge.level.level_wrapper.max_world_version,
                )
                self.assertNotEqual(bridge.version, "unknown")

                planks, has_block_entity = bridge.read_block_with_entity(0, 64, 0)
                self.assertEqual(planks.full_id, "minecraft:oak_planks")
                self.assertFalse(has_block_entity)

                stairs, has_block_entity = bridge.read_block_with_entity(1, 64, 0)
                self.assertEqual(stairs.full_id, "minecraft:oak_stairs")
                self.assertEqual(stairs.properties["facing"], "north")
                self.assertEqual(stairs.properties["half"], "bottom")
                self.assertFalse(has_block_entity)

                chest, has_block_entity = bridge.read_block_with_entity(4, 64, 4)
                self.assertEqual(chest.full_id, "minecraft:chest")
                self.assertTrue(has_block_entity)

                region = bridge.read_region(0, 0, 1, y_min=64, y_max=64)
                self.assertEqual(region.chunks_read, 1)
                self.assertEqual(region.chunks_missing, 8)
                self.assertEqual(region.loaded_chunks, {(0, 0)})
                self.assertIn(BlockPos(4, 64, 4), region.block_entity_positions)
                self.assertEqual(
                    region.get(BlockPos(0, 64, 0)).full_id,
                    "minecraft:oak_planks",
                )
            finally:
                bridge.close()

    def test_sparse_region_read_uses_palette_and_falls_back_only_for_block_entity(self) -> None:
        with tempfile.TemporaryDirectory(prefix="picasso-amulet-fast-read-") as tmp:
            world_path = _create_java_world(Path(tmp))
            bridge = AmuletBridge(world_path)
            try:
                with patch.object(
                    bridge,
                    "read_block_with_entity",
                    wraps=bridge.read_block_with_entity,
                ) as canonical_read:
                    region = bridge.read_region(0, 0, 0, y_min=64, y_max=64)

                self.assertEqual(canonical_read.call_count, 1)
                self.assertEqual((region.y_min, region.y_max), (64, 64))
                self.assertEqual((region.read_y_min, region.read_y_max), (63, 68))
                self.assertEqual(
                    region.get(BlockPos(0, 64, 0)).full_id,
                    "minecraft:oak_planks",
                )
                stairs = region.get(BlockPos(1, 64, 0))
                self.assertEqual(stairs.full_id, "minecraft:oak_stairs")
                self.assertEqual(stairs.properties["facing"], "north")
                self.assertEqual(stairs.properties["half"], "bottom")
                self.assertEqual(stairs.properties["shape"], "straight")
                self.assertIn(BlockPos(4, 64, 4), region.block_entity_positions)
            finally:
                bridge.close()

    def test_unknown_mod_palette_id_survives_sparse_save_reopen_region_read(self) -> None:
        with tempfile.TemporaryDirectory(prefix="picasso-amulet-fast-dd-") as tmp:
            world_path = _create_java_world(Path(tmp))
            target = BlockPos(3, 65, 3)
            changes = RegionData()
            changes.set(target, BlockState.from_id("doomsday_decoration:chair"))
            bridge = AmuletBridge(world_path)
            try:
                self.assertEqual(bridge.write_region(changes), 1)
            finally:
                bridge.close()

            reopened = AmuletBridge(world_path)
            try:
                with patch.object(
                    reopened,
                    "read_block_with_entity",
                    wraps=reopened.read_block_with_entity,
                ) as canonical_read:
                    region = reopened.read_region(0, 0, 0, y_min=65, y_max=65)

                self.assertEqual(region.get(target).full_id, "doomsday_decoration:chair")
                # The only canonical call is the chest block entity at y=64;
                # the unknown mod palette itself stays on the sparse path.
                self.assertEqual(canonical_read.call_count, 1)
            finally:
                reopened.close()

    def test_property_blocks_survive_write_save_and_reopen(self) -> None:
        with tempfile.TemporaryDirectory(prefix="picasso-amulet-test-") as tmp:
            world_path = _create_java_world(Path(tmp))
            bridge = AmuletBridge(world_path)
            changes = RegionData()
            changes.set(
                BlockPos(2, 64, 2),
                BlockState("minecraft", "oak_log", {"axis": "x"}),
            )
            changes.set(
                BlockPos(3, 64, 3),
                BlockState(
                    "minecraft",
                    "oak_stairs",
                    {
                        "facing": "east",
                        "half": "top",
                        "shape": "straight",
                        "waterlogged": "false",
                    },
                ),
            )
            changes.set(
                BlockPos(5, 64, 5),
                BlockState(
                    "minecraft",
                    "oak_leaves",
                    {
                        "distance": "7",
                        "persistent": "true",
                        "waterlogged": "false",
                    },
                ),
            )
            try:
                self.assertEqual(bridge.write_region(changes), 3)
                bridge.place_block(6, 64, 6, BlockState.from_id("minecraft:stone"))
            finally:
                bridge.close()

            reopened = AmuletBridge(world_path)
            try:
                log, _ = reopened.read_block_with_entity(2, 64, 2)
                stairs, _ = reopened.read_block_with_entity(3, 64, 3)
                leaves, _ = reopened.read_block_with_entity(5, 64, 5)
                placed, _ = reopened.read_block_with_entity(6, 64, 6)
                self.assertEqual(log.full_id, "minecraft:oak_log")
                self.assertEqual(log.properties["axis"], "x")
                self.assertEqual(stairs.full_id, "minecraft:oak_stairs")
                self.assertEqual(stairs.properties["facing"], "east")
                self.assertEqual(stairs.properties["half"], "top")
                self.assertEqual(leaves.full_id, "minecraft:oak_leaves")
                self.assertEqual(leaves.properties["persistent"], "true")
                self.assertEqual(placed.full_id, "minecraft:stone")
            finally:
                reopened.close()

    def test_universal_block_is_not_mislabeled_as_canonical_minecraft(self) -> None:
        bridge = object.__new__(AmuletBridge)
        universal = Block(
            "universal_minecraft",
            "planks",
            {"material": StringTag("oak")},
        )
        with self.assertRaisesRegex(ValueError, "universal block"):
            bridge._block_to_state(universal)

        entity = Entity(
            "minecraft",
            "item_frame",
            0.0,
            64.0,
            0.0,
            NamedTag(CompoundTag()),
        )
        with self.assertRaisesRegex(TypeError, "Entity"):
            bridge._block_to_state(entity)


class _FakeLevel:
    def __init__(self) -> None:
        self.blocks: dict[tuple[int, int, int], Any] = {
            (0, 0, 0): "old-a",
            (1, 0, 0): "old-b",
        }
        self.set_calls: list[tuple[tuple[int, int, int], Any]] = []
        self.save_calls = 0
        self.failed_once = False

    def get_version_block(self, x, y, z, _dimension, _version):
        return self.blocks[(x, y, z)], None

    def set_version_block(self, x, y, z, _dimension, _version, block, _block_entity):
        key = (x, y, z)
        self.set_calls.append((key, block))
        self.blocks[key] = block
        if block == "minecraft:bad" and not self.failed_once:
            self.failed_once = True
            raise RuntimeError("simulated setter failure after mutation")

    def save(self) -> None:
        self.save_calls += 1


def _fake_bridge(level: Any) -> AmuletBridge:
    bridge = object.__new__(AmuletBridge)
    bridge.level = level
    bridge.dimension = OVERWORLD
    bridge._version_identifier = ("java", JAVA_VERSION)
    bridge._write_poisoned = False
    return bridge


class AmuletBridgeAtomicityTests(unittest.TestCase):
    def test_all_targets_are_preconstructed_before_any_mutation(self) -> None:
        level = _FakeLevel()
        bridge = _fake_bridge(level)
        calls = 0

        def convert(state: BlockState) -> str:
            nonlocal calls
            calls += 1
            if calls == 2:
                raise ValueError("invalid block properties")
            return state.full_id

        bridge._state_to_amulet_block = convert
        changes = RegionData()
        changes.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:stone"))
        changes.set(BlockPos(1, 0, 0), BlockState.from_id("minecraft:bad"))

        with self.assertRaisesRegex(ValueError, "invalid block properties"):
            bridge.write_region(changes)

        self.assertEqual(level.set_calls, [])
        self.assertEqual(level.blocks[(0, 0, 0)], "old-a")
        self.assertEqual(level.blocks[(1, 0, 0)], "old-b")

    def test_failed_write_rolls_back_every_attempted_position(self) -> None:
        level = _FakeLevel()
        bridge = _fake_bridge(level)
        bridge._state_to_amulet_block = lambda state: state.full_id
        changes = RegionData()
        changes.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:stone"))
        changes.set(BlockPos(1, 0, 0), BlockState.from_id("minecraft:bad"))

        with self.assertRaisesRegex(RuntimeError, "rollback completed"):
            bridge.write_region(changes)

        self.assertEqual(level.blocks[(0, 0, 0)], "old-a")
        self.assertEqual(level.blocks[(1, 0, 0)], "old-b")
        self.assertEqual(level.save_calls, 1)

    def test_incomplete_rollback_poisons_bridge_and_blocks_later_writes(self) -> None:
        class IncompleteRollbackLevel(_FakeLevel):
            def set_version_block(
                self, x, y, z, dimension, version, block, block_entity
            ):
                if self.failed_once and block == "old-b":
                    raise RuntimeError("simulated rollback failure")
                super().set_version_block(
                    x, y, z, dimension, version, block, block_entity
                )

        level = IncompleteRollbackLevel()
        bridge = _fake_bridge(level)
        bridge._state_to_amulet_block = lambda state: state.full_id
        changes = RegionData()
        changes.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:stone"))
        changes.set(BlockPos(1, 0, 0), BlockState.from_id("minecraft:bad"))

        with self.assertRaisesRegex(RuntimeError, "bridge is poisoned"):
            bridge.write_region(changes)
        self.assertTrue(bridge._write_poisoned)

        later = RegionData()
        later.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:dirt"))
        set_call_count = len(level.set_calls)
        with self.assertRaisesRegex(RuntimeError, "Close and reopen"):
            bridge.write_region(later)
        self.assertEqual(len(level.set_calls), set_call_count)

    def test_save_failure_restores_originals_when_rollback_save_succeeds(self) -> None:
        class FirstSaveFailsLevel(_FakeLevel):
            def save(self) -> None:
                self.save_calls += 1
                if self.save_calls == 1:
                    raise OSError("simulated first save failure")

        level = FirstSaveFailsLevel()
        bridge = _fake_bridge(level)
        bridge._state_to_amulet_block = lambda state: state.full_id
        changes = RegionData()
        changes.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:stone"))

        with self.assertRaisesRegex(RuntimeError, "rollback completed"):
            bridge.write_region(changes)

        self.assertEqual(level.blocks[(0, 0, 0)], "old-a")
        self.assertEqual(level.save_calls, 2)
        self.assertFalse(bridge._write_poisoned)

    def test_conversion_error_is_not_returned_as_air(self) -> None:
        class BrokenLevel:
            def get_chunk(self, _cx, _cz, _dimension):
                return object()

            def get_version_block(self, _x, _y, _z, _dimension, _version):
                raise RuntimeError("conversion failed")

        bridge = _fake_bridge(BrokenLevel())
        bridge._y_bounds = (0, 0)

        with self.assertRaisesRegex(RuntimeError, "conversion failed"):
            bridge.read_region(0, 0, 0, y_min=0, y_max=0)

    def test_version_identifier_comes_from_level_wrapper(self) -> None:
        class Wrapper:
            platform = "java"
            version = 3953
            max_world_version = (platform, version)

        class Level:
            level_wrapper = Wrapper()

        bridge = object.__new__(AmuletBridge)
        bridge.level = Level()
        self.assertEqual(bridge._detect_version_identifier(), ("java", 3953))

        class LegacyLevel:
            platform = "java"
            game_version = "Java 1.21.1"

        bridge.level = LegacyLevel()
        self.assertEqual(bridge._detect_version_identifier(), ("java", (1, 21, 1)))

    def test_y_window_must_intersect_the_world_height(self) -> None:
        bridge = object.__new__(AmuletBridge)
        bridge._y_bounds = (-64, 319)

        self.assertEqual(bridge.resolve_y_window(-100, 100), (-64, 100))
        with self.assertRaisesRegex(ValueError, "invalid_y_window"):
            bridge.resolve_y_window(500, 600)
        with self.assertRaisesRegex(ValueError, "invalid_y_window"):
            bridge.resolve_y_window(-500, -100)


if __name__ == "__main__":
    unittest.main()
