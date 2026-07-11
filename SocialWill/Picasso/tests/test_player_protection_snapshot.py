from __future__ import annotations

import json
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from picasso.core.player_protection_snapshot import build_player_protection_snapshot
from picasso.models.block import BlockPos


UTC = timezone.utc
AS_OF = datetime(2026, 7, 10, 12, 0, tzinfo=UTC)


def event_line(
    timestamp: str,
    action: str,
    pos: tuple[int, int, int],
    *,
    player: str = "Steve",
    block: str = "minecraft:stone",
) -> str:
    x, y, z = pos
    return json.dumps(
        {
            "v": 1,
            "t": timestamp,
            "player": player,
            "action": action,
            "pos": {"x": x, "y": y, "z": z},
            "block": block,
            "dim": "minecraft:overworld",
        },
        separators=(",", ":"),
    )


def bounds(x_min: int, x_max: int, z_min: int = 0, z_max: int = 2) -> dict:
    return {
        "x_min": x_min,
        "x_max": x_max,
        "y_min": 60,
        "y_max": 80,
        "z_min": z_min,
        "z_max": z_max,
    }


def structure(
    structure_id: str,
    attribution: str,
    structure_bounds: dict,
    **detected_extra,
) -> dict:
    return {
        "id": structure_id,
        "detected": {
            "attribution": attribution,
            "bounds": structure_bounds,
            **detected_extra,
        },
        "authored": {},
        "stale": False,
    }


class PlayerProtectionSnapshotTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.world = self.root / "world"
        self.logs = self.root / "logs"
        self.world.mkdir()
        self.logs.mkdir()

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def write_registry(self, value) -> None:
        (self.world / "picasso_structures.json").write_text(
            json.dumps(value), encoding="utf-8"
        )

    def write_log(self, day: str, lines: list[str]) -> None:
        (self.logs / f"{day}.jsonl").write_text("\n".join(lines), encoding="utf-8")

    def snapshot(self, **overrides):
        values = {
            "world_path": self.world,
            "build_log_dir": self.logs,
            "as_of": AS_OF,
            "dimension": "minecraft:overworld",
            "lookback_days": 1,
            "min_events": 1,
            "join_dist": 16,
            "join_gap_minutes": 90,
        }
        values.update(overrides)
        return build_player_protection_snapshot(**values)

    def test_player_built_from_list_root_protects_whole_bounds(self) -> None:
        self.write_registry([structure("built", "player_built", bounds(10, 12, 10, 12))])

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "active")
        self.assertEqual(len(evaluator.areas), 1)
        self.assertEqual(
            evaluator.evaluate(BlockPos(11, 70, 11)).reason,
            "player_built_structure",
        )
        self.assertFalse(evaluator.evaluate(BlockPos(20, 70, 20)).protected)
        self.assertEqual(evaluator.source_summary["registry"]["root_format"], "list")

    def test_wrapper_root_manual_override_shadows_attribution_and_bounds_and_stale_skips(self) -> None:
        overridden = structure("override", "native", bounds(0, 2))
        overridden["authored"] = {
            "manual_override": {
                "attribution": "player_built",
                "bounds": bounds(20, 22, 20, 22),
            }
        }
        disabled = structure("disabled", "player_built", bounds(30, 32, 30, 32))
        disabled["authored"] = {"manual_override": {"attribution": "native"}}
        stale = structure("stale", "player_built", bounds(40, 42, 40, 42))
        stale["stale"] = True
        self.write_registry({"structures": [overridden, disabled, stale]})

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "active")
        self.assertEqual([area.area_id for area in evaluator.areas], ["override"])
        self.assertTrue(evaluator.evaluate(BlockPos(21, 70, 21)).protected)
        self.assertFalse(evaluator.evaluate(BlockPos(1, 70, 1)).protected)
        self.assertFalse(evaluator.evaluate(BlockPos(31, 70, 31)).protected)
        self.assertEqual(evaluator.source_summary["registry"]["stale_skipped"], 1)

    def test_player_modified_replay_uses_last_place_not_superseded_by_break(self) -> None:
        self.write_registry(
            {
                "structures": [
                    structure(
                        "modified",
                        "player_modified",
                        bounds(0, 5),
                        first_built="2026-07-01T00:00:00Z",
                    )
                ]
            }
        )
        self.write_log(
            "2026-07-01",
            [
                event_line("2026-07-01T00:00:00Z", "place", (1, 70, 1)),
                event_line("2026-07-01T00:01:00Z", "place", (2, 70, 1)),
                event_line("2026-07-01T00:02:00Z", "break", (1, 70, 1)),
            ],
        )

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "active")
        self.assertEqual(evaluator.areas[0].positions, frozenset({BlockPos(2, 70, 1)}))
        self.assertFalse(evaluator.evaluate(BlockPos(1, 70, 1)).protected)
        self.assertEqual(
            evaluator.evaluate(BlockPos(2, 70, 1)).reason,
            "player_modified_position",
        )

    def test_expired_modified_evidence_keeps_best_effort_position_but_is_incomplete(self) -> None:
        self.write_registry(
            [
                structure(
                    "old-modified",
                    "player_modified",
                    bounds(0, 5),
                    first_built="2026-06-01T00:00:00Z",
                )
            ]
        )
        self.write_log(
            "2026-07-01",
            [event_line("2026-07-01T00:00:00Z", "place", (2, 70, 1))],
        )

        evaluator = self.snapshot()
        decision = evaluator.evaluate(BlockPos(2, 70, 1))

        self.assertEqual(evaluator.status, "unavailable")
        self.assertTrue(decision.protected)
        self.assertEqual(decision.reason, "player_modified_position")
        self.assertEqual(decision.status, "unavailable")
        self.assertIn(
            "retained_evidence_expired",
            evaluator.source_summary["modified_replay"]["diagnostics"][0]["reasons"],
        )

    def test_missing_log_preserves_built_area_but_modified_positions_are_unavailable(self) -> None:
        self.write_registry(
            [
                structure("built", "player_built", bounds(10, 12, 10, 12)),
                structure("modified", "player_modified", bounds(20, 22, 20, 22)),
            ]
        )

        evaluator = self.snapshot(build_log_dir=None)

        self.assertEqual(evaluator.status, "unavailable")
        self.assertEqual([area.area_id for area in evaluator.areas], ["built"])
        self.assertTrue(evaluator.evaluate(BlockPos(11, 70, 11)).protected)
        unknown = evaluator.evaluate(BlockPos(21, 70, 21))
        self.assertFalse(unknown.protected)
        self.assertEqual(unknown.reason, "player_protection_unavailable")

    def test_corrupt_registry_preserves_recent_activity_site_with_unavailable_status(self) -> None:
        (self.world / "picasso_structures.json").write_text("{bad", encoding="utf-8")
        self.write_log(
            "2026-07-10",
            [event_line("2026-07-10T11:00:00Z", "place", (50, 70, 50))],
        )

        evaluator = self.snapshot()
        decision = evaluator.evaluate(BlockPos(50, 70, 50))

        self.assertEqual(evaluator.status, "unavailable")
        self.assertTrue(decision.protected)
        self.assertEqual(decision.reason, "recent_player_activity")
        self.assertEqual(
            evaluator.source_summary["registry"]["diagnostics"][0]["reason"],
            "registry_read_error",
        )

    def test_partial_valid_registry_keeps_known_area_and_marks_incomplete(self) -> None:
        self.write_registry(
            {
                "structures": [
                    structure("built", "player_built", bounds(10, 12, 10, 12)),
                    {"id": "bad", "detected": "not-an-object"},
                ]
            }
        )

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "unavailable")
        self.assertEqual([area.area_id for area in evaluator.areas], ["built"])
        self.assertTrue(evaluator.evaluate(BlockPos(11, 70, 11)).protected)
        self.assertEqual(evaluator.source_summary["registry"]["entries_invalid"], 1)

    def test_malformed_row_cannot_poison_later_valid_row_with_same_id(self) -> None:
        self.write_registry(
            {
                "structures": [
                    {"id": "base", "detected": "not-an-object"},
                    structure("base", "player_built", bounds(10, 12, 10, 12)),
                ]
            }
        )

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "unavailable")
        self.assertEqual([area.area_id for area in evaluator.areas], ["base"])
        self.assertTrue(evaluator.evaluate(BlockPos(11, 70, 11)).protected)

    def test_nonempty_all_bad_log_is_not_reported_as_active_empty(self) -> None:
        self.write_registry([])
        self.write_log("2026-07-10", ["{bad}", "{also-bad}"])

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "unavailable")
        recent = evaluator.source_summary["build_log"]["recent"]
        self.assertEqual(recent["events_valid_v1"], 0)
        self.assertEqual(recent["lines_read"], 2)
        self.assertEqual(recent["status"], "unavailable")

    def test_mixed_bad_and_valid_log_keeps_stats_and_active_site(self) -> None:
        self.write_registry([])
        self.write_log(
            "2026-07-10",
            [
                "{bad}",
                event_line("2026-07-10T11:00:00Z", "place", (5, 70, 5)),
            ],
        )

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "active")
        self.assertTrue(evaluator.evaluate(BlockPos(5, 70, 5)).protected)
        recent = evaluator.source_summary["build_log"]["recent"]
        self.assertEqual(recent["lines_skipped"], 1)
        self.assertEqual(recent["events_valid_v1"], 1)

    def test_modified_replay_with_skipped_record_keeps_position_but_marks_incomplete(self) -> None:
        self.write_registry(
            [structure("modified", "player_modified", bounds(0, 5))]
        )
        self.write_log(
            "2026-07-01",
            [
                "{bad}",
                event_line("2026-07-01T00:01:00Z", "place", (2, 70, 1)),
            ],
        )

        evaluator = self.snapshot()

        self.assertEqual(evaluator.status, "unavailable")
        self.assertTrue(evaluator.evaluate(BlockPos(2, 70, 1)).protected)
        self.assertIn(
            "retained_log_has_skipped_records",
            evaluator.source_summary["modified_replay"]["diagnostics"][0]["reasons"],
        )

    def test_snapshot_is_read_only(self) -> None:
        self.write_registry([structure("built", "player_built", bounds(1, 2))])
        self.write_log(
            "2026-07-10",
            [event_line("2026-07-10T11:00:00Z", "place", (20, 70, 20))],
        )
        before = {
            path.relative_to(self.root).as_posix(): path.read_bytes()
            for path in self.root.rglob("*")
            if path.is_file()
        }

        self.snapshot()

        after = {
            path.relative_to(self.root).as_posix(): path.read_bytes()
            for path in self.root.rglob("*")
            if path.is_file()
        }
        self.assertEqual(after, before)


if __name__ == "__main__":
    unittest.main()
