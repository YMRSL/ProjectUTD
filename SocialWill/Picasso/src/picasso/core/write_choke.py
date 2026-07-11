from __future__ import annotations

import json
import logging
from dataclasses import dataclass, field
from pathlib import Path

from picasso.config import config
from picasso.core.block_taxonomy import BlockTaxonomy
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData

logger = logging.getLogger(__name__)


@dataclass
class ChokeResult:
    changes: RegionData
    skipped: list[dict] = field(default_factory=list)
    modded_positions: list[BlockPos] = field(default_factory=list)
    blocked_error: str | None = None
    blocked_message: str | None = None
    player_protection: dict = field(
        default_factory=lambda: {
            "status": "unavailable",
            "diagnostics": [
                {
                    "source": "player_protection",
                    "reason": "evaluator_not_configured",
                }
            ],
        }
    )

    @property
    def changed_count(self) -> int:
        return len(self.changes.blocks)


class WriteChoke:
    def __init__(
        self,
        safe_replaceable: set[str],
        structural_never_touch: set[str],
        marker_positions: set[BlockPos] | None = None,
        taxonomy: BlockTaxonomy | None = None,
        safety_policy_error: str | None = None,
        known_block_ids: set[str] | None = None,
        allowed_non_vanilla_ids: set[str] | None = None,
        player_protection_evaluator=None,
    ) -> None:
        self.safe_replaceable = set(safe_replaceable)
        self.structural_never_touch = set(structural_never_touch)
        self.marker_positions = marker_positions or set()
        self.taxonomy = taxonomy or BlockTaxonomy(config.block_taxonomy_path)
        self.safety_policy_error = safety_policy_error
        if self.safety_policy_error is None and not (
            self.safe_replaceable or self.structural_never_touch
        ):
            self.safety_policy_error = "Safety policy is missing, invalid, or empty."
        self.known_block_ids = None if known_block_ids is None else set(known_block_ids)
        self.allowed_non_vanilla_ids = (
            None if allowed_non_vanilla_ids is None else set(allowed_non_vanilla_ids)
        )
        self.player_protection_evaluator = player_protection_evaluator

    def validate(
        self,
        source_region: RegionData,
        raw_changes: RegionData,
        *,
        only_safe_blocks: bool = True,
        enforce_modded_gate: bool = False,
        force_modded_write: bool = False,
        include_player_built: bool = False,
    ) -> ChokeResult:
        result = ChokeResult(
            changes=_empty_changes_for(source_region),
            player_protection=self._player_protection_summary(),
        )
        if self.safety_policy_error is not None:
            result.blocked_error = "safety_policy_unavailable"
            result.blocked_message = self.safety_policy_error
            return result

        skipped_positions: set[BlockPos] = set()

        for pos, state in sorted(raw_changes.blocks.items()):
            reason = self._skip_reason(
                source_region,
                raw_changes,
                pos,
                state,
                only_safe_blocks,
                include_player_built,
            )
            if reason:
                result.skipped.append({"pos": pos.to_dict(), "reason": reason})
                skipped_positions.add(pos)
                continue
            final_state = _with_physics_properties(state)
            result.changes.set(pos, final_state)
            if raw_changes.write_contexts.get(pos):
                result.changes.write_contexts[pos] = raw_changes.write_contexts[pos]
            if pos in raw_changes.destructive_positions:
                result.changes.destructive_positions.add(pos)

        _drop_failed_atomic_groups(result, raw_changes, skipped_positions)
        if any(entry.get("reason") == "unknown_catalog_block" for entry in result.skipped):
            result.blocked_error = "unknown_catalog_block"
            result.blocked_message = (
                "The write references a non-vanilla block that is not present in any "
                "loaded semantic catalog. The entire operation was blocked."
            )
            result.changes = _empty_changes_for(source_region)
            return result
        result.modded_positions = [
            pos
            for pos, state in sorted(result.changes.blocks.items())
            if not state.is_air and state.namespace != "minecraft"
        ]

        if (
            enforce_modded_gate
            and result.modded_positions
            and not config.modded_write_verified
            and not force_modded_write
        ):
            result.blocked_error = "modded_write_unverified"
            result.blocked_message = (
                "This write would place non-vanilla blocks, but "
                "PICASSO_MODDED_WRITE_VERIFIED is false. Run the Phase 1.5 "
                "round-trip spike, or pass force_modded_write=true deliberately."
            )
            result.changes = _empty_changes_for(source_region)
        return result

    def _skip_reason(
        self,
        source_region: RegionData,
        raw_changes: RegionData,
        pos: BlockPos,
        state: BlockState,
        only_safe_blocks: bool,
        include_player_built: bool,
    ) -> str | None:
        context = raw_changes.write_contexts.get(pos, "decoration")
        existing = source_region.get(pos)

        region_reason = source_region.modification_block_reason(pos)
        if region_reason is not None:
            return region_reason
        if pos in source_region.block_entity_positions:
            return "block_entity_protected"
        if existing and existing.full_id in self.structural_never_touch:
            return "structural_never_touch"
        if pos in self.marker_positions:
            return "marker_protected"
        player_reason = self._player_protection_reason(pos, include_player_built)
        if player_reason is not None:
            return player_reason
        if self._is_unknown_non_vanilla(state):
            return "unknown_catalog_block"

        writes_air = state.is_air
        destructive = pos in raw_changes.destructive_positions
        if writes_air and context == "decoration" and not destructive:
            return "non_destructive_air_write"

        if context in {"pattern_clear", "room_envelope"}:
            return None

        if only_safe_blocks:
            existing_category = self.taxonomy.category(existing)
            if existing_category in {"air", "air_like"}:
                return None
            if existing and existing.full_id not in self.safe_replaceable:
                return "not_replaceable"
        return None

    def _player_protection_reason(
        self, pos: BlockPos, include_player_built: bool
    ) -> str | None:
        evaluator = self.player_protection_evaluator
        if evaluator is None:
            return None
        decision = evaluator.evaluate(pos)
        if not decision.protected:
            return None
        if include_player_built:
            return next(
                (
                    item.reason
                    for item in decision.provenance
                    if item.reason == "protected_region"
                ),
                None,
            )
        return decision.reason or "player_build_protected"

    def _player_protection_summary(self) -> dict:
        evaluator = self.player_protection_evaluator
        if evaluator is None:
            return {
                "status": "unavailable",
                "diagnostics": [
                    {
                        "source": "player_protection",
                        "reason": "evaluator_not_configured",
                    }
                ],
            }
        summary = evaluator.to_summary()
        return dict(summary) if isinstance(summary, dict) else {"status": "unavailable"}

    def _is_unknown_non_vanilla(self, state: BlockState) -> bool:
        if state.is_air or state.namespace == "minecraft":
            return False
        if self.known_block_ids is None and self.allowed_non_vanilla_ids is None:
            return False
        allowed: set[str] = set()
        if self.known_block_ids is not None:
            allowed.update(self.known_block_ids)
        if self.allowed_non_vanilla_ids is not None:
            allowed.update(self.allowed_non_vanilla_ids)
        return state.full_id not in allowed


def load_marker_positions(world_path: Path | None) -> set[BlockPos]:
    if world_path is None:
        return set()
    marker_dir = world_path / "picasso_markers"
    if not marker_dir.exists():
        return set()
    positions: set[BlockPos] = set()
    for marker_file in marker_dir.glob("*.json"):
        try:
            data = json.loads(marker_file.read_text(encoding="utf-8"))
            pos = data.get("pos", {})
            positions.add(BlockPos(int(pos["x"]), int(pos["y"]), int(pos["z"])))
        except Exception:
            logger.warning("Skipping invalid marker file: %s", marker_file)
    return positions


def _with_physics_properties(state: BlockState) -> BlockState:
    if state.is_air:
        return state
    if state.name.endswith("_leaves") and state.properties.get("persistent") != "true":
        properties = dict(state.properties)
        properties["persistent"] = "true"
        return BlockState(state.namespace, state.name, properties)
    return state


def _empty_changes_for(source_region: RegionData) -> RegionData:
    return RegionData(
        origin_cx=source_region.origin_cx,
        origin_cz=source_region.origin_cz,
        radius_chunks=source_region.radius_chunks,
        y_min=source_region.y_min,
        y_max=source_region.y_max,
        loaded_chunks=set(source_region.loaded_chunks),
    )


def _drop_failed_atomic_groups(
    result: ChokeResult,
    raw_changes: RegionData,
    skipped_positions: set[BlockPos],
) -> None:
    failed_positions = set(skipped_positions)
    groups = [set(group) for group in raw_changes.atomic_groups if len(group) > 1]

    while True:
        expanded = set(failed_positions)
        for group in groups:
            if group.intersection(failed_positions):
                expanded.update(group)
        if expanded == failed_positions:
            break
        failed_positions = expanded

    for pos in sorted(failed_positions - skipped_positions):
        if pos not in result.changes.blocks:
            continue
        result.changes.blocks.pop(pos, None)
        result.changes.write_contexts.pop(pos, None)
        result.changes.destructive_positions.discard(pos)
        result.skipped.append({"pos": pos.to_dict(), "reason": "atomic_group_failed"})


def block_id_from_state_or_air(state: BlockState | None) -> str:
    return state.full_id if state else AIR.full_id
