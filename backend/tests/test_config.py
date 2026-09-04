import pytest
from pydantic import ValidationError
from app.core.config import Settings


def test_development_config():
    settings = Settings(app_env="development", debug=True, secret_key="test-key")
    assert settings.app_env == "development"
    assert settings.debug is True


def test_production_safe_config():
    settings = Settings(app_env="production", debug=False, secret_key="secure-production-secret-key")
    assert settings.app_env == "production"
    assert settings.debug is False


def test_production_debug_rejection():
    with pytest.raises(ValidationError) as excinfo:
        Settings(app_env="production", debug=True, secret_key="secure-production-secret-key")
    assert "DEBUG must be false in production" in str(excinfo.value)


def test_production_cors_no_wildcard():
    settings = Settings(app_env="production", debug=False, secret_key="secure-production-secret-key", cors_origins=[])
    assert settings.cors_origins == []
