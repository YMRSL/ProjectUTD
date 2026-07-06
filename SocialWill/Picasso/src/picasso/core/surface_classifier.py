from __future__ import annotations

from collections import defaultdict

from picasso.models.block import BlockPos
from picasso.models.region import RegionData


HORIZONTAL_OFFSETS = ((1, 0), (-1, 0), (0, 1), (0, -1))


def classify_surfaces(region: RegionData) -> dict[BlockPos, str]:
    if not region.blocks:
        return {}

    column_max: dict[tuple[int, int], int] = defaultdict(lambda: -10_000)
    for pos in region.blocks:
        key = (pos.x, pos.z)
        if pos.y > column_max[key]:
            column_max[key] = pos.y

    classes: dict[BlockPos, str] = {}
    for pos, state in region.blocks.items():
        if state.is_air:
            continue
        above_air = region.get(pos.offset(dy=1)) is None
        below_solid = region.get(pos.offset(dy=-1)) is not None
        horizontal_air = [
            (dx, dz)
            for dx, dz in HORIZONTAL_OFFSETS
            if region.get(pos.offset(dx=dx, dz=dz)) is None
        ]

        local_top = column_max[(pos.x, pos.z)]
        if above_air and pos.y >= local_top - 1:
            surface = "rooftop"
        elif above_air and below_solid:
            surface = "floor"
        elif horizontal_air:
            surface = "outer_wall" if _air_column_extends(region, pos) else "inner_wall"
        else:
            surface = "solid"
        classes[pos] = surface

    region.surface_classes = classes
    region.space_classes = {
        pos: ("exterior" if surface in {"outer_wall", "rooftop"} else "interior")
        for pos, surface in classes.items()
    }
    return classes


def _air_column_extends(region: RegionData, pos: BlockPos) -> bool:
    for dx, dz in HORIZONTAL_OFFSETS:
        side = pos.offset(dx=dx, dz=dz)
        if region.get(side) is not None:
            continue
        air_count = 0
        for dy in range(1, 5):
            if region.get(side.offset(dy=dy)) is None:
                air_count += 1
        if air_count >= 3:
            return True
    return False


def adjacent_air_positions(region: RegionData, pos: BlockPos) -> list[BlockPos]:
    positions: list[BlockPos] = []
    for dx, dz in HORIZONTAL_OFFSETS:
        target = pos.offset(dx=dx, dz=dz)
        if region.get(target) is None:
            positions.append(target)
    return positions
