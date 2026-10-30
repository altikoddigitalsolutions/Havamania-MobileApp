import json
import time
from hashlib import sha256

from sqlalchemy import update

from app.models.chatbot_request import ChatbotRequest


def request_fingerprint(payload, wire):
    # Weather may refresh while the user retries. Pin the first weather snapshot;
    # question, city/trips, mode, tone and privacy/profile changes are conflicts.
    data = payload.model_dump(mode="json", exclude={"context", "request_id"})
    data["context"] = {
        key: wire["context"][key] for key in ("city", "trips", "profile") if key in wire["context"]
    }
    data["resolved_tone"] = wire["tone"]
    data["resolved_health"] = wire["health_consent"]
    # A retry must never resend a frozen body accepted under an older document.
    data["resolved_consent_version"] = wire["consent_version"]
    data["resolved_health_consent_version"] = wire["health_consent_version"]
    return sha256(
        json.dumps(data, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
    ).hexdigest()


def purge_expired_replays(db, owner=None):
    filters = [
        ChatbotRequest.replay_until < int(time.time()),
        ChatbotRequest.replay_until.is_not(None),
    ]
    if owner is not None:
        filters.append(ChatbotRequest.user_id == owner)
    db.execute(
        update(ChatbotRequest)
        .where(*filters)
        .values(platform_payload=None, cached_reply=None)
        .execution_options(synchronize_session=False)
    )
    db.commit()


if __name__ == "__main__":
    # Schedule periodically on the backend. Never initiates external/model calls.
    from app.db.session import SessionLocal

    with SessionLocal() as session:
        purge_expired_replays(session)
