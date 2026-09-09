import sqlalchemy
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from app.core.config import get_settings

settings = get_settings()

engine = create_engine(settings.database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def run_push_token_migration():
    """Safely migrate push_tokens table constraints from (user_id, platform) to unique(token)."""
    try:
        inspector = sqlalchemy.inspect(engine)
        if "push_tokens" in inspector.get_table_names():
            with engine.begin() as conn:
                # Deduplicate tokens if multiple exist, keeping the latest id/created_at
                conn.execute(
                    text(
                        """
                        DELETE FROM push_tokens
                        WHERE id NOT IN (
                            SELECT MAX(id)
                            FROM push_tokens
                            GROUP BY token
                        )
                        """
                    )
                )
                try:
                    conn.execute(text("CREATE UNIQUE INDEX IF NOT EXISTS ix_push_tokens_token ON push_tokens (token)"))
                except Exception:
                    pass
    except Exception as e:
        import logging
        logging.warning(f"Push token migration check notice: {e}")


# Run migration check on startup/import
run_push_token_migration()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
