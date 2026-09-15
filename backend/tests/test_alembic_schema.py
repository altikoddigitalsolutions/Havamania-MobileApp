import os
import subprocess
import sys
from pathlib import Path

from app.db.base import Base
from sqlalchemy import create_engine, inspect


def test_actual_alembic_chain_contains_every_model_column(tmp_path):
    database = tmp_path / "migrated.sqlite"
    env = dict(os.environ, APP_ENV="test", DATABASE_URL=f"sqlite:///{database.as_posix()}")
    result = subprocess.run([sys.executable, "-m", "alembic", "upgrade", "head"],
                            cwd=Path(__file__).resolve().parents[1], env=env,
                            capture_output=True, text=True, timeout=60, check=False)
    assert result.returncode == 0, result.stdout + result.stderr
    engine = create_engine(env["DATABASE_URL"])
    try:
        inspector = inspect(engine)
        for name, table in Base.metadata.tables.items():
            columns = {column["name"] for column in inspector.get_columns(name)}
            assert set(table.columns.keys()) <= columns, f"Missing columns in {name}"
        indexes = inspector.get_indexes("push_tokens")
        assert any(index["unique"] and index["column_names"] == ["token"] for index in indexes)
    finally:
        engine.dispose()
