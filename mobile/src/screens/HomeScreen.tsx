import React, {useCallback, useState, useEffect} from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  RefreshControl,
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
import {useQueries} from '@tanstack/react-query';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/Ionicons';
import {useTranslation} from 'react-i18next';

import {getCurrentWeather, getDailyWeather, getHourlyWeather} from '../services/weatherApi';
import {searchCity} from '../services/openMeteoApi';
import {useAuthStore} from '../store/authStore';
import { City } from '../data/cities';
import {
  AppColors,
  FontSize,
  Radius,
  Spacing,
  formatHour,
  getWeatherEmoji,
  getWeatherLabel,
  useColors,
  useTheme,
} from '../theme';
import { formatPrecipitationProbability } from '../utils/weatherUtils';
import {useThemeStore} from '../store/themeStore';
import { ErrorView, HomeSkeleton } from '../components/StateViews';
import {AtmosphericWeatherCard} from '../components/AtmosphericWeatherCard';
import {WeatherDetailsPanel} from '../components/WeatherDetailsPanel';
import {HavamaniaRecommendationCard} from '../components/HavamaniaRecommendationCard';

const DEFAULT_LAT = 39.6484;
const DEFAULT_LON = 27.8826;
const DEFAULT_CITY = 'Balıkesir, TR';

export function HomeScreen(): React.JSX.Element {
  const {t} = useTranslation();
  const navigation = useNavigation<any>();
  const {theme} = useThemeStore();
  const insets = useSafeAreaInsets();
  const { colors: C, spacing, fontSize, responsive, layout } = useTheme();
  const {userProfile} = useAuthStore();

  const [refreshing, setRefreshing] = useState(false);
  const [lat, setLat] = useState(DEFAULT_LAT);
  const [lon, setLon] = useState(DEFAULT_LON);
  const [city, setCity] = useState(DEFAULT_CITY);
  const [showLocationModal, setShowLocationModal] = useState(false);
  const [locationSearch, setLocationSearch] = useState('');
  const [locationResults, setLocationResults] = useState<any[]>([]);
  const [searchingLocation, setSearchingLocation] = useState(false);

  useEffect(() => {
    const timer = setTimeout(async () => {
      if (locationSearch.length >= 2) {
        setSearchingLocation(true);
        try {
          const results = await searchCity(locationSearch);
          setLocationResults(results as any[]);
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
  const tempUnit = userProfile?.temperatureUnit ?? 'C';

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

  useEffect(() => {
    if (currentQuery.data) {
      setSelectedWeather({
        temperature: currentQuery.data.temperature,
        weather_code: currentQuery.data.weather_code,
        feels_like: currentQuery.data.feels_like,
        time: currentQuery.data.time,
        description: getWeatherLabel(currentQuery.data.weather_code),
        is_day: currentQuery.data.is_day,
        humidity: currentQuery.data.humidity,
        wind_speed: currentQuery.data.wind_speed,
        uv_index: currentQuery.data.uv_index
      });
    }
  }, [currentQuery.data]);

  const isLoading = currentQuery.isLoading || hourlyQuery.isLoading || dailyQuery.isLoading;

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

  const s = makeStyles(C, insets, spacing, fontSize, responsive, layout);

  if (isLoading) return <HomeSkeleton />;
  if (currentQuery.isError || !currentQuery.data) {
    return <ErrorView type={currentQuery.error ? 'api-fail' : 'no-internet'} onRetry={onRefresh} />;
  }

  const current = currentQuery.data;
  const hourlyItems = hourlyQuery.data?.items ?? [];
  const dailyItems = dailyQuery.data?.items ?? [];
  const todayDaily = dailyItems[0];

  return (
    <View style={s.container}>
      <StatusBar barStyle={theme === 'light' ? 'dark-content' : 'light-content'} backgroundColor={C.bg} translucent />
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={s.scrollContent}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={C.accent} progressViewOffset={insets.top + 20} />}
      >
        <View style={s.centeredLayout}>
          <View style={s.header}>
            <TouchableOpacity style={[s.locationBtn, {backgroundColor: C.bgCard}]} activeOpacity={0.8} onPress={() => setShowLocationModal(true)}>
              <Text style={{fontSize: 16}}>📍</Text>
              <Text style={s.locationText} numberOfLines={1}>{city}</Text>
              <Text style={{fontSize: 12, color: C.textSecondary}}> ▼</Text>
            </TouchableOpacity>
            <View style={s.headerRight}>
              <TouchableOpacity style={s.iconBtn} onPress={() => navigation.navigate('TravelCalendar')}><Text style={s.iconBtnText}>✈️</Text></TouchableOpacity>
              <TouchableOpacity style={s.iconBtn} onPress={() => navigation.navigate('Alerts')}><Text style={s.iconBtnText}>⚡</Text></TouchableOpacity>
              <TouchableOpacity style={s.iconBtn} onPress={() => navigation.navigate('Map')}><Text style={s.iconBtnText}>🛰️</Text></TouchableOpacity>
            </View>
          </View>

          <AtmosphericWeatherCard
            city={city.split(',')[0]}
            temperature={selectedWeather?.temperature ?? current.temperature}
            description={selectedWeather?.description ?? getWeatherLabel(current.weather_code)}
            high={todayDaily?.temp_max ?? current.temperature}
            low={todayDaily?.temp_min ?? current.temperature}
            feelsLike={selectedWeather?.feels_like ?? current.feels_like}
            weatherCode={selectedWeather?.weather_code ?? current.weather_code}
            isDay={selectedWeather?.is_day ?? current.is_day}
            humidity={selectedWeather?.humidity ?? current.humidity}
            windSpeed={selectedWeather?.wind_speed ?? current.wind_speed}
            uvIndex={selectedWeather?.uv_index ?? current.uv_index}
            time={selectedWeather?.time}
            sunrise={todayDaily?.sunrise}
            sunset={todayDaily?.sunset}
            lastUpdated={new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})}
            C={C}
          />

          <View style={s.sectionCard}>
            <TouchableOpacity style={s.sectionHeader} onPress={() => navigation.navigate('Hourly', {lat, lon, city})}>
              <Text style={s.sectionTitle}>{t('home.hourlyTitle').toUpperCase()}</Text>
              <Text style={[s.sectionIcon, {color: C.accent}]}>{t('home.seeAll')} ›</Text>
            </TouchableOpacity>
            <FlatList
              horizontal
              data={hourlyItems.slice(0, 8)}
              keyExtractor={(item, idx) => `${item.time}-${idx}`}
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={{gap: spacing.sm, paddingHorizontal: 2, paddingVertical: spacing.xs}}
              renderItem={({item, index}) => {
                const isActive = selectedWeather?.time === item.time;
                return (
                  <TouchableOpacity
                    activeOpacity={0.7}
                    onPress={() => setSelectedWeather(item)}
                    style={[s.hourCard, isActive ? s.hourCardActive : { backgroundColor: 'rgba(255,255,255,0.03)' }]}
                  >
                    <Text style={[s.hourLabel, isActive && { color: '#FFF' }]}>{index === 0 ? t('home.today') : formatHour(item.time)}</Text>
                    <Text style={s.hourEmoji}>{getWeatherEmoji(item.weather_code)}</Text>
                    <Text style={[s.hourTemp, isActive && { color: '#FFF' }]}>{item.temperature}°</Text>
                    {item.precipitation_probability > 20 && (
                      <Text style={[s.hourPrecip, isActive && { color: '#FFF' }]}>💧{formatPrecipitationProbability(item.precipitation_probability)}</Text>
                    )}
                  </TouchableOpacity>
                );
              }}
            />
          </View>

          <HavamaniaRecommendationCard
            weather={current}
            profile={userProfile}
            onPress={(query) => navigation.navigate('AIChat', {initialQuery: query})}
            C={C}
          />

          <View style={s.sectionCard}>
            <View style={s.sectionHeader}>
              <View style={{flexDirection: 'row', alignItems: 'center', gap: 6}}>
                <Text style={s.sectionIcon}>📅</Text>
                <Text style={s.sectionTitle}>{t('home.forecastTitle').toUpperCase()}</Text>
              </View>
            </View>
            {dailyItems.slice(0, 4).map((item, idx) => (
              <DailyRow
                key={item.date}
                item={item}
                isFirst={idx === 0}
                isSelected={selectedWeather?.date === item.date || selectedWeather?.time?.startsWith(item.date)}
                onPress={() => setSelectedWeather({...item, time: item.date + 'T12:00', temperature: item.temp_max, description: getWeatherLabel(item.weather_code), is_day: true})}
                C={C}
                t={t}
              />
            ))}
            <TouchableOpacity style={s.viewMoreBtn} onPress={() => navigation.navigate('Forecast', {lat, lon, city})}>
              <Text style={s.viewMoreText}>{t('forecast.title')} ›</Text>
            </TouchableOpacity>
          </View>

          <WeatherDetailsPanel current={current} todayDaily={todayDaily} C={C} />
        </View>
        <View style={{height: 100 + insets.bottom}} />
      </ScrollView>

      <Modal visible={showLocationModal} animationType="slide" transparent onRequestClose={() => setShowLocationModal(false)}>
        <TouchableOpacity style={s.modalOverlay} activeOpacity={1} onPress={() => setShowLocationModal(false)}>
          <View style={[s.locationModal, {backgroundColor: C.bgCard, borderTopColor: C.border}]}>
            <View style={s.locationModalHandle} />
            <Text style={[s.locationModalTitle, {color: C.text}]}>📍 {t('map.yourLocation')}</Text>
            <View style={[s.locationSearchBar, {backgroundColor: C.bgInput, borderColor: C.border}]}>
              <Icon name="search" size={20} color={C.textMuted} />
              <TextInput style={[s.locationSearchInput, {color: C.text}]} placeholder={t('forecast.searchPlaceholder')} placeholderTextColor={C.textMuted} value={locationSearch} onChangeText={setLocationSearch} autoFocus />
              {searchingLocation && <ActivityIndicator size="small" color={C.accent} />}
            </View>
            <FlatList
              data={locationResults}
              keyExtractor={(item, i) => `${item.latitude}-${item.longitude}-${i}`}
              renderItem={({item}) => (
                <TouchableOpacity style={[s.locationResultItem, {borderBottomColor: C.divider}]} activeOpacity={0.7} onPress={() => selectLocation({name: item.name, lat: item.latitude, lon: item.longitude} as any)}>
                  <View style={s.resultIconBox}><Icon name="location-outline" size={20} color={C.accent} /></View>
                  <View style={{flex: 1}}><Text style={[s.locationResultName, {color: C.text}]}>{item.name}</Text><Text style={[s.locationResultSub, {color: C.textSecondary}]}>{item.admin1 ? `${item.admin1}, ` : ''}{item.country}</Text></View>
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

function DailyRow({item, isFirst, isSelected, onPress, C, t}: any) {
  const barPct = Math.min(100, Math.max(15, ((item.temp_max - item.temp_min) / 15) * 80));
  const { getDayName, formatDayShort } = require('../theme');
  return (
    <TouchableOpacity activeOpacity={0.8} onPress={onPress} style={[dailyStyles(C).row, isSelected && {backgroundColor: C.cardHourlyActive, borderColor: C.accent, borderWidth: 1, borderRadius: Radius.md, marginVertical: 2, paddingHorizontal: 8}]}>
      <View style={{width: 52}}><Text style={dailyStyles(C).day}>{isFirst ? t('home.today') : getDayName(item.date)}</Text><Text style={dailyStyles(C).dateSub}>{formatDayShort(item.date)}</Text></View>
      <Text style={dailyStyles(C).emoji}>{getWeatherEmoji(item.weather_code)}</Text>
      <Text style={dailyStyles(C).tempMin}>{item.temp_min}°</Text>
      <View style={dailyStyles(C).barTrack}><View style={[dailyStyles(C).barFill, {width: `${barPct}%`}]} /></View>
      <Text style={dailyStyles(C).tempMax}>{item.temp_max}°</Text>
    </TouchableOpacity>
  );
}

const dailyStyles = (C: AppColors) => StyleSheet.create({
  row: {flexDirection: 'row', alignItems: 'center', paddingVertical: Spacing.sm, gap: Spacing.xs, borderBottomWidth: 0.5, borderBottomColor: C.divider},
  day: {fontSize: FontSize.md, color: C.text, fontWeight: '700', lineHeight: 18},
  dateSub: {fontSize: 10, color: C.textSecondary, fontWeight: '600'},
  emoji: {fontSize: 22, width: 32, textAlign: 'center'},
  tempMin: {fontSize: FontSize.sm, color: C.textSecondary, width: 30, textAlign: 'right'},
  barTrack: {flex: 1, height: 4, backgroundColor: C.bgInput, borderRadius: 2, overflow: 'hidden', marginHorizontal: 8},
  barFill: {height: '100%', backgroundColor: C.accent, borderRadius: 2},
  tempMax: {fontSize: FontSize.sm, fontWeight: '700', color: C.text, width: 30},
});

const makeStyles = (C: AppColors, insets: any, spacing: any, fontSize: any, responsive: any, layout: any) => StyleSheet.create({
  container: {flex: 1, backgroundColor: C.bg},
  scrollContent: { paddingTop: Platform.OS === 'ios' ? 0 : insets.top },
  centeredLayout: { alignSelf: 'center', width: '100%', maxWidth: layout.maxContentWidth },
  header: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: spacing.pagePadding, paddingTop: Platform.OS === 'ios' ? spacing.md : spacing.md + insets.top, paddingBottom: spacing.sm, zIndex: 10},
  locationBtn: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 8, borderRadius: Radius.full, borderWidth: 1, borderColor: 'rgba(255,255,255,0.08)', flex: 1, marginRight: 12 },
  locationText: {fontSize: fontSize.md, fontWeight: '700', color: C.text, flex: 1},
  headerRight: {flexDirection: 'row', gap: spacing.xs},
  iconBtn: {width: 40, height: 40, borderRadius: 20, backgroundColor: C.bgCard, justifyContent: 'center', alignItems: 'center', borderWidth: 1, borderColor: C.border},
  iconBtnText: {fontSize: 18},
  sectionCard: {marginHorizontal: spacing.pagePadding, marginBottom: spacing.md, backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: spacing.md, borderWidth: 1, borderColor: C.border, overflow: 'hidden'},
  sectionHeader: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: spacing.sm},
  sectionTitle: {fontSize: fontSize.xs, fontWeight: '700', color: C.textSecondary, letterSpacing: 1},
  sectionIcon: {fontSize: 13, color: C.textMuted},
  hourCard: { alignItems: 'center', borderRadius: Radius.lg, paddingHorizontal: spacing.md, paddingVertical: 14, minWidth: 76, gap: 4, borderWidth: 1.5, borderColor: 'rgba(255,255,255,0.08)' },
  hourCardActive: { backgroundColor: C.accent, borderColor: C.accent, shadowColor: C.accent, shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 4 },
  hourLabel: {fontSize: 13, color: C.textSecondary, fontWeight: '700'},
  hourEmoji: {fontSize: 26, marginVertical: 2},
  hourTemp: {fontSize: 17, fontWeight: '800', color: C.text},
  hourPrecip: {fontSize: 11, fontWeight: '900', color: C.accent},
  viewMoreBtn: {alignItems: 'center', paddingTop: spacing.md},
  viewMoreText: {fontSize: fontSize.sm, color: C.accent, fontWeight: '700'},
  modalOverlay: {flex: 1, backgroundColor: 'rgba(0,0,0,0.6)', justifyContent: 'flex-end'},
  locationModal: {borderTopLeftRadius: 28, borderTopRightRadius: 28, paddingHorizontal: spacing.lg, paddingTop: spacing.md, paddingBottom: 40, maxHeight: '85%', borderWidth: 1, borderBottomWidth: 0},
  locationModalHandle: {width: 40, height: 4, backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 2, alignSelf: 'center', marginBottom: spacing.md},
  locationModalTitle: {fontSize: fontSize.xl, fontWeight: '800', marginBottom: spacing.md, textAlign: 'center'},
  locationSearchBar: {flexDirection: 'row', alignItems: 'center', borderRadius: Radius.xl, paddingHorizontal: spacing.md, paddingVertical: 12, gap: spacing.sm, borderWidth: 1, marginBottom: spacing.md},
  locationSearchInput: {flex: 1, fontSize: fontSize.md, padding: 0},
  locationResultItem: {flexDirection: 'row', alignItems: 'center', gap: spacing.md, paddingVertical: 16, borderBottomWidth: 0.5},
  resultIconBox: {width: 40, height: 40, borderRadius: 12, backgroundColor: 'rgba(59, 130, 246, 0.1)', justifyContent: 'center', alignItems: 'center'},
  locationResultName: {fontSize: fontSize.md, fontWeight: '700'},
  locationResultSub: {fontSize: 12, marginTop: 2},
});
