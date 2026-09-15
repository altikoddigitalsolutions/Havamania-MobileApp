from functools import lru_cache
from secrets import token_urlsafe
from uuid import NAMESPACE_URL, uuid5

import firebase_admin
from fastapi import HTTPException
from firebase_admin import auth
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.core.security import hash_password
from app.models.user import User


@lru_cache
def firebase_app(project_id: str):
    # A named app keeps token verification separate from push-provider initialization.
    return firebase_admin.initialize_app(
        options={"projectId": project_id}, name=f"identity-{project_id}"
    )


def verify_firebase_token(token: str, project_id: str) -> dict:
    return auth.verify_id_token(token, app=firebase_app(project_id), check_revoked=True)


def firebase_user(token: str, db: Session) -> User:
    project_id = get_settings().firebase_project_id
    if not project_id:
        raise HTTPException(status_code=401, detail="Invalid authentication credentials")
    try:
        claims = verify_firebase_token(token, project_id)
    except (ValueError, auth.InvalidIdTokenError, auth.RevokedIdTokenError, auth.UserDisabledError) as exc:
        raise HTTPException(status_code=401, detail="Invalid Firebase credentials") from exc
    uid = claims.get("uid") or claims.get("sub")
    email = claims.get("email")
    if not uid or not email:
        raise HTTPException(status_code=401, detail="Firebase identity requires an email")

    # Bind identity to the verified project and UID, never to an email alone.
    user_id = str(uuid5(NAMESPACE_URL, f"https://securetoken.google.com/{project_id}/{uid}"))
    user = db.get(User, user_id)
    if user:
        return user
    if db.query(User).filter(User.email == email).first():
        raise HTTPException(status_code=409, detail="Account linking is required")
    user = User(id=user_id, email=email, full_name=claims.get("name"),
                password_hash=hash_password(token_urlsafe(32)))
    db.add(user)
    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        existing = db.get(User, user_id)
        if existing:
            return existing
        raise HTTPException(status_code=409, detail="Account linking is required") from exc
    db.refresh(user)
    return user
