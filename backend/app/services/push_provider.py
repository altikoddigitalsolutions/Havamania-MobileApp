import logging

import firebase_admin
from firebase_admin import messaging

logger = logging.getLogger(__name__)


class PushProvider:
    def send(self, platform: str, token: str, title: str, body: str) -> bool:
        raise NotImplementedError


class FCMApnsPushProvider(PushProvider):
    """Real FCM push provider implementation using firebase-admin SDK."""

    def __init__(self):
        self._init_firebase()

    def _init_firebase(self):
        if not firebase_admin._apps:
            try:
                firebase_admin.initialize_app()
            except Exception:
                logger.exception("Firebase Admin default initialization failed")

    def send(self, platform: str, token: str, title: str, body: str) -> bool:
        if platform.lower() != "android":
            logger.warning(f"Push platform '{platform}' not supported for FCM delivery.")
            return False

        if not token or not token.strip():
            return False

        try:
            if not firebase_admin._apps:
                logger.error("Firebase Admin SDK is not initialized. Cannot send push notification.")
                return False

            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                token=token,
            )
            response = messaging.send(message)
            logger.info(f"Successfully sent FCM message: {response}")
            return True
        except Exception:
            logger.exception("Failed to send push notification via FCM")
            return False
