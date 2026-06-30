import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { Spacing, Radius, AppColors } from '../theme';

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
    const hour = new Date().getHours();

    let summary = "";
    const points = [];

    // --- Dynamic Summaries ---
    if (temp > 32) summary = "Bugün ekstrem sıcaklıklar bekleniyor, gölgede kalmaya çalış.";
    else if (temp < 2) summary = "Hava buz kesiyor! Dışarı çıkarken en kalın giysilerini seç.";
    else if (rain > 60) summary = "Kuvvetli yağış uyarısı! Bugün iç mekan aktiviteleri daha mantıklı.";
    else if (uv > 8) summary = "Çok yüksek UV indeksi! Güneş koruyucu olmadan dışarı çıkma.";
    else if (cond.includes("güneş")) summary = "Işıl ışıl bir gün! Enerjini güneşten alabilirsin.";
    else if (cond.includes("bulut")) summary = "Gökyüzü biraz mahzun ama hava aktiviteler için hala uygun.";
    else summary = `Bugün hava ${weather.description} ve ${temp}°C civarında seyrediyor.`;

    // --- Business Rules Context (Daily Life Focus) ---
    if (uv > 5 && hour >= 10 && hour <= 17) {
      points.push("Güneş gözlüğü ve koruyucu krem kullanımını ihmal etme.");
    }

    if (rain > 20 && rain <= 50) {
      points.push("Hafif yağış geçişleri olabilir, yanına ince bir şemsiye al.");
    } else if (rain > 50) {
      points.push("Su geçirmeyen ayakkabılar ve sağlam bir şemsiye şart.");
    }

    if (wind > 25) {
      points.push("Sert rüzgar var, açık alanlarda şapka ve uçuşan eşyalara dikkat.");
    }

    if (temp >= 17 && temp <= 24 && rain < 15) {
      points.push("Egzersiz veya park yürüyüşü için ideal şartlar mevcut.");
    }

    if (temp < 12 && temp > 5) {
      points.push("Hava biraz serin, bir hırka veya hafif ceket seni koruyacaktır.");
    }

    if (cond.includes("sis")) {
      points.push("Puslu hava görüşü kısıtlıyor, ulaşımda ekstra zaman ayır.");
    }

    // Fallback logic to ensure at least 2 points
    if (points.length < 2) {
        if (temp > 22) points.push("Sıvı tüketimini artırmayı ve hafif beslenmeyi unutma.");
        else points.push("Günün tadını çıkarmak için hava durumuna uygun plan yap.");
    }

    return { summary, points: points.slice(0, 3) };
  };

  const rec = generateRecommendation();

  return (
    <View style={[styles.container, { backgroundColor: C.bgCard, borderColor: C.border }]}>
      <View style={styles.header}>
        <View style={[styles.iconBox, { backgroundColor: C.accent + '15' }]}>
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
        activeOpacity={0.8}
        style={[styles.btn, { backgroundColor: C.accent }]}
        onPress={() => onPress("Bugünkü hava durumu için detaylı analiz yapar mısın?")}
      >
        <Text style={styles.btnText}>Yapay Zekaya Sor</Text>
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
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 2,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  iconBox: {
    width: 32,
    height: 32,
    borderRadius: 10,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerText: {
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.5,
  },
  summary: {
    fontSize: 17,
    fontWeight: '800',
    lineHeight: 24,
    marginTop: 2,
  },
  points: {
    gap: 8,
    marginTop: 4,
  },
  pointRow: {
    flexDirection: 'row',
    gap: 10,
    alignItems: 'flex-start',
  },
  pointText: {
    fontSize: 14,
    fontWeight: '600',
    lineHeight: 20,
    flex: 1,
  },
  btn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    borderRadius: Radius.lg,
    marginTop: 10,
  },
  btnText: {
    color: '#FFF',
    fontSize: 15,
    fontWeight: '800',
  },
});
