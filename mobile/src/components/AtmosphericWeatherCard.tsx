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
  OVERCAST = 'OVERCAST',
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
  BLIZZARD = 'BLIZZARD',
  WIND = 'WIND'
}

export enum TimeOfDay {
  DAWN = 'DAWN',
  MORNING = 'MORNING',
  DAY = 'DAY',
  GOLDEN_HOUR = 'GOLDEN_HOUR',
  SUNSET = 'SUNSET',
  BLUE_HOUR = 'BLUE_HOUR',
  TWILIGHT = 'TWILIGHT',
  EVENING = 'EVENING',
  NIGHT = 'NIGHT'
}

interface WeatherCardStyle {
  gradients: string[];
  effect: 'sunny' | 'rain' | 'cloudy' | 'snow' | 'fog' | 'thunder' | 'night' | 'wind' | 'partly_cloudy' | 'sunset' | 'dawn';
  isWhiteText: boolean;
  accent: string;
  name: string;
  altitude: number;
}

const getRawCondition = (code: number): WeatherCondition => {
  if (code === 0) return WeatherCondition.SUNNY;
  if (code === 1) return WeatherCondition.MOSTLY_SUNNY;
  if (code === 2) return WeatherCondition.PARTLY_CLOUDY;
  if (code === 3) return WeatherCondition.OVERCAST;
  if (code === 45 || code === 48) return WeatherCondition.FOG;
  if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return WeatherCondition.RAIN;
  if ((code >= 71 && code <= 77) || (code >= 85 && code <= 86)) return WeatherCondition.SNOW;
  if (code >= 95) return WeatherCondition.THUNDERSTORM;
  return WeatherCondition.SUNNY;
};

const calculateAltitude = (hour: number): number => {
  // Very rough simulation: noon is 90, midnight is -18
  if (hour >= 6 && hour <= 18) {
    // Day
    const progress = (hour - 6) / 6; // 0 at 6am, 1 at noon, 2 at 6pm
    return 90 * (1 - Math.abs(progress - 1));
  } else {
    // Night
    const h = hour < 6 ? hour + 24 : hour;
    const progress = (h - 18) / 6; // 0 at 6pm, 1 at midnight, 2 at 6am
    return -18 * (1 - Math.abs(progress - 1));
  }
};

const getTimeOfDayFromAltitude = (altitude: number, hour: number): TimeOfDay => {
  if (altitude <= -12) return TimeOfDay.NIGHT;
  if (altitude <= -6) return TimeOfDay.TWILIGHT;
  if (altitude <= 0) return hour < 12 ? TimeOfDay.DAWN : TimeOfDay.BLUE_HOUR;
  if (altitude <= 4) return hour < 12 ? TimeOfDay.DAWN : TimeOfDay.SUNSET;
  if (altitude <= 12) return hour < 12 ? TimeOfDay.DAWN : TimeOfDay.GOLDEN_HOUR;
  if (altitude <= 25) return hour < 12 ? TimeOfDay.MORNING : TimeOfDay.EVENING;
  return TimeOfDay.DAY;
};

const resolveWeatherCardStyle = (
  condition: WeatherCondition,
  altitude: number,
  timeOfDay: TimeOfDay,
  temperature: number,
  theme: Theme
): WeatherCardStyle => {
  let grads: string[];
  const isNight = altitude <= 0;
  const isCloudy = condition === WeatherCondition.OVERCAST || condition === WeatherCondition.CLOUDY || condition === WeatherCondition.PARTLY_CLOUDY;
  const isRainy = condition === WeatherCondition.RAIN || condition === WeatherCondition.THUNDERSTORM;

  if (isNight) {
    if (isCloudy || isRainy) {
      grads = ['#080C14', '#1E293B', '#334155']; // Deep Desaturated Night
    } else {
      grads = ['#020617', '#0F172A', '#1E1B4B']; // Midnight Blue
    }
  } else if (timeOfDay === TimeOfDay.DAWN) {
    grads = isCloudy ? ['#334155', '#475569', '#FBCFE8'] : ['#2D1B69', '#DB2777', '#F59E0B'];
  } else if (timeOfDay === TimeOfDay.SUNSET || timeOfDay === TimeOfDay.GOLDEN_HOUR) {
    grads = isCloudy ? ['#1F2937', '#374151', '#9CA3AF'] : ['#0F172A', '#7C2D12', '#F97316'];
  } else if (isRainy) {
    grads = ['#334155', '#475569', '#64748B'];
  } else if (isCloudy) {
    grads = condition === WeatherCondition.PARTLY_CLOUDY
      ? ['#0369A1', '#0EA5E9', '#BAE6FD']
      : ['#475569', '#64748B', '#94A3B8'];
  } else {
    grads = temperature > 28 ? ['#0284C7', '#0EA5E9', '#FDE68A'] : ['#0369A1', '#0EA5E9', '#BAE6FD'];
  }

  const style: WeatherCardStyle = {
    gradients: grads,
    effect: 'sunny',
    isWhiteText: isNight || isRainy || isCloudy || (altitude < 10),
    accent: isNight ? '#93C5FD' : '#FDE047',
    name: timeOfDay,
    altitude: altitude
  };

  if (condition === WeatherCondition.THUNDERSTORM) style.effect = 'thunder';
  else if (condition === WeatherCondition.RAIN) style.effect = 'rain';
  else if (condition === WeatherCondition.SNOW) style.effect = 'snow';
  else if (condition === WeatherCondition.FOG) style.effect = 'fog';
  else if (isNight) style.effect = 'night';
  else if (timeOfDay === TimeOfDay.SUNSET) style.effect = 'sunset';
  else if (timeOfDay === TimeOfDay.DAWN) style.effect = 'dawn';
  else if (isCloudy) style.effect = 'partly_cloudy';
  else style.effect = 'sunny';

  return style;
};

const getDisplayTitle = (condition: WeatherCondition, altitude: number, originalDescription: string): string => {
  if (altitude < -6) {
    if (originalDescription.includes('Güneşli')) return 'Açık Gece';
    if (originalDescription.includes('Parçalı Bulutlu')) return 'Parçalı Bulutlu Gece';
    if (originalDescription.includes('Bulutlu')) return 'Bulutlu Gece';
  }
  return originalDescription;
};

const parseHour = (timeStr?: string): number => {
    if (!timeStr) return new Date().getHours();
    const s = String(timeStr);
    if (s.startsWith('24')) return 0;
    if (s.includes('T')) {
        const d = new Date(s);
        return isNaN(d.getTime()) ? new Date().getHours() : d.getHours();
    }
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
  feelsLike: number;
  weatherCode: number;
  isDay: boolean;
  time?: string;
  C: AppColors;
}

const getDisplayEmoji = (condition: WeatherCondition, altitude: number, code: number): string => {
  const isNight = altitude <= 0;
  if (isNight) {
    switch (condition) {
      case WeatherCondition.SUNNY:
      case WeatherCondition.CLEAR:
      case WeatherCondition.CLEAR_NIGHT:
        return '🌙';
      case WeatherCondition.PARTLY_CLOUDY:
        return '☁️'; // Gece bulutlu: ay + bulut
      case WeatherCondition.CLOUDY:
      case WeatherCondition.OVERCAST:
        return '☁️';
      case WeatherCondition.RAIN:
      case WeatherCondition.RAIN_NIGHT:
        return '🌧️';
      case WeatherCondition.THUNDERSTORM:
        return '⛈️';
      case WeatherCondition.SNOW:
      case WeatherCondition.SNOW_NIGHT:
        return '🌨️';
      default:
        return '☁️';
    }
  }
  return getWeatherEmoji(code);
};

export const AtmosphericWeatherCard: React.FC<AtmosphericWeatherCardProps> = ({
  city, temperature, description, feelsLike, weatherCode, isDay, time
}) => {
  const { theme } = useThemeStore();
  const [reducedMotion, setReducedMotion] = useState(false);

  const parsedHour = useMemo(() => parseHour(time), [time]);
  const altitude = useMemo(() => calculateAltitude(parsedHour), [parsedHour]);
  const timeOfDay = useMemo(() => getTimeOfDayFromAltitude(altitude, parsedHour), [altitude, parsedHour]);
  const rawCondition = useMemo(() => getRawCondition(weatherCode), [weatherCode]);
  const style = useMemo(() => resolveWeatherCardStyle(rawCondition, altitude, timeOfDay, temperature, theme), [rawCondition, altitude, timeOfDay, temperature, theme]);

  const displayTitle = getDisplayTitle(rawCondition, altitude, description);
  const displayEmoji = getDisplayEmoji(rawCondition, altitude, weatherCode);

  const masterAnim = useRef(new Animated.Value(0)).current;
  const pulseAnim = useRef(new Animated.Value(0)).current;
  const entryAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReducedMotion);
    if (reducedMotion) {
      entryAnim.setValue(1);
      return;
    }

    Animated.timing(entryAnim, { toValue: 1, duration: 800, easing: Easing.out(Easing.back(1)), useNativeDriver: true }).start();
    Animated.loop(Animated.timing(masterAnim, { toValue: 1, duration: 300000, easing: Easing.linear, useNativeDriver: true })).start();
    Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, { toValue: 1, duration: 8000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
        Animated.timing(pulseAnim, { toValue: 0, duration: 8000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      ])
    ).start();
  }, [reducedMotion]);

  const particles = useMemo(() => ({
    rain: [...Array(40)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      opacity: 0.1 + Math.random() * 0.2,
      speed: 0.8 + Math.random() * 0.5,
    })),
    stars: [...Array(35)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      top: `${Math.random() * 100}%`,
      size: 0.5 + Math.random() * 1.5,
      opacity: 0.1 + Math.random() * 0.4,
    })),
    snow: [...Array(40)].map((_, i) => ({
      left: `${Math.random() * 100}%`,
      size: 2 + Math.random() * 3,
      speed: 0.3 + Math.random() * 0.4,
      drift: Math.random() * 40 - 20,
    }))
  }), []);

  const textColor = style.isWhiteText ? '#FFF' : '#0F172A';
  const secondaryColor = style.isWhiteText ? 'rgba(255,255,255,0.75)' : 'rgba(15,23,42,0.65)';
  const uvIcon = altitude <= 0 ? '🌙' : '☀️';

  const renderEffect = () => {
    if (reducedMotion) return null;

    const sunY = 70 - ((Math.max(0, altitude) / 90) * 55);
    const isNight = altitude < 0;

    return (
      <View style={StyleSheet.absoluteFill}>
        {/* Layer 1: Atmosphere Base Glow */}
        <Animated.View style={[styles.hazeLayer, {
          backgroundColor: isNight ? '#1E293B' : style.accent,
          opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.03, 0.08] })
        }]} />

        {/* Layer 2: Stars/Particles (Night/Global) */}
        {isNight && particles.stars.map((p, i) => (
          <Animated.View key={i} style={[styles.star, {
            left: p.left, top: p.top, width: p.size, height: p.size,
            opacity: pulseAnim.interpolate({
                inputRange: [0, 0.5, 1],
                outputRange: [p.opacity, p.opacity * 2.5, p.opacity]
            })
          }]} />
        ))}

        {/* Layer 3: Clouds */}
        <CloudLayer count={['cloudy', 'rain', 'thunder'].includes(style.effect) ? 6 : 2} color={isNight ? '#334155' : '#FFF'} />

        {/* Celestial Body (Sun or Moon) */}
        {(() => {
          if (isNight) {
            return (
              <View style={styles.moonContainer}>
                <Animated.View style={[styles.moonGlow, {
                  backgroundColor: '#94A3B8',
                  opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.1, 0.3] }),
                  transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1.5, 2.2] }) }]
                }]} />
                <Animated.View style={[styles.moonMain, {
                  shadowOpacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.4, 0.8] }),
                  transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.05] }) }]
                }]} />
                <View style={[styles.moonShadow, { backgroundColor: style.gradients[0] }]} />
              </View>
            );
          } else if (['sunny', 'partly_cloudy', 'sunset', 'dawn'].includes(style.effect)) {
            return (
              <>
                <Animated.View style={[styles.premiumSunSceneGlow, {
                  top: sunY - 45,
                  backgroundColor: style.accent,
                  opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.1, 0.25] }),
                  transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1.2, 1.5] }) }]
                }]} />
                <Animated.View style={[styles.premiumSunSceneGlow, {
                  top: sunY - 30,
                  backgroundColor: style.accent,
                  opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.15, 0.35] }),
                  transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.8, 1.1] }) }]
                }]} />
                <Animated.View style={[styles.premiumSunSceneDisk, {
                  top: sunY,
                  backgroundColor: style.accent,
                  opacity: 1,
                  transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.05] }) }]
                }]} />
              </>
            );
          }
          return null;
        })()}

        {/* Weather Specific Particle Layers */}
        {(() => {
          switch (style.effect) {
            case 'rain':
            case 'thunder':
              return (
                <>
                  {particles.rain.map((p, i) => (
                    <Animated.View key={i} style={[styles.rainLine, {
                      left: p.left, opacity: p.opacity,
                      transform: [{ translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-200, 1500 * p.speed] }) }]
                    }]} />
                  ))}
                  {style.effect === 'thunder' && (
                    <Animated.View style={[StyleSheet.absoluteFill, {
                      backgroundColor: '#FFF',
                      opacity: masterAnim.interpolate({
                        inputRange: [0, 0.1, 0.11, 0.12, 0.2, 0.48, 0.49, 0.5, 0.51, 0.52, 0.8, 0.81, 0.82, 1],
                        outputRange: [0, 0, 0.4, 0, 0, 0, 0.6, 0, 0.3, 0, 0, 0.5, 0, 0]
                      })
                    }]} />
                  )}
                </>
              );
            case 'snow':
              return particles.snow.map((p, i) => (
                <Animated.View key={i} style={[styles.snowParticle, {
                  left: p.left, opacity: 0.6, width: p.size, height: p.size,
                  transform: [
                    { translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-200, 1200 * p.speed] }) },
                    { translateX: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [-p.drift, p.drift] }) }
                  ]
                }]} />
              ));
            default: return null;
          }
        })()}
      </View>
    );
  };

  return (
    <Animated.View style={[styles.outerContainer, {
      opacity: entryAnim,
      transform: [{ scale: entryAnim.interpolate({ inputRange: [0, 1], outputRange: [0.98, 1] }) }]
    }]}>
      <LinearGradient colors={style.gradients} start={{ x: 0, y: 0 }} end={{ x: 0, y: 1 }} style={styles.card}>
        <View style={StyleSheet.absoluteFill}>{renderEffect()}</View>

        {/* Subtle Horizon Haze Overlay */}
        <LinearGradient
          colors={['rgba(255,255,255,0.05)', 'transparent', 'rgba(0,0,0,0.15)']}
          style={StyleSheet.absoluteFill}
          pointerEvents="none"
        />

        <View style={styles.content}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.cityText, { color: secondaryColor }]}>{city.toUpperCase()}</Text>
              <Text style={[styles.conditionText, { color: textColor }]}>{displayTitle.toUpperCase()}</Text>
            </View>
            <View style={styles.iconContainer}>
              <Text style={styles.weatherEmoji}>{displayEmoji}</Text>
            </View>
          </View>

          <View style={styles.mainInfo}>
            <Text style={[styles.tempText, { color: textColor }]}>{Math.round(temperature)}°</Text>
            <Text style={[styles.feelsLikeText, { color: secondaryColor }]}>HİSSEDİLEN {Math.round(feelsLike)}°</Text>
          </View>

          <View style={styles.glassBar}>
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>NEM</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>%65</Text>
            </View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>RÜZGAR</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>12 KM/S</Text>
            </View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}>
              <Text style={[styles.infoLabel, { color: secondaryColor }]}>UV</Text>
              <Text style={[styles.infoValue, { color: textColor }]}>{uvIcon} 4</Text>
            </View>
          </View>
        </View>
      </LinearGradient>
    </Animated.View>
  );
};

const CloudLayer = ({ count, color }: { count: number, color: string }) => {
    const clouds = useMemo(() => [...Array(count)].map((_, i) => ({
        top: 10 + (Math.random() * 60),
        scale: 1.0 + Math.random() * 2.0,
        opacity: 0.08 + Math.random() * 0.15,
        leftOffset: Math.random() * 400 - 200,
        width: 140 + Math.random() * 100,
        height: 70 + Math.random() * 60,
        rotation: (Math.random() - 0.5) * 5,
        floatRange: 5 + Math.random() * 10,
        duration: 120000 + Math.random() * 40000,
        delay: Math.random() * -100000, // Random start phase
    })), [count]);

    return clouds.map((c, i) => (
        <IndividualCloud key={i} cloud={c} color={color} />
    ));
};

const IndividualCloud = ({ cloud, color }: { cloud: any, color: string }) => {
    const drift = useRef(new Animated.Value(0)).current;
    const float = useRef(new Animated.Value(0)).current;

    useEffect(() => {
        const startDrift = () => {
            drift.setValue(0);
            Animated.timing(drift, {
                toValue: 1,
                duration: cloud.duration,
                easing: Easing.linear,
                useNativeDriver: true
            }).start(() => startDrift());
        };

        // Offset the start time
        const timeout = setTimeout(() => {
            startDrift();
        }, cloud.delay < 0 ? 0 : cloud.delay);

        Animated.loop(
            Animated.sequence([
                Animated.timing(float, { toValue: 1, duration: 25000 + Math.random() * 15000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
                Animated.timing(float, { toValue: 0, duration: 25000 + Math.random() * 15000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
            ])
        ).start();

        return () => {
            clearTimeout(timeout);
            drift.stopAnimation();
            float.stopAnimation();
        };
    }, []);

    // Build organic cloud mass with overlapping soft circles
    return (
        <Animated.View style={[styles.cloudWrapper, {
            top: cloud.top,
            transform: [
                { translateX: drift.interpolate({
                  inputRange: [0, 1],
                  outputRange: [-600 + cloud.leftOffset, SCREEN_WIDTH + 400 + cloud.leftOffset]
                }) },
                { translateY: float.interpolate({
                  inputRange: [0, 1],
                  outputRange: [-cloud.floatRange, cloud.floatRange]
                }) },
                { scale: cloud.scale },
                { rotate: `${cloud.rotation}deg` }
            ]
        }]}>
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity, width: 140, height: 70, borderRadius: 70 }]} />
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity * 0.7, width: 100, height: 60, borderRadius: 60, left: -40, top: 5 }]} />
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity * 0.8, width: 110, height: 65, borderRadius: 65, right: -30, top: -10 }]} />
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity * 0.4, width: 80, height: 50, borderRadius: 50, top: -20, left: 20 }]} />
        </Animated.View>
    );
};

const styles = StyleSheet.create({
  outerContainer: { marginHorizontal: Spacing.md, marginBottom: Spacing.lg, borderRadius: 32, elevation: 12 },
  card: { height: 320, borderRadius: 32, overflow: 'hidden', padding: 28, borderWidth: 1, borderColor: 'rgba(255,255,255,0.12)' },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 10 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cityText: { fontSize: 12, fontWeight: '800', letterSpacing: 1.8 },
  conditionText: { fontSize: 22, fontWeight: '900', marginTop: 2, letterSpacing: 1.2 },
  iconContainer: { width: 46, height: 44, justifyContent: 'center', alignItems: 'center', borderRadius: 22, backgroundColor: 'rgba(255,255,255,0.12)' },
  weatherEmoji: { fontSize: 22 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 105, fontWeight: '100', letterSpacing: -5, lineHeight: 115 },
  feelsLikeText: { fontSize: 14, fontWeight: '800', marginTop: -5 },
  glassBar: {
    height: 68,
    backgroundColor: 'rgba(255, 255, 255, 0.08)',
    borderRadius: 28,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    borderWidth: 1.5,
    borderColor: 'rgba(255,255,255,0.18)',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.15,
    shadowRadius: 16
  },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 9, fontWeight: '900' },
  infoValue: { fontSize: 16, fontWeight: '900', marginTop: 2 },
  infoDivider: { width: 1, height: 22, backgroundColor: 'rgba(255,255,255,0.15)' },
  premiumSunSceneDisk: { position: 'absolute', right: 55, width: 38, height: 38, borderRadius: 19, zIndex: 3, shadowColor: '#FFF', shadowRadius: 10, shadowOpacity: 0.4 },
  premiumSunSceneGlow: { position: 'absolute', right: 10, width: 120, height: 120, borderRadius: 60, zIndex: 1 },
  star: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 10 },
  rainLine: { position: 'absolute', width: 1, height: 40, backgroundColor: 'rgba(255,255,255,0.35)', zIndex: 6 },
  cloudWrapper: { position: 'absolute', zIndex: 4, alignItems: 'center', justifyContent: 'center' },
  cloudPuff: { position: 'absolute' },
  snowParticle: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 12, zIndex: 6 },
  hazeLayer: { ...StyleSheet.absoluteFillObject, zIndex: 1 },
  moonContainer: { position: 'absolute', top: 55, right: 65, width: 42, height: 42, zIndex: 3 },
  moonGlow: { position: 'absolute', width: 42, height: 42, borderRadius: 21, zIndex: 1 },
  moonMain: { width: 42, height: 42, borderRadius: 21, backgroundColor: '#F1F5F9', shadowColor: '#94A3B8', shadowOffset: { width: 0, height: 0 }, shadowRadius: 20, elevation: 8 },
  moonShadow: { position: 'absolute', width: 38, height: 38, borderRadius: 19, top: -5, left: 14 },
});
