from __future__ import annotations

import json
import logging
from pathlib import Path

logger = logging.getLogger(__name__)


# The shipped DD catalog is authored in Chinese while Picasso's MCP contract and
# agent playbooks use stable English keys.  Keep the source vocabulary intact in
# responses, but accept either representation at query time.
CATEGORY_ALIASES = {
    "vehicle": "载具",
    "furniture": "家具",
    "appliance": "家电",
    "kitchen_bath": "厨卫",
    "architecture": "建筑构件",
    "doors_windows": "门窗",
    "signage": "招牌标识",
    "storage": "容器储物",
    "lighting": "灯光照明",
    "industrial": "工业管线",
    "military_defense": "军事防御",
    "medical": "医疗",
    "vegetation": "植被自然",
    "electronics": "电子设备",
    "debris": "废墟杂物",
    "misc": "杂项",
}

SURFACE_ALIASES = {
    "floor": "地面",
    "wall": "墙面",
    "any": "任意",
    "tabletop": "桌面",
    "ceiling": "天花",
}

CONTEXT_ALIASES = {
    "ruins": "废墟",
    "street": "街道",
    "residential": "住宅",
    "mall": "商场",
    "office": "办公室",
    "factory": "工厂",
    "military_checkpoint": "军事检查站",
    "hospital": "医院",
    "school": "学校",
    "kitchen": "厨房",
    "supermarket": "超市",
    "bedroom": "卧室",
    "bank": "银行",
    "gas_station": "加油站",
    "pool": "泳池",
    "warehouse": "仓库",
    "subway": "地铁",
    "park": "公园",
    "barracks": "军营",
    "garage": "车库",
    "church": "教堂",
    "restroom": "厕所",
    "cemetery": "墓地",
}

FUNCTION_ALIASES = {
    "decorative": "装饰",
    "obstacle": "障碍",
    "container": "容器",
    "functional": "功能性",
    "light": "光源",
}


class CatalogIndex:
    def __init__(self, catalog_path: str | Path | list[str | Path]) -> None:
        if isinstance(catalog_path, (str, Path)):
            self.catalog_paths = [Path(catalog_path)]
        else:
            self.catalog_paths = [Path(path) for path in catalog_path]
        self.catalog_path = self.catalog_paths[0] if self.catalog_paths else Path()
        self.entries: list[dict] = []
        self.by_id: dict[str, dict] = {}
        self._load()

    def _load(self) -> None:
        merged: dict[str, dict] = {}
        for path in self.catalog_paths:
            if not path.exists():
                logger.warning("Catalog file missing: %s", path)
                continue
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except Exception:
                logger.exception("Invalid catalog file: %s", path)
                continue
            source = path.stem
            for entry in _extract_entries(data):
                if not isinstance(entry, dict) or not entry.get("id"):
                    continue
                normalized = dict(entry)
                normalized.setdefault("source", source)
                normalized.setdefault("name", normalized.get("name_zh"))
                _add_stable_vocab_keys(normalized)
                block_id = normalized["id"]
                if block_id in merged:
                    logger.warning("Catalog id collision for %s; %s wins", block_id, path)
                merged[block_id] = normalized
        self.entries = list(merged.values())
        self.by_id = dict(merged)

    def query(
        self,
        category: str | None = None,
        surface: list[str] | None = None,
        context: list[str] | None = None,
        tags: list[str] | None = None,
        function: str | None = None,
        source: str | None = None,
    ) -> list[dict]:
        results = self.entries
        if category:
            results = [
                entry
                for entry in results
                if _matches_vocab(entry.get("category"), [category], CATEGORY_ALIASES)
            ]
        if function:
            results = [
                entry
                for entry in results
                if _matches_vocab(entry.get("function"), [function], FUNCTION_ALIASES)
            ]
        if source:
            results = [entry for entry in results if entry.get("source") == source]
        if surface:
            results = [
                entry
                for entry in results
                if _matches_vocab(entry.get("surface"), surface, SURFACE_ALIASES)
            ]
        if context:
            results = [
                entry
                for entry in results
                if _matches_vocab(entry.get("context"), context, CONTEXT_ALIASES)
            ]
        if tags:
            requested = set(tags)
            results = [entry for entry in results if requested.intersection(set(_as_list(entry.get("tags"))))]
        return results

    def get_by_id(self, block_id: str) -> dict | None:
        return self.by_id.get(block_id)


def _extract_entries(data) -> list[dict]:
    if isinstance(data, dict):
        if "blocks" in data and isinstance(data["blocks"], dict):
            return list(data["blocks"].values())
        if "blocks" in data and isinstance(data["blocks"], list):
            return data["blocks"]
        if "entries" in data and isinstance(data["entries"], list):
            return data["entries"]
        return [value for value in data.values() if isinstance(value, dict)]
    if isinstance(data, list):
        return data
    return []


def _as_list(value) -> list:
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]


def _matches_vocab(value, requested: list[str], aliases: dict[str, str]) -> bool:
    available = {str(item).casefold() for item in _as_list(value)}
    expanded: set[str] = set()
    reverse = {source.casefold(): key for key, source in aliases.items()}
    for item in requested:
        folded = str(item).casefold()
        expanded.add(folded)
        if folded in aliases:
            expanded.add(aliases[folded].casefold())
        if folded in reverse:
            expanded.add(reverse[folded].casefold())
    return bool(available.intersection(expanded))


def _add_stable_vocab_keys(entry: dict) -> None:
    entry["category_key"] = _stable_key(entry.get("category"), CATEGORY_ALIASES)
    entry["surface_keys"] = [
        _stable_key(value, SURFACE_ALIASES) for value in _as_list(entry.get("surface"))
    ]
    entry["context_keys"] = [
        _stable_key(value, CONTEXT_ALIASES) for value in _as_list(entry.get("context"))
    ]
    entry["function_key"] = _stable_key(entry.get("function"), FUNCTION_ALIASES)


def _stable_key(value, aliases: dict[str, str]) -> str | None:
    if value is None:
        return None
    text = str(value)
    reverse = {source: key for key, source in aliases.items()}
    return reverse.get(text, text)
