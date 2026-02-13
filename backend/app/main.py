from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.router import api_router
from app.core.config import get_settings
from app.core.exceptions import register_exception_handlers
from app.core.logging import setup_logging
from app.core.observability import init_sentry
from app.middleware.metrics import MetricsMiddleware
from app.middleware.rate_limit import InMemoryRateLimitMiddleware
from app.middleware.request_id import RequestIDMiddleware

settings = get_settings()
setup_logging()
init_sentry()

app = FastAPI(title=settings.app_name, debug=settings.debug)

app.add_middleware(RequestIDMiddleware)
app.add_middleware(InMemoryRateLimitMiddleware)
app.add_middleware(MetricsMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

register_exception_handlers(app)


@app.get("/health", tags=["health"])
def healthcheck() -> dict[str, str]:
    return {"status": "ok"}


app.include_router(api_router, prefix=settings.api_v1_prefix)
