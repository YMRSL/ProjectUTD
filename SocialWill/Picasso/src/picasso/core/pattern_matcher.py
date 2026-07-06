from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


@dataclass
class PatternMatch:
    pattern_name: str
    anchor_pos: BlockPos
    blocks: list[BlockPos]
    dd_replacement: str | None
    replacement_anchor_offset: tuple[int, int, int] = (0, 0, 0)
    clear_offsets: list[tuple[int, int, int]] | None = None


class PatternMatcher:
    def __init__(self, patterns_dir: str | Path) -> None:
        self.patterns_dir = Path(patterns_dir)
        self.patterns: dict[str, dict] = {}
        self.reload()

    def reload(self) -> None:
        self.patterns.clear()
        if not self.patterns_dir.exists():
            return
        for path in sorted(self.patterns_dir.glob("*.json")):
            data = json.loads(path.read_text(encoding="utf-8"))
            self.patterns[data["name"]] = data

    def find_matches(self, region: RegionData) -> list[PatternMatch]:
        matches: list[PatternMatch] = []
        occupied: set[BlockPos] = set()
        candidates = list(region.blocks.keys())

        for pattern in self.patterns.values():
            anchor = tuple(pattern.get("anchor", [0, 0, 0]))
            for candidate in candidates:
                anchor_pos = candidate.offset(dx=-anchor[0], dy=-anchor[1], dz=-anchor[2])
                positions: list[BlockPos] = []
                if not self._pattern_matches(region, pattern, anchor_pos, positions):
                    continue
                if any(pos in occupied for pos in positions):
                    continue
                occupied.update(positions)
                matches.append(
                    PatternMatch(
                        pattern_name=pattern["name"],
                        anchor_pos=anchor_pos,
                        blocks=positions,
                        dd_replacement=pattern.get("dd_replacement"),
                        replacement_anchor_offset=tuple(
                            pattern.get("replacement_anchor_offset", [0, 0, 0])
                        ),
                        clear_offsets=[
                            tuple(offset) for offset in pattern.get("clear_offsets", [])
                        ],
                    )
                )
        return matches

    def _pattern_matches(
        self, region: RegionData, pattern: dict[str, Any], anchor_pos: BlockPos, positions: list[BlockPos]
    ) -> bool:
        for block_def in pattern.get("blocks", []):
            dx, dy, dz = block_def["offset"]
            pos = anchor_pos.offset(dx=dx, dy=dy, dz=dz)
            state = region.get(pos)
            if not _match_block(state, block_def.get("match", {})):
                return False
            positions.append(pos)
        return True


def _match_block(state: BlockState | None, condition: dict[str, Any]) -> bool:
    if condition.get("air"):
        return state is None or state.is_air
    if state is None or state.is_air:
        return False
    if condition.get("any"):
        return True
    namespace = condition.get("namespace")
    if namespace and state.namespace != namespace:
        return False
    name = condition.get("name")
    if name and state.name != name:
        return False
    contains = condition.get("name_contains")
    if contains and contains.lower() not in state.name.lower():
        return False
    return True
