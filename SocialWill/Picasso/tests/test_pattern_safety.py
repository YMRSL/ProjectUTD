from __future__ import annotations

import json
import sys
import tempfile
import types
import unittest
from pathlib import Path

from picasso.core.pattern_matcher import PatternMatcher

dotenv_stub = types.ModuleType("dotenv")
dotenv_stub.load_dotenv = lambda *args, **kwargs: False
sys.modules.setdefault("dotenv", dotenv_stub)

from picasso.core.write_choke import WriteChoke
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class PatternSafetyTests(unittest.TestCase):
    def test_experimental_patterns_are_skipped_by_default(self) -> None:
        matcher = PatternMatcher(PROJECT_ROOT / "src" / "picasso" / "data" / "patterns")
        region = RegionData()
        region.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:smooth_stone_slab"))
        region.set(BlockPos(0, 1, 0), BlockState.from_id("minecraft:player_head"))
        region.set(BlockPos(1, 0, 0), BlockState.from_id("minecraft:smooth_stone_slab"))
        region.set(BlockPos(1, 1, 0), BlockState.from_id("minecraft:red_carpet"))
        region.set(BlockPos(0, -1, 0), BlockState.from_id("minecraft:oak_fence"))
        region.set(BlockPos(0, 0, -1), BlockState.from_id("minecraft:oak_stairs"))

        default_names = {match.pattern_name for match in matcher.find_matches(region)}
        experimental_names = {
            match.pattern_name for match in matcher.find_matches(region, include_experimental=True)
        }

        self.assertNotIn("computer_desk_combo", default_names)
        self.assertIn("computer_desk_combo", experimental_names)

    def test_unmatched_optional_clear_offset_is_not_marked_for_clearing(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            patterns_dir = root / "patterns"
            patterns_dir.mkdir()
            (patterns_dir / "optional_guard.json").write_text(
                json.dumps(
                    {
                        "name": "optional_guard",
                        "description": "Required slab with optional carpet clear.",
                        "anchor": [0, 0, 0],
                        "blocks": [
                            {"offset": [0, 0, 0], "match": {"block": "minecraft:smooth_stone_slab"}},
                            {
                                "offset": [1, 0, 0],
                                "optional": True,
                                "match": {"block": "minecraft:red_carpet"},
                            },
                        ],
                        "replacement_anchor_offset": [0, 0, 0],
                        "clear_offsets": [[1, 0, 0]],
                    }
                ),
                encoding="utf-8",
            )
            region = RegionData()
            region.set(BlockPos(0, 0, 0), BlockState.from_id("minecraft:smooth_stone_slab"))
            region.set(BlockPos(1, 0, 0), BlockState.from_id("minecraft:stone"))

            matches = PatternMatcher(patterns_dir).find_matches(region)

            self.assertEqual(len(matches), 1)
            self.assertIn((0, 0, 0), matches[0].matched_offsets)
            self.assertNotIn((1, 0, 0), matches[0].matched_offsets)

    def test_pattern_atomic_group_drops_clears_when_replacement_is_blocked(self) -> None:
        replace_pos = BlockPos(0, 0, 0)
        clear_pos = BlockPos(1, 0, 0)
        source = RegionData()
        source.set(replace_pos, BlockState.from_id("minecraft:diamond_block"))
        source.set(clear_pos, BlockState.from_id("minecraft:oak_slab"))
        raw_changes = RegionData()
        raw_changes.set(replace_pos, BlockState.from_id("doomsday_decoration:officepartitiondesk"))
        raw_changes.write_contexts[replace_pos] = "decoration"
        raw_changes.set(clear_pos, AIR)
        raw_changes.write_contexts[clear_pos] = "pattern_clear"
        raw_changes.atomic_groups.append({replace_pos, clear_pos})

        result = WriteChoke(
            safe_replaceable={"minecraft:oak_slab"},
            structural_never_touch=set(),
        ).validate(source, raw_changes, only_safe_blocks=True)

        self.assertEqual(result.changes.blocks, {})
        reasons = {entry["reason"] for entry in result.skipped}
        self.assertIn("not_replaceable", reasons)
        self.assertIn("atomic_group_failed", reasons)

    def test_default_furniture_pass_does_not_map_experimental_patterns(self) -> None:
        pass_path = PROJECT_ROOT / "src" / "picasso" / "data" / "passes" / "tlou_furniture_modreplace.json"
        data = json.loads(pass_path.read_text(encoding="utf-8"))
        mapped = {mapping["pattern"] for mapping in data["mappings"]}

        self.assertNotIn("computer_desk_combo", mapped)
        self.assertNotIn("supermarket_stair_shelf", mapped)


if __name__ == "__main__":
    unittest.main()
