import React from 'react';
import {
  ActivityIndicator,
  Button,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {useQueries} from '@tanstack/react-query';

import {getCurrentWeather, getDailyWeather, getHourlyWeather} from '../services/weatherApi';

const DEFAULT_LAT = 41.0082;
const DEFAULT_LON = 28.9784;

export function HomeScreen(): React.JSX.Element {
  const [refreshing, setRefreshing] = React.useState(false);

  const [currentQuery, hourlyQuery, dailyQuery] = useQueries({
    queries: [
      {queryKey: ['weather', 'current', DEFAULT_LAT, DEFAULT_LON], queryFn: () => getCurrentWeather(DEFAULT_LAT, DEFAULT_LON)},
      {queryKey: ['weather', 'hourly', DEFAULT_LAT, DEFAULT_LON], queryFn: () => getHourlyWeather(DEFAULT_LAT, DEFAULT_LON, 24)},
      {queryKey: ['weather', 'daily', DEFAULT_LAT, DEFAULT_LON], queryFn: () => getDailyWeather(DEFAULT_LAT, DEFAULT_LON, 7)},
    ],
  });

  const isLoading = currentQuery.isLoading || hourlyQuery.isLoading || dailyQuery.isLoading;
  const isError = currentQuery.isError || hourlyQuery.isError || dailyQuery.isError;

  const onRefresh = async () => {
    setRefreshing(true);
    await Promise.all([currentQuery.refetch(), hourlyQuery.refetch(), dailyQuery.refetch()]);
    setRefreshing(false);
  };

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator />
        <Text>Yukleniyor...</Text>
      </View>
    );
  }

  if (isError) {
    return (
      <View style={styles.center}>
        <Text>Veri alinamadi.</Text>
        <Button title="Tekrar Dene" onPress={() => void onRefresh()} />
      </View>
    );
  }

  const current = currentQuery.data;
  const hourlyItems = hourlyQuery.data?.items ?? [];
  const dailyItems = dailyQuery.data?.items ?? [];

  const metrics = [
    {label: 'Nem', value: `${current.humidity ?? '-'}%`},
    {label: 'Hissedilen', value: `${current.feels_like ?? '-'}°`},
    {label: 'Ruzgar', value: `${current.wind_speed ?? '-'} km/h`},
  ];

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void onRefresh()} />}>
      <Text style={styles.title}>Anlik Hava</Text>
      <Text style={styles.temp}>{current.temperature ?? '-'}°</Text>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>24 Saatlik Tahmin</Text>
        {hourlyItems.slice(0, 8).map((item: any, idx: number) => (
          <Text key={`${item.time}-${idx}`}>{item.time}: {item.temperature ?? '-'}°</Text>
        ))}
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>7 Gunluk Tahmin</Text>
        {dailyItems.map((item: any, idx: number) => (
          <Text key={`${item.date}-${idx}`}>
            {item.date}: {item.temp_max ?? '-'}° / {item.temp_min ?? '-'}°
          </Text>
        ))}
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Metrikler</Text>
        {metrics.map(metric => (
          <Text key={metric.label}>{metric.label}: {metric.value}</Text>
        ))}
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16},
  center: {flex: 1, justifyContent: 'center', alignItems: 'center', gap: 12},
  title: {fontSize: 24, fontWeight: '700'},
  temp: {fontSize: 42, fontWeight: '700', marginVertical: 12},
  section: {marginTop: 20, gap: 6},
  sectionTitle: {fontSize: 16, fontWeight: '700'},
});
