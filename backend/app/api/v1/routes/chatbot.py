from datetime import UTC, datetime, time, timedelta
from math import ceil
from uuid import uuid4

from fastapi import APIRouter, Depends, Header, Response
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.dependencies.premium import require_premium_user
from app.models.profile import Profile
from app.models.user import User
from app.schemas.chatbot import (
    AssistantConsentUpdate,
    ChatbotAskRequest,
    ChatbotAskResponse,
    ChatbotUsageResponse,
)
from app.services import assistant_consent
from app.services.chatbot_bridge import BridgeError, chatbot_bridge_client
from app.services.chatbot_context import TONES, platform_context
from app.services.chatbot_usage import DuplicateChatbotRequest, chatbot_usage_service
from app.services.subscription_service import get_or_create_subscription, is_premium_active

router = APIRouter()
settings = get_settings()


def _is_premium_user(user: User, db: Session) -> bool:
    subscription = get_or_create_subscription(db, user.id)
    return is_premium_active(subscription)


def _get_daily_limit(is_premium: bool) -> int:
    return settings.chatbot_premium_daily_limit if is_premium else settings.chatbot_free_daily_limit


@router.post("/ask", response_model=ChatbotAskResponse)
def ask_chatbot(
    payload: ChatbotAskRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    app_version: str | None = Header(None, alias="X-App-Version", max_length=40),
) -> ChatbotAskResponse | JSONResponse:
    return _ask(payload, current_user, db, _is_premium_user(current_user, db), app_version)


def _ask(
    payload: ChatbotAskRequest, current_user: User, db: Session, is_premium: bool, app_version: str | None = None
) -> ChatbotAskResponse | JSONResponse:
    assistant_doc, health_doc = assistant_consent.current_documents()
    consent = assistant_consent.consent_state(db, current_user.id)
    if not assistant_consent.assistant_is_allowed(consent, assistant_doc) or (
        payload.consent_version is not None and payload.consent_version != assistant_doc["version"]
    ):
        return JSONResponse(status_code=428, content={
            "error": "consent_required", "message": "Asistanı kullanmadan önce güncel metni onaylayın.",
            "consent": {**assistant_consent.public_document(assistant_doc),
                        "health": assistant_consent.public_document(health_doc)},
        })
    profile = db.query(Profile).filter(Profile.user_id == current_user.id).first()
    try:
        platform_context(payload, profile, False)  # Validate byte limits before allocating quota.
    except BridgeError as exc:
        return JSONResponse(status_code=exc.status, content={"error": exc.code, "message": exc.message})

    daily_limit = _get_daily_limit(is_premium)

    try:
        reservation = chatbot_usage_service.reserve_request(
            db, current_user.id, daily_limit, str(payload.request_id or uuid4())
        )
    except DuplicateChatbotRequest:
        return JSONResponse(status_code=409, content={
            "error": "duplicate_request",
            "message": "Bu istek daha önce alındı. Yeni bir mesaj için yeni istek kimliği kullanın.",
        })
    if reservation is None:
        usage = chatbot_usage_service.get_or_create_today_usage(db, current_user.id)
        now = datetime.now(UTC)
        reset_at = datetime.combine(usage.date + timedelta(days=1), time.min, tzinfo=UTC)
        return JSONResponse(status_code=429, headers={
            "Retry-After": str(max(1, ceil((reset_at - now).total_seconds())))
        }, content={
            "error": "daily_limit",
            "message": "Günlük asistan hakkınız doldu. Limit yenilendiğinde tekrar kullanabilirsiniz.",
            "used_messages_today": usage.message_count,
            "remaining_messages_today": 0,
            "reset_at": reset_at.isoformat(),
        })

    try:
        # Re-read after quota commit: a withdrawal must not use an earlier ORM snapshot.
        assistant_doc, health_doc = assistant_consent.current_documents()
        consent = assistant_consent.consent_state(db, current_user.id)
        if not assistant_consent.assistant_is_allowed(consent, assistant_doc) or (
            payload.consent_version is not None and payload.consent_version != assistant_doc["version"]
        ):
            chatbot_usage_service.finish_request(db, reservation, success=False)
            return JSONResponse(status_code=428, content={
                "error": "consent_required", "message": "Güncel asistan onayı gerekli.",
                "consent": {**assistant_consent.public_document(assistant_doc),
                            "health": assistant_consent.public_document(health_doc)},
            })
        allow_health = assistant_consent.health_is_allowed(consent, health_doc, payload)
        context = platform_context(payload, profile, allow_health)
        reply = chatbot_bridge_client.ask(
            question=payload.question, user_id=current_user.id, context=context,
            conversation_id=str(payload.conversation_id) if payload.conversation_id else None,
            mode=payload.mode, tone=payload.tone or TONES.get(profile.assistant_tone if profile else "", "dengeli"),
            language="tr", consent_version=assistant_doc["version"],
            health_consent=bool(allow_health and payload.personalization_enabled),
            health_consent_version=health_doc["version"] if allow_health and payload.personalization_enabled else None,
            client={"platform": "android", **({"app_version": app_version} if app_version else {})},
        )
    except BridgeError as exc:
        chatbot_usage_service.finish_request(db, reservation, success=False)
        return JSONResponse(
            status_code=exc.status, content={"error": exc.code, "message": exc.message},
            headers={"Retry-After": exc.retry_after} if exc.status == 429 and exc.retry_after else None
        )
    except Exception:
        chatbot_usage_service.finish_request(db, reservation, success=False)
        raise
    if not chatbot_usage_service.finish_request(db, reservation, success=True):
        return JSONResponse(status_code=409, content={
            "error": "request_expired", "message": "İstek süresi doldu."
        })
    usage = chatbot_usage_service.get_or_create_today_usage(db, current_user.id)

    return ChatbotAskResponse(
        answer=reply.answer, conversation_id=reply.conversation_id,
        message_id=reply.message_id, disclaimer=reply.disclaimer,
        used_messages_today=usage.message_count,
        remaining_messages_today=max(daily_limit - usage.message_count, 0),
        is_premium=is_premium,
    )


@router.get("/usage", response_model=ChatbotUsageResponse)
def chatbot_usage(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> ChatbotUsageResponse:
    is_premium = _is_premium_user(current_user, db)
    chatbot_usage_service.recover_expired(db, current_user.id)
    usage = chatbot_usage_service.get_or_create_today_usage(db, current_user.id)
    daily_limit = _get_daily_limit(is_premium)

    return ChatbotUsageResponse(
        used_messages_today=usage.message_count,
        remaining_messages_today=max(daily_limit - usage.message_count, 0),
        daily_limit=daily_limit,
        is_premium=is_premium,
    )


@router.post("/ask/premium", response_model=ChatbotAskResponse, dependencies=[Depends(require_premium_user)])
def ask_chatbot_premium(
    payload: ChatbotAskRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    app_version: str | None = Header(None, alias="X-App-Version", max_length=40),
) -> ChatbotAskResponse | JSONResponse:
    return _ask(payload, current_user, db, True, app_version)


@router.get("/consent")
def get_assistant_consent(response: Response, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    response.headers["Cache-Control"] = "no-store"
    assistant, health = assistant_consent.current_documents()
    state = assistant_consent.consent_state(db, current_user.id)
    return {**assistant_consent.public_document(assistant),
            "health": assistant_consent.public_document(health),
            "accepted": assistant_consent.assistant_is_allowed(state, assistant),
            "health_review_required": bool(health and state and state.health_accepted and (
                state.health_version != health["version"] or state.health_hash != health["hash"])),
            "health_accepted": bool(health and state and state.health_accepted
                                    and state.health_version == health["version"]
                                    and state.health_hash == health["hash"])}


@router.post("/consent")
def update_assistant_consent(payload: AssistantConsentUpdate, response: Response,
                             current_user: User = Depends(get_current_user),
                             db: Session = Depends(get_db)):
    response.headers["Cache-Control"] = "no-store"
    return assistant_consent.save_consent(db, current_user.id, payload)
