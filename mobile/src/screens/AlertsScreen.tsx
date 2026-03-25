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

export function AlertsScreen(): React.JSX.Element {
  const navigation = useNavigation<any>();
  const {theme} = useThemeStore();
  const {isGuest} = useAuthStore();
  const C = theme === 'dark' ? DarkColors : LightColors;
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
  const allAlerts: any[] = [
    ...((grouped as any).critical ?? []),
    ...((grouped as any).active ?? []),
    ...((grouped as any).advisory ?? []),
  ];

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle={theme === 'dark' ? 'light-content' : 'dark-content'} backgroundColor={C.bg} />

      {/* Header */}
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <Text style={s.headerTitle}>Uyarılar</Text>
        <View style={{width: 36}} />
      </View>

      {/* Tab Seçici */}
      <View style={s.tabs}>
        <TouchableOpacity
          style={[s.tab, activeTab === 'alerts' && s.tabActive]}
          onPress={() => setActiveTab('alerts')}>
          <Text style={[s.tabText, activeTab === 'alerts' && s.tabTextActive]}>🔔 Hava Uyarıları</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[s.tab, activeTab === 'tips' && s.tabActive]}
          onPress={() => setActiveTab('tips')}>
          <Text style={[s.tabText, activeTab === 'tips' && s.tabTextActive]}>🛡️ Güvenlik</Text>
        </TouchableOpacity>
      </View>

      {activeTab === 'alerts' ? (
        // ── Uyarılar Listesi ──
        <View style={{flex: 1}}>
          {isGuest ? (
            // Misafir modu
            <View style={s.guestState}>
              <Text style={{fontSize: 48}}>🔒</Text>
              <Text style={[s.emptyTitle, {color: C.text}]}>Giriş Gerekiyor</Text>
              <Text style={[s.emptyDesc, {color: C.textSecondary}]}>
                Hava uyarılarını görmek için hesabınıza giriş yapın.
              </Text>
            </View>
          ) : alertsQuery.isLoading ? (
            <View style={s.center}>
              <ActivityIndicator size="large" color={C.accent} />
            </View>
          ) : alertsQuery.isError || allAlerts.length === 0 ? (
            // Uyarı yok
            <ScrollView contentContainerStyle={s.emptyState}>
              <Text style={{fontSize: 64}}>✅</Text>
              <Text style={[s.emptyTitle, {color: C.text}]}>Aktif Uyarı Yok</Text>
              <Text style={[s.emptyDesc, {color: C.textSecondary}]}>
                Bölgenizdeki hava koşulları şu an normal seviyelerde. Herhangi bir önemli uyarı bulunmuyor.
              </Text>

              {/* Son güncelleme */}
              <View style={[s.updateCard, {backgroundColor: C.bgCard, borderColor: C.border}]}>
                <Text style={{fontSize: 20}}>🕐</Text>
                <View style={{flex: 1}}>
                  <Text style={{fontSize: FontSize.sm, fontWeight: '700', color: C.text}}>Son Güncelleme</Text>
                  <Text style={{fontSize: FontSize.xs, color: C.textSecondary}}>
                    {new Date().toLocaleString('tr', {weekday: 'long', hour: '2-digit', minute: '2-digit'})}
                  </Text>
                </View>
              </View>
            </ScrollView>
          ) : (
            // Uyarı listesi
            <FlatList
              data={allAlerts}
              keyExtractor={item => item.id}
              contentContainerStyle={{padding: Spacing.md, gap: Spacing.sm}}
              renderItem={({item}) => {
                const cfg = severityConfig(item.severity);
                return (
                  <TouchableOpacity
                    style={[s.alertCard, {borderLeftColor: cfg.color}]}
                    onPress={() => setSelectedAlertId(item.id)}>
                    <View style={s.alertTop}>
                      <Text style={s.alertEmoji}>{cfg.emoji}</Text>
                      <View style={{flex: 1}}>
                        <Text style={[s.alertTitle, {color: C.text}]}>{item.title}</Text>
                        <Text style={[s.alertSub, {color: C.textSecondary}]}>
                          {item.area ?? 'Bölgeniz'} · {cfg.label}
                        </Text>
                      </View>
                      <Text style={[s.alertChevron, {color: C.textMuted}]}>›</Text>
                    </View>
                    {item.expires_at && (
                      <Text style={[s.alertExpiry, {color: C.textMuted}]}>
                        ⏱ {new Date(item.expires_at).toLocaleString('tr')} tarihine kadar
                      </Text>
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
          contentContainerStyle={{padding: Spacing.md, gap: Spacing.sm}}
          renderItem={({item}) => (
            <View style={[s.tipCard, {backgroundColor: C.bgCard, borderColor: C.border}]}>
              <Text style={s.tipIcon}>{item.icon}</Text>
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
    backBtn: {width: 36},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerTitle: {flex: 1, fontSize: FontSize.xl, fontWeight: '700', color: C.text, textAlign: 'center'},

    tabs: {flexDirection: 'row', paddingHorizontal: Spacing.md, paddingVertical: Spacing.sm, gap: Spacing.sm},
    tab: {flex: 1, paddingVertical: Spacing.sm, borderRadius: Radius.full, alignItems: 'center', backgroundColor: C.bgCard, borderWidth: 1, borderColor: C.border},
    tabActive: {backgroundColor: C.accentBtn, borderColor: C.accentBtn},
    tabText: {fontSize: FontSize.sm, color: C.textSecondary, fontWeight: '600'},
    tabTextActive: {color: '#FFFFFF'},

    // Boş durum
    emptyState: {flexGrow: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xl},
    guestState: {flex: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xl, gap: Spacing.md},
    emptyTitle: {fontSize: FontSize.xl, fontWeight: '800', marginTop: Spacing.md},
    emptyDesc: {fontSize: FontSize.md, textAlign: 'center', lineHeight: 22, marginTop: Spacing.sm},
    updateCard: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md, borderWidth: 1, borderRadius: Radius.lg, padding: Spacing.md, marginTop: Spacing.xl, width: '100%'},

    // Uyarı kartı
    alertCard: {backgroundColor: C.bgCard, borderRadius: Radius.lg, padding: Spacing.md, borderWidth: 1, borderColor: C.border, borderLeftWidth: 4},
    alertTop: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md},
    alertEmoji: {fontSize: 22},
    alertTitle: {fontSize: FontSize.md, fontWeight: '700'},
    alertSub: {fontSize: FontSize.sm, marginTop: 2},
    alertChevron: {fontSize: 22},
    alertExpiry: {fontSize: FontSize.xs, marginTop: Spacing.xs},

    // İpuçları
    tipCard: {flexDirection: 'row', borderRadius: Radius.lg, padding: Spacing.md, gap: Spacing.md, borderWidth: 1, alignItems: 'flex-start'},
    tipIcon: {fontSize: 28},
    tipTitle: {fontSize: FontSize.md, fontWeight: '700', marginBottom: 4},
    tipDesc: {fontSize: FontSize.sm, lineHeight: 20},

    // Modal
    modalBackdrop: {flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end'},
    modalBody: {borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: Spacing.lg, paddingBottom: 36, gap: Spacing.md},
    modalHandle: {width: 40, height: 4, backgroundColor: '#888', borderRadius: 2, alignSelf: 'center', marginBottom: Spacing.sm},
    modalHeader: {flexDirection: 'row', alignItems: 'center', gap: Spacing.md},
    modalEmoji: {fontSize: 28},
    modalTitle: {fontSize: FontSize.xl, fontWeight: '800', flex: 1},
    modalDesc: {fontSize: FontSize.md, lineHeight: 22},
    modalExpiry: {fontSize: FontSize.sm},
    modalCloseBtn: {borderRadius: Radius.full, paddingVertical: 14, alignItems: 'center', marginTop: Spacing.sm},
    modalCloseBtnText: {color: '#FFFFFF', fontWeight: '700', fontSize: FontSize.md},
  });
