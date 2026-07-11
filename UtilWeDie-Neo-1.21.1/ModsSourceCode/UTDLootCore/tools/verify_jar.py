#!/usr/bin/env python3
"""Verify that the built JAR contains the validated runtime and override paths."""

from __future__ import annotations

import argparse
import hashlib
import json
import zipfile
from pathlib import Path


PROJECT_DIR = Path(__file__).resolve().parent.parent
DEFAULT_JAR = PROJECT_DIR / "build" / "libs" / "utd_loot_core-1.0.0-1.21.1.jar"


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest().upper()


def aggregate_hash(hashes: dict[str, str]) -> str:
    digest = hashlib.sha256()
    for relative, file_hash in sorted(hashes.items()):
        digest.update(relative.encode("utf-8"))
        digest.update(b"\0")
        digest.update(file_hash.encode("ascii"))
        digest.update(b"\n")
    return digest.hexdigest().upper()


def verify(jar_path: Path) -> dict[str, int | str]:
    if not jar_path.is_file():
        raise RuntimeError(f"JAR does not exist: {jar_path}")
    with zipfile.ZipFile(jar_path) as archive:
        names = set(archive.namelist())
        required = {
            "META-INF/neoforge.mods.toml",
            "com/projectutd/loot/UtdLootCore.class",
            "com/projectutd/loot/LootCatalog.class",
            "com/projectutd/loot/LootPityHandler.class",
            "data/utd_loot_core/loot/registry.json",
            "data/utd_loot_core/loot/balance.json",
            "data/utd_loot_core/manifest.json",
            "data/utd_loot_core/manifest.hashes.json",
        }
        missing = sorted(required - names)
        if missing:
            raise RuntimeError("JAR is missing required entries: " + ", ".join(missing))

        helper_names = sorted(
            name
            for name in names
            if name.startswith("data/utd_loot_core/loot_table/utd/") and name.endswith(".json")
        )
        chest_names = sorted(
            name
            for name in names
            if name.startswith("data/doomsday_decoration/loot_table/chests/") and name.endswith(".json")
        )
        if len(helper_names) != 78:
            raise RuntimeError(f"JAR helper-table count is {len(helper_names)}, expected 78")
        if len(chest_names) != 280:
            raise RuntimeError(f"JAR chest-table count is {len(chest_names)}, expected 280")
        forbidden_paths = [
            name
            for name in names
            if name.startswith("data/doomsday_functionality/") or "/loot_tables/" in name
        ]
        if forbidden_paths:
            raise RuntimeError("JAR contains legacy resource paths: " + ", ".join(sorted(forbidden_paths)[:10]))

        mod_toml = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
        if 'modId = "doomsday_decoration"' not in mod_toml:
            raise RuntimeError("Doomsday Decoration dependency is absent from mod metadata")
        dependency_section = mod_toml.split('modId = "doomsday_decoration"', 1)[1]
        if 'type = "required"' not in dependency_section or 'ordering = "AFTER"' not in dependency_section:
            raise RuntimeError("Doomsday Decoration dependency must be required/AFTER")

        manifest = json.loads(archive.read("data/utd_loot_core/manifest.json"))
        hash_index = json.loads(archive.read("data/utd_loot_core/manifest.hashes.json"))
        actual_hashes = {}
        for relative, expected_hash in hash_index["files"].items():
            if relative not in names:
                raise RuntimeError(f"hashed resource is absent from JAR: {relative}")
            actual_hash = sha256_bytes(archive.read(relative))
            if actual_hash != expected_hash:
                raise RuntimeError(f"embedded resource hash mismatch: {relative}")
            actual_hashes[relative] = actual_hash
        aggregate = aggregate_hash(actual_hashes)
        if aggregate != manifest["generatedDataAggregateSha256"]:
            raise RuntimeError("embedded aggregate does not match manifest")

        helper_text = "\n".join(archive.read(name).decode("utf-8") for name in helper_names)
        chest_text = "\n".join(archive.read(name).decode("utf-8") for name in chest_names)
        if "paraglider:paraglider" in helper_text:
            raise RuntimeError("paraglider remains in embedded helper tables")
        if "doomsday_functionality" in helper_text + chest_text:
            raise RuntimeError("old namespace remains in embedded loot tables")
        if "kubejs:utd/" in chest_text:
            raise RuntimeError("old KubeJS helper namespace remains in embedded chest tables")

    return {
        "jarBytes": jar_path.stat().st_size,
        "helperTables": len(helper_names),
        "chestTables": len(chest_names),
        "hashedFiles": len(actual_hashes),
        "aggregateSha256": aggregate,
        "jarSha256": sha256_bytes(jar_path.read_bytes()),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", nargs="?", type=Path, default=DEFAULT_JAR)
    args = parser.parse_args()
    try:
        stats = verify(args.jar)
    except Exception as exc:
        print(f"JAR VERIFY FAILED: {exc}")
        return 1
    print("JAR VERIFY OK")
    print(json.dumps(stats, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
