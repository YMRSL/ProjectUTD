from __future__ import annotations

import logging
from datetime import datetime
from typing import Any

from picasso.config import config
from picasso.core.build_log_reader import (
    ActivitySiteNotFound,
    BuildLogReader,
    BuildLogUnavailable,
)
from picasso.models.player_activity import (
    BlockBounds,
    format_utc_datetime,
    parse_utc_datetime,
)


logger = logging.getLogger(__name__)


class _InvalidToolInput(ValueError):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code


def _parse_timestamp(value: object, field_name: str) -> datetime:
    if not isinstance(value, str) or not value:
        raise _InvalidToolInput(
            f"invalid_{field_name}",
            f"{field_name} must be a non-empty ISO-8601 UTC timestamp",
        )
    try:
        return parse_utc_datetime(value, field_name)
    except ValueError as exc:
        raise _InvalidToolInput(f"invalid_{field_name}", str(exc)) from exc


def _parse_query_window(since: object, until: object) -> tuple[datetime, datetime | None]:
    since_utc = _parse_timestamp(since, "since")
    until_utc = _parse_timestamp(until, "until") if until is not None else None
    if until_utc is not None and until_utc < since_utc:
        raise _InvalidToolInput("invalid_time_range", "until must be >= since")
    return since_utc, until_utc


def _parse_bounds(value: object) -> BlockBounds | None:
    if value is None:
        return None
    try:
        return BlockBounds.from_record(value)
    except (TypeError, ValueError, KeyError) as exc:
        raise _InvalidToolInput("invalid_bounds", str(exc)) from exc


def _parse_player(value: object) -> str | None:
    if value is None:
        return None
    if not isinstance(value, str) or not value:
        raise _InvalidToolInput("invalid_player", "player must be a non-empty string")
    return value


def _parse_min_events(value: object) -> int | None:
    if value is None:
        return None
    if isinstance(value, bool) or not isinstance(value, int) or value < 1:
        raise _InvalidToolInput("invalid_min_events", "min_events must be a positive integer")
    return value


def _reader() -> BuildLogReader:
    return BuildLogReader(
        config.build_log_dir,
        join_dist=config.site_join_dist,
        join_gap_minutes=config.site_join_gap_min,
        site_min_events=config.site_min_events,
    )


def _error_response(exc: Exception) -> dict[str, Any]:
    if isinstance(exc, _InvalidToolInput):
        return {"ok": False, "error": exc.code, "message": str(exc)}
    if isinstance(exc, BuildLogUnavailable):
        return {
            "ok": False,
            "error": "build_log_not_configured",
            "message": str(exc),
        }
    if isinstance(exc, ActivitySiteNotFound):
        return {"ok": False, "error": "site_not_found", "message": str(exc)}
    logger.exception("Unexpected activity tool error")
    return {"ok": False, "error": "internal_error", "message": str(exc)}


def register(mcp) -> None:
    @mcp.tool()
    def query_player_activity(
        since: str,
        until: str | None = None,
        player: str | None = None,
        bounds: dict[str, int] | None = None,
        min_events: int | None = None,
    ) -> dict[str, Any]:
        """Return clustered player activity without exposing raw build events."""

        try:
            since_utc, until_utc = _parse_query_window(since, until)
            parsed_player = _parse_player(player)
            parsed_bounds = _parse_bounds(bounds)
            parsed_min_events = _parse_min_events(min_events)
            result = _reader().query_sites(
                since_utc,
                until=until_utc,
                player=parsed_player,
                bounds=parsed_bounds,
                min_events=parsed_min_events,
            )
            return {
                "ok": True,
                "query": {
                    "since": format_utc_datetime(since_utc),
                    "until": (
                        format_utc_datetime(until_utc) if until_utc is not None else None
                    ),
                    "player": parsed_player,
                    "bounds": parsed_bounds.to_dict() if parsed_bounds is not None else None,
                    "min_events": (
                        parsed_min_events
                        if parsed_min_events is not None
                        else config.site_min_events
                    ),
                },
                **result.to_dict(),
            }
        except Exception as exc:
            return _error_response(exc)

    @mcp.tool()
    def get_activity_site(
        site_id: str,
        since: str,
        until: str | None = None,
        min_events: int | None = None,
        player: str | None = None,
        bounds: dict[str, int] | None = None,
    ) -> dict[str, Any]:
        """Return one activity site with its hourly timeline and complete palette."""

        try:
            if not isinstance(site_id, str) or not site_id:
                raise _InvalidToolInput(
                    "invalid_site_id", "site_id must be a non-empty string"
                )
            since_utc, until_utc = _parse_query_window(since, until)
            parsed_min_events = _parse_min_events(min_events)
            parsed_player = _parse_player(player)
            parsed_bounds = _parse_bounds(bounds)
            detail = _reader().get_site(
                site_id,
                since_utc,
                until=until_utc,
                player=parsed_player,
                bounds=parsed_bounds,
                min_events=parsed_min_events,
            )
            return {
                "ok": True,
                "query": {
                    "site_id": site_id,
                    "since": format_utc_datetime(since_utc),
                    "until": (
                        format_utc_datetime(until_utc) if until_utc is not None else None
                    ),
                    "player": parsed_player,
                    "bounds": parsed_bounds.to_dict() if parsed_bounds is not None else None,
                    "min_events": (
                        parsed_min_events
                        if parsed_min_events is not None
                        else config.site_min_events
                    ),
                },
                **detail.to_dict(),
            }
        except Exception as exc:
            return _error_response(exc)
