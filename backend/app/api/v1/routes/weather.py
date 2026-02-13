from fastapi import APIRouter, Depends, Query

from app.dependencies.premium import require_premium_user
from app.dependencies.rate_limit import weather_rate_limit
from app.schemas.weather import WeatherCurrentResponse, WeatherDailyResponse, WeatherHourlyResponse
from app.services.weather.service import weather_service

router = APIRouter(dependencies=[Depends(weather_rate_limit)])


@router.get("/current", response_model=WeatherCurrentResponse)
def get_current_weather(
    lat: float = Query(..., ge=-90, le=90),
    lon: float = Query(..., ge=-180, le=180),
) -> dict:
    return weather_service.get_current(lat=lat, lon=lon)


@router.get("/hourly", response_model=WeatherHourlyResponse)
def get_hourly_weather(
    lat: float = Query(..., ge=-90, le=90),
    lon: float = Query(..., ge=-180, le=180),
    hours: int = Query(default=24, ge=1, le=72),
) -> dict:
    return weather_service.get_hourly(lat=lat, lon=lon, hours=hours)


@router.get("/daily", response_model=WeatherDailyResponse)
def get_daily_weather(
    lat: float = Query(..., ge=-90, le=90),
    lon: float = Query(..., ge=-180, le=180),
    days: int = Query(default=7, ge=1, le=14),
) -> dict:
    return weather_service.get_daily(lat=lat, lon=lon, days=days)


@router.get("/map-layers", dependencies=[Depends(require_premium_user)])
def get_premium_map_layer(
    lat: float = Query(..., ge=-90, le=90),
    lon: float = Query(..., ge=-180, le=180),
    layer: str = Query(default="temperature"),
) -> dict:
    base = weather_service.get_current(lat=lat, lon=lon)
    return {
        "layer": layer,
        "location": base["location"],
        "provider": base["provider"],
        "premium": True,
        "data": {"summary": f"{layer} layer data ready"},
    }
