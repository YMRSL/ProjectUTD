from __future__ import annotations


SERVER_INSTRUCTIONS = """
Picasso is a Minecraft world-analysis and stylization server. For semantic
interpretation, use a progressive read-only workflow: analyze_region finds
deterministic local candidates, then inspect_volume exposes bounded palette/RLE
voxel evidence for the area that actually needs interpretation. Never infer a
building, room, or intended use from aggregate counts alone.

Treat local_semantics.scope=candidate_only as an honesty boundary. Algorithmic
candidates state physical facts and hypotheses; they are not authoritative
human intent. Separate observations from interpretations, cite candidate bounds
and block-state evidence, give alternatives and confidence, and disclose
partial/truncated evidence. Request a smaller or adjacent inspection when the
boundary can change the conclusion. A repeated semantic rule must be supported
by multiple confirmed examples, adversarial negative tests, and human approval
before it is promoted into a reusable Pattern or fingerprint.

Interpretation does not authorize world mutation. Do not call write tools unless
the user separately requests a change and the normal Picasso dry-run, journal,
player-protection, closed-world, and modded-write gates are satisfied.
""".strip()


def register(mcp) -> None:
    @mcp.prompt(
        name="interpret_world_structure",
        title="Interpret Minecraft structure evidence",
        description=(
            "A read-only Agent workflow for explaining a bounded structure from "
            "Picasso candidates and voxel evidence."
        ),
    )
    def interpret_world_structure(
        x_min: int,
        y_min: int,
        z_min: int,
        x_max: int,
        y_max: int,
        z_max: int,
        focus: str = "overall structure and room relationships",
    ) -> str:
        """Build a read-only evidence-review prompt for one bounded volume."""

        return f"""
Review the Minecraft volume bounded inclusively by
({x_min}, {y_min}, {z_min})..({x_max}, {y_max}, {z_max}).
Focus: {focus}

Use this evidence workflow:

1. Confirm that the user-selected world is connected. Never invent or silently
   switch a world path.
2. Call analyze_region over the chunks and Y window covering the bounds. Treat
   its patterns and local semantics as candidates, not final labels.
3. Call inspect_volume for these bounds. If it reports incomplete/truncated
   evidence, subdivide the volume and inspect every relevant part before drawing
   a conclusion. Absence from a truncated view is not evidence of air.
4. Reconstruct geometry from inclusive bounds, the palette, and layer/Z-row/X-run
   encoding. Preserve block properties such as facing, half, shape, axis, and
   connection state when explaining assemblies.
5. Return three separate sections: observed facts; ranked interpretations with
   confidence and alternatives; unresolved evidence or boundary risks. Cite the
   candidate bounds and palette/block-state facts supporting each interpretation.
6. Do not assign ordinal floors until a host building, ground reference,
   horizontal levels, and vertical connectivity are established. Do not name a
   room solely from material or one furniture item.
7. Do not call apply_pass, apply_bundle, place_npc_marker, revert, or any other
   world-mutating operation during this review.

When the user's explanation conflicts with an algorithmic label, preserve the
physical observation, record the user's intent as the semantic ground truth for
this example, and propose a reusable rule only after confirming multiple positive
examples and meaningful negative examples.
""".strip()


__all__ = ["SERVER_INSTRUCTIONS", "register"]
