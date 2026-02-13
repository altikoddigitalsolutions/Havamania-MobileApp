from app.api.v1.routes import chatbot as chatbot_route


class StubBridge:
    def ask(self, question: str, user_id: str) -> str:
        return f"stub-answer:{question}"


class StubFailingBridge:
    def ask(self, question: str, user_id: str) -> str:
        return "Yanıt gecikiyor. Lütfen tekrar deneyin."


def _auth_header(client):
    signup = client.post(
        "/v1/auth/signup",
        json={"email": "chatbot@example.com", "password": "Password123", "full_name": "Chatbot User"},
    )
    token = signup.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


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


def test_chatbot_timeout_fallback_response(client, monkeypatch):
    headers = _auth_header(client)
    monkeypatch.setattr(chatbot_route, "chatbot_bridge_client", StubFailingBridge())

    response = client.post("/v1/chatbot/ask", json={"question": "Gecikme testi"}, headers=headers)

    assert response.status_code == 200
    assert "Yanıt gecikiyor" in response.json()["answer"]
