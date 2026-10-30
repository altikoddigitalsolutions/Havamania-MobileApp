from sqlalchemy import BigInteger, Boolean, ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class PushTokenBinding(Base):
    __tablename__ = "push_token_bindings"
    token_hash: Mapped[str] = mapped_column(String(64), primary_key=True)
    installation_id: Mapped[str] = mapped_column(String(36), nullable=False)
    revision: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), index=True
    )
    bound: Mapped[bool] = mapped_column(Boolean, nullable=False)
