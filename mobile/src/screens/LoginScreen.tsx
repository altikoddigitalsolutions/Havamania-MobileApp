import React, {useMemo, useState} from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';

import {useTranslation} from 'react-i18next';

import {AuthStackParamList} from '../navigation/types';
import {useAuthStore} from '../store/authStore';
import {DarkColors, FontSize, LightColors, Radius, Spacing} from '../theme';
import {useThemeStore} from '../store/themeStore';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

export function LoginScreen({navigation}: Props): React.JSX.Element {
  const {t} = useTranslation();
  const loginWithEmail = useAuthStore(state => state.loginWithEmail);
  const loginAsGuest = useAuthStore(state => state.loginAsGuest);
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const isValid = useMemo(
    () => email.includes('@') && password.length >= 6,
    [email, password],
  );

  const handleLogin = async () => {
    if (!isValid) {
      Alert.alert(t('common.error'), t('auth.loginBtn'));
      return;
    }
    try {
      setLoading(true);
      await loginWithEmail(email.trim(), password);
    } catch (err: any) {
      const msg = err?.response?.data?.detail ?? t('auth.loginBtn');
      Alert.alert(t('auth.loginBtn'), msg);
    } finally {
      setLoading(false);
    }
  };

  const s = styles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />
      <KeyboardAvoidingView
        style={{flex: 1}}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          contentContainerStyle={s.scroll}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}>

          {/* Geri Butonu */}
          <TouchableOpacity style={s.backBtn} onPress={() => navigation.canGoBack() && navigation.goBack()}>
            <Text style={s.backArrow}>‹</Text>
          </TouchableOpacity>

          {/* Logo */}
          <View style={s.logoWrapper}>
            <View style={s.logoCircle}>
              <Text style={s.logoEmoji}>☁️</Text>
            </View>
          </View>

          {/* Başlık */}
          <Text style={s.title}>{t('auth.welcomeBack')}</Text>
          <Text style={s.subtitle}>{t('auth.subtitle')}</Text>

          {/* Form */}
          <View style={s.form}>
            <Text style={s.label}>{t('auth.emailPlaceholder')}</Text>
            <TextInput
              style={s.input}
              placeholder="ornek@email.com"
              placeholderTextColor={C.textMuted}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
              value={email}
              onChangeText={setEmail}
            />

            <Text style={[s.label, {marginTop: Spacing.md}]}>{t('auth.passwordPlaceholder')}</Text>
            <View style={s.passwordRow}>
              <TextInput
                style={[s.input, {flex: 1, marginBottom: 0}]}
                placeholder="••••••••"
                placeholderTextColor={C.textMuted}
                secureTextEntry={!showPassword}
                value={password}
                onChangeText={setPassword}
              />
              <TouchableOpacity
                style={s.eyeBtn}
                onPress={() => setShowPassword(v => !v)}>
                <Text style={s.eyeText}>{showPassword ? '🙈' : '👁️'}</Text>
              </TouchableOpacity>
            </View>

            <TouchableOpacity style={s.forgotBtn}>
              <Text style={s.forgotText}>{t('auth.forgotPassword')}</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[s.loginBtn, (!isValid || loading) && s.loginBtnDisabled]}
              onPress={handleLogin}
              disabled={loading || !isValid}
              activeOpacity={0.85}>
              <Text style={s.loginBtnText}>
                {loading ? t('common.loading') : `${t('auth.loginBtn')}  →`}
              </Text>
            </TouchableOpacity>

            <View style={s.dividerRow}>
              <View style={s.dividerLine} />
              <Text style={s.dividerText}>{t('auth.orDivider')}</Text>
              <View style={s.dividerLine} />
            </View>

            <TouchableOpacity
              style={s.guestCard}
              onPress={loginAsGuest}
              activeOpacity={0.8}>
              <View style={s.guestIcon}>
                <Text style={{fontSize: 24}}>🤖</Text>
              </View>
              <View style={{flex: 1}}>
                <Text style={s.guestTitle}>{t('auth.guestTitle')}</Text>
                <Text style={s.guestDesc}>{t('auth.guestDesc')}</Text>
              </View>
            </TouchableOpacity>
          </View>

          <View style={s.signupRow}>
            <Text style={s.signupText}>{t('auth.noAccount')} </Text>
            <TouchableOpacity onPress={() => navigation.navigate('SignUp')}>
              <Text style={s.signupLink}>{t('auth.signupLink')}</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = (C: typeof DarkColors) =>
  StyleSheet.create({
    safe: {
      flex: 1,
      backgroundColor: C.bg,
    },
    scroll: {
      flexGrow: 1,
      paddingHorizontal: Spacing.lg,
      paddingBottom: Spacing.xl,
    },
    backBtn: {
      marginTop: Spacing.md,
      width: 36,
      height: 36,
      justifyContent: 'center',
    },
    backArrow: {
      fontSize: 32,
      color: C.text,
      lineHeight: 36,
    },
    logoWrapper: {
      alignItems: 'center',
      marginTop: Spacing.xl,
      marginBottom: Spacing.lg,
    },
    logoCircle: {
      width: 80,
      height: 80,
      borderRadius: 40,
      borderWidth: 2,
      borderColor: C.accent,
      backgroundColor: C.bgCard,
      justifyContent: 'center',
      alignItems: 'center',
    },
    logoEmoji: {
      fontSize: 36,
    },
    title: {
      fontSize: 32,
      fontWeight: '700',
      color: C.text,
      textAlign: 'center',
      marginBottom: Spacing.xs,
    },
    subtitle: {
      fontSize: FontSize.md,
      color: C.textSecondary,
      textAlign: 'center',
      marginBottom: Spacing.xl,
    },
    form: {
      gap: 0,
    },
    label: {
      fontSize: FontSize.sm,
      fontWeight: '600',
      color: C.text,
      marginBottom: Spacing.xs,
    },
    input: {
      backgroundColor: C.bgInput,
      borderRadius: Radius.md,
      paddingHorizontal: Spacing.md,
      paddingVertical: 14,
      fontSize: FontSize.md,
      color: C.text,
      marginBottom: Spacing.sm,
      borderWidth: 1,
      borderColor: C.border,
    },
    passwordRow: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: C.bgInput,
      borderRadius: Radius.md,
      borderWidth: 1,
      borderColor: C.border,
      marginBottom: Spacing.sm,
      paddingRight: Spacing.sm,
    },
    eyeBtn: {
      padding: Spacing.sm,
    },
    eyeText: {
      fontSize: 18,
    },
    forgotBtn: {
      alignSelf: 'flex-end',
      marginBottom: Spacing.lg,
    },
    forgotText: {
      fontSize: FontSize.sm,
      color: C.accent,
      fontWeight: '600',
    },
    loginBtn: {
      backgroundColor: C.accentBtn,
      borderRadius: Radius.full,
      paddingVertical: 16,
      alignItems: 'center',
      marginBottom: Spacing.lg,
    },
    loginBtnDisabled: {
      opacity: 0.5,
    },
    loginBtnText: {
      color: '#FFFFFF',
      fontSize: FontSize.lg,
      fontWeight: '700',
      letterSpacing: 0.5,
    },
    dividerRow: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: Spacing.lg,
      gap: Spacing.sm,
    },
    dividerLine: {
      flex: 1,
      height: 1,
      backgroundColor: C.border,
    },
    dividerText: {
      fontSize: FontSize.sm,
      color: C.textMuted,
      fontWeight: '600',
    },
    guestCard: {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: C.bgCard,
      borderRadius: Radius.lg,
      padding: Spacing.md,
      gap: Spacing.md,
      borderWidth: 1,
      borderColor: C.border,
    },
    guestIcon: {
      width: 44,
      height: 44,
      borderRadius: 22,
      backgroundColor: C.bgSecondary,
      justifyContent: 'center',
      alignItems: 'center',
    },
    guestTitle: {
      fontSize: FontSize.md,
      fontWeight: '700',
      color: C.text,
      marginBottom: 2,
    },
    guestDesc: {
      fontSize: FontSize.xs,
      color: C.textSecondary,
      lineHeight: 16,
    },
    signupRow: {
      flexDirection: 'row',
      justifyContent: 'center',
      alignItems: 'center',
      marginTop: Spacing.xl,
    },
    signupText: {
      fontSize: FontSize.sm,
      color: C.textSecondary,
    },
    signupLink: {
      fontSize: FontSize.sm,
      color: C.accent,
      fontWeight: '700',
    },
  });
