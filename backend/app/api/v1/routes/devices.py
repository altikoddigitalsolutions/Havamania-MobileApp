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
    existing = db.query(PushToken).filter(PushToken.token == payload.token).first()
    if existing:
        if existing.user_id != current_user.id:
            existing.user_id = current_user.id
            existing.platform = payload.platform
            db.add(existing)
            db.commit()
            db.refresh(existing)
        else:
            existing.platform = payload.platform
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


@router.delete("/push-token", status_code=status.HTTP_204_NO_CONTENT)
def unregister_push_token(
    payload: PushTokenRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    token_obj = (
        db.query(PushToken)
        .filter(PushToken.user_id == current_user.id, PushToken.token == payload.token)
        .first()
    )
    if token_obj:
        db.delete(token_obj)
        db.commit()
