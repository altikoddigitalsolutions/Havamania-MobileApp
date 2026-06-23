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
const HAVAMANIA_PRIMARY = '#06B6D4'; // Modern Cyan / Electric Blue
const HAVAMANIA_DARK_BG = '#020617'; // Slate 950 - Ultra dark premium background
const HAVAMANIA_DARK_CARD = '#0F172A'; // Slate 900 - Card background

export const DarkColors: AppColors = {
  bg: HAVAMANIA_DARK_BG,
  bgSecondary: HAVAMANIA_DARK_CARD,
  bgCard: 'rgba(15, 23, 42, 0.8)', // Glass effect base
  bgInput: '#1E293B',
  bgChip: '#1E293B',
  text: '#F8FAFC',
  textSecondary: '#94A3B8',
  textMuted: '#475569',
  textOnAccent: '#FFFFFF',
  accent: HAVAMANIA_PRIMARY,
  accentDark: '#0891B2',
  accentBtn: HAVAMANIA_PRIMARY,
  accentBtnPressed: '#0E7490',
  border: 'rgba(255, 255, 255, 0.08)',
  divider: 'rgba(255, 255, 255, 0.05)',
  error: '#EF4444',
  success: '#10B981',
  warning: '#F59E0B',
  tabBar: 'rgba(2, 6, 23, 0.95)',
  tabActive: HAVAMANIA_PRIMARY,
  tabInactive: '#475569',
  cardHourlyActive: 'rgba(6, 182, 212, 0.12)',
  tempGradientHigh: '#F97316',
  tempGradientLow: '#06B6D4',
};

export const LightColors: AppColors = {
  bg: '#F8FAFC',
  bgSecondary: '#FFFFFF',
  bgCard: 'rgba(255, 255, 255, 0.9)',
  bgInput: '#F1F5F9',
  bgChip: '#E2E8F0',
  text: '#0F172A',
  textSecondary: '#475569',
  textMuted: '#94A3B8',
  textOnAccent: '#FFFFFF',
  accent: HAVAMANIA_PRIMARY,
  accentDark: '#0891B2',
  accentBtn: HAVAMANIA_PRIMARY,
  accentBtnPressed: '#0E7490',
  border: 'rgba(0, 0, 0, 0.06)',
  divider: 'rgba(0, 0, 0, 0.04)',
  error: '#DC2626',
  success: '#059669',
  warning: '#D97706',
  tabBar: 'rgba(255, 255, 255, 0.98)',
  tabActive: HAVAMANIA_PRIMARY,
  tabInactive: '#94A3B8',
  cardHourlyActive: 'rgba(6, 182, 212, 0.08)',
  tempGradientHigh: '#EA580C',
  tempGradientLow: '#0891B2',
};

// Seasonal colors refined for premium look
export const SpringColors: AppColors = { ...DarkColors, bg: '#020617', accent: '#10B981', bgCard: 'rgba(6, 31, 22, 0.8)' };
export const SummerColors: AppColors = { ...DarkColors, bg: '#020617', accent: '#F59E0B', bgCard: 'rgba(31, 26, 6, 0.8)' };
export const AutumnColors: AppColors = { ...DarkColors, bg: '#020617', accent: '#F97316', bgCard: 'rgba(31, 13, 6, 0.8)' };
export const WinterColors: AppColors = { ...DarkColors, bg: '#020617', accent: '#38BDF8', bgCard: 'rgba(6, 26, 46, 0.8)' };

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

export const Spacing = { xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48, xxxl: 64 } as const;
export const Radius = { xs: 8, sm: 12, md: 16, lg: 24, xl: 32, xxl: 40, full: 9999 } as const;
export const FontSize = { xs: 12, sm: 14, md: 16, lg: 18, xl: 20, xxl: 24, xxxl: 32, temp: 72 } as const;


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
