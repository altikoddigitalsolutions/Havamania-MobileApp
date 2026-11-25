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


@pytest.mark.parametrize("error_type", [
    firebase_identity.auth.RevokedIdTokenError,
    firebase_identity.auth.UserDisabledError,
    firebase_identity.auth.UserNotFoundError,
    firebase_identity.auth.ExpiredIdTokenError,
])
def test_invalidated_identity_is_rechecked_and_cannot_access_profile(
    client, monkeypatch, db_session, error_type,
):
    from app.models.user import User

    monkeypatch.setattr(get_settings(), "firebase_project_id", "test-project")
    app_marker = object()
    monkeypatch.setattr(firebase_identity, "firebase_app", lambda project: app_marker)
    calls = []
    invalidated = False

    def verify(token, *, app, check_revoked):
        calls.append((token, app, check_revoked))
        if invalidated:
            if error_type is firebase_identity.auth.ExpiredIdTokenError:
                raise error_type("private SDK diagnostic must not reach client", cause=None)
            raise error_type("private SDK diagnostic must not reach client")
        return {"uid": "revocation-user", "email": "revocation@example.com"}

    monkeypatch.setattr(firebase_identity.auth, "verify_id_token", verify)
    headers = {"Authorization": "Bearer firebase-revocation-test"}
    accepted = client.get("/v1/profile", headers=headers)
    assert accepted.status_code == 200
    owner_id = accepted.json()["user_id"]
    invalidated = True
    rejected = client.get("/v1/profile", headers=headers)
    assert rejected.status_code == 401
    assert rejected.json() == {"detail": "Invalid Firebase credentials"}
    assert calls == [("firebase-revocation-test", app_marker, True)] * 2
    assert db_session.query(User).count() == 1
    assert db_session.get(User, owner_id) is not None


def test_missing_firebase_identity_cannot_create_backend_account(client, monkeypatch, db_session):
    from app.models.user import User

    monkeypatch.setattr(get_settings(), "firebase_project_id", "test-project")
    monkeypatch.setattr(firebase_identity, "firebase_app", lambda project: object())

    def verify(token, **kwargs):
        assert kwargs["check_revoked"] is True
        raise firebase_identity.auth.UserNotFoundError("deleted remote UID")

    monkeypatch.setattr(firebase_identity.auth, "verify_id_token", verify)
    response = client.get("/v1/profile", headers={"Authorization": "Bearer deleted-user"})
    assert response.status_code == 401
    assert db_session.query(User).count() == 0
