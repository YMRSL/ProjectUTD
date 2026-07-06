from __future__ import annotations

import json
import logging
import re

from picasso.config import config
from picasso.core.style_engine import StyleEngine, load_pass_registry
from picasso.models.style_pass import StylePass
from picasso.session import session
from picasso.tools.world_io import ensure_region

logger = logging.getLogger(__name__)


def _engine() -> StyleEngine:
    return StyleEngine(
        session.pass_registry,
        config.safe_blocks_path,
        pattern_matcher=session.pattern_matcher,
        fragment_library=session.fragment_library,
    )


def register(mcp) -> None:
    @mcp.tool()
    def list_passes() -> dict:
        """List available Style Passes."""
        try:
            session.pass_registry = load_pass_registry(config.passes_dir)
            passes = [
                {
                    "name": style_pass.name,
                    "description": style_pass.description,
                    "version": style_pass.version,
                    "type": style_pass.type,
                    "targets": style_pass.targets,
                    "rule_count": len(style_pass.rules),
                }
                for style_pass in session.pass_registry.values()
            ]
            return {"ok": True, "passes": passes}
        except Exception as exc:
            logger.exception("Unexpected error in list_passes")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def preview_pass(
        pass_name: str,
        cx: int,
        cz: int,
        radius_chunks: int,
        intensity: float = 1.0,
        seed: int = 42,
        space_filter: str | None = None,
    ) -> dict:
        """Dry-run a Style Pass without writing to disk."""
        try:
            region = ensure_region(cx, cz, radius_chunks)
            preview = _engine().preview(pass_name, region, intensity, seed, space_filter)
            return {"ok": True, **preview}
        except KeyError as exc:
            return {"ok": False, "error": "pass_not_found", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error in preview_pass")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            return {"ok": False, "error": code, "message": str(exc)}

    @mcp.tool()
    def apply_pass(
        pass_name: str,
        cx: int,
        cz: int,
        radius_chunks: int,
        intensity: float = 1.0,
        seed: int = 42,
        space_filter: str | None = None,
    ) -> dict:
        """Execute a Style Pass and write changes to the active world."""
        try:
            if session.bridge is None:
                return {
                    "ok": False,
                    "error": "world_not_set",
                    "message": "Call set_world before apply_pass.",
                }
            region = ensure_region(cx, cz, radius_chunks)
            changes = _engine().apply(pass_name, region, intensity, seed, space_filter)
            changed = session.bridge.write_region(changes)
            session.last_region = None
            return {
                "ok": True,
                "changed": changed,
                "summary": f"Applied {pass_name}. {changed} blocks changed. World saved.",
            }
        except KeyError as exc:
            return {"ok": False, "error": "pass_not_found", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error in apply_pass")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def create_pass(name: str, description: str, rules: list[dict]) -> dict:
        """Create and register a new block Style Pass."""
        try:
            if not re.match(r"^[a-zA-Z0-9_\-]+$", name):
                return {"ok": False, "error": "invalid_name", "message": "Invalid pass name."}
            data = {"name": name, "description": description, "rules": rules}
            style_pass = StylePass.model_validate(data)
            config.passes_dir.mkdir(parents=True, exist_ok=True)
            saved_path = config.passes_dir / f"{name}.json"
            saved_path.write_text(
                json.dumps(style_pass.model_dump(), ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            session.pass_registry[name] = style_pass
            return {
                "ok": True,
                "saved_path": str(saved_path),
                "message": f"Pass '{name}' created and registered.",
            }
        except Exception as exc:
            logger.exception("Unexpected error in create_pass")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
