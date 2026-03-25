import React, {useState} from 'react';
import {
  ActivityIndicator,
  FlatList,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {useQuery} from '@tanstack/react-query';

import {useTranslation} from 'react-i18next';

import {getDailyWeather} from '../services/weatherApi';
import {searchCity, GeoResult, formatSunTime} from '../services/openMeteoApi';
import {DarkColors, FontSize, LightColors, Radius, Spacing, formatDayShort, getWeatherEmoji, AppColors} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';
import {getProfile} from '../services/profileApi';
import {MainStackParamList} from '../navigation/types';

type Props = NativeStackScreenProps<MainStackParamList, 'Forecast'>;

export function ForecastScreen({route, navigation}: Props): React.JSX.Element {
  const {t} = useTranslation();
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;

  const [lat, setLat] = useState(route.params?.lat ?? 41.0082);
  const [lon, setLon] = useState(route.params?.lon ?? 28.9784);
  const [city, setCity] = useState(route.params?.city ?? 'İstanbul, TR');
  const [searchText, setSearchText] = useState('');
  const [searchResults, setSearchResults] = useState<GeoResult[]>([]);
  const [searching, setSearching] = useState(false);

  const {isGuest} = useAuthStore();
  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: !isGuest,
  });
  const tempUnit = profileQuery.data?.temperature_unit ?? 'C';

  const dailyQuery = useQuery({
    queryKey: ['weather', 'daily', lat, lon, 10, tempUnit],
    queryFn: () => getDailyWeather(lat, lon, 10, tempUnit as any),
    staleTime: 5 * 60 * 1000,
  });

  const handleSearch = async () => {
    if (!searchText.trim()) return;
    setSearching(true);
    try {
      const results = await searchCity(searchText.trim());
      setSearchResults(results);
    } catch {
      setSearchResults([]);
    } finally {
      setSearching(false);
    }
  };

  const selectCity = (r: GeoResult) => {
    setLat(r.latitude);
    setLon(r.longitude);
    const label = r.admin1 ? `${r.name}, ${r.admin1}` : `${r.name}, ${r.country}`;
    setCity(label);
    setSearchText('');
    setSearchResults([]);
  };

  const s = makeStyles(C);
  const dailyItems = dailyQuery.data?.items ?? [];

  // AI hava özeti
  const aiInsight = buildAiInsight(dailyItems);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />

      {/* ── Header ── */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>{city}</Text>
        <TouchableOpacity style={s.moreBtn}>
          <Text style={s.moreText}>•••</Text>
        </TouchableOpacity>
      </View>

      {/* ── Arama ── */}
      <View style={s.searchWrapper}>
        <View style={s.searchBar}>
          <Text style={s.searchIcon}>🔍</Text>
          <TextInput
            style={s.searchInput}
            placeholder={t('forecast.searchPlaceholder')}
            placeholderTextColor={C.textMuted}
            value={searchText}
            onChangeText={setSearchText}
            onSubmitEditing={handleSearch}
            returnKeyType="search"
          />
          {searching && <ActivityIndicator size="small" color={C.accent} />}
        </View>

        {/* Arama Sonuçları */}
        {searchResults.length > 0 && (
          <View style={s.searchDropdown}>
            {searchResults.map((r, i) => (
              <TouchableOpacity
                key={`${r.latitude}-${r.longitude}-${i}`}
                style={s.searchResultItem}
                onPress={() => selectCity(r)}>
                <Text style={s.searchResultText}>
                  {r.name}
                  {r.admin1 ? `, ${r.admin1}` : ''}, {r.country}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        )}
      </View>

      {/* ── 10 Günlük Başlık ── */}
      <View style={s.forecastTitleRow}>
        <Text style={s.forecastTitle}>{t('forecast.title')}</Text>
        <Text style={s.calendarIcon}>📅</Text>
      </View>

      {dailyQuery.isLoading ? (
        <View style={s.center}>
          <ActivityIndicator size="large" color={C.accent} />
        </View>
      ) : (
        <FlatList
          data={dailyItems}
          keyExtractor={item => item.date}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{paddingBottom: Spacing.xxl}}
          renderItem={({item, index}) => (
            <ForecastRow item={item} isFirst={index === 0} C={C} onPress={() =>
              navigation.navigate('Hourly', {lat, lon, city})
            } />
          )}
          ListFooterComponent={
            aiInsight ? (
              <View style={s.aiCard}>
                <View style={s.aiCardHeader}>
                  <Text style={s.aiCardIcon}>✦</Text>
                  <Text style={s.aiCardTitle}> {t('forecast.aiInsightsTitle')}</Text>
                </View>
                <Text style={s.aiCardText}>"{aiInsight}"</Text>
              </View>
            ) : null
          }
        />
      )}
    </SafeAreaView>
  );
}

// ── ForecastRow ──────────────────────────────────────────────────────────────
function ForecastRow({
  item,
  isFirst,
  C,
  onPress,
}: {
  item: any;
  isFirst: boolean;
  C: AppColors;
  onPress?: () => void;
}) {
  const {t} = useTranslation();
  const s = rowStyles(C);
  const barPct = Math.min(
    90,
    Math.max(10, ((item.temp_max - item.temp_min + 5) / 20) * 80),
  );

  return (
    <TouchableOpacity activeOpacity={0.7} onPress={onPress} style={[s.row, isFirst && s.rowFirst]}>
      {/* Gün */}
      <View style={s.dayCol}>
        <Text style={s.dayText}>{isFirst ? t('forecast.today') : formatDayShort(item.date)}</Text>
        {isFirst && (
          <Text style={s.dateText}>
            {new Date(item.date + 'T12:00:00').toLocaleDateString('tr', {
              month: 'short',
              day: 'numeric',
            })}
          </Text>
        )}
      </View>

      {/* İkon */}
      <Text style={s.emoji}>{getWeatherEmoji(item.weather_code)}</Text>

      {/* Yağış olasılığı + miktar */}
      <View style={s.precipCol}>
        <Text style={s.precip}>💧{item.precipitation_probability}%</Text>
        {item.precipitation_sum > 0 && (
          <Text style={s.precipSum}>{item.precipitation_sum}mm</Text>
        )}
      </View>

      {/* Sıcaklık barı */}
      <Text style={s.tempMin}>{item.temp_min}°</Text>
      <View style={s.barTrack}>
        <View style={[s.barFill, {width: `${barPct}%`}]} />
      </View>
      <Text style={s.tempMax}>{item.temp_max}°</Text>

      {/* Güneş doğuş / batış */}
      {item.sunrise && (
        <View style={s.sunCol}>
          <Text style={s.sunText}>🌅{formatSunTime(item.sunrise)}</Text>
          <Text style={s.sunText}>🌇{formatSunTime(item.sunset)}</Text>
        </View>
      )}
    </TouchableOpacity>
  );
}

const rowStyles = (C: AppColors) =>
  StyleSheet.create({
    row: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: Spacing.lg,
      paddingVertical: 14,
      borderBottomWidth: 0.5,
      borderBottomColor: C.divider,
      gap: Spacing.sm,
    },
    rowFirst: {
      backgroundColor: C.bgCard,
      borderRadius: Radius.md,
      marginHorizontal: Spacing.sm,
      borderBottomWidth: 0,
      marginBottom: 2,
    },
    dayCol: {width: 52},
    dayText: {
      fontSize: FontSize.md,
      fontWeight: '700',
      color: C.text,
    },
    dateText: {
      fontSize: FontSize.xs,
      color: C.textSecondary,
    },
    emoji: {fontSize: 22, width: 30, textAlign: 'center'},
    precipCol: {width: 52, alignItems: 'flex-end'},
    precip: {
      fontSize: FontSize.sm,
      color: C.accent,
      fontWeight: '600',
    },
    precipSum: {
      fontSize: 10,
      color: '#29B5F6',
    },
    tempMin: {
      fontSize: FontSize.sm,
      color: C.textSecondary,
      width: 26,
      textAlign: 'right',
    },
    barTrack: {
      flex: 1,
      height: 4,
      backgroundColor: C.bgInput,
      borderRadius: 2,
      overflow: 'hidden',
    },
    barFill: {
      height: '100%',
      backgroundColor: C.accent,
      borderRadius: 2,
    },
    tempMax: {
      fontSize: FontSize.sm,
      fontWeight: '700',
      color: C.text,
      width: 26,
    },
    sunCol: {alignItems: 'flex-end', gap: 1},
    sunText: {fontSize: 9, color: C.textMuted},
  });

// ── AI Insight Üretici ───────────────────────────────────────────────────────
function buildAiInsight(items: any[]): string {
  if (items.length < 3) return '';
  const maxTemp = Math.max(...items.slice(0, 7).map((i: any) => i.temp_max));
  const minTemp = Math.min(...items.slice(0, 7).map((i: any) => i.temp_min));
  const rainyDay = items.slice(1, 7).find((i: any) => i.precipitation_probability > 60);
  const weekendDays = items.slice(5, 7);
  const niceWeekend = weekendDays.every(
    (i: any) => i.precipitation_probability < 20 && i.temp_max >= 18,
  );

  const parts: string[] = [];
  parts.push(`Expect temperatures between ${minTemp}° and ${maxTemp}° this week.`);
  if (rainyDay) {
    parts.push(
      `Keep an umbrella handy for ${formatDayShort(rainyDay.date)} with ${rainyDay.precipitation_probability}% chance of rain.`,
    );
  }
  if (niceWeekend) {
    parts.push('The weekend looks perfect for outdoor activities!');
  }
  return parts.join(' ');
}

// ── Stiller ──────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},
    center: {flex: 1, justifyContent: 'center', alignItems: 'center'},

    // Header
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: Spacing.md,
      paddingVertical: Spacing.md,
    },
    backBtn: {width: 36, justifyContent: 'center'},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerTitle: {
      flex: 1,
      fontSize: FontSize.xl,
      fontWeight: '700',
      color: C.text,
      textAlign: 'center',
    },
    moreBtn: {width: 36, alignItems: 'flex-end'},
    moreText: {color: C.text, fontSize: 20, letterSpacing: 2},

    // Arama
    searchWrapper: {
      paddingHorizontal: Spacing.lg,
      marginBottom: Spacing.md,
      position: 'relative',
      zIndex: 10,
    },
    searchBar: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: C.bgCard,
      borderRadius: Radius.full,
      paddingHorizontal: Spacing.md,
      paddingVertical: 10,
      gap: Spacing.sm,
      borderWidth: 1,
      borderColor: C.border,
    },
    searchIcon: {fontSize: 16},
    searchInput: {
      flex: 1,
      fontSize: FontSize.md,
      color: C.text,
      padding: 0,
    },
    searchDropdown: {
      position: 'absolute',
      top: '100%',
      left: Spacing.lg,
      right: Spacing.lg,
      backgroundColor: C.bgCard,
      borderRadius: Radius.md,
      borderWidth: 1,
      borderColor: C.border,
      zIndex: 100,
      elevation: 8,
      shadowColor: '#000',
      shadowOffset: {width: 0, height: 4},
      shadowOpacity: 0.2,
      shadowRadius: 8,
    },
    searchResultItem: {
      paddingHorizontal: Spacing.md,
      paddingVertical: Spacing.md,
      borderBottomWidth: 0.5,
      borderBottomColor: C.divider,
    },
    searchResultText: {
      fontSize: FontSize.md,
      color: C.text,
    },

    // Başlık
    forecastTitleRow: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'center',
      paddingHorizontal: Spacing.lg,
      marginBottom: Spacing.sm,
    },
    forecastTitle: {
      fontSize: FontSize.xxl,
      fontWeight: '800',
      color: C.text,
    },
    calendarIcon: {fontSize: 20},

    // AI Kartı
    aiCard: {
      margin: Spacing.lg,
      backgroundColor: C.bgCard,
      borderRadius: Radius.lg,
      padding: Spacing.lg,
      borderWidth: 1,
      borderColor: C.accent,
    },
    aiCardHeader: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: Spacing.sm,
    },
    aiCardIcon: {fontSize: 14, color: C.accent},
    aiCardTitle: {
      fontSize: FontSize.xs,
      fontWeight: '800',
      color: C.accent,
      letterSpacing: 1,
    },
    aiCardText: {
      fontSize: FontSize.md,
      color: C.textSecondary,
      lineHeight: 22,
      fontStyle: 'italic',
    },
  });
