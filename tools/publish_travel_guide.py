"""Validate editorial guide JSON; publish only with explicit --publish and ADC credentials."""
import argparse
import hashlib
import json
import time
from pathlib import Path
from urllib.parse import urlparse

TRIP_TYPES = set("BUSINESS VACATION FAMILY SPORTS CAMPING CULTURE NATURE ROMANTIC GASTRONOMY BEACH WINTER ADVENTURE PHOTOGRAPHY SHOPPING WEEKEND HEALTH EVENT ROAD_TRIP OTHER".split())


def key(value):
    return value.strip().replace("İ", "i").lower().translate(str.maketrans("çğıöşü", "cgiosu"))


def validate(data):
    city = key(data["city"])
    district = key(data.get("district") or "")
    if district == city:
        district = ""
    if not city or type(data["version"]) is not int or data["version"] <= 0:
        raise ValueError("City/version is invalid")
    if data["validUntilEpochMs"] <= int(time.time() * 1000):
        raise ValueError("Guide has expired")
    for field in ("description", "visit", "food", "experience"):
        if not isinstance(data[field], str) or not data[field].strip() or len(data[field]) > 4000:
            raise ValueError(f"Invalid {field}")
    if not 1 <= len(data["experiences"]) <= 12:
        raise ValueError("Expected 1-12 experiences")
    for item in data["experiences"]:
        if any(value not in TRIP_TYPES for value in item.get("tripTypes", [])):
            raise ValueError("Unknown trip type")
        for field, limit in (("title", 160), ("category", 80), ("description", 2000)):
            if not item[field].strip() or len(item[field]) > limit:
                raise ValueError(f"Invalid experience {field}")
        parsed = urlparse(item["sourceUrl"])
        host = parsed.hostname or ""
        if (parsed.scheme != "https" or parsed.username or parsed.port not in (None, 443)
                or len(item["sourceUrl"]) > 2048
                or not (host == "goturkiye.com" or host.endswith((".gov.tr", ".bel.tr")))):
            raise ValueError("Expected an official HTTPS source")
    payload = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    if len(payload) > 64000:
        raise ValueError("Payload exceeds 64000 characters")
    return hashlib.sha256(f"{city}|{district}".encode()).hexdigest(), payload


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("file", type=Path)
    parser.add_argument("--publish", action="store_true")
    parser.add_argument("--project", help="Required explicit Firebase project for publishing")
    args = parser.parse_args()
    data = json.loads(args.file.read_text(encoding="utf-8-sig"))
    document_id, payload = validate(data)
    print(f"travel_guides/{document_id} version={data['version']}")
    if not args.publish:
        print("Validation only; nothing published.")
        return
    if not args.project:
        parser.error("--project is required with --publish")
    import firebase_admin
    from firebase_admin import firestore

    app = firebase_admin.initialize_app(options={"projectId": args.project})
    db = firestore.client(app)
    ref = db.collection("travel_guides").document(document_id)

    @firestore.transactional
    def publish(transaction):
        previous = ref.get(transaction=transaction)
        if previous.exists:
            old = json.loads(previous.to_dict().get("payload", "{}"))
            if old.get("version", 0) >= data["version"]:
                raise ValueError("Version must increase")
        transaction.set(ref, {"payload": payload})

    publish(db.transaction())
    print("Published.")


if __name__ == "__main__":
    main()
