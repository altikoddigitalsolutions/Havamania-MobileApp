import React, {useCallback, useState, useEffect} from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  RefreshControl,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useWindowDimensions,
  Platform,
} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import {useQueries, useQuery} from '@tanstack/react-query';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/Ionicons';

import {useTranslation} from 'react-i18next';

import {getCurrentWeather, getDailyWeather, getHourlyWeather} from '../services/weatherApi';
import {getProfile} from '../services/profileApi';
import {GeoResult, getMoonPhase, searchCity} from '../services/openMeteoApi';
import {useAuthStore} from '../store/authStore';
import { TURKEY_CITIES, City } from '../data/cities';
import { normalizeText } from '../utils/textUtils';
import {
  AppColors,
  FontSize,
  Radius,
  Spacing,
  formatHour,
  formatDayShort,
  getWeatherEmoji,
  getWeatherLabel,
  useColors,
} from '../theme';
import { formatPrecipitationProbability } from '../utils/weatherUtils';
import {useThemeStore} from '../store/themeStore';
import {WeatherAnimBox} from '../components/WeatherAnimBox';
import {AtmosphericWeatherCard} from '../components/AtmosphericWeatherCard';
import {WeatherDetailsPanel} from '../components/WeatherDetailsPanel';
import {HavamaniaRecommendationCard} from '../components/HavamaniaRecommendationCard';

// ── Varsayılan konum (Balıkesir Odaklı) ──────────────────────────────────────────
const DEFAULT_LAT = 39.6484;
const DEFAULT_LON = 27.8826;
const DEFAULT_CITY = 'Balıkesir, TR';

export function HomeScreen(): React.JSX.Element {
  const {t} = useTranslation();
  const navigation = useNavigation<any>();
  const {theme, animationsEnabled} = useThemeStore();
  const {width: screenWidth} = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const C = useColors();
  const {isGuest} = useAuthStore();

  const [refreshing, setRefreshing] = useState(false);
  const [lat, setLat] = useState(DEFAULT_LAT);
  const [lon, setLon] = useState(DEFAULT_LON);
  const [city, setCity] = useState(DEFAULT_CITY);
  const [showLocationModal, setShowLocationModal] = useState(false);
  const [locationSearch, setLocationSearch] = useState('');
  const [locationResults, setLocationResults] = useState<any[]>([]);
  const [searchingLocation, setSearchingLocation] = useState(false);

  // Otomatik Arama (API Destekli ve Türkiye Odaklı)
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (locationSearch.length >= 2) {
        setSearchingLocation(true);
        try {
          const results = await searchCity(locationSearch);
          // @ts-ignore
          setLocationResults(results);
        } catch (e) {
          console.error(e);
        } finally {
          setSearchingLocation(false);
        }
      } else {
        setLocationResults([]);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [locationSearch]);

  const [selectedWeather, setSelectedWeather] = useState<any>(null);

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: !isGuest,
  });
  const tempUnit = profileQuery.data?.temperature_unit ?? 'C';

  const [currentQuery, hourlyQuery, dailyQuery] = useQueries({
    queries: [
      {
        queryKey: ['weather', 'current', lat, lon, tempUnit],
        queryFn: () => getCurrentWeather(lat, lon, tempUnit as any),
        staleTime: 5 * 60 * 1000,
      },
      {
        queryKey: ['weather', 'hourly', lat, lon, tempUnit],
        queryFn: () => getHourlyWeather(lat, lon, 12, tempUnit as any),
        staleTime: 5 * 60 * 1000,
      },
      {
        queryKey: ['weather', 'daily', lat, lon, tempUnit],
        queryFn: () => getDailyWeather(lat, lon, 10, tempUnit as any),
        staleTime: 5 * 60 * 1000,
      },
    ],
  });

  // Sync selectedWeather with current data on first load or refresh
  useEffect(() => {
    if (currentQuery.data) {
      setSelectedWeather({
        temperature: currentQuery.data.temperature,
        weather_code: currentQuery.data.weather_code,
        feels_like: currentQuery.data.feels_like,
        time: new Date().toISOString(),
        description: getWeatherLabel(currentQuery.data.weather_code),
        is_day: currentQuery.data.is_day
      });
    }
  }, [currentQuery.data]);

  const isLoading =
    currentQuery.isLoading || hourlyQuery.isLoading || dailyQuery.isLoading;

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([
      currentQuery.refetch(),
      hourlyQuery.refetch(),
      dailyQuery.refetch(),
    ]);
    setRefreshing(false);
  }, [currentQuery, hourlyQuery, dailyQuery]);

  const selectLocation = (r: City) => {
    setLat(r.lat);
    setLon(r.lon);
    setCity(`${r.name}, TR`);
    setLocationSearch('');
    setLocationResults([]);
    setShowLocationModal(false);
  };

  const s = makeStyles(C, insets);

  if (isLoading) {
    return (
      <SafeAreaView style={s.safe}>
        <View style={s.center}>
          <ActivityIndicator size="large" color={C.accent} />
          <Text style={s.loadingText}>{t('home.loading')}</Text>
        </View>
      </SafeAreaView>
    );
  }

  // Hata Durumu (API Cevap Vermiyor veya Bağlantı Yok)
  if (currentQuery.isError || !currentQuery.data) {
    return (
      <SafeAreaView style={s.safe}>
        <View style={s.center}>
          <Text style={{fontSize: 40, marginBottom: 16}}>⚠️</Text>
          <Text style={s.errorText}>Hava verisi şu an alınamıyor.</Text>
          <Text style={s.loadingText}>Lütfen internet bağlantını kontrol et.</Text>
          <TouchableOpacity style={s.retryBtn} onPress={onRefresh}>
            <Text style={s.retryText}>Tekrar Dene</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const current = currentQuery.data;
  const hourlyItems = hourlyQuery.data?.items ?? [];
  const dailyItems = dailyQuery.data?.items ?? [];
  const todayDaily = dailyItems[0];

  const cardWidth = screenWidth - Spacing.md * 2;

  return (
    <View style={s.container}>
      <StatusBar
        barStyle={theme === 'light' ? 'dark-content' : 'light-content'}
        backgroundColor={C.bg}
        translucent
      />

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={s.scrollContent}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={C.accent} progressViewOffset={insets.top + 20} />
        }>

        {/* ── Header ── */}
        <View style={s.header}>
          <TouchableOpacity
            style={[s.locationBtn, {backgroundColor: C.bgCard}]}
            activeOpacity={0.8}
            onPress={() => setShowLocationModal(true)}>
            <Text style={{fontSize: 16}}>📍</Text>
            <Text style={s.locationText} numberOfLines={1}>{city}</Text>
            <Text style={{fontSize: 12, color: C.textSecondary}}> ▼</Text>
          </TouchableOpacity>

          <View style={s.headerRight}>
            <TouchableOpacity
              style={s.iconBtn}
              onPress={() => navigation.navigate('TravelCalendar')}>
              <Text style={s.iconBtnText}>✈️</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={s.iconBtn}
              onPress={() => navigation.navigate('Alerts')}>
              <Text style={s.iconBtnText}>⚡</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={s.iconBtn}
              onPress={() => navigation.navigate('Map')}>
              <Text style={s.iconBtnText}>🛰️</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* ── Ana Hava Durumu Kartı ── */}
        <AtmosphericWeatherCard
          city={city.split(',')[0]}
          temperature={selectedWeather?.temperature ?? current.temperature}
          description={selectedWeather?.description ?? getWeatherLabel(selectedWeather?.weather_code ?? current.weather_code)}
          high={todayDaily?.temp_max ?? current.temperature}
          low={todayDaily?.temp_min ?? current.temperature}
          feelsLike={selectedWeather?.feels_like ?? selectedWeather?.apparent_temperature ?? current.feels_like}
          weatherCode={selectedWeather?.weather_code ?? current.weather_code}
          isDay={selectedWeather?.is_day ?? current.is_day}
          humidity={selectedWeather?.humidity ?? current.humidity}
          windSpeed={selectedWeather?.wind_speed ?? current.wind_speed}
          uvIndex={selectedWeather?.uv_index ?? current.uv_index}
          time={selectedWeather?.time}
          lastUpdated={new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}
          C={C}
        />

        {/* ── 24 Saatlik Tahmin (Saatlik Tahmin) ── */}
        <View style={s.sectionCard}>
          <TouchableOpacity
            style={s.sectionHeader}
            onPress={() => navigation.navigate('Hourly', {lat, lon, city})}>
            <Text style={s.sectionTitle}>{t('home.hourlyTitle').toUpperCase()}</Text>
            <Text style={[s.sectionIcon, {color: C.accent}]}>{t('home.seeAll')} ›</Text>
          </TouchableOpacity>
          <FlatList
            horizontal
            data={hourlyItems.slice(0, 8)}
            keyExtractor={(item, idx) => `${item.time}-${idx}`}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{gap: Spacing.sm, paddingVertical: Spacing.xs}}
            renderItem={({item, index}) => (
              <TouchableOpacity
                activeOpacity={0.7}
                onPress={() => setSelectedWeather(item)}
                style={[s.hourCard, selectedWeather?.time === item.time && s.hourCardActive]}>
                <Text style={s.hourLabel}>{index === 0 ? t('home.today') : formatHour(item.time)}</Text>
                <Text style={s.hourEmoji}>{getWeatherEmoji(item.weather_code)}</Text>
                <Text style={s.hourTemp}>{item.temperature}°</Text>
                {item.precipitation_probability > 20 && (
                  <Text style={s.hourPrecip}>💧{formatPrecipitationProbability(item.precipitation_probability)}</Text>
                )}
              </TouchableOpacity>
            )}
          />
        </View>

        {/* ── Havamania Önerisi (AI Recommendation) ── */}
        <HavamaniaRecommendationCard
          weather={current}
          onPress={(query) => navigation.navigate('AIChat', {initialQuery: query})}
          C={C}
        />

        {/* ── 10 Günlük Özet (7/10 Günlük Tahmin) ── */}
        <View style={s.sectionCard}>
          <View style={s.sectionHeader}>
            <View style={{flexDirection: 'row', alignItems: 'center', gap: 6}}>
              <Text style={s.sectionIcon}>📅</Text>
              <Text style={s.sectionTitle}>{t('home.forecastTitle').toUpperCase()}</Text>
            </View>
          </View>
          {dailyItems.slice(0, 4).map((item, idx) => {
            const itemDate = item.date;
            const selectedDate = selectedWeather?.time?.split('T')[0] || selectedWeather?.date;
            const isSelected = itemDate === selectedDate;

            return (
              <DailyRow
                key={item.date}
                item={item}
                isFirst={idx === 0}
                isSelected={isSelected}
                onPress={() => setSelectedWeather({
                  ...item,
                  time: item.date + 'T12:00', // Günlük seçimde öğle vaktini baz al
                  temperature: item.temp_max,
                  description: getWeatherLabel(item.weather_code),
                  weather_code: item.weather_code,
                  feels_like: item.temp_max,
                  is_day: true
                })}
                C={C}
                t={t}
              />
            );
          })}
          <TouchableOpacity
            style={s.viewMoreBtn}
            onPress={() => navigation.navigate('Forecast', {lat, lon, city})}>
            <Text style={s.viewMoreText}>{t('forecast.title')} ›</Text>
          </TouchableOpacity>
        </View>

        {/* ── Premium Hava Detayları ── */}
        <WeatherDetailsPanel
          current={current}
          todayDaily={todayDaily}
          C={C}
        />

        <View style={{height: 100 + insets.bottom}} />
      </ScrollView>

      {/* ── Yeni Konum Modal (Premium) ── */}
      <Modal visible={showLocationModal} animationType="slide" transparent onRequestClose={() => setShowLocationModal(false)}>
        <TouchableOpacity style={s.modalOverlay} activeOpacity={1} onPress={() => setShowLocationModal(false)}>
          <View style={[s.locationModal, {backgroundColor: C.bgCard, borderTopColor: C.border}]}>
            <View style={s.locationModalHandle} />
            <Text style={[s.locationModalTitle, {color: C.text}]}>📍 {t('map.yourLocation')}</Text>

            <View style={[s.locationSearchBar, {backgroundColor: C.bgInput, borderColor: C.border}]}>
              <Icon name="search" size={20} color={C.textMuted} />
              <TextInput
                style={[s.locationSearchInput, {color: C.text}]}
                placeholder={t('forecast.searchPlaceholder')}
                placeholderTextColor={C.textMuted}
                value={locationSearch}
                onChangeText={setLocationSearch}
                autoFocus
              />
              {searchingLocation && <ActivityIndicator size="small" color={C.accent} />}
            </View>

            {locationResults.length === 0 && !searchingLocation && locationSearch.length > 0 && (
              <Text style={{color: C.textMuted, textAlign: 'center', marginTop: 20}}>Sonuç bulunamadı.</Text>
            )}

            <FlatList
              data={locationResults}
              keyExtractor={(item, i) => `${item.latitude}-${item.longitude}-${i}`}
              showsVerticalScrollIndicator={false}
              renderItem={({item}) => (
                <TouchableOpacity
                  style={[s.locationResultItem, {borderBottomColor: C.divider}]}
                  activeOpacity={0.7}
                  onPress={() => selectLocation({
                    name: item.name,
                    lat: item.latitude,
                    lon: item.longitude
                  } as any)}>
                  <View style={s.resultIconBox}>
                    <Icon name="location-outline" size={20} color={C.accent} />
                  </View>
                  <View style={{flex: 1}}>
                    <Text style={[s.locationResultName, {color: C.text}]}>{item.name}</Text>
                    <Text style={[s.locationResultSub, {color: C.textSecondary}]}>
                        {item.admin1 ? `${item.admin1}, ` : ''}{item.country}
                    </Text>
                  </View>
                  <Icon name="chevron-forward" size={16} color={C.textMuted} />
                </TouchableOpacity>
              )}
            />
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

function DailyRow({item, isFirst, isSelected, onPress, C, t}: {item: any; isFirst: boolean; isSelected: boolean; onPress: () => void; C: AppColors; t: any}) {
  const barPct = Math.min(100, Math.max(15, ((item.temp_max - item.temp_min) / 15) * 80));
  return (
    <TouchableOpacity
      activeOpacity={0.8}
      onPress={onPress}
      style={[
        dailyStyles(C).row,
        isSelected && {
          backgroundColor: C.cardHourlyActive,
          borderColor: C.accent,
          borderWidth: 1,
          borderRadius: Radius.md,
          marginVertical: 2,
          paddingHorizontal: 8
        }
      ]}>
      <Text style={dailyStyles(C).day}>{isFirst ? t('home.today') : formatDayShort(item.date)}</Text>
      <Text style={dailyStyles(C).emoji}>{getWeatherEmoji(item.weather_code)}</Text>
      <Text style={dailyStyles(C).tempMin}>{item.temp_min}°</Text>
      <View style={dailyStyles(C).barTrack}><View style={[dailyStyles(C).barFill, {width: `${barPct}%`}]} /></View>
      <Text style={dailyStyles(C).tempMax}>{item.temp_max}°</Text>
    </TouchableOpacity>
  );
}

const dailyStyles = (C: AppColors) => StyleSheet.create({
  row: {flexDirection: 'row', alignItems: 'center', paddingVertical: Spacing.sm, gap: Spacing.xs, borderBottomWidth: 0.5, borderBottomColor: C.divider},
  day: {fontSize: FontSize.md, color: C.text, fontWeight: '600', width: 52},
  emoji: {fontSize: 20, width: 28, textAlign: 'center'},
  tempMin: {fontSize: FontSize.sm, color: C.textSecondary, width: 26, textAlign: 'right'},
  barTrack: {flex: 1, height: 4, backgroundColor: C.bgInput, borderRadius: 2, overflow: 'hidden'},
  barFill: {height: '100%', backgroundColor: C.accent, borderRadius: 2},
  tempMax: {fontSize: FontSize.sm, fontWeight: '700', color: C.text, width: 26},
});

function UvBar({value, C}: {value: number; C: AppColors}) {
  const pct = Math.min(100, (value / 11) * 100);
  const color = value <= 2 ? '#4CAF50' : value <= 5 ? '#FFC107' : value <= 7 ? '#FF9800' : '#F44336';
  return (
    <View style={{height: 4, backgroundColor: C.bgInput, borderRadius: 2, marginTop: 8, overflow: 'hidden'}}>
      <View style={{width: `${pct}%`, height: '100%', backgroundColor: color, borderRadius: 2}} />
    </View>
  );
}

const makeStyles = (C: AppColors, insets: any) => StyleSheet.create({
  container: {flex: 1, backgroundColor: C.bg},
  safe: {flex: 1},
  scrollContent: {
    paddingTop: Platform.OS === 'ios' ? 0 : insets.top,
  },
  center: {flex: 1, justifyContent: 'center', alignItems: 'center', gap: 12},
  loadingText: {color: C.textSecondary, fontSize: FontSize.md, marginTop: 8},
  errorText: {color: C.text, fontSize: FontSize.lg, fontWeight: '700'},
  retryBtn: {backgroundColor: C.accent, paddingHorizontal: Spacing.lg, paddingVertical: Spacing.sm, borderRadius: Radius.full, marginTop: Spacing.sm},
  retryText: {color: '#FFFFFF', fontWeight: '700'},
  header: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: Spacing.lg, paddingTop: Platform.OS === 'ios' ? Spacing.md : Spacing.md + insets.top, paddingBottom: Spacing.sm, zIndex: 10},
  locationBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: Radius.full,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    flex: 1,
    marginRight: 12,
  },
  locationText: {fontSize: FontSize.md, fontWeight: '700', color: C.text, flex: 1},
  headerRight: {flexDirection: 'row', gap: Spacing.xs},
  iconBtn: {width: 40, height: 40, borderRadius: 20, backgroundColor: C.bgCard, justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderColor: C.border},
  iconBtnText: {fontSize: 18},
  weatherIllustration: {alignItems: 'center', marginBottom: Spacing.xs, zIndex: 5},
  weatherBigEmoji: {fontSize: 80},
  tempBlockCard: {alignItems: 'center', zIndex: 5},
  tempText: {fontSize: 72, fontWeight: '800', color: C.text, lineHeight: 76},
  descText: {fontSize: FontSize.xl, color: C.text, fontWeight: '500', marginBottom: 2},
  hiLoText: {fontSize: FontSize.md, color: C.accent, fontWeight: '600'},
  feelsLike: {fontSize: FontSize.sm, color: C.textSecondary, marginTop: 4},
  sectionCard: {marginHorizontal: Spacing.md, marginBottom: Spacing.md, backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border, overflow: 'hidden'},
  sectionHeader: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: Spacing.sm},
  sectionTitle: {fontSize: FontSize.xs, fontWeight: '700', color: C.textSecondary, letterSpacing: 1},
  sectionIcon: {fontSize: 13, color: C.textMuted},
  hourCard: {alignItems: 'center', backgroundColor: C.bgSecondary, borderRadius: Radius.md, paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, minWidth: 72, gap: 2, borderWidth: 1, borderColor: C.border},
  hourCardActive: {backgroundColor: C.cardHourlyActive, borderColor: C.accent},
  hourLabel: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '600'},
  hourEmoji: {fontSize: 22},
  hourTemp: {fontSize: FontSize.md, fontWeight: '700', color: C.text},
  hourPrecip: {fontSize: 12, fontWeight: '800', color: C.accent},
  viewMoreBtn: {alignItems: 'center', paddingTop: Spacing.md},
  viewMoreText: {fontSize: FontSize.sm, color: C.accent, fontWeight: '700'},
  metricsRow: {flexDirection: 'row', marginHorizontal: Spacing.md, marginBottom: Spacing.md, gap: Spacing.sm},
  metricCard: {backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border},
  metricHeader: {flexDirection: 'row', alignItems: 'center', marginBottom: Spacing.xs},
  metricIcon: {fontSize: 13},
  metricLabel: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '700', letterSpacing: 0.8},
  metricValue: {fontSize: FontSize.xxl, fontWeight: '800', color: C.text},
  modalOverlay: {flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end'},
  locationModal: {borderTopLeftRadius: 28, borderTopRightRadius: 28, paddingHorizontal: Spacing.lg, paddingTop: Spacing.md, paddingBottom: 40, maxHeight: '85%', borderWidth: 1, borderBottomWidth: 0},
  locationModalHandle: {width: 40, height: 4, backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 2, alignSelf: 'center', marginBottom: Spacing.md},
  locationModalTitle: {fontSize: FontSize.xl, fontWeight: '800', marginBottom: Spacing.md, textAlign: 'center'},
  locationSearchBar: {flexDirection: 'row', alignItems: 'center', borderRadius: Radius.xl, paddingHorizontal: Spacing.md, paddingVertical: 12, gap: Spacing.sm, borderWidth: 1, marginBottom: Spacing.md},
  locationSearchInput: {flex: 1, fontSize: FontSize.md, padding: 0},
  locationResultItem: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md, paddingVertical: 16, borderBottomWidth: 0.5},
  resultIconBox: {width: 40, height: 40, borderRadius: 12, backgroundColor: 'rgba(59, 130, 246, 0.1)', justifyContent: 'center', alignItems: 'center'},
  locationResultName: {fontSize: FontSize.md, fontWeight: '700'},
  locationResultSub: {fontSize: 12, marginTop: 2},
});
