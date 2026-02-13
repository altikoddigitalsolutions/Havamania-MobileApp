from datetime import UTC, datetime

from sqlalchemy.orm import Session

from app.models.subscription import Subscription

FREE_PLAN = "free"
PREMIUM_MONTHLY = "premium_monthly"
PREMIUM_YEARLY = "premium_yearly"

ACTIVE_STATUSES = {"active", "grace"}


def get_or_create_subscription(db: Session, user_id: str) -> Subscription:
    subscription = db.query(Subscription).filter(Subscription.user_id == user_id).first()
    if subscription:
        return subscription

    subscription = Subscription(user_id=user_id, plan_code=FREE_PLAN, status="expired")
    db.add(subscription)
    db.commit()
    db.refresh(subscription)
    return subscription


def is_premium_active(subscription: Subscription) -> bool:
    now = datetime.now(UTC)

    if subscription.status in ACTIVE_STATUSES:
        if subscription.expires_at is None:
            return True
        return subscription.expires_at > now

    # Cancellation/grace handling: canceled users keep access until expires_at.
    if subscription.status == "canceled" and subscription.expires_at is not None:
        return subscription.expires_at > now

    return False


def upsert_subscription(
    db: Session,
    user_id: str,
    plan_code: str,
    status: str,
    expires_at,
    store: str | None,
    original_transaction_id: str | None,
) -> Subscription:
    subscription = get_or_create_subscription(db, user_id)
    subscription.plan_code = plan_code
    subscription.status = status
    subscription.expires_at = expires_at
    subscription.store = store
    subscription.original_transaction_id = original_transaction_id

    db.add(subscription)
    db.commit()
    db.refresh(subscription)
    return subscription
