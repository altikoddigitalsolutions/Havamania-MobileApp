from uuid import uuid4

import pytest
from app.api.v1.routes import chatbot as route
from app.models.assistant_consent import AssistantConsent, AssistantConsentEvent
from app.models.chatbot_usage_daily import ChatbotUsageDaily
from app.models.profile import Profile
from app.models.user import User
from app.schemas.chatbot import PlatformChatbotReply
from sqlalchemy import delete, func, select


def signup(client, email="consent@example.com"):
    response = client.post("/v1/auth/signup", json={"email": email, "password": "Password123"})
    assert response.status_code in (200, 201)
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


def accept(client, headers, health=False):
    docs = client.get("/v1/chatbot/consent", headers=headers).json()
    return client.post("/v1/chatbot/consent", headers=headers, json={
        "assistant_accepted": True, "consent_version": 2, "consent_hash": docs["text_hash"],
        "health_consent_hash": docs["health"]["text_hash"] if health else None,
        "health_accepted": health, "health_consent_version": 2 if health else None,
    })


def ask(client, headers, **overrides):
    return client.post("/v1/chatbot/ask", headers=headers,
                       json={"question": "Bir öneri sun", "personalization_enabled": True, **overrides})


@pytest.fixture
def consent_setup(client, db_session, assistant_texts, monkeypatch):
    headers = signup(client)
    uid = db_session.scalar(select(User.id).where(User.email == "consent@example.com"))
    profile = db_session.get(Profile, uid) or Profile(user_id=uid)
    profile.health_sensitivities = "PRIVATE_HEALTH_TEST_VALUE"
    db_session.add(profile)
    db_session.commit()
    calls = []

    class Bridge:
        def ask(self, **payload):
            calls.append(payload)
            return PlatformChatbotReply(answer="Yerel cevap", conversation_id=payload.get("conversation_id") or uuid4(), message_id=uuid4())

    monkeypatch.setattr(route, "chatbot_bridge_client", Bridge())
    return headers, uid, calls, assistant_texts


def test_no_consent_blocks_before_quota_or_model(client, db_session, consent_setup):
    headers, _, calls, _ = consent_setup
    response = ask(client, headers, consent_version=2, health_consent=True, health_consent_version=2)
    assert response.status_code == 428
    assert response.json()["consent"]["version"] == 2
    assert calls == []
    assert db_session.scalar(select(func.count()).select_from(ChatbotUsageDaily)) == 0


def test_health_requires_separate_server_consent_and_request_flag(client, consent_setup):
    headers, _, calls, _ = consent_setup
    assert accept(client, headers).status_code == 200
    assert ask(client, headers, health_consent=True, health_consent_version=2).status_code == 200
    assert "health_sensitivities" not in calls[-1]["context"].get("profile", {})
    assert accept(client, headers, health=True).status_code == 200
    assert ask(client, headers).status_code == 200
    assert "health_sensitivities" not in calls[-1]["context"].get("profile", {})
    assert ask(client, headers, health_consent=True, health_consent_version=2).status_code == 200
    assert calls[-1]["context"].get("profile", {})["health_sensitivities"] == "PRIVATE_HEALTH_TEST_VALUE"


def test_health_version_or_text_change_omits_sensitive_field(client, consent_setup, monkeypatch):
    headers, _, calls, settings = consent_setup
    assert accept(client, headers, health=True).status_code == 200
    monkeypatch.setattr(settings, "assistant_health_consent_text", "Changed health text without a version bump")
    assert ask(client, headers, health_consent=True, health_consent_version=2).status_code == 200
    assert "health_sensitivities" not in calls[-1]["context"].get("profile", {})
    state = client.get("/v1/chatbot/consent", headers=headers).json()
    assert state["health_accepted"] is False
    assert state["health_review_required"] is True


def test_health_withdrawal_takes_effect_and_audit_has_no_sensitive_content(client, db_session, consent_setup):
    headers, uid, calls, _ = consent_setup
    assert accept(client, headers, health=True).status_code == 200
    assert accept(client, headers, health=False).status_code == 200
    assert ask(client, headers, health_consent=True, health_consent_version=2).status_code == 200
    assert "health_sensitivities" not in calls[-1]["context"].get("profile", {})
    events = db_session.scalars(select(AssistantConsentEvent).where(AssistantConsentEvent.user_id == uid)).all()
    assert len(events) == 2
    assert {event.health_accepted for event in events} == {True, False}
    assert all(len(event.assistant_hash) == 64 for event in events)
    db_session.execute(delete(User).where(User.id == uid))
    db_session.commit()
    assert db_session.scalar(select(func.count()).select_from(AssistantConsentEvent)) == 0
    assert db_session.get(AssistantConsent, uid) is None


def test_assistant_text_change_requires_new_consent(client, consent_setup, monkeypatch):
    headers, _, calls, settings = consent_setup
    assert accept(client, headers).status_code == 200
    monkeypatch.setattr(settings, "assistant_consent_text", "Changed main consent text without a version bump")
    assert ask(client, headers).status_code == 428
    assert calls == []


def test_missing_configuration_blocks_and_does_not_prevent_withdrawal(client, consent_setup, monkeypatch):
    headers, _, calls, settings = consent_setup
    assert accept(client, headers).status_code == 200
    monkeypatch.setattr(settings, "assistant_consent_text", None)
    assert ask(client, headers).status_code == 503
    assert client.get("/v1/chatbot/consent", headers=headers).status_code == 503
    response = client.post("/v1/chatbot/consent", headers=headers, json={"assistant_accepted": False})
    assert response.status_code == 200
    assert response.json()["health_accepted"] is False
    assert calls == []


def test_account_b_cannot_use_account_a_consent(client, consent_setup):
    headers, uid, calls, _ = consent_setup
    assert accept(client, headers, health=True).status_code == 200
    other = signup(client, "other@example.com")
    assert client.get("/v1/chatbot/consent", headers=other).json()["accepted"] is False
    assert ask(client, other, user_id=uid, consent_version=2).status_code == 428
    assert calls == []


def test_reject_stale_or_non_boolean_acceptance(client, consent_setup):
    headers, _, calls, _ = consent_setup
    assert client.post("/v1/chatbot/consent", headers=headers,
        json={"assistant_accepted": True, "consent_version": 1}).status_code == 409
    assert client.post("/v1/chatbot/consent", headers=headers,
        json={"assistant_accepted": "true", "consent_version": 2}).status_code == 422
    assert client.post("/v1/chatbot/consent", headers=headers,
        json={"assistant_accepted": False, "health_accepted": True,
              "health_consent_version": 2}).status_code == 409
    assert calls == []


def test_withdrawal_during_reservation_blocks_call_and_refunds(client, db_session, consent_setup, monkeypatch):
    from app.schemas.chatbot import AssistantConsentUpdate
    from app.services import assistant_consent

    headers, _, calls, _ = consent_setup
    assert accept(client, headers, health=True).status_code == 200
    original = route.chatbot_usage_service.reserve_request

    def reserve_then_withdraw(db, uid, limit, request_id):
        result = original(db, uid, limit, request_id)
        assistant_consent.save_consent(db, uid, AssistantConsentUpdate(assistant_accepted=False))
        return result

    monkeypatch.setattr(route.chatbot_usage_service, "reserve_request", reserve_then_withdraw)
    assert ask(client, headers, health_consent=True, health_consent_version=2).status_code == 428
    assert calls == []
    assert db_session.scalar(select(ChatbotUsageDaily.message_count)) == 0


def test_consent_endpoints_require_auth_and_disable_cache(client, consent_setup):
    headers, _, _, _ = consent_setup
    assert client.get("/v1/chatbot/consent").status_code == 401
    assert client.post("/v1/chatbot/consent", json={"assistant_accepted": True}).status_code == 401
    assert client.get("/v1/chatbot/consent", headers=headers).headers["Cache-Control"] == "no-store"
    assert accept(client, headers).headers["Cache-Control"] == "no-store"


def test_acceptance_of_text_that_changed_since_display_is_rejected(client, consent_setup, monkeypatch):
    headers, _, _, settings = consent_setup
    displayed = client.get("/v1/chatbot/consent", headers=headers).json()
    monkeypatch.setattr(settings, "assistant_consent_text", "Different text with the same version")
    response = client.post("/v1/chatbot/consent", headers=headers, json={
        "assistant_accepted": True, "consent_version": displayed["version"],
        "consent_hash": displayed["text_hash"],
    })
    assert response.status_code == 409
    assert ask(client, headers).status_code == 428
