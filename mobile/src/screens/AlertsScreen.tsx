import React, {useState} from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import {useQuery} from '@tanstack/react-query';

import {useTranslation} from 'react-i18next';

import {getAlerts, getAlertDetail} from '../services/alertsApi';
import {AppColors, DarkColors, FontSize, LightColors, Radius, Spacing} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';

// ── Uyarı Tipi Yardımcıları ───────────────────────────────────────────────────
function severityConfig(severity: string): {emoji: string; color: string; label: string} {
  switch (severity?.toLowerCase()) {
    case 'critical': return {emoji: '🔴', color: '#F44336', label: 'Kritik'};
    case 'active':   return {emoji: '🟡', color: '#FF9800', label: 'Aktif'};
    case 'advisory': return {emoji: '🔵', color: '#2196F3', label: 'Bilgi'};
    default:         return {emoji: '⚪', color: '#9E9E9E', label: 'Bilinmiyor'};
  }
}

// ── Statik Güvenlik İpuçları (backend yokken gösterilir) ──────────────────────
const SAFETY_TIPS = [
  {id: '1', icon: '⛈️', title: 'Fırtınada Güvende Kal', desc: 'Kapalı alanlara sığın, ağaçlardan uzak dur, elektrikli aletleri fişten çek.'},
  {id: '2', icon: '🌊', title: 'Sel Uyarısı', desc: 'Alçak arazileri terk et, sel sularına girme, araçla geçiş yapma.'},
  {id: '3', icon: '🌡️', title: 'Aşırı Sıcak', desc: 'Bol su iç, öğle saatlerinde dışarı çıkma, yaşlı ve çocuklara dikkat et.'},
  {id: '4', icon: '❄️', title: 'Buzlanma Uyarısı', desc: 'Yavaş sür, uygun kış lastiği kullan, zorunlu olmadıkça çıkma.'},
  {id: '5', icon: '🌪️', title: 'Hortum/Kasırga', desc: 'Alt katlara in, pencerelerden uzak dur, bodrum katları tercih et.'},
];

import {formatNaturalDate, formatSafeEventTime} from '../utils/dateUtils';

export function AlertsScreen(): React.JSX.Element {
  const {t} = useTranslation();
  const navigation = useNavigation<any>();
  const {theme} = useThemeStore();
  const {isGuest} = useAuthStore();
  const C = useColors(); // Use unified useColors
  const [selectedAlertId, setSelectedAlertId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'alerts' | 'tips'>('alerts');

  const alertsQuery = useQuery({
    queryKey: ['alerts'],
    queryFn: () => getAlerts({}),
    retry: false,
    enabled: !isGuest,
  });

  const detailQuery = useQuery({
    queryKey: ['alert', selectedAlertId],
    queryFn: () => getAlertDetail(selectedAlertId!),
    enabled: Boolean(selectedAlertId),
    retry: false,
  });

  const grouped = alertsQuery.data ?? {critical: [], active: [], advisory: []};
  const allAlertsRaw: any[] = [
    ...((grouped as any).critical ?? []),
    ...((grouped as any).active ?? []),
    ...((grouped as any).advisory ?? []),
  ];

  // Gruplama ve Tekilleştirme (Item 6)
  const allAlerts = useMemo(() => {
    const unique = new Map();
    allAlertsRaw.forEach(alert => {
      const key = `${alert.title}-${alert.area}`;
      if (!unique.has(key) || new Date(alert.created_at) > new Date(unique.get(key).created_at)) {
        unique.set(key, alert);
      }
    });
    return Array.from(unique.values()).sort((a, b) =>
        new Date(b.created_at).getTime() - new Date(a.created_at).getTime()
    );
  }, [allAlertsRaw]);

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle={theme === 'dark' ? 'light-content' : 'dark-content'} backgroundColor={C.bg} />

      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Icon name="chevron-back" size={28} color={C.text} />
        </TouchableOpacity>
        <Text style={s.headerTitle}>Bildirim Merkezi</Text>
        <View style={{width: 36}} />
      </View>

      {/* Tab Seçici */}
      <View style={s.tabs}>
        <TouchableOpacity
          style={[s.tab, activeTab === 'alerts' && s.tabActive, {backgroundColor: activeTab === 'alerts' ? C.accent : C.bgCard}]}
          onPress={() => setActiveTab('alerts')}>
          <Text style={[s.tabText, activeTab === 'alerts' && s.tabTextActive]}>Bildirimler</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[s.tab, activeTab === 'tips' && s.tabActive, {backgroundColor: activeTab === 'tips' ? C.accent : C.bgCard}]}
          onPress={() => setActiveTab('tips')}>
          <Text style={[s.tabText, activeTab === 'tips' && s.tabTextActive]}>Güvenlik</Text>
        </TouchableOpacity>
      </View>

      {activeTab === 'alerts' ? (
        // ── Uyarılar Listesi ──
        <View style={{flex: 1}}>
          {isGuest ? (
            <View style={s.guestState}>
              <Text style={{fontSize: 48}}>🔒</Text>
              <Text style={[s.emptyTitle, {color: C.text}]}>{t('alerts.loginRequired')}</Text>
              <Text style={[s.emptyDesc, {color: C.textSecondary}]}>{t('alerts.loginDesc')}</Text>
            </View>
          ) : alertsQuery.isLoading ? (
            <View style={s.center}>
              <ActivityIndicator size="large" color={C.accent} />
            </View>
          ) : allAlerts.length === 0 ? (
            <ScrollView contentContainerStyle={s.emptyState}>
              <Text style={{fontSize: 64}}>✅</Text>
              <Text style={[s.emptyTitle, {color: C.text}]}>Her Şey Yolunda</Text>
              <Text style={[s.emptyDesc, {color: C.textSecondary}]}>Şu an bölgeniz için aktif bir hava uyarısı bulunmuyor.</Text>
            </ScrollView>
          ) : (
            <FlatList
              data={allAlerts}
              keyExtractor={item => item.id}
              contentContainerStyle={{padding: Spacing.md, gap: Spacing.md}}
              renderItem={({item}) => {
                const cfg = severityConfig(item.severity);
                return (
                  <TouchableOpacity
                    style={[s.alertCard, {backgroundColor: C.bgCard, borderColor: C.border}]}
                    onPress={() => setSelectedAlertId(item.id)}>
                    <View style={s.alertTop}>
                      <View style={[s.severityBadge, {backgroundColor: cfg.color + '20'}]}>
                        <Text style={[s.severityText, {color: cfg.color}]}>{cfg.label.toUpperCase()}</Text>
                      </View>
                      <Text style={s.alertTime}>{formatNaturalDate(item.created_at || new Date().toISOString())}</Text>
                    </View>
                    <View style={s.alertContent}>
                        <Text style={[s.alertTitle, {color: C.text}]}>{item.title}</Text>
                        <Text style={[s.alertSub, {color: C.textSecondary}]} numberOfLines={2}>
                          {item.area ?? 'Bölgeniz'} · {item.description || 'Detaylar için dokunun.'}
                        </Text>
                    </View>
                    {item.expires_at && (
                      <View style={s.alertFooter}>
                         <Icon name="time-outline" size={12} color={C.textMuted} />
                         <Text style={[s.alertExpiry, {color: C.textMuted}]}>
                            {formatSafeEventTime(item.expires_at)} tarihine kadar geçerli
                         </Text>
                      </View>
                    )}
                  </TouchableOpacity>
                );
              }}
            />
          )}
        </View>
      ) : (
        // ── Güvenlik İpuçları ──
        <FlatList
          data={SAFETY_TIPS}
          keyExtractor={item => item.id}
          contentContainerStyle={{padding: Spacing.md, gap: Spacing.md}}
          renderItem={({item}) => (
            <View style={[s.tipCard, {backgroundColor: C.bgCard, borderColor: C.border}]}>
              <View style={[s.tipIconBox, {backgroundColor: C.accent + '10'}]}>
                <Text style={s.tipIcon}>{item.icon}</Text>
              </View>
              <View style={{flex: 1}}>
                <Text style={[s.tipTitle, {color: C.text}]}>{item.title}</Text>
                <Text style={[s.tipDesc, {color: C.textSecondary}]}>{item.desc}</Text>
              </View>
            </View>
          )}
        />
      )}


      {/* Uyarı Detay Modal */}
      <Modal
        visible={Boolean(selectedAlertId)}
        animationType="slide"
        transparent
        onRequestClose={() => setSelectedAlertId(null)}>
        <View style={s.modalBackdrop}>
          <View style={[s.modalBody, {backgroundColor: C.bgCard}]}>
            <View style={s.modalHandle} />
            {detailQuery.isLoading ? (
              <ActivityIndicator color={C.accent} style={{paddingVertical: 40}} />
            ) : detailQuery.data ? (
              <>
                <View style={s.modalHeader}>
                  <Text style={s.modalEmoji}>{severityConfig(detailQuery.data.severity).emoji}</Text>
                  <Text style={[s.modalTitle, {color: C.text}]}>{detailQuery.data.title}</Text>
                </View>
                <ScrollView style={{maxHeight: 200}}>
                  <Text style={[s.modalDesc, {color: C.textSecondary}]}>
                    {detailQuery.data.description ?? 'Detay bilgisi mevcut değil.'}
                  </Text>
                </ScrollView>
                {detailQuery.data.expires_at && (
                  <Text style={[s.modalExpiry, {color: C.textMuted}]}>
                    ⏱ {new Date(detailQuery.data.expires_at).toLocaleString('tr')} tarihine kadar geçerli
                  </Text>
                )}
              </>
            ) : null}
            <TouchableOpacity
              style={[s.modalCloseBtn, {backgroundColor: C.accentBtn}]}
              onPress={() => setSelectedAlertId(null)}>
              <Text style={s.modalCloseBtnText}>Kapat</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

// ── Stiller ───────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},
    center: {flex: 1, justifyContent: 'center', alignItems: 'center'},

    header: {flexDirection: 'row', alignItems: 'center', paddingHorizontal: Spacing.md, paddingVertical: Spacing.md, borderBottomWidth: 0.5, borderBottomColor: C.divider},
    backBtn: {width: 40, height: 40, justifyContent: 'center', alignItems: 'flex-start'},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerTitle: {flex: 1, fontSize: 20, fontWeight: '800', color: C.text, textAlign: 'center'},

    tabs: {flexDirection: 'row', paddingHorizontal: Spacing.md, paddingVertical: Spacing.md, gap: 12},
    tab: {flex: 1, paddingVertical: 12, borderRadius: Radius.lg, alignItems: 'center', borderWidth: 1, borderColor: C.border},
    tabActive: {borderColor: 'transparent'},
    tabText: {fontSize: 14, color: C.textSecondary, fontWeight: '800'},
    tabTextActive: {color: '#FFFFFF'},

    // Boş durum
    emptyState: {flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xl},
    guestState: {flex: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xl, gap: Spacing.md},
    emptyTitle: {fontSize: FontSize.xl, fontWeight: '800', marginTop: Spacing.md},
    emptyDesc: {fontSize: FontSize.md, textAlign: 'center', lineHeight: 22, marginTop: Spacing.sm},

    // Uyarı kartı
    alertCard: {
        borderRadius: Radius.xl,
        padding: Spacing.md,
        borderWidth: 1,
        gap: 12,
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 10,
        elevation: 2
    },
    alertTop: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'},
    severityBadge: {paddingHorizontal: 10, paddingVertical: 4, borderRadius: Radius.sm},
    severityText: {fontSize: 10, fontWeight: '900', letterSpacing: 1},
    alertTime: {fontSize: 12, color: C.textMuted, fontWeight: '600'},
    alertContent: {gap: 4},
    alertTitle: {fontSize: 17, fontWeight: '800'},
    alertSub: {fontSize: 14, lineHeight: 20},
    alertFooter: {flexDirection: 'row', alignItems: 'center', gap: 6, paddingTop: 4},
    alertExpiry: {fontSize: 11, fontWeight: '600'},

    // İpuçları
    tipCard: {flexDirection: 'row', borderRadius: Radius.xl, padding: Spacing.md, gap: Spacing.md, borderWidth: 1, alignItems: 'center'},
    tipIconBox: {width: 56, height: 56, borderRadius: Radius.lg, justifyContent: 'center', alignItems: 'center'},
    tipIcon: {fontSize: 28},
    tipTitle: {fontSize: 16, fontWeight: '800', marginBottom: 4},
    tipDesc: {fontSize: 13, lineHeight: 18},

    // Modal
    modalBackdrop: {flex: 1, backgroundColor: 'rgba(0,0,0,0.7)', justifyContent: 'flex-end'},
    modalBody: {borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: Spacing.xl, paddingBottom: 48, gap: Spacing.lg},
    modalHandle: {width: 40, height: 4, backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: 2, alignSelf: 'center', marginBottom: Spacing.sm},
    modalHeader: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md},
    modalEmoji: {fontSize: 32},
    modalTitle: {fontSize: 22, fontWeight: '900', flex: 1},
    modalDesc: {fontSize: 16, lineHeight: 24},
    modalExpiry: {fontSize: 12, fontWeight: '600'},
    modalCloseBtn: {borderRadius: Radius.xl, paddingVertical: 16, alignItems: 'center', marginTop: Spacing.md},
    modalCloseBtnText: {color: '#FFFFFF', fontWeight: '900', fontSize: 16, letterSpacing: 1},
  });

