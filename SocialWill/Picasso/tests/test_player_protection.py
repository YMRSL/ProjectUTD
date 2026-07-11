from __future__ import annotations

import json
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

from picasso.core.player_protection import (
    PlayerProtectionConfig,
    PlayerProtectionEvaluator,
    cluster_activity_sites,
)
from picasso.models.block import BlockPos
from picasso.models.player_activity import (
    BlockBounds,
    BuildEvent,
    ProtectionArea,
)


UTC = timezone.utc
NOW = datetime(2026, 7, 10, 12, 0, tzinfo=UTC)


def event(
    minute: int,
    x: int,
    *,
    y: int = 70,
    z: int = 0,
    action: str = "place",
    player: str = "Steve",
    block: str = "minecraft:oak_planks",
    dimension: str = "minecraft:overworld",
) -> BuildEvent:
    return BuildEvent(
        timestamp=NOW + timedelta(minutes=minute),
        player=player,
        action=action,
        pos=BlockPos(x, y, z),
        block=block,
        dimension=dimension,
    )


class PlayerActivityModelTests(unittest.TestCase):
    def test_build_event_record_is_strict_about_major_and_utc_but_ignores_additions(self) -> None:
        record = {
            "v": 1,
            "t": "2026-07-09T12:00:00.000Z",
            "player": "Steve",
            "action": "place",
            "pos": {"x": 1, "y": 70, "z": 2},
            "block": "minecraft:stone",
            "dim": "minecraft:overworld",
            "future_minor_field": {"ignored": True},
        }

        parsed = BuildEvent.from_record(record)

        self.assertEqual(parsed.pos, BlockPos(1, 70, 2))
        self.assertEqual(parsed.timestamp.tzinfo, UTC)
        with self.assertRaisesRegex(ValueError, "unsupported.*major"):
            BuildEvent.from_record({**record, "v": 2})
        with self.assertRaisesRegex(ValueError, "must use UTC"):
            BuildEvent.from_record({**record, "t": "2026-07-09T14:00:00+02:00"})

    def test_block_bounds_are_inclusive_and_use_horizontal_euclidean_distance(self) -> None:
        bounds = BlockBounds(0, 4, 10, 20, -2, 2)

        self.assertTrue(bounds.contains(BlockPos(4, 20, 2)))
        self.assertFalse(bounds.contains(BlockPos(5, 20, 2)))
        self.assertEqual(bounds.horizontal_distance_squared(BlockPos(7, 99, 6)), 25)
        self.assertTrue(bounds.intersects(BlockBounds(4, 8, 20, 30, 2, 5)))

    def test_player_modified_area_protects_positions_not_its_whole_bbox(self) -> None:
        area = ProtectionArea(
            area_id="structure_007",
            kind="player_modified",
            bounds=BlockBounds(10, 20, 60, 80, 10, 20),
            positions=frozenset({BlockPos(11, 70, 11)}),
        )

        self.assertTrue(area.contains(BlockPos(11, 70, 11)))
        self.assertFalse(area.contains(BlockPos(12, 70, 12)))
        with self.assertRaisesRegex(ValueError, "requires attributed positions"):
            ProtectionArea(
                area_id="structure_bad",
                kind="player_modified",
                bounds=BlockBounds(0, 1, 0, 1, 0, 1),
            )
        with self.assertRaisesRegex(ValueError, "bounds must be a BlockBounds"):
            ProtectionArea(
                area_id="structure_bad_bounds",
                kind="player_built",
                bounds={"x_min": 0},  # type: ignore[arg-type]
            )


class ActivityClusteringTests(unittest.TestCase):
    def test_clustering_is_input_order_independent_and_uses_older_site_tiebreak(self) -> None:
        events = [
            event(-10, 0),
            event(-9, 30, player="Alex"),
            event(-8, 10),
            # Distance 10 from both current bboxes; the older x=0 site wins.
            event(-7, 20, player="Alex"),
        ]

        forward = cluster_activity_sites(events, join_dist=10, min_events=1)
        reverse = cluster_activity_sites(reversed(events), join_dist=10, min_events=1)

        self.assertEqual(
            [site.to_dict() for site in forward],
            [site.to_dict() for site in reverse],
        )
        self.assertEqual(len(forward), 2)
        self.assertEqual(forward[0].bounds.x_min, 0)
        self.assertEqual(forward[0].bounds.x_max, 20)
        self.assertEqual(forward[0].event_count, 3)
        self.assertEqual(forward[1].bounds.x_min, 30)

    def test_sites_never_merge_and_join_gap_is_inclusive(self) -> None:
        start = event(-200, 0)
        at_boundary = BuildEvent(
            timestamp=start.timestamp + timedelta(minutes=90),
            player="Steve",
            action="place",
            pos=BlockPos(1, 70, 0),
            block="minecraft:stone",
        )
        after_boundary = BuildEvent(
            timestamp=at_boundary.timestamp + timedelta(minutes=90, seconds=1),
            player="Steve",
            action="place",
            pos=BlockPos(2, 70, 0),
            block="minecraft:stone",
        )

        sites = cluster_activity_sites(
            (after_boundary, at_boundary, start),
            join_dist=16,
            min_events=1,
        )

        self.assertEqual([site.event_count for site in sites], [2, 1])

    def test_site_ids_are_stable_and_player_stats_are_aggregated(self) -> None:
        events = (
            event(-3, 0, player="Steve"),
            event(-2, 1, player="Alex", action="break", block="minecraft:dirt"),
            event(-1, 2, player="Steve"),
        )

        site = cluster_activity_sites(events, min_events=3)[0]

        self.assertRegex(site.site_id, r"^site_[0-9a-f]{12}$")
        self.assertEqual(site.event_count, 3)
        self.assertEqual(site.net_blocks, 1)
        self.assertEqual(
            [(counts.player, counts.places, counts.breaks) for counts in site.players],
            [("Alex", 0, 1), ("Steve", 2, 0)],
        )


class PlayerProtectionEvaluatorTests(unittest.TestCase):
    def _evaluator(
        self,
        *,
        events=(),
        areas=(),
        lookback_days: int = 14,
        min_events: int = 3,
    ) -> PlayerProtectionEvaluator:
        return PlayerProtectionEvaluator.from_sources(
            activity_events=events,
            protection_areas=areas,
            as_of=NOW,
            config=PlayerProtectionConfig(
                lookback_days=lookback_days,
                min_events=min_events,
            ),
        )

    def test_fixture_events_create_a_recent_bbox_protection_zone(self) -> None:
        fixture = Path(__file__).parent / "fixtures" / "player_activity_events.jsonl"
        records = [json.loads(line) for line in fixture.read_text(encoding="utf-8").splitlines()]

        evaluator = self._evaluator(events=records)
        decision = evaluator.evaluate(BlockPos(101, 70, -20))

        self.assertEqual(evaluator.status, "active")
        self.assertTrue(decision.protected)
        self.assertEqual(decision.reason, "recent_player_activity")
        self.assertEqual(decision.provenance[0].event_count, 3)
        self.assertEqual(decision.provenance[0].players, ("Alex", "Steve"))

    def test_lookback_and_protection_min_events_are_independent_knobs(self) -> None:
        cutoff = NOW - timedelta(days=2)
        events = [
            BuildEvent(cutoff, "Steve", "place", BlockPos(0, 70, 0), "minecraft:stone"),
            BuildEvent(
                cutoff + timedelta(minutes=1),
                "Steve",
                "place",
                BlockPos(1, 70, 0),
                "minecraft:stone",
            ),
            BuildEvent(
                cutoff + timedelta(minutes=2),
                "Steve",
                "place",
                BlockPos(2, 70, 0),
                "minecraft:stone",
            ),
            BuildEvent(
                cutoff - timedelta(seconds=1),
                "Steve",
                "place",
                BlockPos(3, 70, 0),
                "minecraft:stone",
            ),
        ]

        protected = self._evaluator(events=events, lookback_days=2, min_events=3)
        filtered = self._evaluator(events=events, lookback_days=2, min_events=4)

        self.assertTrue(protected.evaluate(BlockPos(1, 70, 0)).protected)
        self.assertFalse(filtered.evaluate(BlockPos(1, 70, 0)).protected)
        self.assertEqual(filtered.evaluate(BlockPos(1, 70, 0)).reason, "not_player_protected")

    def test_future_and_other_dimension_events_do_not_form_protection_sites(self) -> None:
        events = [
            event(1, 0),
            event(2, 1),
            event(3, 2),
            event(-3, 0, dimension="minecraft:the_nether"),
            event(-2, 1, dimension="minecraft:the_nether"),
            event(-1, 2, dimension="minecraft:the_nether"),
        ]

        evaluator = self._evaluator(events=events)

        self.assertEqual(evaluator.sites, ())
        self.assertFalse(evaluator.evaluate(BlockPos(1, 70, 0)).protected)

    def test_registry_bounds_and_modified_positions_are_unioned(self) -> None:
        areas = [
            ProtectionArea(
                "built",
                "player_built",
                bounds=BlockBounds(10, 12, 60, 80, 10, 12),
            ),
            ProtectionArea(
                "modified",
                "player_modified",
                bounds=BlockBounds(20, 29, 60, 80, 20, 29),
                positions=frozenset({BlockPos(21, 70, 21)}),
            ),
            ProtectionArea(
                "ops-zone",
                "protected_region",
                bounds=BlockBounds(40, 42, 60, 80, 40, 42),
                source="operator_policy",
            ),
        ]
        evaluator = self._evaluator(areas=areas)

        self.assertEqual(
            evaluator.evaluate(BlockPos(11, 70, 11)).reason,
            "player_built_structure",
        )
        self.assertEqual(
            evaluator.evaluate(BlockPos(21, 70, 21)).reason,
            "player_modified_position",
        )
        self.assertFalse(evaluator.evaluate(BlockPos(22, 70, 22)).protected)
        self.assertEqual(
            evaluator.evaluate(BlockPos(41, 70, 41)).reason,
            "protected_region",
        )

    def test_overlap_reason_and_provenance_order_do_not_depend_on_area_order(self) -> None:
        built = ProtectionArea(
            "z-built",
            "player_built",
            bounds=BlockBounds(0, 2, 60, 80, 0, 2),
        )
        modified = ProtectionArea(
            "a-modified",
            "player_modified",
            bounds=BlockBounds(0, 2, 60, 80, 0, 2),
            positions=frozenset({BlockPos(1, 70, 1)}),
        )
        events = [event(-3, 0, z=0), event(-2, 1, z=1), event(-1, 2, z=2)]

        first = self._evaluator(events=events, areas=[built, modified]).evaluate(
            BlockPos(1, 70, 1)
        )
        second = self._evaluator(events=reversed(events), areas=[modified, built]).evaluate(
            BlockPos(1, 70, 1)
        )

        self.assertEqual(first.to_dict(), second.to_dict())
        self.assertEqual(first.reason, "player_modified_position")
        self.assertEqual(
            [item.reason for item in first.provenance],
            [
                "player_modified_position",
                "player_built_structure",
                "recent_player_activity",
            ],
        )

    def test_bounds_query_detects_bbox_and_exact_position_intersections(self) -> None:
        area = ProtectionArea(
            "modified",
            "player_modified",
            bounds=BlockBounds(0, 100, 0, 100, 0, 100),
            positions=frozenset({BlockPos(75, 50, 75)}),
        )
        evaluator = self._evaluator(areas=[area])

        self.assertFalse(
            evaluator.evaluate_bounds(BlockBounds(10, 20, 10, 20, 10, 20)).protected
        )
        self.assertTrue(
            evaluator.evaluate_bounds(BlockBounds(74, 76, 49, 51, 74, 76)).protected
        )

    def test_empty_successful_sources_are_active_with_zero_zones(self) -> None:
        evaluator = self._evaluator()

        self.assertEqual(evaluator.status, "active")
        self.assertEqual(evaluator.sites, ())
        self.assertEqual(evaluator.areas, ())
        self.assertEqual(evaluator.to_summary()["records_invalid"], 0)
        self.assertEqual(
            evaluator.evaluate(BlockPos(0, 70, 0)).reason,
            "not_player_protected",
        )

    def test_missing_or_failed_source_is_unavailable_without_protecting_positions(self) -> None:
        missing = PlayerProtectionEvaluator.from_sources(
            activity_events=None,
            protection_areas=(),
            as_of=NOW,
        )

        def broken_reader():
            yield event(-1, 0)
            raise OSError("read failed")

        failed = PlayerProtectionEvaluator.from_sources(
            activity_events=broken_reader(),
            protection_areas=(),
            as_of=NOW,
            config=PlayerProtectionConfig(min_events=1),
        )

        missing_decision = missing.evaluate(BlockPos(0, 70, 0))
        self.assertEqual(missing.status, "unavailable")
        self.assertFalse(missing_decision.protected)
        self.assertEqual(missing_decision.reason, "player_protection_unavailable")
        self.assertEqual(missing.sites, ())

        partial_decision = failed.evaluate(BlockPos(0, 70, 0))
        self.assertEqual(failed.status, "unavailable")
        self.assertEqual(len(failed.sites), 1)
        self.assertTrue(partial_decision.protected)
        self.assertEqual(partial_decision.status, "unavailable")
        self.assertEqual(partial_decision.reason, "recent_player_activity")
        self.assertEqual(
            [item.reason for item in partial_decision.provenance],
            ["recent_player_activity", "source_read_error"],
        )
        unmatched = failed.evaluate(BlockPos(100, 70, 100))
        self.assertFalse(unmatched.protected)
        self.assertEqual(unmatched.reason, "player_protection_unavailable")

    def test_missing_activity_source_preserves_known_registry_protection(self) -> None:
        area = ProtectionArea(
            "known-build",
            "player_built",
            bounds=BlockBounds(10, 12, 60, 80, 10, 12),
        )
        evaluator = PlayerProtectionEvaluator.from_sources(
            activity_events=None,
            protection_areas=[area],
            as_of=NOW,
        )

        decision = evaluator.evaluate(BlockPos(11, 70, 11))

        self.assertEqual(evaluator.status, "unavailable")
        self.assertEqual(evaluator.areas, (area,))
        self.assertTrue(decision.protected)
        self.assertEqual(decision.status, "unavailable")
        self.assertEqual(decision.reason, "player_built_structure")
        self.assertEqual(
            [item.reason for item in decision.provenance],
            ["player_built_structure", "source_unavailable"],
        )
        outside = evaluator.evaluate(BlockPos(20, 70, 20))
        self.assertFalse(outside.protected)
        self.assertEqual(outside.reason, "player_protection_unavailable")

    def test_missing_area_source_preserves_known_activity_protection(self) -> None:
        events = [event(-3, 0), event(-2, 1), event(-1, 2)]
        evaluator = PlayerProtectionEvaluator.from_sources(
            activity_events=events,
            protection_areas=None,
            as_of=NOW,
            config=PlayerProtectionConfig(min_events=3),
        )

        decision = evaluator.evaluate(BlockPos(1, 70, 0))

        self.assertEqual(evaluator.status, "unavailable")
        self.assertEqual(len(evaluator.sites), 1)
        self.assertTrue(decision.protected)
        self.assertEqual(decision.status, "unavailable")
        self.assertEqual(decision.reason, "recent_player_activity")
        self.assertEqual(
            [item.reason for item in decision.provenance],
            ["recent_player_activity", "source_unavailable"],
        )

    def test_malformed_record_is_counted_and_skipped_when_valid_data_remains(self) -> None:
        valid = event(-1, 5).to_dict()
        evaluator = self._evaluator(events=[{"v": 1}, valid], min_events=1)

        self.assertEqual(evaluator.status, "active")
        self.assertEqual(evaluator.to_summary()["records_invalid"], 1)
        self.assertEqual(evaluator.diagnostics[0].reason, "malformed_record_skipped")
        self.assertTrue(evaluator.evaluate(BlockPos(5, 70, 0)).protected)

    def test_nonempty_all_malformed_source_cannot_masquerade_as_active_empty(self) -> None:
        evaluator = self._evaluator(events=[{"v": 1}, "not an event"])

        self.assertEqual(evaluator.status, "unavailable")
        self.assertEqual(evaluator.to_summary()["records_invalid"], 2)
        self.assertEqual(evaluator.diagnostics[-1].reason, "all_records_invalid")
        self.assertFalse(evaluator.evaluate(BlockPos(0, 70, 0)).protected)

    def test_malformed_diagnostics_are_capped_without_truncating_invalid_count(self) -> None:
        malformed = [{"v": 1} for _ in range(75)]
        evaluator = self._evaluator(events=[*malformed, event(-1, 5).to_dict()], min_events=1)

        self.assertEqual(evaluator.status, "active")
        self.assertEqual(evaluator.to_summary()["records_invalid"], 75)
        self.assertEqual(
            sum(
                item.reason == "malformed_record_skipped"
                for item in evaluator.diagnostics
            ),
            50,
        )

    def test_invalid_config_is_rejected_before_evaluation(self) -> None:
        with self.assertRaisesRegex(ValueError, "lookback_days"):
            PlayerProtectionConfig(lookback_days=0)
        with self.assertRaisesRegex(ValueError, "min_events"):
            PlayerProtectionConfig(min_events=0)


if __name__ == "__main__":
    unittest.main()
