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
  ActivityIndicator,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import Icon from 'react-native-vector-icons/Ionicons';
import LinearGradient from 'react-native-linear-gradient';

import {AuthStackParamList} from '../navigation/types';
import {useAuthStore} from '../store/authStore';
import {useTheme} from '../theme';
import {useThemeStore} from '../store/themeStore';

type Props = NativeStackScreenProps<AuthStackParamList, 'Register'>;

export function RegisterScreen({navigation}: Props): React.JSX.Element {
  const signUp = useAuthStore(state => state.signUp);
  const loading = useAuthStore(state => state.loading);
  const {theme} = useThemeStore();
  const { colors: C, spacing, fontSize, radius, layout } = useTheme();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [termsAccepted, setTermsAccepted] = useState(false);

  const handleSignup = async () => {
    if (fullName.length < 2) {
      Alert.alert('Hata', 'Lütfen adınızı ve soyadınızı girin.');
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
    if (password !== confirmPassword) {
      Alert.alert('Hata', 'Şifreler eşleşmiyor.');
      return;
    }
    if (!termsAccepted) {
      Alert.alert('Hata', 'Lütfen kullanım koşullarını kabul edin.');
      return;
    }

    try {
      await signUp(email.trim(), password, fullName.trim());
    } catch (err: any) {
      let msg = 'Kayıt yapılamadı.';
      if (err.code === 'auth/email-already-in-use') {
        msg = 'Bu e-posta adresi zaten kullanımda.';
      } else if (err.code === 'auth/invalid-email') {
        msg = 'Geçersiz e-posta adresi.';
      } else if (err.code === 'auth/weak-password') {
        msg = 'Şifre çok zayıf. Lütfen daha güçlü bir şifre seçin.';
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
            <Text style={[s.title, { color: C.text, fontSize: fontSize.xxxl }]}>Kayıt Ol</Text>
            <Text style={[s.subtitle, { color: C.textSecondary, fontSize: fontSize.md }]}>
              Havamania ailesine katılarak seyahatlerini akıllıca planlamaya başla.
            </Text>
          </View>

          <View style={s.form}>
            <InputGroup label="AD SOYAD" icon="person-outline" placeholder="Ahmet Yılmaz" value={fullName} onChange={setFullName} C={C} fontSize={fontSize} />
            <InputGroup label="E-POSTA" icon="mail-outline" placeholder="ornek@email.com" value={email} onChange={setEmail} C={C} fontSize={fontSize} keyboardType="email-address" />

            <View style={s.inputGroup}>
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
                    <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={s.eyeBtn}>
                        <Icon name={showPassword ? 'eye-off-outline' : 'eye-outline'} size={20} color={C.textMuted} />
                    </TouchableOpacity>
                </View>
            </View>

            <InputGroup label="ŞİFRE TEKRAR" icon="lock-closed-outline" placeholder="••••••••" value={confirmPassword} onChange={setConfirmPassword} C={C} fontSize={fontSize} secureTextEntry={!showPassword} />

            <TouchableOpacity
              style={s.termsRow}
              onPress={() => setTermsAccepted(!termsAccepted)}
              activeOpacity={0.8}>
              <View style={[s.checkbox, { borderColor: C.accent }, termsAccepted && { backgroundColor: C.accent }]}>
                {termsAccepted && <Icon name="checkmark" size={14} color="#FFF" />}
              </View>
              <Text style={[s.termsText, { color: C.textSecondary, fontSize: 13 }]}>
                Kullanım koşullarını ve gizlilik politikasını kabul ediyorum.
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[s.signupBtn, loading && { opacity: 0.7 }]}
              onPress={handleSignup}
              disabled={loading}
              activeOpacity={0.85}>
              <LinearGradient
                colors={[C.accent, C.accentDark]}
                style={s.signupGradient}
                start={{x: 0, y: 0}}
                end={{x: 1, y: 0}}
              >
                {loading ? (
                    <ActivityIndicator color="#FFF" />
                ) : (
                    <Text style={[s.signupBtnText, { fontSize: fontSize.lg }]}>Hesap Oluştur</Text>
                )}
              </LinearGradient>
            </TouchableOpacity>
          </View>

          <View style={s.loginRow}>
            <Text style={[s.loginText, { color: C.textSecondary, fontSize: fontSize.sm }]}>Zaten hesabın var mı? </Text>
            <TouchableOpacity onPress={() => navigation.navigate('Login')}>
              <Text style={[s.loginLink, { color: C.accent, fontSize: fontSize.sm }]}>Giriş Yap</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function InputGroup({ label, icon, placeholder, value, onChange, C, fontSize, keyboardType = 'default', secureTextEntry = false }: any) {
    return (
        <View style={[s.inputGroup, { marginTop: 16 }]}>
            <Text style={[s.label, { color: C.textSecondary, fontSize: fontSize.xs }]}>{label}</Text>
            <View style={[s.inputWrapper, { backgroundColor: C.bgInput, borderColor: C.border }]}>
                <Icon name={icon} size={20} color={C.textMuted} style={s.inputIcon} />
                <TextInput
                    style={[s.input, { color: C.text, fontSize: fontSize.md }]}
                    placeholder={placeholder}
                    placeholderTextColor={C.textMuted}
                    value={value}
                    onChangeText={onChange}
                    keyboardType={keyboardType}
                    autoCapitalize={keyboardType === 'email-address' ? 'none' : 'words'}
                    autoCorrect={false}
                    secureTextEntry={secureTextEntry}
                />
            </View>
        </View>
    );
}

const s = StyleSheet.create({
  safe: { flex: 1 },
  scroll: { flexGrow: 1, paddingHorizontal: 24, paddingBottom: 40 },
  header: { flexDirection: 'row', alignItems: 'center', marginTop: 16, marginBottom: 24 },
  backBtn: { width: 44, height: 44, borderRadius: 12, justifyContent: 'center', alignItems: 'center', elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4 },
  logoWrapper: { flex: 1, alignItems: 'center', marginRight: 44 },
  logoImage: { width: 100, height: 32 },
  titles: { marginBottom: 24 },
  title: { fontWeight: '800', marginBottom: 8 },
  subtitle: { lineHeight: 22, opacity: 0.8 },
  form: { width: '100%' },
  inputGroup: { width: '100%' },
  label: { fontWeight: '700', letterSpacing: 1, marginBottom: 8, marginLeft: 4 },
  inputWrapper: { flexDirection: 'row', alignItems: 'center', borderRadius: 16, borderWidth: 1, paddingHorizontal: 16 },
  inputIcon: { marginRight: 12 },
  input: { flex: 1, height: 56, fontWeight: '500' },
  eyeBtn: { padding: 8 },
  termsRow: { flexDirection: 'row', alignItems: 'center', marginTop: 20, marginBottom: 24, gap: 12 },
  checkbox: { width: 22, height: 22, borderRadius: 6, borderWidth: 2, justifyContent: 'center', alignItems: 'center' },
  termsText: { flex: 1, fontWeight: '500' },
  signupBtn: { height: 58, borderRadius: 29, overflow: 'hidden', elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8 },
  signupGradient: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  signupBtnText: { color: '#FFFFFF', fontWeight: '800', letterSpacing: 0.5 },
  loginRow: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginTop: 24 },
  loginText: { fontWeight: '500' },
  loginLink: { fontWeight: '800' },
});
