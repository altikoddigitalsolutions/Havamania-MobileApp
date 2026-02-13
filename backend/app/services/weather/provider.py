from abc import ABC, abstractmethod


class WeatherProvider(ABC):
    name: str

    @abstractmethod
    def get_current(self, lat: float, lon: float) -> dict:
        raise NotImplementedError

    @abstractmethod
    def get_hourly(self, lat: float, lon: float, hours: int) -> dict:
        raise NotImplementedError

    @abstractmethod
    def get_daily(self, lat: float, lon: float, days: int) -> dict:
        raise NotImplementedError
