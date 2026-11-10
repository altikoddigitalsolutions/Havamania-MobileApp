from hashlib import sha256

import httpx
import pytest
from app.schemas.chatbot import AssistantConsentUpdate
from app.services import assistant_consent, platform_consent
from fastapi import HTTPException
from pydantic import SecretStr


@pytest.fixture
def provider(monkeypatch):
    settings = assistant_consent.settings
    monkeypatch.setattr(settings, "assistant_consent_source", "platform")
    monkeypatch.setattr(settings, "chatbot_api_key", SecretStr("LOCAL_TEST_KEY"))
    monkeypatch.setattr(settings, "chatbot_base_url", "https://platform.invalid/api/bridge")
    instance = platform_consent.PlatformConsentProvider()
    monkeypatch.setattr(assistant_consent, "platform_consent_provider", instance)
    state = {"body": {"version": 2, "text": "Test text"}, "status": 200, "calls": 0, "time": 100.0}
    original = httpx.Client

    def handle(request):
        state["calls"] += 1
        assert str(request.url) == "https://platform.invalid/api/bridge/consent"
        assert request.headers["Authorization"] == "Bearer LOCAL_TEST_KEY"
        return httpx.Response(state["status"], json=state["body"])

    monkeypatch.setattr(
        platform_consent.httpx,
        "Client",
        lambda **kwargs: original(transport=httpx.MockTransport(handle), **kwargs),
    )
    monkeypatch.setattr(platform_consent, "monotonic", lambda: state["time"])
    return instance, state


def test_exact_text_hash_cache_expiry_and_invalidation(provider):
    instance, state = provider
    doc, health = assistant_consent.current_documents()
    assert doc["hash"] == sha256(b"Test text").hexdigest()
    assert health is None
    assistant_consent.current_documents()
    assert state["calls"] == 1
    state["time"] += 16
    state["body"]["version"] = 4
    assert assistant_consent.current_documents()[0]["version"] == 4
    instance.invalidate()
    assistant_consent.current_documents()
    assert state["calls"] == 3


def test_changed_text_cannot_be_accepted_from_cache(provider):
    _, state = provider
    doc, _ = assistant_consent.current_documents()
    state["body"]["text"] = "Changed text with same version"
    payload = AssistantConsentUpdate(
        assistant_accepted=True, consent_version=2, consent_hash=doc["hash"]
    )
    with pytest.raises(HTTPException) as error:
        assistant_consent.save_consent(None, "unused", payload)
    assert error.value.status_code == 409
    assert state["calls"] == 2


@pytest.mark.parametrize(
    "body",
    [
        [],
        {"version": True, "text": "Text"},
        {"version": 0, "text": "Text"},
        {"version": 2, "text": "  "},
        {"version": 2, "text": "Text", "health": {"version": 3, "text": ""}},
        {"version": 2, "text": "x" * 262145},
    ],
)
def test_invalid_response_fails_closed(provider, body):
    _, state = provider
    state["body"] = body
    with pytest.raises(HTTPException) as error:
        assistant_consent.current_documents()
    assert error.value.status_code == 503
    assert "LOCAL_TEST_KEY" not in str(error.value.detail)


def test_expired_cache_never_falls_back_on_failure(provider):
    _, state = provider
    assistant_consent.current_documents()
    state["time"] += 16
    state["status"] = 401
    with pytest.raises(HTTPException) as error:
        assistant_consent.current_documents()
    assert error.value.status_code == 503
    state["status"] = 200
    assert assistant_consent.current_documents()[0]["version"] == 2
    assert state["calls"] == 3
