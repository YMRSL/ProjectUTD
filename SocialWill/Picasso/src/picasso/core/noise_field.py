from __future__ import annotations

import math
import random

from picasso.config import config


_RESOLVED_BACKEND: str | None = None
_NOISE_MODULE = None


def resolve_noise_backend() -> str:
    global _RESOLVED_BACKEND, _NOISE_MODULE
    if _RESOLVED_BACKEND is not None:
        return _RESOLVED_BACKEND

    requested = config.noise_backend
    if requested not in {"auto", "c", "fallback"}:
        raise RuntimeError(
            "PICASSO_NOISE_BACKEND must be one of: auto, c, fallback "
            f"(got {requested!r})"
        )
    try:
        import noise
    except Exception:
        noise = None

    if requested == "c" and noise is None:
        raise RuntimeError("PICASSO_NOISE_BACKEND=c but the optional `noise` package is absent")
    if requested == "fallback" or noise is None:
        _RESOLVED_BACKEND = "fallback"
        _NOISE_MODULE = None
    else:
        _RESOLVED_BACKEND = "c"
        _NOISE_MODULE = noise
    return _RESOLVED_BACKEND


class NoiseField:
    def __init__(self, seed: int) -> None:
        self.seed = seed
        self.backend = resolve_noise_backend()
        self._noise = _NOISE_MODULE

    def sample_2d(self, x: float, z: float, scale: float) -> float:
        if self._noise is not None:
            value = self._noise.pnoise2(x * scale, z * scale, octaves=4, base=self.seed)
            return max(0.0, min(1.0, (value + 1.0) / 2.0))
        return self._fallback_noise(x, 0.0, z, scale)

    def sample_3d(self, x: float, y: float, z: float, scale: float) -> float:
        if self._noise is not None:
            value = self._noise.pnoise3(x * scale, y * scale, z * scale, octaves=4, base=self.seed)
            return max(0.0, min(1.0, (value + 1.0) / 2.0))
        return self._fallback_noise(x, y, z, scale)

    def _fallback_noise(self, x: float, y: float, z: float, scale: float) -> float:
        bucket = (
            math.floor(x * scale * 64),
            math.floor(y * scale * 64),
            math.floor(z * scale * 64),
            self.seed,
        )
        rng = random.Random(hash(bucket))
        return rng.random()
