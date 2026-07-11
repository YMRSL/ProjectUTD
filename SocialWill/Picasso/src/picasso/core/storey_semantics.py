from __future__ import annotations

from collections import defaultdict
from typing import Literal, TypedDict

from picasso.models.block import BlockPos
from picasso.models.region import RegionData


DEFAULT_MIN_CONNECTED_AREA = 16
_SUPPORT_SURFACES = frozenset({"floor", "rooftop"})
_HORIZONTAL_OFFSETS = ((-1, 0), (0, -1), (0, 1), (1, 0))


class StoreyPlaneBounds(TypedDict):
    """Inclusive block-coordinate bounds of a horizontal support component."""

    x_min: int
    x_max: int
    y_min: int
    y_max: int
    z_min: int
    z_max: int


class StoreyLevelCandidate(TypedDict):
    """Evidence for a possible storey level, without assigning an ordinal name."""

    type: Literal["storey_level_candidate"]
    y: int
    area: int
    bounds: StoreyPlaneBounds
    confidence: float


def detect_storey_level_candidates(
    region: RegionData,
    *,
    min_area: int = DEFAULT_MIN_CONNECTED_AREA,
) -> list[StoreyLevelCandidate]:
    """Detect large, connected horizontal support planes in ``region``.

    ``region`` is expected to have already passed through
    :func:`picasso.core.surface_classifier.classify_surfaces`.  Only classified
    ``floor`` and ``rooftop`` positions in the requested target region are
    evidence.  Read-only halo positions remain available to the classifier but
    can never contribute to a candidate here.

    Four-neighbour connectivity is evaluated independently at each Y level.
    This joins adjacent floor/rooftop classifications into one physical plane
    without joining surfaces that merely touch diagonally.  Components smaller
    than ``min_area`` are discarded so table tops and individual stair treads
    do not become storey claims.

    The function is pure with respect to ``region`` and returns records in a
    stable ``(y, bounds)`` order.
    """
    if not isinstance(region, RegionData):
        raise TypeError("region must be a RegionData")
    if isinstance(min_area, bool) or not isinstance(min_area, int) or min_area < 1:
        raise ValueError("min_area must be a positive integer")

    positions_by_y: dict[int, set[BlockPos]] = defaultdict(set)
    for pos, surface_class in region.surface_classes.items():
        if surface_class not in _SUPPORT_SURFACES:
            continue
        if not region.is_target_position(pos):
            continue
        positions_by_y[pos.y].add(pos)

    candidates: list[StoreyLevelCandidate] = []
    for y in sorted(positions_by_y):
        remaining = set(positions_by_y[y])
        while remaining:
            component = _take_component(remaining)
            if len(component) < min_area:
                continue
            candidates.append(_candidate_for(y, component, min_area))

    candidates.sort(
        key=lambda candidate: (
            candidate["y"],
            candidate["bounds"]["x_min"],
            candidate["bounds"]["z_min"],
            candidate["bounds"]["x_max"],
            candidate["bounds"]["z_max"],
        )
    )
    return candidates


def _take_component(remaining: set[BlockPos]) -> set[BlockPos]:
    seed = min(remaining)
    remaining.remove(seed)
    component = {seed}
    pending = [seed]

    while pending:
        current = pending.pop()
        for dx, dz in _HORIZONTAL_OFFSETS:
            neighbour = current.offset(dx=dx, dz=dz)
            if neighbour not in remaining:
                continue
            remaining.remove(neighbour)
            component.add(neighbour)
            pending.append(neighbour)
    return component


def _candidate_for(
    y: int,
    component: set[BlockPos],
    min_area: int,
) -> StoreyLevelCandidate:
    x_min = min(pos.x for pos in component)
    x_max = max(pos.x for pos in component)
    z_min = min(pos.z for pos in component)
    z_max = max(pos.z for pos in component)
    area = len(component)
    bounding_area = (x_max - x_min + 1) * (z_max - z_min + 1)
    compactness = area / bounding_area
    size_strength = min(1.0, area / (min_area * 4))
    confidence = round(min(1.0, 0.5 + 0.35 * size_strength + 0.15 * compactness), 3)

    return {
        "type": "storey_level_candidate",
        "y": y,
        "area": area,
        "bounds": {
            "x_min": x_min,
            "x_max": x_max,
            "y_min": y,
            "y_max": y,
            "z_min": z_min,
            "z_max": z_max,
        },
        "confidence": confidence,
    }
