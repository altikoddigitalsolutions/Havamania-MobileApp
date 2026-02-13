import React from 'react';
import {
  ActivityIndicator,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {useQuery} from '@tanstack/react-query';

import {getAlertDetail, getAlerts} from '../services/alertsApi';
import {getLocations} from '../services/profileApi';

export function AlertsScreen(): React.JSX.Element {
  const [severityFilter, setSeverityFilter] = React.useState<string>('');
  const [locationFilter, setLocationFilter] = React.useState<string>('');
  const [selectedAlertId, setSelectedAlertId] = React.useState<string | null>(null);

  const alertsQuery = useQuery({
    queryKey: ['alerts', severityFilter, locationFilter],
    queryFn: () => getAlerts({severity: severityFilter || undefined, location_id: locationFilter || undefined}),
  });

  const locationsQuery = useQuery({
    queryKey: ['profile', 'locations'],
    queryFn: getLocations,
  });

  const detailQuery = useQuery({
    queryKey: ['alert', selectedAlertId],
    queryFn: () => getAlertDetail(selectedAlertId as string),
    enabled: Boolean(selectedAlertId),
  });

  if (alertsQuery.isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator />
      </View>
    );
  }

  const grouped = alertsQuery.data ?? {critical: [], active: [], advisory: []};
  const merged = [...grouped.critical, ...grouped.active, ...grouped.advisory];

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Alerts</Text>

      <TextInput
        style={styles.input}
        placeholder="Severity filtre (critical/active/advisory)"
        value={severityFilter}
        onChangeText={setSeverityFilter}
      />

      <TextInput
        style={styles.input}
        placeholder="Location ID filtre"
        value={locationFilter}
        onChangeText={setLocationFilter}
      />

      <Text style={styles.subTitle}>Kayitli konumlar: {locationsQuery.data?.length ?? 0}</Text>

      {merged.length === 0 ? (
        <Text style={styles.empty}>Bu filtrede uyari yok.</Text>
      ) : (
        merged.map((alert: any) => (
          <Pressable key={alert.id} style={styles.card} onPress={() => setSelectedAlertId(alert.id)}>
            <Text style={styles.cardTitle}>{alert.title}</Text>
            <Text>{alert.severity}</Text>
          </Pressable>
        ))
      )}

      <Modal visible={Boolean(selectedAlertId)} transparent animationType="slide">
        <View style={styles.modalBackdrop}>
          <View style={styles.modalBody}>
            {detailQuery.isLoading ? (
              <ActivityIndicator />
            ) : (
              <>
                <Text style={styles.cardTitle}>{detailQuery.data?.title}</Text>
                <Text>{detailQuery.data?.description}</Text>
              </>
            )}
            <Pressable style={styles.closeButton} onPress={() => setSelectedAlertId(null)}>
              <Text style={{color: '#fff'}}>Kapat</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 12},
  center: {flex: 1, justifyContent: 'center', alignItems: 'center'},
  title: {fontSize: 22, fontWeight: '700'},
  subTitle: {fontSize: 12, color: '#4b5563'},
  input: {borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8, padding: 10},
  empty: {paddingVertical: 20, color: '#6b7280'},
  card: {borderWidth: 1, borderColor: '#e5e7eb', borderRadius: 8, padding: 12},
  cardTitle: {fontSize: 16, fontWeight: '700'},
  modalBackdrop: {flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end'},
  modalBody: {backgroundColor: '#fff', padding: 16, borderTopLeftRadius: 16, borderTopRightRadius: 16, gap: 10},
  closeButton: {backgroundColor: '#111827', borderRadius: 8, alignSelf: 'flex-start', paddingHorizontal: 14, paddingVertical: 8},
});
