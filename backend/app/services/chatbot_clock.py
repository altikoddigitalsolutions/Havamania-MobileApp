from datetime import UTC, datetime, time, timedelta
from zoneinfo import ZoneInfo

CHATBOT_ZONE = ZoneInfo("Europe/Istanbul")


def chatbot_day(now=None):
    return (now or datetime.now(UTC)).astimezone(CHATBOT_ZONE).date()


def chatbot_reset(day):
    return datetime.combine(day + timedelta(days=1), time.min, tzinfo=CHATBOT_ZONE)
