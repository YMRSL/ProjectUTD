from __future__ import annotations

from dataclasses import dataclass, field

from picasso.models.block import AIR, BlockPos, BlockState


@dataclass
class RegionData:
    blocks: dict[BlockPos, BlockState] = field(default_factory=dict)
    origin_cx: int = 0
    origin_cz: int = 0
    radius_chunks: int = 0
    surface_classes: dict[BlockPos, str] = field(default_factory=dict)
    space_classes: dict[BlockPos, str] = field(default_factory=dict)

    def get(self, pos: BlockPos) -> BlockState | None:
        return self.blocks.get(pos)

    def get_or_air(self, pos: BlockPos) -> BlockState:
        return self.blocks.get(pos, AIR)

    def set(self, pos: BlockPos, state: BlockState) -> None:
        self.blocks[pos] = state

    def copy(self) -> "RegionData":
        return RegionData(
            blocks=dict(self.blocks),
            origin_cx=self.origin_cx,
            origin_cz=self.origin_cz,
            radius_chunks=self.radius_chunks,
            surface_classes=dict(self.surface_classes),
            space_classes=dict(self.space_classes),
        )

    def bounding_box(self) -> tuple[BlockPos, BlockPos]:
        if not self.blocks:
            x0 = (self.origin_cx - self.radius_chunks) * 16
            z0 = (self.origin_cz - self.radius_chunks) * 16
            x1 = (self.origin_cx + self.radius_chunks + 1) * 16 - 1
            z1 = (self.origin_cz + self.radius_chunks + 1) * 16 - 1
            return BlockPos(x0, 0, z0), BlockPos(x1, 0, z1)

        xs = [pos.x for pos in self.blocks]
        ys = [pos.y for pos in self.blocks]
        zs = [pos.z for pos in self.blocks]
        return BlockPos(min(xs), min(ys), min(zs)), BlockPos(max(xs), max(ys), max(zs))

    def to_summary(self) -> dict:
        min_pos, max_pos = self.bounding_box()
        return {
            "block_count": len(self.blocks),
            "bounds": {"min": min_pos.to_dict(), "max": max_pos.to_dict()},
            "cx": self.origin_cx,
            "cz": self.origin_cz,
            "radius_chunks": self.radius_chunks,
        }
