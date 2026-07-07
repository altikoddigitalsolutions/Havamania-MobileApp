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
          title: '📍 Konum İzni',
          message: 'Havamania, bulunduğun şehre göre anlık hava analizi ve kişisel öneriler sunmak için konumunu kullanır.',
          buttonNeutral: 'Daha Sonra',
          buttonNegative: 'İptal',
          buttonPositive: 'İzin Ver',
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

export const requestNotificationPermission = async (): Promise<PermissionResult> => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      try {
        const granted = await PermissionsAndroid.request(
          'android.permission.POST_NOTIFICATIONS' as any,
          {
            title: '🔔 Bildirim İzni',
            message: 'Yağış, UV ve seyahat analizlerindeki önemli değişiklikleri zamanında bildirebilmemiz için bildirim iznine ihtiyacımız var.',
            buttonNeutral: 'Daha Sonra',
            buttonNegative: 'İptal',
            buttonPositive: 'İzin Ver',
          }
        );
        return {
          granted: granted === PermissionsAndroid.RESULTS.GRANTED,
          canRetry: granted !== PermissionsAndroid.RESULTS.NEVER_ASK_AGAIN,
        };
      } catch (err) {
        return { granted: false, canRetry: true };
      }
    }
    return { granted: true, canRetry: true }; // iOS or older Android handles it differently or has it enabled
};

export const showPermissionSettingsAlert = (type: PermissionType) => {
  const title = type === 'location' ? 'Konum İzni Gerekli' : 'Bildirim İzni Gerekli';
  const message = type === 'location'
    ? 'Hava durumunu bulunduğunuz konuma göre otomatik göstermek için ayarlardan konum iznini açmanız gerekiyor.'
    : 'Önemli hava değişimlerinden ve seyahat analizlerinden haberdar olmak için bildirim iznini açmanızı öneririz.';

  Alert.alert(
    title,
    message,
    [
      { text: 'Vazgeç', style: 'cancel' },
      { text: 'Ayarlara Git', onPress: () => Linking.openSettings() }
    ]
  );
};
