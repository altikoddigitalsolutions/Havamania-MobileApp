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
  if (code === 1) return WeatherCondition.CLEAR;
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
    if (rawCondition === WeatherCondition.SUNNY || rawCondition === WeatherCondition.CLEAR) {
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
        style = { gradients: ['#BDEBFF', '#FFD6A5', '#FFF3B0'], effect: 'sunny', isWhiteText: false, accent: '#FFF3B0', name: 'SUNNY_MORNING' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#FFB86B', '#FF7E67', '#7F5A83'], effect: 'sunny', isWhiteText: true, accent: '#FFB86B', name: 'SUNNY_EVENING' };
      } else {
        style = { gradients: ['#56CCF2', '#2F80ED', '#FBCB45'], effect: 'sunny', isWhiteText: true, accent: '#FBCB45', name: 'SUNNY_DAY' };
      }
      break;

    case WeatherCondition.CLEAR_NIGHT:
      style = { gradients: ['#071B33', '#0B2748', '#142F5F'], effect: 'night', isWhiteText: true, accent: '#FFF', name: 'CLEAR_NIGHT' };
      break;

    case WeatherCondition.RAIN:
    case WeatherCondition.RAIN_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#071B33', '#0A233D', '#102F55'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_NIGHT' };
      } else {
        style = { gradients: ['#2D6CDF', '#174C8A', '#0F2E4D'], effect: 'rain', isWhiteText: true, accent: '#60A5FA', name: 'RAIN_DAY' };
      }
      break;

    case WeatherCondition.CLOUDY:
    case WeatherCondition.CLOUDY_NIGHT:
      if (timeOfDay === TimeOfDay.MORNING) {
        style = { gradients: ['#D8E2EC', '#B7C7D8', '#8DA1B5'], effect: 'cloudy', isWhiteText: false, accent: '#FFF', name: 'CLOUDY_MORNING' };
      } else if (timeOfDay === TimeOfDay.DAY) {
        style = { gradients: ['#C6D3DF', '#9BAEC1', '#6F8499'], effect: 'cloudy', isWhiteText: false, accent: '#FFF', name: 'CLOUDY_DAY' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#B8AFC6', '#8C8FA6', '#627386'], effect: 'cloudy', isWhiteText: true, accent: '#FFF', name: 'CLOUDY_EVENING' };
      } else {
        style = { gradients: ['#182433', '#233449', '#30485F'], effect: 'cloudy', isWhiteText: true, accent: '#FFF', name: 'CLOUDY_NIGHT' };
      }
      break;

    case WeatherCondition.PARTLY_CLOUDY:
    case WeatherCondition.PARTLY_CLOUDY_NIGHT:
      if (timeOfDay === TimeOfDay.MORNING || timeOfDay === TimeOfDay.DAY) {
        style = { gradients: ['#A9DDF5', '#7FB8D8', '#EDEFF2'], effect: 'partly_cloudy', isWhiteText: false, accent: '#FFF', name: 'PARTLY_CLOUDY_DAY' };
      } else if (timeOfDay === TimeOfDay.EVENING) {
        style = { gradients: ['#F2A65A', '#B887A6', '#637A96'], effect: 'partly_cloudy', isWhiteText: true, accent: '#F2A65A', name: 'PARTLY_CLOUDY_EVENING' };
      } else {
        style = { gradients: ['#142238', '#243B55', '#34495E'], effect: 'partly_cloudy', isWhiteText: true, accent: '#FFF', name: 'PARTLY_CLOUDY_NIGHT' };
      }
      break;

    case WeatherCondition.SNOW:
    case WeatherCondition.SNOW_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#102538', '#1C3A54', '#A9D6E5'], effect: 'snow', isWhiteText: true, accent: '#A9D6E5', name: 'SNOW_NIGHT' };
      } else {
        style = { gradients: ['#EAF8FF', '#BFE3F6', '#7DB7D9'], effect: 'snow', isWhiteText: false, accent: '#7DB7D9', name: 'SNOW_DAY' };
      }
      break;

    case WeatherCondition.FOG:
    case WeatherCondition.FOG_NIGHT:
      if (timeOfDay === TimeOfDay.NIGHT) {
        style = { gradients: ['#1F2933', '#2F3E4D', '#4B5A68'], effect: 'fog', isWhiteText: true, accent: '#FFF', name: 'FOG_NIGHT' };
      } else {
        style = { gradients: ['#D0D7DE', '#AAB6C2', '#7B8998'], effect: 'fog', isWhiteText: false, accent: '#FFF', name: 'FOG_DAY' };
      }
      break;

    case WeatherCondition.THUNDERSTORM:
      style = { gradients: ['#101B3D', '#27275F', '#4B2E83'], effect: 'thunder', isWhiteText: true, accent: '#A78BFA', name: 'THUNDERSTORM' };
      break;

    case WeatherCondition.WIND:
      style = { gradients: ['#8EC5FC', '#7FB3D5', '#5D7890'], effect: 'wind', isWhiteText: false, accent: '#FFF', name: 'WIND_DAY' };
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

  // Akşam/Gün Batımı Koşulları
  if (timeOfDay === TimeOfDay.EVENING) {
    if (condition === WeatherCondition.SUNNY || condition === WeatherCondition.CLEAR) {
      return 'Açık Akşam';
    }
  }

  // Gündüz Koşulları
  if (timeOfDay === TimeOfDay.DAY || timeOfDay === TimeOfDay.MORNING) {
    if (condition === WeatherCondition.SUNNY || condition === WeatherCondition.CLEAR) {
      return 'Güneşli';
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

export const AtmosphericWeatherCard: React.FC<AtmosphericWeatherCardProps> = ({
  city,
  temperature,
  description,
  feelsLike,
  weatherCode,
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
        return (
          <Animated.View style={[styles.sunGlow, {
            backgroundColor: style.accent,
            opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.15, 0.28] }),
            transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.15] }) }]
          }]} />
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
              <View key={i} style={[styles.star, {
                left: p.left,
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
            transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-30, 30] }) }]
          }]}>
            <View style={[styles.cloudHaze, { top: 40, left: -20, width: 220, height: 140, opacity: 0.06 }]} />
            <View style={[styles.cloudHaze, { bottom: 60, right: -40, width: 280, height: 160, opacity: 0.1 }]} />
            {style.effect === 'partly_cloudy' && (
              <View style={[styles.sunGlowMini, { backgroundColor: style.accent, opacity: 0.15 }]} />
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
            <View style={styles.iconContainer}>
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
  sunGlow: { position: 'absolute', top: -80, right: -80, width: 280, height: 280, borderRadius: 140 },
  sunGlowMini: { position: 'absolute', top: 30, right: 30, width: 80, height: 80, borderRadius: 40 },
  moonGlow: { position: 'absolute', top: 40, right: 40, width: 100, height: 100, borderRadius: 50 },
  star: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 5 },
  rainLine: { position: 'absolute', width: 1, height: 22, backgroundColor: '#FFF' },
  snowFlake: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 10 },
  cloudHaze: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 100 },
  fogLayer: { ...StyleSheet.absoluteFillObject },
  windLine: { position: 'absolute', height: 1, backgroundColor: '#FFF' },
});
