from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


class NoiseConfig(BaseModel):
    type: Literal["perlin"] = "perlin"
    scale: float = 0.05
    threshold: float = 0.4


class ReplaceOption(BaseModel):
    block: str
    properties: dict[str, str] = Field(default_factory=dict)
    weight: float = 1.0


class StyleRule(BaseModel):
    match: dict[str, Any]
    action: Literal["replace", "place_adjacent", "remove"]
    replace_with: list[ReplaceOption] | None = None
    place_block: str | None = None
    direction: str | None = None
    weight: float = 1.0
    noise: NoiseConfig | None = None
    description: str | None = None


class StylePass(BaseModel):
    name: str
    description: str
    version: str = "1.0"
    targets: list[str] = Field(default_factory=list)
    rules: list[StyleRule] = Field(default_factory=list)
    only_safe_blocks: bool = True
    type: str = "block_pass"
    mappings: list[dict[str, Any]] = Field(default_factory=list)
    fragments: list[str] = Field(default_factory=list)
    anchor_surface: str | None = None
    density: float = 0.0
    noise: NoiseConfig | None = None
    min_spacing: int = 0
    space_filter: str | None = None
    only_safe_anchor_blocks: bool = True
