from __future__ import annotations

import json
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = PROJECT_ROOT / "src" / "picasso" / "data"


def _load_directory(name: str) -> dict[str, dict]:
    documents: dict[str, dict] = {}
    for path in sorted((DATA_ROOT / name).glob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        assert document["name"] == path.stem, f"{path}: name must match filename"
        documents[document["name"]] = document
    return documents


def _catalog_ids() -> set[str]:
    path = DATA_ROOT / "catalog" / "doomsday_decoration_semantic.json"
    document = json.loads(path.read_text(encoding="utf-8"))
    blocks = document["blocks"]
    entries = blocks.values() if isinstance(blocks, dict) else blocks
    return {entry["id"] for entry in entries}


def _assert_known_block(block_id: str, catalog_ids: set[str], source: str) -> None:
    assert ":" in block_id, f"{source}: block id must be namespaced: {block_id}"
    if not block_id.startswith("minecraft:"):
        assert block_id in catalog_ids, f"{source}: unknown catalog block id: {block_id}"


def test_shipped_content_references_are_resolvable() -> None:
    catalog_ids = _catalog_ids()
    fragments = _load_directory("fragments")
    patterns = _load_directory("patterns")
    passes = _load_directory("passes")
    bundles = _load_directory("bundles")

    for fragment_name, fragment in fragments.items():
        if fragment.get("deprecated"):
            continue
        assert fragment.get("blocks"), f"fragment {fragment_name}: blocks must not be empty"
        assert fragment.get("anchor_surface") in {
            "floor",
            "outer_wall",
            "inner_wall",
            "ceiling",
            "rooftop",
            "any",
        }
        for block in fragment["blocks"]:
            probability = float(block.get("probability", 1.0))
            assert 0.0 <= probability <= 1.0, (
                f"fragment {fragment_name}: probability out of range: {probability}"
            )
            _assert_known_block(block["block"], catalog_ids, f"fragment {fragment_name}")

    for pass_name, pass_definition in passes.items():
        pass_type = pass_definition.get("type", "block_pass")
        assert pass_type in {"block_pass", "fragment_pass", "pattern_replace"}
        for fragment_name in pass_definition.get("fragments", []):
            assert fragment_name in fragments, (
                f"pass {pass_name}: unknown fragment reference: {fragment_name}"
            )
        for mapping in pass_definition.get("mappings", []):
            pattern_name = mapping["pattern"]
            assert pattern_name in patterns, (
                f"pass {pass_name}: unknown pattern reference: {pattern_name}"
            )
            _assert_known_block(mapping["dd_block"], catalog_ids, f"pass {pass_name}")
        for rule in pass_definition.get("rules", []):
            if rule.get("place_block"):
                _assert_known_block(rule["place_block"], catalog_ids, f"pass {pass_name}")
            for option in rule.get("replace_with", []):
                _assert_known_block(option["block"], catalog_ids, f"pass {pass_name}")

    for pattern_name, pattern in patterns.items():
        replacement = pattern.get("dd_replacement")
        if replacement:
            _assert_known_block(replacement, catalog_ids, f"pattern {pattern_name}")

    for bundle_name, bundle in bundles.items():
        for entry in bundle.get("entries", []):
            for pass_entry in entry.get("passes", []):
                pass_name = pass_entry["name"]
                assert pass_name in passes, (
                    f"bundle {bundle_name}: unknown pass reference: {pass_name}"
                )
                intensity = float(pass_entry.get("intensity", 1.0))
                assert 0.0 <= intensity <= 1.0, (
                    f"bundle {bundle_name}/{pass_name}: intensity out of range: {intensity}"
                )
