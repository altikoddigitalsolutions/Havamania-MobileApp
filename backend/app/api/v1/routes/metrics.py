from fastapi import APIRouter, Response

from app.core.observability import metrics_payload

router = APIRouter()


@router.get('', include_in_schema=False)
def metrics() -> Response:
    payload, content_type = metrics_payload()
    return Response(content=payload, media_type=content_type)
