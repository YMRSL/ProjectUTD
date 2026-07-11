from __future__ import annotations

import base64
import copy
import hashlib
import json
import os
import re
import tempfile
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

from picasso.models.block import BlockPos, BlockState
from picasso.models.region import RegionData


SCHEMA_VERSION = 1
WORLD_ID_SCHEMA_VERSION = 1
_OPERATION_ID_RE = re.compile(r"^[0-9a-f]{32}$")
_SAFE_TOOL_RE = re.compile(r"[^a-zA-Z0-9_-]+")
_ENTRY_STATUSES = {"pending", "committed", "revert_pending", "reverted", "failed"}
_OS_REPLACE = os.replace
_OS_LINK = os.link


class JournalError(RuntimeError):
    """Base class for journal failures safe to expose as structured tool errors."""

    code = "journal_error"

    def __init__(self, message: str, **details: Any) -> None:
        super().__init__(message)
        self.details = details


class JournalUnavailableError(JournalError):
    code = "journal_unavailable"


class JournalEmptyError(JournalError):
    code = "journal_empty"


class JournalNotFoundError(JournalError):
    code = "journal_entry_not_found"


class JournalCorruptError(JournalError):
    code = "journal_corrupt"


class JournalWorldMismatchError(JournalError):
    code = "journal_world_mismatch"


class JournalWriteError(JournalError):
    code = "journal_write_failed"

    def __init__(
        self,
        message: str,
        *,
        stage: str,
        operation_id: str | None = None,
        journal_path: str | None = None,
        rollback_succeeded: bool | None = None,
        rollback_error: str | None = None,
    ) -> None:
        super().__init__(
            message,
            stage=stage,
            operation_id=operation_id,
            journal_path=journal_path,
            rollback_succeeded=rollback_succeeded,
            rollback_error=rollback_error,
        )
        self.stage = stage
        self.operation_id = operation_id
        self.journal_path = journal_path
        self.rollback_succeeded = rollback_succeeded
        self.rollback_error = rollback_error


class JournalRevertError(JournalError):
    code = "journal_revert_failed"


@dataclass(frozen=True)
class JournalArtifact:
    """A restricted companion file committed with a world-block operation.

    Artifacts are stored as exact bytes in the journal so a later process can
    verify and restore them without retaining in-memory callbacks.
    """

    relative_path: str
    after_bytes: bytes
    compound_positions: tuple[BlockPos, ...]
    must_not_exist: bool = False

    @classmethod
    def json_file(
        cls,
        relative_path: str,
        payload: dict[str, Any],
        *,
        compound_positions: Iterable[BlockPos],
        must_not_exist: bool = False,
    ) -> "JournalArtifact":
        data = json.dumps(payload, ensure_ascii=False, indent=2).encode("utf-8")
        return cls(
            relative_path=relative_path,
            after_bytes=data,
            compound_positions=tuple(compound_positions),
            must_not_exist=must_not_exist,
        )


@dataclass(frozen=True)
class JournalWriteResult:
    changed: int
    operation_id: str | None
    status: str
    path: Path | None
    warning: str | None = None

    def reference(self) -> dict[str, Any] | None:
        if self.operation_id is None or self.path is None:
            return None
        return {
            "operation_id": self.operation_id,
            "status": self.status,
            "path": str(self.path),
        }


class Journal:
    """Crash-tolerant, per-world reverse-diff journal.

    A pending entry is durably renamed into place before the bridge is allowed
    to mutate or save the world. State transitions replace that one JSON file
    atomically, preserving either the old or new complete document on a crash.
    """

    def __init__(self, world_path: str | Path, bridge: Any, dimension: str) -> None:
        self.world_path = Path(world_path).expanduser().resolve()
        self.bridge = bridge
        self.dimension = str(dimension)
        self.identity_path = self.world_path / "picasso_world_id.json"
        self.directory = self.world_path / "picasso_journal"
        normalized_path = os.path.normcase(str(self.world_path))
        self.world_path_hash = hashlib.sha256(normalized_path.encode("utf-8")).hexdigest()
        self.world_uuid = self._load_or_create_world_uuid()
        identity_material = f"{self.world_uuid}\0{self.dimension}".encode("utf-8")
        self.world_identity = hashlib.sha256(identity_material).hexdigest()
        self._ensure_available()

    def matches(self, world_path: str | Path, bridge: Any, dimension: str) -> bool:
        try:
            resolved = Path(world_path).expanduser().resolve()
        except Exception:
            return False
        return (
            resolved == self.world_path
            and bridge is self.bridge
            and str(dimension) == self.dimension
        )

    def apply(
        self,
        changes: RegionData,
        *,
        tool: str,
        argument_summary: dict[str, Any] | None = None,
        pass_name: str | None = None,
        seed: int | None = None,
        instance_id: str | None = None,
        layer: str | None = None,
        on_behalf_of: dict[str, Any] | None = None,
        artifacts: Iterable[JournalArtifact] = (),
    ) -> JournalWriteResult:
        """Journal, save, and commit one world write (plus companion files)."""

        if not changes.blocks:
            return JournalWriteResult(0, None, "noop", None)

        self._assert_identity_current()

        operation_id = uuid.uuid4().hex
        now = _utc_now()
        entry_path = self._entry_path(now, tool, seed, operation_id)
        artifact_specs = list(artifacts)

        try:
            change_records = self._snapshot_changes(changes)
            artifact_records = [self._snapshot_artifact(item) for item in artifact_specs]
        except JournalError:
            raise
        except Exception as exc:
            raise JournalWriteError(
                f"Could not capture the pre-write state: {exc}",
                stage="snapshot",
                operation_id=operation_id,
                journal_path=str(entry_path),
            ) from exc

        entry: dict[str, Any] = {
            "schema_version": SCHEMA_VERSION,
            "operation_id": operation_id,
            "world_identity": self.world_identity,
            "world_path_hash": self.world_path_hash,
            "dimension": self.dimension,
            "timestamp": now,
            "applied_at": now,
            "tool": str(tool),
            "pass": pass_name,
            "argument_summary": _json_safe(argument_summary or {}),
            "seed": seed,
            "instance_id": instance_id,
            "layer": layer,
            "on_behalf_of": _json_safe(on_behalf_of) if on_behalf_of is not None else None,
            "status": "pending",
            "changes": change_records,
            "artifacts": artifact_records,
        }

        # Validate the complete document before its first durable appearance.
        # This catches malformed/overlapping compound groups before any world
        # save can occur, rather than producing a self-corrupt journal entry.
        self._validate_entry(entry, entry_path)
        self._validate_world(entry)

        try:
            self._write_entry(entry_path, entry, create=True)
        except Exception as exc:
            raise JournalUnavailableError(
                f"Could not durably create the pending journal entry: {exc}",
                operation_id=operation_id,
                journal_path=str(entry_path),
            ) from exc

        world_stage = "world_write"
        try:
            changed = self.bridge.write_region(changes)
            if changed != len(change_records):
                raise RuntimeError(
                    f"Bridge reported {changed} changes; expected {len(change_records)}."
                )
            world_stage = "world_verify"
            verified, verify_error = self._verify_change_records(change_records, "after")
            if not verified:
                raise RuntimeError(verify_error or "Saved after-state verification failed.")
        except Exception as exc:
            rolled_back, rollback_error = self._verify_change_records(change_records, "before")
            if not rolled_back and not getattr(self.bridge, "_write_poisoned", False):
                initial_verify_error = rollback_error
                rollback_errors = self._write_records(change_records, target="before")
                rolled_back, verify_error = self._verify_change_records(change_records, "before")
                combined = [value for value in (*rollback_errors, verify_error) if value]
                if not rolled_back and initial_verify_error:
                    combined.insert(0, initial_verify_error)
                rollback_error = "; ".join(combined) or None
            self._record_apply_failure(
                entry_path,
                entry,
                stage=world_stage,
                error=exc,
                rolled_back=rolled_back,
                rollback_error=rollback_error,
            )
            raise JournalWriteError(
                f"World write failed after the pending journal entry was saved: {exc}",
                stage=world_stage,
                operation_id=operation_id,
                journal_path=str(entry_path),
                rollback_succeeded=rolled_back,
                rollback_error=rollback_error,
            ) from exc

        applied_artifacts: list[dict[str, Any]] = []
        try:
            for record in artifact_records:
                self._apply_artifact(record)
                applied_artifacts.append(record)
        except Exception as exc:
            rollback_errors: list[str] = []
            rollback_errors.extend(self._restore_artifacts(applied_artifacts, target="before"))
            rollback_errors.extend(self._write_records(change_records, target="before"))
            rolled_back, verify_error = self._verify_transaction_state(
                change_records, artifact_records, target="before"
            )
            if verify_error:
                rollback_errors.append(verify_error)
            rollback_error = "; ".join(rollback_errors) or None
            self._record_apply_failure(
                entry_path,
                entry,
                stage="artifact_write",
                error=exc,
                rolled_back=rolled_back,
                rollback_error=rollback_error,
            )
            raise JournalWriteError(
                f"Companion artifact commit failed; the world transaction was rolled back: {exc}",
                stage="artifact_write",
                operation_id=operation_id,
                journal_path=str(entry_path),
                rollback_succeeded=rolled_back,
                rollback_error=rollback_error,
            ) from exc

        committed = copy.deepcopy(entry)
        committed["status"] = "committed"
        committed["committed_at"] = _utc_now()
        try:
            self._write_entry(entry_path, committed)
        except Exception as exc:
            # This is intentionally not rolled back: the durable pending file
            # contains the complete before/after diff, so a crash or failed
            # status update remains inspectable and recoverable.
            return JournalWriteResult(
                changed=changed,
                operation_id=operation_id,
                status="pending",
                path=entry_path,
                warning=(
                    "World changes were saved, but the journal commit status could not be "
                    f"updated. The durable pending diff was retained: {exc}"
                ),
            )
        return JournalWriteResult(changed, operation_id, "committed", entry_path)

    def list_entries(
        self,
        *,
        limit: int = 50,
        status: str | None = None,
        tool: str | None = None,
        pass_name: str | None = None,
    ) -> dict[str, list[dict[str, Any]]]:
        if limit < 1 or limit > 1000:
            raise ValueError("limit must be between 1 and 1000")
        self._assert_identity_current()
        entries: list[dict[str, Any]] = []
        corrupt: list[dict[str, Any]] = []
        for path in self._entry_files_newest_first():
            try:
                entry = self._load_entry(path)
                self._validate_world(entry)
            except Exception as exc:
                corrupt.append({"path": str(path), "error": str(exc)})
                continue
            if status is not None and entry["status"] != status:
                continue
            if tool is not None and entry["tool"] != tool:
                continue
            if pass_name is not None and entry.get("pass") != pass_name:
                continue
            entries.append(self._entry_summary(entry, path))
            if len(entries) >= limit:
                break
        return {"entries": entries, "corrupt_entries": corrupt}

    def inspect(self, operation_id: str) -> dict[str, Any]:
        self._assert_identity_current()
        path = self._find_entry_path(operation_id)
        entry = self._load_entry(path)
        self._validate_world(entry)
        result = copy.deepcopy(entry)
        result["path"] = str(path)
        return result

    def revert_last(self, operation_id: str | None = None) -> dict[str, Any]:
        self._assert_identity_current()
        path, entry = self._select_revert_entry(operation_id)
        self._validate_world(entry)

        change_by_pos: dict[BlockPos, dict[str, Any]] = {
            _pos_from_dict(item["pos"]): item for item in entry["changes"]
        }
        current_by_pos: dict[BlockPos, tuple[BlockState, bool]] = {}
        conflicts: list[dict[str, Any]] = []
        conflict_positions: set[BlockPos] = set()
        already_reverted: set[BlockPos] = set()

        for pos, record in change_by_pos.items():
            current, has_entity = self.bridge.read_block_with_entity(pos.x, pos.y, pos.z)
            current_by_pos[pos] = (current, has_entity)
            expected_after = _state_from_dict(record["after"])
            expected_before = _state_from_dict(record["before"])
            if not has_entity and current == expected_after:
                continue
            if (
                _entry_allows_before_recovery(entry)
                and not has_entity
                and current == expected_before
            ):
                already_reverted.add(pos)
                continue
            conflict_positions.add(pos)
            conflicts.append(
                {
                    "pos": pos.to_dict(),
                    "expected_after": _state_to_dict(expected_after),
                    "found": {
                        **_state_to_dict(current),
                        "has_block_entity": has_entity,
                    },
                }
            )

        artifact_records = list(entry.get("artifacts") or [])
        artifact_conflicts: set[int] = set()
        artifact_already_reverted: set[int] = set()
        for index, artifact in enumerate(artifact_records):
            positions = {_pos_from_dict(value) for value in artifact["compound_positions"]}
            current_snapshot = self._artifact_snapshot(
                self._artifact_path(artifact["relative_path"])
            )
            if _snapshots_equal(current_snapshot, artifact["after"]):
                pass
            elif _entry_allows_before_recovery(entry) and _snapshots_equal(
                current_snapshot, artifact["before"]
            ):
                artifact_already_reverted.add(index)
            else:
                artifact_conflicts.add(index)
                for pos in sorted(positions):
                    if pos not in conflict_positions:
                        state, has_entity = current_by_pos[pos]
                        record = change_by_pos[pos]
                        conflicts.append(
                            {
                                "pos": pos.to_dict(),
                                "expected_after": record["after"],
                                "found": {
                                    **_state_to_dict(state),
                                    "has_block_entity": has_entity,
                                },
                                "reason": "companion_artifact_conflict",
                                "artifact": {
                                    "path": artifact["relative_path"],
                                    "expected_sha256": artifact["after"].get("sha256"),
                                    "found_sha256": current_snapshot.get("sha256"),
                                },
                            }
                        )
                    conflict_positions.add(pos)

            # A companion artifact and all its bound blocks form one compound
            # unit. Any block conflict skips the artifact and every block in it.
            if positions & conflict_positions:
                artifact_conflicts.add(index)
                conflict_positions.update(positions)

        eligible_positions = set(change_by_pos) - conflict_positions - already_reverted
        eligible_artifacts = [
            record
            for index, record in enumerate(artifact_records)
            if index not in artifact_conflicts and index not in artifact_already_reverted
        ]

        revert_pending = copy.deepcopy(entry)
        revert_pending["status"] = "revert_pending"
        revert_pending["revert_started_at"] = _utc_now()
        revert_pending["revert_conflicts"] = conflicts
        try:
            self._write_entry(path, revert_pending)
        except Exception as exc:
            raise JournalUnavailableError(
                f"Could not durably begin the revert transaction: {exc}",
                operation_id=entry["operation_id"],
                journal_path=str(path),
            ) from exc

        reverse_changes = RegionData()
        for pos in sorted(eligible_positions):
            reverse_changes.set(pos, _state_from_dict(change_by_pos[pos]["before"]))
        eligible_records = [change_by_pos[pos] for pos in sorted(eligible_positions)]

        changed = 0
        revert_world_stage = "world_write"
        try:
            if reverse_changes.blocks:
                changed = self.bridge.write_region(reverse_changes)
                if changed != len(reverse_changes.blocks):
                    raise RuntimeError(
                        f"Bridge reported {changed} reverted blocks; expected "
                        f"{len(reverse_changes.blocks)}."
                    )
                revert_world_stage = "world_verify"
                verified, verify_error = self._verify_change_records(
                    eligible_records, "before"
                )
                if not verified:
                    raise RuntimeError(
                        verify_error or "Saved reverted-state verification failed."
                    )
        except Exception as exc:
            rollback_errors: list[str] = []
            rolled_back, rollback_verify_error = self._verify_change_records(
                eligible_records, "after"
            )
            if not rolled_back and not getattr(self.bridge, "_write_poisoned", False):
                rollback_errors.extend(self._write_records(eligible_records, target="after"))
                rolled_back, rollback_verify_error = self._verify_change_records(
                    eligible_records, "after"
                )
            if rollback_verify_error:
                rollback_errors.append(rollback_verify_error)
            restored = copy.deepcopy(entry)
            restored["last_revert_failure"] = {
                "at": _utc_now(),
                "stage": revert_world_stage,
                "error": f"{type(exc).__name__}: {exc}",
                "rolled_back": rolled_back,
                "rollback_errors": rollback_errors,
            }
            if not rolled_back:
                restored["status"] = "revert_pending"
            self._safe_write_entry(path, restored)
            raise JournalRevertError(
                f"World write or verification failed while reverting: {exc}",
                operation_id=entry["operation_id"],
                stage=revert_world_stage,
                rollback_succeeded=rolled_back,
                rollback_errors=rollback_errors,
            ) from exc

        try:
            artifact_errors = self._restore_artifacts(eligible_artifacts, target="before")
            if artifact_errors:
                raise RuntimeError("; ".join(artifact_errors))
        except Exception as exc:
            rollback_errors = self._write_records(
                eligible_records, target="after"
            )
            rollback_errors.extend(self._restore_artifacts(eligible_artifacts, target="after"))
            rolled_back, verify_error = self._verify_transaction_state(
                eligible_records, eligible_artifacts, target="after"
            )
            if verify_error:
                rollback_errors.append(verify_error)
            restored = copy.deepcopy(entry)
            restored["last_revert_failure"] = {
                "at": _utc_now(),
                "stage": "artifact_restore",
                "error": f"{type(exc).__name__}: {exc}",
                "rolled_back": rolled_back,
                "rollback_errors": rollback_errors,
            }
            if not rolled_back:
                restored["status"] = "revert_pending"
            self._safe_write_entry(path, restored)
            raise JournalRevertError(
                f"Companion artifact restore failed; revert rollback was attempted: {exc}",
                operation_id=entry["operation_id"],
                stage="artifact_restore",
                rollback_succeeded=rolled_back,
                rollback_errors=rollback_errors,
            ) from exc

        reverted = copy.deepcopy(revert_pending)
        reverted["status"] = "reverted"
        reverted["reverted_at"] = _utc_now()
        reverted["reverted_change_count"] = changed
        reverted["already_reverted_count"] = len(already_reverted)
        reverted["skipped_change_count"] = len(conflict_positions)
        warning = None
        final_status = "reverted"
        try:
            self._write_entry(path, reverted)
        except Exception as exc:
            final_status = "revert_pending"
            warning = (
                "World changes were reverted, but the final journal status update failed. "
                f"The durable revert_pending entry was retained: {exc}"
            )

        return {
            "operation_id": entry["operation_id"],
            "path": str(path),
            "status": final_status,
            "changed": changed,
            "already_reverted": len(already_reverted),
            "artifacts_reverted": len(eligible_artifacts),
            "conflicts": conflicts,
            "skipped": len(conflict_positions),
            "warning": warning,
        }

    # Compatibility with the public tool name and likely direct callers.
    def revert_last_apply(self, operation_id: str | None = None) -> dict[str, Any]:
        return self.revert_last(operation_id)

    def _ensure_available(self) -> None:
        try:
            if not self.world_path.exists() or not self.world_path.is_dir():
                raise FileNotFoundError(f"World path does not exist: {self.world_path}")
            self.directory.mkdir(parents=True, exist_ok=True)
            self._assert_journal_directory_current()
            probe = self.directory / f".probe.{uuid.uuid4().hex}"
            _atomic_replace_bytes(probe, b"journal-probe", create=True)
            probe.unlink()
        except Exception as exc:
            raise JournalUnavailableError(
                f"Journal directory is not writable: {self.directory}: {exc}",
                journal_path=str(self.directory),
            ) from exc

    def _load_or_create_world_uuid(self) -> str:
        if not self.world_path.exists() or not self.world_path.is_dir():
            raise JournalUnavailableError(f"World path does not exist: {self.world_path}")
        if self.identity_path.exists() or self.identity_path.is_symlink():
            try:
                if self.identity_path.resolve(strict=True) != self.identity_path:
                    raise JournalUnavailableError(
                        "picasso_world_id.json may not be a symlink or junction."
                    )
            except JournalUnavailableError:
                raise
            except Exception as exc:
                raise JournalUnavailableError(
                    f"Invalid world identity path: {self.identity_path}: {exc}"
                ) from exc
            return self._read_world_uuid()

        world_uuid = uuid.uuid4().hex
        payload = json.dumps(
            {
                "schema_version": WORLD_ID_SCHEMA_VERSION,
                "world_uuid": world_uuid,
            },
            ensure_ascii=False,
            indent=2,
            sort_keys=True,
        ).encode("utf-8")
        try:
            _atomic_replace_bytes(self.identity_path, payload, create=True)
        except FileExistsError:
            # Another correctly-serialized opener won the create race. Never
            # overwrite it; validate and use the winning identity.
            return self._read_world_uuid()
        except Exception as exc:
            raise JournalUnavailableError(
                f"Could not create the world identity file: {self.identity_path}: {exc}"
            ) from exc
        return world_uuid

    def _read_world_uuid(self) -> str:
        try:
            resolved_identity = self.identity_path.resolve(strict=True)
            if resolved_identity != self.identity_path or not resolved_identity.is_file():
                raise ValueError(
                    "picasso_world_id.json must be a regular file at the fixed world path"
                )
            data = json.loads(self.identity_path.read_text(encoding="utf-8"))
            if not isinstance(data, dict):
                raise ValueError("identity document is not an object")
            if data.get("schema_version") != WORLD_ID_SCHEMA_VERSION:
                raise ValueError(
                    f"unsupported schema_version {data.get('schema_version')!r}"
                )
            raw_uuid = data.get("world_uuid")
            if not isinstance(raw_uuid, str):
                raise ValueError("world_uuid is missing or not a string")
            parsed = uuid.UUID(raw_uuid)
            if parsed.hex != raw_uuid:
                raise ValueError("world_uuid must be canonical lowercase UUID hex")
            return parsed.hex
        except Exception as exc:
            raise JournalUnavailableError(
                "The existing picasso_world_id.json is missing, corrupt, or has an "
                f"unsupported schema; refusing to replace it: {exc}",
                identity_path=str(self.identity_path),
            ) from exc

    def _assert_identity_current(self) -> None:
        current_uuid = self._read_world_uuid()
        if current_uuid != self.world_uuid:
            raise JournalWorldMismatchError(
                "The current world identity changed after this journal was opened."
            )

    def _assert_journal_directory_current(self) -> None:
        resolved_directory = self.directory.resolve(strict=True)
        if resolved_directory != self.directory or not resolved_directory.is_dir():
            raise JournalUnavailableError(
                "picasso_journal must be a real directory at the fixed world path; "
                "symlinks and junctions are not allowed"
            )

    def _snapshot_changes(self, changes: RegionData) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for pos, after in sorted(changes.blocks.items()):
            before, has_entity = self.bridge.read_block_with_entity(pos.x, pos.y, pos.z)
            if has_entity:
                raise JournalWriteError(
                    f"Refusing to journal a write over a block entity at {pos}.",
                    stage="snapshot",
                )
            records.append(
                {
                    "pos": pos.to_dict(),
                    "before": _state_to_dict(before),
                    "after": _state_to_dict(after),
                }
            )
        return records

    def _snapshot_artifact(self, artifact: JournalArtifact) -> dict[str, Any]:
        if not artifact.compound_positions:
            raise JournalWriteError(
                "A companion artifact must be bound to at least one block position.",
                stage="snapshot",
            )
        path = self._artifact_path(artifact.relative_path)
        before = self._artifact_snapshot(path)
        if artifact.must_not_exist and before["exists"]:
            raise JournalWriteError(
                f"Companion artifact already exists: {artifact.relative_path}",
                stage="snapshot",
            )
        return {
            "kind": "file",
            "relative_path": path.relative_to(self.world_path).as_posix(),
            "compound_positions": [pos.to_dict() for pos in sorted(artifact.compound_positions)],
            "before": before,
            "after": _bytes_snapshot(artifact.after_bytes),
        }

    def _apply_artifact(self, record: dict[str, Any]) -> None:
        path = self._artifact_path(record["relative_path"])
        current = self._artifact_snapshot(path)
        if not _snapshots_equal(current, record["before"]):
            raise RuntimeError(
                f"Companion artifact changed after journaling: {record['relative_path']}"
            )
        data = _snapshot_bytes(record["after"])
        _atomic_replace_bytes(path, data, create=not current["exists"])
        written = self._artifact_snapshot(path)
        if not _snapshots_equal(written, record["after"]):
            raise RuntimeError(
                f"Companion artifact verification failed: {record['relative_path']}"
            )

    def _artifact_path(self, relative_path: str) -> Path:
        if not isinstance(relative_path, str) or not relative_path.strip():
            raise JournalCorruptError("Artifact path must be a non-empty relative path.")
        raw = Path(relative_path)
        if raw.is_absolute() or raw.drive or ".." in raw.parts:
            raise JournalCorruptError(f"Artifact path escapes the world: {relative_path!r}")
        candidate = (self.world_path / raw).resolve()
        try:
            candidate.relative_to(self.world_path)
        except ValueError as exc:
            raise JournalCorruptError(
                f"Artifact path resolves outside the current world: {relative_path!r}"
            ) from exc
        if candidate == self.identity_path:
            raise JournalCorruptError("Companion artifacts may not modify the world identity.")
        if candidate == self.directory or self.directory in candidate.parents:
            raise JournalCorruptError("Companion artifacts may not modify the journal directory.")
        return candidate

    @staticmethod
    def _artifact_snapshot(path: Path) -> dict[str, Any]:
        if not path.exists():
            return {"exists": False, "content_base64": None, "sha256": None}
        if not path.is_file():
            raise RuntimeError(f"Companion artifact is not a regular file: {path}")
        return _bytes_snapshot(path.read_bytes())

    def _restore_artifacts(
        self, records: Iterable[dict[str, Any]], *, target: str
    ) -> list[str]:
        errors: list[str] = []
        source = "after" if target == "before" else "before"
        for record in reversed(list(records)):
            try:
                path = self._artifact_path(record["relative_path"])
                current = self._artifact_snapshot(path)
                desired = record[target]
                # Never overwrite an unrelated external file during rollback or
                # revert. Exact source/target states are both safe idempotent cases.
                if _snapshots_equal(current, desired):
                    continue
                if not _snapshots_equal(current, record[source]):
                    raise RuntimeError("current artifact differs from both transaction states")
                if desired["exists"]:
                    _atomic_replace_bytes(path, _snapshot_bytes(desired), create=False)
                elif path.exists():
                    path.unlink()
                    _fsync_directory(path.parent)
            except Exception as exc:
                errors.append(f"{record.get('relative_path')}: {exc}")
        return errors

    def _write_records(self, records: Iterable[dict[str, Any]], *, target: str) -> list[str]:
        records = list(records)
        if not records:
            return []
        changes = RegionData()
        for record in records:
            changes.set(_pos_from_dict(record["pos"]), _state_from_dict(record[target]))
        try:
            changed = self.bridge.write_region(changes)
            if changed != len(changes.blocks):
                raise RuntimeError(f"Bridge reported {changed}; expected {len(changes.blocks)}.")
            return []
        except Exception as exc:
            return [f"world {target} restore: {exc}"]

    def _verify_change_records(
        self, records: Iterable[dict[str, Any]], target: str
    ) -> tuple[bool, str | None]:
        try:
            for record in records:
                pos = _pos_from_dict(record["pos"])
                current, has_entity = self.bridge.read_block_with_entity(pos.x, pos.y, pos.z)
                if has_entity or current != _state_from_dict(record[target]):
                    return False, f"State verification failed at {pos}."
            return True, None
        except Exception as exc:
            return False, f"State verification failed: {exc}"

    def _verify_transaction_state(
        self,
        change_records: Iterable[dict[str, Any]],
        artifact_records: Iterable[dict[str, Any]],
        *,
        target: str,
    ) -> tuple[bool, str | None]:
        blocks_ok, block_error = self._verify_change_records(change_records, target)
        if not blocks_ok:
            return False, block_error
        try:
            for record in artifact_records:
                current = self._artifact_snapshot(self._artifact_path(record["relative_path"]))
                if not _snapshots_equal(current, record[target]):
                    return False, f"Artifact verification failed: {record['relative_path']}"
        except Exception as exc:
            return False, str(exc)
        return True, None

    def _record_apply_failure(
        self,
        path: Path,
        entry: dict[str, Any],
        *,
        stage: str,
        error: Exception,
        rolled_back: bool,
        rollback_error: str | None,
    ) -> None:
        failed = copy.deepcopy(entry)
        failed["status"] = "failed"
        failed["failed_at"] = _utc_now()
        failed["failure"] = {
            "stage": stage,
            "error": f"{type(error).__name__}: {error}",
            "rolled_back": rolled_back,
            "rollback_error": rollback_error,
        }
        # A failed status update must never destroy the original durable pending
        # entry. Atomic replacement gives us exactly that fallback.
        self._safe_write_entry(path, failed)

    def _select_revert_entry(
        self, operation_id: str | None
    ) -> tuple[Path, dict[str, Any]]:
        if operation_id is not None:
            path = self._find_entry_path(operation_id)
            entry = self._load_entry(path)
            if not _entry_is_revertible(entry):
                raise JournalError(
                    f"Journal entry {operation_id} is not revertible in status {entry['status']}.",
                    operation_id=operation_id,
                    status=entry["status"],
                )
            return path, entry

        found_any = False
        for path in self._entry_files_newest_first():
            found_any = True
            # Fail closed on a corrupt newest candidate; silently stepping over
            # it could revert the wrong operation.
            entry = self._load_entry(path)
            if _entry_is_revertible(entry):
                return path, entry
        if found_any:
            raise JournalEmptyError("No committed or recoverable pending journal entry remains.")
        raise JournalEmptyError("The current world has no journal entries.")

    def _find_entry_path(self, operation_id: str) -> Path:
        self._assert_journal_directory_current()
        if not _OPERATION_ID_RE.fullmatch(str(operation_id)):
            raise JournalNotFoundError("Invalid journal operation id.", operation_id=operation_id)
        matches = list(self.directory.glob(f"*_{operation_id}.json"))
        if len(matches) != 1:
            raise JournalNotFoundError(
                f"Journal entry not found: {operation_id}", operation_id=operation_id
            )
        return matches[0]

    def _load_entry(self, path: Path) -> dict[str, Any]:
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
            self._validate_entry(data, path)
            return data
        except JournalError:
            raise
        except Exception as exc:
            raise JournalCorruptError(
                f"Invalid journal entry {path.name}: {exc}", path=str(path)
            ) from exc

    def _validate_entry(self, entry: Any, path: Path) -> None:
        if not isinstance(entry, dict):
            raise JournalCorruptError(f"Journal entry is not an object: {path.name}")
        required = {
            "schema_version",
            "operation_id",
            "world_identity",
            "world_path_hash",
            "dimension",
            "timestamp",
            "tool",
            "status",
            "changes",
        }
        missing = required - set(entry)
        if missing:
            raise JournalCorruptError(f"Journal entry lacks fields: {sorted(missing)}")
        if entry["schema_version"] != SCHEMA_VERSION:
            raise JournalCorruptError(
                f"Unsupported journal schema version: {entry['schema_version']!r}"
            )
        if not _OPERATION_ID_RE.fullmatch(str(entry["operation_id"])):
            raise JournalCorruptError("Invalid operation_id in journal entry.")
        if entry["status"] not in _ENTRY_STATUSES:
            raise JournalCorruptError(f"Invalid journal status: {entry['status']!r}")
        if not isinstance(entry["changes"], list):
            raise JournalCorruptError("Journal changes must be an array.")
        seen: set[BlockPos] = set()
        for record in entry["changes"]:
            if not isinstance(record, dict) or set(("pos", "before", "after")) - set(record):
                raise JournalCorruptError("Malformed block change record.")
            pos = _pos_from_dict(record["pos"])
            if pos in seen:
                raise JournalCorruptError(f"Duplicate journal position: {pos}")
            seen.add(pos)
            _state_from_dict(record["before"])
            _state_from_dict(record["after"])
        artifacts = entry.get("artifacts", [])
        if not isinstance(artifacts, list):
            raise JournalCorruptError("Journal artifacts must be an array.")
        artifact_positions: set[BlockPos] = set()
        for artifact in artifacts:
            if not isinstance(artifact, dict) or artifact.get("kind") != "file":
                raise JournalCorruptError("Malformed companion artifact record.")
            self._artifact_path(artifact.get("relative_path"))
            positions = artifact.get("compound_positions")
            if not isinstance(positions, list) or not positions:
                raise JournalCorruptError("Artifact lacks compound block positions.")
            for value in positions:
                pos = _pos_from_dict(value)
                if pos not in seen:
                    raise JournalCorruptError(
                        "Artifact references a position absent from changes."
                    )
                if pos in artifact_positions:
                    raise JournalCorruptError(
                        "A block belongs to multiple artifact compound groups."
                    )
                artifact_positions.add(pos)
            for key in ("before", "after"):
                snapshot = artifact.get(key)
                if snapshot and snapshot.get("exists"):
                    _snapshot_bytes(snapshot)
                else:
                    _validate_absent(snapshot)

    def _validate_world(self, entry: dict[str, Any]) -> None:
        if (
            entry["world_identity"] != self.world_identity
            or entry["world_path_hash"] != self.world_path_hash
            or entry["dimension"] != self.dimension
        ):
            raise JournalWorldMismatchError(
                "Journal entry belongs to a different world path or dimension.",
                operation_id=entry.get("operation_id"),
            )

    def _entry_summary(self, entry: dict[str, Any], path: Path) -> dict[str, Any]:
        return {
            "operation_id": entry["operation_id"],
            "status": entry["status"],
            "tool": entry["tool"],
            "pass": entry.get("pass"),
            "timestamp": entry["timestamp"],
            "applied_at": entry.get("applied_at"),
            "seed": entry.get("seed"),
            "change_count": len(entry["changes"]),
            "artifact_count": len(entry.get("artifacts") or []),
            "instance_id": entry.get("instance_id"),
            "layer": entry.get("layer"),
            "on_behalf_of": entry.get("on_behalf_of"),
            "path": str(path),
        }

    def _entry_files_newest_first(self) -> list[Path]:
        self._assert_journal_directory_current()
        return sorted(self.directory.glob("*.json"), key=lambda path: path.name, reverse=True)

    def _entry_path(self, timestamp: str, tool: str, seed: int | None, operation_id: str) -> Path:
        compact = timestamp.replace("+00:00", "Z").replace("-", "").replace(":", "")
        safe_tool = _SAFE_TOOL_RE.sub("_", str(tool)).strip("_") or "operation"
        seed_token = "noseed" if seed is None else str(seed)
        return self.directory / f"{compact}_{safe_tool}_{seed_token}_{operation_id}.json"

    def _write_entry(self, path: Path, entry: dict[str, Any], *, create: bool = False) -> None:
        self._assert_journal_directory_current()
        encoded = json.dumps(
            entry,
            ensure_ascii=False,
            indent=2,
            sort_keys=True,
        ).encode("utf-8")
        _atomic_replace_bytes(path, encoded, create=create)

    def _safe_write_entry(self, path: Path, entry: dict[str, Any]) -> None:
        try:
            self._write_entry(path, entry)
        except Exception:
            # The previous complete entry remains durable. Callers report the
            # primary failure; inspection can reconcile the retained state.
            pass


def _atomic_replace_bytes(path: Path, data: bytes, *, create: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temp_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=path.parent,
            prefix=f".{path.name}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temp_path = Path(handle.name)
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())
        if create:
            # Publishing a hard link is atomic and exclusive. Unlike
            # os.replace, it cannot overwrite a file created by an external
            # writer between our snapshot and this commit point.
            _OS_LINK(temp_path, path)
            linked_temp = temp_path
            temp_path = None
            try:
                linked_temp.unlink()
            except OSError:
                # The destination is already safely published. A stale hidden
                # temp file is preferable to falsely reporting transaction
                # failure after the commit point.
                pass
        else:
            _OS_REPLACE(temp_path, path)
            temp_path = None
        _fsync_directory(path.parent)
    finally:
        if temp_path is not None:
            try:
                temp_path.unlink()
            except FileNotFoundError:
                pass


def _fsync_directory(directory: Path) -> None:
    # Windows does not permit opening directories with os.open. The file itself
    # is still fsynced; POSIX additionally gets a durable rename barrier.
    if os.name == "nt":
        return
    descriptor = os.open(directory, os.O_RDONLY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)


def _utc_now() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="microseconds")


def _state_to_dict(state: BlockState) -> dict[str, Any]:
    return {"id": state.full_id, "properties": dict(sorted(state.properties.items()))}


def _state_from_dict(value: Any) -> BlockState:
    if not isinstance(value, dict):
        raise JournalCorruptError("Block state must be an object.")
    block_id = value.get("id")
    properties = value.get("properties", {})
    if not isinstance(block_id, str) or ":" not in block_id:
        raise JournalCorruptError(f"Invalid block state id: {block_id!r}")
    if not isinstance(properties, dict) or not all(
        isinstance(key, str) and isinstance(item, str) for key, item in properties.items()
    ):
        raise JournalCorruptError("Block state properties must be string pairs.")
    return BlockState.from_id(block_id, properties)


def _pos_from_dict(value: Any) -> BlockPos:
    if not isinstance(value, dict):
        raise JournalCorruptError("Block position must be an object.")
    try:
        values = (value["x"], value["y"], value["z"])
        if any(isinstance(item, bool) or not isinstance(item, int) for item in values):
            raise TypeError
        return BlockPos(*values)
    except (KeyError, TypeError) as exc:
        raise JournalCorruptError(f"Invalid block position: {value!r}") from exc


def _bytes_snapshot(data: bytes) -> dict[str, Any]:
    return {
        "exists": True,
        "content_base64": base64.b64encode(data).decode("ascii"),
        "sha256": hashlib.sha256(data).hexdigest(),
    }


def _snapshot_bytes(snapshot: Any) -> bytes:
    if not isinstance(snapshot, dict) or snapshot.get("exists") is not True:
        raise JournalCorruptError("Expected a present artifact snapshot.")
    encoded = snapshot.get("content_base64")
    expected_hash = snapshot.get("sha256")
    if not isinstance(encoded, str) or not isinstance(expected_hash, str):
        raise JournalCorruptError("Malformed artifact snapshot.")
    try:
        data = base64.b64decode(encoded, validate=True)
    except Exception as exc:
        raise JournalCorruptError("Invalid base64 artifact content.") from exc
    if hashlib.sha256(data).hexdigest() != expected_hash:
        raise JournalCorruptError("Artifact snapshot checksum mismatch.")
    return data


def _validate_absent(snapshot: Any) -> None:
    if not isinstance(snapshot, dict) or snapshot != {
        "exists": False,
        "content_base64": None,
        "sha256": None,
    }:
        raise JournalCorruptError("Malformed absent artifact snapshot.")


def _snapshots_equal(left: dict[str, Any], right: dict[str, Any]) -> bool:
    return (
        left.get("exists") == right.get("exists")
        and left.get("sha256") == right.get("sha256")
        and left.get("content_base64") == right.get("content_base64")
    )


def _json_safe(value: Any) -> Any:
    # Round-tripping here both rejects unserializable caller metadata before the
    # pending file is written and detaches it from mutable input dictionaries.
    return json.loads(json.dumps(value, ensure_ascii=False))


def _entry_is_revertible(entry: dict[str, Any]) -> bool:
    if entry.get("status") in {"committed", "pending", "revert_pending"}:
        return True
    failure = entry.get("failure")
    return (
        entry.get("status") == "failed"
        and isinstance(failure, dict)
        and failure.get("rolled_back") is False
    )


def _entry_allows_before_recovery(entry: dict[str, Any]) -> bool:
    return entry.get("status") in {"pending", "revert_pending"} or (
        entry.get("status") == "failed"
        and isinstance(entry.get("failure"), dict)
        and entry["failure"].get("rolled_back") is False
    )
