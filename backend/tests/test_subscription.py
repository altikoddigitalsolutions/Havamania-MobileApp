from datetime import UTC, datetime, timedelta


def _auth_header(client):
    signup = client.post(
        "/v1/auth/signup",
        json={"email": "sub@example.com", "password": "Password123", "full_name": "Subscription User"},
    )
    token = signup.json()["access_token"]
    user_id = None
    # fetch from profile endpoint since signup response doesn't return user id
    profile = client.get("/v1/profile", headers={"Authorization": f"Bearer {token}"})
    user_id = profile.json()["user_id"]
    return {"Authorization": f"Bearer {token}"}, user_id


def test_subscription_status_default_free(client):
    headers, _ = _auth_header(client)

    response = client.get("/v1/subscription/status", headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert data["plan_code"] == "free"
    assert data["is_premium_active"] is False


def test_validate_receipt_activates_subscription(client):
    headers, _ = _auth_header(client)

    response = client.post(
        "/v1/subscription/validate-receipt",
        json={"store": "ios", "receipt_data": "demo", "plan_code": "premium_monthly"},
        headers=headers,
    )

    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "active"
    assert data["is_premium_active"] is True

    premium_weather = client.get("/v1/weather/map-layers?lat=41&lon=29&layer=wind", headers=headers)
    assert premium_weather.status_code == 200
    assert premium_weather.json()["premium"] is True

    premium_chatbot = client.post(
        "/v1/chatbot/ask/premium",
        json={"question": "Premium yanit testi"},
        headers=headers,
    )
    assert premium_chatbot.status_code == 200
    assert premium_chatbot.json()["is_premium"] is True


def test_store_webhook_updates_status(client):
    headers, user_id = _auth_header(client)

    response = client.post(
        "/v1/subscription/webhook/store",
        json={
            "user_id": user_id,
            "plan_code": "premium_monthly",
            "status": "grace",
            "expires_at": (datetime.now(UTC) + timedelta(days=2)).isoformat(),
            "store": "ios",
            "original_transaction_id": "tx-123",
        },
    )

    assert response.status_code == 200
    assert response.json()["is_premium_active"] is True

    status_response = client.get("/v1/subscription/status", headers=headers)
    assert status_response.status_code == 200
    assert status_response.json()["status"] == "grace"


def test_premium_endpoints_reject_free_users(client):
    headers, _ = _auth_header(client)

    premium_weather = client.get("/v1/weather/map-layers?lat=41&lon=29&layer=wind", headers=headers)
    assert premium_weather.status_code == 402

    premium_chatbot = client.post(
        "/v1/chatbot/ask/premium",
        json={"question": "Premium gerektiriyor mu?"},
        headers=headers,
    )
    assert premium_chatbot.status_code == 402
