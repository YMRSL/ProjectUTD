from __future__ import annotations

from dataclasses import dataclass, field
from typing import Iterator

from picasso.models.block import AIR, BlockPos, BlockState


@dataclass
class RegionData:
    blocks: dict[BlockPos, BlockState] = field(default_factory=dict)
    origin_cx: int = 0
    origin_cz: int = 0
    radius_chunks: int = 0
    y_min: int | None = None
    y_max: int | None = None
    read_y_min: int | None = None
    read_y_max: int | None = None
    chunks_read: int = 0
    chunks_missing: int = 0
    context_chunks_read: int = 0
    context_chunks_missing: int = 0
    loaded_chunks: set[tuple[int, int]] = field(default_factory=set)
    halo_positions: set[BlockPos] = field(default_factory=set)
    block_entity_positions: set[BlockPos] = field(default_factory=set)
    surface_classes: dict[BlockPos, str] = field(default_factory=dict)
    space_classes: dict[BlockPos, str] = field(default_factory=dict)
    write_contexts: dict[BlockPos, str] = field(default_factory=dict)
    destructive_positions: set[BlockPos] = field(default_factory=set)
    atomic_groups: list[set[BlockPos]] = field(default_factory=list)

    def __post_init__(self) -> None:
        """Preserve compatibility with complete legacy region reads.

        Older bridge implementations only reported aggregate chunk counts.  When
        such a read is known to be complete, every requested chunk is safe to
        treat as loaded.  A partial read without explicit ``loaded_chunks`` stays
        empty and therefore fails closed at write time.
        """
        if self.read_y_min is None:
            self.read_y_min = self.y_min
        if self.read_y_max is None:
            self.read_y_max = self.y_max
        if self.loaded_chunks or self.chunks_missing != 0:
            return
        expected_chunks = (self.radius_chunks * 2 + 1) ** 2
        if self.chunks_read not in {0, expected_chunks}:
            return
        self.loaded_chunks = {
            (chunk_x, chunk_z)
            for chunk_x in range(
                self.origin_cx - self.radius_chunks,
                self.origin_cx + self.radius_chunks + 1,
            )
            for chunk_z in range(
                self.origin_cz - self.radius_chunks,
                self.origin_cz + self.radius_chunks + 1,
            )
        }

    def get(self, pos: BlockPos) -> BlockState | None:
        return self.blocks.get(pos)

    def get_or_air(self, pos: BlockPos) -> BlockState:
        return self.blocks.get(pos, AIR)

    def set(self, pos: BlockPos, state: BlockState) -> None:
        self.blocks[pos] = state

    def is_target_position(self, pos: BlockPos) -> bool:
        """Whether a position belongs to the requested core region, not its halo."""
        if pos in self.halo_positions:
            return False
        x_min = (self.origin_cx - self.radius_chunks) * 16
        z_min = (self.origin_cz - self.radius_chunks) * 16
        x_max = (self.origin_cx + self.radius_chunks + 1) * 16 - 1
        z_max = (self.origin_cz + self.radius_chunks + 1) * 16 - 1
        if not (x_min <= pos.x <= x_max and z_min <= pos.z <= z_max):
            return False
        if self.y_min is not None and pos.y < self.y_min:
            return False
        if self.y_max is not None and pos.y > self.y_max:
            return False
        return True

    def iter_target_blocks(self) -> Iterator[tuple[BlockPos, BlockState]]:
        """Iterate core blocks while retaining halo blocks for neighbor lookups."""
        for pos, state in self.blocks.items():
            if self.is_target_position(pos):
                yield pos, state

    def target_block_count(self) -> int:
        return sum(1 for _pos, _state in self.iter_target_blocks())

    def _target_metrics(self) -> tuple[int, BlockPos, BlockPos]:
        count = 0
        min_x = min_y = min_z = 0
        max_x = max_y = max_z = 0
        for pos, _state in self.iter_target_blocks():
            if count == 0:
                min_x = max_x = pos.x
                min_y = max_y = pos.y
                min_z = max_z = pos.z
            else:
                min_x = min(min_x, pos.x)
                min_y = min(min_y, pos.y)
                min_z = min(min_z, pos.z)
                max_x = max(max_x, pos.x)
                max_y = max(max_y, pos.y)
                max_z = max(max_z, pos.z)
            count += 1
        if count:
            return (
                count,
                BlockPos(min_x, min_y, min_z),
                BlockPos(max_x, max_y, max_z),
            )
        x0 = (self.origin_cx - self.radius_chunks) * 16
        z0 = (self.origin_cz - self.radius_chunks) * 16
        x1 = (self.origin_cx + self.radius_chunks + 1) * 16 - 1
        z1 = (self.origin_cz + self.radius_chunks + 1) * 16 - 1
        y0 = self.y_min if self.y_min is not None else 0
        y1 = self.y_max if self.y_max is not None else y0
        return count, BlockPos(x0, y0, z0), BlockPos(x1, y1, z1)

    def modification_block_reason(self, pos: BlockPos) -> str | None:
        """Return why ``pos`` is outside the region's safe write envelope."""
        if pos in self.halo_positions:
            return "halo_position"
        x_min = (self.origin_cx - self.radius_chunks) * 16
        z_min = (self.origin_cz - self.radius_chunks) * 16
        x_max = (self.origin_cx + self.radius_chunks + 1) * 16 - 1
        z_max = (self.origin_cz + self.radius_chunks + 1) * 16 - 1
        if not (x_min <= pos.x <= x_max and z_min <= pos.z <= z_max):
            return "outside_region"
        if self.y_min is not None and pos.y < self.y_min:
            return "outside_y_window"
        if self.y_max is not None and pos.y > self.y_max:
            return "outside_y_window"
        if (pos.x >> 4, pos.z >> 4) not in self.loaded_chunks:
            return "chunk_not_loaded"
        return None

    def is_modifiable(self, pos: BlockPos) -> bool:
        """Whether ``pos`` was fully read and is inside the requested write area."""
        return self.modification_block_reason(pos) is None

    def copy(self) -> "RegionData":
        return RegionData(
            blocks=dict(self.blocks),
            origin_cx=self.origin_cx,
            origin_cz=self.origin_cz,
            radius_chunks=self.radius_chunks,
            y_min=self.y_min,
            y_max=self.y_max,
            read_y_min=self.read_y_min,
            read_y_max=self.read_y_max,
            chunks_read=self.chunks_read,
            chunks_missing=self.chunks_missing,
            context_chunks_read=self.context_chunks_read,
            context_chunks_missing=self.context_chunks_missing,
            loaded_chunks=set(self.loaded_chunks),
            halo_positions=set(self.halo_positions),
            block_entity_positions=set(self.block_entity_positions),
            surface_classes=dict(self.surface_classes),
            space_classes=dict(self.space_classes),
            write_contexts=dict(self.write_contexts),
            destructive_positions=set(self.destructive_positions),
            atomic_groups=[set(group) for group in self.atomic_groups],
        )

    def bounding_box(self) -> tuple[BlockPos, BlockPos]:
        _count, min_pos, max_pos = self._target_metrics()
        return min_pos, max_pos

    def to_summary(self) -> dict:
        block_count, min_pos, max_pos = self._target_metrics()
        core_loaded_chunks = sum(
            1
            for chunk_x, chunk_z in self.loaded_chunks
            if (
                self.origin_cx - self.radius_chunks
                <= chunk_x
                <= self.origin_cx + self.radius_chunks
                and self.origin_cz - self.radius_chunks
                <= chunk_z
                <= self.origin_cz + self.radius_chunks
            )
        )
        core_block_entities = sum(
            1 for pos in self.block_entity_positions if self.is_target_position(pos)
        )
        return {
            "block_count": block_count,
            "context_block_count": len(self.blocks) - block_count,
            "chunks_read": self.chunks_read,
            "chunks_missing": self.chunks_missing,
            "context_chunks_read": self.context_chunks_read,
            "context_chunks_missing": self.context_chunks_missing,
            "loaded_chunk_count": core_loaded_chunks,
            "context_loaded_chunk_count": len(self.loaded_chunks) - core_loaded_chunks,
            "block_entity_count": core_block_entities,
            "context_block_entity_count": len(self.block_entity_positions)
            - core_block_entities,
            "halo_block_count": len(self.halo_positions),
            "bounds": {"min": min_pos.to_dict(), "max": max_pos.to_dict()},
            "y_window": {"min": self.y_min, "max": self.y_max},
            "read_y_window": {"min": self.read_y_min, "max": self.read_y_max},
            "cx": self.origin_cx,
            "cz": self.origin_cz,
            "radius_chunks": self.radius_chunks,
        }
