from hashlib import sha256
from uuid import uuid4

from fastapi import APIRouter, Depends, status
from sqlalchemy import delete, exists, literal, select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.models.push_token import PushToken
from app.models.push_token_binding import PushTokenBinding
from app.models.user import User
from app.schemas.device import PushTokenRequest, PushTokenResponse
from app.services.device_bindings import binding_conflict, prepare_binding

router = APIRouter()


@router.post("/push-token", response_model=PushTokenResponse, status_code=status.HTTP_201_CREATED)
def register_push_token(
    payload: PushTokenRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PushTokenResponse:
    binding = prepare_binding(db, payload, current_user.id)
    insert = {"postgresql": pg_insert, "sqlite": sqlite_insert}[db.get_bind().dialect.name]
    # A token is globally unique. Atomic upsert also serializes with the token row
    # lock held during dispatch; racing first registrations must not raise 500.
    legacy_guard = (
        ~exists(
            select(PushTokenBinding.token_hash).where(
                PushTokenBinding.token_hash == sha256(payload.token.encode()).hexdigest()
            )
        )
        if not binding
        else True
    )
    statement = insert(PushToken).from_select(
        ["id", "user_id", "platform", "token", "owner_protocol"],
        select(
            literal(str(uuid4())),
            literal(current_user.id),
            literal(payload.platform),
            literal(payload.token),
            literal(payload.owner_protocol or 0),
        ).where(legacy_guard),
    )
    row = db.execute(
        statement.on_conflict_do_update(
            index_elements=["token"],
            set_={
                "user_id": current_user.id,
                "platform": payload.platform,
                "owner_protocol": payload.owner_protocol or 0,
            },
            where=legacy_guard,
        ).returning(PushToken.id, PushToken.user_id, PushToken.platform, PushToken.token)
    ).one_or_none()
    if row is None:
        db.rollback()
        raise binding_conflict()
    response = PushTokenResponse(
        id=row.id, user_id=row.user_id, platform=row.platform, token=row.token, **binding
    )
    db.commit()
    return response


@router.delete("/push-token", status_code=status.HTTP_204_NO_CONTENT)
def unregister_push_token(
    payload: PushTokenRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    prepare_binding(db, payload, current_user.id, bound=False)
    # Predicate must be checked by the DELETE itself, not by an earlier ORM read:
    # an old account's delayed logout must never delete the new owner's token.
    legacy_guard = (
        ~exists(
            select(PushTokenBinding.token_hash).where(
                PushTokenBinding.token_hash == sha256(payload.token.encode()).hexdigest()
            )
        )
        if payload.owner_protocol is None
        else True
    )
    db.execute(
        delete(PushToken).where(
            PushToken.user_id == current_user.id, PushToken.token == payload.token, legacy_guard
        )
    )
    db.commit()
