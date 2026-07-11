from __future__ import annotations

import logging
import json
from pathlib import Path
from time import perf_counter

from picasso.config import config
from picasso.core.amulet_bridge import AmuletBridge
from picasso.core.noise_field import resolve_noise_backend
from picasso.core.surface_classifier import classify_surfaces
from picasso.core.world_lock import WorldLockError, acquire_world_lock, normalize_world_key
from picasso.session import session

logger = logging.getLogger(__name__)


def ensure_region(
    cx: int,
    cz: int,
    radius_chunks: int,
    y_min: int | None = None,
    y_max: int | None = None,
):
    started = perf_counter()
    if radius_chunks < 0:
        raise ValueError("invalid_coordinates")
    if radius_chunks > config.max_radius_chunks:
        raise OverflowError("region_too_large")
    if y_min is not None and y_max is not None and y_min > y_max:
        raise ValueError("invalid_y_window")
    if session.bridge is None:
        raise RuntimeError("world_not_set")
    scan_y_min, scan_y_max = session.bridge.resolve_y_window(y_min, y_max)
    if (
        session.last_region is not None
        and session.last_region.origin_cx == cx
        and session.last_region.origin_cz == cz
        and session.last_region.radius_chunks == radius_chunks
        and session.last_region.y_min == scan_y_min
        and session.last_region.y_max == scan_y_max
    ):
        if not session.last_region.surface_classes:
            classify_surfaces(session.last_region)
        session.last_region_cache_hit = True
        session.last_region_read_seconds = perf_counter() - started
        session.last_region_source = "cache"
        return session.last_region
    region = session.bridge.read_region(cx, cz, radius_chunks, y_min=y_min, y_max=y_max)
    classify_surfaces(region)
    session.last_region = region
    session.last_region_cache_hit = False
    session.last_region_read_seconds = perf_counter() - started
    session.last_region_source = "amulet"
    return region


def register(mcp) -> None:
    @mcp.tool()
    def set_world(world_path: str) -> dict:
        """Open a Minecraft save directory for subsequent Picasso operations."""
        acquired_lock = None
        try:
            started = perf_counter()
            path = Path(world_path)
            if not path.exists():
                return {
                    "ok": False,
                    "error": "world_not_found",
                    "message": f"Path does not exist: {path}",
                }
            requested_key = normalize_world_key(path)
            current_key = getattr(session.world_lock, "world_key", None)
            if current_key is None and session.world_path is not None:
                current_key = normalize_world_key(session.world_path)
            if session.bridge is not None and current_key == requested_key:
                bridge = session.bridge
                session.activate_journal()
                session.noise_backend = resolve_noise_backend()
                session.t_sync, session.t_sync_warning = _read_sync_marker(path)
                return {
                    "ok": True,
                    "already_connected": True,
                    "level_name": bridge.level_name,
                    "version": bridge.version,
                    "world_path": str(session.world_path or path),
                    "journal_status": session.journal_status,
                    "journal_error": session.journal_error,
                    "noise_backend": session.noise_backend,
                    "t_sync": session.t_sync,
                    "t_sync_warning": session.t_sync_warning,
                    "modded_write_verified": config.modded_write_verified,
                    "world_lock": _lock_summary(session.world_lock),
                    "timings": {
                        "open_world_seconds": 0.0,
                        "total_seconds": round(perf_counter() - started, 3),
                    },
                }
            acquired_lock = acquire_world_lock(path)
            if session.bridge is not None:
                session.close_bridge()
            before_open = perf_counter()
            bridge = AmuletBridge(path)
            after_open = perf_counter()
            session.bridge = bridge
            session.world_lock = acquired_lock
            acquired_lock = None
            session.world_path = path
            session.last_region = None
            session.last_region_cache_hit = False
            session.last_region_read_seconds = None
            session.last_region_source = None
            session.noise_backend = resolve_noise_backend()
            session.activate_journal()
            session.t_sync, session.t_sync_warning = _read_sync_marker(path)
            return {
                "ok": True,
                "level_name": bridge.level_name,
                "version": bridge.version,
                "world_path": str(path),
                "journal_status": session.journal_status,
                "journal_error": session.journal_error,
                "noise_backend": session.noise_backend,
                "t_sync": session.t_sync,
                "t_sync_warning": session.t_sync_warning,
                "modded_write_verified": config.modded_write_verified,
                "world_lock": _lock_summary(session.world_lock),
                "timings": {
                    "open_world_seconds": round(after_open - before_open, 3),
                    "total_seconds": round(perf_counter() - started, 3),
                },
            }
        except FileNotFoundError as exc:
            if acquired_lock is not None:
                acquired_lock.release()
            return {"ok": False, "error": "world_not_found", "message": str(exc)}
        except WorldLockError as exc:
            return {
                "ok": False,
                "error": "world_locked",
                "message": (
                    "Another Picasso process appears to have this world open. "
                    "Call close_world in that session or stop the stale picasso.server process."
                ),
                "lock_path": str(exc.lock_path),
                "owner": exc.owner,
            }
        except Exception as exc:
            if acquired_lock is not None:
                acquired_lock.release()
            logger.exception("Unexpected error in set_world")
            return {"ok": False, "error": "amulet_error", "message": str(exc)}

    @mcp.tool()
    def close_world() -> dict:
        """Close the active world and release Picasso's advisory world lock."""
        try:
            previous_path = str(session.world_path) if session.world_path else None
            was_connected = session.bridge is not None
            lock_path = str(session.world_lock.lock_path) if session.world_lock else None
            lock_released = session.close_bridge()
            return {
                "ok": True,
                "was_connected": was_connected,
                "previous_world_path": previous_path,
                "lock_path": lock_path,
                "lock_released": lock_released,
            }
        except Exception as exc:
            logger.exception("Unexpected error in close_world")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def read_region(
        cx: int,
        cz: int,
        radius_chunks: int,
        y_min: int | None = None,
        y_max: int | None = None,
    ) -> dict:
        """Read non-air block data from chunks around a center chunk."""
        try:
            if session.bridge is None:
                return {
                    "ok": False,
                    "error": "world_not_set",
                    "message": "Call set_world before read_region.",
                }
            if radius_chunks < 0:
                return {
                    "ok": False,
                    "error": "invalid_coordinates",
                    "message": "radius_chunks must be >= 0.",
                }
            if radius_chunks > config.max_radius_chunks:
                return {
                    "ok": False,
                    "error": "region_too_large",
                    "message": (
                        f"radius_chunks={radius_chunks} exceeds the configured maximum "
                        f"core radius PICASSO_MAX_RADIUS_CHUNKS="
                        f"{config.max_radius_chunks}. The classification halo adds one "
                        f"chunk to the accepted core radius (actual read radius="
                        f"{radius_chunks + 1})."
                    ),
                }
            if y_min is not None and y_max is not None and y_min > y_max:
                return {
                    "ok": False,
                    "error": "invalid_y_window",
                    "message": "y_min must be <= y_max.",
                }
            started = perf_counter()
            region = ensure_region(cx, cz, radius_chunks, y_min=y_min, y_max=y_max)
            after_region = perf_counter()
            summary = region.to_summary()
            expected_chunks = (radius_chunks * 2 + 1) ** 2
            expected_context_chunks = (radius_chunks * 2 + 3) ** 2 - expected_chunks
            return {
                "ok": True,
                "block_count": summary["block_count"],
                "chunks_read": summary["chunks_read"],
                "chunks_missing": summary["chunks_missing"],
                "context_chunks_read": summary["context_chunks_read"],
                "context_chunks_missing": summary["context_chunks_missing"],
                "context_block_count": summary["context_block_count"],
                "y_window": summary["y_window"],
                "read_y_window": summary["read_y_window"],
                "region_cache_hit": session.last_region_cache_hit,
                "region_source": session.last_region_source,
                "timings": {
                    "ensure_region_seconds": round(
                        session.last_region_read_seconds
                        if session.last_region_read_seconds is not None
                        else after_region - started,
                        3,
                    ),
                    "total_seconds": round(perf_counter() - started, 3),
                },
                "summary": (
                    f"Read {summary['chunks_read']}/{expected_chunks} chunks "
                    f"(cx={cx}+/-{radius_chunks}, cz={cz}+/-{radius_chunks}). "
                    f"Context halo: {summary['context_chunks_read']}/"
                    f"{expected_context_chunks} chunks. "
                    f"{summary['block_count']} non-air blocks. "
                    f"Y range: {summary['bounds']['min']['y']}-"
                    f"{summary['bounds']['max']['y']}."
                ),
                "bounds": summary["bounds"],
            }
        except Exception as exc:
            logger.exception("Unexpected error in read_region")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            if isinstance(exc, OverflowError) and str(exc) == "region_too_large":
                code = "region_too_large"
            if isinstance(exc, ValueError) and str(exc) == "invalid_coordinates":
                code = "invalid_coordinates"
            if isinstance(exc, ValueError) and str(exc) == "invalid_y_window":
                code = "invalid_y_window"
            return {"ok": False, "error": code, "message": str(exc)}


def _read_sync_marker(world_path: Path) -> tuple[str | None, str | None]:
    sync_path = world_path / "picasso_sync.json"
    if not sync_path.exists():
        return None, "unavailable - attribution may overcount"
    try:
        data = json.loads(sync_path.read_text(encoding="utf-8"))
    except Exception:
        logger.exception("Invalid sync marker: %s", sync_path)
        return None, "unavailable - attribution may overcount"
    synced_at = data.get("synced_at")
    if not synced_at:
        return None, "unavailable - attribution may overcount"
    return str(synced_at), None


def _lock_summary(lock) -> dict | None:
    if lock is None:
        return None
    return {
        "lock_path": str(lock.lock_path),
        "world_key": lock.world_key,
    }
