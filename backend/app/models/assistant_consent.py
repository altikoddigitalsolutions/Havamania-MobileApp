from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class ConsentFields:
    assistant_accepted: Mapped[bool] = mapped_column(Boolean, nullable=False)
    assistant_version: Mapped[int | None] = mapped_column(Integer)
    assistant_hash: Mapped[str | None] = mapped_column(String(64))
    health_accepted: Mapped[bool] = mapped_column(Boolean, nullable=False)
    health_version: Mapped[int | None] = mapped_column(Integer)
    health_hash: Mapped[str | None] = mapped_column(String(64))
    recorded_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)


class AssistantConsent(ConsentFields, Base):
    __tablename__ = "assistant_consents"
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True)


class AssistantConsentEvent(ConsentFields, Base):
    __tablename__ = "assistant_consent_events"
    id: Mapped[str] = mapped_column(String(36), primary_key=True)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id", ondelete="CASCADE"), index=True)
