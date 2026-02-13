import httpx

from app.core.config import get_settings
from app.core.observability import mark_chatbot_timeout

settings = get_settings()


class ChatbotBridgeClient:
    def __init__(self, base_url: str | None = None, timeout_seconds: int | None = None):
        self.base_url = base_url or settings.chatbot_base_url
        self.timeout_seconds = timeout_seconds or settings.chatbot_timeout_seconds

    def ask(self, question: str, user_id: str) -> str:
        payload = {"question": question, "user_id": user_id}

        for attempt in range(2):
            try:
                with httpx.Client(timeout=httpx.Timeout(self.timeout_seconds)) as client:
                    response = client.post(f"{self.base_url}/ask", json=payload)
                    response.raise_for_status()
                    data = response.json()
                    return str(data.get("answer", ""))
            except Exception:
                if attempt == 1:
                    mark_chatbot_timeout()
                    return "Yanıt gecikiyor. Lütfen tekrar deneyin."

        return "Yanıt alınamadı."


chatbot_bridge_client = ChatbotBridgeClient()
