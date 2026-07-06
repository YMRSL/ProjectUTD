from __future__ import annotations

from pydantic import BaseModel, Field


class FragmentBlock(BaseModel):
    offset: tuple[int, int, int]
    block: str
    properties: dict[str, str] = Field(default_factory=dict)
    probability: float = 1.0
    preserve_existing: bool = False


class Fragment(BaseModel):
    name: str
    description: str
    anchor_surface: str
    footprint: str
    blocks: list[FragmentBlock]
    requires_clear_above: bool = True
    min_clear_height: int = 2
    destructive: bool = False
    tags: list[str] = Field(default_factory=list)
