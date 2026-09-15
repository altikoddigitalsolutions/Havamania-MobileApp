import pytest
from app.db.session import run_push_token_migration
from sqlalchemy import create_engine, text


def test_migration_fail_fast_on_duplicates(tmp_path):
    db_file = tmp_path / "test_migration.db"
    engine = create_engine(f"sqlite:///{db_file}", connect_args={"check_same_thread": False})

    with engine.begin() as conn:
        conn.execute(text("""
            CREATE TABLE push_tokens (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                platform VARCHAR(16) NOT NULL,
                token VARCHAR(512) NOT NULL,
                created_at DATETIME
            )
        """))
        conn.execute(text("INSERT INTO push_tokens (id, user_id, platform, token) VALUES ('1', 'userA', 'android', 'tokenX')"))
        conn.execute(text("INSERT INTO push_tokens (id, user_id, platform, token) VALUES ('2', 'userB', 'android', 'tokenX')"))

    import app.db.session as db_session
    orig_engine = db_session.engine
    db_session.engine = engine
    try:
        with pytest.raises(RuntimeError, match="Duplicate push tokens detected"):
            run_push_token_migration()

        with engine.begin() as conn:
            cnt = conn.execute(text("SELECT COUNT(*) FROM push_tokens")).scalar()
            assert cnt == 2
    finally:
        db_session.engine = orig_engine


@pytest.mark.parametrize("constraint", ["UNIQUE(user_id, platform)",
                                          "CONSTRAINT old_device UNIQUE(user_id, platform)"])
def test_removes_legacy_constraint_and_preserves_rows(tmp_path, constraint):
    from app.db.push_token_migration import migrate_push_tokens
    from sqlalchemy.exc import IntegrityError

    engine = create_engine(f"sqlite:///{tmp_path / 'legacy.sqlite'}")
    try:
        with engine.begin() as connection:
            connection.execute(text(f"CREATE TABLE push_tokens (id TEXT PRIMARY KEY, "
                                    f"user_id TEXT NOT NULL, platform TEXT NOT NULL, "
                                    f"token TEXT NOT NULL, {constraint})"))
            connection.execute(text("INSERT INTO push_tokens VALUES ('1', 'u', 'android', 'a')"))
            migrate_push_tokens(connection)
            migrate_push_tokens(connection)  # repeated deployment is harmless
            connection.execute(text("INSERT INTO push_tokens VALUES ('2', 'u', 'android', 'b')"))
            assert connection.execute(text("SELECT count(*) FROM push_tokens")).scalar() == 2
        with engine.begin() as connection, pytest.raises(IntegrityError):
            connection.execute(text("INSERT INTO push_tokens VALUES ('3', 'other', 'ios', 'a')"))
    finally:
        engine.dispose()


def test_request_database_dependency_never_runs_migration(monkeypatch):
    from unittest.mock import Mock

    from app.db import session

    migrate = Mock(side_effect=AssertionError("DDL during request"))
    database = Mock()
    monkeypatch.setattr(session, "run_push_token_migration", migrate)
    monkeypatch.setattr(session, "SessionLocal", lambda: database)
    generator = session.get_db()
    assert next(generator) is database
    generator.close()
    database.close.assert_called_once()
    migrate.assert_not_called()

def test_migration_success_and_multi_device(tmp_path):
    db_file = tmp_path / "test_migration_success.db"
    engine = create_engine(f"sqlite:///{db_file}", connect_args={"check_same_thread": False})

    with engine.begin() as conn:
        conn.execute(text("""
            CREATE TABLE push_tokens (
                id VARCHAR(36) PRIMARY KEY,
                user_id VARCHAR(36) NOT NULL,
                platform VARCHAR(16) NOT NULL,
                token VARCHAR(512) NOT NULL,
                created_at DATETIME
            )
        """))
        conn.execute(text("INSERT INTO push_tokens (id, user_id, platform, token) VALUES ('1', 'userU', 'android', 'tokenA')"))
        conn.execute(text("INSERT INTO push_tokens (id, user_id, platform, token) VALUES ('2', 'userU', 'android', 'tokenB')"))

    import app.db.session as db_session
    orig_engine = db_session.engine
    db_session.engine = engine
    try:
        run_push_token_migration()

        with engine.begin() as conn:
            res = conn.execute(text("SELECT token FROM push_tokens WHERE user_id = 'userU'")).fetchall()
            tokens = [r[0] for r in res]
            assert "tokenA" in tokens
            assert "tokenB" in tokens
    finally:
        db_session.engine = orig_engine
