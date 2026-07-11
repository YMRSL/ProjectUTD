from __future__ import annotations

from pathlib import Path

from picasso.core.catalog_index import CatalogIndex


PROJECT_ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = (
    PROJECT_ROOT
    / "src"
    / "picasso"
    / "data"
    / "catalog"
    / "doomsday_decoration_semantic.json"
)


def test_shipped_catalog_supports_stable_english_query_keys() -> None:
    catalog = CatalogIndex(CATALOG_PATH)

    english = catalog.query(category="furniture", surface=["floor"])
    chinese = catalog.query(category="家具", surface=["地面"])

    assert english
    assert {entry["id"] for entry in english} == {entry["id"] for entry in chinese}
    assert all(entry["category_key"] == "furniture" for entry in english)
    assert all("floor" in entry["surface_keys"] for entry in english)
    assert all(entry["name"] for entry in english)


def test_context_and_function_accept_both_vocabularies() -> None:
    catalog = CatalogIndex(CATALOG_PATH)

    english = catalog.query(context=["office"], function="decorative")
    chinese = catalog.query(context=["办公室"], function="装饰")

    assert english
    assert {entry["id"] for entry in english} == {entry["id"] for entry in chinese}
