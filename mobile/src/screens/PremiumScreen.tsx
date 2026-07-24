import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  Dimensions,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useNavigation } from '@react-navigation/native';
import { useColors, Spacing, Radius, FontSize, AppColors } from '../theme';

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

const { width } = Dimensions.get('window');

const PREMIUM_FEATURES = [
  { icon: 'airplane-outline', title: 'Gelişmiş Seyahat Analizi', desc: '15 günlük detaylı hava analizi ve risk raporu.' },
  { icon: 'stats-chart-outline', title: 'Tahmin Karşılaştırması', desc: 'Önceki tahminlerle güncel durumu kıyasla.' },
  { icon: 'notifications-outline', title: 'Akıllı Bildirimler', desc: 'Yağış ve UV değişimlerinde anlık uyarılar.' },
  { icon: 'color-palette-outline', title: 'Atmosferik Görseller', desc: 'Hava durumuna göre değişen dinamik temalar.' },
  { icon: 'briefcase-outline', title: 'AI Destekli Valiz Önerisi', desc: 'Gideceğin yere ve havaya göre otomatik liste.' },
  { icon: 'sunny-outline', title: 'Detaylı UV / Rüzgar / Yağış', desc: 'Saatlik ekstrem hava koşulları analizi.' },
  { icon: 'time-outline', title: 'Sınırsız AI Geçmişi', desc: 'Tüm asistan konuşmalarını kaydedin ve erişin.' },
  { icon: 'sparkles-outline', title: 'Kişiselleştirilmiş Öneriler', desc: 'Profiline ve ilgi alanlarına özel günlük plan.' },
];

export function PremiumScreen() {
  const navigation = useNavigation();
  const C = useColors();
  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle="light-content" />
      <ScrollView showsVerticalScrollIndicator={false}>

        {/* Header Section */}
        <LinearGradient colors={[C.accent, C.accentDark]} style={s.header}>
          <TouchableOpacity style={s.backBtn} onPress={() => navigation.goBack()}>
            <Icon name="close" size={28} color="#FFF" />
          </TouchableOpacity>

          <View style={s.headerContent}>
            <View style={s.crownIcon}>
              <Icon name="ribbon" size={40} color="#FFF" />
            </View>
            <Text style={s.headerTitle}>HAVAMANİA PREMIUM</Text>
            <Text style={s.headerSubtitle}>Sınırsız hava analizi ve AI asistan deneyimini başlatın</Text>
          </View>
        </LinearGradient>

        {/* Features List */}
        <View style={s.featuresContainer}>
          {PREMIUM_FEATURES.map((f, i) => (
            <View key={i} style={s.featureRow}>
              <View style={[s.featureIconBox, { backgroundColor: C.accent + '15' }]}>
                <Icon name={f.icon} size={22} color={C.accent} />
              </View>
              <View style={s.featureText}>
                <Text style={[s.featureTitle, { color: C.text }]}>{f.title}</Text>
                <Text style={[s.featureDesc, { color: C.textSecondary }]}>{f.desc}</Text>
              </View>
            </View>
          ))}
        </View>

        {/* Pricing Card */}
        <View style={s.pricingCard}>
          <LinearGradient colors={['rgba(255,255,255,0.05)', 'rgba(255,255,255,0.01)']} style={s.pricingInner}>
             <View style={s.pricingHeader}>
               <Text style={[s.planTitle, { color: C.text }]}>Yıllık Üyelik</Text>
               <View style={s.saveBadge}><Text style={s.saveText}>%40 TASARRUF</Text></View>
             </View>
             <View style={s.priceRow}>
               <Text style={[s.price, { color: C.text }]}>₺199,99</Text>
               <Text style={[s.pricePeriod, { color: C.textMuted }]}>/ yıl</Text>
             </View>
             <Text style={[s.priceSub, { color: C.textMuted }]}>Ayda sadece ₺16,66</Text>
          </LinearGradient>
        </View>

        <TouchableOpacity style={s.buyBtn} activeOpacity={0.8}>
           <LinearGradient colors={[C.accent, C.accentDark]} style={StyleSheet.absoluteFill} start={{x:0, y:0}} end={{x:1, y:0}} />
           <Text style={s.buyBtnText}>Hemen Başlat</Text>
        </TouchableOpacity>

        <Text style={s.footerNote}>İstediğiniz zaman iptal edebilirsiniz. Ödemeler Google Play hesabınız üzerinden tahsil edilir.</Text>
        <View style={{ height: 40 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const makeStyles = (C: AppColors) => StyleSheet.create({
  safe: { flex: 1, backgroundColor: C.bg },
  header: {
    paddingTop: 40,
    paddingBottom: 60,
    borderBottomLeftRadius: 40,
    borderBottomRightRadius: 40,
    alignItems: 'center',
    paddingHorizontal: Spacing.xl,
  },
  backBtn: { position: 'absolute', top: 20, left: 20, width: 44, height: 44, justifyContent: 'center' },
  headerContent: { alignItems: 'center', marginTop: 20 },
  crownIcon: { width: 80, height: 80, borderRadius: 40, backgroundColor: 'rgba(255,255,255,0.2)', justifyContent: 'center', alignItems: 'center', marginBottom: 20 },
  headerTitle: { fontSize: 24, fontWeight: '900', color: '#FFF', letterSpacing: 2 },
  headerSubtitle: { fontSize: 15, color: 'rgba(255,255,255,0.8)', textAlign: 'center', marginTop: 8, lineHeight: 22 },
  featuresContainer: { padding: Spacing.xl, gap: 24 },
  featureRow: { flexDirection: 'row', gap: 16, alignItems: 'center' },
  featureIconBox: { width: 48, height: 48, borderRadius: 14, justifyContent: 'center', alignItems: 'center' },
  featureText: { flex: 1 },
  featureTitle: { fontSize: 16, fontWeight: '700' },
  featureDesc: { fontSize: 13, marginTop: 2 },
  pricingCard: { marginHorizontal: Spacing.xl, marginBottom: 20, borderRadius: 24, overflow: 'hidden', borderWidth: 1, borderColor: C.border },
  pricingInner: { padding: 24 },
  pricingHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  planTitle: { fontSize: 18, fontWeight: '800' },
  saveBadge: { backgroundColor: '#10B981', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 6 },
  saveText: { color: '#FFF', fontSize: 10, fontWeight: '900' },
  priceRow: { flexDirection: 'row', alignItems: 'baseline', marginTop: 12 },
  price: { fontSize: 32, fontWeight: '900' },
  pricePeriod: { fontSize: 16, fontWeight: '600', marginLeft: 4 },
  priceSub: { fontSize: 12, marginTop: 4 },
  buyBtn: { marginHorizontal: Spacing.xl, height: 64, borderRadius: 20, justifyContent: 'center', alignItems: 'center', overflow: 'hidden', shadowColor: C.accent, shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.3, shadowRadius: 15, elevation: 8 },
  buyBtnText: { color: '#FFF', fontSize: 18, fontWeight: '800' },
  footerNote: { textAlign: 'center', fontSize: 11, color: C.textMuted, marginHorizontal: 40, marginTop: 20, lineHeight: 16 },
});
