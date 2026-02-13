import React from 'react';
import {Alert, Button, PermissionsAndroid, Platform, StyleSheet, Text, View} from 'react-native';
import Slider from '@react-native-community/slider';
import {useQuery} from '@tanstack/react-query';
import Geolocation from 'react-native-geolocation-service';

import {apiClient} from '../services/apiClient';
import {getSubscriptionStatus} from '../services/subscriptionApi';

type Layer = 'temperature' | 'wind' | 'isobar';

const PREMIUM_LAYERS: Layer[] = ['wind', 'isobar'];

export function MapScreen(): React.JSX.Element {
  const [layer, setLayer] = React.useState<Layer>('temperature');
  const [timelineValue, setTimelineValue] = React.useState(0);
  const [lat, setLat] = React.useState(41.0082);
  const [lon, setLon] = React.useState(28.9784);

  const subscriptionQuery = useQuery({
    queryKey: ['subscription', 'status'],
    queryFn: getSubscriptionStatus,
  });

  const mapLayerQuery = useQuery({
    queryKey: ['map-layer', layer, lat, lon],
    queryFn: async () => {
      const response = await apiClient.get('/weather/map-layers', {params: {lat, lon, layer}});
      return response.data;
    },
    enabled: true,
  });

  const requestLocationPermission = async (): Promise<boolean> => {
    if (Platform.OS !== 'android') {
      return true;
    }

    const result = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    );

    return result === PermissionsAndroid.RESULTS.GRANTED;
  };

  const handleMyLocation = async () => {
    const granted = await requestLocationPermission();
    if (!granted) {
      Alert.alert('Izin gerekli', 'Konum izni olmadan mevcut konum alinamaz.');
      return;
    }

    Geolocation.getCurrentPosition(
      position => {
        setLat(position.coords.latitude);
        setLon(position.coords.longitude);
      },
      () => Alert.alert('Hata', 'Konum alinamadi.'),
      {enableHighAccuracy: true, timeout: 10000, maximumAge: 2000},
    );
  };

  const handleLayerSelect = (nextLayer: Layer) => {
    const isPremiumRequired = PREMIUM_LAYERS.includes(nextLayer);
    const isPremium = Boolean(subscriptionQuery.data?.is_premium_active);

    if (isPremiumRequired && !isPremium) {
      Alert.alert('Premium Gerekli', 'Bu katman premium pakette sunulur.');
      return;
    }

    setLayer(nextLayer);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Map Screen</Text>
      <Text>Konum: {lat.toFixed(3)}, {lon.toFixed(3)}</Text>
      <Button title="My Location" onPress={() => void handleMyLocation()} />

      <View style={styles.layers}>
        <Button title="Temperature" onPress={() => handleLayerSelect('temperature')} />
        <Button title="Wind (Premium)" onPress={() => handleLayerSelect('wind')} />
        <Button title="Isobar (Premium)" onPress={() => handleLayerSelect('isobar')} />
      </View>

      <Text>Timeline: {timelineValue.toFixed(0)}</Text>
      <Slider
        style={{width: '100%', height: 40}}
        minimumValue={0}
        maximumValue={24}
        step={1}
        value={timelineValue}
        onValueChange={setTimelineValue}
      />

      <View style={styles.resultBox}>
        <Text>Layer: {layer}</Text>
        <Text>
          Data: {mapLayerQuery.isLoading ? 'Loading...' : mapLayerQuery.isError ? 'Error' : 'Ready'}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 12},
  title: {fontSize: 20, fontWeight: '700'},
  layers: {gap: 8},
  resultBox: {padding: 12, borderRadius: 8, borderWidth: 1, borderColor: '#d1d5db'},
});
