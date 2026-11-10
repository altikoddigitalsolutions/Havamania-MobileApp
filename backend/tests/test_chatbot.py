from uuid import uuid4

import pytest
from app.api.v1.routes import chatbot as chatbot_route
from app.dependencies.premium import require_premium_user
from app.main import app
from app.schemas.chatbot import PlatformChatbotReply
from app.services.chatbot_bridge import BridgeError


@pytest.fixture(autouse=True)
def configure_consent(assistant_texts):
    pass


class StubBridge:
    def ask(self, question: str, user_id: str, context: dict | None = None, **metadata) -> PlatformChatbotReply:
        return PlatformChatbotReply(answer=f"stub-answer:{question}",
            conversation_id=metadata.get("conversation_id") or uuid4(), message_id=uuid4())


class StubFailingBridge:
    def ask(self, question: str, user_id: str, context: dict | None = None, **metadata) -> PlatformChatbotReply:
        raise BridgeError(504, "timeout", "Asistan zamanında cevap veremedi.")


def _auth_header(client):
    signup = client.post(
        "/v1/auth/signup",
        json={"email": "chatbot@example.com", "password": "Password123", "full_name": "Chatbot User"},
    )
    token = signup.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}
    docs = client.get("/v1/chatbot/consent", headers=headers).json()
    accepted = client.post("/v1/chatbot/consent", headers=headers,
                           json={"assistant_accepted": True, "consent_version": 2, "consent_hash": docs["text_hash"]})
    assert accepted.status_code == 200
    return headers


def test_chatbot_ask_increments_usage(client, monkeypatch):
    headers = _auth_header(client)
    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", StubBridge())

    response = client.post("/v1/chatbot/ask", json={"question": "Hava nasıl?"}, headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert data["answer"].startswith("stub-answer")
    assert data["used_messages_today"] == 1


def test_chatbot_usage_endpoint(client):
    headers = _auth_header(client)

    response = client.get("/v1/chatbot/usage", headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert data["daily_limit"] >= data["used_messages_today"]


@pytest.mark.parametrize("endpoint", ["/v1/chatbot/ask", "/v1/chatbot/ask/premium"])
def test_chatbot_timeout_does_not_consume_usage(client, monkeypatch, endpoint):
    app.dependency_overrides[require_premium_user] = lambda: None
    headers = _auth_header(client)
    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", StubFailingBridge())

    response = client.post(endpoint, json={"question": "Gecikme testi"}, headers=headers)

    assert response.status_code == 504
    assert response.json()["error"] == "timeout"
    usage = client.get("/v1/chatbot/usage", headers=headers).json()
    assert usage["used_messages_today"] == 0

def test_failed_request_releases_last_slot(client, monkeypatch):
    headers = _auth_header(client)
    monkeypatch.setattr(chatbot_route.settings, "chatbot_free_daily_limit", 1)
    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", StubFailingBridge())
    assert client.post("/v1/chatbot/ask", json={"question": "test"}, headers=headers).status_code == 504
    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", StubBridge())
    response = client.post("/v1/chatbot/ask", json={"question": "test"}, headers=headers)
    assert response.json()["used_messages_today"] == 1

    class MustNotCall:
        def ask(self, **kwargs):
            raise AssertionError("Full quota must not reach the model")

    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", MustNotCall())
    response = client.post("/v1/chatbot/ask", json={"question": "test"}, headers=headers)
    assert response.status_code == 429
    assert response.json()["error"] == "daily_limit"
    assert "answer" not in response.json()
    assert 1 <= int(response.headers["Retry-After"]) <= 86400
    assert response.json()["remaining_messages_today"] == 0
    assert response.json()["used_messages_today"] == 1


def test_request_replay_is_blocked_without_another_model_call(client, monkeypatch):
    from uuid import uuid4

    headers = _auth_header(client)
    calls = []

    class CountingBridge:
        def ask(self, **kwargs):
            calls.append(kwargs)
            return PlatformChatbotReply(answer="Cevap", conversation_id=kwargs.get("conversation_id") or uuid4(), message_id=uuid4())

    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", CountingBridge())
    payload = {"question": "test", "request_id": str(uuid4())}
    assert client.post("/v1/chatbot/ask", json=payload, headers=headers).status_code == 200
    response = client.post("/v1/chatbot/ask", json=payload, headers=headers)
    assert response.status_code == 200
    assert response.json()["answer"]
    assert len(calls) == 1
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == 1


@pytest.mark.parametrize("endpoint", ["/v1/chatbot/ask", "/v1/chatbot/ask/premium"])
def test_zero_daily_quota_is_an_error_without_calling_model(client, monkeypatch, endpoint):
    from datetime import datetime

    app.dependency_overrides[require_premium_user] = lambda: None
    headers = _auth_header(client)
    monkeypatch.setattr(chatbot_route.settings, "chatbot_free_daily_limit", 0)
    monkeypatch.setattr(chatbot_route.settings, "chatbot_premium_daily_limit", 0)

    class MustNotCall:
        def ask(self, **kwargs):
            raise AssertionError("Quota must be checked before model call")

    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", MustNotCall())
    response = client.post(endpoint, json={"question": "test"}, headers=headers)
    assert response.status_code == 429
    assert response.json()["error"] == "daily_limit"
    assert "answer" not in response.json()
    assert response.json()["used_messages_today"] == 0
    assert 1 <= int(response.headers["Retry-After"]) <= 86400
    assert datetime.fromisoformat(response.json()["reset_at"]).utcoffset().total_seconds() == 10800


def test_contract_context_is_bounded_typed_and_opt_out_is_honored(client, monkeypatch, db_session):
    from app.models.profile import Profile
    from app.models.user import User
    from sqlalchemy import select

    headers = _auth_header(client)
    session = db_session
    uid = session.scalar(select(User.id).where(User.email == "chatbot@example.com"))
    profile = session.get(Profile, uid) or Profile(user_id=uid)
    profile.interest = "Müzeler"
    session.add(profile)
    session.commit()
    calls = []

    class Bridge:
        def ask(self, **payload):
            calls.append(payload)
            return PlatformChatbotReply(answer="Cevap", conversation_id=payload["conversation_id"], message_id=uuid4())

    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", Bridge())
    conversation = str(uuid4())
    payload = {"question": "Öneri sun", "conversation_id": conversation, "tone": "kisa",
               "context": {"city": "Ankara", "weather": None, "trips": "Özel seyahat"}}
    response = client.post("/v1/chatbot/ask", headers={**headers, "X-App-Version": "1.2.3"}, json=payload)
    assert response.status_code == 200
    assert response.json()["conversation_id"] == conversation
    assert calls[-1]["client"]["app_version"] == "1.2.3"
    assert calls[-1]["tone"] == "kisa"
    assert calls[-1]["user_id"] == uid
    assert "trips" not in calls[-1]["context"]
    assert "profile" not in calls[-1]["context"]
    payload["personalization_enabled"] = True
    assert client.post("/v1/chatbot/ask", headers=headers, json=payload).status_code == 200
    assert calls[-1]["context"]["profile"]["interest"] == "Müzeler"
    payload["selected_interests"] = ["Mimari", "Doğa"]
    assert client.post("/v1/chatbot/ask", headers=headers, json=payload).status_code == 200
    assert calls[-1]["context"]["profile"]["interest"] == "Mimari, Doğa"
    payload["selected_interests"] = []
    assert client.post("/v1/chatbot/ask", headers=headers, json=payload).status_code == 200
    assert "interest" not in calls[-1]["context"].get("profile", {})
    payload["context"]["profile"] = {"health_sensitivities": "Forged"}
    assert client.post("/v1/chatbot/ask", headers=headers, json=payload).status_code == 422


def test_platform_rate_limit_preserves_header_and_refunds(client, monkeypatch):
    headers = _auth_header(client)

    class Bridge:
        def ask(self, **payload):
            raise BridgeError(429, "rate_limited", "Bekleyin", retry_after="19")

    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", Bridge())
    response = client.post("/v1/chatbot/ask", headers=headers, json={"question": "Öneri sun"})
    assert response.status_code == 429
    assert response.headers["Retry-After"] == "19"
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == 0


def test_context_byte_limit_rejects_before_quota_and_question_limit_is_consistent(client, monkeypatch, db_session):
    from app.models.chatbot_usage_daily import ChatbotUsageDaily
    from sqlalchemy import func, select

    headers = _auth_header(client)
    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", StubBridge())
    response = client.post("/v1/chatbot/ask", headers=headers, json={
        "question": "Öneri", "personalization_enabled": True,
        "context": {"weather": "🌧" * 2000, "trips": "🌧" * 2000}})
    assert response.status_code == 413
    assert db_session.scalar(select(func.count()).select_from(ChatbotUsageDaily)) == 0
    assert client.post("/v1/chatbot/ask", headers=headers, json={"question": "a" * 4000}).status_code == 200
    assert client.post("/v1/chatbot/ask", headers=headers, json={"question": "a" * 4001}).status_code == 422
