import json
from datetime import UTC, datetime
from uuid import uuid4

import httpx
import pytest
from app.api.v1.routes import chatbot as route
from app.models.chatbot_request import ChatbotRequest
from app.schemas.assistant_weather import AssistantWeatherData
from app.services import chatbot_replay
from app.services.chatbot_bridge import ChatbotBridgeClient, settings
from app.services.chatbot_clock import chatbot_day, chatbot_reset
from pydantic import SecretStr, ValidationError
from sqlalchemy import select

from test_chatbot import _auth_header


@pytest.mark.parametrize("completed", [False, True])
@pytest.mark.parametrize("seconds_remaining", [-1, 0, 1])
def test_replay_cleanup_respects_owner_and_exact_expiration(
    client, connected_bridge, db_session, monkeypatch, completed, seconds_remaining
):
    headers, _, state = connected_bridge
    state["timeout_once"] = not completed
    request_id = str(uuid4())
    response = client.post(
        "/v1/chatbot/ask", headers=headers, json={"question": "Hava?", "request_id": request_id}
    )
    assert response.status_code == (200 if completed else 504)
    db_session.expire_all()
    record = db_session.scalar(
        select(ChatbotRequest).where(ChatbotRequest.request_id == request_id)
    )
    now = int(chatbot_replay.time.time())
    record.replay_until = now + seconds_remaining
    db_session.commit()
    owner = record.user_id
    record_id = record.id
    expected_payload = record.platform_payload
    expected_reply = record.cached_reply
    monkeypatch.setattr(chatbot_replay.time, "time", lambda: now)

    chatbot_replay.purge_expired_replays(db_session, owner=str(uuid4()))
    db_session.expire_all()
    record = db_session.get(ChatbotRequest, record_id)
    assert record.platform_payload == expected_payload
    assert record.cached_reply == expected_reply

    chatbot_replay.purge_expired_replays(db_session, owner=owner)
    db_session.expire_all()
    record = db_session.get(ChatbotRequest, record_id)
    if seconds_remaining <= 0:
        assert record.platform_payload is None and record.cached_reply is None
    else:
        assert record.platform_payload == expected_payload
        assert record.cached_reply == expected_reply
    # Keep the identity tombstone so old request IDs cannot become new calls.
    assert record.request_id == request_id and record.input_hash is not None


@pytest.mark.parametrize(
    "completed,damage",
    [
        (False, "missing"),
        (False, "empty"),
        (False, "wrong_owner"),
        (False, "wrong_id"),
        (False, "wrong_question"),
        (False, "missing_context"),
        (True, "missing"),
        (True, "invalid"),
    ],
)
def test_damaged_replay_never_rebuilds_body_or_calls_platform(
    client, connected_bridge, db_session, completed, damage
):
    headers, calls, state = connected_bridge
    state["timeout_once"] = not completed
    payload = {"question": "Hava?", "request_id": str(uuid4())}
    first = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert first.status_code == (200 if completed else 504)
    db_session.expire_all()
    record = db_session.scalar(
        select(ChatbotRequest).where(ChatbotRequest.request_id == payload["request_id"])
    )
    if completed:
        record.cached_reply = None if damage == "missing" else {"answer": "Incomplete"}
    else:
        saved = dict(record.platform_payload)
        if damage == "missing":
            saved = None
        elif damage == "empty":
            saved = {}
        elif damage == "wrong_owner":
            saved["user_id"] = str(uuid4())
        elif damage == "wrong_id":
            saved["request_id"] = str(uuid4())
        elif damage == "wrong_question":
            saved["question"] = "Different stored question"
        else:
            saved.pop("context")
        record.platform_payload = saved
    db_session.commit()
    retried = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert retried.status_code == 409, retried.text
    assert retried.json()["error"] == "request_expired"
    assert len(calls) == 1
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == int(
        completed
    )


def weather():
    return {
        "observed_at": "2026-10-30T14:30:00+03:00",
        "timezone": "Europe/Istanbul",
        "location": {"name": "Ankara", "lat": 39.9, "lon": 32.8},
        "current": {"temp_c": 18.5, "humidity_pct": 60},
        "stale": False,
        "forecast": [
            {"date": "2026-10-30", "min_c": 10, "max_c": 19},
            {"date": "2026-10-31", "min_c": 9, "max_c": 17},
        ],
    }


@pytest.mark.parametrize(
    "change",
    [
        {"observed_at": "2026-10-30T14:30"},
        {"observed_at": "2026-02-30T14:30Z"},
        {"timezone": "Unknown/City"},
        {"current": {"temp_c": "18"}},
        {"current": {"humidity_pct": 101}},
        {"current": {"temp_c": float("nan")}},
        {"stale": "false"},
        {"forecast": [{"date": "2026-10-30"}, {"date": "2026-11-01"}]},
        {"forecast": [{"date": "2026-10-30"}, {"date": "2026-10-30"}]},
        {"forecast": [{"date": "2026-02-30"}]},
        {"forecast": [{"date": "2026-10-30", "min_c": 20, "max_c": 10}]},
    ],
)
def test_weather_contract_rejects_invalid_data(change):
    with pytest.raises(ValidationError):
        AssistantWeatherData.model_validate(weather() | change)


def test_weather_contract_strips_coordinates_and_sorts_consecutive_dates():
    body = weather()
    body["forecast"].reverse()
    clean = AssistantWeatherData.model_validate(body).model_dump(exclude_none=True)
    assert clean["forecast"][0]["date"] == "2026-10-30"
    assert clean["location"] == {"name": "Ankara"}
    assert AssistantWeatherData.model_validate(
        {"observed_at": "2026-10-30T11:30Z", "timezone": "UTC"}
    )
    assert AssistantWeatherData.model_validate(
        {"observed_at": "2026-10-30T11:30:12.123456789Z", "timezone": "UTC"}
    )


def test_istanbul_quota_day_and_midnight_are_not_utc():
    assert str(chatbot_day(datetime(2026, 10, 30, 20, 59, tzinfo=UTC))) == "2026-10-30"
    assert str(chatbot_day(datetime(2026, 10, 30, 21, 0, tzinfo=UTC))) == "2026-10-31"
    assert (
        chatbot_reset(chatbot_day(datetime(2026, 10, 30, 20, 59, tzinfo=UTC))).isoformat()
        == "2026-10-31T00:00:00+03:00"
    )


@pytest.fixture
def connected_bridge(client, monkeypatch, assistant_texts):
    headers = _auth_header(client)
    original = httpx.Client
    calls = []
    state = {"timeout_once": False, "model_calls": 0, "answers": {}, "error": None}

    def handler(request):
        assert request.headers["Authorization"] == "Bearer LOCAL_PLATFORM_KEY"
        body = json.loads(request.content)
        calls.append(body)
        if state["error"]:
            status, code = state["error"]
            return httpx.Response(status, json={"error": code, "message": "PRIVATE_UPSTREAM"})
        key = body["request_id"]
        if key not in state["answers"]:
            state["model_calls"] += 1
            state["answers"][key] = {
                "answer": "Recorded answer",
                "conversation_id": body["conversation_id"] or str(uuid4()),
                "message_id": str(uuid4()),
                "disclaimer": None,
            }
        if state["timeout_once"] and len(calls) == 1:
            raise httpx.ReadTimeout("Local simulated lost response")
        return httpx.Response(200, json=state["answers"][key])

    monkeypatch.setattr(
        httpx, "Client", lambda **kwargs: original(transport=httpx.MockTransport(handler), **kwargs)
    )
    monkeypatch.setattr(settings, "chatbot_api_key", SecretStr("LOCAL_PLATFORM_KEY"))
    monkeypatch.setattr(
        route,
        "chatbot_bridge_client",
        ChatbotBridgeClient(base_url="https://platform.invalid/api/bridge"),
    )
    return headers, calls, state


def test_api_bridge_v3_replay_preserves_ids_context_optout_and_charges_once(
    client, connected_bridge
):
    headers, calls, state = connected_bridge
    payload = {
        "question": "  Ankara hava?  ",
        "request_id": str(uuid4()),
        "conversation_id": str(uuid4()),
        "personalization_enabled": False,
        "context": {"city": "Ankara", "trips": "PRIVATE_TRIPS", "weather_data": weather()},
    }
    first = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert first.status_code == 200, first.text
    assert first.headers["cache-control"] == "no-store"
    assert calls[0]["request_id"] == payload["request_id"]
    assert calls[0]["conversation_id"] == payload["conversation_id"]
    assert calls[0]["question"] == payload["question"]
    assert calls[0]["personalization"] is False
    assert "trips" not in calls[0]["context"] and "profile" not in calls[0]["context"]
    assert calls[0]["context"]["weather_data"]["location"] == {"name": "Ankara"}
    payload["context"]["weather_data"]["observed_at"] = "2026-10-30T14:31:00+03:00"
    second = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert second.status_code == 200 and second.json() == first.json()
    assert second.headers["cache-control"] == "no-store"
    assert len(calls) == 1 and state["model_calls"] == 1
    conflict = client.post(
        "/v1/chatbot/ask", headers=headers, json=payload | {"question": "Different question"}
    )
    assert conflict.status_code == 422 and conflict.json()["error"] == "request_conflict"
    assert conflict.headers["cache-control"] == "no-store"
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == 1


@pytest.mark.parametrize(
    "status,code",
    [
        (409, "user_deleted"),
        (409, "request_in_progress"),
        (409, "conversation_conflict"),
        (422, "request_id_conflict"),
    ],
)
def test_platform_errors_keep_codes_refund_quota_and_never_cache(
    client, connected_bridge, status, code
):
    headers, calls, state = connected_bridge
    state["error"] = (status, code)
    request_id = str(uuid4())
    payload = {"question": "Hava?", "request_id": request_id}
    response = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert response.status_code == status, response.text
    assert response.json()["error"] == code
    assert "PRIVATE_UPSTREAM" not in response.text
    assert response.headers["cache-control"] == "no-store"
    assert len(calls) == 1 and calls[0]["request_id"] == request_id
    usage = client.get("/v1/chatbot/usage", headers=headers)
    assert usage.json()["used_messages_today"] == 0
    assert usage.headers["cache-control"] == "no-store"
    if code == "request_in_progress":
        state["error"] = None
        retried = client.post("/v1/chatbot/ask", headers=headers, json=payload)
        assert retried.status_code == 200, retried.text
        assert calls[0] == calls[1]
        assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == 1


def test_assistant_auth_and_validation_errors_are_not_cacheable(client, connected_bridge):
    headers, _, _ = connected_bridge
    unauthorized = client.post("/v1/chatbot/ask", json={"question": "Hava?"})
    assert unauthorized.status_code in {401, 403}
    assert unauthorized.headers["cache-control"] == "no-store"
    invalid = client.post("/v1/chatbot/ask", headers=headers, json={"question": "x" * 4001})
    assert invalid.status_code == 422
    assert invalid.headers["cache-control"] == "no-store"
    assert "cache-control" not in client.get("/health").headers


def test_timeout_retry_reuses_frozen_platform_body_and_recovers_answer_without_model_recall(
    client, connected_bridge
):
    headers, calls, state = connected_bridge
    state["timeout_once"] = True
    payload = {
        "question": "Hava?",
        "request_id": str(uuid4()),
        "context": {"city": "Ankara", "weather_data": weather()},
    }
    first = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert first.status_code == 504
    assert len(calls) == 1  # No automatic timeout retry.
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == 0
    payload["context"]["weather_data"]["observed_at"] = "2026-10-30T14:35:00+03:00"
    second = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert second.status_code == 200, second.text
    assert calls[0] == calls[1]
    assert state["model_calls"] == 1
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == 1


@pytest.mark.parametrize("completed", [False, True])
def test_replay_cannot_reuse_old_consent_after_new_document_acceptance(
    client, connected_bridge, monkeypatch, completed
):
    headers, calls, state = connected_bridge
    state["timeout_once"] = not completed
    payload = {"question": "Hava?", "request_id": str(uuid4())}
    first = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert first.status_code == (200 if completed else 504)

    monkeypatch.setattr(settings, "assistant_consent_version", 3)
    monkeypatch.setattr(settings, "assistant_consent_text", "New local test document")
    blocked = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert blocked.status_code == 428
    docs = client.get("/v1/chatbot/consent", headers=headers).json()
    accepted = client.post(
        "/v1/chatbot/consent",
        headers=headers,
        json={"assistant_accepted": True, "consent_version": 3, "consent_hash": docs["text_hash"]},
    )
    assert accepted.status_code == 200, accepted.text

    retry = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert retry.status_code == 422, retry.text
    assert retry.json()["error"] == "request_conflict"
    assert len(calls) == 1
    assert client.get("/v1/chatbot/usage", headers=headers).json()["used_messages_today"] == int(
        completed
    )

    fresh = client.post(
        "/v1/chatbot/ask", headers=headers, json={**payload, "request_id": str(uuid4())}
    )
    assert fresh.status_code == 200, fresh.text
    assert calls[-1]["consent_version"] == 3
