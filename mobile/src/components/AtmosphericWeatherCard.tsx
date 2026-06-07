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

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get('window');

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
  cloudDensity: number;
  isNight: boolean;
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
  if (hour >= 6 && hour <= 18) {
    const progress = (hour - 6) / 6;
    return 90 * (1 - Math.abs(progress - 1));
  } else {
    const h = hour < 6 ? hour + 24 : hour;
    const progress = (h - 18) / 6;
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
  const isFoggy = condition === WeatherCondition.FOG;

  // PREMIUM PALETTES
  if (isNight) {
    grads = (isCloudy || isRainy || isFoggy) ? ['#020617', '#0F172A', '#1E293B'] : ['#020617', '#0F172A', '#1E1B4B'];
  } else if (timeOfDay === TimeOfDay.DAWN) {
    grads = isCloudy ? ['#334155', '#475569', '#FBCFE8'] : ['#2D1B69', '#DB2777', '#F59E0B'];
  } else if (timeOfDay === TimeOfDay.SUNSET || timeOfDay === TimeOfDay.GOLDEN_HOUR) {
    grads = isCloudy ? ['#2C3E50', '#4A5568', '#FED7AA'] : ['#1E293B', '#7C2D12', '#F97316'];
  } else if (isRainy || condition === WeatherCondition.THUNDERSTORM) {
    grads = condition === WeatherCondition.THUNDERSTORM ? ['#0F172A', '#1E1B4B', '#312E81'] : ['#2D3748', '#4A5568', '#718096'];
  } else if (isFoggy) {
    grads = ['#718096', '#A0AEC0', '#CBD5E1'];
  } else if (isCloudy) {
    grads = condition === WeatherCondition.PARTLY_CLOUDY ? ['#4A5568', '#718096', '#A0AEC0'] : ['#5E6B7D', '#6E7B8B', '#4B5565'];
  } else {
    grads = temperature > 28 ? ['#0284C7', '#0EA5E9', '#FDE047'] : ['#0369A1', '#0EA5E9', '#7DD3FC'];
  }

  const cloudDensity = condition === WeatherCondition.OVERCAST ? 8 :
                       condition === WeatherCondition.CLOUDY ? 6 :
                       condition === WeatherCondition.PARTLY_CLOUDY ? 4 : 2;

  const style: WeatherCardStyle = {
    gradients: grads,
    effect: 'sunny',
    isWhiteText: isNight || isRainy || isCloudy || isFoggy || (altitude < 10),
    accent: isNight ? '#93C5FD' : '#FDE047',
    name: timeOfDay,
    altitude: altitude,
    cloudDensity: cloudDensity,
    isNight: isNight
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
      case WeatherCondition.CLEAR_NIGHT: return '🌙';
      case WeatherCondition.PARTLY_CLOUDY:
      case WeatherCondition.CLOUDY:
      case WeatherCondition.OVERCAST: return '☁️';
      case WeatherCondition.RAIN:
      case WeatherCondition.RAIN_NIGHT: return '🌧️';
      case WeatherCondition.THUNDERSTORM: return '⛈️';
      case WeatherCondition.SNOW:
      case WeatherCondition.SNOW_NIGHT: return '🌨️';
      default: return '☁️';
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
  const lightningAnim = useRef(new Animated.Value(0)).current;

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

    if (style.effect === 'thunder') {
        const triggerLightning = () => {
            const delay = Math.random() * 10000 + 5000;
            setTimeout(() => {
                Animated.sequence([
                    Animated.timing(lightningAnim, { toValue: 1, duration: 50, useNativeDriver: true }),
                    Animated.timing(lightningAnim, { toValue: 0, duration: 100, useNativeDriver: true }),
                    Animated.timing(lightningAnim, { toValue: 0.5, duration: 50, useNativeDriver: true }),
                    Animated.timing(lightningAnim, { toValue: 0, duration: 200, useNativeDriver: true }),
                ]).start(() => triggerLightning());
            }, delay);
        };
        triggerLightning();
    }
  }, [reducedMotion, style.effect]);

  const particles = useMemo(() => {
    const starCount = style.isNight ? Math.max(5, 45 - (style.cloudDensity * 5)) : 0;
    return {
      rainFront: [...Array(35)].map((_, i) => ({ left: `${Math.random() * 100}%`, opacity: 0.25 + Math.random() * 0.15, speed: 1.2, height: 45, width: 1.0 })),
      rainBack: [...Array(25)].map((_, i) => ({ left: `${Math.random() * 100}%`, opacity: 0.1 + Math.random() * 0.1, speed: 0.8, height: 25, width: 0.6 })),
      stars: [...Array(starCount)].map((_, i) => ({ left: `${Math.random() * 100}%`, top: `${Math.random() * 75}%`, size: 0.5 + Math.random() * 0.8, opacity: 0.1 + Math.random() * 0.3 })),
      snow: [...Array(35)].map((_, i) => ({ left: `${Math.random() * 100}%`, size: 2 + Math.random() * 3, speed: 0.3 + Math.random() * 0.4, drift: Math.random() * 40 - 20 }))
    };
  }, [style.cloudDensity, style.isNight]);

  const textColor = style.isWhiteText ? '#FFF' : '#0F172A';
  const secondaryColor = style.isWhiteText ? 'rgba(255,255,255,0.75)' : 'rgba(15,23,42,0.65)';
  const uvIcon = altitude <= 0 ? '🌙' : '☀️';

  const renderEffect = () => {
    if (reducedMotion) return null;
    const sunY = 70 - ((Math.max(0, altitude) / 90) * 55);
    const isNight = style.isNight;

    return (
      <View style={StyleSheet.absoluteFill}>
        {/* LIGHTNING FLASH (Storm Only) */}
        {style.effect === 'thunder' && (
            <Animated.View style={[StyleSheet.absoluteFill, { backgroundColor: '#BBDEFB', opacity: lightningAnim.interpolate({ inputRange: [0, 1], outputRange: [0, 0.25] }), zIndex: 10 }]} />
        )}

        {/* ATMOSPHERE BASE PULSE */}
        <Animated.View style={[styles.hazeLayer, { backgroundColor: isNight ? '#1E293B' : style.accent, opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.03, 0.08] }) }]} />

        {/* STARS */}
        {isNight && particles.stars.map((p, i) => (
          <Animated.View key={i} style={[styles.star, { left: p.left, top: p.top, width: p.size, height: p.size, opacity: pulseAnim.interpolate({ inputRange: [0, 0.5, 1], outputRange: [p.opacity, p.opacity * 2.5, p.opacity] }) }]} />
        ))}

        {/* CELESTIAL BODY */}
        {(() => {
          if (isNight) {
            return (
              <View style={styles.moonContainer}>
                <Animated.View style={[styles.moonGlow, { backgroundColor: '#94A3B8', opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.1, 0.2] }), transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1.3, 1.8] }) }] }]} />
                <Animated.View style={[styles.moonMain, { shadowOpacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.4, 0.6] }), transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.03] }) }] }]} />
                <View style={[styles.moonShadow, { backgroundColor: style.gradients[0] }]} />
              </View>
            );
          } else if (['sunny', 'partly_cloudy', 'sunset', 'dawn'].includes(style.effect) && style.cloudDensity < 8) {
            return (
              <>
                <Animated.View style={[styles.premiumSunSceneGlow, { top: sunY - 40, right: 15, width: 130, height: 130, borderRadius: 65, backgroundColor: style.accent, opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.06, 0.12] }), transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.95, 1.05] }) }] }]} />
                <Animated.View style={[styles.premiumSunSceneDisk, { top: sunY, backgroundColor: '#FEFCE8', shadowColor: style.accent, opacity: 0.85, transform: [{ scale: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [1, 1.03] }) }] }]} />
              </>
            );
          }
          return null;
        })()}

        {/* FOG EFFECT (Specific Redesign) */}
        {style.effect === 'fog' && (
            <View style={StyleSheet.absoluteFill}>
                {[...Array(3)].map((_, i) => (
                    <Animated.View
                        key={i}
                        style={[styles.fogStrip, {
                            top: 40 + i * 80,
                            opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.1 + i * 0.05, 0.2 + i * 0.05] }),
                            transform: [{ translateX: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-SCREEN_WIDTH, SCREEN_WIDTH] }) }]
                        }]}
                    />
                ))}
            </View>
        )}

        {/* PARTICLES */}
        {(() => {
          switch (style.effect) {
            case 'thunder':
            case 'rain':
              return (
                <>
                  {particles.rainBack.map((p, i) => (
                    <Animated.View key={`b${i}`} style={[styles.rainLine, { left: p.left, opacity: p.opacity, transform: [{ translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-200, 1000 * p.speed] }) }] }]} />
                  ))}
                  {particles.rainFront.map((p, i) => (
                    <Animated.View key={`f${i}`} style={[styles.rainLine, { left: p.left, opacity: p.opacity, transform: [{ translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-200, 1400 * p.speed] }) }] }]} />
                  ))}
                </>
              );
            case 'snow':
              return particles.snow.map((p, i) => (
                <Animated.View key={i} style={[styles.snowParticle, { left: p.left, opacity: 0.6, width: p.size, height: p.size, transform: [{ translateY: masterAnim.interpolate({ inputRange: [0, 1], outputRange: [-200, 1000 * p.speed] }) }, { translateX: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [-p.drift, p.drift] }) }] }]} />
              ));
            default: return null;
          }
        })()}

        {/* VOLUMETRIC CLOUDS */}
        {style.cloudDensity > 0 && <CloudLayer count={style.cloudDensity} color={isNight ? '#334155' : (style.effect === 'thunder' ? '#475569' : '#E2E8F0')} />}
      </View>
    );
  };

  return (
    <Animated.View style={[styles.outerContainer, { opacity: entryAnim, transform: [{ scale: entryAnim.interpolate({ inputRange: [0, 1], outputRange: [0.98, 1] }) }] }]}>
      <LinearGradient colors={style.gradients} start={{ x: 0.2, y: 0 }} end={{ x: 0.8, y: 1 }} style={styles.card}>
        <View style={StyleSheet.absoluteFill}>{renderEffect()}</View>
        <LinearGradient colors={['rgba(255,255,255,0.02)', 'transparent', 'rgba(0,0,0,0.12)']} style={StyleSheet.absoluteFill} pointerEvents="none" />
        <View style={styles.content}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.cityText, { color: secondaryColor }]}>{city.toUpperCase()}</Text>
              <Text style={[styles.conditionText, { color: textColor }]}>{displayTitle.toUpperCase()}</Text>
            </View>
            <View style={styles.iconContainer}><Text style={styles.weatherEmoji}>{displayEmoji}</Text></View>
          </View>
          <View style={styles.mainInfo}>
            <Text style={[styles.tempText, { color: textColor }]}>{Math.round(temperature)}°</Text>
            <Text style={[styles.feelsLikeText, { color: secondaryColor }]}>HİSSEDİLEN {Math.round(feelsLike)}°</Text>
          </View>
          <View style={styles.glassBar}>
            <View style={styles.infoItem}><Text style={[styles.infoLabel, { color: secondaryColor }]}>NEM</Text><Text style={[styles.infoValue, { color: textColor }]}>%65</Text></View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}><Text style={[styles.infoLabel, { color: secondaryColor }]}>RÜZGAR</Text><Text style={[styles.infoValue, { color: textColor }]}>12 KM/S</Text></View>
            <View style={styles.infoDivider} />
            <View style={styles.infoItem}><Text style={[styles.infoLabel, { color: secondaryColor }]}>UV</Text><Text style={[styles.infoValue, { color: textColor }]}>{uvIcon} {style.isNight ? '0' : '4'}</Text></View>
          </View>
        </View>
      </LinearGradient>
    </Animated.View>
  );
};

const CloudLayer = ({ count, color }: { count: number, color: string }) => {
    const clouds = useMemo(() => [...Array(count)].map((_, i) => ({
        top: 5 + (Math.random() * 55),
        scale: 1.2 + Math.random() * 1.8,
        opacity: 0.12 + Math.random() * 0.15,
        leftOffset: Math.random() * 600 - 300,
        duration: 180000 + Math.random() * 60000,
        delay: Math.random() * -150000,
    })), [count]);
    return clouds.map((c, i) => <IndividualCloud key={i} cloud={c} color={color} />);
};

const IndividualCloud = ({ cloud, color }: { cloud: any, color: string }) => {
    const drift = useRef(new Animated.Value(0)).current;
    const float = useRef(new Animated.Value(0)).current;
    useEffect(() => {
        const startDrift = () => { drift.setValue(0); Animated.timing(drift, { toValue: 1, duration: cloud.duration, easing: Easing.linear, useNativeDriver: true }).start(() => startDrift()); };
        const timeout = setTimeout(() => startDrift(), cloud.delay < 0 ? 0 : cloud.delay);
        Animated.loop(Animated.sequence([Animated.timing(float, { toValue: 1, duration: 40000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }), Animated.timing(float, { toValue: 0, duration: 40000, easing: Easing.inOut(Easing.sine), useNativeDriver: true })])).start();
        return () => { clearTimeout(timeout); drift.stopAnimation(); float.stopAnimation(); };
    }, []);

    return (
        <Animated.View style={[styles.cloudWrapper, { top: cloud.top, transform: [{ translateX: drift.interpolate({ inputRange: [0, 1], outputRange: [-800 + cloud.leftOffset, SCREEN_WIDTH + 400 + cloud.leftOffset] }) }, { translateY: float.interpolate({ inputRange: [0, 1], outputRange: [-15, 15] }) }, { scale: cloud.scale }] }]}>
            {/* VOLUMETRIC PUFFS */}
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity, width: 140, height: 75, borderRadius: 70 }]} />
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity * 0.8, width: 110, height: 65, borderRadius: 60, left: -45, top: 10 }]} />
            <View style={[styles.cloudPuff, { backgroundColor: color, opacity: cloud.opacity * 0.9, width: 120, height: 70, borderRadius: 65, right: -40, top: -15 }]} />
            {/* INNER LIGHTING EFFECT */}
            <View style={[styles.cloudPuff, { backgroundColor: '#FFF', opacity: 0.15, width: 80, height: 35, borderRadius: 40, top: -5 }]} />
        </Animated.View>
    );
};

const styles = StyleSheet.create({
  outerContainer: { marginHorizontal: Spacing.md, marginBottom: Spacing.lg, borderRadius: 32, elevation: 12 },
  card: { height: 320, borderRadius: 32, overflow: 'hidden', padding: 28, borderWidth: 1.5, borderColor: 'rgba(255,255,255,0.12)' },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 10 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  cityText: { fontSize: 12, fontWeight: '800', letterSpacing: 1.8 },
  conditionText: { fontSize: 24, fontWeight: '900', marginTop: 2, letterSpacing: 1.2 },
  iconContainer: { width: 48, height: 48, justifyContent: 'center', alignItems: 'center', borderRadius: 24, backgroundColor: 'rgba(255,255,255,0.12)', borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)' },
  weatherEmoji: { fontSize: 24 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 105, fontWeight: '100', letterSpacing: -5, lineHeight: 115 },
  feelsLikeText: { fontSize: 14, fontWeight: '800', marginTop: -5 },
  glassBar: { height: 68, backgroundColor: 'rgba(255, 255, 255, 0.08)', borderRadius: 28, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 20, borderWidth: 1.2, borderColor: 'rgba(255,255,255,0.12)', shadowColor: '#000', shadowOffset: { width: 0, height: 6 }, shadowOpacity: 0.15, shadowRadius: 12 },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 9, fontWeight: '900' },
  infoValue: { fontSize: 16, fontWeight: '900', marginTop: 2 },
  infoDivider: { width: 1, height: 24, backgroundColor: 'rgba(255,255,255,0.15)' },
  premiumSunSceneDisk: { position: 'absolute', right: 55, width: 42, height: 42, borderRadius: 21, zIndex: 3, shadowRadius: 20, shadowOpacity: 0.5 },
  premiumSunSceneGlow: { position: 'absolute', zIndex: 1 },
  star: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 10 },
  rainLine: { position: 'absolute', width: 0.9, height: 45, backgroundColor: 'rgba(255,255,255,0.3)', zIndex: 6 },
  cloudWrapper: { position: 'absolute', zIndex: 4, alignItems: 'center', justifyContent: 'center' },
  cloudPuff: { position: 'absolute' },
  snowParticle: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 12, zIndex: 6 },
  hazeLayer: { ...StyleSheet.absoluteFillObject, zIndex: 1 },
  fogStrip: { position: 'absolute', width: SCREEN_WIDTH * 2, height: 120, backgroundColor: 'rgba(255,255,255,0.4)', borderRadius: 100, blur: 40 },
  moonContainer: { position: 'absolute', top: 55, right: 65, width: 42, height: 42, zIndex: 3 },
  moonGlow: { position: 'absolute', width: 42, height: 42, borderRadius: 21, zIndex: 1 },
  moonMain: { width: 42, height: 42, borderRadius: 21, backgroundColor: '#F1F5F9', shadowColor: '#94A3B8', shadowOffset: { width: 0, height: 0 }, shadowRadius: 20, elevation: 8 },
  moonShadow: { position: 'absolute', width: 38, height: 38, borderRadius: 19, top: -5, left: 14 },
});
