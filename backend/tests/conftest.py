import os
import sqlite3
from collections.abc import Generator

import httpx
import pytest
import requests
from app import models  # noqa: F401 -- register all SQLAlchemy model metadata
from app.db.base import Base
from app.db.session import get_db
from app.main import app
from app.middleware.rate_limit import InMemoryRateLimitMiddleware
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

# Windows uyumlu geçici DB yolu
TEST_DB_FILE = "test_db.sqlite"
TEST_DATABASE_URL = f"sqlite:///{TEST_DB_FILE}"


@pytest.fixture(autouse=True)
def prohibit_real_http(monkeypatch):
    def blocked(*args, **kwargs):
        pytest.fail("Real HTTP is forbidden in local tests; provide a test transport or stub")

    # TestClient and httpx.MockTransport remain available; real network adapters do not.
    monkeypatch.setattr(httpx.HTTPTransport, "handle_request", blocked)
    monkeypatch.setattr(httpx.AsyncHTTPTransport, "handle_async_request", blocked)
    monkeypatch.setattr(requests.adapters.HTTPAdapter, "send", blocked)

@event.listens_for(Engine, "connect")
def set_sqlite_pragma(dbapi_connection, connection_record):
    if not isinstance(dbapi_connection, sqlite3.Connection):
        return
    cursor = dbapi_connection.cursor()
    cursor.execute("PRAGMA foreign_keys=ON")
    cursor.close()

@pytest.fixture(scope="function")
def db_session() -> Generator[Session, None, None]:
    engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
    TestingSessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)

    Base.metadata.drop_all(bind=engine)
    Base.metadata.create_all(bind=engine)

    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()
        engine.dispose()
        if os.path.exists(TEST_DB_FILE):
            try:
                os.remove(TEST_DB_FILE)
            except PermissionError:
                pass

@pytest.fixture(scope="function")
def client(db_session: Session) -> Generator[TestClient, None, None]:
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        # Tests share one ASGI app, but must not inherit another test's IP quota.
        # Keep the limiter active inside each test; its own behavior is tested separately.
        middleware = app.middleware_stack
        while middleware is not None:
            if isinstance(middleware, InMemoryRateLimitMiddleware):
                middleware.hits.clear()
                middleware._request_counter = 0
            middleware = getattr(middleware, "app", None)
        yield test_client
    app.dependency_overrides.clear()


@pytest.fixture
def assistant_texts(monkeypatch):
    from app.services import assistant_consent
    settings = assistant_consent.settings
    monkeypatch.setattr(settings, "assistant_consent_version", 2)
    monkeypatch.setattr(settings, "assistant_consent_text", "Local test assistant consent; not legal production text.")
    monkeypatch.setattr(settings, "assistant_health_consent_version", 2)
    monkeypatch.setattr(settings, "assistant_health_consent_text", "Local test optional health consent; not production text.")
    return settings
