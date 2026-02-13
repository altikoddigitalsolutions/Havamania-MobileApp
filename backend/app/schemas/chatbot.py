from pydantic import BaseModel, Field


class ChatbotAskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=2000)


class ChatbotAskResponse(BaseModel):
    answer: str
    used_messages_today: int
    remaining_messages_today: int
    is_premium: bool


class ChatbotUsageResponse(BaseModel):
    used_messages_today: int
    remaining_messages_today: int
    daily_limit: int
    is_premium: bool
