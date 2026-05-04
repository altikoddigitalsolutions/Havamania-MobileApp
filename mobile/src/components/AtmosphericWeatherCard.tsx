import React, { useEffect, useRef, useState } from 'react';
import { StyleSheet, View, Text, Animated, Easing, AccessibilityInfo } from 'react-native';
import { Spacing, AppColors } from '../theme';

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

interface AtmosphericWeatherCardProps {
  city: string;
  temperature: number;
  description: string;
  high: number;
  low: number;
  feelsLike: number;
  weatherCode: number;
  isDay: boolean;
  lastUpdated: string;
  C: AppColors;
}

export const AtmosphericWeatherCard: React.FC<AtmosphericWeatherCardProps> = ({
  city,
  temperature,
  description,
  feelsLike,
  weatherCode,
  isDay,
}) => {
  const [reducedMotion, setReducedMotion] = useState(false);

  // Anim Values
  const entryAnim = useRef(new Animated.Value(0)).current;
  const bgAnim = useRef(new Animated.Value(0)).current;
  const rainAnim = useRef(new Animated.Value(0)).current;
  const cloudAnim = useRef(new Animated.Value(0)).current;
  const pulseAnim = useRef(new Animated.Value(0)).current;
  const floatAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReducedMotion);

    if (reducedMotion) return;

    // 1. Entry Animation (500ms)
    Animated.timing(entryAnim, {
      toValue: 1,
      duration: 500,
      easing: Easing.out(Easing.quad),
      useNativeDriver: true,
    }).start();

    // 2. Gradient Motion (15s)
    Animated.loop(
      Animated.sequence([
        Animated.timing(bgAnim, { toValue: 1, duration: 15000, easing: Easing.linear, useNativeDriver: true }),
        Animated.timing(bgAnim, { toValue: 0, duration: 15000, easing: Easing.linear, useNativeDriver: true }),
      ])
    ).start();

    // 3. Floating Icon (5s)
    Animated.loop(
      Animated.sequence([
        Animated.timing(floatAnim, { toValue: 1, duration: 2500, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
        Animated.timing(floatAnim, { toValue: 0, duration: 2500, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      ])
    ).start();

    // Weather Effects
    Animated.loop(
      Animated.timing(rainAnim, { toValue: 1, duration: 2000, easing: Easing.linear, useNativeDriver: true })
    ).start();

    Animated.loop(
      Animated.timing(cloudAnim, { toValue: 1, duration: 30000, easing: Easing.linear, useNativeDriver: true })
    ).start();

    Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, { toValue: 1, duration: 2000, easing: Easing.inOut(Easing.quad), useNativeDriver: true }),
        Animated.timing(pulseAnim, { toValue: 0, duration: 2000, easing: Easing.inOut(Easing.quad), useNativeDriver: true }),
      ])
    ).start();
  }, [reducedMotion]);

  const getAtmosphere = () => {
    const hour = new Date().getHours();
    const isNightTime = hour >= 21 || hour < 6;
    const isEvening = hour >= 18 && hour < 21;

    // 1. CLOUDY + DAY -> PREMIUM TASARIM (Requirement 1 & 2)
    // KESİN KURAL: Bu blok her şeyin üstünde olmalı.
    if (isDay && (weatherCode === 3 || weatherCode === 2)) {
      let gradients = ['#C4D2E0', '#9FB2C6', '#7C92A8'];
      const isLightMode = C.bg === '#F3F4F6';

      if (isLightMode) {
        // Light Mode Adjustment: Daha canlı ve yüksek kontrastlı
        gradients = ['#E2EBF4', '#A9BDD1', '#89A1B9'];
      }

      return {
        gradients,
        accent: '#FFFFFF',
        type: 'cloudy',
        isDark: true, // Soft white text as requested
        premium: true
      };
    }

    if (!isDay || isNightTime) {
      return {
        gradients: hour < 6 ? ['#020617', '#0F172A'] : ['#0F172A', '#1E293B'],
        accent: '#94A3B8',
        type: 'night',
        isDark: true
      };
    }

    if (weatherCode <= 1) {
      if (isEvening) {
        return { gradients: ['#F59E0B', '#7C2D12'], accent: '#FEF3C7', type: 'sunny', isDark: true };
      }
      return { gradients: ['#0EA5E9', '#38BDF8', '#7DD3FC'], accent: '#FFD600', type: 'sunny', isDark: false };
    }

    // Default Cloudy Fallback (If not isDay)
    if (weatherCode <= 3) {
      return { gradients: ['#64748B', '#94A3B8', '#CBD5E1'], accent: '#F1F5F9', type: 'cloudy', isDark: false };
    }

    if (weatherCode >= 51 && weatherCode <= 67 || weatherCode >= 80) {
      return { gradients: ['#1E293B', '#334155', '#475569'], accent: '#60A5FA', type: 'rainy', isDark: true };
    }

    return { gradients: ['#020617', '#1E1B4B', '#1E293B'], accent: '#FACC15', type: 'stormy', isDark: true };
  };

  const atmosphere: any = getAtmosphere();

  // DEBUG - as requested
  const currentHour = new Date().getHours();
  console.log(`WeatherCard: condition=${weatherCode === 3 ? 'CLOUDY' : weatherCode} hour=${currentHour} timeOfDay=${isDay ? 'DAY' : 'NIGHT'} style=${atmosphere.type}`);

  const textColor = atmosphere.isDark ? '#FFF' : '#0F172A';
  const secondaryColor = atmosphere.isDark ? 'rgba(255,255,255,0.85)' : 'rgba(15,23,42,0.7)';

  // Derived styles
  const cardEntryStyle = {
    opacity: reducedMotion ? 1 : entryAnim,
    transform: [{ scale: reducedMotion ? 1 : entryAnim.interpolate({ inputRange: [0, 1], outputRange: [0.97, 1] }) }]
  };

  const bgMotionStyle = {
    transform: [{ translateY: bgAnim.interpolate({ inputRange: [0, 1], outputRange: [-5, 5] }) }]
  };

  const iconMotionStyle = {
    transform: [{ translateY: floatAnim.interpolate({ inputRange: [0, 1], outputRange: [-2, 2] }) }]
  };

  return (
    <Animated.View style={[styles.outerContainer, cardEntryStyle]}>
      <LinearGradient
        colors={atmosphere.gradients}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={styles.card}
      >
        {/* Layer 1: Gradient Motion */}
        <Animated.View style={[StyleSheet.absoluteFill, bgMotionStyle]}>
             <LinearGradient colors={atmosphere.gradients} style={StyleSheet.absoluteFill} />
        </Animated.View>

        {/* Layer 2: Noise */}
        <View style={styles.noiseLayer}>
            {[...Array(20)].map((_, i) => (
                <View key={i} style={[styles.noiseDot, { top: `${(i * 13) % 100}%`, left: `${(i * 19) % 100}%`, opacity: 0.04 }]} />
            ))}
        </View>

        {/* Layer 3: Weather Overlay */}
        <View style={styles.atmosphereContainer}>
            {atmosphere.type === 'sunny' && (
                <Animated.View style={[styles.sunnyGlow, {
                    backgroundColor: atmosphere.accent,
                    opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.12, 0.22] })
                }]} />
            )}

            {atmosphere.type === 'night' && (
                <View style={styles.starContainer}>
                    {[...Array(15)].map((_, i) => (
                        <View key={i} style={[styles.star, { top: `${(i * 17) % 100}%`, left: `${(i * 23) % 100}%`, opacity: (i % 3 === 0) ? 0.6 : 0.3 }]} />
                    ))}
                </View>
            )}

            {atmosphere.type === 'rainy' && (
                <View style={styles.fullFill}>
                    {[...Array(20)].map((_, i) => {
                        const translateY = rainAnim.interpolate({ inputRange: [0, 1], outputRange: [0, 120] });
                        return (
                            <Animated.View key={i} style={[styles.rainLine, {
                                left: `${(i * 5) % 100}%`, top: `${(i * 31) % 100}%`, opacity: 0.12,
                                transform: [{ translateY }]
                            }]} />
                        );
                    })}
                </View>
            )}

            {atmosphere.type === 'cloudy' && (
                <Animated.View style={[styles.fullFill, {
                    transform: [{ translateX: cloudAnim.interpolate({ inputRange: [0, 1], outputRange: [0, 40] }) }]
                }]}>
                    {atmosphere.premium && (
                        <View style={[styles.hazeOverlay, { backgroundColor: '#FFF', opacity: 0.04 }]} />
                    )}
                    <View style={[styles.cloudBlob, { top: 20, left: -20, width: 150, opacity: atmosphere.premium ? 0.12 : 0.05 }]} />
                    <View style={[styles.cloudBlob, { bottom: 60, right: -40, width: 220, opacity: atmosphere.premium ? 0.15 : 0.08 }]} />
                </Animated.View>
            )}

            {/* Premium Glow Overlay */}
            {atmosphere.type === 'cloudy' && atmosphere.premium && (
                <View style={[styles.softGlow, { top: -60, right: -60, backgroundColor: '#FFF', opacity: 0.12 }]} />
            )}
        </View>

        {/* Layer 4: Content */}
        <View style={styles.content}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.cityText, { color: secondaryColor }]}>{city.toUpperCase()}</Text>
              <Text style={[styles.conditionText, { color: textColor }]}>{description}</Text>
            </View>

            <Animated.View style={[styles.iconContainer, iconMotionStyle]}>
                <View style={[styles.iconGlow, { backgroundColor: atmosphere.accent }]} />
                <Text style={styles.weatherEmoji}>
                    {atmosphere.type === 'sunny' ? '☀️' : atmosphere.type === 'night' ? '🌙' : atmosphere.type === 'rainy' ? '💧' : '☁️'}
                </Text>
            </Animated.View>
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
  outerContainer: { marginHorizontal: Spacing.md, marginBottom: Spacing.lg, borderRadius: 32, shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.2, shadowRadius: 15, elevation: 8 },
  card: { height: 320, borderRadius: 32, overflow: 'hidden', padding: 24 },
  noiseLayer: { ...StyleSheet.absoluteFillObject },
  noiseDot: { position: 'absolute', width: 2, height: 2, backgroundColor: '#FFF', borderRadius: 1 },
  atmosphereContainer: { ...StyleSheet.absoluteFillObject, overflow: 'hidden' },
  fullFill: { ...StyleSheet.absoluteFillObject },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 10 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cityText: { fontSize: 12, fontWeight: '900', letterSpacing: 2 },
  conditionText: { fontSize: 24, fontWeight: '800', marginTop: 2 },
  iconContainer: { width: 48, height: 48, justifyContent: 'center', alignItems: 'center' },
  iconGlow: { position: 'absolute', width: 32, height: 32, borderRadius: 16, opacity: 0.2 },
  weatherEmoji: { fontSize: 28 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 100, fontWeight: '100', letterSpacing: -4 },
  feelsLikeText: { fontSize: 14, fontWeight: '600', marginTop: -10 },
  glassBar: { height: 54, backgroundColor: 'rgba(255, 255, 255, 0.1)', borderRadius: 16, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 20, borderBottomWidth: 0.5, borderColor: 'rgba(255,255,255,0.2)' },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 9, fontWeight: '800', letterSpacing: 1 },
  infoValue: { fontSize: 13, fontWeight: '800', marginTop: 1 },
  infoDivider: { width: 1, height: 20, backgroundColor: 'rgba(255,255,255,0.1)' },
  sunnyGlow: { position: 'absolute', top: -50, right: -50, width: 200, height: 200, borderRadius: 100 },
  starContainer: { ...StyleSheet.absoluteFillObject },
  star: { position: 'absolute', width: 2, height: 2, backgroundColor: '#FFF', borderRadius: 1 },
  rainLine: { position: 'absolute', width: 1, height: 15, backgroundColor: '#FFF', transform: [{ rotate: '15deg' }] },
  cloudBlob: { position: 'absolute', height: 100, backgroundColor: '#FFF', borderRadius: 50 },
  softGlow: { position: 'absolute', width: 280, height: 280, borderRadius: 140 },
  hazeOverlay: { ...StyleSheet.absoluteFillObject },
});
