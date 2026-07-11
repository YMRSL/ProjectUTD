from __future__ import annotations

import logging
from datetime import datetime, timezone

from picasso.config import config
from picasso.core.player_protection import PlayerProtectionEvaluator
from picasso.core.player_protection_snapshot import build_player_protection_snapshot
from picasso.session import session


logger = logging.getLogger(__name__)


def current_player_protection() -> PlayerProtectionEvaluator:
    """Freeze the protection sources once for the current tool operation."""

    as_of = datetime.now(timezone.utc)
    if session.world_path is None:
        return _unavailable(as_of, "world_not_set")
    try:
        return build_player_protection_snapshot(
            world_path=session.world_path,
            build_log_dir=config.build_log_dir,
            as_of=as_of,
            dimension=config.dimension,
            lookback_days=config.protection_lookback_days,
            min_events=config.protection_min_events,
            join_dist=config.site_join_dist,
            join_gap_minutes=config.site_join_gap_min,
        )
    except Exception as exc:
        logger.exception("Could not build the player-protection snapshot")
        return _unavailable(as_of, f"{type(exc).__name__}: {exc}")


def _unavailable(as_of: datetime, detail: str) -> PlayerProtectionEvaluator:
    return PlayerProtectionEvaluator.from_sources(
        activity_events=None,
        protection_areas=None,
        as_of=as_of,
        dimension=config.dimension,
        source_summary={
            "snapshot": {
                "status": "unavailable",
                "error": detail[:300],
            }
        },
    )
