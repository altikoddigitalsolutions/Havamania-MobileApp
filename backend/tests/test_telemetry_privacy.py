import copy
import json

from app.core import observability
from app.core.telemetry_privacy import safe_error_event
from sentry_sdk import Client
from sentry_sdk.transport import Transport


def test_error_event_drops_private_data_and_keeps_diagnostic_stack():
    event = {
        "event_id": "local-event",
        "level": "error",
        "release": "1.0",
        "request": {"data": "PRIVATE_QUESTION", "headers": {"Authorization": "PRIVATE_TOKEN"}},
        "user": {"email": "PRIVATE_EMAIL"},
        "extra": {"health": "PRIVATE_HEALTH"},
        "contexts": {"profile": "PRIVATE_PROFILE"},
        "tags": {"trip": "PRIVATE_TRIP"},
        "breadcrumbs": {"values": [{"message": "PRIVATE_BODY"}]},
        "logentry": {"message": "PRIVATE_LOG"},
        "message": "PRIVATE_MESSAGE",
        "exception": {
            "values": [
                {
                    "type": "ValueError",
                    "module": "builtins",
                    "value": "PRIVATE_RESPONSE",
                    "mechanism": {"data": "PRIVATE_CONTEXT"},
                    "stacktrace": {
                        "frames": [
                            {
                                "filename": "app/services/example.py",
                                "function": "ask",
                                "lineno": 42,
                                "in_app": True,
                                "vars": {"key": "PRIVATE_KEY"},
                                "context_line": "PRIVATE_SOURCE",
                                "pre_context": ["PRIVATE_SOURCE"],
                            }
                        ]
                    },
                }
            ]
        },
    }
    original = copy.deepcopy(event)
    safe = safe_error_event(event, {"exc_info": "PRIVATE_HINT"})
    assert "PRIVATE_" not in json.dumps(safe)
    assert safe["event_id"] == "local-event" and safe["release"] == "1.0"
    error = safe["exception"]["values"][0]
    assert error["type"] == "ValueError"
    assert error["stacktrace"]["frames"][0]["lineno"] == 42
    assert event == original


def test_log_event_has_only_generic_message():
    safe = safe_error_event({"message": "PRIVATE_LOG", "extra": {"body": "PRIVATE_BODY"}}, {})
    assert "PRIVATE_" not in json.dumps(safe)
    assert safe["message"]


def test_sentry_initialization_enforces_privacy_without_network(monkeypatch):
    settings = observability.get_settings()
    monkeypatch.setattr(settings, "sentry_dsn_backend", "https://local@example.invalid/1")
    captured = []
    monkeypatch.setattr(observability.sentry_sdk, "init", lambda **kwargs: captured.append(kwargs))
    observability.init_sentry()
    config = captured[0]
    assert config["send_default_pii"] is False
    assert config["max_request_body_size"] == "never"
    assert config["include_local_variables"] is False
    assert config["before_send"] is safe_error_event


def test_sentry_disabled_without_dsn(monkeypatch):
    monkeypatch.setattr(observability.get_settings(), "sentry_dsn_backend", None)
    captured = []
    monkeypatch.setattr(observability.sentry_sdk, "init", lambda **kwargs: captured.append(kwargs))
    observability.init_sentry()
    assert captured == []


def test_installed_sdk_emits_sanitized_envelope_without_network():
    envelopes = []

    class MemoryTransport(Transport):
        def capture_envelope(self, envelope):
            envelopes.append(envelope)

    client = Client(
        dsn="https://local@example.invalid/1",
        transport=MemoryTransport,
        default_integrations=False,
        send_default_pii=False,
        max_request_body_size="never",
        include_local_variables=False,
        before_send=safe_error_event,
    )
    try:
        event_id = client.capture_event(
            {
                "level": "error",
                "message": "PRIVATE_MESSAGE",
                "request": {"data": "PRIVATE_HEALTH"},
                "exception": {"values": [{"type": "ValueError", "value": "PRIVATE_RESPONSE"}]},
            }
        )
        assert event_id and len(envelopes) == 1
        event = envelopes[0].get_event()
        assert event["exception"]["values"][0]["type"] == "ValueError"
        assert "PRIVATE_" not in json.dumps(event)
        assert "request" not in event
    finally:
        client.close()
