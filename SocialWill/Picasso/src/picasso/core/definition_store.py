from __future__ import annotations

import json
import os
import tempfile
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


class DefinitionAlreadyExistsError(FileExistsError):
    pass


def save_json_definition(
    directory: str | Path,
    name: str,
    payload: dict[str, Any],
    *,
    overwrite: bool,
) -> tuple[Path, Path | None]:
    """Durably publish one definition and optionally archive its predecessor.

    New definitions use an exclusive hard-link publication so a writer racing
    us cannot be overwritten. Replacements use same-directory ``os.replace``;
    the original is copied to a collision-proof archive before publication.
    """

    root = Path(directory)
    root.mkdir(parents=True, exist_ok=True)
    existing = find_json_definition(root, name)
    if existing is not None and not overwrite:
        raise DefinitionAlreadyExistsError(f"Definition {name!r} already exists")

    target = existing if existing is not None else root / f"{name}.json"
    archived = _archive_definition(existing) if existing is not None else None
    data = (json.dumps(payload, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode(
        "utf-8"
    )
    temporary_path: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            dir=root,
            prefix=f".{target.name}.",
            suffix=".tmp",
            delete=False,
        ) as handle:
            temporary_path = Path(handle.name)
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())

        if overwrite:
            os.replace(temporary_path, target)
        else:
            try:
                os.link(temporary_path, target)
            except FileExistsError as exc:
                raise DefinitionAlreadyExistsError(
                    f"Definition {name!r} already exists"
                ) from exc
        temporary_path.unlink(missing_ok=True)
        temporary_path = None
        return target, archived
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def find_json_definition(directory: str | Path, name: str) -> Path | None:
    root = Path(directory)
    if not root.exists():
        return None
    expected = f"{name}.json".casefold()
    return next(
        (
            path
            for path in sorted(root.glob("*.json"), key=lambda item: item.name.casefold())
            if path.name.casefold() == expected
        ),
        None,
    )


def _archive_definition(path: Path) -> Path:
    archive_dir = path.parent / "_replaced"
    archive_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    archive_path = archive_dir / f"{path.stem}.{stamp}.{uuid.uuid4().hex[:8]}.json"
    data = path.read_bytes()
    try:
        with archive_path.open("xb") as handle:
            handle.write(data)
            handle.flush()
            os.fsync(handle.fileno())
    except Exception:
        archive_path.unlink(missing_ok=True)
        raise
    return archive_path
