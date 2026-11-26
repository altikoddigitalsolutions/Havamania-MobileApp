import time
from collections import deque
from math import ceil
from threading import Lock

from fastapi import HTTPException, Request, status

# Server policy, deliberately absent from the dependency's HTTP signature.
_MAX_REQUESTS = 60
_WINDOW_SECONDS = 60
_MAX_TRACKED_KEYS = 10000
_hits: dict[str, deque[float]] = {}
_lock = Lock()


def _reject(retry_after: float) -> None:
    raise HTTPException(
        status_code=status.HTTP_429_TOO_MANY_REQUESTS,
        detail="Too many weather requests",
        headers={"Retry-After": str(max(1, ceil(retry_after)))},
    )


def weather_rate_limit(request: Request) -> None:
    """Bounded process-local sliding window; HTTP clients cannot change policy."""
    key = f"weather:{request.client.host if request.client else 'unknown'}"
    # Sync dependencies run in worker threads: expiry/check/append must be atomic.
    with _lock:
        now = time.monotonic()
        if key not in _hits:
            if len(_hits) >= _MAX_TRACKED_KEYS:
                for stale in [k for k, window in _hits.items()
                              if not window or now - window[-1] >= _WINDOW_SECONDS]:
                    del _hits[stale]
                # Never evict a live owner's quota to admit another client.
                if len(_hits) >= _MAX_TRACKED_KEYS:
                    _reject(_WINDOW_SECONDS)
            _hits[key] = deque()
        bucket = _hits[key]
        while bucket and now - bucket[0] >= _WINDOW_SECONDS:
            bucket.popleft()
        if len(bucket) >= _MAX_REQUESTS:
            _reject(_WINDOW_SECONDS - (now - bucket[0]))
        bucket.append(now)
