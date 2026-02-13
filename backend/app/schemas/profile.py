from pydantic import BaseModel, Field


class ProfileResponse(BaseModel):
    user_id: str
    primary_location_id: str | None
    temperature_unit: str
    wind_unit: str
    theme: str


class ProfileUpdateRequest(BaseModel):
    temperature_unit: str | None = Field(default=None, pattern="^(C|F)$")
    wind_unit: str | None = Field(default=None, pattern="^(kmh|mph)$")
    theme: str | None = Field(default=None, pattern="^(light|dark|system)$")
    primary_location_id: str | None = None


class LocationCreateRequest(BaseModel):
    label: str = Field(min_length=1, max_length=255)
    lat: float = Field(ge=-90, le=90)
    lon: float = Field(ge=-180, le=180)
    is_primary: bool = False
    is_tracking_enabled: bool = True


class LocationUpdateRequest(BaseModel):
    label: str | None = Field(default=None, min_length=1, max_length=255)
    lat: float | None = Field(default=None, ge=-90, le=90)
    lon: float | None = Field(default=None, ge=-180, le=180)
    is_primary: bool | None = None
    is_tracking_enabled: bool | None = None


class LocationResponse(BaseModel):
    id: str
    user_id: str
    label: str
    lat: float
    lon: float
    is_primary: bool
    is_tracking_enabled: bool


class NotificationPreferenceResponse(BaseModel):
    user_id: str
    severe_alert_enabled: bool
    daily_summary_enabled: bool
    rain_alert_enabled: bool


class NotificationPreferenceUpdateRequest(BaseModel):
    severe_alert_enabled: bool | None = None
    daily_summary_enabled: bool | None = None
    rain_alert_enabled: bool | None = None
