import {useThemeStore} from '../store/themeStore';
import i18next from 'i18next';

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

// --- MODERN PREMIUM PALETTE ---
const HAVAMANIA_PRIMARY = '#3B82F6'; // Daha canlı bir mavi
const HAVAMANIA_DARK_BG = '#0B0F1A'; // Daha derin, şık gece mavisi
const HAVAMANIA_DARK_CARD = '#161E2E';

export const DarkColors: AppColors = {
  bg: HAVAMANIA_DARK_BG,
  bgSecondary: HAVAMANIA_DARK_CARD,
  bgCard: 'rgba(22, 30, 46, 0.7)',
  bgInput: '#1F2937',
  bgChip: '#374151',
  text: '#F9FAFB',
  textSecondary: '#9CA3AF',
  textMuted: '#6B7280',
  textOnAccent: '#FFFFFF',
  accent: HAVAMANIA_PRIMARY,
  accentDark: '#2563EB',
  accentBtn: HAVAMANIA_PRIMARY,
  accentBtnPressed: '#1D4ED8',
  border: 'rgba(255, 255, 255, 0.08)',
  divider: 'rgba(255, 255, 255, 0.05)',
  error: '#EF4444',
  success: '#10B981',
  warning: '#F59E0B',
  tabBar: 'rgba(11, 15, 26, 0.85)',
  tabActive: '#60A5FA', // Koyu temada daha parlak mavi
  tabInactive: '#4B5563',
  cardHourlyActive: 'rgba(59, 130, 246, 0.15)',
  tempGradientHigh: '#F97316',
  tempGradientLow: '#3B82F6',
};

export const LightColors: AppColors = {
  bg: '#F3F4F6',
  bgSecondary: '#FFFFFF',
  bgCard: 'rgba(255, 255, 255, 0.8)',
  bgInput: '#F9FAFB',
  bgChip: '#E5E7EB',
  text: '#111827',
  textSecondary: '#4B5563',
  textMuted: '#9CA3AF',
  textOnAccent: '#FFFFFF',
  accent: HAVAMANIA_PRIMARY,
  accentDark: '#1D4ED8',
  accentBtn: HAVAMANIA_PRIMARY,
  accentBtnPressed: '#2563EB',
  border: 'rgba(0, 0, 0, 0.05)',
  divider: 'rgba(0, 0, 0, 0.03)',
  error: '#DC2626',
  success: '#059669',
  warning: '#D97706',
  tabBar: 'rgba(255, 255, 255, 0.9)',
  tabActive: HAVAMANIA_PRIMARY,
  tabInactive: '#9CA3AF',
  cardHourlyActive: 'rgba(59, 130, 246, 0.08)',
  tempGradientHigh: '#EA580C',
  tempGradientLow: '#2563EB',
};

// ... (Mevsimsel renkler ve yardımcı fonksiyonlar aynı kalacak şekilde)
export const SpringColors: AppColors = { ...DarkColors, bg: '#061a0d', accent: '#34D399' };
export const SummerColors: AppColors = { ...DarkColors, bg: '#1a1405', accent: '#FBBF24' };
export const AutumnColors: AppColors = { ...DarkColors, bg: '#1a0b05', accent: '#FB923C' };
export const WinterColors: AppColors = { ...DarkColors, bg: '#0b1629', accent: '#38BDF8' };

export function useColors(): AppColors {
  const theme = useThemeStore(state => state.theme);
  switch (theme) {
    case 'spring': return SpringColors;
    case 'summer': return SummerColors;
    case 'autumn': return AutumnColors;
    case 'winter': return WinterColors;
    case 'light': return LightColors;
    default: return DarkColors;
  }
}

export const Spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48 } as const;
export const Radius = { sm: 8, md: 12, lg: 16, xl: 24, full: 9999 } as const;
export const FontSize = { xs: 11, sm: 13, md: 15, lg: 17, xl: 20, xxl: 24, temp: 72 } as const;

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

export function getWeatherIcon(code: number): string {
  if (code === 0) return 'sunny';
  if (code <= 2) return 'partly-sunny';
  if (code === 3) return 'cloudy';
  if (code <= 48) return 'reorder-three';
  if (code <= 55) return 'rainy';
  if (code <= 67) return 'rainy';
  if (code <= 77) return 'snow';
  if (code <= 82) return 'thunderstorm';
  if (code <= 99) return 'thunderstorm';
  return 'sunny';
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

export function formatHour(isoTime: string): string {
  const date = new Date(isoTime);
  const h = date.getHours();
  const m = date.getMinutes();
  return `${h < 10 ? `0${h}` : h}:${m < 10 ? `0${m}` : m}`;
}

export function formatDayShort(isoDate: string): string {
  const date = new Date(isoDate + 'T12:00:00');
  const lang = i18next.language;
  if (lang === 'tr') {
    const daysTr = ['Paz', 'Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt'];
    return daysTr[date.getDay()];
  }
  const daysEn = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  return daysEn[date.getDay()];
}
