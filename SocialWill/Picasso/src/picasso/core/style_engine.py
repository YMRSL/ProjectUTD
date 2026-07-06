from __future__ import annotations

import hashlib
import json
import random
from collections import defaultdict
from pathlib import Path
from typing import Any

from picasso.core.fragment_engine import FragmentEngine
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.noise_field import NoiseField
from picasso.core.pattern_matcher import PatternMatcher
from picasso.core.surface_classifier import adjacent_air_positions, classify_surfaces
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.models.style_pass import ReplaceOption, StylePass


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
        self.safe_replaceable, self.structural_never_touch = _load_safe_blocks(safe_blocks_path)

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
        changes = RegionData(origin_cx=region.origin_cx, origin_cz=region.origin_cz, radius_chunks=region.radius_chunks)
        by_rule: dict[Any, int] = defaultdict(int)
        noise = NoiseField(seed)

        for pos, state in sorted(region.blocks.items()):
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
                if _roll(seed, style_pass.name, rule_index, pos) > max(0.0, min(1.0, rule.weight * intensity)):
                    break
                target_pos, target_state = _apply_rule(region, pos, state, rule, seed, style_pass.name, rule_index)
                if target_pos is not None and target_state is not None:
                    existing = region.get(target_pos)
                    if existing is None or existing.full_id != target_state.full_id or existing.properties != target_state.properties:
                        changes.set(target_pos, target_state)
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
        changes = RegionData(origin_cx=region.origin_cx, origin_cz=region.origin_cz, radius_chunks=region.radius_chunks)
        by_rule: dict[Any, int] = defaultdict(int)
        mappings = {mapping["pattern"]: mapping["dd_block"] for mapping in style_pass.mappings}

        for match in self.pattern_matcher.find_matches(region):
            if match.pattern_name not in mappings:
                continue
            if space_filter and region.space_classes.get(match.anchor_pos) != space_filter:
                continue
            if _roll(seed, style_pass.name, match.pattern_name, match.anchor_pos) > intensity:
                continue
            dx, dy, dz = match.replacement_anchor_offset
            replace_pos = match.anchor_pos.offset(dx=dx, dy=dy, dz=dz)
            changes.set(replace_pos, BlockState.from_id(mappings[match.pattern_name]))
            for offset in match.clear_offsets or []:
                cx, cy, cz = offset
                clear_pos = match.anchor_pos.offset(dx=cx, dy=cy, dz=cz)
                if clear_pos != replace_pos:
                    changes.set(clear_pos, AIR)
            by_rule[match.pattern_name] += 1
        return changes, by_rule


def load_pass_registry(passes_dir: str | Path) -> dict[str, StylePass]:
    registry: dict[str, StylePass] = {}
    path = Path(passes_dir)
    if not path.exists():
        return registry
    for pass_file in sorted(path.glob("*.json")):
        data = json.loads(pass_file.read_text(encoding="utf-8"))
        if "rules" not in data and data.get("type") in {"fragment_pass", "pattern_replace"}:
            data["rules"] = []
        style_pass = StylePass.model_validate(data)
        registry[style_pass.name] = style_pass
    return registry


def _load_safe_blocks(path: str | Path) -> tuple[set[str], set[str]]:
    safe_path = Path(path)
    if not safe_path.exists():
        return set(), set()
    data = json.loads(safe_path.read_text(encoding="utf-8"))
    return set(data.get("replaceable", [])), set(data.get("structural_never_touch", []))


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
    if match.get("adjacent_air") and not adjacent_air_positions(region, pos):
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
            air_positions = adjacent_air_positions(region, pos)
            if not air_positions:
                return None, None
            target = air_positions[int(_roll(seed, pass_name, rule_index, pos) * len(air_positions)) % len(air_positions)]
        else:
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
    marker = _roll(seed, pass_name, rule_index, pos, "replace") * total
    running = 0.0
    for option in options:
        running += max(0.0, option.weight)
        if marker <= running:
            return BlockState.from_id(option.block, option.properties)
    option = options[-1]
    return BlockState.from_id(option.block, option.properties)


def _roll(seed: int, *parts: Any) -> float:
    payload = ":".join(str(part) for part in (seed, *parts)).encode("utf-8")
    digest = hashlib.sha256(payload).digest()
    integer = int.from_bytes(digest[:8], "big")
    return integer / 2**64
