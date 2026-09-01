import urllib.request
import json

url = "https://chatbot.altikodtech.com.tr/api/widget/6/chat"
data = {
    "question": "Ankara'da hava nasıl?\n\n---\nContext Information:\n- Current time: 2026-09-01T10:00:00\n- Weather in Balıkesir: 29C, Az Bulutlu\n---",
    "session_id": "test_context_session"
}
json_data = json.dumps(data).encode('utf-8')
req = urllib.request.Request(url, data=json_data, headers={'Content-Type': 'application/json'}, method='POST')

try:
    with urllib.request.urlopen(req, timeout=10) as response:
        print(f"Status: {response.getcode()}")
        print(f"Body: {response.read().decode('utf-8')}")
except urllib.error.HTTPError as e:
    print(f"FAILED: {e.code} {e.reason}")
    print(f"Details: {e.read().decode('utf-8')}")
except Exception as e:
    print(f"Error: {e}")
