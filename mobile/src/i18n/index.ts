import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';

const resources = {
  tr: {
    translation: {
      home: 'Anasayfa',
      map: 'Harita',
      alerts: 'Uyarilar',
      settings: 'Ayarlar',
      login: 'Giris Yap',
      signup: 'Kaydol',
    },
  },
};

void i18n.use(initReactI18next).init({
  compatibilityJSON: 'v4',
  lng: 'tr',
  fallbackLng: 'tr',
  resources,
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
