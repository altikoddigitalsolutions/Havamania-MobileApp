import {create} from 'zustand';

type Theme = 'light' | 'dark';

type ThemeState = {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
};

export const useThemeStore = create<ThemeState>(set => ({
  // Varsayılan: Dark mode (tasarımlara uygun)
  theme: 'dark',
  setTheme: theme => set({theme}),
  toggleTheme: () => set(state => ({theme: state.theme === 'dark' ? 'light' : 'dark'})),
}));
