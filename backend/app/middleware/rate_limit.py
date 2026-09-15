import time
from collections import defaultdict, deque

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import JSONResponse


class InMemoryRateLimitMiddleware(BaseHTTPMiddleware):
    """Memory-bounded process-local rate limiter middleware."""

    def __init__(self, app, max_requests: int = 120, window_seconds: int = 60, max_tracked_keys: int = 10000):
        super().__init__(app)
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self.max_tracked_keys = max_tracked_keys
        self.hits = defaultdict(deque)
        self._request_counter = 0

    async def dispatch(self, request: Request, call_next):
        key = request.client.host if request.client else "unknown"
        now = time.time()

        # Amortized periodic cleanup of empty deques and expired entries every 200 requests
        self._request_counter += 1
        if self._request_counter >= 200:
            self._request_counter = 0
            self._cleanup(now)

        # Check capacity before defaultdict inserts the new client key.
        if key not in self.hits and len(self.hits) >= self.max_tracked_keys:
            self._cleanup(now)
            if len(self.hits) >= self.max_tracked_keys:
                self.hits.pop(next(iter(self.hits)), None)

        window = self.hits[key]

        while window and now - window[0] > self.window_seconds:
            window.popleft()

        if len(window) >= self.max_requests:
            return JSONResponse(
                status_code=429,
                content={"detail": "Rate limit exceeded"},
            )

        window.append(now)
        return await call_next(request)

    def _cleanup(self, now: float):
        keys_to_delete = []
        for k, window in self.hits.items():
            while window and now - window[0] > self.window_seconds:
                window.popleft()
            if not window:
                keys_to_delete.append(k)
        for k in keys_to_delete:
            self.hits.pop(k, None)
