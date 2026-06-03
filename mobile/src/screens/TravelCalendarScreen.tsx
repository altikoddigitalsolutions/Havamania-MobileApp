import React, { useState, useMemo, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  TextInput,
  Modal,
  SafeAreaView,
  Alert,
  ScrollView,
  Dimensions,
  StatusBar,
  KeyboardAvoidingView,
  Platform,
  Animated,
  Pressable,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useColors, Spacing, Radius, FontSize } from '../theme';
import { SeasonalBackground } from '../components/SeasonalBackground';

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

import { useTravelStore, TravelPlan, TravelType } from '../store/travelStore';
import { TURKEY_CITIES, City } from '../data/cities';
import { normalizeText } from '../utils/textUtils';
import { useQuery } from '@tanstack/react-query';
import { travelAnalysisService } from '../services/travelAnalysisService';

const { width } = Dimensions.get('window');

const TRAVEL_TYPES: { type: TravelType; label: string; icon: string }[] = [
  { type: 'Vacation', label: 'Tatil', icon: 'beach-outline' },
  { type: 'Business', label: 'İş', icon: 'briefcase-outline' },
  { type: 'Family', label: 'Aile', icon: 'people-outline' },
  { type: 'Sports', label: 'Spor', icon: 'football-outline' },
  { type: 'Camping', label: 'Kamp', icon: 'bonfire-outline' },
  { type: 'Culture', label: 'Kültür Gezisi', icon: 'museum-outline' },
  { type: 'Nature', label: 'Doğa', icon: 'leaf-outline' },
  { type: 'Romantic', label: 'Romantik', icon: 'heart-outline' },
  { type: 'Gastronomy', label: 'Gastronomi', icon: 'restaurant-outline' },
  { type: 'Beach', label: 'Deniz Tatili', icon: 'umbrella-outline' },
  { type: 'Winter', label: 'Kış Tatili', icon: 'snow-outline' },
  { type: 'Adventure', label: 'Macera', icon: 'map-outline' },
  { type: 'Photography', label: 'Fotoğraf', icon: 'camera-outline' },
  { type: 'Shopping', label: 'Alışveriş', icon: 'cart-outline' },
  { type: 'Weekend', label: 'Hafta Sonu', icon: 'calendar-outline' },
  { type: 'Health', label: 'Sağlık / Spa', icon: 'medkit-outline' },
  { type: 'Event', label: 'Etkinlik', icon: 'ticket-outline' },
  { type: 'RoadTrip', label: 'Road Trip', icon: 'car-outline' },
  { type: 'Other', label: 'Diğer', icon: 'ellipsis-horizontal-outline' },
];

/**
 * Premium Seyahat Ekranı
 */
export function TravelCalendarScreen() {
  return <TravelScreen />;
}

function TravelScreen() {
  const C = useColors();
  const { plans, addPlan, removePlan, updatePlan, archivePlan, unarchivePlan } = useTravelStore();

  useEffect(() => {
    // Seyahatleri kontrol et ve bildirimleri/otomatik analizleri yönet
    if (plans.length > 0) {
      travelAnalysisService.checkAndNotifyPlans(plans);
    }
  }, []);

  const [modalVisible, setModalVisible] = useState(false);
  const [filter, setFilter] = useState<'Upcoming' | 'Past' | 'Archived'>('Upcoming');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [selectedPlanForDetail, setSelectedPlanForDetail] = useState<TravelPlan | null>(null);

  // Form States
  const [selectedCity, setSelectedCity] = useState<City | null>(null);
  const [citySearch, setCitySearch] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [travelType, setTravelType] = useState<TravelType>('Vacation');
  const [note, setNote] = useState('');

  const filteredCities = useMemo(() => {
    if (citySearch.length < 1) return [];
    const searchNormalized = normalizeText(citySearch);
    return TURKEY_CITIES.filter(c =>
      normalizeText(c.name).includes(searchNormalized)
    ).slice(0, 5);
  }, [citySearch]);

  const displayPlans = useMemo(() => {
    const today = new Date().toISOString().split('T')[0];
    let result = [...plans];
    if (filter === 'Upcoming') {
      result = plans.filter(p => p.endDate >= today && !p.isArchived);
    } else if (filter === 'Past') {
      result = plans.filter(p => p.endDate < today && !p.isArchived);
    } else if (filter === 'Archived') {
      result = plans.filter(p => p.isArchived);
    }
    return result;
  }, [plans, filter]);

  const resetForm = () => {
    setSelectedCity(null);
    setCitySearch('');
    setStartDate('');
    setEndDate('');
    setTravelType('Vacation');
    setNote('');
    setEditingId(null);
    setModalVisible(false);
  };

  const handleSavePlan = () => {
    const today = new Date().toISOString().split('T')[0];

    if (!selectedCity || !startDate || !endDate) {
      Alert.alert('Eksik Bilgi', 'Lütfen şehir ve tarihleri doldurun.');
      return;
    }

    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate) || !/^\d{4}-\d{2}-\d{2}$/.test(endDate)) {
      Alert.alert('Hata', 'Tarih formatı YYYY-MM-DD olmalıdır.');
      return;
    }

    if (startDate < today && !editingId) {
        Alert.alert('Geçersiz Tarih', 'Geçmiş tarihli seyahat oluşturamazsınız.');
        return;
    }

    if (endDate < startDate) {
      Alert.alert('Geçersiz Tarih', 'Bitiş tarihi başlangıçtan önce olamaz.');
      return;
    }

    const payload = {
      city: selectedCity.name,
      lat: selectedCity.lat,
      lon: selectedCity.lon,
      startDate,
      endDate,
      type: travelType,
      note,
    };

    let planId = editingId;
    if (editingId) {
      updatePlan(editingId, payload);
    } else {
      planId = addPlan(payload);
    }

    // Yeni rota eklendiğinde veya düzenlendiğinde analizi tetikle
    setTimeout(async () => {
      const targetPlan = useTravelStore.getState().plans.find(p => p.id === planId);
      if (targetPlan) {
        try {
          const analysis = await travelAnalysisService.generateAnalysis(targetPlan);
          if (analysis) {
            useTravelStore.getState().addAnalysis(targetPlan.id, analysis);
          }
        } catch (e) {
          console.error("Initial analysis failed", e);
        }
      }
    }, 500);

    resetForm();
  };

  const handleEdit = (plan: TravelPlan) => {
    setEditingId(plan.id);
    setSelectedCity({ name: plan.city, lat: plan.lat, lon: plan.lon });
    setStartDate(plan.startDate);
    setEndDate(plan.endDate);
    setTravelType(plan.type);
    setNote(plan.note || '');
    setModalVisible(true);
  };

  const handleDelete = (id: string) => {
    Alert.alert(
      'Seyahati Sil',
      'Bu seyahat planını silmek istediğinizden emin misiniz?',
      [
        { text: 'Vazgeç', style: 'cancel' },
        { text: 'Sil', onPress: () => removePlan(id), style: 'destructive' }
      ]
    );
  };

  const openDetail = (plan: TravelPlan) => {
    setSelectedPlanForDetail(plan);
    setDetailVisible(true);
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: C.bg }]}>
      <StatusBar barStyle="light-content" />
      <LinearGradient colors={[C.bg, C.bgSecondary]} style={StyleSheet.absoluteFill} />
      <SeasonalBackground />

      <View style={styles.header}>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
          <View>
            <Text style={[styles.title, { color: C.text }]}>Seyahat Takvimi</Text>
            <Text style={[styles.subtitle, { color: C.textSecondary }]}>Hava durumuna göre akıllı seyahat planlayıcı</Text>
          </View>
          <TouchableOpacity onPress={() => setFilter('Archived')}>
            <Icon name="archive-outline" size={24} color={filter === 'Archived' ? C.accent : C.textSecondary} />
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.filterContainer}>
        {(['Upcoming', 'Past', 'Archived'] as const).map((f) => (
          <TouchableOpacity
            key={f}
            onPress={() => setFilter(f)}
            style={[
              styles.filterChip,
              { backgroundColor: filter === f ? C.accent : 'rgba(255,255,255,0.05)' },
              filter === f && styles.activeFilterChip
            ]}
          >
            <Text style={[styles.filterText, { color: filter === f ? '#FFF' : C.textSecondary }]}>
              {f === 'Upcoming' ? 'Yaklaşanlar' : f === 'Past' ? 'Geçmiş' : 'Arşiv'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <FlatList
        data={displayPlans}
        keyExtractor={(item) => item.id}
        renderItem={({ item, index }) => (
          <TravelCard
            plan={item}
            index={index}
            onEdit={() => handleEdit(item)}
            onDelete={() => handleDelete(item.id)}
            onArchive={() => archivePlan(item.id)}
            onUnarchive={() => unarchivePlan(item.id)}
            onShowDetail={() => openDetail(item)}
            onReAnalyze={async () => {
              try {
                const analysis = await travelAnalysisService.generateAnalysis(item);
                if (analysis) {
                  useTravelStore.getState().addAnalysis(item.id, analysis);
                }
              } catch (e) {
                Alert.alert("Hata", "Analiz şu an yapılamıyor. Lütfen internet bağlantınızı kontrol edin.");
              }
            }}
          />
        )}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={<TravelEmptyState onAdd={() => setModalVisible(true)} />}
      />

      <PremiumRouteButton onPress={() => setModalVisible(true)} />

      {/* Rota Oluşturma Modalı */}
      <Modal animationType="slide" transparent={true} visible={modalVisible} onRequestClose={resetForm}>
        <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.modalOverlay}>
          <View style={[styles.modalContent, { backgroundColor: C.bgSecondary, borderColor: C.border, borderWidth: 1 }]}>
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: C.text }]}>{editingId ? 'Seyahati Düzenle' : 'Yeni Seyahat Planla'}</Text>
              <TouchableOpacity onPress={resetForm} style={styles.closeBtn}><Icon name="close" size={24} color={C.textSecondary} /></TouchableOpacity>
            </View>
            <ScrollView showsVerticalScrollIndicator={false}>
              <View style={styles.inputGroup}>
                <Text style={[styles.inputLabel, { color: C.textSecondary }]}>NEREYE GİDİYORSUN?</Text>
                {selectedCity ? (
                  <TouchableOpacity style={[styles.selectedCityBox, { backgroundColor: 'rgba(255,255,255,0.05)', borderColor: C.accent }]} onPress={() => setSelectedCity(null)}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}><Icon name="location" size={18} color={C.accent} /><Text style={{ color: C.text, fontWeight: '600' }}>{selectedCity.name}</Text></View>
                    <Icon name="close-circle" size={18} color={C.textMuted} />
                  </TouchableOpacity>
                ) : (
                  <View>
                    <TextInput style={[styles.input, { backgroundColor: 'rgba(255,255,255,0.05)', color: C.text, borderColor: C.border }]} placeholder="Şehir ara (örn: Antalya)" placeholderTextColor={C.textMuted} value={citySearch} onChangeText={setCitySearch} />
                    {citySearch.length > 0 && (
                      <View style={[styles.searchDropdown, { backgroundColor: C.bgSecondary, borderColor: C.border }]}>
                        {filteredCities.map((item) => (
                          <TouchableOpacity key={item.name} style={styles.searchItem} onPress={() => { setSelectedCity(item); setCitySearch(''); }}><Text style={{ color: C.text }}>{item.name}</Text></TouchableOpacity>
                        ))}
                      </View>
                    )}
                  </View>
                )}
              </View>
              <View style={styles.dateRow}>
                <View style={{ flex: 1 }}><Text style={[styles.inputLabel, { color: C.textSecondary }]}>BAŞLANGIÇ</Text><TextInput style={[styles.input, { backgroundColor: 'rgba(255,255,255,0.05)', color: C.text, borderColor: C.border }]} placeholder="YYYY-MM-DD" placeholderTextColor={C.textMuted} value={startDate} onChangeText={setStartDate} /></View>
                <View style={{ flex: 1 }}><Text style={[styles.inputLabel, { color: C.textSecondary }]}>BİTİŞ</Text><TextInput style={[styles.input, { backgroundColor: 'rgba(255,255,255,0.05)', color: C.text, borderColor: C.border }]} placeholder="YYYY-MM-DD" placeholderTextColor={C.textMuted} value={endDate} onChangeText={setEndDate} /></View>
              </View>
              <View style={styles.inputGroup}>
                <Text style={[styles.inputLabel, { color: C.textSecondary }]}>SEYAHAT KONSEPTİ</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 10 }}>
                  {TRAVEL_TYPES.map((t) => (
                    <TouchableOpacity key={t.type} onPress={() => setTravelType(t.type)} style={[styles.typeChipForm, { backgroundColor: travelType === t.type ? C.accent : 'rgba(255,255,255,0.05)' }]}>
                      <Icon name={t.icon} size={16} color={travelType === t.type ? '#FFF' : C.textSecondary} />
                      <Text style={[styles.typeText, { color: travelType === t.type ? '#FFF' : C.textSecondary }]}>{t.label}</Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              </View>
              <View style={styles.inputGroup}>
                <Text style={[styles.inputLabel, { color: C.textSecondary }]}>ÖZEL NOTLAR</Text>
                <TextInput style={[styles.input, styles.textArea, { backgroundColor: 'rgba(255,255,255,0.05)', color: C.text, borderColor: C.border }]} placeholder="Havalimanı transferi, otel adı vb..." placeholderTextColor={C.textMuted} multiline numberOfLines={3} value={note} onChangeText={setNote} />
              </View>
              <TouchableOpacity style={[styles.saveButton, { backgroundColor: C.accent }]} onPress={handleSavePlan}>
                <LinearGradient colors={[C.accent, C.accentDark]} style={StyleSheet.absoluteFill} start={{x: 0, y: 0}} end={{x: 1, y: 0}} />
                <Text style={styles.saveButtonText}>{editingId ? 'Güncelle' : 'Planı Oluştur'}</Text>
              </TouchableOpacity>
            </ScrollView>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      {/* Geçmiş Seyahat Detay Modalı */}
      <PastTripDetailModal visible={detailVisible} plan={selectedPlanForDetail} onClose={() => setDetailVisible(false)} />
    </SafeAreaView>
  );
}

/**
 * Premium Seyahat Kartı
 */
function TravelCard({ plan, index, onEdit, onDelete, onArchive, onUnarchive, onShowDetail, onReAnalyze }: {
  plan: TravelPlan;
  index: number;
  onEdit: () => void;
  onDelete: () => void;
  onArchive: () => void;
  onUnarchive: () => void;
  onShowDetail: () => void;
  onReAnalyze: () => void;
}) {
  const C = useColors();
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(30)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fadeAnim, { toValue: 1, duration: 600, delay: index * 100, useNativeDriver: true }),
      Animated.timing(slideAnim, { toValue: 0, duration: 600, delay: index * 100, useNativeDriver: true }),
    ]).start();
  }, []);

  const typeInfo = TRAVEL_TYPES.find(t => t.type === plan.type) || TRAVEL_TYPES[5];
  const today = useMemo(() => new Date().toISOString().split('T')[0], []);
  const isPastTrip = plan.endDate < today;
  const isActiveTrip = plan.startDate <= today && plan.endDate >= today;
  const isArchived = plan.isArchived;

  const daysUntilStart = useMemo(() => {
    const todayDate = new Date(today);
    const start = new Date(plan.startDate);
    const diffTime = start.getTime() - todayDate.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }, [plan.startDate, today]);

  const { data: weatherData, refetch, isFetching, error } = useQuery({
    queryKey: ['weather', plan.lat, plan.lon, plan.startDate],
    queryFn: () => getDailyWeather(plan.lat, plan.lon, 10),
    staleTime: 10 * 60 * 1000,
    enabled: !isPastTrip && daysUntilStart <= 15 && !isArchived,
  });

  const [isAnalyzing, setIsAnalyzing] = useState(false);

  const analysis = useMemo(() => {
    const daysUntilStart = travelAnalysisService.getDaysUntil(plan.startDate);
    const lastAnalysis = plan.lastAnalysis;

    const MONTHS = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];
    const formatDate = (dateStr: string) => {
      const [y, m, d] = dateStr.split('-');
      return `${parseInt(d)} ${MONTHS[parseInt(m) - 1]}`;
    };

    if (isPastTrip || isArchived) {
      return {
        status: 'past',
        msg: isArchived
            ? `${plan.city} seyahatin arşivlendi. Tüm analiz notların ve hava durumu geçmişi burada saklanıyor.`
            : `${plan.city} seyahatin ${formatDate(plan.endDate)}'da tamamlandı. Bu seyahat geçmiş rotaların arasında saklanıyor. Dilersen seyahati arşivleyebilirsin.`,
        icon: isArchived ? 'archive' : 'checkmark-circle',
        color: C.textMuted
      };
    }

    if (isActiveTrip) {
      const activeMsg = plan.startDate === today ? "Seyahatiniz bugün başlıyor." : "Seyahatiniz devam ediyor.";
      return {
        status: 'active',
        msg: lastAnalysis?.text || activeMsg,
        temp: lastAnalysis ? `${lastAnalysis.tempMin}° / ${lastAnalysis.tempMax}°` : undefined,
        icon: 'navigate-outline',
        color: C.accent
      };
    }

    // 15 Gün Kuralı
    if (daysUntilStart > 15) {
      return {
        status: 'far-future',
        msg: `Seyahatine ${daysUntilStart} gün kaldı. Hava tahmini tutarlılığı için detaylı seyahat analizi, seyahate 15 gün kaldığında hazırlanacaktır.`,
        icon: 'time-outline',
        color: C.textMuted
      };
    }

    // Analiz varsa göster
    if (lastAnalysis) {
      return {
        status: 'ready',
        msg: lastAnalysis.text,
        temp: `${lastAnalysis.tempMin}° / ${lastAnalysis.tempMax}°`,
        summary: lastAnalysis.summary,
        precipProb: lastAnalysis.precipProb,
        color: lastAnalysis.precipProb > 30 ? "#3B82F6" : C.accent,
        icon: lastAnalysis.precipProb > 30 ? 'rainy-outline' : 'sunny-outline',
        packing: lastAnalysis.text.includes("kıyafet") ? "Detaylı öneri metinde" : "Standart"
      };
    }

    return {
      status: 'future-pending',
      msg: daysUntilStart === 0 ? "Seyahatiniz bugün başlıyor." :
           daysUntilStart === 1 ? "Seyahatinize yarın çıkıyorsunuz." :
           `Seyahatinize ${daysUntilStart} gün kaldı.`,
      icon: 'sparkles-outline',
      color: C.accent
    };
  }, [plan.lastAnalysis, plan.startDate, C, isPastTrip, isActiveTrip, isArchived, today, plan.city, plan.endDate]);

  const handleReAnalyzeClick = async () => {
    setIsAnalyzing(true);
    await onReAnalyze();
    setIsAnalyzing(false);
  };

  const diffDays = Math.ceil(Math.abs(new Date(plan.endDate).getTime() - new Date(plan.startDate).getTime()) / (1000 * 60 * 60 * 24)) + 1;

  return (
    <Animated.View style={[styles.card, { backgroundColor: isArchived ? 'rgba(255,255,255,0.02)' : 'rgba(255,255,255,0.03)', borderColor: 'rgba(255,255,255,0.08)', opacity: (isPastTrip && !isArchived) ? 0.8 : 1, transform: [{ translateY: slideAnim }] }]}>
      <LinearGradient colors={['rgba(255,255,255,0.05)', 'transparent']} start={{x:0, y:0}} end={{x:1, y:1}} style={StyleSheet.absoluteFill} />
      <View style={styles.cardHeader}>
        <View style={styles.cardHeaderLeft}>
          <View style={[styles.routeIconBox, { backgroundColor: (analysis?.color || C.accent) + '20' }]}><Icon name={isArchived ? "archive" : "navigate-outline"} size={20} color={analysis?.color || C.accent} /></View>
          <View>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                <Text style={[styles.cardCity, { color: C.text }]}>{plan.city}</Text>
                {isPastTrip && !isArchived && <View style={styles.completedBadge}><Text style={styles.completedBadgeText}>TAMAMLANDI</Text></View>}
            </View>
            <Text style={[styles.cardDates, { color: C.textSecondary }]}>{plan.startDate} • {diffDays} Gün</Text>
          </View>
        </View>
        <View style={styles.weatherSummary}>{analysis?.temp && <><Text style={[styles.cardTemp, { color: C.text }]}>{analysis.temp}</Text><Icon name={analysis.icon as any} size={18} color={analysis.color} /></>}</View>
      </View>

      <TravelAiRecommendationSection analysis={analysis} isFetching={isAnalyzing} isPast={isPastTrip || isArchived} />

      <View style={styles.cardFooter}>
        <View style={styles.footerLeft}><Icon name={typeInfo.icon} size={14} color={C.textMuted} /><Text style={[styles.footerTypeText, { color: C.textMuted }]}>{typeInfo.label}</Text></View>
        <View style={styles.cardActions}>
          <TouchableOpacity style={styles.actionBtn} onPress={onEdit}><Icon name="pencil" size={16} color={C.textSecondary} /><Text style={[styles.actionBtnText, { color: C.textSecondary }]}>Düzenle</Text></TouchableOpacity>
          {(!isPastTrip && !isArchived) ? (
            <TouchableOpacity style={styles.actionBtn} onPress={handleReAnalyzeClick} disabled={isAnalyzing}>
              {isAnalyzing ? (
                <Text style={[styles.actionBtnText, { color: C.textMuted }]}>Analiz ediliyor...</Text>
              ) : (
                <><Icon name="sparkles" size={16} color={C.accent} /><Text style={[styles.actionBtnText, { color: C.accent }]}>Yeniden Analiz</Text></>
              )}
            </TouchableOpacity>
          ) : (
            <TouchableOpacity style={styles.actionBtn} onPress={onShowDetail}><Icon name="stats-chart-outline" size={16} color={C.accent} /><Text style={[styles.actionBtnText, { color: C.accent }]}>Geçmiş Özeti</Text></TouchableOpacity>
          )}
          {isPastTrip && !isArchived && <TouchableOpacity style={[styles.actionBtn, styles.archiveActionBtn]} onPress={onArchive}><Icon name="archive-outline" size={16} color="#60A5FA" /><Text style={[styles.actionBtnText, { color: '#60A5FA' }]}>Arşivle</Text></TouchableOpacity>}
          {isArchived && <TouchableOpacity style={styles.actionBtn} onPress={onUnarchive}><Icon name="arrow-undo-outline" size={16} color={C.textMuted} /><Text style={[styles.actionBtnText, { color: C.textMuted }]}>Geri Al</Text></TouchableOpacity>}
          <TouchableOpacity style={styles.actionBtn} onPress={onDelete}><Icon name="trash" size={16} color="#EF444499" /><Text style={[styles.actionBtnText, { color: '#EF444499' }]}>Sil</Text></TouchableOpacity>
        </View>
      </View>
    </Animated.View>
  );
}

/**
 * AI Önerileri Bölümü
 */
function TravelAiRecommendationSection({ analysis, isFetching, isPast }: { analysis: any; isFetching: boolean, isPast: boolean }) {
  const C = useColors();
  if (isFetching) return <View style={styles.aiSectionLoading}><Text style={{ color: C.textMuted, fontSize: 12 }}>Yapay zeka rotayı analiz ediyor...</Text></View>;
  if (!analysis) return null;

  return (
    <View style={styles.aiSection}>
      <View style={styles.aiTitleRow}><Icon name={isPast ? "information-circle-outline" : "sparkles-outline"} size={14} color={isPast ? C.textMuted : C.accent} /><Text style={[styles.aiTitle, { color: isPast ? C.textMuted : C.accent }]}>{isPast ? "BİLGİLENDİRME" : "HAVAMANIA AI ÖNERİLERİ"}</Text></View>
      <View style={styles.aiGrid}>
        {!isPast && (
          <><View style={[styles.aiMiniCard, { backgroundColor: 'rgba(255,255,255,0.02)' }]}><View style={[styles.aiMiniIcon, { backgroundColor: '#3B82F620' }]}><Icon name="airplane-outline" size={12} color="#3B82F6" /></View><Text style={[styles.aiMiniText, { color: C.text }]} numberOfLines={2}>{analysis.status === 'ready' ? 'Gidiş planı için hava müsait.' : 'Plan bekleniyor.'}</Text></View>
          <View style={[styles.aiMiniCard, { backgroundColor: 'rgba(255,255,255,0.02)' }]}><View style={[styles.aiMiniIcon, { backgroundColor: '#10B98120' }]}><Icon name="briefcase-outline" size={12} color="#10B981" /></View><Text style={[styles.aiMiniText, { color: C.text }]} numberOfLines={2}>Valiz: {analysis.packing ? (Array.isArray(analysis.packing) ? analysis.packing.slice(0, 2).join(', ') : analysis.packing) : 'Standart'}</Text></View></>
        )}
        <View style={[styles.aiMiniCard, { backgroundColor: 'rgba(255,255,255,0.02)', flexBasis: '100%' }]}><View style={[styles.aiMiniIcon, { backgroundColor: (isPast ? C.textMuted : analysis.color) + '20' }]}><Icon name={isPast ? "document-text-outline" : "bulb-outline"} size={12} color={isPast ? C.textMuted : analysis.color} /></View><Text style={[styles.aiMiniText, { color: C.text }]}>{analysis.msg}</Text></View>
      </View>
    </View>
  );
}

/**
 * Geçmiş Seyahat Detay Modalı
 */
function PastTripDetailModal({ visible, plan, onClose }: { visible: boolean; plan: TravelPlan | null; onClose: () => void }) {
  const C = useColors();
  if (!plan) return null;
  const typeInfo = TRAVEL_TYPES.find(t => t.type === plan.type) || TRAVEL_TYPES[5];

  return (
    <Modal animationType="fade" transparent={true} visible={visible} onRequestClose={onClose}>
      <View style={styles.modalOverlay}>
        <View style={[styles.detailContent, { backgroundColor: C.bgSecondary, borderColor: C.border, borderWidth: 1 }]}>
          <View style={styles.modalHeader}>
            <Text style={[styles.modalTitle, { color: C.text }]}>Geçmiş Seyahat Özeti</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}><Icon name="close" size={24} color={C.textSecondary} /></TouchableOpacity>
          </View>
          <ScrollView showsVerticalScrollIndicator={false}>
            <View style={styles.detailHeader}>
              <Text style={[styles.detailCity, { color: C.text }]}>{plan.city}</Text>
              <Text style={[styles.detailDates, { color: C.textSecondary }]}>{plan.startDate} / {plan.endDate}</Text>
              <View style={[styles.typeChipForm, { backgroundColor: 'rgba(255,255,255,0.05)', alignSelf: 'flex-start', marginTop: 12 }]}>
                <Icon name={typeInfo.icon} size={16} color={C.accent} /><Text style={[styles.typeText, { color: C.textSecondary }]}>{typeInfo.label}</Text>
              </View>
            </View>

            <View style={[styles.detailSection, { backgroundColor: 'rgba(255,255,255,0.03)' }]}>
              <View style={styles.detailTitleRow}><Icon name="cloud-done-outline" size={18} color={C.accent} /><Text style={[styles.detailSectionTitle, { color: C.text }]}>HAVA DURUMU KAYITLARI</Text></View>
              <Text style={[styles.detailText, { color: C.textSecondary }]}>Bu seyahat için kayıtlı detaylı hava geçmişi bulunmuyor. Ancak seyahat notların ve önceki analiz önerilerin saklandı.</Text>
            </View>

            {plan.note && (
              <View style={[styles.detailSection, { backgroundColor: 'rgba(255,255,255,0.03)' }]}>
                <View style={styles.detailTitleRow}><Icon name="bookmark-outline" size={18} color={C.accent} /><Text style={[styles.detailSectionTitle, { color: C.text }]}>NOTLARINIZ</Text></View>
                <Text style={[styles.detailText, { color: C.textSecondary }]}>{plan.note}</Text>
              </View>
            )}

            <TouchableOpacity style={[styles.saveButton, { backgroundColor: C.accent, marginTop: 40 }]} onPress={onClose}>
                <LinearGradient colors={[C.accent, C.accentDark]} style={StyleSheet.absoluteFill} start={{x: 0, y: 0}} end={{x: 1, y: 0}} />
                <Text style={styles.saveButtonText}>Bu Rotayı Tekrar Planla</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

/**
 * Boş Durum Bileşeni
 */
function TravelEmptyState({ onAdd }: { onAdd: () => void }) {
  const C = useColors();
  return (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIllustration}><View style={[styles.glowCircle, { backgroundColor: C.accent }]} /><Icon name="map-outline" size={80} color={C.textSecondary} style={{ opacity: 0.5 }} /></View>
      <Text style={[styles.emptyText, { color: C.text }]}>Henüz seyahat planın yok</Text>
      <Text style={[styles.emptySubText, { color: C.textSecondary }]}>Yeni bir rota oluşturarak hava durumuna göre akıllı öneriler alabilirsin.</Text>
      <TouchableOpacity style={[styles.emptyBtn, { backgroundColor: C.accent }]} onPress={onAdd}><Text style={styles.emptyBtnText}>Yeni Rota Oluştur</Text></TouchableOpacity>
    </View>
  );
}

/**
 * Premium Floating Action Button
 */
function PremiumRouteButton({ onPress }: { onPress: () => void }) {
  const C = useColors();
  const scale = useRef(new Animated.Value(1)).current;
  const handlePressIn = () => Animated.spring(scale, { toValue: 0.9, useNativeDriver: true }).start();
  const handlePressOut = () => Animated.spring(scale, { toValue: 1, friction: 3, tension: 40, useNativeDriver: true }).start();
  return (
    <Pressable onPress={onPress} onPressIn={handlePressIn} onPressOut={handlePressOut} style={styles.fabWrapper}>
      <Animated.View style={[styles.fab, { transform: [{ scale }] }]}>
        <LinearGradient colors={[C.accent, C.accentDark]} style={StyleSheet.absoluteFill} start={{x: 0, y: 0}} end={{x: 1, y: 1}} />
        <View style={styles.fabContent}><Icon name="add" size={24} color="#FFF" /><Text style={styles.fabText}>YENİ ROTA</Text></View>
      </Animated.View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: Spacing.xl, paddingTop: 20 },
  title: { fontSize: 32, fontWeight: '800', letterSpacing: -1 },
  subtitle: { fontSize: 14, marginTop: 4, opacity: 0.7 },
  filterContainer: { flexDirection: 'row', paddingHorizontal: Spacing.xl, gap: 10, marginBottom: Spacing.lg },
  filterChip: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: Radius.full },
  activeFilterChip: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  filterText: { fontSize: 13, fontWeight: '700' },
  listContent: { padding: Spacing.xl, paddingBottom: 120 },

  // Card Styles
  card: { borderRadius: 24, borderWidth: 1, marginBottom: 20, overflow: 'hidden', padding: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.1, shadowRadius: 20, elevation: 4 },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  cardHeaderLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  routeIconBox: { width: 44, height: 44, borderRadius: 14, justifyContent: 'center', alignItems: 'center' },
  cardCity: { fontSize: 22, fontWeight: '800' },
  cardDates: { fontSize: 12, fontWeight: '600', marginTop: 2 },
  completedBadge: { backgroundColor: 'rgba(59, 130, 246, 0.1)', borderColor: 'rgba(59, 130, 246, 0.2)', borderWidth: 1, paddingHorizontal: 8, paddingVertical: 2, borderRadius: 6, marginLeft: 4 },
  completedBadgeText: { fontSize: 9, fontWeight: '900', letterSpacing: 0.5, color: '#60A5FA' },
  weatherSummary: { alignItems: 'flex-end', gap: 4 },
  cardTemp: { fontSize: 18, fontWeight: '800' },

  // AI Section
  aiSection: { marginTop: 4, marginBottom: 20 },
  aiTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 12 },
  aiTitle: { fontSize: 10, fontWeight: '900', letterSpacing: 1.5 },
  aiGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  aiMiniCard: { flex: 1, minWidth: '45%', padding: 10, borderRadius: 12, flexDirection: 'row', alignItems: 'center', gap: 8 },
  aiMiniIcon: { width: 24, height: 24, borderRadius: 8, justifyContent: 'center', alignItems: 'center' },
  aiMiniText: { fontSize: 11, fontWeight: '600', flex: 1, lineHeight: 15 },
  aiSectionLoading: { paddingVertical: 20, alignItems: 'center' },

  cardFooter: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: 16, borderTopWidth: 1, borderTopColor: 'rgba(255,255,255,0.05)' },
  footerLeft: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  footerTypeText: { fontSize: 12, fontWeight: '600' },
  cardActions: { flexDirection: 'row', gap: 8 },
  actionBtn: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  archiveActionBtn: { backgroundColor: 'rgba(59, 130, 246, 0.08)', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 8 },
  actionBtnText: { fontSize: 11, fontWeight: '700' },

  // Empty State
  emptyContainer: { alignItems: 'center', justifyContent: 'center', marginTop: 60, paddingHorizontal: 40 },
  emptyIllustration: { width: 160, height: 160, justifyContent: 'center', alignItems: 'center', marginBottom: 24 },
  glowCircle: { position: 'absolute', width: 100, height: 100, borderRadius: 50, opacity: 0.1, transform: [{ scale: 1.5 }] },
  emptyText: { fontSize: 20, fontWeight: '800', marginBottom: 12, textAlign: 'center' },
  emptySubText: { fontSize: 14, textAlign: 'center', lineHeight: 22, marginBottom: 32 },
  emptyBtn: { paddingHorizontal: 24, paddingVertical: 14, borderRadius: 16, elevation: 4 },
  emptyBtnText: { color: '#FFF', fontWeight: '800', fontSize: 15 },

  // FAB
  fabWrapper: { position: 'absolute', bottom: 30, left: 20, right: 20, alignItems: 'center' },
  fab: { height: 56, borderRadius: 28, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, shadowColor: '#3B82F6', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.4, shadowRadius: 12, elevation: 8, overflow: 'hidden' },
  fabContent: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  fabText: { color: '#FFF', fontWeight: '900', fontSize: 14, letterSpacing: 1 },

  // Modal & Detail
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.85)', justifyContent: 'flex-end' },
  modalContent: { borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: Spacing.xl, maxHeight: '92%' },
  detailContent: { borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: Spacing.xl, maxHeight: '85%' },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 },
  modalTitle: { fontSize: 22, fontWeight: '800' },
  closeBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.05)', justifyContent: 'center', alignItems: 'center' },
  detailHeader: { marginBottom: 24 },
  detailCity: { fontSize: 28, fontWeight: '900' },
  detailDates: { fontSize: 14, fontWeight: '600', opacity: 0.6 },
  detailSection: { padding: 20, borderRadius: 24, marginBottom: 16 },
  detailTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 12 },
  detailSectionTitle: { fontSize: 12, fontWeight: '900', letterSpacing: 1 },
  detailText: { fontSize: 14, lineHeight: 22, fontWeight: '500' },
  inputGroup: { marginBottom: 20 },
  inputLabel: { fontSize: 10, fontWeight: '900', letterSpacing: 1.5, marginBottom: 10, marginLeft: 4 },
  input: { height: 56, borderRadius: 16, paddingHorizontal: 16, borderWidth: 1, fontSize: 16, fontWeight: '600' },
  textArea: { height: 100, paddingTop: 16, textAlignVertical: 'top' },
  dateRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  selectedCityBox: { height: 56, borderRadius: 16, paddingHorizontal: 16, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  searchDropdown: { position: 'absolute', top: 90, left: 0, right: 0, borderRadius: 16, borderWidth: 1, zIndex: 1000, elevation: 10, padding: 8 },
  searchItem: { padding: 14, borderRadius: 10 },
  typeChipForm: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 12, borderRadius: 14 },
  typeText: { fontSize: 14, fontWeight: '700' },
  saveButton: { height: 60, borderRadius: 18, justifyContent: 'center', alignItems: 'center', marginTop: 20, overflow: 'hidden' },
  saveButtonText: { color: '#FFF', fontSize: 18, fontWeight: '800', zIndex: 1 },
});
