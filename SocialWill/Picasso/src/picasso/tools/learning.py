from __future__ import annotations

import json
import logging
import re

from picasso.config import config
from picasso.core.fragment_library import FragmentLibrary
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
        tags: list[str] | None = None,
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
                    "tags": tags or [],
                }
            )
            config.fragments_dir.mkdir(parents=True, exist_ok=True)
            saved_path = config.fragments_dir / f"{name}.json"
            saved_path.write_text(
                json.dumps(fragment.model_dump(), ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            if session.fragment_library is None:
                session.fragment_library = FragmentLibrary(config.fragments_dir)
            session.fragment_library.reload()
            return {
                "ok": True,
                "saved_path": str(saved_path),
                "message": f"Fragment '{name}' created and registered.",
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
    ) -> dict:
        """Save a new Style Bundle definition and register it for this server session."""
        try:
            if not _valid_name(name):
                return {"ok": False, "error": "invalid_name", "message": "Invalid bundle name."}
            bundle = {
                "name": name,
                "description": description,
                "version": version,
                "default_seed": default_seed,
                "entries": entries,
            }
            config.bundles_dir.mkdir(parents=True, exist_ok=True)
            saved_path = config.bundles_dir / f"{name}.json"
            saved_path.write_text(json.dumps(bundle, ensure_ascii=False, indent=2), encoding="utf-8")
            session.bundle_registry[name] = bundle
            return {
                "ok": True,
                "saved_path": str(saved_path),
                "message": f"Bundle '{name}' created and registered.",
            }
        except Exception as exc:
            logger.exception("Unexpected error in create_bundle")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
