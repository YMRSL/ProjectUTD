from __future__ import annotations

import json
import logging
from pathlib import Path

from mcp.server.fastmcp import FastMCP

from picasso.config import config
from picasso.core.catalog_index import CatalogIndex
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.pattern_matcher import PatternMatcher
from picasso.core.style_engine import load_pass_registry
from picasso.session import session
from picasso.tools import analysis, bundle, catalog, learning, npc, style, world_io


logging.basicConfig(
    level=getattr(logging, config.log_level, logging.INFO),
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

mcp = FastMCP("picasso")


def load_bundles(bundles_dir: str | Path) -> dict[str, dict]:
    path = Path(bundles_dir)
    registry: dict[str, dict] = {}
    if not path.exists():
        return registry
    for bundle_path in sorted(path.glob("*.json")):
        data = json.loads(bundle_path.read_text(encoding="utf-8"))
        registry[data["name"]] = data
    return registry


def initialize() -> None:
    session.catalog = CatalogIndex(config.catalog_path)
    session.pattern_matcher = PatternMatcher(config.patterns_dir)
    session.fragment_library = FragmentLibrary(config.fragments_dir)
    session.pass_registry = load_pass_registry(config.passes_dir)
    session.bundle_registry = load_bundles(config.bundles_dir)
    logger.info("Loaded catalog entries: %s", len(session.catalog.entries) if session.catalog else 0)
    logger.info("Loaded style passes: %s", len(session.pass_registry))
    logger.info(
        "Loaded fragments: %s",
        len(session.fragment_library.fragments) if session.fragment_library else 0,
    )
    logger.info("Loaded bundles: %s", len(session.bundle_registry))


def register_tools() -> None:
    world_io.register(mcp)
    analysis.register(mcp)
    catalog.register(mcp)
    style.register(mcp)
    bundle.register(mcp)
    learning.register(mcp)
    npc.register(mcp)


def main() -> None:
    initialize()
    register_tools()
    mcp.run()


if __name__ == "__main__":
    main()
