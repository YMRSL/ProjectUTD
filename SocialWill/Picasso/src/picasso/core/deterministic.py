from __future__ import annotations

import hashlib
from typing import Any


def roll(seed: int, *parts: Any) -> float:
    payload = ":".join(str(part) for part in (seed, *parts)).encode("utf-8")
    digest = hashlib.sha256(payload).digest()
    return int.from_bytes(digest[:8], "big") / 2**64


def choice_index(seed: int, count: int, *parts: Any) -> int:
    if count <= 0:
        raise ValueError("count must be > 0")
    return min(count - 1, int(roll(seed, *parts) * count))
