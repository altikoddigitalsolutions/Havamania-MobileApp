import i18n from 'i18next';
import {initReactI18next} from 'react-i18next';

import tr from './tr';
import en from './en';

export const SUPPORTED_LANGUAGES = ['tr', 'en'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

export function getDeviceLanguage(): SupportedLanguage {
  try {
    // Intl is available in React Native 0.73+
    const locale = Intl.DateTimeFormat().resolvedOptions().locale; // e.g. "tr-TR", "en-US"
    const lang = locale.split('-')[0].toLowerCase() as SupportedLanguage;
    return SUPPORTED_LANGUAGES.includes(lang) ? lang : 'en';
  } catch {
    return 'en';
  }
}

void i18n.use(initReactI18next).init({
  compatibilityJSON: 'v3',
  lng: getDeviceLanguage(),
  fallbackLng: 'en',
  resources: {
    tr: {translation: tr},
    en: {translation: en},
  },
  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
