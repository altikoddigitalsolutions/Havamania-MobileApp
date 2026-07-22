import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { Spacing, Radius, AppColors, FontSize } from '../theme';

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
    const interests = profile?.interests?.join(', ').toLowerCase() || "";
    const bio = profile?.bio?.toLowerCase() || "";
    const name = profile?.name || "";

    let summary = "";
    const sections: { icon: string, text: string, title: string, color: string }[] = [];

    // 1. Özet & Yaşam Etkisi
    if (isDay) {
        if (temp > 32) summary = `${name ? name + ', b' : 'B'}ugün ekstrem sıcaklıklar bekleniyor. Özellikle öğle saatlerinde gölgede kalmanı öneririm.`;
        else if (temp < 2) summary = "Hava oldukça dondurucu. Dışarı çıkarken kat kat giyinmen sağlığın için kritik.";
        else if (rain > 60) summary = "Kuvvetli yağış uyarısı! Bugün planlarını iç mekana kaydırmak mantıklı görünüyor.";
        else summary = `Hava ${temp}°C ve ${cond}. Günlük planların için genel olarak elverişli bir hava hakim.`;
    } else {
        summary = `Sakin bir gece bizi bekliyor. Sıcaklık ${temp}°C civarında seyrediyor.`;
    }

    // 2. Kıyafet Önerisi
    let clothingText = "";
    if (temp > 25) clothingText = "İnce, pamuklu ve açık renkli kıyafetler seçmelisin.";
    else if (temp > 15) clothingText = "Hafif bir sweatshirt veya ceket seni rahat ettirecektir.";
    else clothingText = "Kalın bir mont, gerekirse atkı ve bere almayı unutma.";

    if (rain > 30) clothingText += " Yanına şemsiye almayı unutma.";

    sections.push({ icon: 'shirt-outline', title: 'KIYAFET', text: clothingText, color: '#60A5FA' });

    // 3. Sağlık & Risk (Kişiselleştirilmiş)
    if (uv > 6 || bio.includes("uv") || bio.includes("cilt")) {
        sections.push({
          icon: 'sunny-outline',
          title: 'RİSK UYARISI',
          text: `UV seviyesi ${uv}. Güneş kremi ve gözlük kullanımı senin için çok önemli.`,
          color: '#F59E0B'
        });
    } else if (bio.includes("polen") && (cond.includes("güneş") || cond.includes("açık"))) {
        sections.push({
          icon: 'medical-outline',
          title: 'RİSK UYARISI',
          text: "Hava açık, polen seviyesi hassasiyetini tetikleyebilir. Dikkatli ol.",
          color: '#F59E0B'
        });
    }

    // 4. Aktivite Önerisi (Kişiselleştirilmiş)
    let activityText = "";
    if (interests.includes("spor") || interests.includes("koşu")) {
        if (temp > 10 && temp < 25 && rain < 20) activityText = "Koşu veya açık hava antrenmanı için harika bir hava!";
    } else if (interests.includes("fotoğraf")) {
        if (cond.includes("bulutlu") || cond.includes("parçalı")) activityText = "Yumuşak ışık çekimler için ideal, kameranı alıp çıkabilirsin.";
    } else if (interests.includes("doğa") || interests.includes("yürüyüş")) {
        if (temp > 15 && rain < 10) activityText = "Doğa yürüyüşü için parkurlar seni bekliyor.";
    }

    if (activityText) {
      sections.push({ icon: 'fitness-outline', title: 'AKTİVİTE', text: activityText, color: '#10B981' });
    }

    // 5. Seyahat Önerisi (Fallback if sections are few)
    if (sections.length < 3) {
      sections.push({
        icon: 'map-outline',
        title: 'SEYAHAT',
        text: "Hafta sonu yakın yerlere küçük bir kaçamak yapmak için hava oldukça uygun görünüyor.",
        color: '#8B5CF6'
      });
    }

    return { summary, sections: sections.slice(0, 3) };
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

      <View style={styles.sections}>
        {rec.sections.map((s, i) => (
          <View key={i} style={styles.sectionRow}>
            <View style={[styles.sectionIconBox, { backgroundColor: s.color + '15' }]}>
                <Icon name={s.icon} size={16} color={s.color} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={[styles.sectionTitle, { color: s.color }]}>{s.title}</Text>
              <Text style={[styles.sectionText, { color: C.textSecondary }]}>{s.text}</Text>
            </View>
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
    fontSize: 17,
    fontWeight: '800',
    lineHeight: 24,
    marginTop: 2,
  },
  sections: {
    gap: 16,
    marginTop: 8,
    marginBottom: 8,
  },
  sectionRow: {
    flexDirection: 'row',
    gap: 14,
    alignItems: 'flex-start',
  },
  sectionIconBox: {
    width: 36,
    height: 36,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 2,
  },
  sectionTitle: {
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 2,
  },
  sectionText: {
    fontSize: 14,
    fontWeight: '600',
    lineHeight: 20,
  },
  btn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    borderRadius: Radius.lg,
    marginTop: 4,
  },
  btnText: {
    color: '#FFF',
    fontSize: 15,
    fontWeight: '800',
  },
});
