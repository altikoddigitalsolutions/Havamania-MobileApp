from uuid import UUID

from pydantic import BaseModel, Field, StrictInt, model_validator


class PushTokenRequest(BaseModel):
    platform: str = Field(pattern="^(ios|android)$")
    token: str = Field(min_length=10, max_length=512)
    installation_id: UUID | None = None
    revision: StrictInt | None = Field(None, ge=1, le=9007199254740991)
    owner_protocol: StrictInt | None = Field(None, ge=1, le=1)

    @model_validator(mode="after")
    def complete_binding(self):
        fields = (self.installation_id, self.revision, self.owner_protocol)
        if any(value is not None for value in fields) and (
            any(value is None for value in fields) or self.platform != "android"
        ):
            raise ValueError("Complete Android owner protocol binding is required")
        return self


class PushTokenResponse(BaseModel):
    id: str
    user_id: str
    platform: str
    token: str
    installation_id: str | None = None
    revision: int | None = None
    owner_protocol: int = 0
