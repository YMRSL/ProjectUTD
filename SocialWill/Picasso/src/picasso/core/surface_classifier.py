from __future__ import annotations

from collections import defaultdict

from picasso.core.block_taxonomy import BlockTaxonomy
from picasso.config import config
from picasso.models.block import BlockPos
from picasso.models.region import RegionData


HORIZONTAL_OFFSETS = ((1, 0), (-1, 0), (0, 1), (0, -1))
_TAXONOMY = BlockTaxonomy(config.block_taxonomy_path)


def classify_surfaces(region: RegionData) -> dict[BlockPos, str]:
    if not region.blocks:
        return {}

    column_max: dict[tuple[int, int], int] = defaultdict(lambda: -10_000)
    for pos, state in region.blocks.items():
        if not _TAXONOMY.is_solid_for_classification(state):
            continue
        key = (pos.x, pos.z)
        if pos.y > column_max[key]:
            column_max[key] = pos.y

    classes: dict[BlockPos, str] = {}
    for pos, state in region.blocks.items():
        if not _TAXONOMY.is_solid_for_classification(state):
            continue
        above_air = _is_air_for_classification(region, pos.offset(dy=1))
        below_air = _is_air_for_classification(region, pos.offset(dy=-1))
        horizontal_air = [
            (dx, dz)
            for dx, dz in HORIZONTAL_OFFSETS
            if _is_air_for_classification(region, pos.offset(dx=dx, dz=dz))
        ]

        local_top = column_max[(pos.x, pos.z)]
        if above_air and pos.y >= local_top - 1:
            surface = "rooftop"
        elif above_air and not below_air:
            surface = "floor"
        elif not above_air and below_air:
            surface = "ceiling"
        elif horizontal_air:
            surface = "outer_wall" if _air_column_extends(region, pos) else "inner_wall"
        else:
            surface = "embedded"
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
        if not _is_air_for_classification(region, side):
            continue
        air_count = 0
        for dy in range(1, 5):
            if _is_air_for_classification(region, side.offset(dy=dy)):
                air_count += 1
        if air_count >= 3:
            return True
    return False


def adjacent_air_positions(region: RegionData, pos: BlockPos) -> list[BlockPos]:
    positions: list[BlockPos] = []
    for dx, dz in HORIZONTAL_OFFSETS:
        target = pos.offset(dx=dx, dz=dz)
        if _is_air_for_classification(region, target):
            positions.append(target)
    return positions


def _is_air_for_classification(region: RegionData, pos: BlockPos) -> bool:
    return _TAXONOMY.is_air_for_classification(region.get(pos))
