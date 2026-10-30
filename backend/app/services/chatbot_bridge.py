import json
import logging

import httpx
from pydantic import ValidationError

from app.core.config import get_settings
from app.core.observability import mark_chatbot_timeout
from app.schemas.chatbot import PlatformChatbotReply

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

    def ask(self, question: str, user_id: str, context: dict | None = None, **metadata) -> PlatformChatbotReply:
        if not self._api_key:
            raise BridgeError(503, "not_configured", "Asistan bağlantısı henüz hazır değil.")
        payload = {"question": question, "user_id": user_id, "context": context or {}, **metadata}
        if len(json.dumps(payload, ensure_ascii=False).encode("utf-8")) > 32768:
            raise BridgeError(413, "too_large", "Asistan isteği çok uzun. Lütfen kısaltın.")
        for attempt in range(2):
            try:
                with httpx.Client(timeout=httpx.Timeout(self.timeout_seconds, connect=3),
                                  follow_redirects=False) as client:
                    response = client.post(f"{self.base_url}/ask", json=payload,
                                           headers={"Authorization": f"Bearer {self._api_key}"})
            except httpx.ConnectError as exc:
                if attempt == 0:
                    continue
                raise BridgeError(503, "unavailable", "Asistana şu anda ulaşılamıyor.") from exc
            except httpx.TimeoutException as exc:
                mark_chatbot_timeout()
                raise BridgeError(504, "timeout", "Asistan zamanında cevap veremedi.") from exc
            except httpx.RequestError as exc:
                raise BridgeError(503, "unavailable", "Asistana şu anda ulaşılamıyor.") from exc
            if response.status_code != 200:
                self._raise_error(response)
            try:
                reply = PlatformChatbotReply.model_validate(response.json())
            except (ValueError, ValidationError) as exc:
                raise BridgeError(502, "invalid_response", "Asistandan geçerli yanıt alınamadı.") from exc
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
        if status == 409 and code in {"mode_disabled", "conversation_conflict"}:
            # Never retry a conflict automatically: it can also indicate account deletion.
            raise BridgeError(409, code, "Bu sohbet kullanılamıyor. Yeni bir sohbet başlatabilirsiniz.")
        if status == 413:
            raise BridgeError(413, "too_large", "Mesaj veya bağlam çok uzun. Lütfen kısaltın.")
        if status == 400:
            raise BridgeError(400, "invalid_request", "Asistan isteği geçerli değil.")
        if status == 428:
            # Local consent metadata must be synchronized before requesting new acceptance.
            raise BridgeError(503, "consent_sync_required", "Asistan izin metni güncelleniyor. Daha sonra deneyin.")
        if status == 504:
            mark_chatbot_timeout()
            raise BridgeError(504, "timeout", "Asistan zamanında cevap veremedi.")
        raise BridgeError(503, "unavailable", "Asistan şu anda cevap veremiyor.")


chatbot_bridge_client = ChatbotBridgeClient()
