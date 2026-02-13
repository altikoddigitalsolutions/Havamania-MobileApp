from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.models.alert import Alert, AlertSeverity
from app.models.location import Location
from app.models.user import User
from app.schemas.alert import AlertResponse, GroupedAlertsResponse

router = APIRouter()


def _to_alert_response(item: Alert) -> AlertResponse:
    return AlertResponse(
        id=item.id,
        location_id=item.location_id,
        severity=item.severity.value,
        title=item.title,
        description=item.description,
        starts_at=item.starts_at,
        ends_at=item.ends_at,
    )


@router.get("", response_model=GroupedAlertsResponse | list[AlertResponse])
def list_alerts(
    location_id: str | None = Query(default=None),
    severity: AlertSeverity | None = Query(default=None),
    grouped: bool = Query(default=True),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = db.query(Alert).join(Location, Alert.location_id == Location.id).filter(Location.user_id == current_user.id)

    if location_id:
        query = query.filter(Alert.location_id == location_id)
    if severity:
        query = query.filter(Alert.severity == severity)

    alerts = query.order_by(Alert.starts_at.desc()).all()
    mapped = [_to_alert_response(item) for item in alerts]

    if not grouped:
        return mapped

    critical = [item for item in mapped if item.severity == AlertSeverity.CRITICAL.value]
    active = [item for item in mapped if item.severity == AlertSeverity.ACTIVE.value]
    advisory = [item for item in mapped if item.severity == AlertSeverity.ADVISORY.value]
    return GroupedAlertsResponse(critical=critical, active=active, advisory=advisory)


@router.get("/{alert_id}", response_model=AlertResponse)
def get_alert(
    alert_id: str,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> AlertResponse:
    alert = (
        db.query(Alert)
        .join(Location, Alert.location_id == Location.id)
        .filter(Alert.id == alert_id, Location.user_id == current_user.id)
        .first()
    )
    if not alert:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Alert not found")

    return _to_alert_response(alert)
