import {create} from 'zustand';

import i18n, {SUPPORTED_LANGUAGES, SupportedLanguage, getDeviceLanguage} from '../i18n';
import {getStoredLanguage, saveLanguage} from '../services/preferencesStorage';
import {updateProfile} from '../services/profileApi';

interface LanguageState {
  language: SupportedLanguage;
  initialized: boolean;
  /** Uygulama açılışında çağrılır. Depodan veya cihaz dilinden okur. */
  init: () => Promise<void>;
  /** Dili değiştirir, depoya kaydeder, sunucuyla senkronize eder. */
  setLanguage: (lang: SupportedLanguage, syncToServer?: boolean) => Promise<void>;
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

  setLanguage: async (lang, syncToServer = false) => {
    await i18n.changeLanguage(lang);
    set({language: lang});
    await saveLanguage(lang);

    if (syncToServer) {
      try {
        await updateProfile({language: lang});
      } catch {
        // Sunucu yoksa sessizce geç; yerel ayar zaten kaydedildi
      }
    }
  },
}));
