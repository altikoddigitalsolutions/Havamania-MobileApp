from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, StrictBool, field_validator

from app.schemas.assistant_weather import AssistantWeatherData


class AssistantClientContext(BaseModel):
    model_config = ConfigDict(extra="forbid")
    city: str | None = Field(None, max_length=100)
    weather: str | None = Field(None, max_length=2000)
    weather_data: AssistantWeatherData | None = None
    trips: str | None = Field(None, max_length=2000)


class ChatbotAskRequest(BaseModel):
    conversation_id: UUID | None = None
    mode: Literal["genel", "agro", "fly"] = "genel"
    tone: Literal["samimi", "resmi", "dengeli", "kisa", "uzman"] | None = None
    personalization_enabled: StrictBool = False
    selected_interests: list[Annotated[str, Field(max_length=50)]] | None = Field(None, max_length=20)
    context: AssistantClientContext | None = None
    request_id: UUID | None = None
    consent_version: int | None = Field(None, ge=1)
    health_consent: StrictBool = False
    health_consent_version: int | None = Field(None, ge=1)
    question: str = Field(min_length=1, max_length=4000)

    @field_validator("question")
    @classmethod
    def nonblank_question(cls, value):
        if not value.strip():
            raise ValueError("Question is blank")
        return value


class PlatformChatbotReply(BaseModel):
    answer: str = Field(min_length=1, max_length=24000, strict=True)
    conversation_id: UUID
    message_id: UUID
    disclaimer: str | None = Field(None, max_length=4000, strict=True)

    @field_validator("answer")
    @classmethod
    def nonblank_answer(cls, value):
        if not value.strip():
            raise ValueError("Empty answer")
        return value.strip()


class ChatbotAskResponse(BaseModel):
    conversation_id: UUID | None = None
    message_id: UUID | None = None
    disclaimer: str | None = None
    answer: str
    used_messages_today: int
    remaining_messages_today: int
    is_premium: bool


class ChatbotUsageResponse(BaseModel):
    used_messages_today: int
    remaining_messages_today: int
    daily_limit: int
    is_premium: bool


class AssistantConsentUpdate(BaseModel):
    assistant_accepted: StrictBool
    consent_version: int | None = Field(None, ge=1)
    consent_hash: str | None = Field(None, pattern=r"^[0-9a-f]{64}$")
    health_accepted: StrictBool = False
    health_consent_version: int | None = Field(None, ge=1)
    health_consent_hash: str | None = Field(None, pattern=r"^[0-9a-f]{64}$")
