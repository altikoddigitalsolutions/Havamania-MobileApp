import json
from functools import lru_cache
from typing import List, Union, Any

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Havamania API"
    app_env: str = "production"
    debug: bool = False
    api_v1_prefix: str = "/v1"

    secret_key: str = "change-me"
    access_token_expire_minutes: int = 30
    refresh_token_expire_days: int = 30

    database_url: str = "postgresql+psycopg://postgres:postgres@localhost:5432/havamania"
    cors_origins: List[str] = Field(default_factory=list)
    weather_provider: str = "open_meteo"
    chatbot_base_url: str = "http://localhost:9000"
    chatbot_timeout_seconds: int = 15
    chatbot_free_daily_limit: int = 10
    chatbot_premium_daily_limit: int = 100
    sentry_dsn_backend: str | None = None

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore"
    )

    @field_validator("secret_key")
    @classmethod
    def check_secret_key(cls, v: str, info: Any) -> str:
        if v == "change-me" and info.data.get("app_env") == "production":
            raise ValueError("SECRET_KEY must be set in production environment")
        return v

    @field_validator("debug")
    @classmethod
    def check_debug_in_production(cls, v: bool, info: Any) -> bool:
        if v and info.data.get("app_env") == "production":
            raise ValueError("DEBUG must be false in production environment")
        return v

    @field_validator("database_url")
    @classmethod
    def check_database_url(cls, v: str, info: Any) -> str:
        if "localhost" in v and info.data.get("app_env") == "production":
            import logging
            logging.warning("DATABASE_URL points to localhost in production mode")
        return v

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, value: Any) -> List[str]:
        if isinstance(value, list):
            return value
        if isinstance(value, str):
            if not value.strip():
                return []
            if value.startswith("[") and value.endswith("]"):
                try:
                    return json.loads(value)
                except json.JSONDecodeError:
                    pass
            return [item.strip() for item in value.split(",") if item.strip()]
        return []


@lru_cache
def get_settings() -> Settings:
    return Settings()
