from __future__ import annotations

import json
import logging
from collections import defaultdict
from pathlib import Path
from typing import Any

from picasso.core.deterministic import roll
from picasso.core.fragment_engine import FragmentEngine
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.noise_field import NoiseField
from picasso.core.pattern_matcher import PatternMatcher
from picasso.core.surface_classifier import adjacent_air_positions, classify_surfaces
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.models.style_pass import ReplaceOption, StylePass

logger = logging.getLogger(__name__)


def pass_has_destructive_content(
    style_pass: StylePass,
    fragment_library: FragmentLibrary | None,
) -> bool:
    """Resolve operation-level destructiveness, including referenced fragments."""

    if bool(getattr(style_pass, "destructive", False)):
        return True
    if getattr(style_pass, "type", "block_pass") != "fragment_pass":
        return False
    fragment_names = tuple(getattr(style_pass, "fragments", ()))
    if fragment_library is None:
        return bool(fragment_names)
    return any(
        (fragment := fragment_library.get(name)) is None or fragment.destructive
        for name in fragment_names
    )


class StyleEngine:
    def __init__(
        self,
        pass_registry: dict[str, StylePass],
        safe_blocks_path: str | Path,
        pattern_matcher: PatternMatcher | None = None,
        fragment_library: FragmentLibrary | None = None,
    ) -> None:
        self.pass_registry = pass_registry
        self.pattern_matcher = pattern_matcher
        self.fragment_library = fragment_library
        (
            self.safe_replaceable,
            self.structural_never_touch,
            self.safety_policy_error,
        ) = _load_safe_blocks(safe_blocks_path)

    def preview(
        self,
        pass_name: str,
        region: RegionData,
        intensity: float = 1.0,
        seed: int = 42,
        space_filter: str | None = None,
    ) -> dict:
        changes, by_rule = self._compute_changes(pass_name, region, intensity, seed, space_filter)
        samples = []
        for pos, state in list(changes.blocks.items())[:20]:
            old = region.get(pos)
            samples.append(
                {
                    "pos": pos.to_dict(),
                    "from": old.full_id if old else "minecraft:air",
                    "to": state.full_id,
                }
            )
        return {
            "would_change": len(changes.blocks),
            "by_rule": [
                {"rule_index": key, "description": str(key), "count": count}
                for key, count in by_rule.items()
            ],
            "sample_changes": samples,
            "summary": f"{len(changes.blocks)} blocks would change.",
        }

    def apply(
        self,
        pass_name: str,
        region: RegionData,
        intensity: float = 1.0,
        seed: int = 42,
        space_filter: str | None = None,
    ) -> RegionData:
        changes, _ = self._compute_changes(pass_name, region, intensity, seed, space_filter)
        return changes

    def _compute_changes(
        self,
        pass_name: str,
        region: RegionData,
        intensity: float,
        seed: int,
        space_filter: str | None,
    ) -> tuple[RegionData, dict[Any, int]]:
        style_pass = self.pass_registry.get(pass_name)
        if style_pass is None:
            raise KeyError(f"Style pass not found: {pass_name}")
        if not region.surface_classes:
            classify_surfaces(region)

        if style_pass.type == "fragment_pass":
            if self.fragment_library is None:
                raise RuntimeError("Fragment library is not loaded")
            pass_def = style_pass.model_dump()
            if space_filter is not None:
                pass_def["space_filter"] = space_filter
            changes = FragmentEngine(
                self.fragment_library, self.safe_replaceable, self.structural_never_touch
            ).apply(pass_def, region, seed=seed, intensity=intensity)
            return changes, {"fragment": len(changes.blocks)}

        if style_pass.type == "pattern_replace":
            return self._apply_pattern_pass(style_pass, region, intensity, seed, space_filter)

        return self._apply_block_pass(style_pass, region, intensity, seed, space_filter)

    def _apply_block_pass(
        self,
        style_pass: StylePass,
        region: RegionData,
        intensity: float,
        seed: int,
        space_filter: str | None,
    ) -> tuple[RegionData, dict[Any, int]]:
        changes = _changes_for(region)
        by_rule: dict[Any, int] = defaultdict(int)
        noise = NoiseField(seed)

        for pos, state in sorted(region.blocks.items()):
            if not region.is_modifiable(pos):
                continue
            if state.is_air or state.full_id in self.structural_never_touch:
                continue
            if style_pass.only_safe_blocks and state.full_id not in self.safe_replaceable:
                continue
            if space_filter and region.space_classes.get(pos) != space_filter:
                continue
            for rule_index, rule in enumerate(style_pass.rules):
                if not _matches_rule(region, pos, state, rule.match):
                    continue
                if rule.noise and noise.sample_3d(pos.x, pos.y, pos.z, rule.noise.scale) < rule.noise.threshold:
                    break
                if roll(seed, style_pass.name, rule_index, pos) > max(0.0, min(1.0, rule.weight * intensity)):
                    break
                target_pos, target_state = _apply_rule(region, pos, state, rule, seed, style_pass.name, rule_index)
                if target_pos is not None and target_state is not None:
                    existing = region.get(target_pos)
                    if existing is None or existing.full_id != target_state.full_id or existing.properties != target_state.properties:
                        changes.set(target_pos, target_state)
                        changes.write_contexts[target_pos] = "decoration"
                        if target_state.is_air and style_pass.destructive:
                            changes.destructive_positions.add(target_pos)
                        by_rule[rule_index] += 1
                break
        return changes, by_rule

    def _apply_pattern_pass(
        self,
        style_pass: StylePass,
        region: RegionData,
        intensity: float,
        seed: int,
        space_filter: str | None,
    ) -> tuple[RegionData, dict[Any, int]]:
        if self.pattern_matcher is None:
            raise RuntimeError("Pattern matcher is not loaded")
        changes = _changes_for(region)
        by_rule: dict[Any, int] = defaultdict(int)
        mappings = {mapping["pattern"]: mapping["dd_block"] for mapping in style_pass.mappings}

        for match in self.pattern_matcher.find_matches(region):
            if match.pattern_name not in mappings:
                continue
            if not region.is_modifiable(match.anchor_pos):
                continue
            if space_filter and region.space_classes.get(match.anchor_pos) != space_filter:
                continue
            if roll(seed, style_pass.name, match.pattern_name, match.anchor_pos) > intensity:
                continue
            dx, dy, dz = match.replacement_anchor_offset
            replace_pos = match.anchor_pos.offset(dx=dx, dy=dy, dz=dz)
            if not region.is_modifiable(replace_pos):
                continue
            atomic_group = {replace_pos}
            clear_positions: list[BlockPos] = []
            group_is_modifiable = True
            for offset in match.clear_offsets or []:
                if offset not in match.matched_offsets:
                    continue
                cx, cy, cz = offset
                clear_pos = match.anchor_pos.offset(dx=cx, dy=cy, dz=cz)
                existing = region.get(clear_pos)
                if clear_pos != replace_pos and existing is not None and not existing.is_air:
                    if not region.is_modifiable(clear_pos):
                        group_is_modifiable = False
                        break
                    clear_positions.append(clear_pos)
            if not group_is_modifiable:
                continue
            changes.set(replace_pos, BlockState.from_id(mappings[match.pattern_name]))
            changes.write_contexts[replace_pos] = "decoration"
            for clear_pos in clear_positions:
                changes.set(clear_pos, AIR)
                changes.write_contexts[clear_pos] = "pattern_clear"
                atomic_group.add(clear_pos)
            if len(atomic_group) > 1:
                changes.atomic_groups.append(atomic_group)
            by_rule[match.pattern_name] += 1
        return changes, by_rule


def load_pass_registry(passes_dir: str | Path) -> dict[str, StylePass]:
    registry: dict[str, StylePass] = {}
    path = Path(passes_dir)
    if not path.exists():
        logger.warning("Passes directory missing: %s", path)
        return registry
    for pass_file in sorted(path.glob("*.json")):
        try:
            data = json.loads(pass_file.read_text(encoding="utf-8"))
            _validate_pass_data(data, pass_file)
            style_pass = StylePass.model_validate(data)
        except Exception as exc:
            logger.warning("Skipping invalid pass %s: %s", pass_file, exc)
            continue
        registry[style_pass.name] = style_pass
    return registry


def _validate_pass_data(data: Any, path: Path) -> None:
    if not isinstance(data, dict):
        raise ValueError("pass definition must be a JSON object")
    expected_name = path.stem
    if data.get("name") != expected_name:
        raise ValueError(f"name must match filename ({expected_name})")


def _load_safe_blocks(path: str | Path) -> tuple[set[str], set[str], str | None]:
    safe_path = Path(path)
    if not safe_path.exists():
        message = f"Safety policy file is missing: {safe_path}"
        logger.error(message)
        return set(), set(), message
    try:
        data = json.loads(safe_path.read_text(encoding="utf-8"))
    except Exception as exc:
        message = f"Safety policy file is invalid: {safe_path}: {exc}"
        logger.error(message)
        return set(), set(), message
    replaceable = data.get("replaceable") if isinstance(data, dict) else None
    never_touch = data.get("structural_never_touch") if isinstance(data, dict) else None
    if not _is_string_list(replaceable) or not _is_string_list(never_touch):
        message = (
            "Safety policy must contain string lists named replaceable and "
            f"structural_never_touch: {safe_path}"
        )
        logger.error(message)
        return set(), set(), message
    if not replaceable and not never_touch:
        message = f"Safety policy is empty: {safe_path}"
        logger.error(message)
        return set(), set(), message
    return set(replaceable), set(never_touch), None


def _is_string_list(value: Any) -> bool:
    return isinstance(value, list) and all(isinstance(item, str) and item for item in value)


def _changes_for(region: RegionData) -> RegionData:
    return RegionData(
        origin_cx=region.origin_cx,
        origin_cz=region.origin_cz,
        radius_chunks=region.radius_chunks,
        y_min=region.y_min,
        y_max=region.y_max,
        loaded_chunks=set(region.loaded_chunks),
    )


def _matches_rule(region: RegionData, pos: BlockPos, state: BlockState, match: dict[str, Any]) -> bool:
    block = match.get("block")
    if isinstance(block, str) and state.full_id != block:
        return False
    if isinstance(block, list) and state.full_id not in block:
        return False
    namespace = match.get("namespace")
    if namespace and state.namespace != namespace:
        return False
    contains = match.get("name_contains")
    if contains and contains.lower() not in state.name.lower():
        return False
    surface = match.get("surface")
    if isinstance(surface, str) and region.surface_classes.get(pos) != surface:
        return False
    if isinstance(surface, list) and region.surface_classes.get(pos) not in surface:
        return False
    if match.get("adjacent_air") and not any(
        region.is_modifiable(target) for target in adjacent_air_positions(region, pos)
    ):
        return False
    if "y_min" in match and pos.y < int(match["y_min"]):
        return False
    if "y_max" in match and pos.y > int(match["y_max"]):
        return False
    return True


def _apply_rule(
    region: RegionData,
    pos: BlockPos,
    state: BlockState,
    rule: Any,
    seed: int,
    pass_name: str,
    rule_index: int,
) -> tuple[BlockPos | None, BlockState | None]:
    if rule.action == "replace":
        if not rule.replace_with:
            return None, None
        return pos, _choose_replacement(rule.replace_with, seed, pass_name, rule_index, pos)
    if rule.action == "remove":
        return pos, AIR
    if rule.action == "place_adjacent":
        if not rule.place_block:
            return None, None
        direction = rule.direction or "above"
        if direction == "above":
            target = pos.offset(dy=1)
        elif direction == "below":
            target = pos.offset(dy=-1)
        elif direction == "air_side":
            air_positions = [
                target
                for target in adjacent_air_positions(region, pos)
                if region.is_modifiable(target)
            ]
            if not air_positions:
                return None, None
            target = air_positions[int(roll(seed, pass_name, rule_index, pos) * len(air_positions)) % len(air_positions)]
        else:
            return None, None
        if not region.is_modifiable(target):
            return None, None
        if region.get(target) is not None:
            return None, None
        return target, BlockState.from_id(rule.place_block)
    return None, None


def _choose_replacement(
    options: list[ReplaceOption], seed: int, pass_name: str, rule_index: int, pos: BlockPos
) -> BlockState:
    total = sum(max(0.0, option.weight) for option in options)
    if total <= 0:
        option = options[0]
        return BlockState.from_id(option.block, option.properties)
    marker = roll(seed, pass_name, rule_index, pos, "replace") * total
    running = 0.0
    for option in options:
        running += max(0.0, option.weight)
        if marker <= running:
            return BlockState.from_id(option.block, option.properties)
    option = options[-1]
    return BlockState.from_id(option.block, option.properties)
