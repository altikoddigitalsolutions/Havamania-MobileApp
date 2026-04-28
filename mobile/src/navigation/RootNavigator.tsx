import React from 'react';
import {StatusBar} from 'react-native';

import {AuthNavigator} from './AuthNavigator';
import {MainStack} from './MainStack';
import {useAuthStore} from '../store/authStore';
import {useLanguageStore} from '../store/languageStore';
import {TravelInspiredSplash} from '../components/TravelInspiredSplash';

export function RootNavigator(): React.JSX.Element {
  const {isAuthenticated, initializing, initSession} = useAuthStore();
  const initLanguage = useLanguageStore(s => s.init);
  const [showSplash, setShowSplash] = React.useState(true);

  React.useEffect(() => {
    void initLanguage();
    initSession().catch(err => console.error('[DEBUG] RootNavigator: initSession error', err));
  }, [initSession, initLanguage]);

  // Splash ekranı en az 2 saniye görünsün (Premium deneyim için)
  React.useEffect(() => {
    if (!initializing) {
      const timer = setTimeout(() => {
        setShowSplash(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [initializing]);

  if (showSplash) {
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

  return isAuthenticated ? <MainStack /> : <AuthNavigator />;
}
