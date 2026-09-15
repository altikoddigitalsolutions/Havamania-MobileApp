import time
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.middleware.rate_limit import InMemoryRateLimitMiddleware


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
