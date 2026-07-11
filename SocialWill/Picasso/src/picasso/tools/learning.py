from __future__ import annotations

import json
import logging
import os
import re
import shutil
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from pydantic import ValidationError

from picasso.config import config
from picasso.core.bundle_library import BundleReferenceError, validate_bundle_pass_refs
from picasso.core.definition_store import (
    DefinitionAlreadyExistsError,
    save_json_definition,
)
from picasso.core.fragment_library import FragmentLibrary
from picasso.models.bundle import Bundle
from picasso.models.fragment import Fragment
from picasso.session import session

logger = logging.getLogger(__name__)


def _valid_name(name: str) -> bool:
    return bool(re.match(r"^[a-zA-Z0-9_\-]+$", name))


def register(mcp) -> None:
    @mcp.tool()
    def list_fragments(tags_filter: list[str] | None = None) -> dict:
        """List available Fragment templates."""
        try:
            if session.fragment_library is None:
                session.fragment_library = FragmentLibrary(config.fragments_dir)
            fragments = session.fragment_library.list(tags_filter)
            return {
                "ok": True,
                "count": len(fragments),
                "fragments": [
                    {
                        "name": fragment.name,
                        "description": fragment.description,
                        "anchor_surface": fragment.anchor_surface,
                        "tags": fragment.tags,
                        "footprint": fragment.footprint,
                    }
                    for fragment in fragments
                ],
            }
        except Exception as exc:
            logger.exception("Unexpected error in list_fragments")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def create_fragment(
        name: str,
        description: str,
        anchor_surface: str,
        footprint: str,
        blocks: list[dict],
        requires_clear_above: bool = True,
        min_clear_height: int = 2,
        destructive: bool = False,
        orientable: bool = False,
        match_hint: str | None = None,
        tags: list[str] | None = None,
        overwrite: bool = False,
    ) -> dict:
        """Save a new Fragment definition and register it for this server session."""
        try:
            if not _valid_name(name):
                return {"ok": False, "error": "invalid_name", "message": "Invalid fragment name."}
            fragment = Fragment.model_validate(
                {
                    "name": name,
                    "description": description,
                    "anchor_surface": anchor_surface,
                    "footprint": footprint,
                    "blocks": blocks,
                    "requires_clear_above": requires_clear_above,
                    "min_clear_height": min_clear_height,
                    "destructive": destructive,
                    "orientable": orientable,
                    "match_hint": match_hint,
                    "tags": tags or [],
                }
            )
            saved_path, _archived = save_json_definition(
                config.fragments_dir,
                fragment.name,
                fragment.model_dump(mode="json", exclude_none=True),
                overwrite=overwrite,
            )
            if session.fragment_library is None:
                session.fragment_library = FragmentLibrary(config.fragments_dir)
            session.fragment_library.reload()
            return {
                "ok": True,
                "saved_path": str(saved_path),
                "message": f"Fragment '{name}' created and registered.",
            }
        except DefinitionAlreadyExistsError:
            return {
                "ok": False,
                "error": "name_already_exists",
                "message": f"Fragment '{name}' already exists.",
            }
        except ValidationError as exc:
            return {
                "ok": False,
                "error": "invalid_fragment_definition",
                "message": str(exc),
            }
        except Exception as exc:
            logger.exception("Unexpected error in create_fragment")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def create_bundle(
        name: str,
        description: str,
        entries: list[dict],
        version: str = "1.0",
        default_seed: int = 42,
        overwrite: bool = False,
    ) -> dict:
        """Save a new Style Bundle definition and register it for this server session."""
        try:
            if not _valid_name(name):
                return {
                    "ok": False,
                    "error": "invalid_bundle_definition",
                    "message": "Bundle name must contain only letters, numbers, '-' or '_'.",
                }
            bundle = Bundle.model_validate(
                {
                    "name": name,
                    "description": description,
                    "version": version,
                    "default_seed": default_seed,
                    "entries": entries,
                }
            )
            validate_bundle_pass_refs(bundle, session.pass_registry)
            if _name_exists(config.bundles_dir, bundle.name) and not overwrite:
                return {
                    "ok": False,
                    "error": "name_already_exists",
                    "message": f"Bundle '{bundle.name}' already exists.",
                }
            config.bundles_dir.mkdir(parents=True, exist_ok=True)
            saved_path = config.bundles_dir / f"{bundle.name}.json"
            if saved_path.exists() and overwrite:
                _archive_bundle_existing(saved_path)
            bundle_data = bundle.to_registry_dict()
            _atomic_write_bundle(saved_path, bundle_data)
            session.bundle_registry[bundle.name] = bundle_data
            return {
                "ok": True,
                "saved_path": str(saved_path),
                "message": f"Bundle '{bundle.name}' created and registered.",
            }
        except (ValidationError, BundleReferenceError) as exc:
            return {
                "ok": False,
                "error": "invalid_bundle_definition",
                "message": str(exc),
            }
        except Exception as exc:
            logger.exception("Unexpected error in create_bundle")
            return {"ok": False, "error": "internal_error", "message": str(exc)}


def _name_exists(directory, name: str) -> bool:
    target = f"{name.lower()}.json"
    if not directory.exists():
        return False
    return any(path.name.lower() == target for path in directory.glob("*.json"))


def _archive_bundle_existing(path: Path) -> Path:
    archive_dir = path.parent / "_replaced"
    archive_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    candidate = archive_dir / f"{path.stem}.{stamp}.json"
    suffix = 1
    while candidate.exists():
        candidate = archive_dir / f"{path.stem}.{stamp}.{suffix}.json"
        suffix += 1
    shutil.copy2(path, candidate)
    return candidate


def _atomic_write_bundle(path: Path, bundle: dict) -> None:
    payload = (json.dumps(bundle, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=path.parent,
            prefix=f".{path.name}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temporary_path = Path(handle.name)
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temporary_path, path)
        temporary_path = None
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)
