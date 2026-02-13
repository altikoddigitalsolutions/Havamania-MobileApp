from functools import lru_cache
from typing import List

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Havamania API"
    app_env: str = "development"
    debug: bool = True
    api_v1_prefix: str = "/v1"

    secret_key: str = "change-me"
    access_token_expire_minutes: int = 30
    refresh_token_expire_days: int = 30

    database_url: str = "postgresql+psycopg://postgres:postgres@localhost:5432/havamania"
    cors_origins: List[str] = Field(default_factory=lambda: ["http://localhost:3000", "http://localhost:8081"])
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

    @field_validator("cors_origins", mode="before")
    @classmethod
    def parse_cors_origins(cls, value: str | list[str]) -> list[str]:
        if isinstance(value, list):
            return value
        if isinstance(value, str):
            return [item.strip() for item in value.split(",") if item.strip()]
        return []


@lru_cache
def get_settings() -> Settings:
    return Settings()
