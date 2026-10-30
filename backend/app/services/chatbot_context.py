import json

from app.services.chatbot_bridge import BridgeError

TONES = {"friendly": "samimi", "formal": "resmi", "balanced": "dengeli", "short": "kisa",
         "expert": "uzman", "samimi": "samimi", "resmi": "resmi", "dengeli": "dengeli",
         "kisa": "kisa", "uzman": "uzman"}


def platform_context(payload, profile, allow_health):
    context = payload.context.model_dump(exclude_none=True) if payload.context else {}
    if not payload.personalization_enabled:
        context.pop("trips", None)
    else:
        fields = ("interest", "travel_preferences", "activity_types")
        personal = {name: str(getattr(profile, name))[:500] for name in fields
                    if profile and getattr(profile, name)}
        if payload.selected_interests is not None:
            personal.pop("interest", None)
            if payload.selected_interests:
                personal["interest"] = ", ".join(payload.selected_interests)[:500]
        if allow_health and profile and profile.health_sensitivities:
            personal["health_sensitivities"] = profile.health_sensitivities[:500]
        if personal:
            context["profile"] = personal
    if len(json.dumps(context, ensure_ascii=False).encode("utf-8")) > 8192:
        raise BridgeError(413, "too_large", "Asistan bağlamı çok uzun. Lütfen kısaltın.")
    return context
