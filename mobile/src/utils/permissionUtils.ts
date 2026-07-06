import { PermissionsAndroid, Platform, Alert, Linking } from 'react-native';
import Geolocation from 'react-native-geolocation-service';

export type PermissionType = 'location' | 'notifications';

interface PermissionResult {
  granted: boolean;
  canRetry: boolean;
}

export const requestLocationPermission = async (): Promise<PermissionResult> => {
  if (Platform.OS === 'ios') {
    const status = await Geolocation.requestAuthorization('whenInUse');
    return {
      granted: status === 'granted',
      canRetry: status !== 'denied',
    };
  }

  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        {
          title: 'Konum İzni Gerekli',
          message: 'Havamania, bulunduğun şehre göre anlık hava analizi ve kişisel öneriler sunmak için konumunu kullanır.',
          buttonNeutral: 'Sonra Sor',
          buttonNegative: 'İptal',
          buttonPositive: 'Tamam',
        }
      );

      return {
        granted: granted === PermissionsAndroid.RESULTS.GRANTED,
        canRetry: granted !== PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN,
      };
    } catch (err) {
      console.warn(err);
      return { granted: false, canRetry: true };
    }
  }

  return { granted: false, canRetry: true };
};

export const showPermissionSettingsAlert = (type: PermissionType) => {
  const title = type === 'location' ? 'Konum İzni Reddedildi' : 'Bildirim İzni Gerekli';
  const message = type === 'location'
    ? 'Hava durumunu bulunduğunuz konuma göre otomatik göstermek için ayarlardan konum iznini açmanız gerekiyor.'
    : 'Önemli hava değişimlerinden anında haberdar olmak için bildirim iznini açmanızı öneririz.';

  Alert.alert(
    title,
    message,
    [
      { text: 'Vazgeç', style: 'cancel' },
      { text: 'Ayarlara Git', onPress: () => Linking.openSettings() }
    ]
  );
};
