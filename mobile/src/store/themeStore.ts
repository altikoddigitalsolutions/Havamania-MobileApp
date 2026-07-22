import {create} from 'zustand';
import { useAuthStore } from './authStore';

export type Theme = 'light' | 'dark' | 'spring' | 'summer' | 'autumn' | 'winter';

type ThemeState = {
  theme: Theme;
  animationsEnabled: boolean;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
  setAnimationsEnabled: (enabled: boolean) => void;
};

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: 'dark',
  animationsEnabled: true,
  setTheme: (theme) => {
    set({theme});
    const { isAuthenticated, updateProfile } = useAuthStore.getState();
    if (isAuthenticated) {
      updateProfile({ theme });
    }
  },
  toggleTheme: () => {
    const themes: Theme[] = ['light', 'dark', 'spring', 'summer', 'autumn', 'winter'];
    const currentIndex = themes.indexOf(get().theme);
    const nextIndex = (currentIndex + 1) % themes.length;
    const nextTheme = themes[nextIndex];
    set({theme: nextTheme});
  },
  setAnimationsEnabled: (enabled) => set({animationsEnabled: enabled}),
}));
