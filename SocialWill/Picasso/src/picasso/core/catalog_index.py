from __future__ import annotations

import json
from pathlib import Path


class CatalogIndex:
    def __init__(self, catalog_path: str | Path) -> None:
        self.catalog_path = Path(catalog_path)
        self.entries: list[dict] = []
        self.by_id: dict[str, dict] = {}
        if self.catalog_path.exists():
            self._load()

    def _load(self) -> None:
        data = json.loads(self.catalog_path.read_text(encoding="utf-8"))
        if isinstance(data, dict):
            if "blocks" in data and isinstance(data["blocks"], dict):
                entries = list(data["blocks"].values())
            elif "blocks" in data and isinstance(data["blocks"], list):
                entries = data["blocks"]
            elif "entries" in data and isinstance(data["entries"], list):
                entries = data["entries"]
            else:
                entries = list(data.values())
        elif isinstance(data, list):
            entries = data
        else:
            entries = []

        self.entries = [entry for entry in entries if isinstance(entry, dict) and entry.get("id")]
        self.by_id = {entry["id"]: entry for entry in self.entries}

    def query(
        self,
        category: str | None = None,
        surface: list[str] | None = None,
        context: list[str] | None = None,
        tags: list[str] | None = None,
        function: str | None = None,
    ) -> list[dict]:
        results = self.entries
        if category:
            results = [entry for entry in results if entry.get("category") == category]
        if function:
            results = [entry for entry in results if entry.get("function") == function]
        if surface:
            requested = set(surface)
            results = [
                entry for entry in results if requested.intersection(set(_as_list(entry.get("surface"))))
            ]
        if context:
            requested = set(context)
            results = [
                entry for entry in results if requested.intersection(set(_as_list(entry.get("context"))))
            ]
        if tags:
            requested = set(tags)
            results = [entry for entry in results if requested.intersection(set(_as_list(entry.get("tags"))))]
        return results

    def get_by_id(self, block_id: str) -> dict | None:
        return self.by_id.get(block_id)


def _as_list(value) -> list:
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]
