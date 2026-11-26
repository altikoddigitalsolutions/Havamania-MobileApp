"""Anonymous routing smoke check; sends no photo, token, question or bridge key."""
import argparse
import json
from urllib.parse import urlsplit

import httpx


def check_photo_deployment(base_url: str, *, transport=None) -> dict:
    parsed = urlsplit(base_url)
    local = parsed.hostname in {"localhost", "127.0.0.1", "::1"}
    if (not parsed.hostname or parsed.username is not None or parsed.password is not None
            or parsed.query or parsed.fragment or parsed.path not in {"", "/"}
            or (parsed.scheme != "https" and not (parsed.scheme == "http" and local))):
        raise ValueError("Use an HTTPS service root, or HTTP loopback; no credentials/path/query.")
    checks = []
    with httpx.Client(timeout=10, follow_redirects=False, transport=transport) as client:
        for method, path, expected in [
            ("GET", "/health", 200),
            ("GET", "/v1/chatbot/consent", 401),
            ("POST", "/v1/chatbot/attachments", 401),
        ]:
            result = {"method": method, "path": path, "expected_status": expected}
            try:
                # Read headers only; response bodies may contain private upstream details.
                with client.stream(method, base_url.rstrip("/") + path) as response:
                    result["status"] = response.status_code
            except httpx.RequestError as error:
                result["status"] = None
                result["connection_error"] = type(error).__name__
            checks.append(result)
    return {"base_url": base_url.rstrip("/"), "authenticated": False, "photo_sent": False,
            "routing_ok": all(row["status"] == row["expected_status"] for row in checks),
            "checks": checks}


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="https://api.havamania.com")
    args = parser.parse_args(argv)
    try:
        result = check_photo_deployment(args.base_url)
    except ValueError as error:
        parser.error(str(error))
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["routing_ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
