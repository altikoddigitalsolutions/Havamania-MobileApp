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
