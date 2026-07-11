from __future__ import annotations

from picasso.core.deterministic import choice_index, roll
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.noise_field import NoiseField
from picasso.core.surface_classifier import adjacent_air_positions
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

    def preview(
        self,
        pass_def: dict,
        region: RegionData,
        seed: int = 42,
        intensity: float = 1.0,
        initial_placed_anchors: list[BlockPos] | None = None,
    ) -> dict:
        changes = self.apply(
            pass_def,
            region,
            seed=seed,
            intensity=intensity,
            initial_placed_anchors=initial_placed_anchors,
        )
        return _changes_to_preview(changes)

    def apply(
        self,
        pass_def: dict,
        region: RegionData,
        seed: int = 42,
        intensity: float = 1.0,
        initial_placed_anchors: list[BlockPos] | None = None,
    ) -> RegionData:
        noise = NoiseField(seed)
        anchor_surface = pass_def.get("anchor_surface")
        candidates = [
            pos
            for pos, surface in region.surface_classes.items()
            if (anchor_surface == "any" or surface == anchor_surface)
            and region.is_modifiable(pos)
        ]
        changes = RegionData(
            origin_cx=region.origin_cx,
            origin_cz=region.origin_cz,
            radius_chunks=region.radius_chunks,
            y_min=region.y_min,
            y_max=region.y_max,
            loaded_chunks=set(region.loaded_chunks),
        )
        placed_anchors: list[BlockPos] = list(initial_placed_anchors or [])
        density = max(0.0, min(1.0, float(pass_def.get("density", 0.0)) * intensity))
        min_spacing = int(pass_def.get("min_spacing", 0))
        noise_cfg = pass_def.get("noise") or {}
        fragment_names = list(pass_def.get("fragments", []))
        if not fragment_names:
            return changes
        fragments: list[Fragment] = []
        for name in fragment_names:
            fragment = self.fragment_library.get(name)
            if fragment is None:
                raise ValueError(f"fragment pass references missing fragment: {name}")
            fragments.append(fragment)

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
            pass_name = str(pass_def.get("name", "fragment_pass"))
            if roll(seed, pass_name, "density", anchor) >= density:
                continue
            if min_spacing and any(_distance_xz(anchor, placed) < min_spacing for placed in placed_anchors):
                continue
            compatible: list[Fragment] = []
            for candidate_fragment in fragments:
                if _fragment_compatible(
                    candidate_fragment, anchor_surface
                ) and _fragment_matches_anchor(candidate_fragment, region, anchor):
                    compatible.append(candidate_fragment)
            if not compatible:
                continue
            fragment = compatible[
                choice_index(seed, len(compatible), pass_name, "select", anchor)
            ]
            rotation = _resolve_rotation(fragment, region, anchor, pass_name, seed)
            if not self._can_place_fragment(fragment, region, changes, anchor, rotation):
                continue
            emitted_positions = self._place_fragment(
                fragment,
                region,
                changes,
                anchor,
                pass_name,
                seed,
                rotation,
            )
            if len(emitted_positions) > 1:
                changes.atomic_groups.append(emitted_positions)
            placed_anchors.append(anchor)
        return changes

    def _can_place_fragment(
        self,
        fragment: Fragment,
        region: RegionData,
        changes: RegionData,
        anchor: BlockPos,
        rotation: int,
    ) -> bool:
        if fragment.requires_clear_above:
            footprint_columns = {
                (rotated[0], rotated[2])
                for block in fragment.blocks
                for rotated in [_rotate_offset(block.offset, rotation)]
            }
            for dx, dz in sorted(footprint_columns):
                for dy in range(1, fragment.min_clear_height + 1):
                    clearance_pos = anchor.offset(dx=dx, dy=dy, dz=dz)
                    if not region.is_modifiable(clearance_pos):
                        return False
                    if region.get(clearance_pos) is not None:
                        return False
        for block in fragment.blocks:
            dx, dy, dz = _rotate_offset(block.offset, rotation)
            target = anchor.offset(dx=dx, dy=dy, dz=dz)
            if not region.is_modifiable(target):
                return False
            if (
                fragment.anchor_surface in {"outer_wall", "inner_wall"}
                and block.block == "minecraft:vine"
                and not _has_vine_support(region, changes, target, rotation)
            ):
                return False
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
        pass_name: str,
        seed: int,
        rotation: int,
    ) -> set[BlockPos]:
        emitted_positions: set[BlockPos] = set()
        for block in fragment.blocks:
            if roll(seed, pass_name, "block", anchor, block.offset) >= block.probability:
                continue
            dx, dy, dz = _rotate_offset(block.offset, rotation)
            target = anchor.offset(dx=dx, dy=dy, dz=dz)
            existing = changes.get(target) or region.get(target)
            if block.preserve_existing and existing is not None and not existing.is_air:
                continue
            state = (
                AIR
                if block.block == "minecraft:air"
                else BlockState.from_id(block.block, _rotate_properties(block.properties, rotation))
            )
            changes.set(target, state)
            emitted_positions.add(target)
            changes.write_contexts[target] = "decoration"
            if state.is_air and fragment.destructive:
                changes.destructive_positions.add(target)
        return emitted_positions


def _distance_xz(a: BlockPos, b: BlockPos) -> float:
    return ((a.x - b.x) ** 2 + (a.z - b.z) ** 2) ** 0.5


def _fragment_compatible(fragment: Fragment | None, anchor_surface: str | None) -> bool:
    if fragment is None:
        return False
    return fragment.anchor_surface in {"any", anchor_surface}


def _fragment_matches_anchor(
    fragment: Fragment,
    region: RegionData,
    anchor: BlockPos,
) -> bool:
    hint = fragment.match_hint
    if hint is None:
        return True
    state = region.get(anchor)
    if hint == "glass_pane":
        return bool(
            state
            and state.namespace == "minecraft"
            and (
                state.name == "glass_pane"
                or state.name.endswith("_stained_glass_pane")
            )
        )
    raise ValueError(f"unsupported fragment match_hint: {hint}")


def _resolve_rotation(
    fragment: Fragment,
    region: RegionData,
    anchor: BlockPos,
    pass_name: str,
    seed: int,
) -> int:
    if not fragment.orientable:
        return 0
    surface = region.surface_classes.get(anchor)
    if surface in {"outer_wall", "inner_wall"}:
        air_positions = [
            target
            for target in adjacent_air_positions(region, anchor)
            if region.is_modifiable(target)
        ]
        if not air_positions:
            return 0
        chosen = air_positions[choice_index(seed, len(air_positions), pass_name, "normal", anchor)]
        dx = chosen.x - anchor.x
        dz = chosen.z - anchor.z
        if dx == 0 and dz == 1:
            return 0
        if dx == -1 and dz == 0:
            return 90
        if dx == 0 and dz == -1:
            return 180
        if dx == 1 and dz == 0:
            return 270
        return 0
    return (choice_index(seed, 4, pass_name, "yaw", anchor) * 90) % 360


def _has_vine_support(
    region: RegionData,
    changes: RegionData,
    target: BlockPos,
    rotation: int,
) -> bool:
    normal_x, _normal_y, normal_z = _rotate_offset((0, 0, 1), rotation)
    support_pos = target.offset(dx=-normal_x, dz=-normal_z)
    support = changes.get(support_pos) or region.get(support_pos)
    return support is not None and not support.is_air


def _rotate_offset(offset: tuple[int, int, int], rotation: int) -> tuple[int, int, int]:
    x, y, z = offset
    rot = rotation % 360
    if rot == 0:
        return x, y, z
    if rot == 90:
        return -z, y, x
    if rot == 180:
        return -x, y, -z
    if rot == 270:
        return z, y, -x
    return x, y, z


def _rotate_properties(properties: dict[str, str], rotation: int) -> dict[str, str]:
    if not properties or rotation % 360 == 0:
        return dict(properties)
    rotated = dict(properties)
    if "facing" in rotated:
        rotated["facing"] = _rotate_facing(rotated["facing"], rotation)
    if "axis" in rotated and rotated["axis"] in {"x", "z"} and rotation % 180 != 0:
        rotated["axis"] = "z" if rotated["axis"] == "x" else "x"
    return rotated


def _rotate_facing(facing: str, rotation: int) -> str:
    order = ["south", "west", "north", "east"]
    if facing not in order:
        return facing
    steps = (rotation % 360) // 90
    return order[(order.index(facing) + steps) % len(order)]


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
