from __future__ import annotations

import copy
import json
import logging
import os
from pathlib import Path
from types import SimpleNamespace

import pytest
from pydantic import ValidationError

from picasso.core.bundle_library import load_bundle_registry
from picasso.core.style_engine import load_pass_registry
from picasso.models.bundle import Bundle
from picasso.tools import learning


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATA_ROOT = PROJECT_ROOT / "src" / "picasso" / "data"


def _bundle_data() -> dict:
    return {
        "name": "test_bundle",
        "description": "Bundle fixture.",
        "version": "1.0",
        "default_seed": 42,
        "entries": [
            {
                "structure_type": "building",
                "description": "Building fixture.",
                "passes": [
                    {
                        "name": "known_pass",
                        "intensity": 0.5,
                        "space_filter": "interior",
                    }
                ],
            }
        ],
    }


def _validate_with_change(change) -> None:
    data = _bundle_data()
    change(data)
    with pytest.raises(ValidationError):
        Bundle.model_validate(data)


@pytest.mark.parametrize(
    "change",
    [
        lambda data: data.update({"descripton": "typo"}),
        lambda data: data["entries"][0].update({"structure_typ": "typo"}),
        lambda data: data["entries"][0]["passes"][0].update({"intensitty": 0.5}),
        lambda data: data.update({"entries": []}),
        lambda data: data["entries"][0].update({"passes": []}),
        lambda data: data.update({"name": "   "}),
        lambda data: data.update({"description": "   "}),
        lambda data: data.update({"version": "   "}),
        lambda data: data.update({"default_seed": "42"}),
        lambda data: data["entries"][0].update({"structure_type": "   "}),
        lambda data: data["entries"][0]["passes"][0].update({"name": "   "}),
        lambda data: data["entries"][0]["passes"][0].update({"intensity": -0.01}),
        lambda data: data["entries"][0]["passes"][0].update({"intensity": 1.01}),
        lambda data: data["entries"][0]["passes"][0].update({"intensity": "0.5"}),
        lambda data: data["entries"][0]["passes"][0].update(
            {"space_filter": "outside"}
        ),
    ],
)
def test_bundle_schema_rejects_invalid_definitions(change) -> None:
    _validate_with_change(change)


def test_bundle_rejects_duplicate_structure_types_and_passes() -> None:
    duplicate_entry = _bundle_data()
    duplicate_entry["entries"].append(copy.deepcopy(duplicate_entry["entries"][0]))
    with pytest.raises(ValidationError, match="duplicate structure_type"):
        Bundle.model_validate(duplicate_entry)

    duplicate_pass = _bundle_data()
    duplicate_pass["entries"][0]["passes"].append(
        copy.deepcopy(duplicate_pass["entries"][0]["passes"][0])
    )
    with pytest.raises(ValidationError, match="duplicate pass"):
        Bundle.model_validate(duplicate_pass)


def test_bundle_normalizes_strings_and_returns_executor_compatible_dict() -> None:
    data = _bundle_data()
    data["name"] = " normalized_bundle "
    data["entries"][0]["passes"][0] = {"name": " known_pass ", "intensity": 1}

    bundle = Bundle.model_validate(data)
    registry_value = bundle.to_registry_dict()

    assert bundle.name == "normalized_bundle"
    assert registry_value["entries"][0]["passes"][0] == {
        "name": "known_pass",
        "intensity": 1.0,
    }


def test_library_is_resilient_and_rejects_bad_schema_name_and_refs(
    tmp_path: Path,
    caplog: pytest.LogCaptureFixture,
) -> None:
    good = _bundle_data()
    good["name"] = "good"
    (tmp_path / "good.json").write_text(json.dumps(good), encoding="utf-8")

    bad_schema = _bundle_data()
    bad_schema["name"] = "bad_schema"
    bad_schema["entries"][0]["passes"][0]["intensitty"] = 0.2
    (tmp_path / "bad_schema.json").write_text(
        json.dumps(bad_schema), encoding="utf-8"
    )

    bad_ref = _bundle_data()
    bad_ref["name"] = "bad_ref"
    bad_ref["entries"][0]["passes"][0]["name"] = "missing_pass"
    (tmp_path / "bad_ref.json").write_text(json.dumps(bad_ref), encoding="utf-8")

    wrong_name = _bundle_data()
    wrong_name["name"] = "not_the_filename"
    (tmp_path / "wrong_name.json").write_text(
        json.dumps(wrong_name), encoding="utf-8"
    )
    (tmp_path / "malformed.json").write_text("{", encoding="utf-8")
    caplog.set_level(logging.WARNING, logger="picasso.core.bundle_library")

    registry = load_bundle_registry(tmp_path, {"known_pass": object()})

    assert set(registry) == {"good"}
    assert isinstance(registry["good"], dict)
    skipped = [
        record for record in caplog.records if "Skipping invalid bundle" in record.message
    ]
    assert len(skipped) == 4


def test_shipped_tlou_bundle_loads_with_all_pass_references() -> None:
    pass_registry = load_pass_registry(DATA_ROOT / "passes")

    bundle_registry = load_bundle_registry(DATA_ROOT / "bundles", pass_registry)

    assert set(bundle_registry) == {"tlou_complete"}
    assert bundle_registry["tlou_complete"]["name"] == "tlou_complete"
    assert len(bundle_registry["tlou_complete"]["entries"]) == 6


class _FakeMCP:
    def __init__(self) -> None:
        self.tools: dict[str, object] = {}

    def tool(self):
        def decorator(function):
            self.tools[function.__name__] = function
            return function

        return decorator


def _registered_create_bundle(monkeypatch: pytest.MonkeyPatch, bundles_dir: Path):
    fake_session = SimpleNamespace(
        pass_registry={"known_pass": object()},
        bundle_registry={},
    )
    monkeypatch.setattr(learning, "config", SimpleNamespace(bundles_dir=bundles_dir))
    monkeypatch.setattr(learning, "session", fake_session)
    mcp = _FakeMCP()
    learning.register(mcp)
    return mcp.tools["create_bundle"], fake_session


def _create_arguments(name: str = "created_bundle") -> dict:
    return {
        "name": name,
        "description": "Created bundle fixture.",
        "entries": [
            {
                "structure_type": "building",
                "passes": [{"name": "known_pass", "intensity": 0.75}],
            }
        ],
    }


def test_create_bundle_writes_atomically_and_registers_after_success(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    create_bundle, fake_session = _registered_create_bundle(monkeypatch, tmp_path)
    real_fsync = os.fsync
    real_replace = os.replace
    fsync_calls: list[int] = []
    replace_calls: list[tuple[Path, Path]] = []

    def tracked_fsync(fd: int) -> None:
        fsync_calls.append(fd)
        real_fsync(fd)

    def tracked_replace(source, destination) -> None:
        replace_calls.append((Path(source), Path(destination)))
        real_replace(source, destination)

    monkeypatch.setattr(learning.os, "fsync", tracked_fsync)
    monkeypatch.setattr(learning.os, "replace", tracked_replace)

    result = create_bundle(**_create_arguments())

    saved_path = tmp_path / "created_bundle.json"
    assert result["ok"] is True
    assert fsync_calls
    assert replace_calls and replace_calls[0][1] == saved_path
    assert replace_calls[0][0].parent == tmp_path
    assert json.loads(saved_path.read_text(encoding="utf-8"))["name"] == "created_bundle"
    assert set(fake_session.bundle_registry) == {"created_bundle"}
    assert not list(tmp_path.glob("*.tmp"))


def test_create_bundle_rejects_invalid_schema_and_unknown_pass_without_writing(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    create_bundle, fake_session = _registered_create_bundle(monkeypatch, tmp_path)

    invalid_schema = create_bundle(
        name="invalid_schema",
        description="Invalid.",
        entries=[],
    )
    unknown_ref_args = _create_arguments("unknown_ref")
    unknown_ref_args["entries"][0]["passes"][0]["name"] = "missing_pass"
    unknown_ref = create_bundle(**unknown_ref_args)

    assert invalid_schema["error"] == "invalid_bundle_definition"
    assert unknown_ref["error"] == "invalid_bundle_definition"
    assert not list(tmp_path.glob("*.json"))
    assert fake_session.bundle_registry == {}


def test_create_bundle_name_collision_never_overwrites_without_permission(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    create_bundle, fake_session = _registered_create_bundle(monkeypatch, tmp_path)
    existing = tmp_path / "collision.json"
    existing.write_text('{"sentinel": true}', encoding="utf-8")

    result = create_bundle(**_create_arguments("collision"))

    assert result["ok"] is False
    assert result["error"] == "name_already_exists"
    assert existing.read_text(encoding="utf-8") == '{"sentinel": true}'
    assert fake_session.bundle_registry == {}


def test_create_bundle_replace_failure_preserves_original_and_cleans_temp(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    create_bundle, fake_session = _registered_create_bundle(monkeypatch, tmp_path)
    existing = tmp_path / "atomic_failure.json"
    original = b'{"sentinel": "original"}'
    existing.write_bytes(original)
    fake_session.bundle_registry["atomic_failure"] = {"sentinel": "registry"}

    def fail_replace(_source, _destination) -> None:
        raise OSError("simulated replace failure")

    monkeypatch.setattr(learning.os, "replace", fail_replace)

    result = create_bundle(**_create_arguments("atomic_failure"), overwrite=True)

    assert result["ok"] is False
    assert result["error"] == "internal_error"
    assert existing.read_bytes() == original
    assert fake_session.bundle_registry["atomic_failure"] == {"sentinel": "registry"}
    assert not list(tmp_path.glob("*.tmp"))


def test_create_bundle_overwrite_archives_never_collide(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    create_bundle, _fake_session = _registered_create_bundle(monkeypatch, tmp_path)
    existing = tmp_path / "archive_test.json"
    existing.write_text('{"generation": 0}', encoding="utf-8")

    first = create_bundle(**_create_arguments("archive_test"), overwrite=True)
    second = create_bundle(**_create_arguments("archive_test"), overwrite=True)

    archives = sorted((tmp_path / "_replaced").glob("archive_test.*.json"))
    assert first["ok"] is True
    assert second["ok"] is True
    assert len(archives) == 2
    assert archives[0] != archives[1]
