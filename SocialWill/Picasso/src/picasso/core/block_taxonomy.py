from __future__ import annotations

import json
import logging
from pathlib import Path

from picasso.models.block import BlockState

logger = logging.getLogger(__name__)


class BlockTaxonomy:
    def __init__(self, taxonomy_path: str | Path | None = None) -> None:
        self.air_like: set[str] = set()
        self.liquid: set[str] = set()
        if taxonomy_path:
            self.load(taxonomy_path)

    def load(self, taxonomy_path: str | Path) -> None:
        path = Path(taxonomy_path)
        if not path.exists():
            logger.warning("Block taxonomy file missing: %s", path)
            return
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            logger.exception("Invalid block taxonomy file: %s", path)
            return
        self.air_like = set(data.get("air_like", []))
        self.liquid = set(data.get("liquid", []))

    def category(self, state: BlockState | None) -> str:
        if state is None or state.is_air:
            return "air"
        if state.full_id in self.air_like:
            return "air_like"
        if state.full_id in self.liquid:
            return "liquid"
        return "solid"

    def is_air_for_classification(self, state: BlockState | None) -> bool:
        return self.category(state) in {"air", "air_like"}

    def is_solid_for_classification(self, state: BlockState | None) -> bool:
        return self.category(state) == "solid"
