from __future__ import annotations

from collections import Counter
from typing import Any

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


MAX_XZ_SPAN = 32
MAX_Y_SPAN = 24
MAX_VOLUME = 24_576
MAX_RUNS = 4096

PaletteKey = tuple[str, tuple[tuple[str, str], ...]]


class VolumeInspectionError(ValueError):
    """Structured validation failure for a bounded volume inspection."""

    def __init__(self, code: str, message: str, **details: Any) -> None:
        super().__init__(message)
        self.code = code
        self.details = details

    def to_dict(self) -> dict[str, Any]:
        return {
            "error": self.code,
            "message": str(self),
            "details": dict(self.details),
        }


def inspect_region_volume(
    region: RegionData,
    x_min: int,
    y_min: int,
    z_min: int,
    x_max: int,
    y_max: int,
    z_max: int,
    max_runs: int = MAX_RUNS,
) -> dict[str, Any]:
    """Encode a bounded target-region volume as deterministic X-axis RLE.

    Bounds are inclusive. Air is implicit: only non-air runs are emitted.
    Palette counts and aggregate metadata always describe the complete requested
    volume, even when the run stream is deterministically truncated.
    """

    dimensions = validate_volume_request(
        x_min,
        y_min,
        z_min,
        x_max,
        y_max,
        z_max,
        max_runs,
    )
    _validate_target_bounds(region, x_min, y_min, z_min, x_max, y_max, z_max)
    _validate_loaded_chunks(region, x_min, z_min, x_max, z_max)

    states_by_pos: dict[BlockPos, BlockState] = {}
    key_by_pos: dict[BlockPos, PaletteKey] = {}
    palette_counts: Counter[PaletteKey] = Counter()
    for pos, state in region.blocks.items():
        if not _inside(pos, x_min, y_min, z_min, x_max, y_max, z_max):
            continue
        if not region.is_target_position(pos) or state.is_air:
            continue
        key = _palette_key(state)
        states_by_pos[pos] = state
        key_by_pos[pos] = key
        palette_counts[key] += 1

    sorted_keys = sorted(palette_counts)
    palette_index = {key: index for index, key in enumerate(sorted_keys)}
    palette = [
        {
            "index": palette_index[key],
            "block": key[0],
            "properties": dict(key[1]),
            "count": palette_counts[key],
        }
        for key in sorted_keys
    ]

    all_runs: list[tuple[int, int, dict[str, int]]] = []
    for y in range(y_min, y_max + 1):
        for z in range(z_min, z_max + 1):
            x = x_min
            while x <= x_max:
                pos = BlockPos(x, y, z)
                key = key_by_pos.get(pos)
                if key is None:
                    x += 1
                    continue
                run_start = x
                x += 1
                while x <= x_max and key_by_pos.get(BlockPos(x, y, z)) == key:
                    x += 1
                run_end = x - 1
                all_runs.append(
                    (
                        y,
                        z,
                        {
                            "x_min": run_start,
                            "x_max": run_end,
                            "length": run_end - run_start + 1,
                            "palette_index": palette_index[key],
                        },
                    )
                )

    emitted_runs = all_runs[:max_runs]
    omitted_runs = len(all_runs) - len(emitted_runs)
    layers = _layers_from_runs(emitted_runs)

    surface_counts: Counter[str] = Counter()
    for pos, surface in region.surface_classes.items():
        if pos not in states_by_pos:
            continue
        surface_counts[str(surface)] += 1

    block_entity_positions = [
        pos.to_dict()
        for pos in sorted(
            (
                pos
                for pos in region.block_entity_positions
                if _inside(pos, x_min, y_min, z_min, x_max, y_max, z_max)
                and region.is_target_position(pos)
            ),
            key=lambda pos: (pos.y, pos.z, pos.x),
        )
    ]

    truncated = omitted_runs > 0
    return {
        "bounds": _bounds_dict(x_min, y_min, z_min, x_max, y_max, z_max),
        "dimensions": dimensions,
        "non_air_blocks": len(states_by_pos),
        "palette": palette,
        "layers": layers,
        "surface_counts": dict(sorted(surface_counts.items())),
        "block_entity_positions": block_entity_positions,
        "run_count": len(emitted_runs),
        "complete": not truncated,
        "truncated": truncated,
        "omitted_runs": omitted_runs,
    }


def validate_volume_request(
    x_min: int,
    y_min: int,
    z_min: int,
    x_max: int,
    y_max: int,
    z_max: int,
    max_runs: int = MAX_RUNS,
) -> dict[str, int]:
    """Validate request-local limits before any RegionData or world read."""

    coordinates = (x_min, y_min, z_min, x_max, y_max, z_max)
    if any(type(value) is not int for value in coordinates):
        raise VolumeInspectionError(
            "invalid_bounds",
            "all volume bounds must be integers",
            bounds=_bounds_dict(x_min, y_min, z_min, x_max, y_max, z_max),
        )
    if x_min > x_max or y_min > y_max or z_min > z_max:
        raise VolumeInspectionError(
            "invalid_bounds",
            "minimum bounds must be less than or equal to maximum bounds",
            bounds=_bounds_dict(x_min, y_min, z_min, x_max, y_max, z_max),
        )
    if type(max_runs) is not int or not 0 <= max_runs <= MAX_RUNS:
        raise VolumeInspectionError(
            "invalid_max_runs",
            f"max_runs must be an integer between 0 and {MAX_RUNS}",
            max_runs=max_runs,
            max_allowed=MAX_RUNS,
        )

    dimensions = {
        "x": x_max - x_min + 1,
        "y": y_max - y_min + 1,
        "z": z_max - z_min + 1,
    }
    dimensions["volume"] = dimensions["x"] * dimensions["y"] * dimensions["z"]
    violations = []
    if dimensions["x"] > MAX_XZ_SPAN:
        violations.append("x_span")
    if dimensions["z"] > MAX_XZ_SPAN:
        violations.append("z_span")
    if dimensions["y"] > MAX_Y_SPAN:
        violations.append("y_span")
    if dimensions["volume"] > MAX_VOLUME:
        violations.append("volume")
    if violations:
        raise VolumeInspectionError(
            "volume_limit_exceeded",
            "requested volume exceeds the inspection limits",
            dimensions=dimensions,
            limits={
                "max_x_span": MAX_XZ_SPAN,
                "max_y_span": MAX_Y_SPAN,
                "max_z_span": MAX_XZ_SPAN,
                "max_volume": MAX_VOLUME,
            },
            violations=violations,
        )
    return dimensions


def _validate_target_bounds(
    region: RegionData,
    x_min: int,
    y_min: int,
    z_min: int,
    x_max: int,
    y_max: int,
    z_max: int,
) -> None:
    target_x_min = (region.origin_cx - region.radius_chunks) * 16
    target_z_min = (region.origin_cz - region.radius_chunks) * 16
    target_x_max = (region.origin_cx + region.radius_chunks + 1) * 16 - 1
    target_z_max = (region.origin_cz + region.radius_chunks + 1) * 16 - 1
    outside = (
        x_min < target_x_min
        or x_max > target_x_max
        or z_min < target_z_min
        or z_max > target_z_max
        or (region.y_min is not None and y_min < region.y_min)
        or (region.y_max is not None and y_max > region.y_max)
    )
    if not outside:
        return
    raise VolumeInspectionError(
        "bounds_outside_target",
        "requested bounds must stay inside the RegionData target envelope",
        requested=_bounds_dict(x_min, y_min, z_min, x_max, y_max, z_max),
        target={
            "min": {
                "x": target_x_min,
                "y": region.y_min,
                "z": target_z_min,
            },
            "max": {
                "x": target_x_max,
                "y": region.y_max,
                "z": target_z_max,
            },
        },
    )


def _validate_loaded_chunks(
    region: RegionData,
    x_min: int,
    z_min: int,
    x_max: int,
    z_max: int,
) -> None:
    required_chunks = {
        (chunk_x, chunk_z)
        for chunk_x in range(x_min >> 4, (x_max >> 4) + 1)
        for chunk_z in range(z_min >> 4, (z_max >> 4) + 1)
    }
    missing_chunks = sorted(required_chunks - region.loaded_chunks)
    if not missing_chunks:
        return
    raise VolumeInspectionError(
        "incomplete_read",
        "requested bounds include chunks that were not loaded",
        missing_chunks=missing_chunks,
    )


def _palette_key(state: BlockState) -> PaletteKey:
    return state.full_id, tuple(sorted(state.properties.items()))


def _layers_from_runs(
    runs: list[tuple[int, int, dict[str, int]]],
) -> list[dict[str, Any]]:
    layers: list[dict[str, Any]] = []
    current_y: int | None = None
    current_z: int | None = None
    current_layer: dict[str, Any] | None = None
    current_row: dict[str, Any] | None = None
    for y, z, run in runs:
        if y != current_y:
            current_layer = {"y": y, "rows": []}
            layers.append(current_layer)
            current_y = y
            current_z = None
        if z != current_z:
            current_row = {"z": z, "runs": []}
            assert current_layer is not None
            current_layer["rows"].append(current_row)
            current_z = z
        assert current_row is not None
        current_row["runs"].append(run)
    return layers


def _inside(
    pos: BlockPos,
    x_min: int,
    y_min: int,
    z_min: int,
    x_max: int,
    y_max: int,
    z_max: int,
) -> bool:
    return (
        x_min <= pos.x <= x_max
        and y_min <= pos.y <= y_max
        and z_min <= pos.z <= z_max
    )


def _bounds_dict(
    x_min: Any,
    y_min: Any,
    z_min: Any,
    x_max: Any,
    y_max: Any,
    z_max: Any,
) -> dict[str, dict[str, Any]]:
    return {
        "min": {"x": x_min, "y": y_min, "z": z_min},
        "max": {"x": x_max, "y": y_max, "z": z_max},
    }


__all__ = [
    "MAX_VOLUME",
    "MAX_RUNS",
    "MAX_XZ_SPAN",
    "MAX_Y_SPAN",
    "VolumeInspectionError",
    "inspect_region_volume",
    "validate_volume_request",
]
