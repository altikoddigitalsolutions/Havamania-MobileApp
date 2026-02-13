from datetime import datetime

from pydantic import BaseModel, Field


class SubscriptionStatusResponse(BaseModel):
    plan_code: str
    status: str
    expires_at: datetime | None
    is_premium_active: bool


class ValidateReceiptRequest(BaseModel):
    store: str = Field(pattern="^(ios|android)$")
    receipt_data: str
    plan_code: str = "premium_monthly"


class StoreWebhookRequest(BaseModel):
    user_id: str
    plan_code: str
    status: str
    expires_at: datetime | None = None
    store: str | None = None
    original_transaction_id: str | None = None
