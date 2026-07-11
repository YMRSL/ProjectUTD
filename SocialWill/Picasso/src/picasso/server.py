from __future__ import annotations

import inspect
import logging
from functools import wraps
from typing import Any, Callable

from mcp.server.fastmcp import FastMCP

from picasso import prompts
from picasso.config import config
from picasso.core.bundle_library import load_bundle_registry
from picasso.core.catalog_index import CatalogIndex
from picasso.core.fragment_library import FragmentLibrary
from picasso.core.noise_field import resolve_noise_backend
from picasso.core.pattern_matcher import PatternMatcher
from picasso.core.style_engine import load_pass_registry
from picasso.session import session
from picasso.tools import (
    activity,
    analysis,
    bundle,
    catalog,
    diagnostics,
    inspection,
    journal,
    learning,
    npc,
    style,
    world_io,
)


logging.basicConfig(
    level=getattr(logging, config.log_level, logging.INFO),
    format="%(asctime)s %(levelname)s %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


class SingleFlightFastMCP(FastMCP):
    """Serialize every Picasso tool call around the shared world session.

    MCP stdio is a transport, not a single-flight guarantee. The SDK currently
    invokes our synchronous handlers on the event-loop thread, but correctness
    must not depend on that implementation detail changing in a future release.
    """

    def tool(self, *args: Any, **kwargs: Any) -> Callable:
        register = super().tool(*args, **kwargs)

        def decorator(function: Callable) -> Callable:
            if inspect.iscoroutinefunction(function):
                raise TypeError(
                    "Picasso MCP tools must remain synchronous until async "
                    "single-flight locking is implemented."
                )

            @wraps(function)
            def locked(*function_args: Any, **function_kwargs: Any) -> Any:
                with session.operation_lock:
                    return function(*function_args, **function_kwargs)

            return register(locked)

        return decorator


mcp = SingleFlightFastMCP("picasso", instructions=prompts.SERVER_INSTRUCTIONS)


def initialize() -> None:
    session.noise_backend = resolve_noise_backend()
    session.catalog = CatalogIndex(config.catalog_paths)
    session.pattern_matcher = PatternMatcher(config.patterns_dir)
    session.fragment_library = FragmentLibrary(config.fragments_dir)
    session.pass_registry = load_pass_registry(config.passes_dir)
    session.bundle_registry = load_bundle_registry(
        config.bundles_dir,
        session.pass_registry,
    )
    logger.info("Loaded catalog entries: %s", len(session.catalog.entries) if session.catalog else 0)
    logger.info("Loaded style passes: %s", len(session.pass_registry))
    logger.info(
        "Loaded fragments: %s",
        len(session.fragment_library.fragments) if session.fragment_library else 0,
    )
    logger.info("Loaded bundles: %s", len(session.bundle_registry))
    logger.info("Noise backend: %s", session.noise_backend)


def register_tools() -> None:
    world_io.register(mcp)
    analysis.register(mcp)
    inspection.register(mcp)
    catalog.register(mcp)
    style.register(mcp)
    bundle.register(mcp)
    journal.register(mcp)
    activity.register(mcp)
    learning.register(mcp)
    npc.register(mcp)
    diagnostics.register(mcp)


def register_prompts() -> None:
    prompts.register(mcp)


def main() -> None:
    initialize()
    register_tools()
    register_prompts()
    mcp.run()


if __name__ == "__main__":
    main()
