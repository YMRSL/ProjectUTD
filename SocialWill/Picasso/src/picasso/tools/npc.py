from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Any

from picasso.config import config
from picasso.core.journal import JournalArtifact, JournalError, JournalWriteError
from picasso.core.style_engine import StyleEngine
from picasso.core.write_choke import WriteChoke, load_marker_positions
from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData
from picasso.session import session
from picasso.tools.protection import current_player_protection

logger = logging.getLogger(__name__)


def register(mcp) -> None:
    @mcp.tool()
    def place_npc_marker(
        x: int,
        y: int,
        z: int,
        npc_type: str,
        faction: str,
        facing: str = "south",
        dialogue_id: str | None = None,
        quest_id: str | None = None,
        source_agent: str | None = None,
    ) -> dict:
        """Place an invisible NPC marker block and companion JSON metadata file."""
        if session.bridge is None or session.world_path is None:
            return _failure(
                "world_not_set",
                "Call set_world before place_npc_marker.",
            )
        if npc_type not in {"key_npc", "ambient", "enemy", "vendor"}:
            return _failure("invalid_npc_type", npc_type)
        if facing not in {"north", "south", "east", "west"}:
            return _failure("invalid_facing", facing)

        pos = BlockPos(x, y, z)
        marker_dir = session.world_path / "picasso_markers"
        marker_file = marker_dir / f"{_coord_token(x)}_{_coord_token(y)}_{_coord_token(z)}.json"

        try:
            marker_positions = load_marker_positions(session.world_path)
            if pos in marker_positions or marker_file.exists():
                return _failure(
                    "marker_already_exists",
                    f"An NPC marker already exists at ({x}, {y}, {z}).",
                )

            try:
                before_state, has_block_entity = session.bridge.read_block_with_entity(x, y, z)
            except Exception as exc:
                logger.exception("Failed to read NPC marker target")
                return _failure(
                    "marker_target_read_failed",
                    f"Could not safely read marker target ({x}, {y}, {z}): {exc}",
                )
            if has_block_entity:
                return _failure(
                    "marker_target_has_block_entity",
                    f"Marker target ({x}, {y}, {z}) contains a block entity and is protected.",
                    found=before_state.full_id,
                )
            if not before_state.is_air:
                return _failure(
                    "marker_target_occupied",
                    f"Marker target ({x}, {y}, {z}) is occupied by {before_state.full_id}.",
                    found=before_state.full_id,
                )

            source_region = _single_point_region(pos, before_state)
            raw_changes = _single_point_region(
                pos,
                BlockState.from_id("minecraft:structure_void"),
                write_context="decoration",
            )
            engine = StyleEngine(
                session.pass_registry,
                config.safe_blocks_path,
                pattern_matcher=session.pattern_matcher,
                fragment_library=session.fragment_library,
            )
            protection = current_player_protection()
            choke_result = WriteChoke(
                engine.safe_replaceable,
                engine.structural_never_touch,
                marker_positions=marker_positions,
                safety_policy_error=engine.safety_policy_error,
                player_protection_evaluator=protection,
            ).validate(
                source_region,
                raw_changes,
                only_safe_blocks=True,
                enforce_modded_gate=False,
            )
            if choke_result.blocked_error or choke_result.changed_count != 1:
                reason = choke_result.blocked_error or (
                    choke_result.skipped[0]["reason"]
                    if choke_result.skipped
                    else "no_valid_change"
                )
                return _failure(
                    "marker_write_rejected",
                    f"Marker write was rejected by the write safety gate: {reason}.",
                    placements_skipped=choke_result.skipped,
                    player_protection=choke_result.player_protection,
                )

            payload = {
                "pos": {"x": x, "y": y, "z": z},
                "npc_type": npc_type,
                "faction": faction,
                "facing": facing,
                "dialogue_id": dialogue_id,
                "quest_id": quest_id,
                "source_agent": source_agent,
                "created_at": datetime.now(timezone.utc).isoformat(),
            }
            try:
                artifact = JournalArtifact.json_file(
                    marker_file.relative_to(session.world_path).as_posix(),
                    payload,
                    compound_positions=(pos,),
                    must_not_exist=True,
                )
                journal_result = session.require_journal().apply(
                    choke_result.changes,
                    tool="place_npc_marker",
                    argument_summary={
                        "x": x,
                        "y": y,
                        "z": z,
                        "npc_type": npc_type,
                        "faction": faction,
                        "facing": facing,
                        "dialogue_id": dialogue_id,
                        "quest_id": quest_id,
                        "source_agent": source_agent,
                    },
                    artifacts=(artifact,),
                )
                changed = journal_result.changed
            except JournalWriteError as exc:
                logger.exception("Journaled NPC marker transaction failed")
                session.last_region = None
                error = (
                    "marker_metadata_write_failed"
                    if exc.stage == "artifact_write"
                    else "marker_world_write_failed"
                )
                return _failure(
                    error,
                    str(exc),
                    placements_skipped=choke_result.skipped,
                    player_protection=choke_result.player_protection,
                    operation_id=exc.operation_id,
                    journal_path=exc.journal_path,
                    rollback_succeeded=exc.rollback_succeeded,
                    rollback_error=exc.rollback_error,
                )
            except JournalError as exc:
                session.last_region = None
                session.note_journal_error(exc)
                return _failure(
                    exc.code,
                    str(exc),
                    placements_skipped=choke_result.skipped,
                    player_protection=choke_result.player_protection,
                    **exc.details,
                )

            session.last_region = None

            return {
                "ok": True,
                "changed": changed,
                "marker_file": str(marker_file),
                "journal_entry": journal_result.reference(),
                "journal_warning": journal_result.warning,
                **_provenance(choke_result.skipped, choke_result.player_protection),
                "summary": (
                    f"NPC marker placed at ({x}, {y}, {z}). "
                    f"Type: {npc_type}, faction: {faction}."
                ),
            }
        except Exception as exc:
            logger.exception("Unexpected error in place_npc_marker")
            return _failure("internal_error", str(exc))


def _single_point_region(
    pos: BlockPos,
    state: BlockState,
    *,
    write_context: str | None = None,
) -> RegionData:
    region = RegionData(
        origin_cx=pos.x >> 4,
        origin_cz=pos.z >> 4,
        radius_chunks=0,
        y_min=pos.y,
        y_max=pos.y,
        loaded_chunks={(pos.x >> 4, pos.z >> 4)},
        block_entity_positions=set(),
    )
    # Source snapshots follow RegionData's sparse non-air invariant. Raw diffs
    # still need an explicit AIR state to represent rollback/removal writes.
    if not state.is_air or write_context is not None:
        region.set(pos, state)
    if write_context is not None:
        region.write_contexts[pos] = write_context
    return region


def _failure(
    error: str,
    message: str,
    *,
    placements_skipped: list[dict] | None = None,
    player_protection: dict | None = None,
    **extra: Any,
) -> dict:
    return {
        "ok": False,
        "error": error,
        "message": message,
        **_provenance(placements_skipped, player_protection),
        **extra,
    }


def _provenance(
    placements_skipped: list[dict] | None = None,
    player_protection: dict | None = None,
) -> dict:
    protection = player_protection or {
        "status": "unavailable",
        "diagnostics": [
            {
                "source": "player_protection",
                "reason": "snapshot_not_built",
            }
        ],
    }
    return {
        "journal_status": session.journal_status,
        "reversibility_warning": (
            "Journal not running - changes are NOT revertible. Operate on world copies only."
            if session.journal_status != "active"
            else None
        ),
        "space_classification": "heuristic",
        "noise_backend": session.noise_backend,
        "player_protection": protection.get("status", "unavailable"),
        "player_protection_details": protection,
        "placements_skipped": list(placements_skipped or []),
    }


def _coord_token(value: int) -> str:
    return f"n{abs(value)}" if value < 0 else str(value)
