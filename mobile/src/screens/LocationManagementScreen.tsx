import React, {useMemo, useState} from 'react';
import {Alert, Button, FlatList, StyleSheet, Switch, Text, TextInput, View} from 'react-native';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {createLocation, deleteLocation, getLocations, updateLocation} from '../services/profileApi';
import {getSubscriptionStatus} from '../services/subscriptionApi';

export function LocationManagementScreen(): React.JSX.Element {
  const queryClient = useQueryClient();
  const [label, setLabel] = useState('');
  const [lat, setLat] = useState('');
  const [lon, setLon] = useState('');

  const locationsQuery = useQuery({queryKey: ['profile', 'locations'], queryFn: getLocations});
  const subQuery = useQuery({queryKey: ['subscription', 'status'], queryFn: getSubscriptionStatus});

  const addMutation = useMutation({
    mutationFn: createLocation,
    onSuccess: () => {
      setLabel('');
      setLat('');
      setLon('');
      void queryClient.invalidateQueries({queryKey: ['profile', 'locations']});
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteLocation,
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile', 'locations']}),
  });

  const updateMutation = useMutation({
    mutationFn: ({locationId, payload}: {locationId: string; payload: Record<string, unknown>}) =>
      updateLocation(locationId, payload),
    onSuccess: () => void queryClient.invalidateQueries({queryKey: ['profile', 'locations']}),
  });

  const canAddMore = useMemo(() => {
    const isPremium = Boolean(subQuery.data?.is_premium_active);
    const count = locationsQuery.data?.length ?? 0;
    return isPremium || count < 1;
  }, [locationsQuery.data, subQuery.data]);

  const handleAdd = () => {
    if (!canAddMore) {
      Alert.alert('Premium Gerekli', 'Birden fazla konum icin premium gereklidir.');
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
    <View style={styles.container}>
      <Text style={styles.title}>Konumlar</Text>

      <TextInput style={styles.input} placeholder="Etiket" value={label} onChangeText={setLabel} />
      <TextInput style={styles.input} placeholder="Lat" value={lat} onChangeText={setLat} />
      <TextInput style={styles.input} placeholder="Lon" value={lon} onChangeText={setLon} />
      <Button title="Konum Ekle" onPress={handleAdd} />

      <FlatList
        data={locationsQuery.data ?? []}
        keyExtractor={item => item.id}
        renderItem={({item}) => (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>{item.label}</Text>
            <Text>{item.lat}, {item.lon}</Text>
            <View style={styles.row}>
              <Text>Takip</Text>
              <Switch
                value={item.is_tracking_enabled}
                onValueChange={value =>
                  updateMutation.mutate({locationId: item.id, payload: {is_tracking_enabled: value}})
                }
              />
            </View>
            <View style={styles.row}>
              <Button
                title={item.is_primary ? 'Birincil' : 'Birincil Yap'}
                onPress={() => updateMutation.mutate({locationId: item.id, payload: {is_primary: true}})}
              />
              <Button title="Sil" color="red" onPress={() => deleteMutation.mutate(item.id)} />
            </View>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 10},
  title: {fontSize: 20, fontWeight: '700'},
  input: {borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8, padding: 10},
  card: {borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, padding: 12, marginTop: 12, gap: 8},
  cardTitle: {fontSize: 16, fontWeight: '700'},
  row: {flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'},
});
