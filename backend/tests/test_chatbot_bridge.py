from uuid import uuid4

import httpx
import pytest
from app.services.chatbot_bridge import BridgeError, ChatbotBridgeClient, settings
from pydantic import SecretStr


@pytest.fixture(autouse=True)
def local_key(monkeypatch):
    monkeypatch.setattr(settings, "chatbot_api_key", SecretStr("LOCAL_TEST_KEY"))


@pytest.mark.parametrize(
    "outcome,status,attempts",
    [("connect", 503, 2), ("timeout", 504, 1), ("read", 503, 1),
     (401, 503, 1), (500, 503, 1), (504, 504, 1),
     ("json", 502, 1), ([], 502, 1), ({"answer": None}, 502, 1),
     ({"answer": 42}, 502, 1), ({"answer": "  "}, 502, 1)],
)
def test_bridge_failure_never_returns_an_answer(monkeypatch, outcome, status, attempts):
    calls = []

    def post(self, url, **kwargs):
        calls.append(kwargs)
        if outcome == "connect":
            raise httpx.ConnectError("private detail")
        if outcome == "timeout":
            raise httpx.ReadTimeout("private detail")
        if outcome == "read":
            raise httpx.ReadError("private detail")
        if isinstance(outcome, int):
            return httpx.Response(outcome, text="private detail")
        if outcome == "json":
            return httpx.Response(200, text="invalid JSON")
        return httpx.Response(200, json=outcome)

    monkeypatch.setattr(httpx.Client, "post", post)
    with pytest.raises(BridgeError) as caught:
        ChatbotBridgeClient().ask("question", "user")
    assert caught.value.status == status
    assert "private" not in caught.value.message
    assert len(calls) == attempts


def test_connect_retry_can_succeed(monkeypatch):
    calls = []

    def post(self, url, **kwargs):
        calls.append(kwargs)
        if len(calls) == 1:
            raise httpx.ConnectError("offline")
        return httpx.Response(200, json={"answer": " Cevap ", "conversation_id": str(uuid4()), "message_id": str(uuid4())})

    monkeypatch.setattr(httpx.Client, "post", post)
    assert ChatbotBridgeClient().ask("question", "user").answer == "Cevap"
    assert len(calls) == 2


def test_key_stays_in_header_and_payload_preserves_question_and_metadata(monkeypatch):
    conversation = str(uuid4())
    message = str(uuid4())
    calls = []

    def post(self, url, **kwargs):
        calls.append((url, kwargs))
        return httpx.Response(200, json={"answer": "Cevap", "conversation_id": conversation,
                                        "message_id": message, "disclaimer": "Uyarı"})

    monkeypatch.setattr(httpx.Client, "post", post)
    reply = ChatbotBridgeClient(base_url="https://example.invalid/api/bridge/").ask(
        "  Özgün soru?  ", "trusted-user", context={"city": "Ankara"},
        conversation_id=conversation, tone="kisa", consent_version=2)
    url, call = calls[0]
    assert url == "https://example.invalid/api/bridge/ask"
    assert call["headers"]["Authorization"] == "Bearer LOCAL_TEST_KEY"
    assert "LOCAL_TEST_KEY" not in str(call["json"])
    assert call["json"]["question"] == "  Özgün soru?  "
    assert call["json"]["conversation_id"] == conversation
    assert call["json"]["tone"] == "kisa"
    assert str(reply.message_id) == message


def test_missing_key_never_contacts_upstream(monkeypatch):
    monkeypatch.setattr(httpx.Client, "post", lambda *a, **kw: pytest.fail("Unexpected HTTP"))
    with pytest.raises(BridgeError) as caught:
        ChatbotBridgeClient(api_key="").ask("question", "user")
    assert caught.value.code == "not_configured"


@pytest.mark.parametrize("status,code,expected", [
    (429, "daily_limit", "daily_limit"), (429, "rate_limited", "rate_limited"),
    (409, "conversation_conflict", "conversation_conflict"),
    (409, "mode_disabled", "mode_disabled"), (413, "too_large", "too_large"),
    (428, "consent_required", "consent_sync_required"),
])
def test_safe_errors_and_retry_after_without_auto_retry(monkeypatch, status, code, expected):
    calls = []

    def post(self, *args, **kwargs):
        calls.append(kwargs)
        return httpx.Response(status, json={"error": code, "message": "PRIVATE upstream details"},
                              headers={"Retry-After": "23"})

    monkeypatch.setattr(httpx.Client, "post", post)
    with pytest.raises(BridgeError) as caught:
        ChatbotBridgeClient().ask("question", "user")
    assert caught.value.code == expected
    assert "PRIVATE" not in caught.value.message
    assert caught.value.retry_after == ("23" if status == 429 else None)
    assert len(calls) == 1


def test_response_cannot_switch_conversations_or_omit_safety_warning(monkeypatch):
    conversation = str(uuid4())
    monkeypatch.setattr(httpx.Client, "post", lambda *a, **kw: httpx.Response(200, json={
        "answer": "Cevap", "conversation_id": conversation, "message_id": str(uuid4())}))
    with pytest.raises(BridgeError):
        ChatbotBridgeClient().ask("question", "user", conversation_id=str(uuid4()))
    with pytest.raises(BridgeError):
        ChatbotBridgeClient().ask("question", "user", conversation_id=conversation, mode="fly")
