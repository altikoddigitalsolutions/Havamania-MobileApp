import time
from typing import Any


class InMemoryTTLCache:
    def __init__(self):
        self._values: dict[str, tuple[float, Any]] = {}

    def get(self, key: str):
        entry = self._values.get(key)
        if not entry:
            return None
        expires_at, value = entry
        if time.time() > expires_at:
            self._values.pop(key, None)
            return None
        return value

    def set(self, key: str, value: Any, ttl_seconds: int) -> None:
        self._values[key] = (time.time() + ttl_seconds, value)
