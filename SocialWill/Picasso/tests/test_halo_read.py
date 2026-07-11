from __future__ import annotations

import json
import tempfile
import types
import unittest
from pathlib import Path
from unittest.mock import patch

import picasso.core.amulet_bridge as bridge_module
import numpy as np
from picasso.config import config
from picasso.core.amulet_bridge import AmuletBridge
from picasso.core.pattern_matcher import PatternMatcher
from picasso.core.surface_classifier import classify_surfaces
from picasso.core.write_choke import WriteChoke
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.session import session
from picasso.tools import analysis, world_io


STONE = BlockState.from_id("minecraft:stone")


class ChunkNotFound(Exception):
    pass


class FakeLevel:
    def __init__(self, loaded_chunks: set[tuple[int, int]]) -> None:
        self.loaded_chunks = set(loaded_chunks)
        self.chunk_calls: list[tuple[int, int]] = []

    def get_chunk(self, chunk_x: int, chunk_z: int, _dimension: str):
        self.chunk_calls.append((chunk_x, chunk_z))
        if (chunk_x, chunk_z) not in self.loaded_chunks:
            raise ChunkNotFound((chunk_x, chunk_z))
        return object()


def _bridge(
    states: dict[BlockPos, BlockState],
    *,
    loaded_chunks: set[tuple[int, int]],
    entities: set[BlockPos] | None = None,
    y_bounds: tuple[int, int] = (0, 4),
) -> AmuletBridge:
    bridge = object.__new__(AmuletBridge)
    bridge.level = FakeLevel(loaded_chunks)
    bridge.dimension = "minecraft:overworld"
    bridge._y_bounds = y_bounds
    entity_positions = set(entities or ())
    bridge.read_block_with_entity = lambda x, y, z: (
        states.get(BlockPos(x, y, z), AIR),
        BlockPos(x, y, z) in entity_positions,
    )
    return bridge


def _all_chunks(radius: int) -> set[tuple[int, int]]:
    return {
        (chunk_x, chunk_z)
        for chunk_x in range(-radius, radius + 1)
        for chunk_z in range(-radius, radius + 1)
    }


def _surrounded_block(states: dict[BlockPos, BlockState], pos: BlockPos) -> None:
    states[pos] = STONE
    states[pos.offset(dy=-1)] = STONE
    states[pos.offset(dy=1)] = STONE
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        states[pos.offset(dx=dx, dz=dz)] = STONE


class FakeMCP:
    def __init__(self) -> None:
        self.tools = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


class HaloReadTests(unittest.TestCase):
    def test_extra_needed_palette_falls_back_only_for_populated_positions(self) -> None:
        class FakeBlock:
            def __init__(self, namespace: str, name: str) -> None:
                self.namespace = namespace
                self.base_name = name
                self.properties = {}
                self.block_tuple = (self,)

        class Translator:
            def __init__(self) -> None:
                self.calls: list[str] = []

            def from_universal(self, block):
                self.calls.append(block.base_name)
                native = FakeBlock("minecraft", block.base_name)
                return native, None, block.base_name == "needs_context"

        class Storage:
            default_value = 0
            sub_chunks = (0,)

            def __init__(self) -> None:
                self.section = np.zeros((16, 16, 16), dtype=np.uint32)
                self.section[2, 1, 3] = 1

            def get_sub_chunk(self, _section_y):
                return self.section

        class BlockEntities:
            @staticmethod
            def values():
                return ()

        translator = Translator()
        chunk = types.SimpleNamespace(
            blocks=Storage(),
            block_palette=[
                FakeBlock("universal_minecraft", "air"),
                FakeBlock("universal_minecraft", "needs_context"),
            ],
            block_entities=BlockEntities(),
        )
        bridge = object.__new__(AmuletBridge)
        bridge._native_block_translator = translator
        canonical_calls: list[tuple[int, int, int]] = []

        def canonical(x, y, z):
            canonical_calls.append((x, y, z))
            return BlockState.from_id("minecraft:diamond_block"), False

        bridge.read_block_with_entity = canonical

        blocks, block_entities = bridge._read_loaded_chunk(chunk, 0, 0, 0, 2)

        self.assertEqual(canonical_calls, [(2, 1, 3)])
        self.assertEqual(translator.calls.count("air"), 1)
        self.assertEqual(translator.calls.count("needs_context"), 1)
        self.assertEqual(blocks[BlockPos(2, 1, 3)].full_id, "minecraft:diamond_block")
        self.assertEqual(block_entities, set())

    def test_legacy_chunk_api_uses_complete_canonical_fallback(self) -> None:
        bridge = object.__new__(AmuletBridge)
        canonical_calls = 0

        def canonical(_x, _y, _z):
            nonlocal canonical_calls
            canonical_calls += 1
            return AIR, False

        bridge.read_block_with_entity = canonical

        blocks, block_entities = bridge._read_loaded_chunk(object(), 0, 0, 0, 1)

        self.assertEqual(canonical_calls, 16 * 16 * 2)
        self.assertEqual(blocks, {})
        self.assertEqual(block_entities, set())

    def test_narrow_target_y_uses_read_only_vertical_context_for_classification(self) -> None:
        target = BlockPos(8, 1, 8)
        states: dict[BlockPos, BlockState] = {}
        _surrounded_block(states, target)
        above = target.offset(dy=1)
        bridge = _bridge(
            states,
            loaded_chunks={(0, 0)},
            entities={above},
            y_bounds=(0, 4),
        )

        region = bridge.read_region(0, 0, 0, y_min=1, y_max=1)
        classify_surfaces(region)
        summary = region.to_summary()

        self.assertEqual((region.y_min, region.y_max), (1, 1))
        self.assertEqual((region.read_y_min, region.read_y_max), (0, 4))
        self.assertEqual(region.surface_classes[target], "embedded")
        self.assertIn(target.offset(dy=-1), region.halo_positions)
        self.assertIn(above, region.halo_positions)
        self.assertIn(above, region.block_entity_positions)
        self.assertFalse(region.is_modifiable(above))
        self.assertEqual(summary["block_count"], 5)
        self.assertEqual(summary["context_block_count"], 2)
        self.assertEqual(summary["block_entity_count"], 0)
        self.assertEqual(summary["context_block_entity_count"], 1)
        self.assertEqual(summary["y_window"], {"min": 1, "max": 1})
        self.assertEqual(summary["read_y_window"], {"min": 0, "max": 4})

    def test_vertical_context_clamps_to_world_bounds(self) -> None:
        bridge = _bridge({}, loaded_chunks={(0, 0)}, y_bounds=(0, 2))

        region = bridge.read_region(0, 0, 0, y_min=0, y_max=0)

        self.assertEqual((region.y_min, region.y_max), (0, 0))
        self.assertEqual((region.read_y_min, region.read_y_max), (0, 2))

    def test_bridge_reads_core_and_one_chunk_context_once_with_separate_counts(self) -> None:
        core = BlockPos(1, 1, 1)
        halo = BlockPos(16, 1, 1)
        states = {core: STONE, halo: BlockState.from_id("minecraft:chest")}
        bridge = _bridge(
            states,
            loaded_chunks={(0, 0), (1, 0)},
            entities={halo},
            y_bounds=(1, 1),
        )

        region = bridge.read_region(0, 0, 0, y_min=1, y_max=1)

        self.assertEqual(region.chunks_read, 1)
        self.assertEqual(region.chunks_missing, 0)
        self.assertEqual(region.context_chunks_read, 1)
        self.assertEqual(region.context_chunks_missing, 7)
        self.assertEqual(region.loaded_chunks, {(0, 0), (1, 0)})
        self.assertIn(halo, region.halo_positions)
        self.assertNotIn(core, region.halo_positions)
        self.assertIn(halo, region.block_entity_positions)
        self.assertEqual(len(bridge.level.chunk_calls), 9)
        self.assertEqual(len(set(bridge.level.chunk_calls)), 9)

    def test_missing_context_never_inflates_core_missing_count(self) -> None:
        bridge = _bridge({}, loaded_chunks={(0, 0)}, y_bounds=(0, 0))

        region = bridge.read_region(0, 0, 0, y_min=0, y_max=0)

        self.assertEqual((region.chunks_read, region.chunks_missing), (1, 0))
        self.assertEqual(
            (region.context_chunks_read, region.context_chunks_missing),
            (0, 8),
        )

    def test_boundary_wall_classifies_like_whole_read(self) -> None:
        boundary = BlockPos(15, 1, 8)
        states: dict[BlockPos, BlockState] = {}
        _surrounded_block(states, boundary)
        loaded = _all_chunks(2)

        tiled = _bridge(states, loaded_chunks=loaded).read_region(
            0, 0, 0, y_min=0, y_max=4
        )
        whole = _bridge(states, loaded_chunks=loaded).read_region(
            0, 0, 1, y_min=0, y_max=4
        )
        classify_surfaces(tiled)
        classify_surfaces(whole)

        self.assertEqual(tiled.surface_classes[boundary], "embedded")
        self.assertEqual(tiled.surface_classes[boundary], whole.surface_classes[boundary])
        self.assertIn(boundary.offset(dx=1), tiled.halo_positions)

    def test_four_tiles_match_whole_read_at_every_seam_adjacent_target(self) -> None:
        states: dict[BlockPos, BlockState] = {}
        seam_blocks = [
            BlockPos(x, 1, z)
            for x in (-1, 0)
            for z in (-1, 0)
        ]
        for pos in seam_blocks:
            _surrounded_block(states, pos)
        loaded = _all_chunks(2)
        whole = _bridge(states, loaded_chunks=loaded).read_region(
            0, 0, 1, y_min=0, y_max=4
        )
        classify_surfaces(whole)

        compared = 0
        for chunk_x, chunk_z in ((-1, -1), (-1, 0), (0, -1), (0, 0)):
            tile = _bridge(states, loaded_chunks=loaded).read_region(
                chunk_x,
                chunk_z,
                0,
                y_min=0,
                y_max=4,
            )
            classify_surfaces(tile)
            for pos, _state in tile.iter_target_blocks():
                self.assertEqual(tile.surface_classes[pos], whole.surface_classes[pos])
                compared += 1

        self.assertGreaterEqual(compared, len(seam_blocks))

    def test_actual_halo_block_is_never_modifiable_or_written(self) -> None:
        halo = BlockPos(16, 1, 1)
        bridge = _bridge({halo: STONE}, loaded_chunks={(0, 0), (1, 0)}, y_bounds=(1, 1))
        region = bridge.read_region(0, 0, 0, y_min=1, y_max=1)
        proposed = RegionData()
        proposed.set(halo, BlockState.from_id("minecraft:dirt"))

        result = WriteChoke(
            safe_replaceable={"minecraft:stone"},
            structural_never_touch=set(),
        ).validate(
            region,
            proposed,
            only_safe_blocks=True,
            enforce_modded_gate=False,
        )

        self.assertFalse(region.is_modifiable(halo))
        self.assertEqual(region.modification_block_reason(halo), "halo_position")
        self.assertEqual(result.changed_count, 0)
        self.assertEqual(result.skipped[0]["reason"], "halo_position")

    def test_region_summary_and_analysis_exclude_halo_targets(self) -> None:
        core = BlockPos(1, 1, 1)
        halo = BlockPos(16, 1, 1)
        region = RegionData(
            blocks={
                core: STONE,
                halo: BlockState.from_id("minecraft:oak_leaves"),
            },
            origin_cx=0,
            origin_cz=0,
            radius_chunks=0,
            y_min=0,
            y_max=4,
            chunks_read=1,
            context_chunks_read=1,
            loaded_chunks={(0, 0), (1, 0)},
            halo_positions={halo},
            surface_classes={core: "embedded", halo: "rooftop"},
            space_classes={core: "interior", halo: "exterior"},
        )

        class Matcher:
            def find_matches(self, _region):
                return [
                    types.SimpleNamespace(pattern_name="chair", anchor_pos=core),
                    types.SimpleNamespace(pattern_name="chair", anchor_pos=halo),
                ]

        previous_matcher = session.pattern_matcher
        previous_noise = session.noise_backend
        try:
            session.pattern_matcher = Matcher()
            session.noise_backend = "fallback"
            mcp = FakeMCP()
            analysis.register(mcp)
            with patch.object(analysis, "ensure_region", return_value=region):
                result = mcp.tools["analyze_region"](0, 0, 0)
        finally:
            session.pattern_matcher = previous_matcher
            session.noise_backend = previous_noise

        summary = region.to_summary()
        self.assertTrue(result["ok"])
        self.assertEqual(result["surface_counts"], {"embedded": 1})
        self.assertNotIn("rooftop", result["top_blocks_by_surface"])
        self.assertEqual(result["vegetation_coverage"], 0.0)
        self.assertEqual(result["pattern_matches"][0]["count"], 1)
        self.assertIn("Analyzed 1 blocks", result["summary"])
        self.assertEqual(summary["block_count"], 1)
        self.assertEqual(summary["context_block_count"], 1)
        self.assertEqual(summary["bounds"], {"min": core.to_dict(), "max": core.to_dict()})

    def test_halo_pattern_anchor_cannot_suppress_overlapping_core_match(self) -> None:
        region = RegionData(
            blocks={
                BlockPos(-2, 1, 0): STONE,
                BlockPos(-1, 1, 0): STONE,
                BlockPos(0, 1, 0): STONE,
            },
            origin_cx=0,
            origin_cz=0,
            radius_chunks=0,
            y_min=0,
            y_max=4,
            loaded_chunks={(-1, 0), (0, 0)},
            halo_positions={BlockPos(-2, 1, 0), BlockPos(-1, 1, 0)},
        )
        pattern = {
            "name": "west_pair",
            "description": "A pair that may cross the west tile edge.",
            "anchor": [0, 0, 0],
            "blocks": [
                {"offset": [0, 0, 0], "match": {"block": "minecraft:stone"}},
                {"offset": [-1, 0, 0], "match": {"block": "minecraft:stone"}},
            ],
            "replacement_anchor_offset": [0, 0, 0],
            "clear_offsets": [],
        }
        with tempfile.TemporaryDirectory() as tmp:
            Path(tmp, "west_pair.json").write_text(json.dumps(pattern), encoding="utf-8")
            matches = PatternMatcher(tmp).find_matches(region)

        self.assertEqual(len(matches), 1)
        self.assertEqual(matches[0].anchor_pos, BlockPos(0, 1, 0))
        self.assertIn(BlockPos(-1, 1, 0), matches[0].blocks)

    def test_zero_core_radius_remains_valid_when_configured_max_is_zero(self) -> None:
        bridge = _bridge({}, loaded_chunks={(0, 0)}, y_bounds=(0, 0))
        zero_cap = types.SimpleNamespace(max_radius_chunks=0)

        with patch.object(bridge_module, "config", zero_cap):
            region = bridge.read_region(0, 0, 0, y_min=0, y_max=0)

        self.assertEqual(region.radius_chunks, 0)
        self.assertEqual(region.chunks_read, 1)
        self.assertEqual(region.context_chunks_missing, 8)

    def test_tool_limit_accounts_for_required_halo_radius(self) -> None:
        previous_bridge = session.bridge
        try:
            session.bridge = object()
            mcp = FakeMCP()
            world_io.register(mcp)
            result = mcp.tools["read_region"](
                cx=0,
                cz=0,
                radius_chunks=config.max_radius_chunks + 1,
            )
        finally:
            session.bridge = previous_bridge

        self.assertFalse(result["ok"])
        self.assertEqual(result["error"], "region_too_large")
        self.assertIn(
            f"actual read radius={config.max_radius_chunks + 2}",
            result["message"],
        )


if __name__ == "__main__":
    unittest.main()
