import React from 'react';
import {Alert, Button, StyleSheet, Text, View} from 'react-native';
import {useMutation, useQueryClient} from '@tanstack/react-query';

import {initializeIAP, purchasePlan, restorePurchases} from '../services/iapService';
import {validateReceipt} from '../services/subscriptionApi';

const PLANS = [
  {code: 'premium_monthly', title: 'Premium Monthly', subtitle: 'Aylik plan'},
  {code: 'premium_yearly', title: 'Premium Yearly', subtitle: 'Yillik plan'},
];

export function PaywallScreen(): React.JSX.Element {
  const queryClient = useQueryClient();

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
      Alert.alert('Basarili', 'Premium aktif edildi.');
    },
    onError: () => Alert.alert('Hata', 'Satin alim basarisiz.'),
  });

  const restoreMutation = useMutation({
    mutationFn: async () => {
      const restored = await restorePurchases();
      if (!restored) {
        throw new Error('No purchases');
      }
      return validateReceipt({
        store: restored.store,
        receipt_data: restored.receiptData,
        plan_code: 'premium_monthly',
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({queryKey: ['subscription', 'status']});
      Alert.alert('Tamam', 'Satın alımlar geri yüklendi.');
    },
    onError: () => Alert.alert('Hata', 'Restore başarısız.'),
  });

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Go Premium</Text>
      {PLANS.map(plan => (
        <View style={styles.planCard} key={plan.code}>
          <Text style={styles.planTitle}>{plan.title}</Text>
          <Text style={styles.planSubtitle}>{plan.subtitle}</Text>
          <Button
            title={purchaseMutation.isPending ? 'Isleniyor...' : 'Satin Al'}
            onPress={() => purchaseMutation.mutate(plan.code)}
          />
        </View>
      ))}

      <Button
        title={restoreMutation.isPending ? 'Geri Yukleniyor...' : 'Restore Purchases'}
        onPress={() => restoreMutation.mutate()}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 16},
  title: {fontSize: 24, fontWeight: '700'},
  planCard: {borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 10, padding: 12, gap: 8},
  planTitle: {fontSize: 18, fontWeight: '700'},
  planSubtitle: {color: '#4b5563'},
});
