import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { Spacing, Radius, FontSize, AppColors } from '../theme';

interface HavamaniaRecommendationCardProps {
  weather: any;
  onPress: (q: string) => void;
  C: AppColors;
}

export const HavamaniaRecommendationCard: React.FC<HavamaniaRecommendationCardProps> = ({ weather, onPress, C }) => {
  if (!weather) return null;

  const generateRecommendation = () => {
    const temp = weather.temperature;
    const cond = weather.description.toLowerCase();
    const rain = weather.precipitation_probability ?? 0;
    const wind = weather.wind_speed ?? 0;
    const uv = weather.uv_index ?? 0;

    let summary = `Bugün hava ${cond} ve ${temp}°C.`;
    const points = [];

    if (rain > 40) {
      points.push("Yağış riski var, şemsiye almalısın.");
    } else if (temp > 22 && !cond.includes('yağmur')) {
      points.push("Açık hava aktiviteleri için uygun.");
    }

    if (uv > 5) {
      points.push(`UV ${uv}: güneş kremi önerilir.`);
    }

    if (wind > 20) {
      points.push(`Rüzgar ${wind} km/sa: esintili olabilir.`);
    } else {
      points.push("Hava sakin, yürüyüş için ideal.");
    }

    return { summary, points: points.slice(0, 3) };
  };

  const rec = generateRecommendation();

  return (
    <View style={[styles.container, { backgroundColor: 'rgba(6, 182, 212, 0.08)', borderColor: 'rgba(6, 182, 212, 0.2)' }]}>
      <View style={styles.header}>
        <View style={[styles.iconBox, { backgroundColor: C.accent + '20' }]}>
           <Icon name="sparkles" size={16} color={C.accent} />
        </View>
        <Text style={[styles.headerText, { color: C.accent }]}>HAVAMANIA ÖNERİSİ</Text>
      </View>

      <Text style={[styles.summary, { color: C.text }]}>{rec.summary}</Text>

      <View style={styles.points}>
        {rec.points.map((p, i) => (
          <View key={i} style={styles.pointRow}>
            <Text style={{ color: C.accent, fontSize: 16 }}>•</Text>
            <Text style={[styles.pointText, { color: C.textSecondary }]}>{p}</Text>
          </View>
        ))}
      </View>

      <TouchableOpacity
        style={[styles.btn, { backgroundColor: C.accent }]}
        onPress={() => onPress("Bugünkü hava durumu için detaylı analiz yapar mısın?")}
      >
        <Text style={styles.btnText}>Detaylı Analiz İste</Text>
        <Icon name="chevron-forward" size={14} color="#FFF" />
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.md,
    padding: Spacing.lg,
    borderRadius: Radius.xl,
    borderWidth: 1,
    gap: 12,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  iconBox: {
    width: 28,
    height: 28,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerText: {
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1.5,
  },
  summary: {
    fontSize: 16,
    fontWeight: '700',
    lineHeight: 22,
  },
  points: {
    gap: 6,
  },
  pointRow: {
    flexDirection: 'row',
    gap: 8,
    alignItems: 'flex-start',
  },
  pointText: {
    fontSize: 13,
    fontWeight: '600',
    lineHeight: 18,
    flex: 1,
  },
  btn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 12,
    borderRadius: Radius.lg,
    marginTop: 8,
  },
  btnText: {
    color: '#FFF',
    fontSize: 14,
    fontWeight: '800',
  },
});
