import {create} from 'zustand';

type Theme = 'light' | 'dark';

type ThemeState = {
  theme: Theme;
  setTheme: (theme: Theme) => void;
};

export const useThemeStore = create<ThemeState>(set => ({
  theme: 'light',
  setTheme: theme => set({theme}),
}));
