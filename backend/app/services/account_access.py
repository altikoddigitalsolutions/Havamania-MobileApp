"""Shared gate for accounts with an accepted, durable deletion request."""
from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.account_deletion import AccountDeletion
from app.models.user import User


def require_active_account(db: Session, user_id: str, allow_missing=False):
    # Serialize writes with prepare_deletion. Call again after any helper commit.
    with db.no_autoflush:
        existing = db.scalar(select(User.id).where(User.id == user_id).with_for_update())
        pending = db.scalar(select(AccountDeletion.key_hash).where(
            AccountDeletion.backend_user_id == user_id, AccountDeletion.status == "pending"
        ).limit(1))
    if pending is not None:
        raise HTTPException(409, detail={
            "error": "account_deletion_pending", "message": "Hesap silme işlemi devam ediyor."
        }, headers={"Cache-Control": "no-store"})
    if existing is None and not allow_missing:
        raise HTTPException(401, detail="User not found")
