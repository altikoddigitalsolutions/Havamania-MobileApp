from uuid import UUID, uuid4

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request


class RequestIDMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        incoming = request.headers.getlist("x-request-id")
        request_id = str(uuid4())
        if len(incoming) == 1 and len(incoming[0]) == 36:
            try:
                candidate = str(UUID(incoming[0]))
                if candidate == incoming[0].lower():
                    request_id = candidate
            except ValueError:
                pass
        request.state.request_id = request_id

        response = await call_next(request)
        response.headers["x-request-id"] = request_id
        return response
