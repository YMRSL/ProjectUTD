from __future__ import annotations

import json
import shutil
import tempfile
import unittest
from datetime import datetime, timezone
from pathlib import Path

from picasso.core.build_log_reader import (
    ActivitySiteNotFound,
    BuildLogReader,
    BuildLogUnavailable,
)
from picasso.models.block import BlockPos
from picasso.models.player_activity import BlockBounds


UTC = timezone.utc
FIXTURE_DIR = Path(__file__).parent / "fixtures" / "build_log"


def utc(day: int, hour: int = 0, minute: int = 0) -> datetime:
    return datetime(2026, 7, day, hour, minute, tzinfo=UTC)


class BuildLogReaderTests(unittest.TestCase):
    def test_unconfigured_missing_and_empty_directories_have_distinct_results(self) -> None:
        with self.assertRaises(BuildLogUnavailable):
            BuildLogReader(None).query_events(utc(7))

        with tempfile.TemporaryDirectory() as tmp:
            missing = Path(tmp) / "missing"
            with self.assertRaises(BuildLogUnavailable):
                BuildLogReader(missing).query_events(utc(7))

            empty = Path(tmp) / "empty"
            empty.mkdir()
            result = BuildLogReader(empty).query_events(utc(7))

        self.assertEqual(result.events, ())
        self.assertEqual(result.stats.events_total, 0)
        self.assertEqual(result.stats.files_consulted, ())
        self.assertEqual(result.stats.log_coverage.to_dict(), {"from": None, "to": None})

    def test_stream_stats_torn_tail_and_unknown_major_diagnostics(self) -> None:
        result = BuildLogReader(FIXTURE_DIR).query_events(
            utc(7, 10),
            until=utc(7, 13),
        )

        self.assertEqual(
            [(item.player, item.timestamp, item.pos) for item in result.events],
            [
                ("Steve", utc(7, 12), BlockPos(10, 70, 0)),
                ("Alex", utc(7, 12, 1), BlockPos(11, 70, 0)),
            ],
        )
        self.assertEqual(result.stats.lines_read, 8)
        # The non-tail JSON error is counted; the unparseable final line is silent.
        self.assertEqual(result.stats.lines_skipped, 1)
        self.assertEqual(result.stats.events_skipped_dimension, 1)
        self.assertEqual(result.stats.events_skipped_unknown_major, 2)
        self.assertEqual(
            [item.reason for item in result.stats.diagnostics],
            ["malformed_line_skipped", "unknown_schema_major"],
        )
        self.assertEqual(
            sum(item.reason == "unknown_schema_major" for item in result.stats.diagnostics),
            1,
        )
        self.assertEqual(result.stats.log_coverage.to_dict(), {"from": "2026-07-07", "to": "2026-07-07"})

    def test_since_and_until_are_both_inclusive(self) -> None:
        reader = BuildLogReader(FIXTURE_DIR)

        exact = reader.query_events(utc(7, 12), until=utc(7, 12))
        one_minute = reader.query_events(utc(7, 12), until=utc(7, 12, 1))

        self.assertEqual([item.pos.x for item in exact.events], [10])
        self.assertEqual([item.pos.x for item in one_minute.events], [10, 11])
        with self.assertRaisesRegex(ValueError, "until must be >= since"):
            reader.query_events(utc(8), until=utc(7))

    def test_player_and_inclusive_bounds_filters_compose(self) -> None:
        result = BuildLogReader(FIXTURE_DIR).query_events(
            utc(7),
            until=utc(7, 23, 59),
            player="Steve",
            bounds=BlockBounds(10, 10, 70, 70, 0, 0),
        )

        self.assertEqual(len(result), 1)
        self.assertEqual(result.events[0].pos, BlockPos(10, 70, 0))
        self.assertEqual(list(BuildLogReader(FIXTURE_DIR).events(utc(7, 12), utc(7, 12))), list(result.events))

    def test_date_range_selects_only_root_daily_files_and_reports_actual_coverage(self) -> None:
        reader = BuildLogReader(FIXTURE_DIR)

        boundary = reader.query_events(
            utc(7, 23, 59),
            until=utc(8, 0, 5),
        )
        open_ended = reader.query_events(utc(8))

        self.assertEqual([item.pos.x for item in boundary.events], [100])
        self.assertEqual(
            boundary.stats.files_consulted,
            ("2026-07-07.jsonl", "2026-07-08.jsonl"),
        )
        self.assertEqual(
            boundary.stats.log_coverage.to_dict(),
            {"from": "2026-07-07", "to": "2026-07-08"},
        )
        self.assertEqual(len(open_ended.events), 5)
        self.assertNotIn("Ignored", {item.player for item in open_ended.events})
        self.assertEqual(open_ended.stats.files_consulted, ("2026-07-08.jsonl",))

    def test_sites_reuse_deterministic_clustering_and_allow_minimum_override(self) -> None:
        reader = BuildLogReader(FIXTURE_DIR)

        default_sites = reader.query_sites(utc(8), until=utc(8, 23, 59))
        protected_sites = reader.query_sites(
            utc(8),
            until=utc(8, 23, 59),
            min_events=4,
        )
        all_sites = reader.sites(
            utc(8),
            until=utc(8, 23, 59),
            min_events=1,
        )

        self.assertEqual(default_sites.sites, ())
        self.assertEqual(len(protected_sites.sites), 1)
        self.assertEqual(protected_sites.sites[0].event_count, 4)
        self.assertEqual(protected_sites.stats.events_total, 5)
        self.assertEqual([site.event_count for site in all_sites], [4, 1])

    def test_site_detail_has_exact_timeline_and_full_palette(self) -> None:
        reader = BuildLogReader(FIXTURE_DIR)
        site = reader.sites(
            utc(8),
            until=utc(8, 23, 59),
            min_events=4,
        )[0]

        detail = reader.get_site(
            site.site_id,
            utc(8),
            until=utc(8, 23, 59),
            min_events=4,
        )

        self.assertEqual(detail.site, site)
        self.assertEqual(
            [(bucket.hour, bucket.event_count) for bucket in detail.timeline],
            [(utc(8, 0), 2), (utc(8, 1), 2)],
        )
        self.assertEqual(
            [(item.block, item.count) for item in detail.palette_full],
            [
                ("minecraft:stone", 2),
                ("minecraft:glass", 1),
                ("minecraft:oak_planks", 1),
            ],
        )
        json.dumps(detail.to_dict())
        with self.assertRaises(ActivitySiteNotFound):
            reader.get_site("site_missing", utc(8), min_events=1)

    def test_site_detail_replays_player_and_bounds_filters_that_created_id(self) -> None:
        reader = BuildLogReader(FIXTURE_DIR)
        bounds = BlockBounds(101, 101, 71, 71, 0, 0)
        site = reader.query_sites(
            utc(8),
            until=utc(8, 23, 59),
            player="Alex",
            bounds=bounds,
            min_events=1,
        ).sites[0]

        detail = reader.get_site(
            site.site_id,
            utc(8),
            until=utc(8, 23, 59),
            player="Alex",
            bounds=bounds,
            min_events=1,
        )

        self.assertEqual(detail.site, site)
        self.assertEqual(detail.stats.events_total, 1)
        with self.assertRaises(ActivitySiteNotFound):
            reader.get_site(
                site.site_id,
                utc(8),
                until=utc(8, 23, 59),
                min_events=1,
            )

    def test_each_query_closes_daily_files_before_returning(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "logs"
            root.mkdir()
            source = FIXTURE_DIR / "2026-07-08.jsonl"
            copied = root / source.name
            shutil.copyfile(source, copied)

            result = BuildLogReader(root).query_events(utc(8))
            renamed = root / "2026-07-09.jsonl"
            copied.rename(renamed)

            self.assertEqual(len(result.events), 5)
            self.assertTrue(renamed.exists())

    def test_malformed_diagnostics_are_bounded_while_line_count_is_exact(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            path = root / "2026-07-09.jsonl"
            lines = ["{bad json}" for _ in range(75)]
            lines.append(
                json.dumps(
                    {
                        "v": 1,
                        "t": "2026-07-09T12:00:00.000Z",
                        "player": "Steve",
                        "action": "place",
                        "pos": {"x": 1, "y": 70, "z": 1},
                        "block": "minecraft:stone",
                        "dim": "minecraft:overworld",
                    }
                )
            )
            path.write_text("\n".join(lines), encoding="utf-8")

            result = BuildLogReader(root).query_events(utc(9))

        self.assertEqual(result.stats.lines_skipped, 75)
        self.assertEqual(len(result.stats.diagnostics), 50)
        self.assertEqual(result.stats.events_total, 1)

    def test_query_inputs_require_utc_and_valid_types(self) -> None:
        reader = BuildLogReader(FIXTURE_DIR)
        with self.assertRaisesRegex(ValueError, "must use UTC"):
            reader.query_events(datetime(2026, 7, 8))
        with self.assertRaisesRegex(ValueError, "player"):
            reader.query_events(utc(8), player="")
        with self.assertRaisesRegex(ValueError, "bounds"):
            reader.query_events(utc(8), bounds={})  # type: ignore[arg-type]
        with self.assertRaisesRegex(ValueError, "min_events"):
            reader.query_sites(utc(8), min_events=0)


if __name__ == "__main__":
    unittest.main()
