#!/usr/bin/env python3
"""Build deterministic UTD Loot Core resources from the preserved runtime baseline.

This is deliberately a one-way importer.  It reads the current 1.21.1 runtime,
normalizes the namespace, disables known absent-mod records, and writes only below
this mod project's ``src/main/resources`` directory.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path
from typing import Any


SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
MODPACK_DIR = PROJECT_DIR.parent.parent
DEFAULT_RUNTIME = (
    MODPACK_DIR
    / ".minecraft"
    / "versions"
    / "1.21.1-NeoForge_21.1.233"
    / "kubejs"
)
DEFAULT_RESOURCES = PROJECT_DIR / "src" / "main" / "resources"

BASELINE_ARCHIVE_SHA256 = "DED3D9882C5EA821E7FFBD602BB137AE9F0BF4784331951CA548569A1874042A"
GENERATOR_VERSION = "1.0.0"
OLD_NAMESPACE = "doomsday_functionality"
NEW_NAMESPACE = "doomsday_decoration"
OLD_HELPER_PREFIX = "kubejs:utd/"
NEW_HELPER_PREFIX = "utd_loot_core:utd/"

MISSING_MOD_IDS = {
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


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def assert_within(base: Path, target: Path) -> None:
    base_resolved = base.resolve()
    target_resolved = target.resolve()
    if target_resolved != base_resolved and base_resolved not in target_resolved.parents:
        raise RuntimeError(f"refusing to write outside {base_resolved}: {target_resolved}")


def read_json_assignment(path: Path, marker: str) -> Any:
    text = path.read_text(encoding="utf-8-sig")
    position = text.find(marker)
    if position < 0:
        raise ValueError(f"marker {marker!r} not found in {path}")
    payload = text[position + len(marker) :].lstrip()
    value, _ = json.JSONDecoder().raw_decode(payload)
    return value


def replace_strings(value: Any, old: str, new: str) -> Any:
    if isinstance(value, dict):
        return {
            replace_strings(key, old, new): replace_strings(item, old, new)
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [replace_strings(item, old, new) for item in value]
    if isinstance(value, str):
        return value.replace(old, new)
    return value


def scrub_missing_entries(value: Any) -> tuple[Any, int]:
    removed = 0
    if isinstance(value, list):
        output = []
        for item in value:
            if (
                isinstance(item, dict)
                and item.get("type") == "minecraft:item"
                and item.get("name") in MISSING_MOD_IDS
            ):
                removed += 1
                continue
            cleaned, nested_removed = scrub_missing_entries(item)
            removed += nested_removed
            output.append(cleaned)
        return output, removed
    if isinstance(value, dict):
        output = {}
        for key, item in value.items():
            cleaned, nested_removed = scrub_missing_entries(item)
            output[key] = cleaned
            removed += nested_removed
        return output, removed
    return value, 0


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def collect_source_hashes(runtime: Path) -> dict[str, str]:
    relative_paths = [
        "startup_scripts/utd_loot_registry_data.js",
        "startup_scripts/utd_loot_balance_data.js",
        "startup_scripts/utd_loot_registry.js",
        "startup_scripts/utd_loot_families.js",
        "server_scripts/utd_loot_pity.js",
        "server_scripts/utd_loot_utils.js",
        "server_scripts/utd_loot_tables.js",
    ]
    return {relative: sha256_file(runtime / relative) for relative in relative_paths}


def generated_hashes(resources: Path) -> dict[str, str]:
    targets = [
        resources / "data" / "utd_loot_core" / "loot" / "registry.json",
        resources / "data" / "utd_loot_core" / "loot" / "balance.json",
    ]
    targets.extend(
        sorted(
            (resources / "data" / "utd_loot_core" / "loot_table" / "utd").rglob("*.json")
        )
    )
    targets.extend(
        sorted(
            (resources / "data" / "doomsday_decoration" / "loot_table" / "chests").glob("*.json")
        )
    )
    return {
        path.relative_to(resources).as_posix(): sha256_file(path)
        for path in targets
    }


def aggregate_hash(hashes: dict[str, str]) -> str:
    digest = hashlib.sha256()
    for relative, file_hash in sorted(hashes.items()):
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(file_hash.encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest().upper()


def import_data(runtime: Path, resources: Path) -> dict[str, Any]:
    runtime = runtime.resolve()
    resources = resources.resolve()
    assert_within(PROJECT_DIR, resources)

    registry_source = runtime / "startup_scripts" / "utd_loot_registry_data.js"
    balance_source = runtime / "startup_scripts" / "utd_loot_balance_data.js"
    source_helper = runtime / "data" / "kubejs" / "loot_table" / "utd"
    source_chests = runtime / "data" / "doomsday_decoration" / "loot_table" / "chests"

    registry = read_json_assignment(registry_source, "utd.lootRegistrySeed =")
    balance = read_json_assignment(balance_source, "utd.generatedLootBalance =")
    if not isinstance(registry, list):
        raise TypeError("registry seed must be an array")
    if not isinstance(balance, dict):
        raise TypeError("balance data must be an object")

    seen_ids: set[str] = set()
    disabled_found: set[str] = set()
    for record in registry:
        item_id = str(record.get("id", ""))
        if not item_id or item_id in seen_ids:
            raise ValueError(f"blank or duplicate registry id: {item_id!r}")
        seen_ids.add(item_id)
        if item_id in MISSING_MOD_IDS:
            record["lootEnabled"] = False
            record["disabledReason"] = "missing_mod_not_in_1_21_1_pack"
            record["commonBaseWeight"] = 0
            record["directedWeight"] = 0
            disabled_found.add(item_id)
    if disabled_found != MISSING_MOD_IDS:
        missing = sorted(MISSING_MOD_IDS - disabled_found)
        raise ValueError(f"expected missing-mod ids absent from registry source: {missing}")

    balance = replace_strings(balance, OLD_NAMESPACE, NEW_NAMESPACE)

    helper_target = resources / "data" / "utd_loot_core" / "loot_table" / "utd"
    chest_target = resources / "data" / "doomsday_decoration" / "loot_table" / "chests"
    for target in (helper_target, chest_target):
        assert_within(resources, target)
        if target.exists():
            shutil.rmtree(target)

    removed_helper_entries = 0
    helper_files = sorted(source_helper.rglob("*.json"))
    for source in helper_files:
        payload = json.loads(source.read_text(encoding="utf-8-sig"))
        payload, removed = scrub_missing_entries(payload)
        removed_helper_entries += removed
        write_json(helper_target / source.relative_to(source_helper), payload)

    chest_files = sorted(source_chests.glob("*.json"))
    for source in chest_files:
        payload = json.loads(source.read_text(encoding="utf-8-sig"))
        payload = replace_strings(payload, OLD_HELPER_PREFIX, NEW_HELPER_PREFIX)
        payload = replace_strings(payload, OLD_NAMESPACE, NEW_NAMESPACE)
        write_json(chest_target / source.name, payload)

    loot_data_dir = resources / "data" / "utd_loot_core" / "loot"
    write_json(loot_data_dir / "registry.json", registry)
    write_json(loot_data_dir / "balance.json", balance)

    hashes = generated_hashes(resources)
    aggregate = aggregate_hash(hashes)
    hash_payload = {
        "schemaVersion": 1,
        "algorithm": "SHA-256",
        "aggregateSha256": aggregate,
        "files": dict(sorted(hashes.items())),
    }
    write_json(resources / "data" / "utd_loot_core" / "manifest.hashes.json", hash_payload)

    manifest = {
        "schemaVersion": 1,
        "modId": "utd_loot_core",
        "minecraftVersion": "1.21.1",
        "neoForgeVersion": "21.1.233",
        "generatorVersion": GENERATOR_VERSION,
        "generatedAtUtc": "2026-07-11T00:00:00Z",
        "sourceBaseline": {
            "archive": "_local_snapshots/runtime-kubejs-baseline-20260711.zip",
            "sha256": BASELINE_ARCHIVE_SHA256,
            "runtimeRoot": "UtilWeDie-Neo-1.21.1/.minecraft/versions/1.21.1-NeoForge_21.1.233/kubejs",
            "files": collect_source_hashes(runtime),
        },
        "counts": {
            "registrySeed": len(registry),
            "registryEnabled": sum(1 for row in registry if row.get("lootEnabled", True)),
            "disabledMissingMod": len(disabled_found),
            "helperTables": len(helper_files),
            "chestTables": len(chest_files),
            "containerConfig": len(balance.get("containerConfig", {})),
            "familyByLootTable": len(balance.get("familyByLootTable", {})),
            "templates": len(balance.get("templates", {})),
            "removedMissingItemEntries": removed_helper_entries,
        },
        "namespace": {
            "container": NEW_NAMESPACE,
            "helper": "utd_loot_core",
            "legacy": OLD_NAMESPACE,
        },
        "disabledMissingModIds": sorted(disabled_found),
        "generatedDataAggregateSha256": aggregate,
    }
    write_json(resources / "data" / "utd_loot_core" / "manifest.json", manifest)
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--runtime", type=Path, default=DEFAULT_RUNTIME)
    parser.add_argument("--resource-root", type=Path, default=DEFAULT_RESOURCES)
    args = parser.parse_args()
    manifest = import_data(args.runtime, args.resource_root)
    print(json.dumps(manifest["counts"], ensure_ascii=False, sort_keys=True))
    print(f"aggregate={manifest['generatedDataAggregateSha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
