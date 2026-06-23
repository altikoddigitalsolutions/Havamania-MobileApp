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
} from 'react-native';
import {useMutation, useQueryClient} from '@tanstack/react-query';
import Icon from 'react-native-vector-icons/Ionicons';

import {initializeIAP, purchasePlan, restorePurchases} from '../services/iapService';
import {validateReceipt} from '../services/subscriptionApi';
import {useColors, AppColors, Spacing} from '../theme';

const PREMIUM_FEATURES = [
  {icon: 'airplane', title: 'Gelişmiş Seyahat Analizi', desc: '15 günlük detaylı hava ve aktivite raporu.'},
  {icon: 'briefcase', title: 'AI Valiz Önerisi', desc: 'Hava durumuna göre otomatik hazırlanan ihtiyaç listesi.'},
  {icon: 'notifications', title: 'Akıllı Bildirimler', desc: 'Anlık yağış ve UV değişim uyarıları.'},
  {icon: 'images', title: 'Atmosferik Görseller', desc: 'Hava durumuna göre değişen dinamik tema.'},
  {icon: 'analytics', title: 'Değişim Karşılaştırması', desc: 'Tahminlerdeki güncellemeleri takip edin.'},
  {icon: 'chatbubbles', title: 'Sınırsız AI Asistan', desc: 'Gelişmiş kişiselleştirilmiş hava tavsiyeleri.'},
];

const PLANS = [
  {code: 'premium_monthly', title: 'Aylık Premium', price: '₺29.99', subtitle: 'Her ay yenilenir'},
  {code: 'premium_yearly', title: 'Yıllık Premium', price: '₺249.99', subtitle: 'Yıllık öde, %30 tasarruf et', popular: true},
];

export function PaywallScreen(): React.JSX.Element {
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
    },
    onError: () => Alert.alert('Hata', 'Satın alım işlemi şu an gerçekleştirilemiyor.'),
  });

  const restoreMutation = useMutation({
    mutationFn: async () => {
      const restored = await restorePurchases();
      if (!restored) throw new Error('No purchases');
      return validateReceipt({
        store: restored.store,
        receipt_data: restored.receiptData,
        plan_code: 'premium_monthly',
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({queryKey: ['subscription', 'status']});
      Alert.alert('Başarılı', 'Satın alımlarınız geri yüklendi.');
    },
    onError: () => Alert.alert('Bilgi', 'Aktif bir abonelik bulunamadı.'),
  });

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle="light-content" />
      <ScrollView contentContainerStyle={s.scroll}>
        <View style={s.header}>
          <Text style={s.title}>Havamania Premium</Text>
          <Text style={s.subtitle}>Hava durumunu bir asistana dönüştürün.</Text>
        </View>

        <View style={s.featuresContainer}>
          {PREMIUM_FEATURES.map((f, i) => (
            <View key={i} style={s.featureRow}>
              <View style={s.iconBox}>
                <Icon name={f.icon} size={20} color="#F59E0B" />
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
                <View style={s.popularBadge}>
                  <Text style={s.popularText}>EN POPÜLER</Text>
                </View>
              )}
              <View style={s.planHeader}>
                <Text style={s.planTitle}>{plan.title}</Text>
                <Text style={s.planPrice}>{plan.price}</Text>
              </View>
              <Text style={s.planSubtitle}>{plan.subtitle}</Text>
            </TouchableOpacity>
          ))}
        </View>

        <TouchableOpacity style={s.restoreBtn} onPress={() => restoreMutation.mutate()}>
          <Text style={s.restoreText}>Satın Alımları Geri Yükle</Text>
        </TouchableOpacity>

        <View style={s.footer}>
          <Text style={s.footerText}>
            Aboneliğinizi dilediğiniz zaman iptal edebilirsiniz. Ödeme yapmadan önce{' '}
            <Text style={s.link}>Kullanım Şartları</Text> ve{' '}
            <Text style={s.link}>Gizlilik Politikası</Text>'nı inceleyin.
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const makeStyles = (C: AppColors) => StyleSheet.create({
  safe: {flex: 1, backgroundColor: '#111827'},
  scroll: {padding: 24},
  header: {alignItems: 'center', marginBottom: 32, marginTop: 16},
  title: {fontSize: 28, fontWeight: '900', color: '#FFF', marginBottom: 8},
  subtitle: {fontSize: 16, color: '#9CA3AF', textAlign: 'center'},
  featuresContainer: {marginBottom: 40},
  featureRow: {flexDirection: 'row', alignItems: 'center', marginBottom: 20, gap: 16},
  iconBox: {width: 40, height: 40, borderRadius: 12, backgroundColor: 'rgba(245, 158, 11, 0.1)', justifyContent: 'center', alignItems: 'center'},
  featureText: {flex: 1},
  featureTitle: {fontSize: 16, fontWeight: '700', color: '#FFF', marginBottom: 2},
  featureDesc: {fontSize: 13, color: '#9CA3AF'},
  plansContainer: {gap: 16, marginBottom: 24},
  planCard: {
    backgroundColor: '#1F2937',
    borderRadius: 16,
    padding: 20,
    borderWidth: 2,
    borderColor: '#374151',
  },
  popularCard: {borderColor: '#F59E0B'},
  popularBadge: {
    position: 'absolute',
    top: -12,
    right: 20,
    backgroundColor: '#F59E0B',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  popularText: {fontSize: 10, fontWeight: '900', color: '#FFF'},
  planHeader: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4},
  planTitle: {fontSize: 18, fontWeight: '800', color: '#FFF'},
  planPrice: {fontSize: 18, fontWeight: '800', color: '#FFF'},
  planSubtitle: {fontSize: 13, color: '#9CA3AF'},
  restoreBtn: {alignItems: 'center', padding: 12},
  restoreText: {color: '#9CA3AF', fontSize: 14, textDecorationLine: 'underline'},
  footer: {marginTop: 24},
  footerText: {fontSize: 12, color: '#6B7280', textAlign: 'center', lineHeight: 18},
  link: {textDecorationLine: 'underline', color: '#9CA3AF'},
});
