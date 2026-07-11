from __future__ import annotations

import copy
from collections import Counter
from collections.abc import Iterable, Mapping
from dataclasses import dataclass
from datetime import datetime, timedelta
from hashlib import sha256
from typing import Any, Callable, TypeVar

from picasso.models.block import BlockPos
from picasso.models.player_activity import (
    ActivitySite,
    BlockBounds,
    BuildEvent,
    PaletteCount,
    PlayerEventCounts,
    ProtectionArea,
    ProtectionDecision,
    ProtectionProvenance,
    ProtectionStatus,
    format_utc_datetime,
    parse_utc_datetime,
)


@dataclass(frozen=True)
class PlayerProtectionConfig:
    lookback_days: int = 14
    min_events: int = 3
    join_dist: int = 16
    join_gap_minutes: int = 90

    def __post_init__(self) -> None:
        for name in ("lookback_days", "min_events", "join_dist", "join_gap_minutes"):
            value = getattr(self, name)
            if isinstance(value, bool) or not isinstance(value, int):
                raise ValueError(f"{name} must be an integer")
        if self.lookback_days < 1:
            raise ValueError("lookback_days must be >= 1")
        if self.min_events < 1:
            raise ValueError("min_events must be >= 1")
        if self.join_dist < 0:
            raise ValueError("join_dist must be >= 0")
        if self.join_gap_minutes < 1:
            raise ValueError("join_gap_minutes must be >= 1")

    @property
    def join_gap(self) -> timedelta:
        return timedelta(minutes=self.join_gap_minutes)


@dataclass
class _SiteBuilder:
    first: BuildEvent
    last_event: datetime
    bounds: BlockBounds
    event_count: int
    places: int
    breaks: int
    players: dict[str, list[int]]
    palette: Counter[str]
    events: list[BuildEvent]

    @classmethod
    def start(cls, event: BuildEvent) -> "_SiteBuilder":
        places = 1 if event.action == "place" else 0
        breaks = 1 if event.action == "break" else 0
        return cls(
            first=event,
            last_event=event.timestamp,
            bounds=BlockBounds.from_positions((event.pos,)),
            event_count=1,
            places=places,
            breaks=breaks,
            players={event.player: [places, breaks]},
            palette=Counter({event.block: 1}),
            events=[event],
        )

    @property
    def dimension(self) -> str:
        return self.first.dimension

    def add(self, event: BuildEvent) -> None:
        self.last_event = event.timestamp
        self.bounds = self.bounds.include(event.pos)
        self.event_count += 1
        place_delta = 1 if event.action == "place" else 0
        break_delta = 1 if event.action == "break" else 0
        self.places += place_delta
        self.breaks += break_delta
        counts = self.players.setdefault(event.player, [0, 0])
        counts[0] += place_delta
        counts[1] += break_delta
        self.palette[event.block] += 1
        self.events.append(event)

    def finish(self) -> ActivitySite:
        timestamp = format_utc_datetime(self.first.timestamp)
        pos = self.first.pos
        digest = sha256(f"{timestamp}|{pos.x},{pos.y},{pos.z}".encode("utf-8")).hexdigest()
        player_counts = tuple(
            PlayerEventCounts(player, counts[0], counts[1])
            for player, counts in sorted(self.players.items())
        )
        palette_counts = tuple(
            PaletteCount(block, count)
            for block, count in sorted(
                self.palette.items(), key=lambda item: (-item[1], item[0])
            )[:8]
        )
        vertical_spread = self.bounds.y_max - self.bounds.y_min
        if self.places > self.breaks and vertical_spread >= 2 and len(self.palette) >= 2:
            character = "construction"
        elif self.breaks > self.places:
            character = "excavation"
        else:
            character = "mixed"
        return ActivitySite(
            site_id=f"site_{digest[:12]}",
            dimension=self.dimension,
            bounds=self.bounds,
            first_event=self.first.timestamp,
            last_event=self.last_event,
            players=player_counts,
            top_palette=palette_counts,
            event_count=self.event_count,
            net_blocks=self.places - self.breaks,
            character=character,
        )


@dataclass(frozen=True)
class ActivitySiteEventGroup:
    site: ActivitySite
    events: tuple[BuildEvent, ...]


def _cluster_activity_site_builders(
    events: Iterable[BuildEvent],
    *,
    join_dist: int = 16,
    join_gap: timedelta = timedelta(minutes=90),
    min_events: int = 8,
) -> tuple[_SiteBuilder, ...]:

    if isinstance(join_dist, bool) or not isinstance(join_dist, int) or join_dist < 0:
        raise ValueError("join_dist must be a non-negative integer")
    if not isinstance(join_gap, timedelta) or join_gap <= timedelta(0):
        raise ValueError("join_gap must be a positive timedelta")
    if isinstance(min_events, bool) or not isinstance(min_events, int) or min_events < 1:
        raise ValueError("min_events must be a positive integer")

    normalized: list[BuildEvent] = []
    for event in events:
        if not isinstance(event, BuildEvent):
            raise TypeError("events must contain BuildEvent values")
        normalized.append(event)
    normalized.sort(key=lambda item: item.sort_key)

    active: list[_SiteBuilder] = []
    complete: list[_SiteBuilder] = []
    max_distance_squared = join_dist * join_dist

    for event in normalized:
        still_active: list[_SiteBuilder] = []
        for site in active:
            if event.timestamp - site.last_event <= join_gap:
                still_active.append(site)
            else:
                complete.append(site)
        active = still_active

        candidates: list[tuple[tuple[Any, ...], _SiteBuilder]] = []
        for site in active:
            if site.dimension != event.dimension:
                continue
            distance_squared = site.bounds.horizontal_distance_squared(event.pos)
            if distance_squared <= max_distance_squared:
                candidates.append(
                    (
                        (distance_squared, *site.first.sort_key),
                        site,
                    )
                )

        if candidates:
            _, selected = min(candidates, key=lambda item: item[0])
            selected.add(event)
        else:
            active.append(_SiteBuilder.start(event))

    complete.extend(active)
    retained = [site for site in complete if site.event_count >= min_events]
    return tuple(
        sorted(retained, key=lambda site: (site.first.timestamp, site.finish().site_id))
    )


def cluster_activity_event_groups(
    events: Iterable[BuildEvent],
    *,
    join_dist: int = 16,
    join_gap: timedelta = timedelta(minutes=90),
    min_events: int = 8,
) -> tuple[ActivitySiteEventGroup, ...]:
    """Return deterministic sites together with their exact member events."""

    builders = _cluster_activity_site_builders(
        events,
        join_dist=join_dist,
        join_gap=join_gap,
        min_events=min_events,
    )
    return tuple(
        ActivitySiteEventGroup(site=builder.finish(), events=tuple(builder.events))
        for builder in builders
    )


def cluster_activity_sites(
    events: Iterable[BuildEvent],
    *,
    join_dist: int = 16,
    join_gap: timedelta = timedelta(minutes=90),
    min_events: int = 8,
) -> tuple[ActivitySite, ...]:
    """Cluster build events with the deterministic v1 greedy algorithm."""

    return tuple(
        group.site
        for group in cluster_activity_event_groups(
            events,
            join_dist=join_dist,
            join_gap=join_gap,
            min_events=min_events,
        )
    )


_T = TypeVar("_T")
_MAX_MALFORMED_DIAGNOSTICS = 50


@dataclass(frozen=True)
class _SourceResult:
    values: tuple[Any, ...]
    diagnostics: tuple[ProtectionProvenance, ...]
    status: ProtectionStatus
    records_seen: int
    records_invalid: int


def _safe_error_detail(exc: Exception) -> str:
    rendered = f"{type(exc).__name__}: {exc}".replace("\r", " ").replace("\n", " ")
    return rendered[:300]


def _read_source(
    source: Iterable[_T | Mapping[str, Any]] | None,
    *,
    source_name: str,
    expected_type: type[_T],
    parser: Callable[[object], _T],
) -> _SourceResult:
    if source is None:
        diagnostic = ProtectionProvenance(
            source=source_name,
            reason="source_unavailable",
            detail=f"{source_name} was not provided",
        )
        return _SourceResult((), (diagnostic,), "unavailable", 0, 0)
    if isinstance(source, Mapping) or isinstance(source, (str, bytes)):
        diagnostic = ProtectionProvenance(
            source=source_name,
            reason="source_read_error",
            detail=f"{source_name} must be an iterable of records",
        )
        return _SourceResult((), (diagnostic,), "unavailable", 0, 0)

    values: list[_T] = []
    diagnostics: list[ProtectionProvenance] = []
    records_seen = 0
    records_invalid = 0
    try:
        iterator = iter(source)
        for index, item in enumerate(iterator):
            records_seen += 1
            try:
                value = item if isinstance(item, expected_type) else parser(item)
            except (TypeError, ValueError, KeyError) as exc:
                records_invalid += 1
                if records_invalid <= _MAX_MALFORMED_DIAGNOSTICS:
                    diagnostics.append(
                        ProtectionProvenance(
                            source=source_name,
                            source_id=str(index),
                            reason="malformed_record_skipped",
                            detail=_safe_error_detail(exc),
                        )
                    )
                continue
            values.append(value)
    except Exception as exc:
        diagnostics.append(
            ProtectionProvenance(
                source=source_name,
                reason="source_read_error",
                detail=_safe_error_detail(exc),
            )
        )
        return _SourceResult(
            tuple(values),
            tuple(diagnostics),
            "unavailable",
            records_seen,
            records_invalid,
        )

    if records_seen and not values:
        diagnostics.append(
            ProtectionProvenance(
                source=source_name,
                reason="all_records_invalid",
                detail=f"all {records_seen} records were malformed",
            )
        )
        return _SourceResult(
            (), tuple(diagnostics), "unavailable", records_seen, records_invalid
        )
    return _SourceResult(
        tuple(values), tuple(diagnostics), "active", records_seen, records_invalid
    )


_REASON_PRIORITY = {
    "player_modified_position": 0,
    "player_built_structure": 1,
    "protected_region": 2,
    "recent_player_activity": 3,
}


class PlayerProtectionEvaluator:
    """Immutable protection snapshot suitable for later WriteChoke injection."""

    def __init__(
        self,
        *,
        as_of: datetime,
        dimension: str,
        config: PlayerProtectionConfig,
        status: ProtectionStatus,
        sites: tuple[ActivitySite, ...],
        areas: tuple[ProtectionArea, ...],
        diagnostics: tuple[ProtectionProvenance, ...],
        records_seen: int,
        records_invalid: int,
        source_summary: Mapping[str, Any] | None = None,
    ) -> None:
        self._as_of = as_of
        self._dimension = dimension
        self._config = config
        self._status = status
        self._sites = sites
        self._areas = areas
        self._diagnostics = diagnostics
        self._records_seen = records_seen
        self._records_invalid = records_invalid
        self._source_summary = copy.deepcopy(dict(source_summary or {}))

    @classmethod
    def from_sources(
        cls,
        *,
        activity_events: Iterable[BuildEvent | Mapping[str, Any]] | None,
        protection_areas: Iterable[ProtectionArea | Mapping[str, Any]] | None,
        as_of: datetime,
        config: PlayerProtectionConfig | None = None,
        dimension: str = "minecraft:overworld",
        source_summary: Mapping[str, Any] | None = None,
    ) -> "PlayerProtectionEvaluator":
        config = config or PlayerProtectionConfig()
        as_of_utc = parse_utc_datetime(as_of, "as_of")
        if not isinstance(dimension, str):
            raise ValueError("dimension must be namespaced")
        namespace, separator, name = dimension.partition(":")
        if not separator or not namespace or not name:
            raise ValueError("dimension must be namespaced")

        event_source = _read_source(
            activity_events,
            source_name="activity_events",
            expected_type=BuildEvent,
            parser=BuildEvent.from_record,
        )
        area_source = _read_source(
            protection_areas,
            source_name="protection_areas",
            expected_type=ProtectionArea,
            parser=ProtectionArea.from_record,
        )
        diagnostics = event_source.diagnostics + area_source.diagnostics
        records_seen = event_source.records_seen + area_source.records_seen
        records_invalid = event_source.records_invalid + area_source.records_invalid

        cutoff = as_of_utc - timedelta(days=config.lookback_days)
        recent_events = tuple(
            event
            for event in event_source.values
            if event.dimension == dimension and cutoff <= event.timestamp <= as_of_utc
        )
        sites = cluster_activity_sites(
            recent_events,
            join_dist=config.join_dist,
            join_gap=config.join_gap,
            min_events=config.min_events,
        )

        areas = tuple(
            sorted(
                (area for area in area_source.values if area.dimension == dimension),
                key=lambda area: (area.kind, area.source, area.area_id),
            )
        )
        status: ProtectionStatus = (
            "active"
            if event_source.status == "active" and area_source.status == "active"
            else "unavailable"
        )
        return cls(
            as_of=as_of_utc,
            dimension=dimension,
            config=config,
            status=status,
            sites=sites,
            areas=areas,
            diagnostics=diagnostics,
            records_seen=records_seen,
            records_invalid=records_invalid,
            source_summary=source_summary,
        )

    @property
    def status(self) -> ProtectionStatus:
        return self._status

    @property
    def sites(self) -> tuple[ActivitySite, ...]:
        return self._sites

    @property
    def areas(self) -> tuple[ProtectionArea, ...]:
        return self._areas

    @property
    def diagnostics(self) -> tuple[ProtectionProvenance, ...]:
        return self._diagnostics

    @property
    def source_summary(self) -> dict[str, Any]:
        return copy.deepcopy(self._source_summary)

    @property
    def cutoff(self) -> datetime:
        return self._as_of - timedelta(days=self._config.lookback_days)

    def evaluate(self, pos: BlockPos) -> ProtectionDecision:
        if not isinstance(pos, BlockPos):
            raise TypeError("pos must be a BlockPos")
        matches = self._position_matches(pos)
        if matches:
            return self._matched_decision(matches)
        if self._status == "unavailable":
            return ProtectionDecision(
                protected=False,
                reason="player_protection_unavailable",
                status="unavailable",
                provenance=self._diagnostics,
            )
        return self._unprotected_decision()

    def evaluate_bounds(self, bounds: BlockBounds) -> ProtectionDecision:
        if not isinstance(bounds, BlockBounds):
            raise TypeError("bounds must be a BlockBounds")
        matches = self._bounds_matches(bounds)
        if matches:
            return self._matched_decision(matches)
        if self._status == "unavailable":
            return ProtectionDecision(
                protected=False,
                reason="player_protection_unavailable",
                status="unavailable",
                provenance=self._diagnostics,
            )
        return self._unprotected_decision()

    def _position_matches(self, pos: BlockPos) -> tuple[ProtectionProvenance, ...]:
        matches = [self._site_provenance(site) for site in self._sites if site.bounds.contains(pos)]
        matches.extend(
            self._area_provenance(area) for area in self._areas if area.contains(pos)
        )
        return self._sort_matches(matches)

    def _bounds_matches(self, bounds: BlockBounds) -> tuple[ProtectionProvenance, ...]:
        matches = [
            self._site_provenance(site)
            for site in self._sites
            if site.bounds.intersects(bounds)
        ]
        matches.extend(
            self._area_provenance(area) for area in self._areas if area.intersects(bounds)
        )
        return self._sort_matches(matches)

    @staticmethod
    def _site_provenance(site: ActivitySite) -> ProtectionProvenance:
        return ProtectionProvenance(
            source="activity_site",
            source_id=site.site_id,
            reason="recent_player_activity",
            dimension=site.dimension,
            bounds=site.bounds,
            event_count=site.event_count,
            players=tuple(item.player for item in site.players),
            first_event=site.first_event,
            last_event=site.last_event,
        )

    @staticmethod
    def _area_provenance(area: ProtectionArea) -> ProtectionProvenance:
        if area.kind == "player_built":
            reason = "player_built_structure"
        elif area.kind == "player_modified":
            reason = "player_modified_position"
        else:
            reason = "protected_region"
        return ProtectionProvenance(
            source=area.source,
            source_id=area.area_id,
            reason=reason,
            dimension=area.dimension,
            bounds=area.bounds,
            position_count=len(area.positions) if area.kind == "player_modified" else None,
        )

    @staticmethod
    def _sort_matches(
        matches: Iterable[ProtectionProvenance],
    ) -> tuple[ProtectionProvenance, ...]:
        return tuple(
            sorted(
                matches,
                key=lambda item: (
                    _REASON_PRIORITY[item.reason],
                    item.source,
                    item.source_id or "",
                ),
            )
        )

    def _matched_decision(
        self, matches: tuple[ProtectionProvenance, ...]
    ) -> ProtectionDecision:
        provenance = matches
        if self._status == "unavailable":
            provenance += self._diagnostics
        return ProtectionDecision(
            protected=True,
            reason=matches[0].reason,
            status=self._status,
            provenance=provenance,
        )

    @staticmethod
    def _unprotected_decision() -> ProtectionDecision:
        return ProtectionDecision(
            protected=False,
            reason="not_player_protected",
            status="active",
        )

    def to_summary(self) -> dict[str, Any]:
        return {
            "status": self._status,
            "dimension": self._dimension,
            "as_of": format_utc_datetime(self._as_of),
            "cutoff": format_utc_datetime(self.cutoff),
            "lookback_days": self._config.lookback_days,
            "min_events": self._config.min_events,
            "site_count": len(self._sites),
            "area_count": len(self._areas),
            "zone_count": len(self._sites) + len(self._areas),
            "records_seen": self._records_seen,
            "records_invalid": self._records_invalid,
            "diagnostics": [item.to_dict() for item in self._diagnostics],
            "sources": copy.deepcopy(self._source_summary),
        }
