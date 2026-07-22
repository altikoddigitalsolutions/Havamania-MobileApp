import React, {useMemo, useState} from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Image,
  Dimensions,
  ActivityIndicator,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {useTranslation} from 'react-i18next';
import Icon from 'react-native-vector-icons/Ionicons';
import LinearGradient from 'react-native-linear-gradient';

import {AuthStackParamList} from '../navigation/types';
import {useAuthStore} from '../store/authStore';
import {useTheme} from '../theme';
import {useThemeStore} from '../store/themeStore';

const { width } = Dimensions.get('window');

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

export function LoginScreen({navigation}: Props): React.JSX.Element {
  const {t} = useTranslation();
  const signIn = useAuthStore(state => state.signIn);
  const loading = useAuthStore(state => state.loading);
  const {theme} = useThemeStore();
  const { colors: C, spacing, fontSize, radius, layout } = useTheme();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const isValid = useMemo(
    () => email.includes('@') && password.length >= 6,
    [email, password],
  );

  const handleLogin = async () => {
    if (!email.trim()) {
      Alert.alert('Hata', 'Lütfen e-posta adresinizi girin.');
      return;
    }
    if (!email.includes('@')) {
      Alert.alert('Hata', 'Lütfen geçerli bir e-posta adresi girin.');
      return;
    }
    if (password.length < 6) {
      Alert.alert('Hata', 'Şifre en az 6 karakter olmalıdır.');
      return;
    }

    try {
      await signIn(email.trim(), password);
    } catch (err: any) {
      let msg = 'Giriş yapılamadı.';
      if (err.code === 'auth/user-not-found' || err.code === 'auth/wrong-password' || err.code === 'auth/invalid-credential') {
        msg = 'E-posta veya şifre hatalı.';
      } else if (err.code === 'auth/too-many-requests') {
        msg = 'Çok fazla hatalı deneme yapıldı. Lütfen daha sonra tekrar deneyin.';
      } else if (err.code === 'auth/invalid-email') {
        msg = 'Geçersiz e-posta adresi.';
      } else if (err.code === 'auth/user-disabled') {
        msg = 'Bu hesap devre dışı bırakılmış.';
      } else if (err.code === 'auth/network-request-failed') {
        msg = 'İnternet bağlantısı yok.';
      }
      Alert.alert('Hata', msg);
    }
  };

  return (
    <SafeAreaView style={[s.safe, { backgroundColor: C.bg }]}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />
      <KeyboardAvoidingView
        style={{flex: 1}}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          contentContainerStyle={[s.scroll, { maxWidth: layout.maxContentWidth, alignSelf: 'center', width: '100%' }]}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}>

          <View style={s.header}>
            <TouchableOpacity
              style={[s.backBtn, { backgroundColor: C.bgSecondary }]}
              onPress={() => navigation.goBack()}
            >
              <Icon name="arrow-back" size={24} color={C.text} />
            </TouchableOpacity>
            <View style={s.logoWrapper}>
              <Image
                source={require('../assets/havamania_logo_clean.png')}
                style={s.logoImage}
                resizeMode="contain"
              />
            </View>
          </View>

          <View style={s.titles}>
            <Text style={[s.title, { color: C.text, fontSize: fontSize.xxxl }]}>Giriş Yap</Text>
            <Text style={[s.subtitle, { color: C.textSecondary, fontSize: fontSize.md }]}>
              Hava durumuna göre planladığın seyahatlerine devam et.
            </Text>
          </View>

          <View style={s.form}>
            <View style={s.inputGroup}>
              <Text style={[s.label, { color: C.textSecondary, fontSize: fontSize.xs }]}>E-POSTA</Text>
              <View style={[s.inputWrapper, { backgroundColor: C.bgInput, borderColor: C.border }]}>
                <Icon name="mail-outline" size={20} color={C.textMuted} style={s.inputIcon} />
                <TextInput
                  style={[s.input, { color: C.text, fontSize: fontSize.md }]}
                  placeholder="ornek@email.com"
                  placeholderTextColor={C.textMuted}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                  value={email}
                  onChangeText={setEmail}
                />
              </View>
            </View>

            <View style={[s.inputGroup, { marginTop: 20 }]}>
              <Text style={[s.label, { color: C.textSecondary, fontSize: fontSize.xs }]}>ŞİFRE</Text>
              <View style={[s.inputWrapper, { backgroundColor: C.bgInput, borderColor: C.border }]}>
                <Icon name="lock-closed-outline" size={20} color={C.textMuted} style={s.inputIcon} />
                <TextInput
                  style={[s.input, { color: C.text, fontSize: fontSize.md }]}
                  placeholder="••••••••"
                  placeholderTextColor={C.textMuted}
                  secureTextEntry={!showPassword}
                  value={password}
                  onChangeText={setPassword}
                />
                <TouchableOpacity
                  style={s.eyeBtn}
                  onPress={() => setShowPassword(v => !v)}>
                  <Icon name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={20} color={C.textMuted} />
                </TouchableOpacity>
              </View>
            </View>

            <TouchableOpacity
              onPress={() => navigation.navigate('ForgotPassword')}
              style={s.forgotPassBtn}>
              <Text style={[s.forgotPassText, { color: C.accent, fontSize: fontSize.sm }]}>Şifremi Unuttum</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[s.loginBtn, loading && { opacity: 0.7 }]}
              onPress={handleLogin}
              disabled={loading}
              activeOpacity={0.85}>
              <LinearGradient
                colors={[C.accent, C.accentDark]}
                style={s.loginGradient}
                start={{x: 0, y: 0}}
                end={{x: 1, y: 0}}
              >
                {loading ? (
                    <ActivityIndicator color="#FFF" />
                ) : (
                    <Text style={[s.loginBtnText, { fontSize: fontSize.lg }]}>Giriş Yap</Text>
                )}
              </LinearGradient>
            </TouchableOpacity>
          </View>

          <View style={s.signupRow}>
            <Text style={[s.signupText, { color: C.textSecondary, fontSize: fontSize.sm }]}>Hesabın yok mu? </Text>
            <TouchableOpacity onPress={() => navigation.navigate('Register')}>
              <Text style={[s.signupLink, { color: C.accent, fontSize: fontSize.sm }]}>Kayıt Ol</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1 },
  scroll: { flexGrow: 1, paddingHorizontal: 24, paddingBottom: 40 },
  header: { flexDirection: 'row', alignItems: 'center', marginTop: 16, marginBottom: 32 },
  backBtn: { width: 44, height: 44, borderRadius: 12, justifyContent: 'center', alignItems: 'center', elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4 },
  logoWrapper: { flex: 1, alignItems: 'center', marginRight: 44 },
  logoImage: { width: 120, height: 40 },
  titles: { marginBottom: 32 },
  title: { fontWeight: '800', marginBottom: 8 },
  subtitle: { lineHeight: 22, opacity: 0.8 },
  form: { width: '100%' },
  inputGroup: { width: '100%' },
  label: { fontWeight: '700', letterSpacing: 1, marginBottom: 8, marginLeft: 4 },
  inputWrapper: { flexDirection: 'row', alignItems: 'center', borderRadius: 16, borderWidth: 1, paddingHorizontal: 16 },
  inputIcon: { marginRight: 12 },
  input: { flex: 1, height: 56, fontWeight: '500' },
  eyeBtn: { padding: 8 },
  forgotPassBtn: { alignSelf: 'flex-end', marginTop: 12, marginBottom: 32, paddingVertical: 4 },
  forgotPassText: { fontWeight: '700' },
  loginBtn: { height: 58, borderRadius: 29, overflow: 'hidden', elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8 },
  loginGradient: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  loginBtnText: { color: '#FFFFFF', fontWeight: '800', letterSpacing: 0.5 },
  signupRow: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginTop: 32 },
  signupText: { fontWeight: '500' },
  signupLink: { fontWeight: '800' },
});
