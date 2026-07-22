import React, {useState} from 'react';
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

type Props = NativeStackScreenProps<AuthStackParamList, 'ForgotPassword'>;

export function ForgotPasswordScreen({navigation}: Props): React.JSX.Element {
  const resetPassword = useAuthStore(state => state.resetPassword);
  const {theme} = useThemeStore();
  const { colors: C, spacing, fontSize, radius, layout } = useTheme();

  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);

  const handleReset = async () => {
    if (!email.trim()) {
      Alert.alert('Hata', 'Lütfen e-posta adresinizi girin.');
      return;
    }
    if (!email.includes('@')) {
      Alert.alert('Hata', 'Lütfen geçerli bir e-posta adresi girin.');
      return;
    }

    try {
      setLoading(true);
      await resetPassword(email.trim());
      Alert.alert(
        'Başarılı',
        'Şifre sıfırlama bağlantısı e-posta adresinize gönderildi. Lütfen gelen kutunuzu kontrol edin.',
        [{text: 'Tamam', onPress: () => navigation.navigate('Login')}]
      );
    } catch (err: any) {
      let msg = 'Şifre sıfırlama e-postası gönderilemedi.';
      if (err.code === 'auth/user-not-found') {
        msg = 'Bu e-posta adresine kayıtlı bir kullanıcı bulunamadı.';
      } else if (err.code === 'auth/invalid-email') {
        msg = 'Geçersiz e-posta adresi.';
      } else if (err.code === 'auth/network-request-failed') {
        msg = 'İnternet bağlantısı yok.';
      }
      Alert.alert('Hata', msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[s.safe, { backgroundColor: C.bg }]}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />

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

      <KeyboardAvoidingView
        style={{flex: 1}}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScrollView
          contentContainerStyle={[s.scroll, { maxWidth: layout.maxContentWidth, alignSelf: 'center', width: '100%' }]}
          keyboardShouldPersistTaps="handled">

          <View style={s.titles}>
            <Text style={[s.title, { color: C.text, fontSize: fontSize.xxxl }]}>Şifremi Unuttum</Text>
            <Text style={[s.subtitle, { color: C.textSecondary, fontSize: fontSize.md }]}>
              E-posta adresini girerek şifreni sıfırlamak için bir bağlantı alabilirsin.
            </Text>
          </View>

          <View style={s.form}>
            <View style={s.inputGroup}>
              <Text style={[s.label, { color: C.textSecondary, fontSize: fontSize.xs }]}>E-POSTA ADRESİ</Text>
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

            <TouchableOpacity
              style={[s.resetBtn, loading && { opacity: 0.7 }]}
              onPress={handleReset}
              disabled={loading}
              activeOpacity={0.85}>
              <LinearGradient
                colors={[C.accent, C.accentDark]}
                style={s.resetGradient}
                start={{x: 0, y: 0}}
                end={{x: 1, y: 0}}
              >
                {loading ? (
                    <ActivityIndicator color="#FFF" />
                ) : (
                    <Text style={[s.resetBtnText, { fontSize: fontSize.lg }]}>Sıfırlama Bağlantısı Gönder</Text>
                )}
              </LinearGradient>
            </TouchableOpacity>

            <TouchableOpacity
                style={s.backToLoginBtn}
                onPress={() => navigation.navigate('Login')}
            >
                <Text style={[s.backToLoginText, { color: C.textSecondary, fontSize: fontSize.sm }]}>
                    Giriş ekranına geri dön
                </Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 24, marginTop: 16, marginBottom: 32 },
  backBtn: { width: 44, height: 44, borderRadius: 12, justifyContent: 'center', alignItems: 'center', elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4 },
  logoWrapper: { flex: 1, alignItems: 'center', marginRight: 44 },
  logoImage: { width: 100, height: 32 },
  scroll: { flexGrow: 1, paddingHorizontal: 24, paddingBottom: 40 },
  titles: { marginBottom: 32 },
  title: { fontWeight: '800', marginBottom: 8 },
  subtitle: { lineHeight: 22, opacity: 0.8 },
  form: { width: '100%' },
  inputGroup: { width: '100%' },
  label: { fontWeight: '700', letterSpacing: 1, marginBottom: 8, marginLeft: 4 },
  inputWrapper: { flexDirection: 'row', alignItems: 'center', borderRadius: 16, borderWidth: 1, paddingHorizontal: 16 },
  inputIcon: { marginRight: 12 },
  input: { flex: 1, height: 56, fontWeight: '500' },
  resetBtn: { height: 58, borderRadius: 29, overflow: 'hidden', marginTop: 32, elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8 },
  resetGradient: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  resetBtnText: { color: '#FFFFFF', fontWeight: '800', letterSpacing: 0.5 },
  backToLoginBtn: { alignSelf: 'center', marginTop: 24, padding: 8 },
  backToLoginText: { fontWeight: '600', textDecorationLine: 'underline' },
});
