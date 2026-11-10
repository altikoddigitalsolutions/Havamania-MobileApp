"""Server-only platform text retrieval; never fall back to stale or configured text."""

from threading import Lock
from time import monotonic

import httpx
from fastapi import HTTPException
from pydantic import BaseModel, Field, StrictInt, ValidationError

from app.core.config import get_settings


class ConsentText(BaseModel):
    version: StrictInt = Field(ge=1)
    text: str = Field(min_length=1, max_length=50000)


class ConsentTexts(ConsentText):
    health: ConsentText | None = None


class PlatformConsentProvider:
    def __init__(self):
        self._lock = Lock()
        self._cached = None
        self._expires = 0.0
        self._identity = None

    def invalidate(self):
        with self._lock:
            self._cached = None
            self._expires = 0.0

    def load(self, force=False):
        settings = get_settings()
        key = settings.chatbot_api_key.get_secret_value() if settings.chatbot_api_key else ""
        identity = (settings.chatbot_base_url, key)
        with self._lock:
            if (
                not force
                and self._identity == identity
                and self._cached
                and monotonic() < self._expires
            ):
                return self._cached
            self._cached = None
            if not key:
                raise HTTPException(503, "Assistant consent service is not configured")
            try:
                with (
                    httpx.Client(
                        timeout=httpx.Timeout(5, connect=3), follow_redirects=False
                    ) as client,
                    client.stream(
                        "GET",
                        f"{settings.chatbot_base_url.rstrip('/')}/consent",
                        headers={"Authorization": f"Bearer {key}"},
                    ) as response,
                ):
                    if response.status_code != 200:
                        raise ValueError("Consent service unavailable")
                    content = bytearray()
                    for chunk in response.iter_bytes():
                        content.extend(chunk)
                        if len(content) > 262144:
                            raise ValueError("Consent response too large")
                    texts = ConsentTexts.model_validate_json(content)
                    if not texts.text.strip() or (texts.health and not texts.health.text.strip()):
                        raise ValueError("Empty consent text")
            except (httpx.RequestError, ValueError, ValidationError) as exc:
                raise HTTPException(
                    503, "Assistant consent texts are temporarily unavailable"
                ) from exc
            self._identity, self._cached, self._expires = identity, texts, monotonic() + 15
            return texts


platform_consent_provider = PlatformConsentProvider()
