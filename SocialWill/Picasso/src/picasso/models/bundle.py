from __future__ import annotations

from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


NonEmptyStr = Annotated[str, Field(min_length=1)]


class _BundleModel(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
        str_strip_whitespace=True,
    )


class BundlePass(_BundleModel):
    name: NonEmptyStr
    intensity: float = Field(default=1.0, ge=0.0, le=1.0)
    space_filter: Literal["interior", "exterior"] | None = None


class BundleEntry(_BundleModel):
    structure_type: NonEmptyStr
    description: NonEmptyStr | None = None
    passes: list[BundlePass] = Field(min_length=1)

    @model_validator(mode="after")
    def reject_duplicate_passes(self) -> BundleEntry:
        pass_names = [pass_entry.name for pass_entry in self.passes]
        duplicate = _first_duplicate(pass_names)
        if duplicate is not None:
            raise ValueError(
                f"duplicate pass {duplicate!r} in structure_type {self.structure_type!r}"
            )
        return self


class Bundle(_BundleModel):
    name: NonEmptyStr
    description: NonEmptyStr
    version: NonEmptyStr = "1.0"
    default_seed: int = 42
    entries: list[BundleEntry] = Field(min_length=1)

    @model_validator(mode="after")
    def reject_duplicate_structure_types(self) -> Bundle:
        structure_types = [entry.structure_type for entry in self.entries]
        duplicate = _first_duplicate(structure_types)
        if duplicate is not None:
            raise ValueError(f"duplicate structure_type: {duplicate!r}")
        return self

    def to_registry_dict(self) -> dict:
        return self.model_dump(mode="json", exclude_none=True)


def _first_duplicate(values: list[str]) -> str | None:
    seen: set[str] = set()
    for value in values:
        if value in seen:
            return value
        seen.add(value)
    return None
