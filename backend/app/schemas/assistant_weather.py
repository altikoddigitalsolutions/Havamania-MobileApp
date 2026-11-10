import itertools
import json
import re
from datetime import date, datetime, timedelta
from typing import Annotated
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import BaseModel, ConfigDict, Field, StrictBool, field_validator, model_validator

Temperature = Annotated[float, Field(strict=True, ge=-90, le=60)]
Maximum = Annotated[float, Field(strict=True, ge=-90, le=70)]
Percent = Annotated[float, Field(strict=True, ge=0, le=100)]


class WeatherPart(BaseModel):
    model_config = ConfigDict(extra="ignore", allow_inf_nan=False)


class WeatherLocation(WeatherPart):
    name: str = Field(min_length=1, max_length=100)


class CurrentWeather(WeatherPart):
    temp_c: Temperature | None = None
    feels_like_c: Maximum | None = None
    condition: str | None = Field(None, max_length=100)
    humidity_pct: Percent | None = None
    wind_kmh: Annotated[float, Field(strict=True, ge=0, le=500)] | None = None
    precip_mm: Annotated[float, Field(strict=True, ge=0, le=2000)] | None = None


class ForecastWeather(WeatherPart):
    date: str = Field(pattern=r"^\d{4}-\d{2}-\d{2}$")
    min_c: Temperature | None = None
    max_c: Maximum | None = None
    condition: str | None = Field(None, max_length=100)
    precip_prob_pct: Percent | None = None

    @field_validator("date")
    @classmethod
    def calendar_date(cls, value):
        date.fromisoformat(value)
        return value

    @model_validator(mode="after")
    def temperature_range(self):
        if self.min_c is not None and self.max_c is not None and self.min_c > self.max_c:
            raise ValueError("Minimum exceeds maximum")
        return self


class AssistantWeatherData(WeatherPart):
    observed_at: str
    timezone: str = Field(max_length=64)
    location: WeatherLocation | None = None
    current: CurrentWeather | None = None
    forecast: list[ForecastWeather] | None = Field(None, max_length=10)
    stale: StrictBool | None = None
    source: str | None = Field(None, max_length=100)

    @field_validator("observed_at")
    @classmethod
    def offset_timestamp(cls, value):
        if not re.fullmatch(
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,9})?)?(Z|[+-]\d{2}:\d{2})", value
        ):
            raise ValueError("Timestamp must include an offset")
        # fromisoformat normalizes offsets such as +03:99 into +04:39.
        # Reject malformed clock components instead of silently changing the instant.
        if not value.endswith("Z"):
            offset = value[-6:]
            if int(offset[1:3]) > 23 or int(offset[4:6]) > 59:
                raise ValueError("Timestamp offset is invalid")
        datetime.fromisoformat(value)
        return value

    @field_validator("timezone")
    @classmethod
    def known_zone(cls, value):
        try:
            ZoneInfo(value)
        except (ZoneInfoNotFoundError, ValueError) as exc:
            raise ValueError("Unknown IANA timezone") from exc
        return value

    @model_validator(mode="after")
    def consecutive_and_bounded(self):
        if self.forecast:
            ordered = sorted(self.forecast, key=lambda item: item.date)
            days = [date.fromisoformat(item.date) for item in ordered]
            if any(b - a != timedelta(days=1) for a, b in itertools.pairwise(days)):
                raise ValueError("Forecast dates must be unique and consecutive")
            self.forecast = ordered
        encoded = json.dumps(
            self.model_dump(exclude_none=True), ensure_ascii=False, separators=(",", ":")
        ).encode("utf-8")
        if len(encoded) > 4096:
            raise ValueError("Weather snapshot exceeds 4096 bytes")
        return self
