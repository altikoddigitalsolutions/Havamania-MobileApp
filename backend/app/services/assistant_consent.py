from datetime import UTC, datetime
from hashlib import sha256
from uuid import uuid4

from fastapi import HTTPException
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.models.assistant_consent import AssistantConsent, AssistantConsentEvent
from app.schemas.chatbot import AssistantConsentUpdate

settings = get_settings()


def document(version, text):
    if version is None or not text or not text.strip():
        return None
    return {"version": version, "text": text,
            "hash": sha256(text.encode("utf-8")).hexdigest()}


def current_documents():
    assistant = document(settings.assistant_consent_version, settings.assistant_consent_text)
    if assistant is None:
        raise HTTPException(503, "Assistant consent text is not configured")
    health = document(settings.assistant_health_consent_version, settings.assistant_health_consent_text)
    return assistant, health


def public_document(doc):
    return {"version": doc["version"], "text": doc["text"], "text_hash": doc["hash"]} if doc else None


def consent_state(db: Session, uid: str):
    return db.get(AssistantConsent, uid, populate_existing=True)


def assistant_is_allowed(state, doc):
    return bool(state and state.assistant_accepted
                and state.assistant_version == doc["version"]
                and state.assistant_hash == doc["hash"])


def health_is_allowed(state, doc, request):
    return bool(doc and state and state.health_accepted
                and state.health_version == doc["version"] and state.health_hash == doc["hash"]
                and request.health_consent is True
                and request.health_consent_version == doc["version"])


def save_consent(db: Session, uid: str, payload: AssistantConsentUpdate):
    assistant, health = (None, None)
    if payload.assistant_accepted:
        assistant, health = current_documents()
        if payload.consent_version != assistant["version"] or payload.consent_hash != assistant["hash"]:
            raise HTTPException(409, "Consent version changed; fetch the current text")
    if payload.health_accepted and (not payload.assistant_accepted or health is None
                                   or payload.health_consent_version != health["version"]
                                   or payload.health_consent_hash != health["hash"]):
        raise HTTPException(409, "Current separate health consent is required")
    values = {"user_id": uid, "assistant_accepted": payload.assistant_accepted,
              "assistant_version": assistant["version"] if assistant else None,
              "assistant_hash": assistant["hash"] if assistant else None,
              "health_accepted": payload.health_accepted,
              "health_version": health["version"] if payload.health_accepted else None,
              "health_hash": health["hash"] if payload.health_accepted else None,
              "recorded_at": datetime.now(UTC)}
    dialect = db.get_bind().dialect.name
    insert = {"postgresql": pg_insert, "sqlite": sqlite_insert}[dialect]
    statement = insert(AssistantConsent).values(**values)
    db.execute(statement.on_conflict_do_update(index_elements=["user_id"],
               set_={key: value for key, value in values.items() if key != "user_id"}))
    db.add(AssistantConsentEvent(id=str(uuid4()), **values))
    db.commit()
    return {"assistant_accepted": payload.assistant_accepted,
            "health_accepted": payload.health_accepted}
