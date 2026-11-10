from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request

from app.core.observability import RequestTimer, observe_request


class MetricsMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        try:
            with RequestTimer() as timer:
                response = await call_next(request)
        except Exception:
            self.record(request, 500, timer.elapsed)
            raise
        self.record(request, response.status_code, timer.elapsed)
        return response

    @staticmethod
    def record(request: Request, status: int, elapsed: float):
        route = request.scope.get("route")
        path_label = getattr(route, "path", None) or getattr(route, "path_format", None)
        if not path_label:
            path_label = "unmatched" if status == 404 else "other"

        method = request.method
        if method not in {
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "HEAD",
            "OPTIONS",
            "TRACE",
            "CONNECT",
        }:
            method = "OTHER"

        observe_request(
            path=path_label,
            method=method,
            status=status,
            elapsed_seconds=elapsed,
        )
