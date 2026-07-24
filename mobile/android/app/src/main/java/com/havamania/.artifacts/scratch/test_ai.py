import requests
import json

bot_id = "6724b94f6f1c48010ba457c1"
url = f"https://chatbot.altikodtech.com.tr/api/widget/{bot_id}/chat"

payload = {
    "question": "Bugün hava nasıl?",
    "session_id": "test_session_123"
}

headers = {
    "Content-Type": "application/json"
}

try:
    response = requests.post(url, json=payload, headers=headers, timeout=10)
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.text}")
except Exception as e:
    print(f"Error: {e}")
