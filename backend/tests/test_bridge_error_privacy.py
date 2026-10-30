import traceback

import httpx
import pytest
from app.services.chatbot_bridge import BridgeError, ChatbotBridgeClient


@pytest.mark.parametrize("failure", ["connect", "timeout", "read", "json", "schema"])
def test_bridge_error_traceback_does_not_include_upstream_private_details(monkeypatch, failure):
    calls = []

    def post(self, url, **kwargs):
        calls.append(kwargs)
        exceptions = {
            "connect": httpx.ConnectError,
            "timeout": httpx.ReadTimeout,
            "read": httpx.ReadError,
        }
        if failure in exceptions:
            raise exceptions[failure]("PRIVATE_BRIDGE_DETAIL")
        if failure == "json":
            # Invalid JSON is separately covered; malformed schema embeds a value
            # in the original Pydantic exception that must not escape in a chain.
            return httpx.Response(200, text="PRIVATE_BRIDGE_DETAIL")
        return httpx.Response(200, json={"answer": {"secret": "PRIVATE_BRIDGE_DETAIL"}})

    monkeypatch.setattr(httpx.Client, "post", post)
    with pytest.raises(BridgeError) as caught:
        ChatbotBridgeClient(api_key="LOCAL_TEST_KEY").ask("question", "user")
    error = caught.value
    assert error.__cause__ is None
    assert error.__suppress_context__
    rendered = "".join(traceback.TracebackException.from_exception(error).format())
    assert "PRIVATE_BRIDGE_DETAIL" not in rendered
    assert error.code in {"unavailable", "timeout", "invalid_response"}
    assert len(calls) == (2 if failure == "connect" else 1)
