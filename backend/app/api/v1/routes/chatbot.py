import time as epoch_time
from datetime import UTC, datetime
from math import ceil
from uuid import uuid4

from fastapi import APIRouter, Depends, Header, Response
from fastapi.responses import JSONResponse
from pydantic import ValidationError
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.dependencies.premium import require_premium_user
from app.models.account_deletion import AccountDeletion
from app.models.chatbot_request import ChatbotRequest
from app.models.profile import Profile
from app.models.user import User
from app.schemas.chatbot import (
    AssistantConsentUpdate,
    ChatbotAskRequest,
    ChatbotAskResponse,
    ChatbotUsageResponse,
    PlatformChatbotReply,
)
from app.services import assistant_consent
from app.services.chatbot_bridge import BridgeError, chatbot_bridge_client
from app.services.chatbot_clock import chatbot_reset
from app.services.chatbot_context import TONES, platform_context
from app.services.chatbot_replay import request_fingerprint
from app.services.chatbot_usage import (
    ChatbotAccountUnavailable,
    DuplicateChatbotRequest,
    chatbot_usage_service,
)
from app.services.subscription_service import get_or_create_subscription, is_premium_active

router = APIRouter()
settings = get_settings()


def _deletion_pending(db: Session, user_id: str, lock=False) -> bool:
    if lock and db.scalar(select(User.id).where(User.id == user_id).with_for_update()) is None:
        return True
    return (
        db.scalar(
            select(AccountDeletion.key_hash)
            .where(AccountDeletion.backend_user_id == user_id, AccountDeletion.status == "pending")
            .limit(1)
        )
        is not None
    )


def _deletion_response():
    return JSONResponse(
        status_code=409,
        content={
            "error": "account_deletion_pending",
            "message": "Hesap silinirken asistan kullanılamaz.",
        },
    )


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
    payload: ChatbotAskRequest,
    current_user: User,
    db: Session,
    is_premium: bool,
    app_version: str | None = None,
) -> ChatbotAskResponse | JSONResponse:
    user_id = current_user.id
    if _deletion_pending(db, user_id, lock=True):
        return _deletion_response()
    assistant_doc, health_doc = assistant_consent.current_documents()
    consent = assistant_consent.consent_state(db, current_user.id)
    if not assistant_consent.assistant_is_allowed(consent, assistant_doc) or (
        payload.consent_version is not None and payload.consent_version != assistant_doc["version"]
    ):
        return JSONResponse(
            status_code=428,
            content={
                "error": "consent_required",
                "message": "Asistanı kullanmadan önce güncel metni onaylayın.",
                "consent": {
                    **assistant_consent.public_document(assistant_doc),
                    "health": assistant_consent.public_document(health_doc),
                },
            },
        )
    profile = db.query(Profile).filter(Profile.user_id == user_id).first()
    request_id = str(payload.request_id or uuid4())

    def make_wire(assistant, health, consent_state):
        allowed_health = assistant_consent.health_is_allowed(consent_state, health, payload)
        return {
            "question": payload.question,
            "user_id": user_id,
            "request_id": request_id,
            "context": platform_context(payload, profile, allowed_health),
            "conversation_id": str(payload.conversation_id) if payload.conversation_id else None,
            "mode": payload.mode,
            "tone": payload.tone or TONES.get(profile.assistant_tone if profile else "", "dengeli"),
            "personalization": payload.personalization_enabled,
            "language": "tr",
            "consent_version": assistant["version"],
            "health_consent": bool(allowed_health and payload.personalization_enabled),
            "health_consent_version": health["version"]
            if allowed_health and payload.personalization_enabled
            else None,
            "client": {
                "platform": "android",
                **({"app_version": app_version} if app_version else {}),
            },
        }

    def response_for(reply):
        usage = chatbot_usage_service.get_or_create_today_usage(db, user_id)
        return ChatbotAskResponse(
            answer=reply.answer,
            conversation_id=reply.conversation_id,
            message_id=reply.message_id,
            disclaimer=reply.disclaimer,
            used_messages_today=usage.message_count,
            remaining_messages_today=max(daily_limit - usage.message_count, 0),
            is_premium=is_premium,
        )

    try:
        wire = make_wire(assistant_doc, health_doc, consent)
        fingerprint = request_fingerprint(payload, wire)
    except BridgeError as exc:
        return JSONResponse(
            status_code=exc.status, content={"error": exc.code, "message": exc.message}
        )
    daily_limit = _get_daily_limit(is_premium)
    chatbot_usage_service.recover_expired(db, user_id)
    existing = db.scalar(
        select(ChatbotRequest)
        .where(ChatbotRequest.user_id == user_id, ChatbotRequest.request_id == request_id)
        .execution_options(populate_existing=True)
    )
    if existing:
        if existing.input_hash is None:
            return JSONResponse(
                status_code=409,
                content={
                    "error": "request_in_progress"
                    if existing.status == "pending"
                    else "duplicate_request",
                    "message": "Bu isteğin güvenli tekrar kaydı mevcut değil.",
                },
            )
        if existing.input_hash != fingerprint:
            return JSONResponse(
                status_code=422,
                content={
                    "error": "request_conflict",
                    "message": "Aynı istek kimliği farklı içerikle kullanılamaz.",
                },
            )
        if existing.replay_until is None or existing.replay_until <= int(epoch_time.time()):
            return JSONResponse(
                status_code=409,
                content={
                    "error": "request_expired",
                    "message": "İsteğin tekrar süresi doldu. Yeni bir mesaj gönderebilirsiniz.",
                },
            )
        if existing.status == "complete":
            try:
                cached = PlatformChatbotReply.model_validate(existing.cached_reply)
            except ValidationError:
                return JSONResponse(
                    status_code=409,
                    content={
                        "error": "request_expired",
                        "message": "Kayıtlı yanıt doğrulanamadı. Yeni bir mesaj gönderin.",
                    },
                )
            return response_for(cached)
        if existing.status != "failed":
            return JSONResponse(
                status_code=409,
                headers={"Retry-After": "3"},
                content={
                    "error": "request_in_progress",
                    "message": "İstek hâlâ işleniyor. Aynı kimlikle daha sonra kontrol edebilirsiniz.",
                },
            )
        # Missing replay data must not silently rebuild a different platform body.
        saved = existing.platform_payload
        try:
            valid_saved = (
                isinstance(saved, dict)
                and saved.get("request_id") == request_id
                and saved.get("user_id") == user_id
                and all(
                    saved.get(key) == wire[key]
                    for key in (
                        "question",
                        "conversation_id",
                        "mode",
                        "tone",
                        "personalization",
                        "language",
                        "consent_version",
                        "health_consent",
                        "health_consent_version",
                    )
                )
                and request_fingerprint(payload, saved) == fingerprint
            )
        except (KeyError, TypeError, ValueError):
            valid_saved = False
        if not valid_saved:
            return JSONResponse(
                status_code=409,
                content={
                    "error": "request_expired",
                    "message": "İsteğin güvenli tekrar kaydı mevcut değil. Yeni bir mesaj gönderin.",
                },
            )
        wire = saved
    try:
        reservation = (
            chatbot_usage_service.retry_request(db, existing.id, user_id, daily_limit)
            if existing
            else chatbot_usage_service.reserve_request(db, user_id, daily_limit, request_id)
        )
    except ChatbotAccountUnavailable:
        return _deletion_response()
    except DuplicateChatbotRequest:
        return JSONResponse(
            status_code=409,
            headers={"Retry-After": "3"},
            content={
                "error": "request_in_progress",
                "message": "İstek hâlâ işleniyor. Aynı kimlikle yeniden kontrol edebilirsiniz.",
            },
        )
    if reservation is None:
        usage = chatbot_usage_service.get_or_create_today_usage(db, current_user.id)
        now = datetime.now(UTC)
        reset_at = chatbot_reset(usage.date)
        return JSONResponse(
            status_code=429,
            headers={"Retry-After": str(max(1, ceil((reset_at - now).total_seconds())))},
            content={
                "error": "daily_limit",
                "message": "Günlük asistan hakkınız doldu. Limit yenilendiğinde tekrar kullanabilirsiniz.",
                "used_messages_today": usage.message_count,
                "remaining_messages_today": 0,
                "reset_at": reset_at.isoformat(),
            },
        )

    if not existing:
        db.execute(
            update(ChatbotRequest)
            .where(ChatbotRequest.id == reservation)
            .values(
                input_hash=fingerprint,
                platform_payload=wire,
                replay_until=int(epoch_time.time()) + 86400,
            )
        )
        db.commit()

    try:
        # reserve_request commits. Reacquire the user row lock before network I/O;
        # prepare_deletion takes this same lock before committing its durable marker.
        if _deletion_pending(db, user_id, lock=True):
            chatbot_usage_service.finish_request(db, reservation, success=False)
            return _deletion_response()
        # Re-read after quota commit: a withdrawal must not use an earlier ORM snapshot.
        assistant_doc, health_doc = assistant_consent.current_documents()
        consent = assistant_consent.consent_state(db, current_user.id)
        if not assistant_consent.assistant_is_allowed(consent, assistant_doc) or (
            payload.consent_version is not None
            and payload.consent_version != assistant_doc["version"]
        ):
            chatbot_usage_service.finish_request(db, reservation, success=False)
            return JSONResponse(
                status_code=428,
                content={
                    "error": "consent_required",
                    "message": "Güncel asistan onayı gerekli.",
                    "consent": {
                        **assistant_consent.public_document(assistant_doc),
                        "health": assistant_consent.public_document(health_doc),
                    },
                },
            )
        profile = db.get(Profile, user_id, populate_existing=True)
        fresh_wire = make_wire(assistant_doc, health_doc, consent)
        if request_fingerprint(payload, fresh_wire) != fingerprint:
            chatbot_usage_service.finish_request(db, reservation, success=False)
            return JSONResponse(
                status_code=422,
                content={
                    "error": "request_conflict",
                    "message": "İstek hazırlanırken tercihler veya izinler değişti. Yeni mesaj gönderin.",
                },
            )
        reply = chatbot_bridge_client.ask(**wire)

    except BridgeError as exc:
        chatbot_usage_service.finish_request(db, reservation, success=False)
        return JSONResponse(
            status_code=exc.status,
            content={"error": exc.code, "message": exc.message},
            headers={"Retry-After": exc.retry_after}
            if exc.status == 429 and exc.retry_after
            else None,
        )
    except Exception:
        chatbot_usage_service.finish_request(db, reservation, success=False)
        raise
    if not chatbot_usage_service.finish_request(
        db, reservation, success=True, reply=reply.model_dump(mode="json")
    ):
        return JSONResponse(
            status_code=409, content={"error": "request_expired", "message": "İstek süresi doldu."}
        )
    return response_for(reply)


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


@router.post(
    "/ask/premium", response_model=ChatbotAskResponse, dependencies=[Depends(require_premium_user)]
)
def ask_chatbot_premium(
    payload: ChatbotAskRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    app_version: str | None = Header(None, alias="X-App-Version", max_length=40),
) -> ChatbotAskResponse | JSONResponse:
    return _ask(payload, current_user, db, True, app_version)


@router.get("/consent")
def get_assistant_consent(
    response: Response,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    response.headers["Cache-Control"] = "no-store"
    assistant, health = assistant_consent.current_documents()
    state = assistant_consent.consent_state(db, current_user.id)
    return {
        **assistant_consent.public_document(assistant),
        "health": assistant_consent.public_document(health),
        "accepted": assistant_consent.assistant_is_allowed(state, assistant),
        "health_review_required": bool(
            health
            and state
            and state.health_accepted
            and (state.health_version != health["version"] or state.health_hash != health["hash"])
        ),
        "health_accepted": bool(
            health
            and state
            and state.health_accepted
            and state.health_version == health["version"]
            and state.health_hash == health["hash"]
        ),
    }


@router.post("/consent")
def update_assistant_consent(
    payload: AssistantConsentUpdate,
    response: Response,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    response.headers["Cache-Control"] = "no-store"
    return assistant_consent.save_consent(db, current_user.id, payload)
