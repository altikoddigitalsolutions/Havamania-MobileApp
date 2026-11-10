import json
from datetime import date, timedelta

import pytest
from app.schemas.assistant_weather import AssistantWeatherData
from pydantic import ValidationError


def compact_size(body):
    return len(json.dumps(body, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))


def exact_size_weather(size):
    body = {
        "observed_at": "2026-10-30T14:30+03:00",
        "timezone": "Europe/Istanbul",
        "source": "",
        "forecast": [
            {"date": str(date(2026, 10, 30) + timedelta(days=i)), "condition": "x" * 100}
            for i in range(10)
        ],
    }
    remaining = size - compact_size(body)
    assert 0 <= remaining <= 3000
    for day in body["forecast"]:
        chars = list(day["condition"])
        for index in range(100):
            if remaining >= 3:
                chars[index] = "🌦"
                remaining -= 3
            elif remaining:
                chars[index] = "ğ"
                remaining -= 1
        day["condition"] = "".join(chars)
    assert remaining == 0 and compact_size(body) == size
    return body


@pytest.mark.parametrize("size", [4095, 4096])
def test_weather_accepts_valid_utf8_payload_at_byte_boundary(size):
    body = exact_size_weather(size)
    clean = AssistantWeatherData.model_validate(body).model_dump(exclude_none=True)
    assert compact_size(clean) == size
    assert len(json.dumps(clean, ensure_ascii=False).encode("utf-8")) > 4096


def test_weather_rejects_one_byte_over_limit():
    with pytest.raises(ValidationError, match="4096 bytes"):
        AssistantWeatherData.model_validate(exact_size_weather(4097))


def test_unknown_and_null_fields_do_not_consume_weather_budget():
    body = exact_size_weather(4096)
    body.update(current=None, location=None, stale=None, ignored="x" * 5000)
    clean = AssistantWeatherData.model_validate(body).model_dump(exclude_none=True)
    assert compact_size(clean) == 4096
    assert "ignored" not in clean


@pytest.mark.parametrize("offset", ["+03:60", "-03:99", "+00:99", "+24:00", "-24:00"])
def test_timestamp_rejects_offset_normalization(offset):
    with pytest.raises(ValidationError, match="offset is invalid"):
        AssistantWeatherData.model_validate(
            {"observed_at": "2026-10-30T14:30" + offset, "timezone": "Europe/Istanbul"}
        )


@pytest.mark.parametrize("offset", ["Z", "+03:00", "-03:30", "+05:45", "+00:00"])
def test_timestamp_preserves_valid_offset(offset):
    timestamp = "2026-10-30T14:30:00.123456789" + offset
    data = AssistantWeatherData.model_validate(
        {"observed_at": timestamp, "timezone": "Europe/Istanbul"}
    )
    assert data.observed_at == timestamp
