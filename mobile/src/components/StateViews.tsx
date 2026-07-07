import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
  Animated,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import { useColors, Spacing, Radius, FontSize, AppColors } from '../theme';

interface ErrorViewProps {
  type?: 'generic' | 'no-internet' | 'api-fail' | 'location-denied' | 'not-found' | 'ai-fail' | 'permission';
  title?: string;
  message?: string;
  onRetry?: () => void;
  actionLabel?: string;
}

export const ErrorView: React.FC<ErrorViewProps> = ({
  type = 'generic',
  title,
  message,
  onRetry,
  actionLabel = 'Tekrar Dene',
}) => {
  const C = useColors();
  const s = makeStyles(C);

  const config = {
    'generic': { icon: 'alert-circle-outline', title: 'Bir Sorun Oluştu', message: 'Şu an işleminizi gerçekleştiremiyoruz.' },
    'no-internet': { icon: 'wifi-outline', title: 'İnternet Bağlantısı Yok', message: 'Kısa süreli bağlantı sorunu yaşıyoruz. Tekrar deneyebilirsin.' },
    'api-fail': { icon: 'cloud-offline-outline', title: 'Servis Bağlantısı', message: 'Hava durumu sunucularımıza ulaşamıyoruz. Lütfen birazdan tekrar dene.' },
    'location-denied': { icon: 'location-outline', title: 'Konum İzni Gerekli', message: 'Havamania, bulunduğun şehre göre anlık hava analizi ve kişisel öneriler sunmak için konumunu kullanır.' },
    'not-found': { icon: 'search-outline', title: 'Şehir Bulunamadı', message: 'Aradığın şehri bulamadık. Farklı bir isimle aramayı deneyebilirsin.' },
    'ai-fail': { icon: 'sparkles-outline', title: 'Asistan Cevap Veremiyor', message: 'AI asistanımız şu an dinleniyor. Lütfen sorunu tekrar sormayı dene.' },
    'permission': { icon: 'notifications-outline', title: 'Bildirim İzni Gerekli', message: 'Yağış, UV ve seyahat analizlerindeki önemli değişiklikleri bildirebilmemiz için izne ihtiyacımız var.' },
    'data-fail': { icon: 'save-outline', title: 'Veri Kaydedilemedi', message: 'İşlemi şu an gerçekleştiremedik. Lütfen tekrar dene.' },
  };

  const current = config[type] || config.generic;

  return (
    <View style={s.center}>
      <View style={[s.iconBg, { backgroundColor: C.accent + '15' }]}>
        <Icon name={current.icon} size={48} color={C.accent} />
      </View>
      <Text style={s.errorTitle}>{title || current.title}</Text>
      <Text style={s.errorText}>{message || current.message}</Text>

      {onRetry && (
        <TouchableOpacity style={s.retryBtn} onPress={onRetry} activeOpacity={0.8}>
          <Text style={s.retryText}>{actionLabel}</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

export const SkeletonView: React.FC<{ height?: number; width?: any; borderRadius?: number; style?: any }> = ({
  height = 20,
  width = '100%',
  borderRadius = Radius.md,
  style,
}) => {
  const C = useColors();
  const animatedValue = React.useRef(new Animated.Value(0.3)).current;

  React.useEffect(() => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(animatedValue, {
          toValue: 1,
          duration: 800,
          useNativeDriver: true,
        }),
        Animated.timing(animatedValue, {
          toValue: 0.3,
          duration: 800,
          useNativeDriver: true,
        }),
      ])
    ).start();
  }, [animatedValue]);

  return (
    <Animated.View
      style={[
        {
          height,
          width,
          borderRadius,
          backgroundColor: C.bgCard,
          opacity: animatedValue,
        },
        style,
      ]}
    />
  );
};

export const LoadingState: React.FC = () => {
  const C = useColors();
  return (
    <View style={makeStyles(C).center}>
      <ActivityIndicator size="large" color={C.accent} />
      <Text style={[makeStyles(C).errorText, { marginTop: 16 }]}>Yükleniyor...</Text>
    </View>
  );
};

export const HomeSkeleton: React.FC = () => {
  const C = useColors();
  return (
    <View style={{ flex: 1, backgroundColor: C.bg, padding: Spacing.md }}>
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20, marginTop: 40 }}>
        <SkeletonView width={180} height={40} borderRadius={Radius.full} />
        <View style={{ flexDirection: 'row', gap: 8 }}>
          <SkeletonView width={40} height={40} borderRadius={20} />
          <SkeletonView width={40} height={40} borderRadius={20} />
        </View>
      </View>
      <SkeletonView height={320} borderRadius={32} style={{ marginBottom: 24 }} />
      <SkeletonView height={120} borderRadius={24} style={{ marginBottom: 24 }} />
      <SkeletonView height={180} borderRadius={24} style={{ marginBottom: 24 }} />
    </View>
  );
};

const makeStyles = (C: AppColors) => StyleSheet.create({
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
    backgroundColor: C.bg,
  },
  iconBg: {
    width: 100,
    height: 100,
    borderRadius: 50,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.lg,
  },
  errorTitle: {
    fontSize: FontSize.xl,
    fontWeight: '800',
    color: C.text,
    textAlign: 'center',
    marginBottom: Spacing.sm,
  },
  errorText: {
    fontSize: FontSize.md,
    color: C.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: Spacing.xl,
  },
  retryBtn: {
    backgroundColor: C.accent,
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.md,
    borderRadius: Radius.full,
    shadowColor: C.accent,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  retryText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '700',
  },
});
