import React, {useCallback, useState} from 'react';
import {
  ActivityIndicator,
  FlatList,
  Linking,
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
} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import {useQueries} from '@tanstack/react-query';

import {useTranslation} from 'react-i18next';

import {getCurrentWeather, getDailyWeather, getHourlyWeather} from '../services/weatherApi';
import {GeoResult, getMoonPhase, searchCity, formatSunTime} from '../services/openMeteoApi';
import {
  AppColors,
  DarkColors,
  FontSize,
  LightColors,
  Radius,
  Spacing,
  formatDayShort,
  formatHour,
  getWeatherEmoji,
  getWeatherLabel,
} from '../theme';
import {useThemeStore} from '../store/themeStore';

// ── Varsayılan konum ──────────────────────────────────────────────────────────
const DEFAULT_LAT = 41.0082;
const DEFAULT_LON = 28.9784;
const DEFAULT_CITY = 'İstanbul, TR';

export function HomeScreen(): React.JSX.Element {
  const {t} = useTranslation();
  const navigation = useNavigation<any>();
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;

  const [refreshing, setRefreshing] = useState(false);
  const [lat, setLat] = useState(DEFAULT_LAT);
  const [lon, setLon] = useState(DEFAULT_LON);
  const [city, setCity] = useState(DEFAULT_CITY);
  const [showLocationModal, setShowLocationModal] = useState(false);
  const [locationSearch, setLocationSearch] = useState('');
  const [locationResults, setLocationResults] = useState<GeoResult[]>([]);
  const [searchingLocation, setSearchingLocation] = useState(false);

  const [currentQuery, hourlyQuery, dailyQuery] = useQueries({
    queries: [
      {
        queryKey: ['weather', 'current', lat, lon],
        queryFn: () => getCurrentWeather(lat, lon),
        staleTime: 5 * 60 * 1000,
      },
      {
        queryKey: ['weather', 'hourly', lat, lon],
        queryFn: () => getHourlyWeather(lat, lon, 12),
        staleTime: 5 * 60 * 1000,
      },
      {
        queryKey: ['weather', 'daily', lat, lon],
        queryFn: () => getDailyWeather(lat, lon, 10),
        staleTime: 5 * 60 * 1000,
      },
    ],
  });

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

  const handleLocationSearch = async () => {
    if (!locationSearch.trim()) return;
    setSearchingLocation(true);
    try {
      const results = await searchCity(locationSearch.trim());
      setLocationResults(results);
    } finally {
      setSearchingLocation(false);
    }
  };

  const selectLocation = (r: GeoResult) => {
    setLat(r.latitude);
    setLon(r.longitude);
    const label = r.admin1 ? `${r.name}, ${r.admin1}` : `${r.name}, ${r.country}`;
    setCity(label);
    setLocationSearch('');
    setLocationResults([]);
    setShowLocationModal(false);
  };

  const s = makeStyles(C);
  const moon = getMoonPhase();

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

  if (currentQuery.isError) {
    const errMsg = currentQuery.error instanceof Error
      ? currentQuery.error.message
      : 'Bilinmeyen hata';
    return (
      <SafeAreaView style={s.safe}>
        <View style={s.center}>
          <Text style={{fontSize: 48}}>⚠️</Text>
          <Text style={s.errorText}>{t('home.errorTitle')}</Text>
          <Text style={{color: C.textMuted, fontSize: 12, textAlign: 'center', marginTop: 8, paddingHorizontal: 24}}>
            {errMsg}
          </Text>
          <TouchableOpacity style={s.retryBtn} onPress={onRefresh}>
            <Text style={s.retryText}>{t('common.retry')}</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const current = currentQuery.data!;
  const hourlyItems = hourlyQuery.data?.items ?? [];
  const dailyItems = dailyQuery.data?.items ?? [];
  const todayDaily = dailyItems[0];

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />

      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={C.accent} />
        }>

        {/* ── Header ── */}
        <View style={s.header}>
          <TouchableOpacity style={s.locationBtn} onPress={() => setShowLocationModal(true)}>
            <Text style={s.locationPin}>📍</Text>
            <Text style={s.locationText}>{city}</Text>
            <Text style={s.chevDown}>▾</Text>
          </TouchableOpacity>
          <View style={s.headerRight}>
            <TouchableOpacity
              style={s.iconBtn}
              onPress={() => navigation.navigate('Alerts')}>
              <Text style={s.iconBtnText}>🔔</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={s.iconBtn}
              onPress={() => navigation.navigate('AIChat')}>
              <Text style={s.iconBtnText}>✦</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={s.iconBtn}
              onPress={() => navigation.navigate('Map')}>
              <Text style={s.iconBtnText}>🗺️</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* ── AI Prompt Çubuğu ── */}
        <TouchableOpacity
          style={s.aiBar}
          onPress={() => navigation.navigate('AIChat')}
          activeOpacity={0.85}>
          <Text style={s.aiBarIcon}>💬</Text>
          <Text style={s.aiBarText}>"How's the weather for my hike?"</Text>
          <View style={s.aiActiveBadge}>
            <Text style={s.aiActiveBadgeText}>AI{'\n'}ACTIVE</Text>
          </View>
        </TouchableOpacity>

        {/* ── Hava Durumu Emoji ── */}
        <View style={s.weatherIllustration}>
          <Text style={s.weatherBigEmoji}>{getWeatherEmoji(current.weather_code)}</Text>
        </View>

        {/* ── Ana Sıcaklık Bloğu ── */}
        <View style={s.tempBlock}>
          <Text style={s.tempText}>{current.temperature}°</Text>
          <Text style={s.descText}>{getWeatherLabel(current.weather_code)}</Text>
          {todayDaily && (
            <Text style={s.hiLoText}>
              H:{todayDaily.temp_max}°{'  '}L:{todayDaily.temp_min}°
            </Text>
          )}
          <Text style={s.feelsLike}>{t('home.feelsLike')} {current.feels_like}°</Text>
          {current.precipitation > 0 && (
            <Text style={s.precipLabel}>🌧 {current.precipitation} mm {t('home.precipitation')}</Text>
          )}
        </View>

        {/* ── 24 Saatlik Tahmin ── */}
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
              <View style={[s.hourCard, index === 0 && s.hourCardActive]}>
                <Text style={s.hourLabel}>{index === 0 ? t('home.today') : formatHour(item.time)}</Text>
                <Text style={s.hourEmoji}>{getWeatherEmoji(item.weather_code)}</Text>
                <Text style={s.hourTemp}>{item.temperature}°</Text>
                {item.precipitation_probability > 20 && (
                  <Text style={s.hourPrecip}>💧{item.precipitation_probability}%</Text>
                )}
              </View>
            )}
          />
        </View>

        {/* ── 10 Günlük Özet ── */}
        <View style={s.sectionCard}>
          <View style={s.sectionHeader}>
            <View style={{flexDirection: 'row', alignItems: 'center', gap: 6}}>
              <Text style={s.sectionIcon}>📅</Text>
              <Text style={s.sectionTitle}>{t('home.forecastTitle').toUpperCase()}</Text>
            </View>
          </View>
          {dailyItems.slice(0, 4).map((item, idx) => (
            <DailyRow key={item.date} item={item} isFirst={idx === 0} C={C} />
          ))}
          <TouchableOpacity
            style={s.viewMoreBtn}
            onPress={() => navigation.navigate('Forecast', {lat, lon, city})}>
            <Text style={s.viewMoreText}>{t('forecast.title')} ›</Text>
          </TouchableOpacity>
        </View>

        {/* ── UV & Nem ── */}
        <View style={s.metricsRow}>
          <TouchableOpacity
            style={[s.metricCard, {flex: 1}]}
            onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
            <View style={s.metricHeader}>
              <Text style={s.metricIcon}>☀️</Text>
              <Text style={s.metricLabel}> {t('home.uvIndex').toUpperCase()}</Text>
            </View>
            <Text style={s.metricValue}>{current.uv_index}</Text>
            <Text style={s.metricSub}>
              {current.uv_index <= 2 ? 'Düşük' : current.uv_index <= 5 ? 'Orta' : current.uv_index <= 7 ? 'Yüksek' : 'Çok Yüksek'}
            </Text>
            <UvBar value={current.uv_index} C={C} />
          </TouchableOpacity>

          <TouchableOpacity
            style={[s.metricCard, {flex: 1}]}
            onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
            <View style={s.metricHeader}>
              <Text style={s.metricIcon}>💧</Text>
              <Text style={s.metricLabel}> {t('home.humidity').toUpperCase()}</Text>
            </View>
            <Text style={s.metricValue}>{current.humidity}%</Text>
            <Text style={s.metricSub}>Çiğ noktası {current.dew_point}°</Text>
          </TouchableOpacity>
        </View>

        {/* ── Görünürlük & Basınç ── */}
        <View style={s.metricsRow}>
          <TouchableOpacity
            style={[s.metricCard, {flex: 1}]}
            onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
            <View style={s.metricHeader}>
              <Text style={s.metricIcon}>👁️</Text>
              <Text style={s.metricLabel}> {t('home.visibility').toUpperCase()}</Text>
            </View>
            <Text style={s.metricValue}>{current.visibility}</Text>
            <Text style={s.metricSub}>km</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[s.metricCard, {flex: 1}]}
            onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
            <View style={s.metricHeader}>
              <Text style={s.metricIcon}>🌡️</Text>
              <Text style={s.metricLabel}> {t('home.pressure').toUpperCase()}</Text>
            </View>
            <Text style={s.metricValue}>{current.pressure}</Text>
            <Text style={s.metricSub}>hPa</Text>
          </TouchableOpacity>
        </View>

        {/* ── Bulut & Çiğ Noktası ── */}
        <View style={s.metricsRow}>
          <TouchableOpacity
            style={[s.metricCard, {flex: 1}]}
            onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
            <View style={s.metricHeader}>
              <Text style={s.metricIcon}>☁️</Text>
              <Text style={s.metricLabel}> {t('home.cloudCover').toUpperCase()}</Text>
            </View>
            <Text style={s.metricValue}>{current.cloud_cover}%</Text>
            <CloudBar value={current.cloud_cover} C={C} />
          </TouchableOpacity>

          <TouchableOpacity
            style={[s.metricCard, {flex: 1}]}
            onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
            <View style={s.metricHeader}>
              <Text style={s.metricIcon}>🌡️</Text>
              <Text style={s.metricLabel}> {t('home.dewPoint').toUpperCase()}</Text>
            </View>
            <Text style={s.metricValue}>{current.dew_point}°</Text>
            <Text style={s.metricSub}>
              {current.dew_point > current.temperature - 2 ? 'Nemli hava' : 'Konforlu'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* ── Rüzgar ── */}
        <TouchableOpacity
          style={s.sectionCard}
          onPress={() => navigation.navigate('WeatherDetail', {lat, lon})}>
          <View style={s.metricHeader}>
            <Text style={s.metricIcon}>🌬️</Text>
            <Text style={s.metricLabel}> {t('home.wind').toUpperCase()}</Text>
          </View>
          <View style={s.windRow}>
            <View>
              <Text style={s.windSpeed}>{current.wind_speed}</Text>
              <Text style={s.windUnit}>km/h</Text>
              <Text style={s.windDir}>{windDirectionLabel(current.wind_direction)}</Text>
              {current.wind_gusts > 0 && (
                <Text style={s.windGust}>{t('home.gusts')}: {current.wind_gusts} km/h</Text>
              )}
            </View>
            <WindCompass direction={current.wind_direction} C={C} />
          </View>
        </TouchableOpacity>

        {/* ── Güneş & Ay ── */}
        {todayDaily && (
          <View style={s.sectionCard}>
            <View style={s.sunMoonRow}>
              <View style={s.sunBlock}>
                <Text style={s.sunLabel}>🌅 {t('home.sunrise').toUpperCase()}</Text>
                <Text style={s.sunTime}>{formatSunTime(todayDaily.sunrise)}</Text>
              </View>
              <View style={s.sunSeparator} />
              <View style={s.sunBlock}>
                <Text style={s.sunLabel}>🌇 {t('home.sunset').toUpperCase()}</Text>
                <Text style={s.sunTime}>{formatSunTime(todayDaily.sunset)}</Text>
              </View>
              <View style={s.sunSeparator} />
              <View style={s.sunBlock}>
                <Text style={s.sunLabel}>{moon.emoji} {t('home.moonPhase').toUpperCase()}</Text>
                <Text style={s.sunTime}>{moon.label}</Text>
              </View>
            </View>
          </View>
        )}

        <View style={{height: Spacing.xxl}} />
      </ScrollView>

      {/* ── Ask Havamania Butonu ── */}
      <View style={s.askBtnWrapper}>
        <TouchableOpacity
          style={s.askBtn}
          onPress={() => navigation.navigate('AIChat')}
          activeOpacity={0.85}>
          <Text style={s.askBtnIcon}>✦</Text>
          <Text style={s.askBtnText}>{t('home.askHavamania')}</Text>
        </TouchableOpacity>
      </View>

      {/* ── Konum Seçici Modal ── */}
      <Modal
        visible={showLocationModal}
        animationType="slide"
        transparent
        onRequestClose={() => setShowLocationModal(false)}>
        <TouchableOpacity
          style={s.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowLocationModal(false)}>
          <View style={[s.locationModal, {backgroundColor: C.bgCard}]}>
            <View style={s.locationModalHandle} />
            <Text style={[s.locationModalTitle, {color: C.text}]}>📍 {t('map.yourLocation')}</Text>

            <View style={[s.locationSearchBar, {backgroundColor: C.bgInput, borderColor: C.border}]}>
              <Text style={{fontSize: 16}}>🔍</Text>
              <TextInput
                style={[s.locationSearchInput, {color: C.text}]}
                placeholder={t('forecast.searchPlaceholder')}
                placeholderTextColor={C.textMuted}
                value={locationSearch}
                onChangeText={setLocationSearch}
                onSubmitEditing={handleLocationSearch}
                returnKeyType="search"
                autoFocus
              />
              {searchingLocation && <ActivityIndicator size="small" color={C.accent} />}
            </View>

            {locationResults.length > 0 ? (
              <FlatList
                data={locationResults}
                keyExtractor={(item, i) => `${item.latitude}-${i}`}
                renderItem={({item}) => (
                  <TouchableOpacity
                    style={[s.locationResultItem, {borderBottomColor: C.divider}]}
                    onPress={() => selectLocation(item)}>
                    <Text style={s.locationResultEmoji}>📍</Text>
                    <View>
                      <Text style={[s.locationResultName, {color: C.text}]}>{item.name}</Text>
                      <Text style={[s.locationResultSub, {color: C.textSecondary}]}>
                        {item.admin1 ? `${item.admin1}, ` : ''}{item.country}
                      </Text>
                    </View>
                  </TouchableOpacity>
                )}
              />
            ) : (
              <View style={s.locationResultsEmpty}>
                <Text style={{fontSize: 36}}>🌍</Text>
                <Text style={[{color: C.textSecondary, marginTop: 8, textAlign: 'center'}]}>
                  Şehir adı yazıp arama yap
                </Text>
              </View>
            )}
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
}

// ── Alt Bileşenler ────────────────────────────────────────────────────────────

function DailyRow({item, isFirst, C}: {item: any; isFirst: boolean; C: AppColors}) {
  const barPct = Math.min(100, Math.max(15, ((item.temp_max - item.temp_min) / 15) * 80));
  return (
    <View style={dailyStyles(C).row}>
      <Text style={dailyStyles(C).day}>{isFirst ? 'Today' : formatDayShort(item.date)}</Text>
      <Text style={dailyStyles(C).emoji}>{getWeatherEmoji(item.weather_code)}</Text>
      {item.precipitation_probability > 0 && (
        <Text style={dailyStyles(C).precip}>{item.precipitation_probability}%</Text>
      )}
      <Text style={dailyStyles(C).tempMin}>{item.temp_min}°</Text>
      <View style={dailyStyles(C).barTrack}>
        <View style={[dailyStyles(C).barFill, {width: `${barPct}%`}]} />
      </View>
      <Text style={dailyStyles(C).tempMax}>{item.temp_max}°</Text>
    </View>
  );
}

const dailyStyles = (C: AppColors) =>
  StyleSheet.create({
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingVertical: Spacing.sm,
      gap: Spacing.xs,
      borderBottomWidth: 0.5,
      borderBottomColor: C.divider,
    },
    day: {fontSize: FontSize.md, color: C.text, fontWeight: '600', width: 52},
    emoji: {fontSize: 20, width: 28, textAlign: 'center'},
    precip: {fontSize: FontSize.xs, color: C.accent, width: 32, textAlign: 'right'},
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

function CloudBar({value, C}: {value: number; C: AppColors}) {
  return (
    <View style={{height: 4, backgroundColor: C.bgInput, borderRadius: 2, marginTop: 8, overflow: 'hidden'}}>
      <View style={{width: `${value}%`, height: '100%', backgroundColor: C.textSecondary, borderRadius: 2}} />
    </View>
  );
}

function WindCompass({direction, C}: {direction: number; C: AppColors}) {
  return (
    <View style={{width: 72, height: 72, borderRadius: 36, borderWidth: 1.5, borderColor: C.border, justifyContent: 'center', alignItems: 'center'}}>
      <Text style={{color: C.textMuted, fontSize: FontSize.xs, position: 'absolute', top: 6}}>N</Text>
      <Text style={{fontSize: 22, color: C.accent, transform: [{rotate: `${direction}deg`}]}}>↑</Text>
    </View>
  );
}

function windDirectionLabel(deg: number): string {
  const dirs = ['Kuzey', 'K-D', 'Doğu', 'G-D', 'Güney', 'G-B', 'Batı', 'K-B'];
  return dirs[Math.round(deg / 45) % 8];
}

// ── Stiller ───────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},
    center: {flex: 1, justifyContent: 'center', alignItems: 'center', gap: 12},
    loadingText: {color: C.textSecondary, fontSize: FontSize.md, marginTop: 8},
    errorText: {color: C.text, fontSize: FontSize.lg, fontWeight: '700'},
    retryBtn: {backgroundColor: C.accentBtn, paddingHorizontal: Spacing.lg, paddingVertical: Spacing.sm, borderRadius: Radius.full, marginTop: Spacing.sm},
    retryText: {color: '#FFFFFF', fontWeight: '700'},

    header: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: Spacing.lg, paddingTop: Spacing.md, paddingBottom: Spacing.sm},
    locationBtn: {flexDirection: 'row', alignItems: 'center', gap: 4},
    locationPin: {fontSize: 16},
    locationText: {fontSize: FontSize.lg, fontWeight: '700', color: C.text},
    chevDown: {fontSize: 12, color: C.textSecondary, marginLeft: 2},
    headerRight: {flexDirection: 'row', gap: Spacing.xs},
    iconBtn: {width: 34, height: 34, borderRadius: 17, backgroundColor: C.bgCard, justifyContent: 'center', alignItems: 'center'},
    iconBtnText: {fontSize: 15},

    aiBar: {flexDirection: 'row', alignItems: 'center', marginHorizontal: Spacing.lg, marginBottom: Spacing.md, backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, gap: Spacing.sm, borderWidth: 1, borderColor: C.border},
    aiBarIcon: {fontSize: 16},
    aiBarText: {flex: 1, fontSize: FontSize.sm, color: C.textSecondary, fontStyle: 'italic'},
    aiActiveBadge: {backgroundColor: C.accentBtn, paddingHorizontal: 10, paddingVertical: 4, borderRadius: Radius.full},
    aiActiveBadgeText: {fontSize: 10, fontWeight: '800', color: '#FFFFFF', textAlign: 'center', lineHeight: 13},

    weatherIllustration: {alignItems: 'center', marginVertical: Spacing.xs},
    weatherBigEmoji: {fontSize: 100},

    tempBlock: {alignItems: 'center', marginBottom: Spacing.lg},
    tempText: {fontSize: 80, fontWeight: '800', color: C.text, lineHeight: 86},
    descText: {fontSize: FontSize.xl, color: C.text, fontWeight: '500', marginBottom: 2},
    hiLoText: {fontSize: FontSize.md, color: C.accent, fontWeight: '600'},
    feelsLike: {fontSize: FontSize.sm, color: C.textSecondary, marginTop: 4},
    precipLabel: {fontSize: FontSize.sm, color: C.accent, marginTop: 2},

    sectionCard: {marginHorizontal: Spacing.md, marginBottom: Spacing.md, backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border},
    sectionHeader: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: Spacing.sm},
    sectionTitle: {fontSize: FontSize.xs, fontWeight: '700', color: C.textSecondary, letterSpacing: 1},
    sectionIcon: {fontSize: 13, color: C.textMuted},

    hourCard: {alignItems: 'center', backgroundColor: C.bgSecondary, borderRadius: Radius.md, padding: Spacing.sm, minWidth: 62, gap: 2, borderWidth: 1, borderColor: C.border},
    hourCardActive: {backgroundColor: C.cardHourlyActive, borderColor: C.accent},
    hourLabel: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '600'},
    hourEmoji: {fontSize: 22},
    hourTemp: {fontSize: FontSize.md, fontWeight: '700', color: C.text},
    hourPrecip: {fontSize: 9, color: C.accent},

    viewMoreBtn: {alignItems: 'center', paddingTop: Spacing.md},
    viewMoreText: {fontSize: FontSize.sm, color: C.accent, fontWeight: '700'},

    metricsRow: {flexDirection: 'row', marginHorizontal: Spacing.md, marginBottom: Spacing.md, gap: Spacing.sm},
    metricCard: {backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border},
    metricHeader: {flexDirection: 'row', alignItems: 'center', marginBottom: Spacing.xs},
    metricIcon: {fontSize: 13},
    metricLabel: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '700', letterSpacing: 0.8},
    metricValue: {fontSize: FontSize.xxl, fontWeight: '800', color: C.text},
    metricSub: {fontSize: FontSize.xs, color: C.textSecondary, marginTop: 2},

    windRow: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: Spacing.sm},
    windSpeed: {fontSize: 36, fontWeight: '800', color: C.text},
    windUnit: {fontSize: FontSize.sm, color: C.textSecondary},
    windDir: {fontSize: FontSize.sm, color: C.textSecondary, marginTop: 2},
    windGust: {fontSize: FontSize.xs, color: C.accent, marginTop: 2},

    sunMoonRow: {flexDirection: 'row', alignItems: 'center'},
    sunBlock: {flex: 1, alignItems: 'center', gap: 4},
    sunSeparator: {width: 1, height: 36, backgroundColor: C.divider},
    sunLabel: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '700', letterSpacing: 0.5},
    sunTime: {fontSize: FontSize.lg, fontWeight: '700', color: C.text},

    askBtnWrapper: {paddingHorizontal: Spacing.lg, paddingBottom: Spacing.md, backgroundColor: C.bg},
    askBtn: {flexDirection: 'row', alignItems: 'center', justifyContent: 'center', backgroundColor: C.accentBtn, borderRadius: Radius.full, paddingVertical: 16, gap: Spacing.sm},
    askBtnIcon: {fontSize: 18, color: '#FFFFFF'},
    askBtnText: {fontSize: FontSize.lg, fontWeight: '700', color: '#FFFFFF'},

    // Konum Modal
    modalOverlay: {flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end'},
    locationModal: {borderTopLeftRadius: 20, borderTopRightRadius: 20, paddingHorizontal: Spacing.lg, paddingTop: Spacing.md, paddingBottom: 40, maxHeight: '70%'},
    locationModalHandle: {width: 40, height: 4, backgroundColor: '#888', borderRadius: 2, alignSelf: 'center', marginBottom: Spacing.md},
    locationModalTitle: {fontSize: FontSize.xl, fontWeight: '800', marginBottom: Spacing.md},
    locationSearchBar: {flexDirection: 'row', alignItems: 'center', borderRadius: Radius.full, paddingHorizontal: Spacing.md, paddingVertical: 10, gap: Spacing.sm, borderWidth: 1, marginBottom: Spacing.md},
    locationSearchInput: {flex: 1, fontSize: FontSize.md, padding: 0},
    locationResultItem: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md, paddingVertical: Spacing.md, borderBottomWidth: 0.5},
    locationResultEmoji: {fontSize: 18},
    locationResultName: {fontSize: FontSize.md, fontWeight: '600'},
    locationResultSub: {fontSize: FontSize.sm},
    locationResultsEmpty: {alignItems: 'center', paddingVertical: Spacing.xl},
  });
