from datetime import UTC, datetime, timedelta

from sqlalchemy.orm import Session

from app.models.alert import Alert, AlertSeverity


class ExternalAlertProvider:
    """Skeleton provider for future real integrations."""

    def fetch_alerts(self, lat: float, lon: float) -> list[dict]:
        # TODO: Integrate external weather alerts provider
        return [
            {
                "severity": AlertSeverity.ADVISORY,
                "title": "Demo Wind Notice",
                "description": "Sample alert from ingest skeleton.",
                "starts_at": datetime.now(UTC),
                "ends_at": datetime.now(UTC) + timedelta(hours=2),
            }
        ]


def sync_alerts_for_location(db: Session, location_id: str, lat: float, lon: float) -> int:
    provider = ExternalAlertProvider()
    payloads = provider.fetch_alerts(lat=lat, lon=lon)

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
