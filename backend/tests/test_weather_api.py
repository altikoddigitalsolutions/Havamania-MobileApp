from app.api.v1.routes import weather as weather_route


class StubWeatherService:
    def get_current(self, lat: float, lon: float):
        return {
            "location": {"lat": lat, "lon": lon},
            "temperature": 22,
            "humidity": 50,
            "feels_like": 21,
            "wind_speed": 7,
            "provider": "stub",
        }

    def get_hourly(self, lat: float, lon: float, hours: int):
        return {
            "location": {"lat": lat, "lon": lon},
            "hours": hours,
            "items": [
                {
                    "time": "2026-02-13T10:00",
                    "temperature": 22,
                    "precipitation_probability": 20,
                    "weather_code": 1,
                }
            ],
            "provider": "stub",
        }

    def get_daily(self, lat: float, lon: float, days: int):
        return {
            "location": {"lat": lat, "lon": lon},
            "days": days,
            "items": [
                {
                    "date": "2026-02-13",
                    "temp_max": 27,
                    "temp_min": 18,
                    "precipitation_probability": 15,
                    "weather_code": 2,
                }
            ],
            "provider": "stub",
        }


def test_current_weather_endpoint(client, monkeypatch):
    monkeypatch.setattr(weather_route, "weather_service", StubWeatherService())

    response = client.get("/v1/weather/current?lat=41&lon=29")

    assert response.status_code == 200
    assert response.json()["provider"] == "stub"


def test_hourly_weather_endpoint(client, monkeypatch):
    monkeypatch.setattr(weather_route, "weather_service", StubWeatherService())

    response = client.get("/v1/weather/hourly?lat=41&lon=29&hours=24")

    assert response.status_code == 200
    assert response.json()["hours"] == 24


def test_daily_weather_endpoint(client, monkeypatch):
    monkeypatch.setattr(weather_route, "weather_service", StubWeatherService())

    response = client.get("/v1/weather/daily?lat=41&lon=29&days=7")

    assert response.status_code == 200
    assert response.json()["days"] == 7
