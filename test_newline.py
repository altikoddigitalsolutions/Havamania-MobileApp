import urllib.request
import json

url = "https://chatbot.altikodtech.com.tr/api/widget/6/chat"
data = {
    "question": "Ankara hava durumu\n\nŞu anki konum: Balıkesir",
    "session_id": "test_newline_session"
}
json_data = json.dumps(data).encode('utf-8')
req = urllib.request.Request(url, data=json_data, headers={'Content-Type': 'application/json'}, method='POST')

try:
    with urllib.request.urlopen(req, timeout=10) as response:
        print(f"Status: {response.getcode()}")
        print(f"Body: {response.read().decode('utf-8')}")
except Exception as e:
    print(f"Error: {e}")
