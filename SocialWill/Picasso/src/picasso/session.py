from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from picasso.models.region import RegionData
from picasso.models.style_pass import StylePass


@dataclass
class PicassoSession:
    world_path: Path | None = None
    bridge: Any | None = None
    catalog: Any | None = None
    pattern_matcher: Any | None = None
    fragment_library: Any | None = None
    bundle_registry: dict[str, dict] = field(default_factory=dict)
    pass_registry: dict[str, StylePass] = field(default_factory=dict)
    last_region: RegionData | None = None

    def close_bridge(self) -> None:
        if self.bridge is not None:
            self.bridge.close()
        self.bridge = None
        self.world_path = None
        self.last_region = None


session = PicassoSession()
