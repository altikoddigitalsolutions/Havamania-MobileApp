/**
 * Open-Meteo API — Ücretsiz, API key gerektirmez.
 * https://open-meteo.com/
 * Backend çalışmasa bile hava durumu verisi sunar.
 */

const BASE_URL = 'https://api.open-meteo.com/v1/forecast';

// ── Arayüzler ────────────────────────────────────────────────────────────────

export interface CurrentWeatherData {
  temperature: number;
  humidity: number;
  feels_like: number;
  wind_speed: number;
  wind_direction: number;
  wind_gusts: number;
  weather_code: number;
  uv_index: number;
  description: string;
  // Yeni alanlar
  pressure: number;       // hPa
  visibility: number;     // metre → ekranda km
  cloud_cover: number;    // %
  dew_point: number;      // °C
  precipitation: number;  // mm (son 1 saat)
}

export interface HourlyWeatherItem {
  time: string;
  temperature: number;
  weather_code: number;
  precipitation_probability: number;
  precipitation: number;
  cloud_cover: number;
  wind_speed: number;
  wind_gusts: number;
}

export interface HourlyWeatherData {
  items: HourlyWeatherItem[];
}

export interface DailyWeatherItem {
  date: string;
  temp_max: number;
  temp_min: number;
  weather_code: number;
  precipitation_probability: number;
  precipitation_sum: number;     // mm
  wind_speed_max: number;        // km/h
  wind_gusts_max: number;        // km/h
  sunrise: string;               // ISO datetime
  sunset: string;                // ISO datetime
  uv_index_max: number;
}

export interface DailyWeatherData {
  items: DailyWeatherItem[];
}

// ── WMO kodu → Türkçe açıklama ───────────────────────────────────────────────
function describeWeather(code: number): string {
  if (code === 0) return 'Açık';
  if (code === 1) return 'Çoğunlukla Açık';
  if (code === 2) return 'Parçalı Bulutlu';
  if (code === 3) return 'Bulutlu';
  if (code <= 48) return 'Sisli';
  if (code <= 55) return 'Çiseleyen';
  if (code <= 65) return 'Yağmurlu';
  if (code <= 75) return 'Karlı';
  if (code <= 82) return 'Sağanak';
  if (code <= 99) return 'Fırtınalı';
  return 'Parçalı Bulutlu';
}

// ── Mevcut Hava Durumu ────────────────────────────────────────────────────────
export async function fetchCurrentWeather(lat: number, lon: number): Promise<CurrentWeatherData> {
  const currentFields = [
    'temperature_2m',
    'relative_humidity_2m',
    'apparent_temperature',
    'weather_code',
    'wind_speed_10m',
    'wind_direction_10m',
    'wind_gusts_10m',
    'uv_index',
    'surface_pressure',
    'visibility',
    'cloud_cover',
    'dew_point_2m',
    'precipitation',
  ].join(',');

  const url = `${BASE_URL}?latitude=${lat}&longitude=${lon}&current=${currentFields}&timezone=auto`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Open-Meteo current: HTTP ${res.status}`);
  const json = await res.json();
  if (!json.current) throw new Error(`Open-Meteo current: no 'current' in response`);
  const c = json.current;

  return {
    temperature: Math.round(c.temperature_2m ?? 0),
    humidity: Math.round(c.relative_humidity_2m ?? 0),
    feels_like: Math.round(c.apparent_temperature ?? 0),
    wind_speed: Math.round(c.wind_speed_10m ?? 0),
    wind_direction: Math.round(c.wind_direction_10m ?? 0),
    wind_gusts: Math.round(c.wind_gusts_10m ?? 0),
    weather_code: c.weather_code ?? 0,
    uv_index: Math.round((c.uv_index ?? 0) * 10) / 10,
    description: describeWeather(c.weather_code ?? 0),
    pressure: Math.round(c.surface_pressure ?? 1013),
    visibility: Math.round((c.visibility ?? 10000) / 1000 * 10) / 10, // m → km
    cloud_cover: Math.round(c.cloud_cover ?? 0),
    dew_point: Math.round(c.dew_point_2m ?? 0),
    precipitation: Math.round((c.precipitation ?? 0) * 10) / 10,
  };
}

// ── Saatlik Tahmin ────────────────────────────────────────────────────────────
export async function fetchHourlyWeather(
  lat: number,
  lon: number,
  hours = 48,
): Promise<HourlyWeatherData> {
  const hourlyFields = [
    'temperature_2m',
    'weather_code',
    'precipitation_probability',
    'precipitation',
    'cloud_cover',
    'wind_speed_10m',
    'wind_gusts_10m',
  ].join(',');

  const url = `${BASE_URL}?latitude=${lat}&longitude=${lon}&hourly=${hourlyFields}&timezone=auto&forecast_days=3`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Open-Meteo hourly: HTTP ${res.status}`);
  const json = await res.json();
  const h = json.hourly;

  const now = new Date();
  const nowHour = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:00`;
  const startIdx = (h.time as string[]).findIndex(t => t >= nowHour);
  const from = startIdx >= 0 ? startIdx : 0;

  const items: HourlyWeatherItem[] = (h.time as string[])
    .slice(from, from + hours)
    .map((time: string, i: number) => ({
      time,
      temperature: Math.round(h.temperature_2m[from + i] ?? 0),
      weather_code: h.weather_code[from + i] ?? 0,
      precipitation_probability: h.precipitation_probability[from + i] ?? 0,
      precipitation: Math.round((h.precipitation[from + i] ?? 0) * 10) / 10,
      cloud_cover: h.cloud_cover[from + i] ?? 0,
      wind_speed: Math.round(h.wind_speed_10m[from + i] ?? 0),
      wind_gusts: Math.round(h.wind_gusts_10m[from + i] ?? 0),
    }));

  return {items};
}

// ── Günlük Tahmin ─────────────────────────────────────────────────────────────
export async function fetchDailyWeather(
  lat: number,
  lon: number,
  days = 10,
): Promise<DailyWeatherData> {
  const dailyFields = [
    'weather_code',
    'temperature_2m_max',
    'temperature_2m_min',
    'precipitation_probability_max',
    'precipitation_sum',
    'wind_speed_10m_max',
    'wind_gusts_10m_max',
    'sunrise',
    'sunset',
    'uv_index_max',
  ].join(',');

  const url = `${BASE_URL}?latitude=${lat}&longitude=${lon}&daily=${dailyFields}&timezone=auto&forecast_days=${days}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Open-Meteo daily: HTTP ${res.status}`);
  const json = await res.json();
  const d = json.daily;

  const items: DailyWeatherItem[] = (d.time as string[]).map(
    (date: string, i: number) => ({
      date,
      temp_max: Math.round(d.temperature_2m_max[i] ?? 0),
      temp_min: Math.round(d.temperature_2m_min[i] ?? 0),
      weather_code: d.weather_code[i] ?? 0,
      precipitation_probability: d.precipitation_probability_max[i] ?? 0,
      precipitation_sum: Math.round((d.precipitation_sum[i] ?? 0) * 10) / 10,
      wind_speed_max: Math.round(d.wind_speed_10m_max[i] ?? 0),
      wind_gusts_max: Math.round(d.wind_gusts_10m_max[i] ?? 0),
      sunrise: d.sunrise[i] ?? '',
      sunset: d.sunset[i] ?? '',
      uv_index_max: Math.round((d.uv_index_max[i] ?? 0) * 10) / 10,
    }),
  );

  return {items};
}

// ── Şehir Arama (Geocoding) ──────────────────────────────────────────────────
export interface GeoResult {
  name: string;
  country: string;
  admin1?: string;
  latitude: number;
  longitude: number;
}

export async function searchCity(query: string): Promise<GeoResult[]> {
  const params = new URLSearchParams({
    name: query,
    count: '5',
    language: 'en',
    format: 'json',
  });

  const res = await fetch(`https://geocoding-api.open-meteo.com/v1/search?${params}`);
  if (!res.ok) return [];
  const json = await res.json();
  if (!json.results) return [];

  return (json.results as any[]).map(r => ({
    name: r.name,
    country: r.country,
    admin1: r.admin1,
    latitude: r.latitude,
    longitude: r.longitude,
  }));
}

// ── Ay Fazı Hesaplama ─────────────────────────────────────────────────────────
export function getMoonPhase(date: Date = new Date()): {emoji: string; label: string; illumination: number} {
  const year = date.getFullYear();
  const month = date.getMonth() + 1;
  const day = date.getDate();

  // Basit ay fazı algoritması (Julian Day tabanlı)
  let jd = 367 * year
    - Math.floor(7 * (year + Math.floor((month + 9) / 12)) / 4)
    + Math.floor(275 * month / 9)
    + day
    + 1721013.5;

  const cycle = (jd - 2451550.1) / 29.530588853;
  const phase = cycle - Math.floor(cycle); // 0-1 arası

  const illumination = Math.round(50 * (1 - Math.cos(2 * Math.PI * phase)));

  let emoji: string;
  let label: string;
  if (phase < 0.0625 || phase >= 0.9375) { emoji = '🌑'; label = 'Yeni Ay'; }
  else if (phase < 0.1875) { emoji = '🌒'; label = 'Hilal'; }
  else if (phase < 0.3125) { emoji = '🌓'; label = 'İlk Dördün'; }
  else if (phase < 0.4375) { emoji = '🌔'; label = 'Şişen Ay'; }
  else if (phase < 0.5625) { emoji = '🌕'; label = 'Dolunay'; }
  else if (phase < 0.6875) { emoji = '🌖'; label = 'Küçülen Ay'; }
  else if (phase < 0.8125) { emoji = '🌗'; label = 'Son Dördün'; }
  else { emoji = '🌘'; label = 'Eski Hilal'; }

  return {emoji, label, illumination};
}

// ── Güneş Doğuş/Batış Formatlama ─────────────────────────────────────────────
export function formatSunTime(isoDateTime: string): string {
  if (!isoDateTime) return '--:--';
  try {
    const d = new Date(isoDateTime);
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return '--:--';
  }
}

// ── Yardımcılar ──────────────────────────────────────────────────────────────
function pad(n: number): string {
  return String(n).padStart(2, '0');
}
