import sqlalchemy
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from app.core.config import get_settings

settings = get_settings()

engine = create_engine(settings.database_url, pool_pre_ping=True)
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


def run_push_token_migration():
    """Production-safe migration for push_tokens table:
    1. Precheck for duplicate tokens (COUNT(*) > 1). If found, FAIL-FAST (raise RuntimeError, ZERO row deletion, NO DDL).
    2. Dynamically inspect table 'push_tokens' to discover unique constraints and unique indexes on columns {"user_id", "platform"}.
    3. Safely drop discovered constraints/indexes using dialect identifier preparer quoting.
    4. Create unique index on (token).
    5. Final schema metadata verification.
    """
    try:
        inspector = sqlalchemy.inspect(engine)
        if "push_tokens" in inspector.get_table_names():
            with engine.begin() as conn:
                # 1. Precheck for duplicate tokens (fail-fast, zero deletion)
                dups = conn.execute(
                    text(
                        """
                        SELECT token FROM push_tokens
                        GROUP BY token
                        HAVING COUNT(*) > 1
                        """
                    )
                ).fetchall()
                if dups:
                    raise RuntimeError(
                        "Migration fail-fast: Duplicate push tokens detected. "
                        "Manual remediation required to resolve token conflicts without silent data loss."
                    )

                # 2. Dynamic inspection of old uniqueness on {"user_id", "platform"}
                dialect = engine.dialect
                preparer = dialect.identifier_preparer

                try:
                    constraints = inspector.get_unique_constraints("push_tokens")
                    for constraint in constraints:
                        cols = set(constraint.get("column_names", []))
                        if cols == {"user_id", "platform"}:
                            c_name = constraint.get("name")
                            if c_name:
                                quoted_table = preparer.quote("push_tokens")
                                quoted_constraint = preparer.quote(c_name)
                                conn.execute(text(f"ALTER TABLE {quoted_table} DROP CONSTRAINT {quoted_constraint}"))
                except Exception as e:
                    import logging
                    logging.warning(f"Constraint inspection/drop notice: {e}")

                try:
                    indexes = inspector.get_indexes("push_tokens")
                    for index in indexes:
                        cols = set(index.get("column_names", []))
                        if cols == {"user_id", "platform"} and index.get("unique", False):
                            idx_name = index.get("name")
                            if idx_name:
                                quoted_index = preparer.quote(idx_name)
                                conn.execute(text(f"DROP INDEX IF EXISTS {quoted_index}"))
                except Exception as e:
                    import logging
                    logging.warning(f"Index inspection/drop notice: {e}")

                # 3. Create unique index on token
                try:
                    quoted_token_idx = preparer.quote("ix_push_tokens_token")
                    quoted_table = preparer.quote("push_tokens")
                    quoted_col = preparer.quote("token")
                    conn.execute(text(f"CREATE UNIQUE INDEX IF NOT EXISTS {quoted_token_idx} ON {quoted_table} ({quoted_col})"))
                except Exception as e:
                    import logging
                    logging.warning(f"Could not create unique index on token: {e}")

                # 4. Final Schema Verification
                post_inspector = sqlalchemy.inspect(engine)
                post_constraints = post_inspector.get_unique_constraints("push_tokens")
                post_indexes = post_inspector.get_indexes("push_tokens")

                has_old = any(set(c.get("column_names", [])) == {"user_id", "platform"} for c in post_constraints) or \
                          any(set(idx.get("column_names", [])) == {"user_id", "platform"} and idx.get("unique", False) for idx in post_indexes)

                has_new = any(set(idx.get("column_names", [])) == {"token"} and idx.get("unique", False) for idx in post_indexes) or \
                          any(set(c.get("column_names", [])) == {"token"} for c in post_constraints)

                if has_old:
                    raise RuntimeError("Final schema verification failed: Old unique constraint/index on (user_id, platform) still present.")
                if not has_new:
                    raise RuntimeError("Final schema verification failed: New unique index/constraint on (token) not found.")

    except Exception as e:
        import logging
        logging.error(f"Push token migration final verification error: {e}")
        if "getaddrinfo failed" not in str(e) and "operationalerror" not in str(e).lower():
            raise e


def get_db():
    run_push_token_migration()
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
