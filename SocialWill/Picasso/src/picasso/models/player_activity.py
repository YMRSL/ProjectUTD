from __future__ import annotations

from collections.abc import Mapping, Sequence
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from typing import Any, Literal

from picasso.models.block import BlockPos


ActivityAction = Literal["place", "break"]
ProtectionAreaKind = Literal["player_built", "player_modified", "protected_region"]
ProtectionStatus = Literal["active", "unavailable"]


def _require_int(value: object, field_name: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise ValueError(f"{field_name} must be an integer")
    return value


def _require_non_empty_string(value: object, field_name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{field_name} must be a non-empty string")
    return value


def _require_namespaced_id(value: object, field_name: str) -> str:
    identifier = _require_non_empty_string(value, field_name)
    namespace, separator, name = identifier.partition(":")
    if not separator or not namespace or not name:
        raise ValueError(f"{field_name} must be namespaced")
    return identifier


def parse_utc_datetime(value: object, field_name: str = "timestamp") -> datetime:
    if isinstance(value, datetime):
        parsed = value
    elif isinstance(value, str):
        candidate = value[:-1] + "+00:00" if value.endswith(("Z", "z")) else value
        try:
            parsed = datetime.fromisoformat(candidate)
        except ValueError as exc:
            raise ValueError(f"{field_name} must be an ISO-8601 UTC timestamp") from exc
    else:
        raise ValueError(f"{field_name} must be an ISO-8601 UTC timestamp")

    offset = parsed.utcoffset()
    if parsed.tzinfo is None or offset is None or offset != timedelta(0):
        raise ValueError(f"{field_name} must use UTC")
    return parsed.astimezone(timezone.utc)


def format_utc_datetime(value: datetime) -> str:
    parsed = parse_utc_datetime(value)
    if parsed.microsecond:
        precision = "milliseconds" if parsed.microsecond % 1000 == 0 else "microseconds"
        rendered = parsed.isoformat(timespec=precision)
    else:
        rendered = parsed.isoformat(timespec="seconds")
    return rendered.replace("+00:00", "Z")


def block_pos_from_record(value: object, field_name: str = "pos") -> BlockPos:
    if not isinstance(value, Mapping):
        raise ValueError(f"{field_name} must be an object")
    try:
        return BlockPos(
            _require_int(value["x"], f"{field_name}.x"),
            _require_int(value["y"], f"{field_name}.y"),
            _require_int(value["z"], f"{field_name}.z"),
        )
    except KeyError as exc:
        raise ValueError(f"{field_name} is missing {exc.args[0]}") from exc


@dataclass(frozen=True)
class BlockBounds:
    """Inclusive block-coordinate bounds used by activity protection."""

    x_min: int
    x_max: int
    y_min: int
    y_max: int
    z_min: int
    z_max: int

    def __post_init__(self) -> None:
        for name in ("x_min", "x_max", "y_min", "y_max", "z_min", "z_max"):
            _require_int(getattr(self, name), name)
        if self.x_min > self.x_max:
            raise ValueError("x_min must be <= x_max")
        if self.y_min > self.y_max:
            raise ValueError("y_min must be <= y_max")
        if self.z_min > self.z_max:
            raise ValueError("z_min must be <= z_max")

    @classmethod
    def from_record(cls, value: object, field_name: str = "bounds") -> "BlockBounds":
        if not isinstance(value, Mapping):
            raise ValueError(f"{field_name} must be an object")
        names = ("x_min", "x_max", "y_min", "y_max", "z_min", "z_max")
        try:
            values = [_require_int(value[name], f"{field_name}.{name}") for name in names]
        except KeyError as exc:
            raise ValueError(f"{field_name} is missing {exc.args[0]}") from exc
        return cls(*values)

    @classmethod
    def from_positions(cls, positions: Sequence[BlockPos]) -> "BlockBounds":
        if not positions:
            raise ValueError("positions must not be empty")
        return cls(
            x_min=min(pos.x for pos in positions),
            x_max=max(pos.x for pos in positions),
            y_min=min(pos.y for pos in positions),
            y_max=max(pos.y for pos in positions),
            z_min=min(pos.z for pos in positions),
            z_max=max(pos.z for pos in positions),
        )

    def include(self, pos: BlockPos) -> "BlockBounds":
        return BlockBounds(
            min(self.x_min, pos.x),
            max(self.x_max, pos.x),
            min(self.y_min, pos.y),
            max(self.y_max, pos.y),
            min(self.z_min, pos.z),
            max(self.z_max, pos.z),
        )

    def contains(self, pos: BlockPos) -> bool:
        return (
            self.x_min <= pos.x <= self.x_max
            and self.y_min <= pos.y <= self.y_max
            and self.z_min <= pos.z <= self.z_max
        )

    def intersects(self, other: "BlockBounds") -> bool:
        return not (
            self.x_max < other.x_min
            or other.x_max < self.x_min
            or self.y_max < other.y_min
            or other.y_max < self.y_min
            or self.z_max < other.z_min
            or other.z_max < self.z_min
        )

    def horizontal_distance_squared(self, pos: BlockPos) -> int:
        dx = max(self.x_min - pos.x, 0, pos.x - self.x_max)
        dz = max(self.z_min - pos.z, 0, pos.z - self.z_max)
        return dx * dx + dz * dz

    def to_dict(self) -> dict[str, int]:
        return {
            "x_min": self.x_min,
            "x_max": self.x_max,
            "y_min": self.y_min,
            "y_max": self.y_max,
            "z_min": self.z_min,
            "z_max": self.z_max,
        }


@dataclass(frozen=True)
class BuildEvent:
    timestamp: datetime
    player: str
    action: ActivityAction
    pos: BlockPos
    block: str
    dimension: str = "minecraft:overworld"
    schema_version: int = 1

    def __post_init__(self) -> None:
        object.__setattr__(self, "timestamp", parse_utc_datetime(self.timestamp, "t"))
        _require_non_empty_string(self.player, "player")
        if self.action not in ("place", "break"):
            raise ValueError("action must be 'place' or 'break'")
        if not isinstance(self.pos, BlockPos):
            raise ValueError("pos must be a BlockPos")
        _require_int(self.pos.x, "pos.x")
        _require_int(self.pos.y, "pos.y")
        _require_int(self.pos.z, "pos.z")
        _require_namespaced_id(self.block, "block")
        _require_namespaced_id(self.dimension, "dim")
        if _require_int(self.schema_version, "v") != 1:
            raise ValueError(f"unsupported build-event schema major: {self.schema_version}")

    @classmethod
    def from_record(cls, value: object) -> "BuildEvent":
        if not isinstance(value, Mapping):
            raise ValueError("build event must be an object")
        required = ("v", "t", "player", "action", "pos", "block", "dim")
        missing = [name for name in required if name not in value]
        if missing:
            raise ValueError(f"build event is missing {missing[0]}")
        return cls(
            timestamp=parse_utc_datetime(value["t"], "t"),
            player=_require_non_empty_string(value["player"], "player"),
            action=value["action"],
            pos=block_pos_from_record(value["pos"]),
            block=_require_namespaced_id(value["block"], "block"),
            dimension=_require_namespaced_id(value["dim"], "dim"),
            schema_version=_require_int(value["v"], "v"),
        )

    @property
    def sort_key(self) -> tuple[Any, ...]:
        return (
            self.timestamp,
            self.pos.x,
            self.pos.y,
            self.pos.z,
            self.action,
            self.player,
            self.block,
            self.dimension,
        )

    def to_dict(self) -> dict[str, Any]:
        return {
            "v": self.schema_version,
            "t": format_utc_datetime(self.timestamp),
            "player": self.player,
            "action": self.action,
            "pos": self.pos.to_dict(),
            "block": self.block,
            "dim": self.dimension,
        }


@dataclass(frozen=True)
class PlayerEventCounts:
    player: str
    places: int
    breaks: int

    def __post_init__(self) -> None:
        _require_non_empty_string(self.player, "player")
        if _require_int(self.places, "places") < 0:
            raise ValueError("places must be >= 0")
        if _require_int(self.breaks, "breaks") < 0:
            raise ValueError("breaks must be >= 0")


@dataclass(frozen=True)
class PaletteCount:
    block: str
    count: int

    def __post_init__(self) -> None:
        _require_namespaced_id(self.block, "block")
        if _require_int(self.count, "count") < 1:
            raise ValueError("count must be >= 1")


@dataclass(frozen=True)
class ActivitySite:
    site_id: str
    dimension: str
    bounds: BlockBounds
    first_event: datetime
    last_event: datetime
    players: tuple[PlayerEventCounts, ...]
    top_palette: tuple[PaletteCount, ...]
    event_count: int
    net_blocks: int
    character: Literal["construction", "excavation", "mixed"]

    def __post_init__(self) -> None:
        _require_non_empty_string(self.site_id, "site_id")
        _require_namespaced_id(self.dimension, "dimension")
        object.__setattr__(self, "first_event", parse_utc_datetime(self.first_event, "first_event"))
        object.__setattr__(self, "last_event", parse_utc_datetime(self.last_event, "last_event"))
        if self.last_event < self.first_event:
            raise ValueError("last_event must be >= first_event")
        if _require_int(self.event_count, "event_count") < 1:
            raise ValueError("event_count must be >= 1")
        _require_int(self.net_blocks, "net_blocks")
        if self.character not in ("construction", "excavation", "mixed"):
            raise ValueError("invalid activity-site character")
        if sum(item.places + item.breaks for item in self.players) != self.event_count:
            raise ValueError("player counts must sum to event_count")

    def to_dict(self) -> dict[str, Any]:
        return {
            "site_id": self.site_id,
            "dimension": self.dimension,
            "bounds": self.bounds.to_dict(),
            "first_event": format_utc_datetime(self.first_event),
            "last_event": format_utc_datetime(self.last_event),
            "players": {
                item.player: {"places": item.places, "breaks": item.breaks}
                for item in self.players
            },
            "top_palette": [
                {"block": item.block, "count": item.count} for item in self.top_palette
            ],
            "event_count": self.event_count,
            "net_blocks": self.net_blocks,
            "character": self.character,
        }


@dataclass(frozen=True)
class ProtectionArea:
    area_id: str
    kind: ProtectionAreaKind
    dimension: str = "minecraft:overworld"
    bounds: BlockBounds | None = None
    positions: frozenset[BlockPos] = field(default_factory=frozenset)
    source: str = "structure_registry"

    def __post_init__(self) -> None:
        _require_non_empty_string(self.area_id, "id")
        if self.kind not in ("player_built", "player_modified", "protected_region"):
            raise ValueError("invalid protection area kind")
        _require_namespaced_id(self.dimension, "dimension")
        _require_non_empty_string(self.source, "source")
        if self.bounds is not None and not isinstance(self.bounds, BlockBounds):
            raise ValueError("bounds must be a BlockBounds")
        if not isinstance(self.positions, frozenset):
            object.__setattr__(self, "positions", frozenset(self.positions))
        if any(not isinstance(pos, BlockPos) for pos in self.positions):
            raise ValueError("positions must contain BlockPos values")
        if self.kind == "player_modified":
            if not self.positions:
                raise ValueError("player_modified protection requires attributed positions")
            if self.bounds is not None and any(
                not self.bounds.contains(pos) for pos in self.positions
            ):
                raise ValueError("attributed positions must be inside bounds")
        elif self.bounds is None:
            raise ValueError(f"{self.kind} protection requires bounds")

    @classmethod
    def from_record(cls, value: object) -> "ProtectionArea":
        if not isinstance(value, Mapping):
            raise ValueError("protection area must be an object")
        for required in ("id", "kind"):
            if required not in value:
                raise ValueError(f"protection area is missing {required}")
        bounds_value = value.get("bounds")
        bounds = BlockBounds.from_record(bounds_value) if bounds_value is not None else None
        positions_value = value.get("positions", ())
        if isinstance(positions_value, (str, bytes)) or not isinstance(
            positions_value, Sequence
        ):
            raise ValueError("positions must be an array")
        positions = frozenset(
            block_pos_from_record(item, f"positions[{index}]")
            for index, item in enumerate(positions_value)
        )
        return cls(
            area_id=_require_non_empty_string(value["id"], "id"),
            kind=value["kind"],
            dimension=_require_namespaced_id(
                value.get("dimension", "minecraft:overworld"), "dimension"
            ),
            bounds=bounds,
            positions=positions,
            source=_require_non_empty_string(
                value.get("source", "structure_registry"), "source"
            ),
        )

    def contains(self, pos: BlockPos) -> bool:
        if self.kind == "player_modified":
            return pos in self.positions
        return self.bounds is not None and self.bounds.contains(pos)

    def intersects(self, bounds: BlockBounds) -> bool:
        if self.kind == "player_modified":
            return any(bounds.contains(pos) for pos in self.positions)
        return self.bounds is not None and self.bounds.intersects(bounds)

    def to_dict(self) -> dict[str, Any]:
        result: dict[str, Any] = {
            "id": self.area_id,
            "kind": self.kind,
            "dimension": self.dimension,
            "source": self.source,
        }
        if self.bounds is not None:
            result["bounds"] = self.bounds.to_dict()
        if self.positions:
            result["positions"] = [pos.to_dict() for pos in sorted(self.positions)]
        return result


@dataclass(frozen=True)
class ProtectionProvenance:
    source: str
    reason: str
    source_id: str | None = None
    dimension: str | None = None
    bounds: BlockBounds | None = None
    event_count: int | None = None
    players: tuple[str, ...] = ()
    first_event: datetime | None = None
    last_event: datetime | None = None
    position_count: int | None = None
    detail: str | None = None

    def to_dict(self) -> dict[str, Any]:
        result: dict[str, Any] = {"source": self.source, "reason": self.reason}
        if self.source_id is not None:
            result["source_id"] = self.source_id
        if self.dimension is not None:
            result["dimension"] = self.dimension
        if self.bounds is not None:
            result["bounds"] = self.bounds.to_dict()
        if self.event_count is not None:
            result["event_count"] = self.event_count
        if self.players:
            result["players"] = list(self.players)
        if self.first_event is not None:
            result["first_event"] = format_utc_datetime(self.first_event)
        if self.last_event is not None:
            result["last_event"] = format_utc_datetime(self.last_event)
        if self.position_count is not None:
            result["position_count"] = self.position_count
        if self.detail is not None:
            result["detail"] = self.detail
        return result


@dataclass(frozen=True)
class ProtectionDecision:
    protected: bool
    reason: str
    status: ProtectionStatus
    provenance: tuple[ProtectionProvenance, ...] = ()

    @property
    def available(self) -> bool:
        return self.status == "active"

    @property
    def active(self) -> bool:
        return self.status == "active"

    def to_dict(self) -> dict[str, Any]:
        return {
            "protected": self.protected,
            "reason": self.reason,
            "status": self.status,
            "provenance": [item.to_dict() for item in self.provenance],
        }
