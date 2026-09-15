from fastapi import APIRouter, Header, HTTPException, Response, status

from app.core.config import get_settings
from app.core.observability import metrics_payload

router = APIRouter()
settings = get_settings()


@router.get("", include_in_schema=False)
def metrics(authorization: str | None = Header(default=None)) -> Response:
    expected_secret = settings.metrics_secret
    token = None
    if authorization:
        parts = authorization.split(" ")
        if len(parts) == 2 and parts[0].lower() in ("bearer", "token"):
            token = parts[1]
        else:
            token = authorization

    if not expected_secret or expected_secret == "change-me":
        if settings.app_env == "production":
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Metrics endpoint disabled or requires METRICS_SECRET in production",
            )
    else:
        if token != expected_secret:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or missing metrics secret",
            )

    payload, content_type = metrics_payload()
    return Response(content=payload, media_type=content_type)
