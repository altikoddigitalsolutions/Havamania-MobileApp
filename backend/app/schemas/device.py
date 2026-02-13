from pydantic import BaseModel, Field


class PushTokenRequest(BaseModel):
    platform: str = Field(pattern="^(ios|android)$")
    token: str = Field(min_length=10, max_length=512)


class PushTokenResponse(BaseModel):
    id: str
    user_id: str
    platform: str
    token: str
