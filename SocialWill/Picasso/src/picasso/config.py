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


def _paths_from_env(name: str, legacy_name: str, default: Path) -> list[Path]:
    value = os.getenv(name)
    if value:
        return [Path(part).expanduser() for part in value.split(os.pathsep) if part]
    legacy = os.getenv(legacy_name)
    if legacy:
        return [Path(legacy).expanduser()]
    return [default]


def _bool_from_env(name: str, default: bool = False) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Config:
    catalog_paths: list[Path]
    passes_dir: Path
    patterns_dir: Path
    safe_blocks_path: Path
    fragments_dir: Path
    bundles_dir: Path
    brushes_dir: Path
    room_templates_dir: Path
    fingerprints_path: Path
    profiles_dir: Path
    block_taxonomy_path: Path
    build_log_dir: Path | None
    log_level: str
    noise_backend: str
    modded_write_verified: bool
    max_radius_chunks: int
    dimension: str = "minecraft:overworld"
    flat_variance_threshold: float = 1.5
    min_structure_area: int = 50
    min_room_volume: int = 12
    min_stadium_volume: int = 2000
    ground_detection_radius: int = 3
    variant_fatigue_ratio: int = 12
    site_join_dist: int = 16
    site_join_gap_min: int = 90
    site_min_events: int = 8
    protection_min_events: int = 3
    protection_lookback_days: int = 14

    @property
    def catalog_path(self) -> Path:
        return self.catalog_paths[0]


def load_config() -> Config:
    load_dotenv(PROJECT_ROOT / ".env")

    default_catalog = DATA_DIR / "catalog" / "doomsday_decoration_semantic.json"
    catalog_paths = _paths_from_env("PICASSO_CATALOG_PATHS", "PICASSO_CATALOG_PATH", default_catalog)

    build_log_value = os.getenv("PICASSO_BUILD_LOG_DIR")
    return Config(
        catalog_paths=catalog_paths,
        passes_dir=_path_from_env("PICASSO_PASSES_DIR", DATA_DIR / "passes"),
        patterns_dir=_path_from_env("PICASSO_PATTERNS_DIR", DATA_DIR / "patterns"),
        safe_blocks_path=_path_from_env("PICASSO_SAFE_BLOCKS", DATA_DIR / "safe_blocks.json"),
        fragments_dir=_path_from_env("PICASSO_FRAGMENTS_DIR", DATA_DIR / "fragments"),
        bundles_dir=_path_from_env("PICASSO_BUNDLES_DIR", DATA_DIR / "bundles"),
        brushes_dir=_path_from_env("PICASSO_BRUSHES_DIR", DATA_DIR / "brushes"),
        room_templates_dir=_path_from_env(
            "PICASSO_ROOM_TEMPLATES_DIR", DATA_DIR / "room_templates"
        ),
        fingerprints_path=_path_from_env(
            "PICASSO_FINGERPRINTS_PATH", DATA_DIR / "structure_fingerprints.json"
        ),
        profiles_dir=_path_from_env("PICASSO_PROFILES_DIR", DATA_DIR / "profiles"),
        block_taxonomy_path=_path_from_env(
            "PICASSO_BLOCK_TAXONOMY", DATA_DIR / "block_taxonomy.json"
        ),
        build_log_dir=Path(build_log_value).expanduser() if build_log_value else None,
        log_level=os.getenv("PICASSO_LOG_LEVEL", "INFO").upper(),
        noise_backend=os.getenv("PICASSO_NOISE_BACKEND", "auto").lower(),
        modded_write_verified=_bool_from_env("PICASSO_MODDED_WRITE_VERIFIED", False),
        max_radius_chunks=int(os.getenv("PICASSO_MAX_RADIUS_CHUNKS", "12")),
        flat_variance_threshold=float(os.getenv("PICASSO_FLAT_VARIANCE_THRESHOLD", "1.5")),
        min_structure_area=int(os.getenv("PICASSO_MIN_STRUCTURE_AREA", "50")),
        min_room_volume=int(os.getenv("PICASSO_MIN_ROOM_VOLUME", "12")),
        min_stadium_volume=int(os.getenv("PICASSO_MIN_STADIUM_VOLUME", "2000")),
        ground_detection_radius=int(os.getenv("PICASSO_GROUND_DETECTION_RADIUS", "3")),
        variant_fatigue_ratio=int(os.getenv("PICASSO_VARIANT_FATIGUE_RATIO", "12")),
        site_join_dist=int(os.getenv("PICASSO_SITE_JOIN_DIST", "16")),
        site_join_gap_min=int(os.getenv("PICASSO_SITE_JOIN_GAP_MIN", "90")),
        site_min_events=int(os.getenv("PICASSO_SITE_MIN_EVENTS", "8")),
        protection_min_events=int(os.getenv("PICASSO_PROTECTION_MIN_EVENTS", "3")),
        protection_lookback_days=int(os.getenv("PICASSO_PROTECTION_LOOKBACK_DAYS", "14")),
    )


config = load_config()
