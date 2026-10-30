import time
from uuid import uuid4

from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.chatbot_request import ChatbotRequest
from app.models.chatbot_usage_daily import ChatbotUsageDaily
from app.models.user import User
from app.services.chatbot_clock import chatbot_day


class ChatbotUsageService:
    def get_or_create_today_usage(self, db: Session, user_id: str) -> ChatbotUsageDaily:
        today = chatbot_day()
        usage = (
            db.query(ChatbotUsageDaily)
            .filter(ChatbotUsageDaily.user_id == user_id, ChatbotUsageDaily.date == today)
            .first()
        )
        if usage:
            return usage

        usage = ChatbotUsageDaily(user_id=user_id, date=today, message_count=0, token_count=0)
        db.add(usage)
        try:
            db.commit()
        except IntegrityError:
            # Another request may create the unique user/day row first.
            db.rollback()
            usage = (
                db.query(ChatbotUsageDaily)
                .filter(ChatbotUsageDaily.user_id == user_id, ChatbotUsageDaily.date == today)
                .one()
            )
        db.refresh(usage)
        return usage

    def recover_expired(self, db: Session, user_id: str) -> None:
        expired = db.scalars(
            select(ChatbotRequest.id).where(
                ChatbotRequest.user_id == user_id,
                ChatbotRequest.status == "pending",
                ChatbotRequest.expires_at <= int(time.time()),
            )
        ).all()
        for request_id in expired:
            self.finish_request(db, request_id, success=False, expired_only=True)

    def reserve_request(self, db: Session, user_id: str, limit: int, request_id: str) -> str | None:
        self.recover_expired(db, user_id)
        usage = self.get_or_create_today_usage(db, user_id)
        usage_id = usage.id
        # get_or_create may commit: acquire the user lock before inserting a
        # unique request/FK, avoiding a unique-key vs user-row deadlock on replay.
        if db.scalar(select(User.id).where(User.id == user_id).with_for_update()) is None:
            raise ChatbotAccountUnavailable
        record_id = str(uuid4())
        db.add(
            ChatbotRequest(
                id=record_id,
                user_id=user_id,
                request_id=request_id,
                usage_id=usage_id,
                status="pending",
                expires_at=int(time.time()) + 300,
            )
        )
        try:
            db.flush()
        except IntegrityError:
            db.rollback()
            existing = db.scalar(
                select(ChatbotRequest.id).where(
                    ChatbotRequest.user_id == user_id, ChatbotRequest.request_id == request_id
                )
            )
            if existing is None:
                raise
            raise DuplicateChatbotRequest from None
        result = db.execute(
            update(ChatbotUsageDaily)
            .where(ChatbotUsageDaily.id == usage_id, ChatbotUsageDaily.message_count < limit)
            .values(message_count=ChatbotUsageDaily.message_count + 1)
            .execution_options(synchronize_session=False)
        )
        if result.rowcount != 1:
            db.rollback()
            return None
        db.commit()
        return record_id

    def retry_request(self, db: Session, record_id: str, user_id: str, limit: int) -> str | None:
        usage = self.get_or_create_today_usage(db, user_id)
        usage_id = usage.id
        if db.scalar(select(User.id).where(User.id == user_id).with_for_update()) is None:
            raise ChatbotAccountUnavailable
        claimed = db.scalar(
            update(ChatbotRequest)
            .where(
                ChatbotRequest.id == record_id,
                ChatbotRequest.user_id == user_id,
                ChatbotRequest.status == "failed",
            )
            .values(status="pending", usage_id=usage_id, expires_at=int(time.time()) + 300)
            .returning(ChatbotRequest.id)
            .execution_options(synchronize_session=False)
        )
        if claimed is None:
            db.rollback()
            raise DuplicateChatbotRequest
        result = db.execute(
            update(ChatbotUsageDaily)
            .where(ChatbotUsageDaily.id == usage_id, ChatbotUsageDaily.message_count < limit)
            .values(message_count=ChatbotUsageDaily.message_count + 1)
            .execution_options(synchronize_session=False)
        )
        if result.rowcount != 1:
            db.rollback()
            return None
        db.commit()
        return record_id

    def finish_request(
        self,
        db: Session,
        record_id: str,
        success: bool,
        expired_only: bool = False,
        reply: dict | None = None,
    ) -> bool:
        filters = [ChatbotRequest.id == record_id, ChatbotRequest.status == "pending"]
        if expired_only:
            filters.append(ChatbotRequest.expires_at <= int(time.time()))
        values = {"status": "complete" if success else "failed"}
        if success and reply is not None:
            values.update(cached_reply=reply, platform_payload=None)
        usage_id = db.scalar(
            update(ChatbotRequest)
            .where(*filters)
            .values(**values)
            .returning(ChatbotRequest.usage_id)
            .execution_options(synchronize_session=False)
        )
        if usage_id is not None and not success:
            db.execute(
                update(ChatbotUsageDaily)
                .where(ChatbotUsageDaily.id == usage_id, ChatbotUsageDaily.message_count > 0)
                .values(message_count=ChatbotUsageDaily.message_count - 1)
                .execution_options(synchronize_session=False)
            )
        db.commit()
        return usage_id is not None


class DuplicateChatbotRequest(Exception):
    pass


class ChatbotAccountUnavailable(Exception):
    pass


chatbot_usage_service = ChatbotUsageService()
