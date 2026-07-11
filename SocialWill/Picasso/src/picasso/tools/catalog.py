from __future__ import annotations

import logging

from picasso.session import session

logger = logging.getLogger(__name__)


def register(mcp) -> None:
    @mcp.tool()
    def query_catalog(
        category: str | None = None,
        surface: list[str] | None = None,
        context: list[str] | None = None,
        tags: list[str] | None = None,
        function: str | None = None,
        source: str | None = None,
    ) -> dict:
        """Query the Doomsday Decoration semantic catalog."""
        try:
            if session.catalog is None or not session.catalog.entries:
                return {
                    "ok": False,
                    "error": "catalog_not_loaded",
                    "message": "No valid catalog entries were loaded at startup.",
                }
            blocks = session.catalog.query(category, surface, context, tags, function, source)
            return {"ok": True, "count": len(blocks), "blocks": blocks}
        except Exception as exc:
            logger.exception("Unexpected error in query_catalog")
            return {"ok": False, "error": "internal_error", "message": str(exc)}
