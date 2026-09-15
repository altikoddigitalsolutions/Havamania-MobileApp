import pytest
from app.core.config import get_settings
from app.services import firebase_identity


@pytest.fixture
def firebase_claims(monkeypatch):
    monkeypatch.setattr(get_settings(), "firebase_project_id", "test-project")
    claims = {"uid": "firebase-user-1", "email": "firebase@example.com", "name": "User"}
    monkeypatch.setattr(firebase_identity, "verify_firebase_token", lambda token, project: claims)
    return claims


def test_verified_firebase_identity_is_stable(client, firebase_claims):
    headers = {"Authorization": "Bearer firebase-test-token"}
    first = client.get("/v1/profile", headers=headers)
    second = client.get("/v1/profile", headers=headers)
    assert first.status_code == second.status_code == 200
    assert first.json()["user_id"] == second.json()["user_id"]


def test_firebase_cannot_take_over_existing_email(client, firebase_claims):
    assert client.post("/v1/auth/signup", json={
        "email": firebase_claims["email"], "password": "Password123"
    }).status_code == 201
    response = client.get("/v1/profile", headers={"Authorization": "Bearer firebase-test-token"})
    assert response.status_code == 409


def test_invalid_firebase_token_is_rejected(client, monkeypatch):
    monkeypatch.setattr(get_settings(), "firebase_project_id", "test-project")

    def reject(token, project):
        raise ValueError("Invalid token")

    monkeypatch.setattr(firebase_identity, "verify_firebase_token", reject)
    assert client.get("/v1/profile", headers={"Authorization": "Bearer invalid"}).status_code == 401
