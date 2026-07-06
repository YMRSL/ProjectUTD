from __future__ import annotations

import random

from picasso.core.fragment_library import FragmentLibrary
from picasso.core.noise_field import NoiseField
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.fragment import Fragment
from picasso.models.region import RegionData


class FragmentEngine:
    def __init__(
        self,
        fragment_library: FragmentLibrary,
        safe_replaceable: set[str],
        structural_never_touch: set[str],
    ) -> None:
        self.fragment_library = fragment_library
        self.safe_replaceable = safe_replaceable
        self.structural_never_touch = structural_never_touch

    def preview(self, pass_def: dict, region: RegionData, seed: int = 42, intensity: float = 1.0) -> dict:
        changes = self.apply(pass_def, region, seed=seed, intensity=intensity)
        return _changes_to_preview(changes)

    def apply(
        self, pass_def: dict, region: RegionData, seed: int = 42, intensity: float = 1.0
    ) -> RegionData:
        rng = random.Random(f"{seed}:{pass_def.get('name')}:fragment")
        noise = NoiseField(seed)
        candidates = [
            pos
            for pos, surface in region.surface_classes.items()
            if surface == pass_def.get("anchor_surface")
        ]
        changes = RegionData(origin_cx=region.origin_cx, origin_cz=region.origin_cz, radius_chunks=region.radius_chunks)
        placed_anchors: list[BlockPos] = []
        density = float(pass_def.get("density", 0.0)) * intensity
        min_spacing = int(pass_def.get("min_spacing", 0))
        noise_cfg = pass_def.get("noise") or {}
        fragment_names = list(pass_def.get("fragments", []))
        if not fragment_names:
            return changes

        for anchor in sorted(candidates):
            current = region.get(anchor)
            if not current or current.full_id in self.structural_never_touch:
                continue
            if pass_def.get("only_safe_anchor_blocks", True) and current.full_id not in self.safe_replaceable:
                continue
            space_filter = pass_def.get("space_filter")
            if space_filter and region.space_classes.get(anchor) != space_filter:
                continue
            if noise_cfg and noise.sample_2d(anchor.x, anchor.z, float(noise_cfg.get("scale", 0.05))) < float(
                noise_cfg.get("threshold", 0.4)
            ):
                continue
            if rng.random() > density:
                continue
            if min_spacing and any(_distance_xz(anchor, placed) < min_spacing for placed in placed_anchors):
                continue
            fragment = self.fragment_library.get(rng.choice(fragment_names))
            if fragment is None or not self._can_place_fragment(fragment, region, changes, anchor):
                continue
            self._place_fragment(fragment, region, changes, anchor, rng)
            placed_anchors.append(anchor)
        return changes

    def _can_place_fragment(
        self, fragment: Fragment, region: RegionData, changes: RegionData, anchor: BlockPos
    ) -> bool:
        if fragment.requires_clear_above:
            for dy in range(1, fragment.min_clear_height + 1):
                if region.get(anchor.offset(dy=dy)) is not None:
                    return False
        for block in fragment.blocks:
            dx, dy, dz = block.offset
            target = anchor.offset(dx=dx, dy=dy, dz=dz)
            existing = changes.get(target) or region.get(target)
            if existing and existing.full_id in self.structural_never_touch:
                return False
            if block.block == "minecraft:air" and not fragment.destructive:
                return False
        return True

    def _place_fragment(
        self,
        fragment: Fragment,
        region: RegionData,
        changes: RegionData,
        anchor: BlockPos,
        rng: random.Random,
    ) -> None:
        for block in fragment.blocks:
            if rng.random() > block.probability:
                continue
            dx, dy, dz = block.offset
            target = anchor.offset(dx=dx, dy=dy, dz=dz)
            existing = changes.get(target) or region.get(target)
            if block.preserve_existing and existing is not None and not existing.is_air:
                continue
            state = AIR if block.block == "minecraft:air" else BlockState.from_id(block.block, block.properties)
            changes.set(target, state)


def _distance_xz(a: BlockPos, b: BlockPos) -> float:
    return ((a.x - b.x) ** 2 + (a.z - b.z) ** 2) ** 0.5


def _changes_to_preview(changes: RegionData) -> dict:
    samples = [
        {"pos": pos.to_dict(), "to": state.full_id}
        for pos, state in list(changes.blocks.items())[:20]
    ]
    return {
        "would_change": len(changes.blocks),
        "by_rule": [{"rule_index": 0, "description": "fragment placements", "count": len(changes.blocks)}],
        "sample_changes": samples,
    }
