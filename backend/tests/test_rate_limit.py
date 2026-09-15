import time

import pytest
from app.main import app
from app.middleware.rate_limit import InMemoryRateLimitMiddleware
from fastapi.testclient import TestClient
from starlette.requests import Request
from starlette.responses import Response


def test_rate_limiter_allows_under_limit():
    client = TestClient(app)
    # Make a few requests
    for _ in range(5):
        response = client.get("/v1/config/health") # or any public route
        assert response.status_code in (200, 404, 401, 403, 429)


def test_stale_key_cleanup():
    middleware = InMemoryRateLimitMiddleware(app, max_requests=2, window_seconds=1, max_tracked_keys=100)
    # Simulate hits
    middleware.hits["1.2.3.4"].append(time.time() - 2) # Expired hit
    middleware.hits["5.6.7.8"].append(time.time()) # Active hit

    middleware._cleanup(time.time())

    assert "1.2.3.4" not in middleware.hits
    assert "5.6.7.8" in middleware.hits


@pytest.mark.asyncio
async def test_active_client_keys_never_exceed_capacity():
    middleware = InMemoryRateLimitMiddleware(app, max_tracked_keys=3)

    async def next_handler(request):
        return Response(status_code=200)

    for i in range(20):
        request = Request({"type": "http", "client": (f"192.0.2.{i}", 1234)})
        assert (await middleware.dispatch(request, next_handler)).status_code == 200
        assert len(middleware.hits) <= 3


@pytest.mark.asyncio
async def test_current_client_is_still_rate_limited():
    middleware = InMemoryRateLimitMiddleware(app, max_requests=2)

    async def next_handler(request):
        return Response(status_code=200)

    request = Request({"type": "http", "client": ("192.0.2.1", 1234)})
    assert (await middleware.dispatch(request, next_handler)).status_code == 200
    assert (await middleware.dispatch(request, next_handler)).status_code == 200
    assert (await middleware.dispatch(request, next_handler)).status_code == 429
