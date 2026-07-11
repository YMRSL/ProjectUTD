from __future__ import annotations

import json
import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData

logger = logging.getLogger(__name__)

YAW_ROTATIONS = (0, 90, 180, 270)
_MATCH_KEYS = {
    "air",
    "any",
    "block",
    "namespace",
    "name",
    "name_contains",
    "name_contains_any",
    "name_contains_all",
    "name_endswith",
    "properties",
}


@dataclass
class PatternMatch:
    pattern_name: str
    anchor_pos: BlockPos
    blocks: list[BlockPos]
    dd_replacement: str | None
    replacement_anchor_offset: tuple[int, int, int] = (0, 0, 0)
    clear_offsets: list[tuple[int, int, int]] | None = None
    matched_offsets: set[tuple[int, int, int]] = field(default_factory=set)
    yaw: int = 0


class PatternMatcher:
    def __init__(self, patterns_dir: str | Path) -> None:
        self.patterns_dir = Path(patterns_dir)
        self.patterns: dict[str, dict[str, Any]] = {}
        self.reload()

    def reload(self) -> None:
        self.patterns.clear()
        if not self.patterns_dir.exists():
            return
        for path in sorted(self.patterns_dir.glob("*.json"), key=lambda item: item.name.lower()):
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
                _validate_pattern(data, path)
            except Exception as exc:
                logger.warning("Skipping invalid pattern file %s: %s", path, exc)
                continue
            self.patterns[data["name"]] = data

    def find_matches(
        self,
        region: RegionData,
        *,
        include_experimental: bool = False,
    ) -> list[PatternMatch]:
        matches: list[PatternMatch] = []
        occupied: set[BlockPos] = set()
        candidates = sorted(region.blocks)

        for pattern in sorted(self.patterns.values(), key=_pattern_priority, reverse=True):
            if pattern.get("deprecated") or (
                pattern.get("experimental") and not include_experimental
            ):
                continue

            physical_matches: dict[tuple[BlockPos, ...], PatternMatch] = {}
            anchor = _offset(pattern["anchor"])
            for candidate in candidates:
                for yaw in YAW_ROTATIONS:
                    rotated_anchor = _rotate_offset(anchor, yaw)
                    anchor_pos = candidate.offset(
                        dx=-rotated_anchor[0],
                        dy=-rotated_anchor[1],
                        dz=-rotated_anchor[2],
                    )
                    # Halo blocks may satisfy offsets for a core-anchored
                    # pattern, but a halo anchor must never enter physical
                    # de-duplication/occupied state and suppress a core match.
                    if not region.is_target_position(anchor_pos):
                        continue
                    positions: list[BlockPos] = []
                    matched_offsets: set[tuple[int, int, int]] = set()
                    if not self._pattern_matches(
                        region,
                        pattern,
                        anchor_pos,
                        positions,
                        matched_offsets,
                        yaw,
                    ):
                        continue

                    match = PatternMatch(
                        pattern_name=pattern["name"],
                        anchor_pos=anchor_pos,
                        blocks=sorted(positions),
                        dd_replacement=pattern.get("dd_replacement"),
                        replacement_anchor_offset=_rotate_offset(
                            _offset(pattern.get("replacement_anchor_offset", [0, 0, 0])),
                            yaw,
                        ),
                        clear_offsets=[
                            _rotate_offset(_offset(offset), yaw)
                            for offset in pattern.get("clear_offsets", [])
                        ],
                        matched_offsets=matched_offsets,
                        yaw=yaw,
                    )
                    physical_key = tuple(match.blocks)
                    previous = physical_matches.get(physical_key)
                    if previous is None or _match_order(match) < _match_order(previous):
                        physical_matches[physical_key] = match

            for match in sorted(physical_matches.values(), key=_match_order):
                if any(pos in occupied for pos in match.blocks):
                    continue
                occupied.update(match.blocks)
                matches.append(match)
        return matches

    def _pattern_matches(
        self,
        region: RegionData,
        pattern: dict[str, Any],
        anchor_pos: BlockPos,
        positions: list[BlockPos],
        matched_offsets: set[tuple[int, int, int]],
        yaw: int,
    ) -> bool:
        optional_matches = 0
        optional_groups: dict[str, int] = {}
        for block_def in pattern["blocks"]:
            dx, dy, dz = _rotate_offset(_offset(block_def["offset"]), yaw)
            pos = anchor_pos.offset(dx=dx, dy=dy, dz=dz)
            state = region.get(pos)
            if not _match_block(state, block_def["match"], yaw=yaw):
                if block_def.get("optional", False):
                    continue
                return False
            if block_def.get("optional", False):
                optional_matches += 1
                group = block_def.get("optional_group")
                if group is not None:
                    optional_groups[group] = optional_groups.get(group, 0) + 1
            positions.append(pos)
            matched_offsets.add((dx, dy, dz))

        if optional_matches < pattern.get("min_optional_matches", 0):
            return False
        for group, minimum in pattern.get("min_optional_matches_by_group", {}).items():
            if optional_groups.get(group, 0) < minimum:
                return False
        return True


def _match_block(
    state: BlockState | None,
    condition: dict[str, Any],
    *,
    yaw: int = 0,
) -> bool:
    if condition.get("air"):
        return state is None or state.is_air
    if state is None or state.is_air:
        return False

    block = condition.get("block")
    if isinstance(block, str) and state.full_id != block:
        return False
    if isinstance(block, list) and state.full_id not in block:
        return False

    namespace = condition.get("namespace")
    if namespace is not None and state.namespace != namespace:
        return False
    name = condition.get("name")
    if name is not None and state.name != name:
        return False

    lowered_name = state.name.lower()
    contains = condition.get("name_contains")
    if isinstance(contains, str) and contains.lower() not in lowered_name:
        return False
    if isinstance(contains, list) and not any(token.lower() in lowered_name for token in contains):
        return False
    contains_any = condition.get("name_contains_any")
    if contains_any is not None and not any(
        token.lower() in lowered_name for token in contains_any
    ):
        return False
    contains_all = condition.get("name_contains_all")
    if contains_all is not None and not all(
        token.lower() in lowered_name for token in contains_all
    ):
        return False
    endswith = condition.get("name_endswith")
    if isinstance(endswith, str) and not lowered_name.endswith(endswith.lower()):
        return False
    if isinstance(endswith, list) and not any(
        lowered_name.endswith(token.lower()) for token in endswith
    ):
        return False

    for key, expected in condition.get("properties", {}).items():
        actual = state.properties.get(key)
        if actual is None:
            return False
        expected_values = expected if isinstance(expected, list) else [expected]
        rotated_values = {
            _rotate_property_value(key, expected_value, yaw)
            for expected_value in expected_values
        }
        if actual not in rotated_values:
            return False
    return True


def _validate_pattern(data: Any, path: Path) -> None:
    if not isinstance(data, dict):
        raise ValueError("pattern root must be an object")

    name = data.get("name")
    if not isinstance(name, str) or not name:
        raise ValueError("name is required and must be a non-empty string")
    if name != path.stem:
        raise ValueError(f"name must match filename ({path.stem})")
    description = data.get("description")
    if not isinstance(description, str) or not description:
        raise ValueError("description is required and must be a non-empty string")

    anchor = _validate_offset(data.get("anchor"), "anchor")
    blocks = data.get("blocks")
    if not isinstance(blocks, list) or not blocks:
        raise ValueError("blocks is required and must be a non-empty list")

    offsets: set[tuple[int, int, int]] = set()
    optional_count = 0
    optional_group_counts: dict[str, int] = {}
    anchor_block: dict[str, Any] | None = None
    for index, block_def in enumerate(blocks):
        location = f"blocks[{index}]"
        if not isinstance(block_def, dict):
            raise ValueError(f"{location} must be an object")
        offset = _validate_offset(block_def.get("offset"), f"{location}.offset")
        if offset in offsets:
            raise ValueError(f"duplicate block offset: {offset}")
        offsets.add(offset)

        condition = block_def.get("match")
        _validate_match_condition(condition, f"{location}.match")

        optional = block_def.get("optional", False)
        if not isinstance(optional, bool):
            raise ValueError(f"{location}.optional must be a boolean")
        group = block_def.get("optional_group")
        if group is not None:
            if not isinstance(group, str) or not group:
                raise ValueError(f"{location}.optional_group must be a non-empty string")
            if not optional:
                raise ValueError(f"{location}.optional_group requires optional=true")
        if optional:
            optional_count += 1
            if group is not None:
                optional_group_counts[group] = optional_group_counts.get(group, 0) + 1
        if offset == anchor:
            anchor_block = block_def

    if anchor_block is None:
        raise ValueError("anchor must equal one declared block offset")
    if anchor_block.get("optional", False):
        raise ValueError("anchor block must not be optional")
    if anchor_block["match"].get("air"):
        raise ValueError("anchor block must match a non-air candidate")

    _validate_optional_minimum(
        data.get("min_optional_matches", 0),
        optional_count,
        "min_optional_matches",
    )
    group_minimums = data.get("min_optional_matches_by_group", {})
    if not isinstance(group_minimums, dict):
        raise ValueError("min_optional_matches_by_group must be an object")
    for group, minimum in group_minimums.items():
        if not isinstance(group, str) or not group:
            raise ValueError("optional-group minimum keys must be non-empty strings")
        if group not in optional_group_counts:
            raise ValueError(f"unknown optional group: {group}")
        _validate_optional_minimum(
            minimum,
            optional_group_counts[group],
            f"min_optional_matches_by_group.{group}",
        )

    if "replacement_anchor_offset" in data:
        _validate_offset(data["replacement_anchor_offset"], "replacement_anchor_offset")
    clear_offsets = data.get("clear_offsets", [])
    if not isinstance(clear_offsets, list):
        raise ValueError("clear_offsets must be a list")
    seen_clear_offsets: set[tuple[int, int, int]] = set()
    for index, value in enumerate(clear_offsets):
        clear_offset = _validate_offset(value, f"clear_offsets[{index}]")
        if clear_offset in seen_clear_offsets:
            raise ValueError(f"duplicate clear offset: {clear_offset}")
        if clear_offset not in offsets:
            raise ValueError(f"clear offset is not a declared block offset: {clear_offset}")
        seen_clear_offsets.add(clear_offset)

    for field_name in ("experimental", "deprecated"):
        if field_name in data and not isinstance(data[field_name], bool):
            raise ValueError(f"{field_name} must be a boolean")
    if "experimental_reason" in data and not isinstance(data["experimental_reason"], str):
        raise ValueError("experimental_reason must be a string")
    if "dd_replacement" in data:
        replacement = data["dd_replacement"]
        if (
            not isinstance(replacement, str)
            or replacement.count(":") != 1
            or not all(replacement.split(":"))
        ):
            raise ValueError("dd_replacement must be a namespaced block id")


def _validate_match_condition(condition: Any, location: str) -> None:
    if not isinstance(condition, dict) or not condition:
        raise ValueError(f"{location} must be a non-empty object")
    unknown = set(condition) - _MATCH_KEYS
    if unknown:
        raise ValueError(f"{location} contains unsupported keys: {sorted(unknown)}")

    for boolean_key in ("air", "any"):
        if boolean_key in condition and condition[boolean_key] is not True:
            raise ValueError(f"{location}.{boolean_key} must be true when present")
    if condition.get("air") and len(condition) != 1:
        raise ValueError(f"{location}.air cannot be combined with other conditions")

    if "block" in condition:
        _validate_string_or_list(condition["block"], f"{location}.block")
    for key in ("namespace", "name"):
        if key in condition and (not isinstance(condition[key], str) or not condition[key]):
            raise ValueError(f"{location}.{key} must be a non-empty string")
    for key in (
        "name_contains",
        "name_contains_any",
        "name_contains_all",
        "name_endswith",
    ):
        if key in condition:
            _validate_string_or_list(condition[key], f"{location}.{key}")

    properties = condition.get("properties")
    if properties is not None:
        if not isinstance(properties, dict) or not properties:
            raise ValueError(f"{location}.properties must be a non-empty object")
        for key, expected in properties.items():
            if not isinstance(key, str) or not key:
                raise ValueError(f"{location}.properties keys must be non-empty strings")
            _validate_string_or_list(expected, f"{location}.properties.{key}")

    active_keys = set(condition) - {"properties"}
    if not active_keys and properties is None:
        raise ValueError(f"{location} contains no active condition")


def _validate_string_or_list(value: Any, location: str) -> None:
    if isinstance(value, str):
        if not value:
            raise ValueError(f"{location} must not be empty")
        return
    if not isinstance(value, list) or not value:
        raise ValueError(f"{location} must be a string or non-empty string list")
    if any(not isinstance(item, str) or not item for item in value):
        raise ValueError(f"{location} list entries must be non-empty strings")


def _validate_offset(value: Any, location: str) -> tuple[int, int, int]:
    if not isinstance(value, (list, tuple)) or len(value) != 3:
        raise ValueError(f"{location} must contain exactly three integers")
    if any(not isinstance(item, int) or isinstance(item, bool) for item in value):
        raise ValueError(f"{location} must contain exactly three integers")
    return int(value[0]), int(value[1]), int(value[2])


def _validate_optional_minimum(value: Any, available: int, location: str) -> None:
    if not isinstance(value, int) or isinstance(value, bool) or value < 0:
        raise ValueError(f"{location} must be a non-negative integer")
    if value > available:
        raise ValueError(f"{location}={value} exceeds {available} available optional blocks")


def _offset(value: Any) -> tuple[int, int, int]:
    return int(value[0]), int(value[1]), int(value[2])


def _pattern_priority(pattern: dict[str, Any]) -> tuple[int, int, str]:
    return (
        len(pattern["blocks"]),
        pattern.get("min_optional_matches", 0),
        pattern["name"],
    )


def _match_order(match: PatternMatch) -> tuple[Any, ...]:
    return (
        match.anchor_pos.x,
        match.anchor_pos.y,
        match.anchor_pos.z,
        match.yaw,
        tuple(match.blocks),
    )


def _rotate_offset(offset: tuple[int, int, int], yaw: int) -> tuple[int, int, int]:
    x, y, z = offset
    rot = yaw % 360
    if rot == 0:
        return x, y, z
    if rot == 90:
        return -z, y, x
    if rot == 180:
        return -x, y, -z
    if rot == 270:
        return z, y, -x
    raise ValueError(f"yaw must be a multiple of 90, got {yaw}")


def _rotate_property_value(key: str, value: str, yaw: int) -> str:
    rot = yaw % 360
    if key == "facing":
        facing_order = ("south", "west", "north", "east")
        if value in facing_order:
            return facing_order[(facing_order.index(value) + rot // 90) % 4]
    if key == "axis" and value in {"x", "z"} and rot in {90, 270}:
        return "z" if value == "x" else "x"
    if key == "rotation":
        try:
            return str((int(value) + (rot // 90) * 4) % 16)
        except ValueError:
            return value
    return value
