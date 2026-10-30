"""Opt-in integration checks against an isolated LOCAL PostgreSQL server.
Each test owns a random schema. No production URL or existing schema is accepted.
"""
import os
import subprocess
import sys
from pathlib import Path
from uuid import uuid4

import pytest
from app.db.base import Base
from sqlalchemy import create_engine, inspect, text
from sqlalchemy.engine import make_url
from sqlalchemy.exc import IntegrityError

URL = os.environ.get("PG_MIGRATION_TEST_URL")
pytestmark = pytest.mark.skipif(not URL, reason="PG_MIGRATION_TEST_URL required for local PostgreSQL")


@pytest.fixture
def pg_schema():
    url = make_url(URL)
    assert url.get_backend_name() == "postgresql"
    assert url.host in {"127.0.0.1", "localhost", "::1"}
    assert url.database == "havamania_migration_audit", "Dedicated audit database required"
    schema = "audit_" + uuid4().hex
    admin = create_engine(url)
    with admin.begin() as connection:
        connection.execute(text(f'CREATE SCHEMA "{schema}"'))
    engine = create_engine(url, connect_args={"options": f"-csearch_path={schema}"})
    env = dict(os.environ, APP_ENV="test", DATABASE_URL=URL, PGOPTIONS=f"-csearch_path={schema}")

    def migrate(revision="head", expect_success=True):
        result = subprocess.run([sys.executable, "-m", "alembic", "upgrade", revision],
                                cwd=Path(__file__).resolve().parents[1], env=env,
                                capture_output=True, text=True, timeout=90, check=False)
        if expect_success:
            assert result.returncode == 0, result.stdout + result.stderr
        else:
            assert result.returncode != 0
        return result

    try:
        yield engine, migrate
    finally:
        engine.dispose()
        # Only this fixture's generated schema can be removed.
        assert schema.startswith("audit_") and len(schema) == 38
        with admin.begin() as connection:
            connection.execute(text(f'DROP SCHEMA "{schema}" CASCADE'))
        admin.dispose()


def seed(engine):
    with engine.begin() as c:
        c.execute(text("INSERT INTO users (id,email,password_hash) VALUES ('u','test@example.invalid','test')"))
        c.execute(text("INSERT INTO profiles (user_id) VALUES ('u')"))
        c.execute(text("INSERT INTO push_tokens (id,user_id,platform,token) VALUES ('p','u','android','token-1')"))


def test_empty_postgres_matches_models_and_repeated_upgrade(pg_schema):
    engine, migrate = pg_schema
    migrate()
    migrate()
    inspector = inspect(engine)
    for name, table in Base.metadata.tables.items():
        columns = {c["name"] for c in inspector.get_columns(name)}
        assert set(table.columns.keys()) <= columns, name
    with engine.connect() as c:
        assert c.scalar(text("SELECT version_num FROM alembic_version")) == "0015_assistant_consent"


def test_upgrade_preserves_data_removes_old_unique_constraint_and_cascades(pg_schema):
    engine, migrate = pg_schema
    migrate("0010")
    seed(engine)
    with engine.begin() as c:
        c.execute(text("ALTER TABLE push_tokens ADD CONSTRAINT old_device_unique UNIQUE(user_id,platform)"))
    migrate()
    migrate()
    with engine.begin() as c:
        assert c.scalar(text("SELECT token FROM push_tokens WHERE id='p'")) == "token-1"
        assert c.scalar(text("SELECT assistant_tone FROM profiles WHERE user_id='u'")) == "friendly"
        c.execute(text("INSERT INTO push_tokens (id,user_id,platform,token) VALUES ('p2','u','android','token-2')"))
    with pytest.raises(IntegrityError), engine.begin() as c:
        c.execute(text("INSERT INTO push_tokens (id,user_id,platform,token) VALUES ('p3','u','ios','token-1')"))
    with engine.begin() as c:
        c.execute(text("DELETE FROM users WHERE id='u'"))
        assert c.scalar(text("SELECT count(*) FROM profiles")) == 0
        assert c.scalar(text("SELECT count(*) FROM push_tokens")) == 0


def test_duplicate_tokens_stop_upgrade_without_deleting_rows(pg_schema):
    engine, migrate = pg_schema
    migrate("0011_account_deletions")
    seed(engine)
    with engine.begin() as c:
        c.execute(text("INSERT INTO push_tokens (id,user_id,platform,token) VALUES ('p2','u','ios','token-1')"))
    result = migrate(expect_success=False)
    assert "Duplicate push tokens detected" in result.stderr
    with engine.connect() as c:
        assert c.scalar(text("SELECT count(*) FROM push_tokens")) == 2
        assert c.scalar(text("SELECT version_num FROM alembic_version")) == "0011_account_deletions"
    with engine.begin() as c:
        c.execute(text("UPDATE push_tokens SET token='token-2' WHERE id='p2'"))
    migrate()
    with engine.connect() as c:
        assert c.scalar(text("SELECT count(*) FROM push_tokens")) == 2


def test_legacy_standalone_unique_index_is_removed(pg_schema):
    engine, migrate = pg_schema
    migrate("0011_account_deletions")
    seed(engine)
    with engine.begin() as c:
        c.execute(text("CREATE UNIQUE INDEX old_device_index ON push_tokens (user_id,platform)"))
    migrate()
    with engine.begin() as c:
        c.execute(text("INSERT INTO push_tokens (id,user_id,platform,token) VALUES ('p2','u','android','token-2')"))
        assert c.scalar(text("SELECT count(*) FROM push_tokens")) == 2


def test_durable_chatbot_quota_and_duplicate_requests(pg_schema):
    from concurrent.futures import ThreadPoolExecutor

    from app.models.user import User
    from app.services.chatbot_usage import ChatbotUsageService, DuplicateChatbotRequest
    from sqlalchemy.orm import Session

    engine, migrate = pg_schema
    migrate()
    service = ChatbotUsageService()
    with Session(engine) as db:
        user = User(email="quota@example.invalid", password_hash="unused")
        db.add(user)
        db.commit()
        uid = user.id

    def reserve(_):
        with Session(engine) as db:
            return service.reserve_request(db, uid, 3, str(uuid4()))

    with ThreadPoolExecutor(max_workers=8) as pool:
        records = list(pool.map(reserve, range(16)))
    accepted = [record for record in records if record]
    assert len(accepted) == 3
    with Session(engine) as db:
        assert service.get_or_create_today_usage(db, uid).message_count == 3
        assert service.finish_request(db, accepted[0], success=False)
        assert not service.finish_request(db, accepted[0], success=False)
        request_id = str(uuid4())
        assert service.reserve_request(db, uid, 3, request_id)
        with pytest.raises(DuplicateChatbotRequest):
            service.reserve_request(db, uid, 3, request_id)
        assert service.get_or_create_today_usage(db, uid).message_count == 3

    # Identical request IDs racing on separate connections may call the model once.
    with Session(engine) as db:
        service.finish_request(db, accepted[1], success=False)
    shared_id = str(uuid4())

    def same_request(_):
        with Session(engine) as db:
            try:
                return service.reserve_request(db, uid, 3, shared_id)
            except DuplicateChatbotRequest:
                return "duplicate"

    with ThreadPoolExecutor(max_workers=8) as pool:
        outcomes = list(pool.map(same_request, range(8)))
    assert outcomes.count("duplicate") == 7
    assert sum(value not in (None, "duplicate") for value in outcomes) == 1


def test_postgres_consent_upsert_audit_and_account_cascade(pg_schema, assistant_texts):
    from app.models.assistant_consent import AssistantConsent, AssistantConsentEvent
    from app.models.user import User
    from app.schemas.chatbot import AssistantConsentUpdate
    from app.services.assistant_consent import current_documents, save_consent
    from sqlalchemy import delete, func, select
    from sqlalchemy.orm import Session

    engine, migrate = pg_schema
    migrate()
    with Session(engine) as db:
        user = User(email="consent@example.invalid", password_hash="unused")
        db.add(user)
        db.commit()
        uid = user.id
        assistant, health = current_documents()
        save_consent(db, uid, AssistantConsentUpdate(assistant_accepted=True, consent_version=2,
            consent_hash=assistant["hash"], health_accepted=True, health_consent_version=2,
            health_consent_hash=health["hash"]))
        save_consent(db, uid, AssistantConsentUpdate(assistant_accepted=False))
        assert not db.get(AssistantConsent, uid, populate_existing=True).health_accepted
        assert db.scalar(select(func.count()).select_from(AssistantConsentEvent)) == 2
        db.execute(delete(User).where(User.id == uid))
        db.commit()
        assert db.get(AssistantConsent, uid) is None
        assert db.scalar(select(func.count()).select_from(AssistantConsentEvent)) == 0
