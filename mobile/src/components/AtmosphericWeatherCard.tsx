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

// ── Tipler ve Konfigürasyon ──────────────────────────────────────────────────

enum TimePeriod {
  DEEP_NIGHT = 'DEEP_NIGHT',
  SUNRISE = 'SUNRISE',
  MORNING = 'MORNING',
  DAY = 'DAY',
  AFTERNOON = 'AFTERNOON',
  SUNSET = 'SUNSET',
  DUSK = 'DUSK',
  NIGHT = 'NIGHT',
}

const GRADIENT_CONFIG = {
  [TimePeriod.DEEP_NIGHT]: ['#020617', '#0F172A', '#1E293B'],
  [TimePeriod.SUNRISE]:    ['#FFEDD5', '#F97316', '#7DD3FC'],
  [TimePeriod.MORNING]:    ['#7DD3FC', '#38BDF8', '#BAE6FD'],
  [TimePeriod.DAY]:        ['#38BDF8', '#7DD3FC', '#BAE6FD'],
  [TimePeriod.AFTERNOON]:  ['#7DD3FC', '#F97316', '#FEF3C7'],
  [TimePeriod.SUNSET]:     ['#F97316', '#7C2D12', '#431407'],
  [TimePeriod.DUSK]:       ['#1E1B4B', '#2E1065', '#0F172A'],
  [TimePeriod.NIGHT]:      ['#0F172A', '#020617', '#0F172A'],
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

// ── Ana Bileşen ─────────────────────────────────────────────────────────────

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

  // 1. Zaman ve Güneş/Ay Parametreleri
  const now = useMemo(() => (time ? new Date(time) : new Date()), [time]);
  const currentHour = now.getHours() + now.getMinutes() / 60;

  const sunriseHour = useMemo(() => {
    if (!sunrise) return 6;
    const d = new Date(sunrise);
    return d.getHours() + d.getMinutes() / 60;
  }, [sunrise]);

  const sunsetHour = useMemo(() => {
    if (!sunset) return 20.5; // Yaklaşık 20:30
    const d = new Date(sunset);
    return d.getHours() + d.getMinutes() / 60;
  }, [sunset]);

  const isActuallyDay = currentHour >= sunriseHour && currentHour < sunsetHour;

  // 2. Bağımsız Yörünge Hesaplamaları
  const sunProgress = useMemo(() => {
    const p = (currentHour - sunriseHour) / (sunsetHour - sunriseHour);
    return Math.max(0, Math.min(1, p));
  }, [currentHour, sunriseHour, sunsetHour]);

  const moonProgress = useMemo(() => {
    let elapsed;
    const totalNight = (24 - sunsetHour) + sunriseHour;
    if (currentHour >= sunsetHour) elapsed = currentHour - sunsetHour;
    else elapsed = (24 - sunsetHour) + currentHour;
    const p = elapsed / totalNight;
    return Math.max(0, Math.min(1, p));
  }, [currentHour, sunriseHour, sunsetHour]);

  // 3. Animasyon Değerleri
  const sunPosAnim = useRef(new Animated.Value(sunProgress)).current;
  const moonPosAnim = useRef(new Animated.Value(moonProgress)).current;
  const bgFadeAnim = useRef(new Animated.Value(0)).current;
  const entryAnim = useRef(new Animated.Value(0)).current;
  const pulseAnim = useRef(new Animated.Value(0)).current;

  const currentPeriod = useMemo(() => {
    const h = currentHour;
    if (h < 5) return TimePeriod.DEEP_NIGHT;
    if (h < 7) return TimePeriod.SUNRISE;
    if (h < 10) return TimePeriod.MORNING;
    if (h < 17) return TimePeriod.DAY;
    if (h < 19) return TimePeriod.AFTERNOON;
    if (h < 21) return TimePeriod.SUNSET;
    if (h < 23) return TimePeriod.DUSK;
    return TimePeriod.NIGHT;
  }, [currentHour]);

  const [bgLayerFrom, setBgLayerFrom] = useState(currentPeriod);
  const [bgLayerTo, setBgLayerTo] = useState(currentPeriod);

  // 4. Efekt Başlatıcılar
  useEffect(() => {
    AccessibilityInfo.isReduceMotionEnabled().then(setReducedMotion);
    Animated.timing(entryAnim, { toValue: 1, duration: 800, easing: Easing.out(Easing.back(1)), useNativeDriver: true }).start();

    Animated.loop(Animated.sequence([
      Animated.timing(pulseAnim, { toValue: 1, duration: 3000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
      Animated.timing(pulseAnim, { toValue: 0, duration: 3000, easing: Easing.inOut(Easing.sine), useNativeDriver: true }),
    ])).start();
  }, []);

  useEffect(() => {
    // Konum Güncellemeleri
    Animated.parallel([
      Animated.timing(sunPosAnim, { toValue: sunProgress, duration: 1500, easing: Easing.inOut(Easing.sine), useNativeDriver: false }),
      Animated.timing(moonPosAnim, { toValue: moonProgress, duration: 1500, easing: Easing.inOut(Easing.sine), useNativeDriver: false }),
    ]).start();

    // Arka Plan Geçişi
    if (currentPeriod !== bgLayerTo) {
      setBgLayerFrom(bgLayerTo);
      setBgLayerTo(currentPeriod);
      bgFadeAnim.setValue(0);
      Animated.timing(bgFadeAnim, { toValue: 1, duration: 2500, useNativeDriver: true }).start();
    }
  }, [sunProgress, moonProgress, currentPeriod]);

  // 5. Geometri
  const hPadding = 50;
  const arcHeight = 140;

  // GÜNEŞ YÖRÜNGESİ (Sol -> Sağ)
  const sunX = sunPosAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [hPadding, SCREEN_WIDTH - hPadding - 60]
  });
  const sunY = sunPosAnim.interpolate({
    inputRange: [0, 0.5, 1],
    outputRange: [220, 70, 220]
  });
  const sunOpacity = sunPosAnim.interpolate({
    inputRange: [0, 0.05, 0.95, 1],
    outputRange: [0, 1, 1, 0]
  });

  // AY YÖRÜNGESİ (Sağ -> Sol)
  const moonX = moonPosAnim.interpolate({
    inputRange: [0, 1],
    outputRange: [SCREEN_WIDTH - hPadding - 60, hPadding]
  });
  const moonY = moonPosAnim.interpolate({
    inputRange: [0, 0.5, 1],
    outputRange: [220, 70, 220]
  });
  const moonOpacity = moonPosAnim.interpolate({
    inputRange: [0, 0.05, 0.95, 1],
    outputRange: [0, 1, 1, 0]
  });

  // 6. Render Yardımcıları
  const stars = useMemo(() => [...Array(40)].map((_, i) => ({
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 60}%`,
    size: 0.5 + Math.random() * 1.5,
    delay: Math.random() * 5000,
  })), []);

  const textColor = isActuallyDay && (currentHour > 8 && currentHour < 18) ? '#0F172A' : '#FFF';
  const secondaryColor = isActuallyDay && (currentHour > 8 && currentHour < 18) ? 'rgba(15,23,42,0.6)' : 'rgba(255,255,255,0.7)';

  return (
    <Animated.View style={[styles.outerContainer, { opacity: entryAnim, transform: [{ scale: entryAnim.interpolate({ inputRange: [0, 1], outputRange: [0.95, 1] }) }] }]}>
      <View style={styles.card}>

        {/* Arka Plan Katmanları */}
        <LinearGradient colors={GRADIENT_CONFIG[bgLayerFrom]} style={StyleSheet.absoluteFill} start={{x:0, y:0}} end={{x:1, y:1}} />
        <Animated.View style={[StyleSheet.absoluteFill, { opacity: bgFadeAnim }]}>
          <LinearGradient colors={GRADIENT_CONFIG[bgLayerTo]} style={StyleSheet.absoluteFill} start={{x:0, y:0}} end={{x:1, y:1}} />
        </Animated.View>

        {/* Gökyüzü Elemanları */}
        <View style={StyleSheet.absoluteFill}>
          {!isActuallyDay && stars.map((p, i) => <Star key={i} p={p} />)}

          {/* BAĞIMSIZ GÜNEŞ OBJESİ */}
          {isActuallyDay && (
            <Animated.View style={[
                styles.celestialBody,
                {
                  transform: [
                    { translateX: sunX },
                    { translateY: sunY },
                    { scale: sunPosAnim.interpolate({ inputRange: [0, 0.5, 1], outputRange: [0.8, 1.1, 0.8] }) }
                  ],
                  opacity: sunOpacity
                }
            ]}>
                <Animated.View style={[
                    styles.celestialGlow,
                    { backgroundColor: '#FCD34D', opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.2, 0.4] }) }
                ]} />
                <Text style={styles.celestialEmoji}>☀️</Text>
            </Animated.View>
          )}

          {/* BAĞIMSIZ AY OBJESİ */}
          {!isActuallyDay && (
            <Animated.View style={[
                styles.celestialBody,
                {
                  transform: [
                    { translateX: moonX },
                    { translateY: moonY },
                    { scale: moonPosAnim.interpolate({ inputRange: [0, 0.5, 1], outputRange: [0.7, 1.0, 0.7] }) }
                  ],
                  opacity: moonOpacity
                }
            ]}>
                <Animated.View style={[
                    styles.celestialGlow,
                    { backgroundColor: '#F8FAFC', opacity: pulseAnim.interpolate({ inputRange: [0, 1], outputRange: [0.1, 0.25] }) }
                ]} />
                <Text style={styles.celestialEmoji}>🌙</Text>
            </Animated.View>
          )}
        </View>

        <View style={styles.glassOverlay} />

        <View style={styles.content}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.cityText, { color: secondaryColor }]}>{city.toUpperCase()}</Text>
              <Text style={[styles.conditionText, { color: textColor }]}>{getDisplayTitle(weatherCode, isActuallyDay)}</Text>
            </View>
            <View style={styles.iconContainer}>
              <Text style={styles.weatherEmoji}>{getDisplayEmoji(weatherCode, isActuallyDay)}</Text>
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
      </View>
    </Animated.View>
  );
};

// ── Yardımcı Bileşenler ve Fonksiyonlar ──────────────────────────────────────

const Star = ({ p }: any) => {
    const blink = useRef(new Animated.Value(0)).current;
    useEffect(() => {
      Animated.loop(
        Animated.sequence([
          Animated.delay(p.delay),
          Animated.timing(blink, { toValue: 1, duration: 2000, useNativeDriver: true }),
          Animated.timing(blink, { toValue: 0, duration: 2000, useNativeDriver: true }),
        ])
      ).start();
    }, []);
    return (
      <Animated.View style={[styles.star, {
        left: p.left, top: p.top, width: p.size, height: p.size,
        opacity: blink.interpolate({ inputRange: [0, 1], outputRange: [0.1, 0.7] })
      }]} />
    );
};

const getDisplayTitle = (code: number, isDay: boolean) => {
    if (code === 0) return isDay ? "Güneşli" : "Açık Gece";
    if (code <= 2) return "Az Bulutlu";
    if (code === 3) return "Parçalı Bulutlu";
    if (code <= 48) return "Sisli";
    if (code <= 67) return "Yağmurlu";
    if (code <= 82) return "Sağanak Yağış";
    if (code <= 99) return "Fırtınalı";
    return isDay ? "Güneşli" : "Açık Gece";
};

const getDisplayEmoji = (code: number, isDay: boolean) => {
    if (code === 0) return isDay ? '☀️' : '🌙';
    if (code <= 2) return isDay ? '🌤' : '☁️';
    if (code === 3) return '☁️';
    return '🌧️';
};

const styles = StyleSheet.create({
  outerContainer: { marginHorizontal: Spacing.md, marginBottom: Spacing.lg, borderRadius: 32, shadowColor: '#000', shadowOffset: { width: 0, height: 12 }, shadowOpacity: 0.3, shadowRadius: 20, elevation: 12 },
  card: { height: 320, borderRadius: 32, overflow: 'hidden', padding: 24, borderWidth: 1, borderColor: 'rgba(255,255,255,0.15)' },
  glassOverlay: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: 32, borderTopWidth: 1, borderLeftWidth: 1, borderColor: 'rgba(255,255,255,0.2)' },
  content: { flex: 1, justifyContent: 'space-between', zIndex: 20 },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  cityText: { fontSize: 13, fontWeight: '900', letterSpacing: 2, opacity: 0.8 },
  conditionText: { fontSize: 26, fontWeight: '800', marginTop: 2 },
  iconContainer: { width: 52, height: 52, justifyContent: 'center', alignItems: 'center', borderRadius: 26, backgroundColor: 'rgba(255,255,255,0.12)', borderWidth: 0.5, borderColor: 'rgba(255,255,255,0.2)' },
  weatherEmoji: { fontSize: 30 },
  mainInfo: { alignItems: 'center' },
  tempText: { fontSize: 110, fontWeight: '100', letterSpacing: -5, lineHeight: 110 },
  feelsLikeText: { fontSize: 16, fontWeight: '700', opacity: 0.9 },
  glassBar: { height: 68, backgroundColor: 'rgba(255, 255, 255, 0.1)', borderRadius: 24, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 20, borderWidth: 1.5, borderColor: 'rgba(255,255,255,0.2)' },
  infoItem: { flex: 1, alignItems: 'center' },
  infoLabel: { fontSize: 11, fontWeight: '900', letterSpacing: 1.5, opacity: 0.8 },
  infoValue: { fontSize: 18, fontWeight: '900', marginTop: 2 },
  infoDivider: { width: 1, height: 28, backgroundColor: 'rgba(255,255,255,0.15)' },
  celestialBody: { position: 'absolute', width: 60, height: 60, justifyContent: 'center', alignItems: 'center' },
  celestialEmoji: { fontSize: 48 },
  celestialGlow: { position: 'absolute', width: 80, height: 80, borderRadius: 40 },
  star: { position: 'absolute', backgroundColor: '#FFF', borderRadius: 5 },
});
