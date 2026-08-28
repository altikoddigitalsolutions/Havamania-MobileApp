import urllib.request
import json

base_url = "https://chatbot.altikodtech.com.tr/api/widget/{id}/config"

for i in range(1, 11):
    url = base_url.replace("{id}", str(i))
    try:
        with urllib.request.urlopen(url, timeout=2) as response:
            data = json.loads(response.read().decode('utf-8'))
            print(f"ID {i}: {data.get('name')}")
    except:
        pass
