import pytest
from app.core.config import Settings
from pydantic import ValidationError


def test_development_config():
    settings = Settings(app_env="development", debug=True, secret_key="test-key")
    assert settings.app_env == "development"
    assert settings.debug is True


def test_production_safe_config():
    settings = Settings(
        app_env="production",
        debug=False,
        secret_key="secure-production-secret-key",
        database_url="postgresql+psycopg://postgres:postgres@db.internal:5432/havamania",
        chatbot_base_url="http://chatbot.internal:9000"
    )
    assert settings.app_env == "production"
    assert settings.debug is False


def test_production_debug_rejection():
    with pytest.raises(ValidationError) as excinfo:
        Settings(
            app_env="production",
            debug=True,
            secret_key="secure-production-secret-key",
            database_url="postgresql+psycopg://postgres:postgres@db.internal:5432/havamania",
            chatbot_base_url="http://chatbot.internal:9000"
        )
    assert "DEBUG must be false in production" in str(excinfo.value)


def test_production_cors_no_wildcard():
    settings = Settings(
        app_env="production",
        debug=False,
        secret_key="secure-production-secret-key",
        cors_origins=[],
        database_url="postgresql+psycopg://postgres:postgres@db.internal:5432/havamania",
        chatbot_base_url="http://chatbot.internal:9000"
    )
    assert settings.cors_origins == []


def test_production_localhost_db_rejection():
    with pytest.raises(ValidationError) as excinfo:
        Settings(
            app_env="production",
            debug=False,
            secret_key="secure-production-secret-key",
            database_url="postgresql+psycopg://postgres:postgres@localhost:5432/havamania"
        )
    assert "DATABASE_URL cannot use a loopback host" in str(excinfo.value)


def test_production_remote_db_acceptance():
    settings = Settings(
        app_env="production",
        debug=False,
        secret_key="secure-production-secret-key",
        database_url="postgresql+psycopg://postgres:postgres@db.internal:5432/havamania",
        chatbot_base_url="http://chatbot.internal:9000"
    )
    assert settings.database_url is not None
