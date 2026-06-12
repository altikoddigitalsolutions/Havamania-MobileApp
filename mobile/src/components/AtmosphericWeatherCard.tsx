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

const getTimeOfDay = (hour: number): TimeOfDay => {
  if (hour >= 6 && hour < 11) return TimeOfDay.MORNING;
  if (hour >= 11 && hour < 17) return TimeOfDay.DAY;
  if (hour >= 17 && hour < 19) return TimeOfDay.EVENING;
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
      if (timeOfDay === TimeOfDay.MORNING) {
        // Refined Morning: Very light blue-grey, warm cream, sunrise gold
        style = { gradients: ['#E2E8F0', '#FFFBEB', '#FDE68A'], effect: 'sunny', isWhiteText: false, accent: '#F59E0B', name: 'SUNNY_MORNING' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#F97316', '#7C2D12', '#431407'], effect: 'sunny', isWhiteText: true, accent: '#FB923C', name: 'SUNNY_EVENING' };
      } else {
        style = { gradients: ['#38BDF8', '#7DD3FC', '#BAE6FD'], effect: 'sunny', isWhiteText: true, accent: '#FCD34D', name: 'SUNNY_DAY' };
      }
      break;

    case WeatherCondition.MOSTLY_SUNNY:
      if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#A5F3FC', '#E0F2FE'], effect: 'sunny', isWhiteText: false, accent: '#F59E0B', name: 'MOSTLY_SUNNY_MORNING' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#FB923C', '#EA580C', '#334155'], effect: 'sunny', isWhiteText: true, accent: '#FB923C', name: 'MOSTLY_SUNNY_EVENING' };
      } else {
        style = { gradients: ['#7DD3FC', '#E0F2FE'], effect: 'sunny', isWhiteText: true, accent: '#FCD34D', name: 'MOSTLY_SUNNY_DAY' };
      }
      break;

    case WeatherCondition.CLEAR_NIGHT:
      style = { gradients: ['#0F172A', '#1E293B', '#334155'], effect: 'night', isWhiteText: true, accent: '#F8FAFC', name: 'CLEAR_NIGHT' };
      break;

    case WeatherCondition.RAIN:
    case WeatherCondition.RAIN_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0F172A', '#1E293B', '#0F172A'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_NIGHT' };
      } else {
        style = { gradients: ['#1E3A8A', '#1E40AF', '#172554'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_DAY' };
      }
      break;

    case WeatherCondition.CLOUDY:
    case WeatherCondition.CLOUDY_NIGHT:
      if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#E2E8F0', '#94A3B8', '#64748B'], effect: 'cloudy', isWhiteText: false, accent: '#F8FAFC', name: 'CLOUDY_MORNING' };
      } else if (timeOfDay === TimeOfDay.DAY) {
        style = { gradients: ['#94A3B8', '#475569', '#1E293B'], effect: 'cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'CLOUDY_DAY' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#475569', '#1E293B', '#0F172A'], effect: 'cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'CLOUDY_EVENING' };
      } else {
        style = { gradients: ['#020617', '#0F172A', '#1E293B'], effect: 'cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'CLOUDY_NIGHT' };
      }
      break;

    case WeatherCondition.PARTLY_CLOUDY:
    case WeatherCondition.PARTLY_CLOUDY_NIGHT:
      if (timeOfDay === TimeOfDay.MORNING || timeOfDay === TimeOfDay.DAY) {
        // Refined Partly Cloudy: Light blue, turquoise, warm gold
        style = { gradients: ['#BAE6FD', '#CFFAFE', '#FEF3C7'], effect: 'partly_cloudy', isWhiteText: false, accent: '#F8FAFC', name: 'PARTLY_CLOUDY_DAY' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#FB923C', '#475569', '#1E293B'], effect: 'partly_cloudy', isWhiteText: true, accent: '#FB923C', name: 'PARTLY_CLOUDY_EVENING' };
      } else {
        style = { gradients: ['#0F172A', '#1E293B', '#334155'], effect: 'partly_cloudy', isWhiteText: true, accent: '#F8FAFC', name: 'PARTLY_CLOUDY_NIGHT' };
      }
      break;

    case WeatherCondition.SNOW:
    case WeatherCondition.SNOW_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#0F172A', '#1E293B', '#E2E8F0'], effect: 'snow', isWhiteText: true, accent: '#E2E8F0', name: 'SNOW_NIGHT' };
      } else {
        style = { gradients: ['#F8FAFC', '#E2E8F0', '#94A3B8'], effect: 'snow', isWhiteText: false, accent: '#CBD5E1', name: 'SNOW_DAY' };
      }
      break;

    case WeatherCondition.FOG:
    case WeatherCondition.FOG_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#1E293B', '#334155', '#475569'], effect: 'fog', isWhiteText: true, accent: '#F8FAFC', name: 'FOG_NIGHT' };
      } else if (timeOfDay === TimeOfDay.MORNING) {
        // Refined Foggy Morning (06:00 - 11:00): Cold blue-grey, light lavender
        style = { gradients: ['#94A3B8', '#B1B1D8', '#E2E8F0'], effect: 'fog', isWhiteText: false, accent: '#94A3B8', name: 'FOG_MORNING' };
      } else {
        style = { gradients: ['#CBD5E1', '#94A3B8', '#64748B'], effect: 'fog', isWhiteText: false, accent: '#F8FAFC', name: 'FOG_DAY' };
      }
      break;

    case WeatherCondition.THUNDERSTORM:
      // Dramatic Storm: Deep Navy and Dark Purple
      style = { gradients: ['#0F172A', '#1E1B4B', '#2E1065'], effect: 'thunder', isWhiteText: true, accent: '#A855F7', name: 'THUNDERSTORM' };
      break;

    case WeatherCondition.WIND:
      style = { gradients: ['#7DD3FC', '#38BDF8', '#0F172A'], effect: 'wind', isWhiteText: true, accent: '#F8FAFC', name: 'WIND_DAY' };
      break;

    default:
      style = { gradients: ['#38BDF8', '#1E40AF', '#FCD34D'], effect: 'sunny', isWhiteText: true, accent: '#FCD34D', name: 'FALLBACK_SUNNY' };
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
  const timeOfDay = useMemo(() => getTimeOfDay(parsedHour), [parsedHour]);
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
    rain: [...Array(40)].map((_, i) => {
      const type = i % 3; // 0: short, 1: medium, 2: long
      return {
        left: `${Math.random() * 100}%`,
        opacity: type === 0 ? 0.15 : type === 1 ? 0.25 : 0.4,
        speed: 0.8 + Math.random() * 1.2,
        length: type === 0 ? 6 : type === 1 ? 18 : 32,
        width: 0.7,
        offset: Math.random()
      };
    }),
    stars: [...Array(25)].map((_, i) => ({ // Reduced count from 40 to 25
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      size: 0.3 + Math.random() * 0.8, // Smaller stars
      opacity: 0.1 + Math.random() * 0.4, // Lower opacity
      blinkSpeed: 3000 + Math.random() * 4000
    })),
    snow: [...Array(40)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      size: 2 + Math.random() * 4,
      speed: 0.3 + Math.random() * 0.6,
      drift: Math.random() * 40 - 20,
      offset: Math.random()
    })),
    clouds: [...Array(6)].map((_, i) => ({
        top: 20 + Math.random() * 120,
        left: -80 + Math.random() * 180, // Wider range
        width: (150 + Math.random() * 200) * 0.85,
        height: (60 + Math.random() * 100) * 0.85,
        opacity: Math.min((0.05 + Math.random() * 0.15) * 1.1, 0.4),
        speed: 0.02 + (i % 3) * 0.03, // Defined speeds for parallax
        layer: i % 3 // 0: back (slowest), 1: mid, 2: front (fastest)
    }))
  }), []);

  const textColor = style.isWhiteText ? '#FFF' : '#0F172A';
  const secondaryColor = style.isWhiteText ? 'rgba(255,255,255,0.75)' : 'rgba(15,23,42,0.65)';

  const sunOpacity = useMemo(() => {
    if (timeOfDay !== TimeOfDay.EVENING) return 1;
    // Sunset transition: 18:00 - 20:00 fade
    if (parsedHour >= 20) return 0;
    if (parsedHour >= 18) {
      return 1 - (parsedHour - 18) / 2;
    }
    return 1;
  }, [parsedHour, timeOfDay]);

  const renderAurora = (color: string, intensity: number = 1) => {
    if (reducedMotion) return null;
    return (
      <Animated.View style={[styles.auroraLayer, {
        backgroundColor: color,
        opacity: pulseAnim.interpolate({
          inputRange: [0, 1],
          outputRange: [0.005 * intensity, 0.015 * intensity] // Even lower default
        }),
        transform: [
          { scale: 1.8 },
          { translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-40, 40] }) }
        ]
      }]} />
    );
  };

  const renderEffect = () => {
    if (reducedMotion) return null;

    // Overlay Intensity Calculation
    let auroraIntensity = 1;
    if (style.effect === 'rain' || style.effect === 'thunder' || style.effect === 'night') {
        auroraIntensity = 0.3; // Much weaker for these conditions
    }

    switch (style.effect) {
      case 'sunny':
        const isMostlySunny = displayCondition === WeatherCondition.MOSTLY_SUNNY;
        const isEvening = timeOfDay === TimeOfDay.EVENING;
        const isPartly = displayCondition === WeatherCondition.PARTLY_CLOUDY;

        return (
          <View style={StyleSheet.absoluteFill}>
            {/* 0. AURORA LAYER */}
            {renderAurora(isEvening ? '#FB923C' : '#7DD3FC', auroraIntensity)}

            {/* 1. OUTER ATMOSPHERIC GLOW */}
            <Animated.View style={[styles.premiumSunSceneGlow, {
              backgroundColor: isEvening ? '#EA580C' : '#FBBF24',
              opacity: pulseAnim.interpolate({
                inputRange: [0, 1],
                outputRange: [0.12, 0.2] // Reduced slightly
              }),
              transform: [{ scale: 1.8 }] // Bigger glow
            }]} />

            {/* 2. DYNAMIC LIGHT BLOOM */}
            <Animated.View style={[styles.premiumSunSceneGlow, {
              width: 240, height: 240, borderRadius: 120,
              backgroundColor: isEvening ? '#FB923C' : '#FDE68A',
              opacity: pulseAnim.interpolate({
                inputRange: [0, 1],
                outputRange: [0.05, 0.15]
              }),
              transform: [{ scale: 1.3 }]
            }]} />

            {/* 3. ROTATING RAYS */}
            <Animated.View style={[styles.premiumSunSceneRaysContainer, {
              opacity: (isPartly ? 0.1 : 0.2) * sunOpacity,
              transform: [
                { rotate: masterAnim.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] }) },
              ]
            }]}>
               {[...Array(12)].map((_, i) => (
                 <View key={i} style={[styles.premiumSunSceneRay, {
                   backgroundColor: isEvening ? '#FDBA74' : '#FEF3C7',
                   height: 24,
                   transform: [{ rotate: `${i * 30}deg` }, { translateY: 50 }]
                 }]} />
               ))}
            </Animated.View>

            {/* 4. SUN DISK */}
            {sunOpacity > 0 && (
              <Animated.View style={[styles.premiumSunSceneDisk, {
                backgroundColor: isEvening ? '#FB923C' : '#FCD34D',
                opacity: sunOpacity,
                transform: [
                  { scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.05] }) }
                ]
              }]}>
                <View style={[styles.premiumSunSceneInnerHighlight, {
                  backgroundColor: '#FFFBEB',
                  opacity: 0.4, // Softer center
                  position: 'absolute',
                  top: '15%',
                  left: '15%',
                  width: '35%',
                  height: '35%',
                  borderRadius: 20
                }]} />
              </Animated.View>
            )}

            {/* 5. ATMOSPHERIC CLOUDS - PARALLAX MOTION */}
            {(isMostlySunny || isPartly) && particles.clouds.slice(0, 3).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top,
                    width: c.width,
                    height: c.height,
                    opacity: c.opacity * (c.layer + 1.2),
                    zIndex: c.layer + 1,
                    transform: [
                        { translateX: masterAnim.interpolate({
                            inputRange: [0, 1],
                            outputRange: [c.left, c.left + (80 * c.speed)] // More motion
                        }) },
                        { scale: 0.8 + (c.layer * 0.15) }
                    ]
                }]} />
            ))}
          </View>
        );

      case 'rain':
        return (
          <View style={StyleSheet.absoluteFill}>
            {/* 0. AURORA LAYER */}
            {renderAurora('#1E40AF', auroraIntensity)}

            {particles.rain.map((p, i) => (
              <Animated.View key={i} style={[styles.rainLine, {
                left: p.left,
                width: p.width,
                height: p.length,
                opacity: p.opacity,
                transform: [
                    { translateY: masterAnim.interpolate({
                        inputRange: [0, 1],
                        outputRange: [-100, 400 * p.speed]
                    }) },
                    { rotate: '15deg' }
                ]
              }]} />
            ))}
            {/* Parallax Clouds for Rain */}
            {particles.clouds.slice(0, 3).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top, width: c.width, height: c.height,
                    backgroundColor: '#1E293B',
                    opacity: 0.2,
                    transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [c.left, c.left + (40 * c.speed)] }) }]
                }]} />
            ))}
          </View>
        );

      case 'night':
        const isClearNight = displayCondition === WeatherCondition.CLEAR_NIGHT;
        return (
          <View style={StyleSheet.absoluteFill}>
            {/* 0. AURORA LAYER */}
            {renderAurora('#1E3A8A', auroraIntensity)}

            {particles.stars.map((p, i) => (
              <Animated.View key={i} style={[styles.star, {
                left: p.left,
                top: p.top,
                width: p.size,
                height: p.size,
                opacity: pulseAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [p.opacity * 0.4, p.opacity] // Subtle twinkle
                })
              }]} />
            ))}
            {/* Moon Glow */}
            <Animated.View style={[styles.moonGlow, {
                backgroundColor: '#F8FAFC',
                opacity: pulseAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [0.02, 0.05]
                }),
                transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.05] }) }]
            }]} />
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
                  { translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-20, 340 * p.speed] }) },
                  { translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [0, p.drift] }) }
                ]
              }]} />
            ))}
          </View>
        );

      case 'cloudy':
      case 'partly_cloudy':
        return (
          <View style={StyleSheet.absoluteFill}>
            {/* 0. AURORA LAYER */}
            {renderAurora(style.isWhiteText ? '#334155' : '#7DD3FC', auroraIntensity)}

            {/* 3 Parallax Cloud Layers */}
            {particles.clouds.slice(0, 3).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top,
                    width: c.width,
                    height: c.height,
                    opacity: c.opacity * 1.5,
                    backgroundColor: style.isWhiteText ? '#F8FAFC' : '#CBD5E1',
                    transform: [
                        { translateX: masterAnim.interpolate({
                            inputRange: [0, 1],
                            outputRange: [c.left, c.left + (60 * c.speed)] // Defined Parallax
                        }) },
                        { scale: 0.85 + (c.layer * 0.1) }
                    ]
                }]} />
            ))}
            {style.effect === 'partly_cloudy' && (
              <Animated.View style={[styles.sunGlowMini, {
                  backgroundColor: style.accent,
                  opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.03, 0.1] })
              }]} />
            )}
          </View>
        );

      case 'fog':
        const isFogNight = timeOfDay === TimeOfDay.NIGHT;
        const isFogMorning = timeOfDay === TimeOfDay.MORNING;
        return (
            <View style={StyleSheet.absoluteFill}>
                {/* 0. AURORA LAYER */}
                {renderAurora(isFogNight ? '#1E293B' : (isFogMorning ? '#B1B1D8' : '#94A3B8'), auroraIntensity)}

                {/* Stars for Foggy Night - Reduced Density */}
                {isFogNight && particles.stars.slice(0, 12).map((p, i) => (
                  <Animated.View key={i} style={[styles.star, {
                    left: p.left, top: p.top, width: p.size, height: p.size,
                    opacity: pulseAnim.interpolate({
                        inputRange: [0, 1],
                        outputRange: [p.opacity * 0.2, p.opacity * 0.3]
                    })
                  }]} />
                ))}

                {[0.04, 0.06].map((op, i) => (
                    <Animated.View key={i} style={[styles.fogLayer, {
                        backgroundColor: isFogNight ? '#1E293B' : (isFogMorning ? '#D1D1E8' : '#F1F5F9'),
                        opacity: op,
                        transform: [{ translateX: masterAnim.interpolate({
                            inputRange: [0, 1],
                            outputRange: [i * -40, i * 40]
                        }) }]
                    }]} />
                ))}

                {/* BOTTOM FOG LAYER */}
                <Animated.View style={[styles.fogLayer, {
                    backgroundColor: isFogNight ? '#334155' : (isFogMorning ? '#B1B1D8' : '#CBD5E1'),
                    opacity: 0.08,
                    top: '80%',
                    transform: [{ translateY: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0, 8] }) }]
                }]} />
            </View>
        );

      case 'thunder':
        return (
          <View style={StyleSheet.absoluteFill}>
            {/* 0. AURORA LAYER */}
            {renderAurora('#4C1D95', auroraIntensity)}

            {/* Background Storm Clouds - Parallax */}
            {particles.clouds.slice(0, 3).map((c, i) => (
                <Animated.View key={i} style={[styles.cloudHaze, {
                    top: c.top, width: c.width, height: c.height,
                    backgroundColor: '#1E1B4B',
                    opacity: 0.5,
                    transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [c.left, c.left + (30 * c.speed)] }) }]
                }]} />
            ))}
            {/* Stronger Rain for Storm */}
            {particles.rain.slice(0, 30).map((p, i) => (
              <Animated.View key={i} style={[styles.rainLine, {
                left: p.left, width: p.width * 1.5, height: p.length * 1.2,
                backgroundColor: '#CBD5E1',
                opacity: p.opacity * 1.5,
                transform: [
                    { translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-100, 450 * p.speed] }) },
                    { rotate: '20deg' }
                ]
              }]} />
            ))}
            {/* Lightning Flashes - Rare and Subtle */}
            <Animated.View style={[StyleSheet.absoluteFill, {
              backgroundColor: '#DDD6FE',
              opacity: masterAnim.interpolate({
                inputRange: [0, 0.4, 0.41, 0.42, 0.8, 0.81, 0.82, 1],
                outputRange: [0, 0, 0.12, 0, 0, 0.15, 0, 0]
              })
            }]} />
          </View>
        );

      case 'wind':
        return (
            <Animated.View style={[StyleSheet.absoluteFill, {
                transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-200, SCREEN_WIDTH + 100] }) }]
            }]}>
                <View style={[styles.windLine, { top: 100, width: 120, opacity: 0.15 }]} />
                <View style={[styles.windLine, { top: 180, width: 180, opacity: 0.1 }]} />
                <View style={[styles.windLine, { top: 240, width: 100, opacity: 0.2 }]} />
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
            <View style={[
              styles.iconContainer,
              isSunnyScene && { opacity: 0.2, backgroundColor: 'transparent', borderColor: 'transparent' },
              displayCondition === WeatherCondition.THUNDERSTORM && { transform: [{ scale: 0.9 }] } // 10% smaller lightning icon
            ]}>
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
    shadowOpacity: 0.3,
    shadowRadius: 20,
    elevation: 12
  },
  card: {
    height: 320,
    borderRadius: 32,
    overflow: 'hidden',
    padding: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.15)'
  },
  glassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255,255,255,0.03)',
    borderRadius: 32,
    // Subtle surface reflection
    borderTopWidth: 1,
    borderLeftWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)'
  },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 20 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cityText: { fontSize: 13, fontWeight: '900', letterSpacing: 2, opacity: 0.8 },
  conditionText: { fontSize: 28, fontWeight: '800', marginTop: 2, textShadowColor: 'rgba(0,0,0,0.1)', textShadowOffset: {width: 0, height: 1}, textShadowRadius: 2 },
  iconContainer: {
    width: 52,
    height: 52,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.12)',
    borderWidth: 0.5,
    borderColor: 'rgba(255,255,255,0.2)',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.1,
    shadowRadius: 4
  },
  weatherEmoji: { fontSize: 30 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 110, fontWeight: '100', letterSpacing: -5, lineHeight: 115, textShadowColor: 'rgba(0,0,0,0.05)', textShadowOffset: {width: 0, height: 2}, textShadowRadius: 4 },
  feelsLikeText: { fontSize: 15, fontWeight: '600', marginTop: -5, opacity: 0.9 },
  glassBar: {
    height: 62,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderRadius: 22,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.15)',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.05,
    shadowRadius: 8
  },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 10, fontWeight: '800', letterSpacing: 1.2, opacity: 0.7 },
  infoValue: { fontSize: 16, fontWeight: '800', marginTop: 2 },
  infoDivider: { width: 1, height: 24, backgroundColor: 'rgba(255,255,255,0.1)' },

  // --- PREMIUM SUN SCENE STYLES ---
  premiumSunSceneDisk: {
    position: 'absolute',
    top: 60, // Higher
    right: -10, // Bleed off right edge
    width: 50, // Reduced by 20%
    height: 50,
    borderRadius: 25,
    zIndex: 10,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#FBBF24',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.7,
    shadowRadius: 40, // Increased glow area
    elevation: 10
  },
  premiumSunSceneInnerHighlight: {
    width: '70%',
    height: '75%',
    borderRadius: 25,
  },
  premiumSunSceneGlow: {
    position: 'absolute',
    top: 30,
    right: '10%',
    width: 220, // Larger glow
    height: 220,
    borderRadius: 110,
    zIndex: 1,
  },
  premiumSunSceneRaysContainer: {
    position: 'absolute',
    top: 50,
    right: '12%',
    width: 120,
    height: 120,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 2
  },
  premiumSunSceneRay: {
    position: 'absolute',
    width: 2.5,
    borderRadius: 2,
  },

  auroraLayer: {
    position: 'absolute',
    width: SCREEN_WIDTH,
    height: 240,
    borderRadius: 120,
    top: 40,
    left: -20,
    zIndex: 0,
    opacity: 0.05
  },

  sunGlowMini: { position: 'absolute', top: 30, right: 30, width: 80, height: 80, borderRadius: 40 },
  moonGlow: { position: 'absolute', top: 50, right: '20%', width: 85, height: 85, borderRadius: 42.5, shadowColor: '#FFF', shadowOffset: {width: 0, height: 0}, shadowOpacity: 0.5, shadowRadius: 30 },
  star: { position: 'absolute', backgroundColor: '#F8FAFC', borderRadius: 5 },
  rainLine: { position: 'absolute', width: 1, backgroundColor: '#F1F5F9', borderRadius: 1 },
  snowFlake: { position: 'absolute', backgroundColor: '#F8FAFC', borderRadius: 10 },
  cloudHaze: { position: 'absolute', backgroundColor: '#F8FAFC', borderRadius: 100 },
  fogLayer: { ...StyleSheet.absoluteFillObject },
  windLine: { position: 'absolute', height: 1.5, backgroundColor: '#F1F5F9', borderRadius: 1 },
});

