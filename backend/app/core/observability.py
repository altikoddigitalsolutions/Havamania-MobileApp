import time

import sentry_sdk
from prometheus_client import CONTENT_TYPE_LATEST, Counter, Histogram, generate_latest
from sentry_sdk.integrations.fastapi import FastApiIntegration

from app.core.config import get_settings

REQUEST_LATENCY = Histogram('request_latency_seconds', 'HTTP request latency', ['path', 'method'])
REQUEST_COUNT = Counter('request_count_total', 'HTTP request count', ['path', 'method', 'status'])
WEATHER_PROVIDER_ERRORS = Counter('weather_provider_errors_total', 'Weather provider errors')
CHATBOT_TIMEOUTS = Counter('chatbot_timeouts_total', 'Chatbot timeout count')


def init_sentry() -> None:
    settings = get_settings()
    if settings.sentry_dsn_backend:
        sentry_sdk.init(dsn=settings.sentry_dsn_backend, integrations=[FastApiIntegration()])


def observe_request(path: str, method: str, status: int, elapsed_seconds: float) -> None:
    REQUEST_LATENCY.labels(path=path, method=method).observe(elapsed_seconds)
    REQUEST_COUNT.labels(path=path, method=method, status=str(status)).inc()


def mark_weather_provider_error() -> None:
    WEATHER_PROVIDER_ERRORS.inc()


def mark_chatbot_timeout() -> None:
    CHATBOT_TIMEOUTS.inc()


def metrics_payload() -> tuple[bytes, str]:
    return generate_latest(), CONTENT_TYPE_LATEST


class RequestTimer:
    def __enter__(self):
        self.start = time.perf_counter()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.elapsed = time.perf_counter() - self.start
