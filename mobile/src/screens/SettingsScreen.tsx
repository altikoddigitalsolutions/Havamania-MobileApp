import React from 'react';
import {Button, StyleSheet, Switch, Text, View} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {SettingsStackParamList} from '../navigation/SettingsStack';
import {
  getNotificationPreferences,
  getProfile,
  updateNotificationPreferences,
  updateProfile,
} from '../services/profileApi';
import {getSubscriptionStatus} from '../services/subscriptionApi';
import {useAuthStore} from '../store/authStore';
import {useThemeStore} from '../store/themeStore';

type Props = NativeStackScreenProps<SettingsStackParamList, 'SettingsHome'>;

export function SettingsScreen({navigation}: Props): React.JSX.Element {
  const queryClient = useQueryClient();
  const logoutCurrentUser = useAuthStore(state => state.logoutCurrentUser);
  const {theme, setTheme} = useThemeStore();

  const profileQuery = useQuery({queryKey: ['profile'], queryFn: getProfile});
  const prefsQuery = useQuery({queryKey: ['profile', 'notifications'], queryFn: getNotificationPreferences});
  const subQuery = useQuery({queryKey: ['subscription', 'status'], queryFn: getSubscriptionStatus});

  const updateProfileMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile']}),
  });

  const updatePrefsMutation = useMutation({
    mutationFn: updateNotificationPreferences,
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile', 'notifications']}),
  });

  const profile = profileQuery.data;
  const prefs = prefsQuery.data;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Settings</Text>
      <Text>User: {profile?.user_id ?? '-'}</Text>
      <Text>
        Plan: {subQuery.data?.plan_code ?? 'free'} {subQuery.data?.is_premium_active ? '(Premium)' : '(Free)'}
      </Text>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Theme</Text>
        <View style={styles.row}>
          <Button title="Light" onPress={() => setTheme('light')} />
          <Button title="Dark" onPress={() => setTheme('dark')} />
        </View>
        <Text>Current theme: {theme}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Units</Text>
        <View style={styles.row}>
          <Button title="Celsius" onPress={() => updateProfileMutation.mutate({temperature_unit: 'C'})} />
          <Button title="Fahrenheit" onPress={() => updateProfileMutation.mutate({temperature_unit: 'F'})} />
        </View>
        <View style={styles.row}>
          <Button title="kmh" onPress={() => updateProfileMutation.mutate({wind_unit: 'kmh'})} />
          <Button title="mph" onPress={() => updateProfileMutation.mutate({wind_unit: 'mph'})} />
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Notifications</Text>
        <View style={styles.row}>
          <Text>Severe Alerts</Text>
          <Switch
            value={Boolean(prefs?.severe_alert_enabled)}
            onValueChange={value => updatePrefsMutation.mutate({severe_alert_enabled: value})}
          />
        </View>
        <View style={styles.row}>
          <Text>Daily Summary</Text>
          <Switch
            value={Boolean(prefs?.daily_summary_enabled)}
            onValueChange={value => updatePrefsMutation.mutate({daily_summary_enabled: value})}
          />
        </View>
        <View style={styles.row}>
          <Text>Rain Alerts</Text>
          <Switch
            value={Boolean(prefs?.rain_alert_enabled)}
            onValueChange={value => updatePrefsMutation.mutate({rain_alert_enabled: value})}
          />
        </View>
      </View>

      <Button title="Konumlari Yonet" onPress={() => navigation.navigate('LocationManagement')} />
      <Button title="Premium'a Gec" onPress={() => navigation.navigate('Paywall')} />
      <Button title="Logout" color="red" onPress={() => void logoutCurrentUser()} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 14},
  title: {fontSize: 22, fontWeight: '700'},
  section: {gap: 8, padding: 10, borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8},
  sectionTitle: {fontWeight: '700'},
  row: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8},
});
