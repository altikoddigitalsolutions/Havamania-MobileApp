from sqlalchemy.orm import Session

from app.models.alert import Alert
from app.models.location import Location
from app.models.notification_preference import NotificationPreference
from app.models.push_token import PushToken
from app.services.push_provider import FCMApnsPushProvider, PushProvider


class PushService:
    def __init__(self, provider: PushProvider | None = None):
        self.provider = provider or FCMApnsPushProvider()

    def send_critical_alert(self, db: Session, alert: Alert) -> int:
        location = db.get(Location, alert.location_id)
        if not location:
            return 0

        prefs = db.get(NotificationPreference, location.user_id)
        if prefs and not prefs.severe_alert_enabled:
            return 0

        tokens = db.query(PushToken).filter(PushToken.user_id == location.user_id).all()
        sent = 0
        for token in tokens:
            ok = self.provider.send(
                platform=token.platform,
                token=token.token,
                title=f"Kritik Hava Uyarisi: {alert.title}",
                body=alert.description,
            )
            if ok:
                sent += 1
        return sent


push_service = PushService()
