from __future__ import annotations

import asyncio

from picasso import prompts, server


def test_server_instructions_publish_semantic_honesty_boundary() -> None:
    assert server.mcp.instructions == prompts.SERVER_INSTRUCTIONS
    assert "candidate_only" in server.mcp.instructions
    assert "does not authorize world mutation" in server.mcp.instructions


def test_structure_interpretation_prompt_is_registered_and_renderable(
    monkeypatch,
) -> None:
    isolated = server.SingleFlightFastMCP(
        "picasso-prompt-test",
        instructions=prompts.SERVER_INSTRUCTIONS,
    )
    monkeypatch.setattr(server, "mcp", isolated)

    server.register_prompts()

    prompt = isolated._prompt_manager.get_prompt("interpret_world_structure")
    assert prompt is not None
    assert [argument.name for argument in prompt.arguments or []] == [
        "x_min",
        "y_min",
        "z_min",
        "x_max",
        "y_max",
        "z_max",
        "focus",
    ]
    assert [argument.required for argument in prompt.arguments or []] == [
        True,
        True,
        True,
        True,
        True,
        True,
        False,
    ]

    messages = asyncio.run(
        prompt.render(
            {
                "x_min": -8,
                "y_min": -60,
                "z_min": 0,
                "x_max": 8,
                "y_max": -45,
                "z_max": 16,
                "focus": "staircase and second-floor access",
            }
        )
    )

    assert len(messages) == 1
    assert messages[0].role == "user"
    assert "(-8, -60, 0)..(8, -45, 16)" in messages[0].content.text
    assert "staircase and second-floor access" in messages[0].content.text
    assert "inspect_volume" in messages[0].content.text
    assert "world-mutating" in messages[0].content.text
