from __future__ import annotations

import json
import logging
from collections.abc import Mapping
from pathlib import Path
from typing import Any

from picasso.models.bundle import Bundle


logger = logging.getLogger(__name__)


class BundleReferenceError(ValueError):
    """Raised when a bundle refers to a style pass that is not registered."""


def load_bundle_registry(
    bundles_dir: str | Path,
    pass_registry: Mapping[str, Any],
) -> dict[str, dict]:
    registry: dict[str, dict] = {}
    path = Path(bundles_dir)
    if not path.exists():
        logger.warning("Bundles directory missing: %s", path)
        return registry

    for bundle_path in sorted(path.glob("*.json")):
        try:
            data = json.loads(bundle_path.read_text(encoding="utf-8"))
            if not isinstance(data, dict):
                raise ValueError("bundle definition must be a JSON object")
            expected_name = bundle_path.stem
            if data.get("name") != expected_name:
                raise ValueError(f"name must match filename ({expected_name})")
            bundle = Bundle.model_validate(data)
            validate_bundle_pass_refs(bundle, pass_registry)
        except Exception as exc:
            logger.warning("Skipping invalid bundle %s: %s", bundle_path, exc)
            continue
        registry[bundle.name] = bundle.to_registry_dict()
    return registry


def validate_bundle_pass_refs(
    bundle: Bundle,
    pass_registry: Mapping[str, Any],
) -> None:
    missing = sorted(
        {
            pass_entry.name
            for entry in bundle.entries
            for pass_entry in entry.passes
            if pass_entry.name not in pass_registry
        }
    )
    if missing:
        raise BundleReferenceError(
            "unknown style pass reference(s): " + ", ".join(missing)
        )
