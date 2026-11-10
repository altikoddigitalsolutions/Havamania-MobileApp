def safe_error_event(event, hint):
    """Send diagnostic structure, never request bodies or arbitrary error values."""
    safe = {
        key: event[key]
        for key in ("event_id", "timestamp", "level", "platform", "release", "environment")
        if key in event
    }
    exceptions = []
    for error in event.get("exception", {}).get("values", []):
        frames = []
        for frame in error.get("stacktrace", {}).get("frames", []):
            frames.append(
                {
                    key: frame[key]
                    for key in ("filename", "function", "module", "lineno", "in_app")
                    if key in frame
                }
            )
        exceptions.append(
            {
                **{key: error[key] for key in ("type", "module") if key in error},
                "value": "Exception details omitted for privacy",
                "stacktrace": {"frames": frames},
            }
        )
    if exceptions:
        safe["exception"] = {"values": exceptions}
    else:
        safe["message"] = "Backend diagnostic event (details omitted for privacy)"
    return safe
