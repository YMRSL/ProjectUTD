from __future__ import annotations

import math
import random


class NoiseField:
    def __init__(self, seed: int) -> None:
        self.seed = seed
        try:
            import noise
        except Exception:
            noise = None
        self._noise = noise

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
