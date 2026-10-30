from fastapi import HTTPException
from sqlalchemy.orm import Session

from app.models.alert import Alert
from app.models.location import Location
from app.services.account_access import require_active_account


class ExternalAlertProvider:
    """Skeleton provider for future real integrations."""

    def fetch_alerts(self, lat: float, lon: float) -> list[dict]:
        raise NotImplementedError("A real external alert provider must be configured")


def sync_alerts_for_location(db: Session, location_id: str, lat: float, lon: float) -> int:
    location = db.get(Location, location_id, populate_existing=True)
    if location is None:
        return 0
    try:
        require_active_account(db, location.user_id)
    except HTTPException as exc:
        if exc.status_code in {401, 409}:
            return 0
        raise
    provider = ExternalAlertProvider()
    payloads = provider.fetch_alerts(lat=location.lat, lon=location.lon)

    created = 0
    for payload in payloads:
        alert = Alert(
            location_id=location_id,
            severity=payload["severity"],
            title=payload["title"],
            description=payload["description"],
            starts_at=payload["starts_at"],
            ends_at=payload["ends_at"],
        )
        db.add(alert)
        created += 1

    db.commit()
    return created
