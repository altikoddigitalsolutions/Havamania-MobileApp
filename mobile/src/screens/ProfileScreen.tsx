import React, {useState} from 'react';
import {
  Alert,
  Modal,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Switch,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {useTranslation} from 'react-i18next';

import {getProfile} from '../services/profileApi';
import {saveAvatarEmoji} from '../services/preferencesStorage';
import {useAuthStore} from '../store/authStore';
import {useThemeStore} from '../store/themeStore';
import {useLanguageStore} from '../store/languageStore';
import {AppColors, DarkColors, FontSize, LightColors, Radius, Spacing} from '../theme';
import {SupportedLanguage, SUPPORTED_LANGUAGES} from '../i18n';

// ── Emoji havuzu (avatar seçici için) ────────────────────────────────────────
const AVATAR_EMOJIS = [
  '🧑','👩','👨','🧔','👧','👦','🧒','👴','👵',
  '🌤️','⛅','☀️','🌧️','❄️','🌪️','🌈','⛈️','🌊',
  '🦁','🐻','🦊','🐸','🦜','🦅','🦉','🐼','🦋',
  '🤖','👾','🎭','🎪','🔮','🌙','⭐','🌺','🏔️',
];

export function ProfileScreen(): React.JSX.Element {
  const {t} = useTranslation();
  const {theme, setTheme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;
  const {isGuest, logoutCurrentUser} = useAuthStore();
  const {language, setLanguage} = useLanguageStore();

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
    retry: false,
    enabled: !isGuest,
  });

  const [notifEnabled, setNotifEnabled] = useState(true);
  const [tempUnit, setTempUnit] = useState<'C' | 'F'>('C');
  const [avatarEmoji, setAvatarEmoji] = useState('🧑');
  const [showAvatarPicker, setShowAvatarPicker] = useState(false);
  const [showLangPicker, setShowLangPicker] = useState(false);

  const profile = profileQuery.data;
  const displayName = profile?.full_name ?? (isGuest ? t('profile.guestUser') : t('profile.defaultUser'));
  const memberSince = profile?.created_at
    ? formatMemberSince(profile.created_at, t('profile.memberSince'))
    : t('profile.defaultMember');

  const handleLogout = () => {
    Alert.alert(t('profile.logoutTitle'), t('profile.logoutMsg'), [
      {text: t('profile.logoutCancel'), style: 'cancel'},
      {
        text: t('profile.logoutConfirm'),
        style: 'destructive',
        onPress: () => void logoutCurrentUser(),
      },
    ]);
  };

  const handleSelectAvatar = async (emoji: string) => {
    setAvatarEmoji(emoji);
    setShowAvatarPicker(false);
    await saveAvatarEmoji(emoji);
  };

  const handleSelectLanguage = async (lang: SupportedLanguage) => {
    setShowLangPicker(false);
    await setLanguage(lang, !isGuest);
  };

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />

      {/* ── Header ── */}
      <View style={s.header}>
        <View style={s.logoRow}>
          <Text style={s.logoIcon}>☁️</Text>
          <Text style={s.logoText}>Havamania</Text>
        </View>
      </View>

      <ScrollView showsVerticalScrollIndicator={false}>
        {/* ── Avatar & Kullanıcı Bilgisi ── */}
        <View style={s.profileSection}>
          <TouchableOpacity style={s.avatarWrap} onPress={() => setShowAvatarPicker(true)}>
            <View style={s.avatar}>
              <Text style={s.avatarEmoji}>{avatarEmoji}</Text>
            </View>
            <View style={s.avatarEditBadge}>
              <Text style={s.avatarEditText}>✏️</Text>
            </View>
          </TouchableOpacity>

          <Text style={s.changeAvatarHint}>{t('profile.changeAvatar')}</Text>
          <Text style={s.displayName}>{displayName}</Text>
          {!isGuest && (
            <View style={s.locationRow}>
              <Text style={s.locationPin}>📍</Text>
              <Text style={s.locationText}>{profile?.location ?? 'İstanbul, TR'}</Text>
            </View>
          )}
          <Text style={s.memberSince}>{memberSince}</Text>

          {/* İstatistikler */}
          <View style={s.statsRow}>
            <StatCard value={isGuest ? '0' : '12'} label={t('profile.cities')} icon="🔖" C={C} />
            <View style={s.statDivider} />
            <StatCard value={isGuest ? '0' : '148'} label={t('profile.aiAsked')} icon="💬" C={C} />
            <View style={s.statDivider} />
            <StatCard value={t('profile.sunny')} label={t('profile.favorite')} icon="☀️" C={C} />
          </View>
        </View>

        {/* ── Ayarlar ── */}
        <View style={s.settingsSection}>
          <Text style={s.settingsSectionTitle}>{t('profile.settingsTitle')}</Text>

          <View style={s.settingsCard}>
            {/* Sıcaklık Birimi */}
            <TouchableOpacity
              style={s.settingsRow}
              onPress={() => setTempUnit(u => (u === 'C' ? 'F' : 'C'))}>
              <View style={s.settingsLeft}>
                <View style={[s.settingsIconBox, {backgroundColor: '#1A5C9A'}]}>
                  <Text style={s.settingsIconText}>🌡️</Text>
                </View>
                <Text style={s.settingsLabel}>{t('profile.temperatureUnit')}</Text>
              </View>
              <View style={s.settingsRight}>
                <Text style={s.settingsValue}>
                  {tempUnit === 'C' ? t('profile.celsius') : t('profile.fahrenheit')}
                </Text>
                <Text style={s.settingsChevron}>›</Text>
              </View>
            </TouchableOpacity>

            <View style={s.rowDivider} />

            {/* Dil */}
            <TouchableOpacity style={s.settingsRow} onPress={() => setShowLangPicker(true)}>
              <View style={s.settingsLeft}>
                <View style={[s.settingsIconBox, {backgroundColor: '#1A4C2A'}]}>
                  <Text style={s.settingsIconText}>🌐</Text>
                </View>
                <Text style={s.settingsLabel}>{t('profile.language')}</Text>
              </View>
              <View style={s.settingsRight}>
                <Text style={s.settingsValue}>
                  {t(`languages.${language}`)}
                </Text>
                <Text style={s.settingsChevron}>›</Text>
              </View>
            </TouchableOpacity>

            <View style={s.rowDivider} />

            {/* Bildirimler */}
            <View style={s.settingsRow}>
              <View style={s.settingsLeft}>
                <View style={[s.settingsIconBox, {backgroundColor: '#1A5C9A'}]}>
                  <Text style={s.settingsIconText}>🔔</Text>
                </View>
                <Text style={s.settingsLabel}>{t('profile.notifications')}</Text>
              </View>
              <Switch
                value={notifEnabled}
                onValueChange={setNotifEnabled}
                trackColor={{false: C.border, true: C.accent}}
                thumbColor="#FFFFFF"
              />
            </View>

            <View style={s.rowDivider} />

            {/* Gizlilik */}
            <TouchableOpacity style={s.settingsRow}>
              <View style={s.settingsLeft}>
                <View style={[s.settingsIconBox, {backgroundColor: '#1A5C9A'}]}>
                  <Text style={s.settingsIconText}>🛡️</Text>
                </View>
                <Text style={s.settingsLabel}>{t('profile.privacyPolicy')}</Text>
              </View>
              <Text style={s.settingsChevron}>↗</Text>
            </TouchableOpacity>

            <View style={s.rowDivider} />

            {/* Tema */}
            <TouchableOpacity
              style={s.settingsRow}
              onPress={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
              <View style={s.settingsLeft}>
                <View style={[s.settingsIconBox, {backgroundColor: '#1A5C9A'}]}>
                  <Text style={s.settingsIconText}>{theme === 'dark' ? '🌙' : '☀️'}</Text>
                </View>
                <Text style={s.settingsLabel}>{t('profile.appearance')}</Text>
              </View>
              <View style={s.settingsRight}>
                <Text style={s.settingsValue}>
                  {theme === 'dark' ? t('profile.dark') : t('profile.light')}
                </Text>
                <Text style={s.settingsChevron}>›</Text>
              </View>
            </TouchableOpacity>

            <View style={s.rowDivider} />

            {/* Çıkış Yap */}
            <TouchableOpacity style={s.settingsRow} onPress={handleLogout}>
              <View style={s.settingsLeft}>
                <View style={[s.settingsIconBox, {backgroundColor: '#3A1515'}]}>
                  <Text style={s.settingsIconText}>🚪</Text>
                </View>
                <Text style={[s.settingsLabel, {color: DarkColors.error}]}>
                  {t('profile.logout')}
                </Text>
              </View>
            </TouchableOpacity>
          </View>
        </View>

        <Text style={s.footerText}>
          HAVAMANIA {isGuest ? t('profile.freeLabel') : t('profile.proLabel')}
        </Text>
        <View style={{height: Spacing.xxl}} />
      </ScrollView>

      {/* ── Emoji Avatar Seçici Modal ── */}
      <Modal
        visible={showAvatarPicker}
        transparent
        animationType="slide"
        onRequestClose={() => setShowAvatarPicker(false)}>
        <TouchableOpacity style={s.modalOverlay} activeOpacity={1} onPress={() => setShowAvatarPicker(false)}>
          <View style={s.modalSheet}>
            <View style={s.modalHandle} />
            <Text style={s.modalTitle}>{t('profile.avatarPickerTitle')}</Text>
            <View style={s.emojiGrid}>
              {AVATAR_EMOJIS.map(emoji => (
                <TouchableOpacity
                  key={emoji}
                  style={[s.emojiCell, avatarEmoji === emoji && s.emojiCellActive]}
                  onPress={() => void handleSelectAvatar(emoji)}>
                  <Text style={s.emojiCellText}>{emoji}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* ── Dil Seçici Modal ── */}
      <Modal
        visible={showLangPicker}
        transparent
        animationType="slide"
        onRequestClose={() => setShowLangPicker(false)}>
        <TouchableOpacity style={s.modalOverlay} activeOpacity={1} onPress={() => setShowLangPicker(false)}>
          <View style={[s.modalSheet, {paddingBottom: 40}]}>
            <View style={s.modalHandle} />
            <Text style={s.modalTitle}>{t('profile.languagePickerTitle')}</Text>
            {SUPPORTED_LANGUAGES.map(lang => (
              <TouchableOpacity
                key={lang}
                style={[s.langRow, language === lang && s.langRowActive]}
                onPress={() => void handleSelectLanguage(lang)}>
                <Text style={s.langFlag}>{lang === 'tr' ? '🇹🇷' : '🇬🇧'}</Text>
                <Text style={[s.langName, language === lang && {color: C.accent}]}>
                  {t(`languages.${lang}`)}
                </Text>
                {language === lang && <Text style={s.langCheck}>✓</Text>}
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
}

// ── StatCard ─────────────────────────────────────────────────────────────────
function StatCard({value, label, icon, C}: {value: string; label: string; icon: string; C: AppColors}) {
  const s = statStyles(C);
  return (
    <View style={s.card}>
      <Text style={s.icon}>{icon}</Text>
      <Text style={s.value}>{value}</Text>
      <Text style={s.label}>{label}</Text>
    </View>
  );
}

const statStyles = (C: AppColors) =>
  StyleSheet.create({
    card: {flex: 1, alignItems: 'center', gap: 4},
    icon: {fontSize: 22},
    value: {fontSize: FontSize.xxl, fontWeight: '800', color: C.text},
    label: {fontSize: FontSize.xs, color: C.textSecondary, fontWeight: '700', letterSpacing: 0.5},
  });

// ── Yardımcılar ───────────────────────────────────────────────────────────────
function formatMemberSince(isoDate: string, prefix: string): string {
  try {
    const d = new Date(isoDate);
    const month = d.toLocaleDateString('tr', {month: 'short'}).toUpperCase();
    return `${prefix} ${month} ${d.getFullYear()}`;
  } catch {
    return `${prefix} 2024`;
  }
}

// ── Stiller ──────────────────────────────────────────────────────────────────
const makeStyles = (C: AppColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},

    header: {
      flexDirection: 'row',
      justifyContent: 'space-between',
      alignItems: 'center',
      paddingHorizontal: Spacing.lg,
      paddingVertical: Spacing.md,
    },
    logoRow: {flexDirection: 'row', alignItems: 'center', gap: 6},
    logoIcon: {fontSize: 22},
    logoText: {fontSize: FontSize.lg, fontWeight: '800', color: C.text},

    profileSection: {
      alignItems: 'center',
      paddingHorizontal: Spacing.lg,
      paddingBottom: Spacing.xl,
    },
    avatarWrap: {position: 'relative', marginBottom: Spacing.xs},
    avatar: {
      width: 90,
      height: 90,
      borderRadius: 45,
      backgroundColor: C.bgCard,
      justifyContent: 'center',
      alignItems: 'center',
      borderWidth: 2,
      borderColor: C.accent,
      shadowColor: C.accent,
      shadowOffset: {width: 0, height: 4},
      shadowOpacity: 0.3,
      shadowRadius: 8,
      elevation: 6,
    },
    avatarEmoji: {fontSize: 44},
    avatarEditBadge: {
      position: 'absolute',
      bottom: 0,
      right: 0,
      width: 26,
      height: 26,
      borderRadius: 13,
      backgroundColor: C.accent,
      justifyContent: 'center',
      alignItems: 'center',
    },
    avatarEditText: {fontSize: 13},
    changeAvatarHint: {
      fontSize: FontSize.xs,
      color: C.accent,
      fontWeight: '600',
      marginBottom: Spacing.sm,
    },
    displayName: {
      fontSize: FontSize.xxl + 4,
      fontWeight: '800',
      color: C.text,
      marginBottom: Spacing.xs,
    },
    locationRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 4,
      marginBottom: Spacing.xs,
    },
    locationPin: {fontSize: 14},
    locationText: {fontSize: FontSize.md, color: C.accent, fontWeight: '600'},
    memberSince: {
      fontSize: FontSize.xs,
      color: C.textSecondary,
      fontWeight: '700',
      letterSpacing: 1,
      marginBottom: Spacing.lg,
    },

    statsRow: {
      flexDirection: 'row',
      backgroundColor: C.bgCard,
      borderRadius: Radius.lg,
      padding: Spacing.lg,
      width: '100%',
      borderWidth: 1,
      borderColor: C.border,
    },
    statDivider: {
      width: 1,
      backgroundColor: C.divider,
      marginHorizontal: Spacing.sm,
    },

    settingsSection: {paddingHorizontal: Spacing.lg},
    settingsSectionTitle: {
      fontSize: FontSize.xs,
      fontWeight: '800',
      color: C.textSecondary,
      letterSpacing: 1,
      marginBottom: Spacing.sm,
    },
    settingsCard: {
      backgroundColor: C.bgCard,
      borderRadius: Radius.lg,
      borderWidth: 1,
      borderColor: C.border,
      overflow: 'hidden',
    },
    settingsRow: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: Spacing.md,
    },
    settingsLeft: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: Spacing.md,
    },
    settingsIconBox: {
      width: 36,
      height: 36,
      borderRadius: 10,
      justifyContent: 'center',
      alignItems: 'center',
    },
    settingsIconText: {fontSize: 18},
    settingsLabel: {
      fontSize: FontSize.md,
      color: C.text,
      fontWeight: '500',
    },
    settingsRight: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 4,
    },
    settingsValue: {
      fontSize: FontSize.sm,
      color: C.textSecondary,
    },
    settingsChevron: {
      fontSize: FontSize.lg,
      color: C.textSecondary,
    },
    rowDivider: {
      height: 0.5,
      backgroundColor: C.divider,
      marginHorizontal: Spacing.md,
    },

    footerText: {
      textAlign: 'center',
      fontSize: FontSize.xs,
      color: C.textMuted,
      fontWeight: '700',
      letterSpacing: 1,
      marginTop: Spacing.xl,
    },

    // Modal
    modalOverlay: {
      flex: 1,
      backgroundColor: 'rgba(0,0,0,0.5)',
      justifyContent: 'flex-end',
    },
    modalSheet: {
      backgroundColor: C.bgCard,
      borderTopLeftRadius: Radius.xl,
      borderTopRightRadius: Radius.xl,
      padding: Spacing.lg,
    },
    modalHandle: {
      width: 40,
      height: 4,
      backgroundColor: C.border,
      borderRadius: 2,
      alignSelf: 'center',
      marginBottom: Spacing.md,
    },
    modalTitle: {
      fontSize: FontSize.xl,
      fontWeight: '800',
      color: C.text,
      textAlign: 'center',
      marginBottom: Spacing.lg,
    },

    // Emoji Grid
    emojiGrid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      justifyContent: 'center',
      gap: 8,
    },
    emojiCell: {
      width: 56,
      height: 56,
      borderRadius: 14,
      backgroundColor: C.bgInput,
      justifyContent: 'center',
      alignItems: 'center',
      borderWidth: 2,
      borderColor: 'transparent',
    },
    emojiCellActive: {
      borderColor: C.accent,
      backgroundColor: C.cardHourlyActive,
    },
    emojiCellText: {fontSize: 28},

    // Dil Seçici
    langRow: {
      flexDirection: 'row',
      alignItems: 'center',
      padding: Spacing.md,
      borderRadius: Radius.md,
      gap: Spacing.md,
      marginBottom: Spacing.xs,
    },
    langRowActive: {
      backgroundColor: C.bgInput,
    },
    langFlag: {fontSize: 28},
    langName: {
      fontSize: FontSize.lg,
      fontWeight: '600',
      color: C.text,
      flex: 1,
    },
    langCheck: {
      fontSize: FontSize.xl,
      color: C.accent,
      fontWeight: '800',
    },
  });
