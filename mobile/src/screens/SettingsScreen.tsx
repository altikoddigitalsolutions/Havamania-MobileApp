import React, {useState, useEffect} from 'react';
import {
  StyleSheet,
  Switch,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Alert,
  SafeAreaView,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import Icon from 'react-native-vector-icons/Ionicons';

import {SettingsStackParamList} from '../navigation/SettingsStack';
import {
  getNotificationPreferences,
  getProfile,
  updateNotificationPreferences,
  updateProfile,
} from '../services/profileApi';
import {useAuthStore} from '../store/authStore';
import {useThemeStore} from '../store/themeStore';
import {useColors, Spacing, Radius, FontSize, AppColors} from '../theme';

type Props = NativeStackScreenProps<SettingsStackParamList, 'SettingsHome'>;

export function SettingsScreen({navigation}: Props): React.JSX.Element {
  const queryClient = useQueryClient();
  const {isGuest} = useAuthStore();
  const {theme, setTheme, animationsEnabled, setAnimationsEnabled} = useThemeStore();
  const C = useColors();

  const profileQuery = useQuery({queryKey: ['profile'], queryFn: getProfile, enabled: !isGuest});
  const prefsQuery = useQuery({queryKey: ['profile', 'notifications'], queryFn: getNotificationPreferences, enabled: !isGuest});

  const [localPrefs, setLocalPrefs] = useState<any>(null);

  useEffect(() => {
    if (prefsQuery.data) {
      setLocalPrefs(prefsQuery.data);
    }
  }, [prefsQuery.data]);

  const updateProfileMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile']}),
  });

  const updatePrefsMutation = useMutation({
    mutationFn: updateNotificationPreferences,
    onMutate: async (newValues) => {
      await queryClient.cancelQueries({queryKey: ['profile', 'notifications']});
      const previous = queryClient.getQueryData(['profile', 'notifications']);
      const updated = {...(previous as any), ...newValues};
      setLocalPrefs(updated);
      queryClient.setQueryData(['profile', 'notifications'], updated);
      return {previous};
    },
    onError: (err: any, newValues, context) => {
      if (context?.previous) {
        queryClient.setQueryData(['profile', 'notifications'], context.previous);
        setLocalPrefs(context.previous);
      }
      Alert.alert('Hata', 'Ayar kaydedilemedi.');
    },
    onSettled: () => {
      queryClient.invalidateQueries({queryKey: ['profile', 'notifications']});
    },
  });

  const profile = profileQuery.data;
  const prefs = localPrefs || prefsQuery.data;

  const handleTogglePref = (key: string, value: boolean) => {
    if (isGuest) {
      Alert.alert('Üye Olmalısınız', 'Lütfen giriş yapın.');
      return;
    }
    updatePrefsMutation.mutate({[key]: value});
  };

  const themes = [
    {id: 'light', label: 'Açık', icon: 'sunny-outline', color: '#F59E0B'},
    {id: 'dark', label: 'Koyu', icon: 'moon-outline', color: '#3B82F6'},
    {id: 'spring', label: 'Bahar', icon: 'leaf-outline', color: '#10B981'},
    {id: 'summer', label: 'Yaz', icon: 'umbrella-outline', color: '#F97316'},
    {id: 'autumn', label: 'Güz', icon: 'color-filter-outline', color: '#D97706'},
    {id: 'winter', label: 'Kış', icon: 'snow-outline', color: '#06B6D4'},
  ];

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.content}>

        {/* Görünüm Bölümü */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>GÖRÜNÜM</Text>
          <View style={s.card}>
            <View style={s.themeGrid}>
              {themes.map((t) => (
                <TouchableOpacity
                  key={t.id}
                  style={[s.themeItem, theme === t.id && { borderColor: C.accent, backgroundColor: 'rgba(59, 130, 246, 0.05)' }]}
                  onPress={() => setTheme(t.id as any)}>
                  <Icon name={t.icon} size={24} color={theme === t.id ? C.accent : t.color} />
                  <Text style={[s.themeLabel, { color: theme === t.id ? C.accent : C.text }]}>{t.label}</Text>
                </TouchableOpacity>
              ))}
            </View>
            <View style={s.divider} />
            <SettingRow
              icon="sparkles-outline"
              label="Hava Durumu Animasyonları"
              desc="Ana ekrandaki dinamik efektleri açar/kapatır."
              type="switch"
              value={animationsEnabled}
              onValueChange={setAnimationsEnabled}
              C={C}
            />
          </View>
        </View>

        {/* Birimler ve Tercihler */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>TERCİHLER</Text>
          <View style={s.card}>
            <SettingRow
              icon="thermometer-outline"
              label="Sıcaklık Birimi"
              valueText={profile?.temperature_unit === 'F' ? 'Fahrenheit' : 'Celsius'}
              onPress={() => {
                const newUnit = profile?.temperature_unit === 'F' ? 'C' : 'F';
                updateProfileMutation.mutate({ temperature_unit: newUnit });
              }}
              C={C}
            />
            <View style={s.divider} />
            <SettingRow
              icon="globe-outline"
              label="Uygulama Dili"
              valueText="Türkçe"
              onPress={() => {}}
              C={C}
            />
            <View style={s.divider} />
            <SettingRow
              icon="refresh-outline"
              label="Veri Yenileme Sıklığı"
              valueText="15 Dakika"
              onPress={() => {}}
              C={C}
            />
          </View>
        </View>

        {/* Bildirimler */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>BİLDİRİMLER</Text>
          <View style={s.card}>
            <SettingRow
              icon="alert-circle-outline"
              label="Şiddetli Hava Uyarıları"
              desc="Fırtına, sel gibi durumlarda anlık bilgi al."
              type="switch"
              value={Boolean(prefs?.severe_alert_enabled)}
              onValueChange={v => handleTogglePref('severe_alert_enabled', v)}
              C={C}
            />
            <View style={s.divider} />
            <SettingRow
              icon="calendar-outline"
              label="Günlük Hava Özeti"
              desc="Her sabah o günün hava tahminini al."
              type="switch"
              value={Boolean(prefs?.daily_summary_enabled)}
              onValueChange={v => handleTogglePref('daily_summary_enabled', v)}
              C={C}
            />
          </View>
        </View>

        {/* Destek ve Hakkında */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>DİĞER</Text>
          <View style={s.card}>
            <SettingRow icon="shield-checkmark-outline" label="Gizlilik Politikası" onPress={() => {}} C={C} />
            <View style={s.divider} />
            <SettingRow icon="information-circle-outline" label="Hakkında" onPress={() => {}} C={C} />
          </View>
        </View>

        <Text style={s.footerText}>Havamania © 2024</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

function SettingRow({ icon, label, desc, type = 'arrow', value, onValueChange, valueText, onPress, C }: any) {
  return (
    <TouchableOpacity
      style={rowStyles.row}
      onPress={onPress}
      disabled={type === 'switch'}
      activeOpacity={0.7}
    >
      <View style={rowStyles.left}>
        <View style={[rowStyles.iconBox, { backgroundColor: 'rgba(255,255,255,0.03)' }]}>
          <Icon name={icon} size={20} color={C.text} />
        </View>
        <View style={rowStyles.labelContainer}>
          <Text style={[rowStyles.label, { color: C.text }]}>{label}</Text>
          {desc && <Text style={[rowStyles.desc, { color: C.textSecondary }]}>{desc}</Text>}
        </View>
      </View>
      <View style={rowStyles.right}>
        {type === 'switch' ? (
          <Switch
            value={value}
            onValueChange={onValueChange}
            trackColor={{ false: C.border, true: C.accent }}
            thumbColor="#FFF"
          />
        ) : (
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
            {valueText && <Text style={{ color: C.textSecondary, fontSize: 14 }}>{valueText}</Text>}
            <Icon name="chevron-forward" size={16} color={C.textMuted} />
          </View>
        )}
      </View>
    </TouchableOpacity>
  );
}

const rowStyles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16 },
  left: { flexDirection: 'row', alignItems: 'center', gap: 16, flex: 1 },
  iconBox: { width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  labelContainer: { flex: 1 },
  label: { fontSize: 16, fontWeight: '600' },
  desc: { fontSize: 12, marginTop: 2 },
  right: { marginLeft: 8 },
});

const makeStyles = (C: AppColors) => StyleSheet.create({
  safe: { flex: 1, backgroundColor: C.bg },
  content: { padding: Spacing.lg },
  section: { marginBottom: 28 },
  sectionTitle: { fontSize: 12, fontWeight: '800', color: C.textMuted, letterSpacing: 1.5, marginBottom: 12, marginLeft: 4 },
  card: { backgroundColor: C.bgCard, borderRadius: 20, overflow: 'hidden', borderWidth: 1, borderColor: C.border },
  divider: { height: 1, backgroundColor: C.divider, marginHorizontal: 16 },
  themeGrid: { flexDirection: 'row', flexWrap: 'wrap', padding: 16, gap: 12 },
  themeItem: {
    flex: 1,
    minWidth: '28%',
    aspectRatio: 1,
    backgroundColor: 'rgba(255,255,255,0.02)',
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: 'transparent'
  },
  themeLabel: { fontSize: 11, fontWeight: '700', marginTop: 8 },
  footerText: { textAlign: 'center', fontSize: 12, color: C.textMuted, marginTop: 12, marginBottom: 24 },
});
