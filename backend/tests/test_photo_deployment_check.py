import json

import httpx
import pytest
from app.photo_deployment_check import check_photo_deployment


def test_registered_photo_route_requires_auth_without_upload_or_model_request():
    requests = []

    def respond(request):
        requests.append(request)
        return httpx.Response(200 if request.url.path == "/health" else 401)

    result = check_photo_deployment("https://backend.invalid", transport=httpx.MockTransport(respond))
    assert result["routing_ok"] is True
    assert [(r.method, r.url.path) for r in requests] == [
        ("GET", "/health"), ("GET", "/v1/chatbot/consent"), ("POST", "/v1/chatbot/attachments")]
    assert all("authorization" not in r.headers and r.content == b"" for r in requests)
    assert result["photo_sent"] is False


@pytest.mark.parametrize("photo_status", [404, 405, 200, 307, 500])
def test_missing_redirected_unprotected_or_broken_endpoint_fails_gate(photo_status):
    calls = []

    def respond(request):
        calls.append(request.url.path)
        if request.url.path == "/health":
            return httpx.Response(200)
        if request.url.path == "/v1/chatbot/consent":
            return httpx.Response(401)
        return httpx.Response(photo_status, headers={"Location": "https://elsewhere.invalid"})

    result = check_photo_deployment("https://backend.invalid", transport=httpx.MockTransport(respond))
    assert result["routing_ok"] is False
    assert len(calls) == 3


def test_connection_error_text_and_response_bodies_are_not_exposed():
    def respond(request):
        if request.url.path == "/health":
            raise httpx.ConnectError("PRIVATE_TOKEN", request=request)
        return httpx.Response(401, text="PRIVATE_SERVER_BODY")

    result = check_photo_deployment("https://backend.invalid", transport=httpx.MockTransport(respond))
    assert result["routing_ok"] is False
    assert result["checks"][0]["connection_error"] == "ConnectError"
    assert "PRIVATE" not in json.dumps(result)


@pytest.mark.parametrize("url", ["http://backend.invalid", "https://user:password@backend.invalid",
    "https://backend.invalid/?token=private", "https://backend.invalid/#private",
    "https://backend.invalid/api", "file:///tmp/api"])
def test_unsafe_or_ambiguous_service_roots_are_rejected_before_network(url):
    def fail(request):
        raise AssertionError("Network must not be reached")
    with pytest.raises(ValueError):
        check_photo_deployment(url, transport=httpx.MockTransport(fail))


def test_loopback_service_can_be_checked_inside_container():
    result = check_photo_deployment("http://127.0.0.1:8000", transport=httpx.MockTransport(
        lambda request: httpx.Response(200 if request.url.path == "/health" else 401)))
    assert result["routing_ok"] is True
