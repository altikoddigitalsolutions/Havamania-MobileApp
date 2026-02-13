from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

from app.core.observability import RequestTimer, observe_request


class MetricsMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        with RequestTimer() as timer:
            response = await call_next(request)

        observe_request(
            path=request.url.path,
            method=request.method,
            status=response.status_code,
            elapsed_seconds=timer.elapsed,
        )
        return response
