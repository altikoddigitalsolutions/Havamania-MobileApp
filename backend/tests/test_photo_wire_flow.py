import json
from email import policy
from email.parser import BytesParser
from uuid import uuid4

import httpx
import pytest
from sqlalchemy import select
from app.api.v1.routes import chatbot as route
from app.models.user import User
from app.services.chatbot_bridge import BridgeError, ChatbotBridgeClient
from test_assistant_consent import signup, accept


@pytest.mark.parametrize("status,code,expected", [
    (401, "unauthorized", "bridge_auth_failed"),
    (403, "forbidden", "bridge_auth_failed"),
    (404, "missing", "upload_endpoint_unavailable"),
    (405, "wrong_method", "upload_endpoint_unavailable"),
    (501, "missing", "upload_endpoint_unavailable"),
    (503, "not_configured", "not_configured"),
])
def test_upload_deployment_errors_keep_safe_distinct_codes(monkeypatch, status, code, expected):
    monkeypatch.setattr(httpx.Client, "post", lambda *args, **kwargs:
        httpx.Response(status, json={"error": code, "message": "PRIVATE_SERVER_DETAIL"}))
    with pytest.raises(BridgeError) as error:
        ChatbotBridgeClient(api_key="LOCAL_TEST_KEY").upload(b"photo", "image/jpeg", "test-owner", 2)
    assert error.value.status == 503
    assert error.value.code == expected
    assert "PRIVATE_SERVER_DETAIL" not in str(error.value)


def test_real_route_to_bridge_multipart_then_ask_and_replay(client, assistant_texts, db_session, monkeypatch):
    headers = signup(client, "wire-photo-a@example.com")
    other_headers = signup(client, "wire-photo-b@example.com")
    assert accept(client, headers).status_code == 200
    assert accept(client, other_headers).status_code == 200
    owner = db_session.scalar(select(User.id).where(User.email == "wire-photo-a@example.com"))
    attachment, conversation, message = str(uuid4()), str(uuid4()), str(uuid4())
    image = b"\x89PNG\r\n\x1a\nLOCAL_SYNTHETIC_WIRE_FIXTURE"
    calls = []
    model_calls = []
    real_client = httpx.Client

    def platform(request):
        calls.append(request.url.path)
        assert request.headers["authorization"] == "Bearer LOCAL_TEST_KEY"
        if request.url.path == "/api/bridge/upload":
            mime = BytesParser(policy=policy.default).parsebytes(
                f"Content-Type: {request.headers['content-type']}\r\nMIME-Version: 1.0\r\n\r\n".encode() + request.content)
            fields = {part.get_param("name", header="content-disposition"): part for part in mime.iter_parts()}
            assert set(fields) == {"user_id", "consent_version", "platform", "app_version", "file"}
            assert fields["user_id"].get_payload(decode=True).decode() == str(owner)
            assert fields["consent_version"].get_payload(decode=True) == b"2"
            assert fields["platform"].get_payload(decode=True) == b"android"
            assert fields["app_version"].get_payload(decode=True) == b"1.3.1"
            assert fields["file"].get_content_type() == "image/png"
            assert fields["file"].get_payload(decode=True) == image
            return httpx.Response(201, json={"attachment_id": attachment, "media_type": "image/png", "size": len(image)})
        assert request.url.path == "/api/bridge/ask"
        body = json.loads(request.content)
        if body["user_id"] != str(owner):
            return httpx.Response(400, json={"error": "invalid_request", "field": "attachment_ids"})
        assert body["attachment_ids"] == [attachment]
        assert body["consent_version"] == 2
        assert body["question"] == "Bu fotoğrafı yorumlar mısın?"
        model_calls.append(body)
        return httpx.Response(200, json={"answer": "Yerel sahte platform fotoğraf yanıtı",
            "conversation_id": conversation, "message_id": message})

    def local_client(*args, **kwargs):
        kwargs["transport"] = httpx.MockTransport(platform)
        return real_client(*args, **kwargs)

    monkeypatch.setattr(httpx, "Client", local_client)
    monkeypatch.setattr(route, "chatbot_bridge_client", ChatbotBridgeClient(
        base_url="https://platform.invalid/api/bridge", api_key="LOCAL_TEST_KEY"))
    uploaded = client.post("/v1/chatbot/attachments", headers={**headers, "X-App-Version": "1.3.1"},
        files={"file": ("photo.png", image, "image/png")})
    assert uploaded.status_code == 201
    assert uploaded.json()["attachment_id"] == attachment
    payload = {"question": "Bu fotoğrafı yorumlar mısın?", "request_id": str(uuid4()),
        "attachment_ids": [attachment], "personalization_enabled": False}
    first = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert first.status_code == 200
    replay = client.post("/v1/chatbot/ask", headers=headers, json=payload)
    assert replay.status_code == 200
    assert replay.json()["answer"] == first.json()["answer"]
    assert len(model_calls) == 1
    assert calls == ["/api/bridge/upload", "/api/bridge/ask"]
    foreign = client.post("/v1/chatbot/ask", headers=other_headers,
        json={**payload, "request_id": str(uuid4())})
    assert foreign.status_code == 400
    assert len(model_calls) == 1


def test_photo_route_is_registered_and_requires_authentication(client):
    # Production probe must reject an anonymous caller, not report a missing route.
    response = client.post("/v1/chatbot/attachments")
    assert response.status_code == 401
