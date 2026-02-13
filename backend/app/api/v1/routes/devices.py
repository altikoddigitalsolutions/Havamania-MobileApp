from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.models.push_token import PushToken
from app.models.user import User
from app.schemas.device import PushTokenRequest, PushTokenResponse

router = APIRouter()


@router.post("/push-token", response_model=PushTokenResponse, status_code=status.HTTP_201_CREATED)
def register_push_token(
    payload: PushTokenRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PushTokenResponse:
    existing = (
        db.query(PushToken)
        .filter(PushToken.user_id == current_user.id, PushToken.platform == payload.platform)
        .first()
    )
    if existing:
        existing.token = payload.token
        db.add(existing)
        db.commit()
        db.refresh(existing)
        return PushTokenResponse(
            id=existing.id,
            user_id=existing.user_id,
            platform=existing.platform,
            token=existing.token,
        )

    token = PushToken(user_id=current_user.id, platform=payload.platform, token=payload.token)
    db.add(token)
    db.commit()
    db.refresh(token)

    return PushTokenResponse(
        id=token.id,
        user_id=token.user_id,
        platform=token.platform,
        token=token.token,
    )
