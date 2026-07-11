from __future__ import annotations

import ctypes
import hashlib
import json
import os
import tempfile
import time
from dataclasses import dataclass
from pathlib import Path


LOCK_DIR = Path(tempfile.gettempdir()) / "picasso_world_locks"


class WorldLockError(RuntimeError):
    def __init__(self, lock_path: Path, owner: dict) -> None:
        self.lock_path = lock_path
        self.owner = owner
        super().__init__(f"World is already locked by pid={owner.get('pid')}: {lock_path}")


@dataclass
class WorldLock:
    lock_path: Path
    world_key: str

    def release(self) -> bool:
        try:
            self.lock_path.unlink()
            return True
        except FileNotFoundError:
            return False


def acquire_world_lock(world_path: str | Path) -> WorldLock:
    LOCK_DIR.mkdir(parents=True, exist_ok=True)
    world_key = normalize_world_key(world_path)
    lock_path = _lock_path(world_key)
    _remove_stale_lock(lock_path)
    payload = {
        "pid": os.getpid(),
        "created_at": time.time(),
        "world_key": world_key,
        "requested_path": str(world_path),
    }
    try:
        fd = os.open(str(lock_path), os.O_WRONLY | os.O_CREAT | os.O_EXCL)
    except FileExistsError as exc:
        raise WorldLockError(lock_path, _read_lock(lock_path)) from exc
    with os.fdopen(fd, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
    return WorldLock(lock_path=lock_path, world_key=world_key)


def normalize_world_key(world_path: str | Path) -> str:
    resolved = Path(world_path).expanduser().resolve(strict=False)
    value = str(resolved)
    return value.lower() if os.name == "nt" else value


def _lock_path(world_key: str) -> Path:
    digest = hashlib.sha256(world_key.encode("utf-8")).hexdigest()[:24]
    return LOCK_DIR / f"{digest}.lock"


def _remove_stale_lock(lock_path: Path) -> None:
    if not lock_path.exists():
        return
    owner = _read_lock(lock_path)
    pid = owner.get("pid")
    if not isinstance(pid, int) or not _pid_exists(pid):
        try:
            lock_path.unlink()
        except FileNotFoundError:
            pass
        return
    raise WorldLockError(lock_path, owner)


def _read_lock(lock_path: Path) -> dict:
    try:
        return json.loads(lock_path.read_text(encoding="utf-8"))
    except Exception:
        return {"pid": None, "lock_path": str(lock_path)}


def _pid_exists(pid: int) -> bool:
    if pid <= 0:
        return False
    if pid == os.getpid():
        return True
    if os.name == "nt":
        return _windows_pid_exists(pid)
    try:
        os.kill(pid, 0)
    except OSError:
        return False
    return True


def _windows_pid_exists(pid: int) -> bool:
    process_query_limited_information = 0x1000
    handle = ctypes.windll.kernel32.OpenProcess(
        process_query_limited_information, False, int(pid)
    )
    if not handle:
        return False
    ctypes.windll.kernel32.CloseHandle(handle)
    return True
