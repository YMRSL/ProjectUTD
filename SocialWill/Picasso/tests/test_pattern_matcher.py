from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from picasso.core.pattern_matcher import PatternMatcher
from picasso.core.style_engine import StyleEngine
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass


PROJECT_ROOT = Path(__file__).resolve().parents[1]
SHIPPED_PATTERNS = PROJECT_ROOT / "src" / "picasso" / "data" / "patterns"
SAFE_BLOCKS = PROJECT_ROOT / "src" / "picasso" / "data" / "safe_blocks.json"


def _pattern(
    name: str,
    blocks: list[dict],
    *,
    anchor: list[int] | None = None,
    **extra,
) -> dict:
    return {
        "name": name,
        "description": f"Test pattern {name}",
        "anchor": anchor or [0, 0, 0],
        "blocks": blocks,
        "replacement_anchor_offset": [0, 0, 0],
        "clear_offsets": [],
        **extra,
    }


def _block(offset: list[int], **condition) -> dict:
    return {"offset": offset, "match": condition}


def _write_pattern(directory: Path, data: dict, *, filename: str | None = None) -> None:
    path = directory / f"{filename or data['name']}.json"
    path.write_text(json.dumps(data), encoding="utf-8")


def _signature(matches) -> list[tuple]:
    return [
        (
            match.pattern_name,
            match.anchor_pos,
            tuple(match.blocks),
            match.yaw,
            tuple(sorted(match.matched_offsets)),
        )
        for match in matches
    ]


class PatternMatcherDeterminismTests(unittest.TestCase):
    def test_region_insertion_order_does_not_change_overlapping_match_choice(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            _write_pattern(
                patterns_dir,
                _pattern(
                    "stone_pair",
                    [
                        _block([0, 0, 0], block="minecraft:stone"),
                        _block([1, 0, 0], block="minecraft:stone"),
                    ],
                ),
            )
            matcher = PatternMatcher(patterns_dir)
            positions = [BlockPos(0, 64, 0), BlockPos(1, 64, 0), BlockPos(2, 64, 0)]

            forward = RegionData()
            reverse = RegionData()
            for pos in positions:
                forward.set(pos, BlockState.from_id("minecraft:stone"))
            for pos in reversed(positions):
                reverse.set(pos, BlockState.from_id("minecraft:stone"))

            forward_matches = matcher.find_matches(forward)
            reverse_matches = matcher.find_matches(reverse)

            self.assertEqual(_signature(forward_matches), _signature(reverse_matches))
            self.assertEqual(len(forward_matches), 1)
            self.assertEqual(forward_matches[0].anchor_pos, BlockPos(0, 64, 0))
            self.assertEqual(forward_matches[0].yaw, 0)

    def test_pattern_registry_order_does_not_change_winner(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            for name in ("alpha", "zeta"):
                _write_pattern(
                    patterns_dir,
                    _pattern(name, [_block([0, 0, 0], block="minecraft:stone")]),
                )
            matcher = PatternMatcher(patterns_dir)
            region = RegionData()
            region.set(BlockPos(0, 64, 0), BlockState.from_id("minecraft:stone"))

            first = matcher.find_matches(region)
            matcher.patterns = dict(reversed(list(matcher.patterns.items())))
            second = matcher.find_matches(region)

            self.assertEqual(_signature(first), _signature(second))
            self.assertEqual(len(first), 1)

    def test_symmetric_rotations_emit_one_physical_match(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            _write_pattern(
                patterns_dir,
                _pattern(
                    "symmetric_pair",
                    [
                        _block([0, 0, 0], block="minecraft:stone"),
                        _block([1, 0, 0], block="minecraft:stone"),
                    ],
                ),
            )
            region = RegionData()
            region.set(BlockPos(10, 64, 10), BlockState.from_id("minecraft:stone"))
            region.set(BlockPos(11, 64, 10), BlockState.from_id("minecraft:stone"))

            matches = PatternMatcher(patterns_dir).find_matches(region)

            self.assertEqual(len(matches), 1)
            self.assertEqual(
                matches[0].blocks,
                [BlockPos(10, 64, 10), BlockPos(11, 64, 10)],
            )

    def test_geometry_and_directional_properties_rotate_together(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            _write_pattern(
                patterns_dir,
                _pattern(
                    "directional_desk",
                    [
                        _block(
                            [0, 0, 0],
                            block="minecraft:oak_stairs",
                            properties={"facing": "south", "axis": "x", "rotation": "0"},
                        ),
                        _block([1, 0, 0], block="minecraft:oak_planks"),
                    ],
                ),
            )
            region = RegionData()
            region.set(
                BlockPos(4, 70, 4),
                BlockState.from_id(
                    "minecraft:oak_stairs",
                    {"facing": "west", "axis": "z", "rotation": "4"},
                ),
            )
            region.set(BlockPos(4, 70, 5), BlockState.from_id("minecraft:oak_planks"))

            matches = PatternMatcher(patterns_dir).find_matches(region)

            self.assertEqual(len(matches), 1)
            self.assertEqual(matches[0].yaw, 90)
            self.assertEqual(matches[0].anchor_pos, BlockPos(4, 70, 4))

            region.blocks[BlockPos(4, 70, 4)].properties["axis"] = "x"
            self.assertEqual(PatternMatcher(patterns_dir).find_matches(region), [])


class PatternMatcherLoadingTests(unittest.TestCase):
    def test_invalid_files_are_logged_and_skipped(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            valid = _pattern("valid", [_block([0, 0, 0], block="minecraft:stone")])
            _write_pattern(patterns_dir, valid)

            (patterns_dir / "broken.json").write_text("{ definitely broken", encoding="utf-8")
            _write_pattern(
                patterns_dir,
                {"name": "missing_blocks", "description": "missing", "anchor": [0, 0, 0]},
            )
            _write_pattern(
                patterns_dir,
                _pattern("declared_name", [_block([0, 0, 0], block="minecraft:stone")]),
                filename="different_filename",
            )

            with self.assertLogs("picasso.core.pattern_matcher", level="WARNING") as logs:
                matcher = PatternMatcher(patterns_dir)

            self.assertEqual(set(matcher.patterns), {"valid"})
            self.assertEqual(len(logs.output), 3)

    def test_offset_and_match_schema_errors_fail_only_their_file(self) -> None:
        invalid_patterns = {
            "bad_anchor": _pattern(
                "bad_anchor",
                [_block([0, 0, 0], block="minecraft:stone")],
                anchor=[0, 0],
            ),
            "bad_offset": _pattern(
                "bad_offset",
                [_block([0, "zero", 0], block="minecraft:stone")],
            ),
            "bad_clear": {
                **_pattern("bad_clear", [_block([0, 0, 0], block="minecraft:stone")]),
                "clear_offsets": [[0, 0]],
            },
            "bad_condition": _pattern(
                "bad_condition",
                [_block([0, 0, 0], block=42)],
            ),
            "bad_properties": _pattern(
                "bad_properties",
                [_block([0, 0, 0], block="minecraft:stone", properties={"facing": 2})],
            ),
            "bad_replacement": _pattern(
                "bad_replacement",
                [_block([0, 0, 0], block="minecraft:stone")],
                dd_replacement="missing_namespace",
            ),
        }
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            _write_pattern(
                patterns_dir,
                _pattern("valid", [_block([0, 0, 0], block="minecraft:stone")]),
            )
            for data in invalid_patterns.values():
                _write_pattern(patterns_dir, data)

            with self.assertLogs("picasso.core.pattern_matcher", level="WARNING"):
                matcher = PatternMatcher(patterns_dir)

            self.assertEqual(set(matcher.patterns), {"valid"})

    def test_experimental_and_deprecated_filters_are_preserved(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            patterns_dir = Path(tmp)
            _write_pattern(
                patterns_dir,
                _pattern("normal", [_block([0, 0, 0], block="minecraft:stone")]),
            )
            _write_pattern(
                patterns_dir,
                _pattern(
                    "experimental",
                    [_block([0, 0, 0], block="minecraft:dirt")],
                    experimental=True,
                ),
            )
            _write_pattern(
                patterns_dir,
                _pattern(
                    "deprecated",
                    [_block([0, 0, 0], block="minecraft:cobblestone")],
                    deprecated=True,
                ),
            )
            region = RegionData()
            region.set(BlockPos(0, 64, 0), BlockState.from_id("minecraft:stone"))
            region.set(BlockPos(2, 64, 0), BlockState.from_id("minecraft:dirt"))
            region.set(BlockPos(4, 64, 0), BlockState.from_id("minecraft:cobblestone"))
            matcher = PatternMatcher(patterns_dir)

            default_names = {match.pattern_name for match in matcher.find_matches(region)}
            experimental_names = {
                match.pattern_name
                for match in matcher.find_matches(region, include_experimental=True)
            }

            self.assertEqual(default_names, {"normal"})
            self.assertEqual(experimental_names, {"normal", "experimental"})


class ShippedBedPatternTests(unittest.TestCase):
    DIRECTIONS = {
        "south": ((0, 0, 1), 0),
        "west": ((-1, 0, 0), 90),
        "north": ((0, 0, -1), 180),
        "east": ((1, 0, 0), 270),
    }

    def _bed_state(self, part: str, facing: str) -> BlockState:
        return BlockState.from_id(
            "minecraft:red_bed",
            {"part": part, "facing": facing, "occupied": "false"},
        )

    def _region(self, facing: str, *, include_foot: bool = True, include_head: bool = True):
        foot = BlockPos(8, 64, 8)
        dx, dy, dz = self.DIRECTIONS[facing][0]
        head = foot.offset(dx=dx, dy=dy, dz=dz)
        region = RegionData(
            origin_cx=0,
            origin_cz=0,
            radius_chunks=0,
            y_min=60,
            y_max=80,
            loaded_chunks={(0, 0)},
        )
        if include_foot:
            region.set(foot, self._bed_state("foot", facing))
        if include_head:
            region.set(head, self._bed_state("head", facing))
        return region, foot, head

    def test_each_facing_emits_one_physical_match_and_one_atomic_replacement(self) -> None:
        matcher = PatternMatcher(SHIPPED_PATTERNS)
        style_pass = StylePass.model_validate(
            {
                "name": "replace_bed_test",
                "description": "Replace one complete bed.",
                "type": "pattern_replace",
                "mappings": [
                    {
                        "pattern": "bed_frame",
                        "dd_block": "doomsday_decoration:bed",
                    }
                ],
            }
        )
        engine = StyleEngine(
            {style_pass.name: style_pass},
            SAFE_BLOCKS,
            pattern_matcher=matcher,
        )

        for facing, (_offset, expected_yaw) in self.DIRECTIONS.items():
            with self.subTest(facing=facing):
                region, foot, head = self._region(facing)
                matches = [
                    match
                    for match in matcher.find_matches(region)
                    if match.pattern_name == "bed_frame"
                ]

                self.assertEqual(len(matches), 1)
                self.assertEqual(matches[0].anchor_pos, foot)
                self.assertEqual(matches[0].yaw, expected_yaw)
                self.assertEqual(set(matches[0].blocks), {foot, head})

                changes = engine.apply(style_pass.name, region, seed=42)

                self.assertEqual(set(changes.blocks), {foot, head})
                self.assertEqual(
                    changes.blocks[foot].full_id,
                    "doomsday_decoration:bed",
                )
                self.assertTrue(changes.blocks[head].is_air)
                self.assertEqual(changes.write_contexts[foot], "decoration")
                self.assertEqual(changes.write_contexts[head], "pattern_clear")
                self.assertEqual(changes.atomic_groups, [{foot, head}])

    def test_isolated_half_beds_and_mismatched_facing_do_not_match(self) -> None:
        matcher = PatternMatcher(SHIPPED_PATTERNS)

        for facing in self.DIRECTIONS:
            with self.subTest(facing=facing, half="foot"):
                foot_only, _foot, _head = self._region(facing, include_head=False)
                self.assertFalse(
                    any(
                        match.pattern_name == "bed_frame"
                        for match in matcher.find_matches(foot_only)
                    )
                )
            with self.subTest(facing=facing, half="head"):
                head_only, _foot, _head = self._region(facing, include_foot=False)
                self.assertFalse(
                    any(
                        match.pattern_name == "bed_frame"
                        for match in matcher.find_matches(head_only)
                    )
                )

        mismatched, _foot, head = self._region("south")
        mismatched.set(head, self._bed_state("head", "north"))
        self.assertFalse(
            any(
                match.pattern_name == "bed_frame"
                for match in matcher.find_matches(mismatched)
            )
        )


if __name__ == "__main__":
    unittest.main()
