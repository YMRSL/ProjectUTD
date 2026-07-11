from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from types import MappingProxyType
from typing import Literal, Mapping, TypedDict

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


VANILLA_RAIL_AUDIT_BASIS: Mapping[str, str | int] = MappingProxyType(
    {
        "minecraft": "1.21.1",
        "data_version": 3955,
        "world_min_y": -64,
        "world_max_y": 319,
    }
)
_WORLD_MIN_Y = -64
_WORLD_MAX_Y = 319

RailType = Literal["rail", "powered_rail", "detector_rail", "activator_rail"]
RailShape = Literal[
    "north_south",
    "east_west",
    "ascending_east",
    "ascending_west",
    "ascending_north",
    "ascending_south",
    "south_east",
    "south_west",
    "north_west",
    "north_east",
]
PortDirection = Literal["north", "south", "east", "west"]
DiagnosticSeverity = Literal["error"]


class RailDiagnostic(TypedDict):
    code: str
    severity: DiagnosticSeverity
    property: str | None
    value: str | None


class DoubledRailPort(TypedDict):
    x2: int
    y2: int
    z2: int
    direction: PortDirection


class RailNode(TypedDict):
    pos: dict[str, int]
    id: str
    rail_type: RailType
    shape: str | None
    powered: bool | None
    waterlogged: bool | None
    properties: dict[str, str]
    ports: list[DoubledRailPort]
    partial: bool
    diagnostics: list[RailDiagnostic]


class RailEdge(TypedDict):
    a: dict[str, int]
    b: dict[str, int]
    port: dict[str, int]


class RailTerminal(TypedDict):
    pos: dict[str, int]
    port: DoubledRailPort


class RailComponent(TypedDict):
    component_id: str
    nodes: list[dict[str, int]]
    edge_count: int
    terminals: list[RailTerminal]
    partial: bool


class RailGraphBlocker(TypedDict):
    code: str
    pos: dict[str, int] | None
    port: DoubledRailPort | None
    detail: str


class VanillaRailGraphAnalysis(TypedDict):
    nodes: list[RailNode]
    edges: list[RailEdge]
    components: list[RailComponent]
    terminals: list[RailTerminal]
    partial: bool
    blocking: bool
    blockers: list[RailGraphBlocker]


_RAIL_TYPES: dict[str, RailType] = {
    "minecraft:rail": "rail",
    "minecraft:powered_rail": "powered_rail",
    "minecraft:detector_rail": "detector_rail",
    "minecraft:activator_rail": "activator_rail",
}
_STRAIGHT_SHAPES = frozenset(
    {
        "north_south",
        "east_west",
        "ascending_east",
        "ascending_west",
        "ascending_north",
        "ascending_south",
    }
)
_CORNER_SHAPES = frozenset(
    {"south_east", "south_west", "north_west", "north_east"}
)
_ALL_RAIL_SHAPES = _STRAIGHT_SHAPES | _CORNER_SHAPES
_OPPOSITE: dict[PortDirection, PortDirection] = {
    "north": "south",
    "south": "north",
    "east": "west",
    "west": "east",
}
_DIRECTION_OFFSET: dict[PortDirection, tuple[int, int]] = {
    "north": (0, -1),
    "south": (0, 1),
    "east": (1, 0),
    "west": (-1, 0),
}


@dataclass(frozen=True, order=True)
class _Port:
    x2: int
    y2: int
    z2: int
    direction: PortDirection

    @property
    def coordinate(self) -> tuple[int, int, int]:
        return self.x2, self.y2, self.z2

    def to_dict(self) -> DoubledRailPort:
        return {
            "x2": self.x2,
            "y2": self.y2,
            "z2": self.z2,
            "direction": self.direction,
        }


@dataclass
class _RailRecord:
    pos: BlockPos
    state: BlockState
    rail_type: RailType
    core: bool
    shape: str | None
    powered: bool | None
    waterlogged: bool | None
    ports: tuple[_Port, ...]
    diagnostics: list[RailDiagnostic]
    partial: bool = False


def analyze_vanilla_rail_graph(region: RegionData) -> VanillaRailGraphAnalysis:
    """Analyze core vanilla rails using exact half-block boundary geometry.

    Coordinates in each port are doubled block coordinates. Flat rail ports sit
    at ``2*y``; an ascending rail's high port sits at ``2*y + 2``. This makes a
    slope's high end equal the boundary port of a flat rail one block above.

    Context and halo rails participate only in connection evidence. They are
    never returned as nodes or edges and any core-to-context continuation makes
    the result partial and blocking. A scan with no core rails is a valid empty
    graph shape but returns a blocking ``no_core_rails`` diagnostic so callers
    cannot mistake it for an authorized conversion source.
    """
    if not isinstance(region, RegionData):
        raise TypeError("region must be a RegionData")

    records = _parse_records(region)
    core_records = {pos: record for pos, record in records.items() if record.core}
    blockers: list[RailGraphBlocker] = []

    if not core_records:
        blockers.append(
            _blocker(
                "no_core_rails",
                detail="the target region contains no supported vanilla rail nodes",
            )
        )

    for record in core_records.values():
        if record.diagnostics:
            record.partial = True
            for diagnostic in record.diagnostics:
                blockers.append(
                    _blocker(
                        diagnostic["code"],
                        record.pos,
                        detail=_diagnostic_detail(diagnostic),
                    )
                )
        if record.rail_type != "rail":
            blockers.append(
                _blocker(
                    "special_rail_semantic_loss",
                    record.pos,
                    detail=(
                        f"{record.state.full_id} has typed behavior and powered state; "
                        "it cannot be downgraded to an ordinary rail"
                    ),
                )
            )
        if record.waterlogged is True:
            blockers.append(
                _blocker(
                    "liquid_source_semantic_loss",
                    record.pos,
                    detail=(
                        "waterlogged=true carries liquid/source semantics that "
                        "cannot be discarded during rail conversion"
                    ),
                )
            )

    port_index: dict[tuple[int, int, int], list[tuple[_RailRecord, _Port]]] = (
        defaultdict(list)
    )
    for record in records.values():
        for port in record.ports:
            port_index[port.coordinate].append((record, port))

    matched_core_ports: set[tuple[BlockPos, tuple[int, int, int]]] = set()
    edge_keys: set[tuple[BlockPos, BlockPos, tuple[int, int, int]]] = set()

    for coordinate in sorted(port_index):
        entries = sorted(
            port_index[coordinate],
            key=lambda item: (item[0].pos, item[1].direction),
        )
        core_entries = [entry for entry in entries if entry[0].core]
        if not core_entries:
            continue
        if len(entries) == 1:
            # An unmatched port is not ambiguous. It becomes a terminal only
            # after the read-envelope checks below prove that no continuation
            # could exist outside the available evidence.
            continue
        if len(entries) != 2 or entries[0][1].direction != _OPPOSITE[entries[1][1].direction]:
            for record, port in core_entries:
                record.partial = True
                matched_core_ports.add((record.pos, coordinate))
                blockers.append(
                    _blocker(
                        "ambiguous_port_geometry",
                        record.pos,
                        port,
                        f"{len(entries)} rail ports meet at one boundary coordinate",
                    )
                )
            continue

        (first, first_port), (second, second_port) = entries
        for record, port in ((first, first_port), (second, second_port)):
            if record.core:
                matched_core_ports.add((record.pos, coordinate))

        if first.core and second.core:
            a, b = sorted((first.pos, second.pos))
            edge_keys.add((a, b, coordinate))
            continue

        core_record, core_port = (first, first_port) if first.core else (second, second_port)
        context_record = second if first.core else first
        core_record.partial = True
        context_kind = (
            "halo" if context_record.pos in region.halo_positions else "context"
        )
        blockers.append(
            _blocker(
                f"{context_kind}_continuation",
                core_record.pos,
                core_port,
                f"rail continues to non-core position {context_record.pos.to_dict()}",
            )
        )

    terminals: list[RailTerminal] = []
    for record in core_records.values():
        for port in sorted(record.ports):
            column_blockers = _port_column_blockers(
                region,
                records,
                record,
                port,
            )
            if column_blockers:
                record.partial = True
                blockers.extend(column_blockers)
            marker = (record.pos, port.coordinate)
            if marker in matched_core_ports:
                continue
            if column_blockers:
                continue
            terminals.append({"pos": record.pos.to_dict(), "port": port.to_dict()})

    edges = [
        {
            "a": a.to_dict(),
            "b": b.to_dict(),
            "port": {"x2": coordinate[0], "y2": coordinate[1], "z2": coordinate[2]},
        }
        for a, b, coordinate in sorted(edge_keys)
    ]
    terminals.sort(key=_terminal_sort_key)
    components = _build_components(core_records, edge_keys, terminals)
    if len(components) > 1:
        blockers.append(
            _blocker(
                "multiple_core_components",
                detail=f"found {len(components)} disconnected core rail components",
            )
        )

    blockers.sort(key=_blocker_sort_key)
    nodes = [_node_to_dict(core_records[pos]) for pos in sorted(core_records)]
    partial = any(record.partial for record in core_records.values())
    return {
        "nodes": nodes,
        "edges": edges,
        "components": components,
        "terminals": terminals,
        "partial": partial,
        "blocking": bool(blockers),
        "blockers": blockers,
    }


def _parse_records(region: RegionData) -> dict[BlockPos, _RailRecord]:
    records: dict[BlockPos, _RailRecord] = {}
    for pos, state in sorted(region.blocks.items()):
        rail_type = _RAIL_TYPES.get(state.full_id)
        if rail_type is None:
            continue
        diagnostics: list[RailDiagnostic] = []
        shape = _read_shape(state, rail_type, diagnostics)
        if rail_type == "rail":
            powered = None
        else:
            powered = _read_bool_property(state, "powered", diagnostics)
        waterlogged = _read_optional_bool_property(state, "waterlogged", diagnostics)
        ports = _ports_for(pos, shape)
        if not (_WORLD_MIN_Y <= pos.y <= _WORLD_MAX_Y) or any(
            not (_WORLD_MIN_Y <= port.y2 // 2 <= _WORLD_MAX_Y) for port in ports
        ):
            diagnostics.append(
                _diagnostic("invalid_world_height_geometry", "shape", shape)
            )
            ports = ()
        records[pos] = _RailRecord(
            pos=pos,
            state=state,
            rail_type=rail_type,
            core=region.is_target_position(pos),
            shape=shape,
            powered=powered,
            waterlogged=waterlogged,
            ports=ports,
            diagnostics=diagnostics,
        )
    return records


def _read_shape(
    state: BlockState,
    rail_type: RailType,
    diagnostics: list[RailDiagnostic],
) -> str | None:
    raw = state.properties.get("shape")
    if raw is None:
        diagnostics.append(_diagnostic("missing_property", "shape"))
        return None
    allowed = _ALL_RAIL_SHAPES if rail_type == "rail" else _STRAIGHT_SHAPES
    if not isinstance(raw, str) or raw not in allowed:
        diagnostics.append(_diagnostic("invalid_property", "shape", raw))
        return raw if isinstance(raw, str) else None
    return raw


def _read_bool_property(
    state: BlockState,
    name: str,
    diagnostics: list[RailDiagnostic],
) -> bool | None:
    raw = state.properties.get(name)
    if raw is None:
        diagnostics.append(_diagnostic("missing_property", name))
        return None
    if raw == "true":
        return True
    if raw == "false":
        return False
    diagnostics.append(_diagnostic("invalid_property", name, raw))
    return None


def _read_optional_bool_property(
    state: BlockState,
    name: str,
    diagnostics: list[RailDiagnostic],
) -> bool | None:
    if name not in state.properties:
        return None
    return _read_bool_property(state, name, diagnostics)


def _ports_for(pos: BlockPos, shape: str | None) -> tuple[_Port, ...]:
    x2, y2, z2 = pos.x * 2, pos.y * 2, pos.z * 2
    flat = {
        "north": _Port(x2, y2, z2 - 1, "north"),
        "south": _Port(x2, y2, z2 + 1, "south"),
        "east": _Port(x2 + 1, y2, z2, "east"),
        "west": _Port(x2 - 1, y2, z2, "west"),
    }
    ports: dict[str, tuple[_Port, ...]] = {
        "north_south": (flat["north"], flat["south"]),
        "east_west": (flat["west"], flat["east"]),
        "ascending_east": (
            flat["west"],
            _Port(x2 + 1, y2 + 2, z2, "east"),
        ),
        "ascending_west": (
            _Port(x2 - 1, y2 + 2, z2, "west"),
            flat["east"],
        ),
        "ascending_north": (
            _Port(x2, y2 + 2, z2 - 1, "north"),
            flat["south"],
        ),
        "ascending_south": (
            flat["north"],
            _Port(x2, y2 + 2, z2 + 1, "south"),
        ),
        "south_east": (flat["south"], flat["east"]),
        "south_west": (flat["south"], flat["west"]),
        "north_west": (flat["north"], flat["west"]),
        "north_east": (flat["north"], flat["east"]),
    }
    return ports.get(shape, ())


def _port_column_blockers(
    region: RegionData,
    records: dict[BlockPos, _RailRecord],
    record: _RailRecord,
    port: _Port,
) -> list[RailGraphBlocker]:
    dx, dz = _DIRECTION_OFFSET[port.direction]
    neighbour_x = record.pos.x + dx
    neighbour_z = record.pos.z + dz
    chunk = (neighbour_x >> 4, neighbour_z >> 4)
    blockers: list[RailGraphBlocker] = []

    if chunk not in region.loaded_chunks:
        blockers.append(
            _blocker(
                "insufficient_chunk_evidence",
                record.pos,
                port,
                f"neighbour chunk {chunk} was not loaded",
            )
        )
        return blockers

    boundary_y = port.y2 // 2
    candidate_ys = tuple(
        y
        for y in range(boundary_y - 1, boundary_y + 2)
        if _WORLD_MIN_Y <= y <= _WORLD_MAX_Y
    )
    read_y_min, read_y_max = region.read_y_min, region.read_y_max
    if read_y_min is None or read_y_max is None or any(
        y < read_y_min or y > read_y_max for y in candidate_ys
    ):
        blockers.append(
            _blocker(
                "insufficient_y_evidence",
                record.pos,
                port,
                f"possible neighbour rail Y levels {candidate_ys} are not fully read",
            )
        )
        return blockers

    opposite = _OPPOSITE[port.direction]
    for y in candidate_ys:
        neighbour = records.get(BlockPos(neighbour_x, y, neighbour_z))
        if neighbour is None:
            continue
        if neighbour.diagnostics:
            blockers.append(
                _blocker(
                    "invalid_neighbour_rail_state",
                    record.pos,
                    port,
                    f"neighbour rail at {neighbour.pos.to_dict()} has unknown geometry",
                )
            )
            continue
        has_reciprocal_port = any(
            neighbour_port.coordinate == port.coordinate
            and neighbour_port.direction == opposite
            for neighbour_port in neighbour.ports
        )
        if not has_reciprocal_port:
            blockers.append(
                _blocker(
                    "incompatible_neighbor_geometry",
                    record.pos,
                    port,
                    (
                        f"rail at {neighbour.pos.to_dict()} has no {opposite} port "
                        f"at doubled coordinate {port.coordinate}"
                    ),
                )
            )
    return blockers


def _build_components(
    records: dict[BlockPos, _RailRecord],
    edge_keys: set[tuple[BlockPos, BlockPos, tuple[int, int, int]]],
    terminals: list[RailTerminal],
) -> list[RailComponent]:
    adjacency: dict[BlockPos, set[BlockPos]] = {pos: set() for pos in records}
    for a, b, _coordinate in edge_keys:
        adjacency[a].add(b)
        adjacency[b].add(a)

    remaining = set(records)
    components: list[RailComponent] = []
    while remaining:
        seed = min(remaining)
        remaining.remove(seed)
        component_nodes = {seed}
        pending = [seed]
        while pending:
            current = pending.pop()
            for neighbour in sorted(adjacency[current]):
                if neighbour not in remaining:
                    continue
                remaining.remove(neighbour)
                component_nodes.add(neighbour)
                pending.append(neighbour)

        component_terminals = [
            terminal
            for terminal in terminals
            if _pos_from_dict(terminal["pos"]) in component_nodes
        ]
        edge_count = sum(
            1 for a, b, _coordinate in edge_keys if a in component_nodes and b in component_nodes
        )
        components.append(
            {
                "component_id": f"rail_component_{len(components) + 1:03d}",
                "nodes": [pos.to_dict() for pos in sorted(component_nodes)],
                "edge_count": edge_count,
                "terminals": component_terminals,
                "partial": any(records[pos].partial for pos in component_nodes),
            }
        )
    return components


def _node_to_dict(record: _RailRecord) -> RailNode:
    return {
        "pos": record.pos.to_dict(),
        "id": record.state.full_id,
        "rail_type": record.rail_type,
        "shape": record.shape,
        "powered": record.powered,
        "waterlogged": record.waterlogged,
        "properties": {
            name: record.state.properties[name]
            for name in sorted(record.state.properties)
        },
        "ports": [port.to_dict() for port in sorted(record.ports)],
        "partial": record.partial,
        "diagnostics": list(record.diagnostics),
    }


def _diagnostic(code: str, name: str, value: object = None) -> RailDiagnostic:
    return {
        "code": code,
        "severity": "error",
        "property": name,
        "value": None if value is None else str(value),
    }


def _diagnostic_detail(diagnostic: RailDiagnostic) -> str:
    value = "missing" if diagnostic["value"] is None else diagnostic["value"]
    return f"{diagnostic['property']}={value}"


def _blocker(
    code: str,
    pos: BlockPos | None = None,
    port: _Port | None = None,
    detail: str = "",
) -> RailGraphBlocker:
    return {
        "code": code,
        "pos": None if pos is None else pos.to_dict(),
        "port": None if port is None else port.to_dict(),
        "detail": detail,
    }


def _terminal_sort_key(terminal: RailTerminal) -> tuple[object, ...]:
    pos = terminal["pos"]
    port = terminal["port"]
    return (
        pos["x"],
        pos["y"],
        pos["z"],
        port["x2"],
        port["y2"],
        port["z2"],
        port["direction"],
    )


def _blocker_sort_key(blocker: RailGraphBlocker) -> tuple[object, ...]:
    pos = blocker["pos"] or {"x": 0, "y": 0, "z": 0}
    port = blocker["port"] or {"x2": 0, "y2": 0, "z2": 0, "direction": ""}
    return (
        blocker["code"],
        pos["x"],
        pos["y"],
        pos["z"],
        port["x2"],
        port["y2"],
        port["z2"],
        port["direction"],
        blocker["detail"],
    )


def _pos_from_dict(value: dict[str, int]) -> BlockPos:
    return BlockPos(value["x"], value["y"], value["z"])
