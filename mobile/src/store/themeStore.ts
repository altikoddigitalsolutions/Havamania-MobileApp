import {create} from 'zustand';
import {updateProfile} from '../services/profileApi';

type Theme = 'light' | 'dark';

type ThemeState = {
  theme: Theme;
  setTheme: (theme: Theme, syncToServer?: boolean) => void;
  toggleTheme: (syncToServer?: boolean) => void;
};

export const useThemeStore = create<ThemeState>((set, get) => ({
  // Varsayılan: Dark mode (tasarımlara uygun)
  theme: 'dark',
  setTheme: async (theme, syncToServer = false) => {
    set({theme});
    if (syncToServer) {
      try {
        await updateProfile({theme});
      } catch {
        // Sessizce geç
      }
    }
  },
  toggleTheme: async (syncToServer = false) => {
    const newTheme = get().theme === 'dark' ? 'light' : 'dark';
    set({theme: newTheme});
    if (syncToServer) {
      try {
        await updateProfile({theme: newTheme});
      } catch {
        // Sessizce geç
      }
    }
  },
}));
