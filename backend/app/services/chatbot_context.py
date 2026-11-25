import json

from app.services.chatbot_bridge import BridgeError

TONES = {"friendly": "samimi", "formal": "resmi", "balanced": "dengeli", "short": "kisa",
         "expert": "uzman", "samimi": "samimi", "resmi": "resmi", "dengeli": "dengeli",
         "kisa": "kisa", "uzman": "uzman"}



def _health_interest(value):
    normalized = str(value).casefold().translate(str.maketrans("ıçğöşü", "icgosu"))
    normalized = "".join(char for char in normalized if char.isalnum())
    return normalized in {"migren", "migrenhassasiyeti", "uvhassasiyeti", "polen",
                          "polenhassasiyeti", "basinchassasiyeti", "saglikhassasiyeti"}


def _interest_context(values, allow_health):
    return ", ".join(value for value in values if allow_health or not _health_interest(value))[:500]

def platform_context(payload, profile, allow_health):
    context = payload.context.model_dump(exclude_none=True) if payload.context else {}
    if not payload.personalization_enabled:
        context.pop("trips", None)
    else:
        fields = ("interest", "travel_preferences", "activity_types")
        personal = {name: str(getattr(profile, name))[:500] for name in fields
                    if profile and getattr(profile, name)}
        if not allow_health and personal.get("interest"):
            filtered = _interest_context(personal["interest"].split(","), False)
            if filtered:
                personal["interest"] = filtered
            else:
                personal.pop("interest", None)
        if payload.selected_interests is not None:
            personal.pop("interest", None)
            filtered = _interest_context(payload.selected_interests, allow_health)
            if filtered:
                personal["interest"] = filtered
        if allow_health and profile and profile.health_sensitivities:
            personal["health_sensitivities"] = profile.health_sensitivities[:500]
        if personal:
            context["profile"] = personal
    non_weather = {key: value for key, value in context.items() if key != "weather_data"}
    if len(json.dumps(non_weather, ensure_ascii=False).encode("utf-8")) > 8192:
        raise BridgeError(413, "too_large", "Asistan bağlamı çok uzun. Lütfen kısaltın.")
    return context
