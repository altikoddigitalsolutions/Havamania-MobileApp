def _auth_header_for_user(client, email: str = "profile@example.com") -> dict[str, str]:
    signup = client.post(
        "/v1/auth/signup",
        json={"email": email, "password": "Password123", "full_name": "Profile User"},
    )
    access_token = signup.json()["access_token"]
    return {"Authorization": f"Bearer {access_token}"}


def test_get_and_update_profile(client):
    headers = _auth_header_for_user(client)

    get_response = client.get("/v1/profile", headers=headers)
    assert get_response.status_code == 200
    assert get_response.json()["temperature_unit"] == "C"

    patch_response = client.patch(
        "/v1/profile",
        json={"temperature_unit": "F", "wind_unit": "mph", "theme": "dark"},
        headers=headers,
    )
    assert patch_response.status_code == 200
    data = patch_response.json()
    assert data["temperature_unit"] == "F"
    assert data["wind_unit"] == "mph"
    assert data["theme"] == "dark"


def test_location_crud_and_primary_uniqueness(client):
    headers = _auth_header_for_user(client, email="location@example.com")

    first = client.post(
        "/v1/profile/locations",
        json={"label": "Istanbul", "lat": 41.01, "lon": 28.97, "is_primary": True},
        headers=headers,
    )
    assert first.status_code == 201
    first_id = first.json()["id"]

    second = client.post(
        "/v1/profile/locations",
        json={"label": "Ankara", "lat": 39.93, "lon": 32.85, "is_primary": False},
        headers=headers,
    )
    assert second.status_code == 201
    second_id = second.json()["id"]

    promote_second = client.patch(
        f"/v1/profile/locations/{second_id}",
        json={"is_primary": True},
        headers=headers,
    )
    assert promote_second.status_code == 200

    all_locations = client.get("/v1/profile/locations", headers=headers)
    assert all_locations.status_code == 200
    entries = {entry["id"]: entry for entry in all_locations.json()}
    assert entries[first_id]["is_primary"] is False
    assert entries[second_id]["is_primary"] is True

    profile = client.get("/v1/profile", headers=headers)
    assert profile.json()["primary_location_id"] == second_id

    delete_response = client.delete(f"/v1/profile/locations/{second_id}", headers=headers)
    assert delete_response.status_code == 204


def test_get_and_update_notification_preferences(client):
    headers = _auth_header_for_user(client, email="prefs@example.com")

    get_response = client.get("/v1/profile/notifications", headers=headers)
    assert get_response.status_code == 200
    assert get_response.json()["severe_alert_enabled"] is True

    patch_response = client.patch(
        "/v1/profile/notifications",
        json={"daily_summary_enabled": False, "rain_alert_enabled": True},
        headers=headers,
    )
    assert patch_response.status_code == 200
    data = patch_response.json()
    assert data["daily_summary_enabled"] is False
    assert data["rain_alert_enabled"] is True
