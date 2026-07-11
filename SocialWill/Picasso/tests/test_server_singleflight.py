from __future__ import annotations

import threading
import time
from concurrent.futures import ThreadPoolExecutor

from picasso.server import SingleFlightFastMCP


def test_registered_tools_are_single_flight_across_threads() -> None:
    server = SingleFlightFastMCP("single-flight-test")
    state_lock = threading.Lock()
    active = 0
    maximum_active = 0

    @server.tool()
    def probe(delay_seconds: float) -> dict:
        nonlocal active, maximum_active
        with state_lock:
            active += 1
            maximum_active = max(maximum_active, active)
        try:
            time.sleep(delay_seconds)
            return {"ok": True}
        finally:
            with state_lock:
                active -= 1

    registered = server._tool_manager._tools["probe"].fn
    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(registered, [0.02] * 4))

    assert results == [{"ok": True}] * 4
    assert maximum_active == 1


def test_async_tools_are_rejected_until_async_locking_exists() -> None:
    server = SingleFlightFastMCP("async-rejection-test")

    async def unsupported() -> dict:
        return {"ok": True}

    try:
        server.tool()(unsupported)
    except TypeError as exc:
        assert "must remain synchronous" in str(exc)
    else:  # pragma: no cover - explicit failure message is clearer than pytest.raises here
        raise AssertionError("async tool registration should be rejected")
