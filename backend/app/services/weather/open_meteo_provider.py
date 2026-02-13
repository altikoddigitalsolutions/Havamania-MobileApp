from typing import Any

import httpx

from app.services.weather.provider import WeatherProvider


class OpenMeteoProvider(WeatherProvider):
    name = "open_meteo"

    def __init__(self, base_url: str = "https://api.open-meteo.com/v1/forecast", timeout_seconds: int = 8):
        self.base_url = base_url
        self.timeout = httpx.Timeout(timeout_seconds)

    def _request(self, params: dict[str, Any]) -> dict:
        for attempt in range(2):
            try:
                with httpx.Client(timeout=self.timeout) as client:
                    response = client.get(self.base_url, params=params)
                    response.raise_for_status()
                    return response.json()
            except Exception:
                if attempt == 1:
                    raise
        raise RuntimeError("Unreachable retry state")

    def get_current(self, lat: float, lon: float) -> dict:
        return self._request(
            {
                "latitude": lat,
                "longitude": lon,
                "current": "temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m",
                "timezone": "auto",
            }
        )

    def get_hourly(self, lat: float, lon: float, hours: int) -> dict:
        return self._request(
            {
                "latitude": lat,
                "longitude": lon,
                "hourly": "temperature_2m,precipitation_probability,weather_code",
                "forecast_hours": hours,
                "timezone": "auto",
            }
        )

    def get_daily(self, lat: float, lon: float, days: int) -> dict:
        return self._request(
            {
                "latitude": lat,
                "longitude": lon,
                "daily": "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
                "forecast_days": days,
                "timezone": "auto",
            }
        )
