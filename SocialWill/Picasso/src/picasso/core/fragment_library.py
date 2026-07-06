from __future__ import annotations

import json
from pathlib import Path

from picasso.models.fragment import Fragment


class FragmentLibrary:
    def __init__(self, fragments_dir: str | Path) -> None:
        self.fragments_dir = Path(fragments_dir)
        self.fragments: dict[str, Fragment] = {}
        self.reload()

    def reload(self) -> None:
        self.fragments.clear()
        if not self.fragments_dir.exists():
            return
        for path in sorted(self.fragments_dir.glob("*.json")):
            data = json.loads(path.read_text(encoding="utf-8"))
            if data.get("deprecated"):
                continue
            fragment = Fragment.model_validate(data)
            self.fragments[fragment.name] = fragment

    def get(self, name: str) -> Fragment | None:
        return self.fragments.get(name)

    def list(self, tags_filter: list[str] | None = None) -> list[Fragment]:
        fragments = list(self.fragments.values())
        if tags_filter:
            requested = set(tags_filter)
            fragments = [
                fragment for fragment in fragments if requested.intersection(set(fragment.tags))
            ]
        return fragments
