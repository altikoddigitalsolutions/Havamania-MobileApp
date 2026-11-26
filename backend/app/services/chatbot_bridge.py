import json
import logging
from uuid import UUID, uuid4

import httpx
from pydantic import ValidationError

from app.core.config import get_settings
from app.core.observability import mark_chatbot_timeout
from app.schemas.chatbot import PlatformChatbotReply, AssistantAttachmentReceipt

settings = get_settings()
logger = logging.getLogger(__name__)


class BridgeError(Exception):
    """Safe public error; no upstream body, credentials or profile content."""

    def __init__(self, status: int, code: str, message: str, retry_after: str | None = None):
        super().__init__(message)
        self.status, self.code, self.message = status, code, message
        self.retry_after = retry_after


class ChatbotBridgeClient:
    def __init__(self, base_url: str | None = None, timeout_seconds: int | None = None,
                 api_key: str | None = None):
        self.base_url = (base_url or settings.chatbot_base_url).rstrip("/")
        self.timeout_seconds = timeout_seconds or settings.chatbot_timeout_seconds
        self._api_key = api_key if api_key is not None else (
            settings.chatbot_api_key.get_secret_value() if settings.chatbot_api_key else "")

    def upload(self, content: bytes, media_type: str, user_id: str, consent_version: int,
               app_version: str | None = None) -> AssistantAttachmentReceipt:
        if not self._api_key:
            raise BridgeError(503, "not_configured", "Asistan bağlantısı henüz hazır değil.")
        if not content or len(content) > 8 * 1024 * 1024:
            raise BridgeError(413, "too_large", "Fotoğraf en fazla 8 MB olabilir.")
        data = {"user_id": user_id, "consent_version": str(consent_version), "platform": "android"}
        if app_version:
            data["app_version"] = app_version
        try:
            # Never spool to disk or retry an ambiguous upload; it is not idempotent.
            with httpx.Client(timeout=httpx.Timeout(30, connect=5), follow_redirects=False) as client:
                response = client.post(f"{self.base_url}/upload", data=data,
                    files={"file": ("photo", content, media_type)},
                    headers={"Authorization": f"Bearer {self._api_key}"})
        except httpx.TimeoutException:
            raise BridgeError(504, "timeout", "Fotoğraf yüklemesi zamanında tamamlanamadı.") from None
        except httpx.RequestError:
            raise BridgeError(503, "unavailable", "Fotoğraf yüklenemedi. Daha sonra deneyin.") from None
        if response.status_code != 201:
            # Upstream deployment/key failures are not Firebase-session failures.
            # Preserve a safe diagnosis without exposing upstream response text.
            status = response.status_code
            if status in {401, 403}:
                logger.error("Assistant photo bridge credential rejected http_status=%s", status)
                raise BridgeError(503, "bridge_auth_failed", "Fotoğraf hizmetinin bağlantı ayarları doğrulanamadı.")
            if status in {404, 405, 501}:
                logger.error("Assistant photo upload endpoint unavailable http_status=%s", status)
                raise BridgeError(503, "upload_endpoint_unavailable", "Fotoğraf yükleme hizmeti sunucuda kullanılamıyor.")
            if status == 503:
                try:
                    error_body = response.json()
                except ValueError:
                    error_body = None
                if isinstance(error_body, dict) and error_body.get("error") == "not_configured":
                    raise BridgeError(503, "not_configured", "Fotoğraf hizmeti sunucuda henüz hazır değil.")
            self._raise_error(response)
        try:
            return AssistantAttachmentReceipt.model_validate(response.json())
        except (ValueError, ValidationError):
            raise BridgeError(502, "invalid_response", "Fotoğraf yüklemesi doğrulanamadı.") from None

    def delete_user(self, user_id: str) -> None:
        """Idempotent endpoint; retry belongs to the durable account deletion job."""
        if not self._api_key:
            raise BridgeError(503, "not_configured", "Asistan bağlantısı henüz hazır değil.")
        try:
            with httpx.Client(timeout=httpx.Timeout(10, connect=3), follow_redirects=False) as client:
                response = client.post(f"{self.base_url}/delete-user", json={"user_id": user_id},
                                       headers={"Authorization": f"Bearer {self._api_key}"})
            if response.status_code != 200:
                raise ValueError("Deletion not confirmed")
            body = response.json()
            if not isinstance(body, dict) or any(
                type(body.get(field)) is not int or body[field] < 0
                for field in ("deleted_conversations", "deleted_photos")
            ):
                raise ValueError("Invalid deletion receipt")
        except (httpx.RequestError, ValueError):
            # No chained transport exception: deletion worker logs failures, and upstream
            # errors may include credentials or sensitive response bodies.
            raise BridgeError(503, "deletion_pending", "Asistan verilerinin silinmesi yeniden denenecek.") from None

    def ask(self, question: str, user_id: str, context: dict | None = None, **metadata) -> PlatformChatbotReply:
        if not self._api_key:
            raise BridgeError(503, "not_configured", "Asistan bağlantısı henüz hazır değil.")
        if metadata.get("personalization") is False:
            context = {key: value for key, value in (context or {}).items() if key not in {"profile", "trips"}}
            metadata.update(health_consent=False, health_consent_version=None)
        payload = {"question": question, "user_id": user_id, "context": context or {}, **metadata}
        try:
            payload["request_id"] = str(UUID(str(payload["request_id"]))) if payload.get("request_id") else str(uuid4())
        except (ValueError, TypeError, AttributeError):
            raise BridgeError(400, "invalid_request", "İstek kimliği geçerli değil.") from None
        if len(json.dumps(payload, ensure_ascii=False).encode("utf-8")) > 32768:
            raise BridgeError(413, "too_large", "Asistan isteği çok uzun. Lütfen kısaltın.")
        for attempt in range(2):
            try:
                with httpx.Client(timeout=httpx.Timeout(self.timeout_seconds, connect=3),
                                  follow_redirects=False) as client:
                    response = client.post(f"{self.base_url}/ask", json=payload,
                                           headers={"Authorization": f"Bearer {self._api_key}"})
            except httpx.ConnectError:
                if attempt == 0:
                    continue
                raise BridgeError(503, "unavailable", "Asistana şu anda ulaşılamıyor.") from None
            except httpx.TimeoutException:
                mark_chatbot_timeout()
                raise BridgeError(504, "timeout", "Asistan zamanında cevap veremedi.") from None
            except httpx.RequestError:
                raise BridgeError(503, "unavailable", "Asistana şu anda ulaşılamıyor.") from None
            if response.status_code != 200:
                self._raise_error(response)
            try:
                reply = PlatformChatbotReply.model_validate(response.json())
            except (ValueError, ValidationError):
                raise BridgeError(502, "invalid_response", "Asistandan geçerli yanıt alınamadı.") from None
            conversation = metadata.get("conversation_id")
            if conversation and str(reply.conversation_id) != conversation:
                raise BridgeError(502, "invalid_response", "Asistan sohbet kimliği doğrulanamadı.")
            if metadata.get("mode") in {"agro", "fly"} and not (reply.disclaimer or "").strip():
                raise BridgeError(502, "invalid_response", "Asistan güvenlik uyarısı alınamadı.")
            return reply
        raise AssertionError("Unreachable bridge retry state")

    @staticmethod
    def _raise_error(response):
        try:
            body = response.json()
            code = body.get("error") if isinstance(body, dict) else None
        except ValueError:
            code = None
        status = response.status_code
        if status in {401, 403}:
            logger.error("Assistant bridge credential rejected")
        if status == 429:
            retry = response.headers.get("Retry-After", "")
            retry = retry if retry.isascii() and retry.isdigit() and len(retry) <= 5 and 1 <= int(retry) <= 86400 else None
            daily = code == "daily_limit"
            raise BridgeError(429, "daily_limit" if daily else "rate_limited",
                              "Günlük asistan sınırı doldu." if daily else "Lütfen kısa bir süre bekleyin.", retry)
        if status == 409 and code in {"photos_disabled", "mode_disabled", "conversation_conflict", "user_deleted", "request_in_progress"}:
            messages = {"photos_disabled": "Fotoğraf gönderme şu anda kullanılamıyor.", "mode_disabled": "Bu asistan modu şu anda kullanılamıyor.",
                "conversation_conflict": "Sohbet sahipliği veya modu uyuşmuyor. Yeni sohbet başlatın.",
                "user_deleted": "Bu hesap için asistan erişimi silme nedeniyle kapalı.",
                "request_in_progress": "İstek hâlâ işleniyor. Aynı istek kimliğiyle daha sonra kontrol edin."}
            raise BridgeError(409, code, messages[code])
        if status == 503 and code == "busy":
            retry = response.headers.get("Retry-After", "5")
            retry = retry if retry.isascii() and retry.isdigit() and len(retry) <= 5 and 1 <= int(retry) <= 86400 else "5"
            raise BridgeError(503, "busy", "Fotoğraf hizmeti yoğun. Daha sonra deneyin.", retry)
        if status == 415:
            raise BridgeError(415, "unsupported_media_type", "Bu fotoğraf biçimi desteklenmiyor.")
        if status == 422:
            safe_code = code if code in {"request_id_reused", "request_conflict", "request_id_conflict", "request_mismatch", "request_id_mismatch", "invalid_request"} else "request_conflict"
            raise BridgeError(422, safe_code, "İstek içeriği veya istek kimliği doğrulanamadı. Yeni mesaj gönderebilirsiniz.")
        if status == 413:
            raise BridgeError(413, "too_large", "Mesaj veya bağlam çok uzun. Lütfen kısaltın.")
        if status == 400:
            raise BridgeError(400, "invalid_request", "Asistan isteği geçerli değil.")
        if status == 428:
            from app.services.platform_consent import platform_consent_provider
            platform_consent_provider.invalidate()
            # Local consent metadata must be synchronized before requesting new acceptance.
            raise BridgeError(503, "consent_sync_required", "Asistan izin metni güncelleniyor. Daha sonra deneyin.")
        if status == 504:
            mark_chatbot_timeout()
            raise BridgeError(504, "timeout", "Asistan zamanında cevap veremedi.")
        raise BridgeError(503, "unavailable", "Asistan şu anda cevap veremiyor.")


chatbot_bridge_client = ChatbotBridgeClient()
