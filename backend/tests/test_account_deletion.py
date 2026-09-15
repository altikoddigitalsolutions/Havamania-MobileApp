import time
from datetime import UTC, datetime
from hashlib import sha256
from types import SimpleNamespace
from unittest.mock import Mock
from uuid import NAMESPACE_URL, uuid5

import pytest
from app.models import (
    Alert,
    AlertSeverity,
    ChatbotUsageDaily,
    Location,
    NotificationPreference,
    PushToken,
    RefreshToken,
    Subscription,
)
from app.models.account_deletion import AccountDeletion
from app.models.profile import Profile
from app.models.user import User
from app.services import account_deletion as service
from fastapi import HTTPException
from sqlalchemy import select

KEY = "ab" * 32
UID = "firebase-user"
PROJECT = "test-project"
USER_ID = str(uuid5(NAMESPACE_URL, f"https://securetoken.google.com/{PROJECT}/{UID}"))


@pytest.fixture
def configured(monkeypatch, tmp_path):
    settings = SimpleNamespace(account_deletion_enabled=True, firebase_project_id=PROJECT,
                               firebase_storage_bucket="test-bucket")
    monkeypatch.setattr(service, "get_settings", lambda: settings)
    monkeypatch.setattr(service, "verify_firebase_token",
                        lambda *_: {"uid": UID, "auth_time": time.time()})
    monkeypatch.setattr(service, "AVATAR_DIR", tmp_path)
    return settings


def test_requires_recent_auth_without_provisioning_user(db_session, configured, monkeypatch):
    for token in (None,):
        with pytest.raises(HTTPException) as error:
            service.prepare_deletion(db_session, KEY, token)
        assert error.value.status_code == 401
    monkeypatch.setattr(service, "verify_firebase_token",
                        lambda *_: {"uid": UID, "auth_time": time.time() - 301})
    with pytest.raises(HTTPException) as error:
        service.prepare_deletion(db_session, KEY, "stale")
    assert error.value.status_code == 401
    assert db_session.scalars(select(AccountDeletion)).all() == []
    assert db_session.scalars(select(User)).all() == []


@pytest.mark.parametrize("field", ["account_deletion_enabled", "firebase_storage_bucket"])
def test_misconfigured_service_does_not_accept_job(db_session, configured, field):
    setattr(configured, field, None)
    with pytest.raises(HTTPException) as error:
        service.prepare_deletion(db_session, KEY, "valid")
    assert error.value.status_code == 503
    assert db_session.scalars(select(AccountDeletion)).all() == []


def test_resume_after_failure_without_firebase_token(db_session, configured, tmp_path):
    db_session.add_all([User(id=USER_ID, email="delete@example.com", password_hash="unused"),
                        User(id="other", email="keep@example.com", password_hash="unused")])
    db_session.commit()
    db_session.add_all([Profile(user_id=USER_ID), Profile(user_id="other")])
    db_session.commit()
    avatar = tmp_path / f"{USER_ID}_avatar.jpg"
    avatar.write_bytes(b"test")
    other_avatar = tmp_path / "other_avatar.jpg"
    other_avatar.write_bytes(b"keep")

    digest = service.prepare_deletion(db_session, KEY, "valid")
    assert digest == sha256(KEY.encode()).hexdigest()
    assert db_session.get(AccountDeletion, KEY) is None
    gateway = Mock()
    gateway.remove_documents.side_effect = RuntimeError("simulated outage")
    assert not service.execute_deletion(db_session, digest, lambda _: gateway)
    gateway.remove_identity.assert_not_called()
    assert db_session.get(User, USER_ID) is not None
    assert avatar.exists()
    assert db_session.get(AccountDeletion, digest).status == "pending"

    # The identity may already be disabled. The original secret must still work.
    assert service.prepare_deletion(db_session, KEY, None) == digest
    with pytest.raises(HTTPException):
        service.prepare_deletion(db_session, "cd" * 32, None)
    gateway.remove_documents.side_effect = None
    assert service.execute_deletion(db_session, digest, lambda _: gateway)
    db_session.expire_all()
    assert db_session.get(User, USER_ID) is None
    assert db_session.get(Profile, USER_ID) is None
    assert db_session.get(User, "other") is not None
    assert db_session.get(Profile, "other") is not None
    assert not avatar.exists()
    assert other_avatar.exists()
    job = db_session.get(AccountDeletion, digest)
    assert job.status == "complete"
    assert job.firebase_uid is None and job.project_id is None
    gateway.reset_mock()
    assert service.execute_deletion(db_session, digest, lambda _: gateway)
    assert gateway.mock_calls == []


def test_identity_failure_rolls_back_sql_for_retry(db_session, configured):
    db_session.add(User(id=USER_ID, email="delete@example.com", password_hash="unused"))
    db_session.commit()
    digest = service.prepare_deletion(db_session, KEY, "valid")
    gateway = Mock()
    gateway.remove_identity.side_effect = RuntimeError("timeout")
    assert not service.execute_deletion(db_session, digest, lambda _: gateway)
    assert db_session.get(User, USER_ID) is not None
    assert db_session.get(AccountDeletion, digest).status == "pending"
    gateway.remove_identity.side_effect = None
    assert service.execute_deletion(db_session, digest, lambda _: gateway)


def test_firestore_exhausted_bulk_retries_are_failure():
    gateway = object.__new__(service.FirebaseDeletionGateway)
    gateway.db = Mock()
    writer = gateway.db.bulk_writer.return_value

    def recursive_delete(*_, **__):
        callback = writer.on_write_error.call_args.args[0]
        assert callback(SimpleNamespace(attempts=1), writer)
        assert not callback(SimpleNamespace(attempts=3), writer)

    gateway.db.recursive_delete.side_effect = recursive_delete
    with pytest.raises(RuntimeError, match="incomplete"):
        gateway.remove_documents(UID)
    writer.close.assert_called()


def test_endpoint_reports_pending_and_never_caches_secret(client, configured, monkeypatch):
    from app.api.v1.routes import account
    monkeypatch.setattr(account, "execute_deletion", lambda *_: False)
    response = client.post("/v1/account/deletion", headers={"Authorization": "Bearer valid"},
                           json={"continuation_key": KEY})
    assert response.status_code == 202
    assert response.json() == {"status": "pending"}
    assert response.headers["cache-control"] == "no-store"
    assert KEY not in response.text
    monkeypatch.setattr(account, "execute_deletion", lambda *_: True)
    response = client.post("/v1/account/deletion", json={"continuation_key": KEY})
    assert response.status_code == 200
    assert response.json() == {"status": "complete"}


def test_endpoint_rejects_short_continuation_keys(client, configured):
    response = client.post("/v1/account/deletion", json={"continuation_key": "short"})
    assert response.status_code == 422


def test_configuration_change_cannot_delete_from_a_different_bucket(db_session, configured):
    digest = service.prepare_deletion(db_session, KEY, "valid")
    configured.firebase_storage_bucket = "different-bucket"
    factory = Mock()
    assert not service.execute_deletion(db_session, digest, factory)
    factory.assert_not_called()
    assert db_session.get(AccountDeletion, digest).status == "pending"


def test_availability_is_false_until_configured(client, configured, monkeypatch):
    from app.api.v1.routes import account
    monkeypatch.setattr(account, "get_settings", lambda: configured)
    assert client.get("/v1/account/deletion/availability").json() == {"available": True}
    configured.account_deletion_enabled = False
    response = client.get("/v1/account/deletion/availability")
    assert response.json() == {"available": False}
    assert response.headers["cache-control"] == "no-store"


def test_deletion_cascades_through_all_account_tables(db_session, configured):
    db_session.add(User(id=USER_ID, email="delete@example.com", password_hash="unused"))
    db_session.commit()
    location = Location(id="location", user_id=USER_ID, label="Test", lat=1, lon=2)
    db_session.add(location)
    db_session.commit()
    now = datetime.now(UTC)
    db_session.add_all([
        Profile(user_id=USER_ID, primary_location_id=location.id),
        NotificationPreference(user_id=USER_ID),
        PushToken(user_id=USER_ID, platform="android", token="device-token"),
        RefreshToken(user_id=USER_ID, token_hash="hash", expires_at=now),
        ChatbotUsageDaily(user_id=USER_ID, date=now.date()),
        Subscription(user_id=USER_ID),
        Alert(location_id=location.id, severity=AlertSeverity.ACTIVE, title="Test",
              description="Test", starts_at=now, ends_at=now),
    ])
    db_session.commit()
    digest = service.prepare_deletion(db_session, KEY, "valid")
    assert service.execute_deletion(db_session, digest, lambda _: Mock())
    for model in (User, Profile, Location, NotificationPreference, PushToken,
                  RefreshToken, ChatbotUsageDaily, Subscription, Alert):
        assert db_session.scalars(select(model)).all() == []
