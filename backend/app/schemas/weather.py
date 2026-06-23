from pydantic import BaseModel


class WeatherCurrentResponse(BaseModel):
    location: dict
    temperature: float | None
    humidity: float | None
    feels_like: float | None
    wind_speed: float | None
    wind_direction: float | None
    uv_index: float | None
    pressure: float | None
    visibility: float | None
    cloud_cover: float | None
    sunrise: str | None = None
    sunset: str | None = None
    provider: str


class WeatherHourlyItem(BaseModel):
    time: str
    temperature: float | None
    precipitation_probability: float | None
    weather_code: int | None


class WeatherHourlyResponse(BaseModel):
    location: dict
    hours: int
    items: list[WeatherHourlyItem]
    provider: str


class WeatherDailyItem(BaseModel):
    date: str
    temp_max: float | None
    temp_min: float | None
    precipitation_probability: float | None
    weather_code: int | None


class WeatherDailyResponse(BaseModel):
    location: dict
    days: int
    items: list[WeatherDailyItem]
    provider: str
