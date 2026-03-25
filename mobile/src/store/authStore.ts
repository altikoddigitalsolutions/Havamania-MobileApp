import {create} from 'zustand';

import {
  bootstrapSession,
  clearSession,
  login,
  logout,
  persistSession,
  registerRefreshInterceptor,
  signup,
} from '../services/authApi';
import {readTokens, saveTokens} from '../services/tokenStorage';
import {getProfile} from '../services/profileApi';
import {useThemeStore} from './themeStore';
import {useLanguageStore} from './languageStore';

interface AuthState {
  isAuthenticated: boolean;
  isGuest: boolean;
  initializing: boolean;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  signupWithEmail: (email: string, password: string, fullName?: string) => Promise<void>;
  logoutCurrentUser: () => Promise<void>;
  loginAsGuest: () => void;
  initSession: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  isAuthenticated: false,
  isGuest: false,
  initializing: true,

  loginWithEmail: async (email: string, password: string) => {
    const tokens = await login(email, password);
    await persistSession(tokens);
    set({isAuthenticated: true, isGuest: false});
    // Ayarları backend'den çek ve store'ları güncelle
    try {
      const profile = await getProfile();
      if (profile) {
        if (profile.theme) {
          useThemeStore.getState().setTheme(profile.theme as any, false);
        }
        if (profile.language) {
          await useLanguageStore.getState().setLanguage(profile.language as any, false);
        }
      }
    } catch {
      // Sessizce geç
    }
  },

  signupWithEmail: async (email: string, password: string, fullName?: string) => {
    const tokens = await signup(email, password, fullName);
    await persistSession(tokens);
    set({isAuthenticated: true, isGuest: false});
  },

  logoutCurrentUser: async () => {
    const stored = await readTokens();
    if (stored?.refreshToken) {
      try {
        await logout(stored.refreshToken);
      } catch {
        // ignore — misafir modunda veya ağ yoksa
      }
    }
    await clearSession();
    set({isAuthenticated: false, isGuest: false});
  },

  /** Kayıt/giriş olmadan uygulamayı kullan */
  loginAsGuest: () => {
    set({isAuthenticated: true, isGuest: true});
  },

  initSession: async () => {
    console.log('[DEBUG] authStore: initSession start');
    try {
      registerRefreshInterceptor(
        async (accessToken, refreshToken) => {
          await saveTokens(accessToken, refreshToken);
        },
        async () => {
          await clearSession();
          set({isAuthenticated: false, isGuest: false});
        },
      );
      console.log('[DEBUG] authStore: interceptor registered');

      const session = await bootstrapSession();
      console.log('[DEBUG] authStore: bootstrapSession result:', session ? 'SESSION_FOUND' : 'NO_SESSION');
      set({isAuthenticated: Boolean(session), initializing: false});
    } catch (err) {
      console.error('[DEBUG] authStore: initSession error', err);
      set({isAuthenticated: false, initializing: false});
    }
  },
}));
