import React from 'react';
import {
  ActivityIndicator,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {useQueries, useQuery} from '@tanstack/react-query';
import {useTranslation} from 'react-i18next';

import {getCurrentWeather, getDailyWeather} from '../services/weatherApi';
import {getMoonPhase, formatSunTime} from '../services/openMeteoApi';
import {AppColors, FontSize, Radius, Spacing, getWeatherEmoji, useColors} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';
import {getProfile} from '../services/profileApi';
import {MainStackParamList} from '../navigation/types';
import {WeatherDetailsPanel} from '../components/WeatherDetailsPanel';

type Props = NativeStackScreenProps<MainStackParamList, 'WeatherDetail'>;

export function WeatherDetailScreen({route, navigation}: Props): React.JSX.Element {
  const {t} = useTranslation();
  const {theme} = useThemeStore();
  const C = useColors();
  const {lat, lon} = route.params;

  const {isGuest} = useAuthStore();
  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: !isGuest,
  });
  const tempUnit = profileQuery.data?.temperature_unit ?? 'C';

  const [currentQuery, dailyQuery] = useQueries({
    queries: [
      {queryKey: ['weather', 'current', lat, lon, tempUnit], queryFn: () => getCurrentWeather(lat, lon, tempUnit as any), staleTime: 5 * 60 * 1000},
      {queryKey: ['weather', 'daily', lat, lon, 1, tempUnit], queryFn: () => getDailyWeather(lat, lon, 1, tempUnit as any), staleTime: 5 * 60 * 1000},
    ],
  });

  const s = makeStyles(C);
  const current = currentQuery.data;
  const today = dailyQuery.data?.items[0];
  const moon = getMoonPhase();

  if (currentQuery.isLoading) {
    return (
      <SafeAreaView style={s.safe}>
        <View style={s.header}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
            <Text style={s.backArrow}>‹</Text>
          </TouchableOpacity>
          <Text style={s.headerTitle}>{t('detail.todayForecast')}</Text>
          <View style={{width: 36}} />
        </View>
        <View style={s.center}>
          <ActivityIndicator size="large" color={C.accent} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle={theme === 'light' ? 'dark-content' : 'light-content'} backgroundColor={C.bg} />

      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>{t('detail.todayForecast')}</Text>
        <View style={{width: 36}} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>

        {/* Ana sıcaklık özeti */}
        {current && (
          <View style={s.heroCard}>
            <Text style={s.heroEmoji}>{getWeatherEmoji(current.weather_code)}</Text>
            <View style={{flex: 1}}>
              <Text style={s.heroTemp}>{current.temperature}°{tempUnit}</Text>
              <Text style={s.heroDesc}>{current.description}</Text>
              {today && (
                <Text style={s.heroRange}>
                  {t('home.feelsLike')} {current.feels_like}° · {t('home.high')}:{today.temp_max}° {t('home.low')}:{today.temp_min}°
                </Text>
              )}
            </View>
          </View>
        )}

        {current && (
          <WeatherDetailsPanel
            current={current}
            todayDaily={today}
            C={C}
          />
        )}

        {/* Ay Fazı (HavaDetaylarıPanel'de yok, burada tutalım) */}
        <SectionTitle title={t('home.moonPhase').toUpperCase()} C={C} />
        <View style={s.sunCard}>
          <SunRow icon={moon.emoji} label={`${moon.label}`} time={`%${moon.illumination} aydınlık`} C={C} />
        </View>

        <View style={{height: Spacing.xxl}} />
      </ScrollView>
    </SafeAreaView>
  );
}

        <View style={{height: Spacing.xxl}} />
      </ScrollView>
    </SafeAreaView>
  );
}

// ── Alt Bileşenler ────────────────────────────────────────────────────────────

function SectionTitle({title, C}: {title: string; C: AppColors}) {
  return (
    <Text style={{
      fontSize: FontSize.xs,
      fontWeight: '800',
      color: C.textSecondary,
      letterSpacing: 1.2,
      marginHorizontal: Spacing.lg,
      marginTop: Spacing.lg,
      marginBottom: Spacing.sm,
    }}>
      {title}
    </Text>
  );
}

function MetricTile({
  icon, label, value, sub, barValue, barColor, C,
}: {
  icon: string;
  label: string;
  value: string;
  sub?: string;
  barValue?: number;
  barColor?: string;
  C: AppColors;
}) {
  return (
    <View style={[tileStyles(C).tile]}>
      <View style={tileStyles(C).tileHeader}>
        <Text style={tileStyles(C).tileIcon}>{icon}</Text>
        <Text style={tileStyles(C).tileLabel}>{label}</Text>
      </View>
      <Text style={tileStyles(C).tileValue}>{value}</Text>
      {sub && <Text style={tileStyles(C).tileSub}>{sub}</Text>}
      {barValue !== undefined && barColor && (
        <View style={tileStyles(C).barTrack}>
          <View style={[tileStyles(C).barFill, {width: `${Math.min(100, barValue * 100)}%`, backgroundColor: barColor}]} />
        </View>
      )}
    </View>
  );
}

const tileStyles = (C: AppColors) =>
  StyleSheet.create({
    tile: {width: '48%', backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border},
    tileHeader: {flexDirection: 'row', alignItems: 'center', gap: 4, marginBottom: Spacing.xs},
    tileIcon: {fontSize: 13},
    tileLabel: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '700', letterSpacing: 0.5},
    tileValue: {fontSize: FontSize.xxl, fontWeight: '800', color: C.text},
    tileSub: {fontSize: FontSize.xs, color: C.textSecondary, marginTop: 2},
    barTrack: {height: 4, backgroundColor: C.bgInput, borderRadius: 2, marginTop: Spacing.sm, overflow: 'hidden'},
    barFill: {height: '100%', borderRadius: 2},
  });

function SunRow({icon, label, time, C}: {icon: string; label: string; time: string; C: AppColors}) {
  return (
    <View style={{flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: Spacing.sm}}>
      <View style={{flexDirection: 'row', alignItems: 'center', gap: Spacing.sm}}>
        <Text style={{fontSize: 22}}>{icon}</Text>
        <Text style={{fontSize: FontSize.md, color: C.text, fontWeight: '500'}}>{label}</Text>
      </View>
      <Text style={{fontSize: FontSize.lg, fontWeight: '700', color: C.text}}>{time}</Text>
    </View>
  );
}

// ── Yardımcı fonksiyonlar ─────────────────────────────────────────────────────

function visibilityLabel(km: number): string {
  if (km >= 10) return 'Mükemmel görüş';
  if (km >= 5) return 'İyi görüş';
  if (km >= 2) return 'Orta görüş';
  return 'Kötü görüş';
}

function cloudLabel(pct: number): string {
  if (pct <= 10) return 'Açık';
  if (pct <= 30) return 'Az bulutlu';
  if (pct <= 70) return 'Parçalı bulutlu';
  return 'Çok bulutlu';
}

function uvLabel(uv: number): string {
  if (uv <= 2) return 'Düşük';
  if (uv <= 5) return 'Orta';
  if (uv <= 7) return 'Yüksek';
  if (uv <= 10) return 'Çok Yüksek';
  return 'Aşırı';
}

function uvColor(uv: number): string {
  if (uv <= 2) return '#4CAF50';
  if (uv <= 5) return '#FFC107';
  if (uv <= 7) return '#FF9800';
  if (uv <= 10) return '#F44336';
  return '#9C27B0';
}

function uvEmoji(uv: number): string {
  if (uv <= 2) return '😎';
  if (uv <= 5) return '🕶️';
  if (uv <= 7) return '⚠️';
  if (uv <= 10) return '🔆';
  return '🚨';
}

function uvAdvice(uv: number): string {
  if (uv <= 2) return 'Güneş kremi gerekmez. Dışarıda rahatça vakit geçirebilirsin.';
  if (uv <= 5) return 'Güneş kremi önerilir. Şapka ve gözlük faydalı olur.';
  if (uv <= 7) return 'SPF 30+ güneş kremi kullan. Öğle saatlerinde gölgede kal.';
  if (uv <= 10) return 'SPF 50+ zorunlu. 10:00-16:00 arası gölgede kal.';
  return 'Aşırı UV! Mümkünse dışarı çıkma. Tam koruma şart.';
}

function uvSegColor(i: number): string {
  if (i <= 2) return '#4CAF50';
  if (i <= 5) return '#FFC107';
  if (i <= 7) return '#FF9800';
  if (i <= 10) return '#F44336';
  return '#9C27B0';
}

// ── Stiller ───────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},
    center: {flex: 1, justifyContent: 'center', alignItems: 'center'},
    scroll: {paddingBottom: Spacing.xxl},

    header: {flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.md, paddingVertical: Spacing.md},
    backBtn: {width: 36},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerTitle: {flex: 1, fontSize: FontSize.xl, fontWeight: '700', color: C.text, textAlign: 'center'},

    heroCard: {flexDirection: 'row', alignItems: 'center', backgroundColor: C.bgCard, margin: Spacing.lg, borderRadius: Radius.xl, padding: Spacing.lg, gap: Spacing.lg, borderWidth: 1, borderColor: C.border},
    heroEmoji: {fontSize: 56},
    heroTemp: {fontSize: 40, fontWeight: '800', color: C.text},
    heroDesc: {fontSize: FontSize.md, color: C.textSecondary},
    heroRange: {fontSize: FontSize.sm, color: C.accent, marginTop: 2},

    grid: {flexDirection: 'row', flexWrap: 'wrap', paddingHorizontal: Spacing.lg, gap: Spacing.sm},

    sunCard: {backgroundColor: C.bgCard, marginHorizontal: Spacing.lg, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border},
    sunDivider: {height: 0.5, backgroundColor: C.divider},

    uvCard: {marginHorizontal: Spacing.lg, borderRadius: Radius.lg, padding: Spacing.lg, borderWidth: 1},
    uvMain: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: Spacing.md},
    uvValue: {fontSize: 48, fontWeight: '900'},
    uvLabel: {fontSize: FontSize.lg, fontWeight: '700'},
    uvBarTrack: {flexDirection: 'row', gap: 3, marginBottom: Spacing.md},
    uvBarSeg: {flex: 1, height: 8, borderRadius: 2},
    uvDesc: {fontSize: FontSize.sm, lineHeight: 20},
  });
