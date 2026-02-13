import {apiClient} from './apiClient';

export async function getCurrentWeather(lat: number, lon: number) {
  const response = await apiClient.get('/weather/current', {params: {lat, lon}});
  return response.data;
}

export async function getHourlyWeather(lat: number, lon: number, hours = 24) {
  const response = await apiClient.get('/weather/hourly', {params: {lat, lon, hours}});
  return response.data;
}

export async function getDailyWeather(lat: number, lon: number, days = 7) {
  const response = await apiClient.get('/weather/daily', {params: {lat, lon, days}});
  return response.data;
}
