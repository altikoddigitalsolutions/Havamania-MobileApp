from hashlib import sha256

from fastapi import HTTPException
from sqlalchemy import and_, func, or_, select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert

from app.models.push_token import PushToken
from app.models.push_token_binding import PushTokenBinding


def binding_conflict():
    return HTTPException(
        409,
        detail={
            "error": "token_binding_conflict",
            "message": "Token binding was superseded; use the latest installation revision",
        },
        headers={"Cache-Control": "no-store"},
    )


def prepare_binding(db, payload, user_id, bound=True):
    digest = sha256(payload.token.encode()).hexdigest()
    if db.get_bind().dialect.name == "postgresql":
        lock_id = int.from_bytes(bytes.fromhex(digest)[:8], "big", signed=True)
        db.execute(select(func.pg_advisory_xact_lock(lock_id)))
    if payload.owner_protocol is None:
        if db.get(PushTokenBinding, digest, populate_existing=True) is not None:
            raise binding_conflict()
        return {}
    if not bound:
        owner = db.scalar(select(PushToken.user_id).where(PushToken.token == payload.token))
        if owner is not None and owner != user_id:
            raise binding_conflict()
    values = {
        "token_hash": digest,
        "installation_id": str(payload.installation_id),
        "revision": payload.revision,
        "user_id": user_id,
        "bound": bound,
    }
    insert = {"postgresql": pg_insert, "sqlite": sqlite_insert}[db.get_bind().dialect.name]
    binding = PushTokenBinding
    accepted = db.scalar(
        insert(binding)
        .values(**values)
        .on_conflict_do_update(
            index_elements=["token_hash"],
            set_={k: v for k, v in values.items() if k != "token_hash"},
            where=and_(
                binding.installation_id == str(payload.installation_id),
                or_(
                    and_(
                        binding.revision < payload.revision,
                        True if bound else binding.user_id == user_id,
                    ),
                    and_(
                        binding.revision == payload.revision,
                        binding.user_id == user_id,
                        binding.bound == bound,
                    ),
                ),
            ),
        )
        .returning(binding.token_hash)
    )
    if accepted is None:
        db.rollback()
        raise binding_conflict()
    return {
        "installation_id": str(payload.installation_id),
        "revision": payload.revision,
        "owner_protocol": 1,
    }
