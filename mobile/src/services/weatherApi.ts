/**
 * weatherApi — Artık Open-Meteo'yu doğrudan kullanır.
 * Backend/Docker çalışmasa bile hava durumu verileri gelir.
 */
import {
  fetchCurrentWeather,
  fetchDailyWeather,
  fetchHourlyWeather,
} from './openMeteoApi';

export const getCurrentWeather = (lat: number, lon: number, tempUnit?: 'C' | 'F') => fetchCurrentWeather(lat, lon, tempUnit);
export const getHourlyWeather = (lat: number, lon: number, hours?: number, tempUnit?: 'C' | 'F') => fetchHourlyWeather(lat, lon, hours, tempUnit);
export const getDailyWeather = (lat: number, lon: number, days?: number, tempUnit?: 'C' | 'F') => fetchDailyWeather(lat, lon, days, tempUnit);

// Geriye dönük uyumluluk için eski imzaları da koru
export async function getCurrentWeatherLegacy(lat: number, lon: number) {
  return fetchCurrentWeather(lat, lon);
}
