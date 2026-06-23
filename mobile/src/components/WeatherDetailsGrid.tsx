import React from 'react';
import { StyleSheet, View, Text, Dimensions } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { Spacing, Radius, FontSize, AppColors } from '../theme';
import { CurrentWeatherData, DailyWeatherItem, formatSunTime } from '../services/openMeteoApi';
import { formatPrecipitationProbability } from '../utils/weatherUtils';

import { formatPrecipitationProbability } from '../utils/weatherUtils';

const { width } = Dimensions.get('window');
const COLUMN_WIDTH = (width - Spacing.md * 3) / 2;

interface WeatherDetailsGridProps {
  current: CurrentWeatherData;
  todayDaily?: DailyWeatherItem;
  C: AppColors;
}

export const WeatherDetailsGrid: React.FC<WeatherDetailsGridProps> = ({ current, todayDaily, C }) => {
  const getWindDirection = (deg: number) => {
    const directions = ['K', 'KD', 'D', 'GD', 'G', 'GB', 'B', 'KB'];
    return directions[Math.round(deg / 45) % 8];
  };

  const details = [
    {
      icon: 'thermometer-outline',
      label: 'HİSSEDİLEN',
      value: `${current.feels_like}°`,
      desc: current.feels_like > current.temperature ? 'Daha sıcak' : 'Daha soğuk',
      color: '#F97316'
    },
    {
      icon: 'water-outline',
      label: 'NEM',
      value: `%${current.humidity}`,
      desc: `Çiy noktası ${current.dew_point}°`,
      color: '#3B82F6'
    },
    {
      icon: 'rainy-outline',
      label: 'YAĞIŞ OLASILIĞI',
      value: formatPrecipitationProbability(todayDaily?.precipitation_probability),
      desc: `${todayDaily?.precipitation_sum ?? 0} mm beklenen`,
      color: '#60A5FA'
    },
    {
      icon: 'air-outline',
      label: 'RÜZGAR',
      value: `${current.wind_speed} km/sa`,
      desc: `Yön: ${getWindDirection(current.wind_direction)}`,
      color: '#94A3B8'
    },
    {
      icon: 'sunny-outline',
      label: 'UV İNDEKSİ',
      value: `${current.uv_index}`,
      desc: current.uv_index > 5 ? 'Yüksek koruma' : 'Düşük risk',
      color: '#FBBF24'
    },
    {
      icon: 'eye-outline',
      label: 'GÖRÜŞ MESAFESİ',
      value: `${current.visibility} km`,
      desc: current.visibility > 5 ? 'Berrak hava' : 'Puslu hava',
      color: '#10B981'
    },
    {
      icon: 'speedometer-outline',
      label: 'BASINÇ',
      value: `${current.pressure} hPa`,
      desc: 'Deniz seviyesi',
      color: '#8B5CF6'
    },
    {
      icon: 'cloud-outline',
      label: 'BULUTLULUK',
      value: `%${current.cloud_cover}`,
      desc: current.cloud_cover > 50 ? 'Kapalı gökyüzü' : 'Açık gökyüzü',
      color: '#64748B'
    },
    {
      icon: 'sunny-outline',
      label: 'GÜN DOĞUMU',
      value: todayDaily ? formatSunTime(todayDaily.sunrise) : '--:--',
      desc: 'Sabah vakti',
      color: '#F59E0B'
    },
    {
      icon: 'moon-outline',
      label: 'GÜN BATIMI',
      value: todayDaily ? formatSunTime(todayDaily.sunset) : '--:--',
      desc: 'Akşam vakti',
      color: '#EC4899'
    }
  ];

  return (
    <View style={styles.container}>
      {details.map((item, index) => (
        <View key={index} style={[styles.card, { backgroundColor: C.bgCard, borderColor: C.border }]}>
          <View style={styles.header}>
            <Text style={{fontSize: 16}}>{
              item.label === 'HİSSEDİLEN' ? '🌡️' :
              item.label === 'NEM' ? '💧' :
              item.label === 'YAĞIŞ OLASILIĞI' ? '🌧️' :
              item.label === 'RÜZGAR' ? '💨' :
              item.label === 'UV İNDEKSİ' ? '☀️' :
              item.label === 'GÖRÜŞ MESAFESİ' ? '👁️' :
              item.label === 'BASINÇ' ? '⏲️' :
              item.label === 'BULUTLULUK' ? '☁️' :
              item.label === 'GÜN DOĞUMU' ? '🌅' :
              item.label === 'GÜN BATIMI' ? '🌇' : '📍'
            }</Text>
            <Text style={[styles.label, { color: C.textSecondary }]}>{item.label}</Text>
          </View>
          <Text style={[styles.value, { color: C.text }]}>{item.value}</Text>
          <Text style={[styles.desc, { color: C.textMuted }]}>{item.desc}</Text>
        </View>
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.md,
    paddingHorizontal: Spacing.md,
    marginBottom: Spacing.xl,
  },
  card: {
    width: COLUMN_WIDTH,
    padding: Spacing.md,
    borderRadius: Radius.lg,
    borderWidth: 1,
    gap: 4,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 4,
  },
  label: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  value: {
    fontSize: FontSize.xxl,
    fontWeight: '700',
  },
  desc: {
    fontSize: 11,
    fontWeight: '500',
  },
});
