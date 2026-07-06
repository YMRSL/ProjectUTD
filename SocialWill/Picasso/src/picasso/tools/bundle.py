from __future__ import annotations

import logging

from picasso.core.style_engine import StyleEngine
from picasso.config import config
from picasso.session import session
from picasso.tools.world_io import ensure_region

logger = logging.getLogger(__name__)


def register(mcp) -> None:
    @mcp.tool()
    def list_bundles() -> dict:
        """List available Style Bundles."""
        try:
            bundles = [
                {
                    "name": name,
                    "description": bundle.get("description", ""),
                    "version": bundle.get("version", "1.0"),
                    "entry_count": len(bundle.get("entries", [])),
                }
                for name, bundle in sorted(session.bundle_registry.items())
            ]
            return {"ok": True, "bundles": bundles}
        except Exception as exc:
            logger.exception("Unexpected error in list_bundles")
            return {"ok": False, "error": "internal_error", "message": str(exc)}

    @mcp.tool()
    def apply_bundle(
        bundle_name: str,
        cx: int,
        cz: int,
        radius_chunks: int,
        seed: int | None = None,
        dry_run: bool = True,
        structure_type_filter: str | None = None,
    ) -> dict:
        """Apply a Style Bundle to the requested region."""
        try:
            bundle = session.bundle_registry.get(bundle_name)
            if bundle is None:
                return {
                    "ok": False,
                    "error": "bundle_not_found",
                    "message": f"Bundle not found: {bundle_name}",
                }
            if session.bridge is None:
                return {
                    "ok": False,
                    "error": "world_not_set",
                    "message": "Call set_world before apply_bundle.",
                }
            region = ensure_region(cx, cz, radius_chunks)
            engine = StyleEngine(
                session.pass_registry,
                config.safe_blocks_path,
                pattern_matcher=session.pattern_matcher,
                fragment_library=session.fragment_library,
            )
            effective_seed = int(seed if seed is not None else bundle.get("default_seed", 42))
            results = []
            total_changed = 0
            for entry in bundle.get("entries", []):
                structure_type = entry.get("structure_type")
                if structure_type_filter and structure_type != structure_type_filter:
                    continue
                for pass_entry in entry.get("passes", []):
                    pass_name = pass_entry["name"]
                    intensity = float(pass_entry.get("intensity", 1.0))
                    space_filter = pass_entry.get("space_filter")
                    if dry_run:
                        preview = engine.preview(pass_name, region, intensity, effective_seed, space_filter)
                        changed = preview["would_change"]
                    else:
                        changes = engine.apply(pass_name, region, intensity, effective_seed, space_filter)
                        changed = session.bridge.write_region(changes)
                    total_changed += changed
                    results.append(
                        {
                            "structure_type": structure_type,
                            "pass": pass_name,
                            "intensity": intensity,
                            "space_filter": space_filter,
                            "changed": changed,
                        }
                    )
            if not dry_run:
                session.last_region = None
            return {
                "ok": True,
                "dry_run": dry_run,
                "passes_applied": results,
                "total_changed": total_changed,
                "summary": (
                    f"{'Previewed' if dry_run else 'Applied'} bundle {bundle_name}. "
                    f"{total_changed} total block changes."
                ),
            }
        except Exception as exc:
            logger.exception("Unexpected error in apply_bundle")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
