/**
 * weatherApi — Artık Open-Meteo'yu doğrudan kullanır.
 * Backend/Docker çalışmasa bile hava durumu verileri gelir.
 */
import {
  fetchCurrentWeather,
  fetchDailyWeather,
  fetchHourlyWeather,
} from './openMeteoApi';

export {fetchCurrentWeather as getCurrentWeather};
export {fetchHourlyWeather as getHourlyWeather};
export {fetchDailyWeather as getDailyWeather};

// Geriye dönük uyumluluk için eski imzaları da koru
export async function getCurrentWeatherLegacy(lat: number, lon: number) {
  return fetchCurrentWeather(lat, lon);
}
