from __future__ import annotations

import logging
from collections import Counter, defaultdict

from picasso.tools.world_io import ensure_region
from picasso.session import session

logger = logging.getLogger(__name__)


def register(mcp) -> None:
    @mcp.tool()
    def analyze_region(cx: int, cz: int, radius_chunks: int) -> dict:
        """Classify surfaces, detect vanilla furniture patterns, and summarize a region."""
        try:
            region = ensure_region(cx, cz, radius_chunks)
            surface_counts = Counter(region.surface_classes.values())
            by_surface: dict[str, Counter] = defaultdict(Counter)
            vegetation = 0
            damaged = 0
            for pos, state in region.blocks.items():
                surface = region.surface_classes.get(pos, "unknown")
                by_surface[surface][state.full_id] += 1
                if any(token in state.name for token in ("leaf", "leaves", "vine", "moss", "grass")):
                    vegetation += 1
                if any(token in state.name for token in ("cracked", "cobble", "gravel", "rubble")):
                    damaged += 1

            top_blocks_by_surface = {}
            for surface, counter in by_surface.items():
                total = sum(counter.values()) or 1
                top_blocks_by_surface[surface] = [
                    {"block": block, "count": count, "fraction": count / total}
                    for block, count in counter.most_common(10)
                ]

            pattern_rows = []
            if session.pattern_matcher is not None:
                grouped = defaultdict(list)
                for match in session.pattern_matcher.find_matches(region):
                    grouped[match.pattern_name].append(match)
                pattern_rows = [
                    {
                        "pattern": name,
                        "count": len(matches),
                        "sample_pos": matches[0].anchor_pos.to_dict(),
                    }
                    for name, matches in sorted(grouped.items())
                ]

            block_count = len(region.blocks) or 1
            return {
                "ok": True,
                "surface_counts": dict(surface_counts),
                "top_blocks_by_surface": top_blocks_by_surface,
                "pattern_matches": pattern_rows,
                "vegetation_coverage": vegetation / block_count,
                "damage_estimate": damaged / block_count,
                "summary": (
                    f"Analyzed {len(region.blocks)} blocks. "
                    f"Detected {len(pattern_rows)} furniture pattern groups."
                ),
            }
        except Exception as exc:
            logger.exception("Unexpected error in analyze_region")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            return {"ok": False, "error": code, "message": str(exc)}
