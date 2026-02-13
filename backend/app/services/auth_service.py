from datetime import UTC, datetime, timedelta

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    hash_token,
    verify_password,
)
from app.models.refresh_token import RefreshToken
from app.models.user import User

settings = get_settings()


def signup(db: Session, email: str, password: str, full_name: str | None) -> User:
    existing = db.query(User).filter(User.email == email).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email already registered")

    user = User(email=email, password_hash=hash_password(password), full_name=full_name)
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def _issue_tokens(db: Session, user_id: str) -> tuple[str, str]:
    access_token = create_access_token(user_id)
    refresh_token = create_refresh_token(user_id)

    refresh_record = RefreshToken(
        user_id=user_id,
        token_hash=hash_token(refresh_token),
        expires_at=datetime.now(UTC) + timedelta(days=settings.refresh_token_expire_days),
        revoked=False,
    )
    db.add(refresh_record)
    db.commit()

    return access_token, refresh_token


def login(db: Session, email: str, password: str) -> tuple[str, str]:
    user = db.query(User).filter(User.email == email).first()
    if not user or not verify_password(password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password",
        )

    return _issue_tokens(db, user.id)


def refresh(db: Session, refresh_token: str) -> tuple[str, str]:
    try:
        payload = decode_token(refresh_token)
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid refresh token") from exc

    if payload.get("type") != "refresh":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid token type")

    token_hash = hash_token(refresh_token)
    token_record = db.query(RefreshToken).filter(RefreshToken.token_hash == token_hash).first()

    if not token_record or token_record.revoked:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Refresh token revoked")

    if token_record.expires_at < datetime.now(UTC):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Refresh token expired")

    user = db.get(User, payload.get("sub"))
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="User not found")

    token_record.revoked = True
    db.add(token_record)
    db.commit()

    return _issue_tokens(db, user.id)


def logout(db: Session, refresh_token: str) -> None:
    token_record = db.query(RefreshToken).filter(RefreshToken.token_hash == hash_token(refresh_token)).first()
    if token_record and not token_record.revoked:
        token_record.revoked = True
        db.add(token_record)
        db.commit()
