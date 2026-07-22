import React, {useMemo, useState} from 'react';
import {Alert, Button, FlatList, StyleSheet, Switch, Text, TextInput, View, TouchableOpacity, SafeAreaView} from 'react-native';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import Icon from 'react-native-vector-icons/Ionicons';

import * as userService from '../services/userService';
import {useAuthStore} from '../store/authStore';
import {useColors, Spacing, Radius, FontSize} from '../theme';

export function LocationManagementScreen({navigation}: any): React.JSX.Element {
  const queryClient = useQueryClient();
  const C = useColors();
  const {user, isGuest} = useAuthStore();

  const [label, setLabel] = useState('');
  const [lat, setLat] = useState('');
  const [lon, setLon] = useState('');

  const locationsQuery = useQuery({
    queryKey: ['profile', 'locations'],
    queryFn: () => user ? userService.getFavoriteLocations(user.uid) : Promise.resolve([]),
    enabled: !!user && !isGuest
  });

  const addMutation = useMutation({
    mutationFn: (data: any) => user ? userService.addFavoriteLocation(user.uid, data) : Promise.resolve(''),
    onSuccess: () => {
      setLabel('');
      setLat('');
      setLon('');
      void queryClient.invalidateQueries({queryKey: ['profile', 'locations']});
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => user ? userService.deleteFavoriteLocation(user.uid, id) : Promise.resolve(),
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile', 'locations']}),
  });

  const updateMutation = useMutation({
    mutationFn: ({id, data}: {id: string; data: any}) => user ? userService.updateFavoriteLocation(user.uid, id, data) : Promise.resolve(),
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile', 'locations']}),
  });

  const handleAdd = () => {
    if (isGuest) {
      Alert.alert('Üye Olmalısınız', 'Konum kaydetmek için lütfen giriş yapın.');
      return;
    }

    const latNum = Number(lat);
    const lonNum = Number(lon);
    if (!label || Number.isNaN(latNum) || Number.isNaN(lonNum)) {
      Alert.alert('Hata', 'Gecerli konum bilgisi girin.');
      return;
    }

    addMutation.mutate({label, lat: latNum, lon: lonNum, is_primary: false, is_tracking_enabled: true});
  };

  return (
    <SafeAreaView style={[s.safe, {backgroundColor: C.bg}]}>
      <View style={s.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
          <Icon name="arrow-back" size={24} color={C.text} />
        </TouchableOpacity>
        <Text style={[s.headerTitle, {color: C.text}]}>Kayıtlı Konumlar</Text>
        <View style={{width: 40}} />
      </View>

      <View style={s.container}>
        <View style={[s.addCard, {backgroundColor: C.bgCard, borderColor: C.border}]}>
            <TextInput style={[s.input, {color: C.text, borderColor: C.border}]} placeholder="Konum Adı (Ev, İş vb.)" placeholderTextColor={C.textMuted} value={label} onChangeText={setLabel} />
            <View style={s.row}>
                <TextInput style={[s.input, {flex: 1, color: C.text, borderColor: C.border}]} placeholder="Enlem" placeholderTextColor={C.textMuted} value={lat} onChangeText={setLat} keyboardType="numeric" />
                <TextInput style={[s.input, {flex: 1, color: C.text, borderColor: C.border}]} placeholder="Boylam" placeholderTextColor={C.textMuted} value={lon} onChangeText={setLon} keyboardType="numeric" />
            </View>
            <TouchableOpacity style={[s.addBtn, {backgroundColor: C.accent}]} onPress={handleAdd}>
                <Text style={s.addBtnText}>Yeni Konum Ekle</Text>
            </TouchableOpacity>
        </View>

        <FlatList
          data={locationsQuery.data ?? []}
          keyExtractor={item => item.id}
          contentContainerStyle={{paddingBottom: 20}}
          ListEmptyComponent={
            <Text style={{textAlign: 'center', color: C.textSecondary, marginTop: 40}}>
                {isGuest ? 'Konum eklemek için giriş yapın.' : 'Henüz kayıtlı bir konumunuz yok.'}
            </Text>
          }
          renderItem={({item}) => (
            <View style={[s.card, {backgroundColor: C.bgCard, borderColor: C.border}]}>
              <View style={s.cardHeader}>
                <Icon name="location" size={20} color={C.accent} />
                <Text style={[s.cardTitle, {color: C.text}]}>{item.label}</Text>
                <TouchableOpacity onPress={() => deleteMutation.mutate(item.id)}>
                    <Icon name="trash-outline" size={20} color={C.error} />
                </TouchableOpacity>
              </View>
              <Text style={{color: C.textSecondary}}>{item.lat.toFixed(4)}, {item.lon.toFixed(4)}</Text>

              <View style={s.divider} />

              <View style={s.cardFooter}>
                <View style={s.switchRow}>
                    <Text style={{color: C.text, fontSize: 13}}>Bildirim Takibi</Text>
                    <Switch
                        value={item.is_tracking_enabled}
                        onValueChange={value =>
                            updateMutation.mutate({id: item.id, data: {is_tracking_enabled: value}})
                        }
                    />
                </View>
                <TouchableOpacity
                    style={[s.primaryBadge, item.is_primary && {backgroundColor: C.accent + '20'}]}
                    onPress={() => updateMutation.mutate({id: item.id, data: {is_primary: !item.is_primary}})}
                >
                    <Text style={{color: item.is_primary ? C.accent : C.textMuted, fontSize: 12, fontWeight: '700'}}>
                        {item.is_primary ? 'Birincil Konum' : 'Birincil Yap'}
                    </Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
        />
      </View>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: {flex: 1},
  header: {flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: Spacing.md},
  headerTitle: {fontSize: 18, fontWeight: '800'},
  backBtn: {width: 40, height: 40, justifyContent: 'center', alignItems: 'center'},
  container: {flex: 1, padding: Spacing.md},
  addCard: {padding: Spacing.md, borderRadius: Radius.lg, borderWidth: 1, marginBottom: Spacing.lg, gap: Spacing.sm},
  input: {borderWidth: 1, borderRadius: Radius.md, padding: 12, fontSize: 14},
  row: {flexDirection: 'row', gap: Spacing.sm},
  addBtn: {padding: 14, borderRadius: Radius.full, alignItems: 'center', marginTop: Spacing.xs},
  addBtnText: {color: '#FFF', fontWeight: '700', fontSize: 15},
  card: {borderWidth: 1, borderRadius: Radius.lg, padding: 16, marginBottom: Spacing.md, gap: 8},
  cardHeader: {flexDirection: 'row', alignItems: 'center', gap: 10},
  cardTitle: {fontSize: 16, fontWeight: '700', flex: 1},
  divider: {height: 1, backgroundColor: 'rgba(255,255,255,0.05)', marginVertical: 4},
  cardFooter: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'},
  switchRow: {flexDirection: 'row', alignItems: 'center', gap: 8},
  primaryBadge: {paddingVertical: 6, paddingHorizontal: 12, borderRadius: Radius.full},
});
