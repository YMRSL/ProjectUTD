from __future__ import annotations

import logging
from time import perf_counter

from picasso.core.bundle_executor import BundleExecutor
from picasso.core.journal import JournalError, JournalUnavailableError
from picasso.core.style_engine import StyleEngine, pass_has_destructive_content
from picasso.config import config
from picasso.core.write_choke import WriteChoke, load_marker_positions
from picasso.session import session
from picasso.tools.protection import current_player_protection
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
        force_modded_write: bool = False,
        include_player_built: bool = False,
        y_min: int | None = None,
        y_max: int | None = None,
    ) -> dict:
        """Apply a Style Bundle to the requested region."""
        protection_summary = {
            "status": "unavailable",
            "diagnostics": [
                {
                    "source": "player_protection",
                    "reason": "snapshot_not_built",
                }
            ],
        }
        try:
            started = perf_counter()
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
            region = ensure_region(cx, cz, radius_chunks, y_min=y_min, y_max=y_max)
            after_region = perf_counter()
            engine = StyleEngine(
                session.pass_registry,
                config.safe_blocks_path,
                pattern_matcher=session.pattern_matcher,
                fragment_library=session.fragment_library,
            )
            protection = current_player_protection()
            protection_summary = protection.to_summary()
            if (
                not dry_run
                and include_player_built
                and _bundle_has_destructive_pass(
                    bundle,
                    session.pass_registry,
                    fragment_library=session.fragment_library,
                    structure_type_filter=structure_type_filter,
                )
                and session.journal_status != "active"
            ):
                return {
                    "ok": False,
                    "error": "governance_requires_journal",
                    "message": (
                        "Destructive bundle writes in player-built areas require an active "
                        "journal."
                    ),
                    "journal_status": session.journal_status,
                    **_protection_payload(protection_summary),
                }
            choke = WriteChoke(
                engine.safe_replaceable,
                engine.structural_never_touch,
                marker_positions=load_marker_positions(session.world_path),
                safety_policy_error=engine.safety_policy_error,
                known_block_ids=(
                    set(session.catalog.by_id)
                    if session.catalog is not None
                    and isinstance(getattr(session.catalog, "by_id", None), dict)
                    else set()
                ),
                player_protection_evaluator=protection,
            )
            journal = None if dry_run else session.require_journal()
            result = BundleExecutor(
                engine,
                choke,
                bridge=session.bridge,
                journal=journal,
            ).execute_region(
                bundle,
                region,
                seed=seed,
                dry_run=dry_run,
                structure_type_filter=structure_type_filter,
                force_modded_write=force_modded_write,
                include_player_built=include_player_built,
            )
            unavailable = next(
                (item for item in result.errors if item.get("error") == "journal_unavailable"),
                None,
            )
            if unavailable is not None:
                session.note_journal_error(
                    JournalUnavailableError(unavailable.get("message", "Journal unavailable."))
                )
            after_bundle = perf_counter()
            if not dry_run:
                session.last_region = None
            if not result.ok:
                return {
                    "ok": False,
                    "error": result.errors[0]["error"] if result.errors else "internal_error",
                    "message": result.errors[0]["message"] if result.errors else "No passes ran.",
                    "errors": result.errors,
                    "dry_run": dry_run,
                    "space_classification": "heuristic",
                    "noise_backend": session.noise_backend,
                    **_protection_payload(protection_summary),
                    "region_cache_hit": session.last_region_cache_hit,
                    "region_source": session.last_region_source,
                    "y_window": region.to_summary()["y_window"],
                    "timings": {
                        "ensure_region_seconds": round(
                            session.last_region_read_seconds
                            if session.last_region_read_seconds is not None
                            else after_region - started,
                            3,
                        ),
                        "bundle_execute_seconds": round(after_bundle - after_region, 3),
                        "total_seconds": round(after_bundle - started, 3),
                    },
                }
            return {
                "ok": True,
                "dry_run": dry_run,
                "passes_applied": result.passes_applied,
                "journal_entries": [
                    entry["journal_entry"]
                    for entry in result.passes_applied
                    if entry.get("journal_entry") is not None
                ],
                "errors": result.errors,
                "total_changed" if not dry_run else "total_would_change": result.total_changed,
                "region_mode_warning": result.region_mode_warning,
                "reversibility_warning": (
                    "Journal not running - changes are NOT revertible. Operate on world copies only."
                    if not dry_run and session.journal_status != "active"
                    else None
                ),
                "space_classification": "heuristic",
                "noise_backend": session.noise_backend,
                **_protection_payload(protection_summary),
                "modded_write_warning": result.modded_write_warning,
                "placements_skipped": result.placements_skipped[:50],
                "region_cache_hit": session.last_region_cache_hit,
                "region_source": session.last_region_source,
                "y_window": region.to_summary()["y_window"],
                "timings": {
                    "ensure_region_seconds": round(
                        session.last_region_read_seconds
                        if session.last_region_read_seconds is not None
                        else after_region - started,
                        3,
                    ),
                    "bundle_execute_seconds": round(after_bundle - after_region, 3),
                    "total_seconds": round(after_bundle - started, 3),
                },
                "summary": (
                    f"{'Previewed' if dry_run else 'Applied'} bundle {bundle_name}. "
                    f"{result.total_changed} total block changes."
                ),
            }
        except JournalError as exc:
            session.last_region = None
            session.note_journal_error(exc)
            return {
                "ok": False,
                "error": exc.code,
                "message": str(exc),
                "journal_status": session.journal_status,
                **_protection_payload(protection_summary),
                **exc.details,
            }
        except Exception as exc:
            logger.exception("Unexpected error in apply_bundle")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            if isinstance(exc, OverflowError) and str(exc) == "region_too_large":
                code = "region_too_large"
            if isinstance(exc, ValueError) and str(exc) == "invalid_coordinates":
                code = "invalid_coordinates"
            if isinstance(exc, ValueError) and str(exc) == "invalid_y_window":
                code = "invalid_y_window"
            return {"ok": False, "error": code, "message": str(exc)}


def _protection_payload(summary: dict) -> dict:
    return {
        "player_protection": summary.get("status", "unavailable"),
        "player_protection_details": summary,
    }


def _bundle_has_destructive_pass(
    bundle: dict,
    pass_registry: dict,
    *,
    fragment_library,
    structure_type_filter: str | None,
) -> bool:
    for entry in bundle.get("entries", []):
        if structure_type_filter and entry.get("structure_type") != structure_type_filter:
            continue
        for pass_entry in entry.get("passes", []):
            style_pass = pass_registry.get(pass_entry.get("name"))
            if style_pass is not None and pass_has_destructive_content(
                style_pass,
                fragment_library,
            ):
                return True
    return False
