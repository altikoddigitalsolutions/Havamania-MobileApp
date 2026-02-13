class PushProvider:
    def send(self, platform: str, token: str, title: str, body: str) -> bool:
        raise NotImplementedError


class FCMApnsPushProvider(PushProvider):
    """Skeleton adapter for FCM/APNs integration."""

    def send(self, platform: str, token: str, title: str, body: str) -> bool:
        # TODO: Integrate real push provider SDK/API.
        _ = (platform, token, title, body)
        return True
