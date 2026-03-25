import {useThemeStore} from '../store/themeStore';

// ── Renk Arayüzü (her iki tema da bu yapıya uyar) ────────────────────────────
export type AppColors = {
  bg: string;
  bgSecondary: string;
  bgCard: string;
  bgInput: string;
  bgChip: string;
  text: string;
  textSecondary: string;
  textMuted: string;
  textOnAccent: string;
  accent: string;
  accentDark: string;
  accentBtn: string;
  accentBtnPressed: string;
  border: string;
  divider: string;
  error: string;
  success: string;
  warning: string;
  tabBar: string;
  tabActive: string;
  tabInactive: string;
  cardHourlyActive: string;
  tempGradientHigh: string;
  tempGradientLow: string;
};

// ── Renk Paletleri ──────────────────────────────────────────────────────────
export const DarkColors: AppColors = {
  // Arka planlar
  bg: '#0B1929',
  bgSecondary: '#112036',
  bgCard: '#152B40',
  bgInput: '#1C3250',
  bgChip: '#1A3050',

  // Metin
  text: '#FFFFFF',
  textSecondary: '#7AA3C0',
  textMuted: '#4A6A85',
  textOnAccent: '#FFFFFF',

  // Vurgu
  accent: '#29B5F6',
  accentDark: '#0A7AC4',
  accentBtn: '#1A8EF0',
  accentBtnPressed: '#1570CC',

  // Sınır / Ayırıcı
  border: '#1D3251',
  divider: '#1A2E45',

  // Durum
  error: '#F44336',
  success: '#4CAF50',
  warning: '#FF9800',

  // Tab bar
  tabBar: '#0B1929',
  tabActive: '#29B5F6',
  tabInactive: '#4A6A85',

  // Özel
  cardHourlyActive: '#0A3A6E',
  tempGradientHigh: '#FFC837',
  tempGradientLow: '#29B5F6',
};

export const LightColors: AppColors = {
  // Arka planlar
  bg: '#EEF3F8',
  bgSecondary: '#FFFFFF',
  bgCard: '#FFFFFF',
  bgInput: '#F0F4F8',
  bgChip: '#E3EEF8',

  // Metin
  text: '#0B1929',
  textSecondary: '#5A7089',
  textMuted: '#8DA0B3',
  textOnAccent: '#FFFFFF',

  // Vurgu
  accent: '#0288D1',
  accentDark: '#01579B',
  accentBtn: '#0288D1',
  accentBtnPressed: '#0277BD',

  // Sınır / Ayırıcı
  border: '#D8E6F0',
  divider: '#E0EAF0',

  // Durum
  error: '#F44336',
  success: '#4CAF50',
  warning: '#FF9800',

  // Tab bar
  tabBar: '#FFFFFF',
  tabActive: '#0288D1',
  tabInactive: '#8DA0B3',

  // Özel
  cardHourlyActive: '#D1E8F8',
  tempGradientHigh: '#FF8C00',
  tempGradientLow: '#0288D1',
};

// ── Tema Hook'u ─────────────────────────────────────────────────────────────
export function useColors(): AppColors {
  const theme = useThemeStore(state => state.theme);
  return theme === 'dark' ? DarkColors : LightColors;
}

// ── Sabit Değerler ───────────────────────────────────────────────────────────
export const Spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
} as const;

export const Radius = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  full: 9999,
} as const;

export const FontSize = {
  xs: 11,
  sm: 13,
  md: 15,
  lg: 17,
  xl: 20,
  xxl: 24,
  temp: 72,
} as const;

// ── Hava Durumu Yardımcıları ─────────────────────────────────────────────────
export function getWeatherEmoji(code: number): string {
  if (code === 0) return '☀️';
  if (code <= 2) return '🌤️';
  if (code === 3) return '☁️';
  if (code <= 48) return '🌫️';
  if (code <= 55) return '🌦️';
  if (code <= 67) return '🌧️';
  if (code <= 77) return '❄️';
  if (code <= 82) return '🌦️';
  if (code <= 99) return '⛈️';
  return '🌤️';
}

export function getWeatherLabel(code: number): string {
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

export function getWeatherLabelEn(code: number): string {
  if (code === 0) return 'Clear Sky';
  if (code === 1) return 'Mainly Clear';
  if (code === 2) return 'Partly Cloudy';
  if (code === 3) return 'Overcast';
  if (code <= 48) return 'Foggy';
  if (code <= 55) return 'Drizzle';
  if (code <= 65) return 'Rainy';
  if (code <= 75) return 'Snowy';
  if (code <= 82) return 'Rain Showers';
  if (code <= 99) return 'Thunderstorm';
  return 'Mostly Cloudy';
}

export function formatHour(isoTime: string): string {
  const date = new Date(isoTime);
  const h = date.getHours();
  const ampm = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${hour12} ${ampm}`;
}

export function formatDayShort(isoDate: string): string {
  const date = new Date(isoDate + 'T12:00:00');
  const days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  return days[date.getDay()];
}
