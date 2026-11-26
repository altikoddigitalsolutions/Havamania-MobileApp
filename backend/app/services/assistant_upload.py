"""Bounded, memory-only multipart reader. No UploadFile disk spooling."""

from collections import deque
from threading import Lock, BoundedSemaphore
from time import monotonic
from math import ceil
from python_multipart.multipart import MultipartParser, MultipartParseError, parse_options_header
from app.services.chatbot_bridge import BridgeError

MAX_PHOTO_BYTES = 8 * 1024 * 1024
MAX_BODY_BYTES = MAX_PHOTO_BYTES + 64 * 1024


def parse_photo(content_type: str, body: bytes) -> tuple[bytes, str]:
    if len(body) > MAX_BODY_BYTES:
        raise BridgeError(413, "too_large", "Fotoğraf en fazla 8 MB olabilir.")
    if (
        len(content_type) > 256
        or not content_type.isascii()
        or "\r" in content_type
        or "\n" in content_type
    ):
        raise BridgeError(400, "invalid_request", "Fotoğraf isteği geçerli değil.")

    def invalid():
        raise BridgeError(
            400, "invalid_request", "Yalnızca tek bir geçerli fotoğraf alanı gönderin."
        )

    mime, options = parse_options_header(content_type)
    boundary = options.get(b"boundary", b"")
    if mime.lower() != b"multipart/form-data" or not 1 <= len(boundary) <= 70:
        invalid()
    headers = {}
    field, value, content = bytearray(), bytearray(), bytearray()
    part_count = 0
    header_bytes = 0
    complete = False
    media_type = ""

    def part_begin():
        nonlocal part_count
        part_count += 1
        if part_count != 1:
            invalid()

    def header_data(target, data, start, end):
        nonlocal header_bytes
        header_bytes += end - start
        if header_bytes > 8192:
            invalid()
        target.extend(data[start:end])

    def header_end():
        key = bytes(field).lower()
        if not key or key in headers or len(headers) >= 8:
            invalid()
        headers[key] = bytes(value)
        field.clear()
        value.clear()

    def headers_finished():
        nonlocal media_type
        disposition, params = parse_options_header(headers.get(b"content-disposition", b""))
        if disposition.lower() != b"form-data" or params.get(b"name") != b"file":
            invalid()
        if b"content-transfer-encoding" in headers:
            invalid()
        media, _ = parse_options_header(headers.get(b"content-type", b""))
        # Never recursively interpret uploaded bytes as MIME messages or subparts.
        if media.lower().startswith((b"message/", b"multipart/")):
            invalid()
        if media.lower() not in {b"image/jpeg", b"image/png", b"image/gif", b"image/webp"}:
            raise BridgeError(
                415, "unsupported_media_type", "JPEG, PNG, WebP veya GIF fotoğraf seçin."
            )
        media_type = media.decode("ascii").lower()

    def part_data(data, start, end):
        if len(content) + end - start > MAX_PHOTO_BYTES:
            raise BridgeError(413, "too_large", "Fotoğraf en fazla 8 MB olabilir.")
        content.extend(data[start:end])

    def on_end():
        nonlocal complete
        complete = True

    parser = MultipartParser(
        boundary,
        callbacks={
            "on_part_begin": part_begin,
            "on_header_field": lambda data, start, end: header_data(field, data, start, end),
            "on_header_value": lambda data, start, end: header_data(value, data, start, end),
            "on_header_end": header_end,
            "on_headers_finished": headers_finished,
            "on_part_data": part_data,
            "on_end": on_end,
        },
        max_size=MAX_BODY_BYTES,
    )
    try:
        parser.write(body)
        parser.finalize()
    except MultipartParseError:
        raise BridgeError(400, "invalid_request", "Fotoğraf isteği geçerli değil.") from None
    # finalize() alone does not reject an unfinished body in python-multipart.
    if not complete or part_count != 1 or not content:
        invalid()
    content = bytes(content)
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
