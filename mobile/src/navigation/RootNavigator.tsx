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
    // Tüm init işlemlerini birleştir
    const initApp = async () => {
      try {
        await initLanguage();
        // initSession zaten kendi içinde catch barındırıyor ama garantiye alalım
        await initSession();
      } catch (err) {
        console.error('[CRITICAL] App initialization failed:', err);
      }
    };

    void initApp();

    // Emniyet kilidi: 5 saniye sonra ne olursa olsun splash'i kapat
    const safetyTimer = setTimeout(() => {
      setShowSplash(false);
    }, 5000);

    return () => clearTimeout(safetyTimer);
  }, [initSession, initLanguage]);

  // Splash ekranı süresini yönet
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

  // Burada isAuthenticated kontrolü var. Eğer kullanıcı login değilse AuthNavigator (Login) açılır.
  // Kullanıcı "doğrudan Hava ekranı" diyorsa, login olmasa bile Guest olarak girmesini istiyor olabilir.
  // Ancak mevcut yapıyı bozmadan en güvenli geçişi sağlıyoruz.
  return isAuthenticated ? <MainStack /> : <AuthNavigator />;
}
