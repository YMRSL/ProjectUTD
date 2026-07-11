from __future__ import annotations

import logging
import re
from pathlib import Path
from typing import Any

import numpy as np

from picasso.config import config
from picasso.models.block import BlockPos, BlockState
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
        try:
            self.dimension = config.dimension
            self._y_bounds = self._get_y_bounds()
            self._version_identifier = self._detect_version_identifier()
            self._write_poisoned = False
        except Exception:
            close = getattr(self.level, "close", None)
            if callable(close):
                try:
                    close()
                except Exception:
                    logger.exception(
                        "Failed to close Amulet level after bridge initialization error"
                    )
            raise

    @property
    def level_name(self) -> str:
        wrapper = getattr(self.level, "level_wrapper", None)
        return (
            getattr(self.level, "level_name", None)
            or getattr(wrapper, "level_name", None)
            or self.world_path.name
        )

    @property
    def version(self) -> str:
        wrapper = getattr(self.level, "level_wrapper", None)
        game_version_string = getattr(wrapper, "game_version_string", None)
        if game_version_string and "unknown" not in str(game_version_string).lower():
            return str(game_version_string)
        platform, version = self._version_identifier
        if isinstance(version, (tuple, list)):
            version_text = ".".join(str(part) for part in version)
        else:
            version_text = str(version)
        return f"{platform}:{version_text}"

    def read_region(
        self,
        cx: int,
        cz: int,
        radius: int,
        y_min: int | None = None,
        y_max: int | None = None,
    ) -> RegionData:
        if radius < 0:
            raise ValueError("invalid_coordinates")
        # The public cap is the requested/core radius. The mandatory halo is
        # accounted for in actual chunk reads without shrinking user quota.
        if radius > config.max_radius_chunks:
            raise OverflowError("region_too_large")
        blocks: dict[BlockPos, BlockState] = {}
        loaded_chunks: set[tuple[int, int]] = set()
        halo_positions: set[BlockPos] = set()
        block_entity_positions: set[BlockPos] = set()
        target_y_min, target_y_max = self.resolve_y_window(y_min, y_max)
        world_y_min, world_y_max = self._y_bounds
        read_y_min = max(world_y_min, target_y_min - 1)
        read_y_max = min(world_y_max, target_y_max + 4)
        chunks_read = 0
        chunks_missing = 0
        context_chunks_read = 0
        context_chunks_missing = 0

        read_radius = radius + 1
        for chunk_x in range(cx - read_radius, cx + read_radius + 1):
            for chunk_z in range(cz - read_radius, cz + read_radius + 1):
                is_context = abs(chunk_x - cx) > radius or abs(chunk_z - cz) > radius
                try:
                    chunk = self.level.get_chunk(chunk_x, chunk_z, self.dimension)
                except Exception as exc:
                    if self._is_missing_chunk_error(exc):
                        logger.debug("Missing chunk %s,%s: %s", chunk_x, chunk_z, exc)
                        if is_context:
                            context_chunks_missing += 1
                        else:
                            chunks_missing += 1
                        continue
                    raise RuntimeError(
                        f"Could not load chunk {chunk_x},{chunk_z} in {self.dimension}"
                    ) from exc
                if is_context:
                    context_chunks_read += 1
                else:
                    chunks_read += 1
                loaded_chunks.add((chunk_x, chunk_z))
                chunk_blocks, chunk_block_entities = self._read_loaded_chunk(
                    chunk,
                    chunk_x,
                    chunk_z,
                    read_y_min,
                    read_y_max,
                )
                block_entity_positions.update(chunk_block_entities)
                for pos, state in chunk_blocks.items():
                    blocks[pos] = state
                    if is_context or not (target_y_min <= pos.y <= target_y_max):
                        halo_positions.add(pos)

        return self._new_region_data(
            blocks=blocks,
            origin_cx=cx,
            origin_cz=cz,
            radius_chunks=radius,
            y_min=target_y_min,
            y_max=target_y_max,
            read_y_min=read_y_min,
            read_y_max=read_y_max,
            chunks_read=chunks_read,
            chunks_missing=chunks_missing,
            context_chunks_read=context_chunks_read,
            context_chunks_missing=context_chunks_missing,
            loaded_chunks=loaded_chunks,
            halo_positions=halo_positions,
            block_entity_positions=block_entity_positions,
        )

    def read_block_with_entity(self, x: int, y: int, z: int) -> tuple[BlockState, bool]:
        """Read one block in the world's native Java version.

        Amulet stores chunks in its universal format internally. Always going
        through ``get_version_block`` prevents universal names such as
        ``minecraft:planks`` from leaking into Picasso, where callers expect
        canonical Java ids such as ``minecraft:oak_planks``.
        """

        block, block_entity = self._get_version_block_raw(x, y, z)
        return self._block_to_state(block), block_entity is not None

    def resolve_y_window(
        self, y_min: int | None = None, y_max: int | None = None
    ) -> tuple[int, int]:
        world_y_min, world_y_max = self._y_bounds
        scan_y_min = world_y_min if y_min is None else max(world_y_min, int(y_min))
        scan_y_max = world_y_max if y_max is None else min(world_y_max, int(y_max))
        if scan_y_min > scan_y_max:
            raise ValueError("invalid_y_window")
        return scan_y_min, scan_y_max

    def _read_loaded_chunk(
        self,
        chunk: Any,
        chunk_x: int,
        chunk_z: int,
        read_y_min: int,
        read_y_max: int,
    ) -> tuple[dict[BlockPos, BlockState], set[BlockPos]]:
        sparse = self._read_loaded_chunk_sparse(
            chunk,
            chunk_x,
            chunk_z,
            read_y_min,
            read_y_max,
        )
        if sparse is not None:
            return sparse
        return self._read_loaded_chunk_canonical(
            chunk_x,
            chunk_z,
            read_y_min,
            read_y_max,
        )

    def _read_loaded_chunk_canonical(
        self,
        chunk_x: int,
        chunk_z: int,
        read_y_min: int,
        read_y_max: int,
    ) -> tuple[dict[BlockPos, BlockState], set[BlockPos]]:
        blocks: dict[BlockPos, BlockState] = {}
        block_entities: set[BlockPos] = set()
        for lx in range(16):
            wx = chunk_x * 16 + lx
            for lz in range(16):
                wz = chunk_z * 16 + lz
                for y in range(read_y_min, read_y_max + 1):
                    state, has_block_entity = self.read_block_with_entity(wx, y, wz)
                    pos = BlockPos(wx, y, wz)
                    if has_block_entity:
                        block_entities.add(pos)
                    if not state.is_air:
                        blocks[pos] = state
        return blocks, block_entities

    def _read_loaded_chunk_sparse(
        self,
        chunk: Any,
        chunk_x: int,
        chunk_z: int,
        read_y_min: int,
        read_y_max: int,
    ) -> tuple[dict[BlockPos, BlockState], set[BlockPos]] | None:
        """Read populated palette cells without scanning every air position.

        This is intentionally a conservative capability probe for the pinned
        Amulet 1.9.41 chunk API. Any unfamiliar shape, translator requirement,
        or block-entity ambiguity falls back to the canonical per-position
        getter rather than guessing at native block state.
        """

        try:
            storage = chunk.blocks
            palette = chunk.block_palette
            sub_chunks = tuple(int(section_y) for section_y in storage.sub_chunks)
            get_sub_chunk = storage.get_sub_chunk
            default_palette_id = int(storage.default_value)
            translator = self._get_native_block_translator()
            if not callable(get_sub_chunk) or translator is None:
                return None

            translation_cache: dict[int, tuple[BlockState | None, bool]] = {}

            def translated(palette_id: int) -> tuple[BlockState | None, bool]:
                cached = translation_cache.get(palette_id)
                if cached is not None:
                    return cached
                if palette_id < 0 or palette_id >= len(palette):
                    raise IndexError(f"Palette id out of range: {palette_id}")
                result = self._translate_universal_palette_block(
                    palette[palette_id], translator
                )
                translation_cache[palette_id] = result
                return result

            default_state, default_needs_fallback = translated(default_palette_id)
            if default_needs_fallback or default_state is None or not default_state.is_air:
                return None

            entity_positions = self._chunk_block_entity_positions(
                chunk,
                chunk_x,
                chunk_z,
                read_y_min,
                read_y_max,
            )
            if entity_positions is None:
                return None

            blocks: dict[BlockPos, BlockState] = {}
            block_entities = set(entity_positions)
            # Block entities can influence native translation and therefore
            # always use the canonical getter, even if their palette is simple.
            for pos in sorted(entity_positions):
                state, _has_block_entity = self.read_block_with_entity(pos.x, pos.y, pos.z)
                if not state.is_air:
                    blocks[pos] = state

            min_section = read_y_min // 16
            max_section = read_y_max // 16
            for section_y in sub_chunks:
                if section_y < min_section or section_y > max_section:
                    continue
                section = np.asarray(get_sub_chunk(section_y))
                if section.shape != (16, 16, 16) or not np.issubdtype(
                    section.dtype, np.integer
                ):
                    return None
                section_world_y = section_y * 16
                local_y_min = max(0, read_y_min - section_world_y)
                local_y_max = min(15, read_y_max - section_world_y)
                view = section[:, local_y_min : local_y_max + 1, :]
                for raw_palette_id in np.unique(view):
                    palette_id = int(raw_palette_id)
                    state, needs_fallback = translated(palette_id)
                    if state is not None and state.is_air and not needs_fallback:
                        continue
                    for local_x, sliced_y, local_z in np.argwhere(view == raw_palette_id):
                        pos = BlockPos(
                            chunk_x * 16 + int(local_x),
                            section_world_y + local_y_min + int(sliced_y),
                            chunk_z * 16 + int(local_z),
                        )
                        if pos in entity_positions:
                            continue
                        if needs_fallback or state is None:
                            canonical, has_block_entity = self.read_block_with_entity(
                                pos.x, pos.y, pos.z
                            )
                            if has_block_entity:
                                block_entities.add(pos)
                            if not canonical.is_air:
                                blocks[pos] = canonical
                        else:
                            blocks[pos] = state
            return blocks, block_entities
        except Exception as exc:
            logger.debug(
                "Sparse read unavailable for chunk %s,%s; using canonical fallback: %s",
                chunk_x,
                chunk_z,
                exc,
            )
            return None

    def _get_native_block_translator(self) -> Any | None:
        cached = getattr(self, "_native_block_translator", None)
        if cached is not None:
            return cached
        manager = getattr(self.level, "translation_manager", None)
        get_version = getattr(manager, "get_version", None)
        if not callable(get_version):
            return None
        platform, version = self._version_identifier
        translator = getattr(get_version(platform, version), "block", None)
        if not callable(getattr(translator, "from_universal", None)):
            return None
        self._native_block_translator = translator
        return translator

    def _translate_universal_palette_block(
        self, universal_block: Any, translator: Any
    ) -> tuple[BlockState | None, bool]:
        components = getattr(universal_block, "block_tuple", None)
        if not components:
            return None, True
        output = None
        for component in components:
            converted, extra_output, extra_needed = translator.from_universal(component)
            if extra_needed or extra_output is not None:
                return None, True
            output = converted if output is None else output + converted
        try:
            return self._block_to_state(output), False
        except Exception:
            return None, True

    @staticmethod
    def _chunk_block_entity_positions(
        chunk: Any,
        chunk_x: int,
        chunk_z: int,
        read_y_min: int,
        read_y_max: int,
    ) -> set[BlockPos] | None:
        container = getattr(chunk, "block_entities", None)
        values = getattr(container, "values", None)
        if not callable(values):
            return None
        positions: set[BlockPos] = set()
        for block_entity in values():
            try:
                pos = BlockPos(
                    int(block_entity.x),
                    int(block_entity.y),
                    int(block_entity.z),
                )
            except Exception:
                return None
            if (pos.x >> 4, pos.z >> 4) != (chunk_x, chunk_z):
                return None
            if read_y_min <= pos.y <= read_y_max:
                positions.add(pos)
        return positions

    def write_region(self, changes: RegionData) -> int:
        if getattr(self, "_write_poisoned", False):
            raise RuntimeError(
                "Amulet bridge write state is poisoned after an incomplete rollback. "
                "Close and reopen the world before writing again."
            )
        if not changes.blocks:
            return 0

        # Conversion must be side-effect free and complete before the first
        # chunk mutation. A malformed property on the last block must not
        # leave the earlier blocks dirty in Amulet's cache.
        prepared = [
            (pos, self._state_to_amulet_block(state))
            for pos, state in sorted(changes.blocks.items())
        ]
        originals = [
            (pos, *self._get_version_block_raw(pos.x, pos.y, pos.z))
            for pos, _block in prepared
        ]

        attempted: list[tuple[BlockPos, Any, Any]] = []
        try:
            for (pos, block), original in zip(prepared, originals):
                # Include the current position before the call. If an Amulet
                # implementation mutates and then raises, rollback still
                # restores that position.
                attempted.append(original)
                self._set_version_block(pos.x, pos.y, pos.z, block, None)
            self._save()
        except Exception as exc:
            rollback_errors = self._rollback_blocks(attempted)
            failure = f"{type(exc).__name__}: {exc}"
            message = (
                f"Amulet write failed ({failure}); rollback completed and original state "
                "was saved."
            )
            if rollback_errors:
                self._write_poisoned = True
                message = (
                    f"Amulet write failed ({failure}); rollback was incomplete and the bridge "
                    "is poisoned. "
                    "Close and reopen the world before writing again. Details: "
                    + "; ".join(rollback_errors)
                )
            raise RuntimeError(message) from exc
        return len(prepared)

    def place_block(self, x: int, y: int, z: int, block_state: BlockState) -> None:
        changes = RegionData()
        changes.set(BlockPos(x, y, z), block_state)
        self.write_region(changes)

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

    def _get_version_block_raw(self, x: int, y: int, z: int) -> tuple[Any, Any]:
        getter = getattr(self.level, "get_version_block", None)
        if not callable(getter):
            raise RuntimeError(
                "Installed Amulet-Core does not expose get_version_block; "
                "canonical Java block reads are unavailable."
            )
        result = getter(x, y, z, self.dimension, self._version_identifier)
        if isinstance(result, tuple):
            if not result:
                raise RuntimeError(f"Amulet returned an empty block result at ({x}, {y}, {z}).")
            block = result[0]
            block_entity = result[1] if len(result) > 1 else None
        else:
            block = result
            block_entity = None
        if block is None:
            raise RuntimeError(f"Amulet returned no block at ({x}, {y}, {z}).")
        return block, block_entity

    def _block_to_state(self, block: Any) -> BlockState:
        if block is None:
            raise ValueError("Cannot convert a missing Amulet block to BlockState.")
        try:
            from amulet.api.entity import Entity

            if isinstance(block, Entity):
                raise TypeError(
                    "Amulet returned an Entity where a block was required; "
                    "refusing to mislabel it as a BlockState."
                )
        except ImportError:
            pass

        namespace = getattr(block, "namespace", None)
        name = (
            getattr(block, "base_name", None)
            or getattr(block, "name", None)
            or getattr(block, "block_name", None)
        )
        if namespace is None or name is None:
            namespaced = getattr(block, "namespaced_name", None)
            if isinstance(namespaced, str) and ":" in namespaced:
                namespace, name = namespaced.split(":", 1)
            else:
                raise TypeError(f"Unsupported Amulet block object: {block!r}")

        if str(namespace) == "universal_minecraft":
            raise ValueError(
                "Amulet returned a universal block. Use get_version_block with "
                "the target Java version before converting it to BlockState."
            )

        properties = getattr(block, "properties", None) or {}
        clean_properties: dict[str, str] = {}
        for key, value in dict(properties).items():
            clean_properties[str(key)] = self._property_text(value)
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

        properties = {
            str(key): self._string_tag(self._property_text(value))
            for key, value in state.properties.items()
        }
        return block_cls(state.namespace, state.name, properties)

    def _detect_version_identifier(self) -> Any:
        wrapper = getattr(self.level, "level_wrapper", None)
        version_identifier = self._read_version_identifier(wrapper)
        if version_identifier is None:
            version_identifier = self._read_version_identifier(self.level)
        if version_identifier is None:
            raise RuntimeError(
                "Could not determine the world version from Amulet. "
                "Refusing to guess a Java version for block conversion."
            )
        platform, version = version_identifier
        if str(platform).lower() != "java":
            raise RuntimeError(
                f"Picasso currently requires a Java world; Amulet reported {platform!r}."
            )
        if isinstance(version, str):
            stripped_version = version.strip()
            if stripped_version.isdigit():
                version = int(stripped_version)
            else:
                version_tuple = tuple(
                    int(part) for part in re.findall(r"\d+", stripped_version)
                )
                version = version_tuple or stripped_version
        return (str(platform), version)

    @staticmethod
    def _read_version_identifier(owner: Any) -> tuple[Any, Any] | None:
        if owner is None:
            return None
        try:
            candidate = getattr(owner, "max_world_version", None)
            if callable(candidate):
                candidate = candidate()
            if isinstance(candidate, (tuple, list)) and len(candidate) == 2:
                return candidate[0], candidate[1]
        except Exception:
            pass
        try:
            platform = getattr(owner, "platform", None)
            version = (
                getattr(owner, "version", None)
                or getattr(owner, "max_game_version", None)
                or getattr(owner, "game_version", None)
            )
        except Exception:
            return None
        if platform is None or version is None:
            return None
        return platform, version

    def _set_version_block(
        self, x: int, y: int, z: int, block: Any, block_entity: Any = None
    ) -> None:
        setter = getattr(self.level, "set_version_block", None)
        if not callable(setter):
            raise RuntimeError(
                "Installed Amulet-Core does not expose set_version_block; "
                "safe versioned writes are unavailable."
            )
        setter(
            x,
            y,
            z,
            self.dimension,
            self._version_identifier,
            block,
            block_entity,
        )

    def _rollback_blocks(self, attempted: list[tuple[BlockPos, Any, Any]]) -> list[str]:
        if not attempted:
            return []
        errors: list[str] = []
        for pos, block, block_entity in reversed(attempted):
            try:
                self._set_version_block(pos.x, pos.y, pos.z, block, block_entity)
            except Exception as exc:
                errors.append(f"({pos.x},{pos.y},{pos.z}): {exc}")
        if not errors:
            try:
                self._save()
            except Exception as exc:
                errors.append(f"save rollback: {exc}")
        return errors

    @staticmethod
    def _property_text(value: Any) -> str:
        if isinstance(value, bool):
            return "true" if value else "false"
        for attr in ("py_str", "py_data", "value"):
            candidate = getattr(value, attr, None)
            if candidate is not None and not callable(candidate):
                return str(candidate)
        return str(value).strip('"')

    @staticmethod
    def _string_tag(value: str) -> Any:
        try:
            from amulet_nbt import StringTag

            return StringTag(value)
        except ImportError:
            try:
                from amulet_nbt import TAG_String

                return TAG_String(value)
            except ImportError as exc:
                raise RuntimeError("Could not import Amulet NBT string tag type.") from exc

    @staticmethod
    def _is_missing_chunk_error(exc: Exception) -> bool:
        try:
            from amulet.api.errors import ChunkDoesNotExist

            if isinstance(exc, ChunkDoesNotExist):
                return True
        except ImportError:
            pass
        return type(exc).__name__ in {"ChunkDoesNotExist", "ChunkNotFound"}

    @staticmethod
    def _new_region_data(
        *,
        loaded_chunks: set[tuple[int, int]],
        halo_positions: set[BlockPos],
        block_entity_positions: set[BlockPos],
        **kwargs: Any,
    ) -> RegionData:
        region = RegionData(**kwargs)
        # These fields were added after the bridge contract. Assignment keeps
        # this module compatible while RegionData and the bridge land in
        # parallel branches, and continues to work once they are dataclass
        # fields.
        region.loaded_chunks = set(loaded_chunks)
        region.halo_positions = set(halo_positions)
        region.block_entity_positions = set(block_entity_positions)
        return region

    def _save(self) -> None:
        save = getattr(self.level, "save", None)
        if not callable(save):
            raise RuntimeError("Installed Amulet-Core does not expose level.save().")
        save()
