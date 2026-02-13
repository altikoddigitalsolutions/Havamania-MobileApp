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

interface AuthState {
  isAuthenticated: boolean;
  initializing: boolean;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  signupWithEmail: (email: string, password: string, fullName?: string) => Promise<void>;
  logoutCurrentUser: () => Promise<void>;
  initSession: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  isAuthenticated: false,
  initializing: true,

  loginWithEmail: async (email: string, password: string) => {
    const tokens = await login(email, password);
    await persistSession(tokens);
    set({isAuthenticated: true});
  },

  signupWithEmail: async (email: string, password: string, fullName?: string) => {
    const tokens = await signup(email, password, fullName);
    await persistSession(tokens);
    set({isAuthenticated: true});
  },

  logoutCurrentUser: async () => {
    const stored = await readTokens();
    if (stored?.refreshToken) {
      try {
        await logout(stored.refreshToken);
      } catch {
        // ignore
      }
    }
    await clearSession();
    set({isAuthenticated: false});
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
          set({isAuthenticated: false});
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
