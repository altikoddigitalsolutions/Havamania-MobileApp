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
  StatusBar,
  KeyboardAvoidingView,
  Platform,
  Animated,
  Pressable,
  ActivityIndicator,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useTheme } from '../theme';
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
import { getDailyWeather } from '../services/weatherApi';
import { analyzeTravelPlan, TravelAnalysisResult, generateTravelHistorySummary } from '../services/travelAnalysisService';
import { getTripStatus, TripStatus, getTripDayCount } from '../utils/dateUtils';

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

const formatDateRange = (start: string, end: string) => {
  if (!start || !end) return '';
  const MONTHS = ["Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"];
  const format = (d: string) => {
    const parts = d.split('-');
    if (parts.length < 3) return d;
    return `${parseInt(parts[2])} ${MONTHS[parseInt(parts[1]) - 1]}`;
  };
  return `${format(start)} - ${format(end)}`;
};

export function TravelCalendarScreen() {
  const { colors: C, spacing, fontSize, responsive, layout, radius } = useTheme();
  const { plans, fetchPlans, addPlan, removePlan, updatePlan, archivePlan, unarchivePlan } = useTravelStore();

  useEffect(() => {
    fetchPlans();
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
      result = plans.filter(p => {
        const status = getTripStatus(today, p.startDate, p.endDate);
        return status !== TripStatus.PAST && !p.isArchived;
      });
    } else if (filter === 'Past') {
      result = plans.filter(p => {
        const status = getTripStatus(today, p.startDate, p.endDate);
        return status === TripStatus.PAST && !p.isArchived;
      });
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

    if (editingId) {
      updatePlan(editingId, payload);
    } else {
      addPlan(payload);
    }
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

  const s = makeStyles(C, spacing, fontSize, responsive, layout, radius);

  return (
    <SafeAreaView style={s.container}>
      <StatusBar barStyle="light-content" />
      <LinearGradient colors={[C.bg, C.bgSecondary]} style={StyleSheet.absoluteFill} />
      <SeasonalBackground />

      <View style={s.centeredContainer}>
        <View style={s.header}>
          <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
            <View style={{ flex: 1 }}>
              <Text style={s.title}>Seyahat Takvimi</Text>
              <Text style={s.subtitle}>Hava durumuna göre akıllı seyahat planlayıcı</Text>
            </View>
            <TouchableOpacity onPress={() => setFilter('Archived')} style={s.archiveBtn}>
              <Icon name="archive-outline" size={24} color={filter === 'Archived' ? C.accent : C.textSecondary} />
            </TouchableOpacity>
          </View>
        </View>

        <View style={s.filterContainer}>
          {(['Upcoming', 'Past', 'Archived'] as const).map((f) => (
            <TouchableOpacity
              key={f}
              onPress={() => setFilter(f)}
              style={[
                s.filterChip,
                { backgroundColor: filter === f ? C.accent : 'rgba(255,255,255,0.05)' },
                filter === f && s.activeFilterChip
              ]}
            >
              <Text style={[s.filterText, { color: filter === f ? '#FFF' : C.textSecondary }]}>
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
              onReAnalyze={() => {}}
            />
          )}
          contentContainerStyle={s.listContent}
          ListEmptyComponent={<TravelEmptyState onAdd={() => setModalVisible(true)} />}
        />
      </View>

      <PremiumRouteButton onPress={() => setModalVisible(true)} />

      {/* Rota Oluşturma Modalı */}
      <Modal animationType="slide" transparent={true} visible={modalVisible} onRequestClose={resetForm}>
        <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={s.modalOverlay}>
          <View style={s.modalContent}>
            <View style={s.modalHeader}>
              <Text style={s.modalTitle}>{editingId ? 'Seyahati Düzenle' : 'Yeni Seyahat Planla'}</Text>
              <TouchableOpacity onPress={resetForm} style={s.closeBtn}><Icon name="close" size={24} color={C.textSecondary} /></TouchableOpacity>
            </View>
            <ScrollView showsVerticalScrollIndicator={false}>
              <View style={s.inputGroup}>
                <Text style={s.inputLabel}>NEREYE GİDİYORSUN?</Text>
                {selectedCity ? (
                  <TouchableOpacity style={[s.selectedCityBox, { borderColor: C.accent }]} onPress={() => setSelectedCity(null)}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}><Icon name="location" size={18} color={C.accent} /><Text style={{ color: C.text, fontWeight: '600' }}>{selectedCity.name}</Text></View>
                    <Icon name="close-circle" size={18} color={C.textMuted} />
                  </TouchableOpacity>
                ) : (
                  <View>
                    <TextInput style={s.input} placeholder="Şehir ara (örn: Antalya)" placeholderTextColor={C.textMuted} value={citySearch} onChangeText={setCitySearch} />
                    {citySearch.length > 0 && (
                      <View style={s.searchDropdown}>
                        {filteredCities.map((item) => (
                          <TouchableOpacity key={item.name} style={s.searchItem} onPress={() => { setSelectedCity(item); setCitySearch(''); }}><Text style={{ color: C.text }}>{item.name}</Text></TouchableOpacity>
                        ))}
                      </View>
                    )}
                  </View>
                )}
              </View>
              <View style={s.dateRow}>
                <View style={{ flex: 1 }}><Text style={s.inputLabel}>BAŞLANGIÇ</Text><TextInput style={s.input} placeholder="YYYY-MM-DD" placeholderTextColor={C.textMuted} value={startDate} onChangeText={setStartDate} /></View>
                <View style={{ flex: 1 }}><Text style={s.inputLabel}>BİTİŞ</Text><TextInput style={s.input} placeholder="YYYY-MM-DD" placeholderTextColor={C.textMuted} value={endDate} onChangeText={setEndDate} /></View>
              </View>
              <View style={s.inputGroup}>
                <Text style={s.inputLabel}>SEYAHAT KONSEPTİ</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 10 }}>
                  {TRAVEL_TYPES.map((t) => (
                    <TouchableOpacity key={t.type} onPress={() => setTravelType(t.type)} style={[s.typeChipForm, { backgroundColor: travelType === t.type ? C.accent : 'rgba(255,255,255,0.05)' }]}>
                      <Icon name={t.icon} size={16} color={travelType === t.type ? '#FFF' : C.textSecondary} />
                      <Text style={[s.typeText, { color: travelType === t.type ? '#FFF' : C.textSecondary }]}>{t.label}</Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              </View>
              <View style={s.inputGroup}>
                <Text style={s.inputLabel}>ÖZEL NOTLAR</Text>
                <TextInput style={[s.input, s.textArea]} placeholder="Havalimanı transferi, otel adı vb..." placeholderTextColor={C.textMuted} multiline numberOfLines={3} value={note} onChangeText={setNote} />
              </View>
              <TouchableOpacity style={s.saveButton} onPress={handleSavePlan}>
                <LinearGradient colors={[C.accent, C.accentDark]} style={StyleSheet.absoluteFill} start={{x: 0, y: 0}} end={{x: 1, y: 0}} />
                <Text style={s.saveButtonText}>{editingId ? 'Güncelle' : 'Planı Oluştur'}</Text>
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
  const { colors: C, fontSize, spacing, radius } = useTheme();
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

  const tripStatus = useMemo(() => getTripStatus(today, plan.startDate, plan.endDate), [today, plan.startDate, plan.endDate]);
  const isPastTrip = tripStatus === TripStatus.PAST;
  const isArchived = plan.isArchived;

  const { data: weatherData, isFetching } = useQuery({
    queryKey: ['weather', plan.lat, plan.lon, plan.startDate],
    queryFn: () => getDailyWeather(plan.lat, plan.lon, 14),
    staleTime: 15 * 60 * 1000,
    enabled: !isPastTrip && tripStatus !== TripStatus.UPCOMING_LOCKED && !isArchived,
  });

  const analysis = useMemo((): TravelAnalysisResult | null => {
    if (isPastTrip || isArchived) {
        return {
          status: 'past',
          score: 100,
          averageTemp: 0,
          maxPrecipProbability: 0,
          precipitationRiskText: 'Yok',
          summary: '',
          advice: isArchived
              ? `${plan.city} seyahatin arşivlendi.`
              : `${plan.city} seyahatin tamamlandı.`,
          icon: isArchived ? 'archive' : 'checkmark-circle',
          emoji: '✅',
          color: C.textMuted,
          packing: [],
          activities: [],
          localTips: []
        };
    }

    if (tripStatus === TripStatus.UPCOMING_LOCKED) {
        return {
          status: 'far-future',
          score: 0,
          averageTemp: 0,
          maxPrecipProbability: 0,
          precipitationRiskText: 'Bilinmiyor',
          summary: '',
          advice: `${plan.city} seyahatin kayıt altında. Detaylı hava analizi seyahate 15 gün kala burada belirecek.`,
          icon: 'time-outline',
          emoji: '⏳',
          color: C.textMuted,
          packing: [],
          activities: [],
          localTips: []
        };
    }

    if (!weatherData) return null;

    const result = analyzeTravelPlan(weatherData.items, plan.startDate, plan.endDate, plan.type, plan.city);

    if (tripStatus === TripStatus.UPCOMING_ACTIVE) {
        result.advice = `${plan.city} seyahatin yaklaşıyor! ${result.advice}`;
    }

    if (tripStatus === TripStatus.ONGOING) {
        result.status = 'active';
        const dayCount = getTripDayCount(today, plan.startDate);
        result.advice = `${plan.city} seyahatin şu anda devam ediyor. Bugün seyahatinin ${dayCount}. günü. ${result.advice}`;
    }

    return result;
  }, [weatherData, plan.startDate, plan.endDate, plan.type, C, isPastTrip, isArchived, tripStatus, plan.city, today]);

  const s = cardStyles(C, spacing, fontSize, radius);

  return (
    <Animated.View style={[s.card, { opacity: (isPastTrip && !isArchived) ? 0.7 : 1, transform: [{ translateY: slideAnim }] }]}>
      <LinearGradient colors={['rgba(255,255,255,0.05)', 'transparent']} start={{x:0, y:0}} end={{x:1, y:1}} style={StyleSheet.absoluteFill} />
      <View style={s.cardHeader}>
        <View style={s.cardHeaderLeft}>
          <View style={[s.routeIconBox, { backgroundColor: (analysis?.color || C.accent) + '15' }]}><Icon name={isArchived ? "archive" : typeInfo.icon} size={20} color={analysis?.color || C.accent} /></View>
          <View style={{flex: 1}}>
            <View style={{ flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 4 }}>
                <Text style={[s.cardCity, { color: C.text }]} numberOfLines={1} ellipsizeMode="tail">{plan.city}</Text>
                {tripStatus === TripStatus.ONGOING && !isArchived && <View style={[s.completedBadge, { backgroundColor: '#10B98120', borderColor: '#10B98140' }]}><Text style={[s.completedBadgeText, { color: '#10B981' }]}>DEVAM</Text></View>}
                {isPastTrip && !isArchived && <View style={s.completedBadge}><Text style={s.completedBadgeText}>BİTTİ</Text></View>}
            </View>
            <Text style={[s.cardDates, { color: C.textSecondary }]}>{formatDateRange(plan.startDate, plan.endDate)}</Text>
          </View>
        </View>
        {(analysis?.status === 'ready' || analysis?.status === 'active') && tripStatus !== TripStatus.UPCOMING_LOCKED ? (
          <View style={s.weatherSummary}>
            <Text style={[s.cardTemp, { color: C.text }]}>{analysis.emoji} {analysis.averageTemp}°</Text>
          </View>
        ) : null}
      </View>

      {analysis && (analysis.status === 'ready' || analysis.status === 'active') && tripStatus !== TripStatus.UPCOMING_LOCKED && (
        <View style={s.scoreContainer}>
          <View style={s.scoreInfo}>
            <Text style={[s.scoreLabel, { color: C.textSecondary }]}>SKOR</Text>
            <Text style={[s.scoreValue, { color: analysis.color }]}>{analysis.score}</Text>
          </View>
          <View style={s.adviceContainer}>
            <Text style={[s.adviceText, { color: C.text }]} numberOfLines={2}>{analysis.advice}</Text>
          </View>
        </View>
      )}

      {(!analysis || analysis.status === 'pending' || analysis.status === 'far-future' || analysis.status === 'past' || tripStatus === TripStatus.UPCOMING_LOCKED) && (
        <TravelAiRecommendationSection analysis={analysis} isFetching={isFetching} isPast={isPastTrip || isArchived} tripStatus={tripStatus} />
      )}

      <View style={s.cardFooter}>
        <View style={s.cardActions}>
          <TouchableOpacity
            style={[s.primaryAction, { backgroundColor: C.accent }]}
            onPress={onShowDetail}
          >
            <Text style={s.primaryActionText}>{isPastTrip || isArchived ? 'Özeti Gör' : 'Detayları Gör'}</Text>
          </TouchableOpacity>
          <TouchableOpacity style={s.secondaryAction} onPress={onEdit}>
            <Icon name="ellipsis-horizontal" size={20} color={C.textSecondary} />
          </TouchableOpacity>
        </View>
        <TouchableOpacity onPress={() => onDelete()} style={{padding: 8}}><Icon name="trash-outline" size={18} color="#EF444466" /></TouchableOpacity>
      </View>
    </Animated.View>
  );
}

function TravelAiRecommendationSection({ analysis, isFetching, isPast, tripStatus }: { analysis: any; isFetching: boolean, isPast: boolean, tripStatus: TripStatus }) {
  const { colors: C, fontSize, spacing } = useTheme();
  if (isFetching) return <View style={styles.aiSectionLoading}><ActivityIndicator size="small" color={C.accent} /><Text style={{ color: C.textMuted, fontSize: 12, marginTop: 8 }}>Rotalar analiz ediliyor...</Text></View>;
  if (!analysis) return null;

  const isLocked = tripStatus === TripStatus.UPCOMING_LOCKED;

  return (
    <View style={styles.aiSection}>
      <View style={styles.aiTitleRow}><Icon name={isPast ? "information-circle-outline" : "sparkles-outline"} size={14} color={isPast ? C.textMuted : C.accent} /><Text style={[styles.aiTitle, { color: isPast ? C.textMuted : C.accent }]}>{isPast ? "BİLGİLENDİRME" : isLocked ? "SEYAHAT KAYDI" : "AI ÖNERİLERİ"}</Text></View>
      <View style={styles.aiGrid}>
        {!isPast && !isLocked && (
          <><View style={[styles.aiMiniCard, { backgroundColor: 'rgba(255,255,255,0.02)' }]}><View style={[styles.aiMiniIcon, { backgroundColor: '#3B82F620' }]}><Icon name="airplane-outline" size={12} color="#3B82F6" /></View><Text style={[styles.aiMiniText, { color: C.text }]} numberOfLines={2}>{analysis.status === 'ready' || analysis.status === 'active' ? 'Gidiş planı için hava müsait.' : 'Plan bekleniyor.'}</Text></View>
          <View style={[styles.aiMiniCard, { backgroundColor: 'rgba(255,255,255,0.02)' }]}><View style={[styles.aiMiniIcon, { backgroundColor: '#10B98120' }]}><Icon name="briefcase-outline" size={12} color="#10B981" /></View><Text style={[styles.aiMiniText, { color: C.text }]} numberOfLines={2}>Valiz: {analysis.packing?.slice(0, 2).join(', ') || 'Standart'}</Text></View></>
        )}
        <View style={[styles.aiMiniCard, { backgroundColor: 'rgba(255,255,255,0.02)', flexBasis: '100%' }]}><View style={[styles.aiMiniIcon, { backgroundColor: (isPast || isLocked ? C.textMuted : analysis.color) + '20' }]}><Icon name={isPast ? "document-text-outline" : isLocked ? "calendar-outline" : "bulb-outline"} size={12} color={isPast || isLocked ? C.textMuted : analysis.color} /></View><Text style={[styles.aiMiniText, { color: C.text }]}>{analysis.advice}</Text></View>
      </View>
    </View>
  );
}

function PastTripDetailModal({ visible, plan, onClose }: { visible: boolean; plan: TravelPlan | null; onClose: () => void }) {
  const { colors: C, spacing, fontSize, radius, layout, responsive } = useTheme();
  if (!plan) return null;

  const { data: historyWeather } = useQuery({
    queryKey: ['weather', 'history', plan.lat, plan.lon, plan.startDate],
    queryFn: () => getDailyWeather(plan.lat, plan.lon, 14),
    enabled: !!plan,
  });

  const historySummary = useMemo(() => {
    if (!historyWeather) return null;
    return generateTravelHistorySummary(historyWeather.items.filter(i => i.date >= plan.startDate && i.date <= plan.endDate), plan.city);
  }, [historyWeather, plan.startDate, plan.endDate, plan.city]);

  const s = makeStyles(C, spacing, fontSize, responsive, layout, radius);

  return (
    <Modal animationType="fade" transparent={true} visible={visible} onRequestClose={onClose}>
      <View style={s.modalOverlay}>
        <View style={s.detailContent}>
          <View style={s.modalHeader}>
            <Text style={s.modalTitle}>Seyahat Özeti</Text>
            <TouchableOpacity onPress={onClose} style={s.closeBtn}><Icon name="close" size={24} color={C.textSecondary} /></TouchableOpacity>
          </View>
          <ScrollView showsVerticalScrollIndicator={false}>
            <View style={s.detailHeader}>
              <View style={[s.scoreCircle, { borderColor: C.accent }]}>
                <Text style={s.scoreCircleValue}>92</Text>
                <Text style={s.scoreCircleLabel}>SKOR</Text>
              </View>
              <View style={{flex: 1}}>
                <Text style={s.detailCity} numberOfLines={1}>{plan.city}</Text>
                <Text style={s.detailDates}>{formatDateRange(plan.startDate, plan.endDate)}</Text>
              </View>
            </View>

            {historySummary ? (
                <View style={s.historySummaryGrid}>
                    <View style={s.historyItem}>
                        <Icon name="thermometer-outline" size={20} color={C.accent} />
                        <Text style={s.historyLabel}>ORTALAMA</Text>
                        <Text style={[s.historyValue, { color: C.text }]}>{historySummary.averageTemp}°</Text>
                    </View>
                    <View style={s.historyItem}>
                        <Icon name="sunny-outline" size={20} color="#FBBF24" />
                        <Text style={s.historyLabel}>EN GÜNEŞLİ</Text>
                        <Text style={[s.historyValue, { color: '#FBBF24' }]}>{historySummary.sunniestDay}</Text>
                    </View>
                    <View style={s.historyItem}>
                        <Icon name="rainy-outline" size={20} color="#3B82F6" />
                        <Text style={s.historyLabel}>EN YAĞIŞLI</Text>
                        <Text style={[s.historyValue, { color: '#3B82F6' }]}>{historySummary.rainiestDay}</Text>
                    </View>
                </View>
            ) : <ActivityIndicator color={C.accent} />}

            <View style={s.detailSection}>
              <View style={s.detailTitleRow}><Icon name="sparkles-outline" size={18} color={C.accent} /><Text style={s.detailSectionTitle}>ASİSTAN DEĞERLENDİRMESİ</Text></View>
              <Text style={s.detailText}>{historySummary?.evaluation || "Hava durumu kayıtları analiz ediliyor..."}</Text>
            </View>

            {plan.note && (
              <View style={s.detailSection}>
                <View style={s.detailTitleRow}><Icon name="bookmark-outline" size={18} color={C.accent} /><Text style={s.detailSectionTitle}>NOTLARINIZ</Text></View>
                <Text style={s.detailText}>{plan.note}</Text>
              </View>
            )}

            <TouchableOpacity style={[s.saveButton, { marginTop: 40 }]} onPress={onClose}>
                <LinearGradient colors={[C.accent, C.accentDark]} style={StyleSheet.absoluteFill} start={{x: 0, y: 0}} end={{x: 1, y: 0}} />
                <Text style={s.saveButtonText}>Geri Dön</Text>
            </TouchableOpacity>
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

function TravelEmptyState({ onAdd }: { onAdd: () => void }) {
  const { colors: C, spacing, fontSize } = useTheme();
  return (
    <View style={styles.emptyContainer}>
      <View style={styles.emptyIllustration}><View style={[styles.glowCircle, { backgroundColor: C.accent }]} /><Icon name="map-outline" size={80} color={C.textSecondary} style={{ opacity: 0.5 }} /></View>
      <Text style={[styles.emptyText, { color: C.text }]}>Henüz seyahat planın yok</Text>
      <Text style={[styles.emptySubText, { color: C.textSecondary }]}>Yeni bir rota oluşturarak hava durumuna göre akıllı öneriler alabilirsin.</Text>
      <TouchableOpacity style={[styles.emptyBtn, { backgroundColor: C.accent }]} onPress={onAdd}><Text style={styles.emptyBtnText}>Yeni Rota Oluştur</Text></TouchableOpacity>
    </View>
  );
}

function PremiumRouteButton({ onPress }: { onPress: () => void }) {
  const { colors: C } = useTheme();
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

const cardStyles = (C: any, spacing: any, fontSize: any, radius: any) => StyleSheet.create({
  card: { borderRadius: radius.lg, borderWidth: 1, borderColor: 'rgba(255,255,255,0.08)', marginBottom: 20, overflow: 'hidden', padding: 20, backgroundColor: 'rgba(255,255,255,0.03)' },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
  cardHeaderLeft: { flexDirection: 'row', alignItems: 'center', gap: 12, flex: 1 },
  routeIconBox: { width: 44, height: 44, borderRadius: 14, justifyContent: 'center', alignItems: 'center' },
  cardCity: { fontSize: fontSize.lg, fontWeight: '800', flexShrink: 1 },
  cardDates: { fontSize: fontSize.xs, fontWeight: '600', marginTop: 2 },
  completedBadge: { backgroundColor: 'rgba(59, 130, 246, 0.1)', borderColor: 'rgba(59, 130, 246, 0.2)', borderWidth: 1, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 6 },
  completedBadgeText: { fontSize: 8, fontWeight: '900', color: '#60A5FA' },
  weatherSummary: { alignItems: 'flex-end' },
  cardTemp: { fontSize: fontSize.md, fontWeight: '800' },
  scoreContainer: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.02)', borderRadius: 16, padding: 12, marginBottom: 20, borderWidth: 1, borderColor: 'rgba(255,255,255,0.05)' },
  scoreInfo: { borderRightWidth: 1, borderRightColor: 'rgba(255,255,255,0.05)', paddingRight: 12, marginRight: 12, alignItems: 'center' },
  scoreLabel: { fontSize: 8, fontWeight: '900', letterSpacing: 0.5, marginBottom: 2 },
  scoreValue: { fontSize: 20, fontWeight: '900' },
  adviceContainer: { flex: 1 },
  adviceText: { fontSize: 13, fontWeight: '600', lineHeight: 18 },
  cardFooter: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingTop: 16, borderTopWidth: 1, borderTopColor: 'rgba(255,255,255,0.05)' },
  cardActions: { flexDirection: 'row', gap: 12, flex: 1 },
  primaryAction: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  primaryActionText: { color: '#FFF', fontSize: 13, fontWeight: '800' },
  secondaryAction: { width: 44, height: 44, borderRadius: 12, backgroundColor: 'rgba(255,255,255,0.05)', alignItems: 'center', justifyContent: 'center' },
});

const makeStyles = (C: any, spacing: any, fontSize: any, responsive: any, layout: any, radius: any) => StyleSheet.create({
  container: { flex: 1 },
  centeredContainer: { flex: 1, alignSelf: 'center', width: '100%', maxWidth: layout.maxContentWidth },
  header: { padding: spacing.pagePadding, paddingTop: 20 },
  title: { fontSize: fontSize.xxxl, fontWeight: '800', letterSpacing: -1, color: C.text },
  subtitle: { fontSize: fontSize.sm, marginTop: 4, opacity: 0.7, color: C.textSecondary },
  filterContainer: { flexDirection: 'row', paddingHorizontal: spacing.pagePadding, gap: 10, marginBottom: spacing.md },
  filterChip: { flex: 1, paddingVertical: 8, borderRadius: Radius.full, alignItems: 'center', justifyContent: 'center' },
  activeFilterChip: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  filterText: { fontSize: fontSize.xs, fontWeight: '700' },
  listContent: { padding: spacing.pagePadding, paddingBottom: 120 },
  archiveBtn: { padding: 8 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.85)', justifyContent: 'flex-end' },
  modalContent: { borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: spacing.lg, maxHeight: '92%', alignSelf: 'center', width: '100%', maxWidth: layout.maxContentWidth },
  detailContent: { borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: spacing.lg, maxHeight: '85%', alignSelf: 'center', width: '100%', maxWidth: layout.maxContentWidth, backgroundColor: C.bgSecondary, borderColor: C.border, borderWidth: 1 },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 },
  modalTitle: { fontSize: fontSize.xxl, fontWeight: '800', color: C.text },
  closeBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.05)', justifyContent: 'center', alignItems: 'center' },
  detailHeader: { flexDirection: 'row', alignItems: 'center', gap: 20, marginBottom: 24 },
  scoreCircle: { width: 80, height: 80, borderRadius: 40, borderWidth: 4, justifyContent: 'center', alignItems: 'center' },
  scoreCircleValue: { fontSize: 24, fontWeight: '900', color: C.text },
  scoreCircleLabel: { fontSize: 8, fontWeight: '800', marginTop: -2, color: C.textSecondary },
  detailCity: { fontSize: fontSize.xxl, fontWeight: '900', color: C.text },
  detailDates: { fontSize: fontSize.sm, fontWeight: '600', opacity: 0.6, color: C.textSecondary },
  detailSection: { padding: 20, borderRadius: 24, marginBottom: 16, backgroundColor: 'rgba(255,255,255,0.03)' },
  detailTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 12 },
  detailSectionTitle: { fontSize: 12, fontWeight: '900', letterSpacing: 1, color: C.text },
  detailText: { fontSize: 14, lineHeight: 22, fontWeight: '500', color: C.textSecondary },
  inputGroup: { marginBottom: 20 },
  inputLabel: { fontSize: 10, fontWeight: '900', letterSpacing: 1.5, marginBottom: 10, marginLeft: 4, color: C.textSecondary },
  input: { height: 56, borderRadius: 16, paddingHorizontal: 16, borderWidth: 1, fontSize: 16, fontWeight: '600', backgroundColor: 'rgba(255,255,255,0.05)', color: C.text, borderColor: C.border },
  textArea: { height: 100, paddingTop: 16, textAlignVertical: 'top' },
  dateRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  selectedCityBox: { height: 56, borderRadius: 16, paddingHorizontal: 16, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: 'rgba(255,255,255,0.05)' },
  searchDropdown: { position: 'absolute', top: 90, left: 0, right: 0, borderRadius: 16, borderWidth: 1, zIndex: 1000, elevation: 10, padding: 8, backgroundColor: C.bgSecondary, borderColor: C.border },
  searchItem: { padding: 14, borderRadius: 10 },
  typeChipForm: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 12, borderRadius: 14 },
  typeText: { fontSize: 14, fontWeight: '700' },
  saveButton: { height: 60, borderRadius: 18, justifyContent: 'center', alignItems: 'center', marginTop: 20, overflow: 'hidden', backgroundColor: C.accent },
  saveButtonText: { color: '#FFF', fontSize: 18, fontWeight: '800', zIndex: 1 },
});

const styles = StyleSheet.create({
  aiSection: { marginTop: 4, marginBottom: 20 },
  aiTitleRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 12 },
  aiTitle: { fontSize: 10, fontWeight: '900', letterSpacing: 1.5 },
  aiGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  aiMiniCard: { flex: 1, minWidth: '45%', padding: 10, borderRadius: 12, flexDirection: 'row', alignItems: 'center', gap: 8 },
  aiMiniIcon: { width: 24, height: 24, borderRadius: 8, justifyContent: 'center', alignItems: 'center' },
  aiMiniText: { fontSize: 11, fontWeight: '600', flex: 1, lineHeight: 15 },
  aiSectionLoading: { paddingVertical: 20, alignItems: 'center' },
  emptyContainer: { alignItems: 'center', justifyContent: 'center', marginTop: 60, paddingHorizontal: 40 },
  emptyIllustration: { width: 160, height: 160, justifyContent: 'center', alignItems: 'center', marginBottom: 24 },
  glowCircle: { position: 'absolute', width: 100, height: 100, borderRadius: 50, opacity: 0.1, transform: [{ scale: 1.5 }] },
  emptyText: { fontSize: 20, fontWeight: '800', marginBottom: 12, textAlign: 'center' },
  emptySubText: { fontSize: 14, textAlign: 'center', lineHeight: 22, marginBottom: 32 },
  emptyBtn: { paddingHorizontal: 24, paddingVertical: 14, borderRadius: 16, elevation: 4 },
  emptyBtnText: { color: '#FFF', fontWeight: '800', fontSize: 15 },
  fabWrapper: { position: 'absolute', bottom: 30, left: 20, right: 20, alignItems: 'center' },
  fab: { height: 56, borderRadius: 28, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, shadowColor: '#3B82F6', shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.4, shadowRadius: 12, elevation: 8, overflow: 'hidden' },
  fabContent: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  fabText: { color: '#FFF', fontWeight: '900', fontSize: 14, letterSpacing: 1 },
});
