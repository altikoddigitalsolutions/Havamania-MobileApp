from datetime import UTC, datetime, timedelta

from app.models.alert import Alert, AlertSeverity
from app.models.notification_preference import NotificationPreference
from app.services.push_service import PushService


class StubPushProvider:
    def __init__(self):
        self.calls = []

    def send(self, platform: str, token: str, title: str, body: str) -> bool:
        self.calls.append((platform, token, title, body))
        return True


def _auth_headers(client):
    signup = client.post(
        "/v1/auth/signup",
        json={"email": "push@example.com", "password": "Password123", "full_name": "Push User"},
    )
    token = signup.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_register_push_token(client):
    headers = _auth_headers(client)

    response = client.post(
        "/v1/devices/push-token",
        json={"platform": "ios", "token": "ios_token_1234567890"},
        headers=headers,
    )

    assert response.status_code == 201
    assert response.json()["platform"] == "ios"


def test_send_critical_alert_respects_preferences(client, db_session):
    headers = _auth_headers(client)
    location = client.post(
        "/v1/profile/locations",
        json={"label": "Istanbul", "lat": 41.0, "lon": 29.0, "is_primary": True},
        headers=headers,
    ).json()

    client.post(
        "/v1/devices/push-token",
        json={"platform": "ios", "token": "ios_token_1234567890"},
        headers=headers,
    )

    prefs = db_session.get(NotificationPreference, location["user_id"])
    prefs.severe_alert_enabled = True
    db_session.add(prefs)
    db_session.commit()

    alert = Alert(
        location_id=location["id"],
        severity=AlertSeverity.CRITICAL,
        title="Fırtına",
        description="Kritik fırtına uyarısı",
        starts_at=datetime.now(UTC),
        ends_at=datetime.now(UTC) + timedelta(hours=1),
    )
    db_session.add(alert)
    db_session.commit()

    provider = StubPushProvider()
    service = PushService(provider=provider)

    sent = service.send_critical_alert(db_session, alert)

    assert sent == 1
    assert len(provider.calls) == 1
