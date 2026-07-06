from __future__ import annotations

import logging
from pathlib import Path

from picasso.core.amulet_bridge import AmuletBridge
from picasso.core.surface_classifier import classify_surfaces
from picasso.session import session

logger = logging.getLogger(__name__)


def ensure_region(cx: int, cz: int, radius_chunks: int):
    if (
        session.last_region is not None
        and session.last_region.origin_cx == cx
        and session.last_region.origin_cz == cz
        and session.last_region.radius_chunks == radius_chunks
    ):
        if not session.last_region.surface_classes:
            classify_surfaces(session.last_region)
        return session.last_region
    if session.bridge is None:
        raise RuntimeError("world_not_set")
    region = session.bridge.read_region(cx, cz, radius_chunks)
    classify_surfaces(region)
    session.last_region = region
    return region


def register(mcp) -> None:
    @mcp.tool()
    def set_world(world_path: str) -> dict:
        """Open a Minecraft save directory for subsequent Picasso operations."""
        try:
            path = Path(world_path)
            if not path.exists():
                return {
                    "ok": False,
                    "error": "world_not_found",
                    "message": f"Path does not exist: {path}",
                }
            if session.bridge is not None:
                session.bridge.close()
            bridge = AmuletBridge(path)
            session.bridge = bridge
            session.world_path = path
            session.last_region = None
            return {
                "ok": True,
                "level_name": bridge.level_name,
                "version": bridge.version,
                "world_path": str(path),
            }
        except FileNotFoundError as exc:
            return {"ok": False, "error": "world_not_found", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error in set_world")
            return {"ok": False, "error": "amulet_error", "message": str(exc)}

    @mcp.tool()
    def read_region(cx: int, cz: int, radius_chunks: int) -> dict:
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
            region = ensure_region(cx, cz, radius_chunks)
            summary = region.to_summary()
            min_pos, max_pos = region.bounding_box()
            return {
                "ok": True,
                "block_count": summary["block_count"],
                "summary": (
                    f"Read {(radius_chunks * 2 + 1) ** 2} chunks "
                    f"(cx={cx}+/-{radius_chunks}, cz={cz}+/-{radius_chunks}). "
                    f"Found {summary['block_count']} non-air blocks. "
                    f"Y range: {min_pos.y}-{max_pos.y}."
                ),
                "bounds": summary["bounds"],
            }
        except Exception as exc:
            logger.exception("Unexpected error in read_region")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            return {"ok": False, "error": code, "message": str(exc)}
