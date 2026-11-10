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
        chatbot_base_url="https://chatbot.internal", chatbot_api_key="local-test-secret"
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
            chatbot_base_url="https://chatbot.internal", chatbot_api_key="local-test-secret"
        )
    assert "DEBUG must be false in production" in str(excinfo.value)


def test_production_cors_no_wildcard():
    settings = Settings(
        app_env="production",
        debug=False,
        secret_key="secure-production-secret-key",
        cors_origins=[],
        database_url="postgresql+psycopg://postgres:postgres@db.internal:5432/havamania",
        chatbot_base_url="https://chatbot.internal", chatbot_api_key="local-test-secret"
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
        chatbot_base_url="https://chatbot.internal", chatbot_api_key="local-test-secret"
    )
    assert settings.database_url is not None


def test_bridge_secret_is_required_in_production_and_redacted():
    config = {"app_env": "production", "secret_key": "local-test-secret",
              "database_url": "postgresql+psycopg://u:p@db.internal/test",
              "chatbot_base_url": "https://example.invalid/api/bridge", "_env_file": None}
    with pytest.raises(ValidationError, match="CHATBOT_API_KEY must be set"):
        Settings(**config)
    configured = Settings(**config, chatbot_api_key="PRIVATE_TEST_KEY")
    assert "PRIVATE_TEST_KEY" not in repr(configured)
    assert "PRIVATE_TEST_KEY" not in configured.model_dump_json()
    with pytest.raises(ValidationError) as error:
        Settings(**config, chatbot_api_key="PRIVATE_TEST_KEY\n")
    assert "PRIVATE_TEST_KEY" not in str(error.value)


@pytest.mark.parametrize("url", ["http://example.invalid", "https://user:pass@example.invalid",
                                "https://example.invalid/?key=secret", "https://example.invalid/#secret"])
def test_bridge_production_url_cannot_leak_key_over_http_or_credentials(url):
    with pytest.raises(ValidationError):
        Settings(app_env="production", secret_key="local-test-secret", _env_file=None,
                 database_url="postgresql+psycopg://u:p@db.internal/test",
                 chatbot_base_url=url, chatbot_api_key="LOCAL_TEST_KEY")
