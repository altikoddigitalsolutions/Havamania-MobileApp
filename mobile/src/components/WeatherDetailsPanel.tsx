import React from 'react';
import { StyleSheet, View, Text, Dimensions, ScrollView } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { Spacing, Radius, FontSize, AppColors } from '../theme';
import { CurrentWeatherData, DailyWeatherItem, formatSunTime } from '../services/openMeteoApi';
import { formatPrecipitationProbability } from '../utils/weatherUtils';

const { width } = Dimensions.get('window');
const COLUMN_WIDTH = (width - Spacing.md * 3) / 2;

interface WeatherDetailCardProps {
  icon: string;
  label: string;
  value: string | number;
  description?: string;
  C: AppColors;
  color?: string;
}

const WeatherDetailCard: React.FC<WeatherDetailCardProps> = ({ icon, label, value, description, C, color }) => (
  <View style={[styles.card, { backgroundColor: C.bgCard, borderColor: C.border }]}>
    <View style={styles.cardHeader}>
      <Icon name={icon} size={16} color={color || C.accent} />
      <Text style={[styles.cardLabel, { color: C.textSecondary }]}>{label.toUpperCase()}</Text>
    </View>
    <Text style={[styles.cardValue, { color: C.text }]}>{value}</Text>
    {description && (
      <Text style={[styles.cardDesc, { color: C.textMuted }]}>{description}</Text>
    )}
  </View>
);

const SunTimesCard: React.FC<{ today?: DailyWeatherItem; C: AppColors }> = ({ today, C }) => {
  if (!today) return null;
  return (
    <View style={[styles.fullWidthCard, { backgroundColor: C.bgCard, borderColor: C.border }]}>
      <View style={styles.cardHeader}>
        <Icon name="sunny-outline" size={16} color="#F59E0B" />
        <Text style={[styles.cardLabel, { color: C.textSecondary }]}>GÜNEŞ ZAMANLARI</Text>
      </View>
      <View style={styles.sunRow}>
        <View style={styles.sunItem}>
          <Text style={[styles.sunTime, { color: C.text }]}>{formatSunTime(today.sunrise)}</Text>
          <Text style={[styles.sunLabel, { color: C.textMuted }]}>Gün Doğumu</Text>
        </View>
        <View style={styles.sunDivider} />
        <View style={styles.sunItem}>
          <Text style={[styles.sunTime, { color: C.text }]}>{formatSunTime(today.solar_noon)}</Text>
          <Text style={[styles.sunLabel, { color: C.textMuted }]}>Öğle (Solar Noon)</Text>
        </View>
        <View style={styles.sunDivider} />
        <View style={styles.sunItem}>
          <Text style={[styles.sunTime, { color: C.text }]}>{formatSunTime(today.sunset)}</Text>
          <Text style={[styles.sunLabel, { color: C.textMuted }]}>Gün Batımı</Text>
        </View>
      </View>
    </View>
  );
};

const WindDirectionCard: React.FC<{ current: CurrentWeatherData; C: AppColors }> = ({ current, C }) => {
  const getWindDirectionLabel = (deg: number) => {
    const directions = ['Kuzey', 'Kuzeydoğu', 'Doğu', 'Güneydoğu', 'Güney', 'Güneybatı', 'Batı', 'Kuzeybatı'];
    return directions[Math.round(deg / 45) % 8];
  };

  const rotate = `${current.wind_direction}deg`;

  return (
    <View style={[styles.card, { backgroundColor: C.bgCard, borderColor: C.border }]}>
      <View style={styles.cardHeader}>
        <Icon name="compass-outline" size={16} color="#94A3B8" />
        <Text style={[styles.cardLabel, { color: C.textSecondary }]}>RÜZGAR YÖNÜ</Text>
      </View>
      <View style={styles.windContent}>
        <View style={styles.compassContainer}>
            <View style={[styles.compassCircle, { borderColor: C.border }]}>
                <View style={[styles.compassNeedle, { transform: [{ rotate }] }]}>
                    <Icon name="navigation" size={20} color={C.accent} />
                </View>
            </View>
        </View>
        <View>
            <Text style={[styles.cardValueSmall, { color: C.text }]}>{current.wind_direction}°</Text>
            <Text style={[styles.cardDesc, { color: C.textMuted }]}>{getWindDirectionLabel(current.wind_direction)}</Text>
        </View>
      </View>
    </View>
  );
};

const WeatherSuitabilityCard: React.FC<{ current: CurrentWeatherData; today?: DailyWeatherItem; C: AppColors }> = ({ current, today, C }) => {
  const calculateSuitability = () => {
    let score = 100;
    let reasons: string[] = [];

    if (current.precipitation > 0 || (today?.precipitation_probability ?? 0) > 40) {
      score -= 30;
      reasons.push('Yağmur riski');
    }
    if (current.wind_speed > 25) {
      score -= 20;
      reasons.push('Güçlü rüzgar');
    }
    if (current.uv_index > 7) {
      score -= 15;
      reasons.push('Yüksek UV');
    }
    if (current.visibility < 3) {
      score -= 20;
      reasons.push('Düşük görüş');
    }
    if (current.temperature > 35 || current.temperature < 0) {
      score -= 10;
      reasons.push('Ekstrem sıcaklık');
    }

    if (score > 80) return { label: 'Açık hava için mükemmel', color: '#10B981', icon: 'checkmark-circle' };
    if (score > 60) return { label: 'Yürüyüş için uygun', color: '#F59E0B', icon: 'walk' };
    if (score > 40) return { label: 'Seyahat için dikkatli ol', color: '#F97316', icon: 'warning' };
    return { label: reasons[0] || 'Dışarı çıkmak için uygun değil', color: '#EF4444', icon: 'close-circle' };
  };

  const suitability = calculateSuitability();

  return (
    <View style={[styles.fullWidthCard, { backgroundColor: C.bgCard, borderColor: C.border }]}>
      <View style={styles.cardHeader}>
        <Icon name="sparkles-outline" size = { 16 } color = { C.accent } />
        <Text style={[styles.cardLabel, { color: C.textSecondary }]}>HAVAMANİA DURUM SKORU</Text>
      </View>
      <View style={styles.suitabilityRow}>
        <View style={[styles.suitabilityIconContainer, { backgroundColor: `${suitability.color}20` }]}>
          <Icon name={suitability.icon} size={24} color={suitability.color} />
        </View>
        <View>
          <Text style={[styles.suitabilityLabel, { color: C.text }]}>{suitability.label}</Text>
          <Text style={[styles.cardDesc, { color: C.textMuted }]}>Mevcut koşullara göre analiz edildi</Text>
        </View>
      </View>
    </View>
  );
};

interface WeatherDetailsPanelProps {
  current: CurrentWeatherData;
  todayDaily?: DailyWeatherItem;
  C: AppColors;
}

export const WeatherDetailsPanel: React.FC<WeatherDetailsPanelProps> = ({ current, todayDaily, C }) => {
  if (!current) return <Text style={{ color: C.text, padding: 20 }}>Veri yok</Text>;

  const getUVLabel = (uv: number) => {
    if (uv <= 2) return 'Düşük Risk';
    if (uv <= 5) return 'Orta Risk';
    if (uv <= 7) return 'Yüksek Risk';
    if (uv <= 10) return 'Çok Yüksek';
    return 'Ekstrem';
  };

  return (
    <View style={styles.container}>
      <Text style={[styles.sectionTitle, { color: C.text }]}>Hava Detayları</Text>

      <WeatherSuitabilityCard current={current} today={todayDaily} C={C} />

      <View style={styles.grid}>
        <WeatherDetailCard
          icon="thermometer-outline"
          label="Hissedilen"
          value={`${current.feels_like}°`}
          description={current.feels_like > current.temperature ? 'Daha sıcak hissediliyor' : 'Daha soğuk hissediliyor'}
          C={C}
          color="#F97316"
        />
        <WeatherDetailCard
          icon="water-outline"
          label="Nem"
          value={`%${current.humidity}`}
          description={`Çiy noktası ${current.dew_point}°`}
          C={C}
          color="#3B82F6"
        />

        <WeatherDetailCard
          icon="rainy-outline"
          label="Yağış"
          value={`${current.precipitation} mm`}
          description={`Olasılık: ${formatPrecipitationProbability(todayDaily?.precipitation_probability)}`}
          C={C}
          color="#60A5FA"
        />

        <WeatherDetailCard
          icon="air-outline"
          label="Rüzgar"
          value={`${current.wind_speed} km/sa`}
          description={`Hamle: ${current.wind_gusts} km/sa`}
          C={C}
          color="#94A3B8"
        />

        <WindDirectionCard current={current} C={C} />

        <WeatherDetailCard
          icon="sunny-outline"
          label="UV İndeksi"
          value={current.is_day ? current.uv_index : '--'}
          description={current.is_day ? getUVLabel(current.uv_index) : 'Gece vakti'}
          C={C}
          color="#FBBF24"
        />

        <WeatherDetailCard
          icon="eye-outline"
          label="Görüş"
          value={`${current.visibility} km`}
          description={current.visibility < 2 ? 'Kısıtlı görüş' : 'İyi görüş'}
          C={C}
          color={current.visibility < 2 ? '#EF4444' : '#10B981'}
        />

        <WeatherDetailCard
          icon="speedometer-outline"
          label="Basınç"
          value={`${current.pressure} hPa`}
          description="Deniz seviyesi"
          C={C}
          color="#8B5CF6"
        />

        <WeatherDetailCard
          icon="cloud-outline"
          label="Bulutluluk"
          value={`%${current.cloud_cover}`}
          description={current.cloud_cover > 70 ? 'Çok bulutlu' : 'Açık hava'}
          C={C}
          color="#64748B"
        />
      </View>

      <SunTimesCard today={todayDaily} C={C} />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: Spacing.md,
    paddingBottom: Spacing.xxl,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '800',
    marginBottom: Spacing.md,
    marginTop: Spacing.lg,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.md,
    marginBottom: Spacing.md,
  },
  card: {
    width: COLUMN_WIDTH,
    padding: Spacing.md,
    borderRadius: Radius.lg,
    borderWidth: 1,
    gap: 4,
    // Glass effect
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  fullWidthCard: {
    width: '100%',
    padding: Spacing.md,
    borderRadius: Radius.lg,
    borderWidth: 1,
    marginBottom: Spacing.md,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 8,
  },
  cardLabel: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1,
  },
  cardValue: {
    fontSize: 24,
    fontWeight: '700',
  },
  cardValueSmall: {
    fontSize: 20,
    fontWeight: '700',
  },
  cardDesc: {
    fontSize: 11,
    fontWeight: '500',
  },
  sunRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 8,
  },
  sunItem: {
    flex: 1,
    alignItems: 'center',
  },
  sunTime: {
    fontSize: 18,
    fontWeight: '700',
  },
  sunLabel: {
    fontSize: 10,
    fontWeight: '600',
    marginTop: 2,
  },
  sunDivider: {
    width: 1,
    height: 30,
    backgroundColor: 'rgba(128,128,128,0.2)',
  },
  windContent: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  compassContainer: {
    width: 50,
    height: 50,
    justifyContent: 'center',
    alignItems: 'center',
  },
  compassCircle: {
    width: 46,
    height: 46,
    borderRadius: 23,
    borderWidth: 2,
    justifyContent: 'center',
    alignItems: 'center',
  },
  compassNeedle: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  suitabilityRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 16,
    paddingTop: 4,
  },
  suitabilityIconContainer: {
    width: 48,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
  },
  suitabilityLabel: {
    fontSize: 16,
    fontWeight: '700',
  },
});
