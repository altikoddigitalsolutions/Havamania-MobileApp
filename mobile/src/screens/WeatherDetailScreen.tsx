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

import {getCurrentWeather, getDailyWeather} from '../services/weatherApi';
import {getMoonPhase, formatSunTime} from '../services/openMeteoApi';
import {AppColors, DarkColors, FontSize, LightColors, Radius, Spacing, getWeatherEmoji} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';
import {getProfile} from '../services/profileApi';
import {MainStackParamList} from '../navigation/types';

type Props = NativeStackScreenProps<MainStackParamList, 'WeatherDetail'>;

export function WeatherDetailScreen({route, navigation}: Props): React.JSX.Element {
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;
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
          <Text style={s.headerTitle}>Hava Detayları</Text>
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
      <StatusBar barStyle={theme === 'dark' ? 'light-content' : 'dark-content'} backgroundColor={C.bg} />

      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Hava Detayları</Text>
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
                  Hissedilen {current.feels_like}° · H:{today.temp_max}° L:{today.temp_min}°
                </Text>
              )}
            </View>
          </View>
        )}

        {/* Bölüm 1: Temel metrikler */}
        <SectionTitle title="TEMEL BİLGİLER" C={C} />
        <View style={s.grid}>
          <MetricTile icon="🌡️" label="Hissedilen" value={`${current?.feels_like ?? '--'}°`} sub="gerçek hava" C={C} />
          <MetricTile icon="💧" label="Nem" value={`${current?.humidity ?? '--'}%`} sub={`Çiğ noktası ${current?.dew_point ?? '--'}°`} C={C} />
          <MetricTile icon="💨" label="Rüzgar" value={`${current?.wind_speed ?? '--'} km/h`} sub={`Hamle: ${current?.wind_gusts ?? '--'} km/h`} C={C} />
          <MetricTile icon="🌬️" label="Çiğ Noktası" value={`${current?.dew_point ?? '--'}°`} sub={current && current.dew_point > current.temperature - 3 ? 'Nemli hava' : 'Konforlu'} C={C} />
        </View>

        {/* Bölüm 2: Atmosfer */}
        <SectionTitle title="ATMOSFERİK KOŞULLAR" C={C} />
        <View style={s.grid}>
          <MetricTile icon="🌡️" label="Basınç" value={`${current?.pressure ?? '--'}`} sub="hPa" barValue={current ? (current.pressure - 950) / 100 : 0} barColor="#FF9800" C={C} />
          <MetricTile icon="👁️" label="Görünürlük" value={`${current?.visibility ?? '--'} km`} sub={visibilityLabel(current?.visibility ?? 0)} C={C} />
          <MetricTile icon="☁️" label="Bulut Örtüsü" value={`${current?.cloud_cover ?? '--'}%`} sub={cloudLabel(current?.cloud_cover ?? 0)} barValue={(current?.cloud_cover ?? 0) / 100} barColor={C.textSecondary} C={C} />
          <MetricTile icon="🌧️" label="Yağış" value={`${current?.precipitation ?? 0} mm`} sub="son 1 saat" C={C} />
        </View>

        {/* Bölüm 3: Güneş Işığı */}
        <SectionTitle title="GÜNEŞ & AY" C={C} />
        <View style={s.sunCard}>
          {today && (
            <>
              <SunRow icon="🌅" label="Güneş Doğuşu" time={formatSunTime(today.sunrise)} C={C} />
              <View style={s.sunDivider} />
              <SunRow icon="🌇" label="Güneş Batışı" time={formatSunTime(today.sunset)} C={C} />
              <View style={s.sunDivider} />
            </>
          )}
          <SunRow icon={moon.emoji} label={`Ay Fazı · ${moon.label}`} time={`%${moon.illumination} aydınlık`} C={C} />
        </View>

        {/* Bölüm 4: UV */}
        <SectionTitle title="UV İNDEKSİ" C={C} />
        <View style={[s.uvCard, {backgroundColor: C.bgCard, borderColor: C.border}]}>
          <View style={s.uvMain}>
            <View>
              <Text style={[s.uvValue, {color: uvColor(current?.uv_index ?? 0)}]}>{current?.uv_index ?? '--'}</Text>
              <Text style={[s.uvLabel, {color: C.text}]}>{uvLabel(current?.uv_index ?? 0)}</Text>
            </View>
            <Text style={{fontSize: 48}}>{uvEmoji(current?.uv_index ?? 0)}</Text>
          </View>
          <View style={s.uvBarTrack}>
            {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(i => (
              <View
                key={i}
                style={[
                  s.uvBarSeg,
                  {backgroundColor: uvSegColor(i)},
                  i < Math.round(current?.uv_index ?? 0) && {opacity: 1},
                  i >= Math.round(current?.uv_index ?? 0) && {opacity: 0.25},
                ]}
              />
            ))}
          </View>
          <Text style={[s.uvDesc, {color: C.textSecondary}]}>{uvAdvice(current?.uv_index ?? 0)}</Text>
        </View>

        {/* Bölüm 5: Günlük Bilgiler (bugün) */}
        {today && (
          <>
            <SectionTitle title="BUGÜNÜN TAHMİNİ" C={C} />
            <View style={s.grid}>
              <MetricTile icon="⬆️" label="Maks. Sıcaklık" value={`${today.temp_max}°`} C={C} />
              <MetricTile icon="⬇️" label="Min. Sıcaklık" value={`${today.temp_min}°`} C={C} />
              <MetricTile icon="💧" label="Yağış (Günlük)" value={`${today.precipitation_sum} mm`} sub={`%${today.precipitation_probability} olasılık`} C={C} />
              <MetricTile icon="🌬️" label="Maks. Rüzgar" value={`${today.wind_speed_max} km/h`} sub={`Hamle: ${today.wind_gusts_max} km/h`} C={C} />
            </View>
          </>
        )}

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
