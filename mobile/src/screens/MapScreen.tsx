import React, {useState} from 'react';
import {
  ActivityIndicator,
  Alert,
  PermissionsAndroid,
  Platform,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import {useQueries, useQuery} from '@tanstack/react-query';
import Geolocation from 'react-native-geolocation-service';

import {getCurrentWeather, getDailyWeather, getHourlyWeather} from '../services/weatherApi';
import {AppColors, DarkColors, FontSize, LightColors, Radius, Spacing, getWeatherEmoji, getWeatherLabel, formatHour} from '../theme';
import {useThemeStore} from '../store/themeStore';

// ── Bölgeler (Mercury Weather gibi çoklu bölge karşılaştırması) ───────────────
const PRESET_LOCATIONS = [
  {name: 'İstanbul', country: 'TR', lat: 41.0082, lon: 28.9784},
  {name: 'Ankara', country: 'TR', lat: 39.9208, lon: 32.8541},
  {name: 'İzmir', country: 'TR', lat: 38.4237, lon: 27.1428},
  {name: 'Antalya', country: 'TR', lat: 36.8969, lon: 30.7133},
  {name: 'Bursa', country: 'TR', lat: 40.1885, lon: 29.0610},
];

type Layer = 'temperature' | 'precipitation' | 'wind' | 'cloud' | 'humidity';

const LAYERS: {key: Layer; label: string; icon: string}[] = [
  {key: 'temperature', label: 'Sıcaklık', icon: '🌡️'},
  {key: 'precipitation', label: 'Yağış', icon: '🌧️'},
  {key: 'wind', label: 'Rüzgar', icon: '💨'},
  {key: 'cloud', label: 'Bulut', icon: '☁️'},
  {key: 'humidity', label: 'Nem', icon: '💧'},
];

export function MapScreen(): React.JSX.Element {
  const navigation = useNavigation<any>();
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;
  const [activeLayer, setActiveLayer] = useState<Layer>('temperature');
  const [userLat, setUserLat] = useState<number | null>(null);
  const [userLon, setUserLon] = useState<number | null>(null);
  const [locating, setLocating] = useState(false);

  // Tüm preset konumlar için paralel sorgu
  const weatherQueries = useQueries({
    queries: PRESET_LOCATIONS.map(loc => ({
      queryKey: ['weather', 'current', loc.lat, loc.lon],
      queryFn: () => getCurrentWeather(loc.lat, loc.lon),
      staleTime: 10 * 60 * 1000,
    })),
  });

  // Kullanıcı konumu için sorgu
  const userWeatherQuery = useQuery({
    queryKey: ['weather', 'current', userLat, userLon],
    queryFn: () => getCurrentWeather(userLat!, userLon!),
    staleTime: 5 * 60 * 1000,
    enabled: userLat !== null,
  });

  const requestLocation = async () => {
    setLocating(true);
    try {
      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        );
        if (granted !== PermissionsAndroid.RESULTS.GRANTED) {
          Alert.alert('İzin Gerekli', 'Konumunuzu görmek için konum iznine ihtiyaç var.');
          setLocating(false);
          return;
        }
      }
      Geolocation.getCurrentPosition(
        pos => {
          setUserLat(pos.coords.latitude);
          setUserLon(pos.coords.longitude);
          setLocating(false);
        },
        () => {
          Alert.alert('Hata', 'Konum alınamadı.');
          setLocating(false);
        },
        {enableHighAccuracy: true, timeout: 10000, maximumAge: 5000},
      );
    } catch {
      setLocating(false);
    }
  };

  const getValue = (data: any, layer: Layer): string => {
    if (!data) return '--';
    switch (layer) {
      case 'temperature': return `${data.temperature}°`;
      case 'precipitation': return `${data.precipitation ?? 0}mm`;
      case 'wind': return `${data.wind_speed}km/h`;
      case 'cloud': return `${data.cloud_cover}%`;
      case 'humidity': return `${data.humidity}%`;
    }
  };

  const getBarValue = (data: any, layer: Layer): number => {
    if (!data) return 0;
    switch (layer) {
      case 'temperature': return Math.min(1, (data.temperature + 10) / 50);
      case 'precipitation': return Math.min(1, (data.precipitation ?? 0) / 10);
      case 'wind': return Math.min(1, data.wind_speed / 100);
      case 'cloud': return data.cloud_cover / 100;
      case 'humidity': return data.humidity / 100;
    }
  };

  const getBarColor = (layer: Layer): string => {
    switch (layer) {
      case 'temperature': return '#FF9800';
      case 'precipitation': return '#29B5F6';
      case 'wind': return '#9E9E9E';
      case 'cloud': return '#B0BEC5';
      case 'humidity': return '#26C6DA';
    }
  };

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle={theme === 'dark' ? 'light-content' : 'dark-content'} backgroundColor={C.bg} />

      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Hava Haritası</Text>
        <TouchableOpacity onPress={requestLocation} style={s.locateBtn} disabled={locating}>
          {locating ? (
            <ActivityIndicator size="small" color={C.accent} />
          ) : (
            <Text style={s.locateBtnText}>📍</Text>
          )}
        </TouchableOpacity>
      </View>

      {/* Katman seçici */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={s.layerScroll} contentContainerStyle={s.layerContainer}>
        {LAYERS.map(layer => (
          <TouchableOpacity
            key={layer.key}
            style={[s.layerChip, activeLayer === layer.key && s.layerChipActive]}
            onPress={() => setActiveLayer(layer.key)}>
            <Text style={s.layerChipIcon}>{layer.icon}</Text>
            <Text style={[s.layerChipText, activeLayer === layer.key && s.layerChipTextActive]}>
              {layer.label}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>

        {/* Kullanıcı konumu */}
        {userLat !== null && (
          <View style={s.section}>
            <Text style={s.sectionTitle}>📍 BULUNDUĞUN KONUM</Text>
            <LocationCard
              name="Mevcut Konum"
              subtitle={`${userLat.toFixed(2)}°, ${userLon!.toFixed(2)}°`}
              data={userWeatherQuery?.data}
              layer={activeLayer}
              getValue={getValue}
              getBarValue={getBarValue}
              getBarColor={getBarColor}
              C={C}
              isUser
            />
          </View>
        )}

        {/* Şehir karşılaştırması */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>🌍 ŞEHİR KARŞILAŞTIRMASI</Text>

          {/* Görsel harita grid'i */}
          <View style={s.mapGrid}>
            {PRESET_LOCATIONS.map((loc, idx) => {
              const data = weatherQueries[idx]?.data;
              const barVal = getBarValue(data, activeLayer);
              const color = getBarColor(activeLayer);
              return (
                <TouchableOpacity
                  key={loc.name}
                  style={[s.mapCell, {backgroundColor: C.bgCard, borderColor: C.border}]}
                  onPress={() => navigation.navigate('Forecast', {
                    lat: loc.lat,
                    lon: loc.lon,
                    city: `${loc.name}, ${loc.country}`,
                  })}>
                  <Text style={s.mapCellEmoji}>{data ? getWeatherEmoji(data.weather_code) : '⌛'}</Text>
                  <Text style={[s.mapCellCity, {color: C.text}]}>{loc.name}</Text>
                  <Text style={[s.mapCellValue, {color}]}>{getValue(data, activeLayer)}</Text>
                  <View style={[s.mapCellBar, {backgroundColor: C.bgInput}]}>
                    <View style={[s.mapCellBarFill, {width: `${barVal * 100}%`, backgroundColor: color}]} />
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        {/* Detaylı liste */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>📊 DETAYLI LİSTE</Text>
          {PRESET_LOCATIONS.map((loc, idx) => {
            const data = weatherQueries[idx]?.data;
            return (
              <LocationCard
                key={loc.name}
                name={loc.name}
                subtitle={loc.country}
                data={data}
                layer={activeLayer}
                getValue={getValue}
                getBarValue={getBarValue}
                getBarColor={getBarColor}
                C={C}
              />
            );
          })}
        </View>

        <View style={{height: Spacing.xxl}} />
      </ScrollView>
    </SafeAreaView>
  );
}

// ── LocationCard Bileşeni ─────────────────────────────────────────────────────
function LocationCard({
  name, subtitle, data, layer, getValue, getBarValue, getBarColor, C, isUser,
}: {
  name: string;
  subtitle: string;
  data: any;
  layer: Layer;
  getValue: (d: any, l: Layer) => string;
  getBarValue: (d: any, l: Layer) => number;
  getBarColor: (l: Layer) => string;
  C: AppColors;
  isUser?: boolean;
}) {
  const barColor = getBarColor(layer);
  const barVal = getBarValue(data, layer);

  return (
    <View style={[
      cardStyles(C).card,
      isUser && {borderColor: C.accent, borderWidth: 1.5},
    ]}>
      <View style={cardStyles(C).left}>
        <Text style={cardStyles(C).emoji}>{data ? getWeatherEmoji(data.weather_code) : '⌛'}</Text>
        <View>
          <Text style={cardStyles(C).name}>{name}</Text>
          <Text style={cardStyles(C).subtitle}>{data ? getWeatherLabel(data.weather_code) : '...'} · {subtitle}</Text>
        </View>
      </View>
      <View style={cardStyles(C).right}>
        <Text style={[cardStyles(C).value, {color: barColor}]}>{getValue(data, layer)}</Text>
        <View style={cardStyles(C).bar}>
          <View style={[cardStyles(C).barFill, {width: `${barVal * 100}%`, backgroundColor: barColor}]} />
        </View>
        {data && (
          <Text style={cardStyles(C).extra}>💧{data.humidity}% · {data.wind_speed}km/h</Text>
        )}
      </View>
    </View>
  );
}

const cardStyles = (C: AppColors) =>
  StyleSheet.create({
    card: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, marginBottom: Spacing.sm, borderWidth: 1, borderColor: C.border},
    left: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md, flex: 1},
    emoji: {fontSize: 28},
    name: {fontSize: FontSize.md, fontWeight: '700', color: C.text},
    subtitle: {fontSize: FontSize.xs, color: C.textSecondary},
    right: {alignItems: 'flex-end', minWidth: 80},
    value: {fontSize: FontSize.xl, fontWeight: '800'},
    bar: {width: 80, height: 4, backgroundColor: C.bgInput, borderRadius: 2, marginTop: 4, overflow: 'hidden'},
    barFill: {height: '100%', borderRadius: 2},
    extra: {fontSize: FontSize.xs, color: C.textMuted, marginTop: 2},
  });

// ── Stiller ───────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},
    scroll: {paddingBottom: Spacing.xxl},

    header: {flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.md, paddingVertical: Spacing.md, borderBottomWidth: 0.5, borderBottomColor: C.divider},
    backBtn: {width: 36},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerTitle: {flex: 1, fontSize: FontSize.xl, fontWeight: '700', color: C.text, textAlign: 'center'},
    locateBtn: {width: 36, height: 36, alignItems: 'center', justifyContent: 'center'},
    locateBtnText: {fontSize: 22},

    layerScroll: {maxHeight: 56},
    layerContainer: {paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, gap: Spacing.sm},
    layerChip: {flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, borderRadius: Radius.full, backgroundColor: C.bgCard, borderWidth: 1, borderColor: C.border},
    layerChipActive: {backgroundColor: C.accentBtn, borderColor: C.accentBtn},
    layerChipIcon: {fontSize: 14},
    layerChipText: {fontSize: FontSize.sm, color: C.textSecondary, fontWeight: '600'},
    layerChipTextActive: {color: '#FFFFFF'},

    section: {paddingHorizontal: Spacing.md, marginTop: Spacing.md},
    sectionTitle: {fontSize: FontSize.xs, fontWeight: '800', color: C.textSecondary, letterSpacing: 1, marginBottom: Spacing.sm},

    // Grid harita
    mapGrid: {flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.sm, marginBottom: Spacing.sm},
    mapCell: {width: '30%', borderRadius: Radius.md, padding: Spacing.sm, alignItems: 'center', gap: 4, borderWidth: 1},
    mapCellEmoji: {fontSize: 26},
    mapCellCity: {fontSize: FontSize.xs, fontWeight: '700'},
    mapCellValue: {fontSize: FontSize.md, fontWeight: '800'},
    mapCellBar: {width: '100%', height: 3, borderRadius: 2, overflow: 'hidden'},
    mapCellBarFill: {height: '100%', borderRadius: 2},
  });
