from __future__ import annotations

import json
from pathlib import Path
from types import SimpleNamespace

import pytest

import picasso.core.definition_store as definition_store
from picasso.core.definition_store import (
    DefinitionAlreadyExistsError,
    save_json_definition,
)
from picasso.session import session
from picasso.tools import learning, style


class _FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def _register(module) -> dict[str, object]:
    mcp = _FakeMCP()
    module.register(mcp)
    return mcp.tools


def _pass_arguments(name: str = "created_pass") -> dict:
    return {
        "name": name,
        "description": "Definition-store test pass.",
        "rules": [
            {
                "match": {"block": "minecraft:dirt"},
                "action": "replace",
                "replace_with": [{"block": "minecraft:stone"}],
            }
        ],
    }


def _fragment_arguments(name: str = "created_fragment") -> dict:
    return {
        "name": name,
        "description": "Definition-store test fragment.",
        "anchor_surface": "floor",
        "footprint": "1x1",
        "blocks": [{"offset": [0, 0, 0], "block": "minecraft:cobblestone"}],
    }


def test_store_uses_case_insensitive_exclusive_collision_check(tmp_path: Path) -> None:
    existing = tmp_path / "Example.json"
    existing.write_text('{"generation": 0}', encoding="utf-8")

    with pytest.raises(DefinitionAlreadyExistsError):
        save_json_definition(
            tmp_path,
            "example",
            {"generation": 1},
            overwrite=False,
        )

    assert existing.read_text(encoding="utf-8") == '{"generation": 0}'
    assert not list(tmp_path.glob("*.tmp"))


def test_replace_failure_preserves_original_and_cleans_temp(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    existing = tmp_path / "atomic.json"
    original = b'{"generation": 0}'
    existing.write_bytes(original)

    def fail_replace(_source, _destination) -> None:
        raise OSError("simulated replace failure")

    monkeypatch.setattr(definition_store.os, "replace", fail_replace)
    with pytest.raises(OSError, match="simulated replace failure"):
        save_json_definition(
            tmp_path,
            "atomic",
            {"generation": 1},
            overwrite=True,
        )

    assert existing.read_bytes() == original
    assert len(list((tmp_path / "_replaced").glob("atomic.*.json"))) == 1
    assert not list(tmp_path.glob("*.tmp"))


def test_repeated_overwrites_keep_distinct_archives(tmp_path: Path) -> None:
    save_json_definition(tmp_path, "history", {"generation": 0}, overwrite=False)
    save_json_definition(tmp_path, "history", {"generation": 1}, overwrite=True)
    save_json_definition(tmp_path, "history", {"generation": 2}, overwrite=True)

    archives = sorted((tmp_path / "_replaced").glob("history.*.json"))
    assert len(archives) == 2
    assert {
        json.loads(path.read_text(encoding="utf-8"))["generation"]
        for path in archives
    } == {0, 1}
    assert json.loads((tmp_path / "history.json").read_text(encoding="utf-8"))[
        "generation"
    ] == 2


def test_create_pass_is_atomic_and_reports_validation_errors(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    previous_registry = session.pass_registry
    session.pass_registry = {}
    monkeypatch.setattr(style, "config", SimpleNamespace(passes_dir=tmp_path))
    tools = _register(style)
    try:
        invalid = tools["create_pass"](
            name="invalid_pass",
            description="Invalid.",
            rules=[],
        )
        created = tools["create_pass"](**_pass_arguments())
        collision = tools["create_pass"](**_pass_arguments("CREATED_PASS"))
    finally:
        session.pass_registry = previous_registry

    assert invalid["error"] == "invalid_pass_definition"
    assert created["ok"] is True
    assert collision["error"] == "name_already_exists"
    assert (tmp_path / "created_pass.json").is_file()
    assert not list(tmp_path.glob("*.tmp"))


def test_create_pass_can_author_explicitly_destructive_block_pass(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    previous_registry = session.pass_registry
    session.pass_registry = {}
    monkeypatch.setattr(style, "config", SimpleNamespace(passes_dir=tmp_path))
    tools = _register(style)
    try:
        result = tools["create_pass"](
            name="remove_test",
            description="Explicit destructive pass.",
            rules=[{"match": {"block": "minecraft:dirt"}, "action": "remove"}],
            destructive=True,
        )
        registered = session.pass_registry["remove_test"]
    finally:
        session.pass_registry = previous_registry

    assert result["ok"] is True
    assert registered.destructive is True
    assert json.loads((tmp_path / "remove_test.json").read_text(encoding="utf-8"))[
        "destructive"
    ] is True


def test_create_fragment_registers_only_after_atomic_success(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    previous_library = session.fragment_library
    session.fragment_library = None
    monkeypatch.setattr(
        learning,
        "config",
        SimpleNamespace(fragments_dir=tmp_path, bundles_dir=tmp_path / "bundles"),
    )
    tools = _register(learning)
    try:
        invalid = tools["create_fragment"](
            **{**_fragment_arguments("invalid_fragment"), "blocks": []}
        )
        created = tools["create_fragment"](**_fragment_arguments())
        loaded_names = set(session.fragment_library.fragments)
    finally:
        session.fragment_library = previous_library

    assert invalid["error"] == "invalid_fragment_definition"
    assert created["ok"] is True
    assert loaded_names == {"created_fragment"}
    assert not list(tmp_path.glob("*.tmp"))
