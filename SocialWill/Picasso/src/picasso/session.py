from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from threading import Lock
from typing import Any

from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass


@dataclass
class PicassoSession:
    operation_lock: Any = field(default_factory=Lock, repr=False, compare=False)
    world_path: Path | None = None
    bridge: Any | None = None
    world_lock: Any | None = None
    catalog: Any | None = None
    pattern_matcher: Any | None = None
    fragment_library: Any | None = None
    bundle_registry: dict[str, dict] = field(default_factory=dict)
    pass_registry: dict[str, StylePass] = field(default_factory=dict)
    last_region: RegionData | None = None
    journal: Any | None = field(default=None, repr=False, compare=False)
    journal_status: str = "unavailable"
    journal_error: str | None = None
    noise_backend: str = "fallback"
    t_sync: str | None = None
    t_sync_warning: str | None = None
    last_region_cache_hit: bool = False
    last_region_read_seconds: float | None = None
    last_region_source: str | None = None

    def activate_journal(self) -> bool:
        """Bind and probe the per-world journal for the active bridge."""
        if self.world_path is None or self.bridge is None:
            self.journal = None
            self.journal_status = "unavailable"
            self.journal_error = "world_not_set"
            return False
        dimension = str(getattr(self.bridge, "dimension", "minecraft:overworld"))
        try:
            from picasso.core.journal import Journal

            self.journal = Journal(self.world_path, self.bridge, dimension)
        except Exception as exc:
            self.journal = None
            self.journal_status = "unavailable"
            self.journal_error = str(exc)
            return False
        self.journal_status = "active"
        self.journal_error = None
        return True

    def require_journal(self):
        """Return a journal matched to the exact current world and bridge."""
        if self.world_path is None or self.bridge is None:
            raise RuntimeError("world_not_set")
        dimension = str(getattr(self.bridge, "dimension", "minecraft:overworld"))
        if self.journal is not None and self.journal.matches(
            self.world_path, self.bridge, dimension
        ):
            return self.journal
        if self.activate_journal() and self.journal is not None:
            return self.journal

        from picasso.core.journal import JournalUnavailableError

        raise JournalUnavailableError(
            self.journal_error or "Journal could not be activated for the current world."
        )

    def note_journal_error(self, error: Exception) -> None:
        """Degrade status when the durable journal itself becomes unavailable."""
        if getattr(error, "code", None) not in {
            "journal_unavailable",
            "journal_world_mismatch",
        }:
            return
        self.journal = None
        self.journal_status = "unavailable"
        self.journal_error = str(error)

    def close_bridge(self) -> bool:
        lock_released = False
        if self.bridge is not None:
            close = getattr(self.bridge, "close", None)
            if callable(close):
                close()
        if self.world_lock is not None:
            release = getattr(self.world_lock, "release", None)
            if callable(release):
                lock_released = bool(release())
        self.bridge = None
        self.world_lock = None
        self.world_path = None
        self.journal = None
        self.journal_status = "unavailable"
        self.journal_error = None
        self.last_region = None
        self.last_region_cache_hit = False
        self.last_region_read_seconds = None
        self.last_region_source = None
        self.t_sync = None
        self.t_sync_warning = None
        return lock_released


session = PicassoSession()
