from __future__ import annotations

import re
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


_BLOCK_ID_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
_FOOTPRINT_RE = re.compile(r"^([1-9][0-9]*)x([1-9][0-9]*)$")
NonEmptyStr = Annotated[str, Field(min_length=1)]
FragmentMatchHint = Literal["glass_pane"]


class _DefinitionModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class FragmentBlock(_DefinitionModel):
    offset: tuple[int, int, int]
    block: str
    properties: dict[NonEmptyStr, NonEmptyStr] = Field(default_factory=dict)
    probability: float = Field(default=1.0, ge=0.0, le=1.0)
    preserve_existing: bool = False

    @field_validator("block")
    @classmethod
    def validate_block(cls, value: str) -> str:
        if not _BLOCK_ID_RE.fullmatch(value):
            raise ValueError(
                "block must be a canonical namespaced block id "
                "(for example, 'minecraft:stone')"
            )
        return value


class Fragment(_DefinitionModel):
    name: NonEmptyStr
    description: NonEmptyStr
    anchor_surface: Literal[
        "floor", "outer_wall", "inner_wall", "ceiling", "rooftop", "any"
    ]
    footprint: NonEmptyStr
    blocks: list[FragmentBlock]
    requires_clear_above: bool = True
    min_clear_height: int = Field(default=2, ge=0)
    destructive: bool = False
    orientable: bool = False
    tags: list[NonEmptyStr] = Field(default_factory=list)
    match_hint: FragmentMatchHint | None = None

    @field_validator("match_hint", mode="before")
    @classmethod
    def normalize_match_hint(cls, value: object) -> object:
        return value.strip() if isinstance(value, str) else value

    @model_validator(mode="after")
    def validate_block_contract(self) -> Fragment:
        if not self.blocks:
            raise ValueError("fragment blocks must not be empty")
        if not self.destructive and any(
            block.block == "minecraft:air" for block in self.blocks
        ):
            raise ValueError("AIR fragment blocks require destructive=true")

        footprint_match = _FOOTPRINT_RE.fullmatch(self.footprint)
        if footprint_match is None:
            raise ValueError("footprint must use positive '<width>x<depth>' dimensions")
        declared_width, declared_depth = (
            int(footprint_match.group(1)),
            int(footprint_match.group(2)),
        )
        x_offsets = [block.offset[0] for block in self.blocks]
        z_offsets = [block.offset[2] for block in self.blocks]
        actual_width = max(x_offsets) - min(x_offsets) + 1
        actual_depth = max(z_offsets) - min(z_offsets) + 1
        if actual_width > declared_width or actual_depth > declared_depth:
            raise ValueError(
                "fragment blocks exceed declared footprint: "
                f"actual={actual_width}x{actual_depth}, "
                f"declared={declared_width}x{declared_depth}"
            )

        # Repeated offsets are intentional ordered layering. FragmentEngine
        # applies blocks in declaration order, so a later successful write may
        # replace an earlier clear or dressing operation at the same offset.
        return self
