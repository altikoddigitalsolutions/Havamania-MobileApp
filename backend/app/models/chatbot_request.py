from sqlalchemy import BigInteger, ForeignKey, String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ChatbotRequest(Base):
    __tablename__ = "chatbot_requests"
    __table_args__ = (UniqueConstraint("user_id", "request_id", name="uq_chatbot_request_owner"),)

    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id", ondelete="CASCADE"))
    request_id: Mapped[str] = mapped_column(String(36))
    usage_id: Mapped[str] = mapped_column(String(36), ForeignKey("chatbot_usage_daily.id", ondelete="CASCADE"))
    status: Mapped[str] = mapped_column(String(16))
    expires_at: Mapped[int] = mapped_column(BigInteger, index=True)
