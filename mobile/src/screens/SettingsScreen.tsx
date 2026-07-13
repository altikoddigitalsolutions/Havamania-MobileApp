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

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

import {SettingsStackParamList} from '../navigation/SettingsStack';
import {
  getNotificationPreferences,
  getProfile,
  updateNotificationPreferences,
  updateProfile,
} from '../services/profileApi';
import {useAuthStore} from '../store/authStore';
import {useThemeStore} from '../store/themeStore';
import {useTravelStore} from '../store/travelStore';
import {useTheme} from '../theme';

type Props = NativeStackScreenProps<SettingsStackParamList, 'SettingsHome'>;

export function SettingsScreen({navigation}: Props): React.JSX.Element {
  const queryClient = useQueryClient();
  const {isGuest} = useAuthStore();
  const {theme, setTheme, animationsEnabled, setAnimationsEnabled} = useThemeStore();
  const { colors: C, spacing, fontSize, responsive, layout, radius } = useTheme();

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

  const s = makeStyles(C, spacing, fontSize, responsive, layout, radius);

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.content}>
        <View style={s.centeredContainer}>
          {/* Premium Promosyon */}
          <TouchableOpacity
            activeOpacity={0.9}
            style={s.premiumCard}
            onPress={() => (navigation as any).navigate('Premium')}>
            <LinearGradient colors={[C.accent, C.accentDark]} style={s.premiumGradient} start={{x:0, y:0}} end={{x:1, y:1}}>
              <View style={s.premiumContent}>
                <View style={{ flex: 1 }}>
                  <Text style={s.premiumTitle}>Premium'a Geç</Text>
                  <Text style={s.premiumSubtitle} numberOfLines={2}>Tüm özellikleri sınırsız kullanın</Text>
                </View>
                <View style={s.premiumBadge}>
                  <Icon name="ribbon" size={20} color={C.accent} />
                </View>
              </View>
            </LinearGradient>
          </TouchableOpacity>

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
                fontSize={fontSize}
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
                fontSize={fontSize}
              />
              <View style={s.divider} />
              <SettingRow
                icon="globe-outline"
                label="Uygulama Dili"
                valueText="Türkçe"
                onPress={() => {}}
                C={C}
                fontSize={fontSize}
              />
              <View style={s.divider} />
              <SettingRow
                icon="refresh-outline"
                label="Veri Yenileme Sıklığı"
                valueText="15 Dakika"
                onPress={() => {}}
                C={C}
                fontSize={fontSize}
              />
            </View>
          </View>

          {/* AI Asistan Ayarları */}
          <View style={s.section}>
            <Text style={s.sectionTitle}>AI ASİSTAN</Text>
            <View style={s.card}>
              <SettingRow
                icon="chatbubble-ellipses-outline"
                label="Asistan Konuşma Dili"
                valueText={
                  profile?.assistant_tone === 'SAMIMI' ? 'Samimi' :
                  profile?.assistant_tone === 'RESMI' ? 'Resmi' :
                  profile?.assistant_tone === 'KISA_NET' ? 'Kısa ve Net' :
                  profile?.assistant_tone === 'DETAYLI_UZMAN' ? 'Detaylı Uzman' : 'Dengeli'
                }
                onPress={() => {
                  const tones = ['SAMIMI', 'RESMI', 'DENGELI', 'KISA_NET', 'DETAYLI_UZMAN'];
                  const currentIdx = tones.indexOf(profile?.assistant_tone || 'DENGELI');
                  const nextTone = tones[(currentIdx + 1) % tones.length];
                  updateProfileMutation.mutate({ assistant_tone: nextTone });
                }}
                C={C}
                fontSize={fontSize}
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
                fontSize={fontSize}
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
                fontSize={fontSize}
              />
            </View>
          </View>

          {/* Destek ve Hakkında */}
          <View style={s.section}>
            <Text style={s.sectionTitle}>DİĞER</Text>
            <View style={s.card}>
              <SettingRow icon="shield-checkmark-outline" label="Gizlilik Politikası" onPress={() => Alert.alert('Bilgi', 'Gizlilik politikası sayfası açılıyor...')} C={C} fontSize={fontSize} />
              <View style={s.divider} />
              <SettingRow icon="document-text-outline" label="Kullanım Şartları" onPress={() => Alert.alert('Bilgi', 'Kullanım şartları sayfası açılıyor...')} C={C} fontSize={fontSize} />
              <View style={s.divider} />
              <SettingRow icon="information-circle-outline" label="Hakkında" onPress={() => Alert.alert('Hakkında', 'Havamania v1.2.0\nPremium Hava Durumu Asistanı')} C={C} fontSize={fontSize} />
            </View>
          </View>

          {/* Veri Güvenliği ve Temizlik */}
          <View style={s.section}>
            <Text style={s.sectionTitle}>VERİ VE GÜVENLİK</Text>
            <View style={s.card}>
              <SettingRow
                icon="trash-outline"
                label="AI Geçmişini Temizle"
                onPress={() => {
                  Alert.alert(
                    'Geçmişi Temizle',
                    'AI asistan ile olan tüm konuşma geçmişin silinecek. Bu işlem geri alınamaz.',
                    [
                      { text: 'Vazgeç', style: 'cancel' },
                      { text: 'Temizle', style: 'destructive', onPress: () => Alert.alert('Başarılı', 'Konuşma geçmişi temizlendi.') }
                    ]
                  );
                }}
                C={C}
                fontSize={fontSize}
              />
              <View style={s.divider} />
              <SettingRow
                icon="refresh-circle-outline"
                label="Tüm Verileri Sıfırla"
                desc="Hesabındaki tüm seyahatler ve ayarlar silinir."
                onPress={() => {
                  Alert.alert(
                    'Verileri Sıfırla',
                    'Uygulamadaki tüm verilerin (seyahatler, geçmiş, ayarlar) kalıcı olarak silinecektir. Bu işlem geri alınamaz. Emin misin?',
                    [
                      { text: 'İptal', style: 'cancel' },
                      {
                        text: 'Tümünü Sıfırla',
                        style: 'destructive',
                        onPress: async () => {
                           // Gerçek sıfırlama mantığı
                           useTravelStore.getState().plans.forEach(p => useTravelStore.getState().removePlan(p.id));
                           setTheme('light');
                           setAnimationsEnabled(true);
                           Alert.alert('Başarılı', 'Tüm veriler temizlendi.');
                        }
                      }
                    ]
                  );
                }}
                C={C}
                fontSize={fontSize}
              />
            </View>
          </View>

          <Text style={s.footerText}>Havamania © 2024</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

function SettingRow({ icon, label, desc, type = 'arrow', value, onValueChange, valueText, onPress, C, fontSize }: any) {
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
          <Text style={[rowStyles.label, { color: C.text, fontSize: fontSize.md }]} numberOfLines={2}>{label}</Text>
          {desc && <Text style={[rowStyles.desc, { color: C.textSecondary, fontSize: fontSize.xs }]} numberOfLines={2}>{desc}</Text>}
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
            {valueText && <Text style={{ color: C.textSecondary, fontSize: fontSize.sm }}>{valueText}</Text>}
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
  label: { fontWeight: '600' },
  desc: { marginTop: 2 },
  right: { marginLeft: 8 },
});

const makeStyles = (C: any, spacing: any, fontSize: any, responsive: any, layout: any, radius: any) => StyleSheet.create({
  safe: { flex: 1, backgroundColor: C.bg },
  centeredContainer: { alignSelf: 'center', width: '100%', maxWidth: layout.maxContentWidth },
  content: { padding: spacing.pagePadding },
  premiumCard: { marginBottom: 24, borderRadius: radius.lg, overflow: 'hidden', elevation: 8, shadowColor: C.accent, shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 10 },
  premiumGradient: { padding: 20 },
  premiumContent: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  premiumTitle: { color: '#FFF', fontSize: fontSize.lg, fontWeight: '900' },
  premiumSubtitle: { color: 'rgba(255,255,255,0.8)', fontSize: fontSize.sm, marginTop: 2 },
  premiumBadge: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#FFF', justifyContent: 'center', alignItems: 'center' },
  section: { marginBottom: 28 },
  sectionTitle: { fontSize: fontSize.xs, fontWeight: '800', color: C.textMuted, letterSpacing: 1.5, marginBottom: 12, marginLeft: 4 },
  card: { backgroundColor: C.bgCard, borderRadius: radius.md, overflow: 'hidden', borderWidth: 1, borderColor: C.border },
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
