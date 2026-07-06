import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { Spacing, Radius, AppColors } from '../theme';

interface HavamaniaRecommendationCardProps {
  weather: any;
  profile?: any;
  onPress: (q: string) => void;
  C: AppColors;
}

export const HavamaniaRecommendationCard: React.FC<HavamaniaRecommendationCardProps> = ({ weather, profile, onPress, C }) => {
  if (!weather) return null;

  const generateRecommendation = () => {
    const temp = weather.temperature;
    const cond = weather.description.toLowerCase();
    const rain = weather.precipitation_probability ?? 0;
    const wind = weather.wind_speed ?? 0;
    const uv = weather.uv_index ?? 0;
    const isDay = weather.is_day;

    // Profil bazlı veriler
    const interests = profile?.interest?.toLowerCase() || "";
    const health = profile?.health_sensitivities?.toLowerCase() || "";

    let summary = "";
    const points: { icon: string, text: string, type: string }[] = [];

    // 1. Özet & Yaşam Etkisi
    if (isDay) {
        if (temp > 32) summary = "Bugün ekstrem sıcaklıklar bekleniyor. Özellikle öğle saatlerinde gölgede kalmanı öneririm.";
        else if (temp < 2) summary = "Hava oldukça dondurucu. Dışarı çıkarken kat kat giyinmen sağlığın için kritik.";
        else if (rain > 60) summary = "Kuvvetli yağış uyarısı! Bugün planlarını iç mekana kaydırmak mantıklı görünüyor.";
        else summary = `Hava ${temp}°C ve ${cond}. Günlük planların için genel olarak elverişli bir hava hakim.`;
    } else {
        summary = `Sakin bir gece bizi bekliyor. Sıcaklık ${temp}°C civarında seyrediyor.`;
    }

    // 2. Kıyafet Önerisi
    if (temp > 25) {
        points.push({ icon: 'shirt-outline', text: "İnce, pamuklu ve açık renkli kıyafetler seçmelisin.", type: 'clothing' });
    } else if (temp > 15) {
        points.push({ icon: 'shirt-outline', text: "Hafif bir sweatshirt veya ceket seni rahat ettirecektir.", type: 'clothing' });
    } else {
        points.push({ icon: 'shirt-outline', text: "Kalın bir mont, gerekirse atkı ve bere almayı unutma.", type: 'clothing' });
    }

    // 3. Sağlık & Risk (Kişiselleştirilmiş)
    if (uv > 6 || health.includes("uv")) {
        points.push({ icon: 'sunny-outline', text: `UV seviyesi ${uv}. Güneş kremi ve gözlük kullanımı senin için çok önemli.`, type: 'risk' });
    }
    if (health.includes("polen") && (cond.includes("güneş") || cond.includes("açık"))) {
        points.push({ icon: 'medical-outline', text: "Hava açık, polen seviyesi hassasiyetini tetikleyebilir. Dikkatli ol.", type: 'risk' });
    }
    if (rain > 40) {
        points.push({ icon: 'umbrella-outline', text: "Yağış ihtimaline karşı şemsiyeni veya yağmurluğunu hazırda tut.", type: 'risk' });
    }

    // 4. Aktivite Önerisi (Kişiselleştirilmiş)
    if (interests.includes("spor") || interests.includes("koşu")) {
        if (temp > 10 && temp < 25 && rain < 20) {
            points.push({ icon: 'fitness-outline', text: "Koşu veya açık hava antrenmanı için harika bir hava!", type: 'activity' });
        }
    }
    if (interests.includes("fotoğraf")) {
        if (cond.includes("bulutlu") || cond.includes("parçalı")) {
            points.push({ icon: 'camera-outline', text: "Yumuşak ışık çekimler için ideal, kameranı alıp dışarı çıkabilirsin.", type: 'activity' });
        }
    }
    if (interests.includes("doğa") || interests.includes("yürüyüş")) {
        if (temp > 15 && rain < 10) {
            points.push({ icon: 'leaf-outline', text: "Doğa yürüyüşü için parkurlar seni bekliyor.", type: 'activity' });
        }
    }

    // Fallback point if too few
    if (points.length < 2) {
        points.push({ icon: 'bulb-outline', text: "Günün tadını çıkarmak için planlarını hava durumuna göre güncelle.", type: 'general' });
    }

    return { summary, points: points.slice(0, 4) };
  };

  const rec = generateRecommendation();

  return (
    <View style={[styles.container, { backgroundColor: C.bgCard, borderColor: C.border }]}>
      <View style={styles.header}>
        <View style={[styles.iconBox, { backgroundColor: C.accent + '15' }]}>
           <Icon name="sparkles" size={16} color={C.accent} />
        </View>
        <Text style={[styles.headerText, { color: C.accent }]}>HAVAMANIA AI ANALİZİ</Text>
      </View>

      <Text style={[styles.summary, { color: C.text }]}>{rec.summary}</Text>

      <View style={styles.points}>
        {rec.points.map((p, i) => (
          <View key={i} style={styles.pointRow}>
            <View style={[styles.pointIconBox, { backgroundColor: C.divider }]}>
                <Icon name={p.icon} size={14} color={C.textSecondary} />
            </View>
            <Text style={[styles.pointText, { color: C.textSecondary }]}>{p.text}</Text>
          </View>
        ))}
      </View>

      <TouchableOpacity
        activeOpacity={0.8}
        style={[styles.btn, { backgroundColor: C.accent }]}
        onPress={() => onPress("Hava durumuna göre profilimi dikkate alarak detaylı bir gün planı oluşturur musun?")}
      >
        <Text style={styles.btnText}>Detaylı Plan Oluştur</Text>
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
    fontSize: 16,
    fontWeight: '800',
    lineHeight: 22,
    marginTop: 2,
  },
  points: {
    gap: 10,
    marginTop: 4,
  },
  pointRow: {
    flexDirection: 'row',
    gap: 10,
    alignItems: 'center',
  },
  pointIconBox: {
    width: 24,
    height: 24,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
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
