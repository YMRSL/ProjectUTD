from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


PACKAGE_ROOT = Path(__file__).resolve().parent
PROJECT_ROOT = PACKAGE_ROOT.parents[2]
DATA_DIR = PACKAGE_ROOT / "data"


def _path_from_env(name: str, default: Path) -> Path:
    value = os.getenv(name)
    return Path(value).expanduser() if value else default


@dataclass(frozen=True)
class Config:
    catalog_path: Path
    passes_dir: Path
    patterns_dir: Path
    safe_blocks_path: Path
    fragments_dir: Path
    bundles_dir: Path
    fingerprints_path: Path
    log_level: str
    dimension: str = "minecraft:overworld"
    flat_variance_threshold: float = 1.5
    min_structure_area: int = 50
    min_room_volume: int = 12
    ground_detection_radius: int = 3


def load_config() -> Config:
    load_dotenv(PROJECT_ROOT / ".env")

    default_catalog = (
        Path(r"D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\DoomsdayDecoration")
        / "doomsday_decoration_semantic.json"
    )
    if not default_catalog.exists():
        default_catalog = DATA_DIR / "catalog" / "doomsday_decoration_semantic.json"

    return Config(
        catalog_path=_path_from_env("PICASSO_CATALOG_PATH", default_catalog),
        passes_dir=_path_from_env("PICASSO_PASSES_DIR", DATA_DIR / "passes"),
        patterns_dir=_path_from_env("PICASSO_PATTERNS_DIR", DATA_DIR / "patterns"),
        safe_blocks_path=_path_from_env("PICASSO_SAFE_BLOCKS", DATA_DIR / "safe_blocks.json"),
        fragments_dir=_path_from_env("PICASSO_FRAGMENTS_DIR", DATA_DIR / "fragments"),
        bundles_dir=_path_from_env("PICASSO_BUNDLES_DIR", DATA_DIR / "bundles"),
        fingerprints_path=_path_from_env(
            "PICASSO_FINGERPRINTS_PATH", DATA_DIR / "structure_fingerprints.json"
        ),
        log_level=os.getenv("PICASSO_LOG_LEVEL", "INFO").upper(),
        flat_variance_threshold=float(os.getenv("PICASSO_FLAT_VARIANCE_THRESHOLD", "1.5")),
        min_structure_area=int(os.getenv("PICASSO_MIN_STRUCTURE_AREA", "50")),
        min_room_volume=int(os.getenv("PICASSO_MIN_ROOM_VOLUME", "12")),
        ground_detection_radius=int(os.getenv("PICASSO_GROUND_DETECTION_RADIUS", "3")),
    )


config = load_config()
