from datetime import datetime
from uuid import uuid4

from sqlalchemy import DateTime, JSON, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class WeatherSnapshot(Base):
    __tablename__ = "weather_snapshots"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    location_key: Mapped[str] = mapped_column(String(128), index=True, nullable=False)
    provider: Mapped[str] = mapped_column(String(64), nullable=False)
    payload_jsonb: Mapped[dict] = mapped_column(JSON, nullable=False)
    fetched_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
