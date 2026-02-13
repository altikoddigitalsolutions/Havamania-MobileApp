from app.services.weather.cache import InMemoryTTLCache
from app.services.weather.provider import WeatherProvider
from app.services.weather.service import WeatherService


class PrimaryFailProvider(WeatherProvider):
    name = "primary_fail"

    def get_current(self, lat: float, lon: float) -> dict:
        raise RuntimeError("primary failed")

    def get_hourly(self, lat: float, lon: float, hours: int) -> dict:
        raise RuntimeError("primary failed")

    def get_daily(self, lat: float, lon: float, days: int) -> dict:
        raise RuntimeError("primary failed")


class FallbackProvider(WeatherProvider):
    name = "fallback"

    def get_current(self, lat: float, lon: float) -> dict:
        return {"current": {"temperature_2m": 21, "relative_humidity_2m": 40, "apparent_temperature": 20, "wind_speed_10m": 5}}

    def get_hourly(self, lat: float, lon: float, hours: int) -> dict:
        return {
            "hourly": {
                "time": ["2026-02-13T10:00"] * hours,
                "temperature_2m": [20] * hours,
                "precipitation_probability": [10] * hours,
                "weather_code": [1] * hours,
            }
        }

    def get_daily(self, lat: float, lon: float, days: int) -> dict:
        return {
            "daily": {
                "time": ["2026-02-13"] * days,
                "temperature_2m_max": [25] * days,
                "temperature_2m_min": [15] * days,
                "precipitation_probability_max": [20] * days,
                "weather_code": [2] * days,
            }
        }


def test_fallback_provider_is_used_for_current():
    service = WeatherService(primary_provider=PrimaryFailProvider(), fallback_provider=FallbackProvider())

    result = service.get_current(41.0, 29.0)

    assert result["temperature"] == 21


def test_cache_returns_same_payload_without_new_provider_call():
    provider = FallbackProvider()
    service = WeatherService(primary_provider=provider, fallback_provider=None, cache=InMemoryTTLCache())

    first = service.get_current(41.0, 29.0)
    second = service.get_current(41.0, 29.0)

    assert first == second
