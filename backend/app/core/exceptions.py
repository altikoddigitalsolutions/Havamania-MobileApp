import logging
import traceback
from pathlib import PurePath

from fastapi import FastAPI, Request
from fastapi.exception_handlers import request_validation_exception_handler
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.core.config import get_settings

logger = logging.getLogger(__name__)


def register_exception_handlers(app: FastAPI) -> None:
    assistant_path = get_settings().api_v1_prefix.rstrip("/") + "/chatbot"

    @app.exception_handler(RequestValidationError)
    async def assistant_validation_handler(request: Request, exc: RequestValidationError):
        path = request.url.path
        if path != assistant_path and not path.startswith(assistant_path + "/"):
            return await request_validation_exception_handler(request, exc)
        # Pydantic inputs may contain private context or non-JSON floats. Never
        # serialize them (nor exception context) into assistant error responses.
        return JSONResponse(
            status_code=422,
            content={
                "detail": [
                    {
                        "loc": error.get("loc", ()),
                        "type": error.get("type", "value_error"),
                        "msg": "Geçersiz istek alanı.",
                    }
                    for error in exc.errors()
                ]
            },
            headers={"Cache-Control": "no-store"},
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        request_id = getattr(request.state, "request_id", None)
        route = request.scope.get("route")
        route_label = getattr(route, "path", None) or "unmatched"
        frames = [
            (PurePath(frame.filename).name, frame.lineno, frame.name)
            for frame in traceback.extract_tb(exc.__traceback__)
        ]
        # SQL/transport exceptions can embed tokens or personal data in their
        # messages and traceback chains. Keep code locations without those values.
        logger.error(
            "Unhandled %s request_id=%s route=%s frames=%s",
            type(exc).__name__,
            request_id,
            route_label,
            frames,
        )
        return JSONResponse(
            status_code=500,
            content={
                "detail": "Internal server error",
                "request_id": request_id,
            },
            headers={
                "Cache-Control": "no-store",
                **({"X-Request-ID": request_id} if request_id else {}),
            },
        )
