def normalize_current(raw: dict, lat: float, lon: float) -> dict:
    current = raw.get("current", {})
    return {
        "location": {"lat": lat, "lon": lon},
        "temperature": current.get("temperature_2m"),
        "humidity": current.get("relative_humidity_2m"),
        "feels_like": current.get("apparent_temperature"),
        "wind_speed": current.get("wind_speed_10m"),
        "provider": "open_meteo",
    }


def normalize_hourly(raw: dict, lat: float, lon: float, hours: int) -> dict:
    hourly = raw.get("hourly", {})
    times = hourly.get("time", [])[:hours]
    temps = hourly.get("temperature_2m", [])[:hours]
    precip = hourly.get("precipitation_probability", [])[:hours]
    codes = hourly.get("weather_code", [])[:hours]

    items = []
    for idx, timestamp in enumerate(times):
        items.append(
            {
                "time": timestamp,
                "temperature": temps[idx] if idx < len(temps) else None,
                "precipitation_probability": precip[idx] if idx < len(precip) else None,
                "weather_code": codes[idx] if idx < len(codes) else None,
            }
        )

    return {"location": {"lat": lat, "lon": lon}, "hours": hours, "items": items, "provider": "open_meteo"}


def normalize_daily(raw: dict, lat: float, lon: float, days: int) -> dict:
    daily = raw.get("daily", {})
    dates = daily.get("time", [])[:days]
    temp_max = daily.get("temperature_2m_max", [])[:days]
    temp_min = daily.get("temperature_2m_min", [])[:days]
    precip = daily.get("precipitation_probability_max", [])[:days]
    codes = daily.get("weather_code", [])[:days]

    items = []
    for idx, day in enumerate(dates):
        items.append(
            {
                "date": day,
                "temp_max": temp_max[idx] if idx < len(temp_max) else None,
                "temp_min": temp_min[idx] if idx < len(temp_min) else None,
                "precipitation_probability": precip[idx] if idx < len(precip) else None,
                "weather_code": codes[idx] if idx < len(codes) else None,
            }
        )

    return {"location": {"lat": lat, "lon": lon}, "days": days, "items": items, "provider": "open_meteo"}
