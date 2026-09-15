import pytest
from unittest.mock import patch, MagicMock
from app.services.push_provider import FCMApnsPushProvider

@patch("firebase_admin.messaging.send")
@patch("firebase_admin._apps", ["default"])
def test_real_provider_success(mock_send):
    mock_send.return_value = "projects/test/messages/123"
    provider = FCMApnsPushProvider()
    res = provider.send("android", "test_token_123", "Title", "Body")
    assert res is True
    mock_send.assert_called_once()

@patch("firebase_admin.messaging.send")
@patch("firebase_admin._apps", ["default"])
def test_no_fake_success_on_exception(mock_send):
    mock_send.side_effect = Exception("FCM error")
    provider = FCMApnsPushProvider()
    res = provider.send("android", "test_token_123", "Title", "Body")
    assert res is False

@patch("firebase_admin._apps", [])
def test_initialization_failure_returns_false():
    provider = FCMApnsPushProvider()
    res = provider.send("android", "test_token_123", "Title", "Body")
    assert res is False

@patch("firebase_admin._apps", ["default"])
def test_unsupported_platform_returns_false():
    provider = FCMApnsPushProvider()
    res = provider.send("ios", "test_token_123", "Title", "Body")
    assert res is False
