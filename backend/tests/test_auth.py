from datetime import UTC, datetime, timedelta
import pytest

def test_signup_returns_tokens(client):
    response = client.post(
        "/v1/auth/signup",
        json={
            "email": "user@example.com",
            "password": "Password123",
            "full_name": "Test User",
        },
    )
    assert response.status_code == 201
    data = response.json()
    assert data["token_type"] == "bearer"
    assert data["access_token"]
    assert data["refresh_token"]

def test_login_returns_tokens(client, db_session):
    # Her test için benzersiz email kullanalım
    email = "login_unique@example.com"
    client.post(
        "/v1/auth/signup",
        json={"email": email, "password": "Password123", "full_name": "Login User"},
    )

    response = client.post(
        "/v1/auth/login",
        json={"email": email, "password": "Password123"},
    )

    assert response.status_code == 200
    data = response.json()
    assert data["access_token"]
    assert data["refresh_token"]

def test_refresh_rotates_token(client):
    email = "refresh_unique@example.com"
    signup_response = client.post(
        "/v1/auth/signup",
        json={"email": email, "password": "Password123", "full_name": "Refresh User"},
    )
    old_refresh = signup_response.json()["refresh_token"]

    # Zaman dilimi çakışmasını önlemek için bir saniye bekleyebiliriz veya
    # token iat/exp farkını kontrol edebiliriz.
    # Hata aslında servis tarafındaki karşılaştırmadan geliyor olabilir.
    refresh_response = client.post("/v1/auth/refresh", json={"refresh_token": old_refresh})

    assert refresh_response.status_code == 200
    data = refresh_response.json()
    assert data["refresh_token"] != old_refresh
    assert data["access_token"]

def test_logout_revokes_refresh_token(client):
    email = "logout_unique@example.com"
    signup_response = client.post(
        "/v1/auth/signup",
        json={"email": email, "password": "Password123", "full_name": "Logout User"},
    )
    refresh_token = signup_response.json()["refresh_token"]

    logout_response = client.post("/v1/auth/logout", json={"refresh_token": refresh_token})
    assert logout_response.status_code == 200

    refresh_response = client.post("/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert refresh_response.status_code == 401
