from __future__ import annotations

import logging
from collections import Counter, defaultdict
from time import perf_counter

from picasso.core.stair_semantics import StairSemanticCandidate, detect_stair_semantics
from picasso.core.storey_semantics import detect_storey_level_candidates
from picasso.tools.world_io import ensure_region
from picasso.session import session

logger = logging.getLogger(__name__)

_MAX_LOCAL_SEMANTIC_CANDIDATES = 100


def register(mcp) -> None:
    @mcp.tool()
    def analyze_region(
        cx: int,
        cz: int,
        radius_chunks: int,
        y_min: int | None = None,
        y_max: int | None = None,
    ) -> dict:
        """Classify surfaces, detect vanilla furniture patterns, and summarize a region."""
        try:
            started = perf_counter()
            region = ensure_region(cx, cz, radius_chunks, y_min=y_min, y_max=y_max)
            after_region = perf_counter()
            target_surfaces = {
                pos: surface
                for pos, surface in region.surface_classes.items()
                if region.is_target_position(pos)
            }
            surface_counts = Counter(target_surfaces.values())
            by_surface: dict[str, Counter] = defaultdict(Counter)
            vegetation = 0
            damaged = 0
            target_block_count = 0
            for pos, state in region.iter_target_blocks():
                target_block_count += 1
                surface = target_surfaces.get(pos, "unknown")
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
                    if not region.is_target_position(match.anchor_pos):
                        continue
                    grouped[match.pattern_name].append(match)
                pattern_rows = [
                    {
                        "pattern": name,
                        "count": len(matches),
                        "sample_pos": matches[0].anchor_pos.to_dict(),
                    }
                    for name, matches in sorted(grouped.items())
                ]

            stair_candidates = detect_stair_semantics(region)
            storey_candidates = detect_storey_level_candidates(region)
            stair_rows = [
                _stair_candidate_summary(candidate)
                for candidate in stair_candidates[:_MAX_LOCAL_SEMANTIC_CANDIDATES]
            ]
            storey_rows = storey_candidates[:_MAX_LOCAL_SEMANTIC_CANDIDATES]
            local_semantics = {
                "scope": "candidate_only",
                "stair_assemblies": stair_rows,
                "stair_assembly_count": len(stair_candidates),
                "stair_assemblies_truncated": len(stair_candidates)
                > _MAX_LOCAL_SEMANTIC_CANDIDATES,
                "storey_level_candidates": storey_rows,
                "storey_level_candidate_count": len(storey_candidates),
                "storey_level_candidates_truncated": len(storey_candidates)
                > _MAX_LOCAL_SEMANTIC_CANDIDATES,
            }

            block_count = target_block_count or 1
            return {
                "ok": True,
                "surface_counts": dict(surface_counts),
                "top_blocks_by_surface": top_blocks_by_surface,
                "pattern_matches": pattern_rows,
                "local_semantics": local_semantics,
                "vegetation_coverage": vegetation / block_count,
                "damage_estimate": damaged / block_count,
                "space_classification": "heuristic",
                "noise_backend": session.noise_backend,
                "y_window": {"min": region.y_min, "max": region.y_max},
                "region_cache_hit": session.last_region_cache_hit,
                "region_source": session.last_region_source,
                "timings": {
                    "ensure_region_seconds": round(
                        session.last_region_read_seconds
                        if session.last_region_read_seconds is not None
                        else after_region - started,
                        3,
                    ),
                    "analysis_seconds": round(perf_counter() - after_region, 3),
                    "total_seconds": round(perf_counter() - started, 3),
                },
                "summary": (
                    f"Analyzed {target_block_count} blocks. "
                    f"Detected {len(pattern_rows)} furniture pattern groups, "
                    f"{len(stair_candidates)} stair assemblies, and "
                    f"{len(storey_candidates)} storey-level candidates."
                ),
            }
        except Exception as exc:
            logger.exception("Unexpected error in analyze_region")
            code = "world_not_set" if str(exc) == "world_not_set" else "internal_error"
            if isinstance(exc, OverflowError) and str(exc) == "region_too_large":
                code = "region_too_large"
            if isinstance(exc, ValueError) and str(exc) == "invalid_coordinates":
                code = "invalid_coordinates"
            if isinstance(exc, ValueError) and str(exc) == "invalid_y_window":
                code = "invalid_y_window"
            return {"ok": False, "error": code, "message": str(exc)}


def _stair_candidate_summary(candidate: StairSemanticCandidate) -> dict:
    role_counts = Counter(member.role for member in candidate.members)
    return {
        "kind": candidate.kind,
        "confidence": candidate.confidence,
        "anchor": candidate.anchor.to_dict(),
        "bounds": candidate.bounds.to_dict(),
        "member_count": len(candidate.members),
        "role_counts": dict(sorted(role_counts.items())),
    }
