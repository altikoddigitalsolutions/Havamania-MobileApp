from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.models.user import User
from app.schemas.subscription import (
    StoreWebhookRequest,
    SubscriptionStatusResponse,
    ValidateReceiptRequest,
)
from app.services.receipt_adapters import (
    AndroidReceiptValidationAdapter,
    IOSReceiptValidationAdapter,
)
from app.services.subscription_service import (
    get_or_create_subscription,
    is_premium_active,
    upsert_subscription,
)

router = APIRouter()


@router.get("/status", response_model=SubscriptionStatusResponse)
def get_subscription_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> SubscriptionStatusResponse:
    subscription = get_or_create_subscription(db, current_user.id)
    return SubscriptionStatusResponse(
        plan_code=subscription.plan_code,
        status=subscription.status,
        expires_at=subscription.expires_at,
        is_premium_active=is_premium_active(subscription),
    )


@router.post("/validate-receipt", response_model=SubscriptionStatusResponse)
def validate_receipt(
    payload: ValidateReceiptRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> SubscriptionStatusResponse:
    adapter = IOSReceiptValidationAdapter() if payload.store == "ios" else AndroidReceiptValidationAdapter()
    result = adapter.validate(payload.receipt_data, payload.plan_code)

    if not result.is_valid:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid receipt")

    subscription = upsert_subscription(
        db=db,
        user_id=current_user.id,
        plan_code=result.plan_code,
        status=result.status,
        expires_at=result.expires_at,
        store=payload.store,
        original_transaction_id=result.original_transaction_id,
    )

    return SubscriptionStatusResponse(
        plan_code=subscription.plan_code,
        status=subscription.status,
        expires_at=subscription.expires_at,
        is_premium_active=is_premium_active(subscription),
    )


@router.post("/webhook/store", response_model=SubscriptionStatusResponse)
def store_webhook(payload: StoreWebhookRequest, db: Session = Depends(get_db)) -> SubscriptionStatusResponse:
    subscription = upsert_subscription(
        db=db,
        user_id=payload.user_id,
        plan_code=payload.plan_code,
        status=payload.status,
        expires_at=payload.expires_at,
        store=payload.store,
        original_transaction_id=payload.original_transaction_id,
    )

    return SubscriptionStatusResponse(
        plan_code=subscription.plan_code,
        status=subscription.status,
        expires_at=subscription.expires_at,
        is_premium_active=is_premium_active(subscription),
    )
