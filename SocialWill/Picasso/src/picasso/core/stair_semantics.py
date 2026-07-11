from __future__ import annotations

from collections import defaultdict, deque
from dataclasses import dataclass
from typing import Literal

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


StairSemanticKind = Literal[
    "supermarket_shelf",
    "functional_staircase",
    "decorative_floor_trim",
    "seat_candidate",
]
StairMemberRole = Literal[
    "shelf_row_a",
    "shelf_row_b",
    "step",
    "underfill",
    "trim",
    "seat",
]

_FACING_VECTOR = {
    "north": (0, -1),
    "south": (0, 1),
    "west": (-1, 0),
    "east": (1, 0),
}
_OPPOSITE_FACING = {
    "north": "south",
    "south": "north",
    "west": "east",
    "east": "west",
}
_KNOWN_HALVES = {"bottom", "top"}
_KNOWN_SHAPES = {
    "straight",
    "inner_left",
    "inner_right",
    "outer_left",
    "outer_right",
}
_KIND_PRIORITY = {
    "supermarket_shelf": 0,
    "functional_staircase": 1,
    "decorative_floor_trim": 2,
    "seat_candidate": 3,
}


@dataclass(frozen=True, order=True)
class StairBounds:
    minimum: BlockPos
    maximum: BlockPos

    def to_dict(self) -> dict[str, dict[str, int]]:
        return {"min": self.minimum.to_dict(), "max": self.maximum.to_dict()}


@dataclass(frozen=True, order=True)
class StairMember:
    pos: BlockPos
    role: StairMemberRole

    def to_dict(self) -> dict[str, object]:
        return {"pos": self.pos.to_dict(), "role": self.role}


@dataclass(frozen=True)
class StairSemanticCandidate:
    kind: StairSemanticKind
    confidence: float
    bounds: StairBounds
    anchor: BlockPos
    members: tuple[StairMember, ...]

    @property
    def member_positions(self) -> frozenset[BlockPos]:
        return frozenset(member.pos for member in self.members)

    def to_dict(self) -> dict[str, object]:
        return {
            "kind": self.kind,
            "confidence": self.confidence,
            "anchor": self.anchor.to_dict(),
            "bounds": self.bounds.to_dict(),
            "member_count": len(self.members),
            "members": [member.to_dict() for member in self.members],
        }


def detect_stair_semantics(
    region: RegionData,
) -> tuple[StairSemanticCandidate, ...]:
    """Return deterministic, non-overlapping semantic candidates for vanilla stairs.

    This is deliberately a candidate detector, not a room or building classifier.
    It only consumes the trusted core of ``region`` and never reads a world.
    """

    stairs = {
        pos: state
        for pos, state in region.blocks.items()
        if region.is_target_position(pos) and _is_vanilla_stair(state)
    }
    if not stairs:
        return ()

    used: set[BlockPos] = set()
    accepted: list[StairSemanticCandidate] = []
    proposal_groups = (
        _shelf_candidates(stairs),
        _functional_candidates(stairs),
        _trim_candidates(stairs),
        _seat_candidates(stairs),
    )
    for proposals in proposal_groups:
        for candidate in sorted(proposals, key=_proposal_order):
            positions = candidate.member_positions
            if positions.intersection(used):
                continue
            accepted.append(candidate)
            used.update(positions)

    return tuple(
        sorted(
            accepted,
            key=lambda item: (
                _KIND_PRIORITY[item.kind],
                item.anchor,
                item.bounds,
                item.members,
            ),
        )
    )


def _shelf_candidates(
    stairs: dict[BlockPos, BlockState],
) -> list[StairSemanticCandidate]:
    candidates: list[StairSemanticCandidate] = []

    # One canonical facing per opposing pair prevents duplicate assemblies.
    # Accept both physical orders: depending on how the builder describes the
    # shelf's visible side, a north-facing row may be either north or south of
    # its opposing row. The pair is canonicalized to its lower X/Z plane.
    for facing in ("north", "west"):
        dx, dz = _FACING_VECTOR[facing]
        opposite = _OPPOSITE_FACING[facing]
        cells_by_plane: dict[
            int, dict[tuple[int, int], tuple[BlockPos, BlockPos]]
        ] = defaultdict(dict)

        for first, state in sorted(stairs.items()):
            if _property(state, "facing") != facing:
                continue
            for pair_dx, pair_dz in ((dx, dz), (-dx, -dz)):
                second = first.offset(dx=pair_dx, dz=pair_dz)
                second_state = stairs.get(second)
                if second_state is None or _property(second_state, "facing") != opposite:
                    continue
                if _property(second_state, "half") != _property(state, "half"):
                    continue
                if facing == "north":
                    plane = min(first.z, second.z)
                    lateral = first.x
                else:
                    plane = min(first.x, second.x)
                    lateral = first.z
                cells_by_plane[plane][(lateral, first.y)] = (first, second)

        for plane in sorted(cells_by_plane):
            cells = cells_by_plane[plane]
            remaining = set(cells)
            while remaining:
                seed = min(remaining)
                component = _connected_cells(seed, remaining)
                y_levels = sorted({y for _lateral, y in component})
                if not (2 <= len(y_levels) <= 3):
                    continue
                if y_levels != list(range(y_levels[0], y_levels[-1] + 1)):
                    continue
                lateral_by_y = {
                    y: {lateral for lateral, cell_y in component if cell_y == y}
                    for y in y_levels
                }
                first_laterals = lateral_by_y[y_levels[0]]
                if len(first_laterals) < 3 or not _is_contiguous(first_laterals):
                    continue
                if any(laterals != first_laterals for laterals in lateral_by_y.values()):
                    continue

                roles: dict[BlockPos, StairMemberRole] = {}
                for cell in component:
                    row_a, row_b = cells[cell]
                    roles[row_a] = "shelf_row_a"
                    roles[row_b] = "shelf_row_b"
                states = [stairs[pos] for pos in roles]
                halves = {_property(state, "half") for state in states}
                complete = all(
                    _has_complete_properties(state)
                    and _property(state, "shape") == "straight"
                    for state in states
                ) and len(halves) == 1
                candidates.append(
                    _candidate(
                        "supermarket_shelf",
                        0.95 if complete else 0.65,
                        roles,
                        anchor=min(roles, key=_anchor_order),
                    )
                )
    return candidates


def _functional_candidates(
    stairs: dict[BlockPos, BlockState],
) -> list[StairSemanticCandidate]:
    candidates: list[StairSemanticCandidate] = []
    for start, state in sorted(stairs.items()):
        facing = _property(state, "facing")
        if not _is_bottom_step(state, facing):
            continue
        dx, dz = _FACING_VECTOR[facing]
        predecessor = start.offset(dx=-dx, dy=-1, dz=-dz)
        if _is_bottom_step(stairs.get(predecessor), facing):
            continue

        steps: list[BlockPos] = []
        current = start
        while _is_bottom_step(stairs.get(current), facing):
            steps.append(current)
            current = current.offset(dx=dx, dy=1, dz=dz)
        if len(steps) < 3:
            continue

        roles: dict[BlockPos, StairMemberRole] = {pos: "step" for pos in steps}
        opposite = _OPPOSITE_FACING[facing]
        for step in steps:
            underfill = step.offset(dx=dx, dz=dz)
            underfill_state = stairs.get(underfill)
            if (
                underfill_state is not None
                and _property(underfill_state, "half") == "top"
                and _property(underfill_state, "facing") == opposite
            ):
                roles[underfill] = "underfill"

        complete = all(
            _property(stairs[pos], "shape") == "straight" for pos in roles
        )
        candidates.append(
            _candidate(
                "functional_staircase",
                0.93 if complete else 0.70,
                roles,
                anchor=start,
            )
        )
    return candidates


def _trim_candidates(
    stairs: dict[BlockPos, BlockState],
) -> list[StairSemanticCandidate]:
    candidates: list[StairSemanticCandidate] = []
    eligible = {
        pos: state
        for pos, state in stairs.items()
        if _property(state, "facing") in _FACING_VECTOR
        and _property(state, "half") == "top"
        and _property(state, "shape") == "straight"
    }

    for axis in ("x", "z"):
        groups: dict[tuple[str, int, int], dict[int, BlockPos]] = defaultdict(dict)
        for pos, state in sorted(eligible.items()):
            facing = _property(state, "facing")
            if axis == "x" and facing not in {"north", "south"}:
                continue
            if axis == "z" and facing not in {"east", "west"}:
                continue
            if axis == "x":
                key = (facing, pos.y, pos.z)
                coordinate = pos.x
            else:
                key = (facing, pos.y, pos.x)
                coordinate = pos.z
            groups[key][coordinate] = pos

        for key in sorted(groups):
            by_coordinate = groups[key]
            for run in _contiguous_runs(sorted(by_coordinate)):
                if len(run) < 3:
                    continue
                positions = [by_coordinate[coordinate] for coordinate in run]
                roles = {pos: "trim" for pos in positions}
                candidates.append(
                    _candidate(
                        "decorative_floor_trim",
                        0.90,
                        roles,
                        anchor=min(positions, key=_anchor_order),
                    )
                )
    return candidates


def _seat_candidates(
    stairs: dict[BlockPos, BlockState],
) -> list[StairSemanticCandidate]:
    candidates: list[StairSemanticCandidate] = []
    positions = set(stairs)
    for pos, state in sorted(stairs.items()):
        if any(neighbor in positions for neighbor in _nearby_positions(pos)):
            continue
        confidence = 0.55 if _has_complete_properties(state) else 0.30
        candidates.append(
            _candidate(
                "seat_candidate",
                confidence,
                {pos: "seat"},
                anchor=pos,
            )
        )
    return candidates


def _candidate(
    kind: StairSemanticKind,
    confidence: float,
    roles: dict[BlockPos, StairMemberRole],
    *,
    anchor: BlockPos,
) -> StairSemanticCandidate:
    members = tuple(
        sorted(
            (StairMember(pos, role) for pos, role in roles.items()),
            key=lambda member: (member.pos, member.role),
        )
    )
    xs = [member.pos.x for member in members]
    ys = [member.pos.y for member in members]
    zs = [member.pos.z for member in members]
    bounds = StairBounds(
        BlockPos(min(xs), min(ys), min(zs)),
        BlockPos(max(xs), max(ys), max(zs)),
    )
    return StairSemanticCandidate(kind, confidence, bounds, anchor, members)


def _connected_cells(
    seed: tuple[int, int],
    remaining: set[tuple[int, int]],
) -> set[tuple[int, int]]:
    component: set[tuple[int, int]] = set()
    queue = deque([seed])
    remaining.remove(seed)
    while queue:
        lateral, y = queue.popleft()
        cell = (lateral, y)
        component.add(cell)
        for neighbor in (
            (lateral - 1, y),
            (lateral + 1, y),
            (lateral, y - 1),
            (lateral, y + 1),
        ):
            if neighbor in remaining:
                remaining.remove(neighbor)
                queue.append(neighbor)
    return component


def _is_vanilla_stair(state: BlockState) -> bool:
    return state.namespace == "minecraft" and state.name.endswith("_stairs")


def _is_bottom_step(state: BlockState | None, facing: str | None) -> bool:
    return bool(
        state is not None
        and facing in _FACING_VECTOR
        and _property(state, "facing") == facing
        and _property(state, "half") == "bottom"
    )


def _has_complete_properties(state: BlockState) -> bool:
    return (
        _property(state, "facing") in _FACING_VECTOR
        and _property(state, "half") in _KNOWN_HALVES
        and _property(state, "shape") in _KNOWN_SHAPES
    )


def _property(state: BlockState, key: str) -> str | None:
    value = state.properties.get(key)
    return value.lower() if isinstance(value, str) else None


def _is_contiguous(values: set[int]) -> bool:
    return bool(values) and max(values) - min(values) + 1 == len(values)


def _contiguous_runs(values: list[int]) -> list[list[int]]:
    if not values:
        return []
    runs = [[values[0]]]
    for value in values[1:]:
        if value == runs[-1][-1] + 1:
            runs[-1].append(value)
        else:
            runs.append([value])
    return runs


def _nearby_positions(pos: BlockPos) -> tuple[BlockPos, ...]:
    return tuple(
        pos.offset(dx=dx, dy=dy, dz=dz)
        for dx in (-1, 0, 1)
        for dy in (-1, 0, 1)
        for dz in (-1, 0, 1)
        if (dx, dy, dz) != (0, 0, 0)
    )


def _anchor_order(pos: BlockPos) -> tuple[int, int, int]:
    return pos.y, pos.x, pos.z


def _proposal_order(
    candidate: StairSemanticCandidate,
) -> tuple[int, tuple[int, int, int], StairBounds, tuple[StairMember, ...]]:
    return (
        -len(candidate.members),
        _anchor_order(candidate.anchor),
        candidate.bounds,
        candidate.members,
    )


__all__ = [
    "StairBounds",
    "StairMember",
    "StairSemanticCandidate",
    "detect_stair_semantics",
]
