from uuid import uuid4
import httpx
import pytest
from app.services.assistant_upload import parse_photo, check_upload_rate, _upload_hits
from app.services.chatbot_bridge import ChatbotBridgeClient, BridgeError
from app.schemas.chatbot import ChatbotAskRequest
from app.services.chatbot_replay import request_fingerprint
from test_assistant_consent import signup, accept


def multipart(content=b"\xff\xd8\xffphoto", media="image/jpeg", name="file"):
    return (
        f'--test\r\nContent-Disposition: form-data; name="{name}"; filename="x"\r\nContent-Type: {media}\r\n\r\n'.encode()
        + content
        + b"\r\n--test--\r\n"
    )


def test_memory_parser_validates_type_and_field():
    assert parse_photo("multipart/form-data; boundary=test", multipart())[1] == "image/jpeg"
    for body, status in [
        (multipart(b"not a photo"), 415),
        (multipart(name="user_id"), 400),
        (multipart(b"x" * (8 * 1024 * 1024 + 1)), 413),
    ]:
        with pytest.raises(BridgeError) as error:
            parse_photo("multipart/form-data; boundary=test", body)
        assert error.value.status == status


def test_attachment_identity_is_set_not_order_or_case():
    a, b = str(uuid4()), str(uuid4())
    p = ChatbotAskRequest(question="test", attachment_ids=[a, b, a.upper()])
    wire = {
        "context": {},
        "tone": "dengeli",
        "health_consent": False,
        "consent_version": 2,
        "health_consent_version": None,
    }
    assert len(p.attachment_ids) == 2
    assert request_fingerprint(p, wire) == request_fingerprint(
        ChatbotAskRequest(question="test", attachment_ids=[b, a]), wire
    )
    assert request_fingerprint(p, wire) != request_fingerprint(
        ChatbotAskRequest(question="test"), wire
    )


def test_upload_rate_is_per_owner():
    _upload_hits.clear()
    for _ in range(10):
        check_upload_rate("a")
    with pytest.raises(BridgeError) as error:
        check_upload_rate("a")
    assert error.value.status == 429
    check_upload_rate("b")
    _upload_hits.clear()


def test_bridge_upload_metadata_and_no_ambiguous_retry(monkeypatch):
    calls = []
    attachment = str(uuid4())

    def post(self, url, **kwargs):
        calls.append(kwargs)
        assert url.endswith("/upload")
        return httpx.Response(
            201, json={"attachment_id": attachment, "media_type": "image/jpeg", "size": 10}
        )

    monkeypatch.setattr(httpx.Client, "post", post)
    bridge = ChatbotBridgeClient(api_key="LOCAL_KEY")
    assert str(bridge.upload(b"photo", "image/jpeg", "server-owner", 2).attachment_id) == attachment
    assert calls[0]["data"]["user_id"] == "server-owner"

    def timeout(*args, **kwargs):
        calls.append(kwargs)
        raise httpx.ReadTimeout("secret")

    monkeypatch.setattr(httpx.Client, "post", timeout)
    with pytest.raises(BridgeError) as error:
        bridge.upload(b"photo", "image/jpeg", "owner", 2)
    assert len(calls) == 2
    assert "secret" not in str(error.value)


def test_upload_requires_consent_and_does_not_trust_owner(client, assistant_texts, monkeypatch):
    from app.api.v1.routes import chatbot as route
    from app.schemas.chatbot import AssistantAttachmentReceipt

    headers = signup(client, "photo@example.com")
    assert (
        client.post(
            "/v1/chatbot/attachments",
            headers=headers,
            files={"file": ("x.jpg", b"\xff\xd8\xffx", "image/jpeg")},
        ).status_code
        == 428
    )
    assert accept(client, headers).status_code == 200
    calls = []

    def upload(photo, media, owner, version, app_version):
        calls.append((owner, version))
        return AssistantAttachmentReceipt(attachment_id=uuid4(), media_type=media, size=len(photo))

    monkeypatch.setattr(route.chatbot_bridge_client, "upload", upload)
    response = client.post(
        "/v1/chatbot/attachments",
        headers=headers,
        files={"file": ("x.jpg", b"\xff\xd8\xffx", "image/jpeg")},
    )
    assert response.status_code == 201
    assert response.headers["cache-control"] == "no-store"
    assert calls[0][1] == 2
    response = client.post(
        "/v1/chatbot/attachments",
        headers=headers,
        files={"file": ("x.jpg", b"\xff\xd8\xffx", "image/jpeg")},
        data={"user_id": "attacker"},
    )
    assert response.status_code == 400
    assert len(calls) == 1


@pytest.mark.parametrize(
    "status,code,expected",
    [
        (409, "photos_disabled", 409),
        (415, "unsupported_media_type", 415),
        (401, "unauthorized", 503),
    ],
)
def test_platform_photo_errors_are_safe(monkeypatch, status, code, expected):
    monkeypatch.setattr(
        httpx.Client,
        "post",
        lambda *a, **k: httpx.Response(status, json={"error": code, "message": "SECRET"}),
    )
    with pytest.raises(BridgeError) as error:
        ChatbotBridgeClient(api_key="LOCAL_KEY").upload(b"photo", "image/jpeg", "owner", 2)
    assert error.value.status == expected
    assert "SECRET" not in str(error.value)


def test_photo_ask_replay_uses_canonical_ids_and_rejects_changes(
    client, assistant_texts, monkeypatch, db_session
):
    from app.api.v1.routes import chatbot as route
    from app.schemas.chatbot import PlatformChatbotReply
    from app.models.chatbot_usage_daily import ChatbotUsageDaily
    from sqlalchemy import select

    headers = signup(client, "replay-photo@example.com")
    assert accept(client, headers).status_code == 200
    calls = []

    def ask(**wire):
        calls.append(wire)
        return PlatformChatbotReply(
            answer="Yerel fotoğraf yanıtı", conversation_id=uuid4(), message_id=uuid4()
        )

    monkeypatch.setattr(route.chatbot_bridge_client, "ask", ask)
    a, b = str(uuid4()), str(uuid4())
    payload = {
        "question": "Fotoğrafı yorumla",
        "request_id": str(uuid4()),
        "attachment_ids": [a, b],
    }
    first = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert first.status_code == 200
    payload["attachment_ids"] = [b, a.upper(), a]
    second = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert second.status_code == 200
    assert first.json()["answer"] == second.json()["answer"]
    assert len(calls) == 1
    assert calls[0]["attachment_ids"] == sorted([a, b])
    assert db_session.scalar(select(ChatbotUsageDaily)).message_count == 1
    payload["attachment_ids"] = [str(uuid4())]
    assert client.post("/v1/chatbot/ask", headers=headers, json=payload).status_code == 422
    assert len(calls) == 1


def test_five_photos_rejected_before_bridge(client, assistant_texts):
    headers = signup(client, "five-photo@example.com")
    response = client.post(
        "/v1/chatbot/ask",
        headers=headers,
        json={"question": "x", "attachment_ids": [str(uuid4()) for _ in range(5)]},
    )
    assert response.status_code == 422


def test_upload_is_authenticated(client):
    assert (
        client.post(
            "/v1/chatbot/attachments", files={"file": ("x", b"x", "image/jpeg")}
        ).status_code
        == 401
    )


def test_duplicate_ids_count_only_once_before_limit_validation():
    value = str(uuid4())
    assert (
        len(
            ChatbotAskRequest(
                question="x", attachment_ids=[value, value.upper()] * 5
            ).attachment_ids
        )
        == 1
    )


def test_bridge_busy_preserves_bounded_retry_after(monkeypatch):
    monkeypatch.setattr(
        httpx.Client,
        "post",
        lambda *a, **k: httpx.Response(503, json={"error": "busy"}, headers={"Retry-After": "5"}),
    )
    with pytest.raises(BridgeError) as error:
        ChatbotBridgeClient(api_key="LOCAL_KEY").upload(b"photo", "image/jpeg", "owner", 2)
    assert error.value.retry_after == "5"


def test_rejected_photo_ask_refunds_reservation(client, assistant_texts, monkeypatch, db_session):
    from app.api.v1.routes import chatbot as route
    from app.models.chatbot_usage_daily import ChatbotUsageDaily
    from sqlalchemy import select

    headers = signup(client, "expired-photo@example.com")
    assert accept(client, headers).status_code == 200

    def ask(**wire):
        raise BridgeError(400, "invalid_request", "Fotoğraf kullanılamıyor.")

    monkeypatch.setattr(route.chatbot_bridge_client, "ask", ask)
    response = client.post(
        "/v1/chatbot/ask",
        headers=headers,
        json={"question": "x", "request_id": str(uuid4()), "attachment_ids": [str(uuid4())]},
    )
    assert response.status_code == 400
    assert db_session.scalar(select(ChatbotUsageDaily)).message_count == 0


def test_deleting_account_cannot_upload(client, assistant_texts, monkeypatch):
    from app.api.v1.routes import chatbot as route

    headers = signup(client, "deleting-photo@example.com")
    monkeypatch.setattr(route, "_deletion_pending", lambda *a, **k: True)
    response = client.post(
        "/v1/chatbot/attachments", headers=headers, files={"file": ("x", b"x", "image/jpeg")}
    )
    assert response.status_code == 409
    assert response.json()["error"] == "account_deletion_pending"


def test_slow_body_cannot_hold_upload_slot_forever(monkeypatch):
    import anyio
    from app.services import assistant_upload

    monkeypatch.setattr(assistant_upload, "UPLOAD_READ_TIMEOUT", 0.01)

    class SlowRequest:
        async def stream(self):
            yield b"first"
            await anyio.sleep(1)
            yield b"late"

    async def run():
        with pytest.raises(BridgeError) as error:
            await assistant_upload.read_photo_body(SlowRequest())
        assert error.value.status == 408

    anyio.run(run)
