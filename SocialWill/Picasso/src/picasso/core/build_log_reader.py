from __future__ import annotations

import json
import re
from collections import Counter
from collections.abc import Iterator
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any

from picasso.core.player_protection import (
    ActivitySiteEventGroup,
    cluster_activity_event_groups,
    cluster_activity_sites,
)
from picasso.models.player_activity import (
    ActivitySite,
    BlockBounds,
    BuildEvent,
    PaletteCount,
    format_utc_datetime,
    parse_utc_datetime,
)


_DATE_FILE_RE = re.compile(r"^(\d{4}-\d{2}-\d{2})\.jsonl$")
_MISSING = object()
_MAX_MALFORMED_DIAGNOSTICS = 50
_OVERWORLD = "minecraft:overworld"


class BuildLogUnavailable(RuntimeError):
    """The configured build-log source cannot be queried."""

    code = "build_log_not_configured"


class ActivitySiteNotFound(LookupError):
    def __init__(self, site_id: str) -> None:
        super().__init__(f"Activity site not found: {site_id}")
        self.site_id = site_id


@dataclass(frozen=True)
class LogCoverage:
    from_date: date | None
    to_date: date | None

    def to_dict(self) -> dict[str, str | None]:
        return {
            "from": self.from_date.isoformat() if self.from_date is not None else None,
            "to": self.to_date.isoformat() if self.to_date is not None else None,
        }


@dataclass(frozen=True)
class BuildLogDiagnostic:
    reason: str
    file: str
    line: int | None = None
    detail: str | None = None

    def to_dict(self) -> dict[str, Any]:
        result: dict[str, Any] = {"reason": self.reason, "file": self.file}
        if self.line is not None:
            result["line"] = self.line
        if self.detail is not None:
            result["detail"] = self.detail
        return result


@dataclass(frozen=True)
class BuildLogQueryStats:
    events_total: int
    events_valid_v1: int
    lines_read: int
    lines_skipped: int
    events_skipped_dimension: int
    events_skipped_unknown_major: int
    files_consulted: tuple[str, ...]
    log_coverage: LogCoverage
    diagnostics: tuple[BuildLogDiagnostic, ...] = ()

    def to_dict(self) -> dict[str, Any]:
        return {
            "events_total": self.events_total,
            "events_valid_v1": self.events_valid_v1,
            "lines_read": self.lines_read,
            "lines_skipped": self.lines_skipped,
            "events_skipped_dimension": self.events_skipped_dimension,
            "events_skipped_unknown_major": self.events_skipped_unknown_major,
            "files_consulted": list(self.files_consulted),
            "log_coverage": self.log_coverage.to_dict(),
            "diagnostics": [item.to_dict() for item in self.diagnostics],
        }


@dataclass(frozen=True)
class BuildEventQueryResult:
    events: tuple[BuildEvent, ...]
    stats: BuildLogQueryStats

    def __iter__(self) -> Iterator[BuildEvent]:
        return iter(self.events)

    def __len__(self) -> int:
        return len(self.events)


@dataclass(frozen=True)
class ActivitySiteQueryResult:
    sites: tuple[ActivitySite, ...]
    stats: BuildLogQueryStats

    def to_dict(self) -> dict[str, Any]:
        return {
            "sites": [site.to_dict() for site in self.sites],
            **self.stats.to_dict(),
        }


@dataclass(frozen=True)
class ActivityTimelineBucket:
    hour: datetime
    event_count: int

    def to_dict(self) -> dict[str, Any]:
        return {
            "hour": format_utc_datetime(self.hour),
            "event_count": self.event_count,
        }


@dataclass(frozen=True)
class ActivitySiteDetailResult:
    site: ActivitySite
    timeline: tuple[ActivityTimelineBucket, ...]
    palette_full: tuple[PaletteCount, ...]
    stats: BuildLogQueryStats

    def to_dict(self) -> dict[str, Any]:
        return {
            **self.site.to_dict(),
            "timeline": [bucket.to_dict() for bucket in self.timeline],
            "palette_full": [
                {"block": item.block, "count": item.count} for item in self.palette_full
            ],
            "query_stats": self.stats.to_dict(),
        }


@dataclass
class _MutableStats:
    events_valid_v1: int = 0
    lines_read: int = 0
    lines_skipped: int = 0
    events_skipped_dimension: int = 0
    events_skipped_unknown_major: int = 0
    diagnostics: list[BuildLogDiagnostic] = field(default_factory=list)
    malformed_diagnostics: int = 0
    unknown_major_files: set[str] = field(default_factory=set)

    def malformed(self, path: Path, line_number: int, detail: str) -> None:
        self.lines_skipped += 1
        if self.malformed_diagnostics >= _MAX_MALFORMED_DIAGNOSTICS:
            return
        self.malformed_diagnostics += 1
        self.diagnostics.append(
            BuildLogDiagnostic(
                reason="malformed_line_skipped",
                file=path.name,
                line=line_number,
                detail=detail[:300],
            )
        )

    def unknown_major(self, path: Path, line_number: int, version: int) -> None:
        self.events_skipped_unknown_major += 1
        if path.name in self.unknown_major_files:
            return
        self.unknown_major_files.add(path.name)
        self.diagnostics.append(
            BuildLogDiagnostic(
                reason="unknown_schema_major",
                file=path.name,
                line=line_number,
                detail=f"unsupported build-event schema major: {version}",
            )
        )


class BuildLogReader:
    """Streaming reader for the recorder plugin's root-level daily JSONL files.

    Query bounds are inclusive: ``since <= event.t <= until``. ``until=None``
    has no upper bound. Every query opens each selected file in a context manager
    and closes it before returning; no file handle or lazy file iterator escapes.
    """

    def __init__(
        self,
        log_dir: Path | str | None,
        *,
        join_dist: int = 16,
        join_gap_minutes: int = 90,
        site_min_events: int = 8,
    ) -> None:
        self._log_dir = Path(log_dir).expanduser() if log_dir is not None else None
        if isinstance(join_dist, bool) or not isinstance(join_dist, int) or join_dist < 0:
            raise ValueError("join_dist must be a non-negative integer")
        if (
            isinstance(join_gap_minutes, bool)
            or not isinstance(join_gap_minutes, int)
            or join_gap_minutes < 1
        ):
            raise ValueError("join_gap_minutes must be a positive integer")
        if (
            isinstance(site_min_events, bool)
            or not isinstance(site_min_events, int)
            or site_min_events < 1
        ):
            raise ValueError("site_min_events must be a positive integer")
        self._join_dist = join_dist
        self._join_gap = timedelta(minutes=join_gap_minutes)
        self._site_min_events = site_min_events

    def events(
        self,
        since: datetime,
        until: datetime | None = None,
        player: str | None = None,
        bounds: BlockBounds | None = None,
    ) -> Iterator[BuildEvent]:
        """Compatibility iterator matching the pipeline spec.

        Use :meth:`query_events` when query statistics are also required.
        """

        return iter(
            self.query_events(
                since,
                until=until,
                player=player,
                bounds=bounds,
            ).events
        )

    def query_events(
        self,
        since: datetime,
        until: datetime | None = None,
        player: str | None = None,
        bounds: BlockBounds | None = None,
    ) -> BuildEventQueryResult:
        since_utc, until_utc = self._validate_query(since, until, player, bounds)
        selected = self._select_files(since_utc.date(), until_utc.date() if until_utc else None)
        mutable_stats = _MutableStats()
        events: list[BuildEvent] = []
        for _, path in selected:
            events.extend(
                self._read_file(
                    path,
                    since=since_utc,
                    until=until_utc,
                    player=player,
                    bounds=bounds,
                    stats=mutable_stats,
                )
            )

        dates = [file_date for file_date, _ in selected]
        coverage = LogCoverage(
            from_date=min(dates) if dates else None,
            to_date=max(dates) if dates else None,
        )
        stats = BuildLogQueryStats(
            events_total=len(events),
            events_valid_v1=mutable_stats.events_valid_v1,
            lines_read=mutable_stats.lines_read,
            lines_skipped=mutable_stats.lines_skipped,
            events_skipped_dimension=mutable_stats.events_skipped_dimension,
            events_skipped_unknown_major=mutable_stats.events_skipped_unknown_major,
            files_consulted=tuple(path.name for _, path in selected),
            log_coverage=coverage,
            diagnostics=tuple(mutable_stats.diagnostics),
        )
        return BuildEventQueryResult(events=tuple(events), stats=stats)

    def sites(
        self,
        since: datetime,
        until: datetime | None = None,
        player: str | None = None,
        bounds: BlockBounds | None = None,
        *,
        min_events: int | None = None,
    ) -> list[ActivitySite]:
        return list(
            self.query_sites(
                since,
                until=until,
                player=player,
                bounds=bounds,
                min_events=min_events,
            ).sites
        )

    def query_sites(
        self,
        since: datetime,
        until: datetime | None = None,
        player: str | None = None,
        bounds: BlockBounds | None = None,
        *,
        min_events: int | None = None,
    ) -> ActivitySiteQueryResult:
        event_result = self.query_events(
            since,
            until=until,
            player=player,
            bounds=bounds,
        )
        sites = cluster_activity_sites(
            event_result.events,
            join_dist=self._join_dist,
            join_gap=self._join_gap,
            min_events=self._resolve_min_events(min_events),
        )
        return ActivitySiteQueryResult(sites=sites, stats=event_result.stats)

    def get_site(
        self,
        site_id: str,
        since: datetime,
        until: datetime | None = None,
        player: str | None = None,
        bounds: BlockBounds | None = None,
        *,
        min_events: int | None = None,
    ) -> ActivitySiteDetailResult:
        if not isinstance(site_id, str) or not site_id:
            raise ValueError("site_id must be a non-empty string")
        event_result = self.query_events(
            since,
            until=until,
            player=player,
            bounds=bounds,
        )
        groups = cluster_activity_event_groups(
            event_result.events,
            join_dist=self._join_dist,
            join_gap=self._join_gap,
            min_events=self._resolve_min_events(min_events),
        )
        group = next((item for item in groups if item.site.site_id == site_id), None)
        if group is None:
            raise ActivitySiteNotFound(site_id)
        return self._site_detail(group, event_result.stats)

    @staticmethod
    def _site_detail(
        group: ActivitySiteEventGroup,
        stats: BuildLogQueryStats,
    ) -> ActivitySiteDetailResult:
        timeline_counts: Counter[datetime] = Counter()
        palette_counts: Counter[str] = Counter()
        for event in group.events:
            hour = event.timestamp.replace(minute=0, second=0, microsecond=0)
            timeline_counts[hour] += 1
            palette_counts[event.block] += 1
        timeline = tuple(
            ActivityTimelineBucket(hour=hour, event_count=count)
            for hour, count in sorted(timeline_counts.items())
        )
        palette = tuple(
            PaletteCount(block=block, count=count)
            for block, count in sorted(
                palette_counts.items(), key=lambda item: (-item[1], item[0])
            )
        )
        return ActivitySiteDetailResult(
            site=group.site,
            timeline=timeline,
            palette_full=palette,
            stats=stats,
        )

    def _select_files(
        self,
        since_date: date,
        until_date: date | None,
    ) -> list[tuple[date, Path]]:
        directory = self._require_directory()
        selected: list[tuple[date, Path]] = []
        try:
            entries = directory.iterdir()
            for path in entries:
                match = _DATE_FILE_RE.fullmatch(path.name)
                if match is None or not path.is_file():
                    continue
                try:
                    file_date = date.fromisoformat(match.group(1))
                except ValueError:
                    continue
                if file_date < since_date:
                    continue
                if until_date is not None and file_date > until_date:
                    continue
                selected.append((file_date, path))
        except OSError as exc:
            raise BuildLogUnavailable(f"Cannot list build-log directory {directory}: {exc}") from exc
        return sorted(selected, key=lambda item: (item[0], item[1].name))

    def _require_directory(self) -> Path:
        if self._log_dir is None:
            raise BuildLogUnavailable("PICASSO_BUILD_LOG_DIR is not configured")
        if not self._log_dir.is_dir():
            raise BuildLogUnavailable(
                f"Build-log directory does not exist or is not a directory: {self._log_dir}"
            )
        return self._log_dir

    @staticmethod
    def _validate_query(
        since: datetime,
        until: datetime | None,
        player: str | None,
        bounds: BlockBounds | None,
    ) -> tuple[datetime, datetime | None]:
        since_utc = parse_utc_datetime(since, "since")
        until_utc = parse_utc_datetime(until, "until") if until is not None else None
        if until_utc is not None and until_utc < since_utc:
            raise ValueError("until must be >= since")
        if player is not None and (not isinstance(player, str) or not player):
            raise ValueError("player must be a non-empty string")
        if bounds is not None and not isinstance(bounds, BlockBounds):
            raise ValueError("bounds must be a BlockBounds")
        return since_utc, until_utc

    def _resolve_min_events(self, value: int | None) -> int:
        resolved = self._site_min_events if value is None else value
        if isinstance(resolved, bool) or not isinstance(resolved, int) or resolved < 1:
            raise ValueError("min_events must be a positive integer")
        return resolved

    @staticmethod
    def _read_file(
        path: Path,
        *,
        since: datetime,
        until: datetime | None,
        player: str | None,
        bounds: BlockBounds | None,
        stats: _MutableStats,
    ) -> list[BuildEvent]:
        events: list[BuildEvent] = []
        try:
            with path.open("r", encoding="utf-8") as handle:
                current = handle.readline()
                line_number = 1
                while current != "":
                    following = handle.readline()
                    is_final_line = following == ""
                    stats.lines_read += 1
                    event = BuildLogReader._parse_line(
                        current,
                        path=path,
                        line_number=line_number,
                        is_final_line=is_final_line,
                        stats=stats,
                    )
                    if event is not None:
                        stats.events_valid_v1 += 1
                        if event.timestamp < since or (
                            until is not None and event.timestamp > until
                        ):
                            pass
                        elif event.dimension != _OVERWORLD:
                            stats.events_skipped_dimension += 1
                        elif player is not None and event.player != player:
                            pass
                        elif bounds is not None and not bounds.contains(event.pos):
                            pass
                        else:
                            events.append(event)
                    current = following
                    line_number += 1
        except (OSError, UnicodeError) as exc:
            raise BuildLogUnavailable(f"Cannot read build-log file {path}: {exc}") from exc
        return events

    @staticmethod
    def _parse_line(
        line: str,
        *,
        path: Path,
        line_number: int,
        is_final_line: bool,
        stats: _MutableStats,
    ) -> BuildEvent | None:
        try:
            record = json.loads(line)
        except json.JSONDecodeError as exc:
            if not is_final_line:
                stats.malformed(path, line_number, f"invalid JSON: {exc.msg}")
            return None

        if isinstance(record, dict):
            version = record.get("v", _MISSING)
            if (
                isinstance(version, int)
                and not isinstance(version, bool)
                and version != 1
            ):
                stats.unknown_major(path, line_number, version)
                return None
        try:
            return BuildEvent.from_record(record)
        except (TypeError, ValueError, KeyError) as exc:
            stats.malformed(path, line_number, f"{type(exc).__name__}: {exc}")
            return None
