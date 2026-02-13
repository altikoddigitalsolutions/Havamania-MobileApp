from datetime import UTC, datetime, timedelta

from app.models.alert import Alert, AlertSeverity


def _auth_header_and_location(client):
    signup = client.post(
        "/v1/auth/signup",
        json={"email": "alerts@example.com", "password": "Password123", "full_name": "Alerts User"},
    )
    access_token = signup.json()["access_token"]
    headers = {"Authorization": f"Bearer {access_token}"}

    location_response = client.post(
        "/v1/profile/locations",
        json={"label": "Istanbul", "lat": 41.01, "lon": 28.97, "is_primary": True},
        headers=headers,
    )
    return headers, location_response.json()["id"]


def test_list_alerts_grouped(client, db_session):
    headers, location_id = _auth_header_and_location(client)

    db_session.add_all(
        [
            Alert(
                location_id=location_id,
                severity=AlertSeverity.CRITICAL,
                title="Critical",
                description="Critical desc",
                starts_at=datetime.now(UTC),
                ends_at=datetime.now(UTC) + timedelta(hours=1),
            ),
            Alert(
                location_id=location_id,
                severity=AlertSeverity.ACTIVE,
                title="Active",
                description="Active desc",
                starts_at=datetime.now(UTC),
                ends_at=datetime.now(UTC) + timedelta(hours=1),
            ),
        ]
    )
    db_session.commit()

    response = client.get("/v1/alerts", headers=headers)

    assert response.status_code == 200
    data = response.json()
    assert len(data["critical"]) == 1
    assert len(data["active"]) == 1


def test_get_alert_detail(client, db_session):
    headers, location_id = _auth_header_and_location(client)

    alert = Alert(
        location_id=location_id,
        severity=AlertSeverity.ADVISORY,
        title="Advisory",
        description="Advisory desc",
        starts_at=datetime.now(UTC),
        ends_at=datetime.now(UTC) + timedelta(hours=2),
    )
    db_session.add(alert)
    db_session.commit()

    response = client.get(f"/v1/alerts/{alert.id}", headers=headers)

    assert response.status_code == 200
    assert response.json()["id"] == alert.id
