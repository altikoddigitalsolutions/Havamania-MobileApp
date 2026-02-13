from datetime import UTC, datetime

from sqlalchemy.orm import Session

from app.models.chatbot_usage_daily import ChatbotUsageDaily


class ChatbotUsageService:
    def get_or_create_today_usage(self, db: Session, user_id: str) -> ChatbotUsageDaily:
        today = datetime.now(UTC).date()
        usage = (
            db.query(ChatbotUsageDaily)
            .filter(ChatbotUsageDaily.user_id == user_id, ChatbotUsageDaily.date == today)
            .first()
        )
        if usage:
            return usage

        usage = ChatbotUsageDaily(user_id=user_id, date=today, message_count=0, token_count=0)
        db.add(usage)
        db.commit()
        db.refresh(usage)
        return usage

    def increment_message(self, db: Session, usage: ChatbotUsageDaily, token_count: int = 0) -> ChatbotUsageDaily:
        usage.message_count += 1
        usage.token_count += max(token_count, 0)
        db.add(usage)
        db.commit()
        db.refresh(usage)
        return usage


chatbot_usage_service = ChatbotUsageService()
