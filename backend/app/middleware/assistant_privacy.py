from starlette.datastructures import MutableHeaders


class AssistantPrivacyMiddleware:
    """Prevent HTTP caches from retaining assistant data, including errors."""

    def __init__(self, app, prefix: str):
        self.app = app
        self.prefix = prefix.rstrip("/") + "/chatbot"

    async def __call__(self, scope, receive, send):
        path = scope.get("path", "")
        if scope["type"] != "http" or not (
            path == self.prefix or path.startswith(self.prefix + "/")
        ):
            return await self.app(scope, receive, send)

        async def private_send(message):
            if message["type"] == "http.response.start":
                headers = MutableHeaders(scope=message)
                headers["Cache-Control"] = "no-store"
                headers["Pragma"] = "no-cache"
            await send(message)

        await self.app(scope, receive, private_send)
