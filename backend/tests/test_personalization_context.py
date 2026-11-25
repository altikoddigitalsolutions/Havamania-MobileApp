from types import SimpleNamespace

import pytest
from app.schemas.chatbot import ChatbotAskRequest
from app.services.chatbot_context import platform_context


@pytest.fixture
def profile():
    return SimpleNamespace(interest="PRIVATE_OLD_INTEREST", travel_preferences="PRIVATE_TRAVEL",
                           activity_types="PRIVATE_ACTIVITY", health_sensitivities="PRIVATE_HEALTH")


def test_optout_strips_profile_trips_interests_and_health_even_if_supplied(profile):
    payload = ChatbotAskRequest(question="Yerel test", personalization_enabled=False,
        selected_interests=["PRIVATE_CLIENT_INTEREST"], health_consent=True,
        context={"city": "Bursa", "trips": "PRIVATE_TRIPS"})
    result = platform_context(payload, profile, allow_health=True)
    assert result == {"city": "Bursa"}


@pytest.mark.parametrize("interests,expected", [(None, "PRIVATE_OLD_INTEREST"), ([], None), (["kamp"], "kamp")])
def test_current_interest_selection_replaces_stale_server_value(profile, interests, expected):
    payload = ChatbotAskRequest(question="Yerel test", personalization_enabled=True, selected_interests=interests)
    result = platform_context(payload, profile, allow_health=False)["profile"]
    assert result.get("interest") == expected
    assert "health_sensitivities" not in result


def test_health_requires_independent_consent_with_personalization(profile):
    payload = ChatbotAskRequest(question="Yerel test", personalization_enabled=True, selected_interests=["kamp"])
    assert "health_sensitivities" not in platform_context(payload, profile, False)["profile"]
    assert platform_context(payload, profile, True)["profile"]["health_sensitivities"] == "PRIVATE_HEALTH"


def test_structured_health_interests_cannot_bypass_independent_consent(profile):
    interests = ["migren", "uv_hassasiyeti", "polen", "basinc_hassasiyeti", "kamp"]
    payload = ChatbotAskRequest(question="Local", personalization_enabled=True, selected_interests=interests)
    assert platform_context(payload, profile, False)["profile"]["interest"] == "kamp"
    assert platform_context(payload, profile, True)["profile"]["interest"] == ", ".join(interests)


def test_legacy_comma_separated_health_ids_are_also_filtered(profile):
    profile.interest = "migren,polen,UV Hassasiyeti,kamp"
    payload = ChatbotAskRequest(question="Local", personalization_enabled=True)
    assert platform_context(payload, profile, False)["profile"]["interest"] == "kamp"
