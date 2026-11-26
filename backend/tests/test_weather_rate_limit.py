import pytest
from app.dependencies import rate_limit
from app.main import app
from app.services.weather.service import weather_service


@pytest.fixture(autouse=True)
def isolated_weather_limit(monkeypatch):
    rate_limit._hits.clear()
    payload = dict(location={}, provider="local-test", **{name: None for name in (
        "temperature", "humidity", "feels_like", "wind_speed", "wind_direction",
        "uv_index", "pressure", "visibility", "cloud_cover")})
    monkeypatch.setattr(weather_service, "get_current", lambda **kwargs: payload)
    yield
    rate_limit._hits.clear()


def test_weather_openapi_does_not_offer_client_rate_settings():
    for path, operations in app.openapi()["paths"].items():
        if path.startswith("/v1/weather/"):
            names = {p["name"] for p in operations["get"].get("parameters", [])}
            assert not {"max_requests", "window_seconds"} & names


@pytest.mark.parametrize("overrides", [
    {"max_requests": 1000000}, {"window_seconds": -1},
])
def test_client_query_cannot_bypass_weather_quota(client, overrides):
    query = dict(lat=41, lon=29, **overrides)
    for _ in range(60):
        assert client.get("/v1/weather/current", params=query).status_code == 200
    response = client.get("/v1/weather/current", params=query)
    assert response.status_code == 429
    assert 1 <= int(response.headers["Retry-After"]) <= 60


def test_window_expires_at_boundary_and_returns_wait_time(monkeypatch):
    from types import SimpleNamespace
    from starlette.requests import Request
    from fastapi import HTTPException
    clock = [100.0]
    monkeypatch.setattr(rate_limit, "time", SimpleNamespace(monotonic=lambda: clock[0]))
    monkeypatch.setattr(rate_limit, "_MAX_REQUESTS", 2)
    request = Request({"type": "http", "client": ("192.0.2.1", 123)})
    rate_limit.weather_rate_limit(request)
    rate_limit.weather_rate_limit(request)
    clock[0] += 1.2
    with pytest.raises(HTTPException) as error:
        rate_limit.weather_rate_limit(request)
    assert error.value.headers["Retry-After"] == "59"
    clock[0] = 160.0
    rate_limit.weather_rate_limit(request)


def test_concurrent_requests_cannot_overbook_same_client(monkeypatch):
    from concurrent.futures import ThreadPoolExecutor
    from fastapi import HTTPException
    from starlette.requests import Request
    monkeypatch.setattr(rate_limit, "_MAX_REQUESTS", 5)
    request = Request({"type": "http", "client": ("192.0.2.1", 123)})
    def send(_):
        try:
            rate_limit.weather_rate_limit(request)
            return 200
        except HTTPException as error:
            return error.status_code
    with ThreadPoolExecutor(max_workers=16) as pool:
        results = list(pool.map(send, range(100)))
    assert results.count(200) == 5
    assert results.count(429) == 95


def test_capacity_does_not_evict_live_quota_and_recovers_after_expiry(monkeypatch):
    from types import SimpleNamespace
    from starlette.requests import Request
    from fastapi import HTTPException
    clock = [100.0]
    monkeypatch.setattr(rate_limit, "time", SimpleNamespace(monotonic=lambda: clock[0]))
    monkeypatch.setattr(rate_limit, "_MAX_TRACKED_KEYS", 2)
    monkeypatch.setattr(rate_limit, "_MAX_REQUESTS", 1)
    def request(ip):
        return Request({"type": "http", "client": (ip, 123)})
    for ip in ["a", "b"]:
        rate_limit.weather_rate_limit(request(ip))
    with pytest.raises(HTTPException):
        rate_limit.weather_rate_limit(request("c"))
    with pytest.raises(HTTPException):
        rate_limit.weather_rate_limit(request("a"))
    assert len(rate_limit._hits) == 2
    clock[0] = 160.0
    rate_limit.weather_rate_limit(request("c"))
    assert len(rate_limit._hits) == 1


def test_weather_routes_share_quota(client, monkeypatch):
    monkeypatch.setattr(rate_limit, "_MAX_REQUESTS", 1)
    monkeypatch.setattr(weather_service, "get_daily", lambda **kwargs: dict(
        location={}, provider="local-test", days=1, items=[]))
    assert client.get("/v1/weather/current?lat=41&lon=29").status_code == 200
    assert client.get("/v1/weather/daily?lat=41&lon=29&days=1").status_code == 429
