"""Bounded, memory-only multipart reader. No UploadFile disk spooling."""

from collections import deque
from threading import Lock, BoundedSemaphore
from time import monotonic
from math import ceil
from email.parser import BytesParser
from email.policy import default
from app.services.chatbot_bridge import BridgeError

MAX_PHOTO_BYTES = 8 * 1024 * 1024
MAX_BODY_BYTES = MAX_PHOTO_BYTES + 64 * 1024


def parse_photo(content_type: str, body: bytes) -> tuple[bytes, str]:
    if len(body) > MAX_BODY_BYTES:
        raise BridgeError(413, "too_large", "Fotoğraf en fazla 8 MB olabilir.")
    if len(content_type) > 256 or "\r" in content_type or "\n" in content_type:
        raise BridgeError(400, "invalid_request", "Fotoğraf isteği geçerli değil.")
    message = BytesParser(policy=default).parsebytes(
        b"Content-Type: "
        + content_type.encode("ascii", errors="replace")
        + b"\r\nMIME-Version: 1.0\r\n\r\n"
        + body
    )
    parts = list(message.iter_parts())
    if message.get_content_type() != "multipart/form-data" or len(parts) != 1 or message.defects:
        raise BridgeError(400, "invalid_request", "Yalnızca bir fotoğraf gönderin.")
    part = parts[0]
    if (
        part.get_content_disposition() != "form-data"
        or part.get_param("name", header="content-disposition") != "file"
        or part.defects
        or part.is_multipart()
    ):
        raise BridgeError(400, "invalid_request", "Fotoğraf alanı geçerli değil.")
    if part.get("Content-Transfer-Encoding"):
        raise BridgeError(400, "invalid_request", "Fotoğraf kodlaması geçerli değil.")
    content = part.get_payload(decode=True)
    if not content:
        raise BridgeError(400, "invalid_request", "Bir fotoğraf seçin.")
    if len(content) > MAX_PHOTO_BYTES:
        raise BridgeError(413, "too_large", "Fotoğraf en fazla 8 MB olabilir.")
    media_type = part.get_content_type()
    valid = {
        "image/jpeg": content.startswith(b"\xff\xd8\xff"),
        "image/png": content.startswith(b"\x89PNG\r\n\x1a\n"),
        "image/gif": content.startswith((b"GIF87a", b"GIF89a")),
        "image/webp": content.startswith(b"RIFF") and content[8:12] == b"WEBP",
    }
    if not valid.get(media_type):
        raise BridgeError(415, "unsupported_media_type", "JPEG, PNG, WebP veya GIF fotoğraf seçin.")
    return content, media_type


_upload_hits = {}
_upload_lock = Lock()
_upload_slots = BoundedSemaphore(4)


def check_upload_rate(owner: str):
    # Process-local defense; multiworker deployments also require gateway limits.
    with _upload_lock:
        now = monotonic()
        for key in [k for k, hits in _upload_hits.items() if now - hits[-1] >= 60]:
            del _upload_hits[key]
        if owner not in _upload_hits:
            if len(_upload_hits) >= 10000:
                raise BridgeError(503, "busy", "Fotoğraf hizmeti yoğun.", "5")
            _upload_hits[owner] = deque()
        hits = _upload_hits[owner]
        while hits and now - hits[0] >= 60:
            hits.popleft()
        if len(hits) >= 10:
            raise BridgeError(
                429,
                "rate_limited",
                "Çok fazla fotoğraf yüklediniz.",
                str(max(1, ceil(60 - (now - hits[0])))),
            )
        hits.append(now)


UPLOAD_READ_TIMEOUT = 30


async def read_photo_body(request) -> bytes:
    from anyio import fail_after

    body = bytearray()
    try:
        with fail_after(UPLOAD_READ_TIMEOUT):
            async for chunk in request.stream():
                if len(body) + len(chunk) > MAX_BODY_BYTES:
                    raise BridgeError(413, "too_large", "Fotoğraf en fazla 8 MB olabilir.")
                body.extend(chunk)
    except TimeoutError:
        raise BridgeError(
            408, "upload_timeout", "Fotoğraf alınamadı. Bağlantınızı kontrol edip tekrar deneyin."
        ) from None
    return bytes(body)
