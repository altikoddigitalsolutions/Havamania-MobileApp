import React, { useEffect, useRef, useState, useMemo } from 'react';
import { StyleSheet, View, Text, Animated, Easing, AccessibilityInfo, Dimensions } from 'react-native';
import { Spacing, AppColors, Theme, getWeatherEmoji } from '../theme';
import { useThemeStore } from '../store/themeStore';

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export enum WeatherCondition {
  SUNNY = 'SUNNY',
  MOSTLY_CLEAR = 'MOSTLY_CLEAR',
  PARTLY_CLOUDY = 'PARTLY_CLOUDY',
  MOSTLY_CLOUDY = 'MOSTLY_CLOUDY',
  CLOUDY = 'CLOUDY',
  RAIN = 'RAIN',
  LIGHT_RAIN = 'LIGHT_RAIN',
  HEAVY_RAIN = 'HEAVY_RAIN',
  SNOW = 'SNOW',
  FOG = 'FOG',
  THUNDERSTORM = 'THUNDERSTORM',
  WIND = 'WIND'
}

export enum TimeOfDay {
  MORNING = 'MORNING',
  DAY = 'DAY',
  EVENING = 'EVENING',
  NIGHT = 'NIGHT'
}

interface WeatherCardStyle {
  gradients: string[];
  effect: 'sunny' | 'rain' | 'cloudy' | 'snow' | 'fog' | 'thunder' | 'night' | 'wind' | 'partly_cloudy';
  isWhiteText: boolean;
  accent: string;
  name: string;
}

const getRawCondition = (code: number): WeatherCondition => {
  if (code === 0) return WeatherCondition.SUNNY;
  if (code === 1) return WeatherCondition.MOSTLY_CLEAR;
  if (code === 2) return WeatherCondition.PARTLY_CLOUDY;
  if (code === 3) return WeatherCondition.MOSTLY_CLOUDY;
  if (code >= 4 && code <= 10) return WeatherCondition.CLOUDY;
  if (code === 45 || code === 48) return WeatherCondition.FOG;
  if (code >= 51 && code <= 55) return WeatherCondition.LIGHT_RAIN;
  if ((code >= 61 && code <= 65) || (code >= 80 && code <= 82)) return WeatherCondition.RAIN;
  if (code >= 66 && code <= 67) return WeatherCondition.HEAVY_RAIN;
  if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) return WeatherCondition.SNOW;
  if (code >= 95) return WeatherCondition.THUNDERSTORM;
  return WeatherCondition.SUNNY;
};

const getTimeOfDay = (hour: number): TimeOfDay => {
  if (hour >= 6 && hour < 11) return TimeOfDay.MORNING;
  if (hour >= 11 && hour < 17) return TimeOfDay.DAY;
  if (hour >= 17 && hour < 20) return TimeOfDay.EVENING;
  return TimeOfDay.NIGHT;
};

const resolveWeatherCardStyle = (
  condition: WeatherCondition,
  timeOfDay: TimeOfDay,
  theme: Theme
): WeatherCardStyle => {
  let style: WeatherCardStyle;

  switch (condition) {
    case WeatherCondition.SUNNY:
    case WeatherCondition.MOSTLY_CLEAR:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0F172A', '#1E293B', '#334155'], effect: 'night', isWhiteText: true, accent: '#F8FAFC', name: 'CLEAR_NIGHT' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#FFEDD5', '#FEF3C7', '#E0F2FE'], effect: 'sunny', isWhiteText: false, accent: '#F59E0B', name: 'SUNNY_MORNING' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#F97316', '#7C2D12', '#431407'], effect: 'sunny', isWhiteText: true, accent: '#FB923C', name: 'SUNNY_EVENING' };
      } else {
        style = { gradients: ['#38BDF8', '#7DD3FC', '#BAE6FD'], effect: 'sunny', isWhiteText: true, accent: '#FCD34D', name: 'SUNNY_DAY' };
      }
      break;

    case WeatherCondition.PARTLY_CLOUDY:
    case WeatherCondition.MOSTLY_CLOUDY:
    case WeatherCondition.CLOUDY:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#020617', '#0F172A', '#1E293B'], effect: 'cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'CLOUDY_NIGHT' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#94A3B8', '#B1B1D8', '#E2E8F0'], effect: 'cloudy', isWhiteText: false, accent: '#F8FAFC', name: 'CLOUDY_MORNING' };
      } else if (timeOfDay === TimeOfDay.DAY) {
        style = { gradients: ['#64748B', '#475569', '#1E293B'], effect: 'cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'CLOUDY_DAY' };
      } else {
        style = { gradients: ['#334155', '#1E293B', '#0F172A'], effect: 'cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'CLOUDY_EVENING' };
      }
      break;

    case WeatherCondition.RAIN:
    case WeatherCondition.LIGHT_RAIN:
    case WeatherCondition.HEAVY_RAIN:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0F172A', '#1E293B', '#0F172A'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_NIGHT' };
      } else {
        style = { gradients: ['#1E3A8A', '#1E40AF', '#172554'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_DAY' };
      }
      break;

    case WeatherCondition.SNOW:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0F172A', '#1E293B', '#E2E8F0'], effect: 'snow', isWhiteText: true, accent: '#E2E8F0', name: 'SNOW_NIGHT' };
      } else {
        style = { gradients: ['#F8FAFC', '#E2E8F0', '#94A3B8'], effect: 'snow', isWhiteText: false, accent: '#CBD5E1', name: 'SNOW_DAY' };
      }
      break;

    case WeatherCondition.FOG:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#1E293B', '#334155', '#475569'], effect: 'fog', isWhiteText: true, accent: '#F8FAFC', name: 'FOG_NIGHT' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#94A3B8', '#B1B1D8', '#E2E8F0'], effect: 'fog', isWhiteText: false, accent: '#94A3B8', name: 'FOG_MORNING' };
      } else {
        style = { gradients: ['#CBD5E1', '#94A3B8', '#64748B'], effect: 'fog', isWhiteText: false, accent: '#F8FAFC', name: 'FOG_DAY' };
      }
      break;

    case WeatherCondition.THUNDERSTORM:
      style = { gradients: ['#0F172A', '#1E1B4B', '#2E1065'], effect: 'thunder', isWhiteText: true, accent: '#A855F7', name: 'THUNDERSTORM' };
      break;

    case WeatherCondition.WIND:
      style = { gradients: ['#7DD3FC', '#38BDF8', '#0F172A'], effect: 'wind', isWhiteText: true, accent: '#F8FAFC', name: 'WIND_DAY' };
      break;

    default:
      style = { gradients: ['#38BDF8', '#1E40AF', '#FCD34D'], effect: 'sunny', isWhiteText: true, accent: '#FCD34D', name: 'FALLBACK_SUNNY' };
  }

  // Seasonal theme override
  if (theme === 'winter') {
    if (condition === WeatherCondition.SNOW) {
        style.gradients = ['#F0FBFF', '#D1EFFF', '#A9D6E5'];
        style.accent = '#EAF8FF';
    }
  } else if (theme === 'autumn' && timeOfDay === TimeOfDay.EVENING) {
    style.gradients = ['#D35400', '#E67E22', '#7F5A83'];
  } else if (theme === 'spring' && (condition === WeatherCondition.MOSTLY_CLOUDY || condition === WeatherCondition.CLOUDY)) {
    style.gradients = ['#E2F4E2', '#B7D8B7', '#8DA18D'];
  } else if (theme === 'summer' && condition === WeatherCondition.SUNNY) {
    style.accent = '#FFD700';
  }

  return style;
};

const getDisplayTitle = (weatherCode: number, isActuallyDay: boolean): string => {
  const isNight = !isActuallyDay;

  if (weatherCode === 0) return isNight ? "Açık Gece" : "Güneşli";
  if (weatherCode === 1 || weatherCode === 2) return isNight ? "Az Bulutlu Gece" : "Az Bulutlu";
  if (weatherCode === 3) return isNight ? "Bulutlu Gece" : "Bulutlu";
  if (weatherCode === 45 || weatherCode === 48) return "Sisli";
  if (weatherCode >= 51 && weatherCode <= 57) return "Çiseleyen Yağmur";
  if (weatherCode >= 61 && weatherCode <= 67) return isNight ? "Yağmurlu Gece" : "Yağmurlu";
  if (weatherCode >= 80 && weatherCode <= 82) return isNight ? "Sağanak Yağışlı Gece" : "Sağanak Yağış";
  if (weatherCode >= 95 && weatherCode <= 99) return isNight ? "Fırtınalı Gece" : "Fırtınalı";
  if ((weatherCode >= 71 && weatherCode <= 77) || weatherCode === 85 || weatherCode === 86) return isNight ? "Karlı Gece" : "Karlı";

  return isNight ? "Gece" : "Güneşli";
};

const getDisplayEmoji = (weatherCode: number, isActuallyDay: boolean): string => {
  const isNight = !isActuallyDay;

  if (weatherCode === 0) return isNight ? '🌙' : '☀️';
  if (weatherCode === 1 || weatherCode === 2) return isNight ? '☁️' : '🌤';
  if (weatherCode === 3) return '☁️';
  if (weatherCode === 45 || weatherCode === 48) return '🌫️';
  if (weatherCode >= 51 && weatherCode <= 67) return '🌧️';
  if (weatherCode >= 80 && weatherCode <= 82) return '🌧️';
  if (weatherCode >= 71 && weatherCode <= 77) return '🌨️';
  if (weatherCode >= 95) return '🌩';

  return isNight ? '🌙' : '☀️';
};

const Star = ({ p }: any) => {
  const individualBlink = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.loop(
      Animated.sequence([
        Animated.delay(p.blinkDelay),
        Animated.timing(individualBlink, { toValue: 1, duration: p.blinkSpeed, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
        Animated.timing(individualBlink, { toValue: 0, duration: p.blinkSpeed, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      ])
    ).start();
  }, []);

  return (
    <Animated.View style={[styles.star, {
      left: p.left,
      top: p.top,
      width: p.size,
      height: p.size,
      opacity: individualBlink.interpolate({
          inputRange: [0, 1],
          outputRange: [0.4, 0.8]
      })
    }]} />
  );
};

interface AtmosphericWeatherCardProps {
  city: string;
  temperature: number;
  description: string;
  high: number;
  low: number;
  feelsLike: number;
  weatherCode: number;
  isDay: boolean;
  humidity?: number;
  windSpeed?: number;
  uvIndex?: number;
  time?: string;
  sunrise?: string;
  sunset?: string;
  lastUpdated: string;
  C: AppColors;
}

export const AtmosphericWeatherCard: React.FC<AtmosphericWeatherCardProps> = ({
  city,
  temperature,
  description,
  high,
  low,
  feelsLike,
  weatherCode,
  isDay,
  humidity,
  windSpeed,
  uvIndex,
  time,
  sunrise,
  sunset,
}) => {
  const { theme } = useThemeStore();
  const [reducedMotion, setReducedMotion] = useState(false);

  const now = useMemo(() => (time ? new Date(time) : new Date()), [time]);
  const parsedHour = useMemo(() => now.getHours() + now.getMinutes() / 60, [now]);
  const sunriseDate = useMemo(() => (sunrise ? new Date(sunrise) : null), [sunrise]);
  const sunsetDate = useMemo(() => (sunset ? new Date(sunset) : null), [sunset]);

  const isActuallyDay = useMemo(() => {
    if (!sunriseDate || !sunsetDate) return parsedHour >= 6 && parsedHour < 19;
    return now >= sunriseDate && now < sunsetDate;
  }, [now, sunriseDate, sunsetDate, parsedHour]);

  const timeOfDay = useMemo(() => {
    if (!sunriseDate || !sunsetDate) {
        if (parsedHour >= 6 && parsedHour < 11) return TimeOfDay.MORNING;
        if (parsedHour >= 11 && parsedHour < 17) return TimeOfDay.DAY;
        if (parsedHour >= 17 && parsedHour < 20) return TimeOfDay.EVENING;
        return TimeOfDay.NIGHT;
    }
    if (now < sunriseDate) return TimeOfDay.NIGHT;
    if (now >= sunriseDate && now < new Date(sunriseDate.getTime() + 3 * 3600000)) return TimeOfDay.MORNING;
    if (now >= new Date(sunriseDate.getTime() + 3 * 3600000) && now < new Date(sunsetDate.getTime() - 2 * 3600000)) return TimeOfDay.DAY;
    if (now >= new Date(sunsetDate.getTime() - 2 * 3600000) && now < sunsetDate) return TimeOfDay.EVENING;
    return TimeOfDay.NIGHT;
  }, [now, sunriseDate, sunsetDate, parsedHour]);

  const rawCondition = useMemo(() => getRawCondition(weatherCode), [weatherCode]);
  const style = useMemo(() => resolveWeatherCardStyle(rawCondition, timeOfDay, theme), [rawCondition, timeOfDay, theme]);
  const displayTitle = getDisplayTitle(weatherCode, isActuallyDay);
  const displayEmoji = getDisplayEmoji(weatherCode, isActuallyDay);

  const sunProgress = useMemo(() => {
    if (!sunriseDate || !sunsetDate) {
        const p = (parsedHour - 6) / 13;
        return Math.max(0, Math.min(1, p));
    }
    const p = (now.getTime() - sunriseDate.getTime()) / (sunsetDate.getTime() - sunriseDate.getTime());
    return Math.max(0, Math.min(1, p));
  }, [now, sunriseDate, sunsetDate, parsedHour]);

  const masterAnim = useRef(new Animated.Value(0)).current;
  const fastAnim = useRef(new Animated.Value(0)).current;
  const pulseAnim = useRef(new Animated.Value(0)).current;
  const entryAnim = useRef(new Animated.Value(0)).current;
  const cloudOscillateLarge = useRef(new Animated.Value(0)).current;
  const cloudOscillateSmall = useRef(new Animated.Value(0)).current;
  const sunMoonBreath = useRef(new Animated.Value(0)).current;
  const bgShiftAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReducedMotion);
    if (reducedMotion) { entryAnim.setValue(1); return; }

    Animated.timing(entryAnim, { toValue: 1, duration: 800, easing: Easing.out(Easing.back(1)), useNativeDriver: true }).start();

    // Background Slow movement (26s)
    Animated.loop(Animated.timing(masterAnim, { toValue: 1, duration: 26000, easing: Easing.linear, useNativeDriver: true })).start();

    // Foreground movement (12s)
    Animated.loop(Animated.timing(fastAnim, { toValue: 1, duration: 12000, easing: Easing.linear, useNativeDriver: true })).start();

    Animated.loop(Animated.sequence([
      Animated.timing(pulseAnim, { toValue: 1, duration: 3000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      Animated.timing(pulseAnim, { toValue: 0, duration: 3000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
    ])).start();

    // Large Oscillation
    Animated.loop(Animated.sequence([
      Animated.timing(cloudOscillateLarge, { toValue: 1, duration: 8000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      Animated.timing(cloudOscillateLarge, { toValue: 0, duration: 8000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
    ])).start();

    // Small Oscillation
    Animated.loop(Animated.sequence([
      Animated.timing(cloudOscillateSmall, { toValue: 1, duration: 6000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      Animated.timing(cloudOscillateSmall, { toValue: 0, duration: 6000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
    ])).start();

    // Breath
    Animated.loop(Animated.sequence([
      Animated.timing(sunMoonBreath, { toValue: 1, duration: 4000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      Animated.timing(sunMoonBreath, { toValue: 0, duration: 4000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
    ])).start();

    Animated.loop(Animated.sequence([
      Animated.timing(bgShiftAnim, { toValue: 1, duration: 9000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      Animated.timing(bgShiftAnim, { toValue: 0, duration: 9000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
    ])).start();
  }, [reducedMotion]);

  const particles = useMemo(() => ({
    rain: [...Array(60)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      opacity: i % 3 === 0 ? 0.1 : i % 3 === 1 ? 0.2 : 0.35,
      speed: 1.2 + Math.random() * 1.5,
      length: i % 3 === 0 ? 15 : i % 3 === 1 ? 35 : 60,
      width: 0.5
    })),
    stars: [...Array(60)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      size: 0.5 + Math.random() * 1.5,
      opacity: 0.3 + Math.random() * 0.4,
      blinkSpeed: 2000 + Math.random() * 3000,
      blinkDelay: Math.random() * 5000
    })),
    snow: [...Array(40)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      size: 2 + Math.random() * 4,
      speed: 0.3 + Math.random() * 0.6,
      drift: Math.random() * 40 - 20
    })),
    clouds: [...Array(12)].map((_, i) => ({
        top: 5 + Math.random() * 120,
        left: -150 + Math.random() * 150,
        width: 180 + Math.random() * 220,
        height: 80 + Math.random() * 100,
        opacity: 0.1 + Math.random() * 0.15,
        speed: 0.8 + (i % 3) * 0.4,
        layer: i % 3,
        animType: i % 2 === 0 ? 'large' : 'small'
    }))
  }), []);

  const textColor = style.isWhiteText ? '#FFF' : '#0F172A';
  const secondaryColor = style.isWhiteText ? 'rgba(255,255,255,0.75)' : 'rgba(15,23,42,0.65)';

  // Adjusted SUN path to peak higher and stay out of way
  const sunX = (sunProgress * (SCREEN_WIDTH * 0.8)) + (SCREEN_WIDTH * 0.1);
  const sunY = 120 - (Math.sin(sunProgress * Math.PI) * 105);
  const sunScale = 0.75 + (Math.sin(sunProgress * Math.PI) * 0.3);
  const sunOpacity = isActuallyDay ? 1 : 0;
  const moonOpacity = !isActuallyDay ? 1 : 0;

  const renderAurora = (color: string, intensity: number = 1) => (
    <Animated.View style={[styles.auroraLayer, {
      backgroundColor: color,
      opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.005 * intensity, 0.015 * intensity] }),
      transform: [
        { scale: 1.8 },
        { translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-40, 40] }) }
      ]
    }]} />
  );

  const renderEffect = () => {
    if (reducedMotion) return null;
    let auroraIntensity = (style.effect === 'rain' || style.effect === 'thunder' || style.effect === 'night') ? 0.3 : 1;

    switch (style.effect) {
      case 'sunny':
        const cloudCount = rawCondition === WeatherCondition.MOSTLY_CLEAR ? 1 : (rawCondition === WeatherCondition.PARTLY_CLOUDY ? 3 : 0);
        return (
          <View style={StyleSheet.absoluteFill}>
            {renderAurora(timeOfDay === TimeOfDay.EVENING ? '#FB923C' : '#7DD3FC', auroraIntensity)}
            <Animated.View style={[styles.premiumSunSceneGlow, {
              backgroundColor: timeOfDay === TimeOfDay.EVENING ? '#EA580C' : '#FBBF24',
              opacity: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.12, 0.22] }),
              transform: [{ scale: 2.2 }, { translateX: sunX }, { translateY: sunY }]
            }]} />
            {sunOpacity > 0 && (
              <Animated.View style={[styles.premiumSunSceneDisk, {
                backgroundColor: timeOfDay === TimeOfDay.EVENING ? '#EA580C' : '#FCD34D',
                width: 80, height: 80, borderRadius: 40,
                opacity: sunOpacity,
                transform: [
                  { translateX: sunX }, { translateY: sunY },
                  { scale: sunScale },
                  { scale: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [1, 1.03] }) }
                ]
              }]}>
                <Animated.View style={[styles.premiumSunSceneInnerHighlight, {
                  backgroundColor: '#FFFBEB',
                  opacity: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.3, 0.5] }),
                  position: 'absolute', top: '15%', left: '15%', width: '35%', height: '35%', borderRadius: 20
                }]} />
              </Animated.View>
            )}
            {particles.clouds.slice(0, cloudCount).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top, width: c.width, height: c.height,
                    opacity: c.layer === 0 ? sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.45, 0.65] }) : c.opacity * (c.layer + 1.2),
                    transform: [
                        { translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [c.left, c.left + (80 * c.speed)] }) },
                        { translateX: c.animType === 'large' ? cloudOscillateLarge.interpolate({ inputRange: [0, 1], outputRange: [-18, 18] }) : cloudOscillateSmall.interpolate({ inputRange: [0, 1], outputRange: [14, -14] }) },
                        { scale: c.layer === 0 ? sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.98, 1.02] }) : 0.9 + (c.layer * 0.2) }
                    ]
                }]} />
            ))}
          </View>
        );
      case 'rain':
        return (
          <View style={StyleSheet.absoluteFill}>
            {renderAurora('#1E40AF', auroraIntensity)}
            {particles.rain.map((p, i) => (
              <Animated.View key={i} style={[styles.rainLine, {
                left: p.left, width: p.width, height: p.length, opacity: p.opacity,
                transform: [ { translateY: Animated.multiply(fastAnim, 600 * p.speed).interpolate({ inputRange: [0, 600], outputRange: [-150, 600] }) }, { rotate: '12deg' } ]
              }]} />
            ))}
            {particles.clouds.slice(0, 5).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top, width: c.width, height: c.height, backgroundColor: '#1E293B', opacity: 0.25,
                    transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [c.left, c.left + (50 * c.speed)] }) }]
                }]} />
            ))}
          </View>
        );
      case 'night':
        return (
          <View style={StyleSheet.absoluteFill}>
            {renderAurora('#0F172A', auroraIntensity)}
            {particles.stars.map((p, i) => <Star key={i} p={p} />)}
            {moonOpacity > 0 && (
              <>
                <Animated.View style={[styles.moonGlow, {
                    backgroundColor: '#F8FAFC', width: 120, height: 120, borderRadius: 60,
                    opacity: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.02, 0.05] }),
                    transform: [{ scale: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [1, 1.08] }) }]
                }]} />
                <Animated.View style={[styles.premiumSunSceneDisk, {
                    backgroundColor: '#F8FAFC', width: 60, height: 60, borderRadius: 30, top: 50, right: '20%',
                    opacity: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.85, 1.0] }),
                    transform: [{ scale: sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [1, 1.02] }) }],
                    shadowColor: '#FFF', shadowRadius: 40, shadowOpacity: 0.6
                }]}>
                    <View style={{ backgroundColor: 'rgba(0,0,0,0.05)', width: 15, height: 15, borderRadius: 10, position: 'absolute', top: 10, left: 10 }} />
                </Animated.View>
              </>
            )}
          </View>
        );
      case 'snow':
        return (
          <View style={StyleSheet.absoluteFill}>
            {particles.snow.map((p, i) => (
              <Animated.View key={i} style={[styles.snowFlake, {
                left: p.left, width: p.size, height: p.size, opacity: 0.6,
                transform: [ { translateY: Animated.multiply(fastAnim, 400 * p.speed).interpolate({ inputRange: [0, 400], outputRange: [-20, 400] }) }, { translateX: Animated.multiply(fastAnim, p.drift).interpolate({ inputRange: [0, p.drift], outputRange: [0, p.drift] }) } ]
              }]} />
            ))}
          </View>
        );
      case 'cloudy':
      case 'partly_cloudy':
        return (
          <View style={StyleSheet.absoluteFill}>
            {renderAurora(style.isWhiteText ? '#1E293B' : '#7DD3FC', auroraIntensity)}
            {particles.clouds.slice(0, rawCondition === WeatherCondition.MOSTLY_CLOUDY ? 10 : 4).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top, width: c.width, height: c.height,
                    opacity: c.layer === 0 ? sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.45, 0.65] }) : c.opacity * (style.effect === 'cloudy' ? 2 : 1.5),
                    backgroundColor: style.isWhiteText ? '#CBD5E1' : '#F8FAFC',
                    transform: [
                        { translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [c.left, c.left + (120 * c.speed)] }) },
                        { translateX: c.animType === 'large' ? cloudOscillateLarge.interpolate({ inputRange: [0, 1], outputRange: [-18, 18] }) : cloudOscillateSmall.interpolate({ inputRange: [0, 1], outputRange: [14, -14] }) },
                        { scale: c.layer === 0 ? sunMoonBreath.interpolate({ inputRange: [0, 1], outputRange: [0.98, 1.02] }) : 0.85 + (c.layer * 0.15) }
                    ]
                }]} />
            ))}
          </View>
        );
      case 'fog':
        const isFogNight = timeOfDay === TimeOfDay.NIGHT;
        const isFogMorning = timeOfDay === TimeOfDay.MORNING;
        return (
            <View style={StyleSheet.absoluteFill}>
                {renderAurora(isFogNight ? '#1E293B' : (isFogMorning ? '#B1B1D8' : '#94A3B8'), auroraIntensity)}
                {isFogNight && particles.stars.slice(0, 12).map((p, i) => <Star key={i} p={p} />)}
                {[0.04, 0.06].map((op, i) => (
                    <Animated.View key={i} style={[styles.fogLayer, {
                        backgroundColor: isFogNight ? '#1E293B' : (isFogMorning ? '#D1D1E8' : '#F1F5F9'),
                        opacity: op,
                        transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [i * -40, i * 40] }) }]
                    }]} />
                ))}
            </View>
        );
      case 'thunder':
        return (
          <View style={StyleSheet.absoluteFill}>
            {renderAurora('#4C1D95', auroraIntensity)}
            {particles.clouds.slice(0, 3).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, { top: c.top, width: c.width, height: c.height, backgroundColor: '#1E1B4B', opacity: 0.5, transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [c.left, c.left + (30 * c.speed)] }) }] }]} />
            ))}
            {particles.rain.slice(0, 30).map((p, i) => (
              <Animated.View key={i} style={[styles.rainLine, {
                left: p.left, width: p.width * 1.5, height: p.length * 1.2, backgroundColor: '#CBD5E1', opacity: p.opacity * 1.5,
                transform: [ { translateY: Animated.multiply(fastAnim, 550 * p.speed).interpolate({ inputRange: [0, 550], outputRange: [-100, 550] }) }, { rotate: '20deg' } ]
              }]} />
            ))}
            <Animated.View style={[StyleSheet.absoluteFill, { backgroundColor: '#DDD6FE', opacity: masterAnim.interpolate({ inputRange: [0, 0.4, 0.41, 0.42, 0.8, 0.81, 0.82, 1], outputRange: [0, 0, 0.12, 0, 0, 0.15, 0, 0] }) }]} />
          </View>
        );
      default: return null;
    }
  };

  return (
    <Animated.View style={[styles.outerContainer, { opacity: entryAnim, transform: [{ scale: entryAnim.interpolate({ inputRange: [0, 1], outputRange: [0.95, 1] }) }] }]}>
      <LinearGradient colors={style.gradients} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.card}>
        <View style={StyleSheet.absoluteFill}>
          {renderEffect()}
          <Animated.View style={[StyleSheet.absoluteFill, { backgroundColor: '#FFF', opacity: bgShiftAnim.interpolate({ inputRange: [0, 1], outputRange: [0, 0.03] }) }]} />
          <View style={styles.glassOverlay} />
        </View>
        <View style={styles.content}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.cityText, { color: secondaryColor }]}>{city.toUpperCase()}</Text>
              <Text style={[styles.conditionText, { color: textColor }]}>{displayTitle}</Text>
            </View>
            <View style={[styles.iconContainer, (weatherCode === 0 || weatherCode === 1) && { opacity: 0.2, backgroundColor: 'transparent', borderColor: 'transparent' }]}>
              <Text style={styles.weatherEmoji}>{displayEmoji}</Text>
            </View>
          </View>
          <View style={styles.mainInfo}>
            <Text style={[styles.tempText, { color: textColor }]}>{Math.round(temperature)}°</Text>
            <Text style={[styles.feelsLikeText, { color: secondaryColor, marginTop: -15 }]}>Hissedilen {Math.round(feelsLike)}°</Text>
          </View>
          <View style={styles.glassBar}>
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>NEM</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>%{humidity ?? '--'}</Text>
            </View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>RÜZGAR</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>{windSpeed ?? '--'} km/s</Text>
            </View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>UV</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>{isActuallyDay ? (uvIndex ?? '--') : '--'}</Text>
            </View>
          </View>
        </View>
      </LinearGradient>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  outerContainer: { marginHorizontal: Spacing.md, marginBottom: Spacing.lg, borderRadius: 32, shadowColor: '#000', shadowOffset: { width: 0, height: 12 }, shadowOpacity: 0.3, shadowRadius: 20, elevation: 12 },
  card: { height: 320, borderRadius: 32, overflow: 'hidden', padding: 24, borderWidth: 1, borderColor: 'rgba(255,255,255,0.15)' },
  glassOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: 32, borderTopWidth: 1, borderLeftWidth: 1, borderColor: 'rgba(255,255,255,0.2)' },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 20 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cityText: { fontSize: 13, fontWeight: '900', letterSpacing: 2, opacity: 0.8 },
  conditionText: { fontSize: 28, fontWeight: '800', marginTop: 2, textShadowColor: 'rgba(0,0,0,0.15)', textShadowOffset: {width: 0, height: 1}, textShadowRadius: 3 },
  iconContainer: { width: 52, height: 52, justifyContent: 'center', alignItems: 'center', borderRadius: 26, backgroundColor: 'rgba(255,255,255,0.12)', borderWidth: 0.5, borderColor: 'rgba(255,255,255,0.2)', shadowColor: '#000', shadowOffset: {width: 0, height: 4}, shadowOpacity: 0.1, shadowRadius: 4 },
  weatherEmoji: { fontSize: 30 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 115, fontWeight: '100', letterSpacing: -5, lineHeight: 115, textShadowColor: 'rgba(0,0,0,0.1)', textShadowOffset: {width: 0, height: 2}, textShadowRadius: 6 },
  feelsLikeText: { fontSize: 16, fontWeight: '700', opacity: 0.9 },
  glassBar: { height: 68, backgroundColor: 'rgba(255, 255, 255, 0.1)', borderRadius: 24, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 20, borderWidth: 1.5, borderColor: 'rgba(255,255,255,0.2)', shadowColor: '#000', shadowOffset: {width: 0, height: 4}, shadowOpacity: 0.1, shadowRadius: 10 },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 11, fontWeight: '900', letterSpacing: 1.5, opacity: 0.8 },
  infoValue: { fontSize: 18, fontWeight: '900', marginTop: 2 },
  infoDivider: { width: 1, height: 28, backgroundColor: 'rgba(255,255,255,0.15)' },
  premiumSunSceneDisk: { position: 'absolute', zIndex: 10, justifyContent: 'center', alignItems: 'center', elevation: 10 },
  premiumSunSceneInnerHighlight: { width: '70%', height: '75%', borderRadius: 50 },
  premiumSunSceneGlow: { position: 'absolute', width: 260, height: 260, borderRadius: 130, zIndex: 1 },
  premiumSunSceneRaysContainer: { position: 'absolute', width: 140, height: 140, justifyContent: 'center', alignItems: 'center', zIndex: 2 },
  premiumSunSceneRay: { position: 'absolute', width: 3, borderRadius: 3 },
  auroraLayer: { position: 'absolute', width: SCREEN_WIDTH, height: 240, borderRadius: 120, top: 40, left: -20, zIndex: 0, opacity: 0.05 },
  sunGlowMini: { position: 'absolute', top: 30, right: 30, width: 80, height: 80, borderRadius: 40 },
  moonGlow: { position: 'absolute', top: 50, right: '20%', width: 85, height: 85, borderRadius: 42.5, shadowColor: '#FFF', shadowOffset: {width: 0, height: 0}, shadowOpacity: 0.5, shadowRadius: 30 },
  star: { position: 'absolute', backgroundColor: '#F8FAFC', borderRadius: 5 },
  rainLine: { position: 'absolute', width: 1, backgroundColor: '#F1F5F9', borderRadius: 1 },
  snowFlake: { position: 'absolute', backgroundColor: '#F8FAFC', borderRadius: 10 },
  cloudHaze: { position: 'absolute', backgroundColor: '#F8FAFC', borderRadius: 100, shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.1, shadowRadius: 15 },
  fogLayer: { ...StyleSheet.absoluteFillObject },
  windLine: { position: 'absolute', height: 1.5, backgroundColor: '#F1F5F9', borderRadius: 1 },
});
