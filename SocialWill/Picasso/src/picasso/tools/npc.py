from __future__ import annotations

import json
import logging
from datetime import datetime, timezone

from picasso.models.block import BlockState
from picasso.session import session

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
        try:
            if session.bridge is None or session.world_path is None:
                return {
                    "ok": False,
                    "error": "world_not_set",
                    "message": "Call set_world before place_npc_marker.",
                }
            if npc_type not in {"key_npc", "ambient", "enemy", "vendor"}:
                return {"ok": False, "error": "invalid_npc_type", "message": npc_type}
            if facing not in {"north", "south", "east", "west"}:
                return {"ok": False, "error": "invalid_facing", "message": facing}

            session.bridge.place_block(x, y, z, BlockState.from_id("minecraft:structure_void"))
            marker_dir = session.world_path / "picasso_markers"
            marker_dir.mkdir(parents=True, exist_ok=True)
            marker_file = marker_dir / f"{x}_{y}_{z}.json"
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
            marker_file.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
            return {
                "ok": True,
                "marker_file": str(marker_file),
                "summary": f"NPC marker placed at ({x}, {y}, {z}). Type: {npc_type}, faction: {faction}.",
            }
        except Exception as exc:
            logger.exception("Unexpected error in place_npc_marker")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
