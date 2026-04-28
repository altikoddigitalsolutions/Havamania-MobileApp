import {create} from 'zustand';
import {updateProfile} from '../services/profileApi';

export type Theme = 'light' | 'dark' | 'spring' | 'summer' | 'autumn' | 'winter';

type ThemeState = {
  theme: Theme;
  animationsEnabled: boolean;
  setTheme: (theme: Theme, syncToServer?: boolean) => void;
  toggleTheme: (syncToServer?: boolean) => void;
  setAnimationsEnabled: (enabled: boolean) => void;
};

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: 'dark',
  animationsEnabled: true,
  setTheme: async (theme, syncToServer = false) => {
    set({theme});
    if (syncToServer) {
      try {
        const apiTheme = (theme === 'light' || theme === 'dark') ? theme : 'dark';
        await updateProfile({theme: apiTheme});
      } catch {
        // Sessizce geç
      }
    }
  },
  toggleTheme: async (syncToServer = false) => {
    const themes: Theme[] = ['light', 'dark', 'spring', 'summer', 'autumn', 'winter'];
    const currentIndex = themes.indexOf(get().theme);
    const nextIndex = (currentIndex + 1) % themes.length;
    const nextTheme = themes[nextIndex];

    set({theme: nextTheme});
    if (syncToServer) {
      try {
        const apiTheme = (nextTheme === 'light' || nextTheme === 'dark') ? nextTheme : 'dark';
        await updateProfile({theme: apiTheme});
      } catch {
        // Sessizce geç
      }
    }
  },
  setAnimationsEnabled: (enabled) => set({animationsEnabled: enabled}),
}));
