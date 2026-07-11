from __future__ import annotations

import tempfile
from pathlib import Path

from picasso.core.catalog_index import CatalogIndex
from picasso.session import session
from picasso.tools import catalog, style


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def test_preview_pass_reports_negative_radius_as_invalid_coordinates() -> None:
    mcp = FakeMCP()
    style.register(mcp)

    result = mcp.tools["preview_pass"](
        pass_name="does_not_matter",
        cx=0,
        cz=0,
        radius_chunks=-1,
    )

    assert result["ok"] is False
    assert result["error"] == "invalid_coordinates"


def test_empty_catalog_reports_catalog_not_loaded() -> None:
    previous = session.catalog
    try:
        with tempfile.TemporaryDirectory() as tmp:
            session.catalog = CatalogIndex(Path(tmp) / "missing.json")
            mcp = FakeMCP()
            catalog.register(mcp)

            result = mcp.tools["query_catalog"](category="furniture")

        assert result["ok"] is False
        assert result["error"] == "catalog_not_loaded"
    finally:
        session.catalog = previous


def test_style_choke_uses_loaded_catalog_ids() -> None:
    class Catalog:
        by_id = {"example_mod:known": {"id": "example_mod:known"}}

    previous = session.catalog
    try:
        session.catalog = Catalog()
        assert style._known_catalog_block_ids() == {"example_mod:known"}
    finally:
        session.catalog = previous
