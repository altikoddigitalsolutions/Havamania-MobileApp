import time
from collections import OrderedDict
from threading import RLock
from typing import Any


class InMemoryTTLCache:
    def __init__(self, max_entries: int = 1024):
        if max_entries < 1:
            raise ValueError("max_entries must be positive")
        self._values: OrderedDict[str, tuple[float, Any]] = OrderedDict()
        self._max_entries = max_entries
        self._lock = RLock()

    def get(self, key: str):
        with self._lock:
            entry = self._values.get(key)
            if not entry:
                return None
            expires_at, value = entry
            if time.time() >= expires_at:
                self._values.pop(key, None)
                return None
            self._values.move_to_end(key)
            return value

    def set(self, key: str, value: Any, ttl_seconds: int) -> None:
        with self._lock:
            self._values[key] = (time.time() + ttl_seconds, value)
            self._values.move_to_end(key)
            while len(self._values) > self._max_entries:
                self._values.popitem(last=False)
