from fastapi import Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.models.user import User
from app.services.subscription_service import get_or_create_subscription, is_premium_active


def require_premium_user(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> User:
    subscription = get_or_create_subscription(db, current_user.id)
    if not is_premium_active(subscription):
        raise HTTPException(
            status_code=status.HTTP_402_PAYMENT_REQUIRED,
            detail="Premium subscription required",
        )
    return current_user
