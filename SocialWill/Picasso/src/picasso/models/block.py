from __future__ import annotations

from dataclasses import dataclass, field


@dataclass(frozen=True, order=True)
class BlockPos:
    x: int
    y: int
    z: int

    def offset(self, dx: int = 0, dy: int = 0, dz: int = 0) -> "BlockPos":
        return BlockPos(self.x + dx, self.y + dy, self.z + dz)

    def to_dict(self) -> dict[str, int]:
        return {"x": self.x, "y": self.y, "z": self.z}


@dataclass
class BlockState:
    namespace: str
    name: str
    properties: dict[str, str] = field(default_factory=dict)

    @property
    def full_id(self) -> str:
        return f"{self.namespace}:{self.name}"

    @property
    def is_air(self) -> bool:
        return self.full_id in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

    @classmethod
    def from_id(cls, block_id: str, properties: dict[str, str] | None = None) -> "BlockState":
        if ":" not in block_id:
            raise ValueError(f"Block id must be namespaced: {block_id}")
        namespace, name = block_id.split(":", 1)
        return cls(namespace=namespace, name=name, properties=dict(properties or {}))

    def to_dict(self) -> dict:
        return {
            "namespace": self.namespace,
            "name": self.name,
            "full_id": self.full_id,
            "properties": self.properties,
        }


AIR = BlockState("minecraft", "air")
