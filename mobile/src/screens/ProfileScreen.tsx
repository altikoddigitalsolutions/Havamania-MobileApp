import React from 'react';
import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Image,
  Alert,
} from 'react-native';
import { useTranslation } from 'react-i18next';
import Icon from 'react-native-vector-icons/Ionicons';
import { useNavigation } from '@react-navigation/native';

import { useAuthStore } from '../store/authStore';
import { useTravelStore } from '../store/travelStore';
import * as userService from '../services/userService';
import { useColors, Spacing, Radius, FontSize, AppColors } from '../theme';
import { useThemeStore } from '../store/themeStore';

export function ProfileScreen(): React.JSX.Element {
  const { t } = useTranslation();
  const navigation = useNavigation<any>();
  const { theme } = useThemeStore();
  const { user, isGuest, userProfile, signOut, localProfileImage } = useAuthStore();
  const { plans } = useTravelStore();
  const C = useColors();

  const [locationCount, setLocationCount] = React.useState(0);

  React.useEffect(() => {
    if (user && !isGuest) {
      userService.getFavoriteLocations(user.uid).then(locs => setLocationCount(locs.length));
    }
  }, [user, isGuest]);

  const displayName = userProfile?.name ?? (isGuest ? t('profile.guestUser') : 'Kullanıcı');
  const email = userProfile?.email ?? (isGuest ? 'Misafir Modu' : '');

  const stats = [
    { label: 'Şehirler', value: locationCount.toString(), icon: 'location' },
    { label: 'Seyahatler', value: plans.length.toString(), icon: 'airplane' },
    { label: 'Soru', value: isGuest ? '0' : '42', icon: 'chatbubble' },
  ];

  const handleLogout = () => {
    Alert.alert('Çıkış Yap', 'Hesabınızdan çıkış yapmak istediğinize emin misiniz?', [
      { text: 'İptal', style: 'cancel' },
      { text: 'Çıkış Yap', style: 'destructive', onPress: () => signOut() },
    ]);
  };

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar barStyle={theme === 'light' ? 'dark-content' : 'light-content'} />
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scrollContent}>

        {/* Profile Header Card */}
        <View style={s.headerCard}>
          <View style={s.profileInfo}>
            <View style={s.avatarContainer}>
              {((userProfile?.photoURL && userProfile.photoURL.length > 0) || (localProfileImage && localProfileImage.length > 0)) ? (
                <Image source={{ uri: userProfile?.photoURL || localProfileImage || '' }} style={s.avatar} />
              ) : (
                <View style={[s.avatarPlaceholder, { backgroundColor: C.accent }]}>
                  <Text style={s.avatarInitial}>{displayName.charAt(0).toUpperCase()}</Text>
                </View>
              )}
              <TouchableOpacity style={s.editAvatarBadge} onPress={() => navigation.navigate('ProfileEdit')}>
                <Icon name="camera" size={14} color="#FFF" />
              </TouchableOpacity>
            </View>
            <View style={s.nameContainer}>
              <Text style={s.displayName}>{displayName}</Text>
              <Text style={s.emailText}>{email}</Text>
            </View>
          </View>

          <View style={s.statsRow}>
            {stats.map((stat, i) => (
              <View key={i} style={s.statItem}>
                <View style={[s.statIconBox, { backgroundColor: 'rgba(59, 130, 246, 0.1)' }]}>
                  <Icon name={stat.icon} size={18} color={C.accent} />
                </View>
                <Text style={s.statValue}>{stat.value}</Text>
                <Text style={s.statLabel}>{stat.label}</Text>
              </View>
            ))}
          </View>
        </View>

        {/* Action Sections */}
        <View style={s.section}>
          <Text style={s.sectionTitle}>KEŞFET</Text>
          <View style={s.menuCard}>
            <MenuRow
              icon="airplane-outline"
              label="Seyahat Planlarım"
              onPress={() => navigation.navigate('TravelCalendar')}
              C={C}
            />
            <View style={s.divider} />
            <MenuRow
              icon="heart-outline"
              label="Favori Şehirlerim"
              onPress={() => Alert.alert('Bilgi', 'Favori şehirler özelliği çok yakında!')}
              C={C}
            />
            <View style={s.divider} />
            <MenuRow
              icon="location-outline"
              label="Kayıtlı Konumlar"
              onPress={() => navigation.navigate('LocationManagement')}
              C={C}
            />
          </View>
        </View>

        <View style={s.section}>
          <Text style={s.sectionTitle}>HESAP</Text>
          <View style={s.menuCard}>
            <MenuRow
              icon="person-outline"
              label="Profili Düzenle"
              onPress={() => navigation.navigate('ProfileEdit')}
              C={C}
            />
            <View style={s.divider} />
            <MenuRow
              icon="settings-outline"
              label="Ayarlar"
              onPress={() => navigation.navigate('Settings')}
              C={C}
            />
          </View>
        </View>

        {!isGuest && (
          <TouchableOpacity style={s.logoutBtn} onPress={handleLogout}>
            <Icon name="log-out-outline" size={20} color={C.error} />
            <Text style={s.logoutText}>Çıkış Yap</Text>
          </TouchableOpacity>
        )}

        <Text style={s.versionText}>Havamania © 2024</Text>
      </ScrollView>
    </SafeAreaView>
  );
}

function MenuRow({ icon, label, onPress, C, rightElement }: any) {
  return (
    <TouchableOpacity style={styles.menuRow} onPress={onPress}>
      <View style={styles.menuLeft}>
        <View style={[styles.menuIconBox, { backgroundColor: 'rgba(255,255,255,0.05)' }]}>
          <Icon name={icon} size={20} color={C.text} />
        </View>
        <Text style={[styles.menuLabel, { color: C.text }]}>{label}</Text>
      </View>
      <View style={styles.menuRight}>
        {rightElement}
        <Icon name="chevron-forward" size={18} color={C.textMuted} />
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  menuRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 16 },
  menuLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  menuIconBox: { width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center' },
  menuLabel: { fontSize: 16, fontWeight: '500' },
  menuRight: { flexDirection: 'row', alignItems: 'center', gap: 8 },
});

const makeStyles = (C: AppColors) => StyleSheet.create({
  safe: { flex: 1, backgroundColor: C.bg },
  scrollContent: { padding: Spacing.lg },
  headerCard: { backgroundColor: C.bgCard, borderRadius: 24, padding: 24, marginBottom: 32, borderWidth: 1, borderColor: C.border },
  profileInfo: { flexDirection: 'row', alignItems: 'center', gap: 20, marginBottom: 24 },
  avatarContainer: { position: 'relative' },
  avatar: { width: 80, height: 80, borderRadius: 40 },
  avatarPlaceholder: { width: 80, height: 80, borderRadius: 40, justifyContent: 'center', alignItems: 'center' },
  avatarInitial: { fontSize: 32, fontWeight: '800', color: '#FFF' },
  editAvatarBadge: { position: 'absolute', bottom: 0, right: 0, width: 28, height: 28, borderRadius: 14, backgroundColor: C.accent, justifyContent: 'center', alignItems: 'center', borderWidth: 3, borderColor: C.bgCard },
  nameContainer: { flex: 1 },
  displayName: { fontSize: 24, fontWeight: '800', color: C.text },
  emailText: { fontSize: 14, color: C.textSecondary, marginTop: 4 },
  statsRow: { flexDirection: 'row', justifyContent: 'space-between', paddingTop: 20, borderTopWidth: 1, borderTopColor: C.divider },
  statItem: { alignItems: 'center', flex: 1 },
  statIconBox: { width: 40, height: 40, borderRadius: 12, justifyContent: 'center', alignItems: 'center', marginBottom: 8 },
  statValue: { fontSize: 18, fontWeight: '800', color: C.text },
  statLabel: { fontSize: 11, fontWeight: '600', color: C.textSecondary, marginTop: 2 },
  section: { marginBottom: 24 },
  sectionTitle: { fontSize: 12, fontWeight: '800', color: C.textMuted, letterSpacing: 1.5, marginBottom: 12, marginLeft: 4 },
  menuCard: { backgroundColor: C.bgCard, borderRadius: 20, overflow: 'hidden', borderWidth: 1, borderColor: C.border },
  divider: { height: 1, backgroundColor: C.divider, marginHorizontal: 16 },
  logoutBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, padding: 16, borderRadius: 16, backgroundColor: 'rgba(239, 68, 68, 0.05)', marginTop: 8 },
  logoutText: { color: C.error, fontSize: 16, fontWeight: '700' },
  versionText: { textAlign: 'center', fontSize: 12, color: C.textMuted, marginTop: 32, marginBottom: 16 },
});
