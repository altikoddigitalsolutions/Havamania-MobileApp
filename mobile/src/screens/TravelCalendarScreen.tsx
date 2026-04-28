import React, { useState, useMemo, useEffect } from 'react';
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
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useColors, Spacing, Radius, FontSize } from '../theme';

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

const { width } = Dimensions.get('window');

const TRAVEL_TYPES: { type: TravelType; label: string; icon: string }[] = [
  { type: 'Vacation', label: 'Tatil', icon: 'beach-outline' },
  { type: 'Business', label: 'İş', icon: 'briefcase-outline' },
  { type: 'Family', label: 'Aile', icon: 'people-outline' },
  { type: 'Sports', label: 'Spor', icon: 'football-outline' },
  { type: 'Camping', label: 'Kamp', icon: 'bonfire-outline' },
  { type: 'Other', label: 'Diğer', icon: 'ellipsis-horizontal-outline' },
];

export function TravelCalendarScreen() {
  const C = useColors();
  const { plans, addPlan, removePlan, updatePlan } = useTravelStore();

  const [modalVisible, setModalVisible] = useState(false);
  const [filter, setFilter] = useState<'All' | 'Upcoming' | 'Past'>('Upcoming');
  const [editingId, setEditingId] = useState<string | null>(null);

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
      result = plans.filter(p => p.startDate >= today);
    } else if (filter === 'Past') {
      result = plans.filter(p => p.startDate < today);
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
    if (!selectedCity || !startDate || !endDate) {
      Alert.alert('Eksik Bilgi', 'Lütfen şehir ve tarihleri doldurun.');
      return;
    }

    if (!/^\d{4}-\d{2}-\d{2}$/.test(startDate) || !/^\d{4}-\d{2}-\d{2}$/.test(endDate)) {
      Alert.alert('Hata', 'Tarih formatı YYYY-MM-DD olmalıdır.');
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

  const renderTravelCard = ({ item }: { item: TravelPlan }) => (
    <TravelCard plan={item} onEdit={() => handleEdit(item)} onDelete={() => removePlan(item.id)} />
  );

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: C.bg }]}>
      <StatusBar barStyle="light-content" />
      <LinearGradient colors={[C.bg, C.bgSecondary]} style={StyleSheet.absoluteFill} />

      <View style={styles.header}>
        <View>
          <Text style={[styles.title, { color: C.text }]}>Seyahatlerim</Text>
          <Text style={[styles.subtitle, { color: C.textSecondary }]}>Planlarını yönet ve hava durumunu takip et</Text>
        </View>
      </View>

      <View style={styles.filterContainer}>
        {(['Upcoming', 'Past', 'All'] as const).map((f) => (
          <TouchableOpacity
            key={f}
            onPress={() => setFilter(f)}
            style={[
              styles.filterChip,
              { backgroundColor: filter === f ? C.accent : C.bgCard },
              filter === f && styles.activeFilterChip
            ]}
          >
            <Text style={[styles.filterText, { color: filter === f ? '#FFF' : C.textSecondary }]}>
              {f === 'Upcoming' ? 'Yaklaşanlar' : f === 'Past' ? 'Geçmiş' : 'Tümü'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <FlatList
        data={displayPlans}
        keyExtractor={(item) => item.id}
        renderItem={renderTravelCard}
        contentContainerStyle={styles.listContent}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <View style={[styles.emptyIconContainer, { backgroundColor: C.bgCard }]}>
              <Icon name="airplane-outline" size={60} color={C.accent} />
            </View>
            <Text style={[styles.emptyText, { color: C.text }]}>Seyahat Planı Bulunamadı</Text>
            <Text style={[styles.emptySubText, { color: C.textSecondary }]}>
              Yeni bir rota çizmek ve hava durumunu takip etmek için alttaki "+" butonuna dokunun.
            </Text>
          </View>
        }
      />

      <TouchableOpacity
        style={[styles.fab, { backgroundColor: C.accent }]}
        onPress={() => setModalVisible(true)}
      >
        <Icon name="add" size={32} color="#FFF" />
      </TouchableOpacity>

      <Modal
        animationType="slide"
        transparent={true}
        visible={modalVisible}
        onRequestClose={resetForm}
      >
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={styles.modalOverlay}
        >
          <View style={[styles.modalContent, { backgroundColor: C.bgCard }]}>
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: C.text }]}>
                {editingId ? 'Seyahati Düzenle' : 'Yeni Seyahat Planla'}
              </Text>
              <TouchableOpacity onPress={resetForm}>
                <Icon name="close" size={24} color={C.textSecondary} />
              </TouchableOpacity>
            </View>

            <ScrollView showsVerticalScrollIndicator={false}>
              <View style={styles.inputGroup}>
                <Text style={[styles.inputLabel, { color: C.textSecondary }]}>VARALAN ŞEHİR</Text>
                {selectedCity ? (
                  <TouchableOpacity
                    style={[styles.selectedCityBox, { backgroundColor: C.bgInput, borderColor: C.accent }]}
                    onPress={() => setSelectedCity(null)}
                  >
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                      <Icon name="location" size={18} color={C.accent} />
                      <Text style={{ color: C.text, fontWeight: '600' }}>{selectedCity.name}</Text>
                    </View>
                    <Icon name="close-circle" size={18} color={C.textMuted} />
                  </TouchableOpacity>
                ) : (
                  <View>
                    <TextInput
                      style={[styles.input, { backgroundColor: C.bgInput, color: C.text, borderColor: C.border }]}
                      placeholder="Şehir ara (örn: bal)"
                      placeholderTextColor={C.textMuted}
                      value={citySearch}
                      onChangeText={setCitySearch}
                    />
                    {citySearch.length > 0 && (
                      <View style={[styles.searchDropdown, { backgroundColor: C.bgCard, borderColor: C.border }]}>
                        {filteredCities.map((item) => (
                          <TouchableOpacity
                            key={item.name}
                            style={styles.searchItem}
                            onPress={() => { setSelectedCity(item); setCitySearch(''); }}
                          >
                            <Text style={{ color: C.text }}>{item.name}</Text>
                          </TouchableOpacity>
                        ))}
                      </View>
                    )}
                  </View>
                )}
              </View>

              <View style={styles.dateRow}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.inputLabel, { color: C.textSecondary }]}>BAŞLANGIÇ</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: C.bgInput, color: C.text, borderColor: C.border }]}
                    placeholder="YYYY-MM-DD"
                    placeholderTextColor={C.textMuted}
                    value={startDate}
                    onChangeText={setStartDate}
                  />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.inputLabel, { color: C.textSecondary }]}>BİTİŞ</Text>
                  <TextInput
                    style={[styles.input, { backgroundColor: C.bgInput, color: C.text, borderColor: C.border }]}
                    placeholder="YYYY-MM-DD"
                    placeholderTextColor={C.textMuted}
                    value={endDate}
                    onChangeText={setEndDate}
                  />
                </View>
              </View>

              <View style={styles.inputGroup}>
                <Text style={[styles.inputLabel, { color: C.textSecondary }]}>SEYAHAT TİPİ</Text>
                <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ gap: 10 }}>
                  {TRAVEL_TYPES.map((t) => (
                    <TouchableOpacity
                      key={t.type}
                      onPress={() => setTravelType(t.type)}
                      style={[
                        styles.typeChip,
                        { backgroundColor: travelType === t.type ? C.accent : C.bgInput },
                        travelType === t.type && { borderColor: C.accent }
                      ]}
                    >
                      <Icon name={t.icon} size={16} color={travelType === t.type ? '#FFF' : C.textSecondary} />
                      <Text style={[styles.typeText, { color: travelType === t.type ? '#FFF' : C.textSecondary }]}>
                        {t.label}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              </View>

              <View style={styles.inputGroup}>
                <Text style={[styles.inputLabel, { color: C.textSecondary }]}>NOTLAR</Text>
                <TextInput
                  style={[styles.input, styles.textArea, { backgroundColor: C.bgInput, color: C.text, borderColor: C.border }]}
                  placeholder="Seyahat ile ilgili notların..."
                  placeholderTextColor={C.textMuted}
                  multiline
                  numberOfLines={3}
                  value={note}
                  onChangeText={setNote}
                />
              </View>

              <TouchableOpacity
                style={[styles.saveButton, { backgroundColor: C.accent }]}
                onPress={handleSavePlan}
              >
                <Text style={styles.saveButtonText}>{editingId ? 'Güncelle' : 'Seyahati Kaydet'}</Text>
              </TouchableOpacity>
            </ScrollView>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </SafeAreaView>
  );
}

/**
 * Seyahat Kartı Bileşeni
 */
function TravelCard({ plan, onEdit, onDelete }: { plan: TravelPlan; onEdit: () => void; onDelete: () => void }) {
  const C = useColors();
  const typeInfo = TRAVEL_TYPES.find(t => t.type === plan.type) || TRAVEL_TYPES[5];

  const { data: weatherData } = useQuery({
    queryKey: ['weather', plan.lat, plan.lon, plan.startDate],
    queryFn: () => getDailyWeather(plan.lat, plan.lon, 10),
    staleTime: 10 * 60 * 1000,
  });

  const analysis = useMemo(() => {
    if (!weatherData) return null;
    const today = new Date().toISOString().split('T')[0];
    const targetDate = plan.startDate;
    const dayData = weatherData.items.find(i => i.date === targetDate);

    if (!dayData) return { msg: 'Hava tahmini seyahat tarihinize yaklaştığında burada detaylandırılacaktır.', icon: 'time-outline', color: C.textMuted };

    let advice = "";
    let packing = ["Rahat ayakkabı"];
    let color = C.accent;

    if (dayData.weather_code >= 51) {
      advice = "Yağmurlu bir gün bekleniyor, şemsiye ve yağmurluk almayı unutma.";
      packing.push("Şemsiye", "Yağmurluk");
      color = "#3B82F6";
    } else if (dayData.temp_max > 28) {
      advice = "Hava oldukça sıcak olacak. İnce kıyafetler ve güneş kremi tercih et.";
      packing.push("Güneş kremi", "Gözlük", "İnce kıyafetler");
      color = "#F59E0B";
    } else if (dayData.temp_min < 10) {
      advice = "Hava serin olabilir, yanına kalın bir mont veya hırka almalısın.";
      packing.push("Mont", "Atkı");
      color = "#10B981";
    } else {
      advice = "Hava seyahat için oldukça ideal görünüyor.";
      packing.push("Hafif ceket");
    }

    if (dayData.wind_speed_max > 25) {
      advice += " Ayrıca rüzgarlı bir gün, dış mekan aktivitelerinde dikkatli ol.";
    }

    return {
      msg: advice,
      packing: packing.join(", "),
      temp: `${dayData.temp_min}° / ${dayData.temp_max}°`,
      summary: dayData.precipitation_probability > 30 ? 'Yağış İhtimali' : 'Açık/Az Bulutlu',
      color
    };
  }, [weatherData, plan.startDate, C]);

  const startDate = new Date(plan.startDate);
  const endDate = new Date(plan.endDate);
  const diffTime = Math.abs(endDate.getTime() - startDate.getTime());
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;

  return (
    <View style={[styles.card, { backgroundColor: C.bgCard, borderColor: C.border }]}>
      <LinearGradient
        colors={['rgba(255,255,255,0.05)', 'transparent']}
        start={{x:0, y:0}} end={{x:1, y:1}}
        style={StyleSheet.absoluteFill}
      />

      <View style={styles.cardTop}>
        <View style={styles.cardHeaderInfo}>
          <View style={[styles.typeIconBox, { backgroundColor: 'rgba(59, 130, 246, 0.1)' }]}>
            <Icon name={typeInfo.icon} size={20} color={C.accent} />
          </View>
          <View>
            <Text style={[styles.cardCity, { color: C.text }]}>{plan.city}</Text>
            <Text style={[styles.cardTypeLabel, { color: C.textSecondary }]}>{typeInfo.label} • {diffDays} Gün</Text>
          </View>
        </View>
        <View style={styles.actionBtns}>
          <TouchableOpacity onPress={onEdit} style={styles.iconButton}>
            <Icon name="create-outline" size={20} color={C.textSecondary} />
          </TouchableOpacity>
          <TouchableOpacity onPress={() => Alert.alert('Sil', 'Bu seyahati silmek istediğine emin misin?', [{text: 'İptal'}, {text: 'Sil', onPress: onDelete, style: 'destructive'}])}>
            <Icon name="trash-outline" size={20} color={C.error} />
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.dateInfoRow}>
        <View style={styles.dateItem}>
          <Text style={[styles.dateLabel, { color: C.textMuted }]}>GİDİŞ</Text>
          <Text style={[styles.dateValue, { color: C.text }]}>{plan.startDate}</Text>
        </View>
        <Icon name="arrow-forward" size={16} color={C.textMuted} style={{ marginTop: 12 }} />
        <View style={styles.dateItem}>
          <Text style={[styles.dateLabel, { color: C.textMuted }]}>DÖNÜŞ</Text>
          <Text style={[styles.dateValue, { color: C.text }]}>{plan.endDate}</Text>
        </View>
      </View>

      {analysis ? (
        <View style={[styles.analysisBox, { backgroundColor: 'rgba(255,255,255,0.03)', borderLeftColor: analysis.color }]}>
          <View style={styles.analysisHeader}>
            <Text style={[styles.analysisTitle, { color: analysis.color }]}>Hava Analizi & Öneriler</Text>
            {analysis.temp && <Text style={[styles.analysisTemp, { color: C.text }]}>{analysis.temp}</Text>}
          </View>
          <Text style={[styles.analysisMsg, { color: C.text }]}>{analysis.msg}</Text>
          {analysis.packing && (
            <View style={styles.packingRow}>
              <Icon name="briefcase" size={14} color={C.accent} />
              <Text style={[styles.packingText, { color: C.textSecondary }]}>Valiz: {analysis.packing}</Text>
            </View>
          )}
        </View>
      ) : (
        <View style={styles.loadingAnalysis}>
          <Text style={{ color: C.textMuted, fontSize: 12 }}>Hava durumu verileri yükleniyor...</Text>
        </View>
      )}

      {plan.note && (
        <View style={styles.noteSection}>
          <Icon name="document-text-outline" size={14} color={C.textMuted} />
          <Text style={[styles.noteText, { color: C.textMuted }]} numberOfLines={2}>{plan.note}</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { padding: Spacing.xl, paddingTop: 20 },
  title: { fontSize: 32, fontWeight: '800', letterSpacing: -0.5 },
  subtitle: { fontSize: 14, marginTop: 4, opacity: 0.8 },
  filterContainer: { flexDirection: 'row', paddingHorizontal: Spacing.xl, gap: 10, marginBottom: Spacing.lg },
  filterChip: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: Radius.full, borderWidth: 1, borderColor: 'transparent' },
  activeFilterChip: { shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  filterText: { fontSize: 13, fontWeight: '700' },
  listContent: { padding: Spacing.xl, paddingBottom: 100 },
  card: { padding: Spacing.lg, borderRadius: 28, borderWidth: 1, marginBottom: Spacing.xl, overflow: 'hidden', elevation: 5, shadowColor: '#000', shadowOffset: { width: 0, height: 10 }, shadowOpacity: 0.1, shadowRadius: 20 },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 },
  cardHeaderInfo: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  typeIconBox: { width: 44, height: 44, borderRadius: 14, justifyContent: 'center', alignItems: 'center' },
  cardCity: { fontSize: 20, fontWeight: '800' },
  cardTypeLabel: { fontSize: 12, fontWeight: '600', marginTop: 2 },
  actionBtns: { flexDirection: 'row', gap: 8 },
  iconButton: { width: 36, height: 36, borderRadius: 18, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.05)' },
  dateInfoRow: { flexDirection: 'row', alignItems: 'center', gap: 20, marginBottom: 20, paddingHorizontal: 4 },
  dateItem: { flex: 1 },
  dateLabel: { fontSize: 10, fontWeight: '800', letterSpacing: 1 },
  dateValue: { fontSize: 15, fontWeight: '700', marginTop: 4 },
  analysisBox: { padding: 16, borderRadius: 20, borderLeftWidth: 4, marginTop: 4 },
  analysisHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  analysisTitle: { fontSize: 13, fontWeight: '800' },
  analysisTemp: { fontSize: 13, fontWeight: '700' },
  analysisMsg: { fontSize: 14, lineHeight: 20, fontWeight: '500' },
  packingRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 12 },
  packingText: { fontSize: 12, fontWeight: '600' },
  noteSection: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 16, borderTopWidth: 1, borderTopColor: 'rgba(255,255,255,0.05)', paddingTop: 12 },
  noteText: { fontSize: 13, fontStyle: 'italic', flex: 1 },
  loadingAnalysis: { padding: 16, alignItems: 'center' },
  emptyContainer: { alignItems: 'center', justifyContent: 'center', marginTop: 80 },
  emptyIconContainer: { width: 120, height: 120, borderRadius: 60, justifyContent: 'center', alignItems: 'center', marginBottom: 24, borderWidth: 1, borderColor: 'rgba(255,255,255,0.1)' },
  emptyText: { fontSize: 20, fontWeight: '800', marginBottom: 8 },
  emptySubText: { fontSize: 14, textAlign: 'center', paddingHorizontal: 40, lineHeight: 22 },
  fab: { position: 'absolute', bottom: 30, right: 30, width: 64, height: 64, borderRadius: 32, justifyContent: 'center', alignItems: 'center', elevation: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 10 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.7)', justifyContent: 'flex-end' },
  modalContent: { borderTopLeftRadius: 32, borderTopRightRadius: 32, padding: Spacing.xl, maxHeight: '90%' },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 },
  modalTitle: { fontSize: 22, fontWeight: '800' },
  inputGroup: { marginBottom: 20 },
  inputLabel: { fontSize: 11, fontWeight: '800', letterSpacing: 1.2, marginBottom: 10, marginLeft: 4 },
  input: { height: 56, borderRadius: 16, paddingHorizontal: 16, borderWidth: 1, fontSize: 16, fontWeight: '600' },
  textArea: { height: 100, paddingTop: 16, textAlignVertical: 'top' },
  dateRow: { flexDirection: 'row', gap: 12, marginBottom: 20 },
  selectedCityBox: { height: 56, borderRadius: 16, paddingHorizontal: 16, borderWidth: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  searchDropdown: { position: 'absolute', top: 90, left: 0, right: 0, borderRadius: 16, borderWidth: 1, zIndex: 1000, elevation: 5, padding: 8 },
  searchItem: { padding: 14, borderRadius: 10 },
  typeChip: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 16, paddingVertical: 10, borderRadius: 12, borderWidth: 1, borderColor: 'transparent' },
  typeText: { fontSize: 14, fontWeight: '700' },
  saveButton: { height: 60, borderRadius: 18, justifyContent: 'center', alignItems: 'center', marginTop: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4 },
  saveButtonText: { color: '#FFF', fontSize: 18, fontWeight: '800' },
});
