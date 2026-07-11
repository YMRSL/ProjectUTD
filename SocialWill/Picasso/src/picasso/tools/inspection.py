from __future__ import annotations

import logging

from picasso.core.volume_inspector import (
    MAX_RUNS,
    VolumeInspectionError,
    inspect_region_volume,
    validate_volume_request,
)
from picasso.session import session
from picasso.tools.world_io import ensure_region


logger = logging.getLogger(__name__)


def register(mcp) -> None:
    @mcp.tool()
    def inspect_volume(
        x_min: int,
        y_min: int,
        z_min: int,
        x_max: int,
        y_max: int,
        z_max: int,
        max_runs: int = MAX_RUNS,
    ) -> dict:
        """Read bounded block-state evidence as a deterministic palette and RLE view."""

        try:
            dimensions = validate_volume_request(
                x_min,
                y_min,
                z_min,
                x_max,
                y_max,
                z_max,
                max_runs=max_runs,
            )
            if session.bridge is None:
                return {
                    "ok": False,
                    "error": "world_not_set",
                    "message": "Call set_world before inspect_volume.",
                }

            cx, cz, radius_chunks = _covering_chunk_request(
                x_min,
                z_min,
                x_max,
                z_max,
            )
            region = ensure_region(
                cx,
                cz,
                radius_chunks,
                y_min=y_min,
                y_max=y_max,
            )
            inspection = inspect_region_volume(
                region,
                x_min,
                y_min,
                z_min,
                x_max,
                y_max,
                z_max,
                max_runs=max_runs,
            )
            return {
                "ok": True,
                **inspection,
                "encoding": {
                    "order": "Y layers -> Z rows -> inclusive X runs",
                    "air": "implicit only when complete=true",
                    "palette_index": "layers[].rows[].runs[].palette_index",
                },
                "region_request": {
                    "cx": cx,
                    "cz": cz,
                    "radius_chunks": radius_chunks,
                    "y_min": y_min,
                    "y_max": y_max,
                },
                "region_cache_hit": session.last_region_cache_hit,
                "region_source": session.last_region_source,
                "space_classification": "heuristic",
                "summary": (
                    f"Inspected {dimensions['volume']} positions containing "
                    f"{inspection['non_air_blocks']} non-air blocks in "
                    f"{inspection['run_count']} emitted runs. "
                    f"Complete: {inspection['complete']}."
                ),
            }
        except VolumeInspectionError as exc:
            return {"ok": False, **exc.to_dict()}
        except OverflowError as exc:
            code = "region_too_large" if str(exc) == "region_too_large" else "internal_error"
            return {"ok": False, "error": code, "message": str(exc)}
        except ValueError as exc:
            code = "invalid_y_window" if str(exc) == "invalid_y_window" else "invalid_bounds"
            return {"ok": False, "error": code, "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error in inspect_volume")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            return {"ok": False, "error": code, "message": str(exc)}


def _covering_chunk_request(
    x_min: int,
    z_min: int,
    x_max: int,
    z_max: int,
) -> tuple[int, int, int]:
    chunk_x_min = x_min >> 4
    chunk_x_max = x_max >> 4
    chunk_z_min = z_min >> 4
    chunk_z_max = z_max >> 4
    cx = (chunk_x_min + chunk_x_max) // 2
    cz = (chunk_z_min + chunk_z_max) // 2
    radius = max(
        cx - chunk_x_min,
        chunk_x_max - cx,
        cz - chunk_z_min,
        chunk_z_max - cz,
    )
    return cx, cz, radius


__all__ = ["register"]
