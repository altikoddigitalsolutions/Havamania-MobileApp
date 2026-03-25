import React from 'react';
import {ActivityIndicator, StyleSheet, Text, View} from 'react-native';

import {AuthNavigator} from './AuthNavigator';
import {MainStack} from './MainStack';
import {useAuthStore} from '../store/authStore';
import {DarkColors, LightColors} from '../theme';
import {useThemeStore} from '../store/themeStore';

export function RootNavigator(): React.JSX.Element {
  const {isAuthenticated, initializing, initSession} = useAuthStore();
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;

  React.useEffect(() => {
    initSession()
      .catch(err => console.error('[DEBUG] RootNavigator: initSession error', err));
  }, [initSession]);

  if (initializing) {
    return (
      <View style={[s.splash, {backgroundColor: C.bg}]}>
        <Text style={s.splashEmoji}>🌤️</Text>
        <Text style={[s.splashTitle, {color: C.text}]}>Havamania</Text>
        <ActivityIndicator
          size="large"
          color={C.accent}
          style={{marginTop: 32}}
        />
        <Text style={[s.splashSub, {color: C.textSecondary}]}>
          Hava durumu yükleniyor...
        </Text>
      </View>
    );
  }

  return isAuthenticated ? <MainStack /> : <AuthNavigator />;
}

const s = StyleSheet.create({
  splash: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  splashEmoji: {fontSize: 64, marginBottom: 8},
  splashTitle: {
    fontSize: 32,
    fontWeight: '800',
    letterSpacing: 1,
  },
  splashSub: {
    marginTop: 12,
    fontSize: 15,
  },
});
