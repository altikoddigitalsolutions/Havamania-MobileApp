import json

import pytest

from app.schemas.chatbot import ChatbotAskRequest
from app.services.chatbot_context import platform_context


def inventory():
    return json.dumps({
        "schema_version": 1,
        "scope": "ongoing_and_upcoming_only",
        "active_total": 5,
        "listed_count": 2,
        "omitted_count": 3,
        "guidance": "Belirsiz soruda yer ve tarih iste; ilk kaydı varsayma.",
        "records": [
            {"trip_id": "a", "place": "Ayvalık, Balıkesir", "start_date": "2026-12-01", "end_date": "2026-12-02"},
            {"trip_id": "b", "place": "Edremit, Balıkesir", "start_date": "2026-12-03", "end_date": "2026-12-04"},
        ],
    }, ensure_ascii=False)


@pytest.mark.parametrize("enabled", [True, False])
def test_trip_inventory_uses_existing_string_contract_and_respects_opt_out(enabled):
    value = inventory()
    request = ChatbotAskRequest(question="Seyahatimde hava nasıl?", personalization_enabled=enabled,
                               context={"trips": value})
    result = platform_context(request, profile=None, allow_health=False)
    if enabled:
        assert result["trips"] == value
        parsed = json.loads(result["trips"])
        assert parsed["active_total"] == parsed["listed_count"] + parsed["omitted_count"]
        assert parsed["records"][1]["place"] == "Edremit, Balıkesir"
    else:
        assert "trips" not in result
