import {create} from 'zustand';
import i18n, {SUPPORTED_LANGUAGES, SupportedLanguage, getDeviceLanguage} from '../i18n';
import {getStoredLanguage, saveLanguage} from '../services/preferencesStorage';
import { useAuthStore } from './authStore';

interface LanguageState {
  language: SupportedLanguage;
  initialized: boolean;
  init: () => Promise<void>;
  setLanguage: (lang: SupportedLanguage) => Promise<void>;
}

export const useLanguageStore = create<LanguageState>((set, get) => ({
  language: getDeviceLanguage(),
  initialized: false,

  init: async () => {
    if (get().initialized) return;
    try {
      const stored = await getStoredLanguage();
      const lang: SupportedLanguage =
        stored && SUPPORTED_LANGUAGES.includes(stored as SupportedLanguage)
          ? (stored as SupportedLanguage)
          : getDeviceLanguage();

      await i18n.changeLanguage(lang);
      set({language: lang, initialized: true});
    } catch {
      set({initialized: true});
    }
  },

  setLanguage: async (lang) => {
    try {
      await i18n.changeLanguage(lang);
      set({language: lang});
      await saveLanguage(lang);
      const { isAuthenticated, updateProfile } = useAuthStore.getState();
      if (isAuthenticated) {
        updateProfile({ language: lang });
      }
    } catch (err) {
      console.error('[LanguageStore] Failed to set language:', err);
    }
  },
}));
