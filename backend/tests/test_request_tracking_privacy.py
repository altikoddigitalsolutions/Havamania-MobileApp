from uuid import UUID, uuid4

import pytest
from app.core.exceptions import register_exception_handlers
from app.middleware.request_id import RequestIDMiddleware
from fastapi import FastAPI, Request
from fastapi.testclient import TestClient


@pytest.fixture
def tracked_app():
    app = FastAPI()
    app.add_middleware(RequestIDMiddleware)
    register_exception_handlers(app)

    @app.get("/tracking")
    def tracking(request: Request):
        return {"request_id": request.state.request_id}

    @app.get("/failure/{item_id}")
    def failure(item_id: str):
        raise RuntimeError("PRIVATE_EXCEPTION_DETAIL")

    with TestClient(app, raise_server_exceptions=False) as client:
        yield client


@pytest.mark.parametrize(
    "value", ["", "PRIVATE_HEADER", "x" * 4096, "a\r\nb", "{" + str(uuid4()) + "}"]
)
def test_untrusted_tracking_header_is_replaced(tracked_app, value):
    response = tracked_app.get("/tracking", headers={"X-Request-ID": value})
    assert response.status_code == 200
    result = response.headers["x-request-id"]
    assert str(UUID(result)) == result
    assert result == response.json()["request_id"]
    assert result != value


def test_valid_uuid_is_preserved_and_duplicate_headers_are_replaced(tracked_app):
    value = str(uuid4())
    response = tracked_app.get("/tracking", headers={"X-Request-ID": value.upper()})
    assert response.headers["x-request-id"] == value
    duplicated = tracked_app.get(
        "/tracking", headers=[("X-Request-ID", value), ("X-Request-ID", value)]
    )
    assert duplicated.headers["x-request-id"] != value


def test_500_log_keeps_tracking_and_code_location_without_private_values(tracked_app, caplog):
    value = str(uuid4())
    response = tracked_app.get("/failure/PRIVATE_ITEM", headers={"X-Request-ID": value})
    assert response.status_code == 500
    assert response.json()["request_id"] == response.headers["x-request-id"] == value
    assert response.headers["cache-control"] == "no-store"
    records = [record for record in caplog.records if record.name == "app.core.exceptions"]
    assert len(records) == 1
    message = records[0].getMessage()
    assert "PRIVATE_" not in message
    assert value in message and "/failure/{item_id}" in message
    assert "RuntimeError" in message and "test_request_tracking_privacy.py" in message
    assert records[0].exc_info is None
