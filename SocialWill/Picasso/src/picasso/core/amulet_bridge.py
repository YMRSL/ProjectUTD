from __future__ import annotations

import logging
from pathlib import Path
from typing import Any

from picasso.config import config
from picasso.models.block import AIR, BlockPos, BlockState
from picasso.models.region import RegionData

logger = logging.getLogger(__name__)


class AmuletBridge:
    """Small compatibility wrapper around Amulet-Core.

    All Amulet imports intentionally live in this file. Amulet has changed APIs
    across releases, so this bridge keeps those differences away from tools.
    """

    def __init__(self, world_path: str | Path) -> None:
        self.world_path = Path(world_path)
        if not self.world_path.exists():
            raise FileNotFoundError(f"Path does not exist: {self.world_path}")
        try:
            import amulet
        except Exception as exc:  # pragma: no cover - depends on external install
            raise RuntimeError("Amulet-Core is not importable. Run `pip install -e .`.") from exc

        self._amulet = amulet
        self.level = amulet.load_level(str(self.world_path))
        self.dimension = config.dimension
        self._version_identifier = self._detect_version_identifier()

    @property
    def level_name(self) -> str:
        return getattr(self.level, "level_name", None) or self.world_path.name

    @property
    def version(self) -> str:
        version = getattr(self.level, "max_game_version", None) or getattr(
            self.level, "game_version", None
        )
        return str(version or "unknown")

    def read_region(self, cx: int, cz: int, radius: int) -> RegionData:
        blocks: dict[BlockPos, BlockState] = {}
        y_min, y_max = self._get_y_bounds()

        for chunk_x in range(cx - radius, cx + radius + 1):
            for chunk_z in range(cz - radius, cz + radius + 1):
                try:
                    chunk = self.level.get_chunk(chunk_x, chunk_z, self.dimension)
                except Exception as exc:
                    logger.debug("Skipping unreadable chunk %s,%s: %s", chunk_x, chunk_z, exc)
                    continue
                for lx in range(16):
                    wx = chunk_x * 16 + lx
                    for lz in range(16):
                        wz = chunk_z * 16 + lz
                        for y in range(y_min, y_max + 1):
                            state = self._get_block_state(chunk, wx, y, wz, lx, lz)
                            if state is not None and not state.is_air:
                                blocks[BlockPos(wx, y, wz)] = state

        return RegionData(blocks=blocks, origin_cx=cx, origin_cz=cz, radius_chunks=radius)

    def write_region(self, changes: RegionData) -> int:
        changed = 0
        for pos, state in changes.blocks.items():
            self.place_block(pos.x, pos.y, pos.z, state)
            changed += 1
        self._save()
        return changed

    def place_block(self, x: int, y: int, z: int, block_state: BlockState) -> None:
        block = self._state_to_amulet_block(block_state)

        if hasattr(self.level, "set_version_block"):
            attempts = [
                lambda: self.level.set_version_block(
                    x, y, z, self.dimension, self._version_identifier, block, None
                ),
                lambda: self.level.set_version_block(
                    x, y, z, self.dimension, self._version_identifier, block
                ),
            ]
            for attempt in attempts:
                try:
                    attempt()
                    return
                except TypeError:
                    continue
                except Exception as exc:
                    logger.debug("set_version_block failed, trying chunk write: %s", exc)
                    break

        cx, cz = x >> 4, z >> 4
        lx, lz = x & 15, z & 15
        chunk = self.level.get_chunk(cx, cz, self.dimension)
        if hasattr(chunk, "set_block"):
            chunk.set_block(lx, y, lz, block)
        elif hasattr(chunk, "blocks") and hasattr(chunk, "block_palette"):
            palette_index = chunk.block_palette.get_add_block(block)
            chunk.blocks[lx, y, lz] = palette_index
        else:
            raise RuntimeError("Unsupported Amulet chunk API: cannot place block")
        self._mark_chunk_changed(chunk)

    def close(self) -> None:
        close = getattr(self.level, "close", None)
        if callable(close):
            close()

    def _get_y_bounds(self) -> tuple[int, int]:
        for attr in ("bounds", "bounds_for_dimension"):
            bounds_fn = getattr(self.level, attr, None)
            if callable(bounds_fn):
                try:
                    bounds = bounds_fn(self.dimension)
                    min_y = getattr(bounds, "min_y", None)
                    max_y = getattr(bounds, "max_y", None)
                    if min_y is not None and max_y is not None:
                        return int(min_y), int(max_y) - 1
                except Exception:
                    pass
        return -64, 319

    def _get_block_state(
        self, chunk: Any, wx: int, y: int, wz: int, lx: int, lz: int
    ) -> BlockState | None:
        if hasattr(chunk, "get_block"):
            try:
                return self._block_to_state(chunk.get_block(lx, y, lz))
            except Exception:
                pass

        if hasattr(self.level, "get_version_block"):
            attempts = [
                lambda: self.level.get_version_block(
                    wx, y, wz, self.dimension, self._version_identifier
                ),
                lambda: self.level.get_version_block(wx, y, wz, self.dimension),
            ]
            for attempt in attempts:
                try:
                    result = attempt()
                    block = result[0] if isinstance(result, tuple) else result
                    return self._block_to_state(block)
                except TypeError:
                    continue
                except Exception:
                    return None

        if hasattr(chunk, "blocks") and hasattr(chunk, "block_palette"):
            try:
                palette_index = chunk.blocks[lx, y, lz]
                return self._block_to_state(chunk.block_palette[palette_index])
            except Exception:
                return None

        return None

    def _block_to_state(self, block: Any) -> BlockState:
        if block is None:
            return AIR

        namespace = getattr(block, "namespace", None)
        name = (
            getattr(block, "base_name", None)
            or getattr(block, "name", None)
            or getattr(block, "block_name", None)
        )
        if namespace is None or name is None:
            namespaced = getattr(block, "namespaced_name", None) or str(block)
            if ":" in namespaced:
                namespace, name = namespaced.split(":", 1)
            else:
                namespace, name = "minecraft", namespaced

        properties = getattr(block, "properties", None) or {}
        clean_properties: dict[str, str] = {}
        for key, value in dict(properties).items():
            clean_properties[str(key)] = str(value).strip('"')
        return BlockState(str(namespace), str(name), clean_properties)

    def _state_to_amulet_block(self, state: BlockState) -> Any:
        block_cls = None
        import_errors: list[Exception] = []
        for module_name in ("amulet.api.block", "amulet.block", "amulet_nbt"):
            try:
                module = __import__(module_name, fromlist=["Block"])
                block_cls = getattr(module, "Block", None)
                if block_cls is not None:
                    break
            except Exception as exc:
                import_errors.append(exc)
        if block_cls is None:
            raise RuntimeError(f"Could not import Amulet Block class: {import_errors!r}")

        attempts = [
            lambda: block_cls(state.namespace, state.name, state.properties),
            lambda: block_cls(state.namespace, state.name),
        ]
        for attempt in attempts:
            try:
                return attempt()
            except TypeError:
                continue
        return block_cls(state.namespace, state.name, state.properties)

    def _detect_version_identifier(self) -> Any:
        platform = getattr(self.level, "platform", None) or "java"
        version = (
            getattr(self.level, "max_game_version", None)
            or getattr(self.level, "game_version", None)
            or (1, 21, 1)
        )
        if isinstance(version, str):
            version_tuple = tuple(int(part) for part in version.split(".") if part.isdigit())
            version = version_tuple or version
        return (platform, version)

    def _mark_chunk_changed(self, chunk: Any) -> None:
        for attr in ("changed", "modified", "dirty"):
            if hasattr(chunk, attr):
                try:
                    setattr(chunk, attr, True)
                    return
                except Exception:
                    pass

    def _save(self) -> None:
        save = getattr(self.level, "save", None)
        if callable(save):
            save()
