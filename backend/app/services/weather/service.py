from dataclasses import dataclass

from app.core.config import get_settings
from app.core.observability import mark_weather_provider_error
from app.services.weather.cache import InMemoryTTLCache
from app.services.weather.mapper import normalize_current, normalize_daily, normalize_hourly
from app.services.weather.open_meteo_provider import OpenMeteoProvider
from app.services.weather.provider import WeatherProvider


@dataclass
class WeatherService:
    primary_provider: WeatherProvider
    fallback_provider: WeatherProvider | None = None
    cache: InMemoryTTLCache | None = None

    def _with_fallback(self, func_name: str, *args, **kwargs) -> dict:
        try:
            return getattr(self.primary_provider, func_name)(*args, **kwargs)
        except Exception:
            mark_weather_provider_error()
            if self.fallback_provider is None:
                raise
            return getattr(self.fallback_provider, func_name)(*args, **kwargs)

    def get_current(self, lat: float, lon: float) -> dict:
        cache_key = f"weather:current:{lat}:{lon}"
        if self.cache:
            cached = self.cache.get(cache_key)
            if cached:
                return cached

        raw = self._with_fallback("get_current", lat, lon)
        normalized = normalize_current(raw, lat, lon)
        if self.cache:
            self.cache.set(cache_key, normalized, ttl_seconds=180)
        return normalized

    def get_hourly(self, lat: float, lon: float, hours: int) -> dict:
        cache_key = f"weather:hourly:{lat}:{lon}:{hours}"
        if self.cache:
            cached = self.cache.get(cache_key)
            if cached:
                return cached

        raw = self._with_fallback("get_hourly", lat, lon, hours)
        normalized = normalize_hourly(raw, lat, lon, hours)
        if self.cache:
            self.cache.set(cache_key, normalized, ttl_seconds=600)
        return normalized

    def get_daily(self, lat: float, lon: float, days: int) -> dict:
        cache_key = f"weather:daily:{lat}:{lon}:{days}"
        if self.cache:
            cached = self.cache.get(cache_key)
            if cached:
                return cached

        raw = self._with_fallback("get_daily", lat, lon, days)
        normalized = normalize_daily(raw, lat, lon, days)
        if self.cache:
            self.cache.set(cache_key, normalized, ttl_seconds=1800)
        return normalized


class WeatherProviderFactory:
    def __init__(self):
        self.settings = get_settings()

    def create(self) -> WeatherService:
        primary = OpenMeteoProvider()
        fallback = OpenMeteoProvider()

        # Feature flag scaffold for future paid providers.
        provider_flag = getattr(self.settings, "weather_provider", "open_meteo")
        if provider_flag != "open_meteo":
            primary = OpenMeteoProvider()

        return WeatherService(
            primary_provider=primary,
            fallback_provider=fallback,
            cache=InMemoryTTLCache(),
        )


weather_service = WeatherProviderFactory().create()
