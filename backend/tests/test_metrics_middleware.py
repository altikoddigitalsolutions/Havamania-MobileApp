import pytest
from app.middleware import metrics
from fastapi import FastAPI
from fastapi.testclient import TestClient


@pytest.fixture
def measured_app(monkeypatch):
    observations = []
    monkeypatch.setattr(metrics, "observe_request", lambda **kwargs: observations.append(kwargs))
    app = FastAPI()
    app.add_middleware(metrics.MetricsMiddleware)

    @app.get("/items/{item_id}")
    def item(item_id: str):
        return {"ok": True}

    @app.get("/failure/{item_id}")
    def failure(item_id: str):
        raise RuntimeError("PRIVATE_DETAIL")

    with TestClient(app, raise_server_exceptions=False) as client:
        yield client, observations


def test_dynamic_and_missing_paths_never_become_metric_labels(measured_app):
    client, observations = measured_app
    for item in ("PRIVATE_A", "PRIVATE_B"):
        assert client.get("/items/" + item).status_code == 200
        assert client.get("/missing/" + item).status_code == 404
    assert {item["path"] for item in observations} == {"/items/{item_id}", "unmatched"}
    assert "PRIVATE_" not in str(observations)


def test_unknown_http_methods_share_a_bounded_label(measured_app):
    client, observations = measured_app
    for index in range(20):
        assert client.request(f"CUSTOM{index}", "/items/PRIVATE_ID").status_code == 405
    assert len(observations) == 20
    assert {item["method"] for item in observations} == {"OTHER"}


def test_unhandled_error_is_measured_once_and_still_returns_500(measured_app):
    client, observations = measured_app
    response = client.get("/failure/PRIVATE_ID")
    assert response.status_code == 500
    assert len(observations) == 1
    assert observations[0]["status"] == 500
    assert observations[0]["path"] == "/failure/{item_id}"
    assert observations[0]["method"] == "GET"
    assert observations[0]["elapsed_seconds"] >= 0
    assert "PRIVATE_" not in str(observations)
