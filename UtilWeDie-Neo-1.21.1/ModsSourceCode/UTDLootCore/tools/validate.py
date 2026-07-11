#!/usr/bin/env python3
"""Offline, standard-library-only validator for UTD Loot Core resources."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any, Iterable


SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_RESOURCES = SCRIPT_DIR.parent / "src" / "main" / "resources"
EXPECTED_BASELINE_SHA256 = "DED3D9882C5EA821E7FFBD602BB137AE9F0BF4784331951CA548569A1874042A"
EXPECTED_MISSING_IDS = {
    "locks:diamond_lock_pick",
    "locks:gold_lock_pick",
    "locks:iron_lock",
    "locks:iron_lock_pick",
    "locks:key_blank",
    "locks:key_ring",
    "locks:steel_lock",
    "locks:steel_lock_pick",
    "locks:wood_lock",
    "locks:wood_lock_pick",
    "paraglider:paraglider",
    "ropebridge:bridge_builder",
    "ropebridge:ladder_builder",
    "the_ravenous:anti_ravenous",
}


class ValidationError(RuntimeError):
    pass


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValidationError(message)


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8-sig"))
    except Exception as exc:  # pragma: no cover - exercised by CLI failures
        raise ValidationError(f"invalid JSON: {path}: {exc}") from exc


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def aggregate_hash(hashes: dict[str, str]) -> str:
    digest = hashlib.sha256()
    for relative, file_hash in sorted(hashes.items()):
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(file_hash.encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest().upper()


def walk_values(value: Any) -> Iterable[Any]:
    yield value
    if isinstance(value, dict):
        for key, item in value.items():
            yield from walk_values(key)
            yield from walk_values(item)
    elif isinstance(value, list):
        for item in value:
            yield from walk_values(item)


def walk_dicts(value: Any) -> Iterable[dict[str, Any]]:
    if isinstance(value, dict):
        yield value
        for item in value.values():
            yield from walk_dicts(item)
    elif isinstance(value, list):
        for item in value:
            yield from walk_dicts(item)


def helper_id(helper_root: Path, path: Path) -> str:
    relative = path.relative_to(helper_root.parent).as_posix()
    return "utd_loot_core:" + relative.removesuffix(".json")


def validate_hashes(resources: Path, manifest: dict[str, Any]) -> int:
    index_path = resources / "data" / "utd_loot_core" / "manifest.hashes.json"
    index = load_json(index_path)
    require(index.get("algorithm") == "SHA-256", "hash algorithm must be SHA-256")
    files = index.get("files")
    require(isinstance(files, dict), "hash index files must be an object")
    require(len(files) == 360, f"hash index must cover 360 generated files, got {len(files)}")
    actual: dict[str, str] = {}
    for relative, expected in files.items():
        path = resources / relative
        require(path.is_file(), f"hashed resource is missing: {relative}")
        actual_hash = sha256_file(path)
        require(actual_hash == expected, f"hash mismatch: {relative}")
        actual[relative] = actual_hash
    aggregate = aggregate_hash(actual)
    require(aggregate == index.get("aggregateSha256"), "hash-index aggregate mismatch")
    require(
        aggregate == manifest.get("generatedDataAggregateSha256"),
        "manifest aggregate mismatch",
    )
    return len(files)


def validate_resources(resources: Path = DEFAULT_RESOURCES) -> dict[str, int]:
    resources = resources.resolve()
    manifest = load_json(resources / "data" / "utd_loot_core" / "manifest.json")
    registry = load_json(resources / "data" / "utd_loot_core" / "loot" / "registry.json")
    balance = load_json(resources / "data" / "utd_loot_core" / "loot" / "balance.json")
    helper_root = resources / "data" / "utd_loot_core" / "loot_table" / "utd"
    chest_root = resources / "data" / "doomsday_decoration" / "loot_table" / "chests"
    helper_files = sorted(helper_root.rglob("*.json"))
    chest_files = sorted(chest_root.glob("*.json"))

    require(manifest.get("modId") == "utd_loot_core", "wrong manifest modId")
    require(manifest.get("minecraftVersion") == "1.21.1", "wrong Minecraft version")
    require(manifest.get("neoForgeVersion") == "21.1.233", "wrong NeoForge version")
    require(
        manifest.get("sourceBaseline", {}).get("sha256") == EXPECTED_BASELINE_SHA256,
        "wrong runtime baseline SHA-256",
    )
    require(len(registry) == 798, f"registry must contain 798 rows, got {len(registry)}")
    ids = [str(row.get("id", "")) for row in registry]
    require(len(set(ids)) == len(ids), "registry ids must be unique")
    by_id = {str(row["id"]): row for row in registry}
    require(EXPECTED_MISSING_IDS <= by_id.keys(), "one or more missing-mod ids disappeared")
    for item_id in EXPECTED_MISSING_IDS:
        row = by_id[item_id]
        require(row.get("lootEnabled") is False, f"missing-mod id is still enabled: {item_id}")
        require(row.get("disabledReason") == "missing_mod_not_in_1_21_1_pack", f"missing reason: {item_id}")
        require(float(row.get("commonBaseWeight", 0)) == 0, f"missing item has common weight: {item_id}")
        require(float(row.get("directedWeight", 0)) == 0, f"missing item has directed weight: {item_id}")
    disabled_manifest = set(manifest.get("disabledMissingModIds", []))
    require(disabled_manifest == EXPECTED_MISSING_IDS, "manifest disabled-id set differs")

    require(len(helper_files) == 78, f"expected 78 helper tables, got {len(helper_files)}")
    require(len(chest_files) == 280, f"expected 280 chest tables, got {len(chest_files)}")
    helpers = {helper_id(helper_root, path): path for path in helper_files}
    helper_payloads = {key: load_json(path) for key, path in helpers.items()}
    chest_payloads = {path.stem: load_json(path) for path in chest_files}

    for label, payload in list(helper_payloads.items()) + list(chest_payloads.items()):
        require(payload.get("type") == "minecraft:chest", f"{label}: wrong loot-table type")
        require(isinstance(payload.get("pools"), list), f"{label}: pools must be an array")

    missing_item_hits: list[str] = []
    old_namespace_hits: list[str] = []
    for label, payload in helper_payloads.items():
        for value in walk_values(payload):
            if isinstance(value, str) and "doomsday_functionality" in value:
                old_namespace_hits.append(label)
        for node in walk_dicts(payload):
            if node.get("type") == "minecraft:item" and node.get("name") in EXPECTED_MISSING_IDS:
                missing_item_hits.append(f"{label}:{node.get('name')}")
    require(not missing_item_hits, "missing-mod items remain in helper tables: " + ", ".join(missing_item_hits))
    require(not old_namespace_hits, "old namespace remains in helper tables")

    referenced: list[str] = []
    for chest_name, payload in chest_payloads.items():
        for value in walk_values(payload):
            if isinstance(value, str):
                require("doomsday_functionality" not in value, f"{chest_name}: old container namespace")
                require("kubejs:utd/" not in value, f"{chest_name}: old helper namespace")
        for node in walk_dicts(payload):
            if node.get("type") == "minecraft:loot_table":
                reference = node.get("value")
                require(isinstance(reference, str), f"{chest_name}: loot-table entry has no value")
                require(reference.startswith("utd_loot_core:utd/"), f"{chest_name}: foreign helper ref {reference}")
                referenced.append(reference)
    missing_refs = sorted(set(referenced) - helpers.keys())
    require(not missing_refs, "missing helper references: " + ", ".join(missing_refs))
    require(len(referenced) == 1316, f"expected 1316 helper references, got {len(referenced)}")
    require(len(set(referenced)) == 61, f"expected 61 referenced helper tables, got {len(set(referenced))}")

    container_config = balance.get("containerConfig", {})
    family_map = balance.get("familyByLootTable", {})
    all_tables = balance.get("allLootTables", [])
    templates = balance.get("templates", {})
    require(len(container_config) == 280, "containerConfig must contain 280 entries")
    require(len(family_map) == 280, "familyByLootTable must contain 280 entries")
    require(len(all_tables) == 280, "allLootTables must contain 280 entries")
    require(len(templates) == 13, "templates must contain 13 entries")
    expected_containers = {f"doomsday_decoration:chests/{path.stem}" for path in chest_files}
    require(set(container_config) == expected_containers, "containerConfig and chest resources differ")
    require(set(family_map) == expected_containers, "family map and chest resources differ")
    require(set(all_tables) == expected_containers, "allLootTables and chest resources differ")
    for value in walk_values(balance):
        require(
            not (isinstance(value, str) and "doomsday_functionality" in value),
            "old doomsday_functionality namespace remains in balance data",
        )

    channel_templates = balance.get("channelTemplates", {})
    enabled = [row for row in registry if row.get("lootEnabled", True)]
    guarantees = {"civilian": (4,), "medical": (4,), "military": (4, 5)}
    for channel, levels in guarantees.items():
        allowed_templates = set(channel_templates.get(channel, []))
        require(allowed_templates, f"pity channel has no templates: {channel}")
        for level in levels:
            candidates = [
                row
                for row in enabled
                if int(row.get("level", 0)) == level
                and float(row.get("directedWeight", 0)) > 0
                and allowed_templates.intersection(row.get("directedTemplates", []))
            ]
            require(candidates, f"no active level-{level} candidate for {channel}")

    counts = manifest.get("counts", {})
    require(counts.get("registrySeed") == 798, "manifest registry count mismatch")
    require(counts.get("disabledMissingMod") == 14, "manifest disabled count mismatch")
    require(counts.get("helperTables") == 78, "manifest helper count mismatch")
    require(counts.get("chestTables") == 280, "manifest chest count mismatch")
    require(counts.get("containerConfig") == 280, "manifest config count mismatch")
    require(counts.get("removedMissingItemEntries") == 4, "expected four removed paraglider entries")
    hash_files = validate_hashes(resources, manifest)

    return {
        "registry": len(registry),
        "registryEnabled": len(enabled),
        "disabledMissingMod": len(EXPECTED_MISSING_IDS),
        "helperTables": len(helper_files),
        "chestTables": len(chest_files),
        "helperReferences": len(referenced),
        "referencedHelpers": len(set(referenced)),
        "hashedFiles": hash_files,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--resource-root", type=Path, default=DEFAULT_RESOURCES)
    args = parser.parse_args()
    try:
        stats = validate_resources(args.resource_root)
    except ValidationError as exc:
        print(f"VALIDATION FAILED: {exc}")
        return 1
    print("VALIDATION OK")
    print(json.dumps(stats, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
