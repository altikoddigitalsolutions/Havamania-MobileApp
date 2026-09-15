from app.core.config import get_settings
from app.main import app
from fastapi.testclient import TestClient

client = TestClient(app)

def test_metrics_unauthorized_in_production(monkeypatch):
    settings = get_settings()
    monkeypatch.setattr(settings, "app_env", "production")
    monkeypatch.setattr(settings, "metrics_secret", "super-secret-metrics-key")

    response = client.get("/v1/metrics")
    assert response.status_code in (401, 403)

def test_metrics_authorized(monkeypatch):
    settings = get_settings()
    monkeypatch.setattr(settings, "app_env", "production")
    monkeypatch.setattr(settings, "metrics_secret", "super-secret-metrics-key")

    response = client.get("/v1/metrics", headers={"Authorization": "Bearer super-secret-metrics-key"})
    assert response.status_code == 200
    assert "request_count_total" in response.text
