from sqlalchemy import Index, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class AccountDeletion(Base):
    __tablename__ = "account_deletions"

    # Only a SHA-256 digest of the client's 256-bit continuation secret is stored.
    key_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    firebase_uid: Mapped[str | None] = mapped_column(String(128), nullable=True)
    project_id: Mapped[str | None] = mapped_column(String(255), nullable=True)
    storage_bucket: Mapped[str | None] = mapped_column(String(255), nullable=True)
    backend_user_id: Mapped[str | None] = mapped_column(String(36), nullable=True)
    platform_base_url: Mapped[str | None] = mapped_column(String(2048), nullable=True)
    status: Mapped[str] = mapped_column(String(16), default="pending", nullable=False)
    __table_args__ = (Index("ix_account_deletions_user_status", "backend_user_id", "status"),)
