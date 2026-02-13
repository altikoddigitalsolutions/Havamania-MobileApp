from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.config import get_settings
from app.db.session import get_db
from app.dependencies.auth import get_current_user
from app.dependencies.premium import require_premium_user
from app.models.user import User
from app.schemas.chatbot import ChatbotAskRequest, ChatbotAskResponse, ChatbotUsageResponse
from app.services.chatbot_bridge import chatbot_bridge_client
from app.services.chatbot_usage import chatbot_usage_service
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
) -> ChatbotAskResponse:
    is_premium = _is_premium_user(current_user, db)
    usage = chatbot_usage_service.get_or_create_today_usage(db, current_user.id)
    daily_limit = _get_daily_limit(is_premium)

    if usage.message_count >= daily_limit:
        return ChatbotAskResponse(
            answer="Günlük limit doldu. Premium ile daha fazla kullanım açılabilir.",
            used_messages_today=usage.message_count,
            remaining_messages_today=0,
            is_premium=is_premium,
        )

    answer = chatbot_bridge_client.ask(question=payload.question, user_id=current_user.id)
    usage = chatbot_usage_service.increment_message(db, usage)

    return ChatbotAskResponse(
        answer=answer,
        used_messages_today=usage.message_count,
        remaining_messages_today=max(daily_limit - usage.message_count, 0),
        is_premium=is_premium,
    )


@router.get("/usage", response_model=ChatbotUsageResponse)
def chatbot_usage(
    current_user: User = Depends(get_current_user), db: Session = Depends(get_db)
) -> ChatbotUsageResponse:
    is_premium = _is_premium_user(current_user, db)
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
) -> ChatbotAskResponse:
    usage = chatbot_usage_service.get_or_create_today_usage(db, current_user.id)
    daily_limit = settings.chatbot_premium_daily_limit
    answer = chatbot_bridge_client.ask(question=payload.question, user_id=current_user.id)
    usage = chatbot_usage_service.increment_message(db, usage)
    return ChatbotAskResponse(
        answer=answer,
        used_messages_today=usage.message_count,
        remaining_messages_today=max(daily_limit - usage.message_count, 0),
        is_premium=True,
    )
