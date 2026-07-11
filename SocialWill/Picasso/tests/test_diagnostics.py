from __future__ import annotations

from picasso.tools import diagnostics


class FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def test_describe_capabilities_lists_volume_evidence_and_semantic_prompt() -> None:
    mcp = FakeMCP()
    diagnostics.register(mcp)

    result = mcp.tools["describe_capabilities"]()

    tool_names = {item["name"] for item in result["tools"]}
    assert len(tool_names) == 22
    assert "inspect_volume" in tool_names
    assert result["prompts"] == [
        {
            "name": "interpret_world_structure",
            "purpose": "Guide a read-only Agent review of bounded candidate and voxel evidence.",
        }
    ]
    assert any("subdivide truncated views" in step for step in result["safe_workflow"])
