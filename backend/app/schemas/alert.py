from datetime import datetime

from pydantic import BaseModel


class AlertResponse(BaseModel):
    id: str
    location_id: str
    severity: str
    title: str
    description: str
    starts_at: datetime
    ends_at: datetime


class GroupedAlertsResponse(BaseModel):
    critical: list[AlertResponse]
    active: list[AlertResponse]
    advisory: list[AlertResponse]
