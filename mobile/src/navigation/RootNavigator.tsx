import React from 'react';
import {StatusBar} from 'react-native';

import {AuthNavigator} from './AuthNavigator';
import {MainStack} from './MainStack';
import {useAuthStore} from '../store/authStore';
import {useLanguageStore} from '../store/languageStore';
import {TravelInspiredSplash} from '../components/TravelInspiredSplash';
import {onAuthStateChanged} from '../services/firebaseAuth';

export function RootNavigator(): React.JSX.Element {
  const {isAuthenticated, isGuest, initializing, setUser, loadLocalProfile} = useAuthStore();
  const initLanguage = useLanguageStore(s => s.init);
  const [showSplash, setShowSplash] = React.useState(true);

  React.useEffect(() => {
    // Firebase Auth aboneliği
    const unsubscribe = onAuthStateChanged(user => {
      setUser(user);
    });

    const initApp = async () => {
      try {
        await initLanguage();
        await loadLocalProfile();
      } catch (err) {
        console.error('[CRITICAL] App initialization failed:', err);
      }
    };

    void initApp();

    // Emniyet kilidi: 5 saniye sonra ne olursa olsun splash'i kapat
    const safetyTimer = setTimeout(() => {
      setShowSplash(false);
    }, 5000);

    return () => {
      unsubscribe();
      clearTimeout(safetyTimer);
    };
  }, [setUser, initLanguage]);

  // Auth durumu ve init bittiğinde splash'i kapat
  React.useEffect(() => {
    if (!initializing) {
      const timer = setTimeout(() => {
        setShowSplash(false);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [initializing]);

  if (showSplash || (initializing && !isGuest)) {
    return (
      <>
        <StatusBar
          barStyle="light-content"
          backgroundColor="transparent"
          translucent
        />
        <TravelInspiredSplash />
      </>
    );
  }

  return isAuthenticated || isGuest ? <MainStack /> : <AuthNavigator />;
}
