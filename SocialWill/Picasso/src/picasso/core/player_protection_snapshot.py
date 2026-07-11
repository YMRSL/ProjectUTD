from __future__ import annotations

import json
from collections.abc import Iterable, Iterator, Mapping
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, TypeVar

from picasso.core.build_log_reader import (
    BuildEventQueryResult,
    BuildLogQueryStats,
    BuildLogReader,
    BuildLogUnavailable,
)
from picasso.core.player_protection import (
    PlayerProtectionConfig,
    PlayerProtectionEvaluator,
)
from picasso.models.block import BlockPos
from picasso.models.player_activity import (
    BlockBounds,
    BuildEvent,
    ProtectionArea,
    parse_utc_datetime,
)


_OVERWORLD = "minecraft:overworld"
_MAX_DIAGNOSTICS = 50
_T = TypeVar("_T")


@dataclass(frozen=True)
class _ProtectedStructure:
    structure_id: str
    attribution: str
    bounds: BlockBounds
    attribution_evidence: object = None
    first_built: object = None


@dataclass(frozen=True)
class _RegistryLoad:
    structures: tuple[_ProtectedStructure, ...]
    available: bool
    summary: dict[str, Any]


class _IncompleteSource(RuntimeError):
    pass


def build_player_protection_snapshot(
    world_path: str | Path,
    build_log_dir: str | Path | None,
    as_of: datetime,
    dimension: str = _OVERWORLD,
    *,
    lookback_days: int = 14,
    min_events: int = 3,
    join_dist: int = 16,
    join_gap_minutes: int = 90,
) -> PlayerProtectionEvaluator:
    """Build one read-only, best-effort player-protection snapshot.

    Missing or incomplete sources lower the evaluator status to ``unavailable``
    without discarding protection zones that can still be proven from the other
    source. No registry, log, cache, or sidecar file is ever written.
    """

    as_of_utc = parse_utc_datetime(as_of, "as_of")
    config = PlayerProtectionConfig(
        lookback_days=lookback_days,
        min_events=min_events,
        join_dist=join_dist,
        join_gap_minutes=join_gap_minutes,
    )
    _validate_dimension(dimension)
    registry_path = Path(world_path).expanduser() / "picasso_structures.json"
    registry = _load_registry(registry_path)
    modified = tuple(
        item for item in registry.structures if item.attribution == "player_modified"
    )

    reader = BuildLogReader(
        build_log_dir,
        join_dist=join_dist,
        join_gap_minutes=join_gap_minutes,
        site_min_events=min_events,
    )
    cutoff = as_of_utc - timedelta(days=lookback_days)
    recent_result: BuildEventQueryResult | None = None
    recent_error: str | None = None
    if dimension != _OVERWORLD:
        recent_error = f"build log v1 does not support dimension {dimension}"
    else:
        try:
            recent_result = reader.query_events(cutoff, until=as_of_utc)
        except BuildLogUnavailable as exc:
            recent_error = str(exc)

    if recent_result is None:
        activity_events: Iterable[BuildEvent] | None = None
        recent_summary = {
            "status": "unavailable",
            "error": recent_error or "build log unavailable",
        }
    else:
        recent_summary = _stats_summary(recent_result.stats)
        if _nonempty_all_invalid(recent_result.stats):
            recent_summary["status"] = "unavailable"
            recent_summary["error"] = "non-empty build log contained no valid v1 events"
            activity_events = _values_then_error(
                recent_result.events,
                "non-empty build log contained no valid v1 events",
            )
        else:
            recent_summary["status"] = "active"
            activity_events = recent_result.events

    retained_result: BuildEventQueryResult | None = None
    retained_error: str | None = None
    if modified:
        if dimension != _OVERWORLD:
            retained_error = f"build log v1 does not support dimension {dimension}"
        else:
            try:
                retained_result = reader.query_events(
                    datetime.min.replace(tzinfo=timezone.utc),
                    until=as_of_utc,
                )
            except BuildLogUnavailable as exc:
                retained_error = str(exc)

    if not modified:
        retained_summary: dict[str, Any] = {"status": "not_required"}
    elif retained_result is None:
        retained_summary = {
            "status": "unavailable",
            "error": retained_error or "retained build-log replay unavailable",
        }
    else:
        retained_summary = _stats_summary(retained_result.stats)
        if _nonempty_all_invalid(retained_result.stats):
            retained_summary["status"] = "unavailable"
            retained_summary["error"] = (
                "non-empty retained build log contained no valid v1 events"
            )
        else:
            retained_summary["status"] = "active"

    areas, replay_incomplete, replay_summary = _materialize_areas(
        registry.structures,
        retained_result=retained_result,
        retained_summary=retained_summary,
        dimension=dimension,
    )

    area_incomplete = registry.summary["status"] != "active" or replay_incomplete
    if not registry.available:
        protection_areas: Iterable[ProtectionArea] | None = None
    elif area_incomplete:
        protection_areas = _values_then_error(
            areas,
            "registry protection was only partially reconstructable",
        )
    else:
        protection_areas = areas

    source_summary = {
        "build_log": {
            "recent": recent_summary,
            "retained": retained_summary,
        },
        "registry": registry.summary,
        "modified_replay": replay_summary,
    }
    return PlayerProtectionEvaluator.from_sources(
        activity_events=activity_events,
        protection_areas=protection_areas,
        as_of=as_of_utc,
        config=config,
        dimension=dimension,
        source_summary=source_summary,
    )


def _load_registry(path: Path) -> _RegistryLoad:
    summary: dict[str, Any] = {
        "path": str(path),
        "status": "unavailable",
        "root_format": None,
        "entries_total": 0,
        "entries_invalid": 0,
        "stale_skipped": 0,
        "protected_entries": 0,
        "diagnostics": [],
    }
    if not path.is_file():
        summary["diagnostics"].append(
            {"reason": "registry_missing", "detail": "picasso_structures.json not found"}
        )
        return _RegistryLoad((), False, summary)
    try:
        root = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        summary["diagnostics"].append(
            {"reason": "registry_read_error", "detail": _safe_detail(exc)}
        )
        return _RegistryLoad((), False, summary)

    if isinstance(root, list):
        entries = root
        summary["root_format"] = "list"
    elif isinstance(root, dict) and isinstance(root.get("structures"), list):
        entries = root["structures"]
        summary["root_format"] = "object"
    else:
        summary["diagnostics"].append(
            {
                "reason": "registry_root_invalid",
                "detail": "root must be a list or an object containing structures[]",
            }
        )
        return _RegistryLoad((), False, summary)

    summary["entries_total"] = len(entries)
    structures: list[_ProtectedStructure] = []
    seen_ids: set[str] = set()
    for index, entry in enumerate(entries):
        try:
            parsed, stale = _parse_registry_entry(entry, seen_ids)
            if stale:
                summary["stale_skipped"] += 1
                continue
            if parsed is not None:
                structures.append(parsed)
        except (TypeError, ValueError, KeyError) as exc:
            summary["entries_invalid"] += 1
            if len(summary["diagnostics"]) < _MAX_DIAGNOSTICS:
                summary["diagnostics"].append(
                    {
                        "reason": "registry_entry_invalid",
                        "index": index,
                        "detail": _safe_detail(exc),
                    }
                )

    summary["protected_entries"] = len(structures)
    summary["status"] = "active" if summary["entries_invalid"] == 0 else "unavailable"
    return _RegistryLoad(
        tuple(structures),
        True,
        summary,
    )


def _parse_registry_entry(
    entry: object,
    seen_ids: set[str],
) -> tuple[_ProtectedStructure | None, bool]:
    if not isinstance(entry, Mapping):
        raise ValueError("structure entry must be an object")
    stale = entry.get("stale", False)
    if not isinstance(stale, bool):
        raise ValueError("stale must be a boolean")
    if stale:
        return None, True
    structure_id = entry.get("id")
    if not isinstance(structure_id, str) or not structure_id:
        raise ValueError("structure id must be a non-empty string")
    if structure_id in seen_ids:
        raise ValueError(f"duplicate structure id: {structure_id}")
    detected = entry.get("detected")
    if not isinstance(detected, Mapping):
        raise ValueError("detected must be an object")
    authored = entry.get("authored", {})
    if not isinstance(authored, Mapping):
        raise ValueError("authored must be an object")
    override = authored.get("manual_override", {})
    if override is None:
        override = {}
    if not isinstance(override, Mapping):
        raise ValueError("authored.manual_override must be an object")
    effective = dict(detected)
    effective.update(override)
    attribution = effective.get("attribution", "native")
    if attribution in (None, "native"):
        seen_ids.add(structure_id)
        return None, False
    if attribution not in {"player_built", "player_modified"}:
        raise ValueError(f"unsupported attribution: {attribution!r}")
    bounds = BlockBounds.from_record(effective.get("bounds"), "bounds")
    # Reserve the id only after the entry is otherwise valid. A malformed
    # earlier row must not suppress a later valid protection record with the
    # same id during best-effort loading.
    seen_ids.add(structure_id)
    return (
        _ProtectedStructure(
            structure_id=structure_id,
            attribution=attribution,
            bounds=bounds,
            attribution_evidence=effective.get("attribution_evidence"),
            first_built=effective.get("first_built"),
        ),
        False,
    )


def _materialize_areas(
    structures: tuple[_ProtectedStructure, ...],
    *,
    retained_result: BuildEventQueryResult | None,
    retained_summary: Mapping[str, Any],
    dimension: str,
) -> tuple[tuple[ProtectionArea, ...], bool, dict[str, Any]]:
    areas: list[ProtectionArea] = []
    diagnostics: list[dict[str, Any]] = []
    modified_total = 0
    modified_with_positions = 0
    incomplete = False
    retained_events = tuple(retained_result.events) if retained_result is not None else ()
    retained_stats = retained_result.stats if retained_result is not None else None

    for structure in structures:
        if structure.attribution == "player_built":
            areas.append(
                ProtectionArea(
                    area_id=structure.structure_id,
                    kind="player_built",
                    dimension=dimension,
                    bounds=structure.bounds,
                )
            )
            continue

        modified_total += 1
        reasons: list[str] = []
        if retained_result is None or retained_summary.get("status") != "active":
            reasons.append("retained_log_unavailable")
            active_positions: frozenset[BlockPos] = frozenset()
            saw_place = False
        else:
            active_positions, saw_place = _replay_positions(
                retained_events,
                structure.bounds,
            )
            if retained_stats is not None and (
                retained_stats.lines_skipped > 0
                or retained_stats.events_skipped_unknown_major > 0
            ):
                reasons.append("retained_log_has_skipped_records")
            if not saw_place:
                reasons.append("no_retained_place_evidence")
            if structure.attribution_evidence == "stale":
                reasons.append("attribution_evidence_stale")
            if _evidence_predates_coverage(structure.first_built, retained_stats):
                reasons.append("retained_evidence_expired")

        if active_positions:
            areas.append(
                ProtectionArea(
                    area_id=structure.structure_id,
                    kind="player_modified",
                    dimension=dimension,
                    bounds=structure.bounds,
                    positions=active_positions,
                )
            )
            modified_with_positions += 1
        if reasons:
            incomplete = True
            if len(diagnostics) < _MAX_DIAGNOSTICS:
                diagnostics.append(
                    {
                        "structure_id": structure.structure_id,
                        "reasons": reasons,
                        "positions_reconstructed": len(active_positions),
                    }
                )

    summary = {
        "status": "unavailable" if incomplete else "active",
        "structures_total": modified_total,
        "structures_with_positions": modified_with_positions,
        "diagnostics": diagnostics,
    }
    return tuple(areas), incomplete, summary


def _replay_positions(
    events: Iterable[BuildEvent],
    bounds: BlockBounds,
) -> tuple[frozenset[BlockPos], bool]:
    active: set[BlockPos] = set()
    saw_place = False
    for event in sorted(events, key=lambda item: item.sort_key):
        if not bounds.contains(event.pos):
            continue
        if event.action == "place":
            active.add(event.pos)
            saw_place = True
        else:
            active.discard(event.pos)
    return frozenset(active), saw_place


def _evidence_predates_coverage(
    first_built: object,
    stats: BuildLogQueryStats | None,
) -> bool:
    if first_built is None or stats is None or stats.log_coverage.from_date is None:
        return False
    try:
        first_built_at = parse_utc_datetime(first_built, "first_built")
    except ValueError:
        return True
    return first_built_at.date() < stats.log_coverage.from_date


def _nonempty_all_invalid(stats: BuildLogQueryStats) -> bool:
    return stats.lines_read > 0 and stats.events_valid_v1 == 0


def _stats_summary(stats: BuildLogQueryStats) -> dict[str, Any]:
    return stats.to_dict()


def _values_then_error(values: Iterable[_T], detail: str) -> Iterator[_T]:
    yield from values
    raise _IncompleteSource(detail)


def _validate_dimension(value: object) -> None:
    if not isinstance(value, str):
        raise ValueError("dimension must be namespaced")
    namespace, separator, name = value.partition(":")
    if not separator or not namespace or not name:
        raise ValueError("dimension must be namespaced")


def _safe_detail(exc: Exception) -> str:
    return f"{type(exc).__name__}: {exc}".replace("\r", " ").replace("\n", " ")[:300]
