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
    # Benzersiz email ile kayıt
    email = f"push_{datetime.now().timestamp()}@example.com"
    signup = client.post(
        "/v1/auth/signup",
        json={"email": email, "password": "Password123", "full_name": "Push User"},
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
    location_resp = client.post(
        "/v1/profile/locations",
        json={"label": "Istanbul", "lat": 41.0, "lon": 29.0, "is_primary": True},
        headers=headers,
    )
    location = location_resp.json()
    user_id = location["user_id"]

    client.post(
        "/v1/devices/push-token",
        json={"platform": "ios", "token": "ios_token_1234567890"},
        headers=headers,
    )

    # NotificationPreference nesnesi yoksa oluştur, varsa güncelle
    prefs = db_session.get(NotificationPreference, user_id)
    if prefs is None:
        prefs = NotificationPreference(user_id=user_id)
        db_session.add(prefs)

    prefs.severe_alert_enabled = True
    db_session.commit()

    # aware datetime kullanımı
    now = datetime.now(UTC)
    alert = Alert(
        location_id=location["id"],
        severity=AlertSeverity.CRITICAL,
        title="Fırtına",
        description="Kritik fırtına uyarısı",
        starts_at=now,
        ends_at=now + timedelta(hours=1),
    )
    db_session.add(alert)
    db_session.commit()

    provider = StubPushProvider()
    service = PushService(provider=provider)

    sent = service.send_critical_alert(db_session, alert)

    assert sent == 1
    assert len(provider.calls) == 1
