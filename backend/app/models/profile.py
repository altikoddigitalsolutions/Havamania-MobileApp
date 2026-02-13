from sqlalchemy import ForeignKey, String
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base


class Profile(Base):
    __tablename__ = "profiles"

    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    primary_location_id: Mapped[str | None] = mapped_column(
        String(36), ForeignKey("locations.id", ondelete="SET NULL"), nullable=True
    )
    temperature_unit: Mapped[str] = mapped_column(String(8), default="C", nullable=False)
    wind_unit: Mapped[str] = mapped_column(String(16), default="kmh", nullable=False)
    theme: Mapped[str] = mapped_column(String(16), default="system", nullable=False)

    user = relationship("User")
    primary_location = relationship("Location", foreign_keys=[primary_location_id])
