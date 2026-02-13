import time
from collections import defaultdict, deque

from fastapi import HTTPException, Request, status

_hits: dict[str, deque] = defaultdict(deque)


def weather_rate_limit(request: Request, max_requests: int = 60, window_seconds: int = 60) -> None:
    key = f"weather:{request.client.host if request.client else 'unknown'}"
    now = time.time()
    bucket = _hits[key]

    while bucket and now - bucket[0] > window_seconds:
        bucket.popleft()

    if len(bucket) >= max_requests:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Too many weather requests",
        )

    bucket.append(now)
