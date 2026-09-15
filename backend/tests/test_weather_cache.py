from app.services.weather.cache import InMemoryTTLCache


def test_cache_capacity_evicts_least_recently_used():
    cache = InMemoryTTLCache(max_entries=2)
    cache.set("a", 1, 60)
    cache.set("b", 2, 60)
    assert cache.get("a") == 1
    cache.set("c", 3, 60)
    assert cache.get("b") is None
    assert cache.get("a") == 1
    assert cache.get("c") == 3


def test_expired_weather_is_not_returned():
    cache = InMemoryTTLCache()
    cache.set("expired", {"temperature": 20}, -1)
    assert cache.get("expired") is None
