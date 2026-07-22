import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  Image,
  SafeAreaView,
  StatusBar,
  Dimensions,
  ImageBackground,
} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';
import {AuthStackParamList} from '../navigation/types';
import {useTheme} from '../theme';
import {useThemeStore} from '../store/themeStore';
import LinearGradient from 'react-native-linear-gradient';
import { useAuthStore } from '../store/authStore';
import Icon from 'react-native-vector-icons/Ionicons';

const { width } = Dimensions.get('window');

type Props = NativeStackScreenProps<AuthStackParamList, 'AuthWelcome'>;

export function AuthWelcomeScreen({navigation}: Props): React.JSX.Element {
  const { colors: C, spacing, fontSize, radius, layout } = useTheme();
  const {theme} = useThemeStore();
  const loginAsGuest = useAuthStore(state => state.loginAsGuest);

  return (
    <View style={s.root}>
      <StatusBar barStyle="light-content" translucent backgroundColor="transparent" />
      <ImageBackground
        source={require('../assets/splash_travel_bg.png')}
        style={StyleSheet.absoluteFill}
        resizeMode="cover"
      >
        <LinearGradient
          colors={['rgba(2, 6, 23, 0.3)', 'rgba(2, 6, 23, 0.7)', 'rgba(2, 6, 23, 0.95)']}
          style={StyleSheet.absoluteFill}
        />

        <SafeAreaView style={s.safe}>
          <View style={[s.container, { maxWidth: layout.maxContentWidth, alignSelf: 'center' }]}>

            <View style={s.header}>
              <AnimatedLogo />
            </View>

            <View style={s.content}>
              <Text style={[s.title, { fontSize: fontSize.xxxl }]}>
                Havamania
              </Text>
              <Text style={[s.slogan, { color: 'rgba(255,255,255,0.85)', fontSize: fontSize.md }]}>
                Hava durumunu akıllıca takip et,{'\n'}seyahatlerini akıllıca planla.
              </Text>
            </View>

            <View style={s.footer}>
              <TouchableOpacity
                style={s.primaryBtn}
                onPress={() => navigation.navigate('Login')}
                activeOpacity={0.8}
              >
                <LinearGradient
                  colors={[C.accent, C.accentDark]}
                  style={s.gradient}
                  start={{x: 0, y: 0}}
                  end={{x: 1, y: 0}}
                >
                  <Text style={[s.primaryBtnText, { fontSize: fontSize.lg }]}>Giriş Yap</Text>
                </LinearGradient>
              </TouchableOpacity>

              <TouchableOpacity
                style={[s.secondaryBtn, { borderColor: 'rgba(255,255,255,0.2)' }]}
                onPress={() => navigation.navigate('Register')}
                activeOpacity={0.7}
              >
                <Text style={[s.secondaryBtnText, { color: '#FFFFFF', fontSize: fontSize.lg }]}>Kayıt Ol</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={s.guestBtn}
                onPress={loginAsGuest}
                activeOpacity={0.6}
              >
                <Text style={[s.guestBtnText, { color: 'rgba(255,255,255,0.5)', fontSize: fontSize.sm }]}>
                  Misafir olarak devam et
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </SafeAreaView>
      </ImageBackground>
    </View>
  );
}

function AnimatedLogo() {
    return (
        <View style={s.logoContainer}>
             <View style={s.logoCircle}>
                <Image
                    source={require('../assets/havamania_logo_clean.png')}
                    style={s.logoImage}
                    resizeMode="contain"
                />
             </View>
        </View>
    );
}

const s = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#020617' },
  safe: { flex: 1 },
  container: { flex: 1, width: '100%', paddingHorizontal: 32, justifyContent: 'space-between' },
  header: { flex: 1.2, justifyContent: 'center', alignItems: 'center' },
  logoContainer: { alignItems: 'center', justifyContent: 'center' },
  logoCircle: {
    width: width * 0.35,
    height: width * 0.35,
    borderRadius: width * 0.2,
    backgroundColor: 'rgba(255,255,255,0.05)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.1)'
  },
  logoImage: { width: '70%', height: '70%' },
  content: { alignItems: 'center', marginBottom: 40 },
  title: { fontWeight: '900', color: '#FFFFFF', letterSpacing: 2, marginBottom: 12 },
  slogan: { textAlign: 'center', lineHeight: 24, fontWeight: '500' },
  footer: { marginBottom: 40, gap: 16 },
  primaryBtn: { height: 58, borderRadius: 29, overflow: 'hidden', elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8 },
  gradient: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  primaryBtnText: { color: '#FFF', fontWeight: '800', letterSpacing: 0.5 },
  secondaryBtn: { height: 58, borderRadius: 29, borderWidth: 1.5, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.05)' },
  secondaryBtnText: { fontWeight: '700', letterSpacing: 0.5 },
  guestBtn: { alignItems: 'center', marginTop: 8, padding: 8 },
  guestBtnText: { fontWeight: '600', textDecorationLine: 'underline' },
});
