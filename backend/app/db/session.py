from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app.core.config import get_settings
from app.db.push_token_migration import migrate_push_tokens

settings = get_settings()
engine = create_engine(settings.database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def run_push_token_migration():
    """Explicit maintenance entry point. Normal deployment uses Alembic."""
    with engine.begin() as connection:
        migrate_push_tokens(connection)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
