from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
PROJECT_DIR = TOOLS_DIR.parent
RESOURCES = PROJECT_DIR / "src" / "main" / "resources"
sys.path.insert(0, str(TOOLS_DIR))

from validate import EXPECTED_MISSING_IDS, aggregate_hash, validate_resources  # noqa: E402


class LootResourceValidationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.stats = validate_resources(RESOURCES)

    def test_exact_dataset_counts(self) -> None:
        self.assertEqual(798, self.stats["registry"])
        self.assertEqual(14, self.stats["disabledMissingMod"])
        self.assertEqual(78, self.stats["helperTables"])
        self.assertEqual(280, self.stats["chestTables"])

    def test_reference_graph_is_closed(self) -> None:
        self.assertEqual(1316, self.stats["helperReferences"])
        self.assertEqual(61, self.stats["referencedHelpers"])

    def test_missing_mod_rows_are_preserved_but_disabled(self) -> None:
        registry_path = RESOURCES / "data" / "utd_loot_core" / "loot" / "registry.json"
        registry = json.loads(registry_path.read_text(encoding="utf-8"))
        by_id = {row["id"]: row for row in registry}
        self.assertEqual(EXPECTED_MISSING_IDS, EXPECTED_MISSING_IDS.intersection(by_id))
        self.assertTrue(all(by_id[item_id]["lootEnabled"] is False for item_id in EXPECTED_MISSING_IDS))

    def test_hash_aggregation_is_order_independent(self) -> None:
        first = {"b": "BB", "a": "AA"}
        second = {"a": "AA", "b": "BB"}
        self.assertEqual(aggregate_hash(first), aggregate_hash(second))


if __name__ == "__main__":
    unittest.main()
