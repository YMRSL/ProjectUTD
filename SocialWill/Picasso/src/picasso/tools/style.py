from __future__ import annotations

import logging
import re
from time import perf_counter

from pydantic import ValidationError

from picasso.config import config
from picasso.core.definition_store import (
    DefinitionAlreadyExistsError,
    save_json_definition,
)
from picasso.core.journal import JournalError
from picasso.core.style_engine import (
    StyleEngine,
    load_pass_registry,
    pass_has_destructive_content,
)
from picasso.core.write_choke import WriteChoke, block_id_from_state_or_air, load_marker_positions
from picasso.models.style_pass import StylePass
from picasso.session import session
from picasso.tools.protection import current_player_protection
from picasso.tools.world_io import ensure_region

logger = logging.getLogger(__name__)


def _engine() -> StyleEngine:
    return StyleEngine(
        session.pass_registry,
        config.safe_blocks_path,
        pattern_matcher=session.pattern_matcher,
        fragment_library=session.fragment_library,
    )


def _choke(engine: StyleEngine, player_protection_evaluator=None) -> WriteChoke:
    return WriteChoke(
        engine.safe_replaceable,
        engine.structural_never_touch,
        marker_positions=load_marker_positions(session.world_path),
        safety_policy_error=engine.safety_policy_error,
        known_block_ids=_known_catalog_block_ids(),
        player_protection_evaluator=(
            player_protection_evaluator
            if player_protection_evaluator is not None
            else current_player_protection()
        ),
    )


def _known_catalog_block_ids() -> set[str]:
    catalog = session.catalog
    by_id = getattr(catalog, "by_id", None)
    return set(by_id) if isinstance(by_id, dict) else set()


def _protection_payload(summary: dict) -> dict:
    return {
        "player_protection": summary.get("status", "unavailable"),
        "player_protection_details": summary,
    }


def _unavailable_protection_summary(reason: str) -> dict:
    return {
        "status": "unavailable",
        "diagnostics": [
            {
                "source": "player_protection",
                "reason": reason,
            }
        ],
    }


def _preview_payload(region, changes, skipped, protection: dict) -> dict:
    samples = []
    for pos, state in list(changes.blocks.items())[:20]:
        samples.append(
            {
                "pos": pos.to_dict(),
                "from": block_id_from_state_or_air(region.get(pos)),
                "to": state.full_id,
            }
        )
    return {
        "would_change": len(changes.blocks),
        "sample_changes": samples,
        "placements_skipped": skipped[:50],
        "space_classification": "heuristic",
        "noise_backend": session.noise_backend,
        **_protection_payload(protection),
    }


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
                    "deprecated": style_pass.deprecated,
                    "targets": style_pass.targets,
                    "rule_count": len(style_pass.rules),
                    "fragment_count": len(style_pass.fragments),
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
        include_player_built: bool = False,
        y_min: int | None = None,
        y_max: int | None = None,
    ) -> dict:
        """Dry-run a Style Pass without writing to disk."""
        try:
            started = perf_counter()
            region = ensure_region(cx, cz, radius_chunks, y_min=y_min, y_max=y_max)
            after_region = perf_counter()
            engine = _engine()
            style_pass = session.pass_registry.get(pass_name)
            if style_pass is None:
                return {"ok": False, "error": "pass_not_found", "message": pass_name}
            raw_changes = engine.apply(pass_name, region, intensity, seed, space_filter)
            after_engine = perf_counter()
            choke_result = _choke(engine).validate(
                region,
                raw_changes,
                only_safe_blocks=style_pass.only_safe_blocks,
                enforce_modded_gate=False,
                include_player_built=include_player_built,
            )
            after_choke = perf_counter()
            payload = _preview_payload(
                region,
                choke_result.changes,
                choke_result.skipped,
                choke_result.player_protection,
            )
            warnings = []
            if style_pass.deprecated:
                warnings.append(f"Pass {pass_name} is deprecated.")
            if choke_result.modded_positions and not config.modded_write_verified:
                warnings.append(
                    "Preview includes non-vanilla blocks; dry_run=false will require "
                    "PICASSO_MODDED_WRITE_VERIFIED=true or force_modded_write=true."
                )
            return {
                "ok": True,
                **payload,
                "warnings": warnings,
                "region_cache_hit": session.last_region_cache_hit,
                "region_source": session.last_region_source,
                "y_window": region.to_summary()["y_window"],
                "raw_changes": len(raw_changes.blocks),
                "modded_positions": len(choke_result.modded_positions),
                "timings": {
                    "ensure_region_seconds": round(
                        session.last_region_read_seconds
                        if session.last_region_read_seconds is not None
                        else after_region - started,
                        3,
                    ),
                    "engine_apply_seconds": round(after_engine - after_region, 3),
                    "choke_validate_seconds": round(after_choke - after_engine, 3),
                    "total_seconds": round(after_choke - started, 3),
                },
                "summary": f"{payload['would_change']} blocks would change.",
            }
        except KeyError as exc:
            return {"ok": False, "error": "pass_not_found", "message": str(exc)}
        except Exception as exc:
            logger.exception("Unexpected error in preview_pass")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            if isinstance(exc, OverflowError) and str(exc) == "region_too_large":
                code = "region_too_large"
            if isinstance(exc, ValueError) and str(exc) == "invalid_coordinates":
                code = "invalid_coordinates"
            if isinstance(exc, ValueError) and str(exc) == "invalid_y_window":
                code = "invalid_y_window"
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
        force_modded_write: bool = False,
        include_player_built: bool = False,
        y_min: int | None = None,
        y_max: int | None = None,
    ) -> dict:
        """Execute a Style Pass and write changes to the active world."""
        protection_summary = _unavailable_protection_summary("snapshot_not_built")
        try:
            started = perf_counter()
            if session.bridge is None:
                return {
                    "ok": False,
                    "error": "world_not_set",
                    "message": "Call set_world before apply_pass.",
                }
            region = ensure_region(cx, cz, radius_chunks, y_min=y_min, y_max=y_max)
            after_region = perf_counter()
            engine = _engine()
            style_pass = session.pass_registry.get(pass_name)
            if style_pass is None:
                return {"ok": False, "error": "pass_not_found", "message": pass_name}
            protection = current_player_protection()
            protection_summary = protection.to_summary()
            if (
                include_player_built
                and pass_has_destructive_content(style_pass, session.fragment_library)
                and session.journal_status != "active"
            ):
                return {
                    "ok": False,
                    "error": "governance_requires_journal",
                    "message": (
                        "Destructive writes in player-built areas require an active journal."
                    ),
                    "journal_status": session.journal_status,
                    **_protection_payload(protection_summary),
                }
            raw_changes = engine.apply(pass_name, region, intensity, seed, space_filter)
            after_engine = perf_counter()
            choke_result = _choke(engine, protection).validate(
                region,
                raw_changes,
                only_safe_blocks=style_pass.only_safe_blocks,
                enforce_modded_gate=True,
                force_modded_write=force_modded_write,
                include_player_built=include_player_built,
            )
            after_choke = perf_counter()
            if choke_result.blocked_error:
                return {
                    "ok": False,
                    "error": choke_result.blocked_error,
                    "message": choke_result.blocked_message,
                    "space_classification": "heuristic",
                    "noise_backend": session.noise_backend,
                    **_protection_payload(choke_result.player_protection),
                }
            journal_result = session.require_journal().apply(
                choke_result.changes,
                tool="apply_pass",
                pass_name=pass_name,
                seed=seed,
                argument_summary={
                    "pass_name": pass_name,
                    "cx": cx,
                    "cz": cz,
                    "radius_chunks": radius_chunks,
                    "intensity": intensity,
                    "space_filter": space_filter,
                    "force_modded_write": force_modded_write,
                    "include_player_built": include_player_built,
                    "y_min": y_min,
                    "y_max": y_max,
                },
            )
            changed = journal_result.changed
            after_write = perf_counter()
            session.last_region = None
            modded_warning = None
            if choke_result.modded_positions and not config.modded_write_verified:
                modded_warning = "Forced non-vanilla write before Phase 1.5 verification."
            return {
                "ok": True,
                "changed": changed,
                "journal_entry": journal_result.reference(),
                "journal_warning": journal_result.warning,
                "reversibility_warning": (
                    "Journal not running - changes are NOT revertible. Operate on world copies only."
                    if session.journal_status != "active"
                    else None
                ),
                "space_classification": "heuristic",
                "noise_backend": session.noise_backend,
                **_protection_payload(choke_result.player_protection),
                "modded_write_warning": modded_warning,
                "placements_skipped": choke_result.skipped[:50],
                "region_cache_hit": session.last_region_cache_hit,
                "region_source": session.last_region_source,
                "y_window": region.to_summary()["y_window"],
                "raw_changes": len(raw_changes.blocks),
                "timings": {
                    "ensure_region_seconds": round(
                        session.last_region_read_seconds
                        if session.last_region_read_seconds is not None
                        else after_region - started,
                        3,
                    ),
                    "engine_apply_seconds": round(after_engine - after_region, 3),
                    "choke_validate_seconds": round(after_choke - after_engine, 3),
                    "write_seconds": round(after_write - after_choke, 3),
                    "total_seconds": round(after_write - started, 3),
                },
                "warnings": (
                    ([f"Pass {pass_name} is deprecated."] if style_pass.deprecated else [])
                    + ([journal_result.warning] if journal_result.warning else [])
                ),
                "summary": f"Applied {pass_name}. {changed} blocks changed. World saved.",
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
        except KeyError as exc:
            return {"ok": False, "error": "pass_not_found", "message": str(exc)}
        except Exception as exc:
            # A bridge write may have rolled back or poisoned its in-memory
            # state. Never keep serving a pre-write RegionData cache after an
            # apply-path exception.
            session.last_region = None
            logger.exception("Unexpected error in apply_pass")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            if isinstance(exc, OverflowError) and str(exc) == "region_too_large":
                code = "region_too_large"
            if isinstance(exc, ValueError) and str(exc) == "invalid_coordinates":
                code = "invalid_coordinates"
            if isinstance(exc, ValueError) and str(exc) == "invalid_y_window":
                code = "invalid_y_window"
            return {"ok": False, "error": code, "message": str(exc)}
    @mcp.tool()
    def create_pass(
        name: str,
        description: str,
        rules: list[dict],
        version: str = "1.0",
        only_safe_blocks: bool = True,
        destructive: bool = False,
        overwrite: bool = False,
    ) -> dict:
        """Create and register a new block Style Pass."""
        try:
            if not re.match(r"^[a-zA-Z0-9_\-]+$", name):
                return {"ok": False, "error": "invalid_name", "message": "Invalid pass name."}
            data = {
                "name": name,
                "description": description,
                "version": version,
                "rules": rules,
                "only_safe_blocks": only_safe_blocks,
                "destructive": destructive,
            }
            style_pass = StylePass.model_validate(data)
            saved_path, _archived = save_json_definition(
                config.passes_dir,
                style_pass.name,
                style_pass.model_dump(mode="json", exclude_none=True),
                overwrite=overwrite,
            )
            session.pass_registry[style_pass.name] = style_pass
            return {
                "ok": True,
                "saved_path": str(saved_path),
                "message": f"Pass '{style_pass.name}' created and registered.",
            }
        except DefinitionAlreadyExistsError:
            return {
                "ok": False,
                "error": "name_already_exists",
                "message": f"Pass '{name}' already exists.",
            }
        except ValidationError as exc:
            return {
                "ok": False,
                "error": "invalid_pass_definition",
                "message": str(exc),
            }
        except Exception as exc:
            logger.exception("Unexpected error in create_pass")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
