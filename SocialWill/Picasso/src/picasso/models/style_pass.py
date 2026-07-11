from __future__ import annotations

import re
from collections.abc import Mapping
from typing import Annotated, Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


_BLOCK_ID_RE = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")
_NAMESPACE_RE = re.compile(r"^[a-z0-9_.-]+$")
_MAPPING_KEYS = {"pattern", "dd_block", "note"}
_MATCH_KEYS = {
    "block",
    "namespace",
    "name_contains",
    "surface",
    "adjacent_air",
    "y_min",
    "y_max",
}
_MATCH_SURFACES = {"floor", "outer_wall", "inner_wall", "ceiling", "rooftop"}
NonEmptyStr = Annotated[str, Field(min_length=1)]


class _DefinitionModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


def _validated_block_id(value: str, field_name: str = "block") -> str:
    if not _BLOCK_ID_RE.fullmatch(value):
        raise ValueError(
            f"{field_name} must be a canonical namespaced block id "
            "(for example, 'minecraft:stone')"
        )
    return value


class NoiseConfig(_DefinitionModel):
    type: Literal["perlin"] = "perlin"
    scale: float = Field(default=0.05, gt=0.0)
    threshold: float = Field(default=0.4, ge=0.0, le=1.0)


class ReplaceOption(_DefinitionModel):
    block: str
    properties: dict[NonEmptyStr, NonEmptyStr] = Field(default_factory=dict)
    weight: float = Field(default=1.0, ge=0.0)

    @field_validator("block")
    @classmethod
    def validate_block(cls, value: str) -> str:
        return _validated_block_id(value)


class StyleRule(_DefinitionModel):
    match: dict[str, Any]
    action: Literal["replace", "place_adjacent", "remove"]
    replace_with: list[ReplaceOption] | None = None
    place_block: str | None = None
    direction: Literal["air_side", "above", "below"] | None = None
    weight: float = Field(default=1.0, ge=0.0, le=1.0)
    noise: NoiseConfig | None = None
    description: NonEmptyStr | None = None

    @field_validator("place_block")
    @classmethod
    def validate_place_block(cls, value: str | None) -> str | None:
        return _validated_block_id(value, "place_block") if value is not None else None

    @model_validator(mode="after")
    def validate_action_fields(self) -> StyleRule:
        self._validate_match()

        if self.action == "replace":
            if not self.replace_with:
                raise ValueError("replace action requires non-empty replace_with")
            if self.place_block is not None or self.direction is not None:
                raise ValueError(
                    "replace action cannot define place_block or direction"
                )
            if not any(option.weight > 0.0 for option in self.replace_with):
                raise ValueError(
                    "replace action requires at least one positive replacement weight"
                )
        elif self.action == "place_adjacent":
            if self.place_block is None or self.direction is None:
                raise ValueError(
                    "place_adjacent action requires place_block and direction"
                )
            if self.replace_with is not None:
                raise ValueError("place_adjacent action cannot define replace_with")
        else:
            if (
                self.replace_with is not None
                or self.place_block is not None
                or self.direction is not None
            ):
                raise ValueError(
                    "remove action cannot define replace_with, place_block, or direction"
                )
        return self

    def _validate_match(self) -> None:
        unknown_keys = set(self.match) - _MATCH_KEYS
        if unknown_keys:
            raise ValueError(
                "match has unsupported fields: "
                f"{', '.join(sorted(unknown_keys))}"
            )

        if "block" not in self.match:
            self._validate_other_match_fields()
            return
        block = self.match["block"]
        if isinstance(block, str):
            block = block.strip()
            self.match["block"] = _validated_block_id(block, "match.block")
        elif isinstance(block, list) and block and all(
            isinstance(item, str) for item in block
        ):
            normalized = [
                _validated_block_id(item.strip(), "match.block") for item in block
            ]
            self.match["block"] = normalized
        else:
            raise ValueError(
                "match.block must be a block id or a non-empty list of block ids"
            )
        self._validate_other_match_fields()

    def _validate_other_match_fields(self) -> None:
        if "namespace" in self.match:
            namespace = self.match["namespace"]
            namespace = _stripped_non_empty(namespace, "match.namespace")
            if not _NAMESPACE_RE.fullmatch(namespace):
                raise ValueError("match.namespace must be a canonical namespace")
            self.match["namespace"] = namespace

        if "name_contains" in self.match:
            contains = self.match["name_contains"]
            self.match["name_contains"] = _stripped_non_empty(
                contains, "match.name_contains"
            )

        if "surface" in self.match:
            surface = self.match["surface"]
            if isinstance(surface, str):
                normalized_surface = surface.strip()
                if normalized_surface not in _MATCH_SURFACES:
                    raise ValueError("match.surface contains an unsupported surface")
                self.match["surface"] = normalized_surface
            elif isinstance(surface, list) and surface and all(
                isinstance(item, str) for item in surface
            ):
                normalized_surfaces = [item.strip() for item in surface]
                if any(item not in _MATCH_SURFACES for item in normalized_surfaces):
                    raise ValueError("match.surface contains an unsupported surface")
                self.match["surface"] = normalized_surfaces
            else:
                raise ValueError(
                    "match.surface must be a surface or a non-empty list of surfaces"
                )

        if "adjacent_air" in self.match:
            adjacent_air = self.match["adjacent_air"]
            if type(adjacent_air) is not bool:
                raise ValueError("match.adjacent_air must be a boolean")

        for field_name in ("y_min", "y_max"):
            if field_name in self.match:
                value = self.match[field_name]
                if type(value) is not int:
                    raise ValueError(f"match.{field_name} must be an integer")
        if (
            "y_min" in self.match
            and "y_max" in self.match
            and self.match["y_min"] > self.match["y_max"]
        ):
            raise ValueError("match.y_min must be less than or equal to match.y_max")


class StylePass(_DefinitionModel):
    name: NonEmptyStr
    description: NonEmptyStr
    version: NonEmptyStr = "1.0"
    deprecated: bool = False
    targets: list[NonEmptyStr] = Field(default_factory=list)
    rules: list[StyleRule] = Field(default_factory=list)
    only_safe_blocks: bool = True
    destructive: bool = False
    type: Literal["block_pass", "fragment_pass", "pattern_replace"] = "block_pass"
    mappings: list[dict[str, Any]] = Field(default_factory=list)
    fragments: list[NonEmptyStr] = Field(default_factory=list)
    anchor_surface: Literal[
        "floor", "outer_wall", "inner_wall", "ceiling", "rooftop", "any"
    ] | None = None
    density: float = Field(default=0.0, ge=0.0, le=1.0)
    noise: NoiseConfig | None = None
    min_spacing: int = Field(default=0, ge=0)
    only_safe_anchor_blocks: bool = True

    @model_validator(mode="before")
    @classmethod
    def require_type_specific_fields(cls, data: Any) -> Any:
        if not isinstance(data, Mapping):
            return data
        pass_type = data.get("type", "block_pass")
        if pass_type == "block_pass":
            if "rules" not in data:
                raise ValueError("block_pass requires rules")
        elif pass_type == "fragment_pass":
            if not data.get("fragments"):
                raise ValueError("fragment_pass requires non-empty fragments")
            if not data.get("anchor_surface"):
                raise ValueError("fragment_pass requires anchor_surface")
            if "density" not in data:
                raise ValueError("fragment_pass requires density")
        elif pass_type == "pattern_replace":
            if not data.get("mappings"):
                raise ValueError("pattern_replace requires non-empty mappings")
        return data

    @model_validator(mode="after")
    def validate_pass_contract(self) -> StylePass:
        if self.type == "block_pass":
            if not self.rules:
                raise ValueError("block_pass requires non-empty rules")
            if self.fragments or self.mappings:
                raise ValueError(
                    "block_pass cannot define fragments or mappings"
                )
        elif self.type == "fragment_pass":
            if self.rules or self.mappings:
                raise ValueError(
                    "fragment_pass cannot define rules or mappings"
                )
            if not self.fragments:
                raise ValueError("fragment_pass requires non-empty fragments")
        else:
            if self.rules or self.fragments:
                raise ValueError(
                    "pattern_replace cannot define rules or fragments"
                )
            self._validate_mappings()

        if not self.destructive and any(
            _rule_can_write_air(rule) for rule in self.rules
        ):
            raise ValueError(
                "remove rules and AIR writes require destructive=true"
            )
        return self

    def _validate_mappings(self) -> None:
        seen_patterns: set[str] = set()
        for index, mapping in enumerate(self.mappings):
            unknown_keys = set(mapping) - _MAPPING_KEYS
            if unknown_keys:
                raise ValueError(
                    f"mappings[{index}] has unsupported fields: "
                    f"{', '.join(sorted(unknown_keys))}"
                )
            pattern = mapping.get("pattern")
            dd_block = mapping.get("dd_block")
            if not isinstance(pattern, str) or not pattern.strip():
                raise ValueError(f"mappings[{index}].pattern must be a non-empty string")
            pattern = pattern.strip()
            mapping["pattern"] = pattern
            if pattern in seen_patterns:
                raise ValueError(f"duplicate pattern mapping: {pattern}")
            seen_patterns.add(pattern)
            if not isinstance(dd_block, str):
                raise ValueError(f"mappings[{index}].dd_block must be a string")
            dd_block = dd_block.strip()
            mapping["dd_block"] = dd_block
            _validated_block_id(dd_block, f"mappings[{index}].dd_block")
            if "note" in mapping:
                mapping["note"] = _stripped_non_empty(
                    mapping["note"], f"mappings[{index}].note"
                )


def _stripped_non_empty(value: Any, field_name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field_name} must be a non-empty string")
    return value.strip()


def _rule_can_write_air(rule: StyleRule) -> bool:
    if rule.action == "remove" or rule.place_block == "minecraft:air":
        return True
    return any(
        option.block == "minecraft:air" for option in (rule.replace_with or [])
    )
