import json
import logging
from datetime import UTC, datetime, timedelta
from uuid import UUID

import firebase_admin
from firebase_admin import messaging

from app.core.config import get_settings

logger = logging.getLogger(__name__)


class PushProvider:
    def send(self, platform: str, token: str, title: str, body: str, **metadata) -> bool:
        raise NotImplementedError


def owned_push_data(title, body, owner_user_id, event_id, expires_at):
    try:
        if str(UUID(owner_user_id)) != owner_user_id or str(UUID(event_id)) != event_id:
            return None
    except (ValueError, TypeError, AttributeError):
        return None
    if not isinstance(expires_at, datetime):
        return None
    now = datetime.now(UTC)
    end = expires_at.replace(tzinfo=UTC) if expires_at.tzinfo is None else expires_at
    end_seconds = int(min(end, now + timedelta(days=1)).timestamp())
    if end_seconds <= int(now.timestamp()) or not title.strip() or not body.strip():
        return None
    # Match Kotlin String.length (UTF-16 units), including emoji/non-BMP text.
    title = title.encode("utf-16-le", errors="replace")[:400].decode("utf-16-le", errors="ignore")
    body = body.encode("utf-16-le", errors="replace")[:2000].decode("utf-16-le", errors="ignore")
    if any(ord(c) < 32 or ord(c) == 127 for c in title) or "\0" in body:
        return None
    data = {"protocol": "1", "owner_backend_user_id": owner_user_id, "event_id": event_id,
            "expires_at": str(end_seconds), "title": title, "body": body, "target": "weather"}
    return data if len(json.dumps(data, ensure_ascii=False).encode()) <= 4096 else None


class FCMApnsPushProvider(PushProvider):
    """Real FCM push provider implementation using firebase-admin SDK."""

    @property
    def owner_protocol_enabled(self):
        return get_settings().fcm_owner_protocol_enabled

    def _init_firebase(self):
        try:
            firebase_admin.get_app()
            return True
        except ValueError:
            try:
                firebase_admin.initialize_app()
                return True
            except Exception:  # noqa: BLE001 -- credential details must not enter logs
                # Another worker may have initialized the default between our check
                # and initialize_app. A named identity app alone does not qualify.
                try:
                    firebase_admin.get_app()
                    return True
                except ValueError:
                    logger.error("Firebase Admin default initialization failed")
                    return False

    def send(self, platform: str, token: str, title: str, body: str, **metadata) -> bool:
        if not self.owner_protocol_enabled:
            return False
        data = owned_push_data(title, body, metadata.get("owner_user_id"), metadata.get("event_id"), metadata.get("expires_at"))
        if data is None:
            return False
        if platform.lower() != "android":
            logger.warning(f"Push platform '{platform}' not supported for FCM delivery.")
            return False

        if not token or not token.strip():
            return False

        try:
            if not self._init_firebase():
                return False

            message = messaging.Message(
                data=data,
                token=token,
                android=messaging.AndroidConfig(priority="high", ttl=timedelta(
                    seconds=max(0, int(data["expires_at"]) - int(datetime.now(UTC).timestamp())))),
            )
            messaging.send(message)
            logger.info("FCM provider accepted owner-bound data message")
            return True
        except Exception:  # noqa: BLE001 -- SDK failure may be ambiguous; never expose transport details
            logger.error("FCM dispatch failed; result may be unknown")
            return False
