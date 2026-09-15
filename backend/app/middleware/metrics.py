from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

from app.core.observability import RequestTimer, observe_request


class MetricsMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        with RequestTimer() as timer:
            response = await call_next(request)

        route = request.scope.get("route")
        path_label = getattr(route, "path", None) or getattr(route, "path_format", None)
        if not path_label:
            path_label = "unmatched" if response.status_code == 404 else "other"

        observe_request(
            path=path_label,
            method=request.method,
            status=response.status_code,
            elapsed_seconds=timer.elapsed,
        )
        return response
