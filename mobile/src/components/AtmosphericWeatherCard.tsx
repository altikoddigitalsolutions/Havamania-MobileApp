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
  MOSTLY_SUNNY = 'MOSTLY_SUNNY',
  CLEAR = 'CLEAR',
  CLEAR_NIGHT = 'CLEAR_NIGHT',
  CLOUDY = 'CLOUDY',
  CLOUDY_NIGHT = 'CLOUDY_NIGHT',
  PARTLY_CLOUDY = 'PARTLY_CLOUDY',
  PARTLY_CLOUDY_NIGHT = 'PARTLY_CLOUDY_NIGHT',
  RAIN = 'RAIN',
  RAIN_NIGHT = 'RAIN_NIGHT',
  SNOW = 'SNOW',
  SNOW_NIGHT = 'SNOW_NIGHT',
  FOG = 'FOG',
  FOG_NIGHT = 'FOG_NIGHT',
  THUNDERSTORM = 'THUNDERSTORM',
  WIND = 'WIND'
}

export enum TimeOfDay {
  DAWN = 'DAWN',
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
  if (code === 1) return WeatherCondition.MOSTLY_SUNNY;
  if (code === 2) return WeatherCondition.PARTLY_CLOUDY;
  if (code === 3) return WeatherCondition.CLOUDY;
  if (code === 45 || code === 48) return WeatherCondition.FOG;
  if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return WeatherCondition.RAIN;
  if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) return WeatherCondition.SNOW;
  if (code >= 95) return WeatherCondition.THUNDERSTORM;
  return WeatherCondition.SUNNY;
};

const getTimeOfDay = (hour: number, isDay: boolean): TimeOfDay => {
  // Gece Zorlaması: 21:00 sonrası veya isDay false ise (gece yarısı civarı)
  if (hour >= 21 || hour < 5) return TimeOfDay.NIGHT;
  if (!isDay && (hour >= 18 || hour < 6)) return TimeOfDay.NIGHT;

  if (hour >= 5 && hour < 7) return TimeOfDay.DAWN;
  if (hour >= 7 && hour < 11) return TimeOfDay.MORNING;
  if (hour >= 11 && hour < 17) return TimeOfDay.DAY;
  if (hour >= 17 && hour < 21) return TimeOfDay.EVENING;

  return TimeOfDay.NIGHT;
};

// KESİN MANTIK: NORMALIZE CONDITION
const normalizeConditionForDisplay = (
  rawCondition: WeatherCondition,
  timeOfDay: TimeOfDay
): WeatherCondition => {
  if (timeOfDay === TimeOfDay.NIGHT) {
    if (rawCondition === WeatherCondition.SUNNY || rawCondition === WeatherCondition.MOSTLY_SUNNY || rawCondition === WeatherCondition.CLEAR) {
      return WeatherCondition.CLEAR_NIGHT;
    }
    if (rawCondition === WeatherCondition.PARTLY_CLOUDY) {
      return WeatherCondition.PARTLY_CLOUDY_NIGHT;
    }
    if (rawCondition === WeatherCondition.CLOUDY) {
      return WeatherCondition.CLOUDY_NIGHT;
    }
    if (rawCondition === WeatherCondition.RAIN) {
      return WeatherCondition.RAIN_NIGHT;
    }
    if (rawCondition === WeatherCondition.SNOW) {
      return WeatherCondition.SNOW_NIGHT;
    }
    if (rawCondition === WeatherCondition.FOG) {
      return WeatherCondition.FOG_NIGHT;
    }
  }
  return rawCondition;
};

const resolveWeatherCardStyle = (
  condition: WeatherCondition,
  timeOfDay: TimeOfDay,
  theme: Theme
): WeatherCardStyle => {
  let style: WeatherCardStyle;

  switch (condition) {
    case WeatherCondition.SUNNY:
    case WeatherCondition.CLEAR:
      if (timeOfDay === TimeOfDay.DAWN) {
        style = { gradients: ['#A5C9CA', '#E7F6F2', '#FFEFBA'], effect: 'sunny', isWhiteText: false, accent: '#FFE29A', name: 'SUNNY_DAWN' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#8ECAE6', '#CFE7F3', '#FBFAFF'], effect: 'sunny', isWhiteText: false, accent: '#FFB703', name: 'SUNNY_MORNING' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#FFB703', '#FB8500', '#8E44AD'], effect: 'sunny', isWhiteText: true, accent: '#FFB703', name: 'SUNNY_EVENING' };
      } else if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#023047', '#051923', '#000814'], effect: 'night', isWhiteText: true, accent: '#FFF', name: 'CLEAR_NIGHT' };
      } else {
        style = { gradients: ['#0096C7', '#48CAE4', '#90E0EF'], effect: 'sunny', isWhiteText: true, accent: '#FFD166', name: 'SUNNY_DAY' };
      }
      break;

    case WeatherCondition.MOSTLY_SUNNY:
      if (timeOfDay === TimeOfDay.DAWN) {
        style = { gradients: ['#B5E5F9', '#D9E9F2', '#FFF5E1'], effect: 'sunny', isWhiteText: false, accent: '#FFE29A', name: 'MOSTLY_SUNNY_DAWN' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#A9DDF8', '#E3F2FD'], effect: 'sunny', isWhiteText: false, accent: '#FFB703', name: 'MOSTLY_SUNNY_MORNING' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#F39C12', '#D35400', '#2C3E50'], effect: 'sunny', isWhiteText: true, accent: '#FFB703', name: 'MOSTLY_SUNNY_EVENING' };
      } else if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#1A2A6C', '#23395B', '#000000'], effect: 'night', isWhiteText: true, accent: '#FFF', name: 'MOSTLY_SUNNY_NIGHT' };
      } else {
        style = { gradients: ['#0077B6', '#00B4D8', '#CAF0F8'], effect: 'sunny', isWhiteText: true, accent: '#FFD166', name: 'MOSTLY_SUNNY_DAY' };
      }
      break;

    case WeatherCondition.CLEAR_NIGHT:
      style = { gradients: ['#000814', '#001D3D', '#003566'], effect: 'night', isWhiteText: true, accent: '#FFD60A', name: 'CLEAR_NIGHT' };
      break;

    case WeatherCondition.RAIN:
    case WeatherCondition.RAIN_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0B132B', '#1C2541', '#3A506B'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_NIGHT' };
      } else {
        style = { gradients: ['#1B4965', '#5FA8D3', '#62B6CB'], effect: 'rain', isWhiteText: true, accent: '#BEE9E8', name: 'RAIN_DAY' };
      }
      break;

    case WeatherCondition.CLOUDY:
    case WeatherCondition.CLOUDY_NIGHT:
      if (timeOfDay === TimeOfDay.DAWN) {
        style = { gradients: ['#BDC3C7', '#EBEFEF'], effect: 'cloudy', isWhiteText: false, accent: '#FFF', name: 'CLOUDY_DAWN' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#D7E1EC', '#FFFFFF'], effect: 'cloudy', isWhiteText: false, accent: '#FFF', name: 'CLOUDY_MORNING' };
      } else if (timeOfDay === TimeOfDay.DAY) {
        style = { gradients: ['#7F8C8D', '#BDC3C7', '#ECF0F1'], effect: 'cloudy', isWhiteText: false, accent: '#FFF', name: 'CLOUDY_DAY' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#485563', '#29323C'], effect: 'cloudy', isWhiteText: true, accent: '#FFF', name: 'CLOUDY_EVENING' };
      } else {
        style = { gradients: ['#141E30', '#243B55'], effect: 'cloudy', isWhiteText: true, accent: '#FFF', name: 'CLOUDY_NIGHT' };
      }
      break;

    case WeatherCondition.PARTLY_CLOUDY:
    case WeatherCondition.PARTLY_CLOUDY_NIGHT:
      if (timeOfDay === TimeOfDay.DAWN) {
        style = { gradients: ['#A9DDF5', '#F8F9FA'], effect: 'sunny', isWhiteText: false, accent: '#FFF', name: 'PARTLY_CLOUDY_DAWN' };
      } else if (timeOfDay === TimeOfDay.MORNING || timeOfDay === TimeOfDay.DAY) {
        style = { gradients: ['#48CAE4', '#CAF0F8', '#F8F9FA'], effect: 'sunny', isWhiteText: false, accent: '#FFF', name: 'PARTLY_CLOUDY_DAY' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#F2994A', '#F2C94C', '#2C3E50'], effect: 'sunny', isWhiteText: true, accent: '#F2A65A', name: 'PARTLY_CLOUDY_EVENING' };
      } else {
        style = { gradients: ['#0F2027', '#203A43', '#2C5364'], effect: 'partly_cloudy', isWhiteText: true, accent: '#FFF', name: 'PARTLY_CLOUDY_NIGHT' };
      }
      break;

    case WeatherCondition.SNOW:
    case WeatherCondition.SNOW_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0F2027', '#E0EAFC'], effect: 'snow', isWhiteText: true, accent: '#A9D6E5', name: 'SNOW_NIGHT' };
      } else {
        style = { gradients: ['#E0EAFC', '#CFDEF3'], effect: 'snow', isWhiteText: false, accent: '#7DB7D9', name: 'SNOW_DAY' };
      }
      break;

    case WeatherCondition.FOG:
    case WeatherCondition.FOG_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#232526', '#414345'], effect: 'fog', isWhiteText: true, accent: '#FFF', name: 'FOG_NIGHT' };
      } else {
        style = { gradients: ['#D7D2CC', '#304352'], effect: 'fog', isWhiteText: false, accent: '#FFF', name: 'FOG_DAY' };
      }
      break;

    case WeatherCondition.THUNDERSTORM:
      style = { gradients: ['#0F0C29', '#302B63', '#24243E'], effect: 'thunder', isWhiteText: true, accent: '#A78BFA', name: 'THUNDERSTORM' };
      break;

    case WeatherCondition.WIND:
      style = { gradients: ['#3A6073', '#162221'], effect: 'wind', isWhiteText: true, accent: '#FFF', name: 'WIND_DAY' };
      break;

    default:
      style = { gradients: ['#56CCF2', '#2F80ED', '#FBCB45'], effect: 'sunny', isWhiteText: true, accent: '#FBCB45', name: 'FALLBACK_SUNNY' };
  }

  // TEMA UYUMU
  if (theme === 'winter') {
    if (condition === WeatherCondition.SNOW || condition === WeatherCondition.SNOW_NIGHT) {
        style.gradients = ['#F0FBFF', '#D1EFFF', '#A9D6E5'];
        style.accent = '#EAF8FF';
    }
  } else if (theme === 'autumn' && timeOfDay === TimeOfDay.EVENING) {
    style.gradients = ['#D35400', '#E67E22', '#7F5A83'];
  } else if (theme === 'spring' && (condition === WeatherCondition.CLOUDY || condition === WeatherCondition.CLOUDY_NIGHT)) {
    style.gradients = ['#E2F4E2', '#B7D8B7', '#8DA18D'];
  } else if (theme === 'summer' && condition === WeatherCondition.SUNNY) {
    style.accent = '#FFD700';
  }

  return style;
};

const getDisplayTitle = (condition: WeatherCondition, timeOfDay: TimeOfDay, originalDescription: string): string => {
  // Gece Koşulları Normalizasyonu
  if (timeOfDay === TimeOfDay.NIGHT) {
    switch (condition) {
      case WeatherCondition.SUNNY:
      case WeatherCondition.CLEAR:
      case WeatherCondition.CLEAR_NIGHT:
        return 'Açık Gece';
      case WeatherCondition.PARTLY_CLOUDY:
      case WeatherCondition.PARTLY_CLOUDY_NIGHT:
        return 'Parçalı Bulutlu Gece';
      case WeatherCondition.CLOUDY:
      case WeatherCondition.CLOUDY_NIGHT:
        return 'Bulutlu Gece';
      case WeatherCondition.RAIN:
      case WeatherCondition.RAIN_NIGHT:
        return 'Yağmurlu Gece';
      case WeatherCondition.SNOW:
      case WeatherCondition.SNOW_NIGHT:
        return 'Karlı Gece';
      case WeatherCondition.FOG:
      case WeatherCondition.FOG_NIGHT:
        return 'Sisli Gece';
    }
  }

  // Şafak Koşulları
  if (timeOfDay === TimeOfDay.DAWN) {
    if (condition === WeatherCondition.SUNNY || condition === WeatherCondition.CLEAR) {
      return 'Açık Şafak';
    }
  }

  // Akşam/Gün Batımı Koşulları
  if (timeOfDay === TimeOfDay.EVENING) {
    if (condition === WeatherCondition.SUNNY || condition === WeatherCondition.CLEAR) {
      return 'Açık Akşam';
    }
  }

  // Gündüz Koşulları
  if (timeOfDay === TimeOfDay.DAY || timeOfDay === TimeOfDay.MORNING) {
    if (condition === WeatherCondition.SUNNY) {
      return 'Güneşli';
    }
    if (condition === WeatherCondition.MOSTLY_SUNNY || condition === WeatherCondition.CLEAR) {
      return 'Çoğunlukla Güneşli';
    }
  }

  // Fallback: Gece vakti hala "Açık" veya "Güneşli" geliyorsa zorla düzelt
  if (timeOfDay === TimeOfDay.NIGHT && (originalDescription === 'Açık' || originalDescription === 'Güneşli')) {
    return 'Açık Gece';
  }

  return originalDescription;
};

const getDisplayEmoji = (condition: WeatherCondition, code: number): string => {
  switch (condition) {
    case WeatherCondition.CLEAR_NIGHT:
      return '🌙';
    case WeatherCondition.PARTLY_CLOUDY_NIGHT:
    case WeatherCondition.CLOUDY_NIGHT:
      return '☁️';
    case WeatherCondition.RAIN_NIGHT:
      return '🌧️';
    case WeatherCondition.SNOW_NIGHT:
      return '❄️';
    case WeatherCondition.FOG_NIGHT:
      return '🌫️';
    default:
      return getWeatherEmoji(code);
  }
};

const parseHour = (timeStr?: string): number => {
    if (!timeStr) return new Date().getHours();

    const s = String(timeStr);
    // Explicitly handle "24:00" as 0
    if (s.startsWith('24')) return 0;

    // ISO String check (e.g. 2023-10-27T23:00)
    if (s.includes('T')) {
        const d = new Date(s);
        return isNaN(d.getTime()) ? new Date().getHours() : d.getHours();
    }

    // HH:mm check
    const parts = s.split(':');
    if (parts.length >= 1) {
        const h = parseInt(parts[0], 10);
        return isNaN(h) ? new Date().getHours() : h;
    }

    return new Date().getHours();
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
  time?: string;
  lastUpdated: string;
  C: AppColors;
}

// --- PREMIUM SUN SCENE CONFIG ---
const SUNNY_DEBUG_MODE = false; // Set to true to force sun rendering for testing

export const AtmosphericWeatherCard: React.FC<AtmosphericWeatherCardProps> = ({
  city,
  temperature,
  description,
  high,
  low,
  feelsLike,
  weatherCode,
  isDay,
  time,
}) => {
  const { theme } = useThemeStore();
  const [reducedMotion, setReducedMotion] = useState(false);

  // LOGIC FIX: Resolve Time and Normalized Condition
  const parsedHour = useMemo(() => parseHour(time), [time]);
  const timeOfDay = useMemo(() => getTimeOfDay(parsedHour, isDay), [parsedHour, isDay]);
  const rawCondition = useMemo(() => getRawCondition(weatherCode), [weatherCode]);
  const displayCondition = useMemo(() => normalizeConditionForDisplay(rawCondition, timeOfDay), [rawCondition, timeOfDay]);
  const style = useMemo(() => resolveWeatherCardStyle(displayCondition, timeOfDay, theme), [displayCondition, timeOfDay, theme]);

  const isSunnyScene = SUNNY_DEBUG_MODE ||
    displayCondition === WeatherCondition.SUNNY ||
    displayCondition === WeatherCondition.MOSTLY_SUNNY ||
    (displayCondition === WeatherCondition.PARTLY_CLOUDY && timeOfDay !== TimeOfDay.NIGHT);

  const displayTitle = getDisplayTitle(displayCondition, timeOfDay, description);
  const displayEmoji = getDisplayEmoji(displayCondition, weatherCode);

  // MANDATORY DEBUG LOG
  useEffect(() => {
    console.log('WeatherCardResolve:');
    console.log(`  rawCondition=${rawCondition}`);
    console.log(`  displayCondition=${displayCondition}`);
    console.log(`  selectedHour=${time || 'NOW'}`);
    console.log(`  parsedHour=${parsedHour}`);
    console.log(`  timeOfDay=${timeOfDay}`);
    console.log(`  style=${style.name}`);
    console.log(`  title=${displayTitle}`);
  }, [rawCondition, displayCondition, time, parsedHour, timeOfDay, style.name, displayTitle]);

  // Animations
  const masterAnim = useRef(new Animated.Value(0)).current;
  const pulseAnim = useRef(new Animated.Value(0)).current;
  const entryAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReducedMotion);

    if (reducedMotion) {
      entryAnim.setValue(1);
      return;
    }

    Animated.timing(entryAnim, {
      toValue: 1,
      duration: 800,
      easing: Easing.out(Easing.back(1)),
      useNativeDriver: true,
    }).start();

    Animated.loop(
      Animated.timing(masterAnim, {
        toValue: 1,
        duration: 10000,
        easing: Easing.linear,
        useNativeDriver: true,
      })
    ).start();

    Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, { toValue: 1, duration: 3000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
        Animated.timing(pulseAnim, { toValue: 0, duration: 3000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      ])
    ).start();
  }, [reducedMotion]);

  const particles = useMemo(() => ({
    rain: [...Array(25)].map((_, i) => ({
      left: `${(i * 4) % 100}%`,
      opacity: 0.08 + Math.random() * 0.08,
      speed: 0.8 + Math.random() * 0.4,
      offset: Math.random()
    })),
    stars: [...Array(20)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      size: 1 + Math.random() * 1.5,
      opacity: 0.3 + Math.random() * 0.4
    })),
    snow: [...Array(30)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      size: 2 + Math.random() * 3,
      speed: 0.3 + Math.random() * 0.3,
      drift: Math.random() * 30 - 15,
      offset: Math.random()
    }))
  }), []);

  const textColor = style.isWhiteText ? '#FFF' : '#0F172A';
  const secondaryColor = style.isWhiteText ? 'rgba(255,255,255,0.75)' : 'rgba(15,23,42,0.65)';

  const renderEffect = () => {
    if (reducedMotion) return null;

    switch (style.effect) {
      case 'sunny':
        const isMostlySunny = displayCondition === WeatherCondition.MOSTLY_SUNNY;
        const isEvening = timeOfDay === TimeOfDay.EVENING;
        const isDawn = timeOfDay === TimeOfDay.DAWN;
        const isPartly = displayCondition === WeatherCondition.PARTLY_CLOUDY;

        return (
          <View style={StyleSheet.absoluteFill}>
            {/* 1. OUTER ATMOSPHERIC GLOW */}
            <Animated.View style={[styles.premiumSunSceneGlow, {
              backgroundColor: isEvening ? '#FF8A3D' : (isDawn ? '#FFEFBA' : '#FFB703'),
              opacity: pulseAnim.interpolate({
                inputRange: [0, 1],
                outputRange: [isDawn ? 0.15 : 0.22, isDawn ? 0.25 : 0.32]
              }),
              transform: [{ scale: 1.2 }]
            }]} />

            {/* 2. INNER GLOW */}
            <Animated.View style={[styles.premiumSunSceneGlow, {
              width: 150, height: 150, borderRadius: 75,
              backgroundColor: isDawn ? '#FFF5E1' : '#FFD166',
              opacity: pulseAnim.interpolate({
                inputRange: [0, 1],
                outputRange: [0.30, 0.40]
              }),
              transform: [{ scale: 1.1 }]
            }]} />

            {/* 3. ROTATING RAYS */}
            <Animated.View style={[styles.premiumSunSceneRaysContainer, {
              opacity: isPartly ? 0.25 : 0.45,
              transform: [
                { rotate: masterAnim.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] }) },
              ]
            }]}>
               {[...Array(12)].map((_, i) => (
                 <View key={i} style={[styles.premiumSunSceneRay, {
                   backgroundColor: '#FFE8A3',
                   height: 18,
                   transform: [{ rotate: `${i * 30}deg` }, { translateY: 48 }]
                 }]} />
               ))}
            </Animated.View>

            {/* 4. MANDATORY SUN DISK (Alpha 1.0) */}
            <Animated.View style={[styles.premiumSunSceneDisk, {
              backgroundColor: '#FFD166',
              opacity: 1, // Fixed visibility
              transform: [
                { scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.04] }) }
              ]
            }]}>
              {/* 5. SUN HIGHLIGHT */}
              <View style={[styles.premiumSunSceneInnerHighlight, {
                backgroundColor: '#FFF4C2',
                opacity: 0.65,
                position: 'absolute',
                top: '15%',
                left: '15%',
                width: '35%',
                height: '35%',
                borderRadius: 15
              }]} />
            </Animated.View>

            {/* 6. CLOUD HAZE (For Mostly Sunny) */}
            {(isMostlySunny || isPartly) && (
              <Animated.View style={[StyleSheet.absoluteFill, {
                zIndex: 4,
                opacity: isPartly ? 0.4 : 0.25,
                transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-20, 20] }) }]
              }]}>
                <View style={[styles.cloudHaze, { top: 80, right: 20, width: 160, height: 80, opacity: 0.2 }]} />
                <View style={[styles.cloudHaze, { top: 140, left: 60, width: 120, height: 60, opacity: 0.15 }]} />
              </Animated.View>
            )}
          </View>
        );

      case 'rain':
        return (
          <View style={StyleSheet.absoluteFill}>
            {particles.rain.map((p, i) => (
              <Animated.View key={i} style={[styles.rainLine, {
                left: p.left,
                opacity: p.opacity,
                transform: [
                    { translateY: masterAnim.interpolate({
                        inputRange: [0, 1],
                        outputRange: [-50, 350]
                    }) },
                    { rotate: '15deg' }
                ]
              }]} />
            ))}
          </View>
        );

      case 'night':
        return (
          <View style={StyleSheet.absoluteFill}>
            {particles.stars.map((p, i) => (
              <Animated.View key={i} style={[styles.star, {
                left: p.left,
                top: p.top,
                width: p.size,
                height: p.size,
                opacity: pulseAnim.interpolate({
                  inputRange: [0, 0.5, 1],
                  outputRange: [p.opacity, p.opacity * 1.5, p.opacity]
                })
              }]} />
            ))}
            <View style={styles.moonContainer}>
              <View style={styles.moonMain} />
              <View style={[styles.moonShadow, { backgroundColor: style.gradients[0] }]} />
            </View>
          </View>
        );
                top: p.top,
                width: p.size,
                height: p.size,
                opacity: p.opacity
              }]} />
            ))}
            <View style={[styles.moonGlow, { backgroundColor: '#FFF', opacity: 0.05 }]} />
          </View>
        );

      case 'snow':
        return (
          <View style={StyleSheet.absoluteFill}>
            {particles.snow.map((p, i) => (
              <Animated.View key={i} style={[styles.snowFlake, {
                left: p.left,
                width: p.size,
                height: p.size,
                opacity: 0.6,
                transform: [
                  { translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-20, 340] }) },
                  { translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [0, p.drift] }) }
                ]
              }]} />
            ))}
          </View>
        );

      case 'cloudy':
      case 'partly_cloudy':
        return (
          <Animated.View style={[StyleSheet.absoluteFill, {
            transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-20, 20] }) }]
          }]}>
            <View style={[styles.cloudHaze, { top: 30, left: -20, width: 250, height: 150, opacity: 0.08 }]} />
            <View style={[styles.cloudHaze, { bottom: 40, right: -40, width: 300, height: 180, opacity: 0.12 }]} />
            <View style={[styles.cloudHaze, { top: 100, right: 20, width: 180, height: 90, opacity: 0.05 }]} />
            {style.effect === 'partly_cloudy' && (
              <View style={[styles.sunGlowMini, { backgroundColor: style.accent, opacity: 0.2 }]} />
            )}
          </Animated.View>
        );

      case 'fog':
        return <View style={[styles.fogLayer, { backgroundColor: '#FFF', opacity: 0.08 }]} />;

      case 'thunder':
        return (
          <Animated.View style={[StyleSheet.absoluteFill, {
            backgroundColor: '#FFF',
            opacity: masterAnim.interpolate({
              inputRange: [0, 0.45, 0.47, 0.49, 0.51, 0.53, 1],
              outputRange: [0, 0, 0.25, 0, 0.2, 0, 0]
            })
          }]} />
        );

      case 'wind':
        return (
            <Animated.View style={[StyleSheet.absoluteFill, {
                transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-100, SCREEN_WIDTH] }) }]
            }]}>
                <View style={[styles.windLine, { top: 100, width: 80, opacity: 0.1 }]} />
                <View style={[styles.windLine, { top: 180, width: 120, opacity: 0.08 }]} />
                <View style={[styles.windLine, { top: 240, width: 60, opacity: 0.12 }]} />
            </Animated.View>
        );

      default:
        return null;
    }
  };

  return (
    <Animated.View style={[styles.outerContainer, {
      opacity: entryAnim,
      transform: [{ scale: entryAnim.interpolate({ inputRange: [0, 1], outputRange: [0.95, 1] }) }]
    }]}>
      <LinearGradient colors={style.gradients} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.card}>
        <View style={StyleSheet.absoluteFill}>
          {renderEffect()}
          <View style={styles.glassOverlay} />
        </View>

        <View style={styles.content}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.cityText, { color: secondaryColor }]}>{city.toUpperCase()}</Text>
              <Text style={[styles.conditionText, { color: textColor }]}>{displayTitle}</Text>
            </View>
            <View style={[styles.iconContainer, isSunnyScene && { opacity: 0.2, backgroundColor: 'transparent', borderColor: 'transparent' }]}>
              <Text style={styles.weatherEmoji}>{displayEmoji}</Text>
            </View>
          </View>

          <View style={styles.mainInfo}>
            <Text style={[styles.tempText, { color: textColor }]}>{Math.round(temperature)}°</Text>
            <Text style={[styles.feelsLikeText, { color: secondaryColor }]}>Hissedilen {Math.round(feelsLike)}°</Text>
          </View>

          <View style={styles.glassBar}>
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>NEM</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>%65</Text>
            </View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>RÜZGAR</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>12 km/s</Text>
            </View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>UV</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>4</Text>
            </View>
          </View>
        </View>
      </LinearGradient>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  outerContainer: {
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.lg,
    borderRadius: 32,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.25,
    shadowRadius: 16,
    elevation: 10
  },
  card: {
    height: 320,
    borderRadius: 32,
    overflow: 'hidden',
    padding: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)'
  },
  glassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255,255,255,0.02)',
    borderRadius: 32
  },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 10 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cityText: { fontSize: 13, fontWeight: '900', letterSpacing: 2 },
  conditionText: { fontSize: 28, fontWeight: '800', marginTop: 2 },
  iconContainer: {
    width: 52,
    height: 52,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.12)',
    borderWidth: 0.5,
    borderColor: 'rgba(255,255,255,0.2)'
  },
  weatherEmoji: { fontSize: 30 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 105, fontWeight: '100', letterSpacing: -5, lineHeight: 115 },
  feelsLikeText: { fontSize: 15, fontWeight: '600', marginTop: -5 },
  glassBar: {
    height: 58,
    backgroundColor: 'rgba(255, 255, 255, 0.07)',
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)'
  },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 10, fontWeight: '800', letterSpacing: 1 },
  infoValue: { fontSize: 15, fontWeight: '800', marginTop: 2 },
  infoDivider: { width: 1, height: 22, backgroundColor: 'rgba(255,255,255,0.1)' },

  // --- PREMIUM SUN SCENE STYLES ---
  premiumSunSceneDisk: {
    position: 'absolute',
    top: 76, // ~24% of 320 height
    right: '22%', // ~78% from left
    width: 64,
    height: 64,
    borderRadius: 32,
    zIndex: 3,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#FFD166',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 15,
    elevation: 8
  },
  premiumSunSceneInnerHighlight: {
    width: '70%',
    height: '75%',
    borderRadius: 25,
  },
  premiumSunSceneGlow: {
    position: 'absolute',
    top: 38,
    right: '12%',
    width: 140,
    height: 140,
    borderRadius: 70,
    zIndex: 1,
    shadowColor: '#FFB703',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.5,
    shadowRadius: 30,
    elevation: 4
  },
  premiumSunSceneRaysContainer: {
    position: 'absolute',
    top: 48,
    right: '13.5%',
    width: 120,
    height: 120,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 2
  },
  premiumSunSceneRay: {
    position: 'absolute',
    width: 2,
    height: 14, // Ray length
    borderRadius: 1,
  },

  // Legacy / Unused styles cleaning
  sunGlow: { position: 'absolute', top: -80, right: -80, width: 280, height: 280, borderRadius: 140 },
  sunGlowMini: { position: 'absolute', top: 30, right: 30, width: 80, height: 80, borderRadius: 40 },
  moonGlow: { position: 'absolute', top: 40, right: 40, width: 100, height: 100, borderRadius: 50 },
  star: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 5 },
  rainLine: { position: 'absolute', width: 0.8, height: 25, backgroundColor: 'rgba(255,255,255,0.4)' },
  snowFlake: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 10 },
  cloudHaze: { position: 'absolute', backgroundColor: 'rgba(255,255,255,0.8)', borderRadius: 100 },
  fogLayer: { ...StyleSheet.absoluteFillObject },
  windLine: { position: 'absolute', height: 1, backgroundColor: '#FFF' },
  moonContainer: {
    position: 'absolute',
    top: 50,
    right: 50,
    width: 60,
    height: 60,
  },
  moonMain: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#F8F9FA',
    shadowColor: '#FFF',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.3,
    shadowRadius: 10,
    elevation: 5,
  },
  moonShadow: {
    position: 'absolute',
    width: 50,
    height: 50,
    borderRadius: 25,
    top: -5,
    left: 15,
  },
});
