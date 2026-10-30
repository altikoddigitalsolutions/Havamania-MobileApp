import logging
from datetime import UTC, datetime
from hashlib import sha256
from uuid import uuid4

from fastapi import HTTPException
from sqlalchemy import Connection, select, update
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.dialects.sqlite import insert as sqlite_insert
from sqlalchemy.orm import Session

from app.models.alert import Alert, AlertSeverity
from app.models.location import Location
from app.models.notification_preference import NotificationPreference
from app.models.push_delivery import PushDelivery
from app.models.push_token import PushToken
from app.services.account_access import require_active_account
from app.services.push_provider import FCMApnsPushProvider, PushProvider


class PushService:
    def __init__(self, provider: PushProvider | None = None):
        self.provider = provider or FCMApnsPushProvider()

    def send_critical_alert(self, db: Session, alert: Alert) -> int:
        # Input alerts must already be committed. A dedicated session keeps the
        # pre-dispatch claim durable without committing the caller's unrelated work.
        bind = db.get_bind()
        engine = bind.engine if isinstance(bind, Connection) else bind
        with Session(engine, expire_on_commit=False) as delivery_db:
            return self._send_critical_alert(delivery_db, alert.id)

    def _send_critical_alert(self, db: Session, alert_id: str) -> int:
        # Resolve queued IDs again; deleted/changed records must not use cached payloads.
        alert = db.get(Alert, alert_id, populate_existing=True)
        if not self._is_current_critical(alert):
            return 0
        location = db.get(Location, alert.location_id, populate_existing=True)
        if not location:
            return 0
        try:
            # Serialize the claim with deletion acceptance.
            require_active_account(db, location.user_id)
        except HTTPException as exc:
            if exc.status_code in {401, 409}:
                return 0
            raise

        prefs = db.get(NotificationPreference, location.user_id, populate_existing=True)
        if prefs and not prefs.severe_alert_enabled:
            return 0

        owner = location.user_id
        tokens = [
            (t.id, t.token) for t in db.query(PushToken).filter(PushToken.user_id == owner).all()
        ]
        sent = 0
        for token_id, token_value in tokens:
            try:
                require_active_account(db, owner)
            except HTTPException:
                db.rollback()
                break
            delivery_id = str(uuid4())
            insert = {"postgresql": pg_insert, "sqlite": sqlite_insert}[db.get_bind().dialect.name]
            claimed = db.scalar(
                insert(PushDelivery)
                .values(
                    id=delivery_id,
                    alert_id=alert_id,
                    user_id=owner,
                    token_hash=sha256(token_value.encode()).hexdigest(),
                    status="reserved",
                )
                .on_conflict_do_nothing(index_elements=["alert_id", "user_id", "token_hash"])
                .returning(PushDelivery.id)
            )
            db.commit()  # Must precede external delivery, including process crashes.
            if claimed is None:
                continue
            status = "cancelled"
            try:
                # Claim commit releases locks: recheck deletion, ownership, validity
                # and preferences, then retain user + token locks through dispatch.
                require_active_account(db, owner)
                token = db.scalar(
                    select(PushToken)
                    .where(
                        PushToken.id == token_id,
                        PushToken.user_id == owner,
                        PushToken.token == token_value,
                    )
                    .with_for_update()
                )
                current = db.get(Alert, alert_id, populate_existing=True)
                prefs = db.get(NotificationPreference, owner, populate_existing=True)
                if (
                    token
                    and self._is_current_critical(current)
                    and (not prefs or prefs.severe_alert_enabled)
                ):
                    status = "unknown"
                    ok = self.provider.send(
                        platform=token.platform,
                        token=token.token,
                        title=f"Kritik Hava Uyarısı: {current.title}",
                        body=current.description,
                    )
                    if ok:
                        status = "sent"
                        sent += 1
            except HTTPException:
                pass
            except Exception:  # noqa: BLE001 -- ambiguous provider failure must never replay automatically
                status = "unknown"
                logging.getLogger(__name__).error(
                    "Push dispatch outcome unknown; automatic replay suppressed"
                )
            db.execute(
                update(PushDelivery).where(PushDelivery.id == delivery_id).values(status=status)
            )
            db.commit()
        return sent

    @staticmethod
    def _is_current_critical(alert):
        if alert is None or alert.severity != AlertSeverity.CRITICAL:
            return False
        start = (
            alert.starts_at.replace(tzinfo=UTC)
            if alert.starts_at.tzinfo is None
            else alert.starts_at
        )
        end = alert.ends_at.replace(tzinfo=UTC) if alert.ends_at.tzinfo is None else alert.ends_at
        return start <= datetime.now(UTC) < end


push_service = PushService()
