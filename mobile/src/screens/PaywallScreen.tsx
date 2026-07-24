import React from 'react';
import {
  Alert,
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  Dimensions,
  Platform,
} from 'react-native';
import {useMutation, useQueryClient} from '@tanstack/react-query';
import Icon from 'react-native-vector-icons/Ionicons';
import LinearGradient from 'react-native-linear-gradient';

import {initializeIAP, purchasePlan, restorePurchases} from '../services/iapService';
import {validateReceipt} from '../services/subscriptionApi';
import {useColors, AppColors, Spacing, Radius} from '../theme';

const {width, height} = Dimensions.get('window');

const PREMIUM_FEATURES = [
  {icon: 'airplane-outline', title: 'Gelişmiş Seyahat Analizi', desc: '10 günlük detaylı hava, aktivite ve risk raporu.'},
  {icon: 'analytics-outline', title: 'Tahmin Karşılaştırması', desc: 'Hava durumundaki en ufak değişimleri anında takip edin.'},
  {icon: 'notifications-outline', title: 'Akıllı Bildirimler', desc: 'Kritik hava olayları için size özel kişiselleştirilmiş uyarılar.'},
  {icon: 'briefcase-outline', title: 'AI Valiz & Giyim Önerisi', desc: 'Gideceğiniz yere göre otomatik hazırlanan ihtiyaç listesi.'},
  {icon: 'sparkles-outline', title: 'Sınırsız AI Deneyimi', desc: 'Asistanınızla sınırsız sohbet ve yaşam tarzı danışmanlığı.'},
  {icon: 'color-palette-outline', title: 'Atmosferik Temalar', desc: 'Hava durumuna göre dinamik olarak değişen özel arayüz.'},
];

const PLANS = [
  {code: 'premium_monthly', title: 'Aylık Üyelik', price: '₺39.99', subtitle: 'Esnek plan, dilediğin zaman iptal et.'},
  {code: 'premium_yearly', title: 'Yıllık Üyelik', price: '₺299.99', subtitle: 'Yıllık ödeme ile %35 tasarruf edin.', popular: true},
];

export function PaywallScreen({navigation}: any): React.JSX.Element {
  const queryClient = useQueryClient();
  const C = useColors();
  const s = makeStyles(C);

  React.useEffect(() => {
    void initializeIAP();
  }, []);

  const purchaseMutation = useMutation({
    mutationFn: async (planCode: string) => {
      const purchase = await purchasePlan(planCode);
      return validateReceipt({store: purchase.store, receipt_data: purchase.receiptData, plan_code: planCode});
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({queryKey: ['subscription', 'status']});
      Alert.alert('Hoş Geldin!', 'Havamania Premium artık aktif.');
      navigation.goBack();
    },
    onError: () => Alert.alert('Hata', 'Satın alım işlemi şu an gerçekleştirilemiyor.'),
  });

  return (
    <View style={s.container}>
      <StatusBar barStyle="light-content" />
      <LinearGradient colors={['#0F172A', '#1E293B', '#0F172A']} style={StyleSheet.absoluteFill} />

      <SafeAreaView style={{flex: 1}}>
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>

          <TouchableOpacity style={s.closeIcon} onPress={() => navigation.goBack()}>
            <Icon name="close" size={24} color="#94A3B8" />
          </TouchableOpacity>

          <View style={s.header}>
            <View style={s.badgeContainer}>
              <Icon name="sparkles" size={32} color="#F59E0B" />
            </View>
            <Text style={s.title}>HAVAMANİA</Text>
            <View style={s.proTag}><Text style={s.proTagText}>PREMIUM</Text></View>
            <Text style={s.subtitle}>Hava durumunu kişisel bir yaşam asistanına dönüştürün.</Text>
          </View>

          <View style={s.featuresContainer}>
            {PREMIUM_FEATURES.map((f, i) => (
              <View key={i} style={s.featureRow}>
                <View style={s.iconBox}>
                  <Icon name={f.icon} size={22} color="#F59E0B" />
                </View>
                <View style={s.featureText}>
                  <Text style={s.featureTitle}>{f.title}</Text>
                  <Text style={s.featureDesc}>{f.desc}</Text>
                </View>
              </View>
            ))}
          </View>

          <View style={s.plansContainer}>
            {PLANS.map(plan => (
              <TouchableOpacity
                key={plan.code}
                style={[s.planCard, plan.popular && s.popularCard]}
                onPress={() => purchaseMutation.mutate(plan.code)}
                disabled={purchaseMutation.isPending}
              >
                {plan.popular && (
                  <LinearGradient
                    colors={['#F59E0B', '#D97706']}
                    start={{x:0, y:0}} end={{x:1, y:0}}
                    style={s.popularBadge}
                  >
                    <Text style={s.popularText}>EN POPÜLER</Text>
                  </LinearGradient>
                )}
                <View style={s.planHeader}>
                  <View>
                    <Text style={s.planTitle}>{plan.title}</Text>
                    <Text style={s.planSubtitle}>{plan.subtitle}</Text>
                  </View>
                  <Text style={s.planPrice}>{plan.price}</Text>
                </View>
              </TouchableOpacity>
            ))}
          </View>

          <TouchableOpacity
            style={s.restoreBtn}
            onPress={() => restorePurchases().then(() => Alert.alert('Başarılı', 'Satın alımlarınız geri yüklendi.'))}
          >
            <Text style={s.restoreText}>Satın Alımları Geri Yükle</Text>
          </TouchableOpacity>

          <View style={s.footer}>
            <Text style={s.footerText}>
              Aboneliğinizi dilediğiniz zaman App Store ayarlarından iptal edebilirsiniz. Ödeme yaparak{' '}
              <Text style={s.link}>Kullanım Şartları</Text> ve{' '}
              <Text style={s.link}>Gizlilik Politikası</Text>'nı kabul etmiş sayılırsınız.
            </Text>
          </View>
        </ScrollView>
      </SafeAreaView>
    </View>
  );
}

const makeStyles = (C: AppColors) => StyleSheet.create({
  container: {flex: 1},
  scroll: {padding: 24, paddingBottom: 60},
  closeIcon: {alignSelf: 'flex-end', padding: 8, marginBottom: 10},
  header: {alignItems: 'center', marginBottom: 40},
  badgeContainer: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: 'rgba(245, 158, 11, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
    borderWidth: 1,
    borderColor: 'rgba(245, 158, 11, 0.3)',
  },
  title: {fontSize: 34, fontWeight: '900', color: '#FFF', letterSpacing: 2},
  proTag: {
    backgroundColor: '#F59E0B',
    paddingHorizontal: 12,
    paddingVertical: 2,
    borderRadius: 6,
    marginTop: -4,
    marginBottom: 16
  },
  proTagText: {fontSize: 12, fontWeight: '900', color: '#000', letterSpacing: 1},
  subtitle: {fontSize: 16, color: '#94A3B8', textAlign: 'center', lineHeight: 24, paddingHorizontal: 10},
  featuresContainer: {
    backgroundColor: 'rgba(255,255,255,0.03)',
    borderRadius: 32,
    padding: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.05)',
    marginBottom: 40
  },
  featureRow: {flexDirection: 'row', alignItems: 'center', marginBottom: 20, gap: 16},
  iconBox: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: 'rgba(245, 158, 11, 0.15)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  featureText: {flex: 1},
  featureTitle: {fontSize: 16, fontWeight: '800', color: '#FFF', marginBottom: 2},
  featureDesc: {fontSize: 13, color: '#94A3B8', lineHeight: 18},
  plansContainer: {gap: 16, marginBottom: 24},
  planCard: {
    backgroundColor: 'rgba(255,255,255,0.05)',
    borderRadius: 24,
    padding: 24,
    borderWidth: 1.5,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  popularCard: {borderColor: '#F59E0B', backgroundColor: 'rgba(245, 158, 11, 0.08)'},
  popularBadge: {
    position: 'absolute',
    top: -12,
    right: 24,
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 8,
  },
  popularText: {fontSize: 10, fontWeight: '900', color: '#000'},
  planHeader: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'},
  planTitle: {fontSize: 18, fontWeight: '900', color: '#FFF', marginBottom: 4},
  planPrice: {fontSize: 22, fontWeight: '900', color: '#FFF'},
  planSubtitle: {fontSize: 13, color: '#94A3B8', maxWidth: '75%'},
  restoreBtn: {alignItems: 'center', padding: 12},
  restoreText: {color: '#64748B', fontSize: 13, textDecorationLine: 'underline'},
  footer: {marginTop: 32},
  footerText: {fontSize: 11, color: '#475569', textAlign: 'center', lineHeight: 16},
  link: {textDecorationLine: 'underline', color: '#64748B'},
});
