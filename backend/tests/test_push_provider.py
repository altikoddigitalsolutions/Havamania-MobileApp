from datetime import UTC, datetime, timedelta
from uuid import uuid4

import pytest
from app.core.config import get_settings


def metadata():
    return {"owner_user_id": str(uuid4()), "event_id": str(uuid4()), "expires_at": datetime.now(UTC)+timedelta(hours=1)}


@pytest.fixture(autouse=True)
def enabled(monkeypatch):
    monkeypatch.setattr(get_settings(), "fcm_owner_protocol_enabled", True)
    monkeypatch.setattr("firebase_admin.get_app", lambda: object())


from unittest.mock import patch

from app.services.push_provider import FCMApnsPushProvider


@patch("firebase_admin.messaging.send")
def test_real_provider_success(mock_send):
    mock_send.return_value = "projects/test/messages/123"
    provider = FCMApnsPushProvider()
    res = provider.send("android", "test_token_123", "Title", "Body", **metadata())
    assert res is True
    mock_send.assert_called_once()
    message = mock_send.call_args.args[0]
    assert message.notification is None
    assert message.android.priority == "high"
    assert 0 <= message.android.ttl.total_seconds() <= 86400
    assert message.data["protocol"] == "1" and message.data["target"] == "weather"
    assert message.data["owner_backend_user_id"] and message.data["event_id"]

@patch("firebase_admin.messaging.send")
def test_no_fake_success_on_exception(mock_send):
    mock_send.side_effect = Exception("FCM error")
    provider = FCMApnsPushProvider()
    res = provider.send("android", "test_token_123", "Title", "Body", **metadata())
    assert res is False

def test_initialization_failure_returns_false(monkeypatch):
    def missing():
        raise ValueError("No default app")
    def failure():
        raise RuntimeError("PRIVATE_CREDENTIAL_DETAIL")
    monkeypatch.setattr("firebase_admin.get_app", missing)
    monkeypatch.setattr("firebase_admin.initialize_app", failure)
    provider = FCMApnsPushProvider()
    res = provider.send("android", "test_token_123", "Title", "Body", **metadata())
    assert res is False

def test_unsupported_platform_returns_false():
    provider = FCMApnsPushProvider()
    res = provider.send("ios", "test_token_123", "Title", "Body", **metadata())
    assert res is False


@patch("firebase_admin.messaging.send")
def test_disabled_or_missing_owner_never_uses_legacy_notification_fallback(mock_send, monkeypatch):
    provider = FCMApnsPushProvider()
    assert not provider.send("android", "local-token", "Title", "Body")
    monkeypatch.setattr(get_settings(), "fcm_owner_protocol_enabled", False)
    assert not provider.send("android", "local-token", "Title", "Body", **metadata())
    mock_send.assert_not_called()


def test_owned_data_expiry_identity_and_receiver_size_limits():
    from app.services.push_provider import owned_push_data
    meta = metadata()
    data = owned_push_data("T" * 250, "B" * 1100, **meta)
    assert len(data["title"]) == 200 and len(data["body"]) == 1000
    for bad in (dict(meta, owner_user_id="invalid"), dict(meta, event_id=""),
                dict(meta, expires_at=datetime.now(UTC)-timedelta(seconds=1))):
        assert owned_push_data("Title", "Body", **bad) is None
    assert owned_push_data("Bad\nTitle", "Body", **meta) is None
    assert owned_push_data("Title", "Bad\0Body", **meta) is None


def test_emoji_uses_kotlin_utf16_limits():
    from app.services.push_provider import owned_push_data
    data = owned_push_data('🌧' * 150, '🌧' * 600, **metadata())
    assert len(data['title'].encode('utf-16-le')) // 2 <= 200
    assert len(data['body'].encode('utf-16-le')) // 2 <= 1000


@patch("firebase_admin.messaging.send")
def test_named_identity_app_does_not_replace_default_fcm_app(mock_send, monkeypatch):
    from unittest.mock import Mock
    def default_missing():
        raise ValueError("Only named identity app exists")
    initialize = Mock()
    monkeypatch.setattr("firebase_admin.get_app", default_missing)
    monkeypatch.setattr("firebase_admin.initialize_app", initialize)
    assert FCMApnsPushProvider().send("android", "local-token", "Title", "Body", **metadata())
    initialize.assert_called_once_with()
    mock_send.assert_called_once()


@patch("firebase_admin.messaging.send")
def test_parallel_default_initialization_race_uses_existing_app(mock_send, monkeypatch):
    from unittest.mock import Mock
    monkeypatch.setattr("firebase_admin.get_app", Mock(side_effect=[ValueError('not yet'), object()]))
    monkeypatch.setattr("firebase_admin.initialize_app", Mock(side_effect=ValueError('already initialized')))
    assert FCMApnsPushProvider().send("android", "local-token", "Title", "Body", **metadata())
    mock_send.assert_called_once()
