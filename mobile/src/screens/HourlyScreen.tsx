import React, {useMemo} from 'react';
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
import {useQuery} from '@tanstack/react-query';

import {getHourlyWeather} from '../services/weatherApi';
import {AppColors, DarkColors, FontSize, LightColors, Radius, Spacing, formatHour, getWeatherEmoji} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';
import {getProfile} from '../services/profileApi';
import {MainStackParamList} from '../navigation/types';

type Props = NativeStackScreenProps<MainStackParamList, 'Hourly'>;

export function HourlyScreen({route, navigation}: Props): React.JSX.Element {
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;
  const {lat, lon, city} = route.params;

  const {isGuest} = useAuthStore();
  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    enabled: !isGuest,
  });
  const tempUnit = profileQuery.data?.temperature_unit ?? 'C';

  const hourlyQuery = useQuery({
    queryKey: ['weather', 'hourly', lat, lon, 48, tempUnit],
    queryFn: () => getHourlyWeather(lat, lon, 48, tempUnit as any),
    staleTime: 5 * 60 * 1000,
  });

  const items = hourlyQuery.data?.items ?? [];

  // Sıcaklık aralığını bul (grafik normalleştirme için)
  const {minTemp, maxTemp} = useMemo(() => {
    if (!items.length) return {minTemp: 0, maxTemp: 30};
    const temps = items.map(i => i.temperature);
    return {
      minTemp: Math.min(...temps) - 2,
      maxTemp: Math.max(...temps) + 2,
    };
  }, [items]);

  const barHeight = (temp: number) => {
    const range = maxTemp - minTemp;
    if (range === 0) return 50;
    return Math.round(((temp - minTemp) / range) * 60) + 10; // 10-70px
  };

  const s = makeStyles(C);

  // Saatleri gün gruplarına ayır
  const groupedByDay: {date: string; label: string; items: typeof items}[] = [];
  items.forEach(item => {
    const dateKey = item.time.split('T')[0];
    const last = groupedByDay[groupedByDay.length - 1];
    if (!last || last.date !== dateKey) {
      const d = new Date(dateKey + 'T12:00:00');
      const today = new Date();
      const tomorrow = new Date(today);
      tomorrow.setDate(today.getDate() + 1);
      let label = d.toLocaleDateString('tr', {weekday: 'long', day: 'numeric', month: 'long'});
      if (d.toDateString() === today.toDateString()) label = 'Bugün';
      else if (d.toDateString() === tomorrow.toDateString()) label = 'Yarın';
      groupedByDay.push({date: dateKey, label, items: [item]});
    } else {
      last.items.push(item);
    }
  });

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle={theme === 'dark' ? 'light-content' : 'dark-content'} backgroundColor={C.bg} />

      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <View style={s.headerCenter}>
          <Text style={s.headerTitle}>Saatlik Tahmin</Text>
          <Text style={s.headerSub}>{city}</Text>
        </View>
        <View style={{width: 36}} />
      </View>

      {hourlyQuery.isLoading ? (
        <View style={s.center}>
          <ActivityIndicator size="large" color={C.accent} />
        </View>
      ) : (
        <ScrollView showsVerticalScrollIndicator={false}>
          {groupedByDay.map(group => (
            <View key={group.date} style={s.dayGroup}>
              {/* Gün başlığı */}
              <Text style={s.dayLabel}>{group.label}</Text>

              {/* Sıcaklık grafiği + saat kartları */}
              <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                <View>
                  {/* Grafik çubukları */}
                  <View style={s.chartRow}>
                    {group.items.map((item, idx) => (
                      <View key={`${item.time}-${idx}`} style={s.chartCol}>
                        <Text style={s.chartTemp}>{item.temperature}°</Text>
                        <View style={s.chartBarTrack}>
                          <View
                            style={[
                              s.chartBarFill,
                              {height: barHeight(item.temperature)},
                            ]}
                          />
                        </View>
                      </View>
                    ))}
                  </View>

                  {/* Saat kart listesi */}
                  <View style={s.hourRow}>
                    {group.items.map((item, idx) => (
                      <View key={`card-${item.time}-${idx}`} style={s.hourCard}>
                        <Text style={s.hourTime}>{formatHour(item.time)}</Text>
                        <Text style={s.hourEmoji}>{getWeatherEmoji(item.weather_code)}</Text>
                        {item.precipitation_probability > 0 && (
                          <Text style={s.hourPrecip}>
                            💧{item.precipitation_probability}%
                          </Text>
                        )}
                        <Text style={s.hourWind}>{item.wind_speed}km/h</Text>
                        {item.precipitation > 0 && (
                          <Text style={s.hourRain}>{item.precipitation}mm</Text>
                        )}
                        <CloudIndicator pct={item.cloud_cover} C={C} />
                      </View>
                    ))}
                  </View>
                </View>
              </ScrollView>
            </View>
          ))}

          {/* Legend */}
          <View style={s.legend}>
            <View style={s.legendItem}>
              <Text style={s.legendDot}>💧</Text>
              <Text style={s.legendText}>Yağış olasılığı</Text>
            </View>
            <View style={s.legendItem}>
              <Text style={s.legendDot}>💨</Text>
              <Text style={s.legendText}>Rüzgar hızı</Text>
            </View>
            <View style={s.legendItem}>
              <Text style={s.legendDot}>☁️</Text>
              <Text style={s.legendText}>Bulut örtüsü</Text>
            </View>
          </View>

          <View style={{height: Spacing.xxl}} />
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

// ── Bulut Göstergesi ──────────────────────────────────────────────────────────
function CloudIndicator({pct, C}: {pct: number; C: AppColors}) {
  const filled = Math.round(pct / 25); // 0-4 arası
  return (
    <View style={{flexDirection: 'row', gap: 1, marginTop: 2}}>
      {[0, 1, 2, 3].map(i => (
        <View
          key={i}
          style={{
            width: 5,
            height: 5,
            borderRadius: 1,
            backgroundColor: i < filled ? C.textSecondary : C.border,
          }}
        />
      ))}
    </View>
  );
}

// ── Stiller ───────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},
    center: {flex: 1, justifyContent: 'center', alignItems: 'center'},

    header: {flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.md, paddingVertical: Spacing.md},
    backBtn: {width: 36},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerCenter: {flex: 1, alignItems: 'center'},
    headerTitle: {fontSize: FontSize.lg, fontWeight: '700', color: C.text},
    headerSub: {fontSize: FontSize.xs, color: C.textSecondary},

    dayGroup: {marginBottom: Spacing.md},
    dayLabel: {fontSize: FontSize.md, fontWeight: '800', color: C.text, paddingHorizontal: Spacing.lg, paddingVertical: Spacing.sm, backgroundColor: C.bgSecondary, borderBottomWidth: 0.5, borderBottomColor: C.divider},

    // Grafik
    chartRow: {flexDirection: 'row', alignItems: 'flex-end', paddingHorizontal: Spacing.md, paddingTop: Spacing.md, gap: 2},
    chartCol: {width: 64, alignItems: 'center', gap: 2},
    chartTemp: {fontSize: FontSize.xs, color: C.text, fontWeight: '700'},
    chartBarTrack: {width: 6, height: 70, backgroundColor: C.bgInput, borderRadius: 3, justifyContent: 'flex-end', overflow: 'hidden'},
    chartBarFill: {width: '100%', backgroundColor: C.accent, borderRadius: 3},

    // Saat kartları
    hourRow: {flexDirection: 'row', paddingHorizontal: Spacing.md, paddingBottom: Spacing.md, gap: 2},
    hourCard: {width: 64, alignItems: 'center', paddingVertical: Spacing.sm, gap: 2, backgroundColor: C.bgCard, borderRadius: Radius.md, borderWidth: 0.5, borderColor: C.border},
    hourTime: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '700'},
    hourEmoji: {fontSize: 20},
    hourPrecip: {fontSize: 9, color: C.accent},
    hourWind: {fontSize: 9, color: C.textMuted},
    hourRain: {fontSize: 9, color: '#29B5F6'},

    legend: {flexDirection: 'row', justifyContent: 'center', gap: Spacing.lg, paddingVertical: Spacing.md, borderTopWidth: 0.5, borderTopColor: C.divider, marginTop: Spacing.md},
    legendItem: {flexDirection: 'row', alignItems: 'center', gap: 4},
    legendDot: {fontSize: 14},
    legendText: {fontSize: FontSize.xs, color: C.textSecondary},
  });
